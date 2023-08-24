package com.intellij.ide.starter.ide.installer

import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.ide.*
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.utils.logOutput
import org.kodein.di.direct
import org.kodein.di.instance
import kotlin.io.path.createDirectories
import kotlin.io.path.div

class SimpleInstaller : IdeInstallator {

  override fun install(ideInfo: IdeInfo): Pair<String, InstalledIde> {
    val installersDirectory = (GlobalPaths.instance.installersDirectory / ideInfo.productCode).createDirectories()

    //Download
    val ideInstaller = di.direct.instance<IdeDownloader>().downloadIdeInstaller(ideInfo, installersDirectory)
    val installDir = GlobalPaths.instance.getCacheDirectoryFor("builds") / "${ideInfo.productCode}-${ideInstaller.buildNumber}"

    if (ideInstaller.buildNumber == "SNAPSHOT") {
      logOutput("Cleaning up SNAPSHOT IDE installation $installDir")
      installDir.toFile().deleteRecursively()
    }

    //Unpack
    IdeArchiveExtractor.unpackIdeIfNeeded(ideInstaller.installerFile.toFile(), installDir.toFile())

    //Install
    return Pair(ideInstaller.buildNumber, IdeDistributionFactory.installIDE(installDir.toFile(), ideInfo.executableFileName))
  }
}