package com.github.spelens.pprofview.editor

import com.github.spelens.pprofview.services.HotLine
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
 * pprof performance data Inlay renderer
 * Displays performance analysis data at the end of code lines
 */
class PprofInlayRenderer(
    private val text: String,
    private val hotLine: HotLine
) : EditorCustomElementRenderer {
    
    override fun calcWidthInPixels(inlay: Inlay<*>): Int {
        val editor = inlay.editor
        val fontMetrics = editor.contentComponent.getFontMetrics(getFont(editor))
        return fontMetrics.stringWidth(text) + 16 // Add left and right padding
    }
    
    override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
        val editor = inlay.editor
        
        // Enable anti-aliasing
        (g as? java.awt.Graphics2D)?.setRenderingHint(
            java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
            java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )
        
        // Choose colors based on performance data intensity
        val (bgColor, fgColor) = getColors()
        
        // Draw rounded background
        g.color = bgColor
        g.fillRoundRect(
            targetRegion.x + 4,
            targetRegion.y + 2,
            targetRegion.width - 8,
            targetRegion.height - 4,
            6,
            6
        )
        
        // Draw border
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
        
        // Draw text
        g.color = fgColor
        g.font = getFont(editor)
        
        val fontMetrics = g.fontMetrics
        val textX = targetRegion.x + 8
        val textY = targetRegion.y + fontMetrics.ascent + (targetRegion.height - fontMetrics.height) / 2
        
        g.drawString(text, textX, textY)
    }
    
    /**
     * Get font for rendering
     */
    private fun getFont(editor: Editor): Font {
        val baseFont = editor.colorsScheme.getFont(EditorFontType.PLAIN)
        return baseFont.deriveFont(Font.ITALIC, baseFont.size * 0.9f)
    }
    
    /**
     * Get colors based on hotspot data
     * Returns Pair<background color, foreground color>
     */
    private fun getColors(): Pair<Color, Color> {
        val flatValue = parsePerformanceValue(hotLine.flat)
        val cumValue = parsePerformanceValue(hotLine.cum)
        val maxValue = maxOf(flatValue, cumValue)
        
        return when {
            maxValue >= 100 -> {
                // High hotspot: red
                JBColor(
                    Color(255, 235, 238, 200),  // Light theme
                    Color(100, 45, 50, 180)     // Dark theme
                ) to JBColor(
                    Color(198, 40, 40),         // Light theme
                    Color(239, 154, 154)        // Dark theme
                )
            }
            maxValue >= 10 -> {
                // Medium hotspot: orange
                JBColor(
                    Color(255, 243, 224, 200),  // Light theme
                    Color(100, 75, 45, 180)     // Dark theme
                ) to JBColor(
                    Color(230, 81, 0),          // Light theme
                    Color(255, 183, 77)         // Dark theme
                )
            }
            maxValue > 0 -> {
                // Low hotspot: yellow
                JBColor(
                    Color(255, 248, 225, 200),  // Light theme
                    Color(100, 90, 45, 180)     // Dark theme
                ) to JBColor(
                    Color(245, 124, 0),         // Light theme
                    Color(255, 213, 79)         // Dark theme
                )
            }
            else -> {
                // Default: gray
                JBColor(
                    Color(245, 245, 245, 200),  // Light theme
                    Color(60, 60, 60, 180)      // Dark theme
                ) to JBColor(
                    Color(117, 117, 117),       // Light theme
                    Color(189, 189, 189)        // Dark theme
                )
            }
        }
    }
    
    /**
     * Parse performance data value
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
