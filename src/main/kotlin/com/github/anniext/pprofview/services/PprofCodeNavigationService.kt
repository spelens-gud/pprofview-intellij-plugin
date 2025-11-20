package com.github.anniext.pprofview.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * pprof 代码导航服务
 * 负责从 pprof 数据跳转到源代码位置
 */
@Service(Service.Level.PROJECT)
class PprofCodeNavigationService(private val project: Project) {
    private val logger = thisLogger()
    
    /**
     * 导航到函数定义
     * 
     * @param pprofFile pprof 文件
     * @param functionName 函数名
     */
    fun navigateToFunction(pprofFile: VirtualFile, functionName: String) {
        logger.info("导航到函数: $functionName")
        
        // 使用 pprof -list 命令获取函数的源代码信息
        val listOutput = executeListCommand(pprofFile, functionName)
        if (listOutput.isEmpty()) {
            logger.warn("无法获取函数 $functionName 的源代码信息")
            return
        }
        
        // 解析 list 输出，提取文件路径和行号
        val codeLocation = parseListOutput(listOutput)
        if (codeLocation == null) {
            logger.warn("无法解析函数 $functionName 的代码位置")
            return
        }
        
        // 在编辑器中打开文件并高亮
        openAndHighlightCode(codeLocation)
    }
    
    /**
     * 执行 pprof -list 命令
     */
    private fun executeListCommand(pprofFile: VirtualFile, functionName: String): String {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameters("tool", "pprof", "-list=$functionName", pprofFile.path)
        
        return try {
            val process = commandLine.createProcess()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            process.waitFor()
            output
        } catch (e: Exception) {
            logger.error("执行 pprof -list 命令失败", e)
            ""
        }
    }
    
    /**
     * 解析 list 输出
     * 
     * 输出格式示例:
     * ```
     * Total: 10.50s
     * ROUTINE ======================== main.fibonacci in /path/to/file.go
     *       10ms      10ms (flat, cum)  0.10% of Total
     *          .          .     10:func fibonacci(n int) int {
     *          .          .     11:    if n <= 1 {
     *       10ms       10ms     12:        return n
     *          .          .     13:    }
     *          .          .     14:    return fibonacci(n-1) + fibonacci(n-2)
     *          .          .     15:}
     * ```
     */
    private fun parseListOutput(output: String): CodeLocation? {
        val lines = output.lines()
        
        var filePath: String? = null
        val hotLines = mutableListOf<HotLine>()
        
        for (line in lines) {
            // 解析 ROUTINE 行，提取文件路径
            if (line.contains("ROUTINE") && line.contains(" in ")) {
                val parts = line.split(" in ")
                if (parts.size >= 2) {
                    filePath = parts[1].trim()
                }
                continue
            }
            
            // 解析代码行
            // 格式: "       10ms       10ms     12:        return n"
            val codeLinePattern = """^\s*(\S+)\s+(\S+)\s+(\d+):(.*)$""".toRegex()
            val match = codeLinePattern.find(line)
            if (match != null) {
                val (flat, cum, lineNum, code) = match.destructured
                
                // 只记录有性能数据的行（flat 或 cum 不为 "."）
                val hasData = flat != "." || cum != "."
                
                hotLines.add(HotLine(
                    lineNumber = lineNum.toInt(),
                    code = code,
                    flat = flat,
                    cum = cum,
                    isHot = hasData
                ))
            }
        }
        
        if (filePath == null || hotLines.isEmpty()) {
            return null
        }
        
        // 找到第一个热点行作为跳转目标
        val targetLine = hotLines.firstOrNull { it.isHot }?.lineNumber 
            ?: hotLines.firstOrNull()?.lineNumber 
            ?: 1
        
        return CodeLocation(
            filePath = filePath,
            targetLine = targetLine,
            hotLines = hotLines
        )
    }
    
    /**
     * 在编辑器中打开文件并高亮代码
     */
    private fun openAndHighlightCode(location: CodeLocation) {
        ApplicationManager.getApplication().invokeLater {
            // 查找文件
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(location.filePath)
            if (virtualFile == null) {
                logger.warn("文件不存在: ${location.filePath}")
                return@invokeLater
            }
            
            // 打开文件
            val descriptor = OpenFileDescriptor(
                project,
                virtualFile,
                location.targetLine - 1, // 行号从 0 开始
                0
            )
            
            val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            if (editor == null) {
                logger.warn("无法打开编辑器")
                return@invokeLater
            }
            
            // 滚动到目标行
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            
            // 高亮热点代码行
            val markupModel = editor.markupModel
            
            // 清除之前的高亮
            markupModel.removeAllHighlighters()
            
            // 为每个热点行添加高亮
            for (hotLine in location.hotLines) {
                if (!hotLine.isHot) continue
                
                val lineNumber = hotLine.lineNumber - 1 // 行号从 0 开始
                if (lineNumber < 0 || lineNumber >= editor.document.lineCount) continue
                
                val startOffset = editor.document.getLineStartOffset(lineNumber)
                val endOffset = editor.document.getLineEndOffset(lineNumber)
                
                // 根据性能数据强度选择颜色
                val color = getHotLineColor(hotLine)
                
                val textAttributes = TextAttributes().apply {
                    backgroundColor = color
                    effectType = EffectType.BOXED
                    effectColor = color.darker()
                    fontType = Font.BOLD
                }
                
                markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    HighlighterLayer.SELECTION - 1,
                    textAttributes,
                    HighlighterTargetArea.LINES_IN_RANGE
                )
            }
            
            logger.info("已打开文件并高亮: ${location.filePath}:${location.targetLine}")
        }
    }
    
    /**
     * 根据热点数据获取颜色
     */
    private fun getHotLineColor(hotLine: HotLine): Color {
        // 简单的颜色映射：有数据的行使用黄色高亮
        return JBColor(
            Color(255, 255, 200), // 浅黄色 (亮色主题)
            Color(80, 80, 40)     // 深黄色 (暗色主题)
        )
    }
    
    companion object {
        fun getInstance(project: Project): PprofCodeNavigationService {
            return project.getService(PprofCodeNavigationService::class.java)
        }
    }
}

/**
 * 代码位置信息
 */
data class CodeLocation(
    val filePath: String,
    val targetLine: Int,
    val hotLines: List<HotLine>
)

/**
 * 热点代码行
 */
data class HotLine(
    val lineNumber: Int,
    val code: String,
    val flat: String,
    val cum: String,
    val isHot: Boolean
)
