package com.intellij.debugmap.model

data class BreakpointDef(
  override val topicId: Int,
  override val fileUrl: String,
  override val line: Int,
  /** Zero-based column; 0 = whole-line breakpoint, positive = inline (lambda) breakpoint. */
  val column: Int = 0,
  val typeId: String = "java-line",
  val condition: String? = null,
  val logExpression: String? = null,
  override val name: String? = null,
  /** null means IDE default (true). */
  val enabled: Boolean? = null,
  /** Whether to log a standard hit message to the console. null means IDE default (false). */
  val logMessage: Boolean? = null,
  /** Whether to log a full call-stack trace to the console. null means IDE default (false). */
  val logStack: Boolean? = null,
  /** Suspend policy name: "ALL", "THREAD", or "NONE". null means IDE default ("ALL"). */
  val suspendPolicy: String? = null,
  /** Stable id of the master breakpoint this one depends on, or null if no dependency. */
  val masterBreakpointId: Long? = null,
  /** If true, this breakpoint stays enabled after the master fires; if false, fires once then disables. */
  val masterLeaveEnabled: Boolean? = null,
  override val id: Long = kotlin.random.Random.nextLong(),
) : LocationDef(topicId, fileUrl, line, name, id) {

  override fun sameLocation(other: LocationDef): Boolean =
    other is BreakpointDef && fileUrl == other.fileUrl && line == other.line && column == other.column
}
