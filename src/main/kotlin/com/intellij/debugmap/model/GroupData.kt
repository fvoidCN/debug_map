package com.intellij.debugmap.model

data class GroupData(
  val id: Int,
  val name: String,
  val description: String = "",
  val status: GroupStatus = GroupStatus.OPEN,
  val breakpoints: List<BreakpointDef> = emptyList(),
  val bookmarks: List<BookmarkDef> = emptyList(),
)
