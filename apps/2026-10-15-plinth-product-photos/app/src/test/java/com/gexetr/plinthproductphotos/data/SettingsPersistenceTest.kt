package com.gexetr.plinthproductphotos.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsPersistenceTest {
    @Test
    fun storedCompositionDefaultsRoundTripThroughNewStoreInstance() = runBlocking {
        val disk = MemoryDataStore()
        val expected = StudioDefaults(
            background = 2, shadow = 2, marginPct = 0.1f, fillTarget = 0.7f,
            itemScale = 1.2f, horizontalPosition = -0.08f, verticalPosition = 0.09f,
        )
        DataStoreSettingsStore(disk).updateStudioDefaults { expected }

        assertEquals(expected, DataStoreSettingsStore(disk).settings.first().studioDefaults)
    }

    @Test
    fun changingOneControlPreservesOtherDefaultsAndAppPreferences() = runBlocking {
        val store = DataStoreSettingsStore(MemoryDataStore())
        store.setHapticsEnabled(false)
        store.setThemeMode(ThemeMode.DARK)
        store.updateStudioDefaults { it.copy(marginPct = 0.12f) }
        store.updateStudioDefaults { it.copy(shadow = 0) }

        val restored = store.settings.first()
        assertEquals(0.12f, restored.studioDefaults.marginPct)
        assertEquals(0, restored.studioDefaults.shadow)
        assertEquals(ThemeMode.DARK, restored.themeMode)
        assertFalse(restored.hapticsEnabled)
        store.updateStudioDefaults { StudioDefaults() }
        assertEquals(StudioDefaults(), store.settings.first().studioDefaults)
        assertFalse(store.settings.first().hapticsEnabled)
    }

    @Test
    fun failedStudioWriteIsNotReportedAsSuccess() = runBlocking {
        val store = DataStoreSettingsStore(object : DataStore<Preferences> {
            override val data: Flow<Preferences> = MutableStateFlow(emptyPreferences())
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
                throw IOException("Disk full")
        })
        var failure: IOException? = null
        try {
            store.updateStudioDefaults { it.copy(background = 1) }
        } catch (observed: IOException) {
            failure = observed
        }
        assertEquals("Disk full", failure?.message)
    }

    private class MemoryDataStore : DataStore<Preferences> {
        private val values = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override val data: Flow<Preferences> = values
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock { transform(values.value).also { values.value = it } }
    }
}
