package com.github.anniext.pprofview.ui

import com.github.anniext.pprofview.parser.PprofTextReport
import com.github.anniext.pprofview.services.PprofCodeNavigationService
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
        
        // 添加概览面板（默认显示）
        tabbedPane.addTab("概览", createOverviewPanel())
        
        // 添加柱状图
        tabbedPane.addTab("柱状图", createBarChartPanel())
        
        // 添加饼图
        tabbedPane.addTab("饼图", createPieChartPanel())
        
        // 添加表格视图
        tabbedPane.addTab("详细数据", createTablePanel())
        
        add(tabbedPane, BorderLayout.CENTER)
    }
    
    /**
     * 创建概览面板
     */
    private fun createOverviewPanel(): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.background = JBColor.background()
        
        // 创建统计信息面板
        val statsPanel = JBPanel<JBPanel<*>>()
        statsPanel.layout = BoxLayout(statsPanel, BoxLayout.Y_AXIS)
        statsPanel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
        statsPanel.background = JBColor.background()
        
        // 添加标题
        val titleLabel = JLabel("性能分析概览")
        titleLabel.font = Font("SansSerif", Font.BOLD, 20)
        titleLabel.alignmentX = Component.LEFT_ALIGNMENT
        statsPanel.add(titleLabel)
        statsPanel.add(Box.createVerticalStrut(20))
        
        // 添加统计信息
        val totalEntries = report.entries.size
        val topEntries = report.entries.take(10)
        val topTotal = topEntries.sumOf { it.flat }
        val totalFlat = report.entries.sumOf { it.flat }
        val topPercentage = if (totalFlat > 0) (topTotal.toDouble() / totalFlat * 100) else 0.0
        
        addStatRow(statsPanel, "总函数数量", "$totalEntries 个")
        addStatRow(statsPanel, "数据单位", report.unit)
        addStatRow(statsPanel, "Top 10 占比", String.format("%.1f%%", topPercentage))
        
        statsPanel.add(Box.createVerticalStrut(30))
        
        // 添加 Top 10 列表
        val topLabel = JLabel("🔥 热点函数 Top 10")
        topLabel.font = Font("SansSerif", Font.BOLD, 16)
        topLabel.alignmentX = Component.LEFT_ALIGNMENT
        statsPanel.add(topLabel)
        statsPanel.add(Box.createVerticalStrut(10))
        
        topEntries.forEachIndexed { index, entry ->
            val funcPanel = createFunctionCard(index + 1, entry)
            funcPanel.alignmentX = Component.LEFT_ALIGNMENT
            statsPanel.add(funcPanel)
            statsPanel.add(Box.createVerticalStrut(8))
        }
        
        val scrollPane = JBScrollPane(statsPanel)
        scrollPane.border = null
        panel.add(scrollPane, BorderLayout.CENTER)
        
        return panel
    }
    
    /**
     * 创建函数卡片
     */
    private fun createFunctionCard(rank: Int, entry: com.github.anniext.pprofview.parser.PprofEntry): JPanel {
        val card = JBPanel<JBPanel<*>>(BorderLayout())
        card.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor.border(), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        )
        card.background = JBColor.background()
        card.maximumSize = Dimension(Int.MAX_VALUE, 80)
        
        // 左侧：排名和颜色指示器
        val leftPanel = JBPanel<JBPanel<*>>()
        leftPanel.layout = BoxLayout(leftPanel, BoxLayout.X_AXIS)
        leftPanel.background = JBColor.background()
        
        val colorIndicator = JPanel()
        colorIndicator.background = getBarColor(rank - 1)
        colorIndicator.preferredSize = Dimension(4, 60)
        leftPanel.add(colorIndicator)
        leftPanel.add(Box.createHorizontalStrut(10))
        
        val rankLabel = JLabel("#$rank")
        rankLabel.font = Font("SansSerif", Font.BOLD, 18)
        rankLabel.foreground = JBColor.GRAY
        leftPanel.add(rankLabel)
        leftPanel.add(Box.createHorizontalStrut(15))
        
        // 中间：函数信息
        val infoPanel = JBPanel<JBPanel<*>>()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.background = JBColor.background()
        
        val funcNameLabel = JLabel(truncateFunctionName(entry.functionName, 60))
        funcNameLabel.font = Font("SansSerif", Font.BOLD, 13)
        funcNameLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(funcNameLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        
        val detailLabel = JLabel(String.format(
            "Flat: %s (%.1f%%)  |  Cum: %s (%.1f%%)",
            formatValue(entry.flat), entry.flatPercent,
            formatValue(entry.cum), entry.cumPercent
        ))
        detailLabel.font = Font("SansSerif", Font.PLAIN, 11)
        detailLabel.foreground = JBColor.GRAY
        detailLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(detailLabel)
        
        // 右侧：百分比进度条
        val rightPanel = JBPanel<JBPanel<*>>()
        rightPanel.layout = BoxLayout(rightPanel, BoxLayout.Y_AXIS)
        rightPanel.background = JBColor.background()
        rightPanel.preferredSize = Dimension(120, 60)
        
        val percentLabel = JLabel(String.format("%.1f%%", entry.flatPercent))
        percentLabel.font = Font("SansSerif", Font.BOLD, 16)
        percentLabel.foreground = getBarColor(rank - 1)
        percentLabel.alignmentX = Component.CENTER_ALIGNMENT
        rightPanel.add(Box.createVerticalGlue())
        rightPanel.add(percentLabel)
        rightPanel.add(Box.createVerticalGlue())
        
        card.add(leftPanel, BorderLayout.WEST)
        card.add(infoPanel, BorderLayout.CENTER)
        card.add(rightPanel, BorderLayout.EAST)
        
        return card
    }
    
    /**
     * 添加统计行
     */
    private fun addStatRow(panel: JPanel, label: String, value: String) {
        val row = JBPanel<JBPanel<*>>()
        row.layout = BoxLayout(row, BoxLayout.X_AXIS)
        row.background = JBColor.background()
        row.alignmentX = Component.LEFT_ALIGNMENT
        row.maximumSize = Dimension(Int.MAX_VALUE, 30)
        
        val labelComp = JLabel("$label: ")
        labelComp.font = Font("SansSerif", Font.PLAIN, 14)
        labelComp.foreground = JBColor.GRAY
        
        val valueComp = JLabel(value)
        valueComp.font = Font("SansSerif", Font.BOLD, 14)
        
        row.add(labelComp)
        row.add(valueComp)
        row.add(Box.createHorizontalGlue())
        
        panel.add(row)
        panel.add(Box.createVerticalStrut(5))
    }
    
    /**
     * 创建柱状图面板
     */
    private fun createBarChartPanel(): JComponent {
        val panel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawBarChart(g as Graphics2D)
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * 绘制柱状图
     */
    private fun drawBarChart(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        val margin = 80
        val chartWidth = width - 2 * margin
        val chartHeight = height - 2 * margin - 100
        
        // 取前 15 个条目
        val topEntries = report.entries.take(15)
        if (topEntries.isEmpty()) return
        
        // 绘制背景
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // 绘制标题
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 18)
        val title = "Top ${topEntries.size} 函数性能分析"
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 40)
        
        g.font = Font("SansSerif", Font.PLAIN, 12)
        val subtitle = "单位: ${report.unit}"
        val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
        g.drawString(subtitle, (width - subtitleWidth) / 2, 60)
        
        // 绘制网格线
        g.color = JBColor(Color(230, 230, 230), Color(60, 60, 60))
        val maxValue = topEntries.maxOfOrNull { it.flat } ?: 1L
        for (i in 0..5) {
            val y = height - margin - (chartHeight * i / 5)
            g.drawLine(margin, y, width - margin, y)
        }
        
        // 绘制坐标轴
        g.color = JBColor.border()
        g.stroke = BasicStroke(2f)
        g.drawLine(margin, margin, margin, height - margin) // Y 轴
        g.drawLine(margin, height - margin, width - margin, height - margin) // X 轴
        
        // 计算柱状图参数
        val barWidth = chartWidth / topEntries.size
        val barActualWidth = (barWidth * 0.7).toInt()
        
        // 绘制柱状图
        topEntries.forEachIndexed { index, entry ->
            val barHeight = (entry.flat.toDouble() / maxValue * chartHeight).toInt()
            val x = margin + index * barWidth + (barWidth - barActualWidth) / 2
            val y = height - margin - barHeight
            
            // 绘制阴影
            g.color = JBColor(Color(0, 0, 0, 30), Color(0, 0, 0, 50))
            g.fillRect(x + 3, y + 3, barActualWidth, barHeight)
            
            // 绘制柱子（渐变效果）
            val color = getBarColor(index)
            val gradient = GradientPaint(
                x.toFloat(), y.toFloat(), color.brighter(),
                x.toFloat(), (y + barHeight).toFloat(), color
            )
            g.paint = gradient
            g.fillRect(x, y, barActualWidth, barHeight)
            
            // 绘制边框
            g.color = color.darker()
            g.stroke = BasicStroke(1.5f)
            g.drawRect(x, y, barActualWidth, barHeight)
            
            // 绘制数值
            g.color = JBColor.foreground()
            g.font = Font("SansSerif", Font.BOLD, 11)
            val valueText = String.format("%.1f%%", entry.flatPercent)
            val valueWidth = g.fontMetrics.stringWidth(valueText)
            g.drawString(valueText, x + (barActualWidth - valueWidth) / 2, y - 8)
            
            // 绘制函数名 (旋转)
            g.font = Font("SansSerif", Font.PLAIN, 10)
            val funcName = truncateFunctionName(entry.functionName, 25)
            
            val transform = g.transform
            g.rotate(-Math.PI / 6, (x + barActualWidth / 2).toDouble(), (height - margin + 15).toDouble())
            g.drawString(funcName, x + barActualWidth / 2, height - margin + 15)
            g.transform = transform
        }
        
        // 绘制 Y 轴刻度
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.PLAIN, 11)
        for (i in 0..5) {
            val y = height - margin - (chartHeight * i / 5)
            val value = maxValue * i / 5
            g.drawLine(margin - 8, y, margin, y)
            val valueStr = formatValue(value)
            val strWidth = g.fontMetrics.stringWidth(valueStr)
            g.drawString(valueStr, margin - strWidth - 12, y + 4)
        }
    }
    
    /**
     * 创建饼图面板
     */
    private fun createPieChartPanel(): JComponent {
        val panel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                drawPieChart(g as Graphics2D)
            }
        }
        panel.preferredSize = Dimension(800, 600)
        panel.background = JBColor.WHITE
        
        return JBScrollPane(panel)
    }
    
    /**
     * 绘制饼图
     */
    private fun drawPieChart(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        
        val width = g.clipBounds.width
        val height = g.clipBounds.height
        
        // 绘制背景
        g.color = JBColor.background()
        g.fillRect(0, 0, width, height)
        
        // 取前 10 个条目
        val topEntries = report.entries.take(10)
        if (topEntries.isEmpty()) return
        
        // 绘制标题
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 18)
        val title = "Top ${topEntries.size} 函数占比分析"
        val titleWidth = g.fontMetrics.stringWidth(title)
        g.drawString(title, (width - titleWidth) / 2, 40)
        
        g.font = Font("SansSerif", Font.PLAIN, 12)
        val subtitle = "单位: ${report.unit}"
        val subtitleWidth = g.fontMetrics.stringWidth(subtitle)
        g.drawString(subtitle, (width - subtitleWidth) / 2, 60)
        
        // 计算饼图位置和大小
        val pieWidth = minOf(width * 0.5, height - 150.0).toInt()
        val radius = pieWidth / 2
        val centerX = width / 3
        val centerY = height / 2 + 20
        
        // 计算总和
        val total = topEntries.sumOf { it.flat }.toDouble()
        
        // 绘制饼图阴影
        g.color = JBColor(Color(0, 0, 0, 30), Color(0, 0, 0, 50))
        g.fillOval(centerX - radius + 5, centerY - radius + 5, radius * 2, radius * 2)
        
        // 绘制饼图
        var startAngle = 0.0
        topEntries.forEachIndexed { index, entry ->
            val angle = (entry.flat / total) * 360.0
            
            // 绘制扇形（渐变效果）
            val color = getBarColor(index)
            g.color = color
            g.fillArc(
                centerX - radius,
                centerY - radius,
                radius * 2,
                radius * 2,
                startAngle.toInt(),
                angle.toInt()
            )
            
            // 绘制边框
            g.color = color.darker()
            g.stroke = BasicStroke(2f)
            g.drawArc(
                centerX - radius,
                centerY - radius,
                radius * 2,
                radius * 2,
                startAngle.toInt(),
                angle.toInt()
            )
            
            // 绘制百分比标签（如果扇形足够大）
            if (angle > 15) {
                val labelAngle = Math.toRadians(startAngle + angle / 2)
                val labelRadius = radius * 0.7
                val labelX = (centerX + labelRadius * Math.cos(labelAngle)).toInt()
                val labelY = (centerY + labelRadius * Math.sin(labelAngle)).toInt()
                
                g.color = Color.WHITE
                g.font = Font("SansSerif", Font.BOLD, 12)
                val percentText = String.format("%.1f%%", entry.flatPercent)
                val textWidth = g.fontMetrics.stringWidth(percentText)
                g.drawString(percentText, labelX - textWidth / 2, labelY + 5)
            }
            
            startAngle += angle
        }
        
        // 绘制图例
        val legendX = width * 2 / 3
        var legendY = 120
        val legendWidth = width - legendX - 40
        
        g.color = JBColor.foreground()
        g.font = Font("SansSerif", Font.BOLD, 14)
        g.drawString("函数列表", legendX.toInt(), legendY - 10)
        
        topEntries.forEachIndexed { index, entry ->
            // 绘制颜色块（圆角矩形）
            g.color = getBarColor(index)
            g.fillRoundRect(legendX.toInt(), legendY, 24, 24, 6, 6)
            g.color = getBarColor(index).darker()
            g.stroke = BasicStroke(1.5f)
            g.drawRoundRect(legendX.toInt(), legendY, 24, 24, 6, 6)
            
            // 绘制排名
            g.color = Color.WHITE
            g.font = Font("SansSerif", Font.BOLD, 11)
            val rankStr = "${index + 1}"
            val rankWidth = g.fontMetrics.stringWidth(rankStr)
            g.drawString(rankStr, legendX.toInt() + (24 - rankWidth) / 2, legendY + 17)
            
            // 绘制文本
            g.color = JBColor.foreground()
            g.font = Font("SansSerif", Font.PLAIN, 11)
            val percentText = String.format("%.1f%%", entry.flatPercent)
            g.drawString(percentText, legendX.toInt() + 35, legendY + 17)
            
            g.font = Font("SansSerif", Font.PLAIN, 10)
            g.color = JBColor.GRAY
            val funcName = truncateFunctionName(entry.functionName, 30)
            g.drawString(funcName, legendX.toInt() + 35, legendY + 17 + 12)
            
            legendY += 40
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
        
        // 添加鼠标点击监听器
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
        }
        
        val scrollPane = JBScrollPane(table)
        scrollPane.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        return scrollPane
    }
    
    /**
     * 导航到代码位置
     */
    private fun navigateToCode(functionName: String) {
        if (project == null || pprofFile == null) {
            return
        }
        
        val navigationService = PprofCodeNavigationService.getInstance(project)
        navigationService.navigateToFunction(pprofFile, functionName)
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
}
