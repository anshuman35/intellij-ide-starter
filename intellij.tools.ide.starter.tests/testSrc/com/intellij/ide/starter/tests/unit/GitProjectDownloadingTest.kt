package com.intellij.ide.starter.tests.unit

import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.path.GlobalPaths
import com.intellij.ide.starter.project.GitProjectInfo
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.kodein.di.instance
import java.util.concurrent.TimeUnit

class GitProjectDownloadingTest {

  @Test
  @Timeout(value = 2, unit = TimeUnit.MINUTES)
  fun downloadMatplotlibCheatsheetsRepo() {
    val matplotlibCheatsheetsGitProject = GitProjectInfo(repositoryUrl = "https://github.com/matplotlib/cheatsheets.git")

    val projectPath = matplotlibCheatsheetsGitProject.downloadAndUnpackProject()

    projectPath.shouldExist()

    val globalPaths by di.instance<GlobalPaths>()
    projectPath.parent.shouldBe(globalPaths.getCacheDirectoryFor("projects").resolve("unpacked"))
  }
}