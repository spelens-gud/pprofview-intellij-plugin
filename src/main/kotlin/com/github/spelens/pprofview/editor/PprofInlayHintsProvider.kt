package com.github.spelens.pprofview.editor

import com.github.spelens.pprofview.PprofViewBundle
import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.JPanel

/**
 * pprof performance data Inlay Hints Provider
 * Display performance analysis data inline in the editor
 */
@Suppress("UnstableApiUsage")
class PprofInlayHintsProvider : InlayHintsProvider<NoSettings> {
    
    override val key: SettingsKey<NoSettings> = SettingsKey("pprof.hints")
    
    override val name: String = PprofViewBundle.message("pprof.inlay.name")
    
    override val previewText: String? = null
    
    override fun createSettings(): NoSettings = NoSettings()
    
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        // Only provide hints for Go files
        if (file.fileType.name != "Go") return null
        
        return PprofInlayHintsCollector(editor)
    }
    
    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
        return object : ImmediateConfigurable {
            override fun createComponent(listener: ChangeListener): JPanel = JPanel()
        }
    }
}

/**
 * pprof Inlay Hints collector
 */
@Suppress("UnstableApiUsage")
class PprofInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
    
    override fun collect(element: com.intellij.psi.PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        // Hints are dynamically added by PprofCodeNavigationService
        return true
    }
}
