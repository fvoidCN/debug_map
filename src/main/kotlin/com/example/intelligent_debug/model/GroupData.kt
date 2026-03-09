package com.example.intelligent_debug.model

data class GroupData(
  val id: Int,
  val name: String,
  val breakpoints: List<BreakpointDef> = emptyList(),
  val bookmarks: List<BookmarkDef> = emptyList(),
)
