package com.intellij.ide.starter.screenRecorder

import com.intellij.ide.starter.ide.DEFAULT_DISPLAY_ID
import com.intellij.ide.starter.ide.DEFAULT_DISPLAY_RESOLUTION
import com.intellij.ide.starter.runner.IDERunContext
import com.intellij.openapi.util.SystemInfo
import com.intellij.tools.ide.util.common.logOutput
import org.monte.media.Format
import org.monte.media.FormatKeys.MediaType
import org.monte.media.Registry
import org.monte.media.VideoFormatKeys.*
import org.monte.media.math.Rational
import org.monte.screenrecorder.ScreenRecorder
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Toolkit
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.pathString

class IDEScreenRecorder(private val runContext: IDERunContext) {

  companion object {
    fun getScreenRecorder(movieFolder: File, filePrefix: String? = null) =
      object : ScreenRecorder(
        /* cfg = */
        GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration,

        /* captureArea = */
        Rectangle(0, 0, Toolkit.getDefaultToolkit().screenSize.width, Toolkit.getDefaultToolkit().screenSize.height),

        /* fileFormat = */
        Format(MediaTypeKey, MediaType.FILE, MimeTypeKey, MIME_AVI),

        /* screenFormat = */
        Format(MediaTypeKey, MediaType.VIDEO,
               EncodingKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
               CompressorNameKey, ENCODING_AVI_TECHSMITH_SCREEN_CAPTURE,
               DepthKey, 24,
               FrameRateKey, Rational.valueOf(15.0),
               QualityKey, 1.0f,
               KeyFrameIntervalKey, 15 * 60),

        /* mouseFormat = */
        Format(MediaTypeKey, MediaType.VIDEO,
               EncodingKey, "black",
               FrameRateKey, Rational.valueOf(30.0)),


        /* audioFormat = */
        null,

        /* movieFolder = */
        movieFolder) {
        override fun createMovieFile(fileFormat: Format): File {
          if (filePrefix == null) {
            return super.createMovieFile(fileFormat)
          }
          else {
            return movieFolder.resolve(getMovieName(fileFormat, filePrefix))
          }
        }
      }

    private fun getMovieName(fileFormat: Format?, filePrefix: String?): String {
      val dateFormat = SimpleDateFormat("yyyy-MM-dd 'at' HH.mm.ss")
      val prefix = filePrefix ?: "ScreenRecording"
      val extension = Registry.getInstance().getExtension(fileFormat)
      val timestamp = dateFormat.format(Date())

      return "$prefix ${timestamp}.$extension"
    }
  }

  var javaScreenRecorder: ScreenRecorder? = null
  var ffmpegProcess: Process? = null

  init {
    //on Linux, we run xvfb and test process is headless, so we need external tool to record screen
    if (!SystemInfo.isLinux) {
      javaScreenRecorder = runCatching { getScreenRecorder((runContext.logsDir / "screenRecording").toFile()) }.getOrNull()
    }
  }

  fun start() {
    if (javaScreenRecorder != null) {
      javaScreenRecorder?.start()
    }
    else if (SystemInfo.isLinux) {
      synchronized(this) {
        if (ffmpegProcess == null) {
          ffmpegProcess = runCatching { startFFMpegRecording(runContext) }.getOrElse {
            logOutput("Can't start ffmpeg recording: ${it.message}")
            null
          }
        }
      }
    }
  }

  fun stop() {
    javaScreenRecorder?.stop()
    ffmpegProcess?.destroy()
  }

  private fun startFFMpegRecording(ideRunContext: IDERunContext): Process? {
    val processVmOptions = ideRunContext.calculateVmOptions()
    val resolution = DEFAULT_DISPLAY_RESOLUTION
    val processDisplay = processVmOptions.environmentVariables["DISPLAY"] ?: System.getenv("DISPLAY") ?: ":$DEFAULT_DISPLAY_ID"
    val recordingFile = ideRunContext.logsDir / "screen-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH_mm_ss_SSS"))}.mkv"
    val ffmpegLogFile = (ideRunContext.logsDir / "ffmpeg-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH_mm_ss_SSS"))}.log").also { it.createFile() }
    val args = listOf("/usr/bin/ffmpeg", "-f", "x11grab", "-video_size", resolution, "-framerate", "24", "-i",
                      processDisplay,
                      "-codec:v", "libx264", "-preset", "superfast", recordingFile.pathString)
    logOutput("Start screen recording to $recordingFile\nArgs: ${args.joinToString(" ")}")

    //we can't use ProcessExecutor since its start method is blocking and we need a handle to process to stop it
    val processBuilder = ProcessBuilder(args)
    processBuilder.redirectError(ffmpegLogFile.toFile())
    processBuilder.redirectOutput(ffmpegLogFile.toFile())
    return processBuilder.start()
  }
}