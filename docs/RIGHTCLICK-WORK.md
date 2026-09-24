# Rechtsklick und Schwertpose, 24.09.2026

Nutzer meldet BadPacketsJ beim Rechtsklick auf Blöcke mit Ziel 26.2 und eine
falsche Schwertblockpose. Aktuelles `run/logs/latest.log` enthält entsprechende
Meldungen u.a. 00:21:39–00:22:22. Benutzerlog, Konfiguration, Release und Quellen
wurden vor Teststarts unter `build/backups/rightclick-20260924-initial/` gesichert.
HEAD unverändert `ab744f6`; vorhandene uncommittete Korrekturen bleiben erhalten.
Beim Einstieg liefen keine Java-/Python-Prozesse.

## Nachweise

Installierter Grim unverändert `2.3.74-8eb5f28`. BadPacketsJ prüft seit
Client-/Serverversion 1.21 die Übereinstimmung zwischen USE_ITEM-Blickrichtung
und Tickrotation. ViaBackwards 5.12.0 ergänzt USE_ITEM bisher aus seiner letzten
Bewegungsrotation. Original 26.2 erfasst dagegen im `MultiPlayerGameMode.useItem`
die aktuelle Spielerrotation. Dekompilate der hashgeprüften Original-JAR und
der tatsächlich gebündelten ViaBackwards-Klassen: `build/inspection/rightclick/`.

Erste echte Reproduktion:
`interaction-26.2-rightclick-before-1790261426349342300`.
Bei Timestamp 1790261558.187 sendet USE_ITEM Yaw 37, Pitch 60; das Bewegungspaket
des Ticks enthält bereits Yaw 38.5. BadPacketsJ folgt. Gehaltenes Rechtsklicken
mit Stick auf Boden und in Luft beim Drehen reproduziert den Fehler.
Dieser erste Lauf ist insgesamt FAIL: zusätzliche Ausrüstungsfehler im neuen
Harness (64 Schwerter/Schilde statt zulässiger Einzelstücke) machen die so
benannten Schwert-/Schildfälle ungültig. Keine Wertung dieser Fälle als Beleg.
Der Harness verwendet jetzt korrekte Stückzahlen und prüft serverseitig den
tatsächlichen Itemtyp vor jedem Fall. Alte Rohberichte bleiben unverändert.

Der erste Originalvergleich `interaction-26.2-rightclick-original-1790261658564914500`
scheiterte außerdem am neuen reinen Screenshot-Beobachter: 26.2 besitzt
`gameRenderer.mainRenderTarget()`, nicht `Minecraft.getMainRenderTarget()`.
Beobachter korrigiert; der Lauf ist kein vollständiger Vergleichsnachweis.

## Korrektur und laufende Prüfungen

Der interne Hand-Befehl erhält die Rotation zum Klickzeitpunkt. Sie gilt nur
während genau einer synchronen Via-Übersetzung und ersetzt an der 1.21-Grenze
die ergänzten USE_ITEM-Winkel. Kein zusätzliches Bewegungspaket, keine
zusätzlichen Physikticks und keine Änderungen an Grim/Schwellen/Filtern.
Pipelinesmoke prüft unterschiedliche Winkel beider Hände einschließlich einer
Kameraänderung zwischen Erzeugung und Übersetzung sowie Aufräumen des Kontextes.

Original 26.2 verschiebt Nicht-Schild-BLOCK um (-0.14142136, 0.08, 0.14142136)
und dreht X=-102.25°, Y=13.365°, Z=78.05° (links entsprechend gespiegelt).
Der bisherige Einzelhandpfad verwendete die 1.8-Pose; dem Zweihandpfad fehlte
die zusätzliche Transformation. Beide Pfade übernehmen nun die Originalpose
für die bereits unterstützten Blockkomponentenschwerter ab 1.21.5.
Matrixregression vergleicht die tatsächlichen Renderpfade mit diesen
Originaltransformationen; Live-Screenshots ergänzen die Prüfung.

Protokolle der ersten Korrektur: `build/logs/rightclick-original-verified.txt`
für den korrigierten Originalvergleich und `build/logs/rightclick-build-first.txt`
für den ersten Build. Es folgten ein frischer Lauf mit unverändertem Vorherclient
und gültigem Aufbau, korrigierter ViaForge-Client und Versionsgrenze.
Der Vorherclient mit ausschließlich Kamera-/Beobachterergänzung liegt unter
`build/backups/rightclick-20260924-initial/ViaForge-before-with-camera.jar`.

## Bestätigter Vergleich

`interaction-26.2-rightclick-original-verified-1790261823871004000`: **9/9 PASS**,
202 sequenzierte Pakete korrekt, keine Survival-Flags/Setbacks. Je eine Timer-
und TimerLimit-Meldung beim Login bleibt getrennt dokumentiert.

`interaction-26.2-rightclick-before-verified-1790261998217167500`: **3/9**, mit
korrekter Ausrüstung **72 BadPacketsJ** (Stick Boden/Luft je 17, Schild
Boden/Luft je 8, Blöcke 14, Blockkomponentenschwert 8). Statischer Rechtsklick
und gehaltenes Blocken ohne Kamerabewegung bestehen. Kein zufälliger
Server-/Loginfehler wird als Erklärung für die reproduzierte Drehfolge benutzt.

Original-Renderer 1.21.4 (`glt`) und 1.21.5 (`grg`) wurden zusätzlich aus dem
vorhandenen Cache dekompiliert und die JAR-SHA1 gegen Mojangs Versionsmetadaten
verifiziert. Die Transformation ist dieselbe wie in 26.2. Die Unterstützung
des empfangenen `blocks_attacks`-Schwerts beginnt in ViaForge weiterhin bei
1.21.5; daraus wird keine Einführung der Rendertransformation in dieser Version
behauptet. Quellen und Hashes: `RIGHTCLICK-SOURCES.json`.

Rohberichte mit Bedingungen, Flags und Screenshots: `RIGHTCLICK-EVIDENCE.json`.
Reproduktion mit dem gesicherten Vorherclient:

```powershell
py -3 tools/test-servers/interaction_probe.py --version 26.2 --rightclick --baseline --label before
py -3 tools/test-servers/interaction_probe.py --version 26.2 --rightclick --label fixed
py -3 tools/test-servers/interaction_probe.py --version 26.2 --rightclick --client native --label original
```

## Zusätzlicher Befund bei abgelehnter Platzierung

Die ersten korrigierten Läufe waren Grim-sauber, erfüllten aber noch nicht den
Paketvergleich im Stein-Fall. Native 26.2 sendet dort 17 Haupt-Hand-USE_ON und
keine USE_ITEM. ViaForge sendete 3 Haupt-Hand-USE_ON, danach 14 Nebenhand-USE_ON
und zusätzlich 14 USE_ITEM. Dies erklärt die Differenz von 202 zu 216
Sequenzpaketen; sie war **kein** bloßer Unterschied der Messdauer.

Ursache: Der 1.8-Controller bricht bei einer unmöglichen ItemBlock-Platzierung
vor dem Senden ab. Sein boolesches false wird dann wie PASS behandelt: erst
Luftbenutzung, dann Nebenhand. Original 1.13 sendet vor der lokalen Platzierung
und beendet den Klick bei FAIL. Original 1.9 und 1.12.2 besitzen dagegen noch
den frühen Controller-Abbruch und setzen den Klick anschließend fort. Beide
Unterschiede sind in den hashgeprüften Originalclients nachgewiesen.

Die neue Regel `MODERN_BLOCK_USE_FAILURE` gilt ab 1.13. Nur der veraltete
Controller-Vorabtest wird übersprungen; ItemBlock prüft weiterhin selbst
Kollision und Platzierungsrechte. Ein fehlgeschlagener ItemBlock-Versuch
beendet danach den Klick ohne Luftbenutzung, Nebenhand oder Swing.
Der Forge-Smoke ruft den echten Minecraft-Rechtsklick-Handler auf und prüft
beide Versionszweige, ausbleibenden Blockwechsel und unveränderte Stückzahl.
Die Live-Auswertung lehnt nun auch Grim-saubere Fälle mit falscher Hand oder
zusätzlichem USE_ITEM ab. Historische Rohberichte bleiben unverändert;
`RIGHTCLICK-EVIDENCE.json` enthält die strengere Neuauswertung.

| Client / Server | Lauf | Aktuelle Wertung |
|---|---|---|
| ViaForge vorher / 26.2 | `interaction-26.2-rightclick-before-verified-1790261998217167500` | 3/9, 72 BadPacketsJ |
| Original 26.2 / 26.2 | `interaction-26.2-rightclick-original-verified-1790261823871004000` | 9/9 PASS, 202 Sequenzpakete korrekt |
| ViaForge nur Winkel/Pose korrigiert / 26.2 | `interaction-26.2-rightclick-fixed-1790262147332822100` | 8/9, keine Survival-Flags; Platzierungsablauf noch falsch |
| ViaForge nur Winkel/Pose korrigiert / 1.21.1 | `interaction-1.21.1-rightclick-boundary-1790262321042245400` | 6/7, keine Survival-Flags; Platzierungsablauf noch falsch |
| ViaForge final / 26.2 | `interaction-26.2-rightclick-final-1790266594762257300` | **9/9 PASS**, 203 Sequenzpakete korrekt |
| ViaForge final / 1.21.1 | `interaction-1.21.1-rightclick-final-1790266784496914200` | **7/7 PASS**, 177 Sequenzpakete korrekt |

Alle gewerteten Fälle bestätigen Survival, tatsächliche Tasten und
Kameradrehung, empfangene Interaktionspakete sowie Grim ON vor und nach dem
Fall ohne Bypassrechte. Zwischen-/Resetphasen zählen zur Survival-Wertung.
Original 26.2 hatte je ein Timer-/TimerLimit-Flag beim Login; der erste
korrigierte ViaForge-26.2-Lauf zwei Login-Korrekturen. Diese bleiben getrennt.
Auch der finale 26.2-Lauf hat zwei Login-Korrekturen, danach keine
Survival-Flags/Setbacks. Final 1.21.1 hat auch beim Login keine Meldungen.
Grim erkennt auf 26.2 Protokoll 776 / Client 26.2 und auf 1.21.1 Protokoll 767 /
Clientbezeichnung 1.21 (gemeinsames Protokoll), jeweils Marke `fml,forge`.

Im finalen 26.2-Lauf entsprechen die Anzahlen und Hände von USE_ON, USE_ITEM,
Swing und Loslassen in allen acht Rechtsklickfällen dem Originalvergleich.
Insbesondere: 17 Haupt-Hand-USE_ON und drei erfolgreiche Swings im Steinfall,
keine Luftbenutzung und kein Nebenhandversuch. Die zusätzliche Sequenznummer
203 gegenüber 202 beim Original entsteht nachweislich an der Zeitgrenze zum
Reset: Tick 1493 verarbeitet noch `use-turn` und sendet USE_ON um
1790266728.845; erst Tick 1494 ab 1790266728.891 verarbeitet `idle`.
Diese Phase ist ebenfalls Grim-sauber und wird nicht aus der Wertung entfernt.

Der Screenshot `viaforge/screenshots/blocking-1614.png` im ersten korrigierten
26.2-Lauf und `native/screenshots/blocking-1627.png` im Originalvergleich zeigen
dieselbe Schwertausrichtung. Rohbilder und Prüfsummen bleiben erhalten.
Ein gewöhnliches modernes Schwert ohne `blocks_attacks` blockt weiterhin
nicht; die Testkomponente wird ausdrücklich serverseitig gesetzt.
Der finale Screenshot `viaforge/screenshots/blocking-1606.png` bestätigt diese
Pose erneut. Andere Sichtweite/FOV/Wolken sind kein Pixelgleichheitsnachweis;
die GL-Matrixregression prüft separat beide Hände und Renderpfade.

## Abschluss und Artefakte

Build mit Platzierungskorrektur erfolgreich (`rightclick-placement-build.txt`),
134 JUnit-Tests; Python frisch 98/98 (`rightclick-python-final.txt`).
Der volle 49-Profil-Smoke ohne Platzierungskorrektur ist PASS
(`rightclick-client-smoke-final2.txt`). Sein erster Anlauf (`final.txt`)
scheiterte am neuen Test im Hauptmenü ohne Spieler; der Test erzeugt jetzt
einen temporären EntityPlayerSP und stellt den Zustand vor Übersetzung
wieder her. Keine Produktionsänderung für diesen Testfehler.

Der volle Smoke **mit** Platzierungskorrektur ist frisch PASS (49 Profile):
`build/logs/rightclick-client-smoke-final3.txt`, Konsole
`build/logs/rightclick-smoke-final3-console.txt`. Der Bericht beginnt mit PASS.
Frische echte Abschlussläufe stehen oben; die letzten Produktionsänderungen
sind sowohl im Smoke als auch in diesen beiden Läufen enthalten.
Beim Start liefen keine fremden Clients/Server. Der frühere Zwischenbuild ist
zusätzlich in `build/backups/rightclick-before-placement/` gesichert.

Installierter Grim: SHA-256
`91c06e7ae7da53636bc5e500d5af3d36a6180247e155fa5b4340da5a72f9eeb7`.
Unveränderte Konfiguration beider Abschlussläufe: SHA-256
`78c5c8580e0bba27fea8679866b862763363a1a283c778c5cd874741133fe20e`.
Keine Checks oder Schwellen abgeschwächt. BadPacketsJ ist aktiv; Sequenzen
werden zusätzlich direkt geprüft, da BadPacketsH im Laborstandard weiterhin
experimentell deaktiviert ist. 16 finale ViaForge-Fälle und neun passende
Originalfälle sind ein Interaktionsnachweis, kein umfassender Bewegungstest.
Creative dient hier dem Login/Aufwärmen; alle gewerteten Fälle sind Survival.

Release: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`; passende Quellen:
`build/libs/ViaForge-1.8.9-4.4.0-client.1-sources.zip`.
Der abschließende Build steht in `build/logs/rightclick-release-build.txt`.
`build/libs/rightclick-release.json` enthält Größen, SHA-256, Quellstand,
Testberichte und Archivprüfung. Frühere Manifeste beschreiben ihre damaligen
Artefakte, nicht die jetzt unter gleichem Dateinamen gebaute Release.
`build/libs/rightclick-review-inputs.json` erfasst die 455 geprüften
Produktions-/Entwicklungs-/Testdateien. HEAD bleibt `ab744f6`; kein Commit erstellt.
Nach den Abschlussläufen sind keine Java-/Python-Laborprozesse mehr aktiv;
sämtliche isolierten Testwelten, Backups und Rohberichte bleiben erhalten.

Die früheren offenen Befunde (altes ViaVersion-Backend, separate 1.9.4-Hitboxes,
1.14-Grim-ItemReset und 1.19-Forge-Sperre) bleiben bestehen; diese Korrektur ist
kein Nachweis für alle Serverplugins oder Itemkomponenten.
