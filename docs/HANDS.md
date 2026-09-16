# Two hands on supported server profiles

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
settings use the original client-information field. Server versions above 1.12.2
are outside this implementation's scope.

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
