package com.github.spelens.pprofview.parser

import com.intellij.openapi.diagnostic.thisLogger

/**
 * pprof text report parser
 * Parses text format data output by go tool pprof -text
 */
class PprofTextParser {
    private val logger = thisLogger()
    
    /**
     * Parse text report
     */
    fun parse(text: String): PprofTextReport {
        val lines = text.lines().filter { it.isNotBlank() }
        val entries = mutableListOf<PprofEntry>()
        
        var totalSamples = 0L
        var unit = "samples"
        
        // Parse each line
        for (line in lines) {
            // Skip header lines and separators
            if (line.startsWith("Showing") || 
                line.startsWith("File:") ||
                line.startsWith("Type:") ||
                line.startsWith("Time:") ||
                line.startsWith("Duration:") ||
                line.contains("---") ||
                line.trim().isEmpty()) {
                
                // Try to extract unit from Type line
                if (line.startsWith("Type:")) {
                    val parts = line.split(":")
                    if (parts.size > 1) {
                        unit = parts[1].trim()
                    }
                }
                continue
            }
            
            // Parse data line
            val entry = parseEntry(line)
            if (entry != null) {
                entries.add(entry)
                totalSamples += entry.flat
            }
        }
        
        logger.info("Parsing completed: ${entries.size} entries, total $totalSamples $unit")
        
        return PprofTextReport(
            entries = entries,
            totalSamples = totalSamples,
            unit = unit
        )
    }
    
    /**
     * Parse single line data
     * Format example: "      flat  flat%   sum%        cum   cum%"
     *                 "    10.50s 52.50% 52.50%     10.50s 52.50%  main.fibonacci"
     */
    private fun parseEntry(line: String): PprofEntry? {
        try {
            // Parse using regex
            val pattern = """^\s*(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(\S+)\s+(.+)$""".toRegex()
            val match = pattern.find(line) ?: return null
            
            val (flatStr, flatPercentStr, sumPercentStr, cumStr, cumPercentStr, functionName) = match.destructured
            
            return PprofEntry(
                flat = parseValue(flatStr),
                flatPercent = parsePercent(flatPercentStr),
                sumPercent = parsePercent(sumPercentStr),
                cum = parseValue(cumStr),
                cumPercent = parsePercent(cumPercentStr),
                functionName = functionName.trim()
            )
        } catch (e: Exception) {
            logger.warn("Failed to parse line: $line", e)
            return null
        }
    }
    
    /**
     * Parse numeric value (supports values with units, e.g. 10.50s, 1024MB)
     */
    private fun parseValue(str: String): Long {
        try {
            // Remove unit suffix
            val numStr = str.replace(Regex("[a-zA-Z%]+"), "")
            val value = numStr.toDoubleOrNull() ?: 0.0
            
            // Convert based on unit
            return when {
                str.endsWith("s") -> (value * 1000).toLong() // seconds to milliseconds
                str.endsWith("ms") -> value.toLong()
                str.endsWith("MB") -> (value * 1024 * 1024).toLong()
                str.endsWith("KB") -> (value * 1024).toLong()
                str.endsWith("B") -> value.toLong()
                else -> value.toLong()
            }
        } catch (e: Exception) {
            return 0L
        }
    }
    
    /**
     * Parse percentage
     */
    private fun parsePercent(str: String): Double {
        return str.replace("%", "").toDoubleOrNull() ?: 0.0
    }
}

/**
 * pprof text report data model
 */
data class PprofTextReport(
    val entries: List<PprofEntry>,
    val totalSamples: Long,
    val unit: String
)

/**
 * pprof entry
 */
data class PprofEntry(
    val flat: Long,           // Function's own time
    val flatPercent: Double,  // Function's own time percentage
    val sumPercent: Double,   // Cumulative percentage
    val cum: Long,            // Total time of function and its calls
    val cumPercent: Double,   // Total time percentage of function and its calls
    val functionName: String  // Function name
)
