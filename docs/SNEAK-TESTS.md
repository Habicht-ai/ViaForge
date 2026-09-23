# Sneak/Sprint: echte Clienttests

Stand 23.09.2026. Ursache, Originalregeln und reproduzierbare Befehle:
[SNEAK-SPRINT.md](SNEAK-SPRINT.md). Alle Berichtsordner liegen unter
`run/test-servers/`; maßgeblich sind dort `report.json`, Client-Rohdaten und
`plugins/ViaForgeLabAC/events.jsonl`. Ein PASS verlangt reale Bewegung bzw.
beobachteten Hinderniskontakt, ausreichend Predictions, beobachtete Eingaben,
geladenen Aufbau, lebenden Empfänger und tatsächlichen Survival-Modus mit Grim
ON, richtiger Clientversion, echtem Verbose und ohne OP-/Bypassrechte.

## Abschlussläufe der Landmatrix

Alle folgenden ViaForge-Läufe: Marke `fml,forge`, vom installierten Grim
2.3.74-8eb5f28 erkannte Clientversion gleich der Zielversion. Original: `vanilla`.
Keine Survival-Flags oder -Setbacks, einschließlich Resets, Ausrüstung und
Zwischenphasen. Creative-Abschlussabschnitte in den erweiterten Läufen sind
zusätzlich sauber, werden aber nicht als Survival-Physikfälle gezählt.

| Ziel / Protokoll | Client | Survival-Abschnitte | Predictions in Messabschnitten | Größter Messoffset | Login-Korrekturen |
|---|---|---:|---:|---:|---:|
| 1.13.2 / 404 | ViaForge | 30/30 | 969 | 4.64e-8 | 0 |
| 1.14.4 / 498 | ViaForge | 30/30 | 969 | 7.53e-8 | 2 |
| 1.15 / 573 | ViaForge | 30/30 | 975 | 7.53e-8 | 2 |
| 1.19.4 / 762 | ViaForge | 61/61 | 2287 | 8.11e-8 | 2 |
| 1.21.4 / 769 | ViaForge | 30/30 | 999 | 8.11e-8 | 2 |
| 1.21.5 / 770 | ViaForge | 30/30 | 1000 | 7.52e-8 | 2 |
| 26.2 / 776 | ViaForge | 62/62 | 2343 | 7.52e-8 | 2 |
| 26.2 / 776 | Original | 62/62 | 2343 | 7.85e-15 | 0 |

Die kleinen verbleibenden numerischen Unterschiede werden nicht als bitgenaue
Gleichheit ausgegeben. Die gemessenen Tasten-, Sprint- und Haltungsübergänge
entsprechen den untersuchten Originalregeln. Login-Korrekturen sind außerhalb
der Survival-Wertung und bleiben ein eigener offener Befund.

Exakte Berichtsordner in Tabellenreihenfolge:

```
sneak-1.13.2-boundary-final-1790178347116244400
sneak-1.14.4-boundary-final-1790178521265542900
sneak-1.15-boundary-final-1790178689349748800
sneak-1.19.4-boundary-final2-1790179438716226600
sneak-1.21.4-boundary-final2-1790179767012257200
sneak-1.21.5-boundary-final2-1790179939419102400
sneak-26.2-final-1790177851976950900
sneak-26.2-original-final-1790177474153259100
```

Die 30 Grundabschnitte umfassen Gehen/Sprinten, Sneak im Stand und beim Gehen,
beide Eingabereihenfolgen mit Sprint, gleichzeitigen Beginn/Loslassen, diagonale
und rückwärtige Eingaben, Sprünge/Landungen und vollständiges Ausrollen.
Die erweiterten 26.2- und 1.19.4-Läufe enthalten jeweils acht einzelne
Sneak-Ticks beim Gehen, Sprinten und Springen, Wandkontakt, Stufen/Treppen,
Kanten, niedrige Decke, erzwungenes Ducken ohne Shift, trockenes Kriechen nach
Wasserübergang, Austritt bis zum Stehen, Land-zu-Wasser-Eintritt und Swift Sneak.
26.2 enthält zusätzlich das manuell synchronisierte Sneak-Attribut 0.65.

## Zusätzliche Randwerte

| Ziel / Client | Abschnitte | Ergebnis | Ordner |
|---|---:|---|---|
| 26.2 Original | Attribut 0, 1, 0.3 | 3/3 PASS | `sneak-26.2-bounds-native-1790178159904704200` |
| 26.2 ViaForge | Attribut 0, 1, 0.3 | 3/3 PASS | `sneak-26.2-bounds-viaforge-1790178233107575400` |
| 1.19.4 ViaForge | Swift Sneak III unter Wasser | 1/1 PASS | `sneak-1.19.4-sneak-regression-final-1790180508680893300` |
| 26.2 Original | Doppeltippen, Zeitfenster, Sneak-Unterbrechung | 3/3 PASS | `sneak-26.2-timing-final-1790181502693612900` |
| 26.2 ViaForge | dieselben exakten Ticks | 3/3 PASS | `sneak-26.2-timing-final-1790181578119699900` |

Alle Randwertläufe einschließlich Reset/Ausrüstung ohne Survival-Probleme.
Original-Timerlauf ohne Login-Korrektur, die anderen jeweils mit zwei separat
erfassten Login-Korrekturen. Beim Nullattribut wurden echte
Vorwärts-/Seitwärtstasten, Faktor null, endliche Koordinaten und Stillstand
über 60 Ticks mit drei tatsächlichen Predictions beobachtet; hier ist Bewegung
gerade nicht das erwartete Ergebnis. Der Wasserfall hat maximalen Offset
7.04e-15. Die vollständige 1.19.4-Runde prüft denselben Ausrüstungsfall trocken.
Das Doppeltippen wird durch zwei Ticks W, zwei Ticks Pause und erneutes W erzeugt;
bei acht Ticks Pause ist das Zeitfenster abgelaufen. Zwei Ticks Shift in der Pause
unterbrechen den Sprintbeginn ebenfalls. Original und ViaForge zeigen dieselben
Sprintentscheidungen und jeweils genau ein Start-/Stop-Aktionspaar.

## Nicht bestandene und historische Läufe

* Ursprüngliches 26.2-Landverhalten: 54 Simulation-Flags, acht von 30 Abschnitten
  fehlgeschlagen, keine Survival-Setbacks. Bericht
  `sneak-26.2-initial-land-1790122339052591100`.
* Erste Attributerweiterung auf 26.2: 81 Flags, 41 Survival-Setbacks; eigener
  Fehler bei Sneak-Attribut/Swift Sneak und zu kurzer Kriechtunnel-Auslauf.
  Bericht `sneak-26.2-extended-fixture2-1790176199646990800`.
* Original-Vorläufe `original-extended` und `original-fixture2` hatten keine
  Grim-Flags, scheiterten aber an unzureichenden Predictions, einem fehlerhaften
  Wasseraufbau bzw. zu kurzer Tunnelzeit. Sie sind keine vollständigen PASS.
* Erster erweiterter 1.19.4-Lauf: 60/61, insgesamt 59 Flags. Swift Sneak blieb
  Faktor 0.3; der Ausrüstungsabschnitt stand versehentlich noch im Wasser.
  Bericht `sneak-1.19.4-boundary-final-1790178860084194500`.
* Gesonderter trockener Nachweis: 58 Flags im Swift-Sneak-Abschnitt, maximal
  0.008843996707975213; tatsächlich empfangenes NBT aufgezeichnet.
  Bericht `sneak-1.19.4-swift-dry-before-1790179270289381600`.
* Gesicherte Ausgangs-JAR auf 1.14.4: **keine** Survival-Flags/Setbacks in 30
  Abschnitten, aber 29/30 Verhaltensprüfungen. Der etablierte Sprint endet beim
  Sneaken (0 statt 60 Sprintticks). Bericht
  `sneak-1.14.4-pre-sneak-land-1790180576389642800`. Dies belegt eine ältere
  Verhaltensabweichung, ausdrücklich keine reproduzierten Simulation-Flags
  auf 1.14.4. Die korrigierte JAR besteht denselben Fall mit 60 Sprintticks.

Diese Berichte bleiben erhalten. Die ursprünglichen 143 Schwimmfälle und 28
Originalvergleichsfälle aus SWIMMING-TESTS.md sind historische Ergebnisse;
frische Schwimmregressionen werden gesondert ausgewiesen. 1.18.2 ist durch die
dokumentierte Forge-Sperre weiterhin kein Bewegungs-PASS. Ein Live-Originalvergleich
anderer Versionen als 26.2 wird nicht behauptet.

## Frische Schwimmregressionen

Nach den Eingabekorrekturen erneut mit echtem ViaForge-Client und aktivem Grim:

| Ziel | Abschnitte | Predictions | Größter Messoffset | Login-Korrekturen |
|---|---:|---:|---:|---:|
| 1.13.2 | 23/23 | 1136 | 9.72e-15 | 0 |
| 1.14.4 | 23/23 | 1156 | 4.04e-9 | 2 |
| 1.21.5 | 23/23 | 1278 | 3.28e-9 | 3 |
| 26.2 | 28/28 | 1574 | 4.11e-8 | 2 |

Alle 97 Abschnitte einschließlich Reset-/Ausrüstungsphasen ohne Survival-Flags
oder -Setbacks. Die 23 Grundfälle enthalten Auf-/Abtauchen, diagonales Schwimmen,
Sneak bei etabliertem Schwimmen, Ausrollen, Tiefenläufer, Delfineffekt, Aquisator
und Blasensäulen. 26.2 ergänzt engen Durchgang, Beibehalten/Aufheben der flachen
Haltung und Austritt ans trockene Ufer. Exakte Ordner:

```
swim-1.13.2-sneak-regression-final-1790180782224113700
swim-1.14.4-sneak-regression-final-1790180942263600800
swim-1.21.5-sneak-regression-final-1790181132637589700
swim-26.2-sneak-regression-final-1790181310801356000
```

## Abschlussprüfungen und Release

Frischer Gesamtbuild: 132 JUnit-Tests ohne Fehler oder übersprungene Tests,
91 Python-Tests ohne Fehler. Der vollständige Forge-Smoke über 49 Ressourcenprofile
ist am 23.09.2026 um 18:44:38 abgeschlossen; der neue Bericht
`build/logs/sneak-client-smoke-test-final.txt` beginnt mit **PASS**. Gradle bestätigt
ausdrücklich den frischen Bericht und `BUILD SUCCESSFUL`. Dieser lokale Smoke
prüft Ressourcen, Darstellung, Eingaben und Regressionen; er erweitert nicht die
hier ausgewiesene Grim-Livematrix auf 49 Versionen.

Die ausgewiesenen
Abschlussläufe umfassen 280 Sneak-/Sprintfälle und 97 Schwimmregressionen mit
ViaForge sowie 68 vergleichbare Originalclient-26.2-Fälle. Frühere Zwischenläufe
werden nicht zusätzlich zu diesen 377 bzw. 68 Fällen gezählt.
Der kompakte Export [SNEAK-EVIDENCE.json](SNEAK-EVIDENCE.json) enthält 27 erhaltene
Berichte einschließlich gescheiterter Vorläufe: Hashes der Originalberichte,
Clientartefakte, Bedingungen und Ergebnisse jedes Abschnitts.

Release: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, passende Quellen:
`build/libs/ViaForge-1.8.9-4.4.0-client.1-sources.zip`. Größen, SHA-256,
Quellenabgleich und Testnachweise stehen in `build/libs/sneak-release.json`;
daneben liegen die jeweiligen `.sha256`-Dateien. Die Release enthält keine
Entwicklungsbeobachter. Die frühere Release samt Quellen und Nachweisen bleibt
unter `build/backups/sneak-20260923-initial/` erhalten. Änderungen sind noch
nicht committed; Ausgangs-HEAD bleibt `d1d531c585689e14b6a00f8560a612d214ca07f6`.
