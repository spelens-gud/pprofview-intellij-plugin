package com.github.anniext.pprofview.editor

import com.github.anniext.pprofview.services.HotLine
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.colors.EditorFontType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Rectangle

/**
 * pprof 性能数据 Inlay 渲染器
 * 在代码行尾显示性能分析数据
 */
class PprofInlayRenderer(
    private val text: String,
    private val hotLine: HotLine
) : EditorCustomElementRenderer {
    
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val fontMetrics = editor.contentComponent.getFontMetrics(getFont(editor))
        return fontMetrics.stringWidth(text) + 16 // 添加左右边距
    }
    
    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        
        // 设置抗锯齿
        (g as? java.awt.Graphics2D)?.setRenderingHint(
            java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
            java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )
        
        // 根据性能数据强度选择颜色
        val (bgColor, fgColor) = getColors()
        
        // 绘制圆角背景
        g.color = bgColor
        g.fillRoundRect(
            targetRegion.x + 4,
            targetRegion.y + 2,
            targetRegion.width - 8,
            targetRegion.height - 4,
            6,
            6
        )
        
        // 绘制边框
        g.color = fgColor.darker()
        (g as? java.awt.Graphics2D)?.stroke = java.awt.BasicStroke(1.0f)
        g.drawRoundRect(
            targetRegion.x + 4,
            targetRegion.y + 2,
            targetRegion.width - 8,
            targetRegion.height - 4,
            6,
            6
        )
        
        // 绘制文本
        g.color = fgColor
        g.font = getFont(editor)
        
        val fontMetrics = g.fontMetrics
        val textX = targetRegion.x + 8
        val textY = targetRegion.y + fontMetrics.ascent + (targetRegion.height - fontMetrics.height) / 2
        
        g.drawString(text, textX, textY)
    }
    
    /**
     * 获取字体
     */
    private fun getFont(editor: Editor): Font {
        val baseFont = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        return baseFont.deriveFont(Font.ITALIC, baseFont.size * 0.9f)
    }
    
    /**
     * 根据热点数据获取颜色
     * 返回 Pair<背景色, 前景色>
     */
    private fun getColors(): Pair<Color, Color> {
        val flatValue = parsePerformanceValue(hotLine.flat)
        val cumValue = parsePerformanceValue(hotLine.cum)
        val maxValue = maxOf(flatValue, cumValue)
        
        return when {
            maxValue >= 100 -> {
                // 高热点：红色
                JBColor(
                    Color(255, 235, 238, 200),  // 浅色主题
                    Color(100, 45, 50, 180)     // 深色主题
                ) to JBColor(
                    Color(198, 40, 40),         // 浅色主题
                    Color(239, 154, 154)        // 深色主题
                )
            }
            maxValue >= 10 -> {
                // 中热点：橙色
                JBColor(
                    Color(255, 243, 224, 200),  // 浅色主题
                    Color(100, 75, 45, 180)     // 深色主题
                ) to JBColor(
                    Color(230, 81, 0),          // 浅色主题
                    Color(255, 183, 77)         // 深色主题
                )
            }
            maxValue > 0 -> {
                // 低热点：黄色
                JBColor(
                    Color(255, 248, 225, 200),  // 浅色主题
                    Color(100, 90, 45, 180)     // 深色主题
                ) to JBColor(
                    Color(245, 124, 0),         // 浅色主题
                    Color(255, 213, 79)         // 深色主题
                )
            }
            else -> {
                // 默认：灰色
                JBColor(
                    Color(245, 245, 245, 200),  // 浅色主题
                    Color(60, 60, 60, 180)      // 深色主题
                ) to JBColor(
                    Color(117, 117, 117),       // 浅色主题
                    Color(189, 189, 189)        // 深色主题
                )
            }
        }
    }
    
    /**
     * 解析性能数据值
     */
    private fun parsePerformanceValue(value: String): Double {
        if (value == ".") return 0.0
        
        try {
            val numStr = value.replace(Regex("[a-zA-Z%]"), "").trim()
            return numStr.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            return 0.0
        }
    }
}
