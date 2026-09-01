package com.sunjk.sunjktool.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownRenderBlockTest {
    @Test
    fun splitsParagraphsIntoLazyBlocks() {
        val blocks = splitMarkdownBlocks("# Title\n\nFirst paragraph.\n\nSecond paragraph.")
        assertEquals(3, blocks.size)
        assertEquals("# Title", blocks[0].content)
        assertEquals("First paragraph.", blocks[1].content)
        assertEquals("Second paragraph.", blocks[2].content)
        assertTrue(blocks.map { it.key }.distinct().size == blocks.size)
    }

    @Test
    fun keepsCodeAndTableBlocksIntact() {
        val blocks = splitMarkdownBlocks(
            "Before\n\n| A | B |\n| --- | --- |\n| 1 | 2 |\n\n```kotlin\nval x = 1\n\nprintln(x)\n```\n\nAfter"
        )
        assertEquals(4, blocks.size)
        assertTrue(blocks[1].content.contains("| 1 | 2 |"))
        assertTrue(blocks[2].content.contains("\n\nprintln(x)"))
        assertEquals("After", blocks[3].content)
    }
}
