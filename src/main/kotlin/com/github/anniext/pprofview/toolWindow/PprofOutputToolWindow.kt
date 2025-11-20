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
    
    init {
        add(tabbedPane, BorderLayout.CENTER)
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
