@file:Suppress("unused")

package com.sunjk.sunjktool.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

internal enum class TableColumnAlignment { LEFT, CENTER, RIGHT }

internal data class MarkdownTable(
    val header: List<String>,
    val rows: List<List<String>>,
    val alignments: List<TableColumnAlignment>
)

private val tableDelimiterCell = Regex("^:?-{3,}:?$")

internal data class MarkdownRenderBlock(val content: String, val key: String)

/** Split completed Markdown into lazy-renderable blocks without breaking tables/code/math. */
internal fun splitMarkdownBlocks(markdown: String): List<MarkdownRenderBlock> {
    if (markdown.isBlank()) return emptyList()
    val lines = markdown.replace("\r\n", "\n").split('\n')
    val blocks = mutableListOf<MarkdownRenderBlock>()
    val current = mutableListOf<String>()
    var fenced = false
    var displayMath = false
    var inTable = false
    var blockIndex = 0

    fun flush() {
        while (current.lastOrNull()?.isNullOrBlank() == true) current.removeAt(current.lastIndex)
        if (current.isNotEmpty()) {
            val content = current.joinToString("\n")
            blocks += MarkdownRenderBlock(content, "block-$blockIndex-${content.hashCode()}")
            blockIndex++
            current.clear()
        }
    }

    lines.forEachIndexed { index, line ->
        val trimmed = line.trim()
        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            current += line
            fenced = !fenced
            return@forEachIndexed
        }
        if (fenced) {
            current += line
            return@forEachIndexed
        }
        if (trimmed == "$$") {
            current += line
            displayMath = !displayMath
            if (!displayMath) flush()
            return@forEachIndexed
        }
        if (displayMath) {
            current += line
            return@forEachIndexed
        }
        val rowCells = splitMarkdownTableRow(line)
        val nextCells = if (index + 1 < lines.size) splitMarkdownTableRow(lines[index + 1]) else emptyList()
        val startsTable = rowCells.size > 1 && nextCells.size == rowCells.size && nextCells.all { tableDelimiterCell.matches(it) }
        if (startsTable) inTable = true
        if (inTable) {
            val isTableRow = rowCells.size > 1
            if (isTableRow) {
                current += line
                return@forEachIndexed
            }
            flush()
            inTable = false
        }
        if (trimmed.isBlank()) flush() else current += line
    }
    flush()
    return blocks
}



internal fun splitMarkdownTableRow(line: String): List<String> {
    val source = line.trim()
    val start = if (source.startsWith("|")) 1 else 0
    val end = if (source.endsWith("|") && source.length > start) source.length - 1 else source.length
    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var escaped = false
    for (index in start until end) {
        val char = source[index]
        if (escaped) {
            cell.append(char)
            escaped = false
        } else if (char == '\\') {
            escaped = true
        } else if (char == '|') {
            cells += cell.toString().trim()
            cell.clear()
        } else {
            cell.append(char)
        }
    }
    if (escaped) cell.append('\\')
    cells += cell.toString().trim()
    return cells
}

internal fun parseMarkdownTable(headerLine: String, delimiterLine: String, bodyLines: List<String>): MarkdownTable? {
    val header = splitMarkdownTableRow(headerLine)
    val delimiter = splitMarkdownTableRow(delimiterLine)
    if (header.isEmpty() || delimiter.size != header.size || delimiter.any { !tableDelimiterCell.matches(it) }) return null
    val alignments = delimiter.map {
        when {
            it.startsWith(":") && it.endsWith(":") -> TableColumnAlignment.CENTER
            it.startsWith(":") -> TableColumnAlignment.LEFT
            it.endsWith(":") -> TableColumnAlignment.RIGHT
            else -> TableColumnAlignment.LEFT
        }
    }
    val rows = bodyLines.map { line ->
        val parsed = splitMarkdownTableRow(line).toMutableList()
        if (parsed.size > header.size) {
            parsed[header.lastIndex] = parsed.drop(header.size - 1).joinToString(" | ")
            while (parsed.size > header.size) parsed.removeAt(parsed.lastIndex)
        }
        parsed + List(header.size - parsed.size) { "" }
    }
    return MarkdownTable(header, rows, alignments)
}

/** Returns widths whose sum is exactly available when the table overflows. */
internal fun solveTableColumnWidths(
    idealWidths: List<Float>,
    available: Float,
    minimumWidths: List<Float>
): List<Float> {
    if (idealWidths.isEmpty()) return emptyList()
    require(idealWidths.size == minimumWidths.size)
    val total = idealWidths.sum()
    if (total <= available) return idealWidths

    val floors = minimumWidths.map { it.coerceAtLeast(0f) }
    val floorTotal = floors.sum()
    if (floorTotal >= available) {
        // Extremely narrow parent: preserve proportions of the minimums so the
        // solver still returns an exact total instead of overflowing the row.
        val scale = available / floorTotal.coerceAtLeast(1f)
        return floors.map { it * scale }
    }

    val result = floors.toMutableList()
    var remaining = available - floorTotal
    val expandable = idealWidths.mapIndexed { index, ideal ->
        (ideal - floors[index]).coerceAtLeast(0f)
    }
    val expandableTotal = expandable.sum()
    if (expandableTotal > 0f) {
        expandable.forEachIndexed { index, weight ->
            result[index] += remaining * weight / expandableTotal
        }
    } else {
        val share = remaining / result.size
        result.indices.forEach { result[it] += share }
    }
    return result
}

internal fun solveTableColumnWidths(idealWidths: List<Float>, available: Float, minimum: Float = 36f): List<Float> =
    solveTableColumnWidths(idealWidths, available, List(idealWidths.size) { minimum })
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
            // URL link: [text](url) —— sunjktool:// 为内部链接，渲染为可点击的链接样式（pushStringAnnotation + ClickableText）
            remaining.startsWith("[") -> {
                val match = url.find(remaining)
                if (match != null) {
                    if (match.range.first > 0) append(remaining.substring(0, match.range.first))
                    val label = match.groupValues[1]
                    val rawUrl = match.groupValues[2]
                    if (rawUrl.startsWith("sunjktool://")) {
                        pushStringAnnotation("internal_link", rawUrl)
                        // label 已含「⤴」前缀（由 SummaryLinkHelper 生成），此处直接渲染 label，避免重复
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium)) {
                            append(buildInlineAnnotatedString(label, linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges))
                        }
                    } else {
                        withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                            append(buildInlineAnnotatedString(label, linkColor, codeBgColor, blankRevealedSet, blankColor, blankIndexCounter, blankRanges))
                        }
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

// ── HTML <div> → block marker converter ──────────────────────────────────

/**
 * Internal block markers produced by [convertHtmlDivsToBlocks].
 * `:::div-center` / `:::div-right` / `:::div-left` open a div block (carrying
 * the parsed `text-align`), `:::div-end` closes it. Content between markers is
 * left untouched so nested markdown / tables / HTML still render normally.
 */
internal val divOpenMarkerRegex = Regex("""^:::div-(center|right|left)""")
internal const val DIV_END_MARKER = ":::div-end"

/**
 * Convert HTML `<div ...>` / `</div>` tags into internal block markers so the
 * block renderer can wrap div content with alignment. Only `text-align` is
 * honoured (center/right); any other div degrades to a plain left-aligned
 * block. Nested divs are supported: inner markers are kept in the buffered
 * content and handled by the recursive render pass.
 */
internal fun convertHtmlDivsToBlocks(text: String): String {
    val openRegex = Regex("""(?i)<div\b[^>]*>""")
    val alignRegex = Regex("""(?i)\btext-align\s*:\s*(center|right)\b""")
    // Close tags first — opens and closes never overlap, order is irrelevant.
    var result = text.replace(Regex("""(?is)</div\s*>"""), DIV_END_MARKER)
    result = result.replace(openRegex) { m ->
        val align = alignRegex.find(m.value)?.groupValues?.get(1)?.lowercase()
        if (align == "center" || align == "right") ":::div-$align" else ":::div-left"
    }
    return result
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
                val escapedCells = cells.map { it.replace("\\", "\\\\").replace("|", "\\|") }
                sb.append("| ").append(escapedCells.joinToString(" | ")).append(" |\n")
                if (ri == 0) {
                    sb.append("| ").append(cells.joinToString(" | ") { "---" }).append(" |\n")
                }
            }
        sb.toString()
    }
}

// ── Block-level renderers ───────────────────────────────────────────────

@Composable
fun MarkdownRenderer(text: String, onInternalLinkClick: ((String) -> Unit)? = null) {
    val baseStyle = MaterialTheme.typography.bodyMedium
    val monoStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val sanitized = remember(text) { stripDisallowedHtml(text) }
    val withTables = remember(sanitized) { convertHtmlTablesToMd(sanitized) }
    val withDivBlocks = remember(withTables) { convertHtmlDivsToBlocks(withTables) }
    val lines = remember(withDivBlocks) { withDivBlocks.split('\n') }
    var inCodeBlock = false
    var inDisplayMath = false
    val displayMathLines = mutableListOf<String>()
    val tableRows = mutableListOf<String>()
    // <div> 块缓冲：标记之间的行先收集，闭合后递归走完整 Markdown 渲染
    val divBlockLines = mutableListOf<String>()
    var divDepth = 0
    var divAlign: String? = null
    Column {
        lines.forEachIndexed { idx, line ->
            // 正在缓冲 <div> 块内容（嵌套标记留在缓冲里交给递归渲染处理）
            if (divDepth > 0) {
                val trimmed = line.trim()
                val openMatch = divOpenMarkerRegex.find(trimmed)
                when {
                    openMatch != null -> { divDepth++; divBlockLines.add(line) }
                    trimmed == DIV_END_MARKER -> {
                        divDepth--
                        if (divDepth > 0) {
                            divBlockLines.add(line)
                        } else {
                            renderDivBlock(divBlockLines.joinToString("\n"), divAlign, onInternalLinkClick)
                            divBlockLines.clear(); divAlign = null
                        }
                    }
                    else -> divBlockLines.add(line)
                }
                if (idx == lines.lastIndex && divDepth > 0) {
                    // 未闭合的 <div>：按已有内容兜底渲染
                    renderDivBlock(divBlockLines.joinToString("\n"), divAlign, onInternalLinkClick)
                    divBlockLines.clear(); divDepth = 0; divAlign = null
                }
                return@forEachIndexed
            }
            if (tableRows.isNotEmpty() && splitMarkdownTableRow(line).size < 2) {
                renderMdTable(tableRows.toList(), baseStyle, onInternalLinkClick)
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
                        MarkdownInline(formula, monoStyle.copy(color = Color(0xFF1565C0), fontStyle = FontStyle.Italic), internalLinkClick = onInternalLinkClick)
                    }
                    displayMathLines.clear()
                }
                inDisplayMath -> {
                    displayMathLines.add(line)
                }
                line.startsWith("```") -> { inCodeBlock = !inCodeBlock; Spacer(Modifier.height(4.dp)) }
                inCodeBlock -> MarkdownInline(line, monoStyle, Modifier.padding(start = 8.dp), internalLinkClick = onInternalLinkClick)
                line.trimStart().startsWith("|") || line.contains("|") -> {
                    val isTableRow = splitMarkdownTableRow(line).size > 1
                    val nextIsDelimiter = idx + 1 < lines.size && splitMarkdownTableRow(lines[idx + 1]).all { tableDelimiterCell.matches(it) }
                    if (tableRows.isEmpty()) {
                        if (isTableRow && nextIsDelimiter) tableRows.add(line) else MarkdownInline(line, baseStyle, internalLinkClick = onInternalLinkClick)
                    } else if (isTableRow) {
                        tableRows.add(line)
                        if (idx == lines.lastIndex) {
                            renderMdTable(tableRows.toList(), baseStyle, onInternalLinkClick)
                            tableRows.clear()
                        }
                    } else {
                        renderMdTable(tableRows.toList(), baseStyle, onInternalLinkClick)
                        tableRows.clear()
                        MarkdownInline(line, baseStyle, internalLinkClick = onInternalLinkClick)
                    }
                }
                divOpenMarkerRegex.find(line.trim()) != null -> {
                    val match = divOpenMarkerRegex.find(line.trim())!!
                    divDepth = 1
                    divAlign = match.groupValues[1]
                    divBlockLines.clear()
                    val trailing = line.trim().substring(match.range.last + 1)
                    if (trailing.isNotBlank()) divBlockLines.add(trailing.trim())
                }
                // 孤立的 </div>（无对应开始标记）：跳过，避免渲染出原始标记文本
                line.trim() == DIV_END_MARKER -> Unit
                line.startsWith("#### ") -> MarkdownInline(line.removePrefix("#### "), MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), Modifier.padding(top = 4.dp, bottom = 2.dp), internalLinkClick = onInternalLinkClick)
                line.startsWith("### ") -> MarkdownInline(line.removePrefix("### "), MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), Modifier.padding(top = 8.dp, bottom = 2.dp), internalLinkClick = onInternalLinkClick)
                line.startsWith("## ") -> MarkdownInline(line.removePrefix("## "), MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), Modifier.padding(top = 10.dp, bottom = 2.dp), internalLinkClick = onInternalLinkClick)
                line.startsWith("# ") -> MarkdownInline(line.removePrefix("# "), MaterialTheme.typography.titleLarge.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize * 1.15f, fontWeight = FontWeight.Bold), Modifier.padding(top = 10.dp, bottom = 4.dp), internalLinkClick = onInternalLinkClick)
                line.startsWith("> ") -> {
                    val content = line.substringAfter("> ")
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.extraSmall)
                    ) {
                        Box(Modifier.width(3.dp).height(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                        Spacer(Modifier.width(8.dp))
                        MarkdownInline(content, baseStyle.copy(fontStyle = FontStyle.Italic), Modifier.weight(1f), internalLinkClick = onInternalLinkClick)
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
                        MarkdownInline(content, baseStyle, Modifier.alignByBaseline(), internalLinkClick = onInternalLinkClick)
                    }
                }
                line.matches(Regex("^\\d+\\..*")) -> {
                    val content = line.substringAfter(". ")
                    Row(modifier = Modifier.padding(start = 8.dp, top = 2.dp)) {
                        Text("${line.substringBefore(".")}. ", style = baseStyle)
                        MarkdownInline(content, baseStyle, internalLinkClick = onInternalLinkClick)
                    }
                }
                line.matches(Regex("^-{3,}$")) -> {
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(Modifier.height(4.dp))
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> MarkdownInline(line, baseStyle, internalLinkClick = onInternalLinkClick)
            }
        }
    }
}

/**
 * Render the buffered content of an HTML `<div>` block. The content goes
 * through the full [MarkdownRenderer] pipeline recursively (tables, math,
 * inline HTML all work), wrapped in a full-width Box honouring `text-align`.
 */
@Composable
private fun renderDivBlock(content: String, align: String?, onInternalLinkClick: ((String) -> Unit)?) {
    if (content.isBlank()) return
    val alignment = when (align) {
        "center" -> Alignment.Center
        "right" -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        MarkdownRenderer(content, onInternalLinkClick)
    }
}

@Composable
private fun renderMdTable(rows: List<String>, style: androidx.compose.ui.text.TextStyle, internalLinkClick: ((String) -> Unit)? = null) {
    if (rows.size < 2) return
    val table = parseMarkdownTable(rows[0], rows[1], rows.drop(2)) ?: return
    val colCount = table.header.size
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val horizontalPaddingPx = with(density) { 16.dp.toPx() }
    val dividerWidthPx = with(density) { 0.5.dp.toPx() }
    val maxColumnPx = with(density) { 320.dp.toPx() }

    BoxWithConstraints {
        val availablePx = constraints.maxWidth.toFloat()
        val measureStyle = style.copy(textAlign = TextAlign.Left)
        val idealWidths = remember(table, style, availablePx) {
            List(colCount) { column ->
                val content = (listOf(table.header) + table.rows).map { it[column] }
                val maxTextWidth = content.maxOf { value ->
                    textMeasurer.measure(
                        AnnotatedString(value),
                        if (value == table.header.getOrNull(column)) measureStyle.copy(fontWeight = FontWeight.Bold) else measureStyle,
                        constraints = Constraints(maxWidth = maxColumnPx.toInt())
                    ).size.width.toFloat()
                }
                (maxTextWidth + horizontalPaddingPx).coerceAtLeast(horizontalPaddingPx + with(density) { 8.dp.toPx() })
            }
        }
        val minimumWidths = remember(table, style) {
            List(colCount) { column ->
                val values = (listOf(table.header) + table.rows).map { it[column] }.filter { it.isNotEmpty() }
                val minimumCharacterWidth = values
                    .flatMap { it.toList() }
                    .maxOfOrNull { character ->
                        textMeasurer.measure(AnnotatedString(character.toString()), measureStyle).size.width.toFloat()
                    } ?: with(density) { 8.dp.toPx() }
                horizontalPaddingPx + minimumCharacterWidth
            }
        }
        val dividerTotal = dividerWidthPx * (colCount - 1)
        val cellAvailable = (availablePx - dividerTotal).coerceAtLeast(1f)
        val widths = solveTableColumnWidths(idealWidths, cellAvailable, minimumWidths)
        val tableWidth = widths.sum() + dividerTotal
        val tableModifier = if (tableWidth >= availablePx - 0.5f) Modifier.fillMaxWidth()
        else Modifier.width(with(density) { tableWidth.toDp() })
        val borderColor = MaterialTheme.colorScheme.outlineVariant
        val dividerColor = borderColor.copy(alpha = 0.55f)
        val cellHeightPadding = 8.dp

        Column(
            modifier = tableModifier
                .padding(vertical = 6.dp)
                .border(1.dp, dividerColor, MaterialTheme.shapes.small)
        ) {
            TableRow(
                cells = table.header,
                widths = widths,
                alignments = table.alignments,
                style = style.copy(fontWeight = FontWeight.Bold),
                horizontalPadding = horizontalPaddingPx,
                verticalPadding = 6.dp,
                background = dividerColor.copy(alpha = 0.3f),
                dividerColor = dividerColor,
                internalLinkClick = internalLinkClick
            )
            table.rows.forEach { row ->
                HorizontalDivider(thickness = with(density) { dividerWidthPx.toDp() }, color = dividerColor)
                TableRow(
                    cells = row,
                    widths = widths,
                    alignments = table.alignments,
                    style = style,
                    horizontalPadding = horizontalPaddingPx,
                    verticalPadding = cellHeightPadding,
                    background = Color.Transparent,
                    dividerColor = dividerColor,
                    internalLinkClick = internalLinkClick
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    widths: List<Float>,
    alignments: List<TableColumnAlignment>,
    style: androidx.compose.ui.text.TextStyle,
    horizontalPadding: Float,
    verticalPadding: androidx.compose.ui.unit.Dp,
    background: Color,
    dividerColor: Color,
    internalLinkClick: ((String) -> Unit)?
) {
    val density = LocalDensity.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(background)
    ) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) VerticalDivider(thickness = 0.5.dp, color = dividerColor)
            val alignment = when (alignments.getOrElse(index) { TableColumnAlignment.LEFT }) {
                TableColumnAlignment.LEFT -> TextAlign.Left
                TableColumnAlignment.CENTER -> TextAlign.Center
                TableColumnAlignment.RIGHT -> TextAlign.Right
            }
            Box(
                modifier = Modifier
                    .width(with(density) { widths[index].toDp() })
                    .fillMaxHeight()
                    .padding(horizontal = with(density) { horizontalPadding.toDp() / 2 }, vertical = verticalPadding),
                contentAlignment = Alignment.Center
            ) {
                MarkdownInline(cell, style.copy(textAlign = alignment), internalLinkClick = internalLinkClick)
            }
        }
    }
}


@Composable
fun MarkdownInline(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    internalLinkClick: ((String) -> Unit)? = null
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBgColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val annotated = remember(text, linkColor, codeBgColor) {
        buildInlineAnnotatedString(text, linkColor, codeBgColor)
    }
    if (internalLinkClick != null) {
        ClickableText(
            text = annotated,
            style = style,
            modifier = modifier,
            onClick = { offset ->
                annotated.getStringAnnotations("internal_link", offset, offset)
                    .firstOrNull()?.let { internalLinkClick(it.item) }
            }
        )
    } else {
        Text(annotated, style = style, modifier = modifier)
    }
}
