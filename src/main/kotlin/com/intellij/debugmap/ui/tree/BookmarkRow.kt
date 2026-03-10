package com.intellij.debugmap.ui.tree

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.debugmap.ui.DebugMapNode
import org.jetbrains.jewel.ui.component.Icon
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.icons.AllIconsKeys

@Composable
internal fun BookmarkRow(node: DebugMapNode.BookmarkItem) {
  val def = node.def
  val fileName = def.fileUrl.substringAfterLast('/')
  val lineNumber = def.line + 1
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 1.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Spacer(Modifier.width(18.dp))
    Icon(key = AllIconsKeys.Nodes.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
    if (!def.name.isNullOrBlank()) {
      Text(text = def.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(
        text = "$fileName:$lineNumber",
        color = COLOR_INACTIVE,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
    else {
      Text(
        text = "$fileName:$lineNumber",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }
  }
}
