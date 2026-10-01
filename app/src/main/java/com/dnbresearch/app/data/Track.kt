package com.dnbresearch.app.data

import java.time.LocalDate

data class Track(
    val videoId: String,
    val title: String,
    val channel: String,
    val thumbnailUrl: String?,
    /** Release-Datum (aus "Released on:" bei offiziellen Releases, sonst Upload-Datum). */
    val releaseDate: LocalDate,
    val durationSeconds: Long,
    /** true = offizieller YouTube-Music-Release ("Künstler - Topic"-Kanal / "Provided to YouTube by"). */
    val isOfficialRelease: Boolean,
) {
    val youTubeMusicUrl: String get() = "https://music.youtube.com/watch?v=$videoId"

    val durationText: String
        get() {
            val m = durationSeconds / 60
            val s = durationSeconds % 60
            return "%d:%02d".format(m, s)
        }
}
