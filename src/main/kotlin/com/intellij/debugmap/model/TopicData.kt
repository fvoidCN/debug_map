package com.intellij.debugmap.model

data class TopicData(
  val id: Int,
  val name: String,
  val description: String = "",
  val status: TopicStatus = TopicStatus.OPEN,
  val breakpoints: List<BreakpointDef> = emptyList(),
  val bookmarks: List<BookmarkDef> = emptyList(),
)
