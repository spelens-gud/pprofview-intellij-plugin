package com.github.anniext.pprofview.runconfig

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

/**
 * pprof 进程监听器
 * 
 * 在程序结束时自动打开生成的性能分析文件
 */
class PprofProcessListener(
    private val outputDirectory: File,
    private val profileTypes: List<String>
) : ProcessAdapter() {
    
    private val logger = thisLogger()

    override fun processTerminated(event: ProcessEvent) {
        super.processTerminated(event)
        
        ApplicationManager.getApplication().invokeLater {
            openProfileFiles()
        }
    }

    private fun openProfileFiles() {
        val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return
        val fileEditorManager = FileEditorManager.getInstance(project)
        
        profileTypes.forEach { typeStr ->
            val profileType = PprofProfileType.fromString(typeStr) ?: return@forEach
            val file = File(outputDirectory, profileType.fileName)
            
            if (file.exists() && file.length() > 0) {
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                if (virtualFile != null) {
                    fileEditorManager.openFile(virtualFile, true)
                    logger.info("已打开性能分析文件: ${file.absolutePath}")
                } else {
                    logger.warn("无法找到虚拟文件: ${file.absolutePath}")
                }
            } else {
                logger.warn("性能分析文件不存在或为空: ${file.absolutePath}")
            }
        }
    }
}
