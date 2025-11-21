package com.github.spelens.pprofview

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Pprof Plus Plugin icon
 */
object PprofIcons {
    /**
     * Main plugin icon（64x64）
     */
    @JvmField
    val PluginIcon: Icon = IconLoader.getIcon("/META-INF/pluginIcon.svg", PprofIcons::class.java)
    
    /**
     * Tool window icon（13x13）
     */
    @JvmField
    val ToolWindow: Icon = IconLoader.getIcon("/icons/toolWindow.svg", PprofIcons::class.java)
}
