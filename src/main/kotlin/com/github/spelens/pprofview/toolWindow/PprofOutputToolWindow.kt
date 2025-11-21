package com.github.spelens.pprofview.toolWindow

import com.github.spelens.pprofview.PprofViewBundle
import com.github.spelens.pprofview.parser.PprofTextParser
import com.github.spelens.pprofview.services.PprofCodeNavigationService
import com.github.spelens.pprofview.ui.PprofChartPanel
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * pprof output tool window
 */
class PprofOutputToolWindow : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val outputPanel = PprofOutputPanel(project)
        val content = ContentFactory.getInstance().createContent(outputPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
    
    override fun shouldBeAvailable(project: Project): Boolean = true
}

/**
 * pprof output panel
 */
class PprofOutputPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val logger = thisLogger()
    private val tabbedPane = JBTabbedPane()
    private val outputs = mutableMapOf<String, JTextArea>()
    // Record report type for each tab, used for override logic
    private val tabReportTypes = mutableMapOf<String, Int>() // reportType -> tabIndex
    // Record currently processing pprof file
    private var currentPprofFile: com.intellij.openapi.vfs.VirtualFile? = null
    // Record tab title and content for refresh
    private val tabContents = mutableMapOf<String, Pair<String, com.intellij.openapi.vfs.VirtualFile?>>() // title -> (content, pprofFile)
    
    init {
        // Create toolbar
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * Build refresh button tooltip
     */
    private fun buildRefreshTooltip(): String {
        val tabCount = tabbedPane.tabCount
        
        return if (tabCount > 0) {
            // Count tabs with associated files
            var withFileCount = 0
            var totalFileSize = 0L
            val fileNames = mutableSetOf<String>()
            
            for (i in 0 until tabCount) {
                val title = tabbedPane.getTitleAt(i)
                val tabData = tabContents[title]
                if (tabData != null) {
                    val (_, pprofFile) = tabData
                    if (pprofFile != null && pprofFile.exists()) {
                        withFileCount++
                        totalFileSize += java.io.File(pprofFile.path).length()
                        fileNames.add(pprofFile.name)
                    }
                }
            }
            
            buildString {
                append("<html>")
                append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.title"))
                append("<br><hr>")
                append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.tabCount", tabCount))
                append("<br>")
                append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.withFile", withFileCount))
                append("<br>")
                
                if (withFileCount > 0) {
                    // Show total file size
                    val totalSizeStr = when {
                        totalFileSize > 1024 * 1024 -> String.format("%.2f MB", totalFileSize / (1024.0 * 1024.0))
                        totalFileSize > 1024 -> String.format("%.2f KB", totalFileSize / 1024.0)
                        else -> "$totalFileSize bytes"
                    }
                    append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.totalSize", totalSizeStr))
                    append("<br>")
                    
                    // Show associated file list
                    if (fileNames.isNotEmpty()) {
                        append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.files"))
                        append("<br>")
                        fileNames.take(3).forEach { fileName ->
                            append("  • $fileName<br>")
                        }
                        if (fileNames.size > 3) {
                            append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.moreFiles", fileNames.size - 3))
                            append("<br>")
                        }
                    }
                    
                    append("<hr>")
                    append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.clickToRefresh"))
                } else {
                    append("<hr>")
                    append(PprofViewBundle.message("pprof.toolWindow.refreshTooltip.clickToReparse"))
                }
                
                append("</html>")
            }
        } else {
            "<html>${PprofViewBundle.message("pprof.toolWindow.refreshTooltip.title")}<br><hr>${PprofViewBundle.message("pprof.toolWindow.refreshTooltip.noTabs")}</html>"
        }
    }
    
    /**
     * Build clear button tooltip
     */
    private fun buildClearTooltip(): String {
        val tabCount = tabbedPane.tabCount
        
        return buildString {
            append("<html>")
            append(PprofViewBundle.message("pprof.toolWindow.clearTooltip.title"))
            append("<br><hr>")
            append(PprofViewBundle.message("pprof.toolWindow.clearTooltip.count", tabCount))
            append("<br>")
            
            if (tabCount > 0) {
                append(PprofViewBundle.message("pprof.toolWindow.clearTooltip.list"))
                append("<br>")
                for (i in 0 until minOf(tabCount, 5)) {
                    val title = tabbedPane.getTitleAt(i)
                    append("  • $title<br>")
                }
                if (tabCount > 5) {
                    append(PprofViewBundle.message("pprof.toolWindow.clearTooltip.moreTabs", tabCount - 5))
                    append("<br>")
                }
                append("<hr>")
                append(PprofViewBundle.message("pprof.toolWindow.clearTooltip.clickToClear", tabCount))
            } else {
                append(PprofViewBundle.message("pprof.toolWindow.clearTooltip.status"))
                append("<br><hr>")
                append(PprofViewBundle.message("pprof.toolWindow.clearTooltip.noTabsToClear"))
            }
            
            append("</html>")
        }
    }
    
    /**
     * Create toolbar
     */
    private fun createToolbar(): JComponent {
        val toolbar = com.intellij.ui.components.JBPanel<com.intellij.ui.components.JBPanel<*>>()
        toolbar.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 2)
        
        // Refresh button
        val refreshButton = com.intellij.ui.components.JBLabel(
            com.intellij.icons.AllIcons.Actions.Refresh
        )
        refreshButton.toolTipText = buildRefreshTooltip()
        refreshButton.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        refreshButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                refreshCurrentTab()
            }
            
            override fun mouseEntered(e: java.awt.event.MouseEvent) {
                // Dynamically update tooltip to show current tab info
                refreshButton.toolTipText = buildRefreshTooltip()
            }
        })
        
        // Clear all button
        val clearButton = com.intellij.ui.components.JBLabel(
            com.intellij.icons.AllIcons.Actions.GC
        )
        clearButton.toolTipText = buildClearTooltip()
        clearButton.cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
        clearButton.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                clearAll()
            }
            
            override fun mouseEntered(e: java.awt.event.MouseEvent) {
                // Dynamically update tooltip to show tab count
                clearButton.toolTipText = buildClearTooltip()
            }
        })
        
        toolbar.add(refreshButton)
        toolbar.add(javax.swing.Box.createHorizontalStrut(5))
        toolbar.add(clearButton)
        
        return toolbar
    }
    
    /**
     * Refresh all tabs
     */
    private fun refreshCurrentTab() {
        val tabCount = tabbedPane.tabCount
        
        // Clear code highlights when refreshing tabs
        val navigationService = PprofCodeNavigationService.getInstance(project)
        navigationService.clearHighlights()
        logger.info(PprofViewBundle.message("pprof.toolWindow.highlightsCleared"))
        
        // Collect all tab information to refresh
        val tabsToRefresh = mutableListOf<Triple<String, String, com.intellij.openapi.vfs.VirtualFile?>>()
        
        for (i in 0 until tabCount) {
            val title = tabbedPane.getTitleAt(i)
            val tabData = tabContents[title]
            
            if (tabData != null) {
                val (content, pprofFile) = tabData
                tabsToRefresh.add(Triple(title, content, pprofFile))
            }
        }
        
        if (tabsToRefresh.isEmpty()) {
            logger.warn(PprofViewBundle.message("pprof.toolWindow.noTabsToRefresh"))
            showNotification(
                PprofViewBundle.message("pprof.toolWindow.refreshFailed"), 
                PprofViewBundle.message("pprof.toolWindow.noDataToRefresh"), 
                com.intellij.notification.NotificationType.WARNING
            )
            return
        }
        
        // Record currently selected tab index
        val selectedIndex = tabbedPane.selectedIndex
        
        // Refresh all tabs
        var refreshedCount = 0
        var failedCount = 0
        
        tabsToRefresh.forEach { (title, content, pprofFile) ->
            try {
                logger.info(PprofViewBundle.message("pprof.toolWindow.refreshingTab", title))
                
                // If has pprof file, re-read file content
                if (pprofFile != null && pprofFile.exists()) {
                    logger.info(PprofViewBundle.message("pprof.toolWindow.rereadingFile", pprofFile.path))
                    
                    // Refresh virtual file system
                    pprofFile.refresh(false, false)
                    
                    // Re-execute pprof command to get latest data (async)
                    refreshPprofDataAsync(title, pprofFile) { success ->
                        if (success) {
                            refreshedCount++
                        } else {
                            failedCount++
                        }
                        
                        // Check if all tabs have been refreshed
                        if (refreshedCount + failedCount == tabsToRefresh.size) {
                            showRefreshCompleteNotification(refreshedCount, failedCount)
                        }
                    }
                } else {
                    // No pprof file, just refresh existing content
                    removeExistingTab(title)
                    addOutputWithVisualization(title, content, pprofFile)
                    refreshedCount++
                }
            } catch (e: Exception) {
                logger.error(PprofViewBundle.message("pprof.toolWindow.refreshTabFailed", title), e)
                failedCount++
            }
        }
        
        // If no async refresh tasks, show completion notification directly
        if (tabsToRefresh.none { it.third != null && it.third!!.exists() }) {
            showRefreshCompleteNotification(refreshedCount, failedCount)
        }
        
        // Restore selected tab
        if (selectedIndex >= 0 && selectedIndex < tabbedPane.tabCount) {
            tabbedPane.selectedIndex = selectedIndex
        }
    }
    
    /**
     * Show refresh complete notification
     */
    private fun showRefreshCompleteNotification(successCount: Int, failedCount: Int) {
        val message = buildString {
            append(PprofViewBundle.message("pprof.toolWindow.refreshSuccess", successCount))
            if (failedCount > 0) {
                append(PprofViewBundle.message("pprof.toolWindow.refreshWithFailures", failedCount))
            }
        }
        
        val type = if (failedCount > 0) {
            com.intellij.notification.NotificationType.WARNING
        } else {
            com.intellij.notification.NotificationType.INFORMATION
        }
        
        showNotification(PprofViewBundle.message("pprof.toolWindow.refreshComplete"), message, type)
        logger.info("Refresh complete: success $successCount, failed $failedCount")
    }
    
    /**
     * Asynchronously re-execute pprof command to get latest data
     */
    private fun refreshPprofDataAsync(
        title: String,
        pprofFile: com.intellij.openapi.vfs.VirtualFile,
        callback: (Boolean) -> Unit
    ) {
        // Determine report type based on title
        val listMsg = PprofViewBundle.message("pprof.toolWindow.list")
        val peekMsg = PprofViewBundle.message("pprof.toolWindow.peek")
        val args = when {
            title.contains("Top") -> listOf("-top")
            title.contains(listMsg) -> listOf("-list=.")
            title.contains(peekMsg) -> listOf("-peek=.")
            else -> listOf("-text")
        }
        
        val commandLine = com.intellij.execution.configurations.GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("tool")
        commandLine.addParameter("pprof")
        commandLine.addParameters(args)
        commandLine.addParameter(pprofFile.path)
        
        logger.info(PprofViewBundle.message("pprof.toolWindow.executingRefreshCommand", commandLine.commandLineString))
        
        try {
            val processHandler = com.intellij.execution.process.ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)
            
            val output = StringBuilder()
            
            processHandler.addProcessListener(object : com.intellij.execution.process.ProcessListener {
                override fun onTextAvailable(event: com.intellij.execution.process.ProcessEvent, outputType: com.intellij.openapi.util.Key<*>) {
                    output.append(event.text)
                }
                
                override fun processTerminated(event: com.intellij.execution.process.ProcessEvent) {
                    if (event.exitCode == 0) {
                        // Update tab content
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            try {
                                removeExistingTab(title)
                                addOutputWithVisualization(title, output.toString(), pprofFile)
                                callback(true)
                                logger.info(PprofViewBundle.message("pprof.toolWindow.tabRefreshSuccess", title))
                            } catch (e: Exception) {
                                logger.error(PprofViewBundle.message("pprof.toolWindow.updateTabFailed", title), e)
                                callback(false)
                            }
                        }
                    } else {
                        logger.error(PprofViewBundle.message("pprof.toolWindow.refreshCommandFailed", event.exitCode))
                        callback(false)
                    }
                }
            })
            
            processHandler.startNotify()
        } catch (e: Exception) {
            logger.error(PprofViewBundle.message("pprof.toolWindow.refreshCommandError"), e)
            callback(false)
        }
    }
    
    /**
     * Show notification
     */
    private fun showNotification(title: String, content: String, type: com.intellij.notification.NotificationType) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("pprofview.notifications")
            .createNotification(title, content, type)
            .notify(project)
    }
    
    /**
     * Add output tab (text)
     */
    fun addOutput(title: String, content: String) {
        // Check if tab of same type already exists, override if so
        removeExistingTab(title)
        
        val textArea = JTextArea(content)
        textArea.isEditable = false
        textArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
        
        val scrollPane = JBScrollPane(textArea)
        val tabIndex = tabbedPane.tabCount
        tabbedPane.addTab(title, scrollPane)
        tabbedPane.selectedIndex = tabIndex
        
        outputs[title] = textArea
        tabReportTypes[title] = tabIndex
    }
    
    /**
     * Add output tab (with visualization)
     */
    fun addOutputWithVisualization(title: String, content: String, pprofFile: com.intellij.openapi.vfs.VirtualFile? = null) {
        try {
            // Record current pprof file
            if (pprofFile != null) {
                currentPprofFile = pprofFile
            }
            
            // Save tab content for refresh
            tabContents[title] = Pair(content, pprofFile)
            
            // Check if tab of same type already exists, override if so
            removeExistingTab(title)
            
            // Parse text report
            val parser = PprofTextParser()
            val report = parser.parse(content)
            
            if (report.entries.isNotEmpty()) {
                // Show chart directly, pass project and pprofFile to support code navigation
                val chartPanel = PprofChartPanel(report, project, currentPprofFile)
                
                val tabIndex = tabbedPane.tabCount
                tabbedPane.addTab(title, chartPanel)
                tabbedPane.selectedIndex = tabIndex
                
                tabReportTypes[title] = tabIndex
                
                logger.info(PprofViewBundle.message("pprof.toolWindow.addedVisualizationTab", title))
            } else {
                // If parsing fails, show text only
                addOutput(title, content)
            }
        } catch (e: Exception) {
            logger.error(PprofViewBundle.message("pprof.toolWindow.visualizationFailed"), e)
            // Fallback to plain text display
            addOutput(title, content)
        }
    }
    
    /**
     * Remove existing tab of same type
     */
    private fun removeExistingTab(reportType: String) {
        val existingIndex = tabReportTypes[reportType]
        if (existingIndex != null && existingIndex < tabbedPane.tabCount) {
            // Check if tab title matches (index may have changed)
            if (tabbedPane.getTitleAt(existingIndex) == reportType) {
                logger.info(PprofViewBundle.message("pprof.toolWindow.removingExistingTab", reportType, existingIndex))
                tabbedPane.removeTabAt(existingIndex)
                outputs.remove(reportType)
                
                // Update other tab indices
                tabReportTypes.entries.forEach { entry ->
                    if (entry.value > existingIndex) {
                        tabReportTypes[entry.key] = entry.value - 1
                    }
                }
            }
        }
        
        // If index doesn't match, try to find by title
        for (i in 0 until tabbedPane.tabCount) {
            if (tabbedPane.getTitleAt(i) == reportType) {
                logger.info(PprofViewBundle.message("pprof.toolWindow.removingByTitle", reportType, i))
                tabbedPane.removeTabAt(i)
                outputs.remove(reportType)
                
                // Update indices
                tabReportTypes.entries.forEach { entry ->
                    if (entry.value > i) {
                        tabReportTypes[entry.key] = entry.value - 1
                    }
                }
                break
            }
        }
        
        // Clean up mapping
        tabReportTypes.remove(reportType)
    }
    
    /**
     * Add custom component tab
     */
    fun addComponent(title: String, component: JComponent) {
        tabbedPane.addTab(title, component)
        tabbedPane.selectedIndex = tabbedPane.tabCount - 1
    }
    
    /**
     * Clear all output
     */
    fun clearAll() {
        tabbedPane.removeAll()
        outputs.clear()
        tabReportTypes.clear()
        tabContents.clear()
        currentPprofFile = null
        
        // Clear code highlights when clearing all tabs
        val navigationService = PprofCodeNavigationService.getInstance(project)
        navigationService.clearHighlights()
    }
    
    companion object {
        /**
         * Get tool window instance
         */
        fun getInstance(project: Project): PprofOutputPanel? {
            val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                .getToolWindow("pprof Output") ?: return null
            
            val content = toolWindow.contentManager.getContent(0) ?: return null
            return content.component as? PprofOutputPanel
        }
    }
}
