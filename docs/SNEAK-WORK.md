# Sneak/Sprint: Arbeitsstand 23.09.2026

## Aktueller Abschlussstand, 18:48

Die folgenden älteren Abschnitte sind ein chronologisches Arbeitsprotokoll;
ihre offenen Schritte sind durch diesen Abschlussstand überholt.

* Ursache auf 26.2 trocken reproduziert: 54 Simulation-Flags mit gesicherter
  Ausgangsphysik; Original 26.2 ohne Flags. Gemeinsamer Eingabe-/Sprintpfad,
  Posezeitpunkt, Normalisierung und Sneak-Geschwindigkeit gezielt korrigiert.
* Abschluss: 377 ViaForge-Survivalabschnitte auf 1.13.2, 1.14.4, 1.15, 1.19.4,
  1.21.4, 1.21.5 und 26.2 sowie 68 Original-26.2-Vergleichsabschnitte bestanden.
  Darin 97 frische Schwimmregressionen, Swift Sneak trocken/unter Wasser,
  Attributrandwerte und exakte Sprint-Doppeltippfolgen. Keine Survival-Flags
  oder -Setbacks einschließlich Reset-/Ausrüstungs-/Zwischenphasen.
* 132 JUnit-Tests und 91 Python-Tests bestanden. Vollständiger Forge-Smoke
  über 49 Ressourcenprofile frisch PASS am 23.09.2026, 18:44:38:
  `build/logs/sneak-client-smoke-test-final.txt` und zugehöriges Konsolenlog.
  Darstellung/Kamera/Modelle und bestehende Boot-/Elytra-/Blockpfade im Smoke;
  keine Behauptung von 49 Grim-Bewegungstests.
* Finale Matrix und Grenzen: `docs/SNEAK-TESTS.md`; 27 gehashte Berichte
  einschließlich Fehlläufen: `docs/SNEAK-EVIDENCE.json`. Rohdaten/Welten/Backups
  bleiben unter `run/test-servers/` erhalten. Normale Profile weiterhin 49.
* Release und passende Quellen unter `build/libs/`; Nachweis einschließlich
  SHA-256 in `build/libs/sneak-release.json`. Nach der letzten Dokumentations-
  ergänzung wird das Quellenarchiv nochmals gebaut und dateiweise abgeglichen.
  Keine Entwicklungsbeobachter in der Release. Alte Artefakte bleiben gesichert.
* Prozessprüfung nach dem Smoke: keine Java-Clients oder Spielserver aktiv.
  Keine Nutzerinstanz beendet; Testinstanzen regulär gestoppt. HEAD unverändert,
  Änderungen nicht committed. Kein weiterer Testprozess muss fortgesetzt werden.

Weiterhin offen: getrennte Login-Korrekturen; Original-Livevergleiche älterer
Zielversionen; bekannte Forge-Sperre auf 1.18.2; nicht getestete Spezialfolgen
mit modernen Item-Mobilitätskomponenten/streifenden Kollisionen. Auf 1.14.4
ist eine Sprint-Verhaltensabweichung vor dem Fix nachgewiesen, aber dort wurden
keine Simulation-Flags reproduziert. Keine pauschale Fehlerfreiheit behauptet.

## Historischer Ausgangsstand

Ausgang: HEAD `d1d531c585689e14b6a00f8560a612d214ca07f6`, sauber. Keine
AGENTS.md im Repository oder darüber gefunden. Nutzer meldet 26.2, gelegentlich
Sneak beim Laufen und Springen; sein Spielmodus bleibt unbekannt. Normale
Nutzerlogs enthalten 120 Simulation-Flags, aber keine belastbare Tastenfolge.
Originalartefakte und Ereignisdatei: `build/backups/sneak-20260923-initial/`.
Nutzerwelten und normale Profile wurden nicht verändert.

## Reproduktion und Korrektur

Alle genannten Bewegungsprüfungen: echter Client, neuer Nicht-OP, Survival,
unveränderte Grim-Konfiguration, LabAC ON, Loopback, isolierte und erhaltene Welt
mit Backup vor dem Aufbau. Originalclientinstrumentierung steuert Tasten und
beobachtet Zustände; Physik und Netzwerkpfad bleiben original.

Unter `run/test-servers/`:

* `sneak-26.2-initial-land-1790122339052591100`: unveränderte Produktionsphysik,
  30 Abschnitte, 54 Simulation-Flags, keine Survival-Setbacks. Sneak beim Gehen
  lösen: sieben Flags, maximal 0.06859998068882914. Sprint dann Sneak: vier Flags,
  maximal 0.009799959468125202. Kontrollen ohne Sneak sauber.
* `sneak-26.2-original-land-1790122527520487000`: Original 26.2, 30/30 PASS,
  keine Flags/Setbacks. Etablierter Sprint bleibt beim Sneaken erhalten.
* `sneak-26.2-first-fix-land-1790122835850095900`: 30/30 PASS, keine
  Survival-Flags/Setbacks, zwei Login-Korrekturen gesondert.
* `sneak-26.2-extended-fixture2-1790176199646990800`: weitere Abweichung mit
  synchronisiertem sneaking_speed und Swift Sneak reproduziert. 81 Flags und
  41 Setbacks nach Survival-Beginn. Zusätzlich zu kurzer Kriechtunnel-Auslauf
  im Prüffall; kein PASS für diese Runde.
* `sneak-26.2-original-fixture2-1790176562514962600`: Original 54/55, keinerlei
  Grim-Probleme; derselbe zu kurze Tunnel-Auslauf scheitert auch im Original.
* `sneak-26.2-attribute-fixed-1790176902810831900`: 55/55 PASS, keine
  Survival-Flags/Setbacks einschließlich Reset/Ausrüstung; Creative separat
  ohne Flags/Setbacks. Zwei Login-Korrekturen. Tunnel-Auslauf auf sieben Sekunden
  verlängert; erzwungenes trockenes Kriechen und anschließendes Stehen beobachtet.

Nachgewiesen: 1.8-Eingabeverlangsamung und Sprintabbruch blieben auf Land aktiv;
Vorher-/Nachher-Tick der Sneak-Eingabe passte nicht zum Zielclient. Gemeinsamer
Eingabe-/Sprintpfad korrigiert, doppelte Verlangsamung entfernt, quadratische
Eingabe ab 1.21.5 am originalen Zeitpunkt angewandt. Swift Sneak ab 1.19 aus
erhaltenem Original-NBT, ab 1.21 synchronisiertes Attribut vor Via-Verlust.
Keine weiteren Physikticks oder Paketfilter.

Originalquellen: `build/inspection/sneak/source-verification.json` prüft
offizielle Client- und Mapping-SHA-1 gegen Mojang-Metadaten; dekompilierte
Grenzversionen in diesem Verzeichnis. 26.2 unter `build/inspection/push/26.2/`.
Installierter Grim-Build und lokaler Quellstand: Commit
`8eb5f2809591c891deb4958bb2927844871e0600`.

## Aktuell und noch offen

131 JUnit-Tests und Forge-Smoke für Protokoll 776 bestanden; Python inzwischen
91 Tests. Die zusätzliche Nullwert-JUnit-Regression wartet noch auf den Gesamtbuild.
Frischer Bericht: `build/logs/sneak-target-smoke-20260923.txt` beginnt mit PASS.
Noch keine abschließende Release-Freigabe: vollständiger Forge-Smoke, weitere
Versionsgrenzen, Schwimmregressionen und finales Original-/ViaForge-Paar fehlen.

Original `sneak-26.2-original-final-1790177474153259100` und ViaForge
`sneak-26.2-final-1790177851976950900` bestehen jeweils 62/62 Survival-Abschnitte
einschließlich echter Treppen und Land-zu-Wasser-Eintritt. Je 28 Sprintaktionen,
keine doppelten Starts/Stops. Original ohne Loginprobleme, ViaForge zwei getrennte
Login-Korrekturen. Keine Survival- oder Creative-Flags/Setbacks. Der Stand dieser
62 Fälle ist zusätzlich unter `build/backups/sneak-20260923-62-cases/` gesichert.

Seitdem: rechnerisch nachgewiesener NaN-Randfall bei sneaking_speed=0 korrigiert,
Originalcode kehrt vor der zweiten Normalisierung mit dem Nullvektor zurück.
Beweis vorher `build/inspection/sneak/zero-speed/before.txt`, neue JUnit-Regression.
Original-/ViaForge-Randwertläufe und ausgewählte Grenzversionen laufen sequentiell
über `build/logs/sneak-boundary-matrix.txt` (bei Fehler automatischer Halt).
Randwerte 0, 1 und 0.3 inzwischen jeweils 3/3 PASS mit Original und ViaForge,
keine Survival-Probleme; beide Randwertläufe hatten zwei Login-Korrekturen.
1.13.2, 1.14.4 und 1.15 inzwischen je 30/30 PASS, einschließlich aller Resets.
1.13.2 ohne Login-Korrekturen, 1.14.4/1.15 jeweils zwei, keine Bewegungsflags.
1.19.4 läuft erweitert (Swift Sneak ohne modernes Attribut), danach 1.21.4/1.21.5.
Korrektur 18:03: erster 1.19.4-Lauf FAIL, 60/61. Swift-Sneak-Abschnitt war
versehentlich noch im Wasser und meldete Faktor 0.3, insgesamt 59 Flags dort.
Kurzer trockener Nachweis `sneak-1.19.4-swift-dry-before-1790179270289381600`
erzeugt 58 gemessene Flags (max. 0.008843996707975213), weiterhin Faktor 0.3.
Tatsächlich empfangenes NBT in client.jsonl zeigt die Verzauberung im eigenen
`ViaForge|flattenedItem.tag.Enchantments`, ohne das angenommene 1.19-VB-Backup.
Code liest jetzt diesen Original-Snapshot. Forge-Regression mit dem tatsächlich
beobachteten NBT ergänzt. Neuer kompletter 1.19.4-Lauf mit ausdrücklich trockenem
Ausrüstungsabschnitt, danach 1.21.4/1.21.5: `build/logs/sneak-boundary-matrix2.txt`.
Der zuerst fehlgeschlagene Wasserfall wird zusätzlich über `--swift-water-only`
frisch wiederholt. Seine Vorher-Flags werden nicht als trockene Flags ausgegeben.
Fortsetzung 18:21: 1.19.4 61/61 PASS, 1.21.4/1.21.5 je 30/30 PASS, keine
Survival-Probleme, je zwei Login-Korrekturen. Swift-Sneak-Wasserwiederholung
`sneak-1.19.4-sneak-regression-final-1790180508680893300`: 1/1 PASS, Faktor .75.
1.14.4-Ausgangs-JAR `sneak-1.14.4-pre-sneak-land-1790180576389642800`:
keine Flags, aber Sprint beim Sneaken beendet, 29/30 Verhaltensprüfungen.
Der Orchestrator stoppte wegen seiner fälschlichen Erwartung eines Flags;
die Diagnose wertet das ehrlich als Verhaltensabweichung ohne Grim-Reproduktion.
Schwimmregressionen 1.13.2/1.14.4/1.21.5/26.2, danach Original/ViaForge
Sprint-Doppeltippen: `build/logs/sneak-regressions-final2.txt`, sequentiell.
1.13.2 und 1.14.4 inzwischen jeweils 23/23 Schwimmfälle PASS einschließlich
Reset-/Ausrüstungsphasen, keine Survival-Probleme. Die normale Profilliste hat
unverändert 49 Einträge. `git diff --check` unter der tatsächlichen Git-Konfiguration
ist sauber; temporäres Abschalten von autocrlf führte nur zur falschen Bewertung
der vorhandenen Windows-Zeilenenden, ohne Dateien zu ändern.
Noch offen: Vorhervergleich 1.14.4 mit gesichertem Ausgangs-JAR, frische
Schwimmregressionen, voller Forge-Smoke, Gesamtbuild und Release-Metadaten.
Entwicklungsbeobachter erfasst
zusätzlich Eingabe-, Sprint-, Physik-, Pose- und Paketphasen. Vor jeder Fortsetzung
Prozesse und jüngste Berichte erneut prüfen; nur eigene Testprozesse verwalten.
Build-Artefakte sind Zwischenstände; bisherige Release bleibt im Backup erhalten.
