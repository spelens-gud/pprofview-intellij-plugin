package com.github.spelens.pprofview.runconfig

/**
 * pprof data collection mode
 */
enum class PprofCollectionMode(val messageKey: String, val descriptionKey: String) {
    /**
     * Runtime sampling - automatic sampling using runtime/pprof package
     */
    RUNTIME_SAMPLING(
        "pprof.collectionMode.runtime",
        "pprof.collectionMode.runtime.description"
    ),

    /**
     * HTTP server - start pprof HTTP server
     */
    HTTP_SERVER(
        "pprof.collectionMode.http",
        "pprof.collectionMode.http.description"
    ),

    /**
     * Test sampling - sampling during go test
     */
    TEST_SAMPLING(
        "pprof.collectionMode.test",
        "pprof.collectionMode.test.description"
    );
    
    val displayName: String
        get() = com.github.spelens.pprofview.PprofViewBundle.message(messageKey)
    
    val description: String
        get() = com.github.spelens.pprofview.PprofViewBundle.message(descriptionKey)

    companion object {
        fun fromString(value: String?): PprofCollectionMode {
            return entries.find { it.name == value } ?: RUNTIME_SAMPLING
        }
    }
}
