package com.dnbresearch.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.dnbresearch.app.ui.DnbTheme
import com.dnbresearch.app.ui.MainScreen
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /** Google-Kontoauswahl / Zustimmung für den YouTube-Zugriff. */
    private val authLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        try {
            val auth = Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data)
            onAuthorized(auth)
        } catch (e: ApiException) {
            viewModel.onPlaylistSignInFailed(
                if (e.statusCode == CommonStatusCodes.CANCELED) "Anmeldung abgebrochen." else authErrorMessage(e)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DnbTheme {
                MainScreen(
                    viewModel = viewModel,
                    onOpenUrl = ::openInYouTubeMusic,
                    onCreatePlaylist = ::requestYouTubeAccess,
                )
            }
        }
    }

    private fun requestYouTubeAccess() {
        viewModel.onPlaylistSignInStarted()
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(YOUTUBE_SCOPE)))
            .build()
        lifecycleScope.launch {
            try {
                val auth = Identity.getAuthorizationClient(this@MainActivity).authorize(request).await()
                val pendingIntent = auth.pendingIntent
                if (auth.hasResolution() && pendingIntent != null) {
                    authLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    onAuthorized(auth)
                }
            } catch (e: ApiException) {
                viewModel.onPlaylistSignInFailed(authErrorMessage(e))
            } catch (e: Exception) {
                viewModel.onPlaylistSignInFailed("Google-Anmeldung fehlgeschlagen: ${e.message}")
            }
        }
    }

    private fun onAuthorized(auth: AuthorizationResult) {
        val token = auth.accessToken
        if (token.isNullOrEmpty()) {
            viewModel.onPlaylistSignInFailed("Kein Zugriff auf YouTube erhalten.")
        } else {
            viewModel.createPlaylist(token)
        }
    }

    private fun authErrorMessage(e: ApiException): String =
        if (e.statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
            "Google-Login ist noch nicht eingerichtet: In der Google Cloud Console wird eine OAuth-Client-ID " +
                "vom Typ \"Android\" benötigt (Paket com.dnbresearch.app, SHA-1 siehe README)."
        } else {
            "Google-Anmeldung fehlgeschlagen (Code ${e.statusCode}): ${e.message}"
        }

    private fun openInYouTubeMusic(url: String) {
        val uri = Uri.parse(url)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(YT_MUSIC_PACKAGE))
        } catch (e: ActivityNotFoundException) {
            // YouTube Music nicht installiert -> im Browser öffnen.
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private companion object {
        const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
        const val YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube"
    }
}
