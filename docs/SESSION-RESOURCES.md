# F5, Blasensäulen und Ressourcen beim Unterserverwechsel

Folgemeldung mit gespeicherter Paket-Ablehnung und F5-Kompass:
[REJOIN-ITEMS.md](REJOIN-ITEMS.md). Die unten aufgeführten älteren Läufe
enthielten noch keinen DISABLED-Eintrag mit verpflichtendem Paket.

Stand: 26.09.2026; Basis `2e03e916a5b32db40541075a8297c2bc0b0c789d`.
Arbeitsverlauf einschließlich fehlgeschlagener Diagnoseversuche:
[SESSION-RESOURCES-WORK.md](SESSION-RESOURCES-WORK.md).

## Ursachen und Korrekturen

### F5-Schwertblocken

Auf einem echten Vanilla-1.8.9-Backend mit ViaProxy 3.4.13 erhält Original26.2
ein Schwert mit `blocks_attacks`/Benutzungskomponenten. Das ist keine allgemeine
Freigabe alter Schwertbenutzung auf modernen Servern. ViaForge hatte diesen
Benutzungszustand bereits für Eingabe und Ego-Rendering, ließ aber in
`ServerEntityViews.blocking` nur Schilde für die F5-/Fremdspielerpose zu.

Der Renderer prüft jetzt die tatsächlich erhaltene Item-Aktion `BLOCK`.
Beim eigenen Spieler müssen benutzter Stack und Hand übereinstimmen, bei
Fremdspielern gelten die erhaltenen Hand-Metadaten. Die vorhandenen Arm- und
Held-Item-Transformationen werden weiterverwendet. Eingabe, NoSlow, Rotation
beim Use-Paket und Swing-Reihenfolge werden durch diese Änderung nicht berührt.
Ein normales modernes Schwert ohne entsprechende Komponenten bleibt `NONE`.

Realer Dreifachvergleich: `run/test-servers/session-visual-f5-1790382720435059700`.
Sieben Phasen pro Client: Beginn, Halten, Loslassen, Wiederbenutzen, Slotwechsel
und die flankierenden Ruhezustände. Vorher zeigt F5 einen gesenkten Arm trotz
aktiver BLOCK-Benutzung; danach passen Arm und gehaltenes Schwert zum Original.
Beispielbilder: `original/screenshots/blocking-188.png`,
`before/screenshots/f5-hold.png`, `fixed/screenshots/f5-hold.png`.
Das Backend wurde anhand des offiziellen Server-Downloads als **1.8.9** geprüft.

Erweiterter Zweispielernachweis:
`session-f5-extended-1790432098841539200/visual-review.json`.
Beide Clients sehen die Blockhaltung des jeweils anderen Spielers; die
Originalzustände nahe dem Screenshotzeitpunkt bestätigen using=true nur
während Hold. Ego-Transformationen wurden ebenfalls angesehen. Die Szene
bleibt in geladenen Spawn-Chunks, nachdem frühere Originalaufnahmen bei
weiten gleichzeitigen Teleports den anderen Spieler verloren hatten.
`session-f5-extended-1790429340322534800` enthält die Survival-Gegenprobe auf
Paper26.2: Original und ViaForge bleiben mit gewöhnlichem Schwert bei NONE.

### Blasensäulen

Die Darstellung liest ausschließlich die vor verlustbehafteter Via-Übersetzung
erhaltenen Fluiddescriptoren 17/18. Ein Soul-Sand-/Magma-Block allein, beliebiges
Wasser oder eine wassergefüllte Platte erzeugt keine erfundene Säule. Vor 1.13
ist dieser Darstellungspfad ausgeschaltet. Spielerphysik bleibt unverändert.

`ServerBubbleVisuals` übernimmt die originalen Display-Samples (667 Paare,
Radien 16/32), Spawnorte, Anzahl, Einstellungen und Umgebungsgeräusche.
`ServerBubbleParticle` unterscheidet aufsteigende und kreisend absteigende
Partikel. Textur, Reibung, Lebensdauer und die jeweiligen arithmetischen
Versionsgrenzen stammen aus geprüften Originalclients. Insbesondere bleiben
die Unterschiede 1.13/1.14, 1.17, 1.18.2 und 1.21.11 erhalten.

Eine aufsteigende Säulenblase verschwindet beim Verlassen des Wassers; sie
erzeugt dabei im untersuchten Original keinen zusätzlichen BubblePop.
Tatsächliche `bubble_pop`, `current_down` und `bubble_column_up`-Pakete werden
vor ihrer Ersetzung durch Via erhalten. BubblePop hat fünf Originalframes.
Der vorhandene interne Ereignistyp 340 bezeichnet dabei keine Backendversion.

Originalquellen, Zuordnung und Hashes:
`build/inspection/session-resources/bubble-sources.json`.
Realer Vergleich: `run/test-servers/session-visual-bubble-1790384512859887200`.
Der Server bestätigte echte Up-/Down-Säulen und zwei negative Blockzustände.
Aufnahmen enthalten Außenansicht, unter Wasser, Oberfläche sowie verringerte
und minimale Partikel. `particle-review.json` dokumentiert die Bildprüfung:
vorher keine Säulenblasen, danach beide Richtungen. Die kurzen Zufallsfenster
beweisen keine exakte statistische Gleichheit. Wasserfarbe/Fog unterscheiden
sich weiterhin zwischen den Renderern. Kein neuer Physik-PASS wird abgeleitet.

Die drei echten Partikelpakete wurden zusätzlich im Lauf
`session-bubble-packets-1790433154238487800` verglichen. Alle sechs Bilder
wurden angesehen; `visual-review.json` enthält Bilder, Lichtwerte und Grenzen.
Die fünf Pop-Frames aus dem tatsächlich geladenen ResourceManager stimmen
pixelgenau mit Original26.2 überein. Beide Clients verwenden hier gamma=0.5
und dieselbe Tageszeit. Frühere Bilder mit den unterschiedlichen Defaults
(1.8: 0, 26.2: 0.5) zeigten dunklere Pop-Partikel und gelten nicht als
Helligkeitsvergleich. Zufällige Positionen und Aufnahmezeitpunkte bleiben
unterschiedlich; Gesamtbild-Pixelgleichheit wird nicht behauptet.

### Erneutes Join auf bestehender Verbindung

Der native 1.8-Handler öffnet bei Join `GuiDownloadTerrain`, setzt aber
`doneLoadingTerrain` nur beim Erzeugen des Handlers initial auf false. Beim
ersten Positionspaket schließt er den Bildschirm nur bei false → true.
Ein Proxy kann auf demselben Handler ein weiteres Join senden: Dann bleibt
der neu geöffnete Bildschirm stehen, obwohl Position und Chunks da sind.

`MixinResourceLoading` setzt beim tatsächlichen Laden der neuen Join-Welt
`doneLoadingTerrain` auf false. Das echte Positionspaket und die vorhandene
Ressourcenbereitschaft bestimmen weiterhin das Ende des Ladeschutzes. Kein
Timeout, kein zusätzliches Physiktick, keine Entfernung der Bewegungssperre.
Session-Tickets für Zielressourcen und die versionsabhängige Paketqueue bleiben
erhalten. Serverpakete besitzen zusätzlich Verbindungs- und Request-Identitäten.

Lokale Reproduktion: Velocity 4.2.0 build 30, zwei Paper26.2-Backends,
Lobby → GunGame auf derselben Verbindung/Dimension.

* Vorher: `session-before-proxy-v2-1790370753726612100`: nach S01 Join/S08
  Position bleibt der Bildschirm, obwohl terrain_ready/loaded=true und
  awaiting_world=false; Screenshot und JSONL vorhanden.
* Danach: `session-fixed-proxy-v1-1790370865857581200`: zwölf tatsächliche
  abwechselnde Wechsel erfolgreich.
* Original26.2: `session-proxy-original-1790385076703479300`: Wechsel während
  HTTP-Download erfolgreich. Auf dieser Route sendet Velocity **kein** Pop.
  Das fertige Paket bleibt aktiv. Ein explizites Pop verhindert dagegen eine
  noch ausstehende Aktivierung. ViaForge wird gegen dieses Verhalten geprüft.
* `session-switch-edges-1790432200005400800`: zwölf weitere schnelle Wechsel
  (ca. 1.8–3.7 s je Übergang), vier tatsächliche Nether/Overworld-Respawns und
  Reconnect, insgesamt 17 Fälle PASS. Native S01/S07/S08-Beobachtung vorhanden.

Dies belegt einen lokalen Fehlermechanismus. Gomme-Backendversionen und die
historische Paketfolge des gemeldeten Lobby → GunGame-Hängers sind unbekannt;
eine öffentliche Reproduktion wird nicht behauptet.

### Ressourcenpaket-Crash

Vollständiger Ausgangsbericht:
`run/crash-reports/crash-2026-09-25_21.37.49-client.txt`.
`ResourcePackRepository` erstellt den Benutzerpaketordner, aber nicht sicher
den getrennten Servercache. `deleteOldServerResourcesPacks` ruft bereits vor
dem HTTP-Download `FileUtils.listFiles` auf diesem Pfad auf. Bei fehlendem
Verzeichnis entsteht die gemeldete Exception aus GuiYesNo.

`session-before-pack-v3-1790370172453620800` reproduziert genau diesen Aufruf
mit einem echten Client, fehlendem Cache und tatsächlichem Klick auf Annehmen.
Die Diagnosehilfe protokolliert die originale Exception und beendet ihre
eigene Testinstanz. Sie erzeugt deshalb keinen zweiten normalen Crashbericht.

`ServerPackDownload` erstellt den aufgelösten Cache vor dem ersten Zugriff,
lädt in eine eigene temporäre Datei, prüft HTTP-Erfolg/Größe/SHA-1 und publiziert
erst vollständige Bytes. Ein vorhandener anderer Dateityp am Cachepfad wird
nicht gelöscht. Benutzerpakete und alte Caches werden nicht pauschal bereinigt.
Verifizierte Cachedateien werden wiederverwendet. Reload und Download sind
getrennte Phasen; LOADED folgt erst nach tatsächlich abgeschlossenem Reload.
Fehler führen zu passendem Status und erhalten die ausgewählten Benutzerpakete.

Ab 1.20.2 werden Konfigurationsanforderungen vor der verlustbehafteten
Übersetzung erhalten, ab 1.20.3 einschließlich UUID, Push/Pop und Reihenfolge.
Damit entfallen die von Via für einen alten Client simulierten vorzeitigen
Erfolgsmeldungen. Später gepushte Pakete haben Vorrang. Entfernung eines Packs
stellt darunterliegende Ressourcen wieder her. Alte Download-Callbacks dürfen
nach Pop, Ersetzung oder Disconnect keine neue Session überschreiben.
HTTP-Version/Packformat folgen den geprüften Zielressourcen; Original26.2
meldet beispielsweise `X-Minecraft-Pack-Format: 88.0`.

`session-pack-cases-1790428658802363100` bestand 16 tatsächliche Clientprüfungen:
Ablehnen ohne HTTP, frischer Cache, Textur-Reload, Download-/Reload-Reihenfolge,
Priorität, Pop, Cache-Reuse, ungültige ZIP, HTTP503, Hashfehler, Wechsel während
Download, neuer Backend-Pack, explizites Pop bei laufendem Download und Reconnect.
Der damalige Header-Test prüfte noch `88`; der spätere Originalvergleich
forderte die oben beschriebene Korrektur auf `88.0`. Der alte Bericht bleibt
als konkreter Lauf erhalten. Texturbelege stehen im Client-Screenshotordner
und als tatsächlich gelesene ResourceManager-Pixel in `observations.jsonl`.

Konfigurationsphase: Original `session-config-original-1790429017804105100`
und ViaForge `session-config-fixed-1790429239139941700` bestanden die jeweiligen
Prüfungen. Der ViaForge-Lauf prüft zusätzlich erneutes LOADED für das aktive
Paket nach Pop/Reload und keinen unnötigen Reload nach HTTP-Fehler (9/9).
`session-pack-legacy-1790429699236512600` prüft den nativen 1.8.9-Pfad separat:
Ablehnen, fehlender Cache, Textur-Reload und Cache-Reuse mit tatsächlichen
C19-Meldungen DECLINED bzw. ACCEPTED → SUCCESSFULLY_LOADED.

`session-pack-legacy-1790432518159020200` wiederholt diese drei Abläufe und
prüft zusätzlich den echten gebackenen Stein-Block nach Modell-Reload:
sechs Quads mit Y=0 bis 0.5 aus dem heruntergeladenen nativen Modell.
Die vorherige Textur wird ebenfalls tatsächlich im ResourceManager gelesen.
Konfigurations-Ablehnen wurde separat geprüft: Original
`session-config-original-1790433264529629300` und ViaForge
`session-config-fixed-1790433289895149200`, jeweils kein HTTP-Download und
anschließend erfolgreicher Weltbeitritt.

Abschließende Wiederholung mit aktuellem Code:
`session-pack-cases-1790433385130986000`, **16/16 PASS**, einschließlich
Header88.0, Download während Unterserverwechsel, explizitem Pop und Reconnect.
Die vorherige Wiederholung `session-pack-cases-1790433327403495000` wurde
schon beim ersten Backendstart unterbrochen; keine Clientaktivität und kein PASS.

## Regressionen und Lieferung

151 JUnit-Tests ohne Fehler; vollständiger Forge-Smoke über 49 Ressourcenprofile:
`build/logs/session-resources-client-smoke-20260926-v1.txt`, frischer PASS vom
26.09.2026, 16:26. Der Smoke umfasst auch die neue Partikel-Paketzuordnung und
alle Original-Pop-Frames, echte Ressourcen-/Sessionwechsel und die bisherigen
Interaktions-, Hand-, Schwimm- und Flugregressionen. Die Spielerphysik wurde
für diese vier Meldungen nicht umgebaut. Die separate Python-Suite umfasst
110 Tests; Abschlussausgabe: `build/logs/session-resources-python-final.txt`.

Finaler Build: `build/logs/session-resources-build-final.txt`.
Release und passende Quellen liegen unter `build/libs/`; Größen, SHA-256,
Dateiabgleich des gesamten Quellenarchivs und konkrete Laufnachweise stehen in
`build/libs/session-resources-release.json`. Die vorherige Release samt Quellen
und ihren ursprünglichen Hashes bleibt unter
`build/backups/session-resources-initial/build/libs/` erhalten.
Es wurde weder committed noch gepusht.

## Grenzen und Reproduktion

Der Crashfix ist keine vollständige Konvertierung moderner Ressourcenformate.
Die jetzige Überlagerung unterstützt native Ressourcen und statische moderne
Texturpfade; moderne Itemdefinitionen, Atlasdefinitionen, Pack-Overlays,
zusätzliche Modelle, Shader und alle Versionsdetails beliebiger Fremdpakete
sind damit nicht vollständig portiert. Ein erfolgreiches Download-/Reload-
Statuspaket ist kein Nachweis, dass jedes moderne Packelement dargestellt wird.
Mehrere gleichzeitige fehlerhafte Downloads sind noch kein umfassend
originalverifizierter Batchfall. Live-Visualisierung wurde mit Ziel26.2 geprüft;
Quellen-/Pipelineprüfungen decken weitere Versionsgrenzen ab.

Die Harnesses erstellen eigene Instanzen ausschließlich auf 127.0.0.1,
sichern Welten vor Fixture-Änderungen und erhalten jeden Lauf. Sie senden echte
Eingaben/Serverpakete und beobachten; sie ersetzen keine Netzwerkhandler oder
Spielerphysik. Normale Profile und Nutzerclients werden nicht verändert.

```powershell
$env:VIAFORGE_NO_PAUSE='1'
./build.bat build
py -3 tools/test-servers/session_visual_cases.py f5
py -3 tools/test-servers/session_visual_cases.py bubble
py -3 tools/test-servers/session_bubble_packets.py
py -3 tools/test-servers/session_f5_extended.py
py -3 tools/test-servers/session_switch_edges.py
py -3 tools/test-servers/session_visual_probe.py --mode fixed-proxy
py -3 tools/test-servers/session_pack_cases.py
py -3 tools/test-servers/session_pack_original.py original
py -3 tools/test-servers/session_pack_original.py fixed
py -3 tools/test-servers/session_pack_original.py original --decline
py -3 tools/test-servers/session_pack_original.py fixed --decline
py -3 tools/test-servers/session_pack_legacy.py
py -3 tools/test-servers/session_proxy_original.py
py -3 -m unittest discover -s tools/test-servers -p 'test_*.py'
```

Die neuen Fälle sind von den weiterhin offenen historischen Timer-/Simulation-
Flags und der unvollständigen Swept-Effects-Verifikation 1.21.2–1.21.8 getrennt.
Keine dieser alten Grenzen wird durch diesen Bericht geschlossen.
