package com.github.spelens.pprofview.runconfig

import com.github.spelens.pprofview.PprofViewBundle
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
 * Pprof configuration editor
 */
class PprofConfigurationEditor : SettingsEditor<PprofConfiguration>() {
    
    // Basic configuration
    private val enablePprofCheckBox = JBCheckBox(PprofViewBundle.message("pprof.ui.enablePprof"), true)
    private val collectionModeComboBox = ComboBox(PprofCollectionMode.entries.toTypedArray())
    private val samplingModeComboBox = ComboBox(PprofSamplingMode.entries.toTypedArray())
    private val samplingIntervalField = JBTextField("60")
    private val testPatternField = JBTextField().apply {
        toolTipText = "Test function regex, e.g.: ^\\QTestIndexHandler\\E$ to run TestIndexHandler test function"
    }
    
    // Profile types
    private val cpuCheckBox = JBCheckBox(PprofProfileType.CPU.displayName, true)
    private val heapCheckBox = JBCheckBox(PprofProfileType.HEAP.displayName)
    private val goroutineCheckBox = JBCheckBox(PprofProfileType.GOROUTINE.displayName)
    private val threadCreateCheckBox = JBCheckBox(PprofProfileType.THREAD_CREATE.displayName)
    private val blockCheckBox = JBCheckBox(PprofProfileType.BLOCK.displayName)
    private val mutexCheckBox = JBCheckBox(PprofProfileType.MUTEX.displayName)
    private val allocsCheckBox = JBCheckBox(PprofProfileType.ALLOCS.displayName)
    private val traceCheckBox = JBCheckBox(PprofProfileType.TRACE.displayName)
    
    // Output configuration
    private val outputDirectoryField = TextFieldWithBrowseButton()
    private val autoOpenResultCheckBox = JBCheckBox(PprofViewBundle.message("pprof.config.autoOpenResult"), true)
    
    // Sampling configuration
    private val cpuDurationField = JBTextField("30")
    private val httpPortField = JBTextField("6060")
    private val memProfileRateField = JBTextField("524288")
    private val mutexProfileFractionField = JBTextField("1")
    private val blockProfileRateField = JBTextField("1")
    
    // Go program configuration
    private val runKindComboBox = ComboBox(PprofRunKind.entries.toTypedArray())
    private val fileField = TextFieldWithBrowseButton()
    private val directoryField = TextFieldWithBrowseButton()
    private val packageField = ComboBox<String>()
    private val workingDirectoryField = TextFieldWithBrowseButton()
    private val programArgumentsField = JBTextField()
    private val environmentVariablesField = JBTextField()
    private val goBuildFlagsField = JBTextField()
    
    // Save configuration reference for smart auto-fill
    private var currentConfiguration: PprofConfiguration? = null

    init {
        // Set run kind ComboBox renderer
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
        
        // Set package ComboBox as editable
        packageField.isEditable = true
        
        // Configure file choosers
        val singleFileDescriptor = com.intellij.openapi.fileChooser.FileChooserDescriptor(
            true, false, false, false, false, false
        ).withTitle(PprofViewBundle.message("pprof.ui.selectGoFile"))
        
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
                singleFolderDescriptor.withTitle(PprofViewBundle.message("pprof.ui.selectDirectory")),
                null,
                null
            )
            chooser?.let { directoryField.text = it.path }
        }
        
        workingDirectoryField.addActionListener {
            val chooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
                singleFolderDescriptor.withTitle(PprofViewBundle.message("pprof.ui.selectWorkingDirectory")),
                null,
                null
            )
            chooser?.let { workingDirectoryField.text = it.path }
        }
        
        outputDirectoryField.addActionListener {
            val chooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(
                singleFolderDescriptor.withTitle(PprofViewBundle.message("pprof.ui.selectOutputDirectory")),
                null,
                null
            )
            chooser?.let { outputDirectoryField.text = it.path }
        }
        
        // Set ComboBox renderers
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
        // Create profile types panel
        val profileTypesPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder(PprofViewBundle.message("pprof.ui.profileTypes"))
            add(cpuCheckBox)
            add(heapCheckBox)
            add(goroutineCheckBox)
            add(threadCreateCheckBox)
            add(blockCheckBox)
            add(mutexCheckBox)
            add(allocsCheckBox)
            add(traceCheckBox)
        }
        
        // Create advanced configuration panel
        val advancedPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(PprofViewBundle.message("pprof.config.cpuDuration") + ":", cpuDurationField)
            .addLabeledComponent(PprofViewBundle.message("pprof.config.httpPort") + ":", httpPortField)
            .addLabeledComponent(PprofViewBundle.message("pprof.config.memProfileRate") + ":", memProfileRateField)
            .addLabeledComponent(PprofViewBundle.message("pprof.config.mutexProfileFraction") + ":", mutexProfileFractionField)
            .addLabeledComponent(PprofViewBundle.message("pprof.config.blockProfileRate") + ":", blockProfileRateField)
            .panel
        
        advancedPanel.border = BorderFactory.createTitledBorder(PprofViewBundle.message("pprof.ui.advancedConfig"))
        
        // Create run configuration panel
        val runConfigPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder(PprofViewBundle.message("pprof.ui.runConfig"))
        }
        
        // Add run kind selection and corresponding input fields
        val runKindPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.runKind") + ":", runKindComboBox)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.file") + ":", fileField)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.directory") + ":", directoryField)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.package") + ":", packageField)
            .panel
        
        runConfigPanel.add(runKindPanel)
        
        // Create main panel
        val panel = FormBuilder.createFormBuilder()
            .addComponent(runConfigPanel)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.workingDirectory") + ":", workingDirectoryField)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.programArguments") + ":", programArgumentsField)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.environmentVariables") + ":", environmentVariablesField)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.goBuildFlags") + ":", goBuildFlagsField)
            .addSeparator()
            .addComponent(enablePprofCheckBox)
            .addLabeledComponent(PprofViewBundle.message("pprof.config.collectionMode") + ":", collectionModeComboBox)
            .addLabeledComponent(PprofViewBundle.message("pprof.samplingMode.single") + ":", samplingModeComboBox)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.samplingInterval") + ":", samplingIntervalField)
            .addLabeledComponent(PprofViewBundle.message("pprof.ui.testPattern") + ":", testPatternField)
            .addComponent(profileTypesPanel)
            .addLabeledComponent(PprofViewBundle.message("pprof.config.outputDirectory") + ":", outputDirectoryField)
            .addComponent(autoOpenResultCheckBox)
            .addComponent(advancedPanel)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        
        panel.border = JBUI.Borders.empty(10)
        
        // Add run kind change listener
        runKindComboBox.addActionListener {
            onRunKindChanged()
        }
        
        // Add collection mode change listener
        collectionModeComboBox.addActionListener {
            onCollectionModeChanged()
        }
        
        // Add working directory change listener
        workingDirectoryField.textField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent?) = onWorkingDirectoryChanged()
            override fun removeUpdate(e: javax.swing.event.DocumentEvent?) = onWorkingDirectoryChanged()
            override fun changedUpdate(e: javax.swing.event.DocumentEvent?) = onWorkingDirectoryChanged()
        })
        
        return panel
    }

    override fun resetEditorFrom(configuration: PprofConfiguration) {
        // Save configuration reference
        currentConfiguration = configuration
        
        // Run configuration
        val runKind = PprofRunKind.fromString(configuration.runKind)
        runKindComboBox.selectedItem = runKind
        
        fileField.text = configuration.filePath
        directoryField.text = configuration.directoryPath
        packageField.selectedItem = configuration.packagePath
        workingDirectoryField.text = configuration.workingDirectory
        programArgumentsField.text = configuration.programArguments
        environmentVariablesField.text = configuration.environmentVariables
        goBuildFlagsField.text = configuration.goBuildFlags
        
        // Initialize smart options
        initializeSmartOptions(configuration)
        
        enablePprofCheckBox.isSelected = configuration.enablePprof
        
        val mode = PprofCollectionMode.fromString(configuration.collectionMode)
        collectionModeComboBox.selectedItem = mode
        
        val samplingMode = PprofSamplingMode.fromString(configuration.samplingMode)
        samplingModeComboBox.selectedItem = samplingMode
        samplingIntervalField.text = configuration.samplingInterval.toString()
        testPatternField.text = configuration.testPattern
        
        // Reset profile types
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
        
        // Update field availability
        onCollectionModeChanged()
        updateRunKindFields()
    }

    override fun applyEditorTo(configuration: PprofConfiguration) {
        // Run configuration
        configuration.runKind = (runKindComboBox.selectedItem as? PprofRunKind)?.name ?: PprofRunKind.FILE.name
        configuration.filePath = fileField.text
        configuration.directoryPath = directoryField.text
        configuration.packagePath = packageField.selectedItem?.toString() ?: packageField.editor.item?.toString() ?: ""
        configuration.workingDirectory = workingDirectoryField.text
        configuration.programArguments = programArgumentsField.text
        configuration.environmentVariables = environmentVariablesField.text
        configuration.goBuildFlags = goBuildFlagsField.text
        
        // Pprof configuration
        configuration.enablePprof = enablePprofCheckBox.isSelected
        configuration.collectionMode = (collectionModeComboBox.selectedItem as? PprofCollectionMode)?.name
            ?: PprofCollectionMode.RUNTIME_SAMPLING.name
        configuration.samplingMode = (samplingModeComboBox.selectedItem as? PprofSamplingMode)?.name
            ?: PprofSamplingMode.SINGLE.name
        configuration.samplingInterval = samplingIntervalField.text.toIntOrNull() ?: 60
        configuration.testPattern = testPatternField.text
        
        // Collect selected profile types
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
     * Initialize smart options
     */
    private fun initializeSmartOptions(configuration: PprofConfiguration) {
        val workingDir = configuration.workingDirectory.ifEmpty { 
            configuration.project?.basePath ?: ""
        }
        
        if (workingDir.isNotEmpty()) {
            // Auto-fill working directory
            if (workingDirectoryField.text.isEmpty()) {
                workingDirectoryField.text = workingDir
            }
            
            // Fill default values based on run kind
            updateSmartDefaults(workingDir)
        }
    }
    
    /**
     * Response when run kind changes
     */
    private fun onRunKindChanged() {
        updateRunKindFields()
        
        val workingDir = workingDirectoryField.text
        if (workingDir.isNotEmpty()) {
            updateSmartDefaults(workingDir)
        }
    }
    
    /**
     * Response when collection mode changes
     */
    private fun onCollectionModeChanged() {
        val collectionMode = collectionModeComboBox.selectedItem as? PprofCollectionMode
        val isTestMode = collectionMode == PprofCollectionMode.TEST_SAMPLING
        val isHttpMode = collectionMode == PprofCollectionMode.HTTP_SERVER
        
        // Test pattern field is only available in test sampling mode
        testPatternField.isEnabled = isTestMode
        
        // Sampling mode and interval are not available in test mode (test mode uses go test parameters)
        samplingModeComboBox.isEnabled = !isTestMode && !isHttpMode
        samplingIntervalField.isEnabled = !isTestMode && !isHttpMode
        
        // HTTP port is only available in HTTP server mode
        httpPortField.isEnabled = isHttpMode
        
        // Profile type availability
        if (isTestMode) {
            // Test mode: go test only supports the following parameters
            cpuCheckBox.isEnabled = true          // -cpuprofile
            heapCheckBox.isEnabled = true         // -memprofile
            blockCheckBox.isEnabled = true        // -blockprofile
            mutexCheckBox.isEnabled = true        // -mutexprofile
            
            // Types not supported by go test
            goroutineCheckBox.isEnabled = false
            threadCreateCheckBox.isEnabled = false
            allocsCheckBox.isEnabled = false
            traceCheckBox.isEnabled = false
            
            // Uncheck unsupported types
            goroutineCheckBox.isSelected = false
            threadCreateCheckBox.isSelected = false
            allocsCheckBox.isSelected = false
            traceCheckBox.isSelected = false
        } else {
            // Runtime sampling and HTTP server mode: all types available
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
     * Response when working directory changes
     */
    private fun onWorkingDirectoryChanged() {
        val workingDir = workingDirectoryField.text
        if (workingDir.isNotEmpty()) {
            updateSmartDefaults(workingDir)
        }
    }
    
    /**
     * Update smart defaults
     */
    private fun updateSmartDefaults(workingDir: String) {
        val runKind = runKindComboBox.selectedItem as? PprofRunKind ?: PprofRunKind.FILE
        val workingDirFile = java.io.File(workingDir)
        
        if (!workingDirFile.exists() || !workingDirFile.isDirectory) {
            return
        }
        
        when (runKind) {
            PprofRunKind.FILE -> {
                // Auto-find main.go file
                if (fileField.text.isEmpty()) {
                    val mainFile = findMainGoFile(workingDirFile)
                    if (mainFile != null) {
                        fileField.text = mainFile.absolutePath
                    }
                }
            }
            PprofRunKind.DIRECTORY -> {
                // Auto-set to working directory
                if (directoryField.text.isEmpty()) {
                    directoryField.text = workingDir
                }
            }
            PprofRunKind.PACKAGE -> {
                // Auto-read go.mod to get package list
                updatePackageList(workingDirFile)
            }
        }
    }
    
    /**
     * Find main.go file
     */
    private fun findMainGoFile(directory: java.io.File): java.io.File? {
        // First look for main.go
        val mainGo = java.io.File(directory, "main.go")
        if (mainGo.exists() && mainGo.isFile) {
            return mainGo
        }
        
        // Find any .go file containing main function
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
                // Ignore read errors
            }
        }
        
        return null
    }
    
    /**
     * Update package list
     */
    private fun updatePackageList(directory: java.io.File) {
        val packages = mutableListOf<String>()
        
        // Read go.mod to get module path
        val goModFile = java.io.File(directory, "go.mod")
        if (goModFile.exists()) {
            try {
                val content = goModFile.readText()
                val modulePattern = Regex("""module\s+([^\s]+)""")
                val match = modulePattern.find(content)
                if (match != null) {
                    val modulePath = match.groupValues[1]
                    packages.add(modulePath)
                    
                    // Find sub-packages
                    findSubPackages(directory, modulePath, packages)
                }
            } catch (e: Exception) {
                // Ignore read errors
            }
        }
        
        // Update dropdown list
        packageField.removeAllItems()
        packages.forEach { packageField.addItem(it) }
        
        // If current value is not in list, add it
        val currentValue = currentConfiguration?.packagePath
        if (!currentValue.isNullOrEmpty() && !packages.contains(currentValue)) {
            packageField.addItem(currentValue)
            packageField.selectedItem = currentValue
        }
    }
    
    /**
     * Find sub-packages
     */
    private fun findSubPackages(directory: java.io.File, modulePath: String, packages: MutableList<String>) {
        val subdirs = directory.listFiles { file -> 
            file.isDirectory && !file.name.startsWith(".") && file.name != "vendor"
        } ?: return
        
        for (subdir in subdirs) {
            // Check if contains .go files
            val hasGoFiles = subdir.listFiles { file ->
                file.isFile && file.name.endsWith(".go") && !file.name.endsWith("_test.go")
            }?.isNotEmpty() ?: false
            
            if (hasGoFiles) {
                val relativePath = subdir.relativeTo(directory).path.replace(java.io.File.separator, "/")
                packages.add("$modulePath/$relativePath")
            }
            
            // Recursively find subdirectories
            findSubPackages(subdir, modulePath, packages)
        }
    }
    
    /**
     * Update input field availability based on run kind
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
