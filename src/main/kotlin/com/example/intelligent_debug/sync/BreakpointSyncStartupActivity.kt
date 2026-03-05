package com.example.intelligent_debug.sync

import com.example.intelligent_debug.DebugMapService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class BreakpointSyncStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    DebugMapService.getInstance(project).importFloatingBreakpoints()
  }
}
