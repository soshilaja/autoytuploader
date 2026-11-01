package com.autoyt.uploader

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val QUEUE_PREVIEW = 3
private const val ACTIVITY_PREVIEW = 3
private val TWO_COLUMN_WIDTH = 720.dp

@Composable
fun OverviewScreen(
    dashboard: Dashboard,
    folders: List<Album>,
    paused: Set<String>,
    signInError: String?,
    onSignIn: () -> Unit,
    onAllowStorage: () -> Unit,
    onAllowBackground: () -> Unit,
    onUploadNow: () -> Unit,
    onSeeUploads: () -> Unit,
    onManageFolders: () -> Unit,
) {
    val status: @Composable ColumnScope.() -> Unit = {
        StatusCard(dashboard)
        PrimaryButton("Upload now and retry failures", Modifier.padding(top = 12.dp), onClick = onUploadNow)
        ActiveSection(dashboard, onSeeUploads)
    }
    val summary: @Composable ColumnScope.() -> Unit = {
        FolderSection(folders, paused, onManageFolders)
        ActivitySection(dashboard, onSeeUploads)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= TWO_COLUMN_WIDTH
        ScreenColumn(maxWidth = if (wide) 1000.dp else 640.dp) {
            PageHeading("Today's queue", "Looking tidy.", overviewLine(dashboard, folders.isNotEmpty()))
            SetupCard(dashboard, signInError, onSignIn, onAllowStorage, onAllowBackground)
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), content = status)
                    Column(Modifier.weight(1f), content = summary)
                }
            } else {
                status()
                summary()
            }
        }
    }
}

private fun overviewLine(dashboard: Dashboard, hasFolders: Boolean): String {
    val count = UploadRules.videoCount(dashboard.queue.size)
    return when {
        !hasFolders -> "Add a folder and the app takes it from there."
        dashboard.queue.isEmpty() -> "Nothing waiting. Your camera roll can breathe."
        dashboard.isUploading -> "$count moving quietly in the background."
        else -> "$count waiting for their turn."
    }
}

/** Only appears while something needs the user, and goes once it is sorted. */
@Composable
private fun SetupCard(
    dashboard: Dashboard,
    signInError: String?,
    onSignIn: () -> Unit,
    onAllowStorage: () -> Unit,
    onAllowBackground: () -> Unit,
) {
    if (!dashboard.needsSignIn && dashboard.hasStorage && dashboard.unrestricted && signInError == null) return
    AppCard(Modifier.padding(bottom = 12.dp)) {
        Text("A few things to set up", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (dashboard.needsSignIn) PrimaryButton("Sign in to YouTube", onClick = onSignIn)
            signInError?.let {
                Text(
                    it,
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!dashboard.hasStorage) SecondaryButton("Allow access to videos", onClick = onAllowStorage)
            if (!dashboard.unrestricted) SecondaryButton("Allow running in the background", onClick = onAllowBackground)
        }
    }
}

@Composable
private fun StatusCard(dashboard: Dashboard) {
    val colors = AutoTheme.colors
    val (title, detail) = when {
        dashboard.dailyLimitHit -> "Paused for today" to "YouTube's daily limit was reached. Uploads resume after midnight Pacific time."
        dashboard.isUploading -> "Private uploads in progress" to "Your selected videos are going to your own channel."
        else -> "All quiet for now" to "New videos go privately to your own channel."
    }
    AppCard(background = colors.deep) {
        Text(
            title,
            Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
            color = colors.onDeep,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(detail, Modifier.padding(top = 6.dp), color = colors.onDeepSoft, style = MaterialTheme.typography.bodySmall)
        Row(
            Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(dashboard.uploadedThisWeek.toString(), color = colors.onDeep, fontSize = 38.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
            Text("uploaded in the last 7 days", Modifier.padding(bottom = 4.dp), color = colors.onDeepSoft, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ActiveSection(dashboard: Dashboard, onSeeUploads: () -> Unit) {
    val extra = dashboard.queue.size > QUEUE_PREVIEW
    ExpandableSection(
        "Active now",
        action = if (extra) "See all" else null,
        actionDescription = "See all active uploads",
        onAction = onSeeUploads,
    ) {
        if (dashboard.queue.isEmpty()) {
            Text("Nothing waiting.", color = AutoTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
        }
        dashboard.queue.take(QUEUE_PREVIEW).forEach { QueueRow(it) }
    }
}

@Composable
private fun FolderSection(folders: List<Album>, paused: Set<String>, onManage: () -> Unit) {
    val colors = AutoTheme.colors
    val active = folders.filter { it.id !in paused }
    ExpandableSection("Folders", action = "Manage", actionDescription = "Manage folders", onAction = onManage) {
        AppCard(background = colors.mint) {
            Text(
                if (active.size == 1) "1 folder active" else "${active.size} folders active",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                folderSummary(active),
                color = colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun folderSummary(active: List<Album>): String = when {
    active.isEmpty() -> "None yet"
    active.size <= 2 -> active.joinToString(" and ") { it.name }
    else -> "${active[0].name}, ${active[1].name} and ${active.size - 2} more"
}

@Composable
private fun ActivitySection(dashboard: Dashboard, onSeeUploads: () -> Unit) {
    ExpandableSection(
        "Recent activity",
        action = "See all",
        actionDescription = "See all recent activity",
        onAction = onSeeUploads,
    ) {
        if (dashboard.history.isEmpty()) {
            Text("Nothing uploaded yet.", color = AutoTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
        }
        dashboard.history.take(ACTIVITY_PREVIEW).forEach { record ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    VideoThumb(record.mediaId)
                    Column(Modifier.weight(1f)) {
                        Text(
                            record.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        RecordState(record)
                    }
                }
            }
        }
    }
}

fun UploadRecord.stateLabel(): String = when {
    status == UploadStatus.FAILED -> "Failed · retry available"
    message == null -> "Done · moved to Uploaded"
    else -> "Done"
}

@Composable
fun RecordState(record: UploadRecord) {
    val colors = AutoTheme.colors
    Text(
        record.stateLabel(),
        color = if (record.status == UploadStatus.FAILED) colors.rustDark else colors.green,
        style = MaterialTheme.typography.labelSmall,
    )
}

fun relativeTime(millis: Long): CharSequence =
    DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
