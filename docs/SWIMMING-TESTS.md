# Schwimmtests vom 22./23.09.2026

Alle aufgeführten Bewegungsfälle liefen mit einem echten Client, neuem Nicht-OP,
Survival, LabAC ON, echtem Verbose und ohne Bypassrechte. Grim ist in allen Läufen
`2.3.74-8eb5f28`. ViaForge ist ein Forge-1.8.9-Client (11.15.1.2318); die von
Grim erkannten Zielversionen stehen unten. Der Originalclient ist Mojang 26.2
mit reiner Eingabe-/Beobachtungsinstrumentierung.

**PASS bezieht sich auf die aufgeführten Survival-Fälle einschließlich sämtlicher
Reset- und Ausrüstungsphasen. Login-Ereignisse sind separat ausgewiesen.**
Es ist kein pauschaler Nachweis für jede Serverversion oder jeden Block.

| Client / Ziel | Paper-Build | Grim erkennt | Fälle | Survival Flags / Setbacks | Login Flags / Setbacks | Ergebnis |
|---|---:|---|---:|---:|---:|---|
| 1.13.2 | 657 | 1.13.2 / 404 | 23/23 | 0 / 0 | 0 / 0 | PASS |
| 1.14.4 | 245 | 1.14.4 / 498 | 23/23 | 0 / 0 | 0 / 2 | PASS |
| 1.16.5 | 794 | 1.16.4 / 754 | 23/23 | 0 / 0 | 0 / 2 | PASS |
| 1.17.1 | 411 | 1.17.1 / 756 | 23/23 | 0 / 0 | 0 / 2 | PASS |
| 1.21.5 | 114 | 1.21.5 / 770 | 23/23 | 0 / 0 | 0 / 3 | PASS |
| 26.2 ViaForge | 126 | 26.2 / 776 | 28/28 | 0 / 0 | 0 / 2 | PASS |
| 26.2 Original | 126 | 26.2 / 776 | 28/28 | 0 / 0 | 0 / 0 | PASS |

Grim benennt Protokoll 754 als 1.16.4; der gestartete Server ist exakt
Paper 1.16.5. Die gemeinsame Protokollnummer ist keine Versionssubstitution.

1.18.2 / Paper 388 / Protokoll 758: **BLOCKIERT beim Login** durch Grims
Forge-Sperre, kein Bewegungs-PASS. Rohbericht:
[1.18.2](../run/test-servers/swim-1.18.2-matrix2-1790109432657314100/report.json).
1.13, 1.13.1, 1.16 und 26.3 haben weiterhin keinen exakt passenden nutzbaren
Grim-Aufbau. Weitere Zielversionen sind keine live geprüften Einträge dieser Matrix.

## Fälle

Startbedingungen: Wasserbecken mit Boden Y=60, Wasser Y=61…66, im Regelfall
Start (0,5; 63; 0,5), Blick nach Süden. Vor jedem unabhängigen Fall Teleport
und zwei Sekunden Ruhe; anschließende Ausrollfälle behalten den Zustand.
Blasensäulen besitzen eine gesicherte tiefere Teststrecke. Tiefenläufer III,
Delfin- und Aquisatoreffekt werden am tatsächlichen Clientzustand bestätigt.
Die fünf zusätzlichen Übergangsfälle auf 26.2 verwenden einen ein Block hohen
Unterwassergang sowie einen ausreichend langen Landstreifen. Exakte Befehle,
Dauer, Eingaben und Koordinaten stehen in jedem Rohbericht.

| Fall | 1.13.2 | 1.14.4 | 1.16.5 | 1.17.1 | 1.21.5 | 26.2 ViaForge | 26.2 Original |
|---|---:|---:|---:|---:|---:|---:|---:|
| Underwater idle | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Forward without sprint | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Sprint swim level | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Coast after Sprint swim level | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Swim up | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Coast after Swim up | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Swim down | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Coast after Swim down | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Ascend key | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Descend key | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Sprint diagonal | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Coast after Sprint diagonal | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Dive while swimming | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Rise while swimming | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Reverse underwater | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Establish swimming | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Dive during established swim | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Depth strider swimming | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Dolphins grace | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Conduit breathing | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Bubble lift | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Bubble sink | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Bubble sink edge | PASS | PASS | PASS | PASS | PASS | PASS | PASS |
| Swim into low passage | — | — | — | — | — | PASS | PASS |
| Rest inside low passage | — | — | — | — | — | PASS | PASS |
| Swim out of low passage | — | — | — | — | — | PASS | PASS |
| Swim against shore | — | — | — | — | — | PASS | PASS |
| Rest on shore | — | — | — | — | — | PASS | PASS |

## 1.13.2

[Rohbericht](../run/test-servers/swim-1.13.2-release-matrix-1790107825494648600/report.json) · Account `VFpush404876909` · Brand `fml,forge`.

Berichts-SHA-256: `711b903b2b61878a0ccf90828d8c05f5a28b0be65a4c13aca4df173693afed7e`.

Clientartefakt / SHA-256: `C:\Users\Panthera\Desktop\GitHub\ViaVersionClient\run\mods\ViaForge-development.jar` / `0bba51875f29de120837b79b10b796d81abc8a70c003b39566aa3f7747c254c3`.

| Fall | Eingabe | Sekunden | Clientticks / Schwimmpose | Predictions | Max. Abweichung | Flags / Setbacks |
|---|---|---:|---:|---:|---:|---:|
| Underwater idle | idle | 3.00 | 59 / 0 | 22 | 0 | 0 / 0 |
| Forward without sprint | forward | 3.00 | 60 / 0 | 60 | 3.83037e-15 | 0 / 0 |
| Sprint swim level | forward-sprint | 5.00 | 100 / 99 | 100 | 4.94049246e-15 | 0 / 0 |
| Coast after Sprint swim level | idle | 4.00 | 80 / 1 | 28 | 5.36715433e-15 | 0 / 0 |
| Swim up | forward-sprint | 3.00 | 60 / 59 | 60 | 6.45323102e-15 | 0 / 0 |
| Coast after Swim up | idle | 4.00 | 80 / 1 | 45 | 6.04056222e-15 | 0 / 0 |
| Swim down | forward-sprint | 3.00 | 60 / 59 | 60 | 3.38643616e-15 | 0 / 0 |
| Coast after Swim down | idle | 4.00 | 80 / 80 | 25 | 7.77156117e-16 | 0 / 0 |
| Ascend key | jump | 3.00 | 60 / 0 | 54 | 0 | 0 / 0 |
| Descend key | sneak | 3.00 | 60 / 0 | 11 | 0 | 0 / 0 |
| Sprint diagonal | forward-right-sprint | 4.00 | 80 / 79 | 80 | 2.43015876e-15 | 0 / 0 |
| Coast after Sprint diagonal | idle | 4.00 | 80 / 1 | 21 | 3.81302322e-15 | 0 / 0 |
| Dive while swimming | forward-sprint-sneak | 3.00 | 60 / 0 | 34 | 0 | 0 / 0 |
| Rise while swimming | forward-sprint-jump | 3.00 | 60 / 38 | 59 | 7.0746779e-15 | 0 / 0 |
| Reverse underwater | back | 3.00 | 60 / 0 | 60 | 3.58912568e-15 | 0 / 0 |
| Establish swimming | forward-sprint | 1.50 | 30 / 29 | 30 | 4.98041043e-15 | 0 / 0 |
| Dive during established swim | forward-sprint-sneak | 3.00 | 60 / 60 | 60 | 3.47734829e-15 | 0 / 0 |
| Depth strider swimming | forward-sprint | 3.00 | 60 / 59 | 60 | 4.36291783e-15 | 0 / 0 |
| Dolphins grace | forward-sprint | 3.00 | 60 / 59 | 60 | 4.26791616e-15 | 0 / 0 |
| Conduit breathing | idle | 3.00 | 60 / 0 | 23 | 0 | 0 / 0 |
| Bubble lift | idle | 3.00 | 60 / 0 | 59 | 8.68749517e-15 | 0 / 0 |
| Bubble sink | idle | 3.00 | 60 / 0 | 60 | 0 | 0 / 0 |
| Bubble sink edge | idle | 3.00 | 61 / 0 | 58 | 0 | 0 / 0 |

## 1.14.4

[Rohbericht](../run/test-servers/swim-1.14.4-matrix2-1790108849983238900/report.json) · Account `VFpush498927312` · Brand `fml,forge`.

Berichts-SHA-256: `d1ce4795811782b8e1d3ab0d10e6bea3502b7ca040537968cd090fee5fab83a5`.

Clientartefakt / SHA-256: `C:\Users\Panthera\Desktop\GitHub\ViaVersionClient\run\mods\ViaForge-development.jar` / `0361878435cca6df7a16fb8ee885b92cb1a95528a5a4cb66a258fe1e87faaf45`.

| Fall | Eingabe | Sekunden | Clientticks / Schwimmpose | Predictions | Max. Abweichung | Flags / Setbacks |
|---|---|---:|---:|---:|---:|---:|
| Underwater idle | idle | 3.00 | 59 / 0 | 23 | 0 | 0 / 0 |
| Forward without sprint | forward | 3.00 | 59 / 0 | 59 | 3.83670011e-15 | 0 / 0 |
| Sprint swim level | forward-sprint | 5.00 | 100 / 99 | 100 | 2.30734917e-15 | 0 / 0 |
| Coast after Sprint swim level | idle | 4.00 | 79 / 1 | 28 | 3.38192784e-15 | 0 / 0 |
| Swim up | forward-sprint | 3.00 | 60 / 59 | 60 | 6.3436672e-15 | 0 / 0 |
| Coast after Swim up | idle | 4.00 | 80 / 1 | 45 | 6.06328415e-15 | 0 / 0 |
| Swim down | forward-sprint | 3.00 | 60 / 59 | 60 | 2.50354677e-15 | 0 / 0 |
| Coast after Swim down | idle | 4.00 | 80 / 80 | 25 | 1.41553436e-15 | 0 / 0 |
| Ascend key | jump | 3.00 | 60 / 0 | 54 | 5.19029264e-15 | 0 / 0 |
| Descend key | sneak | 3.00 | 60 / 0 | 12 | 0 | 0 / 0 |
| Sprint diagonal | forward-right-sprint | 4.00 | 80 / 79 | 80 | 4.03659353e-09 | 0 / 0 |
| Coast after Sprint diagonal | idle | 4.00 | 80 / 1 | 26 | 3.63310073e-09 | 0 / 0 |
| Dive while swimming | forward-sprint-sneak | 3.00 | 60 / 59 | 60 | 0 | 0 / 0 |
| Rise while swimming | forward-sprint-jump | 3.00 | 60 / 37 | 59 | 2.52086679e-09 | 0 / 0 |
| Reverse underwater | back | 3.00 | 60 / 0 | 60 | 4.06272238e-15 | 0 / 0 |
| Establish swimming | forward-sprint | 1.50 | 30 / 29 | 30 | 2.30755783e-15 | 0 / 0 |
| Dive during established swim | forward-sprint-sneak | 3.00 | 60 / 60 | 60 | 3.48055914e-15 | 0 / 0 |
| Depth strider swimming | forward-sprint | 3.00 | 60 / 59 | 60 | 3.01084064e-15 | 0 / 0 |
| Dolphins grace | forward-sprint | 3.00 | 60 / 59 | 60 | 4.32671612e-15 | 0 / 0 |
| Conduit breathing | idle | 3.00 | 60 / 0 | 23 | 0 | 0 / 0 |
| Bubble lift | idle | 3.00 | 60 / 0 | 59 | 8.68749517e-15 | 0 / 0 |
| Bubble sink | idle | 3.00 | 60 / 0 | 60 | 0 | 0 / 0 |
| Bubble sink edge | idle | 3.00 | 60 / 0 | 58 | 0 | 0 / 0 |

## 1.16.5

[Rohbericht](../run/test-servers/swim-1.16.5-matrix2-1790109053810890100/report.json) · Account `VFpush754115604` · Brand `fml,forge`.

Berichts-SHA-256: `948f20d7a78b5ac8d3f9e6834065a3d8e7eff494e8812be1e210a2387f30b84b`.

Clientartefakt / SHA-256: `C:\Users\Panthera\Desktop\GitHub\ViaVersionClient\run\mods\ViaForge-development.jar` / `12d58be1f2ca2f5d6b1f1079bbd00120f680d784bdf662cbb36e86448796fe9d`.

| Fall | Eingabe | Sekunden | Clientticks / Schwimmpose | Predictions | Max. Abweichung | Flags / Setbacks |
|---|---|---:|---:|---:|---:|---:|
| Underwater idle | idle | 3.00 | 60 / 0 | 23 | 0 | 0 / 0 |
| Forward without sprint | forward | 3.00 | 59 / 0 | 59 | 3.83670011e-15 | 0 / 0 |
| Sprint swim level | forward-sprint | 5.00 | 100 / 99 | 100 | 2.30734917e-15 | 0 / 0 |
| Coast after Sprint swim level | idle | 4.00 | 80 / 1 | 28 | 3.38192784e-15 | 0 / 0 |
| Swim up | forward-sprint | 3.00 | 60 / 59 | 60 | 6.45531979e-15 | 0 / 0 |
| Coast after Swim up | idle | 4.00 | 80 / 1 | 45 | 6.07394741e-15 | 0 / 0 |
| Swim down | forward-sprint | 3.00 | 60 / 59 | 60 | 2.50354677e-15 | 0 / 0 |
| Coast after Swim down | idle | 4.00 | 80 / 80 | 25 | 1.41553436e-15 | 0 / 0 |
| Ascend key | jump | 3.00 | 60 / 0 | 54 | 0 | 0 / 0 |
| Descend key | sneak | 3.00 | 60 / 0 | 12 | 0 | 0 / 0 |
| Sprint diagonal | forward-right-sprint | 4.00 | 80 / 79 | 80 | 4.03659353e-09 | 0 / 0 |
| Coast after Sprint diagonal | idle | 4.00 | 80 / 1 | 21 | 3.63293944e-09 | 0 / 0 |
| Dive while swimming | forward-sprint-sneak | 3.00 | 60 / 58 | 59 | 3.61145264e-15 | 0 / 0 |
| Rise while swimming | forward-sprint-jump | 3.00 | 60 / 36 | 59 | 1.95982311e-09 | 0 / 0 |
| Reverse underwater | back | 3.00 | 60 / 0 | 60 | 4.25701141e-15 | 0 / 0 |
| Establish swimming | forward-sprint | 1.50 | 30 / 29 | 30 | 2.30755783e-15 | 0 / 0 |
| Dive during established swim | forward-sprint-sneak | 3.00 | 60 / 60 | 60 | 3.46903059e-15 | 0 / 0 |
| Depth strider swimming | forward-sprint | 3.00 | 60 / 59 | 60 | 3.01084064e-15 | 0 / 0 |
| Dolphins grace | forward-sprint | 3.00 | 60 / 59 | 60 | 4.32671612e-15 | 0 / 0 |
| Conduit breathing | idle | 3.00 | 60 / 0 | 23 | 0 | 0 / 0 |
| Bubble lift | idle | 3.00 | 60 / 0 | 59 | 8.68749517e-15 | 0 / 0 |
| Bubble sink | idle | 3.00 | 60 / 0 | 60 | 0 | 0 / 0 |
| Bubble sink edge | idle | 3.00 | 60 / 0 | 59 | 0 | 0 / 0 |

## 1.17.1

[Rohbericht](../run/test-servers/swim-1.17.1-matrix2-1790109242758926600/report.json) · Account `VFpush756304985` · Brand `fml,forge`.

Berichts-SHA-256: `a835cfed6ba6e26546ec547752e497d0a41a93a459ccbea7ef7741d15fc21326`.

Clientartefakt / SHA-256: `C:\Users\Panthera\Desktop\GitHub\ViaVersionClient\run\mods\ViaForge-development.jar` / `9a07a970aa87b36adf8a4b93234410cdae70727c74e844b103f69318bd1aff36`.

| Fall | Eingabe | Sekunden | Clientticks / Schwimmpose | Predictions | Max. Abweichung | Flags / Setbacks |
|---|---|---:|---:|---:|---:|---:|
| Underwater idle | idle | 3.00 | 59 / 0 | 22 | 0 | 0 / 0 |
| Forward without sprint | forward | 3.00 | 60 / 0 | 60 | 3.88964617e-15 | 0 / 0 |
| Sprint swim level | forward-sprint | 5.00 | 100 / 99 | 100 | 2.30734917e-15 | 0 / 0 |
| Coast after Sprint swim level | idle | 4.00 | 79 / 1 | 28 | 3.12969693e-15 | 0 / 0 |
| Swim up | forward-sprint | 3.00 | 60 / 59 | 60 | 7.10730075e-15 | 0 / 0 |
| Coast after Swim up | idle | 4.00 | 80 / 1 | 45 | 6.06328415e-15 | 0 / 0 |
| Swim down | forward-sprint | 3.00 | 60 / 59 | 60 | 2.50354677e-15 | 0 / 0 |
| Coast after Swim down | idle | 4.00 | 80 / 80 | 25 | 1.41553436e-15 | 0 / 0 |
| Ascend key | jump | 3.00 | 60 / 0 | 54 | 5.19029264e-15 | 0 / 0 |
| Descend key | sneak | 3.00 | 60 / 0 | 12 | 0 | 0 / 0 |
| Sprint diagonal | forward-right-sprint | 4.00 | 80 / 79 | 80 | 3.17943723e-15 | 0 / 0 |
| Coast after Sprint diagonal | idle | 4.00 | 80 / 1 | 26 | 4.06601569e-15 | 0 / 0 |
| Dive while swimming | forward-sprint-sneak | 3.00 | 60 / 59 | 60 | 3.61145264e-15 | 0 / 0 |
| Rise while swimming | forward-sprint-jump | 3.00 | 60 / 36 | 59 | 2.52086441e-09 | 0 / 0 |
| Reverse underwater | back | 3.00 | 60 / 0 | 60 | 3.65679709e-15 | 0 / 0 |
| Establish swimming | forward-sprint | 1.50 | 30 / 29 | 30 | 2.30755783e-15 | 0 / 0 |
| Dive during established swim | forward-sprint-sneak | 3.00 | 60 / 60 | 60 | 3.46903059e-15 | 0 / 0 |
| Depth strider swimming | forward-sprint | 3.00 | 61 / 60 | 61 | 3.01084064e-15 | 0 / 0 |
| Dolphins grace | forward-sprint | 3.00 | 60 / 59 | 60 | 4.32671612e-15 | 0 / 0 |
| Conduit breathing | idle | 3.00 | 60 / 0 | 23 | 0 | 0 / 0 |
| Bubble lift | idle | 3.00 | 60 / 0 | 59 | 8.68749517e-15 | 0 / 0 |
| Bubble sink | idle | 3.00 | 60 / 0 | 60 | 0 | 0 / 0 |
| Bubble sink edge | idle | 3.00 | 60 / 0 | 58 | 0 | 0 / 0 |

## 1.21.5

[Rohbericht](../run/test-servers/swim-1.21.5-matrix-final-1790109528914996200/report.json) · Account `VFpush770594877` · Brand `fml,forge`.

Berichts-SHA-256: `180e2240d40bf62ba1490a6a003b113c2338f1999330b8f9ae679e73ee3ba674`.

Clientartefakt / SHA-256: `C:\Users\Panthera\Desktop\GitHub\ViaVersionClient\run\mods\ViaForge-development.jar` / `746b796da4c3ea8512150c74962c88cf227460212a3487da83a8d15fa691456d`.

| Fall | Eingabe | Sekunden | Clientticks / Schwimmpose | Predictions | Max. Abweichung | Flags / Setbacks |
|---|---|---:|---:|---:|---:|---:|
| Underwater idle | idle | 3.00 | 58 / 0 | 46 | 3.52842755e-15 | 0 / 0 |
| Forward without sprint | forward | 3.00 | 59 / 0 | 59 | 3.5404427e-15 | 0 / 0 |
| Sprint swim level | forward-sprint | 5.00 | 100 / 99 | 100 | 3.06987552e-15 | 0 / 0 |
| Coast after Sprint swim level | idle | 4.00 | 80 / 1 | 47 | 3.40699691e-15 | 0 / 0 |
| Swim up | forward-sprint | 3.00 | 60 / 59 | 60 | 7.43483479e-15 | 0 / 0 |
| Coast after Swim up | idle | 4.00 | 80 / 1 | 80 | 6.97705782e-15 | 0 / 0 |
| Swim down | forward-sprint | 3.00 | 60 / 59 | 60 | 3.10912006e-15 | 0 / 0 |
| Coast after Swim down | idle | 4.00 | 80 / 80 | 41 | 2.63591232e-15 | 0 / 0 |
| Ascend key | jump | 3.00 | 60 / 0 | 60 | 7.04991621e-15 | 0 / 0 |
| Descend key | sneak | 3.00 | 60 / 0 | 12 | 3.05311332e-15 | 0 / 0 |
| Sprint diagonal | forward-right-sprint | 4.00 | 80 / 79 | 80 | 3.33644647e-15 | 0 / 0 |
| Coast after Sprint diagonal | idle | 4.00 | 80 / 1 | 31 | 4.13310164e-15 | 0 / 0 |
| Dive while swimming | forward-sprint-sneak | 3.00 | 60 / 59 | 60 | 3.13714753e-15 | 0 / 0 |
| Rise while swimming | forward-sprint-jump | 3.00 | 60 / 37 | 60 | 3.27205302e-09 | 0 / 0 |
| Reverse underwater | back | 3.00 | 61 / 0 | 61 | 3.53017378e-15 | 0 / 0 |
| Establish swimming | forward-sprint | 1.50 | 30 / 29 | 30 | 2.49487976e-15 | 0 / 0 |
| Dive during established swim | forward-sprint-sneak | 3.00 | 60 / 60 | 60 | 3.46903059e-15 | 0 / 0 |
| Depth strider swimming | forward-sprint | 3.00 | 60 / 59 | 60 | 3.22610252e-15 | 0 / 0 |
| Dolphins grace | forward-sprint | 3.00 | 60 / 59 | 60 | 3.58507583e-15 | 0 / 0 |
| Conduit breathing | idle | 3.00 | 60 / 0 | 45 | 3.52842755e-15 | 0 / 0 |
| Bubble lift | idle | 3.00 | 60 / 0 | 60 | 6.9388939e-15 | 0 / 0 |
| Bubble sink | idle | 3.00 | 60 / 0 | 60 | 2.95049131e-15 | 0 / 0 |
| Bubble sink edge | idle | 3.00 | 60 / 0 | 48 | 2.83106871e-15 | 0 / 0 |

## 26.2 ViaForge

[Rohbericht](../run/test-servers/swim-26.2-prepared-transitions-1790115286060860700/report.json) · Account `VFpush776328025` · Brand `fml,forge`.

Berichts-SHA-256: `a8a921b540b047240c4ad0cc9975f72944e104f9ca57312080fb63380af01b2f`.

Clientartefakt / SHA-256: `C:\Users\Panthera\Desktop\GitHub\ViaVersionClient\run\mods\ViaForge-development.jar` / `c0a918540e70a6aa0f742cb78ca8f788bfe3090d60b4ec8982b219c6b373515f`.

| Fall | Eingabe | Sekunden | Clientticks / Schwimmpose | Predictions | Max. Abweichung | Flags / Setbacks |
|---|---|---:|---:|---:|---:|---:|
| Underwater idle | idle | 3.00 | 58 / 0 | 46 | 3.52842755e-15 | 0 / 0 |
| Forward without sprint | forward | 3.00 | 60 / 0 | 60 | 3.5404427e-15 | 0 / 0 |
| Sprint swim level | forward-sprint | 5.00 | 99 / 98 | 99 | 3.06987552e-15 | 0 / 0 |
| Coast after Sprint swim level | idle | 4.00 | 80 / 1 | 47 | 3.40699691e-15 | 0 / 0 |
| Swim up | forward-sprint | 3.00 | 60 / 59 | 60 | 7.43483479e-15 | 0 / 0 |
| Coast after Swim up | idle | 4.00 | 80 / 1 | 80 | 6.97705782e-15 | 0 / 0 |
| Swim down | forward-sprint | 3.00 | 60 / 59 | 60 | 3.10912006e-15 | 0 / 0 |
| Coast after Swim down | idle | 4.00 | 80 / 80 | 41 | 2.63591232e-15 | 0 / 0 |
| Ascend key | jump | 3.00 | 60 / 0 | 60 | 7.04991621e-15 | 0 / 0 |
| Descend key | sneak | 3.00 | 60 / 0 | 11 | 3.33066907e-15 | 0 / 0 |
| Sprint diagonal | forward-right-sprint | 4.00 | 80 / 79 | 80 | 3.33644647e-15 | 0 / 0 |
| Coast after Sprint diagonal | idle | 4.00 | 80 / 1 | 39 | 4.13310164e-15 | 0 / 0 |
| Dive while swimming | forward-sprint-sneak | 3.00 | 60 / 59 | 60 | 3.28950413e-15 | 0 / 0 |
| Rise while swimming | forward-sprint-jump | 3.00 | 60 / 37 | 60 | 3.27205302e-09 | 0 / 0 |
| Reverse underwater | back | 3.00 | 60 / 0 | 60 | 3.53017378e-15 | 0 / 0 |
| Establish swimming | forward-sprint | 1.50 | 30 / 29 | 30 | 2.49487976e-15 | 0 / 0 |
| Dive during established swim | forward-sprint-sneak | 3.00 | 60 / 60 | 60 | 3.46903059e-15 | 0 / 0 |
| Depth strider swimming | forward-sprint | 3.00 | 60 / 59 | 60 | 3.56859942e-15 | 0 / 0 |
| Dolphins grace | forward-sprint | 3.00 | 60 / 59 | 60 | 3.5374857e-15 | 0 / 0 |
| Conduit breathing | idle | 3.00 | 60 / 0 | 45 | 3.52842755e-15 | 0 / 0 |
| Bubble lift | idle | 3.00 | 60 / 0 | 60 | 6.88338275e-15 | 0 / 0 |
| Bubble sink | idle | 3.00 | 60 / 0 | 60 | 2.95842785e-15 | 0 / 0 |
| Bubble sink edge | idle | 3.00 | 60 / 0 | 48 | 2.83106871e-15 | 0 / 0 |
| Swim into low passage | forward-sprint | 3.00 | 60 / 59 | 60 | 6.10622664e-16 | 0 / 0 |
| Rest inside low passage | idle | 3.00 | 60 / 60 | 21 | 8.37871439e-16 | 0 / 0 |
| Swim out of low passage | forward-sprint | 5.00 | 100 / 100 | 100 | 5.13478149e-15 | 0 / 0 |
| Swim against shore | forward-sprint-jump | 5.00 | 100 / 35 | 100 | 4.10100082e-08 | 0 / 0 |
| Rest on shore | idle | 3.00 | 61 / 0 | 16 | 1.77592366e-08 | 0 / 0 |

## 26.2 Original

[Rohbericht](../run/test-servers/swim-26.2-prepared-reference-1790115489688158900/report.json) · Account `NativeP25549300` · Brand `vanilla`.

Berichts-SHA-256: `cf9064a79c7514197ae7585e76cca9a170bf8b49117f3f3fb6c4ab8360a4199e`.

Clientartefakt / SHA-256: `run/test-servers/native-clients/26.2/client.jar` / `40896ee9f1e2bec3c934daac7e93d41e9e3d9c2f8ae0ca366d52ffbfd1afa290`.

| Fall | Eingabe | Sekunden | Clientticks / Schwimmpose | Predictions | Max. Abweichung | Flags / Setbacks |
|---|---|---:|---:|---:|---:|---:|
| Underwater idle | idle | 3.00 | 60 / 0 | 43 | 3.52842755e-15 | 0 / 0 |
| Forward without sprint | forward | 3.00 | 60 / 0 | 60 | 3.55455804e-15 | 0 / 0 |
| Sprint swim level | forward-sprint | 5.00 | 100 / 99 | 100 | 3.06987552e-15 | 0 / 0 |
| Coast after Sprint swim level | idle | 4.00 | 80 / 1 | 47 | 3.40699691e-15 | 0 / 0 |
| Swim up | forward-sprint | 3.00 | 60 / 59 | 60 | 7.09984491e-15 | 0 / 0 |
| Coast after Swim up | idle | 4.00 | 80 / 1 | 80 | 6.97705782e-15 | 0 / 0 |
| Swim down | forward-sprint | 3.00 | 60 / 59 | 60 | 3.3586827e-15 | 0 / 0 |
| Coast after Swim down | idle | 4.00 | 79 / 79 | 40 | 8.59555482e-16 | 0 / 0 |
| Ascend key | jump | 3.00 | 60 / 0 | 60 | 7.07767178e-15 | 0 / 0 |
| Descend key | sneak | 3.00 | 60 / 0 | 11 | 3.33066907e-15 | 0 / 0 |
| Sprint diagonal | forward-right-sprint | 4.00 | 80 / 79 | 80 | 3.28209682e-15 | 0 / 0 |
| Coast after Sprint diagonal | idle | 4.00 | 80 / 1 | 31 | 3.40699691e-15 | 0 / 0 |
| Dive while swimming | forward-sprint-sneak | 3.00 | 60 / 59 | 60 | 3.28950413e-15 | 0 / 0 |
| Rise while swimming | forward-sprint-jump | 3.00 | 60 / 36 | 60 | 6.98261e-15 | 0 / 0 |
| Reverse underwater | back | 3.00 | 60 / 0 | 60 | 3.5354073e-15 | 0 / 0 |
| Establish swimming | forward-sprint | 1.50 | 30 / 29 | 30 | 2.49487976e-15 | 0 / 0 |
| Dive during established swim | forward-sprint-sneak | 3.00 | 60 / 60 | 60 | 3.46903059e-15 | 0 / 0 |
| Depth strider swimming | forward-sprint | 3.00 | 60 / 59 | 60 | 3.22610252e-15 | 0 / 0 |
| Dolphins grace | forward-sprint | 3.00 | 60 / 59 | 60 | 3.5374857e-15 | 0 / 0 |
| Conduit breathing | idle | 3.00 | 61 / 0 | 45 | 3.52842755e-15 | 0 / 0 |
| Bubble lift | idle | 3.00 | 61 / 0 | 61 | 6.88338275e-15 | 0 / 0 |
| Bubble sink | idle | 3.00 | 60 / 0 | 60 | 2.95842785e-15 | 0 / 0 |
| Bubble sink edge | idle | 3.00 | 59 / 0 | 48 | 2.83106871e-15 | 0 / 0 |
| Swim into low passage | forward-sprint | 3.00 | 60 / 59 | 60 | 5.689893e-16 | 0 / 0 |
| Rest inside low passage | idle | 3.00 | 60 / 60 | 21 | 4.4408921e-16 | 0 / 0 |
| Swim out of low passage | forward-sprint | 5.00 | 100 / 100 | 100 | 1.63757896e-15 | 0 / 0 |
| Swim against shore | forward-sprint-jump | 5.00 | 101 / 35 | 101 | 7.25690212e-15 | 0 / 0 |
| Rest on shore | idle | 3.00 | 60 / 0 | 16 | 7.16459529e-15 | 0 / 0 |

Rohdaten verbleiben unverändert in den jeweiligen isolierten Testordnern:
`viaforge/client.jsonl` beziehungsweise `native/client.jsonl`,
`plugins/ViaForgeLabAC/events.jsonl`, `sources.json`, Konsolenlogs und Weltbackups.
Die bestehenden aktiven Laborwelten wurden für diese Tests nicht geöffnet.

Die früheren Fehlversuche bleiben erhalten und sind in [SWIMMING.md](SWIMMING.md)
beschrieben. Diese Matrix ersetzt keine Boots-/Elytra-/Entity-Push-Nachweise.
