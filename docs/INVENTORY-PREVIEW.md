# Versionsgetreue Spieleransicht im Inventar

Die Elytra-Flugrotation wurde bislang auf die alte, am Fußpunkt ausgerichtete
1.8-Inventarvorschau angewendet. Dadurch konnte die Figur bei modernen Zielen
vor den Inventarplätzen erscheinen. Das gilt sowohl für Survival als auch für
den Spielerinventar-Reiter im Creative-Menü.

## Originale Versionsgrenzen

| Zielversion | Vorschau |
| --- | --- |
| 1.9 bis 1.20.1 | Bisheriger Fußpunkt und unbegrenzte Darstellung bleiben erhalten, einschließlich der ursprünglichen Flugbesonderheiten. |
| Ab 1.20.2 | Rechteckige Begrenzung und Zentrierung anhand der aktuellen Körperhöhe. Survival: `(26,8)` bis `(75,78)`, Größe 30. Creative: `(73,6)` bis `(105,49)`, Größe 20. Koordinaten relativ zur linken oberen Inventarecke. |
| Ab 1.21.5 | Originale Begrenzung der seitlichen Flugrotation mit absolutem Skalarprodukt und Mindestgeschwindigkeit. |
| Ab 1.21.11 | Flugrotation aus der wirklichen Blick-/Bewegungsrichtung vor der Mausausrichtung erfassen; Vorschau-Flugneigung auf 0 setzen. |

Die drei Regeln heißen `BOUNDED_INVENTORY_PREVIEW`,
`LIMITED_INVENTORY_FLIGHT_YAW` und `CAPTURED_INVENTORY_FLIGHT` und werden über
die gemeinsamen Compatibility-Profile vererbt. Native 1.8.9, Einzelspieler und
unbekannte Zielprotokolle verwenden weiterhin ihre bisherige Vorschau.

Verglichen wurden die SHA-1-geprüften Mojang-Originalclients 1.20.1/1.20.2,
1.20.4, 1.21.1/1.21.3/1.21.4/1.21.5, 1.21.6, 1.21.10/1.21.11 und 26.3:
`InventoryScreen`, `CreativeModeInventoryScreen`, `PlayerRenderer` beziehungsweise
`AvatarRenderer` und die moderne Vorschau-Textur. Die Korrektur der Ausrichtung
ist außerdem in Mojangs [Snapshot 25w44a, MC-2791](https://www.minecraft.net/en-us/article/minecraft-snapshot-25w44a)
dokumentiert. Lokale Untersuchungsdateien: `build/inspection/inventory-flight/`.
Diese Originalprogramme werden nicht in der Mod ausgeliefert.

## Umsetzung

`InventoryEntityPreview` übernimmt ausschließlich die Aufrufe aus den beiden
Inventarbildschirmen. Andere Entitätsvorschauen und die Weltdarstellung behalten
ihren bisherigen Ablauf. Die Position, Größe, Mauswinkel und Flugneigung stammen
aus den Originalfunktionen. Die moderne Rendertextur wird auf dem alten Renderer
durch ein entsprechendes OpenGL-Begrenzungsrechteck nachgebildet.

Bereits aktive Begrenzungen werden geschnitten und anschließend wiederhergestellt.
Die Vorschau bewahrt temporäre Blick-/Körperwinkel, Renderkamera, Schattenflag,
Matrizen und ihren eigenen Flugkontext in `finally`. Sie schreibt weder Bewegung
noch Hitbox, Inventar, Spielmodus oder serverbestätigten Flugzustand.

## Prüfungen

`InventoryFlightSmokeTest` ruft die tatsächlichen Survival-/Creative-GUI-Methoden
mit dem nativen Spielerrenderer auf, sowohl im Flug als auch nach dem Flugende.
Es prüft die GL-Matrizen bei verschiedenen Mauspositionen, sichtbare Figurpixel,
fehlende Überzeichnung außerhalb des Fensters bei modernen Zielen, verschachtelte
Begrenzung und die Wiederherstellung des Spielerzustands. 26.3 wird bei GUI-Skala
1, 2 und 3 geprüft. Bilder liegen unter
`build/logs/screenshots/inventory-flight-{creative,survival}-<Version>.png`.

Der echte Flugtest öffnet während des Flugs das Inventar und schließt es wieder.
Sein Bericht enthält `inventory_open_during_flight`; die Bildschirmaufnahme heißt
`live-inventory-flight-<Protokoll>.png`. Hierfür werden eigene neue Testspieler
verwendet. Ein mit `--reuse-running` verwendeter Server bleibt geöffnet.

Am 20.09.2026 bestanden: 114 JUnit-Tests ohne Fehler/übersprungene Tests und
vollständiger Forge-Smoke über alle 49 registrierten Ressourcenprofile. Der
frische Bericht `build/logs/block-client-smoke-test.txt` beginnt mit `PASS`
(15:08 Uhr); Build-Protokoll: `build/logs/inventory-flight-release-full.log`.
Die Prüfung umfasst beide Inventare während und nach dem Flug sowie das
Zurückschalten zur nativen Vorschau beim Disconnect.

Die echten Forge-Verbindungen zu Vanilla 1.12.2 und 26.3 bestanden ebenfalls
am 20.09.2026 (17:45/17:46 Uhr), jeweils mit 70 Flugticks, geöffnetem/geschlossenem
Inventar, Raketenboost, laufendem Original-Flugton und anschließender Wasserlandung.
Beide Berichte `run/test-servers/<Version>/live-flight-probe.json` enthalten
`inventory_open_during_flight: true`. Protokoll: `build/logs/inventory-flight-live.log`.
Die 26.3-Aufnahme `build/logs/screenshots/live-inventory-flight-777.png` wurde
zusätzlich visuell geprüft; die Figur bleibt innerhalb des Vorschaufensters.
Vorherige Berichte sind als zeitgestempelte `.bak`-Dateien erhalten. Der bereits
laufende 26.3-Server wurde weder angehalten noch neu gestartet.

Aktuelles Release einschließlich [Landefolgen-/Handkorrekturen](REGRESSIONS-2026-09-20.md): `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, 14.378.697 Bytes.
SHA-256: `6f502476570b24e1b070697d9e821ae32bc31b09344e6905c1c0249ecc442d16`.
Die Entwicklungsproben sind nicht in der Release-JAR enthalten.

Das ist kein automatischer Pixelvergleich mit einem parallel laufenden
Originalclient. Insbesondere Fremdmods, Shader und sämtliche modernen
Spielerskalierungsattribute sind dadurch nicht zertifiziert.
