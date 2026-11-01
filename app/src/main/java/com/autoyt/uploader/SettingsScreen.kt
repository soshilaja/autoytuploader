package com.autoyt.uploader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val GOOGLE_PERMISSIONS = "https://myaccount.google.com/permissions"

@Composable
fun SettingsScreen(
    dashboard: Dashboard,
    settings: Settings,
    signInError: String?,
    onSignIn: () -> Unit,
    onChanged: () -> Unit,
) {
    val context = LocalContext.current
    val colors = AutoTheme.colors
    var wifiOnly by remember { mutableStateOf(settings.wifiOnly) }
    var moveAfter by remember { mutableStateOf(settings.moveAfterUpload) }
    var batch by remember { mutableStateOf(settings.batchNotifications) }
    var limit by remember { mutableStateOf(settings.mobileLimitMb.toString()) }

    ScreenColumn {
        PageHeading("Quiet control", "Set the edges.", "The app handles the rest.")

        AppCard(background = colors.deep) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(34.dp).background(colors.cream, CircleShape), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.ic_logo), contentDescription = null, Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        if (dashboard.needsSignIn) "Not signed in" else "Signed in to YouTube",
                        color = colors.onDeep,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text("Uploads go to this account's channel", color = colors.onDeepSoft, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (dashboard.needsSignIn) {
                PrimaryButton(
                    "Sign in to YouTube",
                    Modifier.padding(top = 12.dp),
                    container = colors.onDeep,
                    content = colors.deep,
                    onClick = onSignIn,
                )
            }
            signInError?.let {
                Text(
                    it,
                    Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.onDeepSoft,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        SettingsGroup("Upload behavior") {
            SwitchRow("Wi-Fi only", "Pause uploads on mobile data", wifiOnly) {
                wifiOnly = it
                settings.wifiOnly = it
                onChanged()
            }
            SwitchRow("Move after upload", afterUploadDetail(moveAfter), moveAfter) {
                moveAfter = it
                settings.moveAfterUpload = it
            }
            SwitchRow("Notifications", "Tell me when a batch is done", batch) {
                batch = it
                settings.batchNotifications = it
            }
        }

        SettingsGroup("Limits") {
            val invalid = !wifiOnly && limit.toIntOrNull() == null
            val fieldLabel = "Largest video on mobile data (MB)"
            // Above the field, not floating in its border, so it can wrap at large text sizes.
            Text(
                fieldLabel,
                Modifier.padding(bottom = 6.dp),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedTextField(
                value = limit,
                onValueChange = { value ->
                    limit = value.filter(Char::isDigit)
                    limit.toIntOrNull()?.let {
                        settings.mobileLimitMb = it
                        onChanged()
                    }
                },
                supportingText = {
                    Text(
                        when {
                            wifiOnly -> "Not used while Wi-Fi only is on."
                            invalid -> "Enter a whole number of MB. The app keeps using ${settings.mobileLimitMb} MB until you do."
                            else -> "Larger videos wait for Wi-Fi. 0 means never use mobile data."
                        }
                    )
                },
                isError = invalid,
                enabled = !wifiOnly,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = colors.muted,
                    disabledSupportingTextColor = colors.muted,
                    disabledBorderColor = colors.muted,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = fieldLabel },
            )
        }

        SettingsGroup("Privacy and access") {
            Text(
                "Videos go to your own YouTube channel and are private by default. The app only has permission to upload.",
                color = colors.muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            SecondaryButton("Review permissions", Modifier.padding(top = 10.dp)) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PERMISSIONS)))
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, Modifier.padding(top = 20.dp, bottom = 8.dp).semantics { heading() }, style = MaterialTheme.typography.titleMedium)
    AppCard(content = content)
}
