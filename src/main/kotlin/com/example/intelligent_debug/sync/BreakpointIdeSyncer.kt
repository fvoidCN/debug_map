package com.example.intelligent_debug.sync

import com.example.intelligent_debug.DebugMapService
import com.intellij.openapi.project.Project

/** Syncs IDE breakpoints on checkout. Must be called on EDT inside a write action. */
class BreakpointIdeSyncer(private val project: Project) {

  private val service get() = DebugMapService.getInstance(project)
}
