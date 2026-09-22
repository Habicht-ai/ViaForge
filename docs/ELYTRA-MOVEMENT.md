# Elytra-Bewegung: Sprint-Sprung und niedrige Durchgänge

Nachfolgende Korrekturen an Landefolgen, Pose-Zeitpunkt und Handdarstellung:
[aktueller Regressionsbericht](REGRESSIONS-2026-09-20.md). Die untenstehenden
Build-Ergebnisse und Prüfsummen gehören zum vorherigen Stand.

Stand: 20. September 2026. Ergänzt [ELYTRA.md](ELYTRA.md) und
[INVENTORY-PREVIEW.md](INVENTORY-PREVIEW.md).

## Korrigierte Unterschiede

Der bisherige Controller erlaubte den Start in allen Versionen ausschließlich
bei negativer vertikaler Geschwindigkeit, wartete immer auf Metadaten und
sperrte weitere Startversuche 20 Ticks. Bei Bodenkontakt löschte er den Flugzustand
sofort und vergrößerte die Kollisionsbox ungeprüft auf 1,8 Blöcke.

- `PREDICT_ELYTRA_START` gilt ab 1.15: Der zweite Sprung-Tastendruck darf schon
  im Aufstieg starten und setzt sofort den lokalen Flugzustand. Bis einschließlich
  1.14.4 bleiben Sinkbedingung und Warten auf das Original-Flugflag erhalten.
  Es gibt keine künstliche Wiederholungssperre; gehaltene Tasten senden weiterhin
  nur einmal. Original-Metadaten bestätigen oder verwerfen die lokale Vorhersage.
- Ein Bodenkontakt allein löscht das Flugflag auf dem Client nicht. Wie in den
  untersuchten Originalclients entscheidet die Server-Metadatenrückmeldung.
  Dadurch kann ein unmittelbar folgender Sprint-Sprung die vorhandene horizontale
  Geschwindigkeit mit der Gleitreibung weiterführen. Es wird kein zusätzlicher
  Boost erfunden; Sprungimpuls und Gleitphysik erzeugen den Effekt.
- Die Creative-Doppelsprungbehandlung läuft vor dem Elytra-Start und behält ihren
  Vorrang. Der Controller verändert keine Flugfähigkeiten. Wasser, ungültige
  Ausrüstung, Reiten, Creative-Flug, Tod und Session-Wechsel beenden den lokalen
  Gleitpfad weiterhin.
- Größenänderungen prüfen reale Block-/Entitätskollisionen. Alte Versionen
  behalten ihre kleine Box, solange eine Vergrößerung blockiert ist.
  `CRAWLING_POSE` ab 1.14 wählt bei Platzmangel zunächst die 1,5-Blöcke-Duckpose,
  sonst die 0,6-Blöcke-Kriechpose mit 0,4 Augenhöhe. Sobald genügend Platz besteht,
  richtet sich der Spieler wieder auf. In freiem Raum gibt es keine neue
  Liegen-Taste; das entspricht der automatischen Pose des Originals.
- Der native 1.8-Verdrängungstest mit auf ganze Blöcke gerundeter Kopffreiheit
  wird bei tatsächlich freier kleiner Box unterdrückt. Er darf einen passenden
  Spieler nicht aus einem niedrigen Durchgang schieben.
- Kriechen verwendet verlangsamte Eingabe, den Übergang mit 0,09 pro Tick,
  horizontale Körperrotation und den originalen 26-Phasen-Schwimmzyklus für
  Arme/Beine. Ausrüstung und Skin-Overlays folgen dem Modell. Ab 1.21.2 hält
  `FIXED_CRAWLING_HEAD` den Kopfwinkel auch beim Ausblenden der Pose; ältere
  Modelle kehren beim Aufstehen zur Blickneigung zurück.

Alle Regeln hängen am aktiven Kompatibilitätsprofil. Native 1.8.9 und
Einzelspieler aktivieren diese Ergänzungen nicht. Flugpose und Kriechpose sind
getrennt: Kriechen erlaubt keinen Raketenantrieb und setzt kein Flugflag.

## Originalquellen und Prüfung

Untersucht wurden die lokal aus Mojangs gehashten Downloads vorhandenen
Originalclients 1.9, 1.12.2, 1.14.4, 1.15 und 26.3: `EntityPlayerSP`/
`LocalPlayer`, `EntityPlayer`/`Player`, `EntityLivingBase`/`LivingEntity`.
Zusätzlich wurden die Humanoid-Modelle 1.20.1, 1.20.2, 1.21.1, 1.21.3,
1.21.5, 1.21.10 und 1.21.11 für die Pose verglichen. Die lokalen
Untersuchungsdateien unter `build/inspection/movement/` werden nicht ausgeliefert.
Mojang beschreibt den Zweck der automatischen Pose auch in
[How crawling came to Minecraft](https://www.minecraft.net/en-us/article/how-crawling-came-minecraft).

`ElytraMovementSmokeTest` benutzt die echten nativen Input-, Sprung-, Reise-
und Kollisionsmethoden in Forge. Startaktionen und Bestätigungen durchlaufen
die jeweilige vollständige Via-Paketkette. Er prüft Aufstiegsaktivierung,
Serverablehnung/sofortigen erneuten Tastendruck, verzögerte Landebestätigung,
erneuten Sprint-Sprung mit erhaltener Geschwindigkeit, einen Ein-Block-Tunnel,
Kamera, Kriechbewegung/Modell und Creative-Doppelsprung. Die Testblöcke existieren
nur in der isolierten Client-Testwelt, nicht in gespeicherten Laborwelten.

Die erweiterte Live-Probe führt nach dem bisherigen freien Flug und der
Wasserlandung zwei unmotorisierte Sprint-/Gleitzyklen auf der vorhandenen
Navigationsfläche aus. Sie verwendet neue Testspieler und verändert keine
Ausstellungsblöcke. Vorherige Berichte werden vor dem Ersetzen gesichert.

## Tatsächlich bestanden und ausgeliefertes Artefakt

- 115 JUnit-Tests ohne Fehler, Fehlerausnahmen oder übersprungene Tests.
- 40 Python-Tests, `build/logs/movement-python-tests.log`.
- Vollständiger Build und Forge-Smoke über **49 Ressourcenprofile**,
  Abschluss 20.09.2026 um 19:35 Uhr. Der frisch geschriebene Bericht
  `build/logs/block-client-smoke-test.txt` beginnt mit `PASS`; zugehöriger
  Build-Log: `build/logs/movement-release-full-2.log`.
  Der erste Gesamtlauf endete vorzeitig mit `RUNNING` und zählt nicht als bestanden.
- Echte Vanilla-Server **1.12.2, 1.15 und 26.3**: jeweils 70 Ticks freier Flug
  mit Rakete, Original-Sound-Engine, Handanimation und Inventaransicht;
  anschließend Wasserlandung und zwei unmotorisierte Sprint-/Gleitzyklen.
  Alle melden `sprint_glide_cycles: 2` und
  `ground_glide_server_confirmed: true`. `rising_elytra_start` ist auf 1.12.2
  korrekt `false`, auf 1.15 und 26.3 `true`.
  Berichte: `run/test-servers/<Version>/live-flight-probe.json`;
  Logs: `build/logs/movement-live-boundaries.log` und
  `build/logs/movement-live-26.3.log`. Die eigens gestarteten Server 1.12.2 und
  1.15 wurden gespeichert und gestoppt; der bei Testbeginn laufende 26.3-Server
  wurde von der Probe nicht beendet.

Release: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, **14.375.928 Bytes**.
SHA-256: `c16fbf1f2fd09c40dec31bffed529c9952d5a19e4b18469c0043d55d0e4c085d`.
Die JAR enthält den Flugcontroller, die neuen Mixins und das Kriechmodell;
Entwicklungsproben sind nicht darin enthalten.

## Verbleibende Grenzen

Dies ist keine vollständige Portierung aller neueren Bewegungsarten oder
Attribute (etwa Wasserschwimmen oder beliebige per Server gesetzte Skalierung).
Die Kriechergänzung betrifft zunächst den lokalen Spieler; fremde Spieler
benötigen für ihre zusätzliche Schwimmpose weiterhin eine eigene Anbindung
der Pose-Metadaten. Die niedrige Tunnelpassage wird automatisiert in Forge
geprüft; eine vollständige Video-Gegenüberstellung sämtlicher Zielclients
ist damit nicht belegt.

## Grim-Befunde vom 21.09.2026

Echte Survival-Verbindungen deckten zusätzliche Timing- und Bewegungsabweichungen auf. Neu sind die zielversionsabhängige Restgeschwindigkeitsschwelle 0,003 statt 0,005, die frühere Positions-Erinnerung und geordnete moderne Pong-/Input-/Tick-End-Pakete. Das bedeutet noch keine vollständige moderne Bewegungstreue; insbesondere 26.2 hat weiterhin Grim-Befunde. Ablauf, Nachweise und Grenzen: [REGRESSIONS-2026-09-21.md](REGRESSIONS-2026-09-21.md).

Der [Originalvergleich beim Umgebungsschub](ENTITY-PUSH.md) vom 22.09.2026
belegt zusätzlich die Grenze ab 1.21.5: Bei Spielern gilt horizontal
X²+Z² < 0,003² statt zweier unabhängiger Achsschwellen. Y bleibt separat.
Die Korrektur und ihre Welt-/Livetests beheben diesen nachgewiesenen Unterschied;
die übrigen dokumentierten Elytra-/26.2-Befunde bleiben offen.
