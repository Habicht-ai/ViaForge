# Ressourcen, Unterserverwechsel und visuelle Regressionen

Arbeitsbeginn 25.09.2026, Basis `2e03e916a5b32db40541075a8297c2bc0b0c789d`.
Git war sauber; keine Java-/javaw-/Python-Spiel- oder Testprozesse liefen.
Im Repository und den übergeordneten Verzeichnissen wurde keine AGENTS.md gefunden.
Die dreizehn in der Übergabe genannten Dokumente wurden gelesen; historische
PASS-Ergebnisse gelten ausschließlich für ihre damaligen Fälle.

Vor neuen Builds sind Release, Quellen, Entwicklungs-JAR, Konfiguration und
vorhandene Clientlogs unter `build/backups/session-resources-initial/` gesichert.
Das dortige Manifest enthält Größen und SHA-256. Keine Welt wurde verändert.
Kein Commit oder Push ist beauftragt.

## Erste Befunde, noch keine Fixnachweise

* Der Crash vom 21:37:49 entsteht in `deleteOldServerResourcesPacks`, noch vor
  dem HTTP-Download. `run/server-resource-packs` existiert bei der Bestandsaufnahme
  nicht. Das beweist noch nicht den historischen Zustand zum Crashzeitpunkt.
  Passendes gesichertes Log: `2026-09-25-6.log.gz.txt`. Die Servermarke im
  Crashbericht lautet `Cheetah (Velocity)`; Backendversion und Wechselroute sind
  dadurch nicht bestimmt. Das Log zeigt Verbindungsversuche um 21:37:43/44 und
  einen Atlaswechsel um 21:37:48. Keine erfundene Paketfolge.
* `ServerEntityViews.blocking` akzeptiert ausschließlich Schilde. Die bereits
  vorhandene Komponenten-Schwertbenutzung und Ego-Pose erreichen diesen
  F5-Armhaltungspfad daher nicht.
* Der native `handleJoinGame` öffnet `GuiDownloadTerrain`, setzt aber den auf
  demselben Handler bereits wahren Wert `doneLoadingTerrain` nicht zurück.
  `handlePlayerPosLook` schließt den Bildschirm nur beim Übergang false → true.
  Erneutes Join auf bestehender Verbindung muss gezielt reproduziert werden.
* Bubble-Physik wird nicht aufgrund der Partikelmeldung geändert. Für neue
  Partikel ist der erhaltene echte Bubble-Column-Zustand maßgeblich.

Ausstehend: Originalquellen/Versionsgrenzen, echte Vorher-/Nachherclients,
isolierte Proxy-/Pack-Reproduktion, gezielte Korrekturen, visueller Vergleich,
Regressionen, vollständiger Forge-Smoke und neue Release mit passenden Quellen.

## Wiederaufnahme nach Nutzungslimit, 26.09.2026

HEAD unver?ndert, keine Nutzer- oder Test-Java-/Python-Prozesse bei Wiederaufnahme.
Alle ?nderungen und die Windows-Snapshot-Korrektur der anderen Instanz erhalten.

Tats?chlich ausgef?hrte F?lle (noch keine abschlie?ende Freigabe):
* `session-before-pack-v3-1790370172453620800`: echter Forge-Client best?tigt
  Serverpaket auf Paper 26.2; Cache fehlt, identische directory-Exception aus
  `deleteOldServerResourcesPacks`. Eingabe ?ber die originale GuiYesNo-Clickmethode;
  die Diagnosehilfe protokolliert deren Exception und beendet ihre eigene Instanz.
* `session-fixed-pack-v1-1790370235429816500`: gleicher Ablauf mit frischem Cache,
  Ressourcen-Reload und ACCEPTED -> SUCCESSFULLY_LOADED, Client bleibt aktiv.
  Textur-/Fehler-/Stacktests stehen noch aus.
* `session-before-proxy-v2-1790370753726612100`: Velocity 4.2.0 build 30,
  zwei echte Paper-26.2-Backends. Lobby -> GunGame erzeugt neues S01 Join auf
  demselben Handler und S08 Position. Bildschirm bleibt GuiDownloadTerrain,
  obwohl terrain_ready=true, loaded=true, awaiting_world=false. Screenshot
  und JSONL liegen im Clientverzeichnis. Kein Timeout als Produktfix.
* `session-fixed-proxy-v1-1790370865857581200`: zw?lf tats?chliche Wechsel
  abwechselnd Lobby/GunGame erfolgreich. Moderne Ressourcenpakete bei diesen
  Wechseln noch nicht gepr?ft. Keine Aussage ?ber unbekannte Gomme-Backends.
* `session-visual-f5-1790371235328072600`: echter Vanilla-1.8.9-Server und
  ViaProxy 3.4.13 starteten. Original26.2-Diagnose scheiterte an nicht mehr
  vorhandenem Minecraft.screen-Feld; kein F5-PASS. Originalbeobachter korrigieren.

Build `session-bubble-build-v1.txt` erfolgreich; neue Blasenimplementierung
noch ohne visuellen Vergleich. Sie benutzt ausschlie?lich Fluiddescriptor17/18,
keine neue Spielerphysik. Originalquellen und Hashes siehe
`build/inspection/session-resources/bubble-sources.json`.

Offen: F5 Original/Vorher/Nachher inklusive anderer Spieler und Itemwechsel;
Bubblebilder, tats?chliche H?ufigkeit/Einstellungen/Versionsgrenzen und
Paketpartikel; Fehler/Reload/Status/mehrere moderne Packs; Wechsel w?hrend
Download/Reconnect; vollst?ndige Regressionen und Forge-Smoke; finale Release
und passende Quellen mit neuen Hashes. Kein Commit/Push.

## Verifizierter Zwischenstand, 26.09.2026, Wiederaufnahme 15:17

HEAD weiterhin `2e03e916`; die aufgelisteten Änderungen sind uncommittet.
Bei Wiederaufnahme keine Java-/Python-Spielprozesse. Frühere Artefakte bleiben
unter `build/backups/session-resources-initial` erhalten.

* F5: `session-visual-f5-1790382720435059700` enthält Original26.2,
  unveränderte Produktionsbasis mit reinem Beobachter und korrigiertes ViaForge
  auf demselben hashgeprüften **Vanilla-1.8.9**-Backend über ViaProxy 3.4.13.
  Sieben echte Eingabephasen: idle/start/hold/release/reuse/switch/after_switch.
  Original, alte und neue Hold-Aufnahme wurden angesehen: vorher gesenkter Arm
  trotz BLOCK, danach zum Original passende Blockhaltung samt Schwert.
  Gegenprobe auf nativem modernem Backend und Fremdspieler noch ausstehend.
* Bubble: `session-visual-bubble-1790384512859887200` enthält denselben
  Original/Vorher/Nachher-Vergleich auf Paper26.2. Tatsächliche Blockzustände
  beider Säulen sowie trockener Soul Sand und wassergefüllte Platte wurden
  serverseitig geprüft. Außen-/Unterwasser-/Oberflächenbilder angesehen:
  vorher fehlen Blasen, danach sind beide Richtungen sichtbar. Keine Änderung
  an Spielerphysik. Bewegungsdaten: Down vy=-0.05; Up nähert sich
  0.0283333386. Reduzierte/minimale Einstellung verringert sichtbare Anzahl.
  `particle-review.json` benennt kurze stochastische Messfenster und bestehende
  Unterschiede der Wasserfärbung; keine behauptete exakte Bildgleichheit.
  Der frühere Lauf `...1790383161769136100` hatte ungeladene Fixture-Chunks und
  beweist nichts. Die Korrektur erstellt Blöcke erst nach dem Clientbeitritt.
* Packs: erster umfassender Lauf `session-pack-cases-1790383452918973200`
  fand nach sieben erfolgreichen Fällen vertauschte Statuswerte INVALID_URL
  und FAILED_RELOAD im neuen Code. Am offiziellen 26.2-Bytecode verifiziert
  und durch benannte Statuswerte korrigiert. HTTP-Header-Packformat stammt
  nun aus hashgeprüften Original-JARs (`pack-formats-proof.json`).
* `session-pack-cases-1790383940031754900` verwendete versehentlich die ältere
  installierte Entwicklungs-JAR. Neue Harness-Clients kopieren direkt aus
  `build/development` und protokollieren den SHA-256 in `artifact.json`.
  `...1790384053178045000` wurde vor Clientstart unterbrochen, kein PASS.
* `session-pack-cases-1790384143533164000`: tatsächlich geprüft sind Ablehnen,
  frischer Cache, Textur-Reload, Header88, DOWNLOADED vor LOADED, Stapelpriorität,
  Pop, Cache, ungültige ZIP, HTTP503, Hashfehler und erfolgreicher Wechsel
  während Download. Gesamt-FAIL: Test nahm automatisches Velocity-Pop an.
* Diese Annahme ist widerlegt: `session-proxy-original-1790385076703479300`
  protokolliert **keine** Pop-Pakete während des lokalen Velocity-Wechsels.
  Original26.2 behält Pakete, lässt den Download fertig werden und zeigt die
  neue grüne Textur. Explizites Pop während eines weiteren Downloads verhindert
  dessen Aktivierung (PASS). Harness entsprechend korrigiert; kein Produktfix
  soll fälschlich alle Packs bei einem normalen Unterserverwechsel entfernen.
  Erster Originalversuch scheiterte nur am UI-Klassennamen PackConfirmScreen.

Build `session-resources-build-v3.txt`: erfolgreich, 151 JUnit-Tests ohne Fehler.
Danach hinzugefügte Atlas-/Partikel-Pipeline-Smokes sind noch nicht ausgeführt.
Offen: vollständige Packfälle nach Harnesskorrektur; Konfigurationsphase am
Original und ViaForge; erweiterte F5-Bilder; gesamte Python-Suite und frischer
49-Profil-Forge-Smoke; Dokumentation und finale JAR/Quellen/Hashnachweise.

## Weiterer Stand 26.09.2026, 15:36

* `session-pack-cases-1790428658802363100`: 16/16 tatsächliche Prüfungen PASS,
  einschließlich explizitem Pop bei laufendem Download und Reconnect. Datei
  `client/artifact.json` weist den wirklich verwendeten Build aus. Der Header
  war hier noch `88`; am Original wurde anschließend `88.0` nachgewiesen.
* `session-config-original-1790429017804105100`: Original26.2, Konfigurations-
  Abfrage, Textur, Header88.0, Paketpriorität, Pop und ZIP-/HTTP-Fehler PASS.
  Statuslog zeigt erneutes SUCCESSFULLY_LOADED für das beibehaltene Paket nach
  tatsächlichem Reload. Deshalb wurde dieser Unterschied im neuen Code
  korrigiert; unveränderte Bestände nach HTTP-Fehler werden nicht neu geladen.
* `session-config-fixed-1790429239139941700`: neun entsprechende Prüfungen
  mit ViaForge PASS, einschließlich obiger Statusdetails.
* `session-pack-legacy-1790429699236512600`: echter Vanilla1.8.9-Server,
  ausgewähltes Protokoll47, Ablehnen/frischer Cache/Reload/erneutes Laden aus
  Cache PASS. Tatsächlich beobachtete C19-Statusfolge: DECLINED, zweimal
  ACCEPTED + SUCCESSFULLY_LOADED. Damit wurde auch der native Repository-Pfad
  unabhängig von der modernen UUID-Implementierung live geprüft.
* F5 erweitert `session-f5-extended-1790429340322534800`: moderne Gegenprobe
  Original und ViaForge auf Paper26.2 in Survival beide NONE/using=false.
  ViaForge sieht die richtige Remote-Pose des Originalspielers. Die umgekehrte
  Kameraaufnahme enthielt keinen Fremdspieler; erste Ego-Originalaufnahme hatte
  noch Stein ausgewählt. Kein pauschaler PASS für diesen Lauf.
* Wiederholung `session-f5-extended-1790429543401769100`: Ego-Bilder Original
  und ViaForge angesehen, Schwertblocktransform stimmt überein. Native Remote-
  Liste zeigt den Forge-Spieler zunächst im Spawngebiet, nach gleichzeitigen
  Fixture-Teleports fehlt er. Das ist im Originalclient beobachtet, kein Beweis
  eines ViaForge-Renderfehlers. Harness prüft nun echte Wiederaufnahme ins
  Tracking, bevor er Remote-Bilder bewertet. Dritter Lauf läuft um 15:36.
* Python-Suite: `session-resources-python-v1.txt`, 110 Tests, OK.
* Buildv5 erfolgreich. Aktueller installierter Grim-JAR tatsächlich gelesen:
  2.3.74-8eb5f28, SHA256
  `91c06e7ae7da53636bc5e500d5af3d36a6180247e155fa5b4340da5a72f9eeb7`.
  Die neuen isolierten Sitzungs-/Bildtests verwenden keinen Grim und werden
  nicht als Grim-PASS ausgegeben.

Noch auszuführen: letzte Remote-Bildprüfung, schnelle Wechsel/Dimensionen,
Konfigurations-Ablehnen, abschließender Packlauf mit aktuellem Statuscode,
vollständiger 49-Profil-Smoke und finale Release/Quellen/Hashnachweise.

## Wiederaufnahme und Nachweise, 26.09.2026, 16:21

Wiederaufnahme um 16:14:58: HEAD unverändert, keine Java-/Python-Spielprozesse.
* F5 `session-f5-extended-1790432098841539200`: Szene in geladenen Spawn-Chunks.
  Jetzt ist der Forge-Fremdspieler im Original tatsächlich vorhanden und
  sichtbar. Idle/Hold-Bilder angesehen; Arm/Schwert passen. Zustandsstichproben
  **am Aufnahmezeitpunkt** (action_tick ca.30) zeigen nur bei Hold using=true,
  nach Release false, nach Swap Stone/false. `visual-review.json` dokumentiert
  die Auswahl. Ende einer Aktionsdatei kann bereits die nächste Inputphase
  enthalten, deshalb wurde nicht der jeweils letzte Datensatz ausgewertet.
  Die vorherige weite Teleport-Reproduktion bleibt als Harnessgrenze erhalten.
* `session-switch-edges-1790432200005400800`: zwölf schnelle Wechsel, vier
  tatsächliche Nether/Overworld-Wechsel und Reconnect, 17 Fälle PASS. Die
  Wechsel dauerten ca.1.8–3.7s. JSONL enthält echte S01/S07/S08-Pakete;
  Schutzbedingungen bei Rückkehr: terrain_ready/loaded und !awaiting_world.
* Buildv6 erfolgreich (46s); hinzugefügt wurden passive Pop-Partikelzählung
  und Modell-Vertexbeobachtung im Entwicklungsbeobachter. Spielerphysik und
  Netzwerkhandler bleiben original.
* Lauf `session-bubble-packets-1790432425555462000` prüft derzeit echte
  Server-Partikelpakete. Original erzeugte jeweils 900 Instanzen der drei
  erwarteten Klassen. Noch kein finaler Bildnachweis für ViaForge in diesem Lauf.

Nächste Schritte: Partikelbilder prüfen; native Modell-Reload-Probe;
Konfigurations-Ablehnen und letzter moderner Paketlauf; danach frischer
vollständiger Forge-Smoke, Dokumentationsabschluss und Release-Manifest.

## Abschlussprüfung, 26.09.2026, 16:40

* Vollständiger Forge-Smoke `session-resources-client-smoke-20260926-v1.txt`:
  49 Profile, frischer PASS um16:26. Neue Partikeltypen und Pop-Atlasprüfungen
  enthalten. Buildv7 mit passiver Licht-/Framebeobachtung ebenfalls erfolgreich.
* `session-bubble-packets-1790433154238487800`: sechs echte Original-/ViaForge-
  Bilder angesehen, Details in `visual-review.json`. Tatsächlich geladene
  Pop-Frames pixelgenau gleich. Beide Clients mit gamma0.5 und Tageszeit noon;
  der zuvor auffällige dunkle Pop kam aus unterschiedlichen Clientdefaults,
  nicht aus falschen Texturen. Keine zusätzliche Renderkorrektur vorgenommen.
* `session-pack-legacy-1790432518159020200`: drei Fälle PASS, einschließlich
  nativem Modell-Reload. Tatsächliche gebackene Stein-Geometrie hat sechs Quads
  und Y0..0.5; frischer Cache/Reuse und echte C19-Meldungen beobachtet.
* CONFIG-Ablehnen: Original `session-config-original-1790433264529629300`
  und ViaForge `session-config-fixed-1790433289895149200` PASS, kein HTTP,
  Weltbeitritt erfolgreich.
* `session-pack-cases-1790433327403495000` bei Wiederaufnahme um16:36 ohne
  Prozesse und ohne Clientaktivität vorgefunden: INTERRUPTED dokumentiert.
  Wiederholung `session-pack-cases-1790433385130986000`: alle16 Prüfungen PASS.
* Keine Java-/Python-Spielprozesse nach Abschluss dieser Läufe. Die fremde
  Windows-Snapshotkorrektur in der Diagnosehilfe ist erhalten. Kein Commit/Push.

Abschließender Build, Python-Protokoll und Artefakt-/Quellenabgleich werden in
`session-resources-build-final.txt`, `session-resources-python-final.txt` und
`build/libs/session-resources-release.json` dokumentiert. Die Anleitung und
alle verbleibenden Grenzen stehen in `docs/SESSION-RESOURCES.md`.
