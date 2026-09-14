# Compatibility pipeline

The connection's server version, packet codec, client behavior and resource
release are now independent. Existing 1.9?1.12.2 implementations run through this
pipeline. This refactor does **not** register a Minecraft 1.13+ wire codec yet.
Unknown targets retain normal Via connectivity without borrowing an older parser.

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
`SessionEpoch` issues a fresh ticket for every join, even on the same connection.
A background resource completion is applied only if its exact ticket is current.
Disconnect and server changes invalidate pending work; a late close from an old
connection cannot clear the new session. World unloading clears the shared client
entity, item, hand, cooldown and boat state as before.

## Adding the next family

For 1.13?1.13.2, implement and register exact wire adapters and resource converters,
normalize flattened block/item identities and changed metadata before lossy Via
steps, and provide the matching outgoing conversions. Derive the existing client
rules and override only changes established for those releases. Populate verified
asset manifests and extend content registrations for additions. The generic
transport and existing two-hand/boat/render implementations do not need another
server-version allowlist.

No nearest-version fallback or automatic enabling of an unknown packet format is
used. The registry stays explicit even when behavior is inherited.

## Verification

`CompatibilityArchitectureTest` checks independent target/resources/codec/rules,
immutable inheritance and overrides, capabilities/dependencies, unknown/native
versions, item copy semantics, connection-bound outgoing routing and stale resource
tickets. `CompatibilityDecodePipelineTest` uses a deliberately synthetic wire
format to check preservation before loss, restoration, cancellation, buffer
ownership, disabled-codec fallback and decoder reordering. These are architecture
fixtures, not a claim of real 1.13 support.

The existing Forge smoke suite now creates `CompatibilityDecodeHandler` through
the same registry and adapter factory as production for all ten supported profiles.
Its actual chunk, item, entity, hand, boat, UI, effect and model checks remain in
place. Read `build/logs/block-client-smoke-test.txt`: it must begin with `PASS`.
