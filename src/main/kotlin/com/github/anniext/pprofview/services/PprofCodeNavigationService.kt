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
        val startTime = System.currentTimeMillis()
        logger.info("========================================")
        logger.info("开始导航到函数: $functionName")
        logger.info("pprof 文件: ${pprofFile.path}")
        
        // 使用 pprof -list 命令获取函数的源代码信息
        val listStartTime = System.currentTimeMillis()
        val listOutput = executeListCommand(pprofFile, functionName)
        val listDuration = System.currentTimeMillis() - listStartTime
        logger.info("执行 pprof -list 命令耗时: ${listDuration}ms")
        
        if (listOutput.isEmpty()) {
            logger.warn("无法获取函数 $functionName 的源代码信息")
            logger.info("总耗时: ${System.currentTimeMillis() - startTime}ms")
            logger.info("========================================")
            
            // 显示错误通知
            showNotification(
                "代码导航失败",
                "无法获取函数 $functionName 的源代码信息\n请检查 go tool pprof 是否正常工作",
                com.intellij.notification.NotificationType.WARNING
            )
            return
        }
        
        logger.info("获取到输出长度: ${listOutput.length} 字符")
        
        // 解析 list 输出，提取文件路径和行号
        val parseStartTime = System.currentTimeMillis()
        val codeLocation = parseListOutput(listOutput)
        val parseDuration = System.currentTimeMillis() - parseStartTime
        logger.info("解析输出耗时: ${parseDuration}ms")
        
        if (codeLocation == null) {
            logger.warn("无法解析函数 $functionName 的代码位置")
            logger.warn("pprof -list 输出内容:")
            logger.warn(listOutput)
            logger.info("总耗时: ${System.currentTimeMillis() - startTime}ms")
            logger.info("========================================")
            
            // 显示错误通知
            showNotification(
                "代码导航失败",
                "无法解析函数 $functionName 的代码位置\n请查看日志获取详细信息",
                com.intellij.notification.NotificationType.WARNING
            )
            return
        }
        
        logger.info("解析结果:")
        logger.info("  - 文件路径: ${codeLocation.filePath}")
        logger.info("  - 目标行号: ${codeLocation.targetLine}")
        logger.info("  - 热点行数: ${codeLocation.hotLines.size}")
        logger.info("  - 热点行号: ${codeLocation.hotLines.filter { it.isHot }.map { it.lineNumber }}")
        
        // 在编辑器中打开文件并高亮
        val openStartTime = System.currentTimeMillis()
        openAndHighlightCode(codeLocation)
        val openDuration = System.currentTimeMillis() - openStartTime
        logger.info("打开文件并高亮耗时: ${openDuration}ms")
        
        val totalDuration = System.currentTimeMillis() - startTime
        logger.info("总耗时: ${totalDuration}ms")
        logger.info("性能分解:")
        logger.info("  - pprof -list: ${listDuration}ms (${listDuration * 100 / totalDuration}%)")
        logger.info("  - 解析输出: ${parseDuration}ms (${parseDuration * 100 / totalDuration}%)")
        logger.info("  - 打开高亮: ${openDuration}ms (${openDuration * 100 / totalDuration}%)")
        logger.info("========================================")
    }
    
    /**
     * 执行 pprof -list 命令
     */
    private fun executeListCommand(pprofFile: VirtualFile, functionName: String): String {
        // 尝试多种函数名格式
        val patterns = buildFunctionPatterns(functionName)
        
        for ((index, pattern) in patterns.withIndex()) {
            logger.info("尝试模式 ${index + 1}/${patterns.size}: $pattern")
            
            val commandLine = GeneralCommandLine()
            commandLine.exePath = "go"
            commandLine.addParameters("tool", "pprof", "-list=$pattern", pprofFile.path)
            
            val result = executePprofCommand(commandLine)
            if (result.isNotEmpty()) {
                logger.info("模式 ${index + 1} 成功，获取到输出")
                return result
            }
        }
        
        logger.warn("所有模式都失败了")
        return ""
    }
    
    /**
     * 构建函数名匹配模式
     */
    private fun buildFunctionPatterns(functionName: String): List<String> {
        val patterns = mutableListOf<String>()
        
        // 模式 1: 原始函数名
        patterns.add(functionName)
        
        // 模式 2: 提取最后一部分（方法名）
        // github.com/user/project/pkg.(*Type).Method.func1 -> Method
        val lastPart = functionName.substringAfterLast('.')
        if (lastPart != functionName && !lastPart.startsWith("func")) {
            patterns.add(lastPart)
        }
        
        // 模式 3: 提取类型和方法
        // github.com/user/project/pkg.(*Type).Method.func1 -> (*Type).Method
        if (functionName.contains("(*") && functionName.contains(").")) {
            val typeAndMethod = functionName.substringAfter("(*").substringBefore(".func")
            if (typeAndMethod.isNotEmpty()) {
                patterns.add("(*$typeAndMethod")
            }
        }
        
        // 模式 4: 使用正则表达式匹配（转义特殊字符）
        // 将 . 替换为 \., 将 * 替换为 \*, 将 ( 替换为 \(, 将 ) 替换为 \)
        val escapedName = functionName
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("*", "\\*")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
        patterns.add(escapedName)
        
        // 模式 5: 简化的正则表达式（只匹配最后几个部分）
        val parts = functionName.split('/')
        if (parts.size > 1) {
            val simplifiedPattern = parts.takeLast(2).joinToString("/")
            patterns.add(simplifiedPattern)
        }
        
        // 模式 6: 只用包名和函数名
        // github.com/user/project/pkg.Function -> pkg.Function
        if (functionName.contains('/')) {
            val pkgAndFunc = functionName.substringAfterLast('/')
            if (pkgAndFunc != functionName) {
                patterns.add(pkgAndFunc)
            }
        }
        
        return patterns.distinct()
    }
    
    /**
     * 执行 pprof 命令
     */
    private fun executePprofCommand(commandLine: GeneralCommandLine): String {
        logger.info("执行命令: ${commandLine.commandLineString}")
        
        try {
            val processStartTime = System.currentTimeMillis()
            val process = commandLine.createProcess()
            val processCreateDuration = System.currentTimeMillis() - processStartTime
            logger.info("  - 创建进程耗时: ${processCreateDuration}ms")
            
            val readStartTime = System.currentTimeMillis()
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = stdoutReader.readText()
            val errorOutput = stderrReader.readText()
            
            val readDuration = System.currentTimeMillis() - readStartTime
            logger.info("  - 读取输出耗时: ${readDuration}ms")
            
            val waitStartTime = System.currentTimeMillis()
            val exitCode = process.waitFor()
            val waitDuration = System.currentTimeMillis() - waitStartTime
            logger.info("  - 等待进程结束耗时: ${waitDuration}ms")
            logger.info("  - 进程退出码: $exitCode")
            
            if (exitCode != 0) {
                logger.warn("pprof 命令执行失败，退出码: $exitCode")
                if (errorOutput.isNotEmpty()) {
                    logger.warn("错误输出: $errorOutput")
                }
                if (output.isNotEmpty()) {
                    logger.info("标准输出: $output")
                }
                return ""
            } else {
                logger.info("  - 输出长度: ${output.length} 字符")
                if (errorOutput.isNotEmpty()) {
                    logger.warn("警告输出: $errorOutput")
                }
            }
            
            return output
        } catch (e: Exception) {
            logger.warn("执行 pprof 命令异常: ${e.message}")
            return ""
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
        logger.info("开始解析 list 输出")
        val lines = output.lines()
        logger.info("  - 总行数: ${lines.size}")
        
        var filePath: String? = null
        val hotLines = mutableListOf<HotLine>()
        var routineCount = 0
        var codeLineCount = 0
        
        for (line in lines) {
            // 解析 ROUTINE 行，提取文件路径
            if (line.contains("ROUTINE") && line.contains(" in ")) {
                routineCount++
                val parts = line.split(" in ")
                if (parts.size >= 2) {
                    filePath = parts[1].trim()
                    logger.info("  - 找到 ROUTINE: $filePath")
                }
                continue
            }
            
            // 解析代码行
            // 格式: "       10ms       10ms     12:        return n"
            val codeLinePattern = """^\s*(\S+)\s+(\S+)\s+(\d+):(.*)$""".toRegex()
            val match = codeLinePattern.find(line)
            if (match != null) {
                codeLineCount++
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
        
        logger.info("  - 找到 ROUTINE 数量: $routineCount")
        logger.info("  - 解析代码行数: $codeLineCount")
        logger.info("  - 热点行数: ${hotLines.count { it.isHot }}")
        
        if (filePath == null || hotLines.isEmpty()) {
            logger.warn("  - 解析失败: filePath=$filePath, hotLines.size=${hotLines.size}")
            return null
        }
        
        // 找到第一个热点行作为跳转目标
        val targetLine = hotLines.firstOrNull { it.isHot }?.lineNumber 
            ?: hotLines.firstOrNull()?.lineNumber 
            ?: 1
        
        logger.info("  - 选择目标行: $targetLine")
        
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
            val uiStartTime = System.currentTimeMillis()
            logger.info("开始在 UI 线程中打开文件")
            
            // 查找文件（支持多种路径格式）
            val findFileStartTime = System.currentTimeMillis()
            val virtualFile = findSourceFile(location.filePath)
            val findFileDuration = System.currentTimeMillis() - findFileStartTime
            logger.info("  - 查找文件耗时: ${findFileDuration}ms")
            
            if (virtualFile == null) {
                logger.warn("文件不存在: ${location.filePath}")
                logger.warn("已尝试多种路径查找策略，均未找到文件")
                
                // 显示错误通知
                showNotification(
                    "文件未找到",
                    "无法找到源文件: ${location.filePath}\n" +
                    "请确保源代码在项目中，或检查路径是否正确",
                    com.intellij.notification.NotificationType.WARNING
                )
                return@invokeLater
            }
            
            logger.info("  - 文件大小: ${virtualFile.length} 字节")
            
            // 打开文件
            val openEditorStartTime = System.currentTimeMillis()
            val descriptor = OpenFileDescriptor(
                project,
                virtualFile,
                location.targetLine - 1, // 行号从 0 开始
                0
            )
            
            val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            val openEditorDuration = System.currentTimeMillis() - openEditorStartTime
            logger.info("  - 打开编辑器耗时: ${openEditorDuration}ms")
            
            if (editor == null) {
                logger.warn("无法打开编辑器")
                return@invokeLater
            }
            
            logger.info("  - 文档行数: ${editor.document.lineCount}")
            
            // 滚动到目标行
            val scrollStartTime = System.currentTimeMillis()
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            val scrollDuration = System.currentTimeMillis() - scrollStartTime
            logger.info("  - 滚动到目标行耗时: ${scrollDuration}ms")
            
            // 高亮热点代码行
            val highlightStartTime = System.currentTimeMillis()
            val markupModel = editor.markupModel
            
            // 清除之前的高亮
            val clearStartTime = System.currentTimeMillis()
            markupModel.removeAllHighlighters()
            val clearDuration = System.currentTimeMillis() - clearStartTime
            logger.info("  - 清除旧高亮耗时: ${clearDuration}ms")
            
            // 为每个热点行添加高亮
            var highlightedCount = 0
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
                
                highlightedCount++
            }
            
            val highlightDuration = System.currentTimeMillis() - highlightStartTime
            logger.info("  - 添加高亮耗时: ${highlightDuration}ms")
            logger.info("  - 高亮行数: $highlightedCount")
            
            val uiTotalDuration = System.currentTimeMillis() - uiStartTime
            logger.info("  - UI 操作总耗时: ${uiTotalDuration}ms")
            logger.info("已打开文件并高亮: ${location.filePath}:${location.targetLine}")
        }
    }
    
    /**
     * 智能查找源文件
     * 支持多种路径格式：
     * 1. 绝对路径：/path/to/file.go
     * 2. 相对路径：src/main.go
     * 3. 包名路径：github.com/user/project/main.go
     * 4. GOPATH 路径：/Users/user/go/src/github.com/user/project/main.go
     */
    private fun findSourceFile(filePath: String): VirtualFile? {
        logger.info("开始查找源文件: $filePath")
        
        // 策略 1: 尝试作为绝对路径
        var file = LocalFileSystem.getInstance().findFileByPath(filePath)
        if (file != null) {
            logger.info("  - 策略 1 成功: 绝对路径")
            return file
        }
        
        // 策略 2: 在项目根目录中查找
        val projectBasePath = project.basePath
        if (projectBasePath != null) {
            file = LocalFileSystem.getInstance().findFileByPath("$projectBasePath/$filePath")
            if (file != null) {
                logger.info("  - 策略 2 成功: 项目根目录 + 相对路径")
                return file
            }
        }
        
        // 策略 3: 如果路径包含 GOPATH 结构（如 /go/src/github.com/...），提取包路径部分
        if (filePath.contains("/src/")) {
            val packagePath = filePath.substringAfter("/src/")
            logger.info("  - 检测到 GOPATH 结构，提取包路径: $packagePath")
            
            if (projectBasePath != null) {
                // 尝试在项目中查找
                file = LocalFileSystem.getInstance().findFileByPath("$projectBasePath/$packagePath")
                if (file != null) {
                    logger.info("  - 策略 3 成功: GOPATH 包路径匹配")
                    return file
                }
            }
        }
        
        // 策略 4: 提取文件名，在项目中搜索
        val fileName = filePath.substringAfterLast('/')
        logger.info("  - 提取文件名: $fileName")
        
        // 使用 FilenameIndex 查找文件
        val files = com.intellij.psi.search.FilenameIndex.getFilesByName(
            project,
            fileName,
            com.intellij.psi.search.GlobalSearchScope.projectScope(project)
        )
        
        logger.info("  - 找到 ${files.size} 个同名文件")
        
        if (files.isEmpty()) {
            logger.warn("  - 策略 4 失败: 未找到文件 $fileName")
            return null
        }
        
        // 策略 5: 如果只有一个同名文件，直接返回
        if (files.size == 1) {
            logger.info("  - 策略 5 成功: 唯一匹配")
            return files[0].virtualFile
        }
        
        // 策略 6: 多个文件，尝试精确路径匹配
        logger.info("  - 策略 6: 尝试精确路径匹配")
        
        // 提取路径的关键部分（去除 GOPATH 前缀）
        var cleanPath = filePath
        if (cleanPath.contains("/src/")) {
            cleanPath = cleanPath.substringAfter("/src/")
        }
        
        // 提取路径的后缀部分用于匹配（最后 4 级目录）
        val pathParts = cleanPath.split('/').filter { it.isNotEmpty() }
        val matchDepth = minOf(4, pathParts.size)
        val pathSuffix = pathParts.takeLast(matchDepth).joinToString("/")
        logger.info("  - 清理后的路径: $cleanPath")
        logger.info("  - 路径后缀 (深度=$matchDepth): $pathSuffix")
        
        // 首先尝试精确匹配路径后缀
        for (psiFile in files) {
            val candidatePath = psiFile.virtualFile.path
            logger.info("  - 检查候选文件: $candidatePath")
            
            if (candidatePath.endsWith(pathSuffix)) {
                logger.info("  - 策略 6 成功: 路径后缀精确匹配")
                return psiFile.virtualFile
            }
        }
        
        // 策略 7: 尝试包名路径匹配（逐级匹配）
        logger.info("  - 策略 7: 尝试包名路径逐级匹配")
        
        var bestMatch: com.intellij.psi.PsiFile? = null
        var bestMatchScore = 0
        
        for (psiFile in files) {
            val candidatePath = psiFile.virtualFile.path
            
            // 计算路径匹配分数
            var score = 0
            for (i in pathParts.indices.reversed()) {
                val part = pathParts[i]
                if (candidatePath.contains("/$part/") || candidatePath.endsWith("/$part")) {
                    score++
                } else {
                    break
                }
            }
            
            logger.info("  - 候选文件 $candidatePath 匹配分数: $score/${pathParts.size}")
            
            if (score > bestMatchScore) {
                bestMatchScore = score
                bestMatch = psiFile
            }
        }
        
        if (bestMatch != null && bestMatchScore >= 2) {
            logger.info("  - 策略 7 成功: 最佳匹配 (分数=$bestMatchScore)")
            return bestMatch.virtualFile
        }
        
        // 策略 8: 返回第一个匹配的文件（最后的兜底策略）
        logger.warn("  - 策略 8: 使用第一个匹配的文件（可能不准确）")
        return files[0].virtualFile
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
    
    /**
     * 显示通知
     */
    private fun showNotification(title: String, content: String, type: com.intellij.notification.NotificationType) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("pprofview.notifications")
            .createNotification(title, content, type)
            .notify(project)
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
