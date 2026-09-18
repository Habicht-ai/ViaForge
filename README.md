# ViaForge 1.8.9 Client

ViaForge from the [ver/1.8-1.12](https://github.com/ViaVersion/ViaForge/tree/ver/1.8-1.12) branch,
adapted to target **Minecraft 1.8.9 / Forge 11.15.1.2318** only.
Developer launches include account management adapted from [Vibe](https://codeberg.org/SkidderClub/Vibe).

## Running and building (Windows)

- **`run.bat`** launches Minecraft with ViaForge and the developer account manager.
- **`build.bat`** runs checks and builds the mod JAR without the account manager.
- Double-click **`Testserver.vbs`** (or **`Testserver.bat`**) to open the local web
  dashboard for 48 Vanilla test servers: live status, start/stop, favorites and
  searchable block, item and mob catalogs. See the [test laboratory guide](docs/TESTSERVERS.md).
- Output: **`build/libs/ViaForge-1.8.9-4.4.0-client.1.jar`**.
- For a standard Forge 1.8.9 installation, place the JAR in the `mods` folder.
  It is a Forge mod and cannot be launched directly by double-clicking it.
- The matching project sources are saved alongside it as `*-sources.zip`.

The build script automatically searches for **JDK 21**, including in `%USERPROFILE%/.jdks`.
The game runs on **Java 8**, which Gradle selects as a toolchain and downloads
if needed. The first launch also downloads Minecraft, Forge and the required
libraries. This requires an internet connection and may take a few minutes.
The scripts also work when a different Java version is set as the system default.

`run/` contains the game data. You can pass additional Gradle arguments:

```bat
build.bat clean build
build.bat test
run.bat --info
```

For automated runs, use `set VIAFORGE_NO_PAUSE=1` to skip the final keypress prompt.
The scripts return Gradle's exit code.
On Windows, an available drive letter is temporarily mapped to the project
so the legacy Forge tools can work with short paths.

## Developer accounts

When starting through **`run.bat`** (Gradle `runClient`), open **Accounts** in the
top-right corner of the main menu. Account login support and this button are
included only in the development JAR; the release mod uses the launcher account.

- **Microsoft login**: sign in through your browser and enter the displayed code.
  As in Vibe, Microsoft identifies the application as **In-Game Account Switcher**.
- **Cookie login**: select your own Microsoft cookie file in Netscape format.
- **More â†’ Offline profile**: create a profile for singleplayer and offline servers.
- **Use account**: sign in with a saved account; **Auto Login** enables automatic
  sign-in with that account on the next launch.
- **More** provides backup import/export, access to the cookie folder and an option
  to restore the original launcher session.

Saved credentials are encrypted and stored in `run/ViaForge/accounts/`.
`accounts.vault` and `accounts.key` belong together; keep both private.
The key is stored locally alongside the vault, so encryption does not replace
the need to secure your Windows user account. Vibe backups require their matching
key and cannot be decrypted independently.
Automatic cookie import watches only `run/ViaForge/cookies/`.
Accounts can only be switched when no world is running.

## Server version

The **ViaForge** button opens the protocol selector. ViaForge's existing protocol
translation through ViaVersion, ViaBackwards, ViaRewind, ViaLegacy and ViaAprilFools
is included. The Minecraft client itself always remains on version 1.8.9.

## Versioned blocks and block items

Multiplayer connections using explicit server profiles retain the inherited original
block states before Via converts them. See [modern adapters and verification limits](docs/MODERN.md)
for the additional families through 26.2. The catalog covers the new block IDs
introduced through 1.12.2 (198â€“252 and 255), with each family enabled from its own release:

- **1.9+**: end rods, chorus plants/flowers, purpur blocks/pillars/stairs/slabs,
  end stone bricks, beetroots, grass paths, frosted ice and the new command,
  structure and gateway block representations.
- **1.10+**: magma, nether wart blocks, red nether bricks, bone blocks and structure void.
- **1.11+**: observers and all 16 shulker boxes, including server-controlled lid animation.
- **1.12+**: all concrete, concrete powder and glazed terracotta colors, plus all
  16 bed colors in the world, inventory and hand. Older profiles retain the red bed.

Their block items now render in inventories and hands, support creative pick-block
and preserve server IDs, metadata, names and NBT when moved or placed. Creative
entries are filtered by the connected server's version. Double slabs, frosted ice
and gateways do not invent inventory items absent from vanilla; beetroot plants
use seeds, and structure-block items begin in 1.10. Falling concrete powder retains
its block color. Standalone items through 1.12.2 are also included: end crystals,
chorus fruit and popped fruit, beetroot and soup, dragon's breath, spectral/tipped
arrows, all potion variants, shields with banner patterns, elytra, wooden boats,
totems, shulker shells, iron nuggets and knowledge books. Dragon heads, spawn-egg
variants and Frost Walker/Mending/curse books use their original item data.
Inventory and hand models follow the selected profile; stack sizes, durability,
eating/drinking, bow ammunition and equipment slots are supported. Shields use
target-version third-person poses, including other players' offhand shields.
The local offhand has its original inventory slot, hotbar display and item use.
Press **F** to swap hands; change the main hand under Skin Customization. Both
hands have separate rendering, and bows can use offhand ammunition. See
[two-hand controls and verification](docs/HANDS.md).
Thrown splash/lingering potions retain their models and colors; lingering clouds
display the server's particle color and radius. Creative categories follow the
target release, and the dragon-head inventory orientation follows its 1.11.1 change. Item effects
and consumption remain server-authoritative. See [item scope and limitations](docs/ITEMS.md).
Boats on 1.9â€“1.12.2 servers use the six original wood models, versioned rowing,
water/ice movement and two independent passenger seats. The first passenger drives;
the second can ride and dismount normally. Native 1.8 boats retain their original
behavior. See [boat behavior and verification](docs/BOATS.md).
Beetroot crops acknowledge bonemeal use while immature, restoring the normal hand
swing; growth and item consumption are confirmed by the server.

All three command block types have editors from 1.9 onward, including command
text, output tracking, mode, conditional execution and redstone activation.
Structure blocks have save/load/corner/data editors from 1.10 onward, with size
detection, placement settings and selection previews. The server enforces creative
operator permissions and executes commands and structure operations. Editors wait
for server data, and cancelling leaves the server settings unchanged.

Existing blocks also receive the target version's block textures. The first join
downloads that version's vanilla client archive from Mojang (about 9-10 MB),
verifies its published SHA-1 and caches it in `run/ViaForge/block-assets/`.
Only the required block/item resources and models are loaded; the downloaded client code is never executed.
Until loading finishes, the added blocks use temporary vanilla replacement
textures. Failed downloads keep those textures and report the failure in chat;
reconnecting retries. User and server resource packs retain their normal priority
over the vanilla texture profile.

Singleplayer and native 1.8 connections use their normal block textures. The
profile and creative entries are cleared when leaving a server.
**1.13?1.13.2 and 1.14?1.14.4 now have actual adapters** for the existing
1.9?1.12.2 features and catalog, with original target textures and models.
New content from the aquatic/village updates is not fully implemented. Versions
1.15 through 26.2 retain ordinary Via translation; their extended native feature
adapters remain unregistered. See [verified scope and remaining gaps](docs/FLATTENED.md).


Several patch releases share a protocol and cannot be distinguished by the
protocol selector. Their default resource profiles use the last patch in that
group (for example 1.9.4 for 1.9.3/1.9.4, and 1.10.2 for 1.10.x).
The shared [compatibility pipeline](docs/COMPATIBILITY.md) separates exact packet
codecs, inherited client behavior and target resources. New version families can
reuse existing features through explicit adapters. The flattened families observe
real Via translation boundaries without replaying stateful protocols.

See [the block implementation notes](docs/BLOCKS.md) for the exact profile table,
architecture, verification and next steps.

## Versioned mobs

Multiplayer profiles from 1.9 through 1.12.2 now preserve original mob identities,
textures, models, hitboxes, equipment and sounds. This includes shulkers, polar
bears, strays, husks, llamas, evokers, vindicators, vexes, parrots and illusioners,
with their version-specific variants and projectiles. Existing mobs receive
target textures and relevant pose/variant updates. Llama inventories, shoulder
parrots and the newer dragon phases are also represented in the client.

The server continues to control AI, movement, damage and inventories. Original
assets are downloaded and verified locally; 1.8.9 servers and singleplayer keep
their native behavior. See [the mob implementation notes](docs/MOBS.md) for
scope, resource handling, automated checks and remaining real-server validation.

## Development and attribution

This is a single Gradle project for Minecraft 1.8.9:

| Path | Contents |
| --- | --- |
| `src/main/` | ViaForge, Forge integration and release resources |
| `src/development/` | Developer account manager, screens and menu integration |
| `src/test/` | Account and block compatibility tests |
| `docs/` | Development notes and source attribution |
| `gradle/` | Gradle wrapper for reproducible builds |
| `build/` | Generated JARs, intermediate files and verification reports |
| `run/` | Game data, settings and accounts |
| `.gradle/` | Automatically generated build cache |

`gradle/` and `.gradle/` serve different purposes. You can delete `build/` and `.gradle/`
once the game and Gradle have stopped; both are recreated on the next build.
`run/` contains your personal game data.

The build compiles with JDK 21 and converts the bundled modern libraries to
Java 8. `runClient` uses a separate JAR with development names and account login
support in `build/development/`; the release mod JAR uses Forge SRG names and
contains no account manager classes. Both JARs are verified during `build`.

The adapted account tests cover OAuth flows with simulated responses, token
refresh, cookie validation and encrypted storage, among other checks.
A real Microsoft sign-in requires confirmation by the account owner.

Development: [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md).
Sources, revisions and adaptations: [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md).
License: [GPLv3](LICENSE).
