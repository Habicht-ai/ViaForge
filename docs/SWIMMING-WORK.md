# Schwimmen: Arbeitsstand 23.09.2026

## Abschlussstand 23.09.2026, 00:30

Implementierung, Live-Matrix und vollständiger Build sind abgeschlossen; die
unten benannten Grenzen bleiben offen. HEAD `09de14a`, Änderungen uncommitted.
Maßgeblich: [SWIMMING.md](SWIMMING.md) und [SWIMMING-TESTS.md](SWIMMING-TESTS.md).

- 143/143 ViaForge-Survivalfälle: 1.13.2, 1.14.4, 1.16.5, 1.17.1, 1.21.5
  und 26.2. In diesen Abschlussläufen keine Survival-Flags/Setbacks,
  einschließlich Reset- und Ausrüstungsphasen. Login-Korrekturen separat.
- Final 26.2 ViaForge: `swim-26.2-prepared-transitions-1790115286060860700`,
  28/28 PASS; Original: `swim-26.2-prepared-reference-1790115489688158900`,
  28/28 PASS. Gespeicherte Referenzwelt wurde jeweils nur kopiert.
- Vorherige Umbau-/Teleportfolge `transitions-final2` bleibt FAIL: 17 Flags,
  zwei Setbacks. Alter Stein am gerade umgebauten Teleportziel war zeitweise
  noch im Client sichtbar; genaue Ursache offen. Kein Fix dafür behauptet.
- 1.18.2 beim Login durch Grims Forge-Sperre blockiert. Keine Umgehung.
- 130 JUnit / 78 Python PASS. Vollständiger Forge-Smoke 49/49 PASS frisch:
  `build/logs/swim-all-profiles-final-20260923.txt` und gleichnamiges `.log`.
  Die zuvor fehlgeschlagene Handprobe wartete einen Interpolationstick zu kurz;
  jetzt sind Restwert und vollständiges Ausblenden explizit geprüft.
- Release-JAR gebaut: 15.695.331 Bytes, SHA-256
  `697a2ac0d281d99c3dd1cd4b25018462290d4c64f394b74f66cbeaf99eefc657`.
  Finale Artefaktgrößen und Prüfsummen in `build/libs/swimming-release.json`.
- Originalartefakte: `build/backups/swimming-20260922-205857/`.
  Smoke-Einstellungen unter `build/backups/swim-all-profiles-final-20260923/`;
  `run/options.txt` und `run/ViaForge/viaforge.yml` bytegenau zurückgespielt.

Alle eigenen Testserver/-clients haben ihren Lauf beendet. Vor weiterer Arbeit
aktuelle Prozesse neu prüfen. Bei Wiederaufnahme waren keine Nutzer-JVMs oder
Webserver aktiv; historische PIDs unten sind keine aktuelle Prozessliste.
Es wurden keine bestehenden aktiven Laborwelten geöffnet oder neu erzeugt,
keine Nutzerclients beendet und keine Grim-Prüfungen abgeschwächt.

Weitere Arbeit betrifft die ausdrücklich offenen Grenzen: Umbau/Teleport,
noch ungeprüfte Zielversionen, moderne Unterwasserdarstellung und Blockeffekte.
Sie sind keine bestandenen Nachweise. Für normales Schwimmen sind die konkrete
Implementierung, Testfälle, Rohberichte und Release-Artefakte bereit.

## Historischer Verlauf

Auftrag: originales Schwimmen ab 1.13 einschließlich Haltung und Animationen.
Ausgangspunkt `09de14a97c4ba7f75c996a90afb84ad91eaafc8f`; vorhandene Boots-,
Protokollauswahl- und Entity-Push-Korrekturen bleiben erhalten.

Noch laufende Implementierung, **keine abgeschlossene Freigabe**. Sicherung der
vorherigen Artefakte und Einstellungen: `build/backups/swimming-20260922-205857/`.
Unveränderte Nutzerverwaltung PID 6604, Nutzerserver JVMs 39092 (1.12.2) und
40512 (26.2). Eigene Tests verwenden neue isolierte Loopback-Instanzen und sichern
die jeweilige Welt vor dem Aufbau des Testbeckens. Alle Testwelten bleiben erhalten.

## Bisherige Belege

- `run/test-servers/swim-1.13.2-baseline2-1790104254551752200/`: echter archivierter
  ViaForge-Client vor der Schwimmimplementierung, nur Eingaben/Beobachtung ergänzt.
  Unterwasser-Sprint bleibt aufrecht, keine Schwimmhaltung. Keine Simulation-Flags
  in diesen Bewegungsfällen; Flagfreiheit allein beweist keine Originaltreue.
  Erste Auswertung bewertet ruhende Fälle wegen zu weniger Positionspakete zu streng.
- `run/test-servers/swim-26.2-native-reference-1790104688103959300/`: offizieller
  Mojang-Client 26.2, Eingaben/Beobachtung über Agent ohne Änderung der Physik oder
  Netzwerkpakete. 12 Bewegungs-/Ausrollfälle in Survival mit Nicht-OP und LabAC ON,
  Grim erkennt Protokoll 776. Keine Flags/Setbacks, größte Abweichung etwa 7.1e-15.
- `run/test-servers/swim-1.13.2-implementation2-1790104963206118000/`: erster
  erfolgreich gestarteter ViaForge-Lauf mit Schwimmcode; Auswertung noch offen.
- Frühere `baseline`/`first-implementation`-Läufe scheiterten am Build bzw.
  Mixin-Start (geerbtes Feld `flyToggleTimer`); keine Bewegungsnachweise.

## Code und Quellen

Neue `ServerSwimming`, `SwimmingPhysics`, `SwimmingPackets`, `SwimmingFluids` und
Wasser-/Eingabemixins. Bestehende Crawling-Animation für Schwimmen verbunden.
Originale Schwimmflags/Pose/Effekte und Fluiddaten vor Verlust sichern; Anwendung
auf dem Spielthread in Paketfolge. Versionsregeln 1.13/1.14/1.16/1.17/26.1.

Originalclient-Decompilate unter `build/inspection/swimming/` und
`build/inspection/push/`, `build/inspection/movement/`; Original-JARs unter
`run/ViaForge/block-assets/`. Tatsächliche Grim-Quellen:
`build/inspection/grim/source/common/src/main/java/ac/grim/grimac/`.
Buildlogs `build/logs/swim-*.log`. Rohdaten: `client.jsonl`, `report.json`,
`plugins/ViaForgeLabAC/events.jsonl` je isoliertem Testordner.

## Weiter offen

### Fortsetzung (aktueller Zwischenstand)

HEAD weiterhin `09de14a`; sämtliche Schwimmänderungen uncommitted. Nutzer-JVMs
39092/40512 und Verwaltung 6604 bleiben erhalten. Testprozesse werden ausschließlich
vom jeweiligen Probe-Skript verwaltet; vor Wiederaufnahme Prozesse erneut prüfen.

- `swim-1.13.2-implementation2-1790104963206118000`: 12 Kernfälle PASS.
- `swim-1.14.4-matrix-core-1790105710550845900`: 12 Kernfälle PASS.
- `swim-26.2-precise-packets-1790105519713883200`: 12 Kernfälle PASS nach
  Korrektur der seit 1.18.2 verwendeten Positionspaket-Schwelle (Original 0.0002²).
- `swim-26.2-expanded-viaforge-1790106138744111200`: erweiterter Vorher-Lauf,
  echte Fehler beim gemeinsamen Sprint/Shift-Start, Auftauchen und Blasensäulenrand.
- `swim-26.2-reference-edge-1790106835307999200`: offizieller Originalclient,
  **19/19 PASS**, einschließlich Randtest auf Kopie der vorher fehlschlagenden Welt.
- `swim-26.2-input-fluid-fix-1790107022465900200`: **18/19 PASS**, Shift-Start
  und Blasensäulenrand behoben; Auftauchen bleibt FAIL. Kein Release-PASS.
- `swim-26.2-surface-pose-fix-1790107283381733400`: weiterhin 18/19 PASS;
  Restfehler beim Auftauchen maximal 0.0061318. Neueste, danach implementierte
  `CURRENT_AIR_SPRINT`-Regel ab 1.19.4 noch nicht durch Serverlauf verifiziert.

Nachgewiesene Ursachen: Eingabeverlangsamung muss vor dem neuen Tastensample
bestimmt werden; SWIMMING-Pose bleibt im ersten trockenen Tick erhalten;
Wasserinhalt schließt eine feste Strömungswand auch bei ersetzter Blockdarstellung
aus; ab 1.19.4 liest Luftbeschleunigung den aktuellen Sprintstatus statt Vor-Tick.
Grim-Quellen bestätigen die Versionsgrenze, Mojang-Mappings 1.19.3/1.19.4 zeigen
die Einführung von `Player.getFlyingSpeed`, Original 26.2 ist dekompiliert.

`fluid-states.json` und `generate_swimming_fluids.py` sichern Fluidfakten aus den
39 exakten offiziellen Blockzustands-Berichten vor Blockersetzung; Herkunft und
SHA-256 je Bericht sind enthalten. Dies beseitigt die zuvor dokumentierte
inverse Näherung für neue wasserloggbare Blöcke. Kollisionsformen neuer Inhalte
sind dadurch noch nicht automatisch vollständig implementiert.

Aktuell läuft/zuletzt gestartet: `build/logs/swim-smoke-26-build.log`, Bericht
`build/logs/swim-client-smoke-26.txt` (Protokoll 776, echter Forge-Modelltest).
Build 3 unter `swim-regression-build3.log` war erfolgreich. Seitdem neue Tests,
Pose-/Luftregel, erweitertes Live-Logging und Depth-Strider-Fall hinzugekommen.
Die früheren Python-74-PASS müssen nach diesen Ergänzungen frisch wiederholt werden.

Nächste Schritte: Smoke auswerten, frischer 26.2-Lauf mit aktueller Luftregel,
danach erweiterte Versionsmatrix (mindestens 1.13.2/1.14.4/1.16.5/1.17.1/1.18.2/
1.21.5), Modell-/Fluidregressionen, finaler 49-Profil-Smoke, Dokumentation und
Release-/Quellen-JAR samt Hash. Kein Build seit dem alten Release als freigegeben
bezeichnen. Unterwassernebel/Conduit-Darstellung und früherer nativer Clientvergleich
bleiben bisher Grenzen; echte native Bewegungsvergleiche existieren für 26.2.

### Ursprüngliche offene Punkte (teilweise inzwischen erledigt)

### Zweite Fortsetzung

- 26.2 `swim-26.2-release-matrix-1790107633224465600`: 23/23 gemessene Fälle
  ohne Flags; **Gesamtlauf nicht sauber**: sieben Simulation-Flags beim Anlegen
  der Tiefenläufer-Stiefel in `Reset depth strider`. Zwei Login-Setbacks separat.
  Moderne Vanilla-Bewegung benutzt seit 1.21 ausschließlich das synchronisierte
  Wasserbewegungsattribut (Default 0), nicht vorzeitig die Itemverzauberung. Neue
  Regel `WATER_EFFICIENCY_ATTRIBUTE` und Defaultkorrektur implementiert, noch
  frisch zu prüfen. Ein leerer Attributwert darf nicht auf Stiefel zurückfallen.
- 1.13.2 `swim-1.13.2-release-matrix-1790107825494648600`: 23/23 PASS, keine
  accountweiten Flags/Setbacks.
- 1.14.4 erster 23-Fall-Lauf FAIL bei Shift. Original `dmo`/KeyboardInput
  dekompiliert: bis einschließlich 1.14.4 wirkt weiterhin die aktuelle Shift-Taste,
  auch während Schwimmen. `SHIFT_SWIM_INPUT` gilt ausschließlich 477…498.
  `swim-1.14.4-matrix2-1790108849983238900`: 23/23 PASS; zwei Login-Setbacks.
- `swim-1.16.5-matrix2-1790109053810890100` und
  `swim-1.17.1-matrix2-1790109242758926600`: jeweils 23/23 PASS; jeweils zwei
  Login-Setbacks, keine Survival-Flags/Setbacks einschließlich Resetphasen.
- Matrix-Orchestrator (exec session 29551) läuft sequenziell weiter über 1.18.2
  und 1.21.5. Sammelliste `build/logs/swim-release-matrix.json`, Logs
  `build/logs/swim-matrix2-<version>.log`. Keine parallelen Clients.
- Gezielter Forge-Smoke 776: `build/logs/swim-client-smoke-26-2.txt` PASS.
  Vorheriger Smoke deckte ein unnötiges Fluidereignis außerhalb Y=0…255 auf;
  Ereignis wird jetzt wie das originale Fenster geclippt. 130 JUnit bestanden
  vor letzten kleinen Änderungen. Python zuletzt 76 PASS; neuer Test für Flags
  zwischen Messphasen erhöht Sollzahl auf 77 (noch frisch ausführen).
- Neue `swimming_summary` bewertet ALLE Survival-Flags/Setbacks einschließlich
  Zwischenphasen. Login-Korrekturen werden separat aufgeführt. Bereits gespeicherte
  Berichte behalten ihre Rohdaten; bei Zusammenfassung aktuelle Logik anwenden.
- `--transitions` ergänzt Tunnel-/Ufertests. Noch nicht gelaufen. Die lokale
  Darstellung berücksichtigt auch eine im niedrigen Raum festgehaltene Pose bei
  beendetem aktivem Schwimmen. Quelländerung noch im neuen Tunneltest prüfen.
- `docs/SWIMMING.md` enthält Implementierung, Versionsgrenzen, Herkunft und
  ehrliche Grenzen; Endmatrix/Release noch offen. Fluidtabellen werden einmal
  geladen, trockene Sektionen benötigen keine 4096-Byte-Speicherung.
- Nutzerlauncher ElectricLauncher JVM 108456 ist hinzugekommen und bleibt
  unangetastet. Vor nächstem Test wieder auf einen gestarteten Nutzerclient prüfen.

Frische ViaForge-Auswertung und Korrekturen; erweiterte Wasser-/Land-/Strömungsfälle,
weitere Versionen, Originalvergleich früherer Versionen, Kameraübergang,
Unterwassergeräusche/-darstellung, Regressionen, abschließende Dokumentation und
geprüfte Release-/Quellen-JAR. Moderne neu eingeführte wasserloggbare Blöcke dürfen
nicht durch inverse Näherungs-Mappings stillschweigend als exakt unterstützt gelten.
