# Mittelklick und Platzierungssound, 2026-09-23

## Präzisierter Fehler und Nachweis

Der Nutzer hat den Ablauf präzisiert: Erde oder Quarz per Mittelklick aus der
Welt aufnehmen, rechtsklicken, einen Slot nach rechts scrollen und mit scheinbar
leerer Hand erneut rechtsklicken. Die früheren Creative-Baumenütests prüften einen
anderen Eingabepfad. Ihre Ergebnisse bleiben erhalten, sind kein Gegenbeweis.

Unveränderter Produktionscode, echter ViaForge-Client gegen Paper 26.2 Build 126,
Protokoll 776, Grim `2.3.74-8eb5f28`, neuer Nicht-OP-Account, Creative, Checks ON:
`run/test-servers/hotbar-26.2-pick-baseline-1790120101865381400/`.
Erster Mittelklick: Client-Inventarindex 0 enthält Erde, ausgehendes
`C10PacketCreativeInventoryAction` schreibt **37**, nicht **36**. Der Server hält
die Erde im benachbarten Slot. Erste Platzierung scheitert; Rechtsklick nach
Scrollen mit clientseitig leerem Slot platziert serverseitig Erde.
Alle vier Blockprüfungen des Ablaufs schlagen fehl. Der zweite Durchlauf mit
Quarz beginnt bereits mit dem entstandenen Inventarversatz und wird daher nur
als Folgefehler, nicht als unabhängiger sauberer Ausgangszustand gewertet.

Ursache: Das originale Forge-1.8.9-`Minecraft.middleClickMouse` berechnet den
Slot mit `inventoryContainer.inventorySlots.size() - 9 + currentItem`.
ViaForge ergänzt im Spielercontainer die Nebenhand als Slot 45; die Größe steigt
von 45 auf 46. Die Hotbar bleibt aber auf 36–44. Der normale Creative-Menüpfad
verwendet seinen eigenen Container und war deshalb nicht betroffen.

`MixinHandInput` korrigiert ausschließlich das Slotargument dieses Mittelklick-
Pakets auf `36 + currentItem`, wenn die moderne Zweithandfunktion aktiv ist.
Forge wählt weiterhin das Item und vorhandene passende Stacks selbst; NBT,
Itemübersetzung, Survival-Pick und native 1.8-Verbindungen bleiben im Originalpfad.
Es gibt keine zusätzliche Inventarsimulation oder Ersatzpaketfolge.

Mit nur dieser Slotkorrektur:
`hotbar-26.2-pick-fixed-sound-baseline-1790120243914734900`: vier Platzierungsfälle
bestanden, beide Mittelklick-Schreibpakete Slot 36. Dieser tatsächlich ausgeführte
Build enthielt noch keine Klasse `PlacementSounds`. Die Soundaufzeichnung enthält
keinen Platzierungssound trotz erfolgreicher Blockplatzierung.

## Platzierungssound

Die 1.8-Itemimplementierung ruft bei erfolgreicher lokaler Platzierung
`World.playSoundEffect` auf. Dessen clientseitiger `RenderGlobal.playSound`-Empfänger
ist leer. Der neuere Server erwartet lokale Wiedergabe beim platzierenden Spieler;
die vorhandene Übersetzung liefert ihm keinen ersetzenden Platzierungssound.

`MixinPlacementSound` ergänzt die Wiedergabe nur während einer lokalen modernen
Handinteraktion in `WorldClient`. `PlacementSounds` verlangt, dass der angeforderte
Sound zum Soundtyp des platzierten Blocks passt. Es spielt das `.place`-Ereignis
des geladenen Zielversionskatalogs in Kategorie BLOCKS, mit den Lautstärke-/Pitch-
Werten der Itemimplementierung. Die bisherige native Weltbenachrichtigung bleibt
erhalten. Serverseitige Soundpakete laufen über eine andere Methode und lösen die
Ergänzung nicht aus. Die lokale 1.8-/Einzelspieler-Soundstrecke bleibt unverändert.

`MobSoundAssets` lädt zusätzlich die Platzierungsereignisse und deren referenzierte
Originalaufnahmen aus dem bereits gepinnten Mojang-Index. SHA-1-/Größenkontrolle
und bestehendes Ressourcenlimit bleiben erhalten. Wolle/Slime berücksichtigen die
Umbenennung im vorhandenen Zielkatalog; Metall/Glas werden anhand ihres Soundtyps
unterschieden. Dies erweitert keine bislang unrepräsentierten modernen Blocktypen
und behauptet keine vollständige neue Materialzuordnung für jeden späteren Block.

Erster gemeinsamer Fixlauf:
`hotbar-26.2-pick-sound-fixed-1790120434736005600`: vier Fälle bestanden. Erde
erzeugt genau `block.gravel.place`, Quarz genau `block.stone.place`, Kategorie
BLOCKS, Lautstärke 1, Pitch 0,8, Position (0,5;64,5;0,5). Leere Hand erzeugt keinen
Platzierungssound. Der Probe beobachtet nur Soundereignisse und steuert die echten
Mittelklick-/Rechtsklick-Tasten bzw. die native Mausrad-Inventarmethode.

## Regressionen und Ergebnis

`PickBlockSmokeTest` ruft den echten Mittelklick-Handler für alle neun Hotbar-Slots
auf, prüft vorhandene Stacks und Survival ohne Creative-Schreibpaket. Außerdem
prüft er registrierte Platzierungsereignisse und lesbare Ogg-Dateien der
Zielversion. Er läuft im vollständigen Forge-Smoke und zusätzlich im nativen
1.8-Kontrollzustand. Der Live-Probe prüft serverseitigen Block und Slot getrennt
vom Clientinventar und verlangt optional genau eine tatsächlich erzeugte Audioquelle.
Python-Evidenztests erkennen fehlende/doppelte/falsche Sounds und ungültige Prüfzustände.

Der erste 1.12.2-Gegencheck
(`hotbar-1.12.2-pick-sound-final-1790120915297696200`) ist ausdrücklich kein PASS:
Das vor dem Login ausgeführte `/fill` scheiterte an ungeladenen Chunks. Die
natürliche flache Welt hatte den Boden einen Block tiefer; der Client zielte
deshalb auf eine andere Platzierungsposition. Die Konsole enthält dazu
`Cannot place blocks outside of the world`, die Audioquelle lag auf y=63,5
statt 64,5. Der Treiber baut die kleine Fläche bei alten Versionen nun nach
dem Laden durch den Testspieler, positioniert diesen erneut und prüft Boden,
Zielblock und Randpunkte vor dem Mittelklick. Der fehlgeschlagene Lauf bleibt
mit allen Daten erhalten; Produktionscode wurde dafür nicht verändert.

Der vollständige Forge-Smoke über alle 49 Ressourcenprofile ist frisch bestanden:
`build/logs/pick-sound-all-profiles-20260923.log` / `.txt` (beginnt mit `PASS`).
84 Python-Tests sind frisch bestanden (`build/logs/pick-sound-python-tests.log`).
130 JUnit-Tests wurden im Build mit dem vollständigen Smoke frisch ausgeführt:
keine Fehler, keine übersprungenen Tests.

Alle folgenden Live-Läufe verwenden den echten ViaForge-Forge-1.8.9-Client,
Grim `2.3.74-8eb5f28`, neue Nicht-OP-Accounts in Creative, Checks ON, echtes
Verbose und keine Exempt-/NoSetback-/NoModifyPacket-Rechte. Grim erkennt jeweils
die angegebene Zielversion und das Protokoll, Clientmarke `fml,forge`.

| Zielversion | Paper-Build | Erkanntes Protokoll | Erde / leer / Quarz / leer | Audioquellen | Flags / Setbacks während der Fälle | Setbacks beim Login/Setup |
| --- | ---: | ---: | --- | --- | --- | ---: |
| 26.2 | 126 | 776 | 4/4 PASS | 1 / 0 / 1 / 0 | 0 / 0 | 35 |
| 1.21.5 | 114 | 770 | 4/4 PASS | 1 / 0 / 1 / 0 | 0 / 0 | 2 |
| 1.12.2 | 1620 | 340 | 4/4 PASS | 1 / 0 / 1 / 0 | 0 / 0 | 0 |
| 1.9.4 | 775 | 110 | 4/4 PASS | 1 / 0 / 1 / 0 | 0 / 0 | 0 |

Je Fall 39–40 korrelierte Client-Endticks. Beide Creative-Schreibpakete pro Lauf
schreiben Slot 36, mit den richtigen Items. Die Fälle enthalten weder einen
Disconnect noch ein Setback; das anschließende Beenden des eigenen Testclients
gehört zum kontrollierten Abschluss. In den gesamten vier finalen Sitzungen
gab es keine Flag-Ereignisse. Die aufgeführten Login-/Setup-Setbacks bleiben
als offene, separate Bewegungs-/Verbindungsgrenze erhalten. Dies ist eine
Creative-Inventar- und Soundprüfung, keine Survival-Bewegungszertifizierung.

Vollständige Berichte und JSONL unter `run/test-servers/`:

- `hotbar-26.2-pick-sound-final-1790120841720149900/`
- `hotbar-1.12.2-pick-sound-final2-1790121110845695900/`
- `hotbar-1.21.5-pick-sound-final2-1790121168080221300/`
- `hotbar-1.9.4-pick-sound-final2-1790121253813451300/`

Die maschinenlesbare Zuordnung steht in `build/logs/pick-sound-matrix.json`;
`build/libs/pick-sound-release.json` enthält erneut ausgewertete Fälle,
Setup-Ereignisse, Artefaktgrößen und SHA-256. Die konkreten Vergleiche mit
Originalcode beziehen sich auf den tatsächlich verwendeten Forge-1.8.9-
Mittelklick-/Platzierungspfad und gepinnte Zielversions-Soundressourcen.
Ein zusätzlicher nativer Zielversionsclient wurde für diese Inventarprüfung
nicht ausgeführt. Spätere, vom geerbten Soundtyp abweichende Materialfamilien
aller Blöcke sind nicht vollständig implementiert oder live zertifiziert.

Vorherige Artefakte: `build/backups/pick-block-20260923/`.
Der normale 26.2-Laborserver (PID 91468 beim Start dieser Untersuchung) hatte null
Spieler und blieb unangetastet. Testwelten sind isoliert und gesichert; es wurden
keine Nutzerinventare oder bestehenden Welten verändert. Kein Nutzerclient wurde
beendet. Alle bisherigen Schwimmänderungen bleiben erhalten.
Alle eigenen Live-Testclients und Testserver sind beendet. Der normale
26.2-Server blieb auf PID 91468 aktiv, zuletzt null Spieler und Grim ON.
Clientoptionen und ViaForge-Konfiguration sind auf ihre vorherigen Bytes
zurückgesetzt. Es sind keine weiteren laufenden Tests nötig.

Release: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, passende Quellen:
`build/libs/ViaForge-1.8.9-4.4.0-client.1-sources.zip`, jeweils mit `.sha256`.
Die normale Entwicklungs-JAR unter `run/mods/ViaForge-development.jar`
enthält ebenfalls die Korrekturen. Der Release enthält keine Probe-Klassen.

## Reproduktion

Ohne bereits laufenden Minecraft-Client, aus dem Repository:

```powershell
$env:VIAFORGE_HOTBAR_PICK='1'
$env:VIAFORGE_HOTBAR_SOUND='1'
$env:VIAFORGE_HOTBAR_SOURCE='1'
py -3 tools/test-servers/hotbar_probe.py --version 26.2 --label pick-sound
```

`--version` wählt die exakt installierte Plattform; hier zusätzlich geprüft:
1.12.2, 1.21.5 und 1.9.4. Der Treiber erzeugt eine isolierte, erhaltene Instanz,
sichert die Testwelt vor dem Aufbau und stellt die Clientoptionen anschließend
wieder her. Die normale Web-Profilliste wird nicht erweitert.

Je Block: leerer neuer Creative-Account, Auswahl Slot 1, zwei Sekunden nach
Mittelklick, zwei Sekunden nach Rechtsklick, Zielblock per Server entfernen,
eine Sekunde Ruhe, einen Slot nach rechts scrollen, eine Sekunde warten und
zwei Sekunden nach dem Rechtsklick mit leerer Hand prüfen. Erde und Quarz
werden unabhängig durch vorheriges Leeren dieses Testinventars vorbereitet.
Blockzustand, ausgewählter Serverslot und Clientstack werden separat geprüft.
Die Soundprüfung verlangt genau ein registriertes Zielereignis und eine
wirkliche Audioquelle; Lautstärke, Pitch und Position werden aufgezeichnet.
