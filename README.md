# Hubbell

Ein kleines Android-Spiel: Hubbell, ein sympathischer Metaller mit langer Mähne, Fusselbart und Metal-Shirt, sitzt in seiner Metal-Bar und will auf keinen Fall nüchtern werden. Zwischendurch headbangt er und zeigt die Pommesgabel.

## Spielprinzip

- Oben siehst du Hubbells **Alkoholpegel**. Er sinkt ständig, und mit jedem Level schneller.
- **Tippe auf Hubbell** (oder auf „Prost!“), um ihm ein Getränk zu bringen. Jedes Getränk bringt Pegel, Erfahrung und Taler.
- Im **Upgrade-Shop** schaltest du ab bestimmten Leveln stärkere Getränke frei: Helles → Weizen → Stout → Rotwein → Whisky → Hubbell-Spezial.
  Dazu gibt es Verbesserungen wie den Turbo-Zapfhahn, Spendierhosen, die Eiserne Leber und Kellnerin Gerda, die automatisch serviert.
- Ab 80 % Pegel hat Hubbell beste Laune und es gibt doppelte Taler.
- Fällt der Pegel auf **null**, bleiben dir **10 Sekunden**, um ihn mit einem Getränk zu retten. Sonst ist das Spiel vorbei.
- Jeder Level-Aufstieg wird gefeiert: Konfetti, Pyro-Show, Moshpit, Lightshow und alle zehn Level das große Hubbell-Fest.

Im Hintergrund tauschen zehn verschiedene Metaller ständig die Poster von 50 erfundenen Bands aus, und im Vordergrund hüpfen immer wieder Gäste ins Bild und rufen etwas rein.

Alle Sounds (Gitarrenriffs mit Schlagzeug, Anstoßen, Kasse, Alarm) werden im Code synthetisiert; oben im Bild lässt sich der Ton abschalten.

Der Spielstand und die Rekorde werden lokal auf dem Gerät gespeichert.

## Bauen

Voraussetzungen: JDK 17 und das Android SDK (z. B. über Android Studio).

```sh
./gradlew assembleRelease      # APK unter app/build/outputs/apk/release/
./gradlew testDebugUnitTest    # Tests der Spiellogik
```

Jeder Push auf `main` baut in GitHub Actions ein verkleinertes, signiertes Release-APK und veröffentlicht es auf der [Release-Seite](https://github.com/marklechtermann/alcomon/releases/latest). Der Signierschlüssel liegt als Prototyp-Schlüssel offen im Repo (`app/hubbell-release.jks`), damit neue Versionen sich über alte installieren lassen.

## Aufbau

- `game/` – reine Spiellogik ohne Android-Abhängigkeiten (Getränke, Upgrades, Pegel, Belohnungen)
- `data/` – Speichern von Spielstand und Rekorden
- `ui/` – Jetpack-Compose-Oberfläche; alle Grafiken werden im Code gezeichnet (`ui/draw/`)
