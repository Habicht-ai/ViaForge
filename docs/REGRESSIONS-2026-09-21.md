# Tatsächliche Grim- und Weltprüfungen, 21.09.2026

Ausgangscommit: `a898dca`. Die folgenden Befunde stammen aus neuen Prüfungen während dieser Arbeit. Die historischen PASS-Berichte wurden nicht als Nachweis übernommen. [Installation, Quellen, Bedienung und genaue Versionsmatrix](GRIM-LAB.md).

Nachtrag zur Bedienung: Die [aktiven Laborprofile](LAB-PROFILES.md) fassen die
Verwaltung inzwischen auf einen Eintrag je Minecraft-Version zusammen. Die im
folgenden Bericht verwendeten `-grim`-Namen sind weiterhin gültige interne
Instanzpfade und historische Prüfnachweise; im Versionsmenü der Webseite stehen
die einfachen Minecraft-Versionen. Originalwelten und Backups bleiben erhalten.

## Implementiert

49 Vanilla-Referenzen bleiben erhalten. 43 eigenständige Paper-Varianten mit überprüften Downloads, eigenem Weltpfad, Ports und Java-Auswahl sind installiert. 41 Varianten konnten mit aktivem Grim/API gestartet werden. Paper 1.13/1.13.1 scheitern beim aktuellen Grim-Build an dessen Command-Anbindung. Für fünf weitere alte Releases fehlt eine installierbare exakte Plattform im gewählten offiziellen Paper/Spigot-Pfad. 26.3 wird von Grim nicht unterstützt. Diese Grenzen sind in Manifest/Status und Supportmatrix sichtbar.

`ViaForgeLabAC-1.0.0.jar` liefert persistentes `/labac on|off|status`, echte automatische Verbose-Aktivierung für jeden lokalen Account und Beobachtung von Checks, Vorhersagen und Setbacks. Die Webverwaltung unterscheidet Vanilla und Paper/Grim und bietet dieselben Aktionen sowie geprüfte Sicherung/Rücksicherung. Keine abgeschwächten Grim-Schwellen und keine dauerhaften Bypass-Rechte. Grims Elytra-Standardoption bleibt true.

ViaForge erhielt gezielte Korrekturen aus den Befunden:

- Ab 1.9 wird kleine Restbewegung mit 0,003 statt der nativen 1.8-Schwelle 0,005 abgeschnitten. Dadurch verschwanden reproduzierte kleine Simulation-Abweichungen an Sprungspitzen und bei schnellen Gleitfolgen auf 1.12.2.
- Die Positions-Erinnerung erfolgt ab 1.9 einen Tick früher; das beseitigt reproduzierte `BadPacketsE` bei ruhenden Spielern.
- Moderne Ping-IDs bleiben vollständig 32 Bit erhalten und werden nach Verarbeitung der vorhergehenden Weltpakete auf dem Clientthread beantwortet.
- Ab 1.21.2 werden reale Eingaben und Tick-Enden gesendet. Sowohl ViaBackwards' zeitbasierte als auch seine vor Bewegung abgeleiteten zusätzlichen Tick-Enden werden nur für die clientseitige Verbindung ersetzt. Ein Zustand wird nicht zweimal übersetzt.
- Der lokale Spieler simuliert vor Abschluss der ersten Welt-/Teleportinitialisierung keine Platzhalterposition. Ressourcenladen soll keine nachträgliche Bewegungsserie aus der Ladezeit erzeugen. **26.2-Start-Timing ist damit noch nicht vollständig gelöst**, siehe Befunde.

## Negativkontrolle: Prüfen, Verbose und Schalter

Aktueller Bericht: `run/test-servers/1.12.2-grim/grim-controls-1789945476077713400.json`; Ablaufprotokoll: `build/logs/grim-controls-final.log`.

Paper 1.12.2 Build 1620, Grim 2.3.74-8eb5f28, tatsächliches Protokoll 340. Zwei neue Accounts `GLabA77713400` und `GLabB77713400`, beide Nicht-OP. Diese Clients sind ausdrücklich synthetische Protokollproben, keine originalen Minecraft-Clients.

| Fall | Tatsächlicher Nachweis | Ergebnis |
|---|---|---|
| Beide Logins | Verbose=true, keine Exempt-/NoSetback-/NoModify-Rechte | Bestanden |
| Survival / ON | Prediction-Zähler steigen; absichtliche ungültige Sprintpakete erzeugen `BadPacketsF` | Aktive Prüfungen nachgewiesen |
| Verbose-Empfang | Beide Accounts empfangen das konkrete Flag im Chat, ohne OP und ohne `/grim verbose` | Bestanden |
| OFF, gleiche Verbindung | `disableGrim=true`; bei denselben ungültigen Paketen steigen weder Flags noch Prediction-Zähler | Tatsächlich abgeschaltet |
| ON, gleiche Verbindung | Prüfen und Chat-Verbose setzen wieder ein | Bestanden |
| OFF + Neustart + Reconnect | Gewählter OFF-Zustand bleibt; Verbose beim Login wieder aktiv; keine Flags aus Negativpaketen | Bestanden |
| Nicht-OP `/labac on` | Tatsächlicher Status enabled und erneutes `BadPacketsF` | Bestanden |
| ON + Neustart | Enabled bleibt gespeichert | Bestanden |

Der Helper speichert den Schalter atomar, bevor er den laufenden Zustand verändert. Der Bericht enthält die tatsächlichen Account-, Modus-, Versions-, Permissions- und Chatdaten. Normale Alerts oder eine leere Konsole wären hierfür kein Nachweis.

## ViaForge-Bewegungen: neue Accounts und Survival

Alle Tabellenfälle laufen unter dem unveränderten Grim-Build 2.3.74-8eb5f28. Start im Creative-Modus ist separat erfasst; danach erfolgt die Bewegung in Survival. Kein Account ist OP. Grim meldet `fml,forge` und das Zielprotokoll, nicht 1.8.9.

1.12.2: Paper 1620, Account `VFfly340276966`, erkannt 1.12.2 / 340, zuletzt 276 bestätigte Predictions. Bericht `build/logs/movement-1.12.2-grim-1789945238282041200-grim.json`.

26.2: Paper 126, Account `VFfly776751598`, erkannt 26.2 / 776, zuletzt 346 bestätigte Predictions. Bericht `build/logs/movement-26.2-grim-1789945704108405400-grim.json`. Die Kollisionswand wurde nach Chunk-Laden gesetzt und serverseitig bestätigt. Die vorherige Probe ohne geladenen Chunk wurde als Kollisionsnachweis verworfen.

| Ablauf | 1.12.2: Flags / Setbacks | 26.2: Flags / Setbacks |
|---|---:|---:|
| Creative-Login | 0 / 0 | 6 TimerLimit / 2 |
| Survival setzen, Teleport, ruhen | 0 / 0 | 0 / 0 |
| Gehen zur Wand | 0 / 0 | 0 / 0 |
| Sprint gegen Glaswand | 0 / 0 | 0 / 0 |
| Springen an der Wand | 0 / 0 | 6 Simulation / 0 |
| Glaswand abbauen | 0 / 0 | 4 Simulation / 0 |
| Sneaken am/in 1,5 hohen Durchgang | 0 / 0 | 4 Simulation / 0 |
| Fallstart und freies Gleiten | 0 / 0 | 0 / 0 |
| Rakete während Flug | 0 / 0 | 0 / 0 |
| Inventar während Flug öffnen | 0 / 0 | 0 / 0 |
| Wasserlandung | 0 / 0 | 0 / 0 |
| Schnelle Sprint-/Sprung-/Gleitfolge | 0 / 0 | 44 Simulation / 0 |
| Elytra ausziehen | 0 / 0 | 5 Simulation + 1 GroundSpoof / 2 |
| Nebenhandschild benutzen | 0 / 0 | 0 / 1 |
| Inventar nach Landung öffnen | 0 / 0 | 0 / 1 |

Zeitfenster sind keine automatische Ursachenzuordnung: spätere Setbacks können auf vorhergehende Bewegungsfehler zurückgehen. Beide Clients blieben verbunden und beendeten die Aufzeichnung regulär. Positionsfolgen, Flug-/Wasser-/Boden-/Sprintflags, Kollisionshöhe, Commands, Grim-Zustände und Events sind pro Fall im JSON enthalten. Auf 1.12.2 endet Sneaken an x=-11,3 vor dem 1,5 hohen Durchgang mit Höhe 1,65; auf 26.2 gelangt der Spieler mit Höhe 1,5 hinein. Der abgebrochene Glasblock wurde serverseitig als Luft bestätigt. „Keine Flags“ bedeutet nur den dokumentierten Ablauf, keine vollständige Originaltreue aller Bewegungen.

Die erste Harness-Fassung löste Klicks am Ende des Bewegungsticks aus. Das produzierte selbst `Post`-Flags. Klicks werden nun vor der nächsten Spielerbewegung ausgelöst; die 26.2-Wiederholung hatte **keine Post-Flags**. Diese vorherigen Meldungen werden nicht als ViaForge-Fehler ausgegeben.

Die strengere 26.2-Flugprobe `build/logs/live-flight-26.2-grim-1789944645419866600-grim.json` erreichte 70 Flugticks einschließlich Raketenmodell, Sound, Handanimation, Inventar und Wasserlandung, scheiterte danach bei der dritten schnellen Aufstiegs-/Gleitfolge unter Grim. **Kein Grim-Kompatibilitäts-PASS für 26.2.** Die native 1.8-Kollisionsauflösung, moderne Eingabe-/Bewegungsdetails und Login-Timing sind weiterhin konkrete Untersuchungsfelder. Die verbleibenden Unterschiede wurden nicht durch Ausnahmen oder erhöhte Schwellen kaschiert.

Nach dem abschließenden Release-Build wurde die strenge Flugprobe erneut ausgeführt: **1.12.2 mit Grim bestanden**, Account `VFfly340603443`, 70 Flugticks, Rakete/Sound/Handanimation, Inventar während Flug, Wasserlandung, drei Gleitfolgen und abschließend bestätigtes Stehen; **0 Flags und 0 Setbacks**, aktive Survival-Predictions. Bericht `build/logs/live-flight-1.12.2-grim-1789946580716103000-grim.json`. Auch **Vanilla 26.3 bestanden**, Account `VFfly777559861`, einschließlich versionsabhängigem Aufstiegsstart: `build/logs/live-flight-26.3-1789946543654019300.json`. Das zweite Ergebnis ist ausdrücklich kein Grim-Test. Gemeinsames Log: `build/logs/grim-final-live-flight.log`.

## Originalclient-Vergleich

`build/native-probe/1789943322262770100/report.json`: originaler Mojang-Client 26.2, mit überprüftem Clientdownload und reinem Eingabe-/Positionsagenten, Account `Native62770100`, erkannt `vanilla`, Protokoll 776, Survival. 410 aufgezeichnete Ticks, 219 bestätigte Grim-Predictions, **0 Flags, 0 Setbacks**, reguläres Ende. Gehen, Sprinten, Springen, Sneaken, Fallstart, 30 Gleit- und 70 Raketenticks, Wasserlandung und Ausziehen der Elytra sind aufgezeichnet. Das ist kein vollständiger nativer Vergleich jeder Kollisions-/Schild-/Inventarkombination und kein Nachweis für alle Serverversionen.

## Gespeicherte Welten und Reparatur

Das übergebene Bild wurde geöffnet. Es zeigt störende Bäume/Gelände über der Ausstellung; eine Serverversion ist daraus nicht sicher zuzuordnen. In den **jetzt tatsächlich gespeicherten** 49 Originalwelten wurden richtige flache Generatoren, sämtliche 140 reservierten Chunks und keine zusätzlichen natürlichen Blöcke im geprüften Bereich festgestellt. Bericht: `run/test-servers/terrain-survey-1789944991548746500.json`. Die Erweiterung prüft bis y=319 bzw. bis zur vorhandenen Bauhöhe; sie berücksichtigt Papers geänderten Speicherort der 26.x-Dimensionsdaten.

Für die 43 Paper-Kopien: `terrain-survey-1789945590761137200.json`; die während der Prüfung belegte 26.2-Instanz wurde ausdrücklich nicht als bestanden gewertet und danach vakant nachgeprüft: `terrain-survey-1789946160826535300.json`. Zusammen ebenfalls 43 flache, vollständige Ausstellungsbereiche ohne zusätzliches Gelände. Das widerlegt das Bild nicht und beweist keine nachträgliche Reparatur seiner unbekannten Ausgangswelt. Ohne konkreten heutigen Befund wurden Originalwelten nicht pauschal verändert.

Eine gezielte echte Reparatur wurde in der gesicherten 1.12.2-Paper-Kopie ausgeführt: Holz auf y=250 und Blätter auf y=251 als zusätzliche Störungen, beschädigtes Erd-Exponat und ein Außenblock bei x=80 als Erhaltungsprobe. **Ausstellung wiederherstellen** entfernte die Störungen, stellte das Exponat wieder her und ließ den Außenblock bestehen. Die alte Gamemode-Schaltung blieb entfernt. Prüfung: 145 Weltchecks, 803 Inventar-Items und 1.806 Schilder ohne Schildfehler. Bericht `run/test-servers/1.12.2-grim/terrain-repair-probe-1789945573116825700.json`, Log `build/logs/grim-terrain-repair-final.log`. Anschließend wurde der ursprüngliche Stand der Kopie per geprüfter Sicherung wiederhergestellt; Test- und Reparaturstände bleiben zusätzlich gesichert.

## Webseite und Dateien

Browserprüfung auf 1.11.2-grim: Start, OFF, ON, Status, Speichern/Stoppen, Backup `snapshot-20260921-011024-6471b0`, Rücksicherung jeweils erfolgreich. Die Rücksicherung legt nochmals ein Sicherheitsbackup an. `build/logs/grim-web-actions.json` enthält die konkreten Auftrags-IDs und Ergebnisse. Anzeige: 92 Einträge, davon 43 Grim-Varianten; 26.3 und die beiden fehlerhaften Grim-Initialisierungen als nicht verfügbar. Die Webverwaltung blieb auf 127.0.0.1:8765 verfügbar, ursprüngliche Server und fremde Clients wurden nicht beendet.

`build/logs/grim-installed-integrity.json`: alle 43 Plattform-/Grim-/Helper-JARs stimmen mit den Installationsmanifesten überein; Loopback, Offline-Zugang, Creative-Standard, keine erzwungene Spielmodusänderung, Sichtweite 16 und Simulationsdistanz 3 geprüft. Quellen und SHA-256 jeder Plattform stehen im versionierten Lockfile. Die Laufzeitdaten und ausführlichen Logs liegen absichtlich im ignorierten `run/` beziehungsweise `build/`.

Die Abschlusskontrolle fand außerdem Steam auf TCP 27036. Deshalb wurde ausschließlich die gestoppte 1.16.2-Grim-Variante nach Sicherung ihrer Einstellungen auf **27086 / RCON 27087** verschoben; die freie Bindung wird nun unter Windows zusätzlich exklusiv geprüft. Erneuter wirklicher Start inklusive Grim/API: PASS (`build/logs/grim-port-retest.log`). Steam wurde nicht verändert. Die Webverwaltung wurde anschließend mit dem endgültigen Manifest neu gestartet. Konsolidierter Nachweis: `build/logs/grim-final-summary.json`; abschließende Plattformserie: `run/test-servers/grim-platform-survey-1789946251905823600.json`.

## Frischer Build und Artefakte

`./build.bat build runClient -x preRunClient` mit entferntem `VIAFORGE_SMOKE_PROTOCOL`: **erfolgreich**, neuer `build/logs/block-client-smoke-test.txt` beginnt mit PASS und enthält alle 49 Ressourcenprofile. Vollständiges Log: `build/logs/grim-release-full.log`. Der erweiterte komprimierte Transport-Smoke prüft vollständige signed 32-Bit-Ping-IDs, geordnete einmalige Antworten, reale Inputbits, ausbleibende zusätzliche zeitbasierte Tick-Enden und ausbleibende vor Bewegung inferierte Tick-Enden. Native Weltfixtures der Boot-/Nebenhandproben kennzeichnen nun ihren vorbereiteten Loginzustand explizit.

115 JUnit-Tests, 0 Fehler; 49 Python-Tests, 0 Fehler (`build/logs/grim-python-tests.log`). Neue Tests prüfen unter anderem beschädigte/zusätzliche Snapshotdateien, Inventarerhalt und beide Paper-Nebenwelten bei Rücksicherung, fremde Snapshot-IDs, fehlende Regionen/Chunks, Störmaterial über der früheren Prüfhöhe, erhaltene Pflanzen, Barrieren ausschließlich innerhalb der Grenze sowie wahrheitsgemäßen Status bei fehlgeschlagenem Pluginstart oder alter Statusdatei.

| Datei unter `build/libs/` | Bytes | SHA-256 |
|---|---:|---|
| ViaForge-1.8.9-4.4.0-client.1.jar | 14.389.980 | `b5c2372b8f8aff1a4c71ec9aa7b5cae40311850d791858541d91826f659390d3` |
| ViaForgeLabAC-1.0.0.jar | 11.548 | `27e260cb3326a51b18aac23282df0399bc1cd3264e2d47966fa0ab8c671250fa` |
| ViaForgeLab-LegacyPaperBoot-1.0.0.jar | 2.813 | `604b12f99295b397813a30bc3d1e03bd92626310dd547848472f8e2336b53843` |
| ViaForgeLab-NativeInputProbe-1.0.0.jar | 129.871 | `e085a28a1b07006f589f561c4e43b438043efbecc5c828448a936067fe0b2dd9` |

Die bisherige Release-JAR wurde vor dem Bauen unter `build/inspection/pre-grim-release-*` erhalten. Der neue Helper ist auf allen 43 Varianten installiert und seine Prüfsumme in deren `installation.json` bestätigt. Alle Helper-Quellen liegen unter `tools/test-servers/labac/`; native Vergleichsquellen unter `tools/test-servers/native-probe/`. Build-Eingaben/Quellhashes des Helpers: `build/labac/build.json`.
