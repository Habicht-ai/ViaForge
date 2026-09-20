# Landefolgen, Handhaltung und Versionsmenü

Nachtrag zur [Elytra-Bewegung](ELYTRA-MOVEMENT.md), 20. September 2026.
Die dortigen Ergebnisse und Artefaktprüfsummen beschreiben den vorherigen Build.

## Behobene Ursachen

- **Landebestätigung:** Flug- und Kriechpose sind nun getrennte Zustände. Beide
  haben eine 0,6 Blöcke hohe Kollisionsbox; daraus darf nach gelöschtem Flugflag
  nicht automatisch Kriechen abgeleitet werden. Das führte bisher kurzzeitig zu
  verlangsamter Eingabe und konnte den Sprint für den nächsten Sprung abschalten.
  Die Pose wird nach der Bewegung aktualisiert. Erst echte fehlende Kopffreiheit
  aktiviert die Kriechpose. Das Flugflag bleibt weiterhin serverbestätigt;
  Creative-Fähigkeiten werden nicht für Elytra umgeschaltet.
- **Start:** Der Startbefehl setzt seit 1.15 weiterhin sofort die lokale
  Flugvorhersage, ändert aber nicht vorzeitig die Kollisionsbox vor der Bewegung.
  Frühere Versionen behalten ihre Sinkbedingung und warten auf das Serverflag.
- **Ich-Perspektive:** `RenderPlayer.renderRightArm/renderLeftArm` erhalten ihre
  neutrale Modellpose. Ducken, der Elytra-Kopfwinkel und die Kriechbewegung der
  Ganzkörperdarstellung überschreiben diese Pose nicht mehr. Der Renderkontext
  wird in `finally` zurückgesetzt; folgende Ganzkörperdarstellungen behalten
  Ducken und Kriechen. Normale und schmale Arme werden geprüft.
- **Rakete:** Auch eine einzelne Rakete in der rechten Haupthand verwendet den
  gemeinsamen modernen Handrenderer. Das Modell stammt aus dem tatsächlichen
  Zielclient einschließlich Display-Transformationen. Für 1.13+ ist der native
  Name `fireworks` dem originalen `firework_rocket` zugeordnet. Das Modell wird
  über denselben Importpfad wie Werkzeuge gebacken; seine native Item-ID bleibt
  erhalten. Native 1.8-Verbindungen verwenden weiterhin das native Modell.
- **Versionswahl:** Unsichtbare Zeilen überspringen die Schriftzeichnung. Die
  Klickkoordinaten stammen aus dem jeweiligen Eingabeereignis, nicht dem letzten
  gezeichneten Frame. Der doppelte Callback des nativen `GuiSlot` wird einmal
  verarbeitet. Die globale Auswahl wird beim Verlassen gespeichert, sodass
  Zeilenwechsel keine synchronen YAML-Schreibzugriffe auslösen. Die einfache
  Serverauswahl behält ihren sofortigen Callback. Größenänderungen behalten die
  Scrollposition. Der gemeinsame Aprilfools-Filter bleibt wirksam.

## Prüfungen

`HandUseSmokeTest` verwendet die echten RenderPlayer-Arme und den tatsächlichen
`ItemRenderer.renderItemInFirstPerson`-Einstieg. Er vergleicht die Raketenmatrix
mit leerer/belegter Nebenhand, beide Haupthandeinstellungen, die originale
Modellskalierung und das gebackene Texturmodell. `ElytraMovementSmokeTest` prüft
zusätzlich die unveränderte Vorbewegungsbox beim Start, fehlende Kriechbremsung
nach Landemetadaten, erhaltenen Sprint und neutrale Arme bei voll eingeblendeter
Kriechpose unter einer Decke. Das Metadaten-Testpaket verändert nur das Flugbit
und erhält andere Flags, insbesondere Sprint.

`ProtocolSelectorSmokeTest` prüft vollständige und einfache Auswahl mit echten
GUI-Callbacks und FontRenderer, doppelte Verarbeitung desselben Ereignisses,
ausgelassene unsichtbare Zeilen, Scrollposition nach Größenänderung und das
verzögerte Speichern. Es ist kein Messnachweis einer bestimmten Bildrate.

Die Live-Probe wurde auf drei unmotorisierte Sprint-/Gleitzyklen erweitert.
Danach wartet sie auf die Landebestätigung und kontrolliert die stehende Pose
sowie unveränderte Survival-Flugfähigkeiten. Sie verwendet neue Testspieler und
vorhandene freie Bodenflächen, ohne Ausstellungsblöcke zu ändern.

## Grenzen

Die Änderungen korrigieren die genannten Übergänge und Renderpfade; sie sind
keine vollständige Portierung aller neueren Bewegungsarten und Attribute.
Die native 1.8-Kollisionsauflösung bleibt Grundlage. Vollständige Bild-für-Bild-
und Tick-für-Tick-Vergleiche sämtlicher Originalclients, aller Hindernisse,
Netzwerklatenzen und Servererweiterungen stehen weiterhin aus. Die Laborprobe
belegt kontrollierte echte Verbindungen, keine allgemeine Bewegungsidentität.

## Frische Build-Prüfung und Artefakt

- 115 JUnit-Tests, keine Fehler oder übersprungenen Tests.
- 40 Python-Tests bestanden: `build/logs/regression-python-tests.log`.
- Vollständiger Forge-Smoke über alle **49** registrierten Ressourcenprofile:
  `build/logs/block-client-smoke-test.txt` beginnt mit `PASS`, frisch abgeschlossen
  am 20.09.2026 um 20:22 Uhr. Build: `build/logs/regression-release-full.log`.
  Der fokussierte 26.3-Lauf ist in `regression-focus-smoke-3.txt` dokumentiert.
  Die ersten beiden fokussierten Läufe schlugen an Testfixture-Problemen fehl
  (nicht zurückgesetzter Kriechblend und versehentlich gelöschtes Sprintflag);
  sie bleiben als fehlgeschlagene Berichte erhalten und zählen nicht als PASS.
- Release-JAR: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, **14.378.697 Bytes**.
  SHA-256: `6f502476570b24e1b070697d9e821ae32bc31b09344e6905c1c0249ecc442d16`.
  Neue Arm-Mixinregistrierung und Kontextklasse sind im Artefakt enthalten;
  Entwicklungsproben sind nicht enthalten.
- Echte Vanilla-Server **1.12.2, 1.15 und 26.3**: alle am 20.09.2026 zwischen
  20:23 und 20:25 Uhr bestanden. Jeweils 70 Ticks freier Flug, Raketenantrieb,
  angehängte Rakete, laufende Original-Sound-Engine, Handanimation, Inventar
  während des Fluges, Wasserlandung, **drei** unmotorisierte Sprint-/Gleitzyklen
  und abschließend serverbestätigtes Stehen (`standing_after_landing: true`).
  Aufstiegsaktivierung ist auf 1.12.2 `false`, auf 1.15 und 26.3 `true`.
  Berichte: `run/test-servers/<Version>/live-flight-probe.json` und
  `build/logs/regression-live-servers.log`. Vorherige JSON-Berichte wurden als
  zeitgestempelte `.bak` erhalten. Der eigens gestartete 1.15-Server wurde
  gespeichert und gestoppt; die schon laufenden Server 1.12.2 und 26.3 blieben an.
