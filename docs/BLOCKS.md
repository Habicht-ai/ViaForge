# Versioned server blocks

The implementation targets Forge 1.8.9 connecting to vanilla-protocol
servers with the explicit profiles below and in [MODERN.md](MODERN.md), retaining the catalog implemented through 1.12.2. This is an incremental block backport, not complete
client emulation. Block items and the standalone items introduced through 1.12.2
are included; see [item implementation and limits](ITEMS.md). The longer-term design
covers modern blocks and additional interaction mechanics.

## Profiles and scope

| Connection protocol | Server releases | Default vanilla block resources |
| --- | --- | --- |
| 107 | 1.9 | 1.9 |
| 108 | 1.9.1 | 1.9.1 |
| 109 | 1.9.2 | 1.9.2 |
| 110 | 1.9.3, 1.9.4 | 1.9.4 |
| 210 | 1.10, 1.10.1, 1.10.2 | 1.10.2 |
| 315 | 1.11 | 1.11 |
| 316 | 1.11.1, 1.11.2 | 1.11.2 |
| 335 | 1.12 | 1.12 |
| 338 | 1.12.1 | 1.12.1 |
| 340 | 1.12.2 | 1.12.2 |
| 393 | 1.13 | 1.13 |
| 401 | 1.13.1 | 1.13.1 |
| 404 | 1.13.2 | 1.13.2 |
| 477 | 1.14 | 1.14 |
| 480 | 1.14.1 | 1.14.1 |
| 485 | 1.14.2 | 1.14.2 |
| 490 | 1.14.3 | 1.14.3 |
| 498 | 1.14.4 | 1.14.4 |

For 1.13/1.14 see [flattened data conversion](FLATTENED.md). Later registrations,
verification status and the Y=0..255 native height limit are in [MODERN.md](MODERN.md).
New aquatic/village blocks are not added to the inherited native catalog.

`LegacyBlockCatalog` covers every new vanilla block ID in the range: 198â€“252 and
255, including all color variants. End rods, connected chorus plants and flowers,
purpur pillars/stairs/slabs, end bricks, beetroot crops, grass paths, frosted ice,
command blocks, gateways and structure blocks begin in 1.9. Magma, wart blocks,
red nether bricks, bone and structure void begin in 1.10; observers and shulker
boxes in 1.11; concrete, powder and glazed terracotta in 1.12. IDs 253/254 are unused.
The existing command block ID 137 uses its directional/conditional 1.9 model and
the same editor as repeating and chain command blocks on supported servers.
The existing bed ID 26 is also extended with all 16 colors from 1.12 onward.
Its inventory item remains server ID 355 with color metadata 0-15; older profiles
keep the original red bed and the native creative entry.
Unknown or invalid states retain Via's existing fallback.
Protocol IDs are matched explicitly, so newer or unknown protocols never silently
select an older profile. Shared-protocol patch versions use the listed default;
an exact patch-resource override is a possible later enhancement.

## Data flow

`CompatibilityDecodeHandler` subclasses Via's decoder and delegates wire details
to the selected `PacketAdapter`. See [COMPATIBILITY.md](COMPATIBILITY.md) for the
shared registry, inherited rules, data formats and resource lifecycle. This keeps capture after
decryption/decompression even when the existing compression handler reorders Via.
It retains Via's sharable-handler contract so Netty permits removing and reinserting
the same instance during compression setup. `handlerAdded` still enforces ownership
by the original connection's channel; block state cannot be shared across channels.
The handler reads a duplicate of the untouched PLAY buffer before translation,
then restores supported local states in the resulting 1.8 chunk and block update
packets. The actual protocol connection remains on the selected server version.

`LegacyBlockPackets` handles the old and new 1.9 chunk formats, joins, respawns,
single and multi-block updates, unloads, explosion removals, falling blocks and
block events. New-block events bypass Via's cancellation/replacement and use
Forge's actual local block ID, allowing shulker lid events to reach their tile entity. Original states
are copied into `LegacyBlockWorld`, with one connection-owned store on its Netty
event loop. Full chunks replace old sections; partial chunks preserve others.
Missing sections of a partial chunk remain unknown. Unloaded chunks, dimension
changes and channel closure discard the relevant state. Stray block updates do
not allocate new columns. A decoder error logs a warning and disables restoration
for that connection, retaining Via's output.

Normal Via packet cancellation is not a decoder error. In particular, 1.12's
recipe and advancement packets may be discarded between chunk loading and block
placement confirmations. Clearing the state store here caused Purpur to turn into
quartz and shulkers to use Via's replacement blocks after placement. The decoder
now retains the store on cancellation. Regression fixtures insert actual cancelled
recipe traffic before single and multi-block updates and cover all inherited
Purpur states and all 16 shulker colors/facings on their supported profiles.

The target attack-indicator and empty-offhand icons wait for the current resource
pack before binding their textures. This avoids missing textures during the
asynchronous join/rejoin interval; the llama inventory uses the native horse
background until its target resource arrives.

Block IDs inside the source store normally use **server ID << 4 | metadata**.
Colored beds use a private range `4096 + (color << 4) + metadata` after merging
the chunk's bed block entities or subsequent type-11 block-entity updates. This
range never goes onto the server wire. Matching head/foot halves share color;
occupied-state updates preserve it, while removal/unload discards it. Native
block updates replace the bed tile packet before Via removes its color data.
Local IDs are
resolved from Forge's live `Block.BLOCK_STATE_IDS`, not hard-coded. Do not confuse
this encoding with `Block.getStateId`, which uses a different bit layout in 1.8.
The native 1.8 world receives local block states in its ordinary packets; rendering,
collision, selection, neighbor checks and local block changes therefore see the
same representation. There is no second render-only world.

`ClientBlocks` registers local blocks, ItemBlocks and beetroot seeds. Creative
entries, pick-block and item use are gated by the active multiplayer profile.
No recipes or world generation are registered. Double slabs pick the single slab;
frosted ice and gateways have no inventory items. Structure blocks exist in 1.9,
but their inventory item is only enabled from 1.10.

`LegacyClientBlockTypes` supplies rod orientations, connected chorus collision,
age states, slab halves/merging, pillar axes, paths, ice slipperiness, observer and
command direction/state and glazed terracotta placement. Growth, redstone,
damage and other simulation remain controlled by the server. Shulker tile entities
animate their lids from server events and update their collision bounds.
Colored beds retain head/foot, direction, occupancy and the 9/16 collision height,
and implement the newer landing bounce. Both halves are placed by server updates.
The beetroot crop implements `IGrowable` so vanilla bonemeal returns successful
use and triggers a hand swing at ages 0-2. Age 3 rejects growth; the client does
not predict crop growth or consume bonemeal locally.

`ClientBlockItemRewriter` runs at the native 1.8 edge of the protocol pipeline.
It covers slot/content packets, inventory clicks, creative slots, held items,
item entity metadata, trades and placement stacks. `LegacyBlockItemBridge` uses
the active Via layers' reversible item mappings to recover the server identity
and original NBT. Outgoing local items are first encoded into Via's fallback form;
the normal serverbound translation then restores the original server ID/data.
This also works for newly picked creative items without pre-existing Via tags.
Unknown/newer profiles never receive local Forge item IDs.
The native placement packet retains ViaRewind's signed `BYTE` for the clicked
face, including its `-1` use-in-air sentinel. An unsigned typed wrapper field has
the same wire width but fails the following protocol's typed read.

## Resources and lifetime

`ServerSession` owns resource activation on Minecraft's main thread. Download
completion and delayed channel closure are checked against the active connection
identity, so an old connection cannot replace or clear a new connection's pack.
World unload clears the active resources, including pending download activation.
Profile changes rebuild block models, the texture atlas and its render consumers
after draining existing chunk compilation, without restarting the sound engine.
Native resource-pack reloads still use
Minecraft's normal reload flow.

Target resources are prepared during connection setup and cached for reconnects.
The loading screen waits for the final atlas, so the world is not exposed with
temporary textures. Modern PNG transparency is normalized for Java 8 before atlas
baking. Shield/banner bases moved in 26.x; 26.2 beds now use their original block
models and composite item placement instead of the removed bed entity atlas.
Water tint uses original biome colors from 1.13 onward. See
[resource lifecycle and water retention](COMPATIBILITY.md#resources-and-lifecycle)
and [render regression coverage](MODERN.md#validation).

`BlockAssetCache` downloads only the pinned Mojang client URLs listed in
`assets/viaforge/block-versions.json`. Size and SHA-1 are checked before use;
downloads use temporary files. A matching official launcher archive can be reused.
Archives are read as ZIP data; no downloaded code is loaded or executed. Only
block textures/models/states, item model definitions, shulker/portal textures and
the seeds/structure-void icons and bed entity textures are exposed to the importer. Model inheritance is
flattened by `LegacyModelConverter` because the 1.8 loader rejects simultaneous
`elements` and `parent` fields. Item rotations are converted to 1.8's Y/X/Z order,
and right-hand item transforms are adapted to 1.8 names.
`LegacyBlockModels` converts chorus multipart definitions into the 64 connected
variants and keeps face-texture references resolvable by the 1.8 loader.
`MixinRenderItem` applies the imported GUI transforms with the newer coordinate
system for registered block items, avoiding 1.8's additional rotation/half-scale.
Vanilla items retain their original rendering path.
`MixinItemRenderer` uses the newer first-person placement and swing transform
for these items, so 1.8 does not apply a second `.4` scale to imported hand models.
Shulker items keep their generated entity-textured geometry while inheriting the
original target item's complete `display` section, including parent models such
as `item/shulker_box` and `item/template_shulker_box`. This preserves the original
third-person scale of `.375`, first-person scale of `.4`, GUI scale of `.625`,
ground scale of `.25`, and their rotations/translations for all 16 colors. The
rotation conversion runs once, after resolving inheritance. World block geometry
is independent of these item transforms.
`MixinDroppedServerItem` removes 1.8's extra ground half-scale and compensates
the item renderer's geometry half-scale after applying the target display
transform. Otherwise dropped imported items become four times too small, and
their display translations are also reduced. This applies only to imported
items in an extended server session; native stone and disconnected rendering
retain the original 1.8 scale.
For Forge-generated flat items, the bobbing height reads the perspective model's
actual scale rather than its empty legacy camera fields. Dropped Purpur, seeds,
shields and swords are compared with original target transforms on all profiles.

`VersionBlockPack` overlays `minecraft:textures/blocks/` so existing blocks use
the target textures, while new block models and their dependencies use the
`viaforge` namespace. Known 1.8 block models are not globally replaced with
incompatible later blockstate formats. User/server resource packs keep higher
priority for vanilla textures. Before a profile is ready, the registered new
blocks have working vanilla fallback models. Asset failures retain those models
and report an error; reconnecting retries.

Mojang assets are not bundled in the mod or source ZIP. The local cache lives
under the game directory's `ViaForge/block-assets` and is covered by the ignored
`run/` directory in development.

## Verification

Command blocks have an editor for the command, previous output, output tracking,
impulse/repeat/chain mode, conditional execution and redstone/always-active mode.
Structure blocks have SAVE/LOAD/CORNER/DATA editors from 1.10 onward, including
save/load/detect-size actions, offsets, size, mirror/rotation, entities, integrity,
seed, custom data, selection boxes and air/structure-void markers. The 1.9 structure
placeholder has no functional editor. The editors require creative mode; the
server continues to enforce operator permissions and command-block settings.

Complete type-2/type-7 block-entity updates and chunk NBT reach the client before
Via can discard editor fields. Controls wait for server data; Cancel sends nothing.
Submissions use native `MC|AutoCmd` / `MC|Struct` payloads through the existing Via
pipeline. Commands, structure I/O and world changes are executed only by the server.
Changing command mode retains tile data; removing or unloading its block discards it.

`build.bat` runs the Java 8 tests and verifies both packaged JARs. Block tests
cover intermediate-version codecs, original-state preservation, restoration,
metadata, negative coordinates, light/biome retention, full/partial chunk
replacement, air updates, unloads, dimension changes, explosion removals,
resource filtering, model inheritance, decoder buffer/lifecycle handling,
repeated handler reinsertion and rejection of cross-connection handler reuse.

The opt-in Forge smoke test uses a real client renderer, the full Via translation
pipeline with compressed packets and repeated compression-handler reordering,
native Minecraft packet/chunk decoding, local client worlds and Mojang
block resources. It checks all registered block and item models, including baked
face textures; inventory/creative/click/NBT round trips; native item decoding;
actual first-person scale/position at rest and during swings; native right-click
packets on every face with empty/vanilla/custom stacks and use-in-air; opening,
reading, clicking and closing shulker containers on supported profiles;
all bed colors through chunk/tile updates, native bed halves/collision/pick and
creative version boundaries; every baked bed vertex, UV corner and face winding
against the native entity ModelBox geometry in all four directions, both occupied
states and the complete item model; actual Minecraft bonemeal right-click/hand swings;
real command/structure GUI activation, delayed NBT, controls, cancel, input limits,
mode changes and all editor fields after compressed translation to each target protocol;
rod/slab/seed placement; the 64 chorus connection/collision patterns; stair corners;
shulker lid events and collisions; and falling concrete powder. It does not log in
or connect to a public game server.
It is packaged only with development support:

```powershell
$env:VIAFORGE_NO_PAUSE = '1'
$env:VIAFORGE_BLOCK_SMOKE_TEST = Join-Path (Get-Location) 'build/logs/block-client-smoke-test.txt'
./build.bat runClient
Remove-Item Env:VIAFORGE_BLOCK_SMOKE_TEST
```

Read the report's `PASS`/`FAIL` result; Gradle's exit status alone does not indicate
whether runtime assertions passed. The client shuts down after the checks.
Renderer previews are saved under `build/logs/screenshots/`, including
`block-hand-preview-1.12.2.png` with identical FOV for each held item.
`beds-preview-1.12.2.png` compares all 16 bed world and inventory models.
`block-editors-preview-1.12.2.png` shows the command and structure editor screens.
`ShulkerItemRenderSmokeTest` checks all 16 colors against the unconverted original
item JSON and its inherited display transforms for every registered target with
shulkers (1.11 through 1.14.4). It compares actual Forge renderer matrices for both
third-person hands, normal/slim arms, sneaking, both first-person model transforms,
GUI, fixed model transforms, and dropped item entities. Native stone is checked
before joining, during extended sessions and after disconnect. Review images
`shulker-items-<version>.png` show actual player-held and inventory models for
1.11, 1.12.2, 1.13.2 and 1.14.4.

`shulker-boxes-<version>.png` shows closed, half-open and fully open boxes in all
six orientations for profiles from 1.11 onward. A pixel comparison verifies that
their interior faces remain visible regardless of the preceding renderer's face
culling state. As in vanilla, both sides of the shell are rendered and face
culling is re-enabled afterward. The shell uses vanilla's 0.9995 inset around
the block center to avoid depth fighting with the supporting block. Rendering
that supporting surface before and after the box must produce identical pixels.
`levitation-inventory-<version>.png` exercises the native inventory effect list
with the original levitation icon, name and remaining duration.
The first run may download each profile's assets. Normal `run.bat` launches are
unaffected when this environment variable is absent.

Before considering a server release fully supported, also test gameplay on real
servers: adjacent original/fallback blocks, building and mining with server
corrections, walking all stair directions and corners, chunk reloads, Nether/End
transitions, reconnection, resource reload and return to singleplayer. Test with
OptiFine/Patcher separately when present. Synthetic and renderer checks do not
establish full gameplay or third-party-mod compatibility.

## End gateways and crystals

End gateways use a tile renderer with the original projected, animated portal
layers and distance-dependent detail, rather than a baked texture cube. Only
exposed faces are drawn; those faces determine the native violet portal-particle
count. Original chunk and tile NBT preserve gateway age, and server block events
trigger the activation beam. Creation lasts 200 ticks. Through 1.10.2, cooldown
beams last 20 ticks and extend up to 25 blocks; from 1.11 they last 40 ticks and
extend up to 50 blocks. Colors and interpolation follow the target release,
including the changed dye colors in 1.12. Teleportation remains server-controlled.

End crystals retain the original server `ShowBottom` flag and optional beam target.
The native crystal model selects its base accordingly; a player-placed crystal
therefore has no unwanted bedrock pedestal. Beam targets can be changed or cleared
independently, and removal/world changes discard the visual metadata. Textures
come from the target's cached client archive. Native 1.8 rendering is unchanged.

The Forge packet tests cover crystal metadata changes/removal and gateway
updates, chunk age, cooldown lifetime and exposed-face particle counts for every
profile. Renderer comparisons are saved as `end-crystals-<version>.png`,
`end-crystal-beam-<version>.png` and `end-gateway-<version>.png` for 1.9 and 1.12.2.

## Next milestones

1. Verify real-server interactions and third-party rendering compatibility. Chorus
   multipart conversion selects a deterministic variant from weighted decorative
   alternatives, so small surface details may differ from native random choices.
2. Expand the explicitly registered 1.13-26.2 adapters beyond the inherited
   catalog, preserving the separation between server states, local
   representations and resource profiles.
3. Extend world storage/rendering/light/position handling for modern dimension
   heights. Via's legacy height clipping must be addressed before claiming
   complete 1.18+ world support.
4. Extend the existing standalone-item, combat and [mob](MOBS.md) implementations
   to additional verified server versions.

Protocol support alone does not imply correct rendering or behavior for every
block, and future versions require verified profiles rather than an automatic
"all newer versions" fallback.
