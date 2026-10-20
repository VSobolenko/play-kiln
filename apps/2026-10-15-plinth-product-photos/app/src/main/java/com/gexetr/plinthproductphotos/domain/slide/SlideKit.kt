package com.gexetr.plinthproductphotos.domain.slide

import com.gexetr.plinthproductphotos.domain.pixel.PixelBuffer
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import java.nio.ByteBuffer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

enum class SlideTextSlot(val wireValue: String) {
    TITLE("TITLE"),
    SPEC_1("SPEC_1"),
    SPEC_2("SPEC_2"),
    SPEC_3("SPEC_3"),
    BADGE("BADGE"),
    CAPTION("CAPTION"),
}

enum class SlideTemplate(
    val wireValue: String,
    val slots: Set<SlideTextSlot>,
) {
    HERO("hero", setOf(SlideTextSlot.TITLE, SlideTextSlot.BADGE)),
    SPEC3("spec3", setOf(SlideTextSlot.TITLE, SlideTextSlot.SPEC_1, SlideTextSlot.SPEC_2, SlideTextSlot.SPEC_3)),
    SIZE("size", setOf(SlideTextSlot.TITLE, SlideTextSlot.SPEC_1, SlideTextSlot.SPEC_2)),
    DETAIL("detail", setOf(SlideTextSlot.CAPTION)),
    KIT("kit", setOf(SlideTextSlot.TITLE, SlideTextSlot.CAPTION)),
    USE("use", setOf(SlideTextSlot.CAPTION)),
    VARIANTS("variants", setOf(SlideTextSlot.TITLE, SlideTextSlot.SPEC_1)),
    CLEAN("clean", emptySet()),
}

data class SlideTextLimit(
    val maxCharacters: Int,
    val ruleId: String,
) {
    init {
        require(maxCharacters > 0) { "Slide text limit must be positive" }
        require(ruleId.isNotBlank()) { "Slide text limit must name its marketplace rule" }
    }
}

data class SlideMarketplaceRules(
    val marketplaceId: String,
    val marketplaceName: String,
    val languageTag: String,
    val prohibitedCategories: Set<String>,
    val limits: Map<SlideTextSlot, SlideTextLimit> = emptyMap(),
    val mainBackgroundPolicy: String? = null,
    val phraseRules: Map<String, List<String>> = emptyMap(),
    val smallContrastMinimum: Double = 4.5,
    val largeContrastMinimum: Double = 3.0,
) {
    init {
        require(marketplaceId.isNotBlank())
        require(marketplaceName.isNotBlank())
        require(languageTag.isNotBlank())
        require(smallContrastMinimum > 0.0 && smallContrastMinimum.isFinite())
        require(largeContrastMinimum > 0.0 && largeContrastMinimum.isFinite())
    }
}

data class SlideTextFinding(
    val ruleId: String,
    val slot: SlideTextSlot,
    val category: String?,
    val offendingSubstring: String,
)

data class SlideTextCheck(
    val originalTexts: Map<SlideTextSlot, String>,
    val findings: List<SlideTextFinding>,
    val matcherAvailable: Boolean,
    val prohibitedListNotRead: Boolean,
    val unsupportedCategories: Set<String>,
) {
    val canRender: Boolean
        get() = findings.isEmpty() &&
            unsupportedCategories.isEmpty() &&
            (prohibitedListNotRead || matcherAvailable)
}

object SlideTextSafety {
    private val availableLanguages = setOf("en", "ru")
    private val lookAlikes = mapOf(
        'а' to 'a',
        'е' to 'e',
        'о' to 'o',
        'р' to 'p',
        'с' to 'c',
        'у' to 'y',
        'х' to 'x',
        'к' to 'k',
        'м' to 'm',
        'т' to 't',
        'в' to 'b',
        'н' to 'h',
        'і' to 'i',
        'ј' to 'j',
    )
    private val rulesByCategory = mapOf(
        "price_discount" to listOf(
            Regex("""(?<![\p{L}\p{N}])(?:\d{1,3}(?:[ .,\u00a0]\d{3})*(?:[.,]\d{1,2})?\s*(?:₽|руб(?:\.|лей|ля|ль)?|\$|€)|(?:\$|€)\s*\d+(?:[.,]\d{1,2})?)(?![\p{L}\p{N}])"""),
            Regex("""(?<![\p{L}\p{N}])-\s*\d{1,3}\s*%"""),
            Regex("""(?<![\p{L}\p{N}])(?:скидк\w*|распродаж\w*|акци\w*|дешев\w*)(?![\p{L}\p{N}])"""),
        ),
        "contacts_links" to listOf(
            Regex("""(?<![\p{L}\p{N}])[\w.!#$%&'*+/=?^`{|}~-]+@[\w.-]+\.[a-z]{2,}(?![\p{L}\p{N}])"""),
            Regex("""(?:https?://|www\.)[^\s<>()]+"""),
            Regex("""(?<![\p{L}\p{N}])@[\p{L}\p{N}_.-]+"""),
            Regex("""(?<!\d)\+?\d[\d ()-]{5,}\d(?!\d)"""),
            Regex("""(?<![\p{L}\p{N}])(?:telegram|телеграм(?:м)?|whatsapp|ватсап|viber|вайбер)\s*[:@-]?\s*(?:@[a-z0-9_.-]+|\+?\d[\d ()-]{4,}\d)(?![\p{L}\p{N}])"""),
        ),
        "evaluative_claim" to listOf(
            Regex("""(?<![\p{L}\p{N}])#\s*1(?![\p{L}\p{N}])"""),
            Regex("""(?<![\p{L}\p{N}])(?:хит\w*|топ\w*|лучш\w*|лидер\s+продаж|bestseller)(?![\p{L}\p{N}])"""),
        ),
        "sold_count" to listOf(
            Regex("""(?<![\p{L}\p{N}])(?:продано|sold)\s*[:#-]?\s*\d[\d ,.]*"""),
            Regex("""(?<![\p{L}\p{N}])\d[\d ,.]*\s*(?:шт\.?\s*)?(?:продано|sold)(?![\p{L}\p{N}])"""),
        ),
        "review_reward" to listOf(
            Regex("""(?<![\p{L}\p{N}])(?:отзыв\w*\s+за|кешбэк\w*\s+за|кэшбэк\w*\s+за|review\s+for|cashback\s+for)(?![\p{L}\p{N}])"""),
        ),
        "call_to_action" to listOf(
            Regex("""(?<![\p{L}\p{N}])(?:купи\w*|купите|звоните|закажи\w*|buy\s+now|call|order)(?![\p{L}\p{N}])"""),
        ),
        "delivery_returns" to listOf(
            Regex("""(?<![\p{L}\p{N}])(?:доставк\w*|возврат\w*|free\s+shipping|returns?)(?![\p{L}\p{N}])"""),
        ),
    ).mapValues { (_, patterns) ->
        patterns.map { pattern ->
            // Normalize literal Cyrillic words, leaving regex syntax and escapes intact.
            Regex(Regex("""\p{IsCyrillic}+""").replace(pattern.pattern) {
                normalizeWithSourceOffsets(it.value).text
            })
        }
    }
    private val supportedCategories = rulesByCategory.keys

    fun check(
        texts: Map<SlideTextSlot, String>,
        rules: SlideMarketplaceRules,
    ): SlideTextCheck {
        val language = rules.languageTag.substringBefore('-').lowercase(Locale.ROOT)
        val matcherAvailable = language in availableLanguages
        val unsupportedCategories = rules.prohibitedCategories - supportedCategories
        val findings = mutableListOf<SlideTextFinding>()
        val recordedMatches = mutableSetOf<Triple<SlideTextSlot, String, IntRange>>()

        if (matcherAvailable) {
            for ((slot, text) in texts) {
                val normalized = normalizeWithSourceOffsets(text)
                fun recordMatch(category: String, match: MatchResult) {
                    if (!recordedMatches.add(Triple(slot, category, match.range))) return
                    val start = normalized.sourceStarts[match.range.first]
                    val end = normalized.sourceEnds[match.range.last]
                    findings += SlideTextFinding(
                        ruleId = "L11",
                        slot = slot,
                        category = category,
                        offendingSubstring = text.substring(start, end),
                    )
                }
                for (category in rules.prohibitedCategories) {
                    if (category == "qr_code") continue
                    for (pattern in rulesByCategory[category].orEmpty()) {
                        pattern.findAll(normalized.text).forEach { match -> recordMatch(category, match) }
                    }
                    for (phrase in rules.phraseRules[category].orEmpty()) {
                        val normalizedPhrase = normalizeWithSourceOffsets(phrase).text
                        if (normalizedPhrase.isBlank()) continue
                        val phraseMatcher = Regex(
                            """(?<![\p{L}\p{N}])${Regex.escape(normalizedPhrase)}(?![\p{L}\p{N}])""",
                        )
                        phraseMatcher.findAll(normalized.text).forEach { match -> recordMatch(category, match) }
                    }
                }
            }
        }

        for ((slot, limit) in rules.limits) {
            val text = texts[slot] ?: continue
            val count = text.codePointCount(0, text.length)
            if (count > limit.maxCharacters) {
                val start = text.offsetByCodePoints(0, limit.maxCharacters)
                findings += SlideTextFinding(
                    ruleId = limit.ruleId,
                    slot = slot,
                    category = null,
                    offendingSubstring = text.substring(start),
                )
            }
        }
        return SlideTextCheck(
            originalTexts = texts.toMap(),
            findings = findings,
            matcherAvailable = matcherAvailable,
            prohibitedListNotRead = rules.prohibitedCategories.isEmpty(),
            unsupportedCategories = unsupportedCategories,
        )
    }

    private data class NormalizedText(
        val text: String,
        val sourceStarts: List<Int>,
        val sourceEnds: List<Int>,
    )

    private fun normalizeWithSourceOffsets(source: String): NormalizedText {
        val result = StringBuilder()
        val starts = mutableListOf<Int>()
        val ends = mutableListOf<Int>()
        var sourceOffset = 0
        while (sourceOffset < source.length) {
            val codePoint = source.codePointAt(sourceOffset)
            val sourceEnd = sourceOffset + Character.charCount(codePoint)
            val normalized = Normalizer.normalize(
                String(Character.toChars(codePoint)),
                Normalizer.Form.NFKD,
            ).lowercase(Locale.ROOT)
            for (character in normalized) {
                val category = Character.getType(character)
                if (category == Character.NON_SPACING_MARK.toInt() ||
                    category == Character.COMBINING_SPACING_MARK.toInt() ||
                    category == Character.ENCLOSING_MARK.toInt()
                ) continue
                val mapped = lookAlikes[character] ?: character
                result.append(mapped)
                starts += sourceOffset
                ends += sourceEnd
            }
            sourceOffset = sourceEnd
        }
        return NormalizedText(result.toString(), starts, ends)
    }
}

data class SlideDraft(
    val template: SlideTemplate,
    val texts: Map<SlideTextSlot, String>,
)

class SlideSeriesBackground private constructor(
    val id: String,
    private val source: PixelBuffer,
    val identity: String,
) {
    internal fun pixelsForComposition(
        width: Int = SlideLayoutEngine.DEFAULT_CANVAS_WIDTH,
        height: Int = SlideLayoutEngine.DEFAULT_CANVAS_HEIGHT,
    ): PixelBuffer {
        if (source.width == width && source.height == height) {
            return PixelBuffer(width, height, source.pixels.copyOf())
        }
        val sourceRatio = source.width.toDouble() / source.height
        val targetRatio = width.toDouble() / height
        return if (sourceRatio > targetRatio) {
            val cropWidth = (source.height * targetRatio).roundToInt().coerceIn(1, source.width)
            source.crop((source.width - cropWidth) / 2, 0, cropWidth, source.height).scale(width, height)
        } else {
            val cropHeight = (source.width / targetRatio).roundToInt().coerceIn(1, source.height)
            source.crop(0, (source.height - cropHeight) / 2, source.width, cropHeight).scale(width, height)
        }
    }

    companion object {
        fun create(id: String, pixels: PixelBuffer): SlideSeriesBackground {
            require(id.isNotBlank()) { "A saved series background needs an id" }
            val stablePixels = PixelBuffer(pixels.width, pixels.height, pixels.pixels.copyOf())
            return SlideSeriesBackground(id, stablePixels, digest(stablePixels))
        }

        private fun digest(buffer: PixelBuffer): String {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(buffer.width.toBytes())
            digest.update(buffer.height.toBytes())
            val chunkSize = 4096
            val bytes = ByteBuffer.allocate(chunkSize * Int.SIZE_BYTES)
            for (start in buffer.pixels.indices step chunkSize) {
                bytes.clear()
                val end = min(buffer.pixels.size, start + chunkSize)
                for (index in start until end) bytes.putInt(buffer.pixels[index])
                digest.update(bytes.array(), 0, bytes.position())
            }
            return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        }

        private fun Int.toBytes(): ByteArray = byteArrayOf(
            (this ushr 24).toByte(),
            (this ushr 16).toByte(),
            (this ushr 8).toByte(),
            toByte(),
        )
    }
}

data class SlideStyle(
    val id: String,
    val plateColorArgb: Int = 0xffffffff.toInt(),
    val accentColorArgb: Int = 0xff315b54.toInt(),
) {
    init {
        require(id.isNotBlank()) { "A slide style needs an id" }
        require(plateColorArgb ushr 24 == 0xff) { "Slide text plates must be opaque for contrast measurement" }
    }
}

data class SlideRect(val left: Int, val top: Int, val width: Int, val height: Int) {
    init {
        require(width >= 0 && height >= 0)
    }

    val right: Int get() = left + width
    val bottom: Int get() = top + height
}

sealed interface SlideShape {
    data class Plate(val bounds: SlideRect, val colorArgb: Int) : SlideShape
    data class Line(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int,
        val colorArgb: Int,
        val arrowHeads: Boolean = false,
    ) : SlideShape
}

data class SlideTextPlacement(
    val slot: SlideTextSlot,
    val text: String,
    val bounds: SlideRect,
    val fontSizePx: Float,
    val textColorArgb: Int,
    val plateColorArgb: Int,
    val contrastRatio: Double,
    val contrastMinimum: Double,
)

data class SlideRenderPlan(
    val template: SlideTemplate,
    val background: SlideSeriesBackground,
    val backgroundIdentity: String,
    val baseImage: PixelBuffer,
    val itemSources: List<PixelBuffer>,
    val itemBounds: List<SlideRect>,
    val shapes: List<SlideShape>,
    val textBlocks: List<SlideTextPlacement>,
    val style: SlideStyle,
)

class SlideRenderRefusedException(val checks: List<SlideTextCheck>) :
    IllegalArgumentException("Slide rendering was refused because text safety checks did not pass")

object SlideContrast {
    fun ratio(foreground: Int, background: Int): Double {
        val first = relativeLuminance(foreground)
        val second = relativeLuminance(background)
        return (max(first, second) + 0.05) / (min(first, second) + 0.05)
    }

    fun bestTextColor(plateColorArgb: Int): Pair<Int, Double> {
        val black = 0xff000000.toInt()
        val white = 0xffffffff.toInt()
        val blackRatio = ratio(PixelBuffer.fromArgb(black), PixelBuffer.fromArgb(plateColorArgb))
        val whiteRatio = ratio(PixelBuffer.fromArgb(white), PixelBuffer.fromArgb(plateColorArgb))
        return if (blackRatio >= whiteRatio) black to blackRatio else white to whiteRatio
    }

    private fun relativeLuminance(rgba: Int): Double {
        fun channel(shift: Int): Double {
            val encoded = (rgba ushr shift and 0xff) / 255.0
            return if (encoded <= 0.04045) encoded / 12.92 else ((encoded + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(24) + 0.7152 * channel(16) + 0.0722 * channel(8)
    }

    private fun Double.pow(power: Double): Double = Math.pow(this, power)
}

object SlideLayoutEngine {
    const val DEFAULT_CANVAS_WIDTH = 900
    const val DEFAULT_CANVAS_HEIGHT = 1200
    const val TITLE_FONT_SIZE_PX = 60
    const val BODY_FONT_SIZE_PX = 40
    const val SAFE_MARGIN_PX = 60

    fun renderSeries(
        slides: List<SlideDraft>,
        items: List<PixelBuffer>,
        background: SlideSeriesBackground,
        style: SlideStyle,
        marketplaceRules: SlideMarketplaceRules,
    ): List<SlideRenderPlan> {
        require(slides.isNotEmpty()) { "A slide series must contain at least one slide" }
        require(items.isNotEmpty()) { "A slide series needs at least one original item image" }
        val checks = slides.map { SlideTextSafety.check(it.texts, marketplaceRules) }
        if (checks.any { !it.canRender }) throw SlideRenderRefusedException(checks)
        if (marketplaceRules.mainBackgroundPolicy == "white-255" &&
            slides.any { it.template == SlideTemplate.CLEAN } &&
            !background.pixelsForComposition().pixels.all { it == PixelBuffer.rgba(255, 255, 255) }
        ) {
            throw IllegalArgumentException("The clean template needs the saved white-255 background for this marketplace")
        }
        return slides.map { draft ->
            val templateItems = if (draft.template == SlideTemplate.KIT) items else listOf(items.first())
            layout(draft, templateItems, background, style, marketplaceRules)
        }
    }

    private fun layout(
        draft: SlideDraft,
        items: List<PixelBuffer>,
        background: SlideSeriesBackground,
        style: SlideStyle,
        marketplaceRules: SlideMarketplaceRules,
    ): SlideRenderPlan {
        val originalBackground = background.pixelsForComposition()
        val canvasWidth = originalBackground.width
        val canvasHeight = originalBackground.height
        val scale = min(
            canvasWidth.toFloat() / DEFAULT_CANVAS_WIDTH,
            canvasHeight.toFloat() / DEFAULT_CANVAS_HEIGHT,
        )
        val margin = (SAFE_MARGIN_PX * scale).roundToInt().coerceAtLeast(1)
        val titleHeight = (TITLE_FONT_SIZE_PX * scale * 1.5f).roundToInt()
        val bodyHeight = (BODY_FONT_SIZE_PX * scale * 1.5f).roundToInt()
        val titleFont = TITLE_FONT_SIZE_PX * scale
        val bodyFont = BODY_FONT_SIZE_PX * scale
        val content = SlideRect(margin, margin, canvasWidth - 2 * margin, canvasHeight - 2 * margin)
        val itemArea: SlideRect
        val textAreas = mutableMapOf<SlideTextSlot, SlideRect>()
        val shapes = mutableListOf<SlideShape>()
        val titleArea = SlideRect(content.left, content.top, content.width, titleHeight)
        val bottomArea = SlideRect(
            content.left,
            content.bottom - bodyHeight,
            content.width,
            bodyHeight,
        )

        when (draft.template) {
            SlideTemplate.HERO -> {
                itemArea = SlideRect(
                    content.left,
                    titleArea.bottom,
                    content.width,
                    content.height - titleHeight - bodyHeight,
                )
                textAreas[SlideTextSlot.TITLE] = titleArea
                textAreas[SlideTextSlot.BADGE] = bottomArea
            }
            SlideTemplate.SPEC3 -> {
                textAreas[SlideTextSlot.TITLE] = titleArea
                val bodyTop = titleArea.bottom
                val bodyHeightAvailable = content.bottom - bodyTop
                val columnLeft = content.left + content.width / 2
                itemArea = SlideRect(content.left, bodyTop, content.width / 2, bodyHeightAvailable)
                val specHeight = bodyHeightAvailable / 3
                listOf(SlideTextSlot.SPEC_1, SlideTextSlot.SPEC_2, SlideTextSlot.SPEC_3)
                    .forEachIndexed { index, slot ->
                        textAreas[slot] = SlideRect(
                            columnLeft,
                            bodyTop + index * specHeight,
                            content.right - columnLeft,
                            if (index == 2) content.bottom - (bodyTop + index * specHeight) else specHeight,
                        )
                    }
            }
            SlideTemplate.SIZE -> {
                textAreas[SlideTextSlot.TITLE] = titleArea
                val bodyTop = titleArea.bottom
                val remainingHeight = content.bottom - bodyTop - bodyHeight
                itemArea = SlideRect(content.left, bodyTop, content.width, remainingHeight)
                textAreas[SlideTextSlot.SPEC_1] = SlideRect(
                    content.left,
                    content.bottom - bodyHeight,
                    content.width / 2,
                    bodyHeight,
                )
                textAreas[SlideTextSlot.SPEC_2] = SlideRect(
                    content.left + content.width / 2,
                    content.bottom - bodyHeight,
                    content.width - content.width / 2,
                    bodyHeight,
                )
            }
            SlideTemplate.DETAIL -> {
                itemArea = SlideRect(content.left, content.top, content.width, content.height - bodyHeight)
                textAreas[SlideTextSlot.CAPTION] = bottomArea
            }
            SlideTemplate.KIT -> {
                textAreas[SlideTextSlot.TITLE] = titleArea
                itemArea = SlideRect(
                    content.left,
                    titleArea.bottom,
                    content.width,
                    content.height - titleHeight - bodyHeight,
                )
                textAreas[SlideTextSlot.CAPTION] = bottomArea
            }
            SlideTemplate.USE -> {
                itemArea = SlideRect(content.left, content.top, content.width, content.height - bodyHeight)
                textAreas[SlideTextSlot.CAPTION] = bottomArea
            }
            SlideTemplate.VARIANTS -> {
                textAreas[SlideTextSlot.TITLE] = titleArea
                itemArea = SlideRect(
                    content.left,
                    titleArea.bottom,
                    content.width,
                    content.height - titleHeight - bodyHeight,
                )
                textAreas[SlideTextSlot.SPEC_1] = bottomArea
            }
            SlideTemplate.CLEAN -> {
                itemArea = content
            }
        }

        val baseImage = originalBackground
        val itemBounds = placeItems(
            canvas = baseImage,
            items = items,
            area = itemArea,
            grid = draft.template == SlideTemplate.KIT,
            cover = draft.template == SlideTemplate.DETAIL,
        )
        if (draft.template == SlideTemplate.SIZE && itemBounds.isNotEmpty()) {
            val item = itemBounds.first()
            val dimensionY = min(content.bottom - bodyHeight, item.bottom + margin / 4)
            shapes += SlideShape.Line(
                item.left,
                dimensionY,
                item.right,
                dimensionY,
                style.accentColorArgb,
                arrowHeads = true,
            )
            shapes += SlideShape.Line(
                item.left,
                item.top,
                item.left,
                item.bottom,
                style.accentColorArgb,
                arrowHeads = true,
            )
        }

        val textBlocks = draft.template.slots.mapNotNull { slot ->
            val text = draft.texts[slot]?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val bounds = textAreas[slot] ?: return@mapNotNull null
            val fontSize = if (slot == SlideTextSlot.TITLE) titleFont else bodyFont
            val (textColor, contrast) = SlideContrast.bestTextColor(style.plateColorArgb)
            val requiredContrast = if (fontSize >= 24f) {
                marketplaceRules.largeContrastMinimum
            } else {
                marketplaceRules.smallContrastMinimum
            }
            require(contrast >= requiredContrast) {
                "Text contrast $contrast is below the required $requiredContrast"
            }
            shapes += SlideShape.Plate(bounds, style.plateColorArgb)
            SlideTextPlacement(
                slot = slot,
                text = text,
                bounds = bounds,
                fontSizePx = fontSize,
                textColorArgb = textColor,
                plateColorArgb = style.plateColorArgb,
                contrastRatio = contrast,
                contrastMinimum = requiredContrast,
            )
        }
        return SlideRenderPlan(
            template = draft.template,
            background = background,
            backgroundIdentity = background.identity,
            baseImage = baseImage,
            itemSources = items.toList(),
            itemBounds = itemBounds,
            shapes = shapes,
            textBlocks = textBlocks,
            style = style,
        )
    }

    private fun placeItems(
        canvas: PixelBuffer,
        items: List<PixelBuffer>,
        area: SlideRect,
        grid: Boolean,
        cover: Boolean,
    ): List<SlideRect> {
        require(area.width > 0 && area.height > 0) { "Template leaves no room for the item" }
        require(grid || items.size == 1) { "Only the kit template accepts multiple item images" }
        val columns = if (grid) ceil(kotlin.math.sqrt(items.size.toDouble())).toInt() else 1
        val rows = if (grid) ceil(items.size.toDouble() / columns).toInt() else 1
        val cellWidth = area.width / columns
        val cellHeight = area.height / rows
        return items.mapIndexed { index, source ->
            val cellLeft = area.left + (index % columns) * cellWidth
            val cellTop = area.top + (index / columns) * cellHeight
            val widthScale = cellWidth.toFloat() / source.width
            val heightScale = cellHeight.toFloat() / source.height
            val scale = if (cover) max(widthScale, heightScale) else min(widthScale, heightScale)
            val scaledWidth = max(1, (source.width * scale).roundToInt())
            val scaledHeight = max(1, (source.height * scale).roundToInt())
            val itemWidth = min(cellWidth, scaledWidth)
            val itemHeight = min(cellHeight, scaledHeight)
            val left = cellLeft + (cellWidth - itemWidth) / 2
            val top = cellTop + (cellHeight - itemHeight) / 2
            val scaled = if (scaledWidth == source.width && scaledHeight == source.height) {
                PixelBuffer(source.width, source.height, source.pixels.copyOf())
            } else {
                source.scale(scaledWidth, scaledHeight)
            }
            val fitted = if (cover) {
                scaled.crop(
                    left = (scaled.width - itemWidth) / 2,
                    top = (scaled.height - itemHeight) / 2,
                    width = itemWidth,
                    height = itemHeight,
                )
            } else {
                scaled
            }
            val composite = fitted.compositeOver(canvas, left, top)
            composite.pixels.copyInto(canvas.pixels)
            SlideRect(left, top, itemWidth, itemHeight)
        }
    }
}
