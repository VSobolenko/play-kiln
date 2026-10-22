package com.gexetr.coverkit.domain

import kotlinx.serialization.Serializable

/**
 * Optional local brand kit (Unfold Pro shape, offline only): logo, up to four colours, two fonts.
 */
@Serializable
data class BrandKit(
    val logoUri: String? = null,
    val colors: List<Int> = emptyList(),
    val fontPrimary: String = FONT_DEFAULT_PRIMARY,
    val fontSecondary: String = FONT_DEFAULT_SECONDARY,
) {
    /** UI helper: unsigned ARGB longs for Compose Color(). */
    val colorsArgb: List<Long>
        get() = colors.map { it.toLong() and 0xFFFFFFFFL }

    val isEmpty: Boolean
        get() = logoUri.isNullOrBlank() &&
            colors.isEmpty() &&
            fontPrimary == FONT_DEFAULT_PRIMARY &&
            fontSecondary == FONT_DEFAULT_SECONDARY

    companion object {
        const val FONT_DEFAULT_PRIMARY: String = "sans"
        const val FONT_DEFAULT_SECONDARY: String = "serif"
        const val MAX_COLORS: Int = 4
    }
}
