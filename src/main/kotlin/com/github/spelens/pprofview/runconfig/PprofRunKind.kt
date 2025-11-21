package com.github.spelens.pprofview.runconfig

/**
 * pprof run kind
 * 
 * Reference Go plugin's run configuration types
 */
enum class PprofRunKind(val messageKey: String, val descriptionKey: String) {
    /**
     * Directory - run main package in specified directory
     */
    DIRECTORY(
        "pprof.runKind.directory",
        "pprof.runKind.directory.description"
    ),
    
    /**
     * File - run specified Go file
     */
    FILE(
        "pprof.runKind.file",
        "pprof.runKind.file.description"
    ),
    
    /**
     * Package - run specified Go package
     */
    PACKAGE(
        "pprof.runKind.package",
        "pprof.runKind.package.description"
    );
    
    val displayName: String
        get() = com.github.spelens.pprofview.PprofViewBundle.message(messageKey)
    
    val description: String
        get() = com.github.spelens.pprofview.PprofViewBundle.message(descriptionKey)
    
    companion object {
        fun fromString(value: String?): PprofRunKind {
            return entries.find { it.name == value } ?: FILE
        }
    }
}
