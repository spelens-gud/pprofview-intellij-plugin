package com.github.spelens.pprofview.runconfig

import com.github.spelens.pprofview.PprofViewBundle
import com.github.spelens.pprofview.actions.VisualizationType
import com.github.spelens.pprofview.services.PprofVisualizationService
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * Pprof run state
 */
class PprofRunState(
    environment: ExecutionEnvironment,
    private val configuration: PprofConfiguration
) : CommandLineState(environment) {
    
    override fun startProcess(): ProcessHandler {
        val logger = thisLogger()
        
        // Clear old data from Pprof Plus: Visual Analytics window
        clearPprofOutput()
        
        val collectionMode = PprofCollectionMode.fromString(configuration.collectionMode)
        logger.info("Collection mode: $collectionMode")
        
        // Choose different startup methods based on collection mode
        return when (collectionMode) {
            PprofCollectionMode.RUNTIME_SAMPLING -> startWithRuntimeSampling()
            PprofCollectionMode.HTTP_SERVER -> startWithHttpServer()
            PprofCollectionMode.TEST_SAMPLING -> startWithTestSampling()
        }
    }
    
    /**
     * Start with HTTP server mode
     */
    private fun startWithHttpServer(): ProcessHandler {
        val logger = thisLogger()
        logger.info("Using HTTP server mode")
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("run")
        
        // Add build flags
        if (configuration.goBuildFlags.isNotEmpty()) {
            configuration.goBuildFlags.split(" ").forEach { flag ->
                if (flag.isNotBlank()) {
                    commandLine.addParameter(flag)
                }
            }
        }
        
        // Add parameters based on run kind
        val runKind = PprofRunKind.fromString(configuration.runKind)
        logger.info("Run kind: $runKind")
        when (runKind) {
            PprofRunKind.FILE -> {
                if (configuration.filePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.filePath)
                    logger.info("File path: ${configuration.filePath}")
                }
            }
            PprofRunKind.DIRECTORY -> {
                if (configuration.directoryPath.isNotEmpty()) {
                    commandLine.addParameter(configuration.directoryPath)
                    logger.info("Directory path: ${configuration.directoryPath}")
                }
            }
            PprofRunKind.PACKAGE -> {
                if (configuration.packagePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.packagePath)
                    logger.info("Package path: ${configuration.packagePath}")
                }
            }
        }
        
        // Inject HTTP server initialization file
        var pprofHttpFile: File? = null
        logger.info("Injecting pprof HTTP server initialization file...")
        pprofHttpFile = injectPprofHttpInit()
        if (pprofHttpFile != null) {
            commandLine.addParameter(pprofHttpFile.absolutePath)
            logger.info("Injected pprof HTTP initialization file: ${pprofHttpFile.absolutePath}")
        } else {
            logger.warn("Failed to inject pprof HTTP initialization file")
        }
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // Add program arguments
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // Add environment variables
        addEnvironmentVariables(commandLine)
        
        // Set HTTP port environment variable
        commandLine.environment["PPROF_HTTP_PORT"] = configuration.httpPort.toString()
        
        // Print complete command line for debugging
        logger.info("Execute command: ${commandLine.commandLineString}")
        logger.info("Working directory: ${commandLine.workDirectory}")
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        
        // Listen to output, display pprof HTTP service address
        processHandler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val text = event.text
                // Detect pprof HTTP service startup info
                if (text.contains("[pprofview-http]")) {
                    logger.info("Detected pprof HTTP service info: $text")
                }
            }
            
            override fun processTerminated(event: ProcessEvent) {
                logger.info("HTTP service process terminated, exit code: ${event.exitCode}")
                // Clean up temporary files
                pprofHttpFile?.delete()
            }
        })
        
        // Output prompt information to console
        processHandler.notifyTextAvailable(
            "[pprofview] HTTP service mode started\n" +
            "[pprofview] pprof data will be provided via HTTP service\n" +
            "[pprofview] Default address: http://localhost:${configuration.httpPort}/debug/pprof/\n",
            ProcessOutputTypes.SYSTEM
        )
        
        return processHandler
    }
    
    /**
     * 使用测试时采样模式启动
     */
    private fun startWithTestSampling(): ProcessHandler {
        val logger = thisLogger()
        logger.info("Using test sampling mode")
        
        val outputDir = getOutputDirectory()
        cleanOldPprofFiles(outputDir)
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("test")
        
        // Add build flags
        if (configuration.goBuildFlags.isNotEmpty()) {
            configuration.goBuildFlags.split(" ").forEach { flag ->
                if (flag.isNotBlank()) {
                    commandLine.addParameter(flag)
                }
            }
        }
        
        // Add parameters based on run kind
        val runKind = PprofRunKind.fromString(configuration.runKind)
        logger.info("Run kind: $runKind")
        when (runKind) {
            PprofRunKind.FILE -> {
                // go test doesn't support single files, use directory
                if (configuration.filePath.isNotEmpty()) {
                    val dir = File(configuration.filePath).parent
                    if (dir != null) {
                        commandLine.addParameter(dir)
                        logger.info("Test directory: $dir")
                    }
                }
            }
            PprofRunKind.DIRECTORY -> {
                if (configuration.directoryPath.isNotEmpty()) {
                    commandLine.addParameter(configuration.directoryPath)
                    logger.info("Test directory: ${configuration.directoryPath}")
                }
            }
            PprofRunKind.PACKAGE -> {
                if (configuration.packagePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.packagePath)
                    logger.info("Test package: ${configuration.packagePath}")
                }
            }
        }
        
        // Add test mode options (supports regex)
        if (configuration.testPattern.isNotEmpty()) {
            commandLine.addParameter("-run=${configuration.testPattern}")
            logger.info("Test mode option: ${configuration.testPattern}")
        }
        
        // Add CPU profile parameter
        if (configuration.profileTypes.contains(PprofProfileType.CPU.name)) {
            val cpuProfilePath = File(outputDir, "cpu.pprof").absolutePath
            commandLine.addParameter("-cpuprofile=$cpuProfilePath")
            logger.info("CPU profile 输出: $cpuProfilePath")
        }
        
        // Add memory profile parameter
        if (configuration.profileTypes.contains(PprofProfileType.HEAP.name)) {
            val memProfilePath = File(outputDir, "mem.pprof").absolutePath
            commandLine.addParameter("-memprofile=$memProfilePath")
            logger.info("Memory profile 输出: $memProfilePath")
        }
        
        // Add block profile parameter
        if (configuration.profileTypes.contains(PprofProfileType.BLOCK.name)) {
            val blockProfilePath = File(outputDir, "block.pprof").absolutePath
            commandLine.addParameter("-blockprofile=$blockProfilePath")
            logger.info("Block profile 输出: $blockProfilePath")
        }
        
        // Add mutex profile parameter
        if (configuration.profileTypes.contains(PprofProfileType.MUTEX.name)) {
            val mutexProfilePath = File(outputDir, "mutex.pprof").absolutePath
            commandLine.addParameter("-mutexprofile=$mutexProfilePath")
            logger.info("Mutex profile 输出: $mutexProfilePath")
        }
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // Add program arguments
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // Add environment variables
        addEnvironmentVariables(commandLine)
        
        // Print complete command line for debugging
        logger.info("Execute command: ${commandLine.commandLineString}")
        logger.info("Working directory: ${commandLine.workDirectory}")
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        
        // If auto-open result is enabled, monitor process termination
        if (configuration.autoOpenResult) {
            logger.info("Will visualize after test completion, output directory: ${outputDir.absolutePath}")
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("Test process terminated, exit code: ${event.exitCode}")
                    
                    // 测试结束后自动打开可视化
                    Thread {
                        Thread.sleep(1000) // 等待文件完全写入
                        autoOpenVisualization(outputDir)
                    }.start()
                }
            })
        }
        
        return processHandler
    }
    
    /**
     * Start with runtime sampling mode
     */
    private fun startWithRuntimeSampling(): ProcessHandler {
        val logger = thisLogger()
        logger.info("Using runtime sampling mode")
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("run")
        
        // Add build flags
        if (configuration.goBuildFlags.isNotEmpty()) {
            configuration.goBuildFlags.split(" ").forEach { flag ->
                if (flag.isNotBlank()) {
                    commandLine.addParameter(flag)
                }
            }
        }
        
        // Add parameters based on run kind
        val runKind = PprofRunKind.fromString(configuration.runKind)
        logger.info("Run kind: $runKind")
        when (runKind) {
            PprofRunKind.FILE -> {
                if (configuration.filePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.filePath)
                    logger.info("File path: ${configuration.filePath}")
                } else {
                    logger.warn("File path is empty")
                }
            }
            PprofRunKind.DIRECTORY -> {
                if (configuration.directoryPath.isNotEmpty()) {
                    commandLine.addParameter(configuration.directoryPath)
                    logger.info("Directory path: ${configuration.directoryPath}")
                } else {
                    logger.warn("Directory path is empty")
                }
            }
            PprofRunKind.PACKAGE -> {
                if (configuration.packagePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.packagePath)
                    logger.info("Package path: ${configuration.packagePath}")
                } else {
                    logger.warn("Package path is empty")
                }
            }
        }
        
        // If pprof is enabled and in runtime sampling mode, inject pprof initialization file (after user files)
        var pprofInitFile: File? = null
        logger.info("Pprof 配置: enablePprof=${configuration.enablePprof}")
        if (configuration.enablePprof) {
            logger.info("Starting to inject pprof initialization file...")
            pprofInitFile = injectPprofInit()
            if (pprofInitFile != null) {
                commandLine.addParameter(pprofInitFile.absolutePath)
                logger.info("Injected pprof initialization file: ${pprofInitFile.absolutePath}")
            } else {
                logger.warn("Failed to inject pprof initialization file")
            }
        } else {
            logger.info("Skipping pprof injection")
        }
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // Add program arguments
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // Add environment variables
        addEnvironmentVariables(commandLine)
        
        // Add pprof-related environment variables
        if (configuration.enablePprof) {
            val outputDir = getOutputDirectory()
            cleanOldPprofFiles(outputDir)
            addPprofEnvironmentVariables(commandLine)
        }
        
        // Print complete command line for debugging
        logger.info("Execute command: ${commandLine.commandLineString}")
        logger.info("Working directory: ${commandLine.workDirectory}")
        logger.info("环境变量: ${commandLine.environment.filter { it.key.startsWith("PPROF_") }}")
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        
        // If auto-open result is enabled, monitor output and process termination
        logger.info("autoOpenResult=${configuration.autoOpenResult}")
        if (configuration.enablePprof && configuration.autoOpenResult) {
            val outputDir = getOutputDirectory()
            logger.info("Will visualize after data save completion or process termination, output directory: ${outputDir.absolutePath}")
            
            var visualizationTriggered = false
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    
                    // Detected log of pprof data save completion
                    val allDataSavedMsg = PprofViewBundle.message("pprof.runtime.allDataSaved")
                    if (!visualizationTriggered && text.contains("[pprofview] $allDataSavedMsg")) {
                        visualizationTriggered = true
                        logger.info("Detected pprof data save completion, visualizing immediately")
                        
                        // Execute visualization in background thread to avoid blocking output processing
                        Thread {
                            Thread.sleep(500) // Short delay to ensure file is fully written
                            autoOpenVisualization(outputDir)
                        }.start()
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("Process terminated, exit code: ${event.exitCode}")
                    
                    // Clean up temporary files
                    pprofInitFile?.delete()
                    
                    // If visualization hasn't been triggered yet, trigger it when process ends (fallback)
                    if (!visualizationTriggered) {
                        logger.info("Triggering visualization on process end (fallback)")
                        Thread.sleep(1000)
                        autoOpenVisualization(outputDir)
                    }
                }
            })
        } else if (pprofInitFile != null) {
            // Clean up temporary files even if not auto-opening
            logger.info("Not auto-opening, only registering cleanup listener")
            processHandler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("Process terminated (cleanup only), exit code: ${event.exitCode}")
                    pprofInitFile?.delete()
                }
            })
        }
        
        return processHandler
    }
    
    /**
     * Inject pprof initialization file
     */
    private fun injectPprofInit(): File? {
        val logger = thisLogger()
        try {
            // Read pprof_init.go template from resources
            val inputStream = javaClass.classLoader.getResourceAsStream("pprof_runtime/pprof_init.go")
                ?: return null
            
            // Determine target directory - must be in same directory as user code
            val targetDir = when (PprofRunKind.fromString(configuration.runKind)) {
                PprofRunKind.FILE -> {
                    if (configuration.filePath.isNotEmpty()) {
                        File(configuration.filePath).parentFile
                    } else null
                }
                PprofRunKind.DIRECTORY -> {
                    if (configuration.directoryPath.isNotEmpty()) {
                        File(configuration.directoryPath)
                    } else null
                }
                else -> null
            } ?: File(configuration.workingDirectory).takeIf { it.exists() } ?: return null
            
            // Create temporary file in target directory (cannot start with ., otherwise ignored by Go build tools)
            val tempFile = File(targetDir, "zzz_pprofview_init_${System.currentTimeMillis()}.go")
            
            // Write content
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            logger.info("Created pprof initialization file: ${tempFile.absolutePath}")
            return tempFile
        } catch (e: Exception) {
            logger.error("Cannot inject pprof initialization file", e)
            return null
        }
    }
    
    /**
     * Inject pprof HTTP service initialization file
     */
    private fun injectPprofHttpInit(): File? {
        val logger = thisLogger()
        try {
            // Determine target directory - must be in same directory as user code
            val targetDir = when (PprofRunKind.fromString(configuration.runKind)) {
                PprofRunKind.FILE -> {
                    if (configuration.filePath.isNotEmpty()) {
                        File(configuration.filePath).parentFile
                    } else null
                }
                PprofRunKind.DIRECTORY -> {
                    if (configuration.directoryPath.isNotEmpty()) {
                        File(configuration.directoryPath)
                    } else null
                }
                else -> null
            } ?: File(configuration.workingDirectory).takeIf { it.exists() } ?: return null
            
            // Create temporary file in target directory
            val tempFile = File(targetDir, "zzz_pprofview_http_${System.currentTimeMillis()}.go")
            
            // Generate HTTP service code
            val httpCode = """
package main

import (
    "fmt"
    "log"
    "net/http"
    _ "net/http/pprof"
    "os"
    "strconv"
)

func init() {
    port := 6060
    if portStr := os.Getenv("PPROF_HTTP_PORT"); portStr != "" {
        if p, err := strconv.Atoi(portStr); err == nil {
            port = p
        }
    }
    
    go func() {
        addr := fmt.Sprintf("localhost:%d", port)
        fmt.Printf("[pprofview-http] pprof HTTP service started\n")
        fmt.Printf("[pprofview-http] Access address: http://%s/debug/pprof/\n", addr)
        fmt.Printf("[pprofview-http] CPU Profile: http://%s/debug/pprof/profile\n", addr)
        fmt.Printf("[pprofview-http] Heap Profile: http://%s/debug/pprof/heap\n", addr)
        fmt.Printf("[pprofview-http] Goroutine: http://%s/debug/pprof/goroutine\n", addr)
        
        if err := http.ListenAndServe(addr, nil); err != nil {
            log.Printf("[pprofview-http] HTTP service startup failed: %v\n", err)
        }
    }()
}
""".trimIndent()
            
            // Write content
            tempFile.writeText(httpCode)
            
            logger.info("Created pprof HTTP initialization file: ${tempFile.absolutePath}")
            return tempFile
        } catch (e: Exception) {
            logger.error("Cannot inject pprof HTTP initialization file", e)
            return null
        }
    }
    
    /**
     * Add environment variable
     */
    private fun addEnvironmentVariables(commandLine: GeneralCommandLine) {
        if (configuration.environmentVariables.isNotEmpty()) {
            configuration.environmentVariables.split(";").forEach { envVar ->
                val parts = envVar.split("=", limit = 2)
                if (parts.size == 2) {
                    commandLine.environment[parts[0]] = parts[1]
                }
            }
        }
    }
    
    /**
     * Add pprof-related environment variables
     */
    private fun addPprofEnvironmentVariables(commandLine: GeneralCommandLine) {
        val logger = thisLogger()
        val outputDir = getOutputDirectory()
        
        commandLine.environment["PPROF_OUTPUT_DIR"] = outputDir.absolutePath
        logger.info("Set PPROF_OUTPUT_DIR=${outputDir.absolutePath}")
        
        // Set sampling rates
        if (configuration.memProfileRate > 0) {
            commandLine.environment["PPROF_MEM_RATE"] = configuration.memProfileRate.toString()
        }
        
        if (configuration.blockProfileRate > 0) {
            commandLine.environment["PPROF_BLOCK_RATE"] = configuration.blockProfileRate.toString()
        }
        
        if (configuration.mutexProfileFraction > 0) {
            commandLine.environment["PPROF_MUTEX_FRACTION"] = configuration.mutexProfileFraction.toString()
        }
        
        // Set CPU sampling duration
        commandLine.environment["PPROF_CPU_DURATION"] = configuration.cpuDuration.toString()
        
        // Set sampling mode and interval
        commandLine.environment["PPROF_SAMPLING_MODE"] = configuration.samplingMode
        commandLine.environment["PPROF_SAMPLING_INTERVAL"] = configuration.samplingInterval.toString()
        logger.info("Sampling mode: ${configuration.samplingMode}, interval: ${configuration.samplingInterval} seconds")
        
        // Set enabled profile types
        logger.info("Profile types: ${configuration.profileTypes}")
        configuration.profileTypes.split(",").forEach { typeStr ->
            val type = PprofProfileType.fromString(typeStr.trim())
            if (type != null) {
                when (type) {
                    PprofProfileType.CPU -> {
                        commandLine.environment["PPROF_ENABLE_CPU"] = "true"
                        logger.info("Enabled CPU profiling")
                    }
                    PprofProfileType.HEAP -> {
                        commandLine.environment["PPROF_ENABLE_HEAP"] = "true"
                        logger.info("Enabled HEAP profiling")
                    }
                    PprofProfileType.GOROUTINE -> {
                        commandLine.environment["PPROF_ENABLE_GOROUTINE"] = "true"
                    }
                    PprofProfileType.BLOCK -> {
                        commandLine.environment["PPROF_ENABLE_BLOCK"] = "true"
                    }
                    PprofProfileType.MUTEX -> {
                        commandLine.environment["PPROF_ENABLE_MUTEX"] = "true"
                    }
                    PprofProfileType.ALLOCS -> {
                        commandLine.environment["PPROF_ENABLE_ALLOCS"] = "true"
                    }
                    PprofProfileType.THREAD_CREATE -> {
                        commandLine.environment["PPROF_ENABLE_THREADCREATE"] = "true"
                    }
                    PprofProfileType.TRACE -> {
                        commandLine.environment["PPROF_ENABLE_TRACE"] = "true"
                        logger.info("Enabled TRACE profiling")
                    }
                }
            }
        }
    }
    
    /**
     * Get output directory
     */
    private fun getOutputDirectory(): File {
        val dirPath = if (configuration.outputDirectory.isNotEmpty()) {
            configuration.outputDirectory
        } else {
            FileUtil.getTempDirectory()
        }
        
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir
    }
    
    /**
     * Clear old pprof and trace files from the output directory
     */
    private fun cleanOldPprofFiles(outputDir: File) {
        val logger = thisLogger()
        try {
            val profileFiles = outputDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".pprof") || file.name.endsWith(".out"))
            }
            
            if (!profileFiles.isNullOrEmpty()) {
                logger.info("Cleaning ${profileFiles.size} old profile files")
                profileFiles.forEach { file ->
                    if (file.delete()) {
                        logger.info("Deleted: ${file.name}")
                    } else {
                        logger.warn("Cannot delete: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to clean old profile files", e)
        }
    }
    
    /**
     * Automatically open visualization
     */
    private fun autoOpenVisualization(outputDir: File) {
        val logger = thisLogger()
        ApplicationManager.getApplication().invokeLater {
            val project = environment.project
            
            // Find generated pprof and trace files
            val profileFiles = outputDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".pprof") || file.name.endsWith(".out"))
            }
            
            if (profileFiles.isNullOrEmpty()) {
                logger.warn("No generated profile files found: ${outputDir.absolutePath}")
                return@invokeLater
            }
            
            logger.info("Found ${profileFiles.size} profile files")
            
            // Get user-selected profile types
            val selectedTypes = configuration.profileTypes.split(",")
                .map { it.trim() }
                .mapNotNull { PprofProfileType.fromString(it) }
                .toSet()
            
            logger.info("User-selected profile types: ${selectedTypes.map { it.name }}")
            
            // Refresh file system
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputDir)
            
            // Only generate reports for user-selected profile types
            profileFiles.sortedBy { it.name }.forEach { file ->
                // Determine profile type based on filename
                val profileType = matchProfileType(file.name)
                
                if (profileType != null && profileType in selectedTypes) {
                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    if (virtualFile != null) {
                        logger.info("Generating text report for ${file.name} (type: ${profileType.displayName})")
                        // Use TEXT type to display in Pprof Plus: Visual Analytics window
                        val visualizationService = project.service<PprofVisualizationService>()
                        visualizationService.visualize(virtualFile, VisualizationType.TEXT)
                    }
                } else {
                    logger.info("skip ${file.name}（type: ${profileType?.displayName ?: "unknow"}，Not selected）")
                }
            }
        }
    }
    
    /**
     * Match analysis type according to file name
     */
    private fun matchProfileType(fileName: String): PprofProfileType? {
        return PprofProfileType.entries.find { type ->
            fileName.contains(type.fileName.substringBefore("."), ignoreCase = true)
        }
    }
    
    /**
     * Clear data from the pprof Output window
     */
    private fun clearPprofOutput() {
        val logger = thisLogger()
        ApplicationManager.getApplication().invokeLater {
            try {
                val project = environment.project
                val outputPanel = com.github.spelens.pprofview.toolWindow.PprofOutputPanel.getInstance(project)
                if (outputPanel != null) {
                    outputPanel.clearAll()
                    logger.info("Cleared old data from pprof Output window")
                } else {
                    logger.warn("Cannot get pprof Output window instance")
                }
            } catch (e: Exception) {
                logger.error("Failed to clear pprof Output window", e)
            }
        }
    }
}

