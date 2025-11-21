package com.github.spelens.pprofview.ui

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
 * pprof 图表面板
 * 用于可视化展示 pprof 数据
 */
class PprofChartPanel(
    private val report: PprofTextReport,
    private val project: Project? = null,
    private val pprofFile: VirtualFile? = null
) : JBPanel<PprofChartPanel>(BorderLayout()) {
    
    init {
        // 创建选项卡面板
        val tabbedPane = JTabbedPane()
        
        // 添加表格视图（第一个标签）
        tabbedPane.addTab("详细数据", createTablePanel())
        
        // 添加柱状图
        tabbedPane.addTab("柱状图", createBarChartPanel())
        
        // 添加饼图
        tabbedPane.addTab("饼图", createPieChartPanel())
        
        // 添加热力图
        tabbedPane.addTab("热力图", createHeatmapPanel())
        
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * 创建柱状图面板
     */
    private fun createBarChartPanel(): JComponent {
        val panel = object : JPanel() {
            private var hoveredBarIndex = -1
            
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawBarChart(g as Graphics2D, hoveredBarIndex)
            }
            
            init {
                // 添加鼠标移动监听器，实现悬停效果
                addMouseMotionListener(object : MouseAdapter() {
                    override fun mouseMoved(e: MouseEvent) {
                        val newHoveredIndex = getBarIndexAt(e.x, e.y)
                        if (newHoveredIndex != hoveredBarIndex) {
                            hoveredBarIndex = newHoveredIndex
                            repaint()
                            
                            // 更新工具提示
                            toolTipText = if (hoveredBarIndex >= 0) {
                                buildBarTooltip(hoveredBarIndex)
                            } else {
                                null
                            }
                        }
                    }
                })
                
                // 添加鼠标点击监听器，支持导航
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
             * 获取鼠标位置对应的柱子索引
             */
            private fun getBarIndexAt(mouseX: Int, mouseY: Int): Int {
                val width = this.width
                val height = this.height
                
                // 使用与 drawBarChart 相同的计算逻辑
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
             * 构建柱状图工具提示
             */
            private fun buildBarTooltip(index: Int): String {
                val entry = report.entries[index]
                return buildString {
                    append("<html>")
                    append("<b>函数性能详情</b><br>")
                    append("<hr>")
                    append("<b>排名：</b> #${index + 1}<br>")
                    append("<b>函数名：</b> ${entry.functionName}<br>")
                    append("<hr>")
                    append("<b>Flat：</b> ${formatValue(entry.flat)} ${report.unit} (${String.format("%.2f%%", entry.flatPercent)})<br>")
                    append("<b>Cum：</b> ${formatValue(entry.cum)} ${report.unit} (${String.format("%.2f%%", entry.cumPercent)})<br>")
                    append("<b>Sum%：</b> ${String.format("%.2f%%", entry.sumPercent)}<br>")
                    append("<hr>")
                    append("<i>点击可跳转到代码位置</i>")
                    append("</html>")
                }
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * 绘制柱状图
     */
    private fun drawBarChart(g: Graphics2D, hoveredBarIndex: Int = -1) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        
        // 根据窗口宽度动态调整边距和条目数量
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
        
        // 取前 N 个条目
        val topEntries = report.entries.take(topCount)
        if (topEntries.isEmpty()) return
        
        // 简单背景
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // 绘制标题（窄窗口时缩小字体）
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, if (width < 500) 13 else 16)
        val title = "Top $topCount 函数性能"
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 20)
        
        // 副标题（窄窗口时不显示）
        if (width >= 400) {
            g.font = Font("SansSerif", Font.PLAIN, 10)
            g.color = JBColor.GRAY
            val subtitle = "单位: ${report.unit}"
            val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
            g.drawString(subtitle, (width - subtitleWidth) / 2, 38)
        }
        
        // 绘制网格线
        g.color = JBColor(Color(230, 230, 230), Color(60, 60, 60))
        g.stroke = BasicStroke(1f)
        val maxValue = topEntries.maxOfOrNull { it.flat } ?: 1L
        val gridLines = if (width < 500) 3 else 5
        for (i in 0..gridLines) {
            val y = height - bottomMargin - (chartHeight * i / gridLines)
            g.drawLine(leftMargin, y, width - rightMargin, y)
        }
        
        // 绘制坐标轴
        g.color = JBColor(Color(120, 120, 120), Color(160, 160, 160))
        g.stroke = BasicStroke(2f)
        g.drawLine(leftMargin, topMargin, leftMargin, height - bottomMargin) // Y 轴
        g.drawLine(leftMargin, height - bottomMargin, width - rightMargin, height - bottomMargin) // X 轴
        
        // 计算柱状图参数
        val barWidth = chartWidth / topEntries.size
        val barActualWidth = maxOf(8, (barWidth * 0.7).toInt())
        
        // 绘制柱状图
        topEntries.forEachIndexed { index, entry ->
            val barHeight = maxOf(2, (entry.flat.toDouble() / maxValue * chartHeight).toInt())
            val x = leftMargin + index * barWidth + (barWidth - barActualWidth) / 2
            val y = height - bottomMargin - barHeight
            
            val isHovered = index == hoveredBarIndex
            val color = getBarColor(index)
            
            // 绘制柱子
            g.color = if (isHovered) color.brighter() else color
            g.fillRect(x, y, barActualWidth, barHeight)
            
            // 绘制边框
            g.color = color.darker()
            g.stroke = BasicStroke(if (isHovered) 2f else 1f)
            g.drawRect(x, y, barActualWidth, barHeight)
            
            // 绘制数值（只在柱子足够高且宽时显示）
            if (barHeight > 25 && barActualWidth > 20 && width >= 500) {
                g.font = Font("SansSerif", Font.BOLD, 9)
                val valueText = String.format("%.1f%%", entry.flatPercent)
                val valueWidth = g.fontMetrics.stringWidth(valueText)
                
                g.color = JBColor.foreground()
                g.drawString(valueText, x + (barActualWidth - valueWidth) / 2, y - 4)
            }
            
            // 绘制函数名（只在宽度足够时显示）
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
        
        // 绘制 Y 轴刻度
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
     * 创建饼图面板
     */
    private fun createPieChartPanel(): JComponent {
        val panel = object : JPanel() {
            private var hoveredSliceIndex = -1
            
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawPieChart(g as Graphics2D, hoveredSliceIndex)
            }
            
            init {
                // 添加鼠标移动监听器
                addMouseMotionListener(object : MouseAdapter() {
                    override fun mouseMoved(e: MouseEvent) {
                        val newHoveredIndex = getSliceIndexAt(e.x, e.y)
                        if (newHoveredIndex != hoveredSliceIndex) {
                            hoveredSliceIndex = newHoveredIndex
                            repaint()
                            
                            // 更新工具提示
                            toolTipText = if (hoveredSliceIndex >= 0) {
                                buildPieTooltip(hoveredSliceIndex)
                            } else {
                                null
                            }
                        }
                    }
                })
                
                // 添加鼠标点击监听器
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
             * 获取鼠标位置对应的扇形索引
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
                
                // 计算鼠标相对于圆心的位置
                val dx = mouseX - centerX
                val dy = mouseY - centerY
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
                
                // 检查是否在圆内
                if (distance > radius) return -1
                
                // 计算角度（从右侧开始，逆时针）
                var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble()))
                if (angle < 0) angle += 360
                
                // 查找对应的扇形
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
             * 构建饼图工具提示
             */
            private fun buildPieTooltip(index: Int): String {
                val entry = report.entries[index]
                val total = report.entries.take(10).sumOf { it.flat }
                val percentage = (entry.flat.toDouble() / total * 100)
                
                return buildString {
                    append("<html>")
                    append("<b>函数占比详情</b><br>")
                    append("<hr>")
                    append("<b>排名：</b> #${index + 1}<br>")
                    append("<b>函数名：</b> ${entry.functionName}<br>")
                    append("<hr>")
                    append("<b>Flat：</b> ${formatValue(entry.flat)} ${report.unit}<br>")
                    append("<b>占比：</b> ${String.format("%.2f%%", percentage)}<br>")
                    append("<b>Cum：</b> ${formatValue(entry.cum)} ${report.unit} (${String.format("%.2f%%", entry.cumPercent)})<br>")
                    append("<hr>")
                    append("<i>点击可跳转到代码位置</i>")
                    append("</html>")
                }
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * 绘制饼图
     */
    private fun drawPieChart(g: Graphics2D, hoveredSliceIndex: Int = -1) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        
        // 简单背景
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // 取前 10 个条目
        val topEntries = report.entries.take(10)
        if (topEntries.isEmpty()) return
        
        // 绘制标题
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 16)
        val title = "Top ${topEntries.size} 函数占比"
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 30)
        
        g.font = Font("SansSerif", Font.PLAIN, 11)
        g.color = JBColor.GRAY
        val subtitle = "单位: ${report.unit}"
        val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
        g.drawString(subtitle, (width - subtitleWidth) / 2, 48)
        
        // 根据窗口大小动态调整布局
        val useVerticalLayout = width < 700
        
        // 计算饼图位置和大小
        val availableWidth = if (useVerticalLayout) width - 80 else (width * 0.5).toInt()
        val availableHeight = if (useVerticalLayout) (height * 0.5).toInt() else height - 120
        val pieSize = minOf(availableWidth, availableHeight, 400)
        val radius = pieSize / 2
        
        val centerX = if (useVerticalLayout) width / 2 else width / 3
        val centerY = if (useVerticalLayout) 80 + radius else height / 2
        
        // 计算总和
        val total = topEntries.sumOf { it.flat }.toDouble()
        
        // 绘制饼图
        var startAngle = 0.0
        topEntries.forEachIndexed { index, entry ->
            val angle = (entry.flat / total) * 360.0
            val isHovered = index == hoveredSliceIndex
            
            // 悬停时向外偏移
            val offsetRadius = if (isHovered) 10 else 0
            val offsetAngle = Math.toRadians(startAngle + angle / 2)
            val offsetX = (offsetRadius * Math.cos(offsetAngle)).toInt()
            val offsetY = (offsetRadius * Math.sin(offsetAngle)).toInt()
            
            // 绘制扇形
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
            
            // 绘制边框
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
            
            // 绘制百分比标签（如果扇形足够大）
            if (angle > 10 && radius > 80) {
                val labelAngle = Math.toRadians(startAngle + angle / 2)
                val labelRadius = radius * 0.65
                val labelX = (centerX + offsetX + labelRadius * Math.cos(labelAngle)).toInt()
                val labelY = (centerY + offsetY + labelRadius * Math.sin(labelAngle)).toInt()
                
                g.font = Font("SansSerif", Font.BOLD, 11)
                val percentText = String.format("%.1f%%", entry.flatPercent)
                val textWidth = g.fontMetrics.stringWidth(percentText)
                
                // 标签背景
                g.color = Color(255, 255, 255, 200)
                g.fillRect(labelX - textWidth / 2 - 3, labelY - 10, textWidth + 6, 16)
                
                // 标签文本
                g.color = Color.BLACK
                g.drawString(percentText, labelX - textWidth / 2, labelY + 3)
            }
            
            startAngle += angle
        }
        
        // 绘制图例
        val legendX = if (useVerticalLayout) 40 else (width * 2 / 3).toInt()
        var legendY = if (useVerticalLayout) centerY + radius + 40 else 80
        val legendItemHeight = 28
        
        // 图例标题
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 13)
        g.drawString("函数列表", legendX, legendY)
        legendY += 25
        
        topEntries.forEachIndexed { index, entry ->
            val color = getBarColor(index)
            
            // 绘制颜色块
            g.color = color
            g.fillRect(legendX, legendY, 20, 20)
            g.color = color.darker()
            g.stroke = BasicStroke(1f)
            g.drawRect(legendX, legendY, 20, 20)
            
            // 绘制排名
            g.color = Color.WHITE
            g.font = Font("SansSerif", Font.BOLD, 11)
            val rankStr = "${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankStr)
            g.drawString(rankStr, legendX + (20 - rankWidth) / 2, legendY + 15)
            
            // 绘制文本
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
     * 创建表格面板
     */
    private fun createTablePanel(): JComponent {
        val columnNames = arrayOf("排名", "函数名", "Flat", "Flat%", "Sum%", "Cum", "Cum%")
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
        
        // 设置表头样式
        val header = table.tableHeader
        header.font = Font("SansSerif", Font.BOLD, 12)
        header.background = JBColor.background()
        header.foreground = JBColor.foreground()
        
        // 设置列宽
        table.columnModel.getColumn(0).preferredWidth = 50  // 排名
        table.columnModel.getColumn(1).preferredWidth = 400 // 函数名
        table.columnModel.getColumn(2).preferredWidth = 80  // Flat
        table.columnModel.getColumn(3).preferredWidth = 70  // Flat%
        table.columnModel.getColumn(4).preferredWidth = 70  // Sum%
        table.columnModel.getColumn(5).preferredWidth = 80  // Cum
        table.columnModel.getColumn(6).preferredWidth = 70  // Cum%
        
        // 设置单元格渲染器（添加颜色）
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
                    // 交替行颜色
                    c.background = if (row % 2 == 0) {
                        JBColor.background()
                    } else {
                        JBColor(Color(245, 245, 245), Color(50, 50, 50))
                    }
                    
                    // 排名列使用颜色标识
                    if (column == 0 && row < 10) {
                        c.foreground = getBarColor(row)
                        font = Font("SansSerif", Font.BOLD, 12)
                    } else {
                        c.foreground = JBColor.foreground()
                        font = Font("SansSerif", Font.PLAIN, 12)
                    }
                }
                
                // 数值列右对齐
                horizontalAlignment = if (column in 2..6) SwingConstants.RIGHT else SwingConstants.LEFT
                
                // 函数名列显示为可点击的链接样式
                if (column == 1 && project != null && pprofFile != null) {
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    if (!isSelected) {
                        c.foreground = JBColor(Color(0, 102, 204), Color(100, 150, 255))
                    }
                }
                
                return c
            }
        })
        
        // 添加鼠标监听器
        if (project != null && pprofFile != null) {
            table.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val row = table.rowAtPoint(e.point)
                    val column = table.columnAtPoint(e.point)
                    
                    // 只处理函数名列的点击
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
            
            // 添加鼠标移动监听器，实现悬停工具提示
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
            // 即使没有导航功能，也提供工具提示
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
     * 构建表格工具提示
     */
    private fun buildTableTooltip(row: Int, column: Int, entry: com.github.spelens.pprofview.parser.PprofEntry): String {
        return buildString {
            append("<html>")
            append("<b>性能数据详情</b><br>")
            append("<hr>")
            append("<b>排名：</b> #${row + 1}<br>")
            append("<b>函数名：</b><br>")
            append("<code>${entry.functionName}</code><br>")
            append("<hr>")
            append("<table cellpadding='2'>")
            append("<tr><td><b>Flat：</b></td><td>${formatValue(entry.flat)} ${report.unit}</td><td>(${String.format("%.2f%%", entry.flatPercent)})</td></tr>")
            append("<tr><td><b>Cum：</b></td><td>${formatValue(entry.cum)} ${report.unit}</td><td>(${String.format("%.2f%%", entry.cumPercent)})</td></tr>")
            append("<tr><td><b>Sum%：</b></td><td colspan='2'>${String.format("%.2f%%", entry.sumPercent)}</td></tr>")
            append("</table>")
            append("<hr>")
            append("<small>")
            append("<b>说明：</b><br>")
            append("• <b>Flat</b>: 函数自身执行时间<br>")
            append("• <b>Cum</b>: 函数及其调用的所有函数的总时间<br>")
            append("• <b>Sum%</b>: 累计百分比")
            if (project != null && pprofFile != null && column == 1) {
                append("<br><br><i>💡 点击函数名可跳转到代码位置</i>")
            }
            append("</small>")
            append("</html>")
        }
    }
    
    /**
     * 导航到代码位置
     */
    private fun navigateToCode(functionName: String) {
        if (project == null || pprofFile == null) {
            println("ERROR: project 或 pprofFile 为 null")
            println("  - project: $project")
            println("  - pprofFile: $pprofFile")
            
            // 显示错误通知
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("pprofview.notifications")
                .createNotification(
                    "代码导航失败",
                    "项目或 pprof 文件信息缺失",
                    com.intellij.notification.NotificationType.ERROR
                )
                .notify(project)
            return
        }
        
        val startTime = System.currentTimeMillis()
        println("========================================")
        println("用户点击函数: $functionName")
        println("时间: ${java.time.LocalDateTime.now()}")
        println("项目: ${project.name}")
        println("pprof 文件: ${pprofFile.path}")
        
        try {
            val navigationService = PprofCodeNavigationService.getInstance(project)
            navigationService.navigateToFunction(pprofFile, functionName)
            
            val duration = System.currentTimeMillis() - startTime
            println("点击响应总耗时: ${duration}ms")
        } catch (e: Exception) {
            println("ERROR: 导航失败")
            e.printStackTrace()
            
            // 显示错误通知
            com.intellij.notification.NotificationGroupManager.getInstance()
                .getNotificationGroup("pprofview.notifications")
                .createNotification(
                    "代码导航失败",
                    "错误: ${e.message}",
                    com.intellij.notification.NotificationType.ERROR
                )
                .notify(project)
        }
        println("========================================")
    }
    
    /**
     * 获取柱状图颜色
     */
    private fun getBarColor(index: Int): Color {
        val colors = arrayOf(
            Color(66, 133, 244),   // 蓝色
            Color(234, 67, 53),    // 红色
            Color(251, 188, 5),    // 黄色
            Color(52, 168, 83),    // 绿色
            Color(255, 109, 0),    // 橙色
            Color(156, 39, 176),   // 紫色
            Color(0, 172, 193),    // 青色
            Color(255, 87, 34),    // 深橙色
            Color(121, 85, 72),    // 棕色
            Color(158, 158, 158)   // 灰色
        )
        return colors[index % colors.size]
    }
    
    /**
     * 截断函数名
     */
    private fun truncateFunctionName(name: String, maxLength: Int): String {
        if (name.length <= maxLength) return name
        
        // 尝试只保留函数名部分
        val parts = name.split(".")
        val funcName = parts.lastOrNull() ?: name
        
        return if (funcName.length <= maxLength) {
            funcName
        } else {
            funcName.substring(0, maxLength - 3) + "..."
        }
    }
    
    /**
     * 格式化数值
     */
    private fun formatValue(value: Long): String {
        return when {
            value >= 1000000 -> String.format("%.2fM", value / 1000000.0)
            value >= 1000 -> String.format("%.2fK", value / 1000.0)
            else -> value.toString()
        }
    }
    
    /**
     * 创建热力图面板
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
                // 添加鼠标移动监听器
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
                
                // 添加鼠标点击监听器
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
             * 构建热力图工具提示
             */
            private fun buildHeatmapTooltip(rect: TreemapRect): String {
                val entry = rect.entry
                return buildString {
                    append("<html>")
                    append("<b>函数热力详情</b><br>")
                    append("<hr>")
                    append("<b>排名：</b> #${rect.index + 1}<br>")
                    append("<b>函数名：</b><br>")
                    append("<code>${entry.functionName}</code><br>")
                    append("<hr>")
                    append("<b>Flat：</b> ${formatValue(entry.flat)} ${report.unit} (${String.format("%.2f%%", entry.flatPercent)})<br>")
                    append("<b>Cum：</b> ${formatValue(entry.cum)} ${report.unit} (${String.format("%.2f%%", entry.cumPercent)})<br>")
                    append("<hr>")
                    append("<i>💡 矩形面积代表性能占比<br>")
                    append("颜色深浅代表热点程度<br>")
                    append("点击可跳转到代码位置</i>")
                    append("</html>")
                }
            }
        }
        
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * 绘制热力图（矩形树图）
     */
    private fun drawHeatmap(g: Graphics2D, treemapRects: MutableList<TreemapRect>) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        
        // 根据窗口大小动态调整
        val margin = maxOf(30, width / 30)
        val topCount = when {
            width < 500 -> 12
            width < 800 -> 16
            else -> 20
        }
        
        // 简单背景
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // 取前 N 个条目
        val topEntries = report.entries.take(topCount)
        if (topEntries.isEmpty()) return
        
        // 绘制标题
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 16)
        val title = "Top $topCount 函数热力图"
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 25)
        
        g.font = Font("SansSerif", Font.PLAIN, 10)
        g.color = JBColor.GRAY
        val subtitle = "矩形面积 = 性能占比 | 颜色深浅 = 热点程度"
        val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
        g.drawString(subtitle, (width - subtitleWidth) / 2, 42)
        
        // 计算总值
        val total = topEntries.sumOf { it.flat }.toDouble()
        
        // 使用简化的网格布局
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
     * 布局矩形树图
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
        
        // 使用网格布局
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
            
            // 保存矩形信息
            val treemapRect = TreemapRect(
                x = rectX,
                y = rectY,
                width = rectWidth,
                height = rectHeight,
                entry = entry,
                index = index
            )
            rects.add(treemapRect)
            
            // 绘制矩形
            drawTreemapRect(g, treemapRect, index)
        }
    }
    
    /**
     * 绘制单个矩形
     */
    private fun drawTreemapRect(g: Graphics2D, rect: TreemapRect, index: Int) {
        val entry = rect.entry
        
        // 根据性能数据选择颜色深浅
        val baseColor = getBarColor(index)
        val intensity = (entry.flatPercent / 100.0).coerceIn(0.3, 1.0)
        val heatColor = Color(
            (baseColor.red * intensity).toInt(),
            (baseColor.green * intensity).toInt(),
            (baseColor.blue * intensity).toInt()
        )
        
        // 绘制矩形
        g.color = heatColor
        g.fillRect(rect.x, rect.y, rect.width, rect.height)
        
        // 绘制边框
        g.color = heatColor.darker()
        g.stroke = BasicStroke(1f)
        g.drawRect(rect.x, rect.y, rect.width, rect.height)
        
        // 绘制文本（根据矩形大小调整）
        g.color = Color.WHITE
        
        if (rect.width > 80 && rect.height > 50) {
            // 大矩形：显示排名、百分比、函数名
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
            // 中等矩形：显示排名和百分比
            g.font = Font("SansSerif", Font.BOLD, 11)
            val rankText = "#${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankText)
            g.drawString(rankText, rect.x + (rect.width - rankWidth) / 2, rect.y + rect.height / 2 - 2)
            
            g.font = Font("SansSerif", Font.BOLD, 10)
            val percentText = String.format("%.1f%%", entry.flatPercent)
            val percentWidth = g.fontMetrics.stringWidth(percentText)
            g.drawString(percentText, rect.x + (rect.width - percentWidth) / 2, rect.y + rect.height / 2 + 11)
        } else if (rect.width > 25 && rect.height > 20) {
            // 小矩形：只显示排名
            g.font = Font("SansSerif", Font.BOLD, 9)
            val rankText = "#${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankText)
            g.drawString(rankText, rect.x + (rect.width - rankWidth) / 2, rect.y + rect.height / 2 + 3)
        }
    }
}

/**
 * 矩形树图的矩形信息
 */
data class TreemapRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val entry: com.github.spelens.pprofview.parser.PprofEntry,
    val index: Int
)
