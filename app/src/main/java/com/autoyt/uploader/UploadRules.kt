package com.autoyt.uploader

import java.time.Instant
import java.time.ZoneId

/** Pure decision logic, kept free of Android types so it can be unit tested. */
object UploadRules {
    const val CHUNK_BYTES = 8 * 1024 * 1024
    const val SETTLE_MILLIS = 30_000L
    private const val MAX_TITLE_CHARS = 100
    private val QUOTA_ZONE = ZoneId.of("America/Los_Angeles")

    /** Filename without extension, cleaned up to satisfy YouTube's title rules. */
    fun title(displayName: String): String {
        val cleaned = displayName.substringBeforeLast('.').replace(Regex("[<>]"), "").trim()
        return cleaned.ifEmpty { "Untitled video" }.take(MAX_TITLE_CHARS)
    }

    fun allowedOnNetwork(sizeBytes: Long, unmetered: Boolean, mobileLimitMb: Int): Boolean =
        unmetered || sizeBytes <= mobileLimitMb * 1024L * 1024L

    /** A just-written file may still be growing, so wait for it to settle. */
    fun isSettled(modifiedMillis: Long, nowMillis: Long): Boolean =
        nowMillis - modifiedMillis >= SETTLE_MILLIS

    /** Next byte to send, given a resumable upload `Range: bytes=0-N` header. */
    fun nextOffset(rangeHeader: String?): Long =
        rangeHeader?.substringAfterLast('-')?.trim()?.toLongOrNull()?.plus(1) ?: 0L

    fun contentRange(offset: Long, chunkBytes: Int, total: Long): String =
        "bytes $offset-${offset + chunkBytes - 1}/$total"

    fun isQuotaError(body: String): Boolean =
        listOf("quotaExceeded", "uploadLimitExceeded", "dailyLimitExceeded").any(body::contains)

    /** YouTube quotas reset at midnight Pacific time. */
    fun nextQuotaReset(nowMillis: Long): Long =
        Instant.ofEpochMilli(nowMillis).atZone(QUOTA_ZONE).toLocalDate().plusDays(1)
            .atStartOfDay(QUOTA_ZONE).toInstant().toEpochMilli()
}
