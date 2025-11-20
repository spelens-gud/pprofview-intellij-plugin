package com.github.anniext.pprofview.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * 插件启动活动
 * 用于验证插件是否正确加载
 */
class PluginStartupActivity : ProjectActivity {
    private val logger = thisLogger()
    
    override suspend fun execute(project: Project) {
        logger.warn("=== pprofview plugin 0.1.5 started for project: ${project.name} ===")
        logger.warn("=== Plugin is loaded and running ===")
    }
}
