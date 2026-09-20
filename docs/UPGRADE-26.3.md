# 26.3, Elytraflug und Weltreparatur (19.–20.09.2026)

## Originalversion und Bibliotheken

Ziel ist das veröffentlichte **Java-Release 26.3 vom 15.09.2026**, Protokoll **777**,
Java **25**, Weltformat 5023. Das wurde mit dem Mojang-Versionsmanifest und
`version.json` im Originalserver abgeglichen:

- [Mojang Release](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3)
- [Mojang Manifest](https://piston-meta.mojang.com/v1/packages/96c00d95a31328714d3811cfade2804bb050e455/26.3.json)
- Server-SHA-1: `33680f5f2ac32864d6d7cf5e56a705fdb3e05f4c`
- Client-SHA-1: `e877b6a07acd633fb3bb475002175cec036e7b87`
- Assetindex 34, SHA-1: `d8492bc61d32a4874c77daa03c0cba9201e9b83b`

Eingebunden sind [ViaVersion 5.12.0](https://github.com/ViaVersion/ViaVersion/releases/tag/5.12.0),
[ViaBackwards 5.12.0](https://github.com/ViaVersion/ViaBackwards/releases/tag/5.12.0),
[ViaRewind 4.2.0](https://github.com/ViaVersion/ViaRewind/releases/tag/4.2.0),
[ViaLegacy 3.1.0](https://github.com/ViaVersion/ViaLegacy/releases/tag/3.1.0) und
[ViaAprilFools 4.2.3](https://github.com/ViaVersion/ViaAprilFools/releases/tag/4.2.3).
ViaAprilFools bleibt eingebunden, weil es auch CombatTest8c bereitstellt. Alle
Einträge aus dessen autoritativer `APRIL_FOOLS_PROTOCOLS`-Liste werden aus der
gemeinsamen Auswahl für die einfache und vollständige Ansicht entfernt.
Gleiche numerische IDs unterschiedlicher Protokolltypen werden nicht pauschal entfernt.

26.3 hat eine eigene exakte Profilregistrierung, Originalressourcen und eine
`Protocol26_3To26_2`-Beobachtungsgrenze. Inverse Vorwärts-Mappings bewahren
darstellbare Originalblockzustände vor der verlustbehafteten Via-Übersetzung.
Original-Itempatches verwenden `VersionedTypes.V26_3` einschließlich der
Längen- und Hash-Codecs für ausgehende Inventaraktionen. Es wird kein zweiter
zustandsbehafteter Übersetzungsdurchlauf ausgeführt.

Die Prüfungen verwenden die tatsächlichen 26.3-Layouts: BitSet-Lichtmasken,
VarInt-Spielmodi, vorangestellte Partikeldaten und getrennte Geschwindigkeiten,
neue Swing-Pakete, verschobene Spieleraktionen sowie die passenden Paket-IDs.
Das Originalmodell für gefüllte Karten enthält in 26.3 keine alte
`filled_map_markings`-Textur mehr; diese wird nicht aus einer anderen Version erfunden.

**Grenzen:** ViaForge stellt weiterhin seinen implementierten Inhaltskatalog
bis 1.12.2 auf den neueren Protokollen dar. Zusätzliche 26.3-Inhalte sind dadurch
nicht automatisch nativ implementiert. ViaBackwards 5.12.0 verwirft
`ADD_TRANSIENT_BLOCK` und `POST_EFFECTS`; erweiterte Bewegungswege und
Partikelverteilungen werden für ältere Clients vereinfacht. Der native Renderer
bleibt auf Y=0..255 begrenzt. Ein erfolgreicher Paketpfad zertifiziert nicht alle
Blöcke, Kreaturen, Komponenten oder visuellen Merkmale von 26.3.

## Elytraflug

`ServerElytraFlight` wird nach der Eingabeaktualisierung des lokalen Spielers
aufgerufen. Eine neue Sprungbetätigung sendet über die gemeinsame
Paketpipeline `START_FALL_FLYING`. Bis 1.14.4 sind Fallen und das originale
Server-Metadatenflag nötig; ab 1.15 beginnt die lokale Vorhersage bereits im
Aufstieg und wird durch Server-Metadaten korrigiert. Ausgerüstete Elytra, Resthaltbarkeit, Lebenszustand,
Boden, Wasser/Lava, Reiten, Spectator und Creative-Flug werden berücksichtigt.

Die Glide-Integration übernimmt Blicksteuerung, Auftrieb, Sinkflug, Widerstand
und native Kollisionsauflösung. Positionen laufen über die normalen
Spielerbewegungspakete; Serverkorrekturen bleiben wirksam. Der Client vergibt
keine Flugfähigkeiten. Hitbox, Augenhöhe, Körperrotation und vorhandene
Flügelanimation folgen dem Flugzustand. Serverbestätigte angehängte Raketen
beschleunigen erst ab 1.11.1. Entfernte Raketen, Respawn, Disconnect und ein
neues Profil räumen den Zustand auf. Native 1.8.9 und Einzelspieler erhalten
keine Elytra-Regeln.

Die anschließende Korrektur von Kopf-/Gliedmaßenhaltung, versionsabhängiger
Flügelanimation, Original-Flugton und angehängten Raketen ist in
[ELYTRA.md](ELYTRA.md) beschrieben und mit zusätzlichen Forge-Prüfungen belegt.
Sprint-/Gleitfolgen, verzögerte Landebestätigung und niedrige Kollisions-/
Kriechposen sind in [ELYTRA-MOVEMENT.md](ELYTRA-MOVEMENT.md) beschrieben.

`live_flight_probe.py` startet einen eigenen Forge-Testclient mit einem neuen
Offline-Testspieler gegen einen echten lokalen Server. Bestehende Spieler oder
Clientinstanzen werden dafür nicht beendet oder umkonfiguriert.

## Testlabor und Reparaturgrenzen

Manifest, Webseite, Aktionsvalidierung und Tests bestimmen die Serveranzahl
aus `versions.json`. 26.3 verwendet Spielport **25638**, RCON **26638**, beide
auf **127.0.0.1**. Sichtweite bleibt 16, Simulationsdistanz 3, neue Spieler
starten in Creative und `force-gamemode=false` erhält gespeicherte Spielmodi.
Die Whitelist ist für den lokalen Offline-Zugang ausgeschaltet. Es gibt keine
automatische Adventure-Umschaltung.

Die Elytra-Station liegt bei `-58 225 -53`, fliegt nach Osten über freiem Raum
zur Wasserlandung bei `64 161 -52` und hat Rückkehr-, Ausrüstungs- und explizite
Spielmodusknöpfe. Feuerwerk gibt es nur in den passenden Versionen.

`terrain.py` prüft gespeicherte Generatoren und zusätzliches natürliches Gelände
im reservierten Bereich x=-80..79, z=-112..111, y=64..222. Die bisherigen
Exponat-Stichproben allein konnten dieses Gelände nicht erkennen. Deklarierte
Pflanzen, Stützen, Behälter, Flüssigkeitszellen, Kisten und Mobgehege sind geschützt.
Natürliche Flüssigkeiten direkt am Rand werden mit einer ein Block breiten
Glassperre am äußeren Umfang zurückgehalten; feste Blöcke und Blockentities
außerhalb werden nicht verändert. Bauten außerhalb dieses Bereichs bleiben erhalten.

Vor jedem Reparaturlauf liegt eine vollständige Weltkopie unter
`run/test-servers/<Version>/backups/exhibition-<Zeit>-<ID>/world` samt bisherigen
Manifesten und Konfiguration. Generator-NBT wird durch einen einzelnen
Bytebereichsaustausch geändert; unbekannte Tags und Spielerinventare bleiben erhalten.
Es werden keine bestehenden Welten oder Chunkdateien gelöscht. Exponate und
Beschriftungen werden anschließend gezielt repariert, nur verwaltete Katalogkisten nachgefüllt.

Die Formate unterscheiden sich: alte Presetstrings, strukturierte Flat-NBT ab
1.13, JSON ab 1.16, `structure_overrides` ab 1.18.2, namespaced `level-type` ab
1.19 und separate Generator-Daten ab 26.1. Die Originalserver 1.13–1.15 ignorieren
bei neuer Welt ihre `flat_world_options`; 1.18.2 scheitert dort an fehlenden
RegistryOps. Ausschließlich für noch nicht vorhandene Welten legt der Manager
deshalb gültige Flat-Level-NBT vor der ersten Chunk-Erzeugung an.

`generator_probe.py` verwendet getrennte neue Welten und prüft die tatsächlich
gespeicherten Schichten, einschließlich Grundgestein, Stein, Erde, Gras bei
Y=63 und Luft bei Y=64. `world_audit.py` und die Webaktion „Gespeicherte Exponate
prüfen“ erfassen jetzt auch Gelände und den gespeicherten Generator.

## Tatsächlich ausgeführte Prüfungen

- JUnit: 112 Tests, keine Fehler oder übersprungenen Tests. Python: 40 Tests, alle bestanden.
- `build.bat build runClient -x preRunClient`: erfolgreicher Build und frisch
  erzeugtes `PASS` für alle 49 Ressourcenprofile. Der Gradle-Abschluss prüft
  Datum und erste Zeile von `build/logs/block-client-smoke-test.txt`.
  Elytra-Aktionen, Metadaten und native Bewegung laufen in diesem Test durch die
  jeweiligen vollständigen Via-Paketketten. Auch die bestehende Wurftrankprüfung
  berücksichtigt nun den Metadaten-abhängigen Spawn in ViaRewind 4.2.
- Alle 49 gespeicherten Welten: 50.161 Blockpositionen und 59.092 Schilder ohne
  unerwartete Abweichungen; gespeicherte Generatoren korrekt flach und keine
  erkannten natürlichen Überlagerungen im reservierten Bereich. Die
  Reparatur-/Neustartläufe umfassen 8.106 bestandene Serverprüfungen.
  `run/test-servers/final-report.json` enthält die einzelnen Prüfzeitpunkte und
  Sicherungspfade; vorherige Abschlussberichte wurden als `.bak` erhalten.
- Echte Forge-Verbindungen und Elytraflug auf 1.9, 1.12.2 und 26.3: Start mit
  Serverbestätigung, 70 Flugticks, Blicksteuerung, Hitbox/Augenhöhe und Wasserlandung
  nach Serverteleport. Auf 1.12.2 und 26.3 wurde eine tatsächlich angehängte Rakete
  verwendet. Berichte: `run/test-servers/<Version>/live-flight-probe.json`;
  Bildschirmaufnahmen: `build/logs/screenshots/live-flight-<Protokoll>.png`.
- Originalserver-Neuwelten auf 1.9, 1.13.2, 1.15.2, 1.16.1, 1.18.2 und 26.3:
  gespeicherter Flat-Generator und tatsächliche Blockschichten bestanden.
  Bericht: `run/test-servers/generator-probe-results.json`.
- Webablauf 26.3: Start, echter Login, Weltprüfung, Speichern/Stoppen,
  gesicherte Wiederherstellung, gespeicherte Exponatprüfung und Neustart mit erneutem
  Login. Neuer Spieler startet Creative; gewählter Survival-Modus und Inventar
  überstehen Wiederherstellung und Neustart. Die Wiederherstellung ließ die
  Offline-Spielerdatei bytegenau unverändert. Sichtweite 16 wurde aus dem
  tatsächlichen Login-Paket gelesen. Bericht: `run/test-servers/26.3/web-lifecycle-probe.json`.

Die Flugtests sind kurze kontrollierte Durchläufe, kein Langzeittest aller
Serverkonfigurationen. Kollisionen verwenden die native Auflösung; nicht jede
Wandform, Fremdmod, Verzauberung und moderne Attributkombination wurde live geprüft.
Weltprüfungen vergleichen Blockidentitäten, Schilder und zusätzliches natürliches
Gelände im reservierten Bereich, nicht jedes Blockmerkmal oder die vollständige
Clientdarstellung. Frühere PASS-Berichte in anderen Dokumenten gelten nur für
ihren jeweiligen damaligen Stand.

Aktuelles Release einschließlich [Landefolgen-/Handkorrekturen](REGRESSIONS-2026-09-20.md): `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar` (14.378.697 Bytes).
SHA-256: `6f502476570b24e1b070697d9e821ae32bc31b09344e6905c1c0249ecc442d16`.
Die anschließende Hand-/Schildprüfung ist in [HANDS.md](HANDS.md) dokumentiert:
113 JUnit-Tests und erneutes vollständiges Forge-PASS für alle 49 Profile.
Die neuere [Inventarvorschauprüfung](INVENTORY-PREVIEW.md) umfasst 114 JUnit-Tests
und ein weiteres vollständiges Forge-PASS einschließlich der neuen Renderprüfungen.
26.3 wurde unter Sicherung der bisherigen Liste in `run/servers.dat` ergänzt.
