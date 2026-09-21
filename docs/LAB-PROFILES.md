# Ein Testserver pro Minecraft-Version

Stand: 21.09.2026. Die Webverwaltung und `Testserver.bat` verwenden genau einen
aktiven Server pro Version. Die Anzahl kommt aus den Manifesten, nicht aus einer
festen Obergrenze. `profiles.json` ordnet derzeit 49 Versionen ihren vorhandenen
Instanzen zu. Anzeigen, Favoriten, Aktionen, Logs, Kataloge und Sicherungen verwenden
einfache Namen wie `26.2`. Der bisherige interne Ordner `26.2-grim` bleibt erhalten,
damit Welten, Inventare, Pluginzustände und Backup-Zuordnungen nicht verschoben werden.

Grim an/aus benutzt dieselbe Welt, Plattform und Adresse. Ein Wechsel zu Vanilla
ist zum Abschalten nicht nötig. Die alten Vanilla-Welten sind erhaltene Referenzarchive,
keine weiteren Einträge in der normalen Verwaltung. Es wurde keine Welt gelöscht,
ersetzt, zusammengeführt oder neu generiert. Änderungen in einer Archivwelt werden
nicht automatisch in die aktive Welt übertragen.

## Tatsächliche Verfügbarkeit

41 Profile verwenden die vorhandenen, startgeprüften Paper/Grim-Installationen.
Diese acht Versionen bleiben als Vanilla-Profile erreichbar:

| Version | Grund für fehlendes Grim |
|---|---|
| 1.9, 1.9.1, 1.9.2, 1.11, 1.16 | Kein nutzbarer, exakt versionsgleicher Plattformbuild installiert; Quellen-/Buildgrenzen in GRIM-LAB dokumentiert. |
| 1.13, 1.13.1 | Der eingesetzte Grim-Build scheitert beim Start der Command-Anbindung auf diesen Paper-Versionen. |
| 26.3 | Der eingesetzte offizielle Grim-Build unterstützt nur bis 26.2. |

Es wird keine andere Minecraft-Version als Ersatz ausgegeben. Vollständige Buildnummern,
Hashes, Laufzeiten und konkrete Startfehler stehen in [GRIM-LAB.md](GRIM-LAB.md).

## Warum vorher 50/92 angezeigt wurde

Die alte Oberfläche zählte 49 Vanilla-Instanzen plus 43 Paper-Instanzen. Die Zahl
50 bezeichnete vorhandene erfolgreiche Laufzeitberichte für die 49 Originale und
eine Grim-Instanz. Die übrigen neuen Instanzen hatten keinen solchen Weltbericht.
Die 8.251 Einzelprüfungen waren die Summe dieser Berichte, kein Nachweis für alle
92 Welten und kein Nachweis der ViaForge-Bewegungskompatibilität.

Die Anzeige unterscheidet jetzt:

- **Gespeicherte Welten geprüft:** frischer instanzbezogener Bericht aus
  `world-validation.json`, einschließlich tatsächlichem Generator, Exponaten,
  Schildern und zusätzlichem Gelände in allen reservierten Chunks.
- **Laufzeitprüfungen / Einzelprüfungen:** Ergebnisse der Exponat-, Mob-,
  Bedienelement- und Kistenprüfungen am laufenden Server. Ein neuerer Fehler
  überstimmt ältere erfolgreiche Berichte.

Die vollständige Prüfroutine sichert jede gestoppte Welt, startet sie, prüft die
Ausstellung und den tatsächlichen Grim-Zustand, speichert und stoppt den Server
und liest anschließend seine gespeicherten Chunks. Sie baut nichts neu auf.
Jeder Versuch bleibt als `world-validation-<Zeit>.json` erhalten; der Gesamtlauf
steht in `active-world-survey-<Zeit>.json`. Nach einer Snapshot-Rücksicherung
zählen Prüfergebnisse der verdrängten Welt nicht weiter. Web-Wiederherstellungen
führen anschließend die gespeicherte Weltprüfung erneut aus.

```powershell
py -3 tools/test-servers/world_validation.py active --workers 2
py -3 tools/test-servers/profile_web_probe.py
py -3 -m unittest discover -s tools/test-servers -p 'test_*.py'
```

`Gespeicherte Exponate prüfen` führt nur die Prüfung der gestoppten, gespeicherten
Welt aus. Das ersetzt keine neue Laufzeitprüfung oder echte Clientverbindung.
Diese Weltprüfung ist ausdrücklich kein PASS für ViaForge unter Grim.

## Kommandozeile und Archive

`Testserver.bat start 26.2`, `stop all`, `export-list all` und andere normale
Laboraktionen verwenden die aktiven Profile. `grim.py on 26.2` bzw. `off` und
`status` akzeptieren ebenfalls den einfachen Versionsnamen. Im Spiel bleiben
`/labac on`, `/labac off` und `/labac status` unverändert.
Auch die Kommandozeilen von `exhibition.py`, `snapshots.py`, `world_audit.py`,
`terrain_audit.py` und `world_validation.py` lösen einfache Versionsnamen und
`all` über die aktiven Profile auf.

Für bewusste Arbeit an einem Originalarchiv bleibt beispielsweise
`py -3 tools/test-servers/lab.py status instance:26.2` verfügbar.
`instance:vanilla` wählt alle Originale, `instance:grim` alle historischen
Paper-Installationen einschließlich nicht verfügbarer Builds. Die internen
Werkzeuge `lab.select()` und die bisherigen Diagnoseproben behalten ihre
Instanznamen für reproduzierbare historische Berichte. Die Web-API nimmt nur
aktive Versionsnamen an. Favoriten mit altem `-grim`-Suffix werden zugeordnet.

Exportierte Minecraft-Serverlisten enthalten die echte Minecraft-Version als
`viaForge$version`, niemals einen internen Instanznamen mit `-grim`.
Der Import aktualisiert bereits automatisch erzeugte Einträge nur bei passendem
Labornamen und bekannter Laboradresse. Eigene Namen, andere Server und unbekannte
NBT-Tags bleiben erhalten; vor einer Änderung wird `servers.dat` gesichert.

## Beim vollständigen Lauf gefundene Fehler

- In 1.17 und 1.17.1 lagen je 38 natürliche Tropfsteinblöcke zwischen den
  Exponaten. Die zusätzliche Geländeprüfung erkannte sie trotz erfolgreichem
  Exponatabgleich. Die Webaktion **Ausstellung wiederherstellen** entfernte sie
  nach Sicherung; die anschließende gespeicherte Prüfung fand keine Reste.
- Neuere Paper-Konsolen geben mit JLine eingegebene Befehle während der Eingabe
  mehrfach und teilweise wieder aus. Der bisherige Zähler erfasste diese Echos
  als Erfolge. Die Prüfung zählt jetzt nur vollständige, vom Server bestätigte
  Marker und jeden Prüfbefehl höchstens einmal. Eingabe-Echos können keinen
  fehlenden Prüferfolg ersetzen. Regressionstests decken beide Fälle ab.
- Das Froschlaich-Exponat in 26.3 war verschwunden. Der gespeicherte Untergrund
  war Stein; außerdem schlüpft Froschlaich unabhängig von `randomTickSpeed` durch
  einen geplanten Tick. Die Originalimplementierung wurde aus dem tatsächlichen
  26.3-Server-JAR mit `javap` geprüft (`build/logs/frogspawn-original-bytecode.txt`).
  Die betroffenen Versionen bekommen eine kleine geschlossene Wasserstelle und
  einen beschrifteten Knopf zum erneuten Einsetzen. Im Katalog ist der Block als
  **auf Knopfdruck** erkennbar. Nach dem Schlüpfen ist Luft nur dann ein gültiger
  Zustand, wenn Wasser, Behälter, Knopf und gespeicherter Befehl intakt sind.
  Andere fehlende Exponate werden dadurch nicht ausgeblendet.

Die Froschlaich-Anpassung ist in der bestehenden Wiederherstellung enthalten.
Ein gezieltes Upgrade ohne übrige Ausstellungsänderungen ist ausdrücklich mit
`world_validation.py active --workers 2 --install-frogspawn-stations` möglich:
erst Backup, dann nur diese Station ändern, Laufzeitprüfung und gespeicherte
Prüfung. Die betreffenden Chunks werden vor den Befehlen geladen. Der normale
Prüflauf ohne diese Option verändert keine Exponate.

## Abschlussprüfung der aktiven Profile

Am 21.09.2026 wurden alle **49/49** aktiven Welten frisch gestartet, zur Laufzeit
geprüft, gespeichert, gestoppt und aus ihren gespeicherten Daten geprüft:
**7.797 bestätigte Einzelprüfungen**, **6.860 reservierte Chunks**, keine offenen
Exponat-, Schild-, Gelände- oder Generatorabweichungen. Die Web-API bestätigt
49 gespeicherte Weltprüfungen und 49 Laufzeitprüfungen. **70 gespeicherte
Spielerdateien** stimmen bytegenau mit den Sicherungen vor der jeweiligen
Prüfung überein. Der frühere Zähler 8.251
ist wegen anderer Instanzauswahl und der korrigierten Konsolenauswertung nicht
direkt vergleichbar.

Nachweise im lokalen Laufzeitordner:

- `active-profile-final-report.json`: zusammengeführte aktuelle Nachweise mit
  Quellenbericht, Backup, Plattform, Grim-Zustand und Zeitstempel je Version.
- `active-world-survey-1790010929070897300.json`: abgeschlossener Fortsetzungslauf
  mit 40 erfolgreichen Welten; die neun übrigen besitzen vorher abgeschlossene
  aktuelle Einzelberichte bzw. den vollständigen Web-Lebenszyklustest.
- `1.12.2-grim/profile-web-probe.json`: Start, OFF, Status, ON, Stop, Backup,
  Rücksicherung, erneuter Start, Laufzeitprüfung, Stop und gespeicherte Prüfung
  über die einfachen Versionsnamen der Web-API.
- `active-profile-settings-check.json`: 49/49 Profile mit Creative-Standard,
  freiem gespeicherten Spielmodus, Sichtweite 16 und gegebenenfalls Simulation 3.
- `profile-server-list-check.json`: 49 aktive Laboreinträge, zwei eigene
  Servereinträge unverändert, Originaldatei gesichert.
- `build/logs/consolidation-python-tests.log`: **58 Werkzeugtests bestanden**.

Die Browserprüfung bestätigt 49 Versionszeilen, 41/8 in den Verfügbarkeitsfiltern,
die konkrete 26.3-Grenze, deaktivierte Grim-Bedienung dort und Darstellung bei
1280 sowie 390 Pixel Breite. Die Verwaltung bleibt auf 127.0.0.1:8765 erreichbar;
alle für diese Prüfung gestarteten Spielserver sind gespeichert und gestoppt.

Diese Änderung betrifft Laborwerkzeuge und Welten. Die bereits gebaute ViaForge-
Mod-JAR und die Grim-Helper-JAR wurden dabei nicht verändert. Ein neues Mod-Build
ist für die Profilumstellung nicht erforderlich. Vorhandene Bewegungsbefunde und
Grenzen stehen weiterhin in [REGRESSIONS-2026-09-21.md](REGRESSIONS-2026-09-21.md).
