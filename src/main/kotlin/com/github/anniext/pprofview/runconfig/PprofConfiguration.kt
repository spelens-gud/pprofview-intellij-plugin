package com.github.anniext.pprofview.runconfig

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element

/**
 * Pprof 运行配置
 */
class PprofConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<PprofRunState>(project, factory, name) {
    
    // 配置选项
    var goFilePath: String = ""
    var workingDirectory: String = project.basePath ?: ""
    var programArguments: String = ""
    var environmentVariables: String = ""
    
    // Pprof 选项
    var enablePprof: Boolean = true
    var collectionMode: String = PprofCollectionMode.RUNTIME_SAMPLING.name
    var profileTypes: String = PprofProfileType.CPU.name
    var outputDirectory: String = ""
    var cpuDuration: Int = 30
    var httpPort: Int = 6060
    var autoOpenResult: Boolean = true
    var customBuildFlags: String = ""
    var memProfileRate: Int = 524288
    var mutexProfileFraction: Int = 1
    var blockProfileRate: Int = 1
    
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return PprofConfigurationEditor()
    }
    
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return PprofRunState(environment, this)
    }
    
    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.getChild("pprof-config")?.let { pprofElement ->
            goFilePath = pprofElement.getAttributeValue("goFilePath") ?: goFilePath
            workingDirectory = pprofElement.getAttributeValue("workingDirectory") ?: workingDirectory
            programArguments = pprofElement.getAttributeValue("programArguments") ?: programArguments
            environmentVariables = pprofElement.getAttributeValue("environmentVariables") ?: environmentVariables
            
            enablePprof = pprofElement.getAttributeValue("enablePprof")?.toBoolean() ?: enablePprof
            collectionMode = pprofElement.getAttributeValue("collectionMode") ?: collectionMode
            profileTypes = pprofElement.getAttributeValue("profileTypes") ?: profileTypes
            outputDirectory = pprofElement.getAttributeValue("outputDirectory") ?: outputDirectory
            cpuDuration = pprofElement.getAttributeValue("cpuDuration")?.toIntOrNull() ?: cpuDuration
            httpPort = pprofElement.getAttributeValue("httpPort")?.toIntOrNull() ?: httpPort
            autoOpenResult = pprofElement.getAttributeValue("autoOpenResult")?.toBoolean() ?: autoOpenResult
            customBuildFlags = pprofElement.getAttributeValue("customBuildFlags") ?: customBuildFlags
            memProfileRate = pprofElement.getAttributeValue("memProfileRate")?.toIntOrNull() ?: memProfileRate
            mutexProfileFraction = pprofElement.getAttributeValue("mutexProfileFraction")?.toIntOrNull() ?: mutexProfileFraction
            blockProfileRate = pprofElement.getAttributeValue("blockProfileRate")?.toIntOrNull() ?: blockProfileRate
        }
    }
    
    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        val pprofElement = Element("pprof-config")
        pprofElement.setAttribute("goFilePath", goFilePath)
        pprofElement.setAttribute("workingDirectory", workingDirectory)
        pprofElement.setAttribute("programArguments", programArguments)
        pprofElement.setAttribute("environmentVariables", environmentVariables)
        
        pprofElement.setAttribute("enablePprof", enablePprof.toString())
        pprofElement.setAttribute("collectionMode", collectionMode)
        pprofElement.setAttribute("profileTypes", profileTypes)
        pprofElement.setAttribute("outputDirectory", outputDirectory)
        pprofElement.setAttribute("cpuDuration", cpuDuration.toString())
        pprofElement.setAttribute("httpPort", httpPort.toString())
        pprofElement.setAttribute("autoOpenResult", autoOpenResult.toString())
        pprofElement.setAttribute("customBuildFlags", customBuildFlags)
        pprofElement.setAttribute("memProfileRate", memProfileRate.toString())
        pprofElement.setAttribute("mutexProfileFraction", mutexProfileFraction.toString())
        pprofElement.setAttribute("blockProfileRate", blockProfileRate.toString())
        element.addContent(pprofElement)
    }
}
