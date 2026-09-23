# Sneaken und Sprinten: Diagnose vom 23.09.2026

Diese Untersuchung betrifft die neu gemeldeten Simulation-Flags auf 26.2 beim
gelegentlichen Sneaken während des Laufens und Springens. Der Spielmodus der
Nutzermeldung ist nicht bekannt. Frühere Schwimm-PASS-Berichte deckten diese
Landfolgen nicht ab. Die konkrete Versions-/Fallmatrix steht in
[SNEAK-TESTS.md](SNEAK-TESTS.md); Arbeitsstände und Unterbrechungen sind in
[SNEAK-WORK.md](SNEAK-WORK.md) dokumentiert.

## Nachgewiesene Ursache

Die Produktionsphysik von HEAD `d1d531c585689e14b6a00f8560a612d214ca07f6`
erzeugte auf trockenem Steinboden mit echtem ViaForge-Client 54 Simulation-Flags
in 30 Survival-Abschnitten. Ein neuer Nicht-OP-Account war ohne Bypassrechte mit
Grim 2.3.74-8eb5f28 verbunden; LabAC meldete vor und nach jedem Abschnitt ON,
Protokoll 776, Marke `fml,forge`, Spielmodus SURVIVAL. Server:
Paper 26.2 Build 126. Kontrollen Gehen und Sprinten ohne Sneak waren sauber.

Konkrete Folgen bei Blickrichtung +Z, ohne Gegenstände oder Verzauberungen:

| Folge | Fehler mit Ausgangs-JAR |
|---|---|
| W+Shift 3 s, dann Shift lösen und W 2 s halten | 7 Simulation-Flags; maximal 0.06859998068882914 |
| W+Sprint 2 s, danach zusätzlich Shift 3 s | 4 Flags; maximal 0.009799959468125202 |
| W+Sprint+Sprung, danach zeitweise zusätzlich Shift | 8 Flags im gemessenen Sneak-Sprungabschnitt |

Bericht: `run/test-servers/sneak-26.2-initial-land-1790122339052591100/report.json`.
Die unveränderte Produktionsphysik mit reinem Eingabebeobachter ist unter
`build/backups/sneak-20260923-initial/ViaForge-initial-with-probe.jar` erhalten.
Der Originalclient 26.2 bestand dieselben 30 Abschnitte ohne Flags oder Setbacks.

Die alte 1.8-Sprintentscheidung blieb außerhalb der Wasser-Sonderpfade aktiv.
Außerdem verlangsamte die native Tastatur bereits anhand der aktuellen Shift-
Taste, obwohl moderne Zielclients eine vor der Tastenabtastung berechnete Haltung
verwenden. Eine weitere Kriechverlangsamung konnte zusätzlich eingreifen.
Die seit 1.21.5 geltende Eingabegeometrie wurde bisher nur im Wasser angewandt.

Die Korrektur verwendet den gemeinsamen Eingabeweg für Land und Wasser:
vorherige Eingabe/Haltung erfassen, Tastatur einmal abtasten, versionsabhängig
verlangsamen, Sprint entscheiden, Bewegung ausführen, anschließend Pose/Box
aktualisieren. Ab 1.21.5 erfolgt die Verlangsamung nach der Sprintentscheidung
im Bewegungsinput. Es gibt keine zusätzlichen Physikticks, Positionskorrekturen,
abgeschwächten Grim-Checks oder unterdrückten Fehlermeldungen.

Ein zweiter echter Fehler wurde mit `sneaking_speed=0.65` und Swift Sneak III
nachgewiesen: ViaForge bewegte sich weiterhin mit Faktor 0.3. Die synchronisierten
Attribute werden jetzt vor der verlustbehafteten Via-Grenze erhalten. Vor 1.21
wird die im eigenen `ViaForge|flattenedItem` gesicherte Originalverzauberung gelesen, ohne eine
zustandsbehaftete Itemübersetzung erneut auszuführen. Der gültige Attributwert
null braucht außerdem den originalen Nullvektor-Abbruch vor der Normalisierung;
ohne ihn lieferte die bisherige Rechenfunktion NaN.

Die erste Annahme eines `VB|Protocol1_19To1_18_2|Enchantments`-Backups war im
tatsächlichen Itempfad falsch. Ein zusätzlicher trockener 1.19.4-Lauf belegte
58 gemessene Flags mit Swift Sneak III. Sein empfangenes NBT enthält stattdessen
`ViaForge|flattenedItem.tag.Enchantments`. Nach der gezielten Änderung besteht
die gesamte erweiterte 1.19.4-Runde einschließlich dieses trockenen Falls;
der zuerst entdeckte Wasserfall wurde ebenfalls frisch wiederholt und besteht.

## Originalregeln und Versionsgrenzen

| Ziel | Relevante Originalregel |
|---|---|
| 1.13–1.13.2 | Aktuelle Shift-Taste verlangsamt; die verringerte Vorwärtseingabe beendet Landsprint. |
| 1.14–1.14.4 | Vorherige Haltung **oder** aktuelle Shift-Taste verlangsamt. Bereits laufender Sprint benötigt nur positive Vorwärtseingabe und kann beim Sneaken bestehen bleiben. |
| ab 1.15 | Die zusätzliche Abfrage der aktuellen Shift-Taste entfällt. Gleichzeitiger Beginn von W+Sprint+Shift kann Sprint starten, bevor die Eingabe im nächsten Tick langsamer wird. |
| ab 1.19 | Swift Sneak verändert den Faktor: `clamp(0.3F + Stufe * 0.15F, 0, 1)`. |
| ab 1.21 | Maßgeblich ist das synchronisierte Attribut `sneaking_speed`, nicht eine vorweggenommene Ausrüstungsänderung. |
| ab 1.21.5 | Tastaturvektor normalisieren; Sprint anhand dieser Eingabe entscheiden; danach 0.98, Item-/Sneak-Faktor und quadratische Eingabegeometrie anwenden. |

„Sneaksprinten“ bezeichnet damit keinen pauschalen Fehler. Auf 26.2 bleibt ein
etablierter Sprint beim Sneaken erhalten. Aus bereits verlangsamtem Sneaken
startet er auf trockenem Boden nicht. Das Sprintattribut bleibt beim etablierten
Sprint entsprechend aktiv; eine reine Animation oder ein gehaltenes Sprint-Key
ist kein Nachweis des Sprintstatus.

Auf dem Loslass-Tick von Shift lautet der tatsächliche 26.2-Ablauf:

| Zeitpunkt | Shift | Bewegung vorwärts | Boxhöhe |
|---|---:|---:|---:|
| Eingabe abgetastet / Physik | aus, vorher an | 0.29400003 | 1.5 |
| Pose am Tickende | aus | 0.29400003 | 1.8 |
| Physik im nächsten Tick | aus, vorher aus | 0.98 | 1.8 |

Niedrige Decken können Haltung und Eingabeverlangsamung ohne gehaltene Shift-
Taste erzwingen. Der Test prüft unter einer oberen Stufe 1.5 Höhe sowie nach
einem Wasserübergang trockenes Kriechen mit 0.6 Höhe und späteres Stehen mit 1.8.
Augenhöhen und Kamera/Modell folgen den bestehenden versionsabhängigen Pfaden.

Originalherkunft: `build/inspection/sneak/source-verification.json` enthält
offizielle Mojang-Metadaten-URLs und verifizierte Client-/Mapping-SHA-1 für die
untersuchten Grenzen. Dekompilate liegen unter `build/inspection/sneak/`, 26.2
zusätzlich unter `build/inspection/push/26.2/` und `swimming/26.2/`.
Der lokale Grim-Quellstand entspricht dem installierten Build:
`8eb5f2809591c891deb4958bb2927844871e0600`.

## Reproduzierbare Prüfung

```powershell
py -3 tools/test-servers/sneak_probe.py --version 26.2 --extended --label review
py -3 tools/test-servers/sneak_probe.py --version 26.2 --client native --extended --label original-review
py -3 tools/test-servers/sneak_probe.py --version 26.2 --attributes-only --label attributes-review
py -3 tools/test-servers/sneak_probe.py --version 1.19.4 --swift-only --label swift-review
py -3 tools/test-servers/sneak_probe.py --version 1.19.4 --swift-water-only --label swift-water-review
```

Jeder Lauf verweigert den Start bei einem vorhandenen Minecraft-Client. Er nutzt
eine eigene kleine Instanz auf 127.0.0.1, sichert die Welt vor dem Aufbau und
erhält anschließend Welt, Logs, Clientdaten, Artefakthashes und Bericht. Normale
Profile und gespeicherte Nutzerspielmodi bleiben erhalten. Neue Accounts starten
Creative, wechseln für die Wertung ausdrücklich nach Survival; Creative wird
am Ende getrennt geprüft. Grim-Konfiguration und EULA werden nicht abgeschwächt.

Die Grundmatrix umfasst 30 Abschnitte: Gehen/Sprinten als Kontrolle, Sneaken im
Stand und beim Gehen, beide Reihenfolgen mit Sprint, gleichzeitiger Start und
Stopp, diagonale und rückwärtige Eingaben, Sprünge/Landungen sowie Ausrollen.
Die erweiterte 26.2-Matrix ergänzt einzelne Sneak-Ticks, schnelle Wechsel,
Wandkontakt, Stufen/Treppen, Kanten, erzwungene Haltung, Wasserübergänge, Attribute
und Swift Sneak. Die Randwertmatrix prüft Attribute 0, 1 und Rückkehr zu 0.3.

`client.jsonl` erfasst echte Tasteneingaben, Haltung, Box, Augen, Geschwindigkeit,
Attribute und Kollisionen. `movement.jsonl` ergänzt die Phasen innerhalb des
Ticks. `plugins/ViaForgeLabAC/events.jsonl` enthält empfangene Eingabe-/Sprint- und
Bewegungspakete, Grim-Predictions mit Offsets, Flags und Setbacks. Beobachter
ersetzen keine Physik oder Bewegungspakete und sind nicht Teil der Release-JAR.
Bewertet werden auch Resets, Ausrüstung und Intervalle zwischen den Fällen.
Login-Korrekturen werden separat ausgewiesen, nie als Bewegungs-PASS versteckt.
Im erweiterten 26.2-Vergleich wurden pro ViaForge-Tick genau ein Eingabedurchlauf
und ein Physikbeginn beobachtet (5.042 Ticks, keine doppelten Phasen). Der
separate PHYSICS_END-Hook entfällt bei der bestehenden Wasser-Ersetzung;
deren Endzustand ist im anschließenden POSE_END und vor Paketversand erfasst.
Original und ViaForge sendeten jeweils 28 Sprintaktionen ohne doppelte Starts
oder Stops. Gezielte Testteleports bleiben in den Rohprotokollen enthalten;
die ausgewiesenen Setbacks stammen von Grim.

## Grenzen

Ein bestandener Fall belegt seine konkrete Eingabe, Version und Umgebung. Er ist
kein Nachweis sämtlicher Bewegungen auf allen 49 Profilen. Originalclients älterer
Versionen wurden bisher anhand ihrer Originalimplementierung untersucht; ein
Live-Vergleich wird nur für tatsächlich ausgeführte Originalclients ausgewiesen.
Die bekannte Forge-Sperre auf 1.18.2 wird weder umgangen noch als Bewegungs-PASS
gezählt. Historische Login-/Timer-/Kollisionsbefunde bleiben getrennt bestehen.
Sonderfälle wie moderne Item-Mobilitätskomponenten und streifende Wandkollisionen
sind durch die vorliegende Matrix nicht vollständig abgedeckt.
