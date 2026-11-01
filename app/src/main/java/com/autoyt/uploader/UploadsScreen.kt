package com.autoyt.uploader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Plenty to scan, without building a row for every video in a very long queue. */
private const val MAX_QUEUE_ROWS = 50

private enum class UploadFilter(val label: String) {
    ALL("All"), UPLOADING("Uploading"), DONE("Done"), FAILED("Failed")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UploadsScreen(dashboard: Dashboard, onRetry: (Long) -> Unit) {
    var filter by rememberSaveable { mutableStateOf(UploadFilter.ALL) }
    val colors = AutoTheme.colors
    val queue = if (filter == UploadFilter.ALL || filter == UploadFilter.UPLOADING) dashboard.queue else emptyList()
    val history = dashboard.history.filter {
        when (filter) {
            UploadFilter.ALL -> true
            UploadFilter.UPLOADING -> false
            UploadFilter.DONE -> it.status == UploadStatus.DONE
            UploadFilter.FAILED -> it.status == UploadStatus.FAILED
        }
    }

    ScreenColumn {
        PageHeading("Recent activity", "The paper trail.", "Every video gets a clear finish line.")
        FlowRow(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UploadFilter.entries.forEach { option ->
                val selected = option == filter
                FilterChip(
                    selected = selected,
                    onClick = { filter = option },
                    label = { Text(option.label) },
                    modifier = Modifier.focusRing(),
                    // The tick means the selected filter isn't shown by colour alone.
                    leadingIcon = if (selected) {
                        { Icon(painterResource(R.drawable.ic_check), contentDescription = null, Modifier.size(18.dp)) }
                    } else {
                        null
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.rust,
                        selectedLabelColor = colors.onRust,
                        selectedLeadingIconColor = colors.onRust,
                        labelColor = colors.ink,
                    ),
                )
            }
        }
        Text(
            "Showing ${UploadRules.videoCount(queue.size + history.size)}",
            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = colors.muted,
            style = MaterialTheme.typography.bodySmall,
        )

        if (queue.isEmpty() && history.isEmpty()) {
            Text(emptyMessage(filter), Modifier.padding(top = 12.dp), color = colors.muted, style = MaterialTheme.typography.bodyMedium)
        }
        if (queue.isNotEmpty()) {
            ExpandableSection("In progress", count = queue.size) {
                queue.take(MAX_QUEUE_ROWS).forEach { QueueRow(it) }
                if (queue.size > MAX_QUEUE_ROWS) {
                    Text(
                        "and ${UploadRules.videoCount(queue.size - MAX_QUEUE_ROWS)} more in the queue",
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (history.isNotEmpty()) {
            ExpandableSection("Upload history", count = history.size) {
                history.forEach { record -> HistoryRow(record, onRetry) }
            }
        }
    }
}

private fun emptyMessage(filter: UploadFilter): String = when (filter) {
    UploadFilter.ALL -> "Nothing uploaded yet."
    UploadFilter.UPLOADING -> "Nothing waiting. Your camera roll can breathe."
    UploadFilter.DONE -> "Nothing finished yet."
    UploadFilter.FAILED -> "No failed uploads."
}

@Composable
private fun HistoryRow(record: UploadRecord, onRetry: (Long) -> Unit) {
    val colors = AutoTheme.colors
    val failed = record.status == UploadStatus.FAILED
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VideoThumb(record.mediaId)
            Column(Modifier.weight(1f)) {
                Text(
                    record.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    listOfNotNull(relativeTime(record.updatedAt), record.albumName).joinToString(" · "),
                    color = colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                RecordState(record)
            }
        }
        record.message?.let {
            Text(
                it,
                Modifier.padding(top = 8.dp),
                color = colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (failed) {
            Text(
                "This one needs another try.",
                Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
            )
            PrimaryButton("Retry upload", Modifier.padding(top = 8.dp), description = "Retry upload of ${record.name}") {
                onRetry(record.mediaId)
            }
        }
    }
}
