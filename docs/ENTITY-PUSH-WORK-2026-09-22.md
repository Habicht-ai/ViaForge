# Entity push work checkpoint, 22 September 2026

## Delivery state

The push implementation is finished and the release build succeeded. Final
code passed 123 JUnit tests, 71 Python tests, the fresh 49-profile Forge smoke,
83 ViaForge live contact cases across six versions, and 32 official-client
comparison cases (1.12.2 / 26.2). Detailed per-case limits are in ENTITY-PUSH.md.
The extended boat check has 32 PASS, 2 INCOMPLETE driving cases after underwater
seat loss, 37 preparation observations and one unresolved AimModulo360 on
dismount. No Simulation flags or setbacks occurred in that extended boat run.
The pre-push baseline repeat did not reproduce the aim flag (10 driving PASS).
1.16.1 login decoder failure is reproduced with both builds and remains open.

No owned Minecraft client/server or test worker remains running. No user process
was stopped. run/options.txt and run/ViaForge/viaforge.yml match the initial
backup byte for byte. All worlds/backups from isolated tests are retained;
the 49 active profile manifests/worlds and player files were not modified.
No Git commit was created. Base HEAD and existing boat corrections are retained.

Release: build/libs/ViaForge-1.8.9-4.4.0-client.1.jar, 14,409,177 bytes,
SHA-256 0fa80654268c3e194ff89d862deaa4e1eadba266be7acc3cf1fc24018cc272e1.
Final source sync/evidence packaging scripts: build/push_evidence_final.py and
build/package_push_evidence.py; output manifests under build/libs/ViaForge-push-*.
Release build log: build/logs/push-release-build.log. The tested development JAR
retains SHA-256 82e56ddc209c0f67a308bc1887d804e4d429e791c4af81189e64c83652a92d14.
The release container hash changed during final packaging; production compile
tasks remained up to date. Always use the final release manifest/hash above.

Next independent investigations: capture local-player and outbound rotation
through the boat dismount/teleport transition on original and ViaForge clients;
resolve the 1.16.1 SET_ENTITY_LINK decoder failure; special modern mob poses and
vehicle chains still require their own tests. Do not call these issues fixed.

## Historical checkpoint entries

Base HEAD: 5c1f5c03f30a6bd77650fc76ea7b11a5ff88c108. No AGENTS.md found.
The original boat work was already committed by the user. Preserve it.
Previous release/source/development artifacts and options were backed up at
build/backups/entity-push-20260922-143245/. No user clients were stopped.
All new server cases use separate retained worlds and loopback ports.
No active world/profile/player data or Grim checks/thresholds were changed.

Implemented: local proximity push from other living-entity/remote-player ticks,
versioned original impulse, team collision rule retention, original mob UUIDs,
and original entity positions/deltas instead of the 1.8 1/32 fallback.
See ENTITY-PUSH.md for sources and behavior boundaries.

Evidence:
- 1.12.2 baseline: 80 contact ticks, no displacement, despite no Grim flags.
- Official 1.12.2: side displacement .5034405569740706, diagonal
  .5619279913606302. ViaForge precise-final matches both exactly; all 16
  contact/team/mode/real second-player cases passed, no flags on either account.
- Official 26.2 grounded run: all cases valid and no flags. The older failed
  preparation/reflection/fall-death attempts remain as failed evidence.
- ViaForge 26.2 precise-final: side result exactly matches .5034405593015625;
  diagonal differs because the existing per-axis motion cutoff is obsolete
  since 1.21.5. Source and real ticks prove the horizontal vector cutoff.
  Implemented this additional rule and three actual-tick regressions.
- Full Forge smoke push-full-smoke-d.txt: PASS over 49 profiles before the
  last horizontal-cutoff fix. 123 JUnit and 71 Python tests passed.

Update 16:03 local: final full smoke after the cutoff fix is PASS over all 49
profiles (push-full-smoke-final.txt), 123 JUnit tests. Python suite: 71 OK.
Final 26.2: 16 contact cases PASS, diagonal and static player displacements now
match the original (at most machine-precision difference). The main account had
31 login/preparation setbacks; the original peer had Timer/TimerLimit at login.
Neither is hidden by the contact PASS. 1.16.5, 1.17 and 1.21.5 each have 13 PASS;
1.9.4 has 12 PASS (NoGravity fixture unavailable there, explicitly skipped).
1.16.1 disconnects before contact with SET_ENTITY_LINK/ViaRewind decoder error.
Its provenance is pending a run with the archived pre-push production build.

Current owned sequences: build/push-ground-series.py finishing 1.12.2 with peer;
build/push-tail-series.py then runs the 1.16.1 baseline, fresh official 1.12.2
with peer, and extended 1.12.2 boats including reconnect. Logs are named after
these scripts/jobs under build/logs. Do not start a competing client.
build/push_evidence_final.py derives JSON/CSV without overwriting old reports.
It must be rerun after all jobs finish. Older official 1.12.2 Creative/Spectator
cases lack a settled before-mode snapshot and are excluded under the final gate;
the fresh original run resolves that observation gap.
Next: inspect those outcomes, finalize matrix and limits, build final release/
source checksums and evidence archive. Existing .sha256 files remain STALE until
the final delivery step; do not quote them as new artifact checksums.
Logs and JSONL: run/test-servers/push-*; no PASS from empty chat alone.

Update after the completed series: 83 final ViaForge contact cases PASS on six
versions. Fresh original 1.12.2: 16 PASS, exact static endpoints including player
contact; original 26.2: 16 PASS. 1.16.1 pre-push JAR reproduces the same decoder
failure, so it predates this patch. Config/options match the initial backup.
Extended boat regression completed: 32 PASS, one FAIL at Dismount (AimModulo360,
no Simulation, no setbacks), 37 setup/transition observations not asserted PASS,
and two INCOMPLETE driving/rest phases after immersion: the receiver loses its
seat underwater, leaving insufficient actual driver ticks/predictions.
That flag occurred after server dismount teleport with yaw -329.99933 while the
boat yaw was -689.9993. Existing logs do not record local-player packet yaw, so
do not invent an exact cause. A current isolated baseline run with the pre-push
production JAR is in progress (boat-push-baseline-aim-*), log
build/logs/boat-push-baseline-aim.log. Baseline-stage support was added to the
boat harness. It restores the current development JAR afterward. No production
code has changed since the full 49-profile PASS and the six-version live matrix.
Next: inspect baseline, honestly document AimModulo360 as open if unresolved;
finish build, source sync, scripts build/push_evidence_final.py and
build/package_push_evidence.py. Final artifacts/checksums not yet finalized.
