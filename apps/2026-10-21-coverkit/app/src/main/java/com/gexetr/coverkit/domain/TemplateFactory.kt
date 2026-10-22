package com.gexetr.coverkit.domain

import com.gexetr.coverkit.R
import java.util.UUID

/**
 * Offline starter templates: editable photo + text layers, not flat locked images.
 * Each destination ships at least two templates; [isFree] marks the happy-path subset.
 */
data class CoverTemplate(
    val id: String,
    val destinationId: String,
    val name: String,
    val isFree: Boolean,
) {
    /** Inverse of [isFree] — kept for UI lock badges without restating the free flag. */
    val requiresUnlock: Boolean get() = !isFree
}

object TemplateFactory {

    private data class Spec(
        val template: CoverTemplate,
        val photoColor: Int,
        val headline: String,
        val subline: String?,
        val headlineY: Float,
        val sublineY: Float,
        val layoutIndex: Int,
    )

    private val specs: List<Spec> = buildList {
        val premiumPalette = listOf(
            0xFF160B2D.toInt(),
            0xFF20113F.toInt(),
            0xFF0D1B3D.toInt(),
            0xFF2A0A3D.toInt(),
            0xFF10243D.toInt(),
            0xFF251136.toInt(),
        )
        data class Seed(
            val suffix: String,
            val name: String,
            val free: Boolean,
            val color: Int,
            val headline: String,
            val subline: String?,
            val headlineY: Float,
            val sublineY: Float,
        )

        fun add(
            destinationId: String,
            seed: Seed,
        ) {
            add(
                Spec(
                    template = CoverTemplate(
                        id = "${destinationId}_${seed.suffix}",
                        destinationId = destinationId,
                        name = seed.name,
                        isFree = seed.free,
                    ),
                    photoColor = if (seed.free) seed.color else premiumPalette[size % premiumPalette.size],
                    headline = seed.headline,
                    subline = seed.subline,
                    headlineY = seed.headlineY,
                    sublineY = seed.sublineY,
                    layoutIndex = size,
                ),
            )
        }

        fun addSet(destinationId: String, seeds: List<Seed>) = seeds.forEach { add(destinationId, it) }
        fun s(suffix: String, name: String, free: Boolean, color: Int, headline: String, subline: String?, y: Float, subY: Float) =
            Seed(suffix, name, free, color, headline, subline, y, subY)

        addSet("yt_thumb", listOf(
            s("bold", "Bold headline", true, 0xFFE63946.toInt(), "WATCH THIS", "New video", .36f, .56f),
            s("reaction", "Reaction burst", true, 0xFFFFBE0B.toInt(), "NO WAY!", "My honest take", .66f, .80f),
            s("howto", "How-to stripe", true, 0xFF3A86FF.toInt(), "HOW TO", "Get started today", .28f, .45f),
            s("countdown", "Countdown card", true, 0xFF8338EC.toInt(), "TOP 5", "Things to know", .52f, .68f),
            s("review", "Review spotlight", true, 0xFF06D6A0.toInt(), "IS IT WORTH IT?", "Full review", .74f, .88f),
            s("split", "Split banner", false, 0xFF457B9D.toInt(), "TOP TIP", "In 60 seconds", .34f, .52f),
            s("cinema", "Cinematic reveal", false, 0xFFEF476F.toInt(), "THE REVEAL", "Watch to the end", .62f, .78f),
        ))
        addSet("yt_shorts", listOf(
            s("vertical", "Vertical punch", true, 0xFF2A9D8F.toInt(), "SHORTS", "Tap to watch", .24f, .38f),
            s("quicktip", "Quick tip stack", true, 0xFFFF9F1C.toInt(), "QUICK TIP", "Save this", .44f, .60f),
            s("beforeafter", "Before and after", true, 0xFF4361EE.toInt(), "BEFORE / AFTER", "Wait for it", .68f, .82f),
            s("daily", "Daily dose", true, 0xFFFF595E.toInt(), "TODAY'S IDEA", "Try it now", .31f, .47f),
            s("challenge", "Challenge card", true, 0xFF8AC926.toInt(), "DAY 1", "Join the challenge", .56f, .72f),
            s("neon", "Neon callout", false, 0xFF264653.toInt(), "LIVE NOW", null, .42f, .58f),
            s("afterdark", "After-dark glow", false, 0xFF7209B7.toInt(), "DON'T BLINK", "Exclusive clip", .75f, .88f),
        ))
        addSet("ig_post", listOf(
            s("square", "Square clean", true, 0xFFF4A261.toInt(), "NEW DROP", "Shop the look", .30f, .46f),
            s("quote", "Editorial quote", true, 0xFF118AB2.toInt(), "MAKE IT COUNT", "A note for today", .50f, .66f),
            s("launch", "Launch tile", true, 0xFFFF006E.toInt(), "JUST LANDED", "Available now", .70f, .84f),
            s("minimal", "Minimal mark", true, 0xFF073B4C.toInt(), "LESS, BETTER", "New collection", .25f, .40f),
            s("weekend", "Weekend colour", true, 0xFFFFD166.toInt(), "WEEKEND MODE", "See you outside", .58f, .74f),
            s("grid", "Grid promo", false, 0xFFE76F51.toInt(), "SALE", "This week only", .38f, .54f),
            s("studio", "Studio edition", false, 0xFF5E60CE.toInt(), "MADE WITH CARE", "Limited edition", .76f, .89f),
        ))
        addSet("ig_story", listOf(
            s("story", "Story frame", true, 0xFF9B5DE5.toInt(), "BEHIND", "The scenes", .22f, .37f),
            s("morning", "Morning note", true, 0xFFFFC6FF.toInt(), "GOOD MORNING", "A fresh start", .38f, .54f),
            s("question", "Question card", true, 0xFF00BBF9.toInt(), "ASK ME ANYTHING", "Drop a question", .60f, .76f),
            s("newpost", "New post alert", true, 0xFFFEE440.toInt(), "NEW POST", "Tap to see it", .74f, .88f),
            s("quote", "Mood quote", true, 0xFF00F5D4.toInt(), "CREATE YOUR DAY", "One step at a time", .48f, .64f),
            s("poll", "Poll vibe", false, 0xFFF15BB5.toInt(), "THIS OR THAT", null, .34f, .50f),
            s("velvet", "Velvet night", false, 0xFF3C096C.toInt(), "SOMETHING BIG", "Tomorrow at noon", .70f, .84f),
        ))
        addSet("tiktok_cover", listOf(
            s("hook", "Hook cover", true, 0xFF00F5D4.toInt(), "WAIT FOR IT", "Part 1", .26f, .41f),
            s("recipe", "Recipe reveal", true, 0xFFFF9E00.toInt(), "EASY RECIPE", "Ready in 10 min", .50f, .66f),
            s("truth", "Truth bomb", true, 0xFFFF0054.toInt(), "NOBODY TELLS YOU", "But I will", .72f, .86f),
            s("trend", "Trend tracker", true, 0xFF7B2CBF.toInt(), "TRY THIS TREND", "It actually works", .34f, .50f),
            s("glowup", "Glow-up diary", true, 0xFF70E000.toInt(), "MY GLOW UP", "Seven days later", .60f, .75f),
            s("duet", "Duet style", false, 0xFFFF006E.toInt(), "DUET ME", null, .42f, .58f),
            s("vip", "VIP hook", false, 0xFF240046.toInt(), "THE SECRET", "Watch the full series", .78f, .90f),
        ))
        addSet("wa_status", listOf(
            s("status", "Status flash", true, 0xFF25D366.toInt(), "UPDATE", "Swipe up", .24f, .40f),
            s("hello", "Hello card", true, 0xFF52B788.toInt(), "HELLO, FRIENDS", "Hope you're well", .46f, .62f),
            s("celebrate", "Celebrate today", true, 0xFFFFB703.toInt(), "LET'S CELEBRATE", "Good news inside", .68f, .82f),
            s("weekend", "Weekend plan", true, 0xFF219EBC.toInt(), "WEEKEND PLAN", "Who's in?", .32f, .48f),
            s("thankyou", "Thank-you note", true, 0xFFE5989B.toInt(), "THANK YOU", "For being here", .58f, .74f),
            s("announce", "Announce", false, 0xFF128C7E.toInt(), "BIG NEWS", "Coming soon", .38f, .54f),
            s("gold", "Golden announcement", false, 0xFF6A040F.toInt(), "SAVE THE DATE", "A special moment", .76f, .89f),
        ))
        addSet("fb_cover", listOf(
            s("welcome", "Warm welcome", true, 0xFF264653.toInt(), "WELCOME IN", "We're glad you're here", .36f, .54f),
            s("community", "Community banner", true, 0xFF2A9D8F.toInt(), "OUR COMMUNITY", "Made for people like you", .58f, .74f),
            s("event", "Event ribbon", true, 0xFFE9C46A.toInt(), "JOIN THE EVENT", "Saturday · 4 PM", .25f, .42f),
            s("season", "Seasonal refresh", true, 0xFFE76F51.toInt(), "NEW SEASON", "Fresh ideas every week", .70f, .84f),
            s("mission", "Mission statement", true, 0xFF457B9D.toInt(), "MAKE MORE POSSIBLE", "One small step at a time", .48f, .65f),
            s("signature", "Signature cover", false, 0xFF6D597A.toInt(), "THE OFFICIAL PAGE", "News, stories and more", .32f, .49f),
            s("members", "Members-only header", false, 0xFF1D3557.toInt(), "INSIDE THE CLUB", "Exclusive updates", .74f, .87f),
        ))
        addSet("x_header", listOf(
            s("signal", "Signal header", true, 0xFF0B132B.toInt(), "IDEAS IN MOTION", "Follow the next move", .38f, .55f),
            s("maker", "Maker manifesto", true, 0xFF3A506B.toInt(), "BUILD IN PUBLIC", "One post at a time", .60f, .76f),
            s("launch", "Launch horizon", true, 0xFF5BC0BE.toInt(), "SOMETHING NEW", "Coming into view", .26f, .43f),
            s("dispatch", "Daily dispatch", true, 0xFFFCA311.toInt(), "THE DAILY DISPATCH", "Notes worth sharing", .72f, .85f),
            s("focus", "Focus line", true, 0xFF9B2226.toInt(), "STAY CURIOUS", "The rest follows", .49f, .65f),
            s("midnight", "Midnight premium", false, 0xFF1B263B.toInt(), "AFTER HOURS", "For the deeply curious", .34f, .51f),
            s("spectrum", "Spectrum premium", false, 0xFF560BAD.toInt(), "A BOLDER SIGNAL", "Make your mark", .77f, .89f),
        ))
        addSet("linkedin_banner", listOf(
            s("profile", "Professional profile", true, 0xFF0A66C2.toInt(), "LET'S BUILD TOGETHER", "Strategy · design · delivery", .36f, .53f),
            s("expertise", "Expertise line", true, 0xFF1B4965.toInt(), "TURNING IDEAS INTO ACTION", "Practical work that moves teams", .58f, .74f),
            s("speaker", "Speaker banner", true, 0xFF5FA8D3.toInt(), "SPEAKING & WORKSHOPS", "Invite a useful conversation", .24f, .41f),
            s("portfolio", "Portfolio statement", true, 0xFFCAE9FF.toInt(), "SELECTED WORK", "Clear thinking, visible results", .70f, .84f),
            s("hiring", "Hiring signal", true, 0xFF62B6CB.toInt(), "WE'RE GROWING", "Meet the open roles", .48f, .64f),
            s("executive", "Executive polish", false, 0xFF0D1B2A.toInt(), "LEAD WITH CLARITY", "A calm signal in a busy market", .33f, .50f),
            s("summit", "Summit premium", false, 0xFF415A77.toInt(), "THE NEXT LEVEL", "Work worth remembering", .76f, .88f),
        ))
        addSet("pinterest_pin", listOf(
            s("recipe", "Recipe pin", true, 0xFFE63946.toInt(), "EASY DINNER IDEA", "Save this recipe", .24f, .39f),
            s("home", "Home refresh", true, 0xFFF4A261.toInt(), "COSY HOME IDEAS", "For every room", .46f, .62f),
            s("style", "Style board", true, 0xFF9D4EDD.toInt(), "OUTFIT INSPO", "Looks to try now", .68f, .82f),
            s("travel", "Travel guide", true, 0xFF219EBC.toInt(), "WEEKEND ESCAPE", "A tiny travel guide", .32f, .48f),
            s("craft", "Craft tutorial", true, 0xFF80B918.toInt(), "MAKE THIS TODAY", "Simple DIY steps", .56f, .72f),
            s("editorial", "Editorial pin", false, 0xFF6A040F.toInt(), "THE MOOD BOARD", "Collected with intention", .38f, .54f),
            s("luxe", "Luxe pin", false, 0xFF3C096C.toInt(), "SAVE FOR LATER", "Your next favourite idea", .78f, .90f),
        ))
    }

    fun allTemplates(): List<CoverTemplate> = specs.map { it.template }

    fun templatesFor(destinationId: String): List<CoverTemplate> =
        specs.filter { it.template.destinationId == destinationId }.map { it.template }

    fun freeTemplatesFor(destinationId: String): List<CoverTemplate> =
        templatesFor(destinationId).filter { it.isFree }

    /** Localised display label for every template the factory exposes. */
    fun nameResOrNull(templateId: String): Int? = when (templateId) {
        "yt_thumb_bold" -> R.string.tpl_yt_thumb_bold
        "yt_thumb_reaction" -> R.string.tpl_yt_thumb_reaction
        "yt_thumb_howto" -> R.string.tpl_yt_thumb_howto
        "yt_thumb_countdown" -> R.string.tpl_yt_thumb_countdown
        "yt_thumb_review" -> R.string.tpl_yt_thumb_review
        "yt_thumb_split" -> R.string.tpl_yt_thumb_split
        "yt_thumb_cinema" -> R.string.tpl_yt_thumb_cinema
        "yt_shorts_vertical" -> R.string.tpl_yt_shorts_vertical
        "yt_shorts_quicktip" -> R.string.tpl_yt_shorts_quicktip
        "yt_shorts_beforeafter" -> R.string.tpl_yt_shorts_beforeafter
        "yt_shorts_daily" -> R.string.tpl_yt_shorts_daily
        "yt_shorts_challenge" -> R.string.tpl_yt_shorts_challenge
        "yt_shorts_neon" -> R.string.tpl_yt_shorts_neon
        "yt_shorts_afterdark" -> R.string.tpl_yt_shorts_afterdark
        "ig_post_square" -> R.string.tpl_ig_post_square
        "ig_post_quote" -> R.string.tpl_ig_post_quote
        "ig_post_launch" -> R.string.tpl_ig_post_launch
        "ig_post_minimal" -> R.string.tpl_ig_post_minimal
        "ig_post_weekend" -> R.string.tpl_ig_post_weekend
        "ig_post_grid" -> R.string.tpl_ig_post_grid
        "ig_post_studio" -> R.string.tpl_ig_post_studio
        "ig_story_story" -> R.string.tpl_ig_story_story
        "ig_story_morning" -> R.string.tpl_ig_story_morning
        "ig_story_question" -> R.string.tpl_ig_story_question
        "ig_story_newpost" -> R.string.tpl_ig_story_newpost
        "ig_story_quote" -> R.string.tpl_ig_story_quote
        "ig_story_poll" -> R.string.tpl_ig_story_poll
        "ig_story_velvet" -> R.string.tpl_ig_story_velvet
        "tiktok_cover_hook" -> R.string.tpl_tiktok_cover_hook
        "tiktok_cover_recipe" -> R.string.tpl_tiktok_cover_recipe
        "tiktok_cover_truth" -> R.string.tpl_tiktok_cover_truth
        "tiktok_cover_trend" -> R.string.tpl_tiktok_cover_trend
        "tiktok_cover_glowup" -> R.string.tpl_tiktok_cover_glowup
        "tiktok_cover_duet" -> R.string.tpl_tiktok_cover_duet
        "tiktok_cover_vip" -> R.string.tpl_tiktok_cover_vip
        "wa_status_status" -> R.string.tpl_wa_status_status
        "wa_status_hello" -> R.string.tpl_wa_status_hello
        "wa_status_celebrate" -> R.string.tpl_wa_status_celebrate
        "wa_status_weekend" -> R.string.tpl_wa_status_weekend
        "wa_status_thankyou" -> R.string.tpl_wa_status_thankyou
        "wa_status_announce" -> R.string.tpl_wa_status_announce
        "wa_status_gold" -> R.string.tpl_wa_status_gold
        "fb_cover_welcome" -> R.string.tpl_fb_cover_welcome
        "fb_cover_community" -> R.string.tpl_fb_cover_community
        "fb_cover_event" -> R.string.tpl_fb_cover_event
        "fb_cover_season" -> R.string.tpl_fb_cover_season
        "fb_cover_mission" -> R.string.tpl_fb_cover_mission
        "fb_cover_signature" -> R.string.tpl_fb_cover_signature
        "fb_cover_members" -> R.string.tpl_fb_cover_members
        "x_header_signal" -> R.string.tpl_x_header_signal
        "x_header_maker" -> R.string.tpl_x_header_maker
        "x_header_launch" -> R.string.tpl_x_header_launch
        "x_header_dispatch" -> R.string.tpl_x_header_dispatch
        "x_header_focus" -> R.string.tpl_x_header_focus
        "x_header_midnight" -> R.string.tpl_x_header_midnight
        "x_header_spectrum" -> R.string.tpl_x_header_spectrum
        "linkedin_banner_profile" -> R.string.tpl_linkedin_banner_profile
        "linkedin_banner_expertise" -> R.string.tpl_linkedin_banner_expertise
        "linkedin_banner_speaker" -> R.string.tpl_linkedin_banner_speaker
        "linkedin_banner_portfolio" -> R.string.tpl_linkedin_banner_portfolio
        "linkedin_banner_hiring" -> R.string.tpl_linkedin_banner_hiring
        "linkedin_banner_executive" -> R.string.tpl_linkedin_banner_executive
        "linkedin_banner_summit" -> R.string.tpl_linkedin_banner_summit
        "pinterest_pin_recipe" -> R.string.tpl_pinterest_pin_recipe
        "pinterest_pin_home" -> R.string.tpl_pinterest_pin_home
        "pinterest_pin_style" -> R.string.tpl_pinterest_pin_style
        "pinterest_pin_travel" -> R.string.tpl_pinterest_pin_travel
        "pinterest_pin_craft" -> R.string.tpl_pinterest_pin_craft
        "pinterest_pin_editorial" -> R.string.tpl_pinterest_pin_editorial
        "pinterest_pin_luxe" -> R.string.tpl_pinterest_pin_luxe
        else -> null
    }

    fun createProject(
        templateId: String,
        projectId: String = UUID.randomUUID().toString(),
        nowMs: Long = System.currentTimeMillis(),
    ): CoverProject {
        val spec = specs.firstOrNull { it.template.id == templateId }
            ?: error("Unknown template id: $templateId")
        val layout = spec.layoutIndex % 6
        val premium = spec.template.requiresUnlock
        val photoX = floatArrayOf(.50f, .62f, .38f, .56f, .44f, .50f)[layout]
        val photoY = floatArrayOf(.50f, .44f, .58f, .40f, .62f, .50f)[layout]
        val photoScale = if (premium) {
            floatArrayOf(1.38f, 1.24f, 1.46f, 1.30f, 1.42f, 1.34f)[layout]
        } else {
            floatArrayOf(1.02f, 1.16f, .92f, 1.25f, 1.09f, 1.20f)[layout]
        }
        val headlineX = floatArrayOf(.18f, .74f, .24f, .70f, .30f, .66f)[layout]
        val headlineY = if (premium) {
            floatArrayOf(.20f, .76f, .30f, .70f, .24f, .80f)[layout]
        } else {
            spec.headlineY
        }
        val sublineY = if (premium) (headlineY - .12f).coerceAtLeast(.08f) else spec.sublineY
        val layers = buildList {
            add(
                Layer.PhotoLayer(
                    id = "photo_${spec.template.id}",
                    x = photoX,
                    y = photoY,
                    scale = photoScale,
                    rotationDeg = 0f,
                    zIndex = 0,
                    uriString = null,
                    colorArgb = spec.photoColor,
                ),
            )
            add(
                Layer.TextLayer(
                    id = "text_headline_${spec.template.id}",
                    x = headlineX,
                    y = headlineY,
                    scale = if (premium) 1.34f else floatArrayOf(1.08f, 1.28f, 1.16f, 1.36f, 1.12f, 1.24f)[layout],
                    rotationDeg = 0f,
                    zIndex = 1,
                    text = spec.headline,
                    fillArgb = 0xFFFFFFFF.toInt(),
                    strokeArgb = 0xFF000000.toInt(),
                    strokeWidthPx = 6f,
                ),
            )
            val sub = spec.subline
            if (sub != null) {
                add(
                    Layer.TextLayer(
                        id = "text_sub_${spec.template.id}",
                        x = headlineX,
                        y = sublineY,
                        scale = if (premium) .94f else .85f,
                        rotationDeg = 0f,
                        zIndex = 2,
                        text = sub,
                        fillArgb = 0xFFFFFFF2.toInt(),
                        strokeArgb = 0xFF111111.toInt(),
                        strokeWidthPx = 3f,
                    ),
                )
            }
        }
        return CoverProject(
            id = projectId,
            destinationId = spec.template.destinationId,
            name = spec.template.name,
            layers = layers,
            updatedAtMs = nowMs,
            backdropStyle = layout,
        )
    }

    fun blankProject(
        destinationId: String,
        projectId: String = UUID.randomUUID().toString(),
        nowMs: Long = System.currentTimeMillis(),
        name: String = "Untitled",
    ): CoverProject {
        DestinationCatalog.requireById(destinationId)
        return CoverProject(
            id = projectId,
            destinationId = destinationId,
            name = name,
            layers = listOf(
                Layer.PhotoLayer(
                    id = "photo_blank",
                    colorArgb = 0xFF1D3557.toInt(),
                ),
            ),
            updatedAtMs = nowMs,
            backdropStyle = BackdropStyle.CENTER_STACK.ordinal,
        )
    }
}
