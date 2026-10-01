package com.dnbresearch.app

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dnbresearch.app.data.PlaylistCreator
import com.dnbresearch.app.data.PlaylistResult
import com.dnbresearch.app.data.SearchOptions
import com.dnbresearch.app.data.SettingsStore
import com.dnbresearch.app.data.Track
import com.dnbresearch.app.data.YouTubeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import java.security.MessageDigest

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Success(val tracks: List<Track>) : SearchState
    data class Error(val message: String) : SearchState
}

sealed interface PlaylistState {
    data object Idle : PlaylistState
    data object SigningIn : PlaylistState
    data class Creating(val done: Int, val total: Int) : PlaylistState
    data class Done(val result: PlaylistResult) : PlaylistState
    data class Error(val message: String) : PlaylistState
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsStore(app)

    var state by mutableStateOf<SearchState>(SearchState.Idle)
        private set
    var apiKey by mutableStateOf(settings.apiKey)
        private set
    var days by mutableStateOf(settings.days)
        private set
    var officialOnly by mutableStateOf(settings.officialOnly)
        private set

    var playlistState by mutableStateOf<PlaylistState>(PlaylistState.Idle)
        private set

    private var searchJob: Job? = null

    fun updateApiKey(value: String) {
        settings.apiKey = value
        apiKey = settings.apiKey
    }

    fun updateDays(value: Long) {
        settings.days = value
        days = value
    }

    fun updateOfficialOnly(value: Boolean) {
        settings.officialOnly = value
        officialOnly = value
    }

    fun search() {
        if (apiKey.isBlank()) {
            state = SearchState.Error("Bitte zuerst einen YouTube-API-Key in den Einstellungen (Zahnrad oben rechts) eintragen.")
            return
        }
        searchJob?.cancel()
        state = SearchState.Loading
        playlistState = PlaylistState.Idle
        searchJob = viewModelScope.launch {
            state = try {
                val app = getApplication<Application>()
                val repo = YouTubeRepository(apiKey, app.packageName, signingCertSha1(app))
                SearchState.Success(repo.findNewTracks(SearchOptions(days, officialOnly)))
            } catch (e: IOException) {
                SearchState.Error(e.message ?: "Netzwerkfehler – bist du online?")
            } catch (e: org.json.JSONException) {
                SearchState.Error("Unerwartete Antwort von YouTube: ${e.message}")
            }
        }
    }

    fun onPlaylistSignInStarted() {
        playlistState = PlaylistState.SigningIn
    }

    fun onPlaylistSignInFailed(message: String) {
        playlistState = PlaylistState.Error(message)
    }

    fun dismissPlaylistResult() {
        playlistState = PlaylistState.Idle
    }

    /** Legt mit dem OAuth-Token eine private Playlist mit allen gefundenen Tracks an. */
    fun createPlaylist(accessToken: String) {
        val tracks = (state as? SearchState.Success)?.tracks.orEmpty()
        if (tracks.isEmpty()) {
            playlistState = PlaylistState.Idle
            return
        }
        playlistState = PlaylistState.Creating(0, tracks.size)
        viewModelScope.launch {
            playlistState = try {
                val result = PlaylistCreator(accessToken).create(tracks, days) { done, total ->
                    playlistState = PlaylistState.Creating(done, total)
                }
                PlaylistState.Done(result)
            } catch (e: IOException) {
                PlaylistState.Error(e.message ?: "Netzwerkfehler – bist du online?")
            } catch (e: org.json.JSONException) {
                PlaylistState.Error("Unerwartete Antwort von YouTube: ${e.message}")
            }
        }
    }

    /** SHA-1 des Signaturzertifikats, falls der API-Key auf diese Android-App beschränkt ist. */
    private fun signingCertSha1(app: Application): String? = runCatching {
        val pm = app.packageManager
        val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES).signatures?.firstOrNull()
        } ?: return null
        MessageDigest.getInstance("SHA-1").digest(signature.toByteArray())
            .joinToString("") { "%02X".format(it) }
    }.getOrNull()
}
