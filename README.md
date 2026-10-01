# DnB Radar 🎧

Android-App (Pixel 7a / Android 8+), die auf Knopfdruck **neue Drum-and-Bass-Tracks der letzten 14 Tage**
findet und sie direkt in **YouTube Music** abspielt.

## So funktioniert's

1. Button **„Neue DnB-Tracks suchen“** drücken.
2. Die App sucht über die offizielle YouTube Data API v3 in der Kategorie *Musik* nach
   `drum and bass`, `dnb`, `neurofunk`, `liquid drum and bass` und `jump up dnb` – nur Uploads der letzten Tage.
3. Bei offiziellen YouTube-Music-Releases (Kanäle „Künstler - Topic“, ✔-Symbol) wird das echte
   **Release-Datum** („Released on: …“) verwendet, sonst das Upload-Datum.
4. Mixe, DJ-Sets, Podcasts und Videos > 12 Minuten werden herausgefiltert (abschaltbar).
5. Antippen eines Tracks öffnet ihn in der **YouTube-Music-App** (falls nicht installiert: im Browser).

Filter: 7 / 14 / 30 Tage, „Keine Mixe“, „Nur offizielle Releases“.

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

Optional: Repository-Secret `YOUTUBE_API_KEY` anlegen – dann ist der Key schon in der gebauten APK enthalten.

## Selbst bauen

```bash
./gradlew assembleDebug        # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # Unit-Tests
```

Benötigt JDK 17 und das Android SDK (compileSdk 35).
