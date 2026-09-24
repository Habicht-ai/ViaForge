# Interaktionen und dünne Kollisionsformen

Ausgangspunkt ist `ab744f6`, nach der Sneak-/Sprintkorrektur. Gemeldet wurden
NoSlow beim Schwertblocken, Flags an Leitern/Seerosen, PacketOrderB beim Angriff
und BadPacketsH bei Rechtsklick/Abbau. Ausgewählt war 26.2; dahinter liefen sowohl
26.2 als auch vom Nutzer als 1.8.9 bezeichnete Server mit ViaVersion. Der
Spielmodus und die Backendzuordnung jeder einzelnen Logmeldung sind unbekannt.
Die lokalen Bewegungsnachweise verwenden ausdrücklich Survival.

Nachtrag vom 24.09.: Die damaligen Fälle hielten die Blickrichtung fest und
deckten BadPacketsJ beim Drehen nicht ab. Der [neue Vorher-/Originalvergleich
mit Kamerabewegung und Blockpose](RIGHTCLICK-WORK.md) behandelt diese gesonderte
Abweichung; die bisherigen PASS-Ergebnisse werden dafür nicht umgedeutet.

## Nachgewiesene Abweichungen

| Verhalten | Originalgrenze | Korrektur |
|---|---|---|
| Leiterform | Ab 1.9: 3/16 statt 2/16 dick | Alle vier Ausrichtungen, gemeinsame Kollisions-/Auswahlform |
| Seerose | Ab 1.9: Box (1,0,1) bis (15,1.5,15), jeweils /16 | Kollision, Auswahl und Rechtsklick-Strahl verwenden dieselbe Form |
| Leiter hoch durch Sprungtaste | Ab 1.14: horizontale Kollision **oder** Sprungtaste | Einmaliger originaler Geschwindigkeitsimpuls nach der Landbewegung |
| Angriff/Armschwung | Ab 1.9: Angriff vor Swing | Native Entity-Klickreihenfolge plus sofortiger Hand-Paketpfad für den Swing |
| Benutzen/Abbau | Ab 1.19: gemeinsamer Vorhersagezähler | Einmal an der 1.19-Übersetzungsgrenze; Start/Ende zählen, Abbruch/Loslassen bleiben null |
| Komponenten-Schwert | Ab 1.21.5: `blocks_attacks` | Originalkomponente vor verlustbehafteter Übersetzung erhalten; echtes lokales Blocken und Verlangsamung |
| USE_ITEM beim Drehen | Winkel im Paket ab 1.21 | Blickrichtung am Klick erfassen und einmalig in die Zielübersetzung übernehmen |
| Abgelehnte Blockplatzierung | Ab 1.13 | USE_ON senden; bei FAIL keine Luftbenutzung, Nebenhand oder Swing |

Der Sequenzzähler gehört zur Verbindung und zur ursprünglichen Clientwelt.
Dimensionswechsel setzen ihn zurück, auch ohne zwischenzeitliche Interaktion;
ein Respawn in derselben Dimension erhält ihn. Via-Versionstransformationen
werden nicht erneut ausgeführt. Für moderne Schwerter ohne Blockkomponente
bleibt das normale Nichtblocken erhalten. Die bisherige Inhaltsrevision des
Ressourcenkatalogs darf hier nicht als Verhaltensversion verwendet werden.

Einzelne Angriffe bestanden auch vor der Korrektur: ViaRewind 4.2.0 puffert den
alten Swing bis zum Bewegungspaket und ordnet ihn damit bereits hinter dem
Angriff ein. Die **konkrete Reproduktion** ist dagegen achtmal ein Tick
Angriff+Sneak, jeweils gefolgt von neun Idle-Ticks, auf einen echten zweiten
Spieler. Der Server empfing `ATTACK → PLAYER_INPUT → ANIMATION`; alle acht
Angriffe lösten PacketOrderB `post-attack` aus. Nur die native Klickreihenfolge
zu ändern hätte die Ursache nicht beseitigt. Der vorhandene Hand-Paketpfad
überträgt jetzt auch normale Hauptarm-Swings sofort, ohne die alte Warteschlange.

## Quellen

`build/inspection/interaction/` enthält aus Original-JARs dekompilierte Klassen:
1.8.9 (Forge/MCP), 1.9/1.9.4 `amk` und `apm` (Leiter/Seerose), 1.13.2 `afa`
und 1.14 `aio` (LivingEntity), sowie benannte Originalklassen aus 26.2
(`LivingEntity`, `LadderBlock`, `LilyPadBlock`, `Item`, `MultiPlayerGameMode`,
`ClientPacketListener`). `source.json` verknüpft die älteren JARs mit Mojangs
Downloadhashes. Der Originalclient-Launcher verifiziert die 26.2-JAR gegen
deren Launcher-Metadaten und schreibt `native-source.json` in jeden Lauf.

Die untersuchte Paketübersetzung ist die tatsächlich gebündelte ViaBackwards
5.12.0. Grim ist der installierte Build `2.3.74-8eb5f28`, Quellcommit
`8eb5f2809591c891deb4958bb2927844871e0600`.

Auch die Server-Seite wurde am gepinnten ViaVersion-5.12.0-JAR geprüft:
`BlockItemPacketRewriter1_21_5.appendItemDataFixComponents` entfernt für fünf
Schwerttypen auf Backends bis 1.8 die Consumable-Komponente und setzt
`blocks_attacks` mit Blockverzögerung null. Die clientseitige Prüfung stützt
sich auf diese tatsächlich empfangene Komponente, nicht auf einen geratenen
Backendtyp. Dekompilat: `build/inspection/interaction/server-via/`.

## Reproduzieren

```powershell
py -3 tools/test-servers/interaction_probe.py --version 26.2 --label verify --player-target
py -3 tools/test-servers/interaction_probe.py --version 26.2 --client native --label original --player-target
py -3 tools/test-servers/interaction_probe.py --version 1.19.4 --label boundary
```

Der Test erzeugt eine einzelne isolierte Loopback-Instanz, sichert ihre Welt
vor dem Aufbau und erhält auch fehlgeschlagene Läufe. Vorhandene Clients werden
nicht beendet. Ein neuer Nicht-OP-Account startet Creative, wartet auf geladene
Chunks und wechselt für die ausgewiesenen Fälle in Survival. LabAC ON,
Clientprotokoll, Modus, Verbose und fehlende Bypassrechte werden vor und nach
jedem Fall geprüft. Der optionale zweite Spieler ist ein echter Originalclient.
Die Tastatur-/Mausbeobachter ersetzen weder Physik noch Netzwerkpfad.

Die Matrix umfasst Gehen/Ausrollen, Leiter vorwärts/Sneak/Loslassen/Springen,
Seerosen beim Gehen/Sprinten, Schild in Luft und auf Block, normales Schwert als
Kontrolle, Komponentenschwert, Angriff und Abbau. Tatsächliche Bewegung,
Leiterkontakt, Oberflächenhöhe, Itembenutzung, Predictions und empfangene
Interaktionspakete sind Voraussetzungen für PASS. Auch Reset-/Ausrüstungsphasen
werden ausgewertet; Loginprobleme werden gesondert ausgewiesen.

BadPacketsH ist im unveränderten Laborstandard experimentell deaktiviert.
Deshalb wird die **vollständige empfangene Sequenzfolge zusätzlich direkt
geprüft**. Keine BadPacketsH-Meldung allein wäre hier kein Nachweis. Das
gesicherte Nutzerlog enthält 120 echte Meldungen `expected=1, id=0`.

## Reproduktion und geprüfte Fälle

Die [maschinenlesbare Fallmatrix](INTERACTION-EVIDENCE.json) enthält
Rohdatei-Prüfsummen, erkannte Spielerbedingungen, Predictions, Offsets,
Benutzungszeiten und Fehler je Fall. [Originalquellen](INTERACTION-SOURCES.json)
sind mit Downloadhashes verknüpft. Laufordner liegen unter `run/test-servers/`.

| Client / tatsächlicher Server | Lauf | Ergebnis |
|---|---|---|
| ViaForge 26.2 / Paper 26.2, vorher | `interaction-26.2-before-1790183903751399400` | 10/18; 125 Simulation, 124 Phase, 69 NoSlow im Survival-Intervall; 31 Sequenzfehler |
| ViaForge 26.2 / Paper 26.2, vorher, echter Spieler | `interaction-26.2-attack-transition-before-1790188590335737800` | 12/19; darunter acht PacketOrderB bei Angriff+Sneak und 36 Sequenzfehler |
| ViaForge 26.2 / Paper 26.2, korrigiert, echter Spieler | `interaction-26.2-player-final-1790189213507500200` | **19/19 PASS**, einschließlich Reset-/Ausrüstungsphasen ohne Survival-Flags/Setbacks; 24 Sequenzpakete korrekt |
| Original 26.2 / Paper 26.2, echter Spieler | `interaction-26.2-player-original-final-1790189428604324300` | **19/19 PASS**, kein Survival- oder Login-Flag/Setback; 24 Sequenzpakete korrekt |
| ViaForge 1.21.5 / Paper 1.21.5 | `interaction-1.21.5-boundary-release-1790198629021514400` | **19/19 PASS**, auch Komponentenschwert und Angriff+Sneak; 24 Sequenzpakete korrekt |
| ViaForge 1.19.4 / Paper 1.19.4 | `interaction-1.19.4-boundary-release-1790199019533844200` | **17/17 PASS**, 22 Sequenzpakete korrekt; keine Survival-Flags/Setbacks einschließlich Reset-/Ausrüstungsphasen |
| ViaForge 1.9.4 / Paper 1.9.4 | `interaction-1.9.4-boundary-final-1790189726497443000` | **15/17, insgesamt FAIL**; Bewegung/Items sauber, beide Kuhangriffsfälle je acht Hitboxes-Flags |
| ViaForge 1.14 / Paper 1.14 | `interaction-1.14-boundary-release-1790198428558357800` | Unvollständig, 7/9 Teilfälle; anschließend serverseitiger Grim-ItemReset-Fehler und Disconnect |
| ViaForge 1.14.4 / Paper 1.14.4 | `interaction-1.14.4-boundary-release2-1790198882991677800` | Neun Bewegungsfälle bestanden, danach derselbe Grim-ItemReset-Fehler; insgesamt unvollständig/FAIL |

Die ausgewiesenen 26.2-/1.21.5-/1.19.4-ViaForge-Abschlussläufe haben jeweils zwei
**Login-Korrekturen** vor Survival; diese sind kein verschwiegener Bestandteil
des Bewegungs-PASS. Die 19 bewerteten Fälle enthalten 675 beziehungsweise 676
Grim-Predictions; maximaler Offset jeweils `6.914e-8`. Beim Original 26.2 sind es
676 Predictions mit maximal `6.828e-15`. Auf 1.19.4 sind es 574 Predictions
in 17 Fällen mit maximal `6.914e-8`; die beiden Komponenten-Schwertfälle gelten
erst ab 1.21.5. Das ergibt insgesamt **55 bestandene ViaForge-Fälle** und
**19 bestandene Original-26.2-Vergleichsfälle**. Zwischen- und Resetphasen werden daneben
im gesamten Survival-Intervall auf Flags/Setbacks geprüft.
Die 1.9.4-Laborerkennung nennt `1.9.3` für das gemeinsame Protokoll 110;
ausgewählt und tatsächlich gestartet war 1.9.4. Für die Hitboxes-Meldungen fehlt
ein Live-Vergleich mit dem Original 1.9.4. Sie werden nicht als behoben gezählt.

Frische lokale Regressionen: **134 JUnit**, **96 Python**, kompletter
Forge-Smoke über **49 Ressourcenprofile**. Der Bericht
`build/logs/interaction-client-smoke-release.txt` beginnt mit PASS. Er prüft
unter anderem vier Leiterausrichtungen, Seerosenkollision/Strahl/Auswahl und
Rückkehr zu 1.8, echte native Klickreihenfolge, direkten Haupt-/Nebenarmswing,
Item-Komponenten samt unveränderten Originalhashes sowie gemeinsame Sequenzen
für beide Hände, Platzieren, Abbaustart/-ende/-abbruch, Loslassen und Weltwechsel.
Das ist ein lokaler Regressionstest, keine Grim-Zertifizierung aller 49 Versionen.

## Grenzen

Der alte lokale Referenzserver ist Paper **1.8.8**, keine umbenannte 1.8.9.
Ein ursprünglicher Lauf scheitert auch mit dem Originalclient an
`ByteBuf.writeShortLE`; der separate Netty-4.1.68-Lauf bereits beim Start an
der Netzwerkinitialisierung. Netty 4.1.9 scheitert ebenfalls. Diese Versuche sind
kein Bewegungs-PASS. Deshalb ist Schwertblocken mit tatsächlich von der
26.2-Verbindung empfangener Originalkomponente auf modernem Backend bewiesen,
der Ende-zu-Ende-Vergleich gegen das alte Backend bleibt offen.
Weitere Laufzeitvarianten werden ausdrücklich mit Quellen/Hashes protokolliert.
Die normalen 49 Verwaltungsprofile und ihre Welten werden nicht verändert.

Für ältere serverseitige Schwertemulation über Food-/Consumable-Komponenten
vor 1.21.5 gibt es durch diese Korrektur noch keinen Nachweis. Allgemeine
Item-Mobilitätskomponenten, jedes beliebige Serverplugin und sämtliche
Kombinationen aller Ziel-/Backendversionen sind ebenfalls nicht abgedeckt.
26.3 wird nur im lokalen Ressourcen-/Pipelinesmoke geprüft; der installierte
Grim unterstützt dort keinen echten Bewegungsvergleich.

1.19 wird bereits beim Login von Grims Forge-Sperre (1.18.2–1.19.3) blockiert.
Der Lauf `interaction-1.19-boundary-release2-1790198824281095400` ist kein
Bewegungs-PASS. Die Sperre bleibt unverändert; 1.19.4 ist eine eigenständige
Vergleichsversion, kein Ersatznachweis für 1.19.

Auf den Paper-1.14-/1.14.4-Referenzen scheitert Grims `BukkitItemResetHandler` an
`NoSuchMethodException: EntityLiving.cU()`. Davor fehlte in zwei Leiter-Haltefällen
ausreichender Kontakt. Es gibt dafür keinen Gesamt-PASS und keine Umgehung des
Grim-Fehlers. Auf 1.14.4 bestanden alle neun bis zum Itemwechsel ausgeführten
Bewegungsfälle, einschließlich Leiter-Halten und Sprungklettern.

Konkrete Laufordner, Zwischenfehler und Abschlussnachweise stehen in
[INTERACTION-WORK.md](INTERACTION-WORK.md).
