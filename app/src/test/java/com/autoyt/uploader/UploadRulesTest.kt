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

    @Test fun quotaResetsAtPacificMidnight() {
        val now = Instant.parse("2026-10-31T20:00:00Z").toEpochMilli()
        assertEquals(Instant.parse("2026-11-01T07:00:00Z").toEpochMilli(), UploadRules.nextQuotaReset(now))
    }
}
