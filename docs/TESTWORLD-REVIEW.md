# Aktueller Nachtrag: freie Spielmodi und 16 Chunks Sichtweite

Auf Wunsch wurde die automatische Adventure-Umschaltung wieder entfernt.
Creative und Survival bleiben auch innerhalb der Ausstellung und an der
Ankunftsplattform erhalten. Die vier dafür verwendeten Befehlsblöcke wurden
entfernt; auch die Wiederherstellung installiert sie nicht erneut.
Neue Spieler starten in Creative, bestehende Spieler behalten ihren gespeicherten
Modus und können über den Creative-Knopf wechseln. Die Server-Sichtweite wurde
von 3 auf 16 Chunks angehoben; die unabhängige Simulationsdistanz bleibt bei 3.
Die Weltwiederherstellung auf der Webseite bleibt verfügbar.

Die folgenden älteren Berichte beschreiben den damaligen Stand mit Adventure-Schutz.
Aktuelle Einstellungsprüfungen stehen pro Version in `server-settings-check.json`,
die aktuellen Login-/Spielmodusprüfungen in `free-play-probe.json`.

# Nachprüfung und Reparatur vom 19.09.2026

Alle 48 Welten wurden erneut gesichert und aktualisiert. Die vorherigen Prüfungen
hatten zwei wichtige Fehlerklassen nicht erfasst: Die Kistenprüfung verglich gegen
dieselben falschen Legacy-Blockmetadaten wie der Generator, und fehlende bzw.
ungetragene Beschriftungsschilder wurden nicht systematisch geprüft.

Die Legacy-Kisten verwenden jetzt die tatsächlichen Creative-Stacks der jeweiligen
Originalclients mit geprüfter SHA-1. Beispiel: Purpurtreppen haben im Inventar nur
Metadatum 0; Holzarten und Farben bleiben als echte Itemvarianten erhalten. Spawn-Eier
und Tränke übernehmen die originalen NBT-Daten. Moderne Kisten verwenden ausschließlich
Einträge der versionsrichtigen Itemregistry; technische Operator-Werkzeuge werden
ausgesondert. Die Auswahl entspricht nicht den Beutetabellen natürlicher Truhen.

Beschriftungen an Knöpfen erhalten tragende Quarzblöcke. Alle bekannten Schilder
wurden erneuert und nach dem Speichern auf Block, Text und Unterlage geprüft.
Der Blockabgleich kennt jetzt auch die tatsächlichen Legacy-Block-IDs und erkennt
dadurch beispielsweise einen durch Stein ersetzten Purpurblock.

Adventure schützt die Ausstellung und Ankunftsplattform automatisch vor normalem
Abbauen und Platzieren. Creative und Survival bleiben an den separaten Teststationen
verfügbar. In den Serverdetails auf der Webseite gibt es **Ausstellung wiederherstellen**:
zuvor den Server stoppen; die Aktion sichert die Welt, repariert Blockexponate und
Schilder, erneuert die Steuerung und füllt die Katalogkisten nach. Spielerinventare
und Bauten außerhalb der reservierten Plätze bleiben erhalten. Alte fehlerhafte Items
in Spielerinventaren müssen daher durch neue Exemplare aus den Kisten ersetzt werden.

Abschließende Ergebnisse:

- 48/48 Welten aktualisiert, gesichert und geprüft.
- 48.875 Blockpositionen und 57.195 Beschriftungsschilder: keine unerwarteten Abweichungen.
- 49.580 ausgestellte Itemstacks über alle Versionen; vollständige Kisten-NBT geprüft.
- 7.573 Prüfungen auf laufenden Originalservern bestanden.
- 34 Python-Werkzeugtests bestanden, keine übersprungenen Tests.
- Echte Offline-Logins auf 1.12.2 und 26.2 bestätigen den Spielmodusschutz und die
  weiterhin benutzbaren Creative-/Survival-Testbereiche, auch nach einem Neustart.
- Ein absichtlich ersetzter Purpurblock, ein entferntes Schild und eine abgebaute
  Katalogkiste wurden auf beiden Versionen erkannt bzw. wiederhergestellt. Ein
  zusätzlich platzierter Block außerhalb der Ausstellung blieb dabei erhalten.
- Eine echte Wiederherstellung über den Webknopf wurde erfolgreich abgeschlossen;
  die mobile Detailansicht hat keinen horizontalen Überlauf und keine JavaScript-Fehler.

Der Gesamtbericht liegt unter `run/test-servers/exhibition-final-report.json`;
versionsbezogene Ergebnisse stehen in `exhibition-maintenance.json`, `world-audit.json`
und `verification.json`. Die beiden Login-/Reparaturtests stehen in `exhibition-probe.json`.
Jede Wiederherstellung hat eine eigene Sicherung unter `backups/exhibition-*/world/`.

Grenzen: Nicht sämtliche Blockeigenschaften oder modernen Item-NBT-Varianten werden
geprüft. Kurzlebige Flüssigkeits-/Frostzustände und normale Legacy-Redstone-Wechsel
sind gesondert behandelt. Der Schutz verhindert keine absichtlichen Operator-Befehle.
Eine visuelle Abnahme der ViaForge-Modelle ist damit nicht erfolgt; die Mod-JAR wurde
bei diesen Änderungen am Testlabor nicht verändert.

# Vorherige Testweltprüfung vom 18.09.2026

Alle 48 lokalen Testwelten wurden geprüft und gezielt überarbeitet. Vor dem
ersten Eingriff wurde jede vollständige Welt unter
`run/test-servers/<Version>/backups/before-refinement-1/world/` gesichert.

## Ergebnisse

- 48.875 katalogisierte Blockpositionen aus gespeicherten Chunkdaten abgeglichen.
- 1.135 fehlende bzw. abweichende Exponate über die 48 Welten korrigiert.
- Keine unerwarteten Abweichungen mehr im abschließenden Blockabgleich.
- 7.404 Live-Prüfungen für Weltproben, sämtliche ausgestellten Mobs, Itemkisten
  und Navigationsbefehle bestanden.
- 27 Werkzeugtests einschließlich Web-Beenden und Chunkdaten-Leser bestanden.

Der Blockabgleich prüft bei Legacy-Versionen das Vorhandensein und bei modernen
Versionen zusätzlich die Block-ID. Er ist kein vollständiger Vergleich aller
Blockzustände, NBT-Varianten oder Clientmodelle. 301 kurzlebige/gleichwertige
Zustände werden separat dokumentiert: insbesondere fließende Legacy-Flüssigkeiten
ohne Quelle und Frost-Eis. Die Live-Prüfungen erfassen zusätzlich die konkreten
Zustände ausgewählter stabiler Blöcke und die vollständigen Katalogkisten.

Die maschinenlesbaren Einzelergebnisse stehen in
`run/test-servers/world-refinement-final.json`, die vollständigen Funde je Version
in `world-audit.json`. Frühere Fehlversuche bleiben in den Protokollen erhalten.

## Behobene Fehler

Wassergefüllte Exponate, insbesondere Korallen und Wandfächer, konnten Wasser in
die Ausstellung abgeben. Dadurch verschwanden benachbarte Pflanzen, Fackeln und
Dekorationen; Betonpulver verwandelte sich in Beton. Die Exponate haben nun
geschlossene Glasbehälter. Ausgelaufenes Wasser im reservierten Blockbereich
wurde entfernt und die betroffenen Exponate wurden wiederhergestellt.

Wandfackeln, Wandbanner, Leitern und ansteigende Schienen erhielten passende
Stützen. Unter Sandunterlagen fehlte teilweise ein tragender Block, wodurch
Kakteen und tote Büsche verschwanden. Pflanzen wie Choruspflanzen, Höhlenranken,
Dripleaf und Kannenpflanzen erhielten geeignete Untergründe bzw. vollständige
Pflanzenteile. Überbaute Weg- und Ackerexponate wurden wieder freigestellt.

Diese Regeln werden auch bei künftig neu erzeugten Testwelten verwendet.
Itemkisten und Mob-Ausstellungen wurden nicht neu aufgebaut.

## Orientierung und Gestaltung

- Farbige Zugänge unterscheiden Block-, Item- und Mob-Etagen.
- Beleuchtete Wegweiser kennzeichnen die Ankunftspunkte.
- Zusätzliche Rückwegknöpfe am Ende jeder Etage führen zur Navigation.
- Die Ankunftsplattform hat eine farbige Einfassung, Beleuchtung und kurze Hinweise.
- Moderne Ausstellungsschilder zeigen zur Ankunftsseite.

Für einen späteren Ausbau bieten sich thematisch gruppierte Ausstellungsinseln,
ein natürlicher Biom- und Tageslichttest sowie identische Vergleichsszenen über
mehrere Versionen an. Eine visuelle Ingame-Abnahme im ViaForge-Client wurde in
diesem Durchlauf nicht vorgenommen; insbesondere Renderfehler der Mod sind
damit nicht ausgeschlossen.

## Bedienung auf der Webseite

Oben rechts öffnet **Anwendung beenden** die Auswahl:

- **Alles speichern & beenden:** laufende Webaufträge abschließen, verwaltete
  Minecraft-Server speichern/stoppen und anschließend den Webdienst beenden.
- **Nur Webverwaltung beenden:** Webdienst beenden, Minecraft-Server weiterlaufen lassen.

In den Serverdetails prüft **Gespeicherte Exponate prüfen** die Blockausstellung
eines beendeten Servers. **Laufende Welt prüfen** prüft den gestarteten Server.
Funde erscheinen in der Übersicht und in den Aktionsergebnissen.
