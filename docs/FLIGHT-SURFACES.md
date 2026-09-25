# Elytra straight-up look, slime and shulker lids

The subsequent [landing, swimming-column and repeated-lid report](FLIGHT-EDGES.md)
covers additional failures outside this report's original test sequences.
This is a historical verification snapshot. Its release files and manifest
are preserved under `build/backups/flight-edges-20260925-initial/`; the current
files in `build/libs/` are documented by the follow-up report. Process IDs
and server-running statements below describe the time of this snapshot.

Investigation started from `02001c8477a7e7da92d89eea83331c266507036f`.
The reported setup is selected client protocol 776 (26.2) on a real 1.21.11
server with ViaVersion. The user had only tried the Elytra case in ViaForge.
Direct 26.2 is a separate test endpoint, not a substitute for that setup.

## Original behavior and corrections

| Boundary | Behavior retained from the original client |
| --- | --- |
| 1.9–1.12.2 | Legacy look-vector signs/phase; float table lift and original float drag constants |
| 1.13 | Look vector removes the old negation/PI phase; at pitch −90° horizontal look becomes exactly zero, so momentum is not converted into climb |
| 1.18.2 | Elytra lift uses `Math.cos` with the original float angle, rather than the float table |
| 1.21.11 | Sin/cos lookup uses double index arithmetic and the original double table-generation expression |
| 1.21.2 | Slime's ground step effect runs after travel; suppress its earlier 1.8 callback and apply it once at the original stage |
| 26.2 | Slime rebound includes the fraction of movement completed before collision, effective gravity and air drag |
| 26.3 | The rebound cutoff additionally includes descent exactly equal to gravity (`<=` instead of `<`); covered by original source and Forge regression, not a live Grim claim |
| 1.11–1.16.5 | Shulker lid pushes overlapping entities out of the current extension, including the original closing-stage behavior |
| 1.17 | Shulker pushes only while opening, using the slab swept between successive progress values; usual side steps are approximately 0.11 blocks |

`OriginalLookMath` is used for flight and its rocket acceleration. The change
does not globally replace the 1.8 camera or other movement paths. Flight still
uses one travel and one collision step. The existing captured gravity value and
slow-falling effect participate in flight and the 26.2 rebound.

Each attached rocket now applies its acceleration in its own entity update,
as both original 1.12.2 and 26.2 do. Applying all boosts inside player travel
before position movement was one stage too early. The first corrected flight
run isolated this additional bug: one rocket was tolerated by Grim, but the
second overlapping rocket immediately caused offsets up to 0.073 and 16
Simulation flags. Original-client tick traces have the same end-of-tick
velocities but travel first and apply rocket acceleration afterwards.

The old shulker entity only animated its lid. It now performs the original local
push, with a scoped external-movement context to avoid player sneak-edge backing
off during the push. The collision box was already updated by the shared block
base class; an initially suspected missing box expansion was not the cause.
No interpolation is invented: even original 26.2 has uneven vertical movement
on an upward-opening lid. Horizontal opening must progress through individual
steps, not just end in the same location after a server correction.

## Source provenance

The retained [source manifest](FLIGHT-SURFACES-SOURCES.json) and local
`build/inspection/flight-surfaces/sources.json` record official Mojang
metadata URLs, download SHA-1, local SHA-256 and obfuscated class names. The
unobfuscated 26.2 files have their own source manifest. Inspected classes:
`Entity`, `LivingEntity`, `Mth`, `SlimeBlock`, `ShulkerBoxBlockEntity` and the
original rocket entity. Relevant originals include 1.12.2, 1.13, 1.16.5,
1.17.1, 1.18.1/1.18.2, 1.21.1/1.21.4, 1.21.10/1.21.11, 26.1.2, 26.2 and 26.3.
Decompilation is local with CFR 0.152; original binaries are not modified.

## Reproduction and evidence rules

`py -3 tools/test-servers/surface_probe.py --version 26.2 --backend 1.21.11`
creates one isolated, loopback-only server, retains its world and backs it up
before building the fixture. Add `--client native` for original 26.2 or
`--baseline` for archived production from before this correction. Omit
`--backend` for a direct server matching the selected version.

The archived baseline differs only in development input/observation classes;
`build/backups/flight-surfaces-20260924-initial/baseline-v3.json` records the
archive hashes and updated classes. All other entries were byte-compared.

Cases: stone walk; slime walk/sprint/sneak/jump/rebound/sneak landing; shulker
opening upward/east/north; horizontal, exactly −90° and −89.9° Elytra flight,
without rockets and with real rocket use; rocket climb starting above Y=256;
final coast. Equipment changes,
teleports, input release and water landings are retained as separately named
phases and included in the Survival issue count. Creative is only used for
initial chunk loading. Original and ViaForge use the same input harness.

Each case checks actual non-OP Survival, enabled Grim, automatic verbose,
client protocol and permissions before and after. The client trace records
loaded/alive state, actual keys, position/velocity, flight, camera pitch,
underfoot blocks and lid progress. Missing predictions, dead/grounded flight,
unloaded terrain, a late side-push snap or missing rocket acceleration cannot
be counted as a movement PASS. Server events retain offsets, packets, flags
and setbacks. Login corrections are reported separately.

## Intermediate runs and limits

The first direct-26.2 ViaForge baseline reproduced slime Simulation flags
(10 including coast), an upward-lid Phase flag and repeated Simulation/setbacks
at vertical flight. Its long late-flight cases left valid terrain and are not
valid end-to-end flight comparisons.

An initial original-client observer used an obsolete screen-field name and
failed; this run is not a physics result. A later original run proved zero
Survival flags, but lacked underfoot observations for several cases. Another
26.2 → 1.21.11 original run died when a descending glider was teleported onto
stone, invalidating its remaining rocket cases. The fixture now uses a water
landing between flights, in an enclosed pool to avoid changing the dry tests.

The original 26.2 → 1.21.11 `surfaces-backend-original2` run passed 17/17 with
no Survival or login issues, but still used the earlier uncontained pool.
Final matched-fixture results are recorded separately; this intermediate PASS
is not evidence for dry shulker/coast conditions in every case.

`surfaces-backend-fixed2-1790281989851895000` passed all 17 cases after the
rocket tick correction, including every Survival reset phase; two login
corrections are separate. The subsequent original-client 18-case run
`surfaces-backend-native-final-1790282245963358300` passed the measurements
but failed overall: teleporting the still-boosting high glider into water
produced two Simulation flags (0.058800, 0.116424) and one setback. This
original-client/backend/Grim transition is retained as a separate limitation,
not attributed to ViaForge. Final fixtures let the rockets expire in flight
before teleporting to water; all coast phases remain evaluated.

Flight above build height checks the loaded column at y=63 in both observers.
The old 1.8 `isBlockLoaded` query at the player's position rejects y>=256 even
when the chunk is present. This observation correction does not alter physics
or block data; it permits an explicit high-flight case instead of silently
discarding the height range.

The current surface fix addresses local player movement with ordinary vanilla
attribute values. Arbitrary 26.2 bounciness/air-drag/friction modifiers, other
entity physics and every possible lid obstruction are not implied to pass.
Source-derived version rules do not constitute a live Grim test on every
supported protocol. Fresh completed runs and release evidence follow below.

## Confirmed final live runs

All listed runs use unchanged Grim 2.3.74-8eb5f28. Original 26.2 identifies as
`vanilla`; ViaForge identifies as `fml,forge`. Grim sees the selected protocol,
and every measured player is in Survival, non-OP, verbose enabled, with all
three bypass permissions false. Normal lab worlds/accounts are untouched.

| Client / endpoint | Cases | Survival flags / setbacks | Login corrections |
| --- | ---: | --- | ---: |
| Archived ViaForge 26.2 → 1.21.11 + ViaVersion | 11/18; FAIL | 36 Simulation, 1 Phase / 51 | 2 |
| Original 26.2 → 1.21.11 + ViaVersion | 18/18 PASS | 0 / 0 | 0 |
| Fixed ViaForge 26.2 → 1.21.11 + ViaVersion | 18/18 PASS | 0 / 0 | 2 |
| Fixed ViaForge 26.2 → direct 26.2 | 18/18 PASS | 0 / 0 | 2 |
| Fixed ViaForge 1.12.2 → direct 1.12.2 | 18/18 PASS | 0 / 0 | 0 |
| Fixed ViaForge 1.17.1 → direct 1.17.1 | 18/18 PASS | 0 / 0 | 2 |
| Fixed ViaForge 1.21.11 → direct 1.21.11 | 18/18 PASS | 0 / 0 | 2 |

Report directories under `run/test-servers/`:

- `interaction-26.2-surfaces-before-final-1790282759993938600`
- `interaction-26.2-surfaces-backend-native-final2-1790282527160850600`
- `interaction-26.2-surfaces-fixed-final-1790283006965574800`
- `interaction-26.2-surfaces-direct-final-1790283282850949100`
- `interaction-1.12.2-surfaces-boundary-final3-1790284468092213700`
- `interaction-1.17.1-surfaces-boundary-final-1790283974120657900`
- `interaction-1.21.11-surfaces-boundary-final-1790284197477572800`

Each contains `report.json`, original server/client traces, configuration,
world backup, source manifest and input phase timestamps. The backend is
Paper 1.21.11 build132 with ViaVersion5.12.0; direct26.2 is Paper build126.
The baseline production classes are byte-identical to the archived pre-fix
development build; only key/observer classes were updated.

For the matched 26.2 → 1.21.11 fixture, straight-up unpowered flight had a
maximum Grim offset of 0.132418 blocks before the fix and about 1.6e-14
afterwards, also about 1.6e-14 with the original client. Slime rebound changed
from 0.020245 to about 1.2e-14. A zero flag count alone is insufficient for
original behavior: the baseline slime-walk case itself received no flag but
travelled 5.331694 blocks, versus 4.675650 in the original client and 4.675649
after the correction. Those are the recorded first-to-last tick displacements
of the same four-second input phase, not theoretical speed constants.

The initial 1.12.2 boundary run is invalid: pre-login fill commands failed
because its chunks were not loaded. The missing slime and water caused bad
test geometry and eventually death. The evidence checks rejected that run
despite no Grim flags. Legacy fixtures now build after Creative chunk loading
and reject failed fill output; no existing saved world is rebuilt.

The second 1.12.2 attempt stopped with Windows error 5 while replacing its
report, after three complete passing cases. A simultaneous report reader was
active. That partial run is not an 18-case PASS; completed reports are now
read only after the harness exits.

The final 1.12.2 retry listed above uses the validated, loaded fixture and
passes all 18 cases. Altogether the fixed ViaForge runs cover 90 measured
Survival cases and the matched original client covers 18. All Survival reset,
equipment and transition phases are included in each overall result. The
eight ViaForge login corrections across the other four final runs are
explicitly outside that movement result. Original live comparison here is
26.2 on the reported 1.21.11 + ViaVersion endpoint; the older client boundaries
add original source comparison and ViaForge live tests, not native live runs.

## Build and regression verification

- 136 JUnit tests, zero failures/errors/skips, freshly run at 21:18 UTC on
  24 September 2026. Results: `build/test-results/test/`.
- 103 Python tests passed: `build/logs/surfaces-python-release.txt`.
- Full Forge smoke passed after the final rocket and 26.3 cutoff changes:
  `build/logs/surfaces-client-smoke-final.txt`. This new report begins with
  `PASS`, covers all 49 resource profiles and includes actual transformed
  slime collisions, ticking shulker lids and overlapping rocket entity ticks.
  Build/client output: `build/logs/surfaces-forge-final.txt`.
- Release JAR and matching sources:
  `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar` and
  `build/libs/ViaForge-1.8.9-4.4.0-client.1-sources.zip`.
  `build/libs/flight-surfaces-release.json` records their byte sizes/SHA-256,
  report hashes, case offsets, exact endpoint conditions and archive checks.
  Previous release/source files remain in
  `build/backups/flight-surfaces-20260924-initial/`.

The smoke is an in-client regression check, not a claim of live Grim coverage
on all 49 versions. All isolated servers and test clients were stopped after
testing. The existing normal 26.2 server remains enabled with unchanged Grim;
zero players were connected at the final runtime check. No commit was made.
