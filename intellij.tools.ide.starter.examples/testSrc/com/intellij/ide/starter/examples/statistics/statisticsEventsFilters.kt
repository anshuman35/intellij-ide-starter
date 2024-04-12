package com.intellij.ide.starter.extended.statistics

import com.jetbrains.fus.reporting.model.lion3.LogEvent


fun <T> LogEvent.getDataFromEvent(dataFilter: (Map.Entry<String, Any>) -> Boolean): Map<String, T> {
  return this.event.data.filter(dataFilter).map { entry -> entry.key to entry.value as T }.toMap()
}

fun <T> LogEvent.getDataFromEvent(dataKey: String): T {
  return getDataFromEvent<T> { it.key == dataKey }[dataKey] as T
}

/**
 * Get field `data` from the event
 * Value will be converted with:
 * T - from type
 * M - to type
 */
fun <T, M> LogEvent.getDataFromEvent(dataKey: String, typeConverter: T.() -> M): M {
  val originalData = getDataFromEvent<T> { it.key == dataKey }[dataKey]

  requireNotNull(originalData) {
    """
    Value from logEvent item by key='$dataKey' is null.
    Event: ${this.print()}
    """.trimIndent()
  }

  return typeConverter(originalData)
}

fun Iterable<LogEvent>.filterByEventDataKey(dataKey: String): List<LogEvent> {
  return this
    .filter {
      try {
        it.getDataFromEvent<Any, String>(dataKey) { this.toString() }
        true
      }
      catch (ignored: Throwable) {
        false
      }
    }
}

/**
 * Shortcut for filtering, using internal field `data` of event
 */
fun Iterable<LogEvent>.filterByEventDataKey(dataKey: String, expectedPropertyValue: String): List<LogEvent> {
  return this
    .filter {
      it.getDataFromEvent<String>(dataKey) == expectedPropertyValue
    }
}

fun Iterable<LogEvent>.filterByEventDataKey(dataKey: String, expectedPropertyValue: Boolean): List<LogEvent> {
  return this
    .filter {
      it.getDataFromEvent<Boolean>(dataKey) == expectedPropertyValue
    }
}

/**
 * If the event data key isn't found - key will be null in returned map
 */
fun <T> Iterable<LogEvent>.groupByEventDataKey(dataKey: String): Map<T, List<LogEvent>> {
  val rawMap = this
    .groupBy {
      try {
        it.getDataFromEvent<T, T>(dataKey) { this }
      }
      catch (ignored: Throwable) {
        null
      }
    }.filterNot { it.key == null }

  return rawMap as Map<T, List<LogEvent>>
}

fun <T, M> Iterable<LogEvent>.groupByEventDataKey(dataKey: String, typeConverter: T.() -> M): Map<M, List<LogEvent>> {
  val rawMap = this
    .groupBy {
      try {
        it.getDataFromEvent<T, M>(dataKey) { typeConverter(this) }
      }
      catch (ignored: Throwable) {
        null
      }
    }.filterNot { it.key == null }

  return rawMap as Map<M, List<LogEvent>>
}

fun Iterable<LogEvent>.filterByEventId(vararg eventId: String): List<LogEvent> {
  return this.filter { it.event.id in eventId }
}