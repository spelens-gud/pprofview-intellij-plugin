package com.github.spelens.pprofview.runconfig

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

/**
 * Pprof 配置类型
 * 
 * 在"添加新配置"菜单中显示
 */
class PprofConfigurationType : ConfigurationType {
    
    override fun getDisplayName(): String = "Go Pprof"
    
    override fun getConfigurationTypeDescription(): String = 
        "使用 pprof 进行 Go 程序性能分析"
    
    override fun getIcon(): Icon = AllIcons.Actions.Profile
    
    override fun getId(): String = "GoPprofConfiguration"
    
    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(PprofConfigurationFactory(this))
    }
}
