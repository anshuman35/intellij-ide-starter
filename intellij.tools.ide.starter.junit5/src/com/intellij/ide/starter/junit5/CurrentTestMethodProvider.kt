package com.intellij.ide.starter.junit5

import com.intellij.ide.starter.di.di
import com.intellij.ide.starter.runner.CurrentTestMethod
import com.intellij.ide.starter.runner.TestMethod
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.kodein.di.direct
import org.kodein.di.instance

/**
 * The listener do provides [CurrentTestMethod] to DI
 */
open class CurrentTestMethodProvider : TestExecutionListener {

  override fun executionStarted(testIdentifier: TestIdentifier?) {
    if (testIdentifier?.isTest != true) {
      return
    }

    val methodSource = testIdentifier.source.get() as MethodSource
    di.direct.instance<CurrentTestMethod>().set(TestMethod(
      name = methodSource.methodName,
      declaringClass = methodSource.javaClass.simpleName,
      displayName = testIdentifier.displayName)
    )
  }
}