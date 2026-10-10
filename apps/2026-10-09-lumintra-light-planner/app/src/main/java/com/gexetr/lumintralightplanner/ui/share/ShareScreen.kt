package com.gexetr.lumintralightplanner.ui.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.text.format.DateFormat
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gexetr.lumintralightplanner.R
import com.gexetr.lumintralightplanner.data.CatalogueRepository
import com.gexetr.lumintralightplanner.data.CatalogueResult
import com.gexetr.lumintralightplanner.data.CloudRepository
import com.gexetr.lumintralightplanner.data.CloudResult
import com.gexetr.lumintralightplanner.data.ScoutSnapshot
import com.gexetr.lumintralightplanner.data.ScoutStore
import com.gexetr.lumintralightplanner.data.sky.NightComputation
import com.gexetr.lumintralightplanner.data.sky.NightEngine
import com.gexetr.lumintralightplanner.data.sky.NightRequest
import com.gexetr.lumintralightplanner.data.sky.SkyEphemeris
import com.gexetr.lumintralightplanner.domain.CloudReport
import com.gexetr.lumintralightplanner.domain.FieldOfView
import com.gexetr.lumintralightplanner.domain.FitVerdict
import com.gexetr.lumintralightplanner.domain.LensPreset
import com.gexetr.lumintralightplanner.domain.MoonPhaseName
import com.gexetr.lumintralightplanner.domain.NightSpan
import com.gexetr.lumintralightplanner.domain.SatellitePass
import com.gexetr.lumintralightplanner.domain.Scout
import com.gexetr.lumintralightplanner.domain.ScoutSelection
import com.gexetr.lumintralightplanner.domain.SessionBrief
import com.gexetr.lumintralightplanner.domain.SessionClock
import com.gexetr.lumintralightplanner.domain.SkyBrightnessEstimate
import com.gexetr.lumintralightplanner.domain.SkyObject
import com.gexetr.lumintralightplanner.domain.TargetFit
import com.gexetr.lumintralightplanner.ui.components.LumintraEmptyState
import com.gexetr.lumintralightplanner.ui.components.LumintraPanel
import com.gexetr.lumintralightplanner.ui.components.LumintraScaffoldHeader
import com.gexetr.lumintralightplanner.ui.components.SectionStrip
import com.gexetr.lumintralightplanner.ui.format.formatNightLabel
import com.gexetr.lumintralightplanner.ui.format.formatSignedDegrees
import com.gexetr.lumintralightplanner.ui.format.formatTime
import com.gexetr.lumintralightplanner.ui.theme.LumintraDarkColors
import com.gexetr.lumintralightplanner.ui.theme.LumintraTheme
import com.gexetr.lumintralightplanner.ui.theme.LumintraType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

// =================================================================================================
// 1. The state the screen renders
// =================================================================================================

/** Which payload the screen is previewing — and, because the preview is the payload, which one it sends. */
enum class ShareMode { TEXT, POSTCARD }

/**
 * A one-shot sentence under the preview.
 *
 * Every value is a state the spec names explicitly. A cancelled share is deliberately **not** here:
 * a cancel is not a failure, and the spec forbids reporting it as one.
 */
enum class ShareNotice(@StringRes val textRes: Int) {
    /** No activity can receive the handoff, so the fallback was offered instead of an empty chooser. */
    NO_SHARE_TARGET(R.string.share_no_target_app),

    /** The text brief is on the clipboard, which is the text option's own fallback. */
    COPIED(R.string.share_copied),

    /** The postcard was written where the user chose. */
    SAVED(R.string.share_saved),

    /** The write failed; the preview is untouched and the card can be saved again. */
    SAVE_FAILED(R.string.share_save_failed),

    /** The card could not be composed; the text option stays available and the send control is disabled. */
    POSTCARD_UNAVAILABLE(R.string.share_postcard_unavailable),
}

/**
 * Everything the Share screen renders.
 *
 * **The preview is the payload.** [textPayload] is the exact string that goes into `EXTRA_TEXT`, and
 * [postcard] is the exact bitmap that is written and shared — both are held here rather than rendered
 * twice, because "the same characters" and "the same pixels" cannot be promised by two renderings.
 */
@Immutable
data class ShareUiState(
    val loading: Boolean = true,
    val mode: ShareMode = ShareMode.TEXT,
    /** No saved spot: the payload is not composed and the send control is disabled. */
    val needsSpot: Boolean = false,
    val spotName: String = "",
    val nightLabel: String = "",
    val textPayload: String = "",
    val characterCount: Int = 0,
    val textDescription: String = "",
    val postcard: Bitmap? = null,
    val postcardUnavailable: Boolean = false,
    val postcardDescription: String = "",
    val postcardFileName: String = "",
    val notice: ShareNotice? = null,
    val locale: Locale = Locale.getDefault(),
) {
    /** The selected option has something to send. The send control is disabled until it does. */
    val previewReady: Boolean
        get() = when (mode) {
            ShareMode.TEXT -> textPayload.isNotEmpty()
            ShareMode.POSTCARD -> postcard != null
        }

    val canSend: Boolean get() = !loading && !needsSpot && previewReady

    /** Saving is offered exactly when the card exists, so the picker is never opened for nothing. */
    val canSavePostcard: Boolean get() = postcard != null
}

// =================================================================================================
// 2. The ViewModel — the payload and the two handoffs
// =================================================================================================

/**
 * The Share screen's payload and its two handoffs.
 *
 * **It composes the brief itself**, from the same `NightEngine` computation every other screen reads,
 * so the payload is for the same spot and the same night the entry surface displayed: a share screen
 * that took its facts from somewhere else is exactly how a brief ends up describing a different spot.
 *
 * **It renders the postcard off the main thread and keeps it.** `PostcardExport.render` decodes the
 * bundled ESO panorama, so it is a real cost, and the bitmap it returns is the one that is previewed
 * and the one that is written — never two renders of one night, which could differ by a pixel and
 * break the promise that the preview is the payload.
 *
 * **The two handoffs.** [send] performs the selected one and reports it as a [ShareNotice]:
 * `ACTION_SEND` through the slot's own `FileProvider` for the card, `ACTION_SEND`/`text/plain` for the
 * brief. When nothing on the device can receive it, the text goes to the clipboard and the postcard
 * offers [documentIntent] — the spec's own fallback — rather than an empty chooser.
 * [onShareReturned] is what the caller calls when the sheet comes back, and it deletes the temporary
 * file, so a cancelled share leaves nothing behind.
 *
 * The scratch file lives in `cacheDir/share/`, the only directory the provider exposes, and
 * [PostcardExport.pruneCache] clears anything a killed process left there — the spec's "deleted on the
 * next app start". `init` runs it, so opening Share is enough to clean up after a crash.
 */
class ShareViewModel(
    private val appContext: Context,
    private val scoutStore: ScoutStore,
    private val catalogue: CatalogueRepository,
    private val cloudRepository: CloudRepository,
    private val ephemeris: SkyEphemeris,
    /** The night's passes, as an injected search — the same shape `BriefViewModel` takes. */
    private val passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass> =
        { _, _, _ -> emptyList() },
    private val brightnessEstimate: () -> SkyBrightnessEstimate = { SkyBrightnessEstimate.NONE },
    /** False when the user has turned the cloud forecast off: then no request is made at all. */
    private val cloudEnabled: () -> Boolean = { true },
    private val clock: () -> Instant = { Instant.now() },
) : ViewModel() {

    private val _state = MutableStateFlow(ShareUiState(locale = locale()))
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    private val use24Hour: Boolean = DateFormat.is24HourFormat(appContext.applicationContext)
    private val strings: (Int, List<Any>) -> String = { id, args ->
        appContext.getString(id, *args.toTypedArray())
    }

    private var loaded: Loaded? = null
    private var cardJob: Job? = null

    init {
        viewModelScope.launch {
            // Anything a killed process left in cacheDir/share is gone before this screen can be used.
            withContext(Dispatchers.IO) { PostcardExport.pruneCache(appContext) }
            scoutStore.snapshot.collect { snapshot -> recompute(snapshot) }
        }
    }

    // ------------------------------------------------------------------ the user's own events

    /** Selecting an option is offered **before** any send, and renders the card on first use. */
    fun onModeText() {
        _state.value = _state.value.copy(mode = ShareMode.TEXT, notice = null)
    }

    fun onModePostcard() {
        _state.value = _state.value.copy(mode = ShareMode.POSTCARD, notice = null)
        if (loaded != null && _state.value.postcard == null && !_state.value.postcardUnavailable) {
            renderPostcard()
        }
    }

    /** The notice has been read; it is one-shot, not a state the screen keeps forever. */
    fun onNoticeRead() {
        if (_state.value.notice != null) {
            _state.value = _state.value.copy(notice = null)
        }
    }

    /** The exact characters that go into `EXTRA_TEXT`, for a caller that wants to assert the promise. */
    fun payloadText(): String = _state.value.textPayload

    // ------------------------------------------------------------------ the two handoffs

    /**
     * Performs the selected handoff.
     *
     * @return the sentence the screen should show, or `null` when the intent was started and nothing
     *   needs saying — the sheet itself is the feedback.
     */
    fun send(context: Context): ShareNotice? {
        val current = _state.value
        if (!current.canSend) {
            return null
        }
        return when (current.mode) {
            ShareMode.TEXT -> {
                val intent = PostcardExport.shareTextIntent(current.textPayload)
                if (start(context, Intent.createChooser(intent, chooserTitle(context)))) {
                    null
                } else {
                    copyToClipboard(context, current.textPayload)
                    ShareNotice.COPIED.also { publish(it) }
                }
            }

            ShareMode.POSTCARD -> {
                val bitmap = current.postcard ?: return null
                val file = PostcardExport.writeToCache(context, bitmap, current.postcardFileName)
                if (file == null) {
                    return ShareNotice.SAVE_FAILED.also { publish(it) }
                }
                val intent = PostcardExport.shareIntent(context, file, current.textPayload)
                if (start(context, Intent.createChooser(intent, chooserTitle(context)))) {
                    null
                } else {
                    ShareNotice.NO_SHARE_TARGET.also { publish(it) }
                }
            }
        }
    }

    /** The current card, for a caller that wants to show or write it itself. Never a second render. */
    fun postcard(): Bitmap? = _state.value.postcard

    /** The `ACTION_CREATE_DOCUMENT` half of the postcard fallback: the user picks where it lands. */
    fun documentIntent(): Intent? =
        if (_state.value.postcard == null) {
            null
        } else {
            PostcardExport.exportDocumentIntent(_state.value.postcardFileName)
        }

    /** Writes the card to the URI the user chose, and reports the outcome as a notice. */
    fun onPostcardSaved(context: Context, target: Uri): ShareNotice {
        val bitmap = _state.value.postcard ?: return ShareNotice.SAVE_FAILED.also { publish(it) }
        val written = PostcardExport.writeTo(context, bitmap, target)
        val notice = if (written) ShareNotice.SAVED else ShareNotice.SAVE_FAILED
        publish(notice)
        return notice
    }

    /**
     * The sheet came back — sent or cancelled, the app cannot tell and does not need to. The screen
     * returns to the preview unchanged, shows no error, and the temporary file is removed
     * (`specs/share-brief`: a cancelled share is not a failure and leaves nothing behind).
     */
    fun onShareReturned() {
        viewModelScope.launch { withContext(Dispatchers.IO) { PostcardExport.pruneCache(appContext) } }
    }

    // ------------------------------------------------------------------ the one computation

    private suspend fun recompute(snapshot: ScoutSnapshot) {
        val spot = ScoutSelection.resolve(snapshot.spots, snapshot.activeSpotId)
        if (spot == null) {
            loaded = null
            cardJob?.cancel()
            _state.value = _state.value.copy(
                loading = false,
                needsSpot = true,
                spotName = "",
                nightLabel = "",
                textPayload = "",
                characterCount = 0,
                textDescription = "",
                postcard = null,
                postcardUnavailable = false,
                postcardDescription = "",
                notice = null,
            )
            return
        }

        val horizon = spot.horizonProfileId?.let { snapshot.horizons[it] }
        val now = clock()
        val zone = ZoneId.of(spot.tzId)
        val nightDate = SessionClock.nightOf(now, zone)
        val session = SessionClock.sessionWindow(nightDate, zone, spot.id)

        val objects = withContext(Dispatchers.Default) {
            (catalogue.load() as? CatalogueResult.Success)?.objects.orEmpty()
        }
        val cloud = cloudOf(spot, now)
        val passes = withContext(Dispatchers.Default) { passProvider(spot, session, now) }
        val request = NightRequest(
            spot = spot,
            horizon = horizon,
            equipment = spot.equipment,
            span = session,
            zone = zone,
            nowUtc = now,
            favouriteIds = snapshot.favourites,
            objects = objects,
            cloud = cloud,
            passes = passes,
            elementSetAge = passes.firstOrNull()?.elementSetAge,
            altitudeMaskDeg = NightRequest.DEFAULT_ALTITUDE_MASK_DEG,
            skyBrightnessEstimate = brightnessEstimate(),
        )
        val night = withContext(Dispatchers.Default) { NightEngine.compute(request, ephemeris) }

        val byId = objects.associateBy { it.id }
        val lens = spot.equipment.toLensPreset()
        val brief = night.brief
        val payload = BriefShareText.build(brief, byId, lens, zone, locale(), use24Hour, strings)
        val nightLabel = formatNightLabel(session.startUtc, session.endUtc, zone, locale())
        val data = Loaded(
            spotName = brief.spotName,
            zone = zone,
            brief = brief,
            objects = byId,
            lens = lens,
            fit = fitOf(night, byId, snapshot.favourites),
            instant = cardInstant(night),
            nightDate = nightDate.toString(),
            nightLabel = nightLabel,
        )
        loaded = data

        _state.value = _state.value.copy(
            loading = false,
            needsSpot = false,
            spotName = data.spotName,
            nightLabel = nightLabel,
            textPayload = payload,
            characterCount = BriefShareText.characterCount(payload),
            textDescription = appContext.getString(R.string.share_preview_text_description, payload.length),
            postcard = null,
            postcardUnavailable = false,
            postcardDescription = postcardDescription(data),
            postcardFileName = PostcardExport.fileName(data.nightDate, data.spotName),
            notice = null,
        )
        if (_state.value.mode == ShareMode.POSTCARD) {
            renderPostcard()
        }
    }

    /**
     * Renders the card, off the main thread, and keeps it — the preview and the shared file are the
     * same bitmap. A failed render is a stated state, never an empty or half-drawn image.
     */
    private fun renderPostcard() {
        val data = loaded ?: return
        cardJob?.cancel()
        cardJob = viewModelScope.launch {
            val content = cardContent(data)
            val bitmap = withContext(Dispatchers.Default) {
                runCatching { PostcardExport.render(appContext, content) }.getOrNull()
            }
            _state.value = _state.value.copy(
                postcard = bitmap,
                postcardUnavailable = bitmap == null,
                notice = if (bitmap == null) ShareNotice.POSTCARD_UNAVAILABLE else null,
            )
        }
    }

    /** The card's own text, from the same blocks the message body uses, so the two cannot disagree. */
    private fun cardContent(data: Loaded): PostcardContent {
        val blocks = BriefShareText.blocks(
            brief = data.brief,
            objects = data.objects,
            lens = data.lens,
            zoneId = data.zone,
            locale = locale(),
            use24Hour = use24Hour,
            strings = strings,
        )
        val palette = LumintraDarkColors
        return PostcardContent(
            title = appContext.getString(R.string.app_name),
            subtitle = text(R.string.share_text_night, data.nightLabel),
            nightLine = blocks.dark.firstOrNull().orEmpty(),
            spotLine = text(R.string.share_text_spot, data.spotName),
            positionLine = text(
                R.string.share_text_position,
                formatSignedDegrees(data.brief.latitudeDeg),
                formatSignedDegrees(data.brief.longitudeDeg),
            ),
            timeLines = buildList {
                add(text(R.string.postcard_instant, formatTime(data.instant, data.zone, locale(), use24Hour)))
                addAll(blocks.twilight.take(CARD_TWILIGHT_LINES))
                addAll(blocks.lens)
            },
            targetLines = blocks.targets.take(CARD_TARGET_LINES),
            fitLines = blocks.moon + fitLines(data.fit, data.lens),
            forecastLines = blocks.cloud.take(CARD_FORECAST_LINES),
            // The forecast's own source-and-age line, drawn in the muted ink: a forecast is not a measurement.
            forecastAgeLine = blocks.cloud.getOrNull(1).orEmpty(),
            creditLine = appContext.getString(R.string.credit_eso_credit),
            creditDetail = appContext.getString(R.string.postcard_credit_source),
            creditModification = appContext.getString(R.string.postcard_credit_modified),
            watermark = appContext.getString(R.string.app_name),
            skyTopArgb = palette.skyZenith.toArgb(),
            skyBottomArgb = palette.skyHorizon.toArgb(),
            horizonArgb = palette.skyHorizon.toArgb(),
            groundArgb = palette.skyZenith.toArgb(),
            cardArgb = palette.skyZenith.toArgb(),
            inkArgb = palette.onSky.toArgb(),
            mutedInkArgb = palette.onSkyMuted.toArgb(),
            accentArgb = palette.firstLight.toArgb(),
            moonLitArgb = palette.moonLit.toArgb(),
            moonShadowArgb = palette.moonShadow.toArgb(),
            coreFracX = CORE_FRAC_X,
            coreFracY = CORE_FRAC_Y,
            moonLitFraction = data.brief.moon.illuminatedFraction,
            moonWaxing = isWaxing(data.brief.moon.phaseName),
            objectFractionOfFrame = data.fit?.frameFrac,
        )
    }

    /**
     * The fit statement: the object's own size, the frame's short side, the optics in millimetres.
     *
     * A magnitude never produces a fit line and no size means no fit statement at all — the card says
     * the size is unknown instead, which is what the catalogue actually knows.
     */
    private fun fitLines(fit: TargetFit?, lens: LensPreset): List<String> {
        if (fit == null) {
            return emptyList()
        }
        val size = fit.objectAngularSizeDeg
        if (size == null || fit.verdict == FitVerdict.UNKNOWN_SIZE) {
            return listOf(appContext.getString(R.string.target_fit_point_source))
        }
        val out = ArrayList<String>(3)
        out += when (fit.verdict) {
            FitVerdict.FITS -> appContext.getString(R.string.target_fit_fits)
            FitVerdict.MARGINAL -> appContext.getString(R.string.target_fit_marginal)
            FitVerdict.TOO_LARGE -> appContext.getString(R.string.target_fit_too_large)
            FitVerdict.UNKNOWN_SIZE -> appContext.getString(R.string.target_fit_point_source)
        }
        out += text(
            R.string.target_fit_numbers,
            number(size, 1),
            number(fit.frameFovShortSideDeg, 1),
            number(lens.focalLengthMm, 0),
            number(lens.sensorWidthMm, 1),
            number(lens.sensorHeightMm, 1),
        )
        fit.pixelsOnSensor?.let { out += text(R.string.target_fit_pixels, number(it, 0)) }
        return out
    }

    /** The chosen target's fit: a favourite that is up tonight wins, otherwise the catalogue's first. */
    private fun fitOf(
        night: NightComputation,
        objects: Map<String, SkyObject>,
        favourites: Set<String>,
    ): TargetFit? {
        val up = night.targetWindows.keys.sortedWith(
            Comparator { left, right ->
                val a = objects[left]
                val b = objects[right]
                when {
                    a != null && b != null -> SkyObject.CATALOGUE_ORDER.compare(a, b)
                    a != null -> -1
                    b != null -> 1
                    else -> left.compareTo(right)
                }
            },
        )
        val chosen = up.firstOrNull { it in favourites } ?: up.firstOrNull()
        return chosen?.let { night.targetFits[it] }
    }

    /** The instant the card is drawn for: the middle of the night's own dark window. */
    private fun cardInstant(night: NightComputation): Instant {
        val window = night.bestDarkWindow ?: return night.session.startUtc.plusSeconds(HALF_DAY_SECONDS)
        return Instant.ofEpochSecond((window.startUtc.epochSecond + window.endUtc.epochSecond) / 2L)
    }

    private fun postcardDescription(data: Loaded): String {
        val moon = data.brief.moon
        val percent = Math.round(moon.illuminatedFraction * 100.0)
        val fov = FieldOfView.of(data.lens)
        return text(
            R.string.share_preview_postcard_description,
            data.spotName,
            data.nightLabel,
            formatTime(data.instant, data.zone, locale(), use24Hour),
            number(data.lens.focalLengthMm, 0) + " mm / " + number(fov.fovH, 1) + "\u00b0",
            percent.toString() + "% " + phaseWord(moon.phaseName),
        )
    }

    private fun phaseWord(phase: MoonPhaseName): String = appContext.getString(
        when (phase) {
            MoonPhaseName.NEW_MOON -> R.string.share_phase_new_moon
            MoonPhaseName.WAXING_CRESCENT -> R.string.share_phase_waxing_crescent
            MoonPhaseName.FIRST_QUARTER -> R.string.share_phase_first_quarter
            MoonPhaseName.WAXING_GIBBOUS -> R.string.share_phase_waxing_gibbous
            MoonPhaseName.FULL_MOON -> R.string.share_phase_full_moon
            MoonPhaseName.WANING_GIBBOUS -> R.string.share_phase_waning_gibbous
            MoonPhaseName.LAST_QUARTER -> R.string.share_phase_last_quarter
            MoonPhaseName.WANING_CRESCENT -> R.string.share_phase_waning_crescent
        },
    )

    // ------------------------------------------------------------------ small helpers

    /** The one network call: made only when the user's own preference allows it, never at launch. */
    private suspend fun cloudOf(spot: Scout, now: Instant): CloudReport? {
        if (!cloudEnabled()) {
            return null
        }
        return when (val result = cloudRepository.fetch(spot.point, now)) {
            is CloudResult.Available -> result.report
            is CloudResult.Unavailable -> result.lastReading
        }
    }

    private fun locale(): Locale =
        appContext.resources.configuration.locales.get(0) ?: Locale.getDefault()

    private fun number(value: Double, digits: Int): String = String.format(locale(), "%.${digits}f", value)

    private fun text(id: Int, vararg args: Any): String = appContext.getString(id, *args)

    private fun chooserTitle(context: Context): String = context.getString(R.string.share_title)

    /** The waxing half of the lunation, taken from the phase's own name rather than a second estimate. */
    private fun isWaxing(phase: MoonPhaseName): Boolean = when (phase) {
        MoonPhaseName.WAXING_CRESCENT,
        MoonPhaseName.FIRST_QUARTER,
        MoonPhaseName.WAXING_GIBBOUS,
        -> true

        MoonPhaseName.NEW_MOON,
        MoonPhaseName.WANING_GIBBOUS,
        MoonPhaseName.LAST_QUARTER,
        MoonPhaseName.WANING_CRESCENT,
        MoonPhaseName.FULL_MOON,
        -> false
    }

    /** Starts the intent; `false` means nothing on the device could receive it. */
    private fun start(context: Context, intent: Intent): Boolean {
        if (context !is android.app.Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (missing: android.content.ActivityNotFoundException) {
            false
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.share_title), text))
    }

    private fun publish(notice: ShareNotice) {
        _state.value = _state.value.copy(notice = notice)
    }

    /** Everything the payload and the card need, held once so a mode change cannot recompute the night. */
    private class Loaded(
        val spotName: String,
        val zone: ZoneId,
        val brief: SessionBrief,
        val objects: Map<String, SkyObject>,
        val lens: LensPreset,
        val fit: TargetFit?,
        val instant: Instant,
        val nightDate: String,
        val nightLabel: String,
    )

    companion object {
        /** The list caps the card uses: a card is not a message body, and a block has to fit it. */
        const val CARD_TWILIGHT_LINES: Int = 4
        const val CARD_TARGET_LINES: Int = 3
        const val CARD_FORECAST_LINES: Int = 4

        /** Where the galaxy core sits in the bundled panorama. A property of the image, not of the night. */
        const val CORE_FRAC_X: Float = 0.5f
        const val CORE_FRAC_Y: Float = 0.5f

        private const val HALF_DAY_SECONDS: Long = 12L * 3_600L

        /** The factory taking manual dependencies — this app uses no injection framework. */
        @Suppress("UNCHECKED_CAST")
        fun factory(
            appContext: Context,
            scoutStore: ScoutStore,
            catalogue: CatalogueRepository,
            cloudRepository: CloudRepository,
            ephemeris: SkyEphemeris,
            passProvider: suspend (Scout, NightSpan, Instant) -> List<SatellitePass> =
                { _, _, _ -> emptyList() },
            brightnessEstimate: () -> SkyBrightnessEstimate = { SkyBrightnessEstimate.NONE },
            cloudEnabled: () -> Boolean = { true },
            clock: () -> Instant = { Instant.now() },
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ShareViewModel(
                appContext = appContext.applicationContext,
                scoutStore = scoutStore,
                catalogue = catalogue,
                cloudRepository = cloudRepository,
                ephemeris = ephemeris,
                passProvider = passProvider,
                brightnessEstimate = brightnessEstimate,
                cloudEnabled = cloudEnabled,
                clock = clock,
            ) as T
        }
    }
}

// =================================================================================================
// 3. The screen
// =================================================================================================

/**
 * Share: the last screen before the brief leaves the phone.
 *
 * The order is the promise: **choose** the payload (two options, the selected one named in words and
 * not only by colour), **see** the exact payload (the very characters, selectable; the very bitmap),
 * and only then **send** — the send control exists but is disabled until the selected preview has been
 * rendered, so an empty handoff cannot be triggered by a fast tap.
 *
 * On a wide window the choice sits beside the preview instead of above it: the preview is the content,
 * and the choice is a control over it.
 */
@Composable
fun ShareScreen(
    state: ShareUiState,
    onModeText: () -> Unit,
    onModePostcard: () -> Unit,
    onSend: () -> Unit,
    onSavePostcard: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            LumintraScaffoldHeader(
                title = stringResource(R.string.share_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.loading -> ShareLoading()
                state.needsSpot -> ShareNeedsSpot()
                else -> ShareBody(
                    state = state,
                    onModeText = onModeText,
                    onModePostcard = onModePostcard,
                    onSend = onSend,
                    onSavePostcard = onSavePostcard,
                )
            }
        }
    }
}

@Composable
private fun ShareLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.share_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * No spot: the reason is named and nothing is composed — this app has no default coordinate, so a
 * "brief" for `(0, 0)` is never produced.
 */
@Composable
private fun ShareNeedsSpot() {
    LumintraEmptyState(
        title = stringResource(R.string.share_no_spot_title),
        body = stringResource(R.string.share_no_spot_body),
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        iconRes = R.drawable.ic_ui_map_pin,
    )
}

@Composable
private fun ShareBody(
    state: ShareUiState,
    onModeText: () -> Unit,
    onModePostcard: () -> Unit,
    onSend: () -> Unit,
    onSavePostcard: () -> Unit,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 600
    Column(modifier = Modifier.fillMaxSize()) {
        if (wide) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(modifier = Modifier.sizeIn(minWidth = 240.dp, maxWidth = 320.dp)) {
                    ModeChoice(state = state, onModeText = onModeText, onModePostcard = onModePostcard)
                }
                PreviewColumn(state = state, modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                ModeChoice(state = state, onModeText = onModeText, onModePostcard = onModePostcard)
                Spacer(Modifier.size(16.dp))
                PreviewColumn(state = state, modifier = Modifier.fillMaxWidth())
            }
        }
        ActionBar(state = state, onSend = onSend, onSavePostcard = onSavePostcard)
    }
}

/**
 * The two options, as a radio group: each is at least 48 dp, each is a `RadioButton` to TalkBack, and
 * the selected one says so in words — the choice is never carried by colour alone.
 */
@Composable
private fun ModeChoice(state: ShareUiState, onModeText: () -> Unit, onModePostcard: () -> Unit) {
    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OptionRow(
            label = stringResource(R.string.share_mode_text),
            selected = state.mode == ShareMode.TEXT,
            enabled = state.textPayload.isNotEmpty(),
            onClick = onModeText,
        )
        OptionRow(
            label = stringResource(R.string.share_mode_postcard),
            selected = state.mode == ShareMode.POSTCARD,
            enabled = !state.postcardUnavailable,
            onClick = onModePostcard,
            supporting = if (state.postcardUnavailable) {
                stringResource(R.string.share_postcard_unavailable)
            } else {
                null
            },
        )
    }
}

@Composable
private fun OptionRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    supporting: String? = null,
) {
    LumintraPanel(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(if (selected) R.drawable.ic_ui_check else R.drawable.ic_ui_layers),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) LumintraTheme.colors.firstLight else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                if (selected) {
                    Text(
                        text = stringResource(R.string.share_mode_selected),
                        style = LumintraType.kicker,
                        color = LumintraTheme.colors.firstLight,
                    )
                }
                supporting?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewColumn(state: ShareUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SectionStrip(title = stringResource(R.string.share_preview_header))
        Spacer(Modifier.size(8.dp))
        state.notice?.let { notice ->
            LumintraPanel(modifier = Modifier.fillMaxWidth(), accent = true) {
                Text(
                    text = stringResource(notice.textRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.size(8.dp))
        }
        when (state.mode) {
            ShareMode.TEXT -> TextPreview(state)
            ShareMode.POSTCARD -> PostcardPreview(state)
        }
    }
}

/** The exact characters, selectable and scrollable — the payload is what is being reviewed. */
@Composable
private fun TextPreview(state: ShareUiState) {
    LumintraPanel(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = state.textDescription },
    ) {
        Box(modifier = Modifier.heightIn(max = 360.dp)) {
            SelectionContainer {
                Text(
                    text = state.textPayload,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

/** The exact bitmap, at the card's own ratio, described by the numbers it carries. */
@Composable
private fun PostcardPreview(state: ShareUiState) {
    val bitmap = state.postcard
    if (bitmap == null) {
        LumintraPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(
                    if (state.postcardUnavailable) {
                        R.string.share_postcard_unavailable
                    } else {
                        R.string.share_loading
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = state.postcardDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PostcardExport.WIDTH.toFloat() / PostcardExport.HEIGHT.toFloat()),
    )
}

@Composable
private fun ActionBar(state: ShareUiState, onSend: () -> Unit, onSavePostcard: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onSend,
            enabled = state.canSend,
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ui_share_2),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.share_send),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (state.canSavePostcard) {
            OutlinedButton(
                onClick = onSavePostcard,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_ui_download),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.share_save),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
