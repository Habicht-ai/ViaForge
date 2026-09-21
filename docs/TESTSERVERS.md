# Lokales ViaForge-Testlabor

Die Verwaltung zeigt einen Server je registriertem Ressourcen-Release von 1.9 bis 26.3.
`tools/test-servers/profiles.json` ordnet jeder Version aus `versions.json` genau eine vorhandene
Welt zu: Paper mit schaltbarem Grim, soweit startgeprüft, sonst Vanilla mit erklärter Grim-Grenze.
Derzeit sind es 49 Einträge, davon 41 mit Grim. Die ursprünglichen Vanilla-Welten bleiben archiviert.
Details zur Umstellung und den frischen Prüfungen: [LAB-PROFILES.md](LAB-PROFILES.md). Protokollgleiche Patches
teilen ein Profil; beispielsweise ist 1.16.5 der Testserver für Protokoll 754.
Es handelt sich nicht um einen einzelnen Server mit vorgeschalteter Via-Übersetzung.

Historischer Stand vor dieser Erweiterung: Auf diesem PC wurden am 16.09.2026 alle 48 Testwelten aufgebaut und nach einem
echten Serverneustart erfolgreich geprüft: **7.020 Weltprüfungen**, zusätzlich
13 Werkzeugtests und echte Offline-Logins auf 1.12.2 und 26.2. Der genaue
Abschlussbericht liegt unter `run/test-servers/FINAL-REPORT.md`. Das bestätigt
die Testwelten, nicht die fehlerfreie Darstellung im ViaForge-Client.

Die Erweiterung vom 19.09.2026 und ihre Prüfgrenzen stehen in [UPGRADE-26.3.md](UPGRADE-26.3.md).

## Benutzen

Im Repository **`Testserver.vbs` doppelklicken**. Die lokale Weboberfläche öffnet
sich unter **http://127.0.0.1:8765**, ohne ein Konsolenfenster. Ein Doppelklick auf
`Testserver.bat` öffnet ebenfalls die Webseite. Weitere Doppelklicks verwenden
die bereits laufende Webverwaltung.

Die Webseite zeigt alle Versionen aus dem Manifest mit aktuellem Status, Spielerzahl, Adresse
und letztem Weltprüfergebnis. Suche, Statusfilter, Favoriten und die Gruppen
Regression/Legacy/Modern helfen bei der Auswahl. Einzelne oder ausgewählte
Server lassen sich direkt starten und mit Speichern stoppen. Die Speicheranzeige
zeigt den freien Arbeitsspeicher; der Start behält die vorhandene Speicherprüfung.
Aufträge laufen nacheinander im Hintergrund und erscheinen unter „Letzte Aktionen“
mit Ergebnis und bei Bedarf den genauen Fehlermeldungen.

Ein Klick auf eine Version öffnet ihre Details: Verbindung, Teststationen,
Testpaket/Operator-Rechte für deinen Spielernamen, Weltprüfung, durchsuchbarer
Inhaltskatalog mit Teleportbefehlen und das aktuelle Serverprotokoll. Favoriten
werden lokal im Browser gespeichert. Schließen des Browserfensters beendet
weder Server noch Webverwaltung. Bei einem einzelnen Server speichert „Stoppen“
die Welt und beendet diesen Server.

**Die gesamte Anwendung beenden:** oben rechts **„Anwendung beenden“** anklicken.
„Alles speichern & beenden“ wartet zunächst auf vorherige Webaufträge, speichert
und stoppt alle vom Testlabor verwalteten Minecraft-Server und beendet anschließend
auch den lokalen Webdienst. „Nur Webverwaltung beenden“ lässt Minecraft-Server
weiterlaufen. Bei einem Speicher-/Stoppfehler bleibt die Webseite zur Diagnose
erreichbar. Fremde Java-Prozesse und Minecraft-Clients werden nicht beendet.
Nach erfolgreichem Beenden zeigt die noch geöffnete Seite eine Abschlussmeldung;
zum Neustart wieder `Testserver.vbs` öffnen.

Die Webverwaltung bindet ausschließlich an `127.0.0.1` und benötigt kein Konto.

Historische Browserprüfung vor der Profilauswahl: alle damals 48 Einträge,
Gruppen/Favoriten, Suche, Katalogfilter, Kopieren von Adressen/Teleportbefehlen,
Serverprotokoll und gemeinsames Starten/Stoppen von 1.12.2 und 26.2. Auch der
Start über `Testserver.vbs` und die Darstellung bei 390 Pixel Fensterbreite
wurden geprüft. Die Werkzeugtests umfassen nun 27 erfolgreiche Tests. Das
vollständige Speichern/Beenden einschließlich des Webprozesses wurde ebenfalls
mit echten 1.12.2- und 26.2-Servern geprüft.

Die bisherige Kommandozeile bleibt für Skripte nutzbar, zum Beispiel in PowerShell:

```powershell
./Testserver.bat start 26.2
./Testserver.bat start regression
./Testserver.bat status all
./Testserver.bat op 26.2 DeinSpielername
./Testserver.bat kit 26.2 DeinSpielername
./Testserver.bat find 26.2 shulker
./Testserver.bat stop all
```

In Minecraft unter Multiplayer die lokale Adresse hinzufügen und die passende
ViaForge-Zielversion wählen. Diese häufig gebrauchten Adressen sind fest:

| Version | Adresse |
| --- | --- |
| 1.9 | `127.0.0.1:25590` |
| 1.12.2 | `127.0.0.1:27010` |
| 1.13.2 | `127.0.0.1:27016` |
| 1.21.11 | `127.0.0.1:27080` |
| 26.1.2 | `127.0.0.1:27082` |
| 26.2 | `127.0.0.1:27084` |
| 26.3 | `127.0.0.1:25638` |

`./Testserver.bat import-list all` ergänzt die Einträge aus dem Manifest in **`run/servers.dat`**
der Entwicklungsinstanz, einschließlich ViaForge-Versionseinstellung. Vorhandene
eigene Einträge und unbekannte NBT-Tags bleiben erhalten, eine Sicherung wird angelegt.
Automatisch erzeugte Laboreinträge mit passendem Namen und bekannter Laboradresse
werden auf das aktive Profil aktualisiert; bereits eingetragene Adressen werden nicht dupliziert. Der Import verweigert
Änderungen bei laufendem Minecraft-Client. Andere Launcherprofile werden nicht
automatisch verändert. `export-list all` erzeugt zusätzlich eine separate
`run/test-servers/servers-labor.dat` für eine eigene, leere Testinstanz.

Alle Server laufen im Offline-Modus und binden Spielport **und RCON an 127.0.0.1**.
Eine Microsoft-Anmeldung ist für diesen lokalen Zugang nicht nötig. Keine
Portfreigabe und keine Firewall-Ausnahme erforderlich. Der Starter verweigert
eine veränderte Bind-Adresse. Ein zufälliges RCON-Passwort liegt ausschließlich
im ignorierten Laufzeitordner der jeweiligen Version.

## Testwelt

Der Startpunkt ist die obere Ankunftsplattform bei **0, 177, 0**. Ihr Knopf führt
zu den Teststationen und zur Navigation. Die Ausstellung ist in Etagen unterteilt:

| Bereich | Position / Höhe | Inhalt |
| --- | --- | --- |
| Navigation und Regressionen | `0 65 -100` | Teleportknöpfe, Creative/Survival, Schild und Boot geben |
| Blöcke A | `-78 65 -78` | Beschriftete Blocktypen, bei Legacy auch Metadatenzustände |
| Itemkatalog | `-78 89 -78` | Kisten mit gültigen Inventar-Items; ohne technische Operator-Werkzeuge |
| Mobs | `-78 113 -78` | Benannte, persistente, unbewegliche Lebewesen |
| Blöcke B | `-78 145 -78` | Fortsetzung größerer Kataloge |
| Boss-Arena | `0 209 80` | Bosse auf Knopfdruck, eigener Knopf zum Entfernen |
| Wasser/Boote | `20 65 -95` | Wasserfarbe, Transparenz, Einsteigen, Steuern, zweiter Passagier |
| Höhenproben ab 1.18 | `50 -59 0`, `50 301 0` | Tatsächliche moderne Serverhöhe; aktuelle Clientgrenzen sichtbar machen |

`arena-index.json` im Versionsordner enthält Namen und genaue Positionen aller
Exponate sowie Itemkiste und Slot. Neue Spieler starten in Creative; der gewählte
Spielmodus bleibt auch in der Ausstellung erhalten. Creative, Survival und
Adventure sind über die Knöpfe an den Teststationen frei auswählbar. Bestehende
Spieler behalten ihren gespeicherten Modus und können einmal den Creative-Knopf
benutzen, falls sie noch in Adventure sind. Der Survival-Knopf ermöglicht
beispielsweise den Schildtest mit Q und Strg+Q.
Die Server-Sichtweite beträgt **16 Chunks** (`view-distance=16`). Im Client muss
die Sichtweite ebenfalls auf mindestens 16 stehen, um diesen Radius darzustellen.
Die separate Simulationsdistanz bleibt bei 3; sie begrenzt nicht die Sichtweite.
`kit` gibt einem verbundenen Spieler ein passendes Paket für die bisherigen
Regressionen, unter anderem Schild, Purpur, Elytren, Betten, Shulkerkiste und
Boot, soweit in seiner Serverversion vorhanden. `find` sucht im Katalog nach
englischen Registry-Namen und zeigt die Position bzw. Kiste an.
Zeit, Wetter, zufälliges Blockwachstum und natürliches Mob-Spawning werden für
vergleichbare Bedingungen angehalten. Die Welt wird beim Stoppen gespeichert.

**Abdeckung:** Der Blockkatalog stammt aus den tatsächlichen Serverregistries.
Bei 1.9–1.12.2 wird die Metadatenkodierung des Originalservers benutzt; ab 1.13
werden die Original-Datengeneratorberichte verwendet. Moderne Blocktypen werden
mit einem Standardzustand ausgestellt, nicht mit sämtlichen möglichen
Kombinationen von Ausrichtung, Wachstum und Wasserzustand. Technische bzw.
unsichtbare Blöcke und kurzlebige Zustände sind im Index enthalten, aber nicht
alle können als dauerhaft sichtbares, eigenständiges Exponat existieren.
Ab 1.19.3 werden experimentelle Inhalte anhand der **echten `DEFAULT_FLAGS`
des Originalservers** herausgefiltert. Beispielsweise gehören die in 1.20.4
bereits enthaltenen experimentellen 1.21-Blöcke nicht in dessen normale
Testwelt. `experimental-features.tsv` dokumentiert diese Ausschlüsse.

Die Mob-Auswahl enthält die registrierten Lebewesen einschließlich Golems,
Riesen, Illusionern und Rüstungsständern. Boss-Exponate sind bewusst abrufbar:
sie werden auf ihren beschrifteten Knöpfen in der getrennten Arena gespawnt.
Bei 1.9–1.10 werden Mob-Arten, die noch dieselbe Registry-ID nutzten, gesondert
ergänzt: Wither-Skelett, Elder Guardian, Zombie-Dorfbewohner, Esel, Maultier,
Zombie-/Skelettpferd und ab 1.10 Stray/Husk. Fellfarben, Berufe und andere
NBT-Varianten bilden keinen vollständigen Variantenkatalog. Die Katalogkisten erlauben echte Inventar-Roundtrips über den
Server. Eine erfolgreiche Weltprüfung bestätigt keine fehlerfreie Darstellung
im ViaForge-Client; genau diese soll hier getestet werden.

## Praktischer Regressionstest

1. `start regression`, anschließend auf 1.12.2 verbinden und `kit` für deinen
   Spielernamen ausführen. Purpur und Shulkerkisten aus Inventar/Kiste platzieren,
   abbauen und in F5 betrachten. Die ursprüngliche Server-ID muss erhalten bleiben.
2. Auf 1.13.2 dieselben Schritte wiederholen. Am Wasserbecken Wasserfarbe,
   Transparenz und animierte Textur prüfen.
3. Auf 26.2 Bett- und Shulker-Reihe, Schild, Spawn-Eier und Glas/Gras vergleichen.
   Ein Item aus einer Kiste nehmen, zwischen Slots verschieben und wieder ablegen.
4. Am Survival-Knopf umschalten, Schild mit Q abwerfen, aufheben und mit Strg+Q
   erneut abwerfen. Es darf kein Ghostitem zurückbleiben.
5. Boot aus dem Testpaket platzieren, einsteigen, steuern und aussteigen. Für den
   zweiten Passagier kann eine zweite lokale Clientinstanz oder ein Mob verwendet werden.
6. Zwischen alten und neuen Servern wechseln und erneut verbinden. Auf verspätete
   Texturwechsel, falsche Blockzustände und veränderte Itemmodelle achten.

## Parallelbetrieb

Versionen lassen sich mit Komma auswählen, etwa `start 1.12.2,26.2`. Gruppen:

| Gruppe | Versionen |
| --- | --- |
| `regression` | 1.12.2, 1.13.2, 1.21.11, 26.3 |
| `legacy` | 1.9, 1.10.2, 1.11.2, 1.12.2 |
| `modern` | 1.16.5, 1.18.2, 1.20.6, 26.3 |
| `all` | alle Profile aus dem Manifest |

Alle Profile können als Auswahl angesprochen werden. **Auf diesem 32-GB-PC ist ihr
gleichzeitiger Betrieb mit dieser Konfiguration nicht vorgesehen:** die
Speicherprüfung kalkuliert ungefähr 81 GB inklusive Reserve. Der Starter
verweigert zu große Gruppen, bevor er weitere Prozesse startet. Alte Server
erhalten maximal 768 MiB Java-Heap, mittlere 1 GiB und neuere 1,5 GiB;
zusätzlich werden native Speicheranteile und 3 GiB Systemreserve berücksichtigt.
Die Prüfung ist eine Planungshilfe und keine Garantie für jede Spielsituation.

Die Prozesse laufen ohne zusätzliche Konsolenfenster. `stop` sendet den normalen
Minecraft-Speicher-/Stopbefehl ausschließlich an die vom Labor verwalteten
Server. Eine laufende Minecraft-Clientinstanz wird nicht beendet.

## Dateien und Wiederherstellung

- `tools/test-servers/versions.json`: Versionen, Protokolle, feste Ports, Java-Anforderungen,
  offizielle Downloadadressen und SHA-1-Prüfsummen.
- `run/test-servers/<Version>/`: Originalserver, Welt, Konfiguration und Testberichte.
- `run/test-servers/index.html`: lokale Adressübersicht; kein Webdienst.
- `tools/test-servers/webapp.py`, `web/`: lokale Webverwaltung und Oberfläche.
- `Testserver.vbs`: öffnet die Webverwaltung ohne Konsolenfenster.
- `run/test-servers/web-jobs/`: Protokolle der über die Webseite ausgelösten Aktionen.
- `run/test-servers/<Version>/catalog.html`: durchsuchbarer Inhaltskatalog mit
  Exponat-/Kistenkoordinaten und kopierbaren Teleportbefehlen, auch ohne laufenden Server.
- `run/test-servers/java.json`: erkannte lokale Java-Pfade.
- `run/test-servers/runtime/`: bei Bedarf getrennt installierte Java-Laufzeiten.
- `console.log` und `worker.log`: Serverausgaben bzw. Verwaltungsfehler.
- `arena-report.json`, `verification.json`: Aufbau- und Laufzeitprüfung je Version.
- `persistence-check.json`: Prüfung der gespeicherten Welt nach einem echten Serverneustart.
- `world-audit.json`: zusätzlicher Abgleich der gespeicherten Blockausstellung mit dem Katalog.
- `refinement.json`: Korrekturen, Wegweiserprüfung und verbleibende Exponatprobleme.
- `exhibition-maintenance.json`, `sign-manifest.json`: Kisten-/Schildkorrektur und Wiederherstellung.
- `server-settings-check.json`: geprüfte Entfernung der früheren Modusumschaltung und Sichtweite 16.
- `inventory-catalog.json`: originale Legacy-Creative-Stacks einschließlich NBT und Quellprüfsummen.
- `exhibition-probe.json`: tatsächlicher Login-, Schutz- und Reparaturtest auf 1.12.2 und 26.2.
- `backups/before-refinement-1/`: Sicherung der gesamten Welt vor den Ausstellungskorrekturen.
- `FINAL-REPORT.md`, `final-report.json`: zusammengefasste Prüfung der Profile aus dem Manifest, mit Datum und Grenzen.
- `provision-report.json`: Ergebnisse der Aufbauversuche; ältere Fehler bleiben zur Diagnose erhalten.

Alle Laufzeitdaten sind durch den vorhandenen `run/`-Eintrag vom Git ausgeschlossen.
Zum Sichern zuerst die betreffende Version stoppen und ihren gesamten
Versionsordner kopieren. `setup` ersetzt weder vorhandene Einstellungen noch
Welten; `build` überschreibt keine bereits als aufgebaut markierte Testwelt.
Unvollständige Erstaufbauten können mit `build` fortgesetzt werden. Dabei wird
der reservierte Ausstellungsbereich erneut aufgebaut.
Nach einem Prozessabbruch prüft der Starter beide gespeicherten Prozess-IDs.
Nur wenn Worker und Server beendet sind, wird die eigene verwaiste Sperre
freigegeben. Noch nicht bestätigte Befehle werden als `.interrupted` archiviert
und nicht beim nächsten Start blind erneut ausgeführt.

Neu aufsetzen bzw. auf einem anderen PC vorbereiten (Python 3.11+ und Java 8 JDK
für den Legacy-Registryexport erforderlich):

```powershell
./Testserver.bat setup all
./Testserver.bat catalog all
# Nur nach eigenem Lesen und Akzeptieren der Minecraft-EULA:
./Testserver.bat accept-eula all
./Testserver.bat provision all
```

`provision` startet jeweils höchstens zwei Server, baut die Welten, prüft sie und
beendet die dafür gestarteten Server wieder mit Speichern. Bereits laufende
Laborsitzungen werden am Ende nicht automatisch beendet. Der erstmalige Aufbau
aller Versionen dauert insbesondere mit den alten Beleuchtungsberechnungen.
Für einen einzelnen Test: `start 26.2`, `build 26.2`, `verify 26.2`.
`audit 26.2` startet eine zuvor beendete Testwelt frisch, prüft die gespeicherten
Exponate, Mobs und vollständigen Itemkisten und beendet den Server wieder.
Alle umfangreichen Weltänderungen und Speichervorgänge laufen über die
Serverkonsole; insbesondere alte RCON-Versionen führen solche Änderungen
sonst teilweise auf dem falschen Thread aus.

Die eigentlichen Server-JARs umfassen zusammen etwa 1,74 GiB; Welten,
Bibliotheken, Registryberichte und Java-Laufzeiten brauchen zusätzlichen Platz.
Java 8 wird für alte Server, Java 21 für die mittleren Generationen und Java 25
für 26.x verwendet. Vorhandene passende Laufzeiten werden bevorzugt.
Fehlende Laufzeiten werden als ZIP von Azul bezogen, mit SHA-256 überprüft und
lokal entpackt; die globale Java-Installation und PATH bleiben unverändert.

Werkzeugtests:

```powershell
py -3 -m unittest discover -s tools/test-servers -v
```

Mit laufendem 1.12.2- und/oder 26.2-Server prüft
`py -3 tools/test-servers/login_probe.py 1.12.2,26.2` einen tatsächlichen
Offline-Login einschließlich des ersten Ankunftspunkts. Der 26.2-Probe durchläuft
auch die Konfigurationsphase und verwendet die Paket-IDs aus dem Originalserverbericht.
Er ersetzt keine visuelle Prüfung im Forge-Client.

## Ausstellung prüfen und verbessern

Der [Prüfbericht vom 18.09.2026](TESTWORLD-REVIEW.md) beschreibt die gefundenen
Fehler, die Korrekturen und die Grenzen der Prüfung über alle 48 Welten.

`py -3 tools/test-servers/world_audit.py all` liest die gespeicherten Chunkdaten
der beendeten Server ohne Weltänderung. Es prüft, ob Exponate vorhanden sind und
bei modernen Versionen den richtigen Blocktyp haben. Es prüft nicht sämtliche
Blockzustände, NBT-Daten oder die Clientdarstellung. Fehlende bzw. veränderte
Exponate erscheinen zusätzlich in der Webübersicht.

`py -3 tools/test-servers/world_refine.py all` sichert jede Welt und korrigiert
bekannte Ausstellungsfehler im reservierten Testbereich: geschlossene Behälter
für wassergefüllte Exponate, Stützen für Wandblöcke und Schrägschienen, tragfähige
Sandunterlagen, Pflanzuntergründe sowie vollständige mehrteilige Pflanzen.
Ausgelaufenes Wasser auf den beiden Blocketagen wird entfernt und verdrängte
Exponate werden wiederhergestellt. Itemkisten und Mob-Ausstellungen werden dabei
nicht neu aufgebaut. Hinzu kommen farbige Etagenzugänge, beleuchtete Wegweiser,
zusätzliche Rückwegknöpfe und zur Ankunftsseite ausgerichtete moderne Schilder.
Die vorherige Welt bleibt unter `backups/before-refinement-1/world/` erhalten.
Für einen gezielten zweiten Durchlauf: `--remaining-only` ergänzen.

Fließende Legacy-Flüssigkeitszustände ohne Quelle und auftauendes Frost-Eis sind
kurzlebige Zustände. Sie bleiben im Katalog dokumentiert und zählen nicht als
dauerhaft fehlendes Exponat. Die sonstigen Einträge werden getrennt ausgewiesen.

Weitere gestalterische Ausbaustufen wären thematische Inseln (Bauen, Redstone,
Pflanzen), ein kleiner natürlicher Biom-/Tageslichttest neben der Ausstellung
und Vergleichsräume mit identischer Szene für verschiedene Versionen. Die
vorhandenen Testkoordinaten und Spielerbauten bleiben bei dieser Überarbeitung
erhalten; eine vollständige visuelle Ingame-Abnahme steht weiterhin aus.

## Kisten, Beschriftungen und versehentlich abgebaute Exponate

Die Katalogkisten enthalten Inventar-Items der jeweiligen Version. Bei 1.9–1.12.2
werden die Creative-Itemstacks direkt aus dem jeweiligen originalen, per SHA-1
geprüften Client exportiert, einschließlich Farben, zulässiger Metadaten und NBT
für Spawn-Eier und Tränke. Platzierungszustände (z. B. Treppenrichtungen oder
Stammachsen) sind keine Itemvarianten. Ab 1.13 stammt die Auswahl aus der echten,
bereits um experimentelle Inhalte bereinigten Itemregistry. Luft und technische
Operator-Werkzeuge sind aus den Kisten ausgeschlossen. Das sind Testinventare,
keine nachgebauten Beutetabellen natürlich generierter Truhen.

Der Spielmodus ist überall frei wählbar. Die frühere automatische Umschaltung
auf Adventure wurde auf Wunsch entfernt, einschließlich ihrer vier permanenten
Befehlsblöcke. Creative bleibt auch in der Ausstellung und an der Ankunft aktiv.
Du kannst dadurch dort bauen und abbauen; bei versehentlichen Änderungen steht
die gesicherte Wiederherstellung zur Verfügung. Auch sie aktiviert keine
automatische Modusumschaltung mehr.

Falls etwas fehlt: Auf der Webseite den betreffenden Server **Speichern & stoppen**,
dann in seinen Details **Ausstellung wiederherstellen** anklicken. Die Aktion
sichert zuerst die vollständige Welt unter `backups/exhibition-<Zeit>-<Kennung>/`,
repariert fehlende bzw. durch andere Blocktypen ersetzte Exponate, ergänzt Schilder
samt Unterlagen, setzt Navigationsknöpfe und die Betten-/Shulker-Modellreihe zurück
und füllt sämtliche Katalogkisten mit der vorgesehenen Auswahl neu.
Spielerinventare und Bauten außerhalb der reservierten Exponatplätze bleiben
erhalten. Entnommene Kataloggegenstände werden dabei bewusst nachgefüllt.
Der Server wird zur Reparatur kurz gestartet, geprüft, gespeichert und wieder
beendet; anschließend kannst du ihn normal starten. Laufende Server werden von
der Wiederherstellungsaktion nicht gestoppt oder verändert.

Die gespeicherte Weltprüfung erkennt nun auch falsche Legacy-Block-IDs sowie
fehlende Schilder, falsche Schildtexte und fehlende Unterlagen. Normale Wechsel
zwischen gespeisten und ungespeisten Legacy-Redstone-IDs gelten als gleichwertig.
Sie prüft weiterhin nicht alle Blockeigenschaften oder die ViaForge-Darstellung.
Für Entwickler: `py -3 tools/test-servers/exhibition.py all` aktualisiert ältere
Welten einmalig, `--restore` führt eine erneute Wiederherstellung aus.
`exhibition_probe.py 1.12.2,26.2` prüft mit echten Offline-Logins die frei gewählten Spielmodi
und absichtlich beschädigte Exponate; nur auf zuvor gestoppten Testservern benutzen.
Mit `--modes-only` werden ausschließlich die Spielmodi geprüft. Auf 26.2 prüft der
Login-Test außerdem die tatsächlich im Login-Paket übertragene Sichtweite.
`server_settings.py all` stellt bestehende, zuvor gestoppte Welten auf freie
Spielmodi und 16 Chunks Sichtweite um; vor dem Eingriff wird jede Welt gesichert.

Quellen: [Mojangs Versionsmanifest](https://piston-meta.mojang.com/mc/game/version_manifest_v2.json),
[offizieller Minecraft-Serverdownload](https://www.minecraft.net/en-us/download/server),
[Minecraft-EULA](https://www.minecraft.net/eula),
[Azul OpenJDK-Downloads](https://www.azul.com/downloads/).

## Grim-Varianten, 21.09.2026

Die getrennten Paper-Varianten, Java-Adapter, echten Ein-/Aus-/Verbose-Nachweise und die konkrete Supportmatrix stehen in [GRIM-LAB.md](GRIM-LAB.md). Die ursprünglichen Vanilla-Instanzen bleiben Referenzen. Neuere Prüfergebnisse und offene Kompatibilitätsfehler stehen in [REGRESSIONS-2026-09-21.md](REGRESSIONS-2026-09-21.md).
