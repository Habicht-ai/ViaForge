# New low-flight, bubble swimming and repeated shulker cases — 2026-09-25

Reported endpoint: ViaForge selected26.2 → real1.21.11 with ViaVersion.
Game mode and exact input timings were not supplied. Reproduction uses fresh
non-OP Survival accounts with unchanged Grim. No original-client reproduction
of these new cases has yet been claimed.

HEAD02001c8; all preceding surface fixes remain uncommitted and preserved.
Release/source/development JAR and worktree patch backed up under
`build/backups/flight-edges-20260925-initial/` before edits.
Initial runtime: normal26.2 server127348, web125860/worker127324, Grim ON,
zero players; no Minecraft client. Normal worlds remain untouched.

New `edge_probe.py` reuses the existing isolated server harness and genuine
key/client tick paths. Adds ground/low Elytra launches, repeated upward lid
open/close, and active swimming into soul-sand/magma columns. Initial setup
must be validated; no PASS can be inferred from missing predictions or motion.
Only input instrumentation/test files changed so far; production unchanged.

## Reproduction / original comparison

`interaction-26.2-edges-before-1790287859684484100` reproduced Simulation
on the landing transition and all six swimming/column combinations, plus ten
PositionPlace flags during fast lid use. Initial live fixture construction
also caused TimerLimit during setup/early Survival; do not conflate these.
Fixture now built before login. Archived old production with new key observer:
`build/backups/flight-edges-20260925-initial/ViaForge-before-edge-probe.jar`.
Clean baseline repeat `edges-baseline-final`, session21276, log
`build/logs/edges-baseline-final.txt`; production from the preceding release.

Original `interaction-26.2-edges-original-1790288121765413200` has zero flags,
setbacks or login issues.13/14 measured cases valid: low glide started too late
and never flew, so no full PASS. Raise its initial height from67 to69 and
repeat both clients. Other cases include real swimming and rapid lid cycles.

Original landing tick35 has input_forward0.29400003, ViaForge1.0 before
correction; Grim offset0.08918. LivingEntity.isVisuallySwimming explicitly
includes retained FALL_FLYING pose after the flag clears. Earlier smoke's
"never crawling" assertion was wrong and is corrected, not preserved.

Shulker trace proves client ray hit `f=down` atY64 while player stands atY65.2
above the lid.1.8 voxel traversal passes a shortened ray starting within the
expanded lid; modern traversal clips the full ray. New scoped mixin keeps
the original ray for modern shulker shapes. No packet-face rewriting.

26.2 and original1.21.10 Entity.checkInsideBlocks traverse Y then ordered
horizontal movement axes with one visited-block set. The old end-position
bubble scan misses intermediate axis contacts. Implementing/validating
recorded contacts at the original post-travel stage; no extra physics ticks.

## 00:29 continued

Clean baseline `interaction-26.2-edges-baseline-final-1790288363588145600`:
4/14;124Simulation,10PositionPlace,1GroundSpoof,47setbacks in Survival;
two login corrections. No setup TimerLimit in this run. Baseline archive
byte-check confirms only LiveBoatProbe.class differs from saved previous
development JAR (`baseline-proof.json`).

Original final `interaction-26.2-edges-original-final-1790288606078781200`:
14/14 PASS; zero Survival issues including resets. Five Timer/one TimerLimit
during login/resource load are separate; they are retained, not suppressed.
Build1 succeeded. ViaForge fix1 running session38342,
`interaction-26.2-edges-fixed1-1790288904235261100`, log `edges-fixed1.txt`.
Read its report only after completion. No new release claim yet.
Original1.21.8 already has axis movement (a boolean instead of the newer
Optional member), but lacks the precise-contact flag in entityInside.
The new PRECISE_BLOCK_EFFECTS rule begins protocol773 (1.21.9/1.21.10),
where the precise-contact gate is present. Earlier swept effects are a
separate boundary, not implied by this rule. Do not infer semantics solely
from a field name in mappings.

## 00:44 checkpoint

First correction `interaction-26.2-edges-fixed1-1790288904235261100`:
12/14 measured cases passed, overall FAIL with92 Simulation flags. Upward
column exit revealed wrong block iteration order (Y must be outermost for a
directional scan); repeated caps make this physically observable. Replaced
the endpoint approximation with the original single-axis traversal order,
start/intermediate/end cells, precise gate and shared visited set.

`interaction-26.2-edges-fixed2-1790289287418449700`:14/14 PASS,
zero Survival flags/setbacks including every reset/equipment phase. Two
login corrections retained separately. Column direction now measured from
actual original block/fluid state at the feet, not inferred from coordinates.
Maximum upward-column offset6.86e-9; downward below7.1e-15. Low-glide landing
still has residual4.71e-5, explicitly not exact numeric parity.

Original26.2 matched repeat running session97718,
`interaction-26.2-edges-original-contacts-1790289558814348900`, console
`build/logs/edges-original-contacts.txt`; production physics/network untouched.
New Forge regressions exercise whole-ray lid picking and two different
axis/contact orders with real moveEntity and surface velocity caps.
Fresh106 Python tests pass (`build/logs/edges-python-final.txt`).
No final new JAR delivery/smoke claim yet. Normal server127348 untouched.

## 00:54 checkpoint

Original contact-observed run completed14/14 PASS, zero Survival and zero
login issues. Older `interaction-1.17.1-edges-boundary-final-1790289795146604400`
had no Survival issues but only13/14 valid cases: stationary low glide produced
13 predictions due to its coarser packet threshold. Raised only that older
test's start fromY69 toY72 rather than lower evidence requirements.
Repeat `interaction-1.17.1-edges-boundary-repeat-1790290075148972900` is14/14
PASS (26 low-glide predictions), zero Survival issues, two login corrections.

Full49-profile Forge smoke/build running session13557:
`build/logs/edges-forge-final.txt`, new report
`build/logs/edges-client-smoke-final.txt` (confirmed absent before launch).
Next: final mixed26.2 and direct26.2 runs, finish docs, rebuild matching sources
and generate `build/libs/flight-edges-release.json` with the prepared proof
script `build/flight-edges-release-proof.py`.

Original ray boundary now directly verified in1.12.2(World.amu),1.13(World.axs)
and1.13.2(World.axy): old stepped vector versus unchanged full ray argument.
The reused1.14.4 LivingEntity.class matches the original JAR byte-for-byte.
Pinned Mojang hashes verified for nine original JARs; provenance is
`docs/FLIGHT-EDGES-SOURCES.json`. No new server/check/config bypass was introduced.

## 01:05 IMPORTANT: final repeat found a further failure

Full Forge49 smoke passed at00:54:18;136 JUnit passed,106 Python passed after
one retained Windows10053 HTTP-test socket error (`edges-python-socket-error.txt`).
However `interaction-26.2-edges-release-mixed-1790290615336398100` is FAIL:
13/14 measured,27 Simulation+3 GroundSpoof+1 setback across Survival, two login
corrections. First failure is in Reset flight coast after ground sprintjump,
another in downward landing. All bubble/shulker cases pass. DO NOT use earlier
14/14 reports to declare this new repeat fixed. No final release delivery.

At1790290714.969/.970 server sees two PONGs, transaction447. It then predicts
ordinary standing travel but client END tick15 still has elytra=true atY64,
vz0.3847530387. Original expected carry is0.3868394521, first offset0.0020864133.
Next tick's offset0.1735386734 leads to setback. This suggests metadata/ACK
ordering, not another arbitrary change to flight arithmetic. Existing ordered
ping path must be investigated rather than suppressing packets or flags.

Development-only `MovementPacketProbe` passively queues incoming VF|entity
metadata/VF|pong and outgoing pong timestamps/threads/bytes; MovementTrace
now includes elytra and flushes `packet-order.jsonl` on main thread.
`MovementElytraProbe` also logs application of actual flight metadata.
Observe run session3955, `interaction-26.2-edges-order-observe-1790291065344095700`,
console `build/logs/edges-order-observe.txt`. Verify last observer compiled;
it was added after starting the build. Production remains same as fixed2.
Direct26.2 final run not started. Prepared proof script deliberately expects
PASS and must be updated to the eventual corrected final run name.

## 01:25 queue investigation (not yet a release)

`edges-bundle-observe-1790291354322061900` completed 14/14 without issues.
Raw protocol observation found NO bundle delimiters on this endpoint; the
bundle hypothesis does not explain the recorded failure. Original 1.8 locks
`Minecraft.scheduledTasks` throughout draining and in the receiving thread's
enqueue. Original 1.21.10 and 26.2 have a separate ConcurrentLinkedQueue packet
processor before ordinary tasks; 1.21.8 lacks that separate processor.
Original 1.14.4 already has a concurrent general task queue, a separate older
boundary not ported by this change.

Prepared `ClientPacketTasks`, `PacketTaskQueue`, two connect mixins and rule
DEDICATED_PACKET_QUEUE from protocol 773. Session join/leave retained events
stay in the same FIFO as play packets. No physics, packet acknowledgements or
check configuration changed. A concurrency regression requires the receiver
to enqueue metadata while the preceding ping handler is still running.
This is a proven original scheduling difference; causal coverage for the
intermittent landing failure still requires live verification.

Unchanged pre-queue production is running four repetitions of each flight
case plus columns/lids (26 measured cases), with passive packet observers:
`interaction-26.2-edges-order-stress-before-1790291967588698400`, exec session
52699, console `build/logs/edges-order-stress-before.txt`. Do not build/install
over its active client. Normal server PID 127348 remains untouched. Read the
report only after completion (Windows replace-sharing constraint).

## 15:58 resumed after usage interruption

Queue build completed at 01:27: BUILD SUCCESSFUL, 138 JUnit tests, zero
failures/errors/skips. At resumption no Java process was running (the former
normal server was already stopped externally); no existing client was killed.
Four-round pre-queue stress above ended FAIL, 15/26 valid cases: accumulated
Survival fall damage killed the player on the last landing. Subsequent 170
MultiActionsD flags occurred on the death screen and are not flight evidence.
No Simulation flag occurred in the 15 valid flight cases. All logs retained.

Harness now heals between flight cases, identically for both clients, without
altering movement attributes, game mode or damage rules. Current actual mixed
run: `interaction-26.2-edges-queue-mixed-1790344518497656700`, session 35991,
console `build/logs/edges-queue-mixed.txt`, 26 measured cases including four
rounds of the four flight sequences. Grim ON confirmed, loaded Survival.
Do not read its live report.json. Added a development Forge regression for
the transformed PacketThreadUtil, FIFO/exactly-once/main-thread dispatch and
disconnected payload release; compile/run it after this live client ends.
Fresh 106 Python tests pass (`edges-queue-python.txt`); PowerShell rendered
unittest stderr as NativeCommandError, but the test summary is OK.
Full new Forge smoke, native stress comparison, direct26.2 and release proof
remain pending. No final release claim.

## 16:01 packet queue alone does NOT fix intermittent landing flags

Current four-round mixed run recorded 4 Simulation + 1 GroundSpoof on its
first downward landing. At 1790344677499 raw metadata `6301000008ff` arrives;
Pongs `fffffc3f`/`fffffc3e` are handled/sent at 7500, but translated retained
metadata reaches NetHandlerPlayClient on Netty at 7501. Client tick 1680 ends
7503 still gliding; metadata applies 7517. First Grim offset 0.0097372412,
then 0.0804882407. This occurs even with the concurrent original-style queue:
translation crosses the drain boundary. Do not claim the queue correction
eliminates this race. No artificial delay or early-Pong suppression added.

Added passive native handlePing/handleSetEntityData/send observations (queued
strings flushed on tick end; no packet/physics replacement) for an extended
original26.2 comparison next. Need determine whether this pre-transaction
metadata race reproduces in the original as well. The installed Grim source
updates isGliding at the BEFORE-metadata transaction, not the after one.

## 16:52 resumed, original reproduces timing failure under delayed delivery

Correcting the 16:01 provisional label: the first flag was during ground
sprintjump repeat 2, not the first downward landing. Completed queue-only
mixed run: 24/26 FAIL, 14 Simulation + 2 GroundSpoof + 1 Survival setback;
zero login issues. Packet audit found 2 split flight ends among 32 transitions.

`interaction-26.2-edges-native-order-1790344911041719100`: 34/34 PASS, six
rounds of all four flight cases, all columns/lids, zero flags/setbacks/login
issues. Native packet audit: 48 flight ends, zero split across movement ticks.
`interaction-26.2-edges-queue-direct-1790347374769899300`: 26/26 PASS, four
rounds, zero flags/setbacks/login issues; 32 flight ends, zero split ticks.
Full Forge49 PASS is `build/logs/edges-queue-client-smoke.txt`, build log
`edges-queue-forge.txt` completed 16:12. That build has the dedicated queue
but NOT the subsequent original-packet grouping / snapshot optimization.

New production edits group all translated outputs of one original packet
into a single main-thread task (including retained events), avoiding partial
metadata application. Unneeded block-only observer snapshots at modern codec
boundaries are skipped for unrelated packet IDs; actual Via translation is
unchanged and still occurs once. Added concurrent and actual embedded-pipeline
regressions. These edits still require build, smoke and fresh live repeats.

Separate adverse-network diagnostic running session1713:
`interaction-26.2-edges-native-delayed-frame-1790347771376020500`, console
`build/logs/edges-native-delayed-frame.txt`. Original26.2 + real inputs + same
Grim on mixed endpoint. Loopback TCP relay delays ONE flight-end metadata frame
60 ms, preserves exact bytes/order, never acknowledges/synthesizes packets.
Only isolated server network compression disabled for frame inspection.
Native already produced Simulation (first 0.009137) and GroundSpoof after this
split; this is a controlled diagnostic FAIL, never a normal PASS. Normal native
34/34 above was unshaped. Relay records both stream hashes/byte counts for
verification after closure; tests cover fragmented frames and exact forwarding.
No delay/filter enters production ViaForge. Pending: finish diagnostic, build
grouping/optimization, fresh normal mixed/direct tests and release artifacts.

## 16:59 build/reproduction checkpoint

Delayed native diagnostic completed 13/14 overall FAIL: 45 Simulation,
1 GroundSpoof, zero setbacks/login issues. Exactly one 60.56 ms flight-end
delay, native Pong at1790347860342, still-gliding tick0379, metadata0408.
Relay clientbound13,251,728 and serverbound60,586 bytes were forwarded exactly,
matching SHA-256 in both directions, zero leftovers/errors. This establishes
the pre-metadata transaction problem with the actual original client under
adverse delivery. It does not reclassify normal ViaForge FAIL runs as PASS.

First grouping build failed2/140 tests: default old decoder path attempted
unnecessary protocol lookup; embedded test receiver was after Netty's capture
handler. Fixed callback gating and receiver insertion; original assertions
retained. Failure log `edges-batch-forge-first-failed.txt` remains available.
Current full build session29008: 140 JUnit now pass, 108 Python pass in
`edges-batch-python.txt`. Forge smoke still running through49 profiles,
new report `edges-batch-client-smoke.txt`, output `edges-batch-forge.txt`.
After completion run `edges-batch-mixed-final` six flight rounds,
`edges-batch-direct-final` four rounds and1.17.1 `edges-batch-boundary-final`.
Source proof script updated to REQUIRE these normal runs PASS and exact current
source hashes; the historical FAIL and delayed-native FAIL remain included.
Matching sources/release manifest still pending. No final release claim.

## 17:00 final production built; live verification running

Full build/Forge smoke completed at16:59:43: `edges-batch-client-smoke.txt`
starts PASS,49 resource profiles, all5 modern queue smoke checks;140 JUnit and
108 Python pass. Current normal mixed endpoint run is session23716:
`interaction-26.2-edges-batch-mixed-final-1790348401039594300`, console
`build/logs/edges-batch-mixed-final.txt`, six full flight rounds/34 measured
cases. No relay, normal compression. Do not read live report.json.
After it finishes: direct26.2 four rounds,1.17.1 direct one round; names in
the proof script above. Audit completed packet logs with tracked tool
`tools/test-servers/edge_metadata_audit.py <run-directory-name>`.
No production code changes or parallel builds during these client runs.

## 17:15 final mixed PASS; direct repeat in progress

Normal final mixed run completed 34/34 PASS: zero flags, setbacks, login or
sequence issues; 48 flight-end transitions, none split by a movement tick.
Its 21 recorded production source hashes still match the working tree.
Final direct26.2 run `interaction-26.2-edges-batch-direct-final-1790348991581006900`
is owned by Python PID48084, server44552 and client44140, session67167;
console `build/logs/edges-batch-direct-final.txt`. Four flight rounds have
finished; repeated lids/columns are in progress. Do not read live report.json.
No unrelated client/server is running. Next: completed-report and metadata
audit, direct1.17.1 final boundary repeat, final documentation/build/source
archive and `build/flight-edges-release-proof.py`. No production edits since
the fresh140/108/49 verification. Original delayed-delivery FAIL remains an
explicit limitation and separate diagnostic, not a normal PASS.

## 17:18 direct PASS; older boundary finishing

Final direct26.2 completed26/26 PASS, zero Survival/login/sequence issues.
Metadata audit:32 flight ends, zero intervening movement ticks. Maximum
observed downward-landing residual6.0201887e-5, documented without claiming
bit-exact agreement. Its owned server/client exited; no user process stopped.
The final1.17.1 run is session51106:
`interaction-1.17.1-edges-batch-boundary-final-1790349371474586900`, console
`build/logs/edges-batch-boundary-final.txt`. Paper411, unchanged Grim ON;
all four flight cases finished, lids/columns pending. After completion audit
the report, record actual login issues separately, then build matching source
archive and verify `build/flight-edges-release-proof.py`. The earlier surface
report now explicitly identifies its release/runtime statements as historical.

## 17:26 boundary failure reproduced; ordinary queue correction building

Do not ship the17:18 candidate as fully verified. The final1.17.1 run ended
12/14 FAIL:6 Simulation,3 GroundSpoof,1 Survival setback;2 login corrections
separate. Ground sprintjump/ground jump failed; all lids/columns passed.
At1790349456843 metadata arrives and starts native decoding; Pong and native
metadata execute6844, but retained VF|entity is blocked until6861 after a
movement tick. Audit sees3 split flight ends of8. Original1.17.1 aqv uses
ConcurrentLinkedQueue. Directly decompiled1.12.2 bib uses a synchronized
ArrayDeque, while1.13 cfi and1.13.2 cft use an unlocked ConcurrentLinkedQueue;
the general-task boundary is1.13, NOT1.14 as provisionally assumed earlier.

Added CONCURRENT_CLIENT_TASKS from393, preserved general scheduling through
1.21.8, separate packet stage still starts773. The Minecraft task storage is
concurrent; older targets retain their synchronized producer path. Modern
ordinary producers bypass the old monitor. Original-packet outputs publish
as one ordinary task for393..772, or one dedicated task for773+. Join/leave
events follow the corresponding queue. No extra ticks or Pong delay.
New JUnit tests ordinary-stage atomic publication; expanded Forge test checks
actual transformed producer progress while the old monitor is held, FIFO,
single dispatch and disconnect release for every affected resource profile.

Build/full Forge running session39935, output `edges-general-forge.txt`, new
previously absent report `edges-general-client-smoke.txt`. Source manifest
includes the five exact new decompilations; live source recorder now22 files.
Next: finish141 JUnit/full Forge,108 Python; repeat1.17.1 four flight rounds
first, then normal26.2 mixed/direct. Preserve the failed old boundary report
and record new labels separately. Release proof/source archive still pending.

## 17:29 corrected general stage built; repeated boundary live

Build/full Forge completed:141 JUnit PASS,49 resource profiles PASS,39 actual
transformed concurrent-task checks PASS.108 Python tests PASS. Logs are
`edges-general-forge.txt`, `edges-general-client-smoke.txt` (fresh PASS),
`edges-general-python.txt`. No Java/Python remained before next client launch.
Current1.17.1 four-round test:session86986, label `edges-general-boundary-final`,
console `build/logs/edges-general-boundary-final.txt`,26 measured cases.
Do not read live report.json. Production frozen for this and following mixed
six-round/direct four-round26.2 repeats. Proof script requires all three
new runs PASS, exact22 source hashes,141/108/49, then source ZIP byte equality.
Failed `edges-batch-boundary-final` remains an explicit12/14 FAIL in proof.

## 17:33 remaining older-version timing failure; do not label it PASS

The general-stage repeat already has2 Simulation,1 GroundSpoof,1 Survival
setback (still running, complete counts pending). At1790350248656 Pongs were
sent while original metadata was still translating; native+retained metadata
both apply together at8673. Unlike the earlier failure, the two outputs are
now atomic and the producer is not blocked by the drain monitor. A flight
tick still fits between the pre-metadata Pong and complete translation.
First offset0.002615, then0.131070. This is the same timing pattern reproduced
with original26.2 under delayed delivery; it is NOT a native1.17.1 live proof.
Keep the source-correct general-stage changes, complete all cases and report
this1.17.1 boundary as FAIL with an open timing limitation. Do not repeat
until a lucky PASS or delay acknowledgements. No production changes while
the normal26.2 final repetitions finish. Release evidence must include this
current-source FAIL, not silently require/assume a passing boundary.

## 17:35 completed boundary FAIL; final mixed repeat running

General-stage1.17.1 finished25/26 FAIL: first low glide2 Simulation+1
GroundSpoof+1 Survival setback; all25 other cases PASS, zero login/sequence
issues. Packet audit32 flight ends,1 crossing a tick. Original/retained halves
are atomic at8673; pre-Pongs8656 while translation is pending. The final proof
explicitly requires this known FAIL with exact counts/case and current22
source hashes; it does not silently turn it into a successful boundary.
Final mixed26.2→1.21.11+ViaVersion now session6394, six flight rounds,
`build/logs/edges-general-mixed-final.txt`. After completion: direct26.2 four
rounds, final docs/build/matching source archive and proof. Native26.2 normal
34/34 and separate delayed-frame FAIL remain the comparison evidence.

## 17:43 current mixed26.2 PASS; direct final running

`interaction-26.2-edges-general-mixed-final-1790350525880193700`:34/34 PASS,
zero Survival/login/sequence issues.48 flight-end transitions, zero split
ticks. Maximum landing residual4.905774e-5, upward column6.85248e-9, lids0.
Owned processes exited; no unrelated Java/Python process before direct start.
Final direct26.2 session9391, label `edges-general-direct-final`, four flight
rounds/26 cases, console `build/logs/edges-general-direct-final.txt`.
The current1.17.1 FAIL remains disclosed. After direct completes: audit,
finish docs and runtime checkpoint, build matching sources, run proof script.
Nine original JAR hashes and30 inspection-file hashes freshly verified.
Existing stale SHA sidecars backed up as `previous-*.sha256` in the initial
artifact backup; proof will emit fresh JAR/source sidecars and manifest.

## 17:57 resumed; direct26.2 also FAIL, live matrix now complete

Actual HEAD still02001c8477a7e7da92d89eea83331c266507036f; all working changes
retained. No Minecraft/server/Python process remains. Direct final completed
`interaction-26.2-edges-general-direct-final-1790351013338383600`:24/26 FAIL,
53 Simulation,2 GroundSpoof,1 AntiKB;0 setbacks/login/sequence issues. The
failing cases are low glide round2 and ground jump round3. All shulker,
bubble and final-coast measured cases pass; intervening flags are still
included in the overall FAIL. Packet audit32 ends,2 split movement ticks.

First split:raw metadata1790351165172,Pong5173,still-gliding tick5176,
atomic native+retained metadata5190. Grim already STANDING, AntiKB+first
Simulation both offset0.0010887979, next Simulation0.033483715. Second split:
Pong1790351202073,tick2077,metadata2090. These support the known timing
condition; original26.2 delayed-network reproduction is evidence of the
mechanism, not a passing normal ViaForge certification. No new production
edits or repeat-until-PASS. Current results: mixed26.2 34/34 PASS,
direct26.2 24/26 FAIL,direct1.17.1 25/26 FAIL;83/86 individual cases pass.

`docs/FLIGHT-EDGES.md` now states both open direct failures prominently.
The artifact proof requires these exact FAIL counts/cases as well as all
current22 source hashes and original unchangedGrim/Survival conditions. It
labels the build as having known flight failures, rather than relaxing the
test harness.141 JUnit/108 Python/full49 Forge profiles (39 queue checks)
remain current; only documentation/evidence metadata changed since testing.

Release packaging follows in `build/logs/edges-release-build.txt`, with
`build/libs/flight-edges-release.json` as the final hash/status record. All
previous JAR/source/sidecar backups remain under the initial backup folder.
Remaining engineering work: isolate and reduce pre-Pong/metadata translation
timing without changing original acknowledgement semantics; native1.17.1
live comparison; intermediate1.21.2–1.21.8 swept block effects and untested
column/lid configurations. No universal flight compatibility claim or commit.

## 18:00 release built and evidence validated; known failures remain

Final build `edges-release-build.txt`:BUILD SUCCESSFUL in26s, unchanged
production. Artifact proof completed successfully:141 JUnit,108 Python,
49 Forge profiles/39 transformed queue checks;all22 current-source hashes
match the three live runs, and605 source archive entries match the worktree.
Release JAR is15,740,143 bytes, SHA-256
`83b2cd2f86e9961d7ba65ef8ddd781d196eecf2f0fae332c57f30738631c1e66`.
Both direct FAIL reports and the mixed PASS are included in
`build/libs/flight-edges-release.json`; its release_result explicitly records
known flight failures. Native delayed-network FAIL is separate. The matching
source ZIP is repacked to include this checkpoint; its final hash/size and
both fresh checksum sidecars are recorded in that manifest.
No running Minecraft/server/Python process or user world was left changed.
No commit, no server-side workaround, no check weakening, no flight feature
disabled. Remaining technical limits are listed immediately above and in
`docs/FLIGHT-EDGES.md`; the requested universal absence of flight flags has
not been achieved.

## 18:23 follow-up: fix requested, normal original comparison expanded

The user explicitly requires ViaForge-only failures to be corrected. The
previous release with known direct FAILs is backed up, including sources and
sidecars, under build/backups/flight-edges-20260925-followup/. No production
change in this follow-up yet; do not present the old candidate as fixed.

Fresh NORMAL original26.2/direct26.2 run:
interaction-26.2-edges-native-direct-normal-1790352201562044200:26/26 PASS,
zero flags/setbacks/login/sequence issues. Packet audit32 flight ends,0 split
ticks. This strengthens the obligation to fix the normal ViaForge failures;
the artificial delayed-native run is not a substitute for that comparison.

Added official1.17.1 input/observation agent (native_117_probe.py and
native-probe/Native117Probe.java), with original client SHA1 verified against
Mojang metadata and runtime names from official mappings. Initial test startup
attempts are invalid movement comparisons: ASM RETURN hooks required tracked
locals because original stack frames discard arguments, then CLI autojoin
raced initial resource baking. Fixed the observer with AdviceAdapter and use
the original ConnectScreen after the title screen finishes resource loading.
Current owned run session89784, edges-native117-normal, four flight rounds.
No user client was stopped; failed owned agents were identified by exact
run path plus javaagent commandline before termination. Servers cleaned up
by their harness. No profile or existing world was replaced.

MovementTrace now captures raw immutable bytes/timestamps on Netty and defers
JSON/hex formatting to the client observer. Need test whether previous
observer overhead amplified the timing failure; do not infer a production
fix from this diagnostic change alone. Next: valid native1.17.1 comparison,
ViaForge repeat with low-overhead packet capture, isolate/correct actual
production difference, fresh validation and release. Existing production
changes, pending diffs and all historical failed evidence remain preserved.

## 18:37 follow-up checkpoint: concrete velocity precision defect

Original1.17.1 normal comparison completed:
interaction-1.17.1-edges-native117-compare-1790353362224146200.
All16 flight cases PASS,32 flight-end metadata transitions,0 split ticks.
Overall FAIL23/26: the observer incorrectly marked an open shulker GUI as
unloaded (three cases); one Reset flight position had Timer,TimerLimit,
Simulation and2 setbacks. Do not conflate these with the clean flight cases.
The agent now caches mapped reflection and distinguishes loading screens
from container GUIs; a fresh run remains required.

ViaForge control with low-overhead packet observation is currently running:
session39546, interaction-26.2-edges-observer-direct2-1790353833470397400,
log build/logs/edges-observer-direct2.txt. Production is the prior candidate;
source hashes were recorded at readiness before the following edits. An
initial observer trial crashed at login because Netty4.0 lacks hexDump(byte[]);
the observer now uses its own hex formatting. Not a movement FAIL/PASS.

New proven production defect, independent of ACK timing: 1.21.9+ original
SET_ENTITY_MOTION carries LOW_PRECISION_VECTOR; ViaBackwards converts it to
1/8000 shorts, discarding original values and clipping large velocities.
At mixed-run1790350704456 client velocity became0.34775, while the original
wire value is about0.34779955. The next normal landing tick has residual
4.9057738067e-5. Original clients do not introduce that extra quantization.

Added OriginalVelocity: retain the decoded target vector at the1.21.9 codec
boundary, execute Via once, replace only its corresponding native velocity
packet with event37. One main-thread setVelocity, no second motion application
or extra physics tick. Ordinary older velocity packets remain native.
Added JUnit codec coverage and full compressed-pipeline smoke coverage,
including values beyond the legacy cap and exact native player application.
These edits are NOT BUILT/VALIDATED yet; wait for the active control client
to finish before replacing its development JAR. Next build/tests, direct and
mixed true-client repeats, native1.17 retry, then docs/evidence/release.
Current release is still the backed-up known-failure candidate. No user client
or saved world changed. No intentional Pong delay or Grim modification.

## 18:45 validated candidate; extended real-client run active

Build edges-velocity-build2.txt succeeded (143 JUnit tests);110 Python tests
pass in edges-velocity-python.txt. Full Forge smoke freshly starts PASS in
edges-velocity-client-smoke.txt:49 resource profiles, including the new full
pipeline velocity checks and real player application. Forge build log says
BUILD SUCCESSFUL; PowerShell's native stderr status must not be confused
with its actual Gradle result. The initial test compile failed because upstream
Vector3d extends java.lang.Record while tests compile for Java8; the test now
uses reflected component reads against the already downgraded Java8 classes.

Original bytecode boundaries1.21.8/1.21.9/1.21.10/26.2 are freshly verified
against Mojang hashes in build/inspection/flight-edges/velocity/sources.json;
source/inspection hashes added to docs/FLIGHT-EDGES-SOURCES.json. Exact
recorded vector Z=0.3477995483122749, formerly0.34775. Both1.21.8 legacy cap
and1.21.9 new format were directly inspected, not inferred from Grim.

Current owned live run session72053:
interaction-26.2-edges-velocity-direct-1790354571732530500.
Six flight rounds; log build/logs/edges-velocity-direct.txt. No issues so far
is NOT a final rating. First completed landing observations now have residual
below7e-8, versus the old roughly5e-5 quantization error. Await full run/audit,
then mixed26.2 and direct1.17.1 repeats plus cached original1.17.1 comparison.
No production changes after the validated build. Old artifacts preserved in
both initial and follow-up backups. Do not run the old release proof script:
it intentionally expects the former25/26 and24/26 failures and22 old hashes.
Final packaging/source hash and evidence manifest still need updating.

## 18:56 follow-up: velocity correction does not remove the timing failure

Completed current direct26.2 run interaction-26.2-edges-velocity-direct-1790354571732530500
is FAIL32/34:50 Simulation,2 GroundSpoof,1 Survival setback; no login/sequence
issues. Downward landing round2 and ground jump round6 fail. All lid/column
cases pass. Normal landing residuals now remain below7e-8; the velocity loss
is corrected, but the metadata race is independently still present. Audit48
flight ends,2 split ticks. This run must remain evidence, never be overwritten
by a later passing candidate.

Exact first split: metadata raw1498ms, native translated1498, retained output1499,
Pongs1499, still-gliding tick1501, both metadata pieces1515 (base1790354730000).
Second split repeats the same pattern at1790354895248/5249/5251/5265.

Original1.17.1 normal rerun active: session92233,
interaction-1.17.1-edges-native117-final-1790355103200923400,
build/logs/edges-native117-final.txt. Cached observer; loading check fixed.
No build or competing test runs during this comparison.

Additional production optimization pending build: observe already decoded
metadata directly at1.14/1.13 boundaries via passthrough/resetReader instead
of serializing and decoding it again. Retained data is copied before Via
mutates it. Translation executes once; packet order, Pong timing and native
queue stages are not changed. This removes verified redundant processing;
its effect on the intermittent flags is not yet established. New run source
manifest includes SwimmingPackets.java (26 hashes). Await native completion,
then build, complete smoke and new normal live repeats. A draft release proof
exists in build/flight-edges-followup-proof.py but still needs final run labels
and validation paths; do not use it to claim release completion yet.

## 19:05 follow-up validation and native result

Original1.17.1 final normal comparison is PASS26/26, including all reset and
container phases: zero Survival/login/sequence issues. Packet audit32 flight
ends,0 split ticks. Report interaction-1.17.1-edges-native117-final-1790355103200923400.
All test-owned processes ended normally. This comparison does not reproduce
the earlier normal ViaForge failure on1.17.1.

The first metadata build was interrupted during the usage-limit handoff;
edges-metadata-build-resume.txt completed successfully. Review then required
copying mutable cloud-particle arguments before the detached legacy rewrite.
edges-metadata-build-final.txt now BUILD SUCCESSFUL,143 JUnit tests. First
metadata Forge smoke passed49 profiles; current final smoke session17893 uses
edges-metadata-final-client-smoke.txt/edges-metadata-final-forge.txt and includes
the particle-copy safeguard. Await completion, then direct/mixed26.2 and
direct1.17.1 normal live repeats; no production edits during those runs.

Python initial edges-metadata-python.txt failed one existing web same-origin
test with WinError10053 during the build. Unchanged full rerun passes110/110
in edges-metadata-python-repeat.txt. No assertion or web behavior weakened.
Velocity-only failed candidate JAR, sources and development JAR preserved at
build/backups/flight-edges-velocity-20260925/ (additional to previous backups).
Final release manifest/sidecars remain old and must be regenerated only after
current live evidence and the matching final source archive are ready.

## 19:10: a second observer bottleneck found, current run retained

Metadata production candidate passes143 JUnit,110 Python and final49-profile
Forge smoke. Current normal direct run session30715:
interaction-26.2-edges-metadata-direct-1790355948299890500,
build/logs/edges-metadata-direct.txt. Already has flags; await full completion.

Important additional diagnosis: LiveBoatProbe.log is synchronized and writes
client.jsonl to disk for EVERY send/custom-payload receive and small wire
packet on Netty. The separate MovementTrace improvement did not remove this
older generic logger. Native observers buffer packet logs instead. At a new
failure, raw metadata arrives8750ms, retained payload only8752, while Pong8751
already lets Grim change flight state (base1790356020000). Disk I/O occurs
between those observations. Existing runs are not rewritten or called clean;
the observer's effect must be separated from production behavior.

LiveBoatProbe now captures immutable byte copies/timestamps into an unlocked
concurrent queue and formats/writes them on the observing client at tick END.
No packet timing manipulation, no input/physics changes, no lost log categories.
This is development-only; NOT YET BUILT. Finish the current run before building
the new development JAR, then repeat with identical production bytecode.
The next reports hash observer sources separately. Final proof labels need
adjusting; do not count an instrumentation correction as a production fix.

## 19:14 identical-production observer comparison started

Synchronous observer run completed FAIL22/34:114 Simulation,12 GroundSpoof,
1 AntiKB,15 Survival setbacks; zero login/sequence issues. Audit48 flight ends,
13 split ticks. One case additionally lacks15 predictions. All shulker and
column cases pass. This evidence is retained in the completed metadata-direct
report; no thresholds or acceptance criteria changed.

Development-only build edges-buffered-observer-build.txt succeeded. Bytewise
comparison of both development archives confirms only four LiveBoatProbe
classes changed; ALL production entries are identical and release JAR bytes
are identical. Proof:build/inspection/flight-edges/observer-bytecode-proof.json.
Previous development and release archives retained in
build/backups/flight-edges-metadata-sync-20260925/.

Current owned run session97943, edges-buffered-direct, six flight rounds:
build/logs/edges-buffered-direct.txt. Production is unchanged from the preceding
FAIL; do not claim PASS until completion. Afterwards mixed26.2 and direct1.17.1,
then current documentation/source archive/evidence manifest/checksums.

## 19:20 interruption recovery

Usage-limit handoff ended the harness/server/client during round5 of
interaction-26.2-edges-buffered-direct-1790356440632460700. Actual process check
found none running. Recorded events contain zero flags/setbacks, but the run
is INCOMPLETE, never PASS. Original report/logs/world retained. Added separate
interruption.json; saved current test settings as *.interrupted, restored the
original run/options.txt and run/ViaForge/viaforge.yml from *.before.

Fresh full run session75331, label edges-buffered-direct-final,
build/logs/edges-buffered-direct-final.txt, same production and observer JARs,
six flight rounds. No user process stopped and no previous world reopened.

## 19:28 first complete buffered comparison PASS

interaction-26.2-edges-buffered-direct-final-1790356809257724900:34/34 PASS,
zero Survival/login/sequence issues.48 flight ends,0 split ticks. All24 flight
cases,3 lid cases,6 swimming-column cases and final coast pass with active
unchanged Grim/non-OP Survival. Maximum landing residual6.914e-8. Against the
byte-identical production synchronous-observer FAIL22/34, this demonstrates
measurement perturbation; not an additional production fix. Median shared-
flags translation/dispatch observation drops1ms to0ms,95th percentile2ms to1ms.

Mixed endpoint now active session24060, label edges-buffered-mixed,
build/logs/edges-buffered-mixed.txt, six flight rounds,26.2 on1.21.11+ViaVersion.
Next direct ViaForge1.17.1/four rounds, then final Python check (harness hashes
were added), docs/sourceArchive and evidence manifest with exact sizes/hashes.
Current release JAR already built and smoke-validated; source ZIP/sidecars and
manifest are not yet final. No production changes planned during live runs.

## 19:33 mixed run has separate Timer failure

Active interaction-26.2-edges-buffered-mixed-1790357273384304400 remains running
session24060. In low-glide round4 after landing, END ticks jump from
1790357529337 to1790357529957 (620ms), followed by rapid catch-up ticks.
At1790357529970 Grim raises Timer; so far17 Timer,1 TimerLimit,3 setbacks,
zero Simulation/GroundSpoof/AntiKB. This is a measured stall/burst, not the
previous split-metadata mechanism. Root cause of the stall is unproven. Do
not call this run clean, and do not discard it if a fresh follow-up succeeds.
Normal originals also had separately recorded Timer problems in earlier
reset/login runs; that is not proof of the cause here. Finish/audit before
selecting further work. No timer manipulation or check filtering permitted.

## 20:58 resumed: boundary PASS, passive mixed diagnostic started

HEAD remains 02001c8477a7e7da92d89eea83331c266507036f, existing work preserved.
The mixed run completed33/34 FAIL:17 Timer,1 TimerLimit,3 setbacks; no Simulation,
GroundSpoof,AntiKB,PositionPlace,login/sequence issues.48 flight ends,0 splits.
Windows Kernel-General time-change events were absent for19:25-19:40; this
does not establish the620ms pause's cause. No GC trace existed for that run.

interaction-1.17.1-edges-buffered-boundary-1790362325112774000 completed26/26
PASS,0 Survival/login/sequence issues,32 flight ends0 split ticks, maximum
landing residual6.914e-8. Native1.17.1 final comparison also26/26 PASS. At20:57
no Java/Python processes remained. Normal worlds/accounts remain untouched.

Started interaction-26.2-edges-stall-mixed-1790362681800433700, session60008,
build/logs/edges-stall-mixed.txt. Same production, inputs and buffered observer;
edge_stall_probe.py only adds a Java8 GC/safepoint log and in-memory passive
UTC/monotonic host observations, no injected pause. Diagnostic directory:
build/inspection/flight-edges/stall-1790362681436954600/.
Next: audit completed result and pauses, fresh Python harness check, final
docs, sourceArchive, evidence manifest and release checksums. Source ZIP and
old sidecars/manifest are not yet current; production JAR already validated.

## 21:10 passive diagnostic reproduced additional stalls; profiling active

edges-stall-mixed completed32/34 FAIL:29 Timer,3 TimerLimit,2 Simulation,5
setbacks,0 login/sequence issues. All24 flight cases pass and48 flight ends
have0 split ticks. The two failed cases are bubble lift forward-sprint-jump
and bubble sink forward-sprint. The Simulation flags occur after Timer
cancellation/setbacks, with offsets1.371391 and0.653482; they are not ignored.

New passive evidence excludes long GC/safepoint stops and a whole-host pause
in this run: during Survival max JVM stopped time8.4994ms, host monotonic
sampling gap37.0061ms, UTC-vs-monotonic discrepancy<0.875ms. Client END gaps
reach2180ms. Flag clusters at1790363101251 and1790363111091 follow703ms and
599ms END gaps. Netty also stops logging arrivals during most of each pause.
The cause remains unproven; no physics/timer/Pong changes made. Audit:
tools/test-servers/edge_stall_audit.py; output in the preceding diagnostic
directory's analysis.json. Original helper source archived alongside it.

Started a targeted profiler repeat with the same6-round inputs and unchanged
production/development JAR: edge_stall_probe.py --jfr --label edges-stall-jfr
--flight-rounds6, session87373, build/logs/edges-stall-jfr.txt. Java8 native
Flight Recorder support was verified with a separate JVM before launch.
Run:interaction-26.2-edges-stall-jfr-1790363317821247000.
Diagnostics:build/inspection/flight-edges/stall-1790363317466183600/.
At21:10 this run is active, own isolated server and client only. Next inspect
thread/file/monitor profiles at any long gaps, then decide a justified fix.
Do not finalize the release as all-clear. Sources/sidecars/manifest pending.

## 21:19 final evidence and packaging checkpoint

edges-stall-jfr completed34/34 PASS,0 Survival/login/sequence issues,48 flight
ends0 split ticks. All24 flight,3 lid,6 column cases and final coast pass.
Survival max END gap220ms, JVM stopped75.1854ms, host sample interval35.5047ms.
No captured Client/Netty blocking event>20ms; native/NIO waits are not fully
covered by these events. The earlier long stalls did not recur, so the
recording cannot diagnose them retrospectively. Both earlier mixed FAILs
remain open evidence, not dismissed as Grim-only or relabelled PASS.

Current-production five-run matrix:159/162 measured cases pass, three runs
PASS overall and two mixed runs FAIL. Normal original comparisons are26/26
direct26.2,26/26 direct1.17.1 and34/34 mixed26.2. No production changes since
the19:03 build; this continuation added passive clock/GC/JFR diagnostic tools
and completed the direct1.17.1 and mixed repeats. User worlds preserved.

Fresh Python:110tests OK in build/logs/edges-followup-python.txt. The PowerShell
wrapper reports native stderr as NativeCommandError despite unittest's OK;
no test failed. Production remains the143-JUnit/49-profile-smoke validated
build, including39 transformed queue profiles. git diff --check is clean.
48 original archive/inspection hashes in FLIGHT-EDGES-SOURCES.json were
rechecked. At21:17 no Java process remained; no user client was stopped.

Release JAR:15,742,534 bytes, SHA256
8ecb5b3143884f78d1d13147b54706848c42ea14b1becd44c980d300154e9453.
All five previous artifacts/sidecars/manifest additionally preserved under
build/backups/flight-edges-before-final-package-20260925/ before packaging.
Final packaging uses build.bat sourceArchive and build/flight-edges-followup-
proof.py. The latter verifies every ZIP entry against the working source,
all current26 production/4 observer hashes, actual Grim conditions, reports,
and emits updated checksums plus build/libs/flight-edges-release.json. That
manifest's result must remain "Built with disclosed current live failures;
not an all-clear". It is the authority for final source-ZIP bytes and hashes.

Remaining work beyond the documented fixes: capture one of the long mixed-
endpoint stalls while profiling; identify the blocking call before changing
production timing. Intermediate1.21.2-1.21.8 swept effects and the broader
attribute/obstruction limits in FLIGHT-EDGES.md remain unverified. No commit
or external publication has been made.

## 21:50 checksum audit and NIO-inclusive passive sampling

Packaging found a genuine evidence bookkeeping mismatch: the first observer
proof referred to interrupted buffered run JAR b58c88..., whereas all five
completed buffered runs used8439f689679f9a72d89ad9f24fd498f51fdafcdd55b01b5910041f66e8bdc729.
The development JAR was regenerated before the final direct run. A later
local rebuild at21:36 has SHA b750de..., same entries but later ZIP timestamps.
No production-source difference was found. By changing only DOS ZIP header
times to19:20:54 (entries) and19:20:56 (META-INF directory), its entire archive
hash exactly matches the recorded8439f689... SHA. Recovered file retained as
build/inspection/flight-edges/ViaForge-tested-buffered.jar; timestamp-only
recovery evidence in tested-archive-recovery.json. All8341 non-directory
entries match the later JAR. observer-tested-bytecode-proof.json verifies
that the synchronous-control differences are only4 LiveBoatProbe classes;
release JAR remains byte-identical. Old proof and reports remain untouched.

The new sourceArchive had completed, but final manifest is not yet emitted.
Before closing the unresolved pause issue, started the same mixed6-round
input sequence with a passive stack sampler covering native/NIO call stacks
missing from JFR events. No class transformation or movement/network changes.
Samples remain in memory until JVM shutdown, including sampling-cost timing.
Source:tools/test-servers/native-probe/ThreadStallProbe.java, optional --stacks
on edge_stall_probe.py. Run interaction-26.2-edges-stall-stacks-1790365746732960400,
session45317, build/logs/edges-stall-stacks.txt. Diagnostic directory:
build/inspection/flight-edges/stall-1790365745954215800/. At21:50 active own
isolated server/client. Next inspect any repeated blocking stack and flags,
then refresh docs/source archive/checksums. Do not publish an all-clear from
the clean previous profiler run. HEAD unchanged, all pre-existing work kept.

## 21:58 final stack comparison and delivery

edges-stall-stacks completed34/34 PASS,0 Survival/login/sequence issues and
48 flight ends0 split ticks. Same production/observer bytes as preceding
runs; the b750de... archive differs only by ZIP timestamps from8439f689...
Passive sampler observes7142 Survival stacks for each of Client/Netty,
max sampling pass9.5207ms,p99=0.5952ms. Max END gap149ms, JVM stop11.2055ms.
No long pause recurred, so its origin cannot be inferred from this trace.
Identical idle-selector/frame-limiter stacks sampled repeatedly do not mean
a continuously blocked thread. Audit retained as stack-analysis.json in the
diagnostic directory above; GC/clock audit in analysis.json.

Final current-production matrix is193/196 cases across6 runs,4 overall PASS
and2 mixed overall FAIL. Both pause-related failed runs remain explicit and
unresolved; neither is discarded by the latest clean34/34 repetitions. Full
normal original matrix86/86. No speculative production timing change made.
At21:57 there are no Java/Python processes; options.txt and viaforge.yml are
byte-identical to the pre-test backups. Every world/backup remains retained.

Fresh final Python log:build/logs/edges-final-python.txt,110tests OK. Release
production remains the143JUnit/49Forge-profile validated build. Updated
sourceArchive log:build/logs/edges-final-sources.txt. Final evidence generator
uses the corrected tested-archive proof and all6 completed current reports.
build/libs/flight-edges-release.json records artifact bytes/checksums and
every source ZIP entry comparison; .sha256 sidecars match that manifest.
The earlier21:20 source ZIP/log are additionally backed up under
build/backups/flight-edges-source-checkpoint-20260925/ before final packaging.
Source size/hash are recorded in the manifest to avoid a self-referencing
source archive hash in this work log. No commit made. Remaining investigation:
the intermittent long pauses, intermediate1.21.2-1.21.8 swept contacts and
the other precisely scoped limits in FLIGHT-EDGES.md.
