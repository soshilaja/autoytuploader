package com.autoyt.uploader

import android.util.LruCache
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val CardShape = RoundedCornerShape(12.dp)

/** Compose's own focus highlight is too faint to meet WCAG 2.4.7 for keyboard users. */
fun Modifier.focusRing(shape: Shape = RoundedCornerShape(10.dp)): Modifier = composed {
    var focused by remember { mutableStateOf(false) }
    val color = AutoTheme.colors.ink
    onFocusChanged { focused = it.hasFocus }.then(if (focused) Modifier.border(2.dp, color, shape) else Modifier)
}

/** The scrolling body of a screen, kept to a readable width on large screens. */
@Composable
fun ScreenColumn(maxWidth: Dp = 640.dp, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = maxWidth).fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
fun PageHeading(overline: String, title: String, subtitle: String) {
    Overline(overline)
    Text(
        title,
        Modifier.padding(top = 6.dp).semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
    )
    Text(
        subtitle,
        Modifier.padding(top = 4.dp, bottom = 18.dp),
        color = AutoTheme.colors.muted,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun Overline(text: String, color: Color = AutoTheme.colors.green) {
    Text(text.uppercase(), color = color, fontFamily = DmMono, fontSize = 11.sp, letterSpacing = 0.12.em)
}

@Composable
fun AppCard(modifier: Modifier = Modifier, background: Color = AutoTheme.colors.cream, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(CardShape).background(background).border(1.dp, AutoTheme.colors.line, CardShape).padding(14.dp),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    container: Color = AutoTheme.colors.rust,
    content: Color = AutoTheme.colors.onRust,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).focusRing()
            .semantics { description?.let { contentDescription = it } },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) { Text(text) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).focusRing(),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AutoTheme.colors.rustDark),
    ) { Text(text) }
}

@Composable
fun SwitchRow(title: String, detail: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AutoTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).focusRing()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(detail, color = colors.muted, style = MaterialTheme.typography.bodySmall)
        }
        AppSwitch(checked, onChange = null)
    }
}

@Composable
fun AppSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val colors = AutoTheme.colors
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = colors.green,
            checkedThumbColor = colors.cream,
            uncheckedTrackColor = colors.paper,
            uncheckedBorderColor = colors.muted,
            uncheckedThumbColor = colors.muted,
        ),
    )
}

/**
 * Open or closed is saved under [title], so it survives moving between tabs. Collapsing also gives
 * a way to hide live upload progress, which WCAG 2.2.2 asks for.
 */
@Composable
fun ExpandableSection(
    title: String,
    count: Int? = null,
    action: String? = null,
    actionDescription: String? = null,
    onAction: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(true) }
    val colors = AutoTheme.colors
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f).heightIn(min = 48.dp).focusRing()
                .clickable(onClickLabel = if (open) "Collapse" else "Expand", role = Role.Button) { open = !open }
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
            )
            if (count != null) {
                Text(" · $count", color = colors.muted, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                painterResource(R.drawable.ic_expand),
                contentDescription = null,
                Modifier.padding(start = 4.dp).size(20.dp).rotate(if (open) 180f else 0f),
                tint = colors.muted,
            )
        }
        if (action != null) {
            TextButton(
                onClick = onAction,
                modifier = Modifier.focusRing().semantics { actionDescription?.let { contentDescription = it } },
            ) { Text(action, color = colors.rustDark) }
        }
    }
    AnimatedVisibility(open) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

private val thumbnails = LruCache<Long, ImageBitmap>(64)

/** A phone-generated preview of the local video, or a plain tile when there is none. */
@Composable
fun VideoThumb(mediaId: Long, size: Dp = 42.dp) {
    val context = LocalContext.current
    val image by produceState(thumbnails.get(mediaId), mediaId) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { VideoLibrary(context).thumbnail(mediaId)?.asImageBitmap() }
            value?.let { thumbnails.put(mediaId, it) }
        }
    }
    val shape = RoundedCornerShape(8.dp)
    Box(
        Modifier.size(size).clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF96AD9E), Color(0xFFD4B984)))),
        contentAlignment = Alignment.Center,
    ) {
        image?.let {
            Image(it, contentDescription = null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } ?: Icon(painterResource(R.drawable.ic_play), contentDescription = null, Modifier.size(18.dp), tint = Color(0xFFFFFAF0))
    }
}

@Composable
fun UploadProgressBar(percent: Int, modifier: Modifier = Modifier) {
    val colors = AutoTheme.colors
    LinearProgressIndicator(
        progress = { percent / 100f },
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            .semantics { contentDescription = "Upload progress" },
        color = colors.green,
        trackColor = colors.mint,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

fun QueueState.label(percent: Int): String = when (this) {
    QueueState.UPLOADING -> "Uploading · $percent%"
    QueueState.QUEUED -> "Queued"
    QueueState.SETTLING -> "Queued · still saving"
    QueueState.NEEDS_WIFI -> "Paused · waiting for Wi-Fi"
    QueueState.DAILY_LIMIT -> "Paused · daily limit"
}

/** One queued video, shared by the Overview and Uploads screens. */
@Composable
fun QueueRow(item: QueuedVideo) {
    val colors = AutoTheme.colors
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VideoThumb(item.video.id)
            Column(Modifier.weight(1f)) {
                Text(
                    item.video.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(item.video.albumName, color = colors.muted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.padding(top = 2.dp))
                Text(
                    item.state.label(item.percent),
                    color = if (item.state == QueueState.UPLOADING) colors.green else colors.muted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (item.state == QueueState.UPLOADING) UploadProgressBar(item.percent, Modifier.padding(top = 10.dp))
    }
}
