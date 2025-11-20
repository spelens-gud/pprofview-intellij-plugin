package com.github.anniext.pprofview.runconfig

import com.goide.execution.GoRunConfigurationBase
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import org.jdom.Element
import java.io.File

/**
 * pprof 运行配置扩展
 * 
 * 为 Go 运行配置添加 pprof 性能分析支持
 */
class PprofRunConfigurationExtension : com.intellij.execution.configuration.RunConfigurationExtensionBase<RunConfigurationBase<*>>() {
    
    private val logger = thisLogger()
    
    init {
        logger.warn("=== PprofRunConfigurationExtension initialized ===")
    }
    
    companion object {
        private const val EXTENSION_ID = "com.github.anniext.pprofview.PprofRunConfigurationExtension"
        val OPTIONS_KEY = Key.create<PprofRunConfigurationOptions>("PPROF_OPTIONS")
        
        init {
            println("=== PprofRunConfigurationExtension companion object loaded ===")
            com.intellij.openapi.diagnostic.Logger.getInstance(PprofRunConfigurationExtension::class.java)
                .warn("=== PprofRunConfigurationExtension class loaded ===")
        }
        private const val ENABLE_PPROF_ATTR = "enablePprof"
        private const val COLLECTION_MODE_ATTR = "collectionMode"
        private const val PROFILE_TYPES_ATTR = "profileTypes"
        private const val OUTPUT_DIR_ATTR = "outputDirectory"
        private const val CPU_DURATION_ATTR = "cpuDuration"
        private const val HTTP_PORT_ATTR = "httpPort"
        private const val AUTO_OPEN_ATTR = "autoOpenResult"
        private const val CUSTOM_BUILD_FLAGS_ATTR = "customBuildFlags"
        private const val MEM_PROFILE_RATE_ATTR = "memProfileRate"
        private const val ENABLE_MUTEX_ATTR = "enableMutexProfile"
        private const val MUTEX_FRACTION_ATTR = "mutexProfileFraction"
        private const val ENABLE_BLOCK_ATTR = "enableBlockProfile"
        private const val BLOCK_RATE_ATTR = "blockProfileRate"
    }

    override fun getSerializationId(): String = EXTENSION_ID

    override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean {
        val isApplicable = configuration is GoRunConfigurationBase<*>
        val className = configuration.javaClass.name
        logger.warn("=== isApplicableFor called: $className -> $isApplicable ===")
        return isApplicable
    }

    override fun isEnabledFor(
        applicableConfiguration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?
    ): Boolean {
        // 始终返回 true 以显示配置选项，实际的启用状态由用户在 UI 中控制
        logger.warn("=== isEnabledFor called -> true ===")
        return true
    }

    override fun <T : RunConfigurationBase<*>> createEditor(configuration: T): SettingsEditor<T>? {
        logger.warn("=== createEditor called for: ${configuration.javaClass.simpleName} ===")
        if (!isApplicableFor(configuration)) {
            return null
        }
        logger.warn("=== Creating PprofSettingsEditor ===")
        @Suppress("UNCHECKED_CAST")
        return PprofSettingsEditor() as SettingsEditor<T>
    }

    override fun readExternal(runConfiguration: RunConfigurationBase<*>, element: Element) {
        super.readExternal(runConfiguration, element)
        
        val options = runConfiguration.getUserData(OPTIONS_KEY) ?: PprofRunConfigurationOptions().also {
            runConfiguration.putUserData(OPTIONS_KEY, it)
        }
        
        options.isEnablePprof = element.getAttributeValue(ENABLE_PPROF_ATTR)?.toBoolean() ?: false
        options.collectionModeValue = element.getAttributeValue(COLLECTION_MODE_ATTR) 
            ?: PprofCollectionMode.NONE.name
        
        val profileTypesStr = element.getAttributeValue(PROFILE_TYPES_ATTR) ?: ""
        options.profileTypesList = if (profileTypesStr.isNotEmpty()) {
            profileTypesStr.split(",").toMutableList()
        } else {
            mutableListOf()
        }
        
        options.outputDirectoryPath = element.getAttributeValue(OUTPUT_DIR_ATTR) ?: ""
        options.cpuDurationSeconds = element.getAttributeValue(CPU_DURATION_ATTR)?.toIntOrNull() ?: 30
        options.httpPortNumber = element.getAttributeValue(HTTP_PORT_ATTR)?.toIntOrNull() ?: 6060
        options.isAutoOpenResult = element.getAttributeValue(AUTO_OPEN_ATTR)?.toBoolean() ?: true
        options.customBuildFlagsValue = element.getAttributeValue(CUSTOM_BUILD_FLAGS_ATTR) ?: ""
        options.memProfileRateValue = element.getAttributeValue(MEM_PROFILE_RATE_ATTR)?.toIntOrNull() ?: 524288
        options.isEnableMutexProfile = element.getAttributeValue(ENABLE_MUTEX_ATTR)?.toBoolean() ?: false
        options.mutexProfileFractionValue = element.getAttributeValue(MUTEX_FRACTION_ATTR)?.toIntOrNull() ?: 1
        options.isEnableBlockProfile = element.getAttributeValue(ENABLE_BLOCK_ATTR)?.toBoolean() ?: false
        options.blockProfileRateValue = element.getAttributeValue(BLOCK_RATE_ATTR)?.toIntOrNull() ?: 1
    }

    override fun writeExternal(runConfiguration: RunConfigurationBase<*>, element: Element) {
        super.writeExternal(runConfiguration, element)
        
        val options = runConfiguration.getUserData(OPTIONS_KEY) ?: return
        
        element.setAttribute(ENABLE_PPROF_ATTR, options.isEnablePprof.toString())
        element.setAttribute(COLLECTION_MODE_ATTR, options.collectionModeValue)
        element.setAttribute(PROFILE_TYPES_ATTR, options.profileTypesList.joinToString(","))
        element.setAttribute(OUTPUT_DIR_ATTR, options.outputDirectoryPath)
        element.setAttribute(CPU_DURATION_ATTR, options.cpuDurationSeconds.toString())
        element.setAttribute(HTTP_PORT_ATTR, options.httpPortNumber.toString())
        element.setAttribute(AUTO_OPEN_ATTR, options.isAutoOpenResult.toString())
        element.setAttribute(CUSTOM_BUILD_FLAGS_ATTR, options.customBuildFlagsValue)
        element.setAttribute(MEM_PROFILE_RATE_ATTR, options.memProfileRateValue.toString())
        element.setAttribute(ENABLE_MUTEX_ATTR, options.isEnableMutexProfile.toString())
        element.setAttribute(MUTEX_FRACTION_ATTR, options.mutexProfileFractionValue.toString())
        element.setAttribute(ENABLE_BLOCK_ATTR, options.isEnableBlockProfile.toString())
        element.setAttribute(BLOCK_RATE_ATTR, options.blockProfileRateValue.toString())
    }

    override fun patchCommandLine(
        configuration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String
    ) {
        patchCommandLineInternal(configuration, cmdLine)
    }

    override fun patchCommandLine(
        configuration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String,
        executor: Executor
    ) {
        patchCommandLineInternal(configuration, cmdLine)
    }

    private fun patchCommandLineInternal(
        configuration: RunConfigurationBase<*>,
        cmdLine: GeneralCommandLine
    ) {
        val options = configuration.getUserData(OPTIONS_KEY) ?: return
        
        if (!options.isEnablePprof) {
            return
        }

        try {
            when (PprofCollectionMode.fromString(options.collectionModeValue)) {
                PprofCollectionMode.COMPILE_TIME_INSTRUMENTATION -> {
                    applyCompileTimeInstrumentation(cmdLine, options)
                }
                PprofCollectionMode.RUNTIME_SAMPLING -> {
                    applyRuntimeSampling(cmdLine, options)
                }
                PprofCollectionMode.HTTP_SERVER -> {
                    applyHttpServer(cmdLine, options)
                }
                PprofCollectionMode.MANUAL_COLLECTION -> {
                    // 手动采集模式不需要修改命令行
                    logger.info("使用手动采集模式，请在代码中调用 pprof API")
                }
                PprofCollectionMode.NONE -> {
                    // 不做任何处理
                }
            }
        } catch (e: Exception) {
            logger.error("配置 pprof 失败", e)
        }
    }

    /**
     * 应用编译时插桩配置
     */
    private fun applyCompileTimeInstrumentation(cmdLine: GeneralCommandLine, options: PprofRunConfigurationOptions) {
        val buildFlags = mutableListOf<String>()
        
        // 添加自定义编译参数
        if (options.customBuildFlagsValue.isNotEmpty()) {
            buildFlags.addAll(options.customBuildFlagsValue.split(" "))
        }
        
        // 添加到环境变量
        if (buildFlags.isNotEmpty()) {
            val existingFlags = cmdLine.environment["GOFLAGS"] ?: ""
            cmdLine.environment["GOFLAGS"] = "$existingFlags ${buildFlags.joinToString(" ")}".trim()
        }
        
        logger.info("应用编译时插桩: ${buildFlags.joinToString(" ")}")
    }

    /**
     * 应用运行时采样配置
     */
    private fun applyRuntimeSampling(cmdLine: GeneralCommandLine, options: PprofRunConfigurationOptions) {
        val outputDir = getOutputDirectory(options)
        
        // 设置环境变量
        if (options.memProfileRateValue > 0) {
            cmdLine.environment["GOMEMPROFILERATE"] = options.memProfileRateValue.toString()
        }
        
        if (options.isEnableMutexProfile) {
            cmdLine.environment["GOMUTEXPROFILEFRACTION"] = options.mutexProfileFractionValue.toString()
        }
        
        if (options.isEnableBlockProfile) {
            cmdLine.environment["GOBLOCKPROFILERATE"] = options.blockProfileRateValue.toString()
        }
        
        // 设置输出路径
        options.profileTypesList.forEach { typeStr ->
            val type = PprofProfileType.fromString(typeStr)
            if (type != null) {
                val outputFile = File(outputDir, type.fileName)
                when (type) {
                    PprofProfileType.CPU -> {
                        cmdLine.environment["CPUPROFILE"] = outputFile.absolutePath
                        cmdLine.environment["CPUPROFILE_DURATION"] = options.cpuDurationSeconds.toString()
                    }
                    PprofProfileType.HEAP -> {
                        cmdLine.environment["MEMPROFILE"] = outputFile.absolutePath
                    }
                    PprofProfileType.MUTEX -> {
                        cmdLine.environment["MUTEXPROFILE"] = outputFile.absolutePath
                    }
                    PprofProfileType.BLOCK -> {
                        cmdLine.environment["BLOCKPROFILE"] = outputFile.absolutePath
                    }
                    else -> {
                        // 其他类型需要在代码中手动处理
                    }
                }
            }
        }
        
        logger.info("应用运行时采样配置，输出目录: $outputDir")
    }

    /**
     * 应用 HTTP 服务器配置
     */
    private fun applyHttpServer(cmdLine: GeneralCommandLine, options: PprofRunConfigurationOptions) {
        cmdLine.environment["PPROF_HTTP_PORT"] = options.httpPortNumber.toString()
        logger.info("启用 pprof HTTP 服务器，端口: ${options.httpPortNumber}")
    }

    /**
     * 获取输出目录
     */
    private fun getOutputDirectory(options: PprofRunConfigurationOptions): File {
        val dirPath = if (options.outputDirectoryPath.isNotEmpty()) {
            options.outputDirectoryPath
        } else {
            FileUtil.getTempDirectory()
        }
        
        val dir = File(dirPath)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        
        return dir
    }

    override fun attachToProcess(
        configuration: RunConfigurationBase<*>,
        handler: ProcessHandler,
        runnerSettings: RunnerSettings?
    ) {
        val options = configuration.getUserData(OPTIONS_KEY) ?: return
        
        if (!options.isEnablePprof) {
            return
        }
        
        // 可以在这里添加进程监听器，在程序结束时自动打开分析结果
        if (options.isAutoOpenResult) {
            handler.addProcessListener(PprofProcessListener(getOutputDirectory(options), options.profileTypesList))
        }
    }
}
