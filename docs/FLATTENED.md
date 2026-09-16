# Flattened compatibility families

## Implemented scope

The eight new exact registrations are 1.13 (393), 1.13.1 (401), 1.13.2 (404),
1.14 (477), 1.14.1 (480), 1.14.2 (485), 1.14.3 (490) and 1.14.4 (498).
They reuse the existing catalog and client features implemented through 1.12.2.
The ten earlier profiles remain registered. Native 1.8.9 and singleplayer retain
their original session gates, controls, registry visibility and resource behavior.

This restores the inherited blocks/items, colored beds, shulkers, offhand/shields,
boats, entity visuals, combat, cooldown and Totem implementations on these targets.
It does not add every aquatic/village block, item, entity or gameplay mechanic.
Unsupported content retains ordinary Via approximations.

## Real translation boundaries

`FlattenedProtocolAdapter` is owned by the connection. A mixin observes the actual
Via protocol pass at declared boundaries. Snapshots serialize typed fields and
the unread tail without consuming either; no packet protocol is run a second time.

- Before 1.13 is translated to 1.12.2, `FlattenedBlockData` preserves original
  block identities and color-bearing bed states. Cloud particle metadata is
  retained before ViaBackwards cancels it.
- Before 1.14 is translated to 1.13.2, `VillageBlockData` reads the new position,
  chunk and global entity-type formats. Exact inverse forward mappings distinguish
  inherited states from new blocks that Via approximates. A detached copy feeds
  the shared state store; the actual light cache and chunk translation stay in Via.
- Before the 1.12.2-to-1.12.1 step, normalized entity, metadata, inventory, sound
  and particle packets feed the established client event implementations. Event
  format 340 denotes internal data, while the session retains the real server ID.
- Retained events survive cancelled Via packets; native effects replace their
  corresponding fallback to avoid duplicate animations and particles.

`FlattenedItemDataAdapter` composes the connection's real item rewriters on
copies in both directions. `FlattenedItemSnapshot` supplements the existing
legacy snapshots with original flattened identity/NBT. Namespaced enchantments,
Damage, JSON names, shield pattern data and 1.14 JSON Lore survive round trips.
Live stack counts and damage changes are retained. The identity mixin corrects
ViaBackwards' lossy conversion of non-oak boats before native lookup.

Outgoing hands and boats enter the known 1.9 boundary and continue through the
installed target path. Inventory clicks and Creative packets retain target item
codecs. `FlattenedServerboundPackets` specifically replaces the bundled
ViaBackwards 5.11.0 structure conversion, which omits mirror/rotation fields;
later target layers still translate the resulting dedicated packet normally.

An explicitly unrepresented block update invalidates its previous stored state;
it must not leave a formerly supported block visible over Via's new fallback.

## Original resources

Every target has its own Mojang client archive and asset index, pinned by SHA-1
and size. No Minecraft images or client code are bundled into the mod JAR.

`FlattenedResourceConverter` imports singular `block/` and `item/` texture paths,
renamed block/item models, modern blockstate variants and changed texture names.
The checked-in alias table contains paths, not image data. Native pre-1.9 block
textures use the target archive as well as the inherited new blocks. The 1.13
32-column particle atlas is repacked for the native 16-column UV layout.

`VillageResourceConverter` adds the 1.14 renames and assembles particle,
explosion and sweep sheets from original individual sprites. Villager profession
layers are composed with the original plains/base images for the existing models.
Biome-dependent clothes and new hat geometry are still limited by those models;
this is not full 1.14 villager rendering.

The existing session epoch prevents late downloads from replacing a newer
connection's resources. Unload, disconnect and dimension changes clear retained
world data; Via continues to own and clear its separate 1.14 light cache.

## Verification and limits

The build includes 95 JUnit tests. The additional registration/snapshot tests
reject neighboring unregistered protocols and verify preservation of typed fields
and unread bytes. The earlier synthetic architecture fixtures do not establish
wire compatibility on their own.

`FlattenedPipelineSmokeTest` runs within Forge 1.8.9 using compressed real target
packet formats, the complete installed Via paths, production mixins, native
chunk/item decoders and the real model baker. Each of the eight targets checks:

- 482 distinct inherited block states; single/multi updates, all bed colors
  without tile color NBT, shulker block events and falling blocks.
- All inherited block/item definitions through inventory, click and Creative
  round trips, fresh picks, count changes, Damage and exact original NBT.
- Full/direct/slot-45 offhand updates, equipment and player hand-use metadata;
  inherited mob IDs, boat wood/oar metadata and two passengers.
- Cooldown IDs, cloud particle metadata, particle replacement, Totem and swing
  events; both outgoing hands and 1.14's reordered block-use packet.
- Structure mirror/rotation/flags/seed, original command/gateway/structure block
  entity NBT, chunk unload, stray updates, reload and dimension cleanup.
- Original target texture aliases, particle texels and all inherited baked
  block/item models; 1.14 additionally checks separate sky/block light and Lore.

The ten legacy profiles retain their broader native gameplay, GUI, pose and
render checks. The new profiles' wire fixtures are constructed test data in the
actual formats, not recordings from a live multiplayer session. A successful
Gradle exit alone is insufficient: `build/logs/block-client-smoke-test.txt` must
begin with `PASS`. `runClient` now also rejects a missing, stale or failed report
when `VIAFORGE_BLOCK_SMOKE_TEST` is set. Live multiplayer, latency, server plugins and anti-cheat have
not been certified by these checks.

## Later families through 26.2

The bundled ViaVersion/ViaBackwards 5.11.0 and ViaRewind 4.1.3 provide actual
translation paths from protocol 47 through 26.2 (776). The Forge smoke reports
the initialized paths and verifies the 26.2 endpoint (47 translation layers).
This establishes available protocol translation, not complete client emulation.
The upstream [ViaVersion release](https://github.com/ViaVersion/ViaVersion/releases/tag/5.11.0)
also documents 26.2 handling.

The subsequent adapter implementation is documented in [MODERN.md](MODERN.md),
including its exact protocols, current verification status and original resource
conversion. It preserves the inherited catalog across later chunk, registry,
metadata and item-component boundaries. It does not supply full modern world
heights, new aquatic/village content, swimming/waterlogging, all modern mob
geometry or every later gameplay mechanic. No unknown target is exposed as
1.12.2 or enabled solely because a Via path exists.
