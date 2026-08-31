package com.sunjk.sunjktool.util

/**
 * 从 AI 总结 Markdown 中解析标题大纲，用于平板端双栏大纲面板。
 *
 * 支持 # / ## / ### / #### 四级标题。忽略代码块内的行首 #（通过简单的围栏状态判断）。
 */
object MarkdownOutlineParser {

    private val HEADING_REGEX = Regex("^(#{1,4})\\s+(.+?)\\s*#*\\s*$")

    /** 解析出所有标题（不含首个标题前的导语）。 */
    fun parseOutline(markdown: String): List<OutlineItem> {
        val result = mutableListOf<OutlineItem>()
        var inCodeBlock = false
        for (line in markdown.lines()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inCodeBlock = !inCodeBlock
                continue
            }
            if (inCodeBlock) continue
            val match = HEADING_REGEX.matchEntire(line) ?: continue
            val level = match.groupValues[1].length
            val title = match.groupValues[2].trim()
            if (title.isBlank()) continue
            result.add(OutlineItem(level, title, headingId(title)))
        }
        // 相同文字的小标题（如不同大标题下都有"易错提醒"）会生成相同 headingId，
        // 用于大纲 LazyColumn 的 key 会导致重复 key 闪退，这里保证全文档唯一。
        return result.deduplicateHeadingIds()
    }

    /** 将 markdown 按标题切分为 [MarkdownSection]。首个标题前的内容作为导语（heading = null）。 */
    fun splitSections(markdown: String): List<MarkdownSection> {
        val sections = mutableListOf<MarkdownSection>()
        var inCodeBlock = false
        var preamble = StringBuilder()
        var currentHeading: OutlineItem? = null
        var currentBody = StringBuilder()

        fun flush() {
            if (currentHeading == null) {
                if (preamble.isNotBlank()) {
                    sections.add(MarkdownSection(null, preamble.toString().trimEnd()))
                }
            } else {
                sections.add(MarkdownSection(currentHeading, currentBody.toString().trimEnd()))
            }
            currentHeading = null
            currentBody = StringBuilder()
        }

        for (line in markdown.lines()) {
            val trimmed = line.trimStart()
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                inCodeBlock = !inCodeBlock
                if (currentHeading == null) preamble.appendLine(line) else currentBody.appendLine(line)
                continue
            }
            if (inCodeBlock) {
                if (currentHeading == null) preamble.appendLine(line) else currentBody.appendLine(line)
                continue
            }
            val match = HEADING_REGEX.matchEntire(line)
            if (match != null) {
                val title = match.groupValues[2].trim()
                if (title.isNotBlank()) {
                    flush()
                    currentHeading = OutlineItem(match.groupValues[1].length, title, headingId(title))
                    currentBody = StringBuilder()
                    continue
                }
            }
            if (currentHeading == null) preamble.appendLine(line) else currentBody.appendLine(line)
        }
        flush()
        // 相同文字的小标题会生成相同 headingId，导致大纲 LazyColumn key 重复闪退，
        // 且点击跳转会定位到第一个同名标题。这里保证全文档唯一。
        val headings = sections.mapNotNull { it.heading }
        val deduped = headings.deduplicateHeadingIds()
        var i = 0
        return sections.map { section ->
            if (section.heading == null) section
            else section.copy(heading = deduped[i++])
        }
    }

    private fun headingId(title: String): String =
        title.trim()
            .replace(Regex("[^\\p{L}\\p{N}]"), "-")
            .replace(Regex("-{2,}"), "-")
            .trim('-')
            .ifBlank { "heading" }

    /** 去重：遇到重复的 headingId（同名标题）时追加后缀，确保整个文档内 key 唯一。 */
    private fun List<OutlineItem>.deduplicateHeadingIds(): List<OutlineItem> {
        val used = HashSet<String>()
        return map { item ->
            var id = item.headingId
            var suffix = 2
            while (!used.add(id)) {
                id = "${item.headingId}-$suffix"
                suffix++
            }
            item.copy(headingId = id)
        }
    }
}

data class OutlineItem(
    val level: Int,
    val title: String,
    val headingId: String,
    val offset: Int = 0
)

data class MarkdownSection(
    val heading: OutlineItem?,
    val body: String
)
