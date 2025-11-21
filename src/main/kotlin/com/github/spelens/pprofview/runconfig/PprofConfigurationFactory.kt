package com.github.spelens.pprofview.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

/**
 * Pprof configuration factory
 */
class PprofConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    
    override fun getId(): String = "GoPprofConfigurationFactory"
    
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return PprofConfiguration(project, this, "Go Pprof")
    }
}
