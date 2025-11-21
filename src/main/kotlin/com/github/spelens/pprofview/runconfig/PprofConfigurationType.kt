package com.github.spelens.pprofview.runconfig

import com.github.spelens.pprofview.PprofViewBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * Pprof configuration type
 * 
 * Displayed in "Add New Configuration" menu
 */
class PprofConfigurationType : ConfigurationType {
    
    override fun getDisplayName(): String = PprofViewBundle.message("pprof.configurationType.name")
    
    override fun getConfigurationTypeDescription(): String = 
        PprofViewBundle.message("pprof.configurationType.description")
    
    override fun getIcon(): Icon = AllIcons.Actions.Profile
    
    override fun getId(): String = "GoPprofConfiguration"
    
    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(PprofConfigurationFactory(this))
    }
}
