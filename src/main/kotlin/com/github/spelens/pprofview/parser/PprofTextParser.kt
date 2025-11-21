package com.github.spelens.pprofview.parser

import com.intellij.openapi.diagnostic.thisLogger

/**
 * pprof 文本报告解析器
 * 解析 go tool pprof -text 输出的文本格式数据
 */
class PprofTextParser {
    private val logger = thisLogger()
    
    /**
     * 解析文本报告
     */
    fun parse(text: String): PprofTextReport {
        val lines = text.lines().filter { it.isNotBlank() }
        val entries = mutableListOf<PprofEntry>()
        
        var totalSamples = 0L
        var unit = "samples"
        
        // 解析每一行
        for (line in lines) {
            // 跳过标题行和分隔符
            if (line.startsWith("Showing") || 
                line.startsWith("File:") ||
                line.startsWith("Type:") ||
                line.startsWith("Time:") ||
                line.startsWith("Duration:") ||
                line.contains("---") ||
                line.trim().isEmpty()) {
                
                // 尝试从 Type 行提取单位
                if (line.startsWith("Type:")) {
                    val parts = line.split(":")
                    if (parts.size > 1) {
                        unit = parts[1].trim()
                    }
                }
                continue
            }
            
            // 解析数据行
            val entry = parseEntry(line)
            if (entry != null) {
                entries.add(entry)
                totalSamples += entry.flat
            }
        }
        
        logger.info("解析完成: ${entries.size} 条记录, 总计 $totalSamples $unit")
        
        return PprofTextReport(
            entries = entries,
            totalSamples = totalSamples,
            unit = unit
        )
    }
    
    /**
     * 解析单行数据
     * 格式示例: "      flat  flat%   sum%        cum   cum%"
     *          "    10.50s 52.50% 52.50%     10.50s 52.50%  main.fibonacci"
     */
    private fun parseEntry(line: String): PprofEntry? {
        try {
            // 使用正则表达式解析
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
            logger.warn("解析行失败: $line", e)
            return null
        }
    }
    
    /**
     * 解析数值 (支持带单位的值，如 10.50s, 1024MB)
     */
    private fun parseValue(str: String): Long {
        try {
            // 移除单位后缀
            val numStr = str.replace(Regex("[a-zA-Z%]+"), "")
            val value = numStr.toDoubleOrNull() ?: 0.0
            
            // 根据单位转换
            return when {
                str.endsWith("s") -> (value * 1000).toLong() // 秒转毫秒
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
     * 解析百分比
     */
    private fun parsePercent(str: String): Double {
        return str.replace("%", "").toDoubleOrNull() ?: 0.0
    }
}

/**
 * pprof 文本报告数据模型
 */
data class PprofTextReport(
    val entries: List<PprofEntry>,
    val totalSamples: Long,
    val unit: String
)

/**
 * pprof 条目
 */
data class PprofEntry(
    val flat: Long,           // 函数自身耗时
    val flatPercent: Double,  // 函数自身耗时百分比
    val sumPercent: Double,   // 累计百分比
    val cum: Long,            // 函数及其调用的总耗时
    val cumPercent: Double,   // 函数及其调用的总耗时百分比
    val functionName: String  // 函数名
)
