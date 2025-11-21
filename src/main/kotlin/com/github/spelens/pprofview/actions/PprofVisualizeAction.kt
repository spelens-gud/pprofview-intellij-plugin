package com.github.spelens.pprofview.actions

import com.github.spelens.pprofview.services.PprofVisualizationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFile

/**
 * pprof 文件可视化操作
 * 右键点击 .pprof 文件时显示
 */
class PprofVisualizeAction : AnAction() {
    private val logger = thisLogger()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        if (!isPprofFile(file)) {
            Messages.showErrorDialog(project, "请选择 pprof 格式文件", "错误")
            return
        }
        
        val visualizationService = project.service<PprofVisualizationService>()
        
        // 显示可视化类型选择菜单
        val visualizationTypes = listOf(
            VisualizationType.WEB to "Web 浏览器 (交互式图表)",
            VisualizationType.TEXT to "文本报告 (Top 函数)",
            VisualizationType.GRAPH to "调用图 (SVG)",
            VisualizationType.FLAMEGRAPH to "火焰图 (SVG)",
            VisualizationType.TOP to "Top 10 函数",
            VisualizationType.LIST to "完整函数列表",
            VisualizationType.PEEK to "Peek (简要信息)"
        )
        
        val popup = JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<Pair<VisualizationType, String>>(
                "选择可视化类型",
                visualizationTypes
            ) {
                override fun getTextFor(value: Pair<VisualizationType, String>): String {
                    return value.second
                }
                
                override fun onChosen(
                    selectedValue: Pair<VisualizationType, String>,
                    finalChoice: Boolean
                ): PopupStep<*>? {
                    if (finalChoice) {
                        visualizationService.visualize(file, selectedValue.first)
                    }
                    return super.onChosen(selectedValue, finalChoice)
                }
            }
        )
        
        popup.showInBestPositionFor(e.dataContext)
    }
    
    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && isPprofFile(file)
    }
    
    private fun isPprofFile(file: VirtualFile): Boolean {
        val name = file.name.lowercase()
        return name.endsWith(".pprof") || 
               name.endsWith(".pb.gz") || 
               name.contains("profile") ||
               name.contains("cpu") ||
               name.contains("heap") ||
               name.contains("goroutine") ||
               name.contains("block") ||
               name.contains("mutex") ||
               name.contains("allocs")
    }
}

/**
 * 可视化类型
 */
enum class VisualizationType(val command: String, val description: String) {
    WEB("-http=:0", "在浏览器中打开交互式可视化界面"),
    TEXT("-text", "显示文本格式的性能报告"),
    GRAPH("-svg", "生成调用图 SVG"),
    FLAMEGRAPH("-flame", "生成火焰图 SVG"),
    TOP("-top", "显示 Top 10 函数"),
    LIST("-list=.", "显示完整函数列表"),
    PEEK("-peek=.", "显示简要信息")
}
