package com.github.anniext.pprofview.runconfig

import com.intellij.execution.configurations.RunConfigurationOptions

/**
 * pprof 运行配置选项
 */
class PprofRunConfigurationOptions : RunConfigurationOptions() {
    
    /**
     * 是否启用 pprof
     */
    private val enablePprof = property(false)
    
    /**
     * 采集模式
     */
    private val collectionMode = string(PprofCollectionMode.NONE.name)
    
    /**
     * 性能分析类型（多选）
     */
    private val profileTypes = list<String>()
    
    /**
     * 输出目录
     */
    private val outputDirectory = string("")
    
    /**
     * CPU 采样持续时间（秒）
     */
    private val cpuDuration = property(30)
    
    /**
     * HTTP 服务器端口
     */
    private val httpPort = property(6060)
    
    /**
     * 是否在程序结束时自动打开分析结果
     */
    private val autoOpenResult = property(true)
    
    /**
     * 自定义编译参数
     */
    private val customBuildFlags = string("")
    
    /**
     * 内存采样率（每分配多少字节采样一次）
     */
    private val memProfileRate = property(524288) // 默认 512KB
    
    /**
     * 是否启用互斥锁分析
     */
    private val enableMutexProfile = property(false)
    
    /**
     * 互斥锁采样率
     */
    private val mutexProfileFraction = property(1)
    
    /**
     * 是否启用阻塞分析
     */
    private val enableBlockProfile = property(false)
    
    /**
     * 阻塞采样率
     */
    private val blockProfileRate = property(1)

    // Getters and Setters
    
    var isEnablePprof: Boolean
        get() = enablePprof.getValue(this)
        set(value) = enablePprof.setValue(this, value)
    
    var collectionModeValue: String
        get() = collectionMode.getValue(this) ?: PprofCollectionMode.NONE.name
        set(value) = collectionMode.setValue(this, value)
    
    var profileTypesList: MutableList<String>
        get() = profileTypes.getValue(this) ?: mutableListOf()
        set(value) = profileTypes.setValue(this, value)
    
    var outputDirectoryPath: String
        get() = outputDirectory.getValue(this) ?: ""
        set(value) = outputDirectory.setValue(this, value)
    
    var cpuDurationSeconds: Int
        get() = cpuDuration.getValue(this)
        set(value) = cpuDuration.setValue(this, value)
    
    var httpPortNumber: Int
        get() = httpPort.getValue(this)
        set(value) = httpPort.setValue(this, value)
    
    var isAutoOpenResult: Boolean
        get() = autoOpenResult.getValue(this)
        set(value) = autoOpenResult.setValue(this, value)
    
    var customBuildFlagsValue: String
        get() = customBuildFlags.getValue(this) ?: ""
        set(value) = customBuildFlags.setValue(this, value)
    
    var memProfileRateValue: Int
        get() = memProfileRate.getValue(this)
        set(value) = memProfileRate.setValue(this, value)
    
    var isEnableMutexProfile: Boolean
        get() = enableMutexProfile.getValue(this)
        set(value) = enableMutexProfile.setValue(this, value)
    
    var mutexProfileFractionValue: Int
        get() = mutexProfileFraction.getValue(this)
        set(value) = mutexProfileFraction.setValue(this, value)
    
    var isEnableBlockProfile: Boolean
        get() = enableBlockProfile.getValue(this)
        set(value) = enableBlockProfile.setValue(this, value)
    
    var blockProfileRateValue: Int
        get() = blockProfileRate.getValue(this)
        set(value) = blockProfileRate.setValue(this, value)
}
