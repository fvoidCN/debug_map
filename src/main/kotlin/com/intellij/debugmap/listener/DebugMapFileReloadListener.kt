package com.intellij.debugmap.listener

import com.intellij.debugmap.DebugMapService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.diff.Diff
import com.intellij.util.diff.Diff.Change
import com.intellij.util.diff.FilesTooBigForDiffException
import com.intellij.debugmap.model.BookmarkDef
import com.intellij.debugmap.model.BreakpointDef

class DebugMapFileReloadListener(private val project: Project) : FileDocumentManagerListener {

  private val service get() = DebugMapService.getInstance(project)

  private data class PendingReload(
    val oldContent: String,
    val breakpoints: List<Pair<BreakpointDef, Int>>, // def (carries topicId), currentLine
    val bookmarks: List<Pair<BookmarkDef, Int>>,      // def (carries topicId), currentLine
  )

  private val pendingReloads = mutableMapOf<String, PendingReload>()

  override fun beforeFileContentReload(file: VirtualFile, document: Document) {
    val fileUrl = file.url
    val breakpoints = service.getBreakpointsByFile(fileUrl)
      .map { def -> def to service.getCurrentLine(def.topicId, def) }
    val bookmarks = service.getBookmarksByFile(fileUrl)
      .map { def -> def to def.line }
    if (breakpoints.isEmpty() && bookmarks.isEmpty()) return
    pendingReloads[fileUrl] = PendingReload(document.text, breakpoints, bookmarks)
    // Drop markers now so Document.setText() won't trigger syncToService with invalid markers.
    service.dropFileEntries(fileUrl)
    // Remove active-topic IDE entries before content changes while line numbers are still known.
    // Suppress the callbacks that fire after removal to prevent corrupting the in-memory store.
    val activeTopicId = service.getActiveTopicId()
    val activeBreakpoints = breakpoints
      .filter { (def, _) -> def.topicId == activeTopicId }
      .map { (def, currentLine) -> def.copy(line = currentLine) }
    if (activeBreakpoints.isNotEmpty()) {
      service.suppressBreakpointRemovals(activeBreakpoints)
      service.ideManager.removeBreakpointDefs(activeBreakpoints)
    }
    val activeBookmarks = bookmarks
      .filter { (def, _) -> def.topicId == activeTopicId }
      .map { (def, _) -> def }
    if (activeBookmarks.isNotEmpty()) {
      service.suppressBookmarkRemovals(activeBookmarks)
      service.ideManager.removeBookmarkDefs(activeBookmarks)
    }
  }

  override fun fileContentReloaded(file: VirtualFile, document: Document) {
    val fileUrl = file.url
    val pending = pendingReloads.remove(fileUrl) ?: return
    val newContent = document.text
    val lastLine = (document.lineCount - 1).coerceAtLeast(0)
    val activeTopicId = service.getActiveTopicId()

    val change = try { Diff.buildChanges(pending.oldContent, newContent) } catch (_: FilesTooBigForDiffException) { null }

    val correctedActiveBreakpoints = mutableListOf<BreakpointDef>()
    for ((def, currentLine) in pending.breakpoints) {
      val targetLine = clampedTranslateLine(change, currentLine, lastLine)
      if (targetLine != def.line) service.moveBreakpointLine(def, targetLine)
      if (def.topicId == activeTopicId) correctedActiveBreakpoints.add(def.copy(line = targetLine))
    }
    if (correctedActiveBreakpoints.isNotEmpty()) {
      service.ideManager.addBreakpointDefs(correctedActiveBreakpoints)
    }

    val correctedActiveBookmarks = mutableListOf<BookmarkDef>()
    for ((def, currentLine) in pending.bookmarks) {
      val targetLine = clampedTranslateLine(change, currentLine, lastLine)
      if (targetLine != def.line) service.moveBookmarkLine(def, targetLine)
      if (def.topicId == activeTopicId) correctedActiveBookmarks.add(def.copy(line = targetLine))
    }
    if (correctedActiveBookmarks.isNotEmpty()) {
      val topicName = service.getTopics().find { it.id == activeTopicId }?.name
      service.ideManager.addBookmarkDefs(correctedActiveBookmarks, topicName)
    }

    service.onFileOpened(file)
  }

  private fun clampedTranslateLine(change: Change?, line: Int, lastLine: Int): Int {
    val result = Diff.translateLine(change, line)
    return (if (result >= 0) result else line).coerceAtMost(lastLine)
  }
}
