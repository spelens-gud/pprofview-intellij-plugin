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
    private val samplingModeComboBox = ComboBox(PprofSamplingMode.entries.toTypedArray())
    private val samplingIntervalField = JBTextField("60")
    private val testPatternField = JBTextField().apply {
        toolTipText = "测试函数正则表达式，例如：^\\QTestIndexHandler\\E$ 表示运行 TestIndexHandler 测试函数"
    }
    
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
    
    // Go 程序配置
    private val runKindComboBox = ComboBox(PprofRunKind.entries.toTypedArray())
    private val fileField = TextFieldWithBrowseButton()
    private val directoryField = TextFieldWithBrowseButton()
    private val packageField = ComboBox<String>()
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val programArgumentsField = JBTextField()
    private val environmentVariablesField = JBTextField()
    private val goBuildFlagsField = JBTextField()
    
    // 保存配置引用，用于智能填充
    private var currentConfiguration: PprofConfiguration? = null

    init {
        // 设置运行种类 ComboBox 渲染器
        runKindComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): java.awt.Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is PprofRunKind) {
                    text = value.displayName
                }
                return component
            }
        }
        
        // 设置软件包 ComboBox 为可编辑
        packageField.isEditable = true
        
        // 配置文件选择器
        val singleFileDescriptor = com.intellij.openapi.fileChooser.FileChooserDescriptor(
            true, false, false, false, false, false
        ).withTitle("选择 Go 文件")
        
        fileField.addActionListener {
            val chooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
                singleFileDescriptor,
                null,
                null
            )
            chooser?.let { fileField.text = it.path }
        }
        
        val singleFolderDescriptor = com.intellij.openapi.fileChooser.FileChooserDescriptor(
            false, true, false, false, false, false
        )
        
        directoryField.addActionListener {
            val chooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
                singleFolderDescriptor.withTitle("选择目录"),
                null,
                null
            )
            chooser?.let { directoryField.text = it.path }
        }
        
        workingDirectoryField.addActionListener {
            val chooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
                singleFolderDescriptor.withTitle("选择工作目录"),
                null,
                null
            )
            chooser?.let { workingDirectoryField.text = it.path }
        }
        
        outputDirectoryField.addActionListener {
            val chooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
                singleFolderDescriptor.withTitle("选择输出目录"),
                null,
                null
            )
            chooser?.let { outputDirectoryField.text = it.path }
        }
        
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
        
        samplingModeComboBox.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): java.awt.Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is PprofSamplingMode) {
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
            .panel
        
        advancedPanel.border = BorderFactory.createTitledBorder("高级配置")
        
        // 创建运行配置面板
        val runConfigPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("运行配置")
        }
        
        // 添加运行种类选择和对应的输入框
        val runKindPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("运行种类:", runKindComboBox)
            .addLabeledComponent("文件:", fileField)
            .addLabeledComponent("目录:", directoryField)
            .addLabeledComponent("软件包:", packageField)
            .panel
        
        runConfigPanel.add(runKindPanel)
        
        // 创建主面板
        val panel = FormBuilder.createFormBuilder()
            .addComponent(runConfigPanel)
            .addLabeledComponent("工作目录:", workingDirectoryField)
            .addLabeledComponent("程序参数:", programArgumentsField)
            .addLabeledComponent("环境变量:", environmentVariablesField)
            .addLabeledComponent("Go 构建标志:", goBuildFlagsField)
            .addSeparator()
            .addComponent(enablePprofCheckBox)
            .addLabeledComponent("采集模式:", collectionModeComboBox)
            .addLabeledComponent("采样模式:", samplingModeComboBox)
            .addLabeledComponent("采样间隔（秒）:", samplingIntervalField)
            .addLabeledComponent("测试模式选项:", testPatternField)
            .addComponent(profileTypesPanel)
            .addLabeledComponent("输出目录:", outputDirectoryField)
            .addComponent(autoOpenResultCheckBox)
            .addComponent(advancedPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        panel.border = JBUI.Borders.empty(10)
        
        // 添加运行种类变化监听器
        runKindComboBox.addActionListener {
            onRunKindChanged()
        }
        
        // 添加采集模式变化监听器
        collectionModeComboBox.addActionListener {
            onCollectionModeChanged()
        }
        
        // 添加工作目录变化监听器
        workingDirectoryField.textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onWorkingDirectoryChanged()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onWorkingDirectoryChanged()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onWorkingDirectoryChanged()
        })
        
        return panel
    }

    override fun resetEditorFrom(configuration: PprofConfiguration) {
        // 保存配置引用
        currentConfiguration = configuration
        
        // 运行配置
        val runKind = PprofRunKind.fromString(configuration.runKind)
        runKindComboBox.selectedItem = runKind
        
        fileField.text = configuration.filePath
        directoryField.text = configuration.directoryPath
        packageField.selectedItem = configuration.packagePath
        workingDirectoryField.text = configuration.workingDirectory
        programArgumentsField.text = configuration.programArguments
        environmentVariablesField.text = configuration.environmentVariables
        goBuildFlagsField.text = configuration.goBuildFlags
        
        // 初始化智能选项
        initializeSmartOptions(configuration)
        
        enablePprofCheckBox.isSelected = configuration.enablePprof
        
        val mode = PprofCollectionMode.fromString(configuration.collectionMode)
        collectionModeComboBox.selectedItem = mode
        
        val samplingMode = PprofSamplingMode.fromString(configuration.samplingMode)
        samplingModeComboBox.selectedItem = samplingMode
        samplingIntervalField.text = configuration.samplingInterval.toString()
        testPatternField.text = configuration.testPattern
        
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
        
        // 更新字段可用性
        onCollectionModeChanged()
        updateRunKindFields()
    }

    override fun applyEditorTo(configuration: PprofConfiguration) {
        // 运行配置
        configuration.runKind = (runKindComboBox.selectedItem as? PprofRunKind)?.name ?: PprofRunKind.FILE.name
        configuration.filePath = fileField.text
        configuration.directoryPath = directoryField.text
        configuration.packagePath = packageField.selectedItem?.toString() ?: packageField.editor.item?.toString() ?: ""
        configuration.workingDirectory = workingDirectoryField.text
        configuration.programArguments = programArgumentsField.text
        configuration.environmentVariables = environmentVariablesField.text
        configuration.goBuildFlags = goBuildFlagsField.text
        
        // Pprof 配置
        configuration.enablePprof = enablePprofCheckBox.isSelected
        configuration.collectionMode = (collectionModeComboBox.selectedItem as? PprofCollectionMode)?.name
            ?: PprofCollectionMode.RUNTIME_SAMPLING.name
        configuration.samplingMode = (samplingModeComboBox.selectedItem as? PprofSamplingMode)?.name
            ?: PprofSamplingMode.SINGLE.name
        configuration.samplingInterval = samplingIntervalField.text.toIntOrNull() ?: 60
        configuration.testPattern = testPatternField.text
        
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
    }
    
    /**
     * 初始化智能选项
     */
    private fun initializeSmartOptions(configuration: PprofConfiguration) {
        val workingDir = configuration.workingDirectory.ifEmpty { 
            configuration.project?.basePath ?: ""
        }
        
        if (workingDir.isNotEmpty()) {
            // 自动填充工作目录
            if (workingDirectoryField.text.isEmpty()) {
                workingDirectoryField.text = workingDir
            }
            
            // 根据运行种类填充默认值
            updateSmartDefaults(workingDir)
        }
    }
    
    /**
     * 运行种类变化时的响应
     */
    private fun onRunKindChanged() {
        updateRunKindFields()
        
        val workingDir = workingDirectoryField.text
        if (workingDir.isNotEmpty()) {
            updateSmartDefaults(workingDir)
        }
    }
    
    /**
     * 采集模式变化时的响应
     */
    private fun onCollectionModeChanged() {
        val collectionMode = collectionModeComboBox.selectedItem as? PprofCollectionMode
        val isTestMode = collectionMode == PprofCollectionMode.TEST_SAMPLING
        val isHttpMode = collectionMode == PprofCollectionMode.HTTP_SERVER
        
        // 测试模式选项只在测试时采样模式下可用
        testPatternField.isEnabled = isTestMode
        
        // 采样模式和采样间隔在测试模式下不可用（测试模式使用 go test 的参数）
        samplingModeComboBox.isEnabled = !isTestMode && !isHttpMode
        samplingIntervalField.isEnabled = !isTestMode && !isHttpMode
        
        // HTTP 端口只在 HTTP 服务模式下可用
        httpPortField.isEnabled = isHttpMode
        
        // 性能分析类型的可用性
        if (isTestMode) {
            // 测试模式：go test 只支持以下参数
            cpuCheckBox.isEnabled = true          // -cpuprofile
            heapCheckBox.isEnabled = true         // -memprofile
            blockCheckBox.isEnabled = true        // -blockprofile
            mutexCheckBox.isEnabled = true        // -mutexprofile
            
            // go test 不支持的类型
            goroutineCheckBox.isEnabled = false
            threadCreateCheckBox.isEnabled = false
            allocsCheckBox.isEnabled = false
            traceCheckBox.isEnabled = false
            
            // 取消勾选不支持的类型
            goroutineCheckBox.isSelected = false
            threadCreateCheckBox.isSelected = false
            allocsCheckBox.isSelected = false
            traceCheckBox.isSelected = false
        } else {
            // 运行时采样和 HTTP 服务模式：所有类型都可用
            cpuCheckBox.isEnabled = true
            heapCheckBox.isEnabled = true
            goroutineCheckBox.isEnabled = true
            threadCreateCheckBox.isEnabled = true
            blockCheckBox.isEnabled = true
            mutexCheckBox.isEnabled = true
            allocsCheckBox.isEnabled = true
            traceCheckBox.isEnabled = true
        }
    }
    
    /**
     * 工作目录变化时的响应
     */
    private fun onWorkingDirectoryChanged() {
        val workingDir = workingDirectoryField.text
        if (workingDir.isNotEmpty()) {
            updateSmartDefaults(workingDir)
        }
    }
    
    /**
     * 更新智能默认值
     */
    private fun updateSmartDefaults(workingDir: String) {
        val runKind = runKindComboBox.selectedItem as? PprofRunKind ?: PprofRunKind.FILE
        val workingDirFile = java.io.File(workingDir)
        
        if (!workingDirFile.exists() || !workingDirFile.isDirectory) {
            return
        }
        
        when (runKind) {
            PprofRunKind.FILE -> {
                // 自动查找 main.go 文件
                if (fileField.text.isEmpty()) {
                    val mainFile = findMainGoFile(workingDirFile)
                    if (mainFile != null) {
                        fileField.text = mainFile.absolutePath
                    }
                }
            }
            PprofRunKind.DIRECTORY -> {
                // 自动设置为工作目录
                if (directoryField.text.isEmpty()) {
                    directoryField.text = workingDir
                }
            }
            PprofRunKind.PACKAGE -> {
                // 自动读取 go.mod 获取包列表
                updatePackageList(workingDirFile)
            }
        }
    }
    
    /**
     * 查找 main.go 文件
     */
    private fun findMainGoFile(directory: java.io.File): java.io.File? {
        // 首先查找 main.go
        val mainGo = java.io.File(directory, "main.go")
        if (mainGo.exists() && mainGo.isFile) {
            return mainGo
        }
        
        // 查找任何包含 main 函数的 .go 文件
        val goFiles = directory.listFiles { file ->
            file.isFile && file.name.endsWith(".go") && !file.name.endsWith("_test.go")
        } ?: return null
        
        for (file in goFiles) {
            try {
                val content = file.readText()
                if (content.contains("func main()") && content.contains("package main")) {
                    return file
                }
            } catch (e: Exception) {
                // 忽略读取错误
            }
        }
        
        return null
    }
    
    /**
     * 更新软件包列表
     */
    private fun updatePackageList(directory: java.io.File) {
        val packages = mutableListOf<String>()
        
        // 读取 go.mod 获取模块路径
        val goModFile = java.io.File(directory, "go.mod")
        if (goModFile.exists()) {
            try {
                val content = goModFile.readText()
                val modulePattern = Regex("""module\s+([^\s]+)""")
                val match = modulePattern.find(content)
                if (match != null) {
                    val modulePath = match.groupValues[1]
                    packages.add(modulePath)
                    
                    // 查找子包
                    findSubPackages(directory, modulePath, packages)
                }
            } catch (e: Exception) {
                // 忽略读取错误
            }
        }
        
        // 更新下拉列表
        packageField.removeAllItems()
        packages.forEach { packageField.addItem(it) }
        
        // 如果当前值不在列表中，添加它
        val currentValue = currentConfiguration?.packagePath
        if (!currentValue.isNullOrEmpty() && !packages.contains(currentValue)) {
            packageField.addItem(currentValue)
            packageField.selectedItem = currentValue
        }
    }
    
    /**
     * 查找子包
     */
    private fun findSubPackages(directory: java.io.File, modulePath: String, packages: MutableList<String>) {
        val subdirs = directory.listFiles { file -> 
            file.isDirectory && !file.name.startsWith(".") && file.name != "vendor"
        } ?: return
        
        for (subdir in subdirs) {
            // 检查是否包含 .go 文件
            val hasGoFiles = subdir.listFiles { file ->
                file.isFile && file.name.endsWith(".go") && !file.name.endsWith("_test.go")
            }?.isNotEmpty() ?: false
            
            if (hasGoFiles) {
                val relativePath = subdir.relativeTo(directory).path.replace(java.io.File.separator, "/")
                packages.add("$modulePath/$relativePath")
            }
            
            // 递归查找子目录
            findSubPackages(subdir, modulePath, packages)
        }
    }
    
    /**
     * 根据运行种类更新输入框的可用性
     */
    private fun updateRunKindFields() {
        val runKind = runKindComboBox.selectedItem as? PprofRunKind ?: PprofRunKind.FILE
        
        when (runKind) {
            PprofRunKind.FILE -> {
                fileField.isEnabled = true
                directoryField.isEnabled = false
                packageField.isEnabled = false
            }
            PprofRunKind.DIRECTORY -> {
                fileField.isEnabled = false
                directoryField.isEnabled = true
                packageField.isEnabled = false
            }
            PprofRunKind.PACKAGE -> {
                fileField.isEnabled = false
                directoryField.isEnabled = false
                packageField.isEnabled = true
            }
        }
    }
}
