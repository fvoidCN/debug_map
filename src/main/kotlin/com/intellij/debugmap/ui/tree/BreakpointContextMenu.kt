package com.intellij.debugmap.ui.tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import com.intellij.debugmap.DebugMapBundle
import com.intellij.debugmap.DebugMapService
import com.intellij.debugmap.model.GroupData
import com.intellij.debugmap.ui.DebugMapNode
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BreakpointContextMenu(
  nodes: List<DebugMapNode.BreakpointItem>,
  project: Project,
  service: DebugMapService,
  groups: List<GroupData>,
  activeGroupId: Int?,
  offset: Offset,
  onDismiss: () -> Unit,
) {
  val isSingle = nodes.size == 1
  val node = nodes.firstOrNull() ?: return
  val moveUpKeybinding = remember { shortcutHint("PreviousOccurence") }
  val moveDownKeybinding = remember { shortcutHint("NextOccurence") }
  val renameKeybinding = remember { shortcutHint("Tree-startEditing") }
  val deleteKeybinding = remember { shortcutHint("\$Delete") }
  val copyReferenceKeybinding = remember { shortcutHint("\$Copy") }
  val breakpoints = remember(groups, node.def.groupId) { groups.find { it.id == node.def.groupId }?.breakpoints ?: emptyList() }
  val breakpointIndex = if (isSingle) breakpoints.indexOfFirst { it.fileUrl == node.def.fileUrl && it.line == node.def.line && it.column == node.def.column } else -1

  PopupMenu(
    onDismissRequest = { onDismiss(); true },
    popupPositionProvider = rememberPopupPositionProviderAtPosition(offset),
    adContent = null,
  ) {
    if (isSingle && breakpointIndex > 0) {
      selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.MoveUp,
        keybinding = moveUpKeybinding,
        onClick = {
          onDismiss()
          service.reorderBreakpoint(node.def.groupId, node.def, -1)
        },
      ) { Text(DebugMapBundle.message("action.move.up")) }
    }
    if (isSingle && breakpointIndex >= 0 && breakpointIndex < breakpoints.size - 1) {
      selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.MoveDown,
        keybinding = moveDownKeybinding,
        onClick = {
          onDismiss()
          service.reorderBreakpoint(node.def.groupId, node.def, 1)
        },
      ) { Text(DebugMapBundle.message("action.move.down")) }
    }
    if (isSingle) {
      copyReferenceItem(buildCopyText("breakpoint", service.buildReference(node.def.fileUrl, node.def.line), node.def.name), copyReferenceKeybinding, onDismiss)
    }
    if (isSingle && node.def.groupId != activeGroupId) {
      checkoutItem(node.def.groupId, service, onDismiss)
    }
    if (isSingle) {
      selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.Edit,
        keybinding = renameKeybinding,
        onClick = {
          onDismiss()
          val current = node.def.name ?: ""
          val name = Messages.showInputDialog(
            project,
            DebugMapBundle.message("dialog.rename.breakpoint.label"),
            DebugMapBundle.message("dialog.rename.breakpoint.title"),
            null, current, null,
          ) ?: return@selectableItem
          service.renameBreakpoint(node.def, name)
        },
      ) { Text(DebugMapBundle.message("action.rename.breakpoint")) }
    }
    selectableItem(
      selected = false,
      iconKey = AllIconsKeys.General.Remove,
      keybinding = deleteKeybinding,
      onClick = {
        onDismiss()
        WriteAction.run<Exception> {
          nodes.forEach { service.removeBreakpointByToolWindow(it.def.groupId, it.def.fileUrl, it.def.line, it.def.column) }
        }
      },
    ) {
      val key = if (nodes.size == 1) "action.delete.breakpoint" else "action.delete.breakpoints"
      Text(DebugMapBundle.message(key))
    }
  }
}
