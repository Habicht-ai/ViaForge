# Grim im lokalen Testlabor

Stand: 21.09.2026. Die Verwaltung zeigt **einen Server pro Minecraft-Version**, derzeit 49, aus `profiles.json`. Davon verwenden 41 eine startgeprüfte Paper/Grim-Installation; acht bleiben mit konkretem Grund ohne Grim verfügbar. Die ursprünglichen 49 Vanilla-Welten und die 43 zuvor installierten Paper-Welten bleiben auf der Platte erhalten. Ihre internen Instanznamen sind für Archive und Prüfnachweise weiter gültig, aber keine zusätzlichen Einträge in der Verwaltung. Siehe [aktive Profile und vollständige Weltprüfung](LAB-PROFILES.md). **Installiert bedeutet nicht bewegungskompatibel.** Die konkrete Startmatrix folgt unten; ViaForge-Befunde stehen in [REGRESSIONS-2026-09-21.md](REGRESSIONS-2026-09-21.md).

## Quellen und Grenzen

- [Offizielles Grim-Repository](https://github.com/GrimAnticheat/Grim), geprüfter Commit `8eb5f2809591c891deb4958bb2927844871e0600`: README nennt Minecraft 1.8–26.2, Java 17+, Bukkit/Spigot/Paper/Folia und Fabric. **26.3 ist nicht unterstützt.** Es gibt keine umbenannte 26.2-Instanz als Ersatz.
- Eingesetzt: offizieller Bukkit-Build **2.3.74-8eb5f28**, Modrinth-Version `Gd6BG1HA`, veröffentlicht 10.09.2026. [Download](https://cdn.modrinth.com/data/LJNGWSvH/versions/Gd6BG1HA/grimac-bukkit-2.3.74-8eb5f28.jar). SHA-256: `91c06e7ae7da53636bc5e500d5af3d36a6180247e155fa5b4340da5a72f9eeb7`. PacketEvents ist im Bukkit-JAR enthalten; es wurde kein zusätzliches PacketEvents installiert.
- [Plattformhinweise](https://github.com/GrimAnticheat/Grim/wiki/Supported-platforms): Fabric ist eine andere Distribution, keine Bukkit-Plugininstallation. Der untersuchte Fabric-Metadatensatz beginnt bei 1.16.1 und begrenzt 26.x auf 26.1.2 bis vor 26.3; Fabric Loader mindestens 0.16. Für die vorhandenen Laborversionen verwendet diese Integration exakte Paper-Builds.
- [Paper Download-Service](https://fill.papermc.io/v3/projects/paper): vollständige URLs, Buildnummern und SHA-256 in [`grim-sources.json`](../tools/test-servers/grim-sources.json) und [`grim-versions.json`](../tools/test-servers/grim-versions.json). Downloads werden vor Verwendung geprüft. Jede Instanz besitzt `installation.json` mit den tatsächlich installierten Hashes.
- Exakte Paper-Downloads fehlen für 1.9, 1.9.1, 1.9.2, 1.11 und 1.16. Zusätzliche offizielle Spigot-BuildTools-Versuche für 1.9, 1.9.2 und 1.11 scheiterten an nicht mehr verfügbaren `bungeecord-chat:1.9-SNAPSHOT` beziehungsweise `1.10-SNAPSHOT` aus Sonatype. Logs: `build/spigot-*/BuildTools.log.txt`. Keine fremden Server-JARs und keine Versionssubstitution.
- Paper 1.13 und 1.13.1 booten, aber der aktuelle Grim-Build scheitert an seiner Cloud-Command-Anbindung: `NoSuchMethodException: VanillaCommandWrapper.getListener(CommandSender)`. Diese beiden Varianten sind ausdrücklich **nicht verfügbar**, nicht bestanden. Ein passender älterer Grim-Build wurde nicht als kompatibel nachgewiesen.

## Java und alte Paper-Versionen

Vanilla behält seine bisherigen Laufzeiten. Grim verwendet Java 17 bei alten Servern, Java 21 ab 1.20.6 und Java 25 für 26.x. `runtime_java` im Manifest und `run/test-servers/java.json` bestimmen die tatsächliche JVM. Java 17 wurde als Azul Zulu 17.0.20.1 installiert; offizieller ZIP-SHA-256 `96ab2160b55d59511ca4b5883d241ada5358847aff7c679f324035589af36b0d`, Quelle und Metadaten in `runtime/java-17-source.json`.

Alte Paperclip-JARs verweisen teils auf verschwundene S3-Adressen. `paper_boot.py` lädt dieselben Mojang-Bytes vom inhaltsadressierten offiziellen CDN, prüft Paperclips `originalHash` und startet bei alten URLClassLoader-Annahmen dessen unveränderte Ausgabe nach Prüfung von `patchedHash` direkt. Für die starre Java-Obergrenze in älteren Paper-Versionen gibt es den separaten, eng begrenzten Startagenten `ViaForgeLab-LegacyPaperBoot-1.0.0.jar`: nur der nach SHA-256 und Bytecodeform geprüfte Java-Versionsvergleich in CraftBukkit Main wird auf Java 17 angepasst. Der Agent verweigert unbekannte Klassenformen und andere Laufzeiten. Originaldownloads bleiben unverändert. `paper-boot-adapter.json` protokolliert jeden Eingriff. Das ist eine lokale Startanpassung, keine Behauptung offizieller Java-17-Unterstützung dieser historischen Paper-Builds.

## Benutzung und tatsächliche Umschaltung

`Testserver.vbs` öffnet die Verwaltung auf `http://127.0.0.1:8765`. Die gewünschte Minecraft-Version wählen, zum Beispiel **26.2**, dann in den Details **Grim an** oder **Grim aus**. Beide Zustände verwenden dieselbe Welt und denselben Spielport. Details zeigen Plattform/Build, tatsächliche Minecraft-Version, Grim-Version, Zustand und erkannte Spieler. Spielserver, RCON und Verwaltung binden nur `127.0.0.1`; Offline-Accounts bleiben möglich. Ports wurden auf freie lokale Bindung geprüft und im Manifest festgehalten. Grim aus bedeutet weiterhin Paper mit abgeschalteten Prüfungen, kein Wechsel der Serverplattform.

| Laborbefehl | Wirkung |
|---|---|
| `/labac on` | Aktiviert Grim für bereits verbundene und neue Spieler. |
| `/labac off` | Setzt ausschließlich während OFF `grim.disabled`; Grim verfolgt die Verbindung weiter, führt aber seine damit geschützten Checks, Prediction und Eingriffe nicht aus. |
| `/labac status` | Zeigt angeforderten und tatsächlich beobachteten Zustand sowie Spielerdaten. |

Konsole/RCON verwenden dieselben Befehle ohne `/`; die Webbuttons rufen sie über RCON auf. Standard ist ON. Die Auswahl wird vor ihrer Bestätigung atomar in `plugins/ViaForgeLabAC/config.yml` gespeichert. Kein Plugin-Unload/Reload. `pending` bedeutet, dass die Beobachtung der Grim-Netzwerkthreads noch aussteht; fehlender/frischerloser Status wird niemals als ON ausgegeben. Eine vor mehr als acht Sekunden geschriebene Statusdatei reicht bei laufendem Server nicht aus.

Die Erweiterung gewährt `grim.verbose`, außerdem nur in OFF `grim.disabled`. `labac.control` und `labac.status` gelten für alle lokalen Accounts. Keine OP-Erteilung, keine Wildcards, keine `grim.exempt`, `grim.nosetback` oder `grim.nomodifypacket`. Im eingeschalteten Zustand muss Grim für jeden Spieler ohne diese Ausnahmen bestätigt sein.

## Echtes automatisches Verbose

[Offizielle Permissions](https://github.com/GrimAnticheat/Grim/wiki/Permissions), [Commands](https://github.com/GrimAnticheat/Grim/wiki/Commands) und [API](https://github.com/GrimAnticheat/Grim/wiki/Developer-API) wurden gegen die Quellen des eingesetzten Builds geprüft. `/grim alerts` mit `grim.alerts` ist die normale Meldungsumschaltung. `/grim verbose` mit `grim.verbose` liefert jedes Flag unterhalb des Alert-Puffers. `/grim profile <player>` braucht `grim.profile`; Debug-/Reload-/Upload-Rechte erhalten Testaccounts nicht zusätzlich.

`ViaForgeLabAC` benutzt `setVerboseEnabled(user, true, true)`, beim Login und danach einmal pro Sekunde. Das setzt den echten Verbose-Zustand auch für vorhandene Accounts nach Reconnect/Neustart; gespeicherte frühere Toggles werden übersteuert. Die API aktiviert dabei gemäß Grim-Semantik auch Alerts. Empfangenes Chat-Verbose wurde mit zwei verschiedenen Nicht-OP-Accounts und absichtlich ausgelöstem `BadPacketsF` nachgewiesen. Ein leerer Chat wird nicht als Prüferfolg gewertet.

Die Webseite zeigt zusätzlich `events.jsonl`: beobachtete Flags mit Check/Verbose, Setbacks, Teleports und Prediction-Zähler. Das ersetzt den getesteten Ingame-Verbose-Empfang nicht. Die API-Beobachter übernehmen vorhandene Cancellation-Werte unverändert. Optionales `trace-packets` zeichnet höchstens 1.200 relevante Pakete je Verbindung auf; Standard ist false.

## Messregeln und Welten

Grims Checks, Schwellen, Punishments und Originalkonfiguration bleiben unverändert. Insbesondere wurde im eingesetzten JAR und in den gestarteten Instanzen `exploit.allow-sprint-jumping-when-using-elytra: true` geprüft und beibehalten. Der Schalter ist Grims Standard, keine Freigabe beliebiger abweichender Flugphysik.

ViaForge übersetzt auf dem Client. Grim erkennt beim tatsächlichen Test **1.12.2 / 340** beziehungsweise **26.2 / 776**, Marke `fml,forge`. Der native Vergleich meldet `vanilla` / 776. Es wird kein natives 1.8-Bewegungsmodell erzwungen. Creative bleibt für neue Spieler verfügbar; die Bewegungsfolgen erfolgen ausdrücklich auch in Survival und mit bestätigter aktiver Prediction.

`world-origin.json` dokumentiert jede Kopie eines gestoppten Originals. Paper öffnet ausschließlich die eigene Kopie und darf nur diese konvertieren. Kein gleichzeitiges Öffnen einer Welt durch zwei Server. Manuelle Sicherung und Rücksicherung funktionieren für gestoppte Instanzen einschließlich Paper-Nebenwelten, Inventaren, Pluginzustand und Servereinstellungen. Snapshots haben vollständige SHA-256-Manifeste; vor Rücksicherung entsteht eine zusätzliche Sicherung. Manipulierte, zusätzliche ungeprüfte Dateien und fremde Instanz-IDs werden abgelehnt. Alte Zustände bleiben erhalten.

**Ausstellung wiederherstellen** erfordert weiterhin einen gestoppten Server und legt zuerst ein Backup an. Die Geländeprüfung betrachtet den tatsächlich gespeicherten Generator und alle 140 reservierten Chunks, inklusive der Luftsäule bis y=319 beziehungsweise der vorhandenen Bauhöhe. Sie meldet zusätzliches natürliches Material und fehlende Chunks; bekannte Exponate, Pflanzen, Stützen, Behälter, Schilder und Mobbereiche sind geschützt. Flüssigkeitsbarrieren entstehen innerhalb der Grenze. Außenbauten bleiben erhalten. Die vier alten Gamemode-Befehlsblöcke x=1..4, y=60, z=1 bleiben Luft; Creative-Standard, `force-gamemode=false`, Sichtweite 16 und separate Simulationsdistanz 3 bleiben bestehen.

## Reproduzieren

```powershell
py -3 tools/test-servers/grim.py build-bridge
py -3 tools/test-servers/paper_boot.py
py -3 tools/test-servers/grim.py install grim
py -3 tools/test-servers/grim_startup_probe.py grim
py -3 tools/test-servers/grim_probe.py
py -3 tools/test-servers/movement_probe.py 1.12.2-grim,26.2-grim
py -3 tools/test-servers/terrain_audit.py all
py -3 tools/test-servers/terrain_repair_probe.py
```

Installation verlangt gestoppte Zielinstanzen; eine erstmalige Weltkopie zusätzlich ein gestopptes Original. Ein reines Helper-Update erfolgt mit `grim.py update-bridge grim`, mit Sicherung des vorherigen JARs und Konfigurationsstands. Bewegungs- und Reparaturproben verändern nur ausdrücklich isolierte, leere Varianten, sichern vorher und stellen danach deren Ausgangsstand wieder her. Native Vergleichsprobe: `native_probe.py` für den vorhandenen originalen 26.2-Client, mit Eingabe-/Positionsagent ohne Änderung von Physik oder Netzwerklogik. Das ist ein instrumentierter Originalclient, keine manuelle Spielsitzung.

## Konkrete Matrix

Startnachweise bestätigen Grim/API/Standardkonfiguration, nicht sämtliche Spielabläufe. Einträge ohne Variante behalten ihren originalen Vanilla-Server.

| Minecraft | Plattform | Java | Spiel / RCON | Nachweis |
|---|---|---|---|---|
| 1.9 | Vanilla-Referenz | 8 | 25590 / 26590 | Kein exakter verifizierter Plattform-Build installiert |
| 1.9.1 | Vanilla-Referenz | 8 | 25591 / 26591 | Kein exakter verifizierter Plattform-Build installiert |
| 1.9.2 | Vanilla-Referenz | 8 | 25592 / 26592 | Kein exakter verifizierter Plattform-Build installiert |
| 1.9.4 | Paper 775 | 17 | 27000 / 27001 | Grim/API gestartet |
| 1.10.2 | Paper 918 | 17 | 27002 / 27003 | Grim/API gestartet |
| 1.11 | Vanilla-Referenz | 8 | 25595 / 26595 | Kein exakter verifizierter Plattform-Build installiert |
| 1.11.2 | Paper 1106 | 17 | 27004 / 27005 | Grim/API gestartet |
| 1.12 | Paper 1169 | 17 | 27006 / 27007 | Grim/API gestartet |
| 1.12.1 | Paper 1204 | 17 | 27008 / 27009 | Grim/API gestartet |
| 1.12.2 | Paper 1620 | 17 | 27010 / 27011 | Zusätzlich echte Verbindungen; Befunde separat |
| 1.13 | Paper 173 | 17 | 27012 / 27013 | Grim-Start fehlgeschlagen |
| 1.13.1 | Paper 386 | 17 | 27014 / 27015 | Grim-Start fehlgeschlagen |
| 1.13.2 | Paper 657 | 17 | 27016 / 27017 | Grim/API gestartet |
| 1.14 | Paper 17 | 17 | 27018 / 27019 | Grim/API gestartet |
| 1.14.1 | Paper 50 | 17 | 27020 / 27021 | Grim/API gestartet |
| 1.14.2 | Paper 107 | 17 | 27022 / 27023 | Grim/API gestartet |
| 1.14.3 | Paper 134 | 17 | 27024 / 27025 | Grim/API gestartet |
| 1.14.4 | Paper 245 | 17 | 27026 / 27027 | Grim/API gestartet |
| 1.15 | Paper 21 | 17 | 27028 / 27029 | Grim/API gestartet |
| 1.15.1 | Paper 62 | 17 | 27030 / 27031 | Grim/API gestartet |
| 1.15.2 | Paper 393 | 17 | 27032 / 27033 | Grim/API gestartet |
| 1.16 | Vanilla-Referenz | 8 | 25611 / 26611 | Kein exakter verifizierter Plattform-Build installiert |
| 1.16.1 | Paper 138 | 17 | 27034 / 27035 | Grim/API gestartet |
| 1.16.2 | Paper 189 | 17 | 27086 / 27087 | Grim/API gestartet |
| 1.16.3 | Paper 253 | 17 | 27038 / 27039 | Grim/API gestartet |
| 1.16.5 | Paper 794 | 17 | 27040 / 27041 | Grim/API gestartet |
| 1.17 | Paper 79 | 17 | 27042 / 27043 | Grim/API gestartet |
| 1.17.1 | Paper 411 | 17 | 27044 / 27045 | Grim/API gestartet |
| 1.18.1 | Paper 216 | 17 | 27046 / 27047 | Grim/API gestartet |
| 1.18.2 | Paper 388 | 17 | 27048 / 27049 | Grim/API gestartet |
| 1.19 | Paper 81 | 17 | 27050 / 27051 | Grim/API gestartet |
| 1.19.2 | Paper 307 | 17 | 27052 / 27053 | Grim/API gestartet |
| 1.19.3 | Paper 448 | 17 | 27054 / 27055 | Grim/API gestartet |
| 1.19.4 | Paper 550 | 17 | 27056 / 27057 | Grim/API gestartet |
| 1.20.1 | Paper 196 | 17 | 27058 / 27059 | Grim/API gestartet |
| 1.20.2 | Paper 318 | 17 | 27060 / 27061 | Grim/API gestartet |
| 1.20.4 | Paper 499 | 17 | 27062 / 27063 | Grim/API gestartet |
| 1.20.6 | Paper 151 | 21 | 27064 / 27065 | Grim/API gestartet |
| 1.21.1 | Paper 133 | 21 | 27066 / 27067 | Grim/API gestartet |
| 1.21.3 | Paper 83 | 21 | 27068 / 27069 | Grim/API gestartet |
| 1.21.4 | Paper 232 | 21 | 27070 / 27071 | Grim/API gestartet |
| 1.21.5 | Paper 114 | 21 | 27072 / 27073 | Grim/API gestartet |
| 1.21.6 | Paper 48 | 21 | 27074 / 27075 | Grim/API gestartet |
| 1.21.8 | Paper 60 | 21 | 27076 / 27077 | Grim/API gestartet |
| 1.21.10 | Paper 130 | 21 | 27078 / 27079 | Grim/API gestartet |
| 1.21.11 | Paper 132 | 21 | 27080 / 27081 | Grim/API gestartet |
| 26.1.2 | Paper 74 | 25 | 27082 / 27083 | Grim/API gestartet |
| 26.2 | Paper 126 | 25 | 27084 / 27085 | Zusätzlich echte Verbindungen; Befunde separat |
| 26.3 | Vanilla-Referenz | 25 | 25638 / 26638 | Grim erklärt keinen 26.3-Support |
