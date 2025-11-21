package com.github.spelens.pprofview

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Pprof Plus 插件图标
 */
object PprofIcons {
    /**
     * 插件主图标（64x64）
     */
    @JvmField
    val PluginIcon: Icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", PprofIcons::class.java)
    
    /**
     * 工具窗口图标（13x13）
     */
    @JvmField
    val ToolWindow: Icon = IconLoader.getIcon("/icons/toolWindow.svg", PprofIcons::class.java)
}
