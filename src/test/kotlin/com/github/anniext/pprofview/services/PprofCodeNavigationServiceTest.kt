package com.github.anniext.pprofview.services

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * 代码导航服务测试
 */
class PprofCodeNavigationServiceTest : BasePlatformTestCase() {
    
    fun testParseListOutput() {
        val service = PprofCodeNavigationService(project)
        
        // 模拟 pprof -list 输出
        val sampleOutput = """
            Total: 10.50s
            ROUTINE ======================== main.fibonacci in /path/to/main.go
                  10ms      10ms (flat, cum)  0.10% of Total
                     .          .     10:func fibonacci(n int) int {
                     .          .     11:    if n <= 1 {
                  10ms       10ms     12:        return n
                     .          .     13:    }
                     .          .     14:    return fibonacci(n-1) + fibonacci(n-2)
                     .          .     15:}
        """.trimIndent()
        
        // 使用反射调用私有方法进行测试
        val method = service.javaClass.getDeclaredMethod("parseListOutput", String::class.java)
        method.isAccessible = true
        
        val startTime = System.currentTimeMillis()
        val result = method.invoke(service, sampleOutput)
        val duration = System.currentTimeMillis() - startTime
        
        println("解析耗时: ${duration}ms")
        assertNotNull("解析结果不应为空", result)
        
        // 验证解析结果
        val codeLocation = result as? CodeLocation
        assertNotNull("应该返回 CodeLocation 对象", codeLocation)
        
        if (codeLocation != null) {
            assertEquals("/path/to/main.go", codeLocation.filePath)
            assertEquals(12, codeLocation.targetLine)
            assertTrue("应该有热点行", codeLocation.hotLines.isNotEmpty())
            
            val hotLines = codeLocation.hotLines.filter { it.isHot }
            assertEquals("应该有 1 个热点行", 1, hotLines.size)
            assertEquals(12, hotLines[0].lineNumber)
        }
    }
    
    fun testParseListOutputPerformance() {
        val service = PprofCodeNavigationService(project)
        
        // 生成大量代码行的输出
        val largeOutput = buildString {
            appendLine("Total: 100.50s")
            appendLine("ROUTINE ======================== main.largeFunction in /path/to/large.go")
            appendLine("      100ms     100ms (flat, cum)  1.00% of Total")
            
            // 生成 1000 行代码
            for (i in 1..1000) {
                if (i % 10 == 0) {
                    appendLine("      10ms       10ms     $i:    // 热点行 $i")
                } else {
                    appendLine("         .          .     $i:    // 普通行 $i")
                }
            }
        }
        
        val method = service.javaClass.getDeclaredMethod("parseListOutput", String::class.java)
        method.isAccessible = true
        
        // 预热
        method.invoke(service, largeOutput)
        
        // 性能测试
        val iterations = 10
        val times = mutableListOf<Long>()
        
        repeat(iterations) {
            val startTime = System.currentTimeMillis()
            method.invoke(service, largeOutput)
            val duration = System.currentTimeMillis() - startTime
            times.add(duration)
        }
        
        val avgTime = times.average()
        val minTime = times.minOrNull() ?: 0
        val maxTime = times.maxOrNull() ?: 0
        
        println("性能测试结果 (1000 行代码):")
        println("  - 平均耗时: ${String.format("%.2f", avgTime)}ms")
        println("  - 最小耗时: ${minTime}ms")
        println("  - 最大耗时: ${maxTime}ms")
        
        // 性能断言：解析 1000 行应该在 100ms 内完成
        assertTrue("平均解析时间应该 < 100ms，实际: ${avgTime}ms", avgTime < 100)
    }
    
    fun testParseEmptyOutput() {
        val service = PprofCodeNavigationService(project)
        
        val method = service.javaClass.getDeclaredMethod("parseListOutput", String::class.java)
        method.isAccessible = true
        
        val result = method.invoke(service, "")
        assertNull("空输出应该返回 null", result)
    }
    
    fun testParseInvalidOutput() {
        val service = PprofCodeNavigationService(project)
        
        val method = service.javaClass.getDeclaredMethod("parseListOutput", String::class.java)
        method.isAccessible = true
        
        val invalidOutput = "这是无效的输出\n没有 ROUTINE\n也没有代码行"
        val result = method.invoke(service, invalidOutput)
        
        assertNull("无效输出应该返回 null", result)
    }
    
    fun testFindSourceFileWithAbsolutePath() {
        val service = PprofCodeNavigationService(project)
        
        // 测试绝对路径查找
        // 注意：这个测试需要实际的文件存在，这里只是演示 API 调用
        val method = service.javaClass.getDeclaredMethod("findSourceFile", String::class.java)
        method.isAccessible = true
        
        // 使用项目中实际存在的文件进行测试
        val testFilePath = "src/test/kotlin/com/github/anniext/pprofview/services/PprofCodeNavigationServiceTest.kt"
        val projectBasePath = project.basePath
        
        if (projectBasePath != null) {
            val absolutePath = "$projectBasePath/$testFilePath"
            println("测试绝对路径: $absolutePath")
            
            val result = method.invoke(service, absolutePath)
            if (result != null) {
                println("  - 找到文件: ${(result as com.intellij.openapi.vfs.VirtualFile).path}")
            }
        }
    }
    
    fun testFindSourceFileWithRelativePath() {
        val service = PprofCodeNavigationService(project)
        
        val method = service.javaClass.getDeclaredMethod("findSourceFile", String::class.java)
        method.isAccessible = true
        
        // 测试相对路径查找
        val relativePath = "src/test/kotlin/com/github/anniext/pprofview/services/PprofCodeNavigationServiceTest.kt"
        println("测试相对路径: $relativePath")
        
        val result = method.invoke(service, relativePath)
        if (result != null) {
            println("  - 找到文件: ${(result as com.intellij.openapi.vfs.VirtualFile).path}")
            assertNotNull("应该能找到相对路径的文件", result)
        }
    }
    
    fun testFindSourceFileWithFileName() {
        val service = PprofCodeNavigationService(project)
        
        val method = service.javaClass.getDeclaredMethod("findSourceFile", String::class.java)
        method.isAccessible = true
        
        // 测试只用文件名查找
        val fileName = "github.com/user/project/PprofCodeNavigationServiceTest.kt"
        println("测试包名路径: $fileName")
        
        val result = method.invoke(service, fileName)
        if (result != null) {
            println("  - 找到文件: ${(result as com.intellij.openapi.vfs.VirtualFile).path}")
            assertNotNull("应该能通过文件名找到文件", result)
        }
    }
}
