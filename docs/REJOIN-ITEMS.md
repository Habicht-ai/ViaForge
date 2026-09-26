# Erneuter Beitritt nach Serverpaket und Größe gehaltener Items

Stand: 26.09.2026, Basis `2e03e916a5b32db40541075a8297c2bc0b0c789d` mit den
uncommitteten Änderungen aus [SESSION-RESOURCES.md](SESSION-RESOURCES.md).
Die damaligen Tests deckten keinen gespeicherten DISABLED-Eintrag mit einem
verpflichtenden Paket ab. Die dortige Freigabe wird nicht auf diesen Fall erweitert.

## Befund aus dem Nutzerlauf

`run/logs/latest.log` und `fml-client-latest.log` vom 26.09., 16:43–16:47 wurden
vor neuen Starts gesichert, ebenso Serverliste, Cache und vorherige Artefakte:
`build/backups/rejoin-items-1790434204730097100/manifest.json`.
Bei der Bestandsaufnahme liefen keine Java-/Python-Spielprozesse.

Zwei Gomme-Einträge enthalten tatsächlich `acceptTextures=0`; ein dritter hat
keine explizite Einstellung. Die vorige Implementierung hielt eine Zustimmung
nur im ServerData-Objekt und speicherte sie nicht in `servers.dat`. Bei einem
später wieder aus der Datei geladenen DISABLED-Eintrag lehnte sie auch ein
Pflichtpaket automatisch ab und schloss den Kanal ohne aussagekräftigen Grund.

Das ist lokal mit echtem Client reproduziert:
`rejoin-items-before-1790434479342958900`. Pflichtpaket + gespeichertes DISABLED
erzeugen ohne HTTP-Zugriff genau `Connection Lost / Disconnected`; Screenshot
`client/screenshots/disabled-required-disconnect.png`.
Original 26.2 (`rejoin-items-original-1790434602265796600`) zeigt unter derselben
gespeicherten Einstellung eine erneute Abfrage, lädt nach Zustimmung und
speichert `acceptTextures=1`. Originalquellen aus dem bereits SHA-geprüften
Client stehen unter `build/inspection/rejoin-items/`.

Das belegt den lokalen Fehlermechanismus. Der alte öffentliche Gomme-Lauf
enthält keine vollständige Paketaufzeichnung; dessen Pflicht-Bit wird daher
nicht nachträglich behauptet. Sein früheres `disconnect.timeout` und weitere
Status-Ping-/FML-Meldungen sind keine Belege für denselben Mechanismus.
Es wurden keine automatisierten öffentlichen Serverversuche durchgeführt.

## Korrektur der Abfrage

`ModernServerPacks` fragt bei erforderlichem Paket und bisheriger Ablehnung
erneut nach. Zustimmung wird über den nativen ServerList-Speicherpfad gesichert.
Eine explizite Ablehnung eines Pflichtpakets beendet weiterhin die Verbindung,
jetzt mit einem verständlichen Grund; sie setzt wie das Original keine neue
gespeicherte Ablehnungspräferenz. Die Abfrage kennzeichnet die Pflicht und die
Schaltflächen Proceed/Disconnect. Optionale Pakete bleiben ablehnbar.
UUID, Statusreihenfolge, Reload und Verbindungs-/Requestbesitz bleiben erhalten.
DEBUG-Meldungen protokollieren Paket-ID, Pflicht, Präferenz und Statusphase.

## F5-Kompass und Uhr

Ab 1.21.4 werden diese Modelle über Itemdefinitionen und Winkel-/Zeitbereiche
ausgewählt. Die Auflösung betrachtete nur `fallback`; Kompass und Uhr besitzen
stattdessen gültige Einträge ab Schwellwert 0. Ihre Aliasmodelle fehlten deshalb,
und die Handdarstellung fiel auf native 1.8-Transformationen zurück.

Die Auflösung berücksichtigt jetzt den bei 0 gültigen Range-Eintrag samt
vererbten Originaltransformationen. Die Handgröße stammt weiterhin aus dem
Zielmodell (beim generierten Originalitem Third-Person-Skala 0.55).
Die vorhandene native Texturanimation bleibt erhalten. Kein globaler
Skalierungsfaktor und keine Änderung der Spielerphysik. Handmodell-Caches
werden außerdem beim echten Ressourcen-Reload invalidiert.

Vorher-/Originalbilder für Kompass, Uhr und unverändertes Schwert liegen in
den oben genannten Clientläufen. Neue GL-Prüfungen vergleichen beide Hände
in First-/Third-Person zusätzlich für Kompass, Uhr und Papier über die
Ressourcenprofile mit den unabhängig gelesenen Originalmatrizen.
Zwei JUnit-Fälle prüfen fehlenden Fallback und einen positiven Schwellwert,
der den bei 0 gültigen Fallback nicht verdrängen darf.

## Reproduktion

```powershell
$env:VIAFORGE_NO_PAUSE='1'
./build.bat build
py -3 tools/test-servers/rejoin_item_probe.py before
py -3 tools/test-servers/rejoin_item_probe.py original
py -3 tools/test-servers/rejoin_item_probe.py fixed
```

Die Instrumentierung setzt Einstellungen und echte Eingaben in isolierten
Clients. Sie ersetzt keine Netzwerkhandler. Alle Dienste binden an 127.0.0.1;
Welten werden vor Fixture-Änderungen gesichert. Nutzer-Serverliste und Cache
werden zur Untersuchung nur gelesen und gesichert, nicht umgestellt oder gelöscht.

Die Formatgrenzen moderner Fremdpakete aus SESSION-RESOURCES.md bestehen fort.
Der gesicherte tatsächliche Gomme-Cache enthält moderne Pack-Overlays und
Itemdefinitionen; dieser Fix bedeutet keine vollständige Unterstützung davon.
Die vorherigen Timer-/Simulation- und Swept-Effects-Grenzen bleiben ebenfalls offen.

## Tatsächliche Nachprüfung

`rejoin-items-fixed-1790434941629859200`: sechs Verhaltensprüfungen bestanden.
Gespeichertes DISABLED → erneute Pflichtabfrage → Annehmen → gespeichertes
ENABLED → Disconnect → erneutes Lesen des ServerList-Eintrags → Beitritt aus
Cache ohne Abfrage und ohne weiteren HTTP-Zugriff. Anschließend explizite
Pflicht-Ablehnung aus PROMPT: verständlicher Disconnect, PROMPT bleibt erhalten.
Die drei F5-Bilder wurden gegen Original und Vorher angesehen; Details stehen
in `visual-review.json` dieses Laufs.

Der erste Nachherlauf `rejoin-items-fixed-1790434836686746200` bleibt FAIL:
Seine letzte Assertion erwartete fälschlich, dass ein im Test ausdrücklich
auf DISABLED gesetztes ServerData einen älteren ENABLED-Wert auf der Platte
erhalten müsse. Der Originalcode speichert den tatsächlichen ServerData-Wert.
Die Wiederholung prüft deshalb einen konsistent gespeicherten PROMPT-Eintrag
und dessen Erhalt beim Ablehnen eines Pflichtpakets. Kein Produktcode wurde
aufgrund dieser falschen Testannahme umgebaut.

Abschließende Paketregression: `session-pack-cases-1790435394104510700`,
16/16 PASS mit dem neuen Code. Enthalten sind optionale Ablehnung, frischer
Cache, Textur-Reload, Hash-/HTTP-/ZIP-Fehler, Paketpriorität, Pop, Wechsel
während Download und Reconnect mit veraltetem Callback.

Vollständiger neuer Forge-Smoke: `build/logs/rejoin-items-client-smoke-v1.txt`,
49 Profile, frischer PASS vom 26.09.2026 gegen 17:05. Die echten GL-Matrizen
von Kompass, Uhr und Papier entsprechen für beide Hände dem jeweiligen
Originalmodell. 153 JUnit-Tests ohne Fehler. Die separate Python-Suite umfasst
110 Tests; Abschlussausgabe `build/logs/rejoin-items-python-final.txt`.

Finaler Build: `build/logs/rejoin-items-build-final.txt`.
Release und passende Quellen: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`
und `ViaForge-1.8.9-4.4.0-client.1-sources.zip`. Größen, SHA-256, der Abgleich
aller Quellenarchivdateien, getesteter Produktionsklassen und gesicherter
Nutzerdaten stehen in `build/libs/rejoin-items-release.json`.
Der Entwicklungsclient in `run/mods/` enthält die Korrekturen ebenfalls.
Kein Commit und kein Push.
