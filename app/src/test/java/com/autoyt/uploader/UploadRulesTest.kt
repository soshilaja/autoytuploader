package com.autoyt.uploader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class UploadRulesTest {
    @Test fun titleDropsExtensionOnly() {
        assertEquals("VID_20261031_143200", UploadRules.title("VID_20261031_143200.mp4"))
        assertEquals("my.trip", UploadRules.title("my.trip.mov"))
    }

    @Test fun titleObeysYouTubeRules() {
        assertEquals("ab", UploadRules.title("<a>b.mp4"))
        assertEquals(100, UploadRules.title("x".repeat(150) + ".mp4").length)
        assertEquals("Untitled video", UploadRules.title(".mp4"))
    }

    @Test fun mobileDataOnlyCarriesSmallVideos() {
        val mb = 1024L * 1024L
        assertTrue(UploadRules.allowedOnNetwork(5000 * mb, unmetered = true, mobileLimitMb = 100))
        assertTrue(UploadRules.allowedOnNetwork(100 * mb, unmetered = false, mobileLimitMb = 100))
        assertFalse(UploadRules.allowedOnNetwork(100 * mb + 1, unmetered = false, mobileLimitMb = 100))
    }

    @Test fun wifiOnlyBlocksMobileButKeepsTheSavedLimit() {
        assertEquals(0, UploadRules.mobileLimit(wifiOnly = true, limitMb = 100))
        assertEquals(100, UploadRules.mobileLimit(wifiOnly = false, limitMb = 100))
        assertFalse(UploadRules.allowedOnNetwork(1, unmetered = false, UploadRules.mobileLimit(true, 100)))
        assertTrue(UploadRules.allowedOnNetwork(1, unmetered = true, UploadRules.mobileLimit(true, 100)))
    }

    @Test fun queueStateShowsTheMostSpecificReason() {
        fun state(active: Boolean = false, limit: Boolean = false, settled: Boolean = true, allowed: Boolean = true) =
            UploadRules.queueState(active, limit, settled, allowed)
        assertEquals(QueueState.QUEUED, state())
        assertEquals(QueueState.NEEDS_WIFI, state(allowed = false))
        assertEquals(QueueState.SETTLING, state(settled = false, allowed = false))
        assertEquals(QueueState.DAILY_LIMIT, state(limit = true, settled = false))
        assertEquals(QueueState.UPLOADING, state(active = true, limit = true, allowed = false))
    }

    @Test fun videoCountReadsNaturally() {
        assertEquals("1 video", UploadRules.videoCount(1))
        assertEquals("3 videos", UploadRules.videoCount(3))
    }

    @Test fun rangeHeaderGivesNextByte() {
        assertEquals(8388608L, UploadRules.nextOffset("bytes=0-8388607"))
        assertEquals(0L, UploadRules.nextOffset(null))
    }

    @Test fun contentRangeIsInclusive() {
        assertEquals("bytes 8-17/100", UploadRules.contentRange(8, 10, 100))
    }

    @Test fun chunkSizeIsMultipleOfRequiredBlock() {
        assertEquals(0, UploadRules.CHUNK_BYTES % (256 * 1024))
    }

    @Test fun recentFilesAreNotSettled() {
        assertFalse(UploadRules.isSettled(1000, 1000 + UploadRules.SETTLE_MILLIS - 1))
        assertTrue(UploadRules.isSettled(1000, 1000 + UploadRules.SETTLE_MILLIS))
    }

    @Test fun quotaErrorsAreRecognised() {
        assertTrue(UploadRules.isQuotaError("""{"error":{"errors":[{"reason":"quotaExceeded"}]}}"""))
        assertFalse(UploadRules.isQuotaError("""{"error":{"errors":[{"reason":"forbidden"}]}}"""))
    }

    @Test fun foldersMoveUpAndDown() {
        assertEquals(listOf("b", "a", "c"), UploadRules.reordered(listOf("a", "b", "c"), 1, -1))
        assertEquals(listOf("a", "c", "b"), UploadRules.reordered(listOf("a", "b", "c"), 1, 1))
    }

    @Test fun foldersStayPutAtEitherEnd() {
        assertEquals(listOf("a", "b"), UploadRules.reordered(listOf("a", "b"), 0, -1))
        assertEquals(listOf("a", "b"), UploadRules.reordered(listOf("a", "b"), 1, 1))
    }

    @Test fun quotaResetsAtPacificMidnight() {
        val now = Instant.parse("2026-10-31T20:00:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-11-01T07:00:00Z").toEpochMilli(), UploadRules.nextQuotaReset(now))
    }
}
