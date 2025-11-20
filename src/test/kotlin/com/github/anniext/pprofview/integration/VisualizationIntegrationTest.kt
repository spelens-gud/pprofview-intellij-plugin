package com.github.anniext.pprofview.integration

import com.github.anniext.pprofview.parser.PprofTextParser
import com.github.anniext.pprofview.ui.PprofChartPanel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * 可视化集成测试
 */
class VisualizationIntegrationTest : BasePlatformTestCase() {
    
    fun testParseCpuReportAndCreateChart() {
        // 读取测试数据
        val testDataFile = File("src/test/testData/sample_cpu_report.txt")
        assertTrue("测试数据文件不存在", testDataFile.exists())
        
        val content = testDataFile.readText()
        
        // 解析报告
        val parser = PprofTextParser()
        val report = parser.parse(content)
        
        // 验证解析结果
        assertEquals(10, report.entries.size)
        assertEquals("cpu", report.unit)
        
        // 验证第一个条目
        val firstEntry = report.entries[0]
        assertEquals("main.fibonacci", firstEntry.functionName)
        assertEquals(36.84, firstEntry.flatPercent, 0.01)
        
        // 创建图表面板（验证不会抛出异常）
        val chartPanel = PprofChartPanel(report, project, null)
        assertNotNull(chartPanel)
    }
    
    fun testParseHeapReportAndCreateChart() {
        // 读取测试数据
        val testDataFile = File("src/test/testData/sample_heap_report.txt")
        assertTrue("测试数据文件不存在", testDataFile.exists())
        
        val content = testDataFile.readText()
        
        // 解析报告
        val parser = PprofTextParser()
        val report = parser.parse(content)
        
        // 验证解析结果
        assertEquals(10, report.entries.size)
        assertEquals("alloc_space", report.unit)
        
        // 验证第一个条目
        val firstEntry = report.entries[0]
        assertEquals("main.allocateMemory", firstEntry.functionName)
        assertTrue(firstEntry.flat > 0)
        
        // 创建图表面板（验证不会抛出异常）
        val chartPanel = PprofChartPanel(report, project, null)
        assertNotNull(chartPanel)
    }
    
    fun testParseEmptyReport() {
        val parser = PprofTextParser()
        val report = parser.parse("")
        
        assertTrue(report.entries.isEmpty())
        assertEquals(0L, report.totalSamples)
    }
    
    fun testParseInvalidReport() {
        val parser = PprofTextParser()
        val report = parser.parse("这是无效的数据\n随机文本\n123456")
        
        // 应该能够处理无效数据而不崩溃
        assertNotNull(report)
    }
}
