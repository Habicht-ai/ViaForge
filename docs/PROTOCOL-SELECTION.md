# Versionsauswahl und Verbindungsprotokoll

## Behobene Ursachen

- `VersionTracker` teilte eine ungeschützte, nur nach IP adressierte Map zwischen
  Serverlisten-Pings und Logins. Verbindungen zur gleichen IP konnten ihre
  Versionen gegenseitig überschreiben oder entfernen, auch bei verschiedenen Ports.
- Jeder Ping schrieb außerdem die globale Auswahl um. Das Schließen beliebiger
  Kanäle setzte sie aus der Konfiguration zurück. Ein noch laufender Login konnte
  dadurch eine andere als die ausgewählte Version lesen.
- Der gemeinsam verwendete `OldServerPinger` hielt den aktuellen Server in einem
  Instanzfeld, obwohl mehrere Ping-Threads gleichzeitig darauf zugreifen.
- Das Menü für einzelne Server zeigte die globale Auswahl statt des gespeicherten
  Serverprotokolls. Eine vorhandene Servervorgabe konnte nicht auf Vererbung
  zurückgesetzt werden. Die F3-Anzeige las ebenfalls nur die globale Auswahl.
- Ein frischer Belastungslauf reproduzierte zusätzlich `NullPointerException:
  group`: Die native 1.8-Initialisierung der Netzwerk-Threads veröffentlicht ihren
  „geladen“-Zustand zu früh. Der Netzwerk-Fabrikaufruf synchronisiert diesen
  einmaligen Zugriff jetzt auf dem jeweiligen `LazyLoadBase`.

## Verhalten nach der Änderung

Die globale Auswahl wird einmal beim Verlassen des Menüs gespeichert. Eine
gespeicherte Servervorgabe hat Vorrang. Das Servermenü zeigt diese Vorgabe und
entfernt sie mit **Use global**; es speichert dabei keine Kopie des aktuellen
globalen Werts. Beim Öffnen scrollt die Liste zur aktuellen Auswahl. Eine
Größenänderung erhält die bisherige Scrollposition.

Beide `GuiConnecting`-Konstruktoren erfassen die Auswahl vor dem Start des
Verbindungs-Threads. Ein expliziter Host-Aufruf ohne Serverdaten verwendet die
globale Version, auch wenn noch Daten einer früheren Verbindung vorhanden sind.
Ein begrenzter ThreadLocal-Kontext übergibt das Protokoll synchron an den
`NetworkManager`, bevor Netty seine Pipeline anlegt. Jeder Manager behält seinen
eigenen Wert. Verschachtelte und fehlgeschlagene Fabrikaufrufe räumen den Kontext
in `finally` auf. Pings verwenden ihr eigenes Methodenargument. Keine dieser
Operationen schreibt die globale Auswahl oder reagiert darauf, welche Version
ein Server in seiner Statusantwort bewirbt.

F3 zeigt die laufende Verbindung, einschließlich nativem Protokoll 47. Die
optionale Patcher-Anbindung verwendet bei einer laufenden Verbindung ebenfalls
deren Protokoll. Die ursprüngliche Konfigurationsoption für die F3-Anzeige bleibt
wirksam.

## Reproduzierbare Prüfungen

`VersionTrackerTest` prüft Vorrang/Vererbung, zwölf gleichzeitig offene
Verbindungskontexte sowie verschachtelte Fehler und das Aufräumen auf
wiederverwendeten Threads. `ProtocolSelectorSmokeTest` läuft im echten Forge-
Client und prüft Auswahlcallbacks, verzögertes Speichern, Scrollen, Hervorhebung,
Rücksetzen sowie Speichern und Kopieren der Serverdaten.
Zusätzlich durchlaufen zwei aufgestaute Maus-Klicks mit abweichenden alten
Zeigerkoordinaten den echten LWJGL-Ereignisleser und die native GuiSlot-Treffprüfung
bei gescrollter Liste. Jeder Klick muss genau die angeklickte Zeile einmal auswählen.

`py -3 tools/test-servers/protocol_selection_probe.py` erstellt eine zusätzliche,
isolierte Paper-1.12.2-Instanz (Build 1620) mit ViaVersion 5.12.0. Eine neue flache
Welt und neue Nicht-OP-Accounts sind ausschließlich diesem Test zugeordnet.
Creative bleibt verfügbar; bestehende Laborwelten werden nicht geöffnet.
Spielserver, RCON und der aufzeichnende TCP-Proxy binden nur an `127.0.0.1`.

Der Test verbindet einen echten Forge-Client siebenmal mit demselben
Mehrversionsserver. Vor jedem Login laufen 24 Pings desselben `OldServerPinger`
mit vier verschiedenen Zielprotokollen. Er erfasst globale Auswahl,
Servervorgabe, tatsächliches Handshake-Protokoll, vollständigen Weltbeitritt,
F3-Anzeige und die serverseitige `viaversion list`. Direkt nach dem Start des
Verbindungsauftrags werden globale Auswahl und Serverdaten absichtlich geändert;
die bereits gestartete Verbindung muss ihre ursprüngliche Auswahl behalten.

Paper-Download und eingebettete Ausgabe werden gegen die vorhandenen
Prüfsummen geprüft. ViaVersion stammt vom
[offiziellen Release 5.12.0](https://github.com/ViaVersion/ViaVersion/releases/tag/5.12.0):
`ViaVersion-5.12.0.jar`, SHA-256
`72c40a6a702d67f226fc9a0d8ad82aba1483fdabe2e6159bcdddb2dc070750b0`.
Quellen, Java-Pfad, Ports, Konsolenlogs und Einzelbefunde stehen pro Lauf unter
`run/test-servers/protocol-selection-<Zeitstempel>/`. Die Clientkonfiguration wird
vorher gesichert und nach normalem Clientende wiederhergestellt. Der Testserver
wird gespeichert und gestoppt, sein Testverzeichnis bleibt erhalten.

Diese Prüfung betrifft die Versionsauswahl und echte Verbindungen. Sie ist kein
Nachweis vollständiger Bewegungs-, Inhalts- oder Anticheat-Kompatibilität aller
Protokolle und Serverplattformen.

## Tatsächlich geprüfter Lauf vom 21.09.2026

Frischer PASS: `run/test-servers/protocol-selection-1790002522133298200/report.json`.
Sieben vollständige Beitritte mit frischen Nicht-OP-Accounts, jeweils Creative,
mindestens 40 Spielerticks und 24 erfolgreich abgeschlossene gleichzeitige Pings:

| Globale Auswahl beim Start | Servervorgabe | Handshake / serverseitig erkannt | Ergebnis |
| --- | --- | --- | --- |
| 1.12.2 | geerbt | 340 / 1.12.2 | PASS |
| 1.13 | geerbt | 393 / 1.13 | PASS |
| 1.13 | 1.12.2 | 340 / 1.12.2 | PASS |
| 1.12.2 | 1.13 | 393 / 1.13 | PASS |
| 1.12.2 | 1.20/1.20.1 | 763 / 1.20–1.20.1 | PASS |
| 1.13 | 26.3 | 777 / 26.3 | PASS |
| 1.12.2, direkter Host-Aufruf | veraltete Serverdaten absichtlich vorhanden | 340 / 1.12.2 | PASS |

Nach jedem Verbindungsauftrag wurde die globale Auswahl auf 1.8 (47) geändert,
bevor der Login fertig war. Trotzdem blieb die gewünschte Verbindungsversion
erhalten; ebenso unverändert blieb danach die neue globale Auswahl trotz aller
Ping-Abschlüsse und Verbindungsabbrüche. F3 zeigte jeweils das Verbindungsprotokoll.
Der TCP-Mitschnitt enthält exakt 42 Status-Handshakes je Protokoll 340/393/763/777
(168 insgesamt) und die sieben erwarteten Login-Handshakes. Die serverseitigen
Versionsangaben wurden zusätzlich automatisch auf Übereinstimmung geprüft.

Der separate Server nutzte Paper 1.12.2 Build 1620 mit ViaVersion 5.12.0,
keinen Grim-Build. Eine Verbindung mit Protokoll 777 zu diesem Mehrversionsserver
ist ausdrücklich keine Aussage über Grim auf einem nativen 26.3-Server.

Zusätzliche frische Abschlussprüfungen:

- 118 JUnit-Tests, keine Fehler/übersprungenen Tests; Ergebnisse unter
  `build/test-results/test/`, inklusive drei neuer `VersionTrackerTest`-Fälle.
- 49 Python-Tests: `build/logs/protocol-selection-python-tests.log`.
- Vollständiger Forge-Smoke über alle 49 Ressourcenprofile, Bericht beginnt mit
  `PASS`: `build/logs/protocol-selection-forge-smoke.txt`. Menüprüfung einschließlich
  echter LWJGL-Ereignisverarbeitung ebenfalls PASS.
- `build.bat build runClient -x preRunClient` erfolgreich, frischer Bericht vom
  Build geprüft: `build/logs/protocol-selection-release-full.log`.
- Release enthält 7.967 Java-8-kompatible Klassen und die erforderlichen
  Forge-/Mixin-Metadaten; Entwicklungskonten und Testtreiber bleiben ausgeschlossen.

Gebaut: `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, 14.394.062 Bytes,
SHA-256 `c80a0c8f2eaebcd18a82cb9ad2b745d7f8beecf3f39d59e2264eba0901db53ed`.
Die Entwicklungsversion in `run/mods/ViaForge-development.jar` ist ebenfalls
aktualisiert. Vorherige Release-JAR und Konfiguration wurden unter
`build/backups/protocol-selection-20260921-163944/` gesichert.

Für diesen Lauf wurde `JAVA_TOOL_OPTIONS=-XX:ActiveProcessorCount=4` gesetzt,
nachdem der Java-Downgrader einmal mit einem parallelen internen HashMap-Fehler
abgebrochen war. Der erfolgreiche Build und die Laufzeitprüfungen sind frische
Ergebnisse; fehlgeschlagene Testläufe bleiben separat nachvollziehbar erhalten.
