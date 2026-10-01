package com.dnbresearch.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class YouTubeApiException(message: String) : IOException(message)

data class SearchOptions(
    val days: Long = 14,
    val hideMixes: Boolean = true,
    val officialOnly: Boolean = false,
)

/**
 * Sucht über die offizielle YouTube Data API v3 nach Drum-and-Bass-Tracks
 * der Musik-Kategorie, die in den letzten [SearchOptions.days] Tagen veröffentlicht wurden.
 * Abgespielt werden die Treffer anschließend in der YouTube-Music-App.
 */
class YouTubeRepository(
    private val apiKey: String,
    /** Für API-Keys mit Android-App-Beschränkung. */
    private val packageName: String? = null,
    private val certSha1: String? = null,
) {

    private val queries = listOf(
        "drum and bass",
        "dnb",
        "neurofunk",
        "liquid drum and bass",
        "jump up dnb",
    )

    suspend fun findNewTracks(options: SearchOptions): List<Track> = coroutineScope {
        // Etwas großzügiger suchen: Upload und Release können einen Tag auseinanderliegen.
        val publishedAfter = Instant.now().minus(options.days + 1, ChronoUnit.DAYS)
            .truncatedTo(ChronoUnit.SECONDS).toString()

        val ids = queries
            .map { q -> async(Dispatchers.IO) { search(q, publishedAfter) } }
            .awaitAll()
            .flatten()
            .distinct()

        ids.chunked(50)
            .map { chunk -> async(Dispatchers.IO) { videoDetails(chunk) } }
            .awaitAll()
            .flatten()
            .filter { it.passes(options) }
            .map { it.track }
            .sortedWith(compareByDescending<Track> { it.releaseDate }.thenByDescending { it.isOfficialRelease })
    }

    private class Candidate(val track: Track, val dnbKeyword: Boolean) {
        fun passes(o: SearchOptions): Boolean {
            if (!TrackFilter.isWithinDays(track.releaseDate, o.days)) return false
            if (track.durationSeconds in 1 until TrackFilter.MIN_TRACK_SECONDS) return false
            if (o.hideMixes && TrackFilter.isLikelyMix(track.title, track.durationSeconds)) return false
            if (o.officialOnly && !track.isOfficialRelease) return false
            // Offizielle Releases haben oft keine Genre-Begriffe in den Metadaten; YouTubes
            // Suchtreffer reichen dort. Bei normalen Uploads verlangen wir einen DnB-Begriff.
            return track.isOfficialRelease || dnbKeyword
        }
    }

    private suspend fun search(query: String, publishedAfter: String): List<String> {
        val url = "https://www.googleapis.com/youtube/v3/search" +
            "?part=snippet&type=video&videoCategoryId=10&order=date&maxResults=50" +
            "&publishedAfter=${enc(publishedAfter)}&q=${enc(query)}&key=${enc(apiKey)}"
        val items = getJson(url).optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).mapNotNull {
            items.getJSONObject(it).optJSONObject("id")?.optString("videoId")?.takeIf(String::isNotEmpty)
        }
    }

    private suspend fun videoDetails(ids: List<String>): List<Candidate> {
        val url = "https://www.googleapis.com/youtube/v3/videos" +
            "?part=snippet,contentDetails&maxResults=50&id=${enc(ids.joinToString(","))}&key=${enc(apiKey)}"
        val items = getJson(url).optJSONArray("items") ?: return emptyList()
        val zone = ZoneId.systemDefault()
        return (0 until items.length()).mapNotNull { i ->
            val item = items.getJSONObject(i)
            val snippet = item.optJSONObject("snippet") ?: return@mapNotNull null
            val title = snippet.optString("title")
            val channel = snippet.optString("channelTitle")
            val description = snippet.optString("description")
            val tags = snippet.optJSONArray("tags")?.let { arr ->
                (0 until arr.length()).joinToString(" ") { arr.optString(it) }
            } ?: ""
            val uploaded = runCatching {
                Instant.parse(snippet.optString("publishedAt")).atZone(zone).toLocalDate()
            }.getOrElse { LocalDate.now() }
            val thumbs = snippet.optJSONObject("thumbnails")
            val thumb = listOf("high", "medium", "default")
                .firstNotNullOfOrNull { thumbs?.optJSONObject(it)?.optString("url")?.takeIf(String::isNotEmpty) }
            val duration = TrackFilter.parseIsoDuration(
                item.optJSONObject("contentDetails")?.optString("duration") ?: ""
            )
            Candidate(
                track = Track(
                    videoId = item.optString("id"),
                    title = title,
                    channel = channel.removeSuffix(" - Topic"),
                    thumbnailUrl = thumb,
                    releaseDate = TrackFilter.parseReleaseDate(description) ?: uploaded,
                    durationSeconds = duration,
                    isOfficialRelease = TrackFilter.isOfficialRelease(channel, description),
                ),
                dnbKeyword = TrackFilter.isDnbRelated(title, description, tags, channel),
            )
        }
    }

    private suspend fun getJson(url: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            packageName?.let { setRequestProperty("X-Android-Package", it) }
            certSha1?.let { setRequestProperty("X-Android-Cert", it) }
        }
        try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw YouTubeApiException(apiErrorMessage(code, body))
            JSONObject(body)
        } finally {
            conn.disconnect()
        }
    }

    private fun apiErrorMessage(code: Int, body: String): String {
        val error = runCatching { JSONObject(body).getJSONObject("error") }.getOrNull()
        val reason = error?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason") ?: ""
        val message = error?.optString("message") ?: ""
        return when {
            reason == "quotaExceeded" ->
                "Tageskontingent der YouTube API ist aufgebraucht. Morgen geht's weiter."
            reason == "keyInvalid" || message.contains("API key not valid") ->
                "Der API-Key ist ungültig. Bitte in den Einstellungen prüfen."
            reason == "accessNotConfigured" || message.contains("has not been used") ->
                "Die \"YouTube Data API v3\" ist für diesen Key nicht aktiviert."
            code == 403 -> "Zugriff verweigert (403): $message"
            else -> "YouTube-Fehler $code: $message"
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
