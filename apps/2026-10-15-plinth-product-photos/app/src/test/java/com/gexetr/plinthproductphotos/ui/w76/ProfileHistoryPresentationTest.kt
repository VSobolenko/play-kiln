package com.gexetr.plinthproductphotos.ui.w76

import java.nio.file.Files
import com.gexetr.plinthproductphotos.ui.profile.ProfileHistoryCandidate
import com.gexetr.plinthproductphotos.ui.profile.ProfileBalanceStatus
import com.gexetr.plinthproductphotos.ui.profile.ProfileResult
import com.gexetr.plinthproductphotos.ui.profile.profileBalancePresentation
import com.gexetr.plinthproductphotos.ui.profile.recentProfileResults
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileHistoryPresentationTest {
    @Test
    fun importedUuidFilenameIsNotPresentedAsAResultTitle() {
        val output = Files.createTempFile("profile-uuid", ".png").toFile()
        try {
            val result = recentProfileResults(
                listOf(
                    ProfileHistoryCandidate(
                        "a62f12d2-f9fc-43cd-bcd4-55de26fd5162.jpg",
                        output.absolutePath,
                        30L,
                    ),
                ),
            ).single()

            org.junit.Assert.assertFalse(result.title.contains("a62f12d2"))
            org.junit.Assert.assertTrue(result.title.isNotBlank())
        } finally {
            output.delete()
        }
    }

    @Test
    fun showsOnlyRecentSavedResultsWithoutExposingRecordIds() {
        val directory = Files.createTempDirectory("profile-history-test").toFile()
        try {
            val older = directory.resolve("older.png").apply { createNewFile() }
            val newest = directory.resolve("newest.png").apply { createNewFile() }
            val reference = directory.resolve("reference.png").apply { createNewFile() }
            val middle = directory.resolve("middle.png").apply { createNewFile() }
            val results = recentProfileResults(
                listOf(
                    ProfileHistoryCandidate("Older card", older.absolutePath, 10L, photoId = "photo-older"),
                    ProfileHistoryCandidate("Newest card", newest.absolutePath, 30L, photoId = "photo-newest"),
                    ProfileHistoryCandidate("Reference photo", reference.absolutePath, 20L, photoId = "photo-reference", isReference = true),
                    ProfileHistoryCandidate("Middle card", middle.absolutePath, 20L, photoId = "photo-middle"),
                ),
                limit = 2,
            )

            assertEquals(listOf("Newest card", "Middle card"), results.map { it.title })
            assertEquals(listOf(30L, 20L), results.map { it.createdAt })
            assertEquals(listOf(newest.absolutePath, middle.absolutePath), results.map { it.imagePath })
            assertEquals(listOf("photo-newest", "photo-middle"), results.map { it.photoId })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun twoResultsOfOnePhotoKeepDistinctOutputIds() {
        val directory = Files.createTempDirectory("profile-output-id-test").toFile()
        try {
            val a = directory.resolve("a.png").apply { createNewFile() }
            val b = directory.resolve("b.png").apply { createNewFile() }
            val results = recentProfileResults(
                listOf(
                    ProfileHistoryCandidate("A", a.absolutePath, 10L, photoId = "photo-1", outputId = "out-a"),
                    ProfileHistoryCandidate("B", b.absolutePath, 20L, photoId = "photo-1", outputId = "out-b"),
                ),
            )

            assertEquals(listOf("out-b", "out-a"), results.map { it.outputId })
            assertEquals(listOf("photo-1", "photo-1"), results.map { it.photoId })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun emptyLocalLibraryProducesNoFabricatedHistory() {
        assertEquals(emptyList<ProfileResult>(), recentProfileResults(emptyList()))
    }

    @Test
    fun omitsSavedResultsWhoseOutputFileIsMissing() {
        val readableOutput = Files.createTempFile("profile-result", ".png").toFile()
        try {
            val results = recentProfileResults(
                listOf(
                    ProfileHistoryCandidate("Missing result", "does-not-exist.png", 20L),
                    ProfileHistoryCandidate("Saved result", readableOutput.absolutePath, 10L),
                ),
            )

            assertEquals(listOf("Saved result"), results.map { it.title })
        } finally {
            readableOutput.delete()
        }
    }

    @Test
    fun cachedCreditsAreIdentifiedAsStaleInsteadOfCurrent() {
        val balance = profileBalancePresentation(
            serverConfirmedBalance = null,
            cachedBalance = 17,
            refreshFailed = true,
        )

        assertEquals(17, balance.credits)
        assertEquals(ProfileBalanceStatus.CACHED, balance.status)
    }

    @Test
    fun missingCreditsDoNotBecomeAZeroBalance() {
        val balance = profileBalancePresentation(
            serverConfirmedBalance = null,
            cachedBalance = null,
            refreshFailed = true,
        )

        assertEquals(null, balance.credits)
        assertEquals(ProfileBalanceStatus.ERROR, balance.status)
    }

    @Test
    fun noSessionDoesNotPretendThatAnUnavailableBalanceIsZero() {
        val balance = profileBalancePresentation(
            serverConfirmedBalance = null,
            cachedBalance = null,
        )

        assertEquals(null, balance.credits)
        assertEquals(ProfileBalanceStatus.UNAVAILABLE, balance.status)
    }

    @Test
    fun aServerConfirmedZeroIsStillAValidCurrentBalance() {
        val balance = profileBalancePresentation(
            serverConfirmedBalance = 0,
            cachedBalance = 17,
        )

        assertEquals(0, balance.credits)
        assertEquals(ProfileBalanceStatus.CURRENT, balance.status)
    }
}
