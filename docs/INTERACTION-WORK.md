# Interaktionen und Kollisionsformen, 23.09.2026

Ausgangspunkt: sauberer Commit `ab744f6` (Sneak-/Sprintkorrektur). Nutzer
bestätigt Zielprotokoll 26.2 sowohl gegen tatsächliche 26.2-Server ohne
serverseitiges ViaVersion als auch gegen als 1.8.9 bezeichnete Server mit
ViaVersion. Spielmodus nicht angegeben. Lokale Reproduktionen in Survival.

Gesichert unter `build/backups/interaction-20260923-initial/`: aktuelle Release,
Quellen, Prüfsummen, Entwicklungs-JAR, ViaForge-Konfiguration und Nutzerlog.
Das Log enthält NoSlow ab 18:59:59 und BadPacketsH `expected=1, id=0` bis
19:12:01. Kein Schluss auf die konkrete Backendversion jeder einzelnen Meldung.

Prozessaufnahme: Webverwaltung läuft (PIDs 100652/100676), kein Java-Client
oder Spielserver. Normale Profile/Welten bleiben unangetastet; Testinstanzen
werden isoliert auf Loopback mit Sicherung vor dem Aufbau betrieben.

Quellbefunde, noch keine abschließenden Fixnachweise:

* Installierter Grim-Quellstand `8eb5f280...`: BadPacketsH prüft Sequenzen ab
  Client und Server 1.19; PacketOrderB erwartet ab Client 1.9 Angriff vor Swing.
* Aktueller Client enthält keine Sequenzverwaltung für moderne Benutzen- und
  Abbaupakete; alte Tastenklickfolge schwingt vor Angriff.
* Keine expliziten modernen Kollisionsanpassungen für Leiter/Seerose gefunden.
* ViaVersion fügt auf alten Servern modernen Schwertern Komponenten zum
  Blocken hinzu; die lokale SwordUse-Mixin unterdrückt natives Schwertbenutzen
  pauschal bei modernem Kampfverhalten. Erhaltene Originalkomponenten prüfen.

Nächste Schritte: reine Eingabe-/Beobachtungsinstrumentierung erweitern,
26.2-Vorherlauf mit Originalvergleich und eigenständige alte Serverinstanz.
Erst danach gezielte Produktionskorrekturen, Versionsgrenzen und Regressionen.

## Fortsetzung nach Unterbrechung

Die unveränderte Produktionsphysik mit neuer Eingabebeobachtung ist zusätzlich
als `ViaForge-initial-with-probe.jar` im oben genannten Backup gesichert.
Alle Ordner unten liegen unter `run/test-servers/`.

* `interaction-26.2-before-1790183903751399400`: 10/18 PASS, Leiter/Seerose
  erzeugen Phase/Simulation, Komponentenschwert NoSlow. Erste Angriffsausrichtung
  verfehlte die Kuh; dieser Abschnitt ist ausdrücklich FAIL, kein Angriffsnachweis.
* `interaction-26.2-original-before-1790184073837689900`: unvollständig;
  Beobachter suchte `clickCount` auf einer Unterklasse statt `KeyMapping`.
  Reiner Instrumentierungsfehler korrigiert; Originalphysik unverändert.
* `interaction-26.2-original-fixture2-1790184212961699400`: 16/18,
  Leiter und Komponentenschwert korrekt, Sequenzen korrekt. Seerosenabschnitte
  haben Timer/Simulation; kein vollständiger PASS. Neuer Aufbau wartet länger
  auf die anfängliche Chunkverarbeitung; Fehllauf bleibt erhalten.
* `interaction-26.2-first-fix-1790184615229861700`: 16/18, Leiter/Seerose
  und Angriff sauber, 33 Sequenzpakete korrekt. Komponentenschwert blieb inaktiv:
  fälschliche Abfrage `contentSince(770)` (Katalogrevision ist kein Verhalten).
  Durch explizite Regel COMPONENT_BLOCKING ab 1.21.5 ersetzt; Regression ergänzt.
* `interaction-26.2-via-1.8.8-first-1790184771628786700`: scheitert bereits
  Creative/Login mit TimerLimit/Disconnect, kein Survival-PASS.
* `interaction-26.2-via-1.8.8-original-1790184859754537600`: auch Original
  verliert Verbindung: serverseitiger `NoSuchMethodError ByteBuf.writeShortLE`.
  Kein Bewegungsnachweis. Original 1.8.8-Paper enthält Netty 4.0.23.

Produktionsänderungen: Leiter 3/16 ab 1.9, Seerosenkollision 14x1.5x14/16 ab 1.9,
Klettern per Sprung ab 1.14, Angriff vor Swing ab 1.9, gemeinsame Benutzen-/Abbau-
Sequenz ab 1.19, erhaltene Original-Blockkomponente für Schwerter ab 1.21.5.
Die gemeinsame Sequenz wird einmal an der 1.19-ViaBackwards-Grenze gesetzt.
Grim und Paketfilter wurden nicht abgeschwächt. BadPacketsH ist im unveränderten
Laborstandard experimentell deaktiviert: empfangene Sequenzen werden deshalb
zusätzlich direkt ausgewertet; Nutzerlog enthält 120 tatsächliche BadPacketsH.

Aktiver nächster Lauf beim Schreiben: `interaction-26.2-component-fixed-1790185053112071800`,
Konsole `build/logs/interaction-component-fixed-26.2.txt` (eigener Client/Server).
Letzter Build `build/logs/interaction-build2.txt` SUCCESSFUL; danach nur weitere
Test-/Smoke-Ergänzungen, noch keine Release-Freigabe. Originalvergleich und
Vorher-Angriffsfolge frisch wiederholen, dann ältere Versionsgrenzen, Regressionen,
voller Smoke und Release-Nachweis.

Die 1.8.8-Referenz ist ausdrücklich nicht als 1.8.9 ausgegeben. Originaldownloads
mit Hashes unter `build/inspection/interaction/platform/`; die normale 49er-Liste
bleibt unverändert. Optionaler **separater** Referenzlauf mit unverändertem
Netty 4.1.68 im Laufzeit-Classpath: `--backend 1.8.8 --netty-runtime`.
`netty-source.json`/jeweiliges `netty-runtime.json` dokumentieren die Bibliotheks-
anpassung; Server-JAR, Physik, Checks und Schwellen bleiben unverändert.

## Zweite Fortsetzung / belastbare Angriffskorrelation

* `interaction-26.2-component-fixed-1790185053112071800`: 18/18 Survival PASS,
  null Survival-Flags/Setbacks, 24 empfangene Sequenzpakete korrekt. Zwei
  Login-Korrekturen separat. Noch vor sofortigem Swing-Pfad/Weltwechselergänzung.
* `interaction-26.2-original-warm-1790185236990394800`: Original 18/18 Survival
  PASS; Creative/Login hat Timer und TimerLimit, getrennt ausgewiesen.
* `interaction-26.2-via-1.8.8-netty-original-1790185397781198900`: Netty 4.1.68
  scheitert bereits beim Start mit NetworkManager.channel null. Kein Clientlauf.
* `interaction-26.2-player-before-1790188261297900200`: unveränderte Produktion,
  echter zweiter Originalspieler; acht einzelne Angriffe sind sauber. Die übrigen
  Fehler erneut reproduziert (125 Simulation, 124 Phase, 70 NoSlow im Survival-
  Intervall). 36 Fehler in 38 Sequenzpaketen. Kein PacketOrderB-Nachweis allein.
* **`interaction-26.2-attack-transition-before-1790188590335737800`**: erstmals
  eindeutige PacketOrderB-Reproduktion am echten zweiten Spieler: achtmal
  `1:attack-sneak|9:idle` ergibt **acht `post-attack`-Flags**. Erstes Ereignis
  1790188769.629: ATTACK, 1790188769.631 Flag, dann PLAYER_INPUT mit Sneak=true,
  ANIMATION und Bewegung. Reine Einzelklicks desselben Laufs bleiben sauber.

Ursache präzisiert: ViaRewind 4.2.0 puffert alte SWING-Pakete bis zum nächsten
Bewegungspaket. Die native 1.8-Klickreihenfolge allein erklärt die gelegentlichen
Flags nicht und deren Umordnung allein reicht nicht. MixinHandNetwork schickt
jetzt sämtliche modernen Swings sofort über den vorhandenen Handpfad;
MixinAttackOrder stellt dafür die moderne Klickreihenfolge her. Die neue
Smoke-Prüfung ruft die echte Minecraft-Klickmethode ohne folgenden Bewegungstick
auf. Ein anfänglicher Smoke-Fehler war der noch aktive GUI-Klicksperrtimer im
Test; der Test speichert/löscht/restauriert ihn jetzt ausdrücklich.

Weitere Regressionen: Weltwechsel ohne dazwischenliegende Interaktion setzen
den Sequenzzähler zurück; gleiche Dimension bleibt erhalten (Original-26.2-
ClientPacketListener geprüft). Seerosenbox nach Rückkehr von modern zu 1.8
unabhängig von gemeinsam gespeicherten Auswahlmaßen. Aktueller vollständiger
Smoke: `build/logs/interaction-client-smoke-final2.txt` mit Konsole
`interaction-smoke-final2-console.txt`; nach PASS wartet die serielle Matrix
`interaction-final-matrix3.txt` auf ViaForge/Original26.2 mit echtem Spieler,
separate alte Serverreferenz und gezielte 1.9.4/1.14/1.19/1.21.5-Läufe.
Jeder Harnessstart prüft vorhandene Clients. Kein Nutzerclient wurde beendet.

Der zweite Originalspieler wird für die Abschlussläufe bereits in der Creative-
Ladephase gestartet. Im Vorherlauf fiel sein Ressourcenladen noch in Survival;
etwaige Reset-/Timerereignisse bleiben im Vorherbericht erhalten. Die Tastensequenz
selbst bleibt unverändert. BadPacketsH weiterhin unveränderte Laborkonfiguration,
plus unabhängige Paketfolgenauswertung. Python bisher frisch 95/95 PASS.

## Stand 23:25 Uhr

Nach weiterer Unterbrechung waren keine Java-/Testprozesse mehr aktiv. Die
angefangene 1.19-Instanz `interaction-1.19-boundary-final-1790189878232960900`
hat keinen Abschlussbericht und wird nicht gewertet. Keine fremden Prozesse
wurden beendet, keine ursprünglichen Profile/Welten geändert.

Abschlussfähige neue Befunde:

* `interaction-26.2-player-final-1790189213507500200`: **19/19 PASS**, 24
  Sequenzpakete korrekt, acht Angriff+Sneak-Paare ohne dazwischenliegendes
  Zustandspaket, null Flags/Setbacks im gesamten Survival-Intervall. Zwei
  Login-Korrekturen separat; keine Login-Flags.
* `interaction-26.2-player-original-final-1790189428604324300`: Original
  **19/19 PASS**, dieselben 24 Sequenzpakete korrekt, null Survival-/Login-
  Flags oder Setbacks. Angriffe in beiden Läufen gegen echten Originalspieler.
* `interaction-1.9.4-boundary-final-1790189726497443000`: **15/17**, Bewegung
  und Items sauber, beide Kuhangriffsfälle je acht Hitboxes-Flags. Paketfolge
  korrekt. Noch kein Originalclientvergleich dieses separaten Hitboxbefunds;
  nicht stillschweigend als vollständig kompatibel ausgeben. Grim nennt
  Protokoll 110 `1.9.3`, Server/ausgewählte Ressourcen sind tatsächlich 1.9.4.
* `interaction-1.14-boundary-final-1790189875270085400`: Start scheiterte am
  Java-Guard. Teststarter berücksichtigt jetzt auch Paperclips `patch.json`
  und verwendet unveränderte hashgeprüfte Ausgabe mit vorhandenem Bootadapter.
* `interaction-1.14-boundary-release-1790198428558357800`: 7/9 Teilfälle,
  zwei Leiter-Haltefälle ohne ausreichenden Kontakt. Danach Grim-Fehler
  `BukkitItemResetHandler → NoSuchMethodException EntityLiving.cU()` und
  Disconnect `Invalid packet`; **Gesamtlauf FAIL**. Zusätzlich alte Commodore-
  ASM-Warnungen über Java-17-Klassen. Keine Check-/Serverpatches zur Umgehung.
* `interaction-1.19-boundary-release-1790198572961599100`: Harness erwartete
  `[Server]` statt tatsächlichem `<Server>` für den Konsolenmarker, kein
  Clientlauf. Beide unverfälschten Logformate werden jetzt erkannt.
* Netty-4.1.9-Referenz ebenfalls Start-Timeout. Rein beobachtender
  `LegacyNettyTrace` ändert nur die Ausgabe bereits auftretender Exceptions;
  gesicherte Welt, Agentquelle und `netty-diagnostics.json` liegen beim Lauf.
  Kein Client-/Bewegungsnachweis; die Serververbindung bleibt blockiert.

**Frischer voller Smoke PASS**: `build/logs/interaction-client-smoke-release.txt`
(49 Ressourcenprofile, einschließlich normaler 1.8-Fallbackkontrolle).
134 JUnit-Tests, 96 Python-Tests PASS. Neuere Smoke-Prüfung deckt auch originalen
Abbau-vor-Swing ab (ab 1.9; Originalklasse `bcf` geprüft), sofortiges Hauptarm-
Paket, Nebenhand, gemeinsame Abbau-/Benutzensequenz und Dimensionsreset ab.

Aktiv: serielle Matrizen `build/logs/interaction-boundaries-release.txt` und
`interaction-boundaries-release2.txt`; aktuelle 1.21.5-Instanz
`interaction-1.21.5-boundary-release-1790198629021514400`, danach 1.19 und 1.14.4.
`interaction_evidence.py` wertet erhaltene Rohberichte neu aus, ohne sie zu
überschreiben; `docs/INTERACTION-EVIDENCE.json` enthält bereits die fünf
wichtigsten Vorher-/Nachherläufe, wird nach der Matrix ergänzt.
Release noch nicht abschließend ausgeliefert: Quellenarchiv/Manifest und
Abschlussmatrix müssen nach den letzten Läufen frisch gebaut werden.

## Abschlussstand 23.09.2026, 23:45 Uhr

Die oben genannten Matrizen sind beendet. Bei der erneuten Prozessprüfung
liefen keine Java-/Python-Prozesse und kein Minecraft-Client. HEAD bleibt
`ab744f6be5521b033f6431a9b0daad8fb81709aa`; die Interaktionskorrekturen liegen
uncommittet im Arbeitsbaum. Vorherige Release und Quellen bleiben unter
`build/backups/interaction-20260923-initial/` erhalten.

Weitere abgeschlossene Läufe:

* `interaction-1.21.5-boundary-release-1790198629021514400`: **19/19 PASS**,
  24 Sequenzpakete korrekt, keine Survival-Flags/Setbacks; zwei Login-Korrekturen.
* `interaction-1.19.4-boundary-release-1790199019533844200`: **17/17 PASS**,
  22 Sequenzpakete korrekt, 574 Predictions, maximaler Offset `6.914e-8`,
  keine Survival-Flags/Setbacks; zwei Login-Korrekturen. Komponentenschwerter
  gelten erst ab 1.21.5 und sind hier keine vermeintlich bestandenen Fälle.
* `interaction-1.19-boundary-release2-1790198824281095400`: unveränderte
  Forge-Sperre beim Login; kein Bewegungsnachweis.
* `interaction-1.14.4-boundary-release2-1790198882991677800`: alle neun
  ausgeführten Bewegungsfälle bestanden. Beim Itemwechsel scheitert Grims
  ItemReset-Handler wie auf 1.14; unvollständiger Gesamtlauf, kein Gesamt-PASS.

Die elf ausgewerteten Rohberichte sind mit Fallbedingungen und Prüfsummen in
`docs/INTERACTION-EVIDENCE.json` verknüpft. Insgesamt bestehen 55 ViaForge-Fälle
auf 26.2, 1.21.5 und 1.19.4 sowie 19 Original-26.2-Vergleichsfälle. Auch die
Zwischenphasen gehören zur Survival-Auswertung. PacketOrderB ist durch den
Vorher-/Nachhervergleich mit echten Spielern belegt; BadPacketsH wird wegen
der unveränderten experimentellen Laboreinstellung anhand der tatsächlichen
Sequenzpakete bewertet, nicht aus fehlenden Flags abgeleitet.

Die 452 Produktions-/Entwicklungs-/Testdateien stimmen weiterhin exakt mit
`build/libs/interaction-review-inputs.json` vom vollständigen 49-Profil-Smoke
überein. Dessen Bericht `build/logs/interaction-client-smoke-release.txt`
beginnt mit PASS; JUnit meldet 134 Tests ohne Fehler/Auslassungen. Erneuter
Python-Gesamtlauf: 96/96 PASS in
`build/logs/interaction-python-release-final.txt`; `git diff --check` sauber.

Auslieferungsdateien: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar` und
das gleichnamige `-sources.zip`. Das nach dem Abschlussbuild erzeugte
`build/libs/interaction-release.json` verbindet Größe/Prüfsummen, Quellstand,
Testberichte und Grenzen. Buildprotokoll:
`build/logs/interaction-release-build.txt`. Es wird kein Nutzerclient beendet
und kein Commit oder Austausch einer externen Installation vorgenommen.

Offen bleiben der Ende-zu-Ende-Nachweis auf dem alten ViaVersion-Backend
(lokale 1.8.8-Referenz scheitert auch im Originalvergleich an ihrer
Netzwerklaufzeit), die separaten 1.9.4-Hitboxes-Flags und die genannten
Laborblockaden. Nächste gezielte Arbeiten wären ein funktionierendes,
unverfälschtes altes Referenzbackend und der Original-1.9.4-Angriffsvergleich.
Diese Grenzen werden nicht durch zusätzliche Check-Ausnahmen verdeckt.
