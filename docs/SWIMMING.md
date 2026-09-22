# Versionsabhängiges Schwimmen ab 1.13

ViaForge simuliert im lokalen Forge-1.8.9-Spieler das Schwimmen des ausgewählten
Serverprotokolls. Sprint im Wasser aktiviert die flache Haltung, Blickrichtung
beeinflusst den Auf-/Abtrieb, Sprung steigt auf und Schleichen taucht ab. Die
Kollisionsbox ist in Schwimmhaltung 0,6 × 0,6, die Augenhöhe 0,4 Blöcke.
Creative-Flug und Reiten bleiben eigene Bewegungspfade. Native 1.8-Verbindungen
und Einzelspieler verwenden weiterhin die native Bewegung.

Implementiert sind Wasserreibung, Ausrollen, Tiefenläufer, Gunst des Delfins,
Blasensäulen und Aquisator-Atmung. Wasserhöhe, Strömung und wasserloggbare Zustände
werden vor verlustbehafteter Blockübersetzung erhalten. Eingaben, Fluidabtastung,
Bewegung, Pose und Paketversand laufen an getrennten Punkten des Originalticks;
es gibt weder zusätzliche Physikticks noch Grim-abhängige Koordinatenkorrekturen.

## Versionsregeln und Darstellung

| Zielversion | Unterschied |
|---|---|
| 1.13–1.13.2 | Float-Eingabebewegung, eigene Fluidoberfläche, Schleichen verlangsamt Eingaben; gleichzeitiger Sprint/Shift-Start erzeugt deshalb keine neue Schwimmhaltung. |
| 1.14+ | Double-Eingabevektor, volle Fluidhöhe bei Wasser darüber, Pose mit Kollisionsprüfung und geglättete Kameraaugenhöhe. |
| nur 1.14–1.14.4 | Die neue Haltungsverlangsamung und die aktuelle Schleichen-Taste wirken beide; dies stammt aus `KeyboardInput.tick`. Ab 1.15 entfällt diese zusätzliche Shift-Abfrage. |
| 1.16+ | Verzögerter Unterwasser-Augenstatus und originale Mindestströmung bei fast ruhendem Spieler. |
| 1.17+ | Zum Beginn des Schwimmens müssen auch die Füße in Wasser liegen. |
| 1.18.2+ | Positionspakete ab der originalen Distanzschwelle 0,0002² statt des 1.8-Werts 0,03². Relevant für kleine Wasserbewegungen. |
| 1.19.4+ | Luftbeschleunigung liest den aktuellen Sprintzustand; verhindert falschen Impuls beim Auftauchen. |
| 1.21+ | Wasserbewegung verwendet das synchronisierte Attribut `water_movement_efficiency` mit Default 0. Die Itemverzauberung darf dessen noch ausstehendes Paket nicht vorwegnehmen. |
| 1.21.2+ | Blasensäuleneffekte nach der Bewegung statt innerhalb des alten Bewegungsaufrufs. |
| 1.21.5+ | Normalisierte Tastatureingaben und quadratische Diagonalbewegung. |
| 26.1+ | Fluidtiefe relativ zur Originalbox, Augenprüfung und Mindestbetrag der angesammelten Strömung nach dem neuen Fluidtracker. |

Das Modell verwendet den originalen 26-Phasen-Armzug, Beinbewegung, Kopfneigung,
Rotation zur Blickrichtung und Übergangsinterpolation mit 0,09 pro Tick. Hautlagen
folgen den Gliedmaßen. Seit 1.14 werden aktive Handbenutzung und die schwingende
Hand entsprechend berücksichtigt; 1.13 behält seine ältere Armbehandlung.
Metadaten anderer Spieler erhalten Schwimmflag und Pose getrennt. Kameraübergänge
ab 1.14 ändern nur die Darstellung, nicht Augenposition für Physik oder Raycasts.
Unterwasser-Ein-/Austritt und der 40-Tick-Ambientloop verwenden die verifizierten
Mojang-Aufnahmen der gewählten Ressourcenversion.

## Datenpfad und Herkunft

`SwimmingPackets` erhält Flags, Pose, Wassereffekte und moderne Wasserbewegungs-
Attribute vor ViaBackwards. Die internen Ereignisse 34–36 verwenden weiterhin
das normalisierte Ereignisformat 340. Dieses Format ist **keine Serverversion**.
`SwimmingFluids` wendet abgetrennte Chunk-/Blockdaten in Paketfolge auf dem
Spielthread an; Respawn, Weltwechsel, Entladen und Disconnect verwerfen die
jeweiligen Daten. Die eigentliche zustandsbehaftete Via-Übersetzung läuft einmal.

`assets/viaforge/fluid-states.json` enthält kompakte Fluidfakten aus allen 39
offiziellen Blockzustands-Berichten ab 1.13. Der Generator
`tools/test-servers/generate_swimming_fluids.py` erfasst Server-Download und
Berichts-SHA-256. Aliasnamen entsprechen Via-Grenzen innerhalb desselben
Blockregisters und sind keine zusätzlichen getesteten Serverversionen.
Neue wasserloggbare Blöcke behalten ihren Wasserinhalt auch dann, wenn ihre
Blockdarstellung noch angenähert wird. Bei fallendem Wasser ist ein Nachbar mit
Wasserinhalt keine feste Strömungswand; eine ältere Ersatzdarstellung darf diese
Entscheidung nicht überschreiben.

Originalimplementierungen wurden aus den hashgeprüften Mojang-Clients untersucht:
1.13/1.13.2, 1.14.4, 1.17.1, 1.18.1/1.18.2, 1.19.3/1.19.4 und 26.2. Lokale Analyse unter
`build/inspection/swimming/`, `build/inspection/push/` und
`build/inspection/movement/`. Der tatsächlich installierte Grim-Commit
[`8eb5f28`](https://github.com/GrimAnticheat/Grim/tree/8eb5f28) wird mit seinen
[`PlayerBaseTick`](https://github.com/GrimAnticheat/Grim/blob/8eb5f28/common/src/main/java/ac/grim/grimac/predictionengine/PlayerBaseTick.java)-
und Wasser-/Kollisionsvorhersagen verglichen. Grim-Einstellungen bleiben erhalten.

## Reproduzierbare Prüfungen

`py -3 tools/test-servers/swim_probe.py --version 26.2 --label validation`
startet eine isolierte Loopback-Paper-Instanz mit aktivem Grim und neuem Nicht-OP.
Vor Beckenaufbau wird ihre Welt gesichert. Bestehende Laborprofile werden weder
dupliziert noch verändert. Pro Test gibt es Manifest, Client-JAR-Hash, Eingaben,
Tickdaten, Wasserzustände, Position/Velocity/Box/Animation, Paketmitschnitt,
Grim-Predictionvektoren, Flags und Setbacks. Der Testaccount beginnt Creative;
die Bewegungstests bestätigen Survival, korrekt erkanntes Zielprotokoll, ON,
Verbose und fehlende Bypassrechte vor und nach jedem Fall.

Die 23 Fälle enthalten Ruhe, Vorwärtsbewegung ohne Sprint, Sprint geradeaus und
diagonal, Blick auf-/abwärts, vier Ausrollphasen, Auf-/Abstiegstaste, kombinierte
Eingaben, Rückwärtsbewegung, Abtauchen aus bestehender Schwimmhaltung,
Tiefenläufer, Delfineffekt, Aquisator-Atmung und auf-/abwärts gerichtete
Blasensäulen einschließlich Randkontakt. Es müssen genügend echte Clientticks,
Predictions, Wasserberührung, Bewegung und erwartete Pose vorliegen. Leerer Chat,
fehlender Effekt, Creative-Ausnahme oder fehlende Teststrecke ergeben kein PASS.

`--client native` verwendet für 26.2 den hashgeprüften Originalclient mit einem
Agenten, der ausschließlich Tasten steuert und Zustand beobachtet. Die originale
Physik und Netzwerkimplementierung bleiben unverändert. `--world-template`
kopiert eine explizite gespeicherte Testwelt in eine neue Instanz. So wurden die
zuvor fehlschlagenden Blasensäulenbedingungen mit dem Original verglichen.

Die vollständige [Versions-/Fallmatrix](SWIMMING-TESTS.md) enthält 143 bestandene
ViaForge-Survivalfälle auf 1.13.2, 1.14.4, 1.16.5, 1.17.1, 1.21.5 und 26.2.
Auf 26.2 bestehen zusätzlich dieselben 28 Fälle mit dem Originalclient. In diesen
ausgewiesenen Abschlussläufen treten auch zwischen den Messfällen keine
Survival-Flags oder Setbacks auf. Einige ViaForge-Logins erzeugen weiterhin
Korrekturen; Anzahl und getrennte Bewertung stehen in der Matrix. Die gescheiterten
Zwischenstände, insbesondere die unten beschriebene Umbaufolge, bleiben erhalten.

130 JUnit-Tests und 78 Python-Werkzeugtests bestehen frisch. Die neuen Prüfungen
decken Versionsgrenzen, Originalmetadaten, Effekt-/Fluidübertragung,
Wasserzustände vor Blockersetzung, Entladen sowie belastbare Live-Auswertung
einschließlich der Resetphasen ab. `SwimmingSmokeTest` prüft im echten Forge-Modell
die fremde Pose, Augenhöhe, Phasen des Armzugs, Hautlagen und Rückkehr zur Laufpose.

## Abschließende Regressionen

Der abschließende vollständige Forge-Smoke vom 23.09.2026, 00:30 Uhr, besteht
frisch für **49/49 Ressourcenprofile**:
[PASS-Bericht](../build/logs/swim-all-profiles-final-20260923.txt) und
[Buildlog](../build/logs/swim-all-profiles-final-20260923.log). Er umfasst auch
die bisherigen Boots-, Elytra-, Entity-Push-, Protokollauswahl- und
Ressourcenregressionen. Der erste Gesamtlauf fand eine zu kurze Ausblendewartezeit
in der Handprobe: Nach zwölf Ticks war der aktuelle Schwimmblendwert 0, der
vorherige Wert aber noch etwa 0,01. Die Probe wartet nun den dreizehnten Tick ab;
eine eigene Assertion bestätigt beide Interpolationszustände. Die originale
Animationsrate und die geprüfte Bewegungsphysik wurden dafür nicht verändert.

## Release

`build.bat build` erzeugte und überprüfte
[`ViaForge-1.8.9-4.4.0-client.1.jar`](../build/libs/ViaForge-1.8.9-4.4.0-client.1.jar):
**15.695.331 Bytes**, SHA-256
`697a2ac0d281d99c3dd1cd4b25018462290d4c64f394b74f66cbeaf99eefc657`.
7988 Klassen sind Java-8-kompatibel; Forge-Registrierung und Mixin-Mappings sind
vorhanden, Entwicklungsaccounts und Testproben ausgeschlossen. Passende Quellen:
[`ViaForge-1.8.9-4.4.0-client.1-sources.zip`](../build/libs/ViaForge-1.8.9-4.4.0-client.1-sources.zip).
Beide Artefakt-Prüfsummen stehen nach Abschluss in
[`swimming-release.json`](../build/libs/swimming-release.json).

Die vorherige Release-JAR und Quellen sind unverändert unter
`build/backups/swimming-20260922-205857/` erhalten. Die Quellenbasis ist HEAD
`09de14a` plus die im Arbeitsbaum vorliegenden Schwimmänderungen. Die Mod wird
hier nicht als vollständig kompatibel mit allen Zielversionen erklärt.

## Nachgewiesene Fehler der Zwischenstände

Der archivierte Client vor dieser Änderung blieb beim Unterwasser-Sprint
aufrecht. Seine fehlenden Grim-Flags waren kein Nachweis für originales Schwimmen
(`swim-1.13.2-baseline2-1790104254551752200`). Die folgenden Fehler der ersten
Implementierungsstände wurden anschließend durch echte Schwimmticks,
Grim-Predictions und die jeweilige Originalimplementierung eingegrenzt:

- Die native 1.8-Paketschwelle ließ kleine Wasserbewegungen aus. Seit 1.18.2
  erwartet der Originalpfad die feinere Schwelle; nach deren Übernahme bestanden
  die zwölf Kernfälle auf 26.2.
- Sprint und Schleichen müssen die Verlangsamung aus der korrekten Eingabephase
  verwenden. 1.14 besitzt dabei zusätzlich eine eigene Shift-Regel. Der erste
  1.14.4-Lauf mit 23 Fällen scheiterte daran; der wiederholte Lauf bestand.
- Am fallenden Blasensäulenrand wurde Wasserinhalt eines ersetzten Nachbarblocks
  als feste Wand behandelt. Originale Fluiddaten und die entsprechende
  Strömungswand-Prüfung beseitigen diese Abweichung. Ein Originalclient fuhr auf
  einer Kopie derselben gespeicherten Testwelt.
- Beim Auftauchen gingen die noch gültige flache Pose und anschließend der
  aktuelle Luft-Sprintimpuls verloren. Die getrennten Korrekturen reduzierten
  zunächst die Abweichung von etwa 0,017836 auf 0,006132; erst die originale
  Luftregel ab 1.19.4 beseitigte den Restfehler. Diese Zwischenstände sind keine
  PASS-Nachweise (`input-fluid-fix` und `surface-pose-fix`).
- Der erste 26.2-Lauf mit 23 bestandenen Messfällen erzeugte dennoch sieben
  Simulation-Flags **zwischen** den Fällen beim Anlegen von Tiefenläufer.
  Ursache war der vorzeitige Rückgriff auf die Itemverzauberung statt des
  synchronisierten Attributs. Die Auswertung erfasst deshalb ausdrücklich auch
  sämtliche Survival-Zwischenphasen; ein solcher Lauf kann keinen Gesamt-PASS
  erhalten.

Der erste zusätzliche Ufertest (`transitions-final`) war ebenfalls kein
Gesamt-PASS: Der zu kurze Teststreifen wurde überlaufen, und der Schwimmanlauf
begann zu dicht an der Oberfläche. Obwohl keine Grim-Ereignisse auftraten,
fehlten die geforderten Start-/Endbedingungen. Der Testaufbau wurde verlängert
und sowohl mit dem Originalclient als auch mit ViaForge erneut ausgeführt.

`swim-26.2-transitions-final2-1790114993887961200` bestand zwar die 28 Messfälle,
ist insgesamt aber **FAIL**: 17 Flags und zwei Setbacks beim Streckenumbau in
`Reset shore`. Direkt vor dem Teleport wurde die vorherige Gangdecke am
Ziel (0,5; 63; 10,5) von Stein in Wasser geändert. Die Clientticks zeigen dort
zunächst weiterhin `water=false`, Bodenkontakt und die flache Kollisionsbox.
Zeitgleich entstand neuer Stein um die vorherige Spielerposition bei Z≈22.
Der Kontrolllauf des Originalclients hatte in dieser Befehlsfolge keine Flags;
die genaue Ursache der unterschiedlichen Paket-/Blockaktualisierung ist damit
noch nicht abschließend geklärt. Dies wird nicht als behobener Physikfehler
deklariert. Für den anschließenden Bewegungstest stehen beide Strecken vor dem
Befahren fest; Ufer und Gang sind räumlich getrennt, und das Teleportziel wird
nicht gleichzeitig umgebaut. Die fehlgeschlagene Umbaufolge bleibt als eigene
offene Grenze dokumentiert.

## Grenzen

Die aufgeführten Tests sind kein vollständiger Kompatibilitätsnachweis für jede
Serverversion und jeden Block. Physikvergleiche mit einem laufenden offiziellen
Originalclient existieren bisher für 26.2; frühe Versionen wurden zusätzlich am
Originalcode und mit echtem ViaForge gegen Grim geprüft. 1.13/1.13.1, 1.16 und
26.3 besitzen im bestehenden Labor weiterhin keinen verwendbaren exakt passenden
Grim-Server; diese Lücke wird nicht durch Versionsersetzung verdeckt.

Der echte Verbindungsversuch auf 1.18.2 endet schon beim Login mit Grims
Forge-Sperre für die erkannten Zielversionen 1.18.2–1.19.3 (Verweis auf
[Forge #9309](https://github.com/MinecraftForge/MinecraftForge/issues/9309)).
Für 1.18.2 liegt deshalb kein Schwimm-PASS vor. Die weiteren Versionen dieses
Sperrbereichs wurden in dieser Matrix nicht separat gestartet. Clientkennung,
Grim-Konfiguration und Bypassrechte werden dafür nicht verändert.

Der native Welt-/Renderbereich bleibt Y=0…255. Neue Blockkollisionsformen,
benutzerdefinierte Größen-/Schwerkraft-/Schleichattribute und vollständige moderne
Unterwassernebel-/Lichtdarstellung sind nicht durch die Fluidregister allein
implementiert. Zufällige seltene Unterwasser-Ambientzusätze fehlen bisher.
Bei modernen Blasensäulen ist der Zeitpunkt nach der Bewegung berücksichtigt;
die vollständige Auswertung aller während eines schnellen Ticks überstrichenen
Blockeffekt-Volumen ist noch nicht portiert. Die nachgewiesenen Säulen-/Randfälle
decken diese weitergehende moderne Kollisionslogik nicht vollständig ab.
Riptide und neue Mob-Schwimmanimationen gehören nicht zu dieser Spielerimplementierung.
Die bekannten unabhängigen 26.2-Timer-, Kollisions- und Elytra-Grenzen bleiben
offen. Boots- und Entity-Push-Nachweise werden nicht als Schwimmnachweis verwendet.
