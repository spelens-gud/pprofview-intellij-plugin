package com.github.anniext.pprofview.toolWindow

import com.github.anniext.pprofview.parser.PprofTextParser
import com.github.anniext.pprofview.ui.PprofChartPanel
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
 * pprof 输出工具窗口
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
 * pprof 输出面板
 */
class PprofOutputPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val logger = thisLogger()
    private val tabbedPane = JBTabbedPane()
    private val outputs = mutableMapOf<String, JTextArea>()
    // 记录每个标签页的报告类型，用于覆盖逻辑
    private val tabReportTypes = mutableMapOf<String, Int>() // reportType -> tabIndex
    // 记录当前正在处理的 pprof 文件
    private var currentPprofFile: com.intellij.openapi.vfs.VirtualFile? = null
    // 记录当前标签页的标题和内容，用于刷新
    private val tabContents = mutableMapOf<String, Pair<String, com.intellij.openapi.vfs.VirtualFile?>>() // title -> (content, pprofFile)
    
    init {
        // 创建工具栏
        val toolbar = createToolbar()
        add(toolbar, BorderLayout.NORTH)
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * 构建刷新按钮的工具提示
     */
    private fun buildRefreshTooltip(): String {
        val tabCount = tabbedPane.tabCount
        
        return if (tabCount > 0) {
            // 统计有关联文件的标签页数量
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
                append("<b>🔄 刷新所有标签页数据</b><br>")
                append("<hr>")
                append("<b>标签页总数：</b> $tabCount<br>")
                append("<b>有关联文件：</b> $withFileCount 个<br>")
                
                if (withFileCount > 0) {
                    // 显示总文件大小
                    val totalSizeStr = when {
                        totalFileSize > 1024 * 1024 -> String.format("%.2f MB", totalFileSize / (1024.0 * 1024.0))
                        totalFileSize > 1024 -> String.format("%.2f KB", totalFileSize / 1024.0)
                        else -> "$totalFileSize bytes"
                    }
                    append("<b>总文件大小：</b> $totalSizeStr<br>")
                    
                    // 显示关联的文件列表
                    if (fileNames.isNotEmpty()) {
                        append("<b>关联文件：</b><br>")
                        fileNames.take(3).forEach { fileName ->
                            append("  • $fileName<br>")
                        }
                        if (fileNames.size > 3) {
                            append("  • ... 还有 ${fileNames.size - 3} 个文件<br>")
                        }
                    }
                    
                    append("<hr>")
                    append("<i>点击将重新读取所有 pprof 文件并刷新可视化图表</i>")
                } else {
                    append("<hr>")
                    append("<i>点击将重新解析所有标签页的现有数据</i>")
                }
                
                append("</html>")
            }
        } else {
            "<html><b>🔄 刷新所有标签页数据</b><br><hr><i>当前没有标签页</i></html>"
        }
    }
    
    /**
     * 构建清除按钮的工具提示
     */
    private fun buildClearTooltip(): String {
        val tabCount = tabbedPane.tabCount
        
        return buildString {
            append("<html>")
            append("<b>🗑️ 清除所有标签页</b><br>")
            append("<hr>")
            append("<b>当前标签页数量：</b> $tabCount<br>")
            
            if (tabCount > 0) {
                append("<b>标签页列表：</b><br>")
                for (i in 0 until minOf(tabCount, 5)) {
                    val title = tabbedPane.getTitleAt(i)
                    append("  • $title<br>")
                }
                if (tabCount > 5) {
                    append("  • ... 还有 ${tabCount - 5} 个标签页<br>")
                }
                append("<hr>")
                append("<i>点击将清除所有 $tabCount 个标签页</i>")
            } else {
                append("<b>状态：</b> 无标签页<br>")
                append("<hr>")
                append("<i>当前没有可清除的标签页</i>")
            }
            
            append("</html>")
        }
    }
    
    /**
     * 创建工具栏
     */
    private fun createToolbar(): JComponent {
        val toolbar = com.intellij.ui.components.JBPanel<com.intellij.ui.components.JBPanel<*>>()
        toolbar.layout = java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 5, 2)
        
        // 刷新按钮
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
                // 动态更新工具提示，显示当前标签页信息
                refreshButton.toolTipText = buildRefreshTooltip()
            }
        })
        
        // 清除所有按钮
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
                // 动态更新工具提示，显示标签页数量
                clearButton.toolTipText = buildClearTooltip()
            }
        })
        
        toolbar.add(refreshButton)
        toolbar.add(javax.swing.Box.createHorizontalStrut(5))
        toolbar.add(clearButton)
        
        return toolbar
    }
    
    /**
     * 刷新所有标签页
     */
    private fun refreshCurrentTab() {
        val tabCount = tabbedPane.tabCount
        
        if (tabCount == 0) {
            logger.warn("没有标签页")
            showNotification("刷新失败", "当前没有标签页", com.intellij.notification.NotificationType.WARNING)
            return
        }
        
        logger.info("开始刷新所有标签页，共 $tabCount 个")
        
        // 收集所有需要刷新的标签页信息
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
            logger.warn("没有可刷新的标签页数据")
            showNotification("刷新失败", "没有可刷新的标签页数据", com.intellij.notification.NotificationType.WARNING)
            return
        }
        
        // 记录当前选中的标签页索引
        val selectedIndex = tabbedPane.selectedIndex
        
        // 显示开始刷新的通知
        showNotification(
            "开始刷新",
            "正在刷新 ${tabsToRefresh.size} 个标签页...",
            com.intellij.notification.NotificationType.INFORMATION
        )
        
        // 刷新所有标签页
        var refreshedCount = 0
        var failedCount = 0
        
        tabsToRefresh.forEach { (title, content, pprofFile) ->
            try {
                logger.info("刷新标签页: $title")
                
                // 如果有 pprof 文件，重新读取文件内容
                if (pprofFile != null && pprofFile.exists()) {
                    logger.info("重新读取 pprof 文件: ${pprofFile.path}")
                    
                    // 刷新虚拟文件系统
                    pprofFile.refresh(false, false)
                    
                    // 重新执行 pprof 命令获取最新数据（异步）
                    refreshPprofDataAsync(title, pprofFile) { success ->
                        if (success) {
                            refreshedCount++
                        } else {
                            failedCount++
                        }
                        
                        // 检查是否所有标签页都已刷新完成
                        if (refreshedCount + failedCount == tabsToRefresh.size) {
                            showRefreshCompleteNotification(refreshedCount, failedCount)
                        }
                    }
                } else {
                    // 没有 pprof 文件，只刷新现有内容
                    removeExistingTab(title)
                    addOutputWithVisualization(title, content, pprofFile)
                    refreshedCount++
                }
            } catch (e: Exception) {
                logger.error("刷新标签页失败: $title", e)
                failedCount++
            }
        }
        
        // 如果没有异步刷新任务，直接显示完成通知
        if (tabsToRefresh.none { it.third != null && it.third!!.exists() }) {
            showRefreshCompleteNotification(refreshedCount, failedCount)
        }
        
        // 恢复选中的标签页
        if (selectedIndex >= 0 && selectedIndex < tabbedPane.tabCount) {
            tabbedPane.selectedIndex = selectedIndex
        }
    }
    
    /**
     * 显示刷新完成通知
     */
    private fun showRefreshCompleteNotification(successCount: Int, failedCount: Int) {
        val message = buildString {
            append("成功刷新 $successCount 个标签页")
            if (failedCount > 0) {
                append("，失败 $failedCount 个")
            }
        }
        
        val type = if (failedCount > 0) {
            com.intellij.notification.NotificationType.WARNING
        } else {
            com.intellij.notification.NotificationType.INFORMATION
        }
        
        showNotification("刷新完成", message, type)
        logger.info("刷新完成: 成功 $successCount 个，失败 $failedCount 个")
    }
    
    /**
     * 异步重新执行 pprof 命令获取最新数据
     */
    private fun refreshPprofDataAsync(
        title: String,
        pprofFile: com.intellij.openapi.vfs.VirtualFile,
        callback: (Boolean) -> Unit
    ) {
        // 根据标题判断报告类型
        val args = when {
            title.contains("Top") -> listOf("-top")
            title.contains("列表") -> listOf("-list=.")
            title.contains("简要") -> listOf("-peek=.")
            else -> listOf("-text")
        }
        
        val commandLine = com.intellij.execution.configurations.GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("tool")
        commandLine.addParameter("pprof")
        commandLine.addParameters(args)
        commandLine.addParameter(pprofFile.path)
        
        logger.info("执行刷新命令: ${commandLine.commandLineString}")
        
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
                        // 更新标签页内容
                        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                            try {
                                removeExistingTab(title)
                                addOutputWithVisualization(title, output.toString(), pprofFile)
                                callback(true)
                                logger.info("标签页刷新成功: $title")
                            } catch (e: Exception) {
                                logger.error("更新标签页失败: $title", e)
                                callback(false)
                            }
                        }
                    } else {
                        logger.error("刷新命令执行失败，退出码: ${event.exitCode}")
                        callback(false)
                    }
                }
            })
            
            processHandler.startNotify()
        } catch (e: Exception) {
            logger.error("执行刷新命令失败", e)
            callback(false)
        }
    }
    
    /**
     * 显示通知
     */
    private fun showNotification(title: String, content: String, type: com.intellij.notification.NotificationType) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("pprofview.notifications")
            .createNotification(title, content, type)
            .notify(project)
    }
    
    /**
     * 添加输出标签页（文本）
     */
    fun addOutput(title: String, content: String) {
        // 检查是否已存在同类型的标签页，如果存在则覆盖
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
     * 添加输出标签页（带可视化）
     */
    fun addOutputWithVisualization(title: String, content: String, pprofFile: com.intellij.openapi.vfs.VirtualFile? = null) {
        try {
            // 记录当前的 pprof 文件
            if (pprofFile != null) {
                currentPprofFile = pprofFile
            }
            
            // 保存标签页内容，用于刷新
            tabContents[title] = Pair(content, pprofFile)
            
            // 检查是否已存在同类型的标签页，如果存在则覆盖
            removeExistingTab(title)
            
            // 解析文本报告
            val parser = PprofTextParser()
            val report = parser.parse(content)
            
            if (report.entries.isNotEmpty()) {
                // 直接显示图表，传入 project 和 pprofFile 以支持代码导航
                val chartPanel = PprofChartPanel(report, project, currentPprofFile)
                
                val tabIndex = tabbedPane.tabCount
                tabbedPane.addTab(title, chartPanel)
                tabbedPane.selectedIndex = tabIndex
                
                tabReportTypes[title] = tabIndex
                
                logger.info("已添加可视化标签页: $title")
            } else {
                // 如果解析失败，只显示文本
                addOutput(title, content)
            }
        } catch (e: Exception) {
            logger.error("创建可视化失败", e)
            // 降级到纯文本显示
            addOutput(title, content)
        }
    }
    
    /**
     * 移除已存在的同类型标签页
     */
    private fun removeExistingTab(reportType: String) {
        val existingIndex = tabReportTypes[reportType]
        if (existingIndex != null && existingIndex < tabbedPane.tabCount) {
            // 检查标签页标题是否匹配（因为索引可能已经变化）
            if (tabbedPane.getTitleAt(existingIndex) == reportType) {
                logger.info("移除已存在的标签页: $reportType (索引: $existingIndex)")
                tabbedPane.removeTabAt(existingIndex)
                outputs.remove(reportType)
                
                // 更新其他标签页的索引
                tabReportTypes.entries.forEach { entry ->
                    if (entry.value > existingIndex) {
                        tabReportTypes[entry.key] = entry.value - 1
                    }
                }
            }
        }
        
        // 如果索引不匹配，尝试通过标题查找
        for (i in 0 until tabbedPane.tabCount) {
            if (tabbedPane.getTitleAt(i) == reportType) {
                logger.info("通过标题查找并移除标签页: $reportType (索引: $i)")
                tabbedPane.removeTabAt(i)
                outputs.remove(reportType)
                
                // 更新索引
                tabReportTypes.entries.forEach { entry ->
                    if (entry.value > i) {
                        tabReportTypes[entry.key] = entry.value - 1
                    }
                }
                break
            }
        }
        
        // 清理映射
        tabReportTypes.remove(reportType)
    }
    
    /**
     * 添加自定义组件标签页
     */
    fun addComponent(title: String, component: JComponent) {
        tabbedPane.addTab(title, component)
        tabbedPane.selectedIndex = tabbedPane.tabCount - 1
    }
    
    /**
     * 清除所有输出
     */
    fun clearAll() {
        tabbedPane.removeAll()
        outputs.clear()
        tabReportTypes.clear()
        tabContents.clear()
        currentPprofFile = null
        
        logger.info("已清除所有标签页")
        showNotification("清除完成", "已清除所有标签页", com.intellij.notification.NotificationType.INFORMATION)
    }
    
    companion object {
        /**
         * 获取工具窗口实例
         */
        fun getInstance(project: Project): PprofOutputPanel? {
            val toolWindow = com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                .getToolWindow("pprof Output") ?: return null
            
            val content = toolWindow.contentManager.getContent(0) ?: return null
            return content.component as? PprofOutputPanel
        }
    }
}
