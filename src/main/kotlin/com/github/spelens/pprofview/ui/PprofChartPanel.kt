package com.github.spelens.pprofview.ui

import com.github.spelens.pprofview.PprofViewBundle
import com.github.spelens.pprofview.parser.PprofTextReport
import com.github.spelens.pprofview.services.PprofCodeNavigationService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * pprof chart panel
 * Used for visualizing pprof data
 */
class PprofChartPanel(
    private val report: PprofTextReport,
    private val project: Project? = null,
    private val pprofFile: VirtualFile? = null
) : JBPanel<PprofChartPanel>(BorderLayout()) {
    
    init {
        // Create tabbed pane
        val tabbedPane = JTabbedPane()
        
        // Add table view (first tab)
        tabbedPane.addTab(PprofViewBundle.message("pprof.chart.detailedData"), createTablePanel())
        
        // Add bar chart
        tabbedPane.addTab(PprofViewBundle.message("pprof.chart.barChart"), createBarChartPanel())
        
        // Add pie chart
        tabbedPane.addTab(PprofViewBundle.message("pprof.chart.pieChart"), createPieChartPanel())
        
        // Add heatmap
        tabbedPane.addTab(PprofViewBundle.message("pprof.chart.heatmap"), createHeatmapPanel())
        
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * Create bar chart panel
     */
    private fun createBarChartPanel(): JComponent {
        val panel = object : JPanel() {
            private var hoveredBarIndex = -1
            
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawBarChart(g as Graphics2D, hoveredBarIndex)
            }
            
            init {
                // Add mouse motion listener for hover effect
                addMouseMotionListener(object : MouseAdapter() {
                    override fun mouseMoved(e: MouseEvent) {
                        val newHoveredIndex = getBarIndexAt(e.x, e.y)
                        if (newHoveredIndex != hoveredBarIndex) {
                            hoveredBarIndex = newHoveredIndex
                            repaint()
                            
                            // Update tooltip
                            toolTipText = if (hoveredBarIndex >= 0) {
                                buildBarTooltip(hoveredBarIndex)
                            } else {
                                null
                            }
                        }
                    }
                })
                
                // Add mouse click listener for navigation
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        val barIndex = getBarIndexAt(e.x, e.y)
                        if (barIndex >= 0) {
                            val entry = report.entries[barIndex]
                            navigateToCode(entry.functionName)
                        }
                    }
                    
                    override fun mouseExited(e: MouseEvent) {
                        if (hoveredBarIndex != -1) {
                            hoveredBarIndex = -1
                            repaint()
                            toolTipText = null
                        }
                    }
                })
            }
            
            /**
             * Get bar index at mouse position
             */
            private fun getBarIndexAt(mouseX: Int, mouseY: Int): Int {
                val width = this.width
                val height = this.height
                
                // Use same calculation logic as drawBarChart
                val leftMargin = when {
                    width < 400 -> 35
                    width < 600 -> 45
                    else -> maxOf(50, width / 20)
                }
                val rightMargin = when {
                    width < 400 -> 10
                    width < 600 -> 15
                    else -> 20
                }
                val topMargin = 60
                val bottomMargin = when {
                    width < 500 -> 40
                    width < 700 -> 50
                    else -> 60
                }
                
                val topCount = when {
                    width < 400 -> 5
                    width < 600 -> 8
                    width < 900 -> 12
                    else -> 15
                }
                
                val chartWidth = width - leftMargin - rightMargin
                val chartHeight = height - topMargin - bottomMargin
                
                val topEntries = report.entries.take(topCount)
                if (topEntries.isEmpty()) return -1
                
                val barWidth = chartWidth / topEntries.size
                val barActualWidth = maxOf(8, (barWidth * 0.7).toInt())
                val maxValue = topEntries.maxOfOrNull { it.flat } ?: 1L
                
                topEntries.forEachIndexed { index, entry ->
                    val barHeight = maxOf(2, (entry.flat.toDouble() / maxValue * chartHeight).toInt())
                    val x = leftMargin + index * barWidth + (barWidth - barActualWidth) / 2
                    val y = height - bottomMargin - barHeight
                    
                    if (mouseX >= x && mouseX <= x + barActualWidth &&
                        mouseY >= y && mouseY <= height - bottomMargin) {
                        return index
                    }
                }
                
                return -1
            }
            
            /**
             * Build bar chart tooltip
             */
            private fun buildBarTooltip(index: Int): String {
                val entry = report.entries[index]
                return buildString {
                    append("<html>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.functionPerformanceDetails")}</b><br>")
                    append("<hr>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.rank")}：</b> #${index + 1}<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.functionName")}：</b> ${entry.functionName}<br>")
                    append("<hr>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.flat")}：</b> ${formatValue(entry.flat)} ${report.unit} (${String.format("%.2f%%", entry.flatPercent)})<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.cum")}：</b> ${formatValue(entry.cum)} ${report.unit} (${String.format("%.2f%%", entry.cumPercent)})<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.sumPercent")}：</b> ${String.format("%.2f%%", entry.sumPercent)}<br>")
                    append("<hr>")
                    append("<i>${PprofViewBundle.message("pprof.chart.tooltip.clickToNavigate")}</i>")
                    append("</html>")
                }
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * Draw bar chart
     */
    private fun drawBarChart(g: Graphics2D, hoveredBarIndex: Int = -1) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        
        // Dynamically adjust margins and entry count based on window width
        val leftMargin = when {
            width < 400 -> 35
            width < 600 -> 45
            else -> maxOf(50, width / 20)
        }
        val rightMargin = when {
            width < 400 -> 10
            width < 600 -> 15
            else -> 20
        }
        val topMargin = 60
        val bottomMargin = when {
            width < 500 -> 40
            width < 700 -> 50
            else -> 60
        }
        
        val topCount = when {
            width < 400 -> 5
            width < 600 -> 8
            width < 900 -> 12
            else -> 15
        }
        
        val chartWidth = width - leftMargin - rightMargin
        val chartHeight = height - topMargin - bottomMargin
        
        // Take top N entries
        val topEntries = report.entries.take(topCount)
        if (topEntries.isEmpty()) return
        
        // Simple background
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // Draw title (smaller font for narrow windows)
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, if (width < 500) 13 else 16)
        val title = PprofViewBundle.message("pprof.chart.topFunctionsPerformance", topCount)
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 20)
        
        // Subtitle (hidden for narrow windows)
        if (width >= 400) {
            g.font = Font("SansSerif", Font.PLAIN, 10)
            g.color = JBColor.GRAY
            val subtitle = PprofViewBundle.message("pprof.chart.unit", report.unit)
            val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
            g.drawString(subtitle, (width - subtitleWidth) / 2, 38)
        }
        
        // Draw grid lines
        g.color = JBColor(Color(230, 230, 230), Color(60, 60, 60))
        g.stroke = BasicStroke(1f)
        val maxValue = topEntries.maxOfOrNull { it.flat } ?: 1L
        val gridLines = if (width < 500) 3 else 5
        for (i in 0..gridLines) {
            val y = height - bottomMargin - (chartHeight * i / gridLines)
            g.drawLine(leftMargin, y, width - rightMargin, y)
        }
        
        // Draw axes
        g.color = JBColor(Color(120, 120, 120), Color(160, 160, 160))
        g.stroke = BasicStroke(2f)
        g.drawLine(leftMargin, topMargin, leftMargin, height - bottomMargin) // Y axis
        g.drawLine(leftMargin, height - bottomMargin, width - rightMargin, height - bottomMargin) // X axis
        
        // Calculate bar chart parameters
        val barWidth = chartWidth / topEntries.size
        val barActualWidth = maxOf(8, (barWidth * 0.7).toInt())
        
        // Draw bar chart
        topEntries.forEachIndexed { index, entry ->
            val barHeight = maxOf(2, (entry.flat.toDouble() / maxValue * chartHeight).toInt())
            val x = leftMargin + index * barWidth + (barWidth - barActualWidth) / 2
            val y = height - bottomMargin - barHeight
            
            val isHovered = index == hoveredBarIndex
            val color = getBarColor(index)
            
            // Draw bar
            g.color = if (isHovered) color.brighter() else color
            g.fillRect(x, y, barActualWidth, barHeight)
            
            // Draw border
            g.color = color.darker()
            g.stroke = BasicStroke(if (isHovered) 2f else 1f)
            g.drawRect(x, y, barActualWidth, barHeight)
            
            // Draw value (only when bar is tall and wide enough)
            if (barHeight > 25 && barActualWidth > 20 && width >= 500) {
                g.font = Font("SansSerif", Font.BOLD, 9)
                val valueText = String.format("%.1f%%", entry.flatPercent)
                val valueWidth = g.fontMetrics.stringWidth(valueText)
                
                g.color = JBColor.foreground()
                g.drawString(valueText, x + (barActualWidth - valueWidth) / 2, y - 4)
            }
            
            // Draw function name (only when width is sufficient)
            if (barActualWidth > 15 && width >= 400) {
                g.font = Font("SansSerif", Font.PLAIN, if (width < 500) 7 else 9)
                g.color = JBColor.foreground()
                val maxLen = maxOf(6, barActualWidth / 5)
                val funcName = truncateFunctionName(entry.functionName, maxLen)
                
                val transform = g.transform
                val rotateX = x + barActualWidth / 2
                val rotateY = height - bottomMargin + 8
                g.rotate(-Math.PI / 4, rotateX.toDouble(), rotateY.toDouble())
                g.drawString(funcName, rotateX, rotateY)
                g.transform = transform
            }
        }
        
        // Draw Y axis scale
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.PLAIN, if (width < 500) 8 else 10)
        g.stroke = BasicStroke(1f)
        for (i in 0..gridLines) {
            val y = height - bottomMargin - (chartHeight * i / gridLines)
            val value = maxValue * i / gridLines
            g.drawLine(leftMargin - 4, y, leftMargin, y)
            val valueStr = formatValue(value)
            val strWidth = g.fontMetrics.stringWidth(valueStr)
            g.drawString(valueStr, leftMargin - strWidth - 6, y + 3)
        }
    }
    
    /**
     * Create pie chart panel
     */
    private fun createPieChartPanel(): JComponent {
        val panel = object : JPanel() {
            private var hoveredSliceIndex = -1
            
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawPieChart(g as Graphics2D, hoveredSliceIndex)
            }
            
            init {
                // Add mouse motion listener
                addMouseMotionListener(object : MouseAdapter() {
                    override fun mouseMoved(e: MouseEvent) {
                        val newHoveredIndex = getSliceIndexAt(e.x, e.y)
                        if (newHoveredIndex != hoveredSliceIndex) {
                            hoveredSliceIndex = newHoveredIndex
                            repaint()
                            
                            // Update tooltip
                            toolTipText = if (hoveredSliceIndex >= 0) {
                                buildPieTooltip(hoveredSliceIndex)
                            } else {
                                null
                            }
                        }
                    }
                })
                
                // Add mouse click listener
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        val sliceIndex = getSliceIndexAt(e.x, e.y)
                        if (sliceIndex >= 0) {
                            val entry = report.entries[sliceIndex]
                            navigateToCode(entry.functionName)
                        }
                    }
                    
                    override fun mouseExited(e: MouseEvent) {
                        if (hoveredSliceIndex != -1) {
                            hoveredSliceIndex = -1
                            repaint()
                            toolTipText = null
                        }
                    }
                })
            }
            
            /**
             * Get slice index at mouse position
             */
            private fun getSliceIndexAt(mouseX: Int, mouseY: Int): Int {
                val width = this.width
                val height = this.height
                val topEntries = report.entries.take(10)
                if (topEntries.isEmpty()) return -1
                
                val pieWidth = minOf(width * 0.5, height - 150.0).toInt()
                val radius = pieWidth / 2
                val centerX = width / 3
                val centerY = height / 2 + 20
                
                // Calculate mouse position relative to center
                val dx = mouseX - centerX
                val dy = mouseY - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                
                // Check if inside circle
                if (distance > radius) return -1
                
                // Calculate angle (starting from right, counterclockwise)
                var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble()))
                if (angle < 0) angle += 360
                
                // Find corresponding slice
                val total = topEntries.sumOf { it.flat }.toDouble()
                var startAngle = 0.0
                topEntries.forEachIndexed { index, entry ->
                    val sliceAngle = (entry.flat / total) * 360.0
                    val endAngle = startAngle + sliceAngle
                    
                    if (angle >= startAngle && angle < endAngle) {
                        return index
                    }
                    
                    startAngle = endAngle
                }
                
                return -1
            }
            
            /**
             * Build pie chart tooltip
             */
            private fun buildPieTooltip(index: Int): String {
                val entry = report.entries[index]
                val total = report.entries.take(10).sumOf { it.flat }
                val percentage = (entry.flat.toDouble() / total * 100)
                
                return buildString {
                    append("<html>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.functionRatioDetails")}</b><br>")
                    append("<hr>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.rank")}：</b> #${index + 1}<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.functionName")}：</b> ${entry.functionName}<br>")
                    append("<hr>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.flat")}：</b> ${formatValue(entry.flat)} ${report.unit}<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.ratio")}：</b> ${String.format("%.2f%%", percentage)}<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.cum")}：</b> ${formatValue(entry.cum)} ${report.unit} (${String.format("%.2f%%", entry.cumPercent)})<br>")
                    append("<hr>")
                    append("<i>${PprofViewBundle.message("pprof.chart.tooltip.clickToNavigate")}</i>")
                    append("</html>")
                }
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * Draw pie chart
     */
    private fun drawPieChart(g: Graphics2D, hoveredSliceIndex: Int = -1) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        
        // Simple background
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // Take top 10 entries
        val topEntries = report.entries.take(10)
        if (topEntries.isEmpty()) return
        
        // Draw title
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 16)
        val title = PprofViewBundle.message("pprof.chart.topFunctionsRatio", topEntries.size)
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 30)
        
        g.font = Font("SansSerif", Font.PLAIN, 11)
        g.color = JBColor.GRAY
        val subtitle = PprofViewBundle.message("pprof.chart.unit", report.unit)
        val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
        g.drawString(subtitle, (width - subtitleWidth) / 2, 48)
        
        // Dynamically adjust layout based on window size
        val useVerticalLayout = width < 700
        
        // Calculate pie chart position and size
        val availableWidth = if (useVerticalLayout) width - 80 else (width * 0.5).toInt()
        val availableHeight = if (useVerticalLayout) (height * 0.5).toInt() else height - 120
        val pieSize = minOf(availableWidth, availableHeight, 400)
        val radius = pieSize / 2
        
        val centerX = if (useVerticalLayout) width / 2 else width / 3
        val centerY = if (useVerticalLayout) 80 + radius else height / 2
        
        // Calculate total
        val total = topEntries.sumOf { it.flat }.toDouble()
        
        // Draw pie chart
        var startAngle = 0.0
        topEntries.forEachIndexed { index, entry ->
            val angle = (entry.flat / total) * 360.0
            val isHovered = index == hoveredSliceIndex
            
            // Offset outward when hovered
            val offsetRadius = if (isHovered) 10 else 0
            val offsetAngle = Math.toRadians(startAngle + angle / 2)
            val offsetX = (offsetRadius * Math.cos(offsetAngle)).toInt()
            val offsetY = (offsetRadius * Math.sin(offsetAngle)).toInt()
            
            // Draw slice
            val color = getBarColor(index)
            g.color = if (isHovered) color.brighter() else color
            g.fillArc(
                centerX - radius + offsetX,
                centerY - radius + offsetY,
                radius * 2,
                radius * 2,
                startAngle.toInt(),
                angle.toInt()
            )
            
            // Draw border
            g.color = color.darker()
            g.stroke = BasicStroke(if (isHovered) 2f else 1f)
            g.drawArc(
                centerX - radius + offsetX,
                centerY - radius + offsetY,
                radius * 2,
                radius * 2,
                startAngle.toInt(),
                angle.toInt()
            )
            
            // Draw percentage label (if slice is large enough)
            if (angle > 10 && radius > 80) {
                val labelAngle = Math.toRadians(startAngle + angle / 2)
                val labelRadius = radius * 0.65
                val labelX = (centerX + offsetX + labelRadius * Math.cos(labelAngle)).toInt()
                val labelY = (centerY + offsetY + labelRadius * Math.sin(labelAngle)).toInt()
                
                g.font = Font("SansSerif", Font.BOLD, 11)
                val percentText = String.format("%.1f%%", entry.flatPercent)
                val textWidth = g.fontMetrics.stringWidth(percentText)
                
                // Label background
                g.color = Color(255, 255, 255, 200)
                g.fillRect(labelX - textWidth / 2 - 3, labelY - 10, textWidth + 6, 16)
                
                // Label text
                g.color = Color.BLACK
                g.drawString(percentText, labelX - textWidth / 2, labelY + 3)
            }
            
            startAngle += angle
        }
        
        // Draw legend
        val legendX = if (useVerticalLayout) 40 else (width * 2 / 3).toInt()
        var legendY = if (useVerticalLayout) centerY + radius + 40 else 80
        val legendItemHeight = 28
        
        // Legend title
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 13)
        g.drawString(PprofViewBundle.message("pprof.chart.functionList"), legendX, legendY)
        legendY += 25
        
        topEntries.forEachIndexed { index, entry ->
            val color = getBarColor(index)
            
            // Draw color block
            g.color = color
            g.fillRect(legendX, legendY, 20, 20)
            g.color = color.darker()
            g.stroke = BasicStroke(1f)
            g.drawRect(legendX, legendY, 20, 20)
            
            // Draw rank
            g.color = Color.WHITE
            g.font = Font("SansSerif", Font.BOLD, 11)
            val rankStr = "${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankStr)
            g.drawString(rankStr, legendX + (20 - rankWidth) / 2, legendY + 15)
            
            // Draw text
            g.color = JBColor.foreground()
            g.font = Font("SansSerif", Font.BOLD, 11)
            val percentText = String.format("%.1f%%", entry.flatPercent)
            g.drawString(percentText, legendX + 28, legendY + 12)
            
            g.font = Font("SansSerif", Font.PLAIN, 9)
            g.color = JBColor.GRAY
            val maxLen = if (useVerticalLayout) 35 else 25
            val funcName = truncateFunctionName(entry.functionName, maxLen)
            g.drawString(funcName, legendX + 28, legendY + 12 + 11)
            
            legendY += legendItemHeight
        }
    }
    
    /**
     * Create table panel
     */
    private fun createTablePanel(): JComponent {
        val columnNames = arrayOf(
            PprofViewBundle.message("pprof.chart.table.rank"),
            PprofViewBundle.message("pprof.chart.table.functionName"),
            PprofViewBundle.message("pprof.chart.table.flat"),
            PprofViewBundle.message("pprof.chart.table.flatPercent"),
            PprofViewBundle.message("pprof.chart.table.sumPercent"),
            PprofViewBundle.message("pprof.chart.table.cum"),
            PprofViewBundle.message("pprof.chart.table.cumPercent")
        )
        val data = report.entries.mapIndexed { index, entry ->
            arrayOf(
                "${index + 1}",
                entry.functionName,
                formatValue(entry.flat),
                String.format("%.2f%%", entry.flatPercent),
                String.format("%.2f%%", entry.sumPercent),
                formatValue(entry.cum),
                String.format("%.2f%%", entry.cumPercent)
            )
        }.toTypedArray()
        
        val table = JTable(data, columnNames)
        table.autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
        table.font = Font("SansSerif", Font.PLAIN, 12)
        table.rowHeight = 28
        table.gridColor = JBColor.border()
        table.showVerticalLines = true
        table.showHorizontalLines = true
        
        // Set table header style
        val header = table.tableHeader
        header.font = Font("SansSerif", Font.BOLD, 12)
        header.background = JBColor.background()
        header.foreground = JBColor.foreground()
        
        // Set column widths
        table.columnModel.getColumn(0).preferredWidth = 50  // Rank
        table.columnModel.getColumn(1).preferredWidth = 400 // Function name
        table.columnModel.getColumn(2).preferredWidth = 80  // Flat
        table.columnModel.getColumn(3).preferredWidth = 70  // Flat%
        table.columnModel.getColumn(4).preferredWidth = 70  // Sum%
        table.columnModel.getColumn(5).preferredWidth = 80  // Cum
        table.columnModel.getColumn(6).preferredWidth = 70  // Cum%
        
        // Set cell renderer (add colors)
        table.setDefaultRenderer(Any::class.java, object : javax.swing.table.DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                
                if (!isSelected) {
                    // Alternating row colors
                    c.background = if (row % 2 == 0) {
                        JBColor.background()
                    } else {
                        JBColor(Color(245, 245, 245), Color(50, 50, 50))
                    }
                    
                    // Use color for rank column
                    if (column == 0 && row < 10) {
                        c.foreground = getBarColor(row)
                        font = Font("SansSerif", Font.BOLD, 12)
                    } else {
                        c.foreground = JBColor.foreground()
                        font = Font("SansSerif", Font.PLAIN, 12)
                    }
                }
                
                // Right align numeric columns
                horizontalAlignment = if (column in 2..6) SwingConstants.RIGHT else SwingConstants.LEFT
                
                // Display function name column as clickable link style
                if (column == 1 && project != null && pprofFile != null) {
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    if (!isSelected) {
                        c.foreground = JBColor(Color(0, 102, 204), Color(100, 150, 255))
                    }
                }
                
                return c
            }
        })
        
        // Add mouse listener
        if (project != null && pprofFile != null) {
            table.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val row = table.rowAtPoint(e.point)
                    val column = table.columnAtPoint(e.point)
                    
                    // Only handle clicks on function name column
                    if (row >= 0 && column == 1) {
                        val functionName = table.getValueAt(row, column) as String
                        navigateToCode(functionName)
                    }
                }
                
                override fun mouseEntered(e: MouseEvent) {
                    val column = table.columnAtPoint(e.point)
                    if (column == 1) {
                        table.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    }
                }
                
                override fun mouseExited(e: MouseEvent) {
                    table.cursor = Cursor.getDefaultCursor()
                }
            })
            
            // Add mouse motion listener for hover tooltip
            table.addMouseMotionListener(object : MouseAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    val row = table.rowAtPoint(e.point)
                    val column = table.columnAtPoint(e.point)
                    
                    if (row >= 0 && row < report.entries.size) {
                        val entry = report.entries[row]
                        table.toolTipText = buildTableTooltip(row, column, entry)
                    } else {
                        table.toolTipText = null
                    }
                }
            })
        } else {
            // Provide tooltip even without navigation functionality
            table.addMouseMotionListener(object : MouseAdapter() {
                override fun mouseMoved(e: MouseEvent) {
                    val row = table.rowAtPoint(e.point)
                    val column = table.columnAtPoint(e.point)
                    
                    if (row >= 0 && row < report.entries.size) {
                        val entry = report.entries[row]
                        table.toolTipText = buildTableTooltip(row, column, entry)
                    } else {
                        table.toolTipText = null
                    }
                }
            })
        }
        
        val scrollPane = JBScrollPane(table)
        scrollPane.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        return scrollPane
    }
    
    /**
     * Build table tooltip
     */
    private fun buildTableTooltip(row: Int, column: Int, entry: com.github.spelens.pprofview.parser.PprofEntry): String {
        return buildString {
            append("<html>")
            append("<b>${PprofViewBundle.message("pprof.chart.tooltip.performanceDataDetails")}</b><br>")
            append("<hr>")
            append("<b>${PprofViewBundle.message("pprof.chart.tooltip.rank")}：</b> #${row + 1}<br>")
            append("<b>${PprofViewBundle.message("pprof.chart.tooltip.functionName")}：</b><br>")
            append("<code>${entry.functionName}</code><br>")
            append("<hr>")
            append("<table cellpadding='2'>")
            append("<tr><td><b>${PprofViewBundle.message("pprof.chart.tooltip.flat")}：</b></td><td>${formatValue(entry.flat)} ${report.unit}</td><td>(${String.format("%.2f%%", entry.flatPercent)})</td></tr>")
            append("<tr><td><b>${PprofViewBundle.message("pprof.chart.tooltip.cum")}：</b></td><td>${formatValue(entry.cum)} ${report.unit}</td><td>(${String.format("%.2f%%", entry.cumPercent)})</td></tr>")
            append("<tr><td><b>${PprofViewBundle.message("pprof.chart.tooltip.sumPercent")}：</b></td><td colspan='2'>${String.format("%.2f%%", entry.sumPercent)}</td></tr>")
            append("</table>")
            append("<hr>")
            append("<small>")
            append("<b>${PprofViewBundle.message("pprof.chart.tooltip.description")}：</b><br>")
            append("• ${PprofViewBundle.message("pprof.chart.tooltip.flatDescription")}<br>")
            append("• ${PprofViewBundle.message("pprof.chart.tooltip.cumDescription")}<br>")
            append("• ${PprofViewBundle.message("pprof.chart.tooltip.sumPercentDescription")}")
            if (project != null && pprofFile != null && column == 1) {
                append("<br><br><i>💡 ${PprofViewBundle.message("pprof.chart.tooltip.clickToNavigate")}</i>")
            }
            append("</small>")
            append("</html>")
        }
    }
    
    /**
     * Navigate to code location
     */
    private fun navigateToCode(functionName: String) {
        if (project == null || pprofFile == null) {
            println("ERROR: project or pprofFile is null")
            println("  - project: $project")
            println("  - pprofFile: $pprofFile")
            
            // Show error notification
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("pprofview.notifications")
                .createNotification(
                    PprofViewBundle.message("pprof.chart.navigation.failed"),
                    PprofViewBundle.message("pprof.chart.navigation.missingInfo"),
                    com.intellij.notification.NotificationType.ERROR
                )
                .notify(project)
            return
        }
        
        val startTime = System.currentTimeMillis()
        println("========================================")
        println("User clicked function: $functionName")
        println("Time: ${java.time.LocalDateTime.now()}")
        println("Project: ${project.name}")
        println("pprof file: ${pprofFile.path}")
        
        try {
            val navigationService = PprofCodeNavigationService.getInstance(project)
            navigationService.navigateToFunction(pprofFile, functionName)
            
            val duration = System.currentTimeMillis() - startTime
            println("Click response total time: ${duration}ms")
        } catch (e: Exception) {
            println("ERROR: Navigation failed")
            e.printStackTrace()
            
            // Show error notification
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("pprofview.notifications")
                .createNotification(
                    PprofViewBundle.message("pprof.chart.navigation.failed"),
                    "${PprofViewBundle.message("pprof.chart.navigation.failed")}: ${e.message}",
                    com.intellij.notification.NotificationType.ERROR
                )
                .notify(project)
        }
        println("========================================")
    }
    
    /**
     * Get bar chart color
     */
    private fun getBarColor(index: Int): Color {
        val colors = arrayOf(
            Color(66, 133, 244),   // Blue
            Color(234, 67, 53),    // Red
            Color(251, 188, 5),    // Yellow
            Color(52, 168, 83),    // Green
            Color(255, 109, 0),    // Orange
            Color(156, 39, 176),   // Purple
            Color(0, 172, 193),    // Cyan
            Color(255, 87, 34),    // Deep Orange
            Color(121, 85, 72),    // Brown
            Color(158, 158, 158)   // Gray
        )
        return colors[index % colors.size]
    }
    
    /**
     * Truncate function name
     */
    private fun truncateFunctionName(name: String, maxLength: Int): String {
        if (name.length <= maxLength) return name
        
        // Try to keep only the function name part
        val parts = name.split(".")
        val funcName = parts.lastOrNull() ?: name
        
        return if (funcName.length <= maxLength) {
            funcName
        } else {
            funcName.substring(0, maxLength - 3) + "..."
        }
    }
    
    /**
     * Format value
     */
    private fun formatValue(value: Long): String {
        return when {
            value >= 1000000 -> String.format("%.2fM", value / 1000000.0)
            value >= 1000 -> String.format("%.2fK", value / 1000.0)
            else -> value.toString()
        }
    }
    
    /**
     * Create heatmap panel
     */
    private fun createHeatmapPanel(): JComponent {
        val panel = object : JPanel() {
            private var hoveredRect: TreemapRect? = null
            private val treemapRects = mutableListOf<TreemapRect>()
            
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                treemapRects.clear()
                drawHeatmap(g as Graphics2D, treemapRects)
            }
            
            init {
                // Add mouse motion listener
                addMouseMotionListener(object : MouseAdapter() {
                    override fun mouseMoved(e: MouseEvent) {
                        val newHoveredRect = treemapRects.firstOrNull { rect ->
                            e.x >= rect.x && e.x <= rect.x + rect.width &&
                            e.y >= rect.y && e.y <= rect.y + rect.height
                        }
                        
                        if (newHoveredRect != hoveredRect) {
                            hoveredRect = newHoveredRect
                            repaint()
                            
                            toolTipText = hoveredRect?.let { buildHeatmapTooltip(it) }
                        }
                    }
                })
                
                // Add mouse click listener
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        val clickedRect = treemapRects.firstOrNull { rect ->
                            e.x >= rect.x && e.x <= rect.x + rect.width &&
                            e.y >= rect.y && e.y <= rect.y + rect.height
                        }
                        
                        clickedRect?.let {
                            navigateToCode(it.entry.functionName)
                        }
                    }
                    
                    override fun mouseExited(e: MouseEvent) {
                        if (hoveredRect != null) {
                            hoveredRect = null
                            repaint()
                            toolTipText = null
                        }
                    }
                })
            }
            
            /**
             * Build heatmap tooltip
             */
            private fun buildHeatmapTooltip(rect: TreemapRect): String {
                val entry = rect.entry
                return buildString {
                    append("<html>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.functionHeatDetails")}</b><br>")
                    append("<hr>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.rank")}：</b> #${rect.index + 1}<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.functionName")}：</b><br>")
                    append("<code>${entry.functionName}</code><br>")
                    append("<hr>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.flat")}：</b> ${formatValue(entry.flat)} ${report.unit} (${String.format("%.2f%%", entry.flatPercent)})<br>")
                    append("<b>${PprofViewBundle.message("pprof.chart.tooltip.cum")}：</b> ${formatValue(entry.cum)} ${report.unit} (${String.format("%.2f%%", entry.cumPercent)})<br>")
                    append("<hr>")
                    append("<i>💡 ${PprofViewBundle.message("pprof.chart.tooltip.areaRepresentsRatio")}<br>")
                    append("${PprofViewBundle.message("pprof.chart.tooltip.colorRepresentsHeat")}<br>")
                    append("${PprofViewBundle.message("pprof.chart.tooltip.clickToNavigate")}</i>")
                    append("</html>")
                }
            }
        }
        
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * Draw heatmap (treemap)
     */
    private fun drawHeatmap(g: Graphics2D, treemapRects: MutableList<TreemapRect>) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        
        // Dynamically adjust based on window size
        val margin = maxOf(30, width / 30)
        val topCount = when {
            width < 500 -> 12
            width < 800 -> 16
            else -> 20
        }
        
        // Simple background
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // Take top N entries
        val topEntries = report.entries.take(topCount)
        if (topEntries.isEmpty()) return
        
        // Draw title
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 16)
        val title = PprofViewBundle.message("pprof.chart.topFunctionsHeatmap", topCount)
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 25)
        
        g.font = Font("SansSerif", Font.PLAIN, 10)
        g.color = JBColor.GRAY
        val subtitle = PprofViewBundle.message("pprof.chart.heatmapSubtitle")
        val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
        g.drawString(subtitle, (width - subtitleWidth) / 2, 42)
        
        // Calculate total value
        val total = topEntries.sumOf { it.flat }.toDouble()
        
        // Use simplified grid layout
        val availableWidth = width - 2 * margin
        val availableHeight = height - margin - 55
        
        layoutTreemap(
            topEntries,
            margin,
            55,
            availableWidth,
            availableHeight,
            total,
            treemapRects,
            g
        )
    }
    
    /**
     * Layout treemap
     */
    private fun layoutTreemap(
        entries: List<com.github.spelens.pprofview.parser.PprofEntry>,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        total: Double,
        rects: MutableList<TreemapRect>,
        g: Graphics2D
    ) {
        if (entries.isEmpty() || width <= 0 || height <= 0) return
        
        // Use grid layout
        val cols = Math.ceil(Math.sqrt(entries.size.toDouble())).toInt()
        val rows = Math.ceil(entries.size.toDouble() / cols).toInt()
        
        val cellWidth = width / cols
        val cellHeight = height / rows
        val padding = 3
        
        entries.forEachIndexed { index, entry ->
            val row = index / cols
            val col = index % cols
            
            val rectX = x + col * cellWidth + padding
            val rectY = y + row * cellHeight + padding
            val rectWidth = cellWidth - 2 * padding
            val rectHeight = cellHeight - 2 * padding
            
            // Save rectangle information
            val treemapRect = TreemapRect(
                x = rectX,
                y = rectY,
                width = rectWidth,
                height = rectHeight,
                entry = entry,
                index = index
            )
            rects.add(treemapRect)
            
            // Draw rectangle
            drawTreemapRect(g, treemapRect, index)
        }
    }
    
    /**
     * Draw single rectangle
     */
    private fun drawTreemapRect(g: Graphics2D, rect: TreemapRect, index: Int) {
        val entry = rect.entry
        
        // Choose color intensity based on performance data
        val baseColor = getBarColor(index)
        val intensity = (entry.flatPercent / 100.0).coerceIn(0.3, 1.0)
        val heatColor = Color(
            (baseColor.red * intensity).toInt(),
            (baseColor.green * intensity).toInt(),
            (baseColor.blue * intensity).toInt()
        )
        
        // Draw rectangle
        g.color = heatColor
        g.fillRect(rect.x, rect.y, rect.width, rect.height)
        
        // Draw border
        g.color = heatColor.darker()
        g.stroke = BasicStroke(1f)
        g.drawRect(rect.x, rect.y, rect.width, rect.height)
        
        // Draw text (adjust based on rectangle size)
        g.color = Color.WHITE
        
        if (rect.width > 80 && rect.height > 50) {
            // Large rectangle: show rank, percentage, function name
            g.font = Font("SansSerif", Font.BOLD, 14)
            val rankText = "#${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankText)
            g.drawString(rankText, rect.x + (rect.width - rankWidth) / 2, rect.y + 20)
            
            g.font = Font("SansSerif", Font.BOLD, 13)
            val percentText = String.format("%.1f%%", entry.flatPercent)
            val percentWidth = g.fontMetrics.stringWidth(percentText)
            g.drawString(percentText, rect.x + (rect.width - percentWidth) / 2, rect.y + rect.height / 2 + 5)
            
            if (rect.height > 65) {
                g.font = Font("SansSerif", Font.PLAIN, 9)
                val maxLen = maxOf(10, rect.width / 7)
                val funcName = truncateFunctionName(entry.functionName, maxLen)
                val funcWidth = g.fontMetrics.stringWidth(funcName)
                g.drawString(funcName, rect.x + (rect.width - funcWidth) / 2, rect.y + rect.height - 8)
            }
        } else if (rect.width > 45 && rect.height > 30) {
            // Medium rectangle: show rank and percentage
            g.font = Font("SansSerif", Font.BOLD, 11)
            val rankText = "#${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankText)
            g.drawString(rankText, rect.x + (rect.width - rankWidth) / 2, rect.y + rect.height / 2 - 2)
            
            g.font = Font("SansSerif", Font.BOLD, 10)
            val percentText = String.format("%.1f%%", entry.flatPercent)
            val percentWidth = g.fontMetrics.stringWidth(percentText)
            g.drawString(percentText, rect.x + (rect.width - percentWidth) / 2, rect.y + rect.height / 2 + 11)
        } else if (rect.width > 25 && rect.height > 20) {
            // Small rectangle: show rank only
            g.font = Font("SansSerif", Font.BOLD, 9)
            val rankText = "#${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankText)
            g.drawString(rankText, rect.x + (rect.width - rankWidth) / 2, rect.y + rect.height / 2 + 3)
        }
    }
}

/**
 * Rectangle information for treemap
 */
data class TreemapRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val entry: com.github.spelens.pprofview.parser.PprofEntry,
    val index: Int
)
