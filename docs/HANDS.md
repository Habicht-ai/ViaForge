# Two hands on supported server profiles

The latest first-person crouching/crawling and rocket-model corrections and their
validation are described in [the September 20 regression report](REGRESSIONS-2026-09-20.md).

Supported multiplayer resource/protocol profiles now expose the original offhand
inventory slot, item use and rendering. Native 1.8 connections and singleplayer
retain their original inventory and controls. The profile tables in [BLOCKS.md](BLOCKS.md) and [MODERN.md](MODERN.md)
also apply here; protocol-identical patch releases share a resource profile.

## Controls and behavior

- **F** swaps held items. The key can be rebound under Controls.
- Right click tries the main hand, then the offhand if the main-hand action does
  not succeed. This includes blocks, entity interaction, shields, food, potions,
  buckets and bows. Mining and attacking use the main hand.
- The survival and Creative player inventories expose slot 45. Clicks, cursor
  transfers and Creative synchronization preserve original item IDs, NBT and
  durability. Shift-clicking a shield equips an empty offhand slot.
- The hotbar shows the occupied offhand, including item count and cooldown overlay.
- Skin Customization offers Main Hand: Right/Left on supported servers. This is
  saved as `left-main-hand` in ViaForge's configuration and sent in client settings.
- First-person rendering has separate equip progress, hand-specific swings and
  use transforms, the target models' left/right transforms, and one-handed maps.
  Player rendering displays both hands, shield blocking and the active bow pose.

Bows prioritize ammunition in the offhand, then main hand, then inventory.
The client predicts arrow consumption; server updates supply bow durability and
the actual projectile. Inventory changes, effect results, interaction acceptance
and combat remain server-authoritative.

## Rocket use and shield blocking

The version-specific player preview while gliding in Survival/Creative inventory
is documented in [INVENTORY-PREVIEW.md](INVENTORY-PREVIEW.md).

Successful rocket air use during server-confirmed gliding consumes the selected
hand's interaction. Native 1.8's firework item has no air-use result; previously
the client sent the use but omitted its animation and could fall through to an
offhand shield. The hand now dips on 1.11.1 onward, including Creative. From 1.15
(`ROCKET_USE_SWING`) it also swings, matching the original clients. The native
single-hand renderer and the two-hand renderer both receive the equip reset.
Stack consumption and rocket spawning remain server-owned. Grounded use and
cooldowns do not predict a successful boost.

26.3 predicts successful interaction swings locally. Its use packet authorizes
the server animation; sending the old SWING through Via would additionally send
the new PUNCH attack packet. `SERVER_OWNS_USE_SWING` suppresses that extra packet
only inside successful-use animation, with the temporary context restored in
`finally`. Normal attacking and mining retain their network action.

Shields use the original target `shield_blocking` first-person model transform
while held in use, then return to `shield` on release. This includes both hands
and left-handed main-hand preference. Third-person blocking follows the original
arm pose. From 1.20.3 (`SHIELD_FOLLOWS_LOOK`) the shield also follows head pitch
clamped to -80/+25 degrees and yaw clamped to -30/+30 degrees. Older profiles keep
their fixed blocking pose. Equipment removal clears the pose; normal/slim skin
sleeves follow the same arm angles. Native 1.8 and singleplayer remain gated out.

The version boundaries were compared against checksum-verified Mojang clients:
1.12.2/1.14.4 versus 1.15 for rocket use, 1.20.1/1.20.2 versus 1.20.4/1.20.6 for
the shield pose, and the unobfuscated 26.3 `Minecraft.startUseItem`,
`FireworkRocketItem`, `HumanoidModel` and `FirstPersonHandsAndItemsRenderer`.
Local inspection files are under `build/inspection/hand-actions/` and are not
shipped in the mod.

`HandUseSmokeTest` exercises actual `Minecraft.rightClickMouse`, the native
network mixin and target Via encoding. It checks both hands, main-hand
preferences, Survival/Creative rocket use, cooldowns, unsuccessful grounded
use, preservation of inventory references, and absence of unintended attacks.
It also checks actual GL transforms when raising/releasing/swinging a shield,
both arm models at extreme look angles, skin overlays and equipment removal.
The test runs within the flight fixture for each selected resource profile.
Live flight probes additionally use this real mouse input and report
`rocket_hand_animation`; `--reuse-running` leaves an existing server running.

Validation on 20 September 2026: 113 JUnit tests without failures/errors/skips,
40 passing Python tests, and a fresh full Forge `PASS` across all 49 registered
resource profiles at 14:34. Logs: `build/logs/hand-use-release-full.log`,
`build/logs/block-client-smoke-test.txt`, `build/logs/hand-use-python.log`.
Actual Vanilla-server flights passed on 1.12.2 at 14:35 and 26.3 at 14:38:
both report `rocket_hand_animation`, `boosted`, `attached_rocket_visual` and
`original_flight_sound_playing` as true, followed by successful water landing.
Reports: `run/test-servers/<version>/live-flight-probe.json`; earlier reports
were preserved as timestamped `.bak` files. The existing 26.3 server remained
running. Logs: `build/logs/hand-use-live.log`,
`build/logs/hand-use-live-26.3.log`. The first 26.3 client startup was interrupted
before login; the completed rerun supplies the result above.
These checks do not establish pixel-for-pixel equivalence in every camera pose
or compatibility with other rendering mods.

Current release including the subsequent inventory preview correction:
Current artifact and fresh validation: [regression report](REGRESSIONS-2026-09-20.md).
`build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`, 14,378,697 bytes.
SHA-256: `6f502476570b24e1b070697d9e821ae32bc31b09344e6905c1c0249ecc442d16`.
Development fixtures are excluded from the release JAR.

## Dropping from the hotbar

From 1.13.1 onward, the client removes one selected item for Q, or the entire
selected stack for Ctrl-Q. The original clients in the verified archives do
this locally; a modern server can suppress the corresponding slot echo because
it expects that prediction. Native 1.8 only sends the action, which previously
left a ghost shield in the hotbar on 26.2.

The inherited `PREDICT_HOTBAR_DROPS` rule enables this local inventory change.
The existing native method still sends exactly one original action through Via;
no extra Creative or click packet is sent. Offhand and neighboring slots remain
untouched, and later server slot updates override the prediction. Native 1.8,
singleplayer, unknown targets and profiles through 1.13 retain their old behavior.

Direct player-inventory updates normalized by Via to window `-2` now reach the
native main/armor inventory on supported servers. The 1.8 handler otherwise
ignores that window. They work even while another container is open; offhand
index 40 remains owned by its existing captured event.

`DropItemSmokeTest` invokes the real native player drop method in Survival and
Creative with a component-bearing shield and stackable blocks. The flattened
pipeline tests cover Q/Ctrl-Q, an empty hand, outbound action fields, server
rejection/restoration, empty slot updates and the modern direct player-inventory
packet. The final disconnect check verifies that native drop behavior is restored.

## Integration

The [shared compatibility pipeline](COMPATIBILITY.md) now selects features,
resources and both wire directions. Hand input emits internal operations; the
selected adapter performs target encoding.

`LegacyEntityPackets` captures the original player inventory's slot 45, full
46-slot contents and direct inventory slot 40 before ViaRewind omits the offhand.
Main-thread handling stores the item in an additional `ContainerPlayer` slot;
the native 40-slot `InventoryPlayer` array and armor indices remain intact.
Original arm animations preserve the selected swing hand. Entity destruction and
session cleanup remove the corresponding temporary state.

`HandActions` runs the native item implementations under a temporary active-hand
context and restores the selected main-hand slot in `finally`. Continuous item
use and use completion resolve the correct stack. Forge interaction hooks remain
available. `VF|hands` carries hand-aware native operations internally to Via's
1.9 boundary. The remaining translators encode the chosen target version, then
the normal connection applies compression/encryption. This private channel is
consumed inside the client; servers receive the original protocol packets.

Inventory clicks retain transaction IDs and the target mode field type. A swap
synchronizes the selected hotbar slot before sending action 6. Left/right main-hand
settings use the original client-information field. Later versions continue
through the flattened/component adapters; see [MODERN.md](MODERN.md).

## Verification

The Java 8 packet tests cover all ten profiles: direct/full offhand updates,
unrelated inventory windows, empty slots and both arm animation types. The opt-in
Forge smoke test described in [BLOCKS.md](BLOCKS.md) exercises the real native
network mixin and compressed Via pipeline, inventory/Creative interactions,
F swaps, both use packets, entity interaction, swing and client settings.
It also checks continuous shield use, food priority/fallback, both bow hands,
ammunition and disconnect cleanup.

Actual GUI tests click the Creative offhand slot, check armor/slot coordinates,
and compare Forge hotbar pixels with/without an offhand item. Renderer captures
are written to `build/logs/screenshots/offhand-<version>.png` and
`offhand-survival-<version>.png`, `offhand-creative-<version>.png`,
`offhand-hotbar-<version>.png`. The smoke report must begin with `PASS`.

These automated fixtures do not replace a live two-player server test of latency,
plugins or anti-cheat. Original model and GUI assets come from the verified
Mojang client archives; no downloaded client code is executed.

The eight 1.13?1.14.4 profiles also route real flattened inventory/equipment data
and both hand directions through these implementations. Their additional smoke
fixtures check modern item IDs, Damage, namespaced enchantments, JSON names/Lore,
slot 45/direct slot 40/full inventories, remote player active-hand metadata and
1.14's reordered block-use fields. Full GUI/pose fixtures remain the ten legacy
profiles; see [flattened validation limits](FLATTENED.md).
