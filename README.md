# DnB Radar 🎧

Android-App (Pixel 7a / Android 8+), die auf Knopfdruck **neue Drum-and-Bass-Tracks der letzten 14 Tage**
findet und sie direkt in **YouTube Music** abspielt.

## So funktioniert's

1. Button **„Neue DnB-Tracks suchen“** drücken.
2. Die App sucht über die offizielle YouTube Data API v3 in der Kategorie *Musik* nach
   `drum and bass`, `dnb`, `neurofunk`, `liquid drum and bass` und `jump up dnb` – nur Uploads der letzten Tage.
3. Bei offiziellen YouTube-Music-Releases (Kanäle „Künstler - Topic“, ✔-Symbol) wird das echte
   **Release-Datum** („Released on: …“) verwendet, sonst das Upload-Datum.
4. **Nur einzelne Tracks:** Mixe, DJ-Sets, B2Bs, Podcasts, Radioshows, komplette Alben/EPs,
   „Best of“-Zusammenstellungen, Shorts sowie alles unter 1:30 oder über 9 Minuten wird immer herausgefiltert.
   Doppelte Einträge (offizieller Release + Label-Upload desselben Tracks) werden zusammengefasst.
5. Antippen eines Tracks öffnet ihn in der **YouTube-Music-App** (falls nicht installiert: im Browser).
6. Optional: **„Als YouTube-Music-Playlist speichern“** legt in deinem Konto eine private Playlist
   „DnB Radar · TT.MM.JJJJ“ mit allen gefundenen Tracks an und öffnet sie in YouTube Music.

Filter: 7 / 14 / 30 Tage, „Nur offizielle Releases“.

## Installation auf dem Pixel 7a

1. Auf GitHub unter **Releases → „DnB Radar (neuester Build)“** die Datei `DnB-Radar.apk` herunterladen
   (wird bei jedem Push automatisch von GitHub Actions gebaut).
2. Öffnen → Android fragt nach „Installation aus unbekannten Quellen“ → für den Browser/Dateien-App erlauben.
3. App starten.

## YouTube-API-Key (einmalig, kostenlos)

YouTube Music hat keine offizielle öffentliche API, deshalb nutzt die App die YouTube Data API v3:

1. <https://console.cloud.google.com/> öffnen, ein Projekt anlegen.
2. **APIs & Dienste → Bibliothek → „YouTube Data API v3“ → Aktivieren**.
3. **APIs & Dienste → Anmeldedaten → Anmeldedaten erstellen → API-Schlüssel**.
4. Den Key in der App über das ⚙️-Symbol eintragen.

Kontingent: 10.000 Einheiten/Tag kostenlos, eine Suche kostet ~500 → etwa **20 Suchen pro Tag**.

## Playlist-Funktion einrichten (einmalig, nur wenn du sie nutzen willst)

Zum Anlegen einer Playlist muss die App in deinem YouTube-Konto schreiben dürfen (Google-Login).
Im **selben** Google-Cloud-Projekt wie der API-Key:

1. **APIs & Dienste → OAuth-Zustimmungsbildschirm** einrichten: Typ „Extern“, App-Name z.B. „DnB Radar“,
   Status „Testen“ reicht. Unter **Testnutzer** deine eigene Google-Adresse eintragen.
2. **APIs & Dienste → Anmeldedaten → Anmeldedaten erstellen → OAuth-Client-ID → Typ „Android“**:
   - Paketname: `com.dnbresearch.app`
   - SHA-1-Zertifikat-Fingerabdruck: `42:19:62:2F:4C:6C:8C:98:9C:5F:09:CB:4B:23:1C:34:13:67:96:6D`
     (das ist der feste Signaturschlüssel der APKs aus diesem Repo)
3. In der App „Als YouTube-Music-Playlist speichern“ tippen → Google-Konto wählen → YouTube-Zugriff erlauben.
   (Weil die App nicht von Google geprüft ist, erscheint ein Warnhinweis → „Weiter“.)

Die Playlist ist **privat** und erscheint in YouTube Music unter *Mediathek → Playlists*.
Kontingent: Playlist anlegen 50 Einheiten + 50 pro Track (30 Tracks ≈ 1.550 Einheiten).

Optional: Repository-Secret `YOUTUBE_API_KEY` anlegen – dann ist der Key schon in der gebauten APK enthalten.

## Selbst bauen

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # Unit-Tests
```

Benötigt JDK 17 und das Android SDK (compileSdk 35).
