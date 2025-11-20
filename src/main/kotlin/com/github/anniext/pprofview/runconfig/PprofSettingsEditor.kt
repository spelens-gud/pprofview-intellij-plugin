package com.github.anniext.pprofview.runconfig

import com.goide.execution.GoRunConfigurationBase
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*

/**
 * pprof 配置编辑器
 */
class PprofSettingsEditor : SettingsEditor<RunConfigurationBase<*>>() {
    
    private val panel: JPanel
    
    // 基本配置
    private val enablePprofCheckBox = JBCheckBox("启用 pprof 性能分析")
    private val collectionModeComboBox = ComboBox(PprofCollectionMode.entries.toTypedArray())
    
    // 性能分析类型
    private val cpuCheckBox = JBCheckBox("CPU 分析")
    private val heapCheckBox = JBCheckBox("堆内存分析")
    private val goroutineCheckBox = JBCheckBox("协程分析")
    private val threadCreateCheckBox = JBCheckBox("线程创建分析")
    private val blockCheckBox = JBCheckBox("阻塞分析")
    private val mutexCheckBox = JBCheckBox("互斥锁分析")
    private val allocsCheckBox = JBCheckBox("内存分配分析")
    private val traceCheckBox = JBCheckBox("执行追踪")
    
    // 输出配置
    private val outputDirectoryField = TextFieldWithBrowseButton()
    private val autoOpenResultCheckBox = JBCheckBox("程序结束后自动打开分析结果", true)
    
    // 采样配置
    private val cpuDurationField = JBTextField("30")
    private val httpPortField = JBTextField("6060")
    private val memProfileRateField = JBTextField("524288")
    private val mutexProfileFractionField = JBTextField("1")
    private val blockProfileRateField = JBTextField("1")
    
    // 编译配置
    private val customBuildFlagsField = JBTextField()
    
    // 高级配置面板
    private val advancedPanel: JPanel

    init {
        // 设置 ComboBox 渲染器
        collectionModeComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): java.awt.Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is PprofCollectionMode) {
                    text = value.displayName
                }
                return component
            }
        }
        
        // 配置输出目录选择器
        outputDirectoryField.addBrowseFolderListener(
            "选择输出目录",
            "选择 pprof 文件输出目录",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        
        // 创建性能分析类型面板
        val profileTypesPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("性能分析类型")
            add(cpuCheckBox)
            add(heapCheckBox)
            add(goroutineCheckBox)
            add(threadCreateCheckBox)
            add(blockCheckBox)
            add(mutexCheckBox)
            add(allocsCheckBox)
            add(traceCheckBox)
        }
        
        // 创建高级配置面板
        advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("CPU 采样持续时间（秒）:", cpuDurationField)
            .addLabeledComponent("HTTP 服务器端口:", httpPortField)
            .addLabeledComponent("内存采样率（字节）:", memProfileRateField)
            .addLabeledComponent("互斥锁采样率:", mutexProfileFractionField)
            .addLabeledComponent("阻塞采样率:", blockProfileRateField)
            .addLabeledComponent("自定义编译参数:", customBuildFlagsField)
            .panel
        
        advancedPanel.border = BorderFactory.createTitledBorder("高级配置")
        
        // 创建主面板
        panel = FormBuilder.createFormBuilder()
            .addComponent(enablePprofCheckBox)
            .addLabeledComponent("采集模式:", collectionModeComboBox)
            .addComponent(profileTypesPanel)
            .addLabeledComponent("输出目录:", outputDirectoryField)
            .addComponent(autoOpenResultCheckBox)
            .addComponent(advancedPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        panel.border = JBUI.Borders.empty(10)
        
        // 添加监听器
        enablePprofCheckBox.addActionListener {
            updateEnabledState()
        }
        
        collectionModeComboBox.addActionListener {
            updateEnabledState()
        }
        
        updateEnabledState()
    }

    override fun createEditor(): JComponent = panel

    override fun resetEditorFrom(configuration: RunConfigurationBase<*>) {
        val options = configuration.getUserData(PprofRunConfigurationExtension.OPTIONS_KEY)
            ?: PprofRunConfigurationOptions()
        
        enablePprofCheckBox.isSelected = options.isEnablePprof
        
        val mode = PprofCollectionMode.fromString(options.collectionModeValue)
        collectionModeComboBox.selectedItem = mode
        
        // 重置性能分析类型
        val selectedTypes = options.profileTypesList.toSet()
        cpuCheckBox.isSelected = PprofProfileType.CPU.name in selectedTypes
        heapCheckBox.isSelected = PprofProfileType.HEAP.name in selectedTypes
        goroutineCheckBox.isSelected = PprofProfileType.GOROUTINE.name in selectedTypes
        threadCreateCheckBox.isSelected = PprofProfileType.THREAD_CREATE.name in selectedTypes
        blockCheckBox.isSelected = PprofProfileType.BLOCK.name in selectedTypes
        mutexCheckBox.isSelected = PprofProfileType.MUTEX.name in selectedTypes
        allocsCheckBox.isSelected = PprofProfileType.ALLOCS.name in selectedTypes
        traceCheckBox.isSelected = PprofProfileType.TRACE.name in selectedTypes
        
        outputDirectoryField.text = options.outputDirectoryPath
        autoOpenResultCheckBox.isSelected = options.isAutoOpenResult
        
        cpuDurationField.text = options.cpuDurationSeconds.toString()
        httpPortField.text = options.httpPortNumber.toString()
        memProfileRateField.text = options.memProfileRateValue.toString()
        mutexProfileFractionField.text = options.mutexProfileFractionValue.toString()
        blockProfileRateField.text = options.blockProfileRateValue.toString()
        
        customBuildFlagsField.text = options.customBuildFlagsValue
        
        updateEnabledState()
    }

    override fun applyEditorTo(configuration: RunConfigurationBase<*>) {
        val options = configuration.getUserData(PprofRunConfigurationExtension.OPTIONS_KEY)
            ?: PprofRunConfigurationOptions().also {
                configuration.putUserData(PprofRunConfigurationExtension.OPTIONS_KEY, it)
            }
        
        options.isEnablePprof = enablePprofCheckBox.isSelected
        options.collectionModeValue = (collectionModeComboBox.selectedItem as? PprofCollectionMode)?.name
            ?: PprofCollectionMode.NONE.name
        
        // 收集选中的性能分析类型
        val selectedTypes = mutableListOf<String>()
        if (cpuCheckBox.isSelected) selectedTypes.add(PprofProfileType.CPU.name)
        if (heapCheckBox.isSelected) selectedTypes.add(PprofProfileType.HEAP.name)
        if (goroutineCheckBox.isSelected) selectedTypes.add(PprofProfileType.GOROUTINE.name)
        if (threadCreateCheckBox.isSelected) selectedTypes.add(PprofProfileType.THREAD_CREATE.name)
        if (blockCheckBox.isSelected) selectedTypes.add(PprofProfileType.BLOCK.name)
        if (mutexCheckBox.isSelected) selectedTypes.add(PprofProfileType.MUTEX.name)
        if (allocsCheckBox.isSelected) selectedTypes.add(PprofProfileType.ALLOCS.name)
        if (traceCheckBox.isSelected) selectedTypes.add(PprofProfileType.TRACE.name)
        options.profileTypesList = selectedTypes
        
        options.outputDirectoryPath = outputDirectoryField.text
        options.isAutoOpenResult = autoOpenResultCheckBox.isSelected
        
        options.cpuDurationSeconds = cpuDurationField.text.toIntOrNull() ?: 30
        options.httpPortNumber = httpPortField.text.toIntOrNull() ?: 6060
        options.memProfileRateValue = memProfileRateField.text.toIntOrNull() ?: 524288
        options.mutexProfileFractionValue = mutexProfileFractionField.text.toIntOrNull() ?: 1
        options.blockProfileRateValue = blockProfileRateField.text.toIntOrNull() ?: 1
        
        options.customBuildFlagsValue = customBuildFlagsField.text
    }

    /**
     * 根据启用状态更新控件可用性
     */
    private fun updateEnabledState() {
        val enabled = enablePprofCheckBox.isSelected
        val mode = collectionModeComboBox.selectedItem as? PprofCollectionMode
        
        collectionModeComboBox.isEnabled = enabled
        
        cpuCheckBox.isEnabled = enabled
        heapCheckBox.isEnabled = enabled
        goroutineCheckBox.isEnabled = enabled
        threadCreateCheckBox.isEnabled = enabled
        blockCheckBox.isEnabled = enabled
        mutexCheckBox.isEnabled = enabled
        allocsCheckBox.isEnabled = enabled
        traceCheckBox.isEnabled = enabled
        
        outputDirectoryField.isEnabled = enabled
        autoOpenResultCheckBox.isEnabled = enabled
        
        // 根据采集模式显示/隐藏相关配置
        val showCpuDuration = enabled && mode == PprofCollectionMode.RUNTIME_SAMPLING
        val showHttpPort = enabled && mode == PprofCollectionMode.HTTP_SERVER
        val showBuildFlags = enabled && mode == PprofCollectionMode.COMPILE_TIME_INSTRUMENTATION
        
        cpuDurationField.isEnabled = showCpuDuration
        httpPortField.isEnabled = showHttpPort
        customBuildFlagsField.isEnabled = showBuildFlags
        
        memProfileRateField.isEnabled = enabled
        mutexProfileFractionField.isEnabled = enabled
        blockProfileRateField.isEnabled = enabled
    }
}
