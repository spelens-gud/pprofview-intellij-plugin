package com.github.anniext.pprofview.runconfig

import com.github.anniext.pprofview.actions.VisualizationType
import com.github.anniext.pprofview.services.PprofVisualizationService
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
 * Pprof 运行状态
 */
class PprofRunState(
    environment: ExecutionEnvironment,
    private val configuration: PprofConfiguration
) : CommandLineState(environment) {
    
    override fun startProcess(): ProcessHandler {
        val logger = thisLogger()
        
        // 清除 pprof Output 窗口的旧数据
        clearPprofOutput()
        
        val collectionMode = PprofCollectionMode.fromString(configuration.collectionMode)
        logger.info("采集模式: $collectionMode")
        
        // 根据采集模式选择不同的启动方式
        return when (collectionMode) {
            PprofCollectionMode.RUNTIME_SAMPLING -> startWithRuntimeSampling()
            PprofCollectionMode.HTTP_SERVER -> startWithHttpServer()
            PprofCollectionMode.TEST_SAMPLING -> startWithTestSampling()
        }
    }
    
    /**
     * 使用 HTTP 服务模式启动
     */
    private fun startWithHttpServer(): ProcessHandler {
        val logger = thisLogger()
        logger.info("使用 HTTP 服务模式")
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("run")
        
        // 添加构建标志
        if (configuration.goBuildFlags.isNotEmpty()) {
            configuration.goBuildFlags.split(" ").forEach { flag ->
                if (flag.isNotBlank()) {
                    commandLine.addParameter(flag)
                }
            }
        }
        
        // 根据运行种类添加参数
        val runKind = PprofRunKind.fromString(configuration.runKind)
        logger.info("运行种类: $runKind")
        when (runKind) {
            PprofRunKind.FILE -> {
                if (configuration.filePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.filePath)
                    logger.info("文件路径: ${configuration.filePath}")
                }
            }
            PprofRunKind.DIRECTORY -> {
                if (configuration.directoryPath.isNotEmpty()) {
                    commandLine.addParameter(configuration.directoryPath)
                    logger.info("目录路径: ${configuration.directoryPath}")
                }
            }
            PprofRunKind.PACKAGE -> {
                if (configuration.packagePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.packagePath)
                    logger.info("包路径: ${configuration.packagePath}")
                }
            }
        }
        
        // 注入 HTTP 服务初始化文件
        var pprofHttpFile: File? = null
        logger.info("注入 pprof HTTP 服务初始化文件...")
        pprofHttpFile = injectPprofHttpInit()
        if (pprofHttpFile != null) {
            commandLine.addParameter(pprofHttpFile.absolutePath)
            logger.info("已注入 pprof HTTP 初始化文件: ${pprofHttpFile.absolutePath}")
        } else {
            logger.warn("注入 pprof HTTP 初始化文件失败")
        }
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // 添加程序参数
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // 添加环境变量
        addEnvironmentVariables(commandLine)
        
        // 设置 HTTP 端口环境变量
        commandLine.environment["PPROF_HTTP_PORT"] = configuration.httpPort.toString()
        
        // 打印完整的命令行用于调试
        logger.info("执行命令: ${commandLine.commandLineString}")
        logger.info("工作目录: ${commandLine.workDirectory}")
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        
        // 监听输出，显示 pprof HTTP 服务地址
        processHandler.addProcessListener(object : ProcessListener {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val text = event.text
                // 检测 pprof HTTP 服务启动信息
                if (text.contains("[pprofview-http]")) {
                    logger.info("检测到 pprof HTTP 服务信息: $text")
                }
            }
            
            override fun processTerminated(event: ProcessEvent) {
                logger.info("HTTP 服务进程已终止，退出码: ${event.exitCode}")
                // 清理临时文件
                pprofHttpFile?.delete()
            }
        })
        
        // 在控制台输出提示信息
        processHandler.notifyTextAvailable(
            "[pprofview] HTTP 服务模式已启动\n" +
            "[pprofview] pprof 数据将通过 HTTP 服务提供\n" +
            "[pprofview] 默认地址: http://localhost:${configuration.httpPort}/debug/pprof/\n",
            ProcessOutputTypes.SYSTEM
        )
        
        return processHandler
    }
    
    /**
     * 使用测试时采样模式启动
     */
    private fun startWithTestSampling(): ProcessHandler {
        val logger = thisLogger()
        logger.info("使用测试时采样模式")
        
        val outputDir = getOutputDirectory()
        cleanOldPprofFiles(outputDir)
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("test")
        
        // 添加构建标志
        if (configuration.goBuildFlags.isNotEmpty()) {
            configuration.goBuildFlags.split(" ").forEach { flag ->
                if (flag.isNotBlank()) {
                    commandLine.addParameter(flag)
                }
            }
        }
        
        // 根据运行种类添加参数
        val runKind = PprofRunKind.fromString(configuration.runKind)
        logger.info("运行种类: $runKind")
        when (runKind) {
            PprofRunKind.FILE -> {
                // go test 不支持单个文件，使用目录
                if (configuration.filePath.isNotEmpty()) {
                    val dir = File(configuration.filePath).parent
                    if (dir != null) {
                        commandLine.addParameter(dir)
                        logger.info("测试目录: $dir")
                    }
                }
            }
            PprofRunKind.DIRECTORY -> {
                if (configuration.directoryPath.isNotEmpty()) {
                    commandLine.addParameter(configuration.directoryPath)
                    logger.info("测试目录: ${configuration.directoryPath}")
                }
            }
            PprofRunKind.PACKAGE -> {
                if (configuration.packagePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.packagePath)
                    logger.info("测试包: ${configuration.packagePath}")
                }
            }
        }
        
        // 添加 CPU profile 参数
        if (configuration.profileTypes.contains(PprofProfileType.CPU.name)) {
            val cpuProfilePath = File(outputDir, "cpu.pprof").absolutePath
            commandLine.addParameter("-cpuprofile=$cpuProfilePath")
            logger.info("CPU profile 输出: $cpuProfilePath")
        }
        
        // 添加内存 profile 参数
        if (configuration.profileTypes.contains(PprofProfileType.HEAP.name)) {
            val memProfilePath = File(outputDir, "mem.pprof").absolutePath
            commandLine.addParameter("-memprofile=$memProfilePath")
            logger.info("Memory profile 输出: $memProfilePath")
        }
        
        // 添加阻塞 profile 参数
        if (configuration.profileTypes.contains(PprofProfileType.BLOCK.name)) {
            val blockProfilePath = File(outputDir, "block.pprof").absolutePath
            commandLine.addParameter("-blockprofile=$blockProfilePath")
            logger.info("Block profile 输出: $blockProfilePath")
        }
        
        // 添加互斥锁 profile 参数
        if (configuration.profileTypes.contains(PprofProfileType.MUTEX.name)) {
            val mutexProfilePath = File(outputDir, "mutex.pprof").absolutePath
            commandLine.addParameter("-mutexprofile=$mutexProfilePath")
            logger.info("Mutex profile 输出: $mutexProfilePath")
        }
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // 添加程序参数
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // 添加环境变量
        addEnvironmentVariables(commandLine)
        
        // 打印完整的命令行用于调试
        logger.info("执行命令: ${commandLine.commandLineString}")
        logger.info("工作目录: ${commandLine.workDirectory}")
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        
        // 如果启用了自动打开结果，监控进程终止
        if (configuration.autoOpenResult) {
            logger.info("将在测试结束后进行可视化，输出目录: ${outputDir.absolutePath}")
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("测试进程已终止，退出码: ${event.exitCode}")
                    
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
     * 使用运行时采样模式启动
     */
    private fun startWithRuntimeSampling(): ProcessHandler {
        val logger = thisLogger()
        logger.info("使用运行时采样模式")
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("run")
        
        // 添加构建标志
        if (configuration.goBuildFlags.isNotEmpty()) {
            configuration.goBuildFlags.split(" ").forEach { flag ->
                if (flag.isNotBlank()) {
                    commandLine.addParameter(flag)
                }
            }
        }
        
        // 根据运行种类添加参数
        val runKind = PprofRunKind.fromString(configuration.runKind)
        logger.info("运行种类: $runKind")
        when (runKind) {
            PprofRunKind.FILE -> {
                if (configuration.filePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.filePath)
                    logger.info("文件路径: ${configuration.filePath}")
                } else {
                    logger.warn("文件路径为空")
                }
            }
            PprofRunKind.DIRECTORY -> {
                if (configuration.directoryPath.isNotEmpty()) {
                    commandLine.addParameter(configuration.directoryPath)
                    logger.info("目录路径: ${configuration.directoryPath}")
                } else {
                    logger.warn("目录路径为空")
                }
            }
            PprofRunKind.PACKAGE -> {
                if (configuration.packagePath.isNotEmpty()) {
                    commandLine.addParameter(configuration.packagePath)
                    logger.info("包路径: ${configuration.packagePath}")
                } else {
                    logger.warn("包路径为空")
                }
            }
        }
        
        // 如果启用了 pprof 且是运行时采样模式，注入 pprof 初始化文件（放在用户文件之后）
        var pprofInitFile: File? = null
        logger.info("Pprof 配置: enablePprof=${configuration.enablePprof}")
        if (configuration.enablePprof) {
            logger.info("开始注入 pprof 初始化文件...")
            pprofInitFile = injectPprofInit()
            if (pprofInitFile != null) {
                commandLine.addParameter(pprofInitFile.absolutePath)
                logger.info("已注入 pprof 初始化文件: ${pprofInitFile.absolutePath}")
            } else {
                logger.warn("注入 pprof 初始化文件失败")
            }
        } else {
            logger.info("跳过 pprof 注入")
        }
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // 添加程序参数
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // 添加环境变量
        addEnvironmentVariables(commandLine)
        
        // 添加 pprof 相关的环境变量
        if (configuration.enablePprof) {
            val outputDir = getOutputDirectory()
            cleanOldPprofFiles(outputDir)
            addPprofEnvironmentVariables(commandLine)
        }
        
        // 打印完整的命令行用于调试
        logger.info("执行命令: ${commandLine.commandLineString}")
        logger.info("工作目录: ${commandLine.workDirectory}")
        logger.info("环境变量: ${commandLine.environment.filter { it.key.startsWith("PPROF_") }}")
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        
        // 如果启用了自动打开结果，监控输出和进程终止
        logger.info("autoOpenResult=${configuration.autoOpenResult}")
        if (configuration.enablePprof && configuration.autoOpenResult) {
            val outputDir = getOutputDirectory()
            logger.info("将在数据保存完成或进程结束后进行可视化，输出目录: ${outputDir.absolutePath}")
            
            var visualizationTriggered = false
            
            processHandler.addProcessListener(object : ProcessListener {
                override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                    val text = event.text
                    
                    // 检测到 pprof 数据保存完成的日志
                    if (!visualizationTriggered && text.contains("[pprofview] 所有 pprof 数据已保存")) {
                        visualizationTriggered = true
                        logger.info("检测到 pprof 数据保存完成，立即进行可视化")
                        
                        // 在后台线程中执行可视化，避免阻塞输出处理
                        Thread {
                            Thread.sleep(500) // 短暂延迟确保文件完全写入
                            autoOpenVisualization(outputDir)
                        }.start()
                    }
                }
                
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("进程已终止，退出码: ${event.exitCode}")
                    
                    // 清理临时文件
                    pprofInitFile?.delete()
                    
                    // 如果还没有触发可视化，在进程结束时触发（备用方案）
                    if (!visualizationTriggered) {
                        logger.info("进程结束时触发可视化（备用方案）")
                        Thread.sleep(1000)
                        autoOpenVisualization(outputDir)
                    }
                }
            })
        } else if (pprofInitFile != null) {
            // 即使不自动打开，也要清理临时文件
            logger.info("不自动打开，仅注册清理监听器")
            processHandler.addProcessListener(object : ProcessListener {
                override fun processTerminated(event: ProcessEvent) {
                    logger.info("进程已终止（仅清理），退出码: ${event.exitCode}")
                    pprofInitFile?.delete()
                }
            })
        }
        
        return processHandler
    }
    
    /**
     * 注入 pprof 初始化文件
     */
    private fun injectPprofInit(): File? {
        val logger = thisLogger()
        try {
            // 从资源中读取 pprof_init.go 模板
            val inputStream = javaClass.classLoader.getResourceAsStream("pprof_runtime/pprof_init.go")
                ?: return null
            
            // 确定目标目录 - 必须与用户代码在同一目录
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
            
            // 在目标目录创建临时文件（不能以 . 开头，否则会被 Go 构建工具忽略）
            val tempFile = File(targetDir, "zzz_pprofview_init_${System.currentTimeMillis()}.go")
            
            // 写入内容
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            logger.info("创建 pprof 初始化文件: ${tempFile.absolutePath}")
            return tempFile
        } catch (e: Exception) {
            logger.error("无法注入 pprof 初始化文件", e)
            return null
        }
    }
    
    /**
     * 注入 pprof HTTP 服务初始化文件
     */
    private fun injectPprofHttpInit(): File? {
        val logger = thisLogger()
        try {
            // 确定目标目录 - 必须与用户代码在同一目录
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
            
            // 在目标目录创建临时文件
            val tempFile = File(targetDir, "zzz_pprofview_http_${System.currentTimeMillis()}.go")
            
            // 生成 HTTP 服务代码
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
        fmt.Printf("[pprofview-http] pprof HTTP 服务已启动\n")
        fmt.Printf("[pprofview-http] 访问地址: http://%s/debug/pprof/\n", addr)
        fmt.Printf("[pprofview-http] CPU Profile: http://%s/debug/pprof/profile\n", addr)
        fmt.Printf("[pprofview-http] Heap Profile: http://%s/debug/pprof/heap\n", addr)
        fmt.Printf("[pprofview-http] Goroutine: http://%s/debug/pprof/goroutine\n", addr)
        
        if err := http.ListenAndServe(addr, nil); err != nil {
            log.Printf("[pprofview-http] HTTP 服务启动失败: %v\n", err)
        }
    }()
}
""".trimIndent()
            
            // 写入内容
            tempFile.writeText(httpCode)
            
            logger.info("创建 pprof HTTP 初始化文件: ${tempFile.absolutePath}")
            return tempFile
        } catch (e: Exception) {
            logger.error("无法注入 pprof HTTP 初始化文件", e)
            return null
        }
    }
    
    /**
     * 添加环境变量
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
     * 添加 pprof 相关的环境变量
     */
    private fun addPprofEnvironmentVariables(commandLine: GeneralCommandLine) {
        val logger = thisLogger()
        val outputDir = getOutputDirectory()
        
        commandLine.environment["PPROF_OUTPUT_DIR"] = outputDir.absolutePath
        logger.info("设置 PPROF_OUTPUT_DIR=${outputDir.absolutePath}")
        
        // 设置采样率
        if (configuration.memProfileRate > 0) {
            commandLine.environment["PPROF_MEM_RATE"] = configuration.memProfileRate.toString()
        }
        
        if (configuration.blockProfileRate > 0) {
            commandLine.environment["PPROF_BLOCK_RATE"] = configuration.blockProfileRate.toString()
        }
        
        if (configuration.mutexProfileFraction > 0) {
            commandLine.environment["PPROF_MUTEX_FRACTION"] = configuration.mutexProfileFraction.toString()
        }
        
        // 设置 CPU 采样持续时间
        commandLine.environment["PPROF_CPU_DURATION"] = configuration.cpuDuration.toString()
        
        // 设置采样模式和间隔
        commandLine.environment["PPROF_SAMPLING_MODE"] = configuration.samplingMode
        commandLine.environment["PPROF_SAMPLING_INTERVAL"] = configuration.samplingInterval.toString()
        logger.info("采样模式: ${configuration.samplingMode}, 间隔: ${configuration.samplingInterval}秒")
        
        // 设置启用的分析类型
        logger.info("Profile types: ${configuration.profileTypes}")
        configuration.profileTypes.split(",").forEach { typeStr ->
            val type = PprofProfileType.fromString(typeStr.trim())
            if (type != null) {
                when (type) {
                    PprofProfileType.CPU -> {
                        commandLine.environment["PPROF_ENABLE_CPU"] = "true"
                        logger.info("启用 CPU profiling")
                    }
                    PprofProfileType.HEAP -> {
                        commandLine.environment["PPROF_ENABLE_HEAP"] = "true"
                        logger.info("启用 HEAP profiling")
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
                        logger.info("启用 TRACE profiling")
                    }
                }
            }
        }
    }
    
    /**
     * 获取输出目录
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
     * 清除输出目录中的旧 pprof 和 trace 文件
     */
    private fun cleanOldPprofFiles(outputDir: File) {
        val logger = thisLogger()
        try {
            val profileFiles = outputDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".pprof") || file.name.endsWith(".out"))
            }
            
            if (!profileFiles.isNullOrEmpty()) {
                logger.info("清除 ${profileFiles.size} 个旧的性能分析文件")
                profileFiles.forEach { file ->
                    if (file.delete()) {
                        logger.info("已删除: ${file.name}")
                    } else {
                        logger.warn("无法删除: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("清除旧性能分析文件失败", e)
        }
    }
    
    /**
     * 自动打开可视化
     */
    private fun autoOpenVisualization(outputDir: File) {
        val logger = thisLogger()
        ApplicationManager.getApplication().invokeLater {
            val project = environment.project
            
            // 查找生成的 pprof 和 trace 文件
            val profileFiles = outputDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".pprof") || file.name.endsWith(".out"))
            }
            
            if (profileFiles.isNullOrEmpty()) {
                logger.warn("未找到生成的性能分析文件: ${outputDir.absolutePath}")
                return@invokeLater
            }
            
            logger.info("找到 ${profileFiles.size} 个性能分析文件")
            
            // 获取用户选中的分析类型
            val selectedTypes = configuration.profileTypes.split(",")
                .map { it.trim() }
                .mapNotNull { PprofProfileType.fromString(it) }
                .toSet()
            
            logger.info("用户选中的分析类型: ${selectedTypes.map { it.name }}")
            
            // 刷新文件系统
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputDir)
            
            // 只为用户选中的分析类型生成报告
            profileFiles.sortedBy { it.name }.forEach { file ->
                // 根据文件名判断分析类型
                val profileType = matchProfileType(file.name)
                
                if (profileType != null && profileType in selectedTypes) {
                    val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                    if (virtualFile != null) {
                        logger.info("生成 ${file.name} 的文本报告（类型: ${profileType.displayName}）")
                        // 使用 TEXT 类型在 pprof Output 窗口显示
                        val visualizationService = project.service<PprofVisualizationService>()
                        visualizationService.visualize(virtualFile, VisualizationType.TEXT)
                    }
                } else {
                    logger.info("跳过 ${file.name}（类型: ${profileType?.displayName ?: "未知"}，未被选中）")
                }
            }
        }
    }
    
    /**
     * 根据文件名匹配分析类型
     */
    private fun matchProfileType(fileName: String): PprofProfileType? {
        return PprofProfileType.entries.find { type ->
            fileName.contains(type.fileName.substringBefore("."), ignoreCase = true)
        }
    }
    
    /**
     * 清除 pprof Output 窗口的数据
     */
    private fun clearPprofOutput() {
        val logger = thisLogger()
        ApplicationManager.getApplication().invokeLater {
            try {
                val project = environment.project
                val outputPanel = com.github.anniext.pprofview.toolWindow.PprofOutputPanel.getInstance(project)
                if (outputPanel != null) {
                    outputPanel.clearAll()
                    logger.info("已清除 pprof Output 窗口的旧数据")
                } else {
                    logger.warn("无法获取 pprof Output 窗口实例")
                }
            } catch (e: Exception) {
                logger.error("清除 pprof Output 窗口失败", e)
            }
        }
    }
}

