# Elytra: Flugdarstellung und Ton

Stand: 20. September 2026. Ergänzung zum Flugcontroller in [UPGRADE-26.3.md](UPGRADE-26.3.md).

Die anschließende versionsabhängige Korrektur der Flugansicht im Survival- und
Creative-Inventar ist in [INVENTORY-PREVIEW.md](INVENTORY-PREVIEW.md) dokumentiert.
Die Korrektur von Sprint-Sprung, Aufstiegsaktivierung, Bodenkontakt und
Kriechpose folgt in [ELYTRA-MOVEMENT.md](ELYTRA-MOVEMENT.md).
Das später ergänzte [Schwimmen ab 1.13](SWIMMING.md) verwendet dieselben
Modellübergänge mit eigener Wasserphysik und erhaltener fremder Schwimmpose.
Seine Live-Nachweise sind von den historischen Elytra-Läufen unten getrennt.

Die bisherige Darstellung drehte den Körper, ließ aber die native 1.8-Laufpose
weiterlaufen. Außerdem stieg die native Feuerwerksentität unabhängig vom
bereits korrekt bestätigten Boost auf. Der lokale Elytra-Flugton fehlte.

## Umsetzung

- Nach vier Flugticks nimmt der Kopf den originalen Winkel von −45° ein.
  Arm- und Beinbewegungen werden mit der dritten Potenz der quadratischen
  Geschwindigkeit gedämpft. Der Bewegungsersatz aktualisiert auch die
  Animationszeit; Handhaltung, Angriff und Skin-Overlays bleiben angebunden.
- Der Übergang in die Körperrotation verwendet auch bei anderen Spielern
  deren tatsächliche Flugticks. Flügelstellung folgt Sinkrichtung und Bewegung.
  Bis 1.21.1 gilt die originale Glättung pro Renderaufruf; ab 1.21.2
  (`TICKED_ELYTRA_WINGS`) die Aktualisierung mit Faktor 0,3 pro Tick und
  Interpolation. Sie läuft auch aus der Ich-Perspektive weiter.
- `item.elytra.flying` einschließlich seiner Ogg-Aufnahme wird aus dem
  SHA-1-geprüften Mojang-Assetindex der gewählten Version geladen. Der lokale
  Loop verwendet die Kategorie „Spieler“, 20 stille Anfangsticks, anschließend
  20 Ticks Einblendung sowie geschwindigkeitsabhängige Lautstärke/Tonhöhe.
  Flugende, Tod, Disconnect, Respawn und Ressourcenwechsel räumen ihn auf.
- Eine über Original-Metadaten angehängte Boost-Rakete folgt dem Spieler.
  Ihr Entitätsmodell wird wie im Original ausgeblendet; die Rakete in der Hand
  bleibt sichtbar. Ein Funkenpartikel entsteht pro Tick am richtigen Ort.
  Vor 1.18.2 liegt die Spur 0,3 Blöcke unter der Raketenposition; ab 1.18.2
  berücksichtigt `FIREWORK_HAND_TRAIL` die Handseite, Hauptarm und Blickrichtung.
  Leere moderne Attachment-Metadaten werden als „nicht angehängt“ behandelt.
- Die Darstellung berechnet keinen weiteren Boost. Freie Raketen steigen
  weiterhin auf. Bei neueren Servern kommt das Startgeräusch bereits über die
  gemeinsame Sound-Pipeline; der zusätzliche lokale 1.8-Startton entfällt.
  Raketenantrieb bleibt auf Versionen ab 1.11.1 begrenzt.

Grundlage sind die lokal mit geprüfter Mojang-Prüfsumme vorhandenen
Originalclients: `ModelBiped`/`RenderPlayer`/`ModelElytra`/`ElytraSound` und
`EntityFireworkRocket` der älteren Versionen sowie `HumanoidModel`,
`ElytraAnimationState`, `ElytraOnPlayerSoundInstance` und `FireworkRocketEntity`
der modernen Versionen. Die Versionsgrenzen wurden insbesondere an 1.18.1/
1.18.2 und 1.21.1/1.21.3 verglichen. Lokale Untersuchungsdateien liegen unter
`build/inspection/flight/` und werden nicht mit der Mod ausgeliefert.

## Prüfungen

`ElytraFlightSmokeTest` verwendet native Spielermodelle, Bewegungs- und
Raketenentitäten unter Forge. Spawn, Flugflag und Rocket-Attachment durchlaufen
die vollständigen jeweiligen Via-Paketketten. Geprüft werden Kopf/Arme/Beine,
Wiederherstellung der Laufpose, Flügel-Zeitbasis, Auflösung der Ogg-Datei,
Sound-Einblendung, Position/Unsichtbarkeit der Rakete, Funkenposition,
Nebenhandwechsel, Lösen der Rakete und das Ausbleiben eines zweiten Boosts.

`live_flight_probe.py` prüft zusätzlich echte Verbindungen zu den lokalen
Vanilla-Servern 1.9, 1.12.2 und 26.3 mit eigenen neuen Testspielern. Die Berichte
weisen `original_flight_sound_playing` und bei den Raketen-Versionen
`attached_rocket_visual` nach. Das Sound-Ergebnis prüft die laufende Sound-Engine;
es ist keine menschliche Hörprüfung der Kopfhörerausgabe.

Am 20.09.2026 tatsächlich bestanden:

- Vollständiger Build mit Forge-Smoke für alle 49 registrierten Ressourcenprofile,
  Abschluss 14:05 Uhr; frisch erzeugtes `PASS` in
  `build/logs/block-client-smoke-test.txt`. Build-Protokoll:
  `build/logs/elytra-visual-release-full-2.log`.
- 112 JUnit-Tests ohne Fehler oder übersprungene Tests und 40 Python-Tests.
- Echte Flugabläufe 1.9, 1.12.2 und 26.3, jeweils 70 Flugticks, anschließend
  Serverteleport ins Wasser und Flugende. Alle drei berichten einen laufenden
  Original-Flugton; 1.12.2 und 26.3 zusätzlich eine unsichtbare, mitgeführte
  Boost-Rakete. Berichte: `run/test-servers/<Version>/live-flight-probe.json`.
- Original-Aufnahmen per SHA-1 geprüft: 1.9/1.12.2
  `90d599a1760137a7c4d7a52c77f5c65c02f137d4`; 26.3
  `75c3df1293a7a559a780b47d2f9e630e7b0a46ff`. Die neuere Aufnahme wird tatsächlich
  verwendet, nicht durch die alte Datei ersetzt.

Aktuelles Release einschließlich Handanimation und Inventarvorschau
([HANDS.md](HANDS.md), [INVENTORY-PREVIEW.md](INVENTORY-PREVIEW.md))
und [Landefolgen-/Handkorrekturen](REGRESSIONS-2026-09-20.md):
`build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, 14.378.697 Bytes, SHA-256
`6f502476570b24e1b070697d9e821ae32bc31b09344e6905c1c0249ecc442d16`.
Die Entwicklungsproben sind nicht in dieser Release-JAR enthalten.

Es gibt keine automatische Pixel-für-Pixel-Gegenüberstellung aller Kamerawinkel
mit allen Originalclients. Die Echtservertests sind kurze Flugabläufe;
Fremdmods und Ressourcenpakete wurden dabei nicht systematisch verglichen.
