package com.github.anniext.pprofview.editor

import com.intellij.codeInsight.hints.*
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.JPanel

/**
 * pprof 性能数据 Inlay Hints Provider
 * 在编辑器中内嵌显示性能分析数据
 */
@Suppress("UnstableApiUsage")
class PprofInlayHintsProvider : InlayHintsProvider<NoSettings> {
    
    override val key: SettingsKey<NoSettings> = SettingsKey("pprof.hints")
    
    override val name: String = "pprof 性能数据"
    
    override val previewText: String? = null
    
    override fun createSettings(): NoSettings = NoSettings()
    
    override fun getCollectorFor(
        file: PsiFile,
        editor: Editor,
        settings: NoSettings,
        sink: InlayHintsSink
    ): InlayHintsCollector? {
        // 只为 Go 文件提供 hints
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
 * pprof Inlay Hints 收集器
 */
@Suppress("UnstableApiUsage")
class PprofInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
    
    override fun collect(element: com.intellij.psi.PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        // Hints 由 PprofCodeNavigationService 动态添加
        return true
    }
}
