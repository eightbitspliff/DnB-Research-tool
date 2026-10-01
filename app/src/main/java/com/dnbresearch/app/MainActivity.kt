package com.dnbresearch.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.dnbresearch.app.data.Track
import com.dnbresearch.app.ui.DnbTheme
import com.dnbresearch.app.ui.MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DnbTheme {
                MainScreen(viewModel = viewModel, onOpenTrack = ::openInYouTubeMusic)
            }
        }
    }

    private fun openInYouTubeMusic(track: Track) {
        val uri = Uri.parse(track.youTubeMusicUrl)
        val ytMusic = Intent(Intent.ACTION_VIEW, uri).setPackage(YT_MUSIC_PACKAGE)
        try {
            startActivity(ytMusic)
        } catch (e: ActivityNotFoundException) {
            // YouTube Music nicht installiert -> im Browser öffnen.
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private companion object {
        const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    }
}
