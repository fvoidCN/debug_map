package com.intellij.debugmap.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import org.jetbrains.jewel.foundation.ExperimentalJewelApi
import org.jetbrains.jewel.foundation.lazy.SelectableColumnKeybindings
import org.jetbrains.jewel.foundation.lazy.SelectableLazyItemScope
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListKey
import org.jetbrains.jewel.foundation.lazy.SelectableLazyListState
import org.jetbrains.jewel.foundation.lazy.SelectionMode
import org.jetbrains.jewel.foundation.lazy.tree.BasicLazyTree
import org.jetbrains.jewel.foundation.lazy.tree.DefaultTreeViewKeyActions
import org.jetbrains.jewel.foundation.lazy.tree.DefaultTreeViewPointerEventAction
import org.jetbrains.jewel.foundation.lazy.tree.KeyActions
import org.jetbrains.jewel.foundation.lazy.tree.Tree
import org.jetbrains.jewel.foundation.lazy.tree.TreeElementState
import org.jetbrains.jewel.foundation.lazy.tree.TreeState
import org.jetbrains.jewel.foundation.lazy.tree.rememberTreeState
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.theme.LocalContentColor
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.styling.LazyTreeStyle
import org.jetbrains.jewel.ui.component.styling.contentFor
import org.jetbrains.jewel.ui.theme.treeStyle

/**
 * Implements IDEA-native right-click selection behaviour:
 * - right-click on an unselected item → select it (single selection) then notify
 * - right-click on an already-selected item → keep the existing selection then notify
 */
@OptIn(ExperimentalComposeUiApi::class)
private class RightClickPointerEventActions(
  treeState: TreeState,
  private val onRightClick: (key: Any, offset: Offset) -> Unit,
) : DefaultTreeViewPointerEventAction(treeState) {

  override fun handlePointerEventPress(
    pointerEvent: PointerEvent,
    keybindings: SelectableColumnKeybindings,
    selectableLazyListState: SelectableLazyListState,
    selectionMode: SelectionMode,
    allKeys: List<SelectableLazyListKey>,
    key: Any,
  ) {
    if (pointerEvent.button == PointerButton.Secondary) {
      if (key !in selectableLazyListState.selectedKeys) {
        selectableLazyListState.selectedKeys = setOf(key)
        selectableLazyListState.lastActiveItemIndex = allKeys.indexOfFirst { it.key == key }
      }
      val offset = pointerEvent.changes.firstOrNull()?.position ?: Offset.Zero
      onRightClick(key, offset)
      return
    }
    super.handlePointerEventPress(pointerEvent, keybindings, selectableLazyListState, selectionMode, allKeys, key)
  }
}

/**
 * A variant of [org.jetbrains.jewel.ui.component.LazyTree] that adds an [onRightClick] callback,
 * which fires with the tree-node key and cursor offset when the user right-clicks an item.
 * Right-click selection follows IDEA-native behaviour (see [RightClickPointerEventActions]).
 *
 * Everything else is identical to the upstream LazyTree.
 */
@OptIn(ExperimentalJewelApi::class)
@Composable
internal fun <T> DebugMapLazyTree(
  tree: Tree<T>,
  modifier: Modifier = Modifier,
  onElementClick: (Tree.Element<T>) -> Unit = {},
  treeState: TreeState = rememberTreeState(),
  onElementDoubleClick: (Tree.Element<T>) -> Unit = {},
  onSelectionChange: (List<Tree.Element<T>>) -> Unit = {},
  keyActions: KeyActions = DefaultTreeViewKeyActions(treeState),
  style: LazyTreeStyle = JewelTheme.treeStyle,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  onRightClick: ((key: Any, offset: Offset) -> Unit)? = null,
  nodeContent: @Composable (SelectableLazyItemScope.(Tree.Element<T>) -> Unit),
) {
  val colors = style.colors
  val metrics = style.metrics

  val pointerEventScopedActions = if (onRightClick != null) {
    remember(treeState, onRightClick) { RightClickPointerEventActions(treeState, onRightClick) }
  } else {
    remember(treeState) { DefaultTreeViewPointerEventAction(treeState) }
  }

  BasicLazyTree(
    tree = tree,
    elementBackgroundFocused = colors.backgroundActive,
    elementBackgroundSelectedFocused = colors.backgroundSelectedActive,
    elementBackgroundSelected = colors.backgroundSelected,
    indentSize = metrics.indentSize,
    elementBackgroundCornerSize = metrics.simpleListItemMetrics.selectionBackgroundCornerSize,
    elementPadding = metrics.simpleListItemMetrics.outerPadding,
    elementContentPadding = metrics.simpleListItemMetrics.innerPadding,
    elementMinHeight = metrics.elementMinHeight,
    chevronContentGap = metrics.chevronContentGap,
    onElementClick = onElementClick,
    onElementDoubleClick = onElementDoubleClick,
    onSelectionChange = onSelectionChange,
    modifier = modifier,
    treeState = treeState,
    keyActions = keyActions,
    interactionSource = interactionSource,
    pointerEventScopedActions = pointerEventScopedActions,
    chevronContent = { elementState ->
      val iconKey = style.icons.chevron(elementState.isExpanded, elementState.isSelected)
      Icon(iconKey, contentDescription = null)
    },
    nodeContent = {
      val resolvedContentColor =
        style.colors
          .contentFor(TreeElementState.of(focused = isActive, selected = isSelected, expanded = false))
          .value
          .takeOrElse { LocalContentColor.current }
      CompositionLocalProvider(LocalContentColor provides resolvedContentColor) { nodeContent(it) }
    },
  )
}
