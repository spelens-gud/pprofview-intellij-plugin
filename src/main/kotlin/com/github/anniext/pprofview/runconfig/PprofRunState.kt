package com.github.anniext.pprofview.runconfig

import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment

/**
 * Pprof 运行状态
 */
class PprofRunState(
    environment: ExecutionEnvironment,
    private val configuration: PprofConfiguration
) : CommandLineState(environment) {
    
    override fun startProcess(): ProcessHandler {
        val commandLine = GeneralCommandLine()
        commandLine.exePath = "go"
        commandLine.addParameter("run")
        commandLine.addParameter(configuration.goFilePath)
        
        if (configuration.workingDirectory.isNotEmpty()) {
            commandLine.setWorkDirectory(configuration.workingDirectory)
        }
        
        // 添加程序参数
        if (configuration.programArguments.isNotEmpty()) {
            commandLine.addParameters(configuration.programArguments.split(" "))
        }
        
        // 添加环境变量
        if (configuration.environmentVariables.isNotEmpty()) {
            configuration.environmentVariables.split(";").forEach { envVar ->
                val parts = envVar.split("=", limit = 2)
                if (parts.size == 2) {
                    commandLine.environment[parts[0]] = parts[1]
                }
            }
        }
        
        // 添加 pprof 相关的环境变量
        if (configuration.enablePprof) {
            commandLine.environment["GOMEMPROFILERATE"] = configuration.memProfileRate.toString()
            // 可以添加更多 pprof 相关的配置
        }
        
        val processHandler = ProcessHandlerFactory.getInstance().createColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }
}
