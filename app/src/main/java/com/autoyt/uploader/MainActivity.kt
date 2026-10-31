package com.autoyt.uploader

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) { Screen() } } }
    }
}

@Composable
private fun Screen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { Settings(context) }
    val db = remember { UploadDb(context) }
    val library = remember { VideoLibrary(context) }

    var tick by remember { mutableIntStateOf(0) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var limit by remember { mutableStateOf(settings.mobileLimitMb.toString()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            tick++
        }
    }

    val onSignedIn = {
        settings.needsSignIn = false
        signInError = null
        Scheduler.uploadNow(context)
        tick++
    }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        scope.launch {
            if (runCatching { YouTubeAuth.tokenOrNull(context) }.getOrNull() != null) onSignedIn()
            else signInError = "Sign-in was not completed."
        }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }

    // Read on each tick so returning from a system settings screen updates the UI.
    val records = remember(tick) { db.recent() }
    val hasStorage = remember(tick) { Environment.isExternalStorageManager() }
    val unrestricted = remember(tick) {
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }
    val albums = remember(hasStorage, menuOpen) { if (hasStorage) library.albums() else emptyList() }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Auto YT Uploader", style = MaterialTheme.typography.headlineSmall)

        if (settings.needsSignIn) {
            Button(onClick = {
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
            }) { Text("Sign in to YouTube") }
        } else {
            Text("Signed in to YouTube")
        }
        signInError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        if (!hasStorage) {
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(
                        AndroidSettings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    )
                )
            }) { Text("Allow access to videos") }
        }
        if (!unrestricted) {
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(
                        AndroidSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${context.packageName}"),
                    )
                )
            }) { Text("Allow running in the background") }
        }

        Text("Album to upload from: ${settings.albumName ?: "none chosen"}")
        Column {
            OutlinedButton(enabled = hasStorage, onClick = { menuOpen = true }) { Text("Choose album") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                albums.forEach { album ->
                    DropdownMenuItem(text = { Text(album.name) }, onClick = {
                        settings.albumId = album.id
                        settings.albumName = album.name
                        menuOpen = false
                        Scheduler.uploadNow(context)
                    })
                }
            }
        }

        OutlinedTextField(
            value = limit,
            onValueChange = { value ->
                limit = value.filter(Char::isDigit)
                limit.toIntOrNull()?.let { settings.mobileLimitMb = it }
            },
            label = { Text("Largest video on mobile data (MB)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(onClick = { db.retryFailed(); settings.pausedUntil = 0; Scheduler.uploadNow(context) }) {
            Text("Upload now and retry failures")
        }

        HorizontalDivider()
        Text("Recent uploads", style = MaterialTheme.typography.titleMedium)
        if (records.isEmpty()) Text("Nothing uploaded yet.")
        records.forEach { record ->
            Column {
                Text(record.name)
                Text(
                    listOfNotNull(record.status.name.lowercase(), record.message).joinToString(": "),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
