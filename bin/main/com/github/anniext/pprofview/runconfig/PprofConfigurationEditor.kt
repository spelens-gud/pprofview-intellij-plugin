package com.github.anniext.pprofview.runconfig

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.*

/**
 * Pprof 配置编辑器
 */
class PprofConfigurationEditor : SettingsEditor<PprofConfiguration>() {
    
    // 基本配置
    private val enablePprofCheckBox = JBCheckBox("启用 pprof 性能分析", true)
    private val collectionModeComboBox = ComboBox(PprofCollectionMode.entries.toTypedArray())
    
    // 性能分析类型
    private val cpuCheckBox = JBCheckBox("CPU 分析", true)
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
    
    // Go 程序配置
    private val goFileField = TextFieldWithBrowseButton()
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val programArgumentsField = JBTextField()
    private val environmentVariablesField = JBTextField()

    init {
        // 配置文件选择器
        goFileField.addBrowseFolderListener(
            "选择 Go 文件",
            "选择要分析的 Go 主文件",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        
        workingDirectoryField.addBrowseFolderListener(
            "选择工作目录",
            "选择程序运行的工作目录",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        
        outputDirectoryField.addBrowseFolderListener(
            "选择输出目录",
            "选择 pprof 文件输出目录",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        )
        
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
    }

    override fun createEditor(): JComponent {
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
        val advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("CPU 采样持续时间（秒）:", cpuDurationField)
            .addLabeledComponent("HTTP 服务器端口:", httpPortField)
            .addLabeledComponent("内存采样率（字节）:", memProfileRateField)
            .addLabeledComponent("互斥锁采样率:", mutexProfileFractionField)
            .addLabeledComponent("阻塞采样率:", blockProfileRateField)
            .addLabeledComponent("自定义编译参数:", customBuildFlagsField)
            .panel
        
        advancedPanel.border = BorderFactory.createTitledBorder("高级配置")
        
        // 创建主面板
        val panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Go 文件:", goFileField)
            .addLabeledComponent("工作目录:", workingDirectoryField)
            .addLabeledComponent("程序参数:", programArgumentsField)
            .addLabeledComponent("环境变量:", environmentVariablesField)
            .addSeparator()
            .addComponent(enablePprofCheckBox)
            .addLabeledComponent("采集模式:", collectionModeComboBox)
            .addComponent(profileTypesPanel)
            .addLabeledComponent("输出目录:", outputDirectoryField)
            .addComponent(autoOpenResultCheckBox)
            .addComponent(advancedPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        panel.border = JBUI.Borders.empty(10)
        
        return panel
    }

    override fun resetEditorFrom(configuration: PprofConfiguration) {
        goFileField.text = configuration.goFilePath
        workingDirectoryField.text = configuration.workingDirectory
        programArgumentsField.text = configuration.programArguments
        environmentVariablesField.text = configuration.environmentVariables
        
        enablePprofCheckBox.isSelected = configuration.enablePprof
        
        val mode = PprofCollectionMode.fromString(configuration.collectionMode)
        collectionModeComboBox.selectedItem = mode
        
        // 重置性能分析类型
        val selectedTypes = configuration.profileTypes.split(",").toSet()
        cpuCheckBox.isSelected = PprofProfileType.CPU.name in selectedTypes
        heapCheckBox.isSelected = PprofProfileType.HEAP.name in selectedTypes
        goroutineCheckBox.isSelected = PprofProfileType.GOROUTINE.name in selectedTypes
        threadCreateCheckBox.isSelected = PprofProfileType.THREAD_CREATE.name in selectedTypes
        blockCheckBox.isSelected = PprofProfileType.BLOCK.name in selectedTypes
        mutexCheckBox.isSelected = PprofProfileType.MUTEX.name in selectedTypes
        allocsCheckBox.isSelected = PprofProfileType.ALLOCS.name in selectedTypes
        traceCheckBox.isSelected = PprofProfileType.TRACE.name in selectedTypes
        
        outputDirectoryField.text = configuration.outputDirectory
        autoOpenResultCheckBox.isSelected = configuration.autoOpenResult
        
        cpuDurationField.text = configuration.cpuDuration.toString()
        httpPortField.text = configuration.httpPort.toString()
        memProfileRateField.text = configuration.memProfileRate.toString()
        mutexProfileFractionField.text = configuration.mutexProfileFraction.toString()
        blockProfileRateField.text = configuration.blockProfileRate.toString()
        
        customBuildFlagsField.text = configuration.customBuildFlags
    }

    override fun applyEditorTo(configuration: PprofConfiguration) {
        configuration.goFilePath = goFileField.text
        configuration.workingDirectory = workingDirectoryField.text
        configuration.programArguments = programArgumentsField.text
        configuration.environmentVariables = environmentVariablesField.text
        
        configuration.enablePprof = enablePprofCheckBox.isSelected
        configuration.collectionMode = (collectionModeComboBox.selectedItem as? PprofCollectionMode)?.name
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
        configuration.profileTypes = selectedTypes.joinToString(",")
        
        configuration.outputDirectory = outputDirectoryField.text
        configuration.autoOpenResult = autoOpenResultCheckBox.isSelected
        
        configuration.cpuDuration = cpuDurationField.text.toIntOrNull() ?: 30
        configuration.httpPort = httpPortField.text.toIntOrNull() ?: 6060
        configuration.memProfileRate = memProfileRateField.text.toIntOrNull() ?: 524288
        configuration.mutexProfileFraction = mutexProfileFractionField.text.toIntOrNull() ?: 1
        configuration.blockProfileRate = blockProfileRateField.text.toIntOrNull() ?: 1
        
        configuration.customBuildFlags = customBuildFlagsField.text
    }
}
