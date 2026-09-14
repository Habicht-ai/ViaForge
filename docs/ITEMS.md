# Versioned server items

The Forge 1.8.9 client registers representations of every standalone item added
from 1.9 through 1.12.2, alongside the existing block-item extension. They appear
in creative inventory only while connected to a supported newer server. Native
1.8 and singleplayer sessions retain their normal inventory entries and textures.

## Coverage

| Introduction | Items and variants |
| --- | --- |
| 1.9 | End crystal; chorus fruit and popped chorus fruit; beetroot and soup; dragon's breath; splash/lingering potions; spectral/tipped arrows; shield; elytra; spruce, birch, jungle, acacia and dark oak boats; dragon head; shulker spawn egg; Frost Walker I/II and Mending books |
| 1.10 | Polar bear spawn egg |
| 1.11 | Totem of Undying; shulker shell; separate donkey, mule, skeleton/zombie horse, elder guardian, wither skeleton, stray, husk and zombie villager eggs; evoker, vindicator, vex and llama eggs; Binding/Vanishing curse books |
| 1.11.1 | Iron nugget; Sweeping Edge I-III books |
| 1.12 | Knowledge book; parrot spawn egg |

The target formats of ordinary potions, spawn eggs, enchanted books and the oak
boat are covered too. Beetroot seeds and the sixteen 1.12 bed colors are registered
by `ClientBlocks`. Item ID 451 is unused. The ten protocol/resource profiles are
listed in [BLOCKS.md](BLOCKS.md).

The 1.12.2 profile represents 211 standalone variants: 36 drinkable potions,
36 splash potions, 36 lingering potions, 32 tipped arrows, 43 spawn eggs, eight new
enchanted-book variants and the other individual items. Incoming custom potion
effects/colors, entity data, names/lore, enchantments, shield patterns, recipe lists
and equipment damage are retained. Other enchantments on received books retain
the native book tooltip. Invalid/empty variants can be received without being
added to the normal creative list. The knowledge book is command-only, as in
vanilla, so the normal search contains 210 of these variants.

Creative categories follow the target release. End rods, chorus plants/flowers,
shulker boxes, beds, glazed terracotta, dragon heads and end crystals belong to
Decorations; observers belong to Redstone and dragon's breath to Brewing.
Materials use the separate Materials tab through 1.11.2 and Miscellaneous from
1.12, including pre-existing items such as iron ingots. Command/structure blocks,
structure void and grass paths have no normal creative entry. They can still be
received or obtained with server commands. Enchantment categories show only the
highest applicable level; search also includes Frost Walker I. Mending belongs
to Tools, and from 1.11 also Combat; the curses follow their 1.11 categories.
The underlying tab layout remains the 1.8 interface.
Within the tabs and search, entries use the target's numeric item-registry order
instead of local Forge IDs. Shulker boxes, glazed terracotta, concrete and beds
therefore form separate color groups, interleaved with existing items at their
vanilla positions. Native sub-item order is retained, including the reversed
banner metadata order. Old and new enchanted books share enchantment-ID/level
order; through 1.11.2 books follow the normal items, while 1.12 puts them at item
ID 403. Other mods' entries keep their positions. Disconnected/native 1.8 creative
lists are unchanged.

## Representation and interaction

`LegacyItemCatalog` records the vanilla wire ID, metadata, first protocol, maximum
stack size, durability and model name. `ClientItems` resolves these to Forge IDs.
`LegacyItemDefinition` shares the wire-identity contract with block items.

`LegacyBlockItemBridge` reverses Via's mappings on a copy for display, then rebuilds
the fallback before outgoing packets enter Via. Some ViaRewind/ViaBackwards item
mappings do not preserve original data exactly: examples include potion metadata,
shield durability, elytra names and empty display compounds. `LegacyItemSnapshot`
saves the item at those mapping boundaries and restores it when reversing that
layer. Temporary snapshots are grouped by layer and removed before the client or
server receives the item; the current count is retained when splitting stacks.
Native equipment with newer enchantments uses the same restoration path, retaining
the original enchantment order and NBT instead of substitute tooltip lore.

The normal packet path covers slots, full inventories, creative actions, container
clicks, equipment, item entity metadata, merchant trades and use-on-block packets.
All new actions use the main hand. Eating and drinking use native animations;
the server confirms hunger, consumption, bowls/bottles, effects and teleportation.
The bow can charge with only spectral/tipped arrows. Shields have their own active
model and blocking pose in first and third person. Other players' active-hand
metadata and offhand shields are retained, including release and equipment-clear
updates. Both standard and slim arms use the newer grip and target display
transforms. Elytra can be equipped in the chest slot and have zero
armor points, 432 durability, a broken inventory model and a worn wing model.
Dragon heads use their own inventory/world/worn geometry and can be picked with
the middle mouse button. The inventory head yaw follows the change in 1.11.1;
older profiles use the original 1.9 orientation. Shield patterns are composited
using the target textures.

Sweeping Edge (enchantment ID 22) displays on received books and swords. From
1.11.1, Combat offers level III and search includes I-III; older profiles do not
offer it. The sweep attack itself exists since 1.9. Its original server particle
packet now renders the four-tick animated sheet exactly once, without replacement
particles or a locally invented sweep on every swing.

Newer servers also use the original crosshair attack indicator and a game-tick
attack timer. The charged-attack icon appears from 1.11.1; earlier profiles show
only the recharge bar, with their original positioning. Server attack-speed base values and all three modifier operations
are retained; local weapon switches reapply the target's sword/tool speed. The
hand lowers and recovers using the original cubic charge curve. All 25 original
sword/tool models use target display transforms in first/third person and inventory.
Sword right-click no longer triggers the 1.8 blocking pose on these profiles.
The ViaRewind title cooldown replacement is suppressed only for supported native
profiles. Attack damage, cooldown penalties and sweep eligibility remain server-owned.

Worn elytra use a visible player's cape texture, falling back to the target's
default wings when the cape is unavailable or disabled in skin settings. The cape
layer is suppressed while elytra are worn. Wing pivots, pose smoothing and crouching
follow vanilla, with server-reported fall-flying flags used for wing spread.
Enchantment glint passes share the same pose. This does not add local flight controls.

`LegacyEntityPackets` carries original visual data through the native packet
queue after Via translation, including when Via cancels an unsupported cloud or
offhand update. `ServerEntityViews` applies it on Minecraft's main thread and
scopes it to the current world. Thrown splash/lingering potions retain their item
model, potion NBT and tint. Lingering area-effect clouds emit native spell
particles with the server's color, radius and waiting flag; partial metadata
updates retain the other fields. Native removal packets end the cloud, and a
world change clears the view state. Same-dimension respawns retain tracked
entities. Clouds have a particle-only renderer, without 1.8's fallback box mesh.

Textures and models come from the hash-verified, cached vanilla client archives.
The resource importer includes item textures and the required shield, elytra and
dragon textures. Model inheritance and 1.9 display transforms are converted for
the 1.8 renderer; generated icons and entity-backed models both use this path.
No Minecraft texture files are bundled in the release JAR.

## Projectile effects and Totem activation

Potion impacts use the original server event exactly once, before Via can generate
replacement effects. The 1.9/1.10 potion-type IDs and the 1.11/1.12 RGB/instant-event
formats are handled separately. Normal and instant bursts use their original spell
particles, colors, velocity distribution and eight splash-bottle fragments.
Tipped arrows retain server RGB (including custom colors), emit twice per flight
tick and once every five grounded ticks; ordinary arrows have no potion trail.
Spectral arrows use the original projectile texture and instant-spell flight trail.
Color expiration and entity removal remain server-controlled.

From 1.11, entity status 35 starts the original 30-tick Totem particle emitter and
plays Mojang's original use sound. The local player also sees the original 40-tick
item activation movement, rotation and scaling. The world particles use the target
atlas, animation frames, green/yellow colors, full brightness, friction and fade.
Explicit Totem particle packets are also supported. World changes and disconnects
clear the animation. The sound is fetched once into the local verified asset cache;
it is not bundled in the mod.

## Item cooldowns and dragon-head animation

For each supported 1.9-1.12.2 profile, original `COOLDOWN` packets preserve the
server item ID and duration through Via. The client blocks the item's local use
while it cools down and draws the original fading white overlay in inventory and
hotbar slots. Activating a block (for example, a chest or lever) still works with
a cooling item in hand, and use packets follow the target controller's ordering.
Cooldowns belong to the original item type, so moving stacks, changing their NBT,
or using a second slot does not bypass them. Updates replace the current duration;
zero removes it. Respawn, a new local player and disconnect clear the tracker.

The original client code was compared for all ten resource profiles, including
1.9.1, 1.9.2, 1.9.4, 1.10.2, 1.11, 1.11.2, 1.12 and 1.12.1. These timings agree
throughout the supported range:

| Item/action | Vanilla behavior |
| --- | --- |
| Chorus fruit | 32 ticks of eating; server starts a 20-tick cooldown after completion |
| Ender pearl | Client predicts 20 ticks immediately on use, including Creative; server updates override it |
| Disabled shield | Server sets 100 ticks; the client follows the packet rather than guessing the disable trigger |
| Other server-assigned cooldowns | Original item ID and duration, without a hard-coded item whitelist |

The separate attack recharge timer remains independent of these item cooldowns.

In the same versions, worn dragon heads animate from the renderer's limb-swing
phase, not elapsed entity age. Idle players, mobs and armor stands therefore do
not endlessly open/close their mouths. Child and villager head offsets follow
the original layer. Held, inventory and dropped head items use a fixed phase.
Placed dragon heads use a per-tile clock that advances only while powered by
redstone. Removing power freezes the current pose; restoring power resumes it.
The behavior applies to floor and wall placement without affecting other skulls.

The compressed-pipeline smoke run checks cooldown replacement/removal, independent
items/shared variants, slot changes, use-in-air and use-on-block, Creative pearls,
respawn and the exact expiration tick for all profiles. Pixel checks exercise the
native inventory overlay and its disappearance after expiration; screenshots are
written as `item-cooldown-<id>.png`. Renderer checks cover worn/idle/moving heads,
item rendering and powered/unpowered placed heads in all five orientations.

## Current limits

Local offhand inventory, controls and both-hand rendering are documented in
[HANDS.md](HANDS.md). This item extension does not supply Elytra flight input/physics,
firework flight boosts or a 1.12 recipe-book screen.
Modern boats now have original models, movement and two seats; see [BOATS.md](BOATS.md).
The mob compatibility extension is documented in [MOBS.md](MOBS.md). Thrown potions,
lingering clouds, tipped/spectral arrows, Totem activation and other players' offhand shields are supported as described
above. Other unsupported entities use the existing protocol translation where
available. The knowledge book sends
its recipe list to the server; the 1.8 client has no recipe-book UI. The worn Elytra
model covers standing/sneaking and server-reported wing spread, not full player flight rendering.
Enchanted shield glint remains a visual follow-up. This is not complete 1.12 client emulation.

The existing native item registry is also connected to 1.13?1.13.2 and
1.14?1.14.4 through bidirectional flattened item adapters. New aquatic/village
items still use Via fallbacks. See [flattened validation and limits](FLATTENED.md).

## Verification

The opt-in Forge smoke run described in [BLOCKS.md](BLOCKS.md) now also checks every
available standalone creative variant across all ten profiles through a compressed
Via pipeline. It checks item IDs, metadata/durability and full NBT in both directions,
including fresh creative stacks, ordinary clicks, custom names/lore, shield patterns,
enchanted books and knowledge-book recipes. It checks target models, broken/active
model selection, first-person scale/orientation/position/swing, eating/drinking,
shield use, new bow ammunition, equipment slots and hidden disconnected entries.
The entity tests send original packets through the compressed pipeline and native
handlers: potion item metadata, cloud spawn/RGB/radius/partial updates/removal,
same-dimension respawn, offhand shield equipment and active-hand changes. Actual
shield rendering matrices are compared against the original JSON for both hands,
idle/blocking/sneaking poses and both skin widths on all ten profiles. Tests also
check emitted particle density, bounds and color, the mesh-free cloud renderer,
and actual Creative-tab membership and enchantment levels.
The creative checks also compare the full decoration sequence from end rods
through end crystals, exercise the real screen's empty/filtered search and
scrollable grid, and verify the original order returns on disconnect.
The report must say `PASS`; Gradle's success status alone is insufficient.
The effect fixtures additionally check all 37 potion-type IDs, instant/custom RGB,
exactly one burst per event, actual arrow collisions and particle cadence, status
events, local/remote Totem activation, emitter/overlay lifetime and sound registration
through the same compressed protocol pipeline.

Review images from the actual Forge renderer are written to:

- `build/logs/screenshots/items-preview-1.12.2.png`
- `build/logs/screenshots/items-hand-preview-1.12.2.png`
- `build/logs/screenshots/entity-visuals-1.9.png`
- `build/logs/screenshots/entity-visuals-1.12.2.png`
- `build/logs/screenshots/lingering-cloud-1.12.2.png`
- `build/logs/screenshots/creative-order-1.12.2.png`
- `build/logs/screenshots/elytra-cape-1.12.2.png` (diagnostic cape pattern)
- `build/logs/screenshots/sword-swing-1.12.2-tick-14.png` (also ticks 0/4/9)
- `build/logs/screenshots/sweep-1.12.2.png`
- `build/logs/screenshots/potion-impact-1.12.2.png`
- `build/logs/screenshots/arrow-effects-1.12.2.png`
- `build/logs/screenshots/totem-1.12.2-tick-16.png` (also ticks 4/32 and version 1.11)

These fixtures do not replace live-server gameplay testing of latency, server
plugins and anti-cheat behavior.

Registry IDs, recipes, spawn-egg colors and model layouts were checked against
the original cached vanilla client archives for these profiles, including the
1.12.2 item, potion-type, entity, shield and dragon-head definitions. The archives
are read as data and reference bytecode; the downloaded client code is not executed.
