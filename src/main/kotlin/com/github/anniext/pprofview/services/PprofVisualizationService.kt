package com.github.anniext.pprofview.services

import com.github.anniext.pprofview.actions.VisualizationType
import com.github.anniext.pprofview.toolWindow.PprofOutputPanel
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import java.awt.Desktop
import java.io.File
import java.net.URI
import java.util.regex.Pattern

/**
 * pprof 可视化服务
 * 负责调用 go tool pprof 进行数据可视化
 */
@Service(Service.Level.PROJECT)
class PprofVisualizationService(private val project: Project) {
    private val logger = thisLogger()
    
    /**
     * 可视化 pprof 文件
     */
    fun visualize(file: VirtualFile, type: VisualizationType) {
        logger.info("开始可视化文件: ${file.path}, 类型: ${type.name}")
        
        // 检查是否是 trace 文件
        if (file.name.endsWith(".out") || file.name.contains("trace")) {
            visualizeTrace(file)
            return
        }
        
        when (type) {
            VisualizationType.WEB -> visualizeInBrowser(file)
            VisualizationType.TEXT -> visualizeAsText(file)
            VisualizationType.GRAPH -> generateSvg(file, "graph")
            VisualizationType.FLAMEGRAPH -> generateSvg(file, "flame")
            VisualizationType.TOP -> showTop(file)
            VisualizationType.LIST -> showList(file)
            VisualizationType.PEEK -> showPeek(file)
        }
    }
    
    /**
     * 可视化 trace 文件
     */
    private fun visualizeTrace(file: VirtualFile) {
        logger.info("显示 trace 信息: ${file.path}")
        
        // 获取文件信息
        val fileSize = File(file.path).length()
        val fileSizeStr = when {
            fileSize > 1024 * 1024 -> String.format("%.2f MB", fileSize / (1024.0 * 1024.0))
            fileSize > 1024 -> String.format("%.2f KB", fileSize / 1024.0)
            else -> "$fileSize bytes"
        }
        
        // 构建输出内容
        val output = buildTraceOutput(file, fileSizeStr)
        
        // 在工具窗口中显示
        showOutputInToolWindow("Trace - 执行追踪", output)
        
        // 同时启动 web 服务器
        startTraceWebServer(file)
    }
    
    /**
     * 启动 trace web 服务器
     */
    private fun startTraceWebServer(file: VirtualFile) {
        logger.info("启动 trace web 服务器: ${file.path}")
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameters("tool", "trace", "-http=:0", file.path)
        
        try {
            val processHandler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    if (outputType == ProcessOutputTypes.STDOUT || outputType == ProcessOutputTypes.STDERR) {
                        // 匹配 "Serving web UI on http://localhost:xxxxx"
                        val pattern = Pattern.compile("http://[^\\s]+")
                        val matcher = pattern.matcher(text)
                        if (matcher.find()) {
                            val url = matcher.group()
                            logger.info("检测到 trace web 服务地址: $url")
                            openInBrowser(url)
                            
                            showNotification(
                                "Trace 可视化已启动",
                                "浏览器将自动打开 $url\n关闭浏览器后，进程会自动停止",
                                NotificationType.INFORMATION
                            )
                        }
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("trace web 服务已停止")
                }
            })
            
            processHandler.startNotify()
        } catch (e: Exception) {
            logger.error("启动 trace web 服务失败", e)
            showNotification(
                "启动失败",
                "无法启动 trace web 服务: ${e.message}\n请确保已安装 Go 工具链",
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * 构建 trace 输出内容
     */
    private fun buildTraceOutput(file: VirtualFile, fileSize: String): String {
        val sb = StringBuilder()
        sb.appendLine("=" .repeat(80))
        sb.appendLine("执行追踪分析报告")
        sb.appendLine("=" .repeat(80))
        sb.appendLine()
        sb.appendLine("文件: ${file.name}")
        sb.appendLine("路径: ${file.path}")
        sb.appendLine("大小: $fileSize")
        sb.appendLine()
        sb.appendLine("-" .repeat(80))
        sb.appendLine("关于执行追踪")
        sb.appendLine("-" .repeat(80))
        sb.appendLine()
        sb.appendLine("执行追踪（Execution Trace）记录了程序运行期间的详细事件信息，包括：")
        sb.appendLine()
        sb.appendLine("  • Goroutine 的创建、阻塞、唤醒和销毁")
        sb.appendLine("  • 系统调用的进入和退出")
        sb.appendLine("  • GC 事件")
        sb.appendLine("  • 处理器的启动和停止")
        sb.appendLine("  • 网络阻塞事件")
        sb.appendLine()
        sb.appendLine("-" .repeat(80))
        sb.appendLine("查看可视化")
        sb.appendLine("-" .repeat(80))
        sb.appendLine()
        sb.appendLine("✓ 交互式 Web 界面已自动启动")
        sb.appendLine()
        sb.appendLine("如果浏览器没有自动打开，请手动在终端运行：")
        sb.appendLine("  go tool trace ${file.path}")
        sb.appendLine()
        sb.appendLine("Web 界面提供以下视图：")
        sb.appendLine()
        sb.appendLine("  • View trace - 时间线视图，显示所有事件")
        sb.appendLine("  • Goroutine analysis - 分析 goroutine 的执行情况")
        sb.appendLine("  • Network blocking profile - 网络阻塞分析")
        sb.appendLine("  • Synchronization blocking profile - 同步阻塞分析")
        sb.appendLine("  • Syscall blocking profile - 系统调用阻塞分析")
        sb.appendLine("  • Scheduler latency profile - 调度延迟分析")
        sb.appendLine()
        sb.appendLine("-" .repeat(80))
        sb.appendLine("使用提示")
        sb.appendLine("-" .repeat(80))
        sb.appendLine()
        sb.appendLine("1. 在时间线视图中，可以使用 WASD 键或鼠标拖动来导航")
        sb.appendLine("2. 点击事件可以查看详细信息")
        sb.appendLine("3. 使用搜索功能快速定位特定的 goroutine 或事件")
        sb.appendLine("4. 关注长时间阻塞的 goroutine，这些可能是性能瓶颈")
        sb.appendLine()
        
        return sb.toString()
    }
    
    /**
     * 从文件名中提取报告类型
     * 例如: cpu.pprof -> CPU, goroutine.pprof -> Goroutine
     */
    private fun extractReportType(file: VirtualFile): String {
        val fileName = file.nameWithoutExtension
        
        // 常见的 pprof 类型
        return when {
            fileName.contains("cpu", ignoreCase = true) -> "CPU"
            fileName.contains("goroutine", ignoreCase = true) -> "Goroutine"
            fileName.contains("heap", ignoreCase = true) -> "Heap"
            fileName.contains("allocs", ignoreCase = true) -> "Allocs"
            fileName.contains("mutex", ignoreCase = true) -> "Mutex"
            fileName.contains("block", ignoreCase = true) -> "Block"
            fileName.contains("threadcreate", ignoreCase = true) -> "ThreadCreate"
            else -> fileName.split("_", "-", ".").firstOrNull()?.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase() else it.toString() 
            } ?: "Profile"
        }
    }
    
    /**
     * 在浏览器中打开交互式可视化
     */
    private fun visualizeInBrowser(file: VirtualFile) {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameters("tool", "pprof", "-http=:0", file.path)
        
        try {
            val processHandler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    if (outputType == ProcessOutputTypes.STDOUT || outputType == ProcessOutputTypes.STDERR) {
                        // 匹配 "Serving web UI on http://localhost:xxxxx"
                        val pattern = Pattern.compile("http://[^\\s]+")
                        val matcher = pattern.matcher(text)
                        if (matcher.find()) {
                            val url = matcher.group()
                            logger.info("检测到 pprof web 服务地址: $url")
                            openInBrowser(url)
                            
                            showNotification(
                                "pprof 可视化已启动",
                                "浏览器将自动打开 $url\n关闭浏览器后，进程会自动停止",
                                NotificationType.INFORMATION
                            )
                        }
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("pprof web 服务已停止")
                }
            })
            
            processHandler.startNotify()
            
            showNotification(
                "正在启动 pprof Web 服务",
                "请稍候，浏览器将自动打开...",
                NotificationType.INFORMATION
            )
        } catch (e: Exception) {
            logger.error("启动 pprof web 服务失败", e)
            showNotification(
                "启动失败",
                "无法启动 pprof web 服务: ${e.message}\n请确保已安装 Go 工具链",
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * 显示文本格式报告
     */
    private fun visualizeAsText(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-text"), reportType)
    }
    
    /**
     * 生成 SVG 图表
     */
    private fun generateSvg(file: VirtualFile, graphType: String) {
        val outputFile = File(file.parent.path, "${file.nameWithoutExtension}_$graphType.svg")
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameters("tool", "pprof", "-$graphType", "-output=${outputFile.absolutePath}", file.path)
        
        try {
            val process = commandLine.createProcess()
            val exitCode = process.waitFor()
            
            if (exitCode == 0 && outputFile.exists()) {
                logger.info("SVG 文件已生成: ${outputFile.absolutePath}")
                
                // 刷新文件系统
                file.parent.refresh(false, false)
                
                // 在浏览器中打开
                openInBrowser(outputFile.toURI().toString())
                
                showNotification(
                    "SVG 已生成",
                    "文件保存在: ${outputFile.absolutePath}",
                    NotificationType.INFORMATION
                )
            } else {
                showNotification(
                    "生成失败",
                    "无法生成 SVG 文件，退出码: $exitCode",
                    NotificationType.ERROR
                )
            }
        } catch (e: Exception) {
            logger.error("生成 SVG 失败", e)
            showNotification(
                "生成失败",
                "无法生成 SVG: ${e.message}",
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * 显示 Top 函数
     */
    private fun showTop(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-top"), "$reportType - Top")
    }
    
    /**
     * 显示函数列表
     */
    private fun showList(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-list=."), "$reportType - 列表")
    }
    
    /**
     * 显示简要信息
     */
    private fun showPeek(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-peek=."), "$reportType - 简要")
    }
    
    /**
     * 执行命令并显示输出
     */
    private fun executeAndShowOutput(file: VirtualFile, args: List<String>, title: String) {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("tool")
        commandLine.addParameter("pprof")
        commandLine.addParameters(args)
        commandLine.addParameter(file.path)
        
        logger.info("执行 pprof 命令: ${commandLine.commandLineString}")
        
        try {
            val processHandler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)
            
            val output = StringBuilder()
            val errorOutput = StringBuilder()
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    output.append(text)
                    
                    // 分别记录标准错误输出
                    if (outputType == ProcessOutputTypes.STDERR) {
                        errorOutput.append(text)
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode == 0) {
                        // 在工具窗口中显示输出，传入 pprof 文件以支持代码导航
                        showOutputInToolWindow(title, output.toString(), file)
                    } else {
                        val errorMsg = if (errorOutput.isNotEmpty()) {
                            errorOutput.toString().trim()
                        } else {
                            output.toString().trim()
                        }
                        
                        logger.error("pprof 命令执行失败，退出码: ${event.exitCode}, 错误信息: $errorMsg")
                        
                        showNotification(
                            "执行失败",
                            "命令执行失败，退出码: ${event.exitCode}\n" +
                            "命令: ${commandLine.commandLineString}\n" +
                            if (errorMsg.isNotEmpty()) "错误: ${errorMsg.take(200)}" else "",
                            NotificationType.ERROR
                        )
                    }
                }
            })
            
            processHandler.startNotify()
        } catch (e: Exception) {
            logger.error("执行 pprof 命令失败", e)
            showNotification(
                "执行失败",
                "无法执行 pprof 命令: ${e.message}",
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * 在工具窗口中显示输出
     */
    private fun showOutputInToolWindow(title: String, content: String, pprofFile: VirtualFile? = null) {
        ApplicationManager.getApplication().invokeLater {
            // 打开工具窗口
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("pprof Output")
            toolWindow?.show {
                // 获取输出面板并添加内容（带可视化）
                val outputPanel = PprofOutputPanel.getInstance(project)
                outputPanel?.addOutputWithVisualization(title, content, pprofFile)
            }
            
            // 同时显示通知
            val lines = content.lines().take(5)
            val preview = lines.joinToString("\n")
            showNotification(
                title,
                preview + if (content.lines().size > 5) "\n...\n查看 pprof Output 工具窗口获取完整输出和可视化图表" else "",
                NotificationType.INFORMATION
            )
        }
        
        logger.info("$title 输出:\n$content")
    }
    
    /**
     * 在浏览器中打开 URL
     */
    private fun openInBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            logger.error("无法打开浏览器", e)
        }
    }
    
    /**
     * 显示通知
     */
    private fun showNotification(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("pprofview.notifications")
            .createNotification(title, content, type)
            .notify(project)
    }
}
