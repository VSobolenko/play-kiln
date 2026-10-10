package com.gexetr.lumintralightplanner.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.gexetr.lumintralightplanner.data.BundledAsset
import com.gexetr.lumintralightplanner.data.SkyAssetSource
import java.io.File

/**
 * The postcard: a **composed** 1080 × 1350 sRGB PNG, drawn by this slot rather than photographed
 * from a system view (`share-brief` spec: "a composed postcard (its own drawing), never a screenshot
 * of a system view"). It is also the second half of the share surface: [shareIntent] hands the file
 * out through the slot's own `FileProvider`, and [exportDocumentIntent] is the `ACTION_CREATE_DOCUMENT`
 * half that writes the same bitmap wherever the user picks.
 *
 * **No Compose and no `Context` inside the drawing.** [render] composes onto a plain
 * [android.graphics.Canvas] from a [PostcardContent] the caller has already formatted, so the drawing
 * is deterministic and testable; colours arrive as ARGB ints taken from the slot's own palette, so the
 * card cannot drift from the theme.
 *
 * **The sky is the credited panorama, not an imitation of it.** When
 * `assets/sky/milky_way_eso0932a.webp` is packaged, [render] decodes it (downsampled — a 4096 × 2048
 * WebP is 33 MB raw) and draws a window of it centred on the galaxy core, over the slot's night
 * gradient. When it is absent the card falls back to the gradient and the star field alone; the credit
 * line is drawn either way, because the credit is about the app's imagery as a whole and the moon disc
 * and the band are the same attribution set (design.md § "Sky assets and attribution").
 *
 * **The credit is mandatory and is never elided.** [PostcardContent.creditLine] holds the verbatim
 * `ESO/S. Brunier` attribution and is always drawn at the card's foot, whatever else is dropped to fit.
 */
object PostcardExport {

    /** The share image's canvas: 4:5, the portrait ratio the brief's postcard is composed on. */
    const val WIDTH: Int = 1080
    const val HEIGHT: Int = 1350

    /** `share-brief` spec: the shared postcard is a PNG. */
    const val MIME: String = "image/png"

    /** The `FileProvider` authority suffix declared in the manifest as `${applicationId}.fileprovider`. */
    private const val AUTHORITY_SUFFIX: String = ".fileprovider"

    /** `res/xml/file_paths.xml` exposes `cache-path name="share" path="share/"`. */
    private const val SHARE_DIR: String = "share"

    /** The panorama is decoded at most this wide: the card is 1080 px, and 4096 px would cost 33 MB. */
    private const val PANORAMA_MAX_WIDTH: Int = 2048

    private const val MAX_FILE_STEM: Int = 60

    // ---------------------------------------------------------------- the composed bitmap

    /**
     * Draws the postcard. Call off the main thread: a bundled panorama is decoded here.
     *
     * The composition, back to front: the night gradient, the panorama window (or the star field when
     * it is absent), the galaxy-core glow, the horizon silhouette, the Moon disc clipped to its phase,
     * the object-marker inset, and the text card that carries the readings and the credit.
     */
    fun render(context: Context, content: PostcardContent): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val skyBottom = (HEIGHT * HORIZON_FRACTION).toInt()
        drawSky(canvas, paint, content, skyBottom)
        val panorama = decodePanorama(context)
        if (panorama != null) {
            drawPanoramaWindow(canvas, paint, content, panorama, skyBottom)
            panorama.recycle()
        } else {
            drawStarField(canvas, paint, content, skyBottom)
        }
        drawCoreGlow(canvas, paint, content, skyBottom)
        drawHorizon(canvas, paint, content, skyBottom)
        drawMoon(canvas, paint, content, skyBottom)
        drawObjectMarker(canvas, paint, content)
        drawCreditOverImage(canvas, paint, content, skyBottom)
        drawTextCard(canvas, paint, content, skyBottom)
        return bitmap
    }

    private fun drawSky(canvas: Canvas, paint: Paint, content: PostcardContent, skyBottom: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            skyBottom.toFloat(),
            content.skyTopArgb,
            content.skyBottomArgb,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), skyBottom.toFloat(), paint)
        paint.shader = null
    }

    /**
     * The credited ESO panorama `eso0932a` (`ESO/S. Brunier`), windowed on the core.
     *
     * The source is equirectangular; the window is a rectangle of it whose centre is the core's own
     * position, sized to the card's upper band, so the band's brightest patch sits where the session
     * actually put the core. It is drawn under a graded scrim: a photograph is not legible enough to
     * carry text, and the readings are what the postcard is for.
     */
    private fun drawPanoramaWindow(
        canvas: Canvas,
        paint: Paint,
        content: PostcardContent,
        panorama: Bitmap,
        skyBottom: Int,
    ) {
        val bandHeight = skyBottom.toFloat()
        // Cover the band: scale so the panorama's height fills it, then take a window as wide as the card.
        val scale = bandHeight / panorama.height.toFloat()
        val windowW = (WIDTH / scale).coerceAtMost(panorama.width.toFloat())
        val windowH = panorama.height.toFloat()
        val coreX = (content.coreFracX.coerceIn(0.05f, 0.95f) * panorama.width)
        val coreY = (content.coreFracY.coerceIn(0.05f, 0.95f) * windowH)
        val left = (coreX - windowW / 2f).coerceIn(0f, (panorama.width - windowW).coerceAtLeast(0f))
        val top = (coreY - windowH / 2f).coerceIn(0f, (panorama.height - windowH).coerceAtLeast(0f))

        paint.reset()
        paint.isAntiAlias = true
        paint.alpha = PANORAMA_ALPHA
        canvas.drawBitmap(
            panorama,
            Rect(
                left.toInt(),
                top.toInt(),
                (left + windowW).toInt(),
                (top + windowH).toInt(),
            ),
            RectF(0f, 0f, WIDTH.toFloat(), bandHeight),
            paint,
        )
        paint.alpha = 255

        // The scrim: opaque at the foot of the band, transparent at the top, so the text card reads.
        paint.shader = LinearGradient(
            0f,
            0f,
            0f,
            bandHeight,
            intArrayOf(SCRIM_TOP, SCRIM_BOTTOM),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), bandHeight, paint)
        paint.shader = null
    }

    /**
     * The fallback sky when the panorama is not packaged: a deterministic star field. It is a
     * *drawing*, deliberately sparse and non-pictorial — the card never pretends to be a photograph
     * of a session the app did not take.
     */
    private fun drawStarField(canvas: Canvas, paint: Paint, content: PostcardContent, skyBottom: Int) {
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = content.inkArgb
        var seed = 0x5eedL
        for (index in 0 until STAR_COUNT) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) and Long.MAX_VALUE
            val fx = (seed ushr 12) % 10_000L
            val fy = (seed ushr 30) % 10_000L
            val fr = (seed ushr 48) % 100L
            val x = fx / 10_000f * WIDTH
            val y = fy / 10_000f * skyBottom
            val radius = 1.1f + fr / 100f * 2.4f
            // Fade the field out towards the horizon so the silhouette has a clean edge.
            val fade = (1f - (y / skyBottom) * 0.7f).coerceIn(0.15f, 1f)
            paint.alpha = (40 + 180 * fade).toInt()
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.alpha = 255
    }

    /** The core's glow: the one warm patch the card shows, placed from the session, never decorative. */
    private fun drawCoreGlow(canvas: Canvas, paint: Paint, content: PostcardContent, skyBottom: Int) {
        val cx = content.coreFracX.coerceIn(0.1f, 0.9f) * WIDTH
        val cy = (0.20f + content.coreFracY.coerceIn(0f, 1f) * 0.45f) * skyBottom
        val radius = WIDTH * 0.55f
        paint.reset()
        paint.isAntiAlias = true
        paint.shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(GLOW_CORE, GLOW_EDGE),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)
        paint.shader = null
    }

    /** The ground: a two-step silhouette, so the card reads as a horizon and not as a gradient wash. */
    private fun drawHorizon(canvas: Canvas, paint: Paint, content: PostcardContent, skyBottom: Int) {
        val far = Path().apply {
            moveTo(0f, skyBottom - 46f)
            cubicTo(
                WIDTH * 0.22f, skyBottom - 118f,
                WIDTH * 0.38f, skyBottom - 12f,
                WIDTH * 0.58f, skyBottom - 62f,
            )
            cubicTo(
                WIDTH * 0.76f, skyBottom - 104f,
                WIDTH * 0.9f, skyBottom - 30f,
                WIDTH.toFloat(), skyBottom - 74f,
            )
            lineTo(WIDTH.toFloat(), (skyBottom + 160).toFloat())
            lineTo(0f, (skyBottom + 160).toFloat())
            close()
        }
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = content.horizonArgb
        canvas.drawPath(far, paint)

        canvas.drawRect(0f, (skyBottom + 26).toFloat(), WIDTH.toFloat(), (skyBottom + 160).toFloat(), paint)
        paint.color = content.groundArgb
        canvas.drawRect(0f, (skyBottom + 96).toFloat(), WIDTH.toFloat(), (skyBottom + 160).toFloat(), paint)
    }

    /**
     * The Moon, clipped to its illuminated fraction.
     *
     * The illuminated region is drawn as one closed path — the outer limb on the lit side plus the
     * terminator, an ellipse whose signed semi-axis is `r × (1 − 2f)`. That single expression covers
     * every phase the app can name: a crescent (the ellipse bows towards the dark limb), a half, and a
     * gibbous (the semi-axis goes negative and the terminator bows the other way). A new Moon is drawn
     * as the unlit disc with no lit path at all, which is what a new Moon looks like.
     */
    private fun drawMoon(canvas: Canvas, paint: Paint, content: PostcardContent, skyBottom: Int) {
        val radius = WIDTH * 0.062f
        val cx = WIDTH * 0.79f
        val cy = skyBottom * 0.20f
        val fraction = content.moonLitFraction.coerceIn(0.0, 1.0)
        val side = if (content.moonWaxing) 1f else -1f

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = content.moonShadowArgb
        canvas.drawCircle(cx, cy, radius, paint)

        if (fraction > 0.02) {
            val terminator = (radius * (1.0 - 2.0 * fraction)).toFloat()
            val lit = Path().apply {
                // Outer limb: the lit half of the disc, from the north pole round the lit side.
                arcTo(RectF(cx - radius, cy - radius, cx + radius, cy + radius), -90f, 180f * side, false)
                // Terminator: the phase ellipse, closing back to the north pole.
                arcTo(
                    RectF(cx - terminator, cy - radius, cx + terminator, cy + radius),
                    90f,
                    -180f * side,
                    false,
                )
                close()
            }
            paint.color = content.moonLitArgb
            canvas.drawPath(lit, paint)
        }
    }

    /**
     * The object marker: the frame squashed to a square (it is a *fit* diagram, so the frame's short
     * side is what is drawn) with the target boxed inside it at the catalogue's angular size.
     *
     * The diagram carries no numbers of its own — [PostcardContent.fitLines] carries them, measured
     * and in millimetres for the optics — because a drawing that labelled itself could disagree with
     * the brief it shares a card with.
     */
    private fun drawObjectMarker(canvas: Canvas, paint: Paint, content: PostcardContent) {
        val objectFrac = content.objectFractionOfFrame
        val cx = WIDTH * 0.79f
        val cy = HEIGHT * 0.235f
        val frameHalf = WIDTH * 0.105f

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = content.mutedInkArgb
        canvas.drawRect(cx - frameHalf, cy - frameHalf, cx + frameHalf, cy + frameHalf, paint)
        // Corner ticks, so the square reads as a frame and not as a picture frame.
        paint.strokeWidth = 6f
        paint.color = content.accentArgb
        val tick = frameHalf * 0.28f
        for ((dx, dy) in CORNER_SIGNS) {
            canvas.drawLine(cx + dx * frameHalf, cy + dy * frameHalf, cx + dx * (frameHalf - tick), cy + dy * frameHalf, paint)
            canvas.drawLine(cx + dx * frameHalf, cy + dy * frameHalf, cx + dx * frameHalf, cy + dy * (frameHalf - tick), paint)
        }

        if (objectFrac == null) {
            return
        }
        val half = (frameHalf * objectFrac.coerceIn(0.02, 1.0)).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = content.accentArgb
        canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = content.inkArgb
        canvas.drawLine(cx - half, cy, cx + half, cy, paint)
        canvas.drawLine(cx, cy - half, cx, cy + half, paint)
    }

    /**
     * The ESO credit, **on the image itself and never separated from it**.
     *
     * The licence (CC BY 4.0) requires the credit to be unaltered, clearly visible and attached to the
     * image it credits, so it is drawn over the Milky Way band rather than in the reading card below:
     * `ESO/S. Brunier` in the accent ink, the source link beneath it, and the modification statement
     * CC BY 4.0 asks for when a composition is altered. A near-opaque plate sits behind the three lines,
     * so the contrast the licence requires does not depend on how bright the underlying panorama happens
     * to be at that spot. Nothing in this app removes, moves or crops it.
     */
    private fun drawCreditOverImage(canvas: Canvas, paint: Paint, content: PostcardContent, skyBottom: Int) {
        val left = CARD_MARGIN * 0.72f
        val maxWidth = WIDTH - left * 2f
        val plateTop = skyBottom - CREDIT_PLATE_RISE

        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = CREDIT_PLATE
        canvas.drawRoundRect(
            RectF(left - 30f, plateTop, WIDTH - left + 30f, skyBottom - CREDIT_PLATE_FALL),
            CARD_RADIUS * 0.6f,
            CARD_RADIUS * 0.6f,
            paint,
        )

        var y = plateTop + CREDIT_ON_IMAGE_SIZE + 18f
        y = drawLine(
            canvas,
            paint,
            content.creditLine,
            left,
            y,
            maxWidth,
            CREDIT_ON_IMAGE_SIZE,
            content.accentArgb,
            bold = true,
            tracked = true,
        )
        y = drawLine(canvas, paint, content.creditDetail, left, y + 10f, maxWidth, CREDIT_LINK_SIZE, content.inkArgb)
        drawLine(canvas, paint, content.creditModification, left, y + 6f, maxWidth, CREDIT_LINK_SIZE, content.mutedInkArgb)
    }

    /** The reading card. Body lines fill what is left of the card; the credit is on the image above it. */
    private fun drawTextCard(canvas: Canvas, paint: Paint, content: PostcardContent, skyBottom: Int) {
        val left = CARD_MARGIN
        val right = WIDTH - CARD_MARGIN
        val maxWidth = right - left

        val cardTop = (skyBottom + 168).toFloat()
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = content.cardArgb
        canvas.drawRoundRect(
            RectF(left - 34f, cardTop - 34f, right + 34f, HEIGHT - 28f),
            CARD_RADIUS,
            CARD_RADIUS,
            paint,
        )

        var y = cardTop + TITLE_SIZE
        y = drawLine(canvas, paint, content.title, left, y, maxWidth, TITLE_SIZE, content.inkArgb, bold = true, tracked = true)
        y = drawLine(canvas, paint, content.subtitle, left, y + 12f, maxWidth, SUBTITLE_SIZE, content.mutedInkArgb)
        y = drawLine(canvas, paint, content.nightLine, left, y + 6f, maxWidth, BODY_SIZE, content.accentArgb, bold = true)
        y = drawLine(canvas, paint, content.spotLine, left, y + 18f, maxWidth, BODY_SIZE, content.inkArgb)
        y = drawLine(canvas, paint, content.positionLine, left, y, maxWidth, SMALL_SIZE, content.mutedInkArgb)

        for (line in content.timeLines) {
            y = drawLine(canvas, paint, line, left, y + 4f, maxWidth, SMALL_SIZE, content.mutedInkArgb)
        }

        // The watermark is the only reserved footer left: the ESO credit lives on the image above, and
        // the body fills everything between the readings and that line.
        val bodyLimit = HEIGHT - FOOT_HEIGHT
        for (line in content.targetLines + content.fitLines + content.forecastLines) {
            if (y + BODY_SIZE > bodyLimit) {
                break
            }
            y = drawLine(
                canvas,
                paint,
                line,
                left,
                y + 16f,
                maxWidth,
                BODY_SIZE,
                if (line == content.forecastAgeLine) content.mutedInkArgb else content.inkArgb,
            )
        }

        drawLine(
            canvas,
            paint,
            content.watermark,
            left,
            HEIGHT - 42f,
            maxWidth,
            WATERMARK_SIZE,
            content.mutedInkArgb,
            bold = true,
            tracked = true,
        )
    }

    /**
     * One wrapped, left-aligned line block. Returns the y to continue from.
     *
     * `breakText` does the measuring, so what is drawn is what fits: a card is a fixed canvas, and a
     * wrapped string that overflowed the card would simply be invisible.
     */
    private fun drawLine(
        canvas: Canvas,
        paint: Paint,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        size: Float,
        color: Int,
        bold: Boolean = false,
        tracked: Boolean = false,
    ): Float {
        if (text.isEmpty()) {
            return y
        }
        paint.reset()
        paint.isAntiAlias = true
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) {
            Typeface.create(TYPEFACE, Typeface.BOLD)
        } else {
            Typeface.create(TYPEFACE, Typeface.NORMAL)
        }
        paint.letterSpacing = if (tracked) 0.06f else 0f
        paint.setShadowLayer(6f, 0f, 2f, SHADOW)

        var cursor = y
        var remaining = text
        var guard = 0
        while (remaining.isNotEmpty() && guard < MAX_WRAP_LINES) {
            guard++
            val count = paint.breakText(remaining, true, maxWidth, null)
            if (count <= 0) {
                break
            }
            canvas.drawText(remaining.substring(0, count), x, cursor, paint)
            remaining = remaining.substring(count).trimStart()
            if (remaining.isNotEmpty()) {
                cursor += size * 1.28f
            }
        }
        paint.clearShadowLayer()
        paint.letterSpacing = 0f
        return cursor
    }

    /** Decodes the bundled panorama, or `null` when it is not packaged or will not decode. */
    private fun decodePanorama(context: Context): Bitmap? {
        // `Present<T>` is generic: Kotlin has no raw types, so the bare `is BundledAsset.Present` is a
        // compile error and erasure forbids a concrete type argument. The star projection is the only
        // legal check, and the payload is recovered by a checked cast.
        val asset = SkyAssetSource(context).milkyWayMap()
        val bytes = when (asset) {
            is BundledAsset.Present<*> -> asset.value as ByteArray
            is BundledAsset.Absent -> return null
            is BundledAsset.Malformed -> return null
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= PANORAMA_MAX_WIDTH) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) }.getOrNull()
    }

    // ---------------------------------------------------------------- the share surface

    /** `cache-path name="share" path="share/"`: the one directory the provider exposes for sharing. */
    fun cacheShareDir(context: Context): File = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }

    /** The file the postcard is written to before it is shared. */
    fun cacheFileFor(context: Context, fileName: String): File = File(cacheShareDir(context), fileName)

    /**
     * The `FileProvider` authority. It is built from the runtime `packageName`, which *is* the
     * `applicationId`, so it agrees with the manifest's `${applicationId}.fileprovider` on every build
     * variant — including one carrying an `applicationIdSuffix`.
     */
    fun authority(context: Context): String = context.packageName + AUTHORITY_SUFFIX

    /** A stable, filesystem-safe name for the card: `lumintra-<night>-<spot>.png`, sanitised, bounded. */
    fun fileName(nightDate: String, spot: String): String {
        val stem = buildString {
            append("lumintra")
            if (nightDate.isNotEmpty()) {
                append('-').append(nightDate)
            }
            if (spot.isNotEmpty()) {
                append('-').append(spot)
            }
        }
        val safe = stem.map { character ->
            if (character.isLetterOrDigit() && character.code < 128) character else '-'
        }.joinToString("").trim('-').replace(Regex("-{2,}"), "-")
        val bounded = if (safe.length > MAX_FILE_STEM) safe.substring(0, MAX_FILE_STEM).trim('-') else safe
        return (if (bounded.isEmpty()) "lumintra-postcard" else bounded) + ".png"
    }

    /** Writes the PNG into the slot's own cache. Returns the file, or `null` when the write failed. */
    fun writeToCache(context: Context, bitmap: Bitmap, fileName: String): File? {
        val file = cacheFileFor(context, fileName)
        val written = runCatching {
            file.outputStream().use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
        }.getOrDefault(false)
        return if (written) file else null
    }

    /** Writes the PNG to a URI the user chose through `ACTION_CREATE_DOCUMENT`. */
    fun writeTo(context: Context, bitmap: Bitmap, target: Uri): Boolean =
        runCatching {
            context.contentResolver.openOutputStream(target)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            } ?: false
        }.getOrDefault(false)

    /** Removes stale share files left by a killed process (spec: cleaned on next app start). */
    fun pruneCache(context: Context) {
        cacheShareDir(context).listFiles()?.forEach { file -> file.delete() }
    }

    /** The provider URI for a file in the share directory. Throws when the file is outside it. */
    fun uriFor(context: Context, file: File): Uri = FileProvider.getUriForFile(context, authority(context), file)

    /**
     * `ACTION_SEND` for the composed card. The brief's text travels as `EXTRA_TEXT` beside it, so a
     * receiver that cannot take an image still gets the reading — the choice is the user's, not ours.
     */
    fun shareIntent(context: Context, file: File, text: String): Intent {
        val uri = uriFor(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = MIME
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_TITLE, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, file.name, uri)
        }
    }

    /** `ACTION_SEND` for the text brief alone. */
    fun shareTextIntent(text: String): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    /** The export half of the surface: the user picks where the card lands. */
    fun exportDocumentIntent(fileName: String): Intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = MIME
        putExtra(Intent.EXTRA_TITLE, fileName)
    }

    // ---------------------------------------------------------------- geometry and constants

    /** The horizon sits a little above two thirds, which is where the band's own gradient ends. */
    private const val HORIZON_FRACTION: Float = 0.56f

    private const val STAR_COUNT: Int = 260
    private const val PANORAMA_ALPHA: Int = 214
    private const val SCRIM_TOP: Int = 0x14040910
    private const val SCRIM_BOTTOM: Int = 0xCC050912L.toInt()
    private const val GLOW_CORE: Int = 0x2E7C8CC4
    private const val GLOW_EDGE: Int = 0x00000000
    private const val SHADOW: Int = 0xB3000000L.toInt()

    private const val CARD_MARGIN: Float = 92f
    private const val CARD_RADIUS: Float = 40f
    private const val TITLE_SIZE: Float = 62f
    private const val SUBTITLE_SIZE: Float = 34f
    private const val BODY_SIZE: Float = 31f
    private const val SMALL_SIZE: Float = 26f
    private const val CREDIT_ON_IMAGE_SIZE: Float = 46f
    private const val CREDIT_LINK_SIZE: Float = 28f
    private const val CREDIT_PLATE: Int = 0xEB050912L.toInt()
    private const val CREDIT_PLATE_RISE: Float = 268f
    private const val CREDIT_PLATE_FALL: Float = 92f
    private const val WATERMARK_SIZE: Float = 24f
    private const val FOOT_HEIGHT: Float = 96f
    private const val MAX_WRAP_LINES: Int = 6
    private const val TYPEFACE: String = "sans-serif-condensed"

    private val CORNER_SIGNS: List<Pair<Float, Float>> = listOf(
        -1f to -1f,
        1f to -1f,
        -1f to 1f,
        1f to 1f,
    )
}

/**
 * Everything the postcard draws, already formatted for the card.
 *
 * It is deliberately *strings plus a few numbers*: the sheet's lines are built by the ViewModel from
 * the same [com.gexetr.lumintralightplanner.ui.share.BriefShareText] material the text brief uses, so a
 * card and a brief shared side by side cannot state different readings. The four geometry values are
 * the only numbers this file reasons with, and each one comes from the session, never from a default.
 *
 * @param skyTopArgb the sky gradient's zenith colour, from the slot's own palette
 * @param coreFracX horizontal position of the galaxy core in the panorama (0..1), from the session
 * @param coreFracY vertical position of the galaxy core in the panorama (0..1), from the session
 * @param moonLitFraction the Moon's illuminated fraction, 0..1
 * @param moonWaxing true while the Moon is waxing, which decides the lit limb
 * @param objectFractionOfFrame the target's major axis as a fraction of the frame's *shorter* side,
 *   or `null` when the catalogue has no size for it — in which case no marker is drawn and no fit
 *   line is stated, exactly as the spec requires
 * @param forecastAgeLine the cloud outlook's own age line, drawn in the muted ink because it is a
 *   forecast and the card must not let it read as a measurement
 */
data class PostcardContent(
    val title: String,
    val subtitle: String,
    val nightLine: String,
    val spotLine: String,
    val positionLine: String,
    val timeLines: List<String>,
    val targetLines: List<String>,
    val fitLines: List<String>,
    val forecastLines: List<String>,
    val forecastAgeLine: String,
    val creditLine: String,
    val creditDetail: String,
    val creditModification: String,
    val watermark: String,
    val skyTopArgb: Int,
    val skyBottomArgb: Int,
    val horizonArgb: Int,
    val groundArgb: Int,
    val cardArgb: Int,
    val inkArgb: Int,
    val mutedInkArgb: Int,
    val accentArgb: Int,
    val moonLitArgb: Int,
    val moonShadowArgb: Int,
    val coreFracX: Float,
    val coreFracY: Float,
    val moonLitFraction: Double,
    val moonWaxing: Boolean,
    val objectFractionOfFrame: Double?,
)
