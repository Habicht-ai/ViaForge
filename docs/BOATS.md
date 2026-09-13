# Versioned boats (Forge 1.8.9 client)

The modern boat implementation is enabled only on the supported 1.9–1.12.2
server connections. Native 1.8 connections and singleplayer retain `EntityBoat`,
its original renderer, one-seat mounting and original movement.

## Appearance and control

All six wood variants use the target archive's original 128×64 textures, hull,
oars, pivots and UV layout. The renderer includes the depth-only water mask,
damage wobble and interpolated paddle animation. The modern collision box is
1.375 blocks wide and 0.5625 blocks high.

Only the first passenger predicts boat movement. Forward/backward thrust,
steering inertia, water height/buoyancy, underwater/flowing-water behavior,
gravity, block collisions and support-block slipperiness follow the original
boat implementation. Remote boats interpolate server positions over ten ticks.
Original double positions and relative coordinates at 1/4096 block precision
avoid ViaRewind's legacy boat height and yaw adjustments.

| Target | Version differences |
| --- | --- |
| 1.9–1.10.2 | Holding both steering keys activates both paddles; animation advances by 0.01 per tick with the original ×40 model phase. |
| 1.10–1.12.2 | The entity's NoGravity metadata flag is available. |
| 1.11–1.12.2 | Opposing steering keys cancel the paddle signals unless forward is also held. |
| 1.12–1.12.2 | Paddle phase advances by 0.3926991 per tick; original water/land rowing sounds play at the stroke threshold. |

The ten tested profiles are 1.9, 1.9.1, 1.9.2, 1.9.4, 1.10.2, 1.11,
1.11.2, 1.12, 1.12.1 and 1.12.2. Releases sharing a protocol use the
corresponding profile (1.9.3, 1.10/1.10.1 and 1.11.1).

## Passengers and placement

The original server passenger list supplies both seats directly. Each passenger
ticks once and rides the boat itself. Seat order determines the driver; delayed
entity spawns cannot promote the second passenger to driver. Transfers remove
the previous mount relationship, and destruction/world changes clear state.
Boarding aligns the local view with the boat and shows the dismount key hint.
View turning is limited to the original ±105 degrees. Animal seat offsets and
sideways poses also apply to the imported animals.

Driver movement, paddle state and passenger input enter Via at its 1.9 protocol
boundary, then pass through the remaining target translators, compression and
encryption. This bypasses the old client's synthetic paddle conversion. A second
player sends passenger input, including sneak to dismount, without controlling
the boat. Right-click interaction and final mount acceptance remain server-owned.

All six boat items use the original five-block placement ray, water offset and
collision check. Successful placement swings the hand in survival and creative;
only survival consumes the item locally. The server creates the boat, confirms
inventory contents, controls animal boarding and underwater ejection, and handles
damage, destruction and drops. No duplicate client boat is spawned.

## Verification

`build.bat build` runs the Java 8 unit tests and verifies the release archive.
The opt-in Forge client test from [BLOCKS.md](BLOCKS.md) also exercises boats
through the actual compressed Via pipeline on every profile. It covers original
spawns, all wood metadata, precision/teleports/corrections, passenger tick counts,
seat transfers, delayed spawns, real driver/passenger ticks, target movement/input
packets, version differences, water/ice and placement in both game modes.
Disconnect checks retain the native boat hitbox, renderer and disabled modern
item actions. Packet tests cover entity-ID reuse and dimension changes.

The report must begin with `PASS` in `build/logs/block-client-smoke-test.txt`.
Actual Forge renders are saved as `build/logs/screenshots/boats-<version>.png`.
The automated fixtures do not replace a two-client live-server session for latency,
server plugins and anti-cheat behavior. Versions after 1.12.2 are outside this
boat implementation's scope.
