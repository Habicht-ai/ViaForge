# Elytra, Slime und Shulkerdeckel – laufende Untersuchung

Ausgangsstand am 24.09.2026: `02001c8477a7e7da92d89eea83331c266507036f`,
sauberer Arbeitsbaum. Nutzer meldet Elytra-Rücksetzungen bei Blick ganz nach
oben auf 26.2, mit und ohne Raketen; außerdem schlechte Bewegung auf Slime und
fehlendes weiches Verschieben durch öffnende Shulkerboxen. Nutzer präzisierte:
Elytra bisher nur ViaForge; Slime/Shulker mit ausgewählter 26.2 auf tatsächlichem
1.21.11-Server mit ViaVersion. Direkter 26.2-Server wird getrennt geprüft.

Die normale Webverwaltung (PID 38200), ihr Worker (41368) und Paper 26.2
(41360, 127.0.0.1:27084/27085) bleiben unangetastet. Tatsächlich abgefragt:
Grim 2.3.74-8eb5f28 ON, keine Spieler. Beim Einstieg kein laufender Client.
PIDs sind eine Momentaufnahme. HEAD enthält die vorherigen Interaktionsfixes.

Vor Teststarts gesichert: Benutzerlog, Optionen, ViaForge-Konfiguration,
Entwicklungs-JAR, Release, Quellen und Release-Nachweis unter
`build/backups/flight-surfaces-20260924-initial/`; Hashliste `artifacts.json`.
Das Nutzerlog enthält 31 Simulation-Meldungen um 21:11:55–21:11:59;
die einzelnen Meldungen werden ohne weitere Belege keinem Szenario zugeordnet.

## Aktuell

Versionsabhängige Produktionskorrekturen bauen mit 136 bestandenen JUnit-Tests,
sind noch nicht live validiert: originale Blickarithmetik/Flugberechnung, Slime-Schritteffekt nach
Travel ab 1.21.2 und Kollisionsanteil beim Abprallen ab 26.2, Shulker-Schub
(anderer Schub ab 1.17). Die Kollisionsbox war durch die Basisklasse bereits
beweglich; der zunächst verdächtigte Override war nicht die Ursache.
Die echten Eingabeproben wurden
um kameragesteuerten Blick −90°/−89,9°, Schließen des Containers und Beobachtung
von Flugflag, Boosts und Shulkerfortschritt erweitert. Keine ersetzte Physik,
keine synthetischen Bewegungspakete. Neuer Ablauf: `surface_probe.py`.
Die Vorher-JAR mit ausschließlich Probeergänzungen liegt als
`ViaForge-before-with-probe.jar` im Sicherungsordner.

Erster echter Lauf:
`run/test-servers/interaction-26.2-surfaces-before-1790277572143523300/`;
Konsole `build/logs/surfaces-26.2-before.txt`. Ein einzelner isolierter
Loopback-Server, eigene Welt vor Aufbau gesichert, neue Nicht-OP-Spieler;
Creative nur zum Laden, alle bewerteten Fälle Survival. Normale Laborwelten
und Verwaltungsprofile werden nicht verändert.

Originalquellen werden mit Mojangs Downloadhashes verifiziert und unter
`build/inspection/flight-surfaces/` dekompiliert. Bestätigte Codebefunde:

- Die vorhandene Shulker-TileEntity animiert nur den Deckel. Ihr lokaler
  Verdrängungsschritt fehlt. Die Box wird schon durch `Cube` aktualisiert.
- Original 26.2 verwendet andere Blickvektor-/Trigonometrie-Arithmetik als
  der 1.8-Client. Ob dies die konkrete Elytra-Meldung erklärt, ist noch offen.
- Original 26.2 berechnet Abprallen mit Kollisionsanteil, Gravitation und
  Luftreibung. Slime-Schritteffekte laufen nach dem Reiseabschnitt. Grenzen
  zu älteren Originalclients werden gerade untersucht.

Vorherlauf direkt 26.2: Slime-Abprallen erzeugt 10 Simulation-Meldungen
einschließlich Ausrollen; Shulker oben Phase, seitlich nur ein später Sprung
um 1,1 Blöcke statt zehn Originalschritten. Vertikaler Elytraflug erzeugt
Simulation und Rücksetzungen. Späte Flugfälle verlassen den gültigen Aufbau
und zählen nicht als saubere Vergleichsfälle.

Originallauf `interaction-26.2-surfaces-original2-1790277893757442300`:
keine Survival-Flags/Rücksetzungen. Seitlich zehn Schritte von rund 0,11;
auch das Original bewegt sich auf dem nach oben öffnenden Deckel ungleichmäßig.
Fehlende Untergrundtelemetrie und zu lange Flugsequenz beschränken die bisherige
Fallwertung. Der vorangegangene `original`-Lauf brach wegen einer falschen
Observer-Feldannahme ab und liefert keinen Bewegungsnachweis.

Fortsetzung: Untergrundbeobachtung ergänzt, seitliche Schubkontinuität geprüft,
Flugfälle höher und kürzer angelegt. Original 26.2 → 1.21.11 mit ViaVersion 5.12.0:
`interaction-26.2-surfaces-backend-original2-1790280735906066400` 17/17,
keine Survival-/Loginprobleme. Direkter Original-26.2-Lauf:
`interaction-26.2-surfaces-original-final-1790280971286619800` 17/17,
keine Survivalprobleme, zwei Login-Korrekturen. Beide benutzen noch den offenen
Landepool; dieser wird für identische endgültige Vergleiche eingefasst, weil
das Wasser sonst in den trockenen Shulker-/Ausrollbereich läuft.
`surfaces-backend-before-1790281183185314700` läuft jetzt mit eingefasstem Pool
und alter Produktion, Observer-v2-Archiv mit unveränderter Produktion
byteweise geprüft (`baseline-v2.json`). Vollbuild 2 erfolgreich; Forge-Smoke
und echte Fixläufe stehen noch aus. 98 bisherige Python-Tests sowie die drei
neuen Oberflächen-Evidenztests bestehen; Gesamtlauf wird abschließend wiederholt.
Nächste Schritte: unveränderte Produktionsbasis mit denselben Folgen prüfen,
Fix frisch wiederholen, gezielte Versionsgrenzen/Regressionen, Release/Quellen.
Ein früheres PASS ist kein Nachweis für diese neuen Folgen.

## Fortsetzung 22:31

Vorher auf Nutzerkombination: `surfaces-backend-before-1790281183185314700`:
10/17; Survival 33 Simulation, 1 Phase, 47 Rücksetzungen. Zehn Simulation
kommen aus Slime/Coast, seitliche Deckel scheitern an fehlenden Schubschritten.
Erster Fix `surfaces-backend-fixed1-1790281416711498800`: 15/17,
Slime, Shulker und Flug ohne Raketen sauber. 16 Simulation beim zweiten
überlappenden Rocketboost; Endgeschwindigkeit stimmt mit Original, aber ViaForge
bewegt vorzeitig mit dem bereits geboosteten Vektor. Korrektur verschiebt die
Acceleration aus `ServerElytraFlight.move` in den echten `EntityFireworkRocket`
Tick (`ServerFireworks.move`). Original 1.12.2 bestätigt dieselbe Reihenfolge.
Neue Forge-Regression prüft Velocityänderung ohne zusätzliche Spielerbewegung
und zwei einzelne Raketenticks.

Forge-Smoke vor dieser zusätzlichen Boostkorrektur vollständig PASS:
`build/logs/surfaces-client-smoke-20260924.txt`; muss danach frisch wiederholt
werden. `surfaces-backend-fixed2` läuft aktuell; Raketenstart jetzt y=180,
damit alle Messpunkte unter der 1.8-BlockLoaded-Grenze 256 bleiben. Das bisherige
y=240 führte trotz lebendigem Spieler zu ungültiger Loaded-Telemetrie.
Kein Releaseabschluss bisher. Normalserver weiter unverändert.

## Bestätigter Fix 22:37

`surfaces-backend-fixed2-1790281989851895000`: 17/17 PASS, null Survival-Flags
oder Rücksetzungen (zwei Login-Korrekturen). Slime rebound max Offset
1.2e-14, alle Elytraabschnitte etwa 1e-14 bis 2.4e-14. Shulker seitlich zehn
Schritte zu 0,11. Tatsächlich Grim: 26.2 / 776, fml,forge, Survival, Nicht-OP,
alle Bypassrechte false, Verbose ON.

Zusätzlich wird ein 18. Fall oberhalb Y=256 geprüft. ViaForge beobachtet dafür
die wirklich geladene Chunkspalte bei y=63; die native 1.8-isBlockLoaded-Abfrage
auf Spielerhöhe verwechselt y>=256 mit fehlendem Chunk. Original-Observer prüft
ebenfalls hasChunkAt. Keine Änderung der Physik für diese Messung.
`surfaces-backend-native-final-1790282245963358300` läuft; Build 3 ergänzt die
neue Telemetrie. Python-Gesamtlauf `surfaces-python-final2.txt`: 102 PASS.
Noch ausstehend: identischer finaler Baseline-/Fixvergleich mit 18 Fällen,
direkter 26.2-Fixlauf, ältere Originalversionsgrenzen, erneuter Forge-Smoke nach
Boostkorrektur, finaler Release-/Quellen-Nachweis.

## Original-Resetgrenze 22:42

Original `surfaces-backend-native-final-1790282245963358300`: 18/18 Messfälle,
aber Gesamtlauf FAIL: der Wasserteleport nach dem hohen Raketenflug mit noch
aktivem Boost erzeugt zwei Simulation (0.058800/0.116424), einen Setback.
Kein ViaForge-Ursachennachweis. Finale Probe wartet drei Sekunden im Flug auf
Boostablauf; diese Phase wird mit ausgewertet.
Original `surfaces-backend-native-final2-1790282527160850600` läuft aktuell.
`build/surface-final-suite.py` wartet auf dessen Abschluss/PASS und führt dann
sechs isolierte Läufe nacheinander aus (Baseline/Fixed auf Backend, direkt26.2,
1.12.2,1.17.1,1.21.11). Konsole `surfaces-final-suite2.txt`, Ergebnisliste
`surfaces-final-suite.json`. Laufzellen: native 62141, Suite 60077.
Baseline-v3 hat die neue Spaltenbeobachtung, sonst bytegleich alte Produktion.
Build3 erfolgreich; 136 JUnit, 102 Python. Finaler Forge-Smoke noch ausstehend.

Original `surfaces-backend-native-final2-1790282527160850600` abgeschlossen:
18/18 PASS, null Survival-/Loginprobleme. Finale Suite läuft ab
`surfaces-before-final-1790282759993938600` (Baseline). Neue Spalten-Telemetrie
und Raketenablaufphase sind hier enthalten. Produktionscode seit Fix2 unverändert;
zusätzlich nur die explizite Oberflächenzeile im Forge-Smoke-Bericht ergänzt.

Nachprüfung der geerbten 26.3-Regel am hashgeprüften Original ergab eine weitere
Grenze: Abprallunterdrückung dort `-vy <= gravity`, in 26.2 noch `<`. Neue Regel
`SUPPRESS_GRAVITY_EQUAL_BOUNCE` nur für 777 und echte Kollisionsregression
ergänzt. Änderungen wirken nicht auf die laufenden 26.2-Fälle. 26.3 bekommt
weiterhin keinen behaupteten Live-Grim-PASS. Quellmanifest um 26.3 ergänzt.

## Fortsetzung nach Nutzerhinweis

Finale Fixläufe abgeschlossen: `surfaces-fixed-final-1790283006965574800`
auf Backend1.21.11 und `surfaces-direct-final-1790283282850949100` direkt26.2:
jeweils 18/18 PASS, keine Survivalprobleme, jeweils zwei Login-Korrekturen.
Finale Baseline `surfaces-before-final-1790282759993938600`: 11/18,
36 Simulation, 1 Phase, 51 Setbacks in Survival.

Suite stoppte korrekt bei `interaction-1.12.2-surfaces-boundary-final-1790283514650868900`.
Keine Grim-Flags, aber Aufbau ungültig: vor Login fehlen geladene Chunks;
alle Fill-Befehle melden `Cannot place blocks outside of the world`.
Nicht als Oberflächen-/Flug-PASS zählen. Fixture für <1.13 jetzt nach Creative-
Chunkladen, Fehlerausgabe hart geprüft. `surfaces-boundary-final2` 1.12.2 läuft
(Zelle37871; Log `surfaces-1.12.2-final2.txt`). Danach noch1.17.1/1.21.11,
finaler Forge-Smoke/Build/Release-Nachweis. Suite60077 ist beendet.

1.12.2 final2 brach beim atomaren Reportersatz mit WinError5 ab (gleichzeitiger
Leser). Drei bis dahin vollständige Fälle PASS, Rest ungetestet; sauber beendet,
kein vollständiger PASS. Keine Änderung an `lab.py`; abschließende Reports erst
nach Prozessabschluss lesen. `build/surface-boundary-suite.py` Zelle51101 prüft
jetzt1.17.1, danach1.21.11 (`surfaces-boundary-suite.txt`). Danach1.12.2 erneut
ohne laufenden Reportleser. Python-Release-Lauf103TestsPASS.

## Fortsetzung 23:14

1.17.1 (`surfaces-boundary-final-1790283974120657900`) und 1.21.11
(`surfaces-boundary-final-1790284197477572800`) jeweils 18/18 PASS, keine
Survival-Flags/Setbacks einschließlich Resetphasen; je zwei Login-Korrekturen.
Suite51101 beendet. Erneuter 1.12.2-Lauf `surfaces-boundary-final3-1790284468092213700`
läuft, Zelle36602, Konsole `build/logs/surfaces-1.12.2-final3.txt`. Dessen
report.json erst nach Abschluss lesen. Normalserver Java41360 unverändert.
Danach noch frischer vollständiger Forge-Smoke nach Raketen-/26.3-Korrektur,
finaler Build und Release-/Quellarchivnachweis. ELYTRA.md beschreibt jetzt
den tatsächlichen Raketen-Entitätstick und verweist auf die neuen Ergebnisse.

## Abschlussläufe 23:18

1.12.2 final3 beendet: 18/18 PASS, null Survival- und Loginprobleme.
Damit final90ViaForge-Fälle und18Originalfälle PASS, alle Survivalresetphasen
ohne Flags/Setbacks. Acht Login-Korrekturen in den vier neueren ViaForge-
Läufen bleiben separat. Forge-Gesamtprüfung läuft in Zelle62261:
`build.bat build runClient -x preRunClient`, Berichtspfad vor Start noch nicht
vorhanden: `build/logs/surfaces-client-smoke-final.txt`, Konsole
`build/logs/surfaces-forge-final.txt`. Kein Benutzerclient vor Start vorhanden.
Anschließend Abschlussprüfung der Artefakte und Prüfsummen; kein Commit erzeugt.

## Finale Prüfung 23:21

Forge-Zelle62261 erfolgreich beendet. Frischer Bericht beginnt mit PASS,
49 Ressourcenprofile, einschließlich neuer Surface-/Raketenregression.
136 JUnit frisch um23:18 bestanden; 103 Python bestanden. Alle Testclients und
isolierten Server beendet. Nur normale Verwaltung38200/41368 und unveränderter
26.2-Server41360 laufen; Grim ON,0Spieler tatsächlich erneut bestätigt.
Abschlussdokumentation in FLIGHT-SURFACES.md. Releasebuild um23:21 erfolgreich;
585 Dateien des Quellenarchivs mit `build/flight-surfaces-release-proof.py`
byteweise geprüft. Größen, SHA-256 und sämtliche Abschlussberichte stehen in
`build/libs/flight-surfaces-release.json`. Das Quellenarchiv wird nach dieser
letzten Protokollaktualisierung nochmals abgeglichen. Alte Artefakte bleiben gesichert.
Offen bleiben die dokumentierten Grenzen: Originalclient-Wasserteleport mit
aktivem Boost, separate Login-Korrekturen, nicht geprüfte Attributmodifikatoren
und Hinderniskombinationen, kein Live-Grim-Nachweis für26.3. Kein Commit.
