package com.dnbresearch.app.data

import java.time.LocalDate

/** Reine Hilfsfunktionen ohne Android-Abhängigkeiten (per Unit-Test geprüft). */
object TrackFilter {

    /** DnB-Singles (auch Extended Mixes) sind selten länger; alles darüber sind Mixe, Sets oder Alben. */
    const val MAX_TRACK_SECONDS = 9 * 60L
    const val MIN_TRACK_SECONDS = 90L

    private val durationRegex = Regex("""^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?$""")
    private val releasedOnRegex = Regex("""Released on:\s*(\d{4}-\d{2}-\d{2})""")

    // Bewusst nicht einfach "mix": "(Original Mix)" oder "(VIP Mix)" sind normale Tracks.
    private val mixWords = listOf(
        "dj mix", "mixtape", "mix tape", "mixed by", "minimix", "mini mix", "guest mix", "megamix",
        "continuous mix", "essential mix", "year mix", "yearmix", "album mix", "ep mix", "showcase mix",
        "dj set", "live set", "b2b", "podcast", "radio show", "livestream", "live stream",
        "full album", "full ep", "album stream", "album sampler", "ep sampler", "compilation",
        "boiler room", "live at", "live @", "best of", "top 10", "top 20", "top 50", "top 100",
        "playlist", "1 hour", "2 hour", "3 hour", "hour of", "#shorts",
    )

    private val dnbWords = listOf(
        "drum and bass", "drum & bass", "drum n bass", "drum'n'bass", "drumandbass", "drum&bass",
        "dnb", "d&b", "d'n'b", "neurofunk", "liquid funk", "liquid dnb", "jump up", "jungle", "rollers",
    )

    /** ISO-8601-Dauer wie "PT4M13S" in Sekunden. Unbekanntes Format -> 0. */
    fun parseIsoDuration(iso: String): Long {
        val m = durationRegex.find(iso) ?: return 0
        val (d, h, min, s) = m.destructured
        return (d.toLongOrNull() ?: 0) * 86400 +
            (h.toLongOrNull() ?: 0) * 3600 +
            (min.toLongOrNull() ?: 0) * 60 +
            (s.toLongOrNull() ?: 0)
    }

    /** "Released on: 2026-09-25" aus der Beschreibung automatisch erzeugter YouTube-Music-Tracks. */
    fun parseReleaseDate(description: String): LocalDate? =
        releasedOnRegex.find(description)?.groupValues?.get(1)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }

    fun isOfficialRelease(channel: String, description: String): Boolean =
        channel.endsWith(" - Topic") || description.contains("Provided to YouTube by")

    fun isLikelyMix(title: String, durationSeconds: Long): Boolean {
        if (durationSeconds > MAX_TRACK_SECONDS) return true
        val t = title.lowercase()
        return mixWords.any { t.contains(it) }
    }

    fun isDnbRelated(vararg texts: String): Boolean {
        val joined = texts.joinToString(" ").lowercase()
        return dnbWords.any { joined.contains(it) }
    }

    fun isWithinDays(date: LocalDate, days: Long, today: LocalDate = LocalDate.now()): Boolean =
        !date.isBefore(today.minusDays(days)) && !date.isAfter(today.plusDays(1))

    /**
     * Derselbe Track taucht oft doppelt auf: als offizieller Release ("Titel" vom Kanal "Künstler - Topic")
     * und als Label-Upload ("Künstler - Titel"). Dann behalten wir nur den offiziellen Release.
     */
    fun removeDuplicates(tracks: List<Track>): List<Track> {
        val official = tracks.filter { it.isOfficialRelease }
        return tracks.distinctBy { it.videoId }.filter { t ->
            t.isOfficialRelease || official.none { o ->
                val title = t.title.lowercase()
                title.contains(o.title.lowercase()) && title.contains(o.channel.lowercase())
            }
        }
    }
}
