package com.sunjk.sunjktool.util

/**
 * 题集解析 ↔ 学习记录 AI 总结 双向关联的链接工具。
 *
 * 内部链接使用伪协议 URL：`sunjktool://log/{entryId}?heading={headingId}`，
 * 生成端把它写成 markdown 链接 `[⤴ 标题](sunjktool://log/…)` 嵌入解析正文，
 * MarkdownRenderer 识别该协议渲染为可点击的圆形图标；反向侧用 [extractInternalLinks]
 * 从正文里扫描出所有 (entryId, headingId)，即可统计"某章节被哪些题目引用"。
 */
object SummaryLinkHelper {

    /** 内部链接伪协议前缀。 */
    const val SCHEME_PREFIX = "sunjktool://log/"

    private val REF_MARKER_REGEX = Regex("""\[\[ref:(\d+)\]\]""")
    private val INTERNAL_LINK_REGEX = Regex("""sunjktool://log/(\d+)\?heading=([^\s\)\]\x5d]+)""")

    /** 构造内部链接 URL：`sunjktool://log/{entryId}?heading={headingId}`。 */
    fun buildInternalLinkUrl(logEntryId: Long, headingId: String): String =
        "$SCHEME_PREFIX$logEntryId?heading=$headingId"

    /** 构造 markdown 形式的内部链接文本：`[⤴ 标题](url)`。 */
    fun buildInternalLinkMarkdown(title: String, logEntryId: Long, headingId: String): String {
        val safeTitle = title.replace(Regex("""[\[\]]"""), "").trim()
        return "[⤴ $safeTitle](${buildInternalLinkUrl(logEntryId, headingId)})"
    }

    /**
     * 把 AI 生成的解析正文里的 `[[ref:N]]` 标记替换为可点击的内部链接。
     *
     * @param analysis 生成端返回的原始解析正文
     * @param refs 编号 → 引用目标 的有序列表（下标即 N）
     * @return 替换后的正文；越界的 N 直接丢弃（不留空链接、不降级成纯文本）
     */
    fun replaceRefMarkers(analysis: String, refs: List<SummaryLinkRef>): String =
        REF_MARKER_REGEX.replace(analysis) { m ->
            val idx = m.groupValues[1].toIntOrNull() ?: return@replace ""
            val ref = refs.getOrNull(idx) ?: return@replace ""
            if (ref.headingId.isBlank()) return@replace ""
            buildInternalLinkMarkdown(ref.title, ref.logEntryId, ref.headingId)
        }

    /** 从一段解析正文里提取所有内部链接目标（用于反向统计）。 */
    fun extractInternalLinks(analysis: String): List<InternalLinkTarget> =
        INTERNAL_LINK_REGEX.findAll(analysis).map { m ->
            InternalLinkTarget(
                logEntryId = m.groupValues[1].toLongOrNull() ?: 0L,
                headingId = m.groupValues[2]
            )
        }.filter { it.logEntryId > 0L }.toList()

    /** 判断一个 markdown 链接 URL 是否为内部链接。 */
    fun isInternalLinkUrl(url: String): Boolean = url.startsWith(SCHEME_PREFIX)

    private val INTERNAL_LINK_PARSE_REGEX = Regex("""^sunjktool://log/(\d+)\?heading=(.+)$""")

    /** 解析内部链接 URL 为目标（entryId + headingId）；非内部链接返回 null。 */
    fun parseInternalLinkUrl(url: String): InternalLinkTarget? {
        val m = INTERNAL_LINK_PARSE_REGEX.matchEntire(url) ?: return null
        val entryId = m.groupValues[1].toLongOrNull() ?: return null
        if (entryId <= 0L) return null
        val headingId = m.groupValues[2].trim()
        if (headingId.isEmpty()) return null
        return InternalLinkTarget(entryId, headingId)
    }
}

/** `[[ref:N]]` 编号对应的引用目标（生成端在写背景知识时按序建立）。 */
data class SummaryLinkRef(
    val logEntryId: Long,
    val headingId: String,
    val title: String
)

/** 从解析正文提取出的一个内部链接目标。 */
data class InternalLinkTarget(
    val logEntryId: Long,
    val headingId: String
)

/** 反向关联中"引用某章节的一道题目"（列表页条目用）。 */
data class QuestionLinkRef(
    val questionId: Long,
    val categoryId: Long,
    val categoryName: String,
    val content: String
)
