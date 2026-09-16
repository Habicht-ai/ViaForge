# Modern compatibility families

ViaForge reuses the client catalog and features implemented through 1.12.2 on
explicit adapters for later server protocols. This does not add every block,
item, entity or gameplay mechanic introduced by those later releases.

## Protocol support and profiles

The bundled ViaVersion/ViaBackwards 5.11.0 and ViaRewind 4.1.3 provide a path from
Forge's protocol 47 to protocol 776 (26.2), including 47 translation layers for
that target. Library connectivity and ViaForge client feature coverage are
separate. Snapshots and unknown protocol IDs do not inherit extended features.

| Server family | Exact registered protocols | Resource release per protocol |
| --- | --- | --- |
| 1.15 | 573, 575, 578 | 1.15, 1.15.1, 1.15.2 |
| 1.16 | 735, 736, 751, 753, 754 | 1.16, 1.16.1, 1.16.2, 1.16.3, 1.16.5 |
| 1.17 | 755, 756 | 1.17, 1.17.1 |
| 1.18 | 757, 758 | 1.18.1, 1.18.2 |
| 1.19 | 759, 760, 761, 762 | 1.19, 1.19.2, 1.19.3, 1.19.4 |
| 1.20 | 763, 764, 765, 766 | 1.20.1, 1.20.2, 1.20.4, 1.20.6 |
| 1.21 | 767, 768, 769, 770, 771, 772, 773, 774 | 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.6, 1.21.8, 1.21.10, 1.21.11 |
| 26.x | 775, 776 | 26.1.2, 26.2 |

Releases sharing a wire protocol use the latest listed resource patch. The
handshake cannot distinguish, for example, 1.20.5 from 1.20.6 or 26.1 from
26.1.2. Resources are pinned to original Mojang client archives and asset indexes
with size and SHA-1 verification. Earlier profiles are listed in BLOCKS.md.

## Adapter boundaries

ModernBlockFamilies selects the actual Via protocol class and matching chunk
codec. It composes inverse *forward* block mappings before ViaBackwards can
replace originals with approximate older states. Only inherited, representable
states enter the client catalog. An unrepresentable update invalidates any
previous retained state at that position.

- 1.15: 3D biome arrays, seed/death-screen fields, separate light and mob spawns.
- 1.16: padded chunk palettes, dimension registry, equipment arrays; 1.16.2 adds
  section-position and VarLong block changes.
- 1.17: dimension-dependent masks, frozen metadata index, inventory cursor and
  click changes; 1.17.1 adds inventory state IDs and VarInt counts.
- 1.18: combined chunk/light packets, section biomes and typed block entities.
  The original 1.17-to-1.18 blockstate mapping is identity; preservation at the
  1.17 boundary follows Via's live chunk conversion without duplicating it.
- 1.19: registry mappings, unified entity spawns, death-location fields, use
  sequences; later patches change metadata types and remove light flags.
- 1.20.2: CONFIGURATION/PLAY transition, registry packets, anonymous NBT and player
  spawn changes. Internally sent/cancelled Via joins activate the session once.
- 1.20.5: structured component patches replace old item NBT. Exact component data,
  removed defaults, custom text and limits are retained separately from the live
  stack count and durability. 1.21 uses the server enchantment registry.
- 1.21.2: VarInt container IDs, direct player inventory slots, equipment resources,
  separate boat entity types, cooldown groups, sea level and border flags.
- 1.21.4: item definitions/special model renderers and another component layout.
- 1.21.5: new chunk heightmaps/palettes, hashed inventory clicks and length-prefixed
  Creative items; later patches use their own component registries/codecs.
- 1.21.9: compact movement vectors before entity spawn rotation/data.
- 1.21.11: original metadata type conversion remains owned by Via.
- 26.1: chunk section fluid counts, compact interaction vectors and separate attack
  packets; 26.2 adds the online-mode Join field and updated mappings/components.

CompatibilityDecodeHandler runs the live Via pipeline once. Existing client
consumers receive their declared internal legacy event formats. Native item
bridges stop at the internal 1.12 identity boundary; they do not replay modern
component rewriters just to look up a Forge item. Component snapshots are attached
after the original item is hashed, and use Via's own original-hash storage when
the extra tag changes custom_data. No fabricated/unknown hash replaces server data.

## Resources

Converters compose over earlier families: original chest faces are projected
onto the old UV layout; dirt-path, equipment and entity paths are renamed;
GUI sprites rebuild the offhand and attack HUD atlases; special item definitions
restore renderer selection; individual egg models use original untinted species
textures. The 1.19.3 Vex uses its smaller mesh and corresponding arm transforms.
26.x adds the llama/mooshroom/snow-golem path changes and 26.2 pillar side texture
names. The 26.2 sign atlas is projected onto ModelSign UVs; every visible face
is checked against the original block model UV coordinates.
26.x shield/banner base paths are aliased into the native renderer layout. The
26.2 beds use the actual target block models, textures and composite item foot
translation, replacing the former missing-atlas wood fallback. Java 8 PNG
transparency/gray brightness and native layered-mask image types are normalized
before composition. Banner/shield dye colors follow the 1.12+ texture palette.
Original target textures also overlay the existing 1.8 blocks during a supported
server session. Native 1.8 and singleplayer keep their existing behavior.

SessionEpoch invalidates resource publication on disconnect, server change and
world unload. Both successful and failed late downloads must match the current
session ticket. A resource failure is observable by the smoke harness immediately.
Preparation starts during server address resolution, caches two prepared releases,
and leaves the loading screen visible until the target atlas is ready. This avoids
showing temporary textures during joins; it does not eliminate first downloads or
the main-thread atlas upload. Banner and shield composites are invalidated on
version changes and disconnects.

## Validation

Completed on 2026-09-15: `build/logs/modern-26-signs.log` reports BUILD SUCCESSFUL
and the fresh `build/logs/block-client-smoke-test.txt` begins with PASS. All **48
registered profiles** passed: ten existing legacy profiles and 38 flattened/modern
profiles, including all 30 added protocols from 1.15 through 26.2. All **98 JUnit
tests** passed with zero failures, errors or skipped tests. This validates the
specific checks below, not complete gameplay emulation of every target release.

FlattenedPipelineSmokeTest sends actual target packet layouts through compression,
all installed Via layers, mixins and Forge's native decoder. It checks inherited
block states/chunks/updates, block entities, inventory/Creative round trips,
original component patches and hashes, edited durability, offhand, both hand
packet directions, attacks, boat passengers and selected mob/effect events.
Original enchantment registries come from the release data-pack JSON; 1.21.11
and 26.x also use original dimension/biome data with the actual attribute layout.
Checks cover the changed humanoid-arm field and age-locked metadata removal. Resource tests compare original pixels/UVs, selected
models, and all 16 shulker item colors' first/third-person, GUI and dropped matrices.
The older Purpur/shulker placement regression tests remain in the full suite.
These are in-process protocol/render tests, not an external multiplayer playtest.

Render bug follow-up, 2026-09-16: `render-fixes-smoke-3.log` completed all 48 profiles
with a fresh PASS and **106 JUnit tests**, no failures/errors/skips. New checks use
independent original 26.2 RGBA hashes (including grayscale color-key PNGs), actual
atlas alpha values, shield GPU texels, all 16 bed models' original face UVs and
16 distinct rendered banner dyes. `build/logs/screenshots/render-fixes-26.2.png`
shows the resulting inventory rendering. Water checks now retain aquatic biome
colors and custom server registry colors across the actual packet pipeline;
negative source heights, unload and dimension changes are included. The final
follow-up also checks original 1.19.4+ biome updates before Via cancels them,
the resource gate on the actual first position packet, and restoration of native
water colors and banner/shield caches on disconnect.

## Inventory and boat regressions (2026-09-16)

Bed items now resolve each color's inherited target display model instead of
falling back to generic cube transforms when `item/bed.json` is absent. The
26.2 composite model still uses its original head/foot geometry and transforms.
The Forge renderer test compares all 16 colors in both hands, GUI, fixed and
dropped contexts against the unconverted original archives and checks that
the complete baked geometry fits within a 16-pixel inventory slot.

Flattened spawn eggs retain a separate client appearance key derived from the
original item ID. Modern entity components may contain renamed mob IDs or even
deliberately summon another mob; those server-owned data must not select the
wrong legacy icon. The original snapshots remove the client key on return.
Tests cover all 43 inherited species as ID-only server items, fresh Creative
picks, server echoes and inventory round trips on every flattened profile.
26.2 additionally covers typed entity components, component hashes, and a
Shulker egg with an overridden spawn species without changing its appearance
or its original server data.

Vehicle input uses the correct signed-byte field in the in-memory Via wrapper.
The previous unsigned-byte field reproduced the reported 26.2 boat kick at the
1.21.2 translation step. Tests include all direction/jump/dismount combinations,
both independent paddle flags and vehicle position/rotation through every
flattened target's complete pipeline, including the later ground flag.

## Remaining limits

- The native 1.8 world, collision and renderer still display only Y=0..255.
  Tests send 24 sections from Y=-64..319 and check alignment and clipping without
  wrapping. This is **not full tall-world support**. Both native world storage
  and rendering/interaction need further work below 0 and above 255.
- Content added after the inherited 1.12.2 catalog still uses Via's fallback
  behavior unless separately implemented. A registered family is not full
  emulation of that release. Waterlogging/swimming and later mechanics are not
  certified by the inherited catalog tests.
- Villager biome/hat geometry, the zombified-piglin mesh, and later baby/variant
  models are incomplete. Loading their original texture does not certify geometry.
- Arbitrary later item-definition expressions and component gameplay semantics
  exceed the inherited renderer. Preserving a component does not implement it.
- Authentication with an external server, actual multiplayer inventory acceptance,
  and sustained gameplay on each target have not been tested here.
