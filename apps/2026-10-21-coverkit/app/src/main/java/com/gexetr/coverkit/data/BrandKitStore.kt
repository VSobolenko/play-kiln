package com.gexetr.coverkit.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gexetr.coverkit.domain.BrandKit
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local brand kit store. Never blocks happy-path export when unset.
 */
interface BrandKitStore {
    val brandKit: Flow<BrandKit>
    suspend fun save(kit: BrandKit)
    suspend fun setLogoUri(uri: String?)
    suspend fun setColors(colors: List<Int>)
    suspend fun setFonts(primary: String, secondary: String)
    suspend fun clear()
}

private val Context.brandKitDataStore by preferencesDataStore(name = "brand_kit")

class DataStoreBrandKitStore(
    private val context: Context,
) : BrandKitStore {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override val brandKit: Flow<BrandKit> = context.brandKitDataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { preferences ->
            val raw = preferences[BRAND_KIT_JSON]
            if (raw.isNullOrBlank()) {
                BrandKit()
            } else {
                runCatching { json.decodeFromString<BrandKit>(raw) }.getOrDefault(BrandKit())
            }
        }

    override suspend fun save(kit: BrandKit) {
        val normalised = kit.copy(
            logoUri = kit.logoUri?.takeIf { it.isNotBlank() },
            colors = kit.colors.take(BrandKit.MAX_COLORS),
            fontPrimary = kit.fontPrimary.ifBlank { BrandKit.FONT_DEFAULT_PRIMARY },
            fontSecondary = kit.fontSecondary.ifBlank { BrandKit.FONT_DEFAULT_SECONDARY },
        )
        context.brandKitDataStore.edit { preferences ->
            preferences[BRAND_KIT_JSON] = json.encodeToString(normalised)
        }
    }

    override suspend fun setLogoUri(uri: String?) {
        update { it.copy(logoUri = uri?.takeIf { value -> value.isNotBlank() }) }
    }

    override suspend fun setColors(colors: List<Int>) {
        update { it.copy(colors = colors.take(BrandKit.MAX_COLORS)) }
    }

    override suspend fun setFonts(primary: String, secondary: String) {
        update {
            it.copy(
                fontPrimary = primary.ifBlank { BrandKit.FONT_DEFAULT_PRIMARY },
                fontSecondary = secondary.ifBlank { BrandKit.FONT_DEFAULT_SECONDARY },
            )
        }
    }

    override suspend fun clear() {
        context.brandKitDataStore.edit { it.clear() }
    }

    private suspend fun update(transform: (BrandKit) -> BrandKit) {
        context.brandKitDataStore.edit { preferences ->
            val current = preferences[BRAND_KIT_JSON]
                ?.let { raw -> runCatching { json.decodeFromString<BrandKit>(raw) }.getOrNull() }
                ?: BrandKit()
            preferences[BRAND_KIT_JSON] = json.encodeToString(transform(current))
        }
    }

    private companion object {
        val BRAND_KIT_JSON = stringPreferencesKey("brand_kit_json")
    }
}
