# Versionsgetreues Schieben durch Spieler und Mobs

ViaForge nutzt den Forge-1.8.9-Client. Dessen clientseitiger Lebewesen-Tick
führt den später eingeführten Umgebungsschub nicht aus. Ein anderer Spieler
besitzt außerdem einen eigenen Interpolationstick ohne diesen Schritt.
Der Fehler wurde am 22.09.2026 mit echten Clients reproduziert: ViaForge blieb
bei 80 beobachteten Kontaktticks eines Schweins stehen, der Originalclient
1.12.2 wurde bei derselben Ausgangslage um 0,5034405569740706 Blöcke verschoben.
Beide hatten aktives Grim. Ausbleibende Flags waren daher allein kein Beweis
für korrekte Physik.

## Implementierung

Ab Zielversion 1.9 ruft der Tick des jeweils anderen Lebewesens den originalen
Push-Schritt auf; andere Spieler tun dies nach ihrer Interpolation. Empfänger
ist im Client der lokale, lebende, nicht kletternde und nicht zuschauende Spieler.
Entscheidend sind die tatsächlichen, nicht vergrößerten Kollisionsboxen.
No-clip, gemeinsames Fahrzeug, besetzte Fahrzeuge und die Teamregeln werden
berücksichtigt. Der lokale Tick startet keine zusätzliche Umgebungssimulation.
Native 1.8-Verbindungen und die serverseitigen 1.8-Ticks behalten ihr Verhalten.

Die Regel wird aus `ServerSession` gewählt, unabhängig vom internen Ereignisformat.
Die Originalimplementierungen wurden aus den lokal vorhandenen offiziellen,
geprüften Zielclient-JARs gelesen. Arbeitskopien und Herkunftshashes liegen unter
`build/inspection/push/`.

| Grenze | Verhalten |
|---|---|
| 1.9 | Clientseitiger Schub; Teamregeln beider Kontaktpartner; unvergrößerte Box |
| 1.14 | Schlafender Empfänger nimmt keinen Schub an |
| 1.17 | Double-Quadratwurzel; die alte `entityCollisionReduction` entfällt |
| 1.19 | Einzelimpulse berücksichtigen zusätzlich die Schiebbarkeit des jeweiligen Empfängers |
| 1.21.5 | Bei Spielern stoppt horizontale Restbewegung anhand von X²+Z² < 0,003², nicht mehr je Achse |

Der Impuls verwendet die originalen Konstanten `.01F` und `.05F`, Maximalbetrag
der horizontalen Abstände, Quadratwurzel und begrenzten Kehrwert. Bis 1.16.5
wird das Ergebnis der Quadratwurzel wie im Original auf Float gerundet.
Es gibt keine zum Unterschreiten einer Anti-Cheat-Schwelle erfundene Korrektur.

Diese letzte Grenze wurde zusätzlich im echten 26.2-Diagonaltest reproduziert:
ViaForge stoppte Z bei einer Restgeschwindigkeit von 0,002099219569475292,
obwohl X noch 0,004198439138950585 betrug. Das Original bewegte beide Achsen
noch einen Tick. Die getrennten bisherigen Achsvergleiche werden deshalb nur
für die entsprechenden Spielerversionen durch die ursprüngliche Vektorprüfung
ersetzt. Y behält seine unabhängige Grenze. Native Weltregressionen prüfen auch
zwei einzeln kleine Achsen, deren gemeinsamer Betrag noch oberhalb der Grenze liegt.

`LegacyEntityPackets` erhält Kollisionsregeln vor ihrer Entfernung durch ViaRewind.
Die geordneten Ereignisse setzen die Regel auf dem tatsächlichen nativen
`ScorePlayerTeam`; Löschung und Neuanlage verwenden dessen normalen Lebenszyklus.
Mob-UUIDs werden ebenfalls vor Verlust im 1.8-Spawnformat erhalten, weil
Nichtspieler über ihre UUID im Scoreboard stehen.

Der erste Nachtest fand außerdem einen Positionsverlust: Ein Mob bei X=0,9
erschien bei X=0,90625. Damit entstand trotz richtigen Impulses eine falsche
Kontaktfolge. `ClientEntityMotion` erhält deshalb ursprüngliche Spawnpositionen,
Teleports und relative Short-Deltas von Lebewesen. Via aktualisiert seine
zustandsbehafteten Tracker weiterhin genau einmal. Die native Anwendung der
gerundeten Bewegung entfällt bei diesen Entitäten; das Originalereignis erteilt
genau einen Interpolationsauftrag. Bootsbewegung behält ihren eigenen Pfad.

| Grenze | Positionsverhalten |
|---|---|
| 1.9–1.14.4 | Floor auf das ursprüngliche 1/4096-Raster auch bei Rotationsupdates |
| 1.15 | Reine Rotation verändert das Positionsziel nicht mehr |
| 1.16.2 | Null-Deltas erhalten die jeweilige Achse; die alte Ausnahme für kleine Teleports entfällt |
| 1.19.3 | `Math.round` statt Floor beim Kodieren relativer Koordinaten |

## Reproduzieren und Belege

```powershell
$env:VIAFORGE_NO_PAUSE='1'
./build.bat build
$env:VIAFORGE_PUSH_PEER='1' # optional, exakter Original-Zweitclient auf 1.12.2 oder 26.2
py -3 tools/test-servers/push_probe.py --version 1.12.2 --label verification
py -3 tools/test-servers/push_probe.py --version 1.12.2 --client native --label original
```

Für andere Versionen `VIAFORGE_PUSH_PEER` entfernen. Der Treiber prüft vor dem
Start vorhandene Minecraft-Clients und verwendet eine einzelne isolierte,
anschließend erhaltene Welt auf freien Loopback-Ports. Vor dem Aufbau wird die
Welt gesichert. Aktive Profilwelten und ihre Spieler bleiben unberührt.
Ein neuer Nicht-OP-Account startet Creative und wird für Kontaktfälle ausdrücklich
in Survival gesetzt. Creative und Spectator werden separat geprüft.

Die Originalclient-Agenten steuern Tasten und beobachten Tickzustände. Sie ändern
weder Physik noch Netzwerkpakete. Der Forge-Treiber verwendet echte Clientticks.
`report.json`, `sources.json`, Serverkonsole, Client-JSONL und LabAC-Ereignisse
liegen unter `run/test-servers/push-<Version>-<Label>-<Zeit>/`.

Jeder Kontaktfall erfordert beobachtete Bewegung beziehungsweise begründeten
Stillstand, passende Kollisionsboxen, tatsächliche Grim-Predictions, aktives ON,
erkanntes Zielprotokoll, korrekten Spielmodus und fehlende Bypass-Rechte.
Ein toter Empfänger, ausbleibende Predictions oder leerer Chat ergeben keinen
erfolgreichen Bewegungstest. Bei anlaufenden Spielern wird die beobachtete
positive Verschiebung geprüft; deren Distanz hängt zusätzlich vom Kontaktzeitpunkt
und der über das Netzwerk interpolierten Bewegung ab.

Der verwendete Grim-Commit `8eb5f28` behandelt diese Kontakte in
`MovementTicker.handleEntityCollisions` als Bewegungsunsicherheit und wertet
Teamregeln über `EntityPredicates.canBePushedBy` aus. Er berechnet keinen exakten
tickweisen Gegenimpuls für jeden Kontakt. Deshalb ergänzen Originalvergleich
und numerische Regressionen den Anti-Cheat-Nachweis. Lokale Quellen:
`build/inspection/grim/source/`; offizielles Repository:
https://github.com/GrimAnticheat/Grim/tree/8eb5f28

## Frische Kontaktmatrix vom 22.09.2026

Alle folgenden ViaForge-Läufe verwenden den korrigierten Entwicklungsclient
mit denselben Produktionsquellen wie die Release-JAR, Paper der exakt genannten
Version und Grim `2.3.74-8eb5f28`. Grim meldet jeweils das angegebene Zielprotokoll.
Der Beobachtungstreiber und die Testaccounts sind nicht in der Release-JAR.

| Zielversion | Paper-Build | Protokoll | Ausgewertete Kontaktfälle | Laufverzeichnis unter `run/test-servers/` |
|---|---:|---:|---|---|
| 1.9.4 | 775 | 110 | 12/12 PASS; Höhenfall ausgelassen | `push-1.9.4-ground-release-1790085500456042000` |
| 1.12.2 | 1620 | 340 | 16/16 PASS, einschließlich echtem zweiten Spieler | `push-1.12.2-ground-release-1790085607712304300` |
| 1.16.1 | 138 | 736 | Nicht prüfbar: Decoder-Abbruch vor Kontakt | `push-1.16.1-ground-release-1790085055110444600` |
| 1.16.5 | 794 | 754 | 13/13 PASS | `push-1.16.5-ground-release-1790085107118855200` |
| 1.17 | 79 | 755 | 13/13 PASS | `push-1.17-ground-release-1790085231826900100` |
| 1.21.5 | 114 | 770 | 13/13 PASS | `push-1.21.5-ground-release-1790085360574404900` |
| 26.2 | 126 | 776 | 16/16 PASS, einschließlich echtem zweiten Spieler | `push-26.2-release-1790084368509077900` |

Die 13 Grundfälle umfassen leeren Boden, einen Mob außerhalb der Kontaktbox,
einen Mob darüber, Kontakt von rechts und diagonal, `never` auf jeder der beiden
Seiten, beide übrigen Teamregeln jeweils mit gleichem und anderem Team sowie
Creative am Boden und Spectator. Bei den 16 Fällen kommen stehender Spielerkontakt,
dessen Team-Ausschluss und ein tatsächlich anlaufender Original-Zweitclient hinzu.
Die Eingabe des Empfängers bleibt dabei leer. Kontaktbeobachtungen dauern ein bis
vier Sekunden; einlaufende Spieler werden anschließend drei Sekunden ausgerollt.
Jeder Fall speichert Startzustand, Zeitfenster, Eingaben, tatsächliche Positionen,
Geschwindigkeiten, Boxkontakte, Predictions, Offsets, Flags und Setbacks.

PASS bedeutet hier korrekte beobachtete Kontaktwirkung und aktive Prüfungen im
ausgewiesenen Zeitfenster. In allen 83 bestandenen Kontaktfällen sind Flags und
Setbacks null, die protokollierten Prediction-Offsets ebenfalls null. Spectator
erfordert erwartungsgemäß keine Bewegungsprediction. Vorbereitungsphasen werden
separat ausgewiesen: 26.2 hatte beim ViaForge-Login 31 Setbacks; sein Original-
Zweitclient meldete beim Login einmal `Timer` und einmal `TimerLimit`. Auch
1.16.5, 1.17 und 1.21.5 hatten jeweils zwei Login-Setbacks. Diese Vorkommnisse
sind kein Kontakt-PASS und werden nicht als durch diese Änderung behoben erklärt.

1.9.4 kennt das `NoGravity`-Flag noch nicht. Der erste Höhenversuch ließ das Tier
deshalb herunterfallen; er ist als fehlgeschlagener Aufbau erhalten. Die frische
Matrix überspringt diesen einen Fall ausdrücklich. Außerdem bewegt sich das
serverseitige Schwein dort beim Kontakt mit; seine Enddistanz ist deshalb nicht
direkt mit dem unbeweglichen `NoAI`-Schwein späterer Server zu vergleichen.

Der 1.16.1-Abbruch wurde mit der archivierten Produktionsimplementierung vor
diesen Schubänderungen frisch wiederholt und ist dort identisch:
`SET_ENTITY_LINK`, Paket-ID 27, unzureichende Daten für den zweiten Integer in
ViaRewind `EntityPacketRewriter1_9`. Der Vergleich liegt unter
`push-1.16.1-pre-push-1790085754647696700/`, einschließlich SHA-256 des alten
Artefakts. Er belegt einen bereits bestehenden Loginfehler, aber noch nicht dessen
Ursache oder eine Reparatur. 1.16.1 erhält deshalb keinen Kontakt-PASS.

Der maschinenlesbare Fallbericht wird aus den erhaltenen Rohdaten neu erzeugt:
`build/logs/push-evidence-final.json` und `build/logs/push-case-matrix-final.csv`.
Historische `report.json` bleiben unverändert, auch bei später verschärften
Nachweiskriterien. Frühere Läufe vor den Positions- und Restbewegungskorrekturen
können die Kontakt-/Grim-Kriterien bereits bestehen und dennoch vom Original
abweichen; der numerische Originalvergleich ist daher ein zusätzlicher Nachweis.

## Vergleich mit unveränderter Originalphysik

Die offiziellen Clients 1.12.2 und 26.2 bestehen jeweils dieselben 16 Fälle.
Die Läufe liegen unter `push-1.12.2-original-final-1790085805400862200/` und
`push-26.2-original-grounded-1790083220895525500/`. Client-JARs werden gegen die
offiziellen SHA-1-Werte geprüft; `native-source.json` enthält außerdem den
SHA-256-Wert und den verwendeten Beobachtungsagenten. In diesen beiden
Originalvergleichsläufen gab es auf keinem der beiden Accounts Flags oder Setbacks.

| Version/Fall | Original: Verschiebung in Blöcken | ViaForge: Verschiebung in Blöcken |
|---|---:|---:|
| 1.12.2, Mob rechts | 0,5034405569740706 | 0,5034405569740706 |
| 1.12.2, Mob diagonal | 0,5619279913606302 | 0,5619279913606302 |
| 1.12.2, stehender Spieler | 0,32289471217447085 | 0,32289471217447085 |
| 26.2, Mob rechts | 0,5034405593015625 | 0,5034405593015625 |
| 26.2, Mob diagonal | 0,5628636566144041 | 0,5628636566144040 |
| 26.2, stehender Spieler | 0,3228947131234158 | 0,3228947131234158 |

Die 26.2-Diagonaldifferenz beträgt nun etwa 1,11×10⁻¹⁶ Blöcke. Vor der belegten
Restbewegungskorrektur betrug die ViaForge-Distanz dagegen 0,5619279939511691.
Beim anlaufenden 1.12.2-Spieler wurden 0,10065721437355246 bzw.
0,10190352960386284 Blöcke beobachtet. Diese Läufe synchronisieren Tastenphasen,
aber nicht jeden Netzwerk-/Interpolationstick beider Clients; sie beweisen daher
keine numerisch identische zeitliche Kontaktfolge. Beobachteter Kontakt, positiver
Schub und aktive Grim-Prüfung bestehen. Für die übrigen Versionen gibt es in
dieser Lieferung Quellvergleich, Regressionen und ViaForge-Livetests, jedoch
keinen zusätzlichen echten Originalclient-Lauf.

## Regressionen und Grenzen

Die frische erweiterte Bootsregression liegt unter
`boat-1.12.2-push-regression-1790085934898844300/`: 32 bestandene Fälle,
4.403 Fahrzeug-Predictions, kein `Simulation`-Flag und kein Setback.
Der größte protokollierte Fahrzeugoffset beträgt 0,0003142326276802096.
Beschleunigen, lange Ausrollphasen, Kurven, Ufer, Land/Wasser, Eis, Strömung,
Passagiere einschließlich echtem zweiten Spieler, Fahrerwechsel, Teleport und
Reconnect wurden tatsächlich beobachtet. Zwei Folgephasen nach dem Eintauchen
sind **INCOMPLETE**, weil der Spieler unter Wasser seinen Sitz verliert und
danach die erforderlichen Fahrerticks fehlen. 37 weitere Phasen sind Aufbau
oder Übergänge ohne eigenen Kompatibilitäts-PASS.

**Offen bleibt ein `AimModulo360`-Flag beim Aussteigen** nach mehreren Kurven.
Der Server schickt dabei eine Ausstiegsposition mit Yaw −329,99933, während
das Boot zuvor −689,9993 meldet. Grims betreffende Prüfung erkennt große
Winkelsprünge zurück in den Bereich −360 bis +360. Die bestehende Aufzeichnung
enthält nicht jeden lokalen Spieler-/Paketwinkel; die exakte verursachende
Tickfolge ist damit noch nicht bewiesen. Dieser gesamte Lauf ist deshalb
ausdrücklich kein pauschaler Boots-PASS. Es wurde weder eine Winkelkorrektur
auf Verdacht eingebaut noch der Check verändert.
Der anschließende Kontrolllauf des gleichen Grundablaufs mit der archivierten
Vor-Schub-JAR (`boat-1.12.2-push-baseline-aim-1790086413454229500/`) besteht
seine zehn Fahrfälle ohne Flags. Ein einmaliger Gegenlauf ohne Flag klärt die
zeitabhängige Ursache nicht; eine Zuordnung zur neuen Schubänderung oder ein
Ausschluss dieser Zuordnung wäre damit nicht belegt.

123 JUnit-Tests und 71 Python-Werkzeugtests bestehen. Der frische vollständige
Forge-Smoke beginnt mit `PASS` und prüft 49 Ressourcenprofile, einschließlich
echter Mob-/Spielerticks, genau einem Schubimpuls, nächstem lokalen Bewegungstick,
Boxüberschneidung, No-clip, totem und kletterndem Empfänger, gemeinsamem Fahrzeug,
Team-Lebenszyklus, UUID-Erhaltung, relativer Mob-Interpolation und den drei
horizontalen Restbewegungsfällen. Nachweise: `build/logs/push-full-build-final.log`,
`push-full-smoke-final.txt` und `push-python-release.txt`.

Das ist keine Zertifizierung jedes später eingeführten Mobs oder jeder Pose: Für Inhalte,
die Via nur als Ersatzentität darstellt, gelten weiterhin die dokumentierten
Inhalts- und Kollisionsgrenzen. Spezialfälle wie Shulker-Lidbewegung, Riptide,
komplexe Reitketten und Serverplugins benötigen eigene Livefälle.
Die separat dokumentierten 26.2-Login-/Timer- und Elytra-Abweichungen sind nicht
mit diesem Kontaktfehler gleichzusetzen. Die vorherigen Bootskorrekturen bleiben
erhalten; ihre eigene Matrix steht in [BOAT-GRIM-2026-09-21.md](BOAT-GRIM-2026-09-21.md).

## Artefakte

Die Release-JAR liegt unter `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`
(14.409.177 Bytes), SHA-256:
`0fa80654268c3e194ff89d862deaa4e1eadba266be7acc3cf1fc24018cc272e1`.
Passende Quellen einschließlich Testtreibern und Originalclient-Agenten:
`build/libs/ViaForge-1.8.9-4.4.0-client.1-sources.zip`.

`build/libs/ViaForge-push-evidence-20260922.zip` bündelt ausgewählte Rohdaten,
Fallmatrix, Regressionsergebnisse, Herkunftsnachweise und SHA-256 je enthaltener
Datei. Welten und RCON-Zugangsdaten sind nicht enthalten. Die erhaltenen
isolierten Welten samt Sicherungen verbleiben unter `run/test-servers/`.
`build/libs/ViaForge-push-release-20260922.json` und die `.sha256`-Dateien geben
die tatsächlichen Größen und Prüfsummen aller drei Lieferartefakte an.
Vorherige JARs und Quellen sind unter
`build/backups/entity-push-20260922-143245/` erhalten. Die aktiven 49 Laborprofile
und deren Welten, Inventare und gespeicherte Spielmodi wurden nicht verändert.
