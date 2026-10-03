# Flitz der Igel

Ein schnelles Jump'n'Run im Stil der klassischen 16-Bit-Igel-Spiele, gebaut für Handys mit Wischsteuerung.
Eine einzige HTML-Datei ohne Abhängigkeiten: `index.html` im Browser öffnen.

- 15 Level: 5 Zonen (Grüne Hügel, Sonnenwüste, Kristallhöhle, Lavafabrik, Himmelstempel) mit je 2 Akten und einem Boss
- Hügel mit Hangphysik, Rampen, Federn, Boost-Pads, bewegliche Plattformen, Stacheln, Abgründe und Lava
- Ringe schützen vor einem Treffer, 100 Ringe geben ein Extraleben, Checkpoints in jedem Akt
- Sterne pro Level (Ziel erreicht, 40 % der Ringe, schnelle Zeit ohne Tod); Fortschritt wird im Browser gespeichert

## Steuerung

| Geste | Aktion |
| --- | --- |
| Nach rechts / links wischen | Losrennen / bremsen und umdrehen |
| Tippen oder nach oben wischen | Springen |
| In der Luft nochmal tippen | Zielangriff auf den nächsten Gegner (sonst Luft-Sprint) |
| Nach unten wischen | Rollen; im Stand Spin-Dash; in der Luft Stampfer |
| Finger gedrückt halten | Anhalten |

Tastatur: Pfeiltasten, Leertaste, P für Pause.

`game.html` ist derselbe Inhalt ohne Dokument-Kopf (für die Veröffentlichung als Artifact).
