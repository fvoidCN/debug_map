package com.intellij.debugmap.ui.tree

import com.intellij.debugmap.DebugMapBundle
import com.intellij.debugmap.DebugMapService
import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.keymap.KeymapUtil
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.jetbrains.jewel.ui.component.MenuScope
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.separator
import org.jetbrains.jewel.ui.icons.AllIconsKeys
import java.awt.datatransfer.StringSelection

internal fun shortcutHint(actionId: String): Set<String>? {
  val shortcuts = KeymapUtil.getActiveKeymapShortcuts(actionId).shortcuts
  val keystroke = shortcuts.filterIsInstance<KeyboardShortcut>().firstOrNull()?.firstKeyStroke
    ?: return null
  return setOf(KeymapUtil.getKeystrokeText(keystroke))
}

internal fun copyToClipboard(text: String) {
  CopyPasteManager.getInstance().setContents(StringSelection(text))
}

internal fun buildCopyText(type: String, reference: String, name: String?): String =
  buildJsonObject {
    put("type", JsonPrimitive(type))
    put("ref", JsonPrimitive(reference))
    if (!name.isNullOrBlank()) put("name", JsonPrimitive(name))
  }.toString()

internal fun MenuScope.copyReferenceItem(
  referenceText: String,
  keybinding: Set<String>?,
  onDismiss: () -> Unit,
) {
  selectableItem(
    selected = false,
    iconKey = AllIconsKeys.Actions.Copy,
    keybinding = keybinding,
    onClick = {
      onDismiss()
      copyToClipboard(referenceText)
    },
  ) { Text(DebugMapBundle.message("action.copy.reference")) }
  separator()
}

internal fun MenuScope.checkoutItem(
  groupId: Int,
  service: DebugMapService,
  onDismiss: () -> Unit,
) {
  selectableItem(
    selected = false,
    iconKey = AllIconsKeys.Actions.CheckOut,
    onClick = {
      onDismiss()
      WriteAction.run<Exception> { service.checkout(groupId) }
    },
  ) { Text(DebugMapBundle.message("action.checkout.group")) }
  separator()
}
