package com.github.spelens.pprofview.services

import com.github.spelens.pprofview.PprofViewBundle
import com.github.spelens.pprofview.editor.PprofInlayRenderer
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * pprof code navigation service
 * Responsible for navigating from pprof data to source code locations
 */
@Service(Service.Level.PROJECT)
class PprofCodeNavigationService(private val project: Project) {
    private val logger = thisLogger()
    
    // Store current highlighted editor for clearing highlights
    private var currentHighlightedEditor: Editor? = null
    
    // Store current highlighted file for monitoring file close events
    private var currentHighlightedFile: VirtualFile? = null
    
    // Store current inlay hints for clearing
    private val currentInlays = mutableListOf<Inlay<*>>()
    
    init {
        // Listen for file close events
        setupFileCloseListener()
    }
    
    /**
     * Setup file close listener
     * Clear highlights when highlighted file is closed
     */
    private fun setupFileCloseListener() {
        val connection = project.messageBus.connect()
        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
            override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                // Check if closed file is the currently highlighted file
                if (file == currentHighlightedFile) {
                    logger.info("Detected highlighted file closed: ${file.path}, clearing highlights")
                    clearHighlights()
                }
            }
        })
    }
    
    /**
     * Navigate to function definition
     * 
     * @param pprofFile pprof file
     * @param functionName function name
     */
    fun navigateToFunction(pprofFile: VirtualFile, functionName: String) {
        val startTime = System.currentTimeMillis()
        logger.info("========================================")
        logger.info(PprofViewBundle.message("pprof.navigation.starting", functionName))
        logger.info("pprof file: ${pprofFile.path}")
        
        // Use pprof -list command to get function source code information
        val listStartTime = System.currentTimeMillis()
        val listOutput = executeListCommand(pprofFile, functionName)
        val listDuration = System.currentTimeMillis() - listStartTime
        logger.info("Executing pprof -list command took: ${listDuration}ms")
        
        if (listOutput.isEmpty()) {
            logger.warn("Cannot get source code information for function $functionName")
            logger.info("Total time: ${System.currentTimeMillis() - startTime}ms")
            logger.info("========================================")
            
            // Show error notification
            showNotification(
                PprofViewBundle.message("pprof.navigation.failed"),
                PprofViewBundle.message("pprof.navigation.noSourceInfo", functionName),
                com.intellij.notification.NotificationType.WARNING
            )
            return
        }
        
        logger.info("Got output length: ${listOutput.length} characters")
        
        // Parse list output, extract file path and line number
        val parseStartTime = System.currentTimeMillis()
        val codeLocation = parseListOutput(listOutput)
        val parseDuration = System.currentTimeMillis() - parseStartTime
        logger.info("Parsing output took: ${parseDuration}ms")
        
        if (codeLocation == null) {
            logger.warn("Cannot parse code location for function $functionName")
            logger.warn("pprof -list output content:")
            logger.warn(listOutput)
            logger.info("Total time: ${System.currentTimeMillis() - startTime}ms")
            logger.info("========================================")
            
            // Show error notification
            showNotification(
                PprofViewBundle.message("pprof.navigation.failed"),
                PprofViewBundle.message("pprof.navigation.parseError", functionName),
                com.intellij.notification.NotificationType.WARNING
            )
            return
        }
        
        logger.info("Parse result:")
        logger.info("  - File path: ${codeLocation.filePath}")
        logger.info("  - Target line: ${codeLocation.targetLine}")
        logger.info("  - Hot lines count: ${codeLocation.hotLines.size}")
        logger.info("  - Hot line numbers: ${codeLocation.hotLines.filter { it.isHot }.map { it.lineNumber }}")
        
        // Open file and highlight in editor
        val openStartTime = System.currentTimeMillis()
        openAndHighlightCode(codeLocation)
        val openDuration = System.currentTimeMillis() - openStartTime
        logger.info("Open file and highlight took: ${openDuration}ms")
        
        val totalDuration = System.currentTimeMillis() - startTime
        logger.info("Total time: ${totalDuration}ms")
        logger.info("Performance breakdown:")
        logger.info("  - pprof -list: ${listDuration}ms (${listDuration * 100 / totalDuration}%)")
        logger.info("  - Parse output: ${parseDuration}ms (${parseDuration * 100 / totalDuration}%)")
        logger.info("  - Open and highlight: ${openDuration}ms (${openDuration * 100 / totalDuration}%)")
        logger.info("========================================")
    }
    
    /**
     * Execute pprof -list command
     */
    private fun executeListCommand(pprofFile: VirtualFile, functionName: String): String {
        // Try multiple function name patterns
        val patterns = buildFunctionPatterns(functionName)
        
        for ((index, pattern) in patterns.withIndex()) {
            logger.info("Trying pattern ${index + 1}/${patterns.size}: $pattern")
            
            val commandLine = GeneralCommandLine()
            commandLine.exePath = "go"
            commandLine.addParameters("tool", "pprof", "-list=$pattern", pprofFile.path)
            
            val result = executePprofCommand(commandLine)
            if (result.isNotEmpty()) {
                logger.info("Pattern ${index + 1} succeeded, got output")
                return result
            }
        }
        
        logger.warn("All patterns failed")
        return ""
    }
    
    /**
     * Build function name matching patterns
     */
    private fun buildFunctionPatterns(functionName: String): List<String> {
        val patterns = mutableListOf<String>()
        
        // Pattern 1: Original function name
        patterns.add(functionName)
        
        // Pattern 2: Extract last part (method name)
        // github.com/user/project/pkg.(*Type).Method.func1 -> Method
        val lastPart = functionName.substringAfterLast('.')
        if (lastPart != functionName && !lastPart.startsWith("func")) {
            patterns.add(lastPart)
        }
        
        // Pattern 3: Extract type and method
        // github.com/user/project/pkg.(*Type).Method.func1 -> (*Type).Method
        if (functionName.contains("(*") && functionName.contains(").")) {
            val typeAndMethod = functionName.substringAfter("(*").substringBefore(".func")
            if (typeAndMethod.isNotEmpty()) {
                patterns.add("(*$typeAndMethod")
            }
        }
        
        // Pattern 4: Use regex matching (escape special characters)
        // Replace . with \., * with \*, ( with \(, ) with \)
        val escapedName = functionName
            .replace("\\", "\\\\")
            .replace(".", "\\.")
            .replace("*", "\\*")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("[", "\\[")
            .replace("]", "\\]")
        patterns.add(escapedName)
        
        // Pattern 5: Simplified regex (match only last few parts)
        val parts = functionName.split('/')
        if (parts.size > 1) {
            val simplifiedPattern = parts.takeLast(2).joinToString("/")
            patterns.add(simplifiedPattern)
        }
        
        // Pattern 6: Only package name and function name
        // github.com/user/project/pkg.Function -> pkg.Function
        if (functionName.contains('/')) {
            val pkgAndFunc = functionName.substringAfterLast('/')
            if (pkgAndFunc != functionName) {
                patterns.add(pkgAndFunc)
            }
        }
        
        return patterns.distinct()
    }
    
    /**
     * Execute pprof command
     */
    private fun executePprofCommand(commandLine: GeneralCommandLine): String {
        logger.info("Executing command: ${commandLine.commandLineString}")
        
        try {
            val processStartTime = System.currentTimeMillis()
            val process = commandLine.createProcess()
            val processCreateDuration = System.currentTimeMillis() - processStartTime
            logger.info("  - Process creation took: ${processCreateDuration}ms")
            
            val readStartTime = System.currentTimeMillis()
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = stdoutReader.readText()
            val errorOutput = stderrReader.readText()
            
            val readDuration = System.currentTimeMillis() - readStartTime
            logger.info("  - Reading output took: ${readDuration}ms")
            
            val waitStartTime = System.currentTimeMillis()
            val exitCode = process.waitFor()
            val waitDuration = System.currentTimeMillis() - waitStartTime
            logger.info("  - Waiting for process took: ${waitDuration}ms")
            logger.info("  - Process exit code: $exitCode")
            
            if (exitCode != 0) {
                logger.warn("pprof command execution failed, exit code: $exitCode")
                if (errorOutput.isNotEmpty()) {
                    logger.warn("Error output: $errorOutput")
                }
                if (output.isNotEmpty()) {
                    logger.info("Standard output: $output")
                }
                return ""
            } else {
                logger.info("  - Output length: ${output.length} characters")
                if (errorOutput.isNotEmpty()) {
                    logger.warn("Warning output: $errorOutput")
                }
            }
            
            return output
        } catch (e: Exception) {
            logger.warn("Exception executing pprof command: ${e.message}")
            return ""
        }
    }
    
    /**
     * Parse list output
     * 
     * Output format example:
     * ```
     * Total: 10.50s
     * ROUTINE ======================== main.fibonacci in /path/to/file.go
     *       10ms      10ms (flat, cum)  0.10% of Total
     *          .          .     10:func fibonacci(n int) int {
     *          .          .     11:    if n <= 1 {
     *       10ms       10ms     12:        return n
     *          .          .     13:    }
     *          .          .     14:    return fibonacci(n-1) + fibonacci(n-2)
     *          .          .     15:}
     * ```
     */
    private fun parseListOutput(output: String): CodeLocation? {
        logger.info("Starting to parse list output")
        val lines = output.lines()
        logger.info("  - Total lines: ${lines.size}")
        
        var filePath: String? = null
        val hotLines = mutableListOf<HotLine>()
        var routineCount = 0
        var codeLineCount = 0
        
        for (line in lines) {
            // Parse ROUTINE line, extract file path
            if (line.contains("ROUTINE") && line.contains(" in ")) {
                routineCount++
                val parts = line.split(" in ")
                if (parts.size >= 2) {
                    filePath = parts[1].trim()
                    logger.info("  - Found ROUTINE: $filePath")
                }
                continue
            }
            
            // Parse code line
            // Format: "       10ms       10ms     12:        return n"
            val codeLinePattern = """^\s*(\S+)\s+(\S+)\s+(\d+):(.*)$""".toRegex()
            val match = codeLinePattern.find(line)
            if (match != null) {
                codeLineCount++
                val (flat, cum, lineNum, code) = match.destructured
                
                // Only record lines with performance data (flat or cum not ".")
                val hasData = flat != "." || cum != "."
                
                hotLines.add(HotLine(
                    lineNumber = lineNum.toInt(),
                    code = code,
                    flat = flat,
                    cum = cum,
                    isHot = hasData
                ))
            }
        }
        
        logger.info("  - Found ROUTINE count: $routineCount")
        logger.info("  - Parsed code lines: $codeLineCount")
        logger.info("  - Hot lines count: ${hotLines.count { it.isHot }}")
        
        if (filePath == null || hotLines.isEmpty()) {
            logger.warn("  - Parse failed: filePath=$filePath, hotLines.size=${hotLines.size}")
            return null
        }
        
        // Find first hot line as jump target
        val targetLine = hotLines.firstOrNull { it.isHot }?.lineNumber 
            ?: hotLines.firstOrNull()?.lineNumber 
            ?: 1
        
        logger.info("  - Selected target line: $targetLine")
        
        return CodeLocation(
            filePath = filePath,
            targetLine = targetLine,
            hotLines = hotLines
        )
    }
    
    /**
     * Open file and highlight code in editor
     */
    private fun openAndHighlightCode(location: CodeLocation) {
        ApplicationManager.getApplication().invokeLater {
            val uiStartTime = System.currentTimeMillis()
            logger.info("Starting to open file in UI thread")
            
            // Find file (supports multiple path formats)
            val findFileStartTime = System.currentTimeMillis()
            val virtualFile = findSourceFile(location.filePath)
            val findFileDuration = System.currentTimeMillis() - findFileStartTime
            logger.info("  - Finding file took: ${findFileDuration}ms")
            
            if (virtualFile == null) {
                logger.warn("File does not exist: ${location.filePath}")
                logger.warn("Tried multiple path search strategies, file not found")
                
                // Show error notification
                showNotification(
                    PprofViewBundle.message("pprof.navigation.fileNotFound"),
                    PprofViewBundle.message("pprof.navigation.fileNotFoundMsg", location.filePath),
                    com.intellij.notification.NotificationType.WARNING
                )
                return@invokeLater
            }
            
            logger.info("  - File size: ${virtualFile.length} bytes")
            
            // Open file
            val openEditorStartTime = System.currentTimeMillis()
            val descriptor = OpenFileDescriptor(
                project,
                virtualFile,
                location.targetLine - 1, // Line numbers start from 0
                0
            )
            
            val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
            val openEditorDuration = System.currentTimeMillis() - openEditorStartTime
            logger.info("  - Opening editor took: ${openEditorDuration}ms")
            
            if (editor == null) {
                logger.warn("Cannot open editor")
                return@invokeLater
            }
            
            logger.info("  - Document line count: ${editor.document.lineCount}")
            
            // Scroll to target line
            val scrollStartTime = System.currentTimeMillis()
            editor.scrollingModel.scrollToCaret(ScrollType.CENTER)
            val scrollDuration = System.currentTimeMillis() - scrollStartTime
            logger.info("  - Scrolling to target line took: ${scrollDuration}ms")
            
            // Highlight hot code lines
            val highlightStartTime = System.currentTimeMillis()
            val markupModel = editor.markupModel
            
            // If same editor, clear old highlights and inlay hints first to avoid stacking
            val clearStartTime = System.currentTimeMillis()
            if (currentHighlightedEditor == editor) {
                logger.info("  - Detected same editor, clearing old highlights to avoid stacking")
                // Clear highlights
                markupModel.removeAllHighlighters()
                
                // Clear inlay hints
                currentInlays.forEach { inlay ->
                    try {
                        inlay.dispose()
                    } catch (e: Exception) {
                        logger.warn("Failed to clear inlay: ${e.message}")
                    }
                }
                currentInlays.clear()
            }
            
            // Update current highlighted editor and file reference
            currentHighlightedEditor = editor
            currentHighlightedFile = virtualFile
            val clearDuration = System.currentTimeMillis() - clearStartTime
            logger.info("  - Clearing old highlights took: ${clearDuration}ms")
            
            // Add highlights and inlay hints for each hot line
            var highlightedCount = 0
            var inlayCount = 0
            logger.info("  - Starting to process ${location.hotLines.size} hot lines")
            
            for (hotLine in location.hotLines) {
                logger.info("  - Processing line ${hotLine.lineNumber}: isHot=${hotLine.isHot}, flat=${hotLine.flat}, cum=${hotLine.cum}")
                
                if (!hotLine.isHot) continue
                
                val lineNumber = hotLine.lineNumber - 1 // Line numbers start from 0
                if (lineNumber < 0 || lineNumber >= editor.document.lineCount) {
                    logger.warn("  - Skipping invalid line number: $lineNumber (document total lines: ${editor.document.lineCount})")
                    continue
                }
                
                val startOffset = editor.document.getLineStartOffset(lineNumber)
                val endOffset = editor.document.getLineEndOffset(lineNumber)
                
                logger.info("  - Line ${hotLine.lineNumber} offset: start=$startOffset, end=$endOffset")
                
                // Choose color based on performance data intensity
                val (backgroundColor, borderColor) = getHotLineColors(hotLine)
                
                // Create text attributes (with background color and rounded border)
                val textAttributes = TextAttributes().apply {
                    this.backgroundColor = backgroundColor
                    effectColor = borderColor
                    // Use rounded box effect for better appearance
                    effectType = EffectType.ROUNDED_BOX
                    fontType = Font.BOLD
                }
                
                // Add highlight
                val highlighter = markupModel.addRangeHighlighter(
                    startOffset,
                    endOffset,
                    HighlighterLayer.SELECTION - 1,
                    textAttributes,
                    HighlighterTargetArea.LINES_IN_RANGE
                )
                
                // Set tooltip
                val tooltip = buildTooltip(hotLine)
                highlighter.errorStripeTooltip = tooltip
                
                highlightedCount++
                
                // Add end-of-line inlay hint to display performance data
                addInlayHint(editor, lineNumber, hotLine)
                inlayCount++
            }
            
            logger.info("  - Processing complete: highlighted $highlightedCount lines, attempted to add $inlayCount inlay hints")
            
            val highlightDuration = System.currentTimeMillis() - highlightStartTime
            logger.info("  - Adding highlights took: ${highlightDuration}ms")
            logger.info("  - Highlighted lines count: $highlightedCount")
            
            val uiTotalDuration = System.currentTimeMillis() - uiStartTime
            logger.info("  - UI operation total time: ${uiTotalDuration}ms")
            logger.info(PprofViewBundle.message("pprof.navigation.opened", location.filePath, location.targetLine))
        }
    }
    
    /**
     * Smart source file finder
     * Supports multiple path formats:
     * 1. Absolute path: /path/to/file.go
     * 2. Relative path: src/main.go
     * 3. Package path: github.com/user/project/main.go
     * 4. GOPATH path: /Users/user/go/src/github.com/user/project/main.go
     */
    private fun findSourceFile(filePath: String): VirtualFile? {
        logger.info("Starting to find source file: $filePath")
        
        // Strategy 1: Try as absolute path
        var file = LocalFileSystem.getInstance().findFileByPath(filePath)
        if (file != null) {
            logger.info("  - Strategy 1 succeeded: absolute path")
            return file
        }
        
        // Strategy 2: Find in project root directory
        val projectBasePath = project.basePath
        if (projectBasePath != null) {
            file = LocalFileSystem.getInstance().findFileByPath("$projectBasePath/$filePath")
            if (file != null) {
                logger.info("  - Strategy 2 succeeded: project root + relative path")
                return file
            }
        }
        
        // Strategy 3: If path contains GOPATH structure (like /go/src/github.com/...), extract package path
        if (filePath.contains("/src/")) {
            val packagePath = filePath.substringAfter("/src/")
            logger.info("  - Detected GOPATH structure, extracting package path: $packagePath")
            
            if (projectBasePath != null) {
                // Try to find in project
                file = LocalFileSystem.getInstance().findFileByPath("$projectBasePath/$packagePath")
                if (file != null) {
                    logger.info("  - Strategy 3 succeeded: GOPATH package path match")
                    return file
                }
            }
        }
        
        // Strategy 4: Extract filename and search in project
        val fileName = filePath.substringAfterLast('/')
        logger.info("  - Extracted filename: $fileName")
        
        // Use FilenameIndex to find files
        val files = com.intellij.psi.search.FilenameIndex.getVirtualFilesByName(
            fileName,
            com.intellij.psi.search.GlobalSearchScope.projectScope(project)
        ).mapNotNull { virtualFile ->
            com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile)
        }.toTypedArray()
        
        logger.info("  - Found ${files.size} files with same name")
        
        if (files.isEmpty()) {
            logger.warn("  - Strategy 4 failed: file not found $fileName")
            return null
        }
        
        // Strategy 5: If only one file with same name, return directly
        if (files.size == 1) {
            logger.info("  - Strategy 5 succeeded: unique match")
            return files[0].virtualFile
        }
        
        // Strategy 6: Multiple files, try exact path matching
        logger.info("  - Strategy 6: trying exact path matching")
        
        // Extract key part of path (remove GOPATH prefix)
        var cleanPath = filePath
        if (cleanPath.contains("/src/")) {
            cleanPath = cleanPath.substringAfter("/src/")
        }
        
        // Extract path suffix for matching (last 4 directory levels)
        val pathParts = cleanPath.split('/').filter { it.isNotEmpty() }
        val matchDepth = minOf(4, pathParts.size)
        val pathSuffix = pathParts.takeLast(matchDepth).joinToString("/")
        logger.info("  - Cleaned path: $cleanPath")
        logger.info("  - Path suffix (depth=$matchDepth): $pathSuffix")
        
        // First try exact path suffix matching
        for (psiFile in files) {
            val candidatePath = psiFile.virtualFile.path
            logger.info("  - Checking candidate file: $candidatePath")
            
            if (candidatePath.endsWith(pathSuffix)) {
                logger.info("  - Strategy 6 succeeded: path suffix exact match")
                return psiFile.virtualFile
            }
        }
        
        // Strategy 7: Try package path matching (level by level)
        logger.info("  - Strategy 7: trying package path level-by-level matching")
        
        var bestMatch: com.intellij.psi.PsiFile? = null
        var bestMatchScore = 0
        
        for (psiFile in files) {
            val candidatePath = psiFile.virtualFile.path
            
            // Calculate path match score
            var score = 0
            for (i in pathParts.indices.reversed()) {
                val part = pathParts[i]
                if (candidatePath.contains("/$part/") || candidatePath.endsWith("/$part")) {
                    score++
                } else {
                    break
                }
            }
            
            logger.info("  - Candidate file $candidatePath match score: $score/${pathParts.size}")
            
            if (score > bestMatchScore) {
                bestMatchScore = score
                bestMatch = psiFile
            }
        }
        
        if (bestMatch != null && bestMatchScore >= 2) {
            logger.info("  - Strategy 7 succeeded: best match (score=$bestMatchScore)")
            return bestMatch.virtualFile
        }
        
        // Strategy 8: Return first matching file (last fallback strategy)
        logger.warn("  - Strategy 8: using first matching file (may not be accurate)")
        return files[0].virtualFile
    }
    
    /**
     * Get colors and styles based on hotspot data
     * Returns Pair<background color, border color>
     */
    private fun getHotLineColors(hotLine: HotLine): Pair<Color, Color> {
        // Parse performance data intensity
        val flatValue = parsePerformanceValue(hotLine.flat)
        val cumValue = parsePerformanceValue(hotLine.cum)
        val maxValue = maxOf(flatValue, cumValue)
        
        // Choose color based on performance data intensity
        return when {
            maxValue >= 100 -> {
                // High hotspot: Red (Material Design Red)
                JBColor(
                    Color(255, 235, 238, 80),  // Light theme: light red background
                    Color(100, 45, 50, 70)     // Dark theme: dark red background
                ) to JBColor(
                    Color(239, 83, 80),        // Light theme: red border
                    Color(229, 115, 115)       // Dark theme: bright red border
                )
            }
            maxValue >= 10 -> {
                // Medium hotspot: Orange (Material Design Orange)
                JBColor(
                    Color(255, 243, 224, 80),  // Light theme: light orange background
                    Color(100, 75, 45, 70)     // Dark theme: dark orange background
                ) to JBColor(
                    Color(255, 152, 0),        // Light theme: orange border
                    Color(255, 183, 77)        // Dark theme: bright orange border
                )
            }
            maxValue > 0 -> {
                // Low hotspot: Yellow (Material Design Amber)
                JBColor(
                    Color(255, 248, 225, 80),  // Light theme: light yellow background
                    Color(100, 90, 45, 70)     // Dark theme: dark yellow background
                ) to JBColor(
                    Color(255, 193, 7),        // Light theme: amber border
                    Color(255, 213, 79)        // Dark theme: bright amber border
                )
            }
            else -> {
                // Default: Green (Material Design Green)
                JBColor(
                    Color(232, 245, 233, 80),  // Light theme: light green background
                    Color(45, 80, 50, 70)      // Dark theme: dark green background
                ) to JBColor(
                    Color(76, 175, 80),        // Light theme: green border
                    Color(102, 187, 106)       // Dark theme: bright green border
                )
            }
        }
    }
    
    /**
     * Parse performance data value (supports units: ms, s, MB, KB, etc.)
     */
    private fun parsePerformanceValue(value: String): Double {
        if (value == ".") return 0.0
        
        try {
            // Remove units, keep only numbers
            val numStr = value.replace(Regex("[a-zA-Z%]"), "").trim()
            return numStr.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            return 0.0
        }
    }
    
    /**
     * Build tooltip text
     */
    private fun buildTooltip(hotLine: HotLine): String {
        val flatValue = parsePerformanceValue(hotLine.flat)
        val cumValue = parsePerformanceValue(hotLine.cum)
        val maxValue = maxOf(flatValue, cumValue)
        
        val statusIcon = when {
            maxValue >= 100 -> "🔥"
            maxValue >= 10 -> "⚠️"
            maxValue > 0 -> "📊"
            else -> "✅"
        }
        
        val statusText = when {
            maxValue >= 100 -> PprofViewBundle.message("pprof.inlay.hotspot.high")
            maxValue >= 10 -> PprofViewBundle.message("pprof.inlay.hotspot.medium")
            maxValue > 0 -> PprofViewBundle.message("pprof.inlay.hotspot.low")
            else -> PprofViewBundle.message("pprof.inlay.hotspot.default")
        }
        
        return buildString {
            append("$statusIcon ${PprofViewBundle.message("pprof.inlay.tooltip.title")}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("${PprofViewBundle.message("pprof.inlay.tooltip.line", hotLine.lineNumber)}\n")
            append("${PprofViewBundle.message("pprof.inlay.tooltip.flat", hotLine.flat)}\n")
            append("${PprofViewBundle.message("pprof.inlay.tooltip.cumulative", hotLine.cum)}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("$statusIcon ${PprofViewBundle.message("pprof.inlay.tooltip.status", statusText)}\n")
            if (hotLine.code.isNotBlank()) {
                append("━━━━━━━━━━━━━━━━━━━━\n")
                append("${PprofViewBundle.message("pprof.inlay.tooltip.code", hotLine.code.trim())}")
            }
        }
    }
    
    /**
     * Add end-of-line inlay hint to display performance data
     */
    private fun addInlayHint(editor: Editor, lineNumber: Int, hotLine: HotLine) {
        try {
            // Build inlay hint text
            val hintText = buildInlayHintText(hotLine)
            
            if (hintText.isEmpty()) {
                logger.info("  - Skipping empty inlay hint: line ${hotLine.lineNumber}")
                return
            }
            
            // Get end-of-line offset
            val offset = editor.document.getLineEndOffset(lineNumber)
            
            logger.info("  - Preparing to add inlay hint: line ${hotLine.lineNumber}, lineNumber: $lineNumber, offset: $offset, text: $hintText")
            
            // Create inlay hint (display at end of line)
            // Use addAfterLineEndElement to add inlay at end of line
            val inlay = editor.inlayModel.addAfterLineEndElement(
                offset,
                true,
                PprofInlayRenderer(hintText, hotLine)
            )
            
            if (inlay != null) {
                currentInlays.add(inlay)
                logger.info("  - Successfully added inlay hint: line ${hotLine.lineNumber}")
            } else {
                logger.warn("  - inlay is null: line ${hotLine.lineNumber}")
            }
        } catch (e: Exception) {
            logger.error("Failed to add inlay hint: ${e.message}", e)
        }
    }
    
    /**
     * Build inlay hint text
     */
    private fun buildInlayHintText(hotLine: HotLine): String {
        val parts = mutableListOf<String>()
        
        if (hotLine.flat != ".") {
            parts.add("flat: ${hotLine.flat}")
        }
        
        if (hotLine.cum != ".") {
            parts.add("cum: ${hotLine.cum}")
        }
        
        return if (parts.isNotEmpty()) {
            "  // ${parts.joinToString(", ")}"
        } else {
            ""
        }
    }
    
    /**
     * Clear highlights and inlay hints in current editor
     */
    fun clearHighlights() {
        currentHighlightedEditor?.let { editor ->
            ApplicationManager.getApplication().invokeLater {
                try {
                    // Clear highlights
                    editor.markupModel.removeAllHighlighters()
                    
                    // Clear inlay hints
                    currentInlays.forEach { inlay ->
                        try {
                            inlay.dispose()
                        } catch (e: Exception) {
                            logger.warn("Failed to clear inlay: ${e.message}")
                        }
                    }
                    currentInlays.clear()
                    
                    logger.info("Cleared editor highlights and inlay hints")
                } catch (e: Exception) {
                    logger.warn("Failed to clear highlights: ${e.message}")
                }
            }
        }
        currentHighlightedEditor = null
    }
    
    /**
     * Show notification
     */
    private fun showNotification(title: String, content: String, type: com.intellij.notification.NotificationType) {
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("pprofview.notifications")
            .createNotification(title, content, type)
            .notify(project)
    }
    
    companion object {
        fun getInstance(project: Project): PprofCodeNavigationService {
            return project.getService(PprofCodeNavigationService::class.java)
        }
    }
}

/**
 * Code location information
 */
data class CodeLocation(
    val filePath: String,
    val targetLine: Int,
    val hotLines: List<HotLine>
)

/**
 * Hot code line
 */
data class HotLine(
    val lineNumber: Int,
    val code: String,
    val flat: String,
    val cum: String,
    val isHot: Boolean
)
