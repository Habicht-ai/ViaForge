# Testweltprüfung vom 18.09.2026

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
