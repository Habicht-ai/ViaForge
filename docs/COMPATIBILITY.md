# Compatibility pipeline

The connection's server version, packet codec, client behavior and resource
release are independent. Existing implementations through 1.12.2 are reused by
explicit later adapters. These registrations retain the inherited catalog; they
do not implement every later release's content. Unknown targets keep normal Via
connectivity without borrowing an older parser. See [flattened families](FLATTENED.md)
and [modern families, validation status and limits](MODERN.md).

Boat behavior has separate [live Grim evidence and limits](BOAT-GRIM-2026-09-21.md).
Earlier player/Elytra PASS reports did not cover boats. The 2026-09-21 boat fix
restores original passenger/vehicle input timing in the shared `ServerBoat` path;
it does not resolve the previously recorded 26.2 player movement discrepancies.

## Connection and packet flow

`CompatibilityRegistry` resolves an exact server protocol to an immutable
`CompatibilityProfile`. The profile contains:

- The real negotiated `serverProtocol`, retained throughout the connection.
- Cumulative `VersionRules`: enabled `ClientFeature` modules, content availability
  and named `ClientRule` overrides.
- A `ResourceProfile`: exact vanilla asset release and resource-layout converter.
- A `ProtocolAdapterFactory`: supported capabilities, connection-owned inbound
  codec/state, bidirectional item identity conversion and outgoing operations.

The resolved profile is stored on Via's `UserConnection`. Both packet directions
use that object. A target mismatch after a server change discards the old selection.

```mermaid
flowchart TD
    A[Original server PLAY packet] --> B[Selected PacketAdapter: preserve original data]
    B --> C[Existing Via protocol translation]
    C --> D[Adapter: restore client states and retained events]
    D --> E[Shared client implementations]
    E --> F[Native 1.8.9 world, inventory and renderer]
    E --> G[Outgoing hand / boat / inventory operation]
    G --> H[Same target adapter and item conversion]
    H --> I[Via translation, compression and encryption]
```

`CompatibilityDecodeHandler` owns transport concerns: compression reordering,
channel ownership, packet validation, buffers, Via cancellation, restoration,
failure fallback and closing. It has no legacy packet IDs or resource selection.
`LegacyProtocolAdapter` contains the existing block/entity wire readers and
connection-local `LegacyBlockWorld`. `LegacyServerboundPackets` contains the
1.9-boundary hand/boat encoders previously embedded in input classes.

Adapters inspect original data **before** Via can replace/drop it. Retained events
survive a cancelled Via packet, while events with a native implementation replace
the fallback to avoid duplicate effects. Packet methods borrow their input and
transfer ownership of returned buffers to the shared decoder. An adapter failure
clears its cached state and retains normal Via translation for that connection.

## Reuse and version overrides

`LegacyCompatibility` builds the current family cumulatively:

1. 1.9 supplies blocks, items, entities/mobs, two hands, boats, combat and cooldowns.
2. 1.10 adds its catalog revision and crystal-beam rule.
3. 1.11 adds Totems and changed gateway, boat, shulker and entity-name rules.
4. 1.11.1 changes the dragon-head item orientation and attack indicator.
5. 1.12 adds its content and changed colors, Creative categories, oar cycle and
   illager arm model.

Patch profiles reuse those rules while selecting their own wire layout and
resources. `VersionRules.derive()` copies the parent; changing the child cannot
mutate it. A later family can retain two hands, boats and other implementations,
then override individual rules. Features declare dependencies, and profile
construction rejects capabilities that the selected adapter cannot supply.

Runtime modules use `ServerSession.has(feature)` and named behavior rules.
`contentSince(revision)` is reserved for introduction revisions of existing
catalog entries; it is **not** a network protocol comparison or codec selection.
The deprecated `ServerBlockSession`/`BlockPreservingDecodeHandler` constructors
remain only as compatibility entry points for older fixtures.

## Shared client data boundary

The current client item catalog has a stable internal legacy ID/data namespace.
`ItemDataAdapter` translates server identity/NBT into that namespace before native
registration lookup, and converts it back before serverbound encoding. Forge's
local registry IDs must never reach the remote endpoint. Existing item snapshots
still preserve information before individual Via item rewriters discard it.

`ClientEventEnvelope` declares an internal `ClientEventFormat` separately from
`ServerSession.profile().serverProtocol()`. Current formats retain the established
1.9?1.12 payload layouts so the existing entity, inventory and effect consumers
remain reusable. A newer adapter can emit normalized events in that format; it
must translate metadata indices, IDs and sound registry entries first. It must
not write a modern server protocol number onto an unconverted payload and let
legacy consumers guess its layout. Entity behavior uses the actual session rules.

This is a staged normalization boundary, not a claim that every future block or
entity is already represented. New content still needs client registrations and
rendering/interaction support. The legacy adapter's fixed-height state store is
not imposed by the generic PacketAdapter interface.

## Resources and lifecycle

Resources are converted into the renderer's expected layout before publication.
They are selected explicitly for each target, independently of inherited behavior.
The original verified download/cache machinery remains in use.
Server address resolution starts preparation early. PNG conversion and generated
models run off the render thread; the last two prepared releases are cached for
reconnects. The first position packet keeps a cancellable loading screen open until
the target atlas is ready, while network processing continues. First-time asset
downloads still take time. A failed preparation reports an error instead of silently
showing fallback textures. Native/unsupported targets do not use this loading gate.
`SessionEpoch` issues a fresh ticket for every join, even on the same connection.
A background resource completion is applied only if its exact ticket is current.
Disconnect and server changes invalidate pending work; a late close from an old
connection cannot clear the new session. World unloading clears the shared client
entity, item, hand, cooldown and boat state as before.

PNG grayscale samples and grayscale/RGB color-key transparency are expanded to
RGBA before any converter decodes images: Java 8 otherwise loses transparency or
changes gray brightness. Banner/shield masks and bases also become RGBA because
the native layered texture renderer skips other image types. Resource publication
invalidates both the GL textures and their banner/shield design caches.

`WaterColors` retains original 1.13+ biome colors before Via collapses biome IDs.
It reads numeric and hexadecimal registry colors, keeps the visible Y=0..255
window aligned with negative source heights, and handles later biome update
packets even when Via cancels their native translation. Immutable columns are
read by chunk render workers; the main thread schedules affected chunk rebuilds.
Unload, respawn and connection closure discard the columns. `BIOME_WATER_COLORS`
is inherited from 1.13; native 1.8 and legacy targets retain their original tint.

## Registered flattened families

`FlattenedProtocolAdapter` observes the actual `AbstractProtocol.transform` pass.
The 1.13 boundary retains original flattened blocks (including bed colors) and
cloud particles; the subsequent 1.12.2 boundary emits the established internal
client events after Via has normalized entity/item/metadata/sound IDs. The
1.14 boundary additionally retains original block identities before approximate
mappings, using `VillageBlockData`. Chunk light remains owned by Via's original
translation. No stateful protocol is replayed on a probe connection or packet.

`FlattenedItemDataAdapter` uses the connection's bidirectional item rewriters on
copies. `FlattenedItemSnapshot` and the existing legacy snapshots retain original
NBT and identity across lossy steps. A targeted structure-packet conversion fixes
the missing mirror/rotation fields in the bundled ViaBackwards version.

Profiles inherit the earlier behavior and select exact verified archives plus
`FlattenedResourceConverter` or `VillageResourceConverter`. Texture paths, block
variants, parent/texture references, renamed item models and particle atlases are
converted from the target's original resources before publication.

ModernBlockFamilies adds explicit preservation boundaries through 26.3 with the
source chunk codec, inverse forward mappings and metadata/block-entity layout.
ComponentItemSnapshot retains original component patches across structured item
changes, including Via's original hashes for inventory clicks. The native item
bridge only reverses the lower layers to recover the internal 1.12 identity;
modern item translations remain in the actual packet pipeline.

Resource converters compose chest UV conversion, renamed block/item paths,
GUI sprite atlases, equipment, item definitions and individual spawn eggs.
See MODERN.md for exact protocols, test status and the native world-height limit.
No nearest-version fallback or automatic enabling of unknown formats is used.

## Verification

`CompatibilityArchitectureTest` checks independent target/resources/codec/rules,
immutable inheritance and overrides, capabilities/dependencies, unknown/native
versions, item copy semantics, connection-bound outgoing routing and stale resource
tickets. `CompatibilityDecodePipelineTest` uses a deliberately synthetic wire
format to check preservation before loss, restoration, cancellation, buffer
ownership, disabled-codec fallback and decoder reordering. These are architecture
fixtures, not a claim of real 1.13 support.

The existing Forge smoke suite now creates `CompatibilityDecodeHandler` through
the same registry and adapter factory as production for all registered legacy and flattened/modern resource profiles.
Its actual chunk, item, entity, hand, boat, UI, effect and model checks remain in
place. Read `build/logs/block-client-smoke-test.txt`: it must begin with `PASS`.

`FlattenedCompatibilityTest` verifies exact registration and non-consuming packet
observation. `FlattenedPipelineSmokeTest` uses actual compressed target packet
formats through every installed Via layer, the real Forge mixins and native
block/item decoders. It checks original archives and baked models, separate 1.14
light, offhand/player/mob events, both item/hand directions, structure packets,
block entity NBT, unload and dimension cleanup. See `FLATTENED.md` and `MODERN.md` for the precise
coverage and the distinction from live server gameplay.

The vehicle regressions send the native `VF|boat` payload through every target
pipeline, including movement, both paddles and all 36 direction/jump/dismount
combinations. The 1.9 `PLAYER_INPUT` flags must be a typed `BYTE`: an
`UNSIGNED_BYTE` has identical wire width but fails Via's in-memory typed read at
the 1.21.2 boundary. The test reproduced the reported 26.2 encoder exception.

For a focused Forge regression run, optionally set `VIAFORGE_SMOKE_PROTOCOL`
to an exact registered protocol (for example `776`). Omit it for the required
full run over every registered resource profile; a focused PASS does not certify the other profiles.

## Elytra and 26.3 update

See [UPGRADE-26.3.md](UPGRADE-26.3.md) for the explicit protocol 777 boundary, current dependency versions, Elytra movement and saved-world repairs. ELYTRA is a client feature with ITEMS and ENTITY_VISUALS dependencies; ELYTRA_FIREWORKS is a separate rule starting at protocol 316. Flight actions use the existing serverbound adapter, and original metadata arrives through the session-tagged client event queue.

[ELYTRA-MOVEMENT.md](ELYTRA-MOVEMENT.md) documents local start prediction since 1.15,
server-owned landing flags, collision-aware size changes and crawling since 1.14.
These are independent named behavior rules, not tests against packet-layout revisions.
