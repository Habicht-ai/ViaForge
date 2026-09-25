# Low Elytra landings, swimming columns and repeated lid use

25 September 2026. Follow-up to [the flight/surface corrections](FLIGHT-SURFACES.md).
The new report names selected client 26.2 on actual 1.21.11 with ViaVersion.
Earlier surface PASS reports did not cover these input sequences.

Physics, ray picking and packet-stage corrections are implemented. The earlier
release candidate passed the reported mixed endpoint but failed direct flight
repetitions. Those failures remain evidence below. Follow-up work has found
an additional lost-velocity defect and measurement overhead. The current
production passes direct 26.2 (34/34) and direct 1.17.1 (26/26), with the
corrected passive observer. The latest mixed-endpoint run also passes 34/34,
but two earlier repeats remain overall FAIL (33/34 and 32/34) because of Timer
bursts after measured pauses; the second also has two subsequent Simulation
flags. The pause's cause remains open despite the clean repetition.
It is **not an all-clear for every flag or delivery schedule**.
An artificially delayed original-client failure does not excuse normal
ViaForge-only failures. Normal original-client comparisons pass on all three
listed endpoints. Failed earlier runs remain in the evidence and matrix.

## Follow-up: original comparisons, observation and velocity precision

Normal original26.2 on direct26.2 now also passes26/26 cases with zero flags,
setbacks or login issues:
`interaction-26.2-edges-native-direct-normal-1790352201562044200`.
All32 observed flight ends applied without a tick between the preceding Pong
and metadata. Together with the normal mixed original run, this is stronger
evidence than the separately labelled artificial delivery experiment.

The new official1.17.1 observer completed its first comparison:
`interaction-1.17.1-edges-native117-compare-1790353362224146200`.
All16 flight cases pass and32 flight ends have no split tick. Overall23/26 is
FAIL: three open-container cases were incorrectly labelled unloaded by the
observer, and one position-reset phase had Timer,TimerLimit,Simulation and
two setbacks. That reset is not excluded from the overall result. The observer
now distinguishes loading screens from container screens and caches mapped
reflection. Its fresh complete normal run,
`interaction-1.17.1-edges-native117-final-1790355103200923400`, passes **26/26**
with zero flags, setbacks, login or sequence issues, including every reset.
All 32 flight ends apply without an intervening tick. Earlier instrumented
startup failures are not movement comparisons. The earlier ViaForge 1.17.1
FAIL is therefore not dismissed as an original-client failure.

The old ViaForge packet observer serialized metadata and formatted JSON/hex
on Netty. It now captures timestamps and immutable bytes, deferring formatting
to the observing client thread. The control with unchanged production code,
`interaction-26.2-edges-observer-direct2-1790353833470397400`, passes26/26 with
zero issues and32 flight ends without split ticks. This indicates measurement
sensitivity; one clean control does not prove the previous race impossible.
The failed first observer trial lacked a Netty4.0 byte-array hex API and
crashed at login; the compatibility fix changes only diagnostics.

Original velocity changes format in1.21.9. The decoded vector was passed by
ViaBackwards through three legacy signed shorts, introducing a second1/8000
quantization and an inappropriate3.9 speed cap. The recorded mixed-case wire
`65 01 f9 ff ac 82 ff fd` contains Z=0.3477995483122749; ViaForge applied
0.34775. The following landing produced a4.9057738067e-5 prediction residual.
`OriginalVelocity` now retains values at the1.21.9 codec boundary and replaces
the corresponding native velocity output after Via executes once. Internal
event37 applies one native `setVelocity`, without an extra movement step.
Targets through1.21.8 retain their original short format. Original1.21.8,
1.21.9,1.21.10 and26.2 archives and bytecode are hashed in the source manifest.
This precision defect is distinct from the pre-metadata acknowledgement race.

That distinction is confirmed by the first velocity-corrected direct run,
`interaction-26.2-edges-velocity-direct-1790354571732530500`: **32/34, overall
FAIL**, with 50 Simulation, 2 GroundSpoof and one Survival setback. Downward
landing round 2 and ground jump round 6 fail; every lid/column case passes.
No login or sequence issues. Normal landing residuals fall below 7e-8, but
two of 48 flight ends still cross a tick. This is retained failed evidence.

The next candidate removes redundant metadata serialization at the 1.14 and
1.13 observers: already decoded Via fields are inspected and restored with
`passthrough`/`resetReader`, rather than encoded into a snapshot and parsed
again. Retained swim, pose and cloud data is copied before subsequent Via
mutation. Via transforms each packet once, in the same order. This changes
translation cost, not acknowledgement timing or the original packet stage.
Its completed direct validation and observer comparison are recorded below.

A further observer defect was found during that validation: the generic
`LiveBoatProbe.log` still held a monitor and wrote `client.jsonl` synchronously
on Netty for wire and decoded packets. Fixing only `MovementTrace` had left
this separate bottleneck intact. The native agents buffer their packet logs.
The general observer now captures timestamps and immutable wire bytes into a
concurrent queue; JSON/hex formatting and file writes occur on the observing
client thread at tick END. Packet forwarding, acknowledgements, inputs and
physics are untouched. This is a **test-instrumentation correction**, absent
from the release JAR, and must not be presented as a production physics fix.
The old synchronous-observer run,
`interaction-26.2-edges-metadata-direct-1790355948299890500`, is **22/34 FAIL**:
114 Simulation, 12 GroundSpoof, 1 AntiKB, 15 setbacks, and 13 split ticks
among 48 flight ends. All lid/column cases pass. The identical-production run,
`interaction-26.2-edges-buffered-direct-final-1790356809257724900`, is **34/34
PASS**, zero Survival, login or sequence issues, and zero split ticks among
48 flight ends. A bytewise comparison confirms only the four development
`LiveBoatProbe` classes differ; the release JAR is identical. Proof:
`build/inspection/flight-edges/observer-tested-bytecode-proof.json`.
The earlier `observer-bytecode-proof.json` names the interrupted first build.
Packaging checked the distinction: the first five completed buffered runs used
development JAR SHA-256 `8439f689679f9a72d89ad9f24fd498f51fdafcdd55b01b5910041f66e8bdc729`.
A later rebuilt archive differs only in ZIP timestamps. Restoring just those
header times reproduces the recorded whole-file SHA-256 exactly; the recovered
archive and timestamp-only recovery proof are retained alongside the new
entry-by-entry comparison. This does not modify the historical reports.

For shared-flags packets, median raw-receive-to-retained-output time falls
from 1 ms to 0 ms and the 95th percentile from 2 ms to 1 ms (millisecond clock
resolution; 132/133 samples). The direct run's maximum normal landing residual
is 6.914e-8; stationary glide and jump cases reach floating-point noise. This
comparison demonstrates that the synchronous observer materially perturbed
packet delivery. It does not prove that every possible delivery schedule is
safe. The first buffered run was interrupted during round 5 by the usage-limit
handoff; it contains no recorded flags but remains INCOMPLETE, not PASS.

The corresponding ViaForge 1.17.1 run,
`interaction-1.17.1-edges-buffered-boundary-1790362325112774000`, passes
**26/26**, including every Survival reset, with zero flags, setbacks, login or
sequence issues. All 32 flight ends have no split tick. The maximum landing
residual is 6.914e-8. The normal original 1.17.1 comparison is also 26/26 PASS.

## Separate Timer failure in the current mixed run

`interaction-26.2-edges-buffered-mixed-1790357273384304400` finishes **33/34,
overall FAIL**: 17 Timer, 1 TimerLimit and 3 Survival setbacks in low-glide
round 4. There are no Simulation, GroundSpoof, AntiKB or PositionPlace flags,
and no login or sequence issues. All 48 flight ends apply without a split
tick; the flagged case's maximum prediction offset is 6.231e-15.

The client is already grounded and no longer flying when successive END
observations jump from 1790357529337 to 1790357529957 ms (620 ms), followed by
ticks 3877 through 3887 within approximately 31 ms. First Timer: 1790357529970.
The wire observer also has a 605 ms receive gap. These observations establish
a pause and subsequent catch-up, but do not identify whether the client JVM,
host scheduling or another cause produced the pause. No GC/safepoint trace
exists for this run, and no system-clock adjustment event was found in the
corresponding Windows System log interval. The installed Grim timer checks
and their clock accounting were inspected; no threshold, timer speed, Pong
timing or movement packet behavior is changed to hide this result.

`edge_stall_probe.py` runs the same input harness with optional Java 8
GC/safepoint logging and a passive UTC/monotonic host trace. It injects no
pause, buffers host samples until completion and leaves acceptance rules
unchanged. That repeat,
`interaction-26.2-edges-stall-mixed-1790362681800433700`, is **32/34 FAIL**:
29 Timer, 3 TimerLimit, 2 Simulation and 5 setbacks; no login/sequence issues.
All 24 flight cases pass and all 48 flight ends have no split tick. Bubble
lift with sprint+jump and bubble sink with sprint fail. The Simulation
offsets (1.371391 and 0.653482) occur after Timer cancellations/setbacks;
they still count toward the result.

During Survival the maximum recorded JVM stop is 8.4994 ms, the maximum host
monotonic sampling interval 37.0061 ms and the UTC/monotonic discrepancy
below 0.875 ms. Client END gaps reach 2180 ms. The flag clusters follow gaps
of 703 and 599 ms. At both gaps, the long interval is between LIVING_BEGIN
and INPUT_BEFORE; the following physical travel takes milliseconds. The
evidence therefore does not support changing the bubble-force arithmetic
to address these pauses. JVM GC/safepoint stops and a whole-host scheduling
pause of the observed length are excluded in this particular run.
`edge_stall_audit.py` records the correlations and source hashes in
`build/inspection/flight-edges/stall-1790362681436954600/analysis.json`.
The additional unchanged-input run with Java 8 Flight Recorder,
`interaction-26.2-edges-stall-jfr-1790363317821247000`, passes **34/34** with
zero flags, setbacks, login or sequence issues; 48 flight ends, zero split
ticks. It injects no delay. In Survival its largest END interval is 220 ms,
largest JVM stop 75.1854 ms and largest host sampling interval 35.5047 ms.
The recording has no captured Client/Netty monitor, park or file operation
longer than 20 ms during Survival; this does not exclude uninstrumented
native/NIO waits. The earlier long pauses did not recur,
so this trace cannot retrospectively identify their cause. The recording,
extracted events and audits remain in
`build/inspection/flight-edges/stall-1790363317466183600/`.
This clean repetition does not change earlier FAILs. No speculative physics,
timer or acknowledgement change has been applied for the remaining stalls.

The final passive stack-sampling run,
`interaction-26.2-edges-stall-stacks-1790365746732960400`, also passes **34/34**
with zero issues and 48 flight ends without split ticks. It uses the later
archive whose 8,341 non-directory entries match the exact tested JAR above;
the different archive SHA is only due to ZIP timestamps. The separate agent
transforms no class and retains stack observations in memory until shutdown.
It collected 7,142 Survival samples each for Client and Netty threads. The
sampling pass costs at most 9.5207 ms (99th percentile 0.5952 ms). Maximum
END gap: 149 ms; maximum JVM stop: 11.2055 ms. The earlier long pauses again
did not occur. Repeated idle selector/frame-limiter samples are not evidence
of continuous blocking between samples. Correlations and hashes are in
`build/inspection/flight-edges/stall-1790365745954215800/stack-analysis.json`.
The tools preserve the unresolved failures instead of inferring their cause
from these two clean diagnostic runs.

## Reproduced differences and corrections

The clean archived ViaForge baseline reproduced 124 Simulation,
10 PositionPlace and 1 GroundSpoof flags, with 47 Survival setbacks. Four of
14 measured cases passed. Setup, resets and equipment phases are included
in this overall FAIL. Original 26.2 passes the same 14 cases without a flag.

* **Landing pose/input:** clearing the flight flag does not immediately clear
  the retained FALL_FLYING pose. Original LivingEntity.isVisuallySwimming
  includes that pose while the flag is false; LocalPlayer consequently slows
  one input tick before the post-travel pose update. The baseline incorrectly
  excluded this state from crawling. At the observed landing transition,
  original forward input is 0.29400003 versus ViaForge 1.0, with a Grim offset
  of 0.08918. `ServerElytraFlight` now retains flight pose independently of the
  flag. The input/pose timing remains unchanged. This uses CRAWLING_POSE from
  1.14 onward; 1.14.4 LivingEntity.bk/official mappings explicitly confirm the
  retained flight pose. Active flight never becomes crawling or gains an
  extra travel tick. The previous smoke's "never crawl after flight" assertion
  was wrong and has been replaced with the original transition assertion.
* **Swimming columns:** the old final-box scan lost the modern axis contacts.
  Entity records each collision-resolved movement and applies block effects
  after travel. Y is visited first; horizontal order follows requested X/Z.
  Start/intermediate/end cells, per-movement iteration limits, the precise
  contact gate and a shared visited set matter. Within a directional scan,
  Y is the outer loop. At a column's upper exit, interleaving inside and
  surface callbacks changes velocity caps; endpoint-only arithmetic was
  insufficient. `ServerBubbleColumns` observes real `moveEntity` results and
  consumes them at the existing post-travel stage. It never moves the player
  or sends another movement packet. PRECISE_BLOCK_EFFECTS begins protocol 773
  (1.21.9/1.21.10), verified against original 1.21.8 and 1.21.10. Version 1.21.8 already
  has axis movements, but lacks this precise-contact gate; it is not silently
  assigned the newer behavior.
* **PositionPlace:** while standing on the opening lid and looking down,
  ViaForge could report the hidden DOWN face at Y=64 although the player was
  above Y=65.2. The 1.8 voxel walker shortened the ray to the next cell face inside
  the extended lid. The original modern shape clip uses the full ray.
  `MixinExpandedBlockRay` preserves that origin for shulker boxes from 1.13.
  It corrects picking, not outgoing face packets or Grim. Both opening and
  closing lids have transformed-world ray regression checks.
* **Concurrent task processing (1.13+):** 1.12.2 still locks an ArrayDeque
  while draining tasks; original 1.13/1.13.2 use a ConcurrentLinkedQueue and
  an unlocked producer. Later BlockableEventLoop retains concurrent reception.
  `MixinConcurrentClientTasks` restores that producer behavior from protocol
  393. Through 1.21.8, packets and ordinary tasks retain their shared FIFO.
  A late 1.17.1 repetition exposed the missing older path: 12/14 cases passed,
  with 6 Simulation, 3 GroundSpoof and one Survival setback during ground
  launches. Three of eight flight-end updates crossed a movement tick. Native
  metadata ran at 1790349456844 ms; its retained flight flag was blocked until
  6861 ms. Both pieces now publish together into the ordinary task stage.
* **Dedicated packet-processing stage (1.21.9+):** the original `PacketProcessor` uses
  a concurrent FIFO before ordinary scheduled tasks. The 1.8 client locks its
  entire task queue while draining it; Netty cannot enqueue the next metadata
  packet during that drain. `ClientPacketTasks` restores the dedicated stage
  from protocol 773. Retained join/leave events use the same FIFO; disconnected
  retained payloads are released. Main-thread handlers and the existing Pong
  path remain intact. No extra ticks, acknowledgement delay, packet filtering
  to hide checks or changes to flight arithmetic are introduced.
  All outputs from one original packet are published together: its translated
  native packets and retained compatibility events cannot straddle a player
  tick. Unrelated metadata is no longer re-serialized by each block-only
  observer in the modern codec chain. Actual Via translation still traverses
  every required boundary exactly once.

The intermittent earlier landing failure had a Pong followed by another client
flight tick, while Grim already predicted standing travel (offset 0.002086,
then 0.173539). That establishes a state-timing disagreement. The queue lock
is a verified difference from the original and a plausible cause; that failing
run predates raw receive/application instrumentation, so its exact thread
interleaving cannot retrospectively be proved. Later raw traces contained no
bundle delimiters: missing bundle processing is not supported as its cause.
The concurrent-producer regression and fresh live repetitions are recorded
separately below; a finite clean repetition is not proof of every interleaving.

## Remaining metadata timing condition, reproduced in original 26.2

The queue-only normal mixed repeat (`edges-queue-mixed`) had 24/26 passing
cases and an overall FAIL:
14 Simulation, 2 GroundSpoof and one Survival setback, including resets. All
column and lid cases passed. In its two failing flight transitions the Pong
was sent before the retained flight-end flag reached the game thread. Packet
audit: 2 of 32 flight ends crossed a movement tick. A normal direct26.2 repeat
passed 26/26, with no issues and no split among 32 observed flight ends.
The normal original26.2 mixed comparison passed 34/34, with no issues and no
split among 48 flight ends. These are observations, not an exclusion proof.

A separate original-client diagnostic then delayed **one** flight-end metadata
frame by 60 ms in a loopback TCP relay. Native physics, input, handlers and
acknowledgements remained original. The same installed Grim build raised
45 Simulation and 1 GroundSpoof, zero setbacks, on that sprintjump and its
following coast. The other 13/14 cases passed; the overall run is a diagnostic
FAIL and is never included in normal PASS totals.

The native Pong was sent at 1790347860342 ms; a movement tick at 0379 ms still
had fallFlying=true; metadata applied at 0408 ms. First offset: 0.009137.
Both relay streams are byte-identical before/after forwarding (matching
SHA-256 and counts: 13,251,728 clientbound and 60,586 serverbound bytes), with
zero unforwarded bytes and no relay errors. Only network compression on that
isolated server was disabled for frame inspection. The relay creates, edits,
drops and acknowledges no packets, and is absent from the mod.

The pinned Grim `PacketSelfMetadataListener` schedules `isGliding` at the
transaction sent **before** metadata. That transaction can be acknowledged
while metadata is still in delivery/translation. The controlled original run
demonstrates the resulting false prediction. It does not prove that every
unshaped ViaForge flag has an exclusively server-side cause: translation adds
latency, and final normal repetitions remain necessary. ViaForge does not
delay Pongs, clear flight early, or weaken Grim to conceal this condition.

The earlier general-queue candidate also has a **normal-network 1.17.1
failure**: 25/26 cases passed, but the first low glide produced 2 Simulation,
1 GroundSpoof and one Survival setback. All other cases and all login phases
were clean. Its native and retained metadata now both apply at
1790350248673 ms; the pre-metadata Pongs were already sent at 8656 ms while
translation was still finishing. The packet audit sees one split among 32
flight ends. At that checkpoint this was an open 1.17.1 timing limitation,
not a PASS or proof of
an exclusively Grim-side fault in that version. Native1.17.1 had not yet run
at that checkpoint; the follow-up comparison is documented above.

That candidate's **direct 26.2** repeat likewise ended **24/26 overall FAIL**:
53 Simulation, 2 GroundSpoof and 1 AntiKB, with zero setbacks/login issues.
The failures are low glide round 2 and ground jump round 3. All shulker and
column cases pass. Two of 32 flight ends crossed a movement tick. In the
first case raw flight-end metadata arrives at 1790351165172 ms; Pong is sent
at 5173, the client finishes a still-gliding tick at 5176, and both metadata
halves apply at 5190. Grim already predicts STANDING. The AntiKB and first
Simulation report the same 0.0010887979 offset; the next tick reaches
0.033483715. The second transition has Pong at 1790351202073, a still-gliding
tick at 2077 and metadata at 2090. The audit preserves these traces and the
following coast flags. Original delayed-delivery evidence supports the timing
mechanism, but does not certify ViaForge latency or excuse this normal FAIL.

Evidence: `interaction-26.2-edges-native-delayed-frame-1790347771376020500`
contains the original client, packet/application trace, Grim predictions,
`network-relay.json` and all command/reset phases. The separate normal original
report is `interaction-26.2-edges-native-order-1790344911041719100`.

No check thresholds, bypass rights, configuration, input features or action
packets were suppressed. Existing rocket, slime, push, sneak, hand-use and
protocol-selection fixes remain intact.

## Vertical shulker movement in the original

Yes: the original 26.2 upward opening also has small rises and falls while
gravity, collision and lid pushes interact. One observed opening has Y values
65, 65.11, 65.16, 65.33, 65.3616, 65.31637, 65.4716, 65.5032, 65.46, 65.6132,
65.6448, 65.5. This is unlike the previously measured horizontal 0.11 steps.
Artificially smoothing the vertical player movement would change original
physics. The new correction addresses the bad ray face on repeated use;
it does not add visual interpolation to the physics.

## Live matrix

The harness uses fresh non-OP Survival accounts with no bypass permissions.
Actual Grim client version, protocol, brand, mode, verbose and enabled state
are recorded before/after cases. Grim 2.3.74-8eb5f28 is unchanged. The mixed
endpoint is Paper 1.21.11 build 132 with ViaVersion 5.12.0. Original 26.2 is
`vanilla`; ViaForge is `fml,forge`, both protocol 776. All endpoints bind to
127.0.0.1. Existing worlds and the normal server are untouched.

Each 14-case run covers four low/ground Elytra launch-and-landing sequences,
three repeated shulker sequences (24/24, 4/4, 2/2 open/closed ticks, eight cycles),
six active-swimming cases (up/down column with sprint, sprint+sneak,
sprint+jump), and final coast. Tests require real flight followed by landing,
actual swimming and observed column direction, repeated lid progression,
loaded/alive Survival client and sufficient Grim predictions. Missing
evidence fails even when there are no flags. All Survival reset phases count.

| Client / endpoint | Cases | Survival flags / setbacks | Login issues |
| --- | ---: | --- | --- |
| Archived ViaForge 26.2 / 1.21.11 + ViaVersion | 4/14 FAIL | 124 Simulation, 10 PositionPlace, 1 GroundSpoof / 47 | 2 corrections |
| Fixed ViaForge 26.2 / 1.21.11 + ViaVersion | 14/14 PASS | 0 / 0 | 2 corrections |
| Original 26.2 / 1.21.11 + ViaVersion | 14/14 PASS | 0 / 0 | 0 |
| Fixed ViaForge 1.17.1 / direct 1.17.1 | 14/14 PASS | 0 / 0 | 2 corrections |
| Queue-only ViaForge 26.2 / 1.21.11 + ViaVersion, four flight rounds | 24/26 FAIL | 14 Simulation, 2 GroundSpoof / 1 | 0 |
| Original 26.2 / 1.21.11 + ViaVersion, six flight rounds | 34/34 PASS | 0 / 0 | 0 |
| Queue-only ViaForge 26.2 / direct 26.2, four flight rounds | 26/26 PASS | 0 / 0 | 0 |
| Dedicated-queue candidate ViaForge 26.2 / 1.21.11 + ViaVersion, six flight rounds | 34/34 PASS | 0 / 0 | 0 |
| Dedicated-queue candidate ViaForge 26.2 / direct 26.2, four flight rounds | 26/26 PASS | 0 / 0 | 0 |
| Same candidate ViaForge 1.17.1 / direct 1.17.1 | 12/14 FAIL | 6 Simulation, 3 GroundSpoof / 1 | 2 corrections |
| Earlier general-queue ViaForge 1.17.1 / direct 1.17.1, four flight rounds | 25/26 FAIL | 2 Simulation, 1 GroundSpoof / 1 | 0 |
| Earlier general-queue ViaForge 26.2 / 1.21.11 + ViaVersion, six flight rounds | 34/34 PASS | 0 / 0 | 0 |
| Earlier general-queue ViaForge 26.2 / direct 26.2, four flight rounds | 24/26 FAIL | 53 Simulation, 2 GroundSpoof, 1 AntiKB / 0 | 0 |
| Velocity-corrected candidate / direct 26.2, six flight rounds | 32/34 FAIL | 50 Simulation, 2 GroundSpoof / 1 | 0 |
| Original 26.2 / direct 26.2, four flight rounds | 26/26 PASS | 0 / 0 | 0 |
| Original 1.17.1 / direct 1.17.1, four flight rounds | 26/26 PASS | 0 / 0 | 0 |
| Final production, synchronous observer / direct 26.2, six flight rounds | 22/34 FAIL | 114 Simulation, 12 GroundSpoof, 1 AntiKB / 15 | 0 |
| Same final production, buffered observer / direct 26.2, six flight rounds | 34/34 PASS | 0 / 0 | 0 |
| Same final production, buffered observer / 1.21.11 + ViaVersion, six flight rounds | 33/34 FAIL | 17 Timer, 1 TimerLimit / 3 | 0 |
| Same final production, buffered observer / direct 1.17.1, four flight rounds | 26/26 PASS | 0 / 0 | 0 |
| Same final production, mixed endpoint with passive pause logging, six flight rounds | 32/34 FAIL | 29 Timer, 3 TimerLimit, 2 Simulation / 5 | 0 |
| Same final production, mixed endpoint with passive Flight Recorder, six flight rounds | 34/34 PASS | 0 / 0 | 0 |
| Same final production, mixed endpoint with passive stack sampling, six flight rounds | 34/34 PASS | 0 / 0 | 0 |

Completed reports in `run/test-servers/`:

* `interaction-26.2-edges-baseline-final-1790288363588145600`
* `interaction-26.2-edges-fixed2-1790289287418449700`
* `interaction-26.2-edges-original-contacts-1790289558814348900`
* `interaction-1.17.1-edges-boundary-repeat-1790290075148972900`
* `interaction-26.2-edges-batch-mixed-final-1790348401039594300`
* `interaction-26.2-edges-batch-direct-final-1790348991581006900`
* `interaction-1.17.1-edges-batch-boundary-final-1790349371474586900`
* `interaction-1.17.1-edges-general-boundary-final-1790350145587375600`
* `interaction-26.2-edges-general-mixed-final-1790350525880193700`
* `interaction-26.2-edges-general-direct-final-1790351013338383600`

Baseline production was archived before edits and byte-compared; only the
input observer changed (`build/backups/flight-edges-20260925-initial/baseline-proof.json`).
The corrected maximum upward-column offset is 6.86e-9; downward is below 7.1e-15.
The earlier general-queue mixed run has a maximum landing residual of 4.906e-5. Earlier
candidate mixed/direct runs reached 5.613e-5/6.021e-5, also without flags. This is
not a claim of bit-exact agreement in every landing tick. Native physics and packet
paths were not replaced; the agent only sets real inputs and observes ticks.
That earlier mixed packet audit observes 48 flight ends with no intervening
tick between the preceding Pong and flag application. A finite run cannot exclude
the original-compatible adverse-delivery condition documented above.

An exploratory after-login fixture build caused TimerLimit; the fixture now
exists before login. The initial original run had 13/14 valid cases because
one glide never started. First correction passed 12/14 but produced 92 Simulation
flags on upward exits; it is explicitly superseded, not counted as a PASS.
An intermediate original 14-case PASS had five Timer/one TimerLimit at login;
the fresh contact-observed original run above has none. Logs remain available.
The first 1.17.1 run had no Survival issues but only 13 predictions in its short
stationary glide. It remains 13/14 valid, not a full PASS. The successful repeat
starts this older-version case at Y=72 instead of Y=69, yielding 26 predictions;
the requirement of at least 15 is unchanged.

An early extended pre-queue test exhausted the player's Survival health after
repeated legitimate falls: 15/26 valid cases, followed by death-screen input
flags. It is retained as FAIL. Extended tests now heal between flight cases,
equally for both clients; no movement attribute, damage rule or game mode is
changed. All healing/reset phases remain part of the Survival audit.

Original JAR download URLs, Mojang hashes, locally verified SHA-256 and exact
inspection-file hashes are in [the source manifest](FLIGHT-EDGES-SOURCES.json).
The ray boundary was checked directly in 1.12.2, 1.13 and 1.13.2 World methods.

## Scope and remaining limits

These results certify the listed cases, not every version or column layout.
Live original-client comparisons cover 26.2 on both direct 26.2 and 1.21.11+
ViaVersion, and 1.17.1 on direct 1.17.1. The final original 1.17.1 comparison
includes all resets and container cases, without the initial observer defect.
The intermediate 1.21.2–1.21.8 swept-effect implementation still needs its own
port/comparison; the new rule does not claim to fix that family. Extremely
large motion batches, obstructed/covered columns, custom attributes and all
lid obstructions are not live-certified here. The native world range remains
Y=0…255. Existing independently documented login/Timer problems remain open.

## Reproduction and build verification

Run from the repository in PowerShell, with no other Minecraft client:

```powershell
py -3 tools/test-servers/edge_probe.py --label edges-repeat --flight-rounds 6
py -3 tools/test-servers/edge_probe.py --client native --label edges-original-repeat --flight-rounds 6
py -3 tools/test-servers/edge_probe.py --version 1.17.1 --direct --label edges-older-repeat
py -3 tools/test-servers/edge_probe.py --direct --label edges-direct-repeat --flight-rounds 4
py -3 tools/test-servers/edge_stall_probe.py --jfr --label edges-profile-repeat --flight-rounds 6
py -3 tools/test-servers/edge_stall_probe.py --stacks --label edges-stacks-repeat --flight-rounds 6
```

Run these sequentially. The harness checks running clients, creates one small
isolated instance per run, backs up its world before fixtures, records actual
inputs/ticks/packets and every Grim event, and stops only its own processes.
The ordinary web profile list is not expanded. `--baseline` uses the archived
pre-correction development JAR with the updated input observer.

For the explicitly separate adverse-network original diagnostic, use
`--client native --metadata-delay-ms 60 --label edges-original-delayed`.
This is expected to expose the timing limitation; never count it as a normal
network PASS. `edge_metadata_audit.py <completed-run-directory-name>` correlates
Pongs, flight-end metadata and movement ticks and hashes the inspected logs.

Fresh JUnit: 143 tests, zero failures/errors/skips. Fresh Python: 110 tests,
all passing in `build/logs/edges-final-python.txt`. An earlier repeat during Forge startup encountered
Windows socket error 10053 in an existing HTTP authorization test; failed
logs are retained as `edges-python-socket-error.txt` and `edges-metadata-python.txt`. No production web code or
test assertion was changed; the subsequent full run passed.

Full Forge smoke completed at 19:05 local time on 25 September:
`build/logs/edges-metadata-final-client-smoke.txt` starts with PASS and covers 49 resource
profiles. It exercises actual transformed landing input, expanded-lid picking,
two column contact orders/caps, earlier slime/rocket behavior, Creative inputs
and existing protocol/hand/block regressions. It also checks the transformed
packet queue for all 39 affected resource profiles: main-thread FIFO,
exactly-once handling, ordinary producer progress while the old monitor is
held, and retained-buffer release on disconnect. JUnit checks concurrent
reception, ordinary-stage publication, and atomic publication through the
actual embedded Via decoder. The additional velocity checks exercise modern
wire decoding, exactly one retained output, older native short behavior, and
actual native player velocity without rounding, clipping or an extra move.
Output: `build/logs/edges-metadata-final-forge.txt`. These 49 profiles are not
49 live Grim runs. The first grouping build's two regression failures and
their fixes are retained in `edges-batch-forge-first-failed.txt` and the work log.

The earlier general-queue matrix had 83/86 passing ViaForge cases across
three runs; only its mixed 34-case run passed overall. Both direct FAILs remain
historical evidence. The original normal comparisons now pass 34/34 mixed26.2,
26/26 direct26.2 and26/26 direct1.17.1. The adverse-network original diagnostic
remains a separate FAIL. Current buffered-observer runs pass 193/196 measured
cases across six runs. Four runs pass overall; two mixed repeats fail
overall because of the pause-related issues above. This aggregate does not
turn either failed run into a PASS.

Build/release outputs are `build/libs/ViaForge-1.8.9-4.4.0-client.1.jar` and
`build/libs/ViaForge-1.8.9-4.4.0-client.1-sources.zip`. Their `.sha256` sidecars
and `build/libs/flight-edges-release.json` identify the delivered bytes,
matching source entries, source hashes for final runs, and both
PASS and FAIL evidence. The final manifest explicitly labels the build as
having disclosed live failures. The sources exclude local worlds/logs;
the live input harness and regression tests are included. Older artifacts
remain under `build/backups/flight-edges-20260925-initial/`.

At the final live-test audit (21:57 local on 25 September), no Minecraft,
server or Python process remained. Only owned test processes were stopped;
the normal server had already been stopped externally before resumption.
Worlds/backups were retained, no normal lab profiles were added, and no commit
was made. Build completion and exact artifact hashes are in the manifest;
the chronological [work log](FLIGHT-EDGES-WORK.md) retains intermediate failures.
