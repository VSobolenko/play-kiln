package com.gexetr.coverkit.domain

import androidx.annotation.StringRes
import com.gexetr.coverkit.R

/**
 * Exact social-network canvas sizes from the Coverkit catalog
 * (`openspec/changes/app-coverkit/specs/social-cover-studio/spec.md`).
 */
sealed class Destination(
    val id: String,
    val widthPx: Int,
    val heightPx: Int,
    @StringRes val labelRes: Int,
) {
    data object YtThumb : Destination(
        id = "yt_thumb",
        widthPx = 1280,
        heightPx = 720,
        labelRes = R.string.dest_yt_thumb,
    )

    data object YtShorts : Destination(
        id = "yt_shorts",
        widthPx = 1080,
        heightPx = 1920,
        labelRes = R.string.dest_yt_shorts,
    )

    data object IgPost : Destination(
        id = "ig_post",
        widthPx = 1080,
        heightPx = 1080,
        labelRes = R.string.dest_ig_post,
    )

    data object IgStory : Destination(
        id = "ig_story",
        widthPx = 1080,
        heightPx = 1920,
        labelRes = R.string.dest_ig_story,
    )

    data object TiktokCover : Destination(
        id = "tiktok_cover",
        widthPx = 1080,
        heightPx = 1920,
        labelRes = R.string.dest_tiktok_cover,
    )

    data object WaStatus : Destination(
        id = "wa_status",
        widthPx = 1080,
        heightPx = 1920,
        labelRes = R.string.dest_wa_status,
    )

    data object FbCover : Destination(
        id = "fb_cover",
        widthPx = 820,
        heightPx = 312,
        labelRes = R.string.dest_fb_cover,
    )

    data object XHeader : Destination(
        id = "x_header",
        widthPx = 1500,
        heightPx = 500,
        labelRes = R.string.dest_x_header,
    )

    data object LinkedinBanner : Destination(
        id = "linkedin_banner",
        widthPx = 1584,
        heightPx = 396,
        labelRes = R.string.dest_linkedin_banner,
    )

    data object PinterestPin : Destination(
        id = "pinterest_pin",
        widthPx = 1000,
        heightPx = 1500,
        labelRes = R.string.dest_pinterest_pin,
    )
}

object DestinationCatalog {
    val all: List<Destination> = listOf(
        Destination.YtThumb,
        Destination.YtShorts,
        Destination.IgPost,
        Destination.IgStory,
        Destination.TiktokCover,
        Destination.WaStatus,
        Destination.FbCover,
        Destination.XHeader,
        Destination.LinkedinBanner,
        Destination.PinterestPin,
    )

    fun byId(id: String): Destination? = all.firstOrNull { it.id == id }

    fun requireById(id: String): Destination =
        byId(id) ?: error("Unknown destination id: $id")

    /** Alias used by pack export / multi-size fit. */
    fun require(id: String): Destination = requireById(id)
}
