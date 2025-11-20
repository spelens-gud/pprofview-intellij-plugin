package com.github.anniext.pprofview.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

/**
 * Pprof 配置工厂
 */
class PprofConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    
    override fun getId(): String = "GoPprofConfigurationFactory"
    
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return PprofConfiguration(project, this, "Go Pprof")
    }
    
    override fun getOptionsClass(): Class<out BaseState> {
        return PprofRunConfigurationOptions::class.java
    }
}
