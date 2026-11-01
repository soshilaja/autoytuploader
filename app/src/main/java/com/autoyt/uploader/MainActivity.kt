package com.autoyt.uploader

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AutoTheme { Screen() } }
    }
}

private enum class Tab(val label: String, @DrawableRes val icon: Int) {
    OVERVIEW("Overview", R.drawable.ic_home),
    FOLDERS("Folders", R.drawable.ic_folder),
    UPLOADS("Uploads", R.drawable.ic_upload),
    SETTINGS("Settings", R.drawable.ic_settings),
}

@Composable
private fun Screen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { Settings(context) }
    val db = remember { UploadDb(context) }
    val loader = remember { DashboardLoader(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var tab by rememberSaveable { mutableStateOf(Tab.OVERVIEW) }
    val tabState = rememberSaveableStateHolder()
    var dashboard by remember { mutableStateOf(Dashboard()) }
    var refreshes by remember { mutableIntStateOf(0) }
    var signInError by remember { mutableStateOf<String?>(null) }

    // Polling also picks up changes made on system settings screens when the user returns.
    LaunchedEffect(refreshes) {
        while (true) {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                dashboard = withContext(Dispatchers.IO) { loader.load() }
            }
            delay(2000)
        }
    }

    val startUploads: () -> Unit = {
        Scheduler.uploadNow(context)
        refreshes++
    }
    val onSignedIn = {
        settings.needsSignIn = false
        signInError = null
        startUploads()
    }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        scope.launch {
            if (runCatching { YouTubeAuth.tokenOrNull(context) }.getOrNull() != null) onSignedIn()
            else signInError = "Sign-in was not completed."
        }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }

    val signIn: () -> Unit = {
        scope.launch {
            try {
                val result = YouTubeAuth.authorize(context)
                val pending = result.pendingIntent
                if (result.hasResolution() && pending != null) {
                    consent.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                } else {
                    onSignedIn()
                }
            } catch (e: Exception) {
                signInError = "Sign-in failed: ${e.message}"
            }
        }
    }
    val openSystem = { action: String ->
        context.startActivity(Intent(action, Uri.parse("package:${context.packageName}")))
    }

    Scaffold(
        containerColor = AutoTheme.colors.paper,
        bottomBar = { BottomBar(tab) { tab = it } },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()
                .semantics { paneTitle = tab.label },
        ) {
            TopBar(tab)
            tabState.SaveableStateProvider(tab.name) {
                when (tab) {
                    Tab.OVERVIEW -> OverviewScreen(
                        dashboard = dashboard,
                        folders = settings.albums,
                        paused = settings.pausedAlbums,
                        signInError = signInError,
                        onSignIn = signIn,
                        onAllowStorage = { openSystem(AndroidSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION) },
                        onAllowBackground = { openSystem(AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) },
                        onUploadNow = {
                            db.retryFailed()
                            settings.pausedUntil = 0
                            startUploads()
                        },
                        onSeeUploads = { tab = Tab.UPLOADS },
                        onManageFolders = { tab = Tab.FOLDERS },
                    )
                    Tab.FOLDERS -> FoldersScreen(dashboard, settings, onChanged = startUploads)
                    Tab.UPLOADS -> UploadsScreen(dashboard, onRetry = { mediaId ->
                        db.retry(mediaId)
                        startUploads()
                    })
                    Tab.SETTINGS -> SettingsScreen(dashboard, settings, signInError, onSignIn = signIn, onChanged = startUploads)
                }
            }
        }
    }
}

@Composable
private fun TopBar(tab: Tab) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Image(painterResource(R.drawable.ic_logo), contentDescription = null, Modifier.size(28.dp))
        Text(if (tab == Tab.OVERVIEW) "Auto YT" else tab.label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text("private", color = AutoTheme.colors.green, style = MaterialTheme.typography.labelSmall)
    }
}

/** Hand-built because Material's equal slots split labels mid-word at 200% text size. */
@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val colors = AutoTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.cream).navigationBarsPadding().padding(vertical = 6.dp).selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            Column(
                Modifier.widthIn(min = 64.dp).focusRing()
                    .selectable(isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier.size(width = 56.dp, height = 30.dp)
                        .background(if (isSelected) colors.rust else Color.Transparent, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(tab.icon),
                        contentDescription = null,
                        Modifier.size(24.dp),
                        tint = if (isSelected) colors.onRust else colors.muted,
                    )
                }
                Text(
                    tab.label,
                    color = if (isSelected) colors.rustDark else colors.muted,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    softWrap = false,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
