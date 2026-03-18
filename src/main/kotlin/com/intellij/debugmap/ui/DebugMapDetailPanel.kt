package com.intellij.debugmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intellij.debugmap.DebugMapService
import com.intellij.debugmap.model.TopicData
import com.intellij.debugmap.ui.tree.COLOR_INACTIVE
import com.intellij.debugmap.ui.tree.copyToClipboard
import org.jetbrains.jewel.ui.Orientation
import org.jetbrains.jewel.ui.component.Divider
import org.jetbrains.jewel.ui.component.IconActionButton
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun DebugMapDetailPanel(node: DebugMapNode, topics: List<TopicData>, service: DebugMapService) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
  ) {
    when (node) {
      is DebugMapNode.BookmarkItem -> BookmarkDetail(node, service)
      is DebugMapNode.BreakpointItem -> BreakpointDetail(node, topics, service)
      is DebugMapNode.Topic -> Unit
    }
  }
}

@Composable
private fun BookmarkDetail(node: DebugMapNode.BookmarkItem, service: DebugMapService) {
  val def = node.def
  val fileText = remember(def.fileUrl, def.line) { service.buildReference(def.fileUrl, def.line) }
  if (!def.name.isNullOrBlank()) DetailRow("Name", def.name, copyValue = def.name)
  DetailRow("File", fileText, copyValue = fileText)
}

@Composable
private fun BreakpointDetail(node: DebugMapNode.BreakpointItem, topics: List<TopicData>, service: DebugMapService) {
  val def = node.def
  val fileText = remember(def.fileUrl, def.line, def.column) {
    val ref = service.buildReference(def.fileUrl, def.line)
    if (def.column > 0) "$ref:${def.column}" else ref
  }

  if (!def.name.isNullOrBlank()) DetailRow("Name", def.name, copyValue = def.name)
  DetailRow("File", fileText, copyValue = fileText)
  if (!def.condition.isNullOrBlank()) DetailRow("Condition", def.condition)
  if (!def.logExpression.isNullOrBlank()) DetailRow("Log", def.logExpression)
  if (def.enabled == false) DetailRow("Enabled", "No")
  if (def.suspendPolicy != null && def.suspendPolicy != "ALL") {
    DetailRow("Suspend", def.suspendPolicy.lowercase().replaceFirstChar { it.uppercase() })
  }
  val masterDef = remember(def.masterBreakpointId, topics) {
    def.masterBreakpointId?.let { id -> topics.flatMap { it.breakpoints }.firstOrNull { it.id == id } }
  }
  if (masterDef != null) {
    val masterText = remember(masterDef.fileUrl, masterDef.line) { service.buildReference(masterDef.fileUrl, masterDef.line) }
    val leaveEnabled = if (def.masterLeaveEnabled == true) " (keep enabled)" else ""
    DetailRow("Master", "$masterText$leaveEnabled")
  }
}

@Composable
private fun DetailRow(label: String, value: String, copyValue: String? = null) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = label,
      color = COLOR_INACTIVE,
      modifier = Modifier.width(72.dp),
      maxLines = 1,
    )
    Text(text = value, modifier = Modifier.weight(1f))
    if (copyValue != null) {
      IconActionButton(
        key = AllIconsKeys.Actions.Copy,
        contentDescription = "Copy",
        modifier = Modifier.size(16.dp),
        onClick = { copyToClipboard(copyValue) },
      )
    }
  }
  Divider(orientation = Orientation.Horizontal, modifier = Modifier.fillMaxWidth())
}
