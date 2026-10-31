package com.autoyt.uploader

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

object YouTubeAuth {
    private const val UPLOAD_SCOPE = "https://www.googleapis.com/auth/youtube.upload"

    /** Play services identifies this app by package name and signing certificate, so no secret is stored. */
    suspend fun authorize(context: Context): AuthorizationResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(UPLOAD_SCOPE)))
            .build()
        return Identity.getAuthorizationClient(context).authorize(request).await()
    }

    /** Returns a token, or null when the user has to approve access in the app again. */
    suspend fun tokenOrNull(context: Context): String? {
        val result = authorize(context)
        return if (result.hasResolution()) null else result.accessToken
    }

    fun clearToken(context: Context, token: String) = GoogleAuthUtil.clearToken(context, token)
}
