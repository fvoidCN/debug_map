package com.intellij.debugmap.model

data class BreakpointDef(
  override val groupId: Int,
  override val fileUrl: String,
  override val line: Int,
  /** Zero-based column; 0 = whole-line breakpoint, positive = inline (lambda) breakpoint. */
  val column: Int = 0,
  val typeId: String = "java-line",
  val condition: String? = null,
  val logExpression: String? = null,
  override val name: String? = null,
) : LocationDef(groupId, fileUrl, line, name) {

  override fun sameLocation(other: LocationDef): Boolean =
    other is BreakpointDef && fileUrl == other.fileUrl && line == other.line && column == other.column
}
