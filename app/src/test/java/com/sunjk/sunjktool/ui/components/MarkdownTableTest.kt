package com.sunjk.sunjktool.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTableTest {
    @Test
    fun parsesGfmAlignmentAndUnwrappedRows() {
        val table = parseMarkdownTable(
            "Name | Value | Note",
            ":--- | :---: | ---:",
            listOf("A | 2 | ok")
        )!!
        assertEquals(listOf("Name", "Value", "Note"), table.header)
        assertEquals(listOf("A", "2", "ok"), table.rows.single())
        assertEquals(
            listOf(TableColumnAlignment.LEFT, TableColumnAlignment.CENTER, TableColumnAlignment.RIGHT),
            table.alignments
        )
    }

    @Test
    fun respectsEscapedPipesAndNormalizesRows() {
        val table = parseMarkdownTable(
            "A | B | C",
            "--- | --- | ---",
            listOf("one \\| two | value", "only one", "x | y | z | extra")
        )!!
        assertEquals(listOf("one | two", "value", ""), table.rows[0])
        assertEquals(listOf("only one", "", ""), table.rows[1])
        assertEquals(listOf("x", "y", "z | extra"), table.rows[2])
    }

    @Test
    fun rejectsInvalidDelimiter() {
        assertEquals(null, parseMarkdownTable("A | B", "-- | ---", emptyList()))
        assertEquals(null, parseMarkdownTable("A | B", "---", emptyList()))
    }

    @Test
    fun solvesOverflowWidthsExactlyWithoutEqualizingShortColumns() {
        val widths = solveTableColumnWidths(listOf(40f, 300f, 60f), 300f, minimum = 36f)
        assertEquals(300f, widths.sum(), 0.01f)
        assertTrue(widths[1] > widths[0])
        assertTrue(widths[1] > widths[2])
        assertFalse(widths[0] == widths[1])
    }

    @Test
    fun keepsEachColumnReadableWhenContentLengthsDiffer() {
        val widths = solveTableColumnWidths(
            idealWidths = listOf(20f, 500f),
            available = 180f,
            minimumWidths = listOf(18f, 18f)
        )
        assertEquals(180f, widths.sum(), 0.01f)
        assertTrue(widths[0] >= 18f)
        assertTrue(widths[1] > widths[0])
    }
    @Test
    fun preservesIdealWidthsWhenTheyFit() {
        assertEquals(listOf(40f, 60f), solveTableColumnWidths(listOf(40f, 60f), 200f))
    }
}
