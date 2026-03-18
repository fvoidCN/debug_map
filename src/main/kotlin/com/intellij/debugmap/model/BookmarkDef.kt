package com.intellij.debugmap.model

import com.intellij.ide.bookmark.BookmarkType

data class BookmarkDef(
  override val topicId: Int,
  override val fileUrl: String,
  override val line: Int,
  override val name: String? = null,
  val type: BookmarkType = BookmarkType.DEFAULT,
  override val id: Long = kotlin.random.Random.nextLong(),
) : LocationDef(topicId, fileUrl, line, name, id)
