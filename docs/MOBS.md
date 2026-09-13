# Versioned mobs

The Forge 1.8.9 client preserves original multiplayer mob identities and metadata
for the same ten 1.9-1.12.2 profiles listed in [BLOCKS.md](BLOCKS.md). Native 1.8.9
connections and singleplayer retain the ordinary 1.8 entities and resources.
Profiles with a shared protocol use the resource patch described in that table.
This extension does not add support for mobs introduced after 1.12.2.

| First release | Added client representations |
| --- | --- |
| 1.9 | Shulkers and their projectiles; dragon fireballs and dragon phase visuals |
| 1.10 | Polar bears, strays and husks |
| 1.11 | Llamas and spit, evokers and fangs, vindicators, vexes; colored shulkers |
| 1.12 | Parrots and shoulder parrots, illusioners and their mirror images |

Existing mob identities are retained too. The catalog includes elder guardians,
wither skeletons, zombie villagers and horse/donkey/mule/undead horse variants,
including their shared spawn IDs before 1.11. The existing 1.8 implementations
remain in use where applicable. Skeletons, zombies and zombie villagers use new
client models/poses; newer villager professions, sheared snow golems and the
smaller 1.9 rabbit dimensions are handled explicitly.

## Rendering and state

Original spawn, metadata, equipment, passenger and status packets pass through
the compressed Via pipeline. Via retains its protocol trackers; a private native
payload then creates the appropriate client entity. Partial metadata updates
retain fields that were not sent. Native movement, teleport, equipment and destroy
packets still identify the same entity. The old DataWatcher conversion is skipped
for the custom living entities and mob projectiles because its slot types
describe substitute mobs. Projectiles use only original name/visibility metadata,
so Via's generated "Shulker Bullet" label is not displayed. Actual server-provided
custom names and their later removal remain supported.
World changes clear the preserved state.

The implementation includes target texture selection, model boxes and UVs,
walking and attack poses, left/right held equipment, armor, child sizes, shulker
lid/attachment/teleport animation and expanding hitboxes, polar-bear standing,
llama coats/chests/carpets, vex charging, illager spells and illusioner copies.
Parrots support all five variants, flying, sitting, dancing near a jukebox and
both player shoulders. Creative picking uses the target's available spawn eggs;
it does not create an illusioner egg absent from these versions.

Llama inventories use their original carpet slot and zero to five chest columns,
with original server slot numbers for content updates and clicks. Via's donkey
inventory padding is disabled only on these supported client connections.
The inventory key also opens the llama menu while riding. The server controls
taming, chest attachment, strength, inventory contents and mount movement.

The dragon has the additional neck part with the server's eight part IDs, smaller
head hitbox, phase-dependent head/neck pose, perched wing speed, growls and breath
particles. Shulker bullets, llama spit, evoker fangs and dragon fireballs have
their original geometry/textures and client motion/particle behavior. Hits,
damage, spawning, AI, loot and authoritative movement remain server decisions.

Original update/remove packets for effects 24-27 also reach the native client
effect list. Levitation therefore appears in the inventory with its target
version's icon, translated name, level and remaining duration. Ambient/particle
flags and server removal are preserved. Via still processes the same original
packets for its existing levitation movement handling; displaying the effect
does not add a second movement simulation.

## Resources and sounds

Entity and armor textures and the llama inventory sheet are read from the verified
Mojang client archive for the target profile. Original sound definitions and OGGs
come from the pinned Mojang asset indexes in `mob-asset-indexes.json`; downloads
are bounded and checked against the index size/SHA-1, and recordings are cached
by hash. An installed Minecraft asset cache is reused when it contains a verified
copy. The first connection can require additional downloads.

Each protocol profile has its own numeric sound-event order, including the extra
event in 1.9.1. Original entity sound packets replace Via's substitute events;
status-driven hurt/death sounds use the corresponding mob and damage type.
Local 1.8 ambient playback is suppressed for tracked mobs to avoid duplicating
the server's sound packets. Sound categories, pitch, volume, weighted samples and
referenced events are retained. Leaving the session removes the target resource
overlay. No Minecraft client archives, textures or OGGs are bundled in the mod.

## Verification

`build.bat build` runs the JVM tests and verifies Java 8 bytecode, Forge metadata
and mixin mappings in the packaged JAR. `MobCatalogTest` checks release boundaries,
pre-1.11 variants and all ten numeric sound registries.

The existing `VIAFORGE_BLOCK_SMOKE_TEST` Forge-client run also exercises mob spawns,
partial metadata, movement, health, hurt statuses, equipment, child hitboxes,
original variant textures, shulker attachment, dragon phases/part IDs, projectile
spawns, sounds, shoulder NBT and llama inventory content/clicks for every profile.
It renders mob galleries and detail views in the real Forge renderer:

- `build/logs/screenshots/mobs-<version>.png`
- `build/logs/screenshots/mob-details-<version>.png`
- `build/logs/screenshots/llama-inventory-1.12.2.png`

These are synthetic packet fixtures inside the actual client. They do not replace
side-by-side gameplay checks on real servers. In particular, test combat against
moving mobs, riding/leashing, the full dragon fight, teleportation and world
changes with real server timing. Check OptiFine/Patcher separately. Pixel-for-pixel
agreement for every animation frame and compatibility with other rendering mods
have not been established by the automated checks.
