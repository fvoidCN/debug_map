package com.intellij.debugmap.ui.tree

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import com.intellij.debugmap.DebugMapBundle
import com.intellij.debugmap.DebugMapService
import com.intellij.debugmap.ui.DebugMapNode
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.jewel.ui.component.PopupMenu
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun BookmarkContextMenu(
  nodes: List<DebugMapNode.BookmarkItem>,
  project: Project,
  service: DebugMapService,
  offset: Offset,
  onDismiss: () -> Unit,
) {
  val isSingle = nodes.size == 1
  val node = nodes.firstOrNull() ?: return

  PopupMenu(
    onDismissRequest = { onDismiss(); true },
    popupPositionProvider = rememberPopupPositionProviderAtPosition(offset),
    adContent = null,
  ) {
    if (isSingle) {
      selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.Find,
        onClick = {
          onDismiss()
          val file = VirtualFileManager.getInstance().findFileByUrl(node.def.fileUrl)
          if (file != null) OpenFileDescriptor(project, file, node.def.line, 0).navigate(true)
        },
      ) { Text(DebugMapBundle.message("action.navigate")) }
      selectableItem(
        selected = false,
        iconKey = AllIconsKeys.Actions.Edit,
        onClick = {
          onDismiss()
          val current = node.def.name ?: ""
          val name = Messages.showInputDialog(
            project,
            DebugMapBundle.message("dialog.rename.bookmark.label"),
            DebugMapBundle.message("dialog.rename.bookmark.title"),
            null, current, null,
          ) ?: return@selectableItem
          service.renameBookmark(node.def, name)
        },
      ) { Text(DebugMapBundle.message("action.rename")) }
    }
    selectableItem(
      selected = false,
      iconKey = AllIconsKeys.General.Remove,
      onClick = {
        onDismiss()
        WriteAction.run<Exception> {
          nodes.forEach { service.removeBookmarkByToolWindow(it.def.groupId, it.def.fileUrl, it.def.line) }
        }
      },
    ) { Text(DebugMapBundle.message("action.delete")) }
  }
}
