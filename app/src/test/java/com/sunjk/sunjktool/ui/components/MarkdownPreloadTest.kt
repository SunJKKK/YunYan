package com.sunjk.sunjktool.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPreloadTest {
    @Test
    fun preloadKeepsLargeSummaryInStableBlocks() {
        val summary = buildString {
            repeat(200) { index ->
                append("## Section $index\n\n")
                append("A paragraph with enough content to represent a completed summary.\n\n")
                append("| Key | Value |\n| --- | --- |\n| $index | stable |\n\n")
            }
        }
        val blocks = splitMarkdownBlocks(summary)
        assertEquals(600, blocks.size)
        assertEquals(blocks.size, blocks.map { it.key }.distinct().size)
        assertTrue(blocks.all { it.content.isNotBlank() })
        assertTrue(blocks.filter { it.content.startsWith("|") }.all { it.content.lines().size == 3 })
    }
}
