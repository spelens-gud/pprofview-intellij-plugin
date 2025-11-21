package com.github.spelens.pprofview.services

import com.github.spelens.pprofview.PprofViewBundle
import com.github.spelens.pprofview.actions.VisualizationType
import com.github.spelens.pprofview.toolWindow.PprofOutputPanel
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
 * pprof visualization service
 * Responsible for calling go tool pprof for data visualization
 */
@Service(Service.Level.PROJECT)
class PprofVisualizationService(private val project: Project) {
    private val logger = thisLogger()
    
    /**
     * Visualize pprof file
     */
    fun visualize(file: VirtualFile, type: VisualizationType) {
        logger.info(PprofViewBundle.message("pprof.viz.startingFile", file.path, type.name))
        
        // Check if it's a trace file
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
     * Visualize trace file
     */
    private fun visualizeTrace(file: VirtualFile) {
        logger.info(PprofViewBundle.message("pprof.viz.showingTrace", file.path))
        
        // Get file information
        val fileSize = File(file.path).length()
        val fileSizeStr = when {
            fileSize > 1024 * 1024 -> String.format("%.2f MB", fileSize / (1024.0 * 1024.0))
            fileSize > 1024 -> String.format("%.2f KB", fileSize / 1024.0)
            else -> "$fileSize bytes"
        }
        
        // Build output content
        val output = buildTraceOutput(file, fileSizeStr)
        
        // Show in tool window
        showOutputInToolWindow(PprofViewBundle.message("pprof.trace.title"), output)
        
        // Start web server
        startTraceWebServer(file)
    }
    
    /**
     * Start trace web server
     */
    private fun startTraceWebServer(file: VirtualFile) {
        logger.info(PprofViewBundle.message("pprof.viz.startingTraceServer", file.path))
        
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
                        // Match "Serving web UI on http://localhost:xxxxx"
                        val pattern = Pattern.compile("http://[^\\s]+")
                        val matcher = pattern.matcher(text)
                        if (matcher.find()) {
                            val url = matcher.group()
                            logger.info(PprofViewBundle.message("pprof.viz.traceAddressDetected", url))
                            openInBrowser(url)
                            
                            showNotification(
                                PprofViewBundle.message("pprof.trace.started"),
                                PprofViewBundle.message("pprof.visualization.browserWillOpen", url),
                                NotificationType.INFORMATION
                            )
                        }
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    logger.info(PprofViewBundle.message("pprof.viz.traceStopped"))
                    
                    // Clear code highlights when process stops
                    val navigationService = PprofCodeNavigationService.getInstance(project)
                    navigationService.clearHighlights()
                    logger.info(PprofViewBundle.message("pprof.viz.highlightsClearedTrace"))
                }
            })
            
            processHandler.startNotify()
        } catch (e: Exception) {
            logger.error(PprofViewBundle.message("pprof.viz.traceStartFailed"), e)
            showNotification(
                PprofViewBundle.message("pprof.visualization.startFailed"),
                PprofViewBundle.message("pprof.viz.traceStartError", e.message ?: ""),
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * Build trace output content
     */
    private fun buildTraceOutput(file: VirtualFile, fileSize: String): String {
        val sb = StringBuilder()
        sb.appendLine("=" .repeat(80))
        sb.appendLine(PprofViewBundle.message("pprof.viz.reportTitle"))
        sb.appendLine("=" .repeat(80))
        sb.appendLine()
        sb.appendLine("${PprofViewBundle.message("pprof.viz.file")}: ${file.name}")
        sb.appendLine("${PprofViewBundle.message("pprof.viz.path")}: ${file.path}")
        sb.appendLine("${PprofViewBundle.message("pprof.viz.size")}: $fileSize")
        sb.appendLine()
        sb.appendLine("-" .repeat(80))
        sb.appendLine(PprofViewBundle.message("pprof.viz.aboutTrace"))
        sb.appendLine("-" .repeat(80))
        sb.appendLine()
        sb.appendLine(PprofViewBundle.message("pprof.viz.traceDescription"))
        sb.appendLine()
        sb.appendLine(PprofViewBundle.message("pprof.viz.goroutineEvents"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.syscallEvents"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.gcEvents"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.processorEvents"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.networkEvents"))
        sb.appendLine()
        sb.appendLine("-" .repeat(80))
        sb.appendLine(PprofViewBundle.message("pprof.viz.viewVisualization"))
        sb.appendLine("-" .repeat(80))
        sb.appendLine()
        sb.appendLine(PprofViewBundle.message("pprof.viz.webStarted"))
        sb.appendLine()
        sb.appendLine(PprofViewBundle.message("pprof.viz.manualCommand"))
        sb.appendLine("  go tool trace ${file.path}")
        sb.appendLine()
        sb.appendLine(PprofViewBundle.message("pprof.viz.webViews"))
        sb.appendLine()
        sb.appendLine(PprofViewBundle.message("pprof.viz.viewTrace"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.goroutineAnalysis"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.networkBlocking"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.syncBlocking"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.syscallBlocking"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.schedulerLatency"))
        sb.appendLine()
        sb.appendLine("-" .repeat(80))
        sb.appendLine(PprofViewBundle.message("pprof.viz.usageTips"))
        sb.appendLine("-" .repeat(80))
        sb.appendLine()
        sb.appendLine(PprofViewBundle.message("pprof.viz.tip1"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.tip2"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.tip3"))
        sb.appendLine(PprofViewBundle.message("pprof.viz.tip4"))
        sb.appendLine()
        
        return sb.toString()
    }
    
    /**
     * Extract report type from the file name
     * For example: cpu.pprof -> CPU, goroutine.pprof -> Goroutine
     */
    private fun extractReportType(file: VirtualFile): String {
        val fileName = file.nameWithoutExtension
        
        // Common pprof types
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
     * Open interactive visualization in browser
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
                        // Match "Serving web UI on http://localhost:xxxxx"
                        val pattern = Pattern.compile("http://[^\\s]+")
                        val matcher = pattern.matcher(text)
                        if (matcher.find()) {
                            val url = matcher.group()
                            logger.info(PprofViewBundle.message("pprof.viz.pprofAddressDetected", url))
                            openInBrowser(url)
                            
                            showNotification(
                                PprofViewBundle.message("pprof.visualization.started"),
                                PprofViewBundle.message("pprof.visualization.browserWillOpen", url),
                                NotificationType.INFORMATION
                            )
                        }
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    logger.info(PprofViewBundle.message("pprof.viz.pprofStopped"))
                    
                    // Clear code highlights when process stops
                    val navigationService = PprofCodeNavigationService.getInstance(project)
                    navigationService.clearHighlights()
                    logger.info(PprofViewBundle.message("pprof.viz.highlightsClearedPprof"))
                }
            })
            
            processHandler.startNotify()
            
            showNotification(
                PprofViewBundle.message("pprof.visualization.starting"),
                PprofViewBundle.message("pprof.visualization.pleaseWait"),
                NotificationType.INFORMATION
            )
        } catch (e: Exception) {
            logger.error(PprofViewBundle.message("pprof.viz.pprofStartFailed"), e)
            showNotification(
                PprofViewBundle.message("pprof.visualization.startFailed"),
                PprofViewBundle.message("pprof.viz.pprofStartError", e.message ?: ""),
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * Display text format report
     */
    private fun visualizeAsText(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-text"), reportType)
    }
    
    /**
     * Generate SVG chart
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
                logger.info(PprofViewBundle.message("pprof.viz.svgGenerated", outputFile.absolutePath))
                
                // Refresh file system
                file.parent.refresh(false, false)
                
                // Open in browser
                openInBrowser(outputFile.toURI().toString())
                
                showNotification(
                    PprofViewBundle.message("pprof.visualization.svgGenerated"),
                    PprofViewBundle.message("pprof.visualization.svgSavedAt", outputFile.absolutePath),
                    NotificationType.INFORMATION
                )
            } else {
                showNotification(
                    PprofViewBundle.message("pprof.visualization.generateFailed"),
                    PprofViewBundle.message("pprof.viz.svgGenerateFailed", exitCode),
                    NotificationType.ERROR
                )
            }
        } catch (e: Exception) {
            logger.error(PprofViewBundle.message("pprof.visualization.generateFailed"), e)
            showNotification(
                PprofViewBundle.message("pprof.visualization.generateFailed"),
                PprofViewBundle.message("pprof.viz.svgGenerateError", e.message ?: ""),
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * Show Top Functions
     */
    private fun showTop(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-top"), "$reportType - Top")
    }
    
    /**
     * Show function list
     */
    private fun showList(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-list=."), "$reportType - ${PprofViewBundle.message("pprof.viz.list")}")
    }
    
    /**
     * Show brief information
     */
    private fun showPeek(file: VirtualFile) {
        val reportType = extractReportType(file)
        executeAndShowOutput(file, listOf("-peek=."), "$reportType - ${PprofViewBundle.message("pprof.viz.peek")}")
    }
    
    /**
     * Execute command and show output
     */
    private fun executeAndShowOutput(file: VirtualFile, args: List<String>, title: String) {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("tool")
        commandLine.addParameter("pprof")
        commandLine.addParameters(args)
        commandLine.addParameter(file.path)
        
        logger.info("Executing pprof command: ${commandLine.commandLineString}")
        
        try {
            val processHandler = ProcessHandlerFactory.getInstance()
                .createColoredProcessHandler(commandLine)
            
            val output = StringBuilder()
            val errorOutput = StringBuilder()
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    output.append(text)
                    
                    // Record stderr separately
                    if (outputType == ProcessOutputTypes.STDERR) {
                        errorOutput.append(text)
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    if (event.exitCode == 0) {
                        // Show output in tool window, pass pprof file to support code navigation
                        showOutputInToolWindow(title, output.toString(), file)
                    } else {
                        val errorMsg = if (errorOutput.isNotEmpty()) {
                            errorOutput.toString().trim()
                        } else {
                            output.toString().trim()
                        }
                        
                        logger.error("pprof command execution failed, exit code: ${event.exitCode}, error: $errorMsg")
                        
                        showNotification(
                            PprofViewBundle.message("pprof.visualization.executionFailed"),
                            PprofViewBundle.message("pprof.viz.commandFailed", 
                                event.exitCode, 
                                commandLine.commandLineString,
                                if (errorMsg.isNotEmpty()) errorMsg.take(200) else ""
                            ),
                            NotificationType.ERROR
                        )
                    }
                    
                    // Clear code highlights when process stops
                    val navigationService = PprofCodeNavigationService.getInstance(project)
                    navigationService.clearHighlights()
                    logger.info(PprofViewBundle.message("pprof.viz.highlightsClearedCommand"))
                }
            })
            
            processHandler.startNotify()
        } catch (e: Exception) {
            logger.error("Failed to execute pprof command", e)
            showNotification(
                PprofViewBundle.message("pprof.visualization.executionFailed"),
                PprofViewBundle.message("pprof.viz.commandError", e.message ?: ""),
                NotificationType.ERROR
            )
        }
    }
    
    /**
     * Show output in tool window
     */
    private fun showOutputInToolWindow(title: String, content: String, pprofFile: VirtualFile? = null) {
        ApplicationManager.getApplication().invokeLater {
            // Open tool window
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("pprof Output")
            toolWindow?.show {
                // Get output panel and add content (with visualization)
                val outputPanel = PprofOutputPanel.getInstance(project)
                outputPanel?.addOutputWithVisualization(title, content, pprofFile)
            }
        }
        
        logger.info(PprofViewBundle.message("pprof.viz.outputShown", title, content))
    }
    
    /**
     * Open URL in browser
     */
    private fun openInBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            logger.error(PprofViewBundle.message("pprof.viz.browserOpenFailed"), e)
        }
    }
    
    /**
     * Show notifications
     */
    private fun showNotification(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("pprofview.notifications")
            .createNotification(title, content, type)
            .notify(project)
    }
}
