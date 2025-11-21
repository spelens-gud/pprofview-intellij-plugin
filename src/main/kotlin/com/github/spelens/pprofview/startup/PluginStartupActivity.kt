package com.github.spelens.pprofview.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Plugin startup activity
 * Used to verify that the plugin is loaded correctly
 */
class PluginStartupActivity : ProjectActivity {
    private val logger = thisLogger()
    
    override suspend fun execute(project: Project) {
        logger.warn("=== pprofview plugin 0.1.5 started for project: ${project.name} ===")
        logger.warn("=== Plugin is loaded and running ===")
    }
}
