package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Renders text with `<blank>term</blank>` tags as clickable theme-colored blocks.
 * Uses [buildInlineAnnotatedString] with blank parameters for inline parsing,
 * which supports blanks nested inside markdown formatting (e.g., **...<blank>...</blank>...**).
 */
@Composable
fun SelfCheckRenderer(
    text: String,
    revealedSet: Set<Int>,
    primaryColor: Color,
    onBlankClick: (Int) -> Unit
) {
    val baseStyle = MaterialTheme.typography.bodyMedium
    val monoStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val sanitized = remember(text) { stripDisallowedHtml(text) }
    val lines = sanitized.split("\n")
    val blankRegex = remember { Regex("<blank>(.*?)</blank>") }

    // Precompute global blank start index for each line
    val lineBlankStarts = remember(sanitized) {
        val starts = mutableListOf<Int>()
        var total = 0
        for (line in lines) { starts.add(total); total += blankRegex.findAll(line).count() }
        starts
    }

    var inCodeBlock = false
    val tableRows = remember { mutableStateListOf<String>() }
    Column {
        lines.forEachIndexed { idx, line ->
            if (tableRows.isNotEmpty() && (!line.trimStart().startsWith("|") || !line.trimEnd().endsWith("|"))) {
                renderSelfCheckTable(tableRows.toList(), baseStyle, revealedSet, primaryColor, onBlankClick)
                tableRows.clear()
            }
            val startIdx = lineBlankStarts.getOrElse(idx) { 0 }
            when {
                line.startsWith("```") -> { inCodeBlock = !inCodeBlock; Spacer(Modifier.height(4.dp)) }
                inCodeBlock -> SelfCheckInline(line, revealedSet, startIdx, primaryColor, monoStyle, onBlankClick, Modifier.padding(start = 8.dp))
                line.trimStart().startsWith("|") && line.trimEnd().endsWith("|") -> {
                    tableRows.add(line)
                    if (idx == lines.lastIndex) {
                        renderSelfCheckTable(tableRows.toList(), baseStyle, revealedSet, primaryColor, onBlankClick)
                        tableRows.clear()
                    }
                }
                line.startsWith("#### ") -> SelfCheckInline(line.removePrefix("#### "), revealedSet, startIdx, primaryColor, MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), onBlankClick, Modifier.padding(top = 4.dp, bottom = 2.dp))
                line.startsWith("### ") -> SelfCheckInline(line.removePrefix("### "), revealedSet, startIdx, primaryColor, MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), onBlankClick, Modifier.padding(top = 8.dp, bottom = 2.dp))
                line.startsWith("## ") -> SelfCheckInline(line.removePrefix("## "), revealedSet, startIdx, primaryColor, MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), onBlankClick, Modifier.padding(top = 10.dp, bottom = 2.dp))
                line.startsWith("# ") -> SelfCheckInline(line.removePrefix("# "), revealedSet, startIdx, primaryColor, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), onBlankClick, Modifier.padding(top = 10.dp, bottom = 4.dp))
                line.startsWith("> ") -> {
                    val content = line.substringAfter("> ")
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraSmall)) {
                        Box(Modifier.width(3.dp).height(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                        Spacer(Modifier.width(8.dp))
                        SelfCheckInline(content, revealedSet, startIdx, primaryColor, baseStyle.copy(fontStyle = FontStyle.Italic), onBlankClick, Modifier.weight(1f))
                    }
                }
                // Unordered list at any nesting level: "- "/"* " with optional leading indent
                // (2 spaces or 1 tab = one level; bullet: ▪ → ▪(0.75x) → ·, big to small)
                (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) -> {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    val level = ((indent.count { it == ' ' } + indent.count { it == '\t' } * 2) / 2).coerceAtMost(3)
                    val bullet = if (level >= 2) "· " else "▪ "
                    val bulletStyle = if (level == 1) baseStyle.copy(fontSize = baseStyle.fontSize * 0.75f) else baseStyle
                    val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(start = (8 + level * 16).dp, top = 2.dp)) {
                        Text(bullet, style = bulletStyle, modifier = Modifier.alignByBaseline())
                        SelfCheckInline(content, revealedSet, startIdx, primaryColor, baseStyle, onBlankClick, Modifier.alignByBaseline())
                    }
                }
                line.matches(Regex("^\\d+\\..*")) -> {
                    val content = line.substringAfter(". ")
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                        Text("${line.substringBefore(".")}. ", style = baseStyle)
                        SelfCheckInline(content, revealedSet, startIdx, primaryColor, baseStyle, onBlankClick)
                    }
                }
                line.matches(Regex("^-{3,}$")) -> {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(Modifier.height(4.dp))
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> SelfCheckInline(line, revealedSet, startIdx, primaryColor, baseStyle, onBlankClick)
            }
        }
    }
}

@Composable
private fun renderSelfCheckTable(
    rows: List<String>, style: TextStyle, revealedSet: Set<Int>,
    primaryColor: Color, onBlankClick: (Int) -> Unit
) {
    if (rows.size < 2) return
    val parsed = rows.map { row -> row.trim().removeSurrounding("|").split("|").map { it.trim() } }
    val headerRow = parsed.first()
    val dataRows = parsed.drop(1).filter { cells -> !cells.all { it.matches(Regex("^[-: ]+$")) } }
    if (headerRow.isEmpty()) return
    val blankRegex = Regex("<blank>(.*?)</blank>")
    var cellStartIdx = 0
    val headerStarts = headerRow.map { cell -> val i = cellStartIdx; cellStartIdx += blankRegex.findAll(cell).count(); i }
    val dataStarts = dataRows.map { row ->
        row.take(headerRow.size).map { cell -> val i = cellStartIdx; cellStartIdx += blankRegex.findAll(cell).count(); i }
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(borderColor.copy(alpha = 0.15f), MaterialTheme.shapes.small).padding(1.dp)) {
        Row(Modifier.fillMaxWidth().background(borderColor.copy(alpha = 0.25f)).padding(horizontal = 8.dp, vertical = 6.dp)) {
            headerRow.forEachIndexed { i, cell ->
                SelfCheckInline(cell, revealedSet, headerStarts[i], primaryColor, style.copy(fontWeight = FontWeight.Bold), onBlankClick, Modifier.weight(1f))
            }
        }
        dataRows.forEachIndexed { ri, cells ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                cells.take(headerRow.size).forEachIndexed { ci, cell ->
                    SelfCheckInline(cell, revealedSet, dataStarts[ri][ci], primaryColor, style, onBlankClick, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SelfCheckInline(
    text: String, revealedSet: Set<Int>, startBlankIndex: Int,
    primaryColor: Color, style: TextStyle,
    onBlankClick: (Int) -> Unit, modifier: Modifier = Modifier
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val ranges = remember { mutableListOf<Triple<Int, Int, Int>>() }
    val counter = remember { IntArray(1) }
    val annotated = remember(text, revealedSet, startBlankIndex, primaryColor, linkColor, codeBgColor) {
        counter[0] = startBlankIndex
        ranges.clear()
        buildInlineAnnotatedString(text, linkColor, codeBgColor, revealedSet, primaryColor, counter, ranges)
    }
    val captured = remember(annotated) { ranges.toList() }
    ClickableText(
        text = annotated, style = style, modifier = modifier,
        onClick = { offset ->
            for ((idx, s, e) in captured) {
                if (offset in s until e) { onBlankClick(idx); return@ClickableText }
            }
        }
    )
}
