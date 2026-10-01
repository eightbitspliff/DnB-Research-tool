package com.dnbresearch.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dnbresearch.app.MainViewModel
import com.dnbresearch.app.SearchState
import com.dnbresearch.app.data.Track
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onOpenTrack: (Track) -> Unit) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.apiKey.isBlank()) showSettings = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("DnB Radar", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Button(
                onClick = viewModel::search,
                enabled = viewModel.state != SearchState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DnbRed),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Neue DnB-Tracks suchen", style = MaterialTheme.typography.titleMedium)
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(7L, 14L, 30L).forEach { d ->
                    FilterChip(
                        selected = viewModel.days == d,
                        onClick = { viewModel.updateDays(d) },
                        label = { Text("$d Tage") },
                    )
                }
                FilterChip(
                    selected = viewModel.hideMixes,
                    onClick = { viewModel.updateHideMixes(!viewModel.hideMixes) },
                    label = { Text("Keine Mixe") },
                )
                FilterChip(
                    selected = viewModel.officialOnly,
                    onClick = { viewModel.updateOfficialOnly(!viewModel.officialOnly) },
                    label = { Text("Nur offizielle Releases") },
                )
            }

            when (val s = viewModel.state) {
                SearchState.Idle -> CenterMessage(
                    "Drück den Button, um Drum & Bass zu finden, der in den letzten ${viewModel.days} Tagen erschienen ist.",
                )
                SearchState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DnbRed)
                }
                is SearchState.Error -> CenterMessage(s.message, isError = true)
                is SearchState.Success ->
                    if (s.tracks.isEmpty()) {
                        CenterMessage("Keine neuen Tracks gefunden. Versuch es mit weniger Filtern.")
                    } else {
                        TrackList(s.tracks, onOpenTrack)
                    }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentKey = viewModel.apiKey,
            onSave = {
                viewModel.updateApiKey(it)
                showSettings = false
            },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun TrackList(tracks: List<Track>, onOpenTrack: (Track) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "${tracks.size} Tracks · Antippen öffnet YouTube Music",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(tracks, key = { it.videoId }) { track ->
            TrackRow(track) { onOpenTrack(track) }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = DnbRed)
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (track.isOfficialRelease) {
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Offizieller Release",
                            tint = DnbRed,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        track.channel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "${relativeDate(track.releaseDate)} · ${track.durationText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Abspielen", tint = DnbRed)
        }
    }
}

private fun relativeDate(date: LocalDate): String =
    when (val d = ChronoUnit.DAYS.between(date, LocalDate.now())) {
        in Long.MIN_VALUE..0L -> "Heute"
        1L -> "Gestern"
        in 2L..13L -> "vor $d Tagen"
        else -> date.format(dateFormat)
    }

@Composable
private fun CenterMessage(text: String, isError: Boolean = false) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDialog(currentKey: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf(currentKey) }
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("YouTube-API-Key") },
        text = {
            Column {
                Text(
                    "Die App nutzt die offizielle YouTube Data API v3 (kostenlos, ~20 Suchen pro Tag). " +
                        "Lege in der Google Cloud Console einen API-Key an und aktiviere dort die \"YouTube Data API v3\".",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "→ Google Cloud Console öffnen",
                    color = DnbRed,
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .clickable { uriHandler.openUri("https://console.cloud.google.com/apis/library/youtube.googleapis.com") },
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API-Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(key) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
