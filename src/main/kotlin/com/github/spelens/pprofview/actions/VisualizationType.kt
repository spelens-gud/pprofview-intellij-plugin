package com.github.spelens.pprofview.actions

/**
 * pprof visualization type
 */
enum class VisualizationType {
    /**
     * Web browser (Interactive UI)
     */
    WEB,
    
    /**
     * Text report (Top functions)
     */
    TEXT,
    
    /**
     * Call graph (SVG)
     */
    GRAPH,
    
    /**
     * Flame graph (SVG)
     */
    FLAMEGRAPH,
    
    /**
     * Top 10 functions
     */
    TOP,
    
    /**
     * Complete function list
     */
    LIST,
    
    /**
     * Peek (Brief info)
     */
    PEEK
}
