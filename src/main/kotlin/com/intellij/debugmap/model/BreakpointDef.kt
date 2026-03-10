package com.intellij.debugmap.model

data class BreakpointDef(
  override val groupId: Int,
  override val fileUrl: String,
  override val line: Int,
  val typeId: String = "java-line",
  val condition: String? = null,
  val logExpression: String? = null,
  override val name: String? = null,
) : LocationDef(groupId, fileUrl, line, name)
