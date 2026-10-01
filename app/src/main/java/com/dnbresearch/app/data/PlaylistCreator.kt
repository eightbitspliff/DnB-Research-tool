package com.dnbresearch.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class PlaylistResult(val playlistId: String, val added: Int, val failed: Int) {
    val youTubeMusicUrl: String get() = "https://music.youtube.com/playlist?list=$playlistId"
}

/**
 * Legt im YouTube-Konto des Nutzers eine private Playlist an und fügt die Tracks hinzu.
 * YouTube-Playlists erscheinen automatisch auch in der YouTube-Music-Mediathek.
 */
class PlaylistCreator(private val accessToken: String) {

    suspend fun create(
        tracks: List<Track>,
        days: Long,
        onProgress: (done: Int, total: Int) -> Unit,
    ): PlaylistResult {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val playlistId = post(
            "playlists?part=snippet,status",
            JSONObject()
                .put(
                    "snippet",
                    JSONObject()
                        .put("title", "DnB Radar · $today")
                        .put("description", "Neue Drum & Bass Releases der letzten $days Tage – erstellt mit DnB Radar."),
                )
                .put("status", JSONObject().put("privacyStatus", "private")),
        ).getString("id")

        var added = 0
        var failed = 0
        // Nacheinander einfügen: parallele Inserts in dieselbe Playlist liefern bei YouTube 409-Konflikte.
        tracks.forEachIndexed { index, track ->
            try {
                post(
                    "playlistItems?part=snippet",
                    JSONObject().put(
                        "snippet",
                        JSONObject()
                            .put("playlistId", playlistId)
                            .put("resourceId", JSONObject().put("kind", "youtube#video").put("videoId", track.videoId)),
                    ),
                )
                added++
            } catch (e: YouTubeApiException) {
                // Kontingent leer -> abbrechen; einzelne nicht einfügbare Videos (z.B. gesperrt) überspringen.
                if (e.reason == "quotaExceeded") {
                    failed += tracks.size - index
                    onProgress(tracks.size, tracks.size)
                    return PlaylistResult(playlistId, added, failed)
                }
                failed++
            }
            onProgress(index + 1, tracks.size)
        }
        return PlaylistResult(playlistId, added, failed)
    }

    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val conn = (URL("https://www.googleapis.com/youtube/v3/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw YouTubeApiException.fromResponse(code, text)
            JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
