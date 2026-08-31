@file:Suppress("unused")

package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Three highlight colours used in AI summaries (Material 200-level, heavier
 * than the old 50-level palette — dark text remains legible on all three in
 * both light and dark themes).
 *
 * Prompt semantics:
 * - [CONCEPT] blue: 概念名词、专业术语
 * - [KNOWLEDGE] yellow: 知识点
 * - [MISTAKE] red: 易错点、常见错误
 */
object HighlightColors {
    /** Blue 200 — 概念名词、专业术语 */
    val CONCEPT   = Color(0xFF90CAF9)
    /** Yellow 300 — 知识点 */
    val KNOWLEDGE = Color(0xFFFFF176)
    /** Red 200 — 易错点、常见错误 */
    val MISTAKE   = Color(0xFFEF9A9A)
}

// ── Public helpers ──────────────────────────────────────────────────────

/**
 * Parse a CSS colour value into a Compose [Color].
 *
 * Supported formats:
 * - `#RGB` (each digit expanded: `#F71` → `#FF7711`)
 * - `#RRGGBB`
 * - ~25 named colours (`red`, `blue`, `green`, `yellow`, `orange`, `purple`,
 *   `pink`, `black`, `white`, `gray`, `grey`, `brown`, `cyan`, `magenta`,
 *   `gold`, `navy`, `teal`, `maroon`, `olive`, `lime`, `indigo`, `violet`,
 *   `crimson`, `coral`, `salmon`, `tomato`)
 *
 * Returns `null` for unparseable input.
 */
internal fun parseCssColor(value: String): Color? {
    val v = value.trim().lowercase()
    if (v.isEmpty()) return null

    // Named colours (~25 common CSS keywords)
    namedColors[v]?.let { return it }

    // #RGB or #RRGGBB
    if (v.startsWith("#")) {
        val hex = v.drop(1)
        return when (hex.length) {
            3 -> try {
                val r = hex[0].toString().repeat(2).toInt(16)
                val g = hex[1].toString().repeat(2).toInt(16)
                val b = hex[2].toString().repeat(2).toInt(16)
                Color(0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
            } catch (_: Exception) { null }
            6 -> try {
                Color(0xFF000000L or hex.toLong(16))
            } catch (_: Exception) { null }
            else -> null
        }
    }
    return null
}

private val namedColors: Map<String, Color> = mapOf(
    "red" to Color(0xFFFF0000), "blue" to Color(0xFF0000FF),
    "green" to Color(0xFF008000), "yellow" to Color(0xFFFFFF00),
    "orange" to Color(0xFFFFA500), "purple" to Color(0xFF800080),
    "pink" to Color(0xFFFFC0CB), "black" to Color(0xFF000000),
    "white" to Color(0xFFFFFFFF), "gray" to Color(0xFF808080),
    "grey" to Color(0xFF808080), "brown" to Color(0xFFA52A2A),
    "cyan" to Color(0xFF00FFFF), "magenta" to Color(0xFFFF00FF),
    "gold" to Color(0xFFFFD700), "navy" to Color(0xFF000080),
    "teal" to Color(0xFF008080), "maroon" to Color(0xFF800000),
    "olive" to Color(0xFF808000), "lime" to Color(0xFF00FF00),
    "indigo" to Color(0xFF4B0082), "violet" to Color(0xFFEE82EE),
    "crimson" to Color(0xFFDC143C), "coral" to Color(0xFFFF7F50),
    "salmon" to Color(0xFFFA8072), "tomato" to Color(0xFFFF6347),
)

/** CSS font-size keywords → relative [TextUnit] multipliers. */
private val fontSizeKeywords: Map<String, TextUnit> = mapOf(
    "xx-small" to 0.6.em, "x-small" to 0.75.em, "small" to 0.85.em,
    "medium" to 1.em, "large" to 1.2.em, "x-large" to 1.5.em,
    "xx-large" to 2.em, "xxx-large" to 3.em,
    "smaller" to 0.85.em, "larger" to 1.2.em,
)

/**
 * Parse a CSS `style` attribute value into a [SpanStyle].
 *
 * Supported properties:
 * - `color`, `background` / `background-color`
 * - `font-size` (em, rem, %, px, pt, keywords large / x-large / …)
 * - `font-weight` (bold, bolder, numeric 100-900)
 * - `font-style` (italic, oblique)
 * - `text-decoration` (underline, line-through)
 *
 * All other properties (including `text-shadow`) are silently ignored —
 * Compose [SpanStyle] cannot represent per-character shadows; the
 * combination of large font + bold + vivid colour + distinct background
 * provides sufficient emphasis for the "extremely important" case.
 *
 * **Contrast rule**: when [background] is set but [color] is not,
 * a dark text colour is forced so the text remains readable on the
 * mid-strength (200-level) highlight backgrounds in both light and
 * dark themes.
 */
internal fun parseSpanStyle(styleAttr: String): SpanStyle {
    if (styleAttr.isBlank()) return SpanStyle()

    var color: Color? = null
    var background: Color? = null
    var fontSize: TextUnit? = null
    var fontWeight: FontWeight? = null
    var fontStyle: FontStyle? = null
    var textDecoration: TextDecoration? = null

    fun parseFontWeight(raw: String): FontWeight? {
        val w = raw.trim().lowercase()
        if (w == "bold") return FontWeight.Bold
        if (w == "bolder") return FontWeight.ExtraBold
        if (w == "lighter") return FontWeight.Light
        if (w == "normal") return FontWeight.Normal
        val n = w.toIntOrNull()?.coerceIn(100, 900) ?: return null
        return FontWeight(n)
    }

    fun parseFontSize(raw: String): TextUnit? {
        val r = raw.trim().lowercase()
        fontSizeKeywords[r]?.let { return it }
        if (r.endsWith("em") || r.endsWith("rem"))
            return r.dropLast(if (r.endsWith("rem")) 3 else 2).toFloatOrNull()?.em
        if (r.endsWith("%"))
            return (r.dropLast(1).toFloatOrNull()?.div(100f))?.em
        if (r.endsWith("px"))
            return r.dropLast(2).toFloatOrNull()?.sp
        if (r.endsWith("pt"))
            return (r.dropLast(2).toFloatOrNull()?.times(1.333f))?.sp
        return null
    }

    for (decl in styleAttr.split(";")) {
        val colon = decl.indexOf(':')
        if (colon == -1) continue
        val prop = decl.substring(0, colon).trim().lowercase()
        val rawVal = decl.substring(colon + 1).trim()
        if (rawVal.isEmpty()) continue

        when (prop) {
            "color" -> color = parseCssColor(rawVal) ?: color
            "background", "background-color" -> background = parseCssColor(rawVal) ?: background
            "font-size" -> fontSize = parseFontSize(rawVal) ?: fontSize
            "font-weight" -> fontWeight = parseFontWeight(rawVal) ?: fontWeight
            "font-style" -> {
                val s = rawVal.lowercase()
                if (s == "italic" || s == "oblique") fontStyle = FontStyle.Italic
            }
            "text-decoration" -> {
                val td = TextDecoration.None
                if ("underline" in rawVal) textDecoration = (textDecoration ?: td) + TextDecoration.Underline
                if ("line-through" in rawVal) textDecoration = (textDecoration ?: td) + TextDecoration.LineThrough
            }
            // text-shadow, padding, border-radius, etc. — silently ignored
        }
    }

    // Contrast rule
    if (background != null && color == null) {
        color = Color(0xFF212121)
    }

    return SpanStyle(
        color = color ?: Color.Unspecified,
        background = background ?: Color.Unspecified,
        fontSize = fontSize ?: TextUnit.Unspecified,
        fontWeight = fontWeight ?: FontWeight.Normal,
        fontStyle = fontStyle ?: FontStyle.Normal,
        textDecoration = textDecoration ?: TextDecoration.None,
    )
}

/**
 * Remove banned HTML elements from [text] before rendering.
 *
 * Two-pass regex (case-insensitive, dot-matches-all):
 * 1. Full elements including content: `<script>…</script>` → removed
 * 2. Leftover unmatched / self-closing tags: `<script>` / `<iframe />` → removed
 *
 * Elements targeted: script, iframe, object, embed, form, input.
 */
internal fun stripDisallowedHtml(text: String): String {
    val banned = "(script|iframe|object|embed|form|input)"
    // Pass 1: full elements with content
    var result = text.replace(
        Regex("(?is)<$banned\\b[^>]*>.*?</$banned\\s*>"), ""
    )
    // Pass 2: leftover unmatched / self-closing tags
    result = result.replace(
        Regex("(?is)</?$banned\\b[^>]*/?>"), ""
    )
    return result
}

/**
 * Walk [text] from [fromIndex] to find the matching `</span>` that balances
 * nested `<span ...>` opens. Returns the index of `</span>`, or -1 if none.
 */
internal fun findMatchingSpanClose(text: String, fromIndex: Int): Int {
    var depth = 1
    var i = fromIndex
    while (i < text.length) {
        when {
            text.startsWith("</span>", i, ignoreCase = true) -> {
                depth--
                if (depth == 0) return i
                i += 7
            }
            text.startsWith("<span", i, ignoreCase = true) -> {
                val next = text.getOrElse(i + 5) { break }
                if (next == ' ' || next == '>' || next == '\t' || next == '\n' || next == '\r') {
                    depth++
                }
                i += 5
            }
            else -> i++
        }
    }
    return -1
}

// ── Inline parser ───────────────────────────────────────────────────────

/**
 * Build an [AnnotatedString] from inline markdown + HTML `<span>` tags.
 *
 * Markdown: **bold**, *italic*, ~~strikethrough~~, `inline code`, [link](url)
 *
 * HTML `<span style="...">`: supports `color`, `background`/`background-color`,
 * `font-size`, `font-weight`, `font-style`, `text-decoration` (see
 * [parseSpanStyle]). Nested spans are supported. Legacy `<span
 * style="background: #XXXXXX">` is fully backward-compatible.
 *
 * `<blank>text</blank>` self-check blanks are supported when [blankColor] is
 * not [Color.Unspecified].
 */
internal fun buildInlineAnnotatedString(
    text: String,
    linkColor: Color,
    codeBgColor: Color,
    blankRevealedSet: Set<Int> = emptySet(),
    blankColor: Color = Color.Unspecified,
    blankIndexCounter: IntArray? = null,
    blankRanges: MutableList<Triple<Int, Int, Int>>? = null
): AnnotatedString = buildAnnotatedString {
    val url = Regex("\\[([^]]+)]\\(([^)]+)\\)")
    val spanOpenRegex = Regex("""^<span\b[^>]*>""", RegexOption.IGNORE_CASE)
    val spanStyleRegex = Regex("""style\s*=\s*"([^"]*)"|style\s*=\s*'([^']*)'""", RegexOption.IGNORE_CASE)
    val blankOpenRegex = Regex("""^<blank>""")
    val blankCloseStr = "</blank>"
    val hasBlanks = blankColor != Color.Unspecified
    var remaining = text
    while (remaining.isNotEmpty()) {
        when {
            // --- Inline formula \(...\) ---
            remaining.startsWith("\\(") -> {
                val end = remaining.indexOf("\\)", 2)
                if (end == -1) {
                    append(remaining.take(2))
                    remaining = remaining.substring(2)
                } else {
                    val formula = remaining.substring(2, end)
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontStyle = FontStyle.Italic, background = codeBgColor, color = Color(0xFF1565C0))) {
                        append(formula)
                    }
                    remaining = remaining.substring(end + 2)
                }
            }
            // --- Inline formula $...$ ---
            remaining.startsWith("$") -> {
                val end = remaining.indexOf("$", 1)
                if (end == -1 || end == 1) {
                    append(remaining.take(1))
                    remaining = remaining.substring(1)
                } else {
                    val formula = remaining.substring(1, end)
                    withStyle(SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontStyle = FontStyle.Italic,
                        background = codeBgColor,
                        color = Color(0xFF1565C0)
                    )) {
                        append(formula)
                    }
                    remaining = remaining.substring(end + 1)
                }
            }
            // --- HTML <br> line break ---
            remaining.startsWith("<br", ignoreCase = true) -> {
                val brMatch = Regex("""^<br\s*/?>""", RegexOption.IGNORE_CASE).find(remaining)
                if (brMatch != null) {
                    append("\n")
                    remaining = remaining.substring(brMatch.range.last + 1)
                } else {
                    append("<"); remaining = remaining.substring(1)
                }
            }
            // --- HTML <span style="..."> (generalised) ---
            remaining.startsWith("<span", ignoreCase = true) -> {
                val match = spanOpenRegex.find(remaining)
                if (match != null) {
                    val tagEnd = match.range.last + 1 // position of '>'
                    val openTag = match.value
                    // Extract style attribute (optional)
                    val styleAttr = spanStyleRegex.find(openTag)?.let { m ->
                        m.groupValues[1].ifBlank { null } ?: m.groupValues[2].ifBlank { null } ?: ""
                    } ?: ""
                    val spanStyle = parseSpanStyle(styleAttr)

                    val closeIdx = findMatchingSpanClose(remaining, tagEnd)
                    val innerContent: String
                    val consumed: Int
                    if (closeIdx == -1) {
                        // Streaming: no matching close tag yet.
                        // Render the remainder WITH the style so raw HTML doesn't flash.
                        innerContent = remaining.substring(tagEnd)
                        consumed = remaining.length
                    } else {
                        innerContent = remaining.substring(tagEnd, closeIdx)
                        consumed = closeIdx + 7 // skip past </span>
                    }
                    withStyle(spanStyle) {
                        append(buildInlineAnnotatedString(innerContent, linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges))
                    }
                    remaining = remaining.substring(consumed)
                } else {
                    // Partial tag like "<span sty" — suppress to avoid flashing
                    val gt = remaining.indexOf('>')
                    if (gt == -1) break // no complete tag ahead; suppress remainder
                    else { append("<"); remaining = remaining.substring(1) }
                }
            }
            // --- Self-check blank: <blank>text</blank> ---
            hasBlanks && remaining.startsWith("<blank>") -> {
                val match = blankOpenRegex.find(remaining)
                if (match != null) {
                    val closeIdx = remaining.indexOf(blankCloseStr, match.range.last + 1)
                    if (closeIdx == -1) { append(remaining); break }
                    val blankContent = remaining.substring(match.range.last + 1, closeIdx)
                    val idx = blankIndexCounter?.get(0) ?: 0
                    val startOffset = length
                    val isRevealed = blankRevealedSet.contains(idx)
                    val innerAnnotated = buildInlineAnnotatedString(blankContent, linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges)
                    val renderedText = innerAnnotated.text
                    pushStringAnnotation(tag = "blank_$idx", annotation = "blank_$idx")
                    if (isRevealed) {
                        withStyle(SpanStyle(background = blankColor.copy(alpha = 0.08f))) {
                            append(innerAnnotated)
                        }
                    } else {
                        withStyle(SpanStyle(background = blankColor, color = blankColor)) {
                            append(renderedText)
                        }
                    }
                    pop()
                    val endOffset = length
                    blankRanges?.add(Triple(idx, startOffset, endOffset))
                    if (blankIndexCounter != null) blankIndexCounter[0] = idx + 1
                    remaining = remaining.substring(closeIdx + blankCloseStr.length)
                } else { append("<"); remaining = remaining.substring(1) }
            }
            // Bold: **text**
            remaining.startsWith("**") -> {
                val end = remaining.indexOf("**", 2)
                if (end == -1) { append(remaining); break }
                val inner = remaining.substring(2, end)
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(buildInlineAnnotatedString(inner, linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges))
                }
                remaining = remaining.substring(end + 2)
            }
            // Strikethrough: ~~text~~
            remaining.startsWith("~~") -> {
                val end = remaining.indexOf("~~", 2)
                if (end == -1) { append(remaining); break }
                val inner = remaining.substring(2, end)
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                    append(buildInlineAnnotatedString(inner, linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges))
                }
                remaining = remaining.substring(end + 2)
            }
            // Italic: *text*
            remaining.startsWith("*") -> {
                val nextChar = remaining.getOrNull(1) ?: ' '
                if (nextChar == '*' || nextChar == ' ') {
                    append(remaining.take(if (nextChar == '*') 2 else 1))
                    remaining = remaining.drop(if (nextChar == '*') 2 else 1)
                } else {
                    val end = remaining.indexOf("*", 2).takeIf { it > 1 } ?: -1
                    if (end == -1) { append(remaining); break }
                    val preClose = remaining.getOrNull(end - 1) ?: ' '
                    if (preClose == ' ') { append(remaining); break }
                    val inner = remaining.substring(1, end)
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(buildInlineAnnotatedString(inner, linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges))
                    }
                    remaining = remaining.substring(end + 1)
                }
            }
            // Inline code: `text`
            remaining.startsWith("`") -> {
                val end = remaining.indexOf("`", 1)
                if (end == -1) { append(remaining); break }
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBgColor)) {
                    append(remaining.substring(1, end))
                }
                remaining = remaining.substring(end + 1)
            }
            // URL link: [text](url)
            remaining.startsWith("[") -> {
                val match = url.find(remaining)
                if (match != null) {
                    if (match.range.first > 0) append(remaining.substring(0, match.range.first))
                    withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                        append(buildInlineAnnotatedString(match.groupValues[1], linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges))
                    }
                    remaining = remaining.substring(match.range.last + 1)
                } else { append(remaining); break }
            }
            // Plain text until next special marker
            else -> {
                val spanIdx = remaining.indexOf("<span", ignoreCase = true).let { if (it < 0) -1 else it }
                val brIdx = remaining.indexOf("<br", ignoreCase = true).let { if (it < 0) -1 else it }
                val blankIdx = if (hasBlanks) remaining.indexOf("<blank>") else -1
                val nextSpecial = listOf(
                    remaining.indexOf("**"), remaining.indexOf("~~"), remaining.indexOf("*"),
                    remaining.indexOf("`"), remaining.indexOf("["), remaining.indexOf("\\("),
                    remaining.indexOf("$"), spanIdx, brIdx, blankIdx
                ).filter { it >= 0 }.minOrNull() ?: remaining.length
                append(remaining.substring(0, nextSpecial))
                remaining = remaining.substring(nextSpecial)
            }
        }
    }
}

// ── HTML <table> → Markdown table converter ───────────────────────────────

/** Convert `<table>` HTML blocks to Markdown table format for downstream rendering. */
internal fun convertHtmlTablesToMd(text: String): String {
    val tableRegex = Regex("""(?is)<table\b[^>]*>(.*?)</table\s*>""")
    return tableRegex.replace(text) { match ->
        val inner = match.groupValues[1]
        val rows = mutableListOf<List<String>>()
        val trRegex = Regex("""(?is)<tr\b[^>]*>(.*?)</tr\s*>""")
        trRegex.findAll(inner).forEach { trMatch ->
            val cells = mutableListOf<String>()
            val tdRegex = Regex("""(?is)<t[hd]\b[^>]*>(.*?)</t[hd]\s*>""")
            tdRegex.findAll(trMatch.groupValues[1]).forEach { tdMatch ->
                cells.add(tdMatch.groupValues[1].replace(Regex("<[^>]+>"), "").trim())
            }
            if (cells.isNotEmpty()) rows.add(cells)
        }
        if (rows.size < 2) return@replace match.value // need at least header + one row
        val sb = StringBuilder()
        rows.forEachIndexed { ri, cells ->
            sb.append("| ").append(cells.joinToString(" | ")).append(" |\n")
            if (ri == 0) {
                sb.append("| ").append(cells.joinToString(" | ") { "---" }).append(" |\n")
            }
        }
        sb.toString()
    }
}

// ── Block-level renderers ───────────────────────────────────────────────

@Composable
fun MarkdownRenderer(text: String) {
    val baseStyle = MaterialTheme.typography.bodyMedium
    val monoStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val sanitized = remember(text) { stripDisallowedHtml(text) }
    val withTables = remember(sanitized) { convertHtmlTablesToMd(sanitized) }
    val lines = withTables.split("\n")
    var inCodeBlock = false
    var inDisplayMath = false
    val displayMathLines = remember { mutableStateListOf<String>() }
    val tableRows = remember(text) { mutableStateListOf<String>() }
    Column {
        lines.forEachIndexed { idx, line ->
            if (tableRows.isNotEmpty() && (!line.trimStart().startsWith("|") || !line.trimEnd().endsWith("|"))) {
                renderMdTable(tableRows.toList(), baseStyle)
                tableRows.clear()
            }
            when {
                // Display math $$...$$
                line.trim() == "$$" && !inDisplayMath -> {
                    inDisplayMath = true
                    displayMathLines.clear()
                }
                inDisplayMath && line.trim() == "$$" -> {
                    inDisplayMath = false
                    val formula = displayMathLines.joinToString("\n")
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MarkdownInline(formula, monoStyle.copy(color = Color(0xFF1565C0), fontStyle = FontStyle.Italic))
                    }
                    displayMathLines.clear()
                }
                inDisplayMath -> {
                    displayMathLines.add(line)
                }
                line.startsWith("```") -> { inCodeBlock = !inCodeBlock; Spacer(Modifier.height(4.dp)) }
                inCodeBlock -> MarkdownInline(line, monoStyle, Modifier.padding(start = 8.dp))
                line.trimStart().startsWith("|") && line.trimEnd().endsWith("|") -> {
                    tableRows.add(line)
                    if (idx == lines.lastIndex) {
                        renderMdTable(tableRows.toList(), baseStyle)
                        tableRows.clear()
                    }
                }
                line.startsWith("#### ") -> MarkdownInline(line.removePrefix("#### "), MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), Modifier.padding(top = 4.dp, bottom = 2.dp))
                line.startsWith("### ") -> MarkdownInline(line.removePrefix("### "), MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), Modifier.padding(top = 8.dp, bottom = 2.dp))
                line.startsWith("## ") -> MarkdownInline(line.removePrefix("## "), MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), Modifier.padding(top = 10.dp, bottom = 2.dp))
                line.startsWith("# ") -> MarkdownInline(line.removePrefix("# "), MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.15f, fontWeight = FontWeight.Bold), Modifier.padding(top = 10.dp, bottom = 4.dp))
                line.startsWith("> ") -> {
                    val content = line.substringAfter("> ")
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraSmall)
                    ) {
                        Box(Modifier.width(3.dp).height(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                        Spacer(Modifier.width(8.dp))
                        MarkdownInline(content, baseStyle.copy(fontStyle = FontStyle.Italic), Modifier.weight(1f))
                    }
                }
                // Unordered list at any nesting level
                (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) -> {
                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                    val level = ((indent.count { it == ' ' } + indent.count { it == '\t' } * 2) / 2).coerceAtMost(3)
                    val bullet = if (level >= 2) "· " else "▪ "
                    val bulletStyle = if (level == 1) baseStyle.copy(fontSize = baseStyle.fontSize * 0.75f) else baseStyle
                    val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                    Row(modifier = Modifier.padding(start = (8 + level * 16).dp, top = 2.dp)) {
                        Text(bullet, style = bulletStyle, modifier = Modifier.alignByBaseline())
                        MarkdownInline(content, baseStyle, Modifier.alignByBaseline())
                    }
                }
                line.matches(Regex("^\\d+\\..*")) -> {
                    val content = line.substringAfter(". ")
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                        Text("${line.substringBefore(".")}. ", style = baseStyle)
                        MarkdownInline(content, baseStyle)
                    }
                }
                line.matches(Regex("^-{3,}$")) -> {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(Modifier.height(4.dp))
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> MarkdownInline(line, baseStyle)
            }
        }
    }
}

@Composable
private fun renderMdTable(rows: List<String>, style: androidx.compose.ui.text.TextStyle) {
    if (rows.size < 2) return
    val parsed = rows.map { row ->
        row.trim().removeSurrounding("|").split("|").map { it.trim() }
    }
    val headerRow = parsed.first()
    val dataRows = parsed.drop(1).filter { cells ->
        !cells.all { it.matches(Regex("^[-: ]+$")) }
    }
    if (headerRow.isEmpty()) return
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val dividerColor = borderColor.copy(alpha = 0.5f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, dividerColor, MaterialTheme.shapes.small)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(dividerColor.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
            headerRow.forEachIndexed { ci, cell ->
                if (ci > 0) VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 2.dp), thickness = 0.5.dp, color = dividerColor)
                MarkdownInline(cell, style.copy(fontWeight = FontWeight.Bold), Modifier.weight(1f))
            }
        }
        // Data rows
        dataRows.forEachIndexed { ri, cells ->
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 10.dp, vertical = 4.dp)) {
                cells.take(headerRow.size).forEachIndexed { ci, cell ->
                    if (ci > 0) VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 2.dp), thickness = 0.5.dp, color = dividerColor)
                    MarkdownInline(cell, style, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MarkdownInline(text: String, style: androidx.compose.ui.text.TextStyle, modifier: Modifier = Modifier) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val annotated = remember(text, linkColor, codeBgColor) {
        buildInlineAnnotatedString(text, linkColor, codeBgColor)
    }
    Text(annotated, style = style, modifier = modifier)
}
