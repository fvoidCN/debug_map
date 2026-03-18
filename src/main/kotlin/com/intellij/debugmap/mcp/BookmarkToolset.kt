@file:Suppress("FunctionName", "unused")

package com.intellij.debugmap.mcp

import com.intellij.debugmap.DebugMapBundle
import com.intellij.debugmap.DebugMapService
import com.intellij.debugmap.model.BookmarkDef
import com.intellij.ide.bookmark.BookmarkType
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.mcpserver.project
import com.intellij.mcpserver.reportToolActivity
import com.intellij.mcpserver.toolsets.Constants
import com.intellij.mcpserver.util.resolveInProject
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.Serializable

class BookmarkToolset : McpToolset {

  @McpTool(name = "debug_bookmark_list")
  @McpDescription("""
        |Lists bookmarks in the project, optionally filtered by topic and/or path substring.
    """)
  suspend fun list_bookmarks(
    @McpDescription("Filter by topic name. Omit to include all topics.")
    topic: String? = null,
    @McpDescription("Filter by path substring. Omit to include all files.")
    path: String? = null,
  ): BookmarkListResult {
    currentCoroutineContext().reportToolActivity(DebugMapBundle.message("tool.activity.listing.bookmarks"))
    val project = currentCoroutineContext().project

    val service = DebugMapService.getInstance(project)

    val items = mutableListOf<BookmarkInfo>()
    val activeTopicId = service.getActiveTopicId()
    for (t in service.getTopics()) {
      if (topic != null && t.name != topic) continue
      val isActive = t.id == activeTopicId
      for (bookmark in t.bookmarks) {
        if (path != null && !bookmark.fileUrl.contains(path)) continue
        val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(project.resolveInProject(bookmark.fileUrl))
        items.add(BookmarkInfo(
          path = bookmark.fileUrl,
          line = bookmark.line + 1,
          topic = t.name,
          active = isActive,
          name = bookmark.name,
          mnemonic = bookmark.type.takeIf { it != BookmarkType.DEFAULT }?.mnemonic?.toString(),
          content = file?.let { lineContent(it, bookmark.line) }
        ))
      }
    }

    return BookmarkListResult(bookmarks = items, total = items.size)
  }

  @McpTool(name = "debug_bookmark_upsert")
  @McpDescription("""
        |Creates or updates a line bookmark at the specified file and line within a topic.
        |If the bookmark already exists in the topic, updates its description and mnemonic.
        |If it does not exist, creates it. The topic is created automatically if needed.
    """)
  suspend fun upsert_bookmark(
    @McpDescription(Constants.RELATIVE_PATH_IN_PROJECT_DESCRIPTION)
    path: String,
    @McpDescription("1-based line number where the bookmark should be placed")
    line: Int,
    @McpDescription("Source text of the line to bookmark")
    content: String,
    @McpDescription("Bookmark topic name. Created automatically if it does not exist.")
    topic: String,
    @McpDescription("Optional description text for the bookmark. Pass empty string to clear.")
    description: String? = null,
    @McpDescription("Optional single-character mnemonic ('0'-'9' or 'A'-'Z'). Leave empty for a plain bookmark.")
    mnemonic: String? = null,
  ): BookmarkResult {
    currentCoroutineContext().reportToolActivity(DebugMapBundle.message("tool.activity.upserting.bookmark", path, line))
    val project = currentCoroutineContext().project
    val service = DebugMapService.getInstance(project)

    val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(project.resolveInProject(path))
               ?: mcpFail("File not found: $path")

    val lineZeroBased = resolveLineByContent(file, line - 1, content) ?: run {
      val actual = lineContent(file, line - 1)
      mcpFail("Line $line contains '${actual ?: ""}', not '$content'. Re-read the file and pass the exact source text of the target line.")
    }

    val topicId = service.getTopicIdByName(topic) ?: service.createTopic(topic)
    val existing = service.getTopicBookmarks(topicId).firstOrNull { it.fileUrl == file.url && it.line == lineZeroBased }

    if (existing != null) {
      service.renameBookmark(existing, description ?: "")
      return BookmarkResult(path = path, line = line, status = "updated")
    }

    service.addBookmarkByToolWindow(topicId, BookmarkDef(
      topicId = topicId,
      fileUrl = file.url,
      line = lineZeroBased,
      name = description?.takeIf { it.isNotBlank() },
      type = resolveBookmarkType(mnemonic),
    ))

    return BookmarkResult(path = path, line = line, status = "created")
  }

  @McpTool(name = "debug_bookmark_remove")
  @McpDescription("""
        |Removes the line bookmark at the specified file and line.
        |If topic is specified, removes only from that topic; otherwise removes from the active topic.
        |Reports not_found if no matching bookmark exists.
    """)
  suspend fun remove_bookmark(
    @McpDescription(Constants.RELATIVE_PATH_IN_PROJECT_DESCRIPTION)
    path: String,
    @McpDescription("1-based line number of the bookmark to remove")
    line: Int,
    @McpDescription("Bookmark topic name. Defaults to the currently active topic if omitted.")
    topic: String? = null,
  ): BookmarkResult {
    currentCoroutineContext().reportToolActivity(DebugMapBundle.message("tool.activity.removing.bookmark", path, line))
    val project = currentCoroutineContext().project
    val service = DebugMapService.getInstance(project)

    val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(project.resolveInProject(path))
               ?: mcpFail("File not found: $path")

    val lineZeroBased = line - 1

    val topicId = if (topic != null) {
      service.getTopicIdByName(topic) ?: mcpFail("Bookmark topic not found: $topic")
    }
    else {
      service.getActiveTopicId() ?: mcpFail("No active topic")
    }

    val exists = service.getTopicBookmarks(topicId).any { it.fileUrl == file.url && it.line == lineZeroBased }
    if (!exists) return BookmarkResult(path = path, line = line, status = "not_found")

    service.removeBookmarkByToolWindow(topicId, file.url, lineZeroBased)

    return BookmarkResult(path = path, line = line, status = "removed")
  }

  // ----- helpers -----

  private fun resolveBookmarkType(mnemonic: String?): BookmarkType {
    val ch = mnemonic?.trim()?.firstOrNull()?.uppercaseChar() ?: return BookmarkType.DEFAULT
    return BookmarkType.get(ch)
  }

  // ----- data classes -----

  @Serializable
  data class BookmarkInfo(
    val path: String,
    val line: Int?,
    val topic: String,
    val active: Boolean,
    val name: String? = null,
    val mnemonic: String? = null,
    val content: String? = null,
  )

  @Serializable
  data class BookmarkResult(
    val path: String,
    val line: Int,
    /** "created" | "updated" | "removed" | "not_found" */
    val status: String,
  )

  @Serializable
  data class BookmarkListResult(
    val bookmarks: List<BookmarkInfo>,
    val total: Int,
  )
}
