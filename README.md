# ViaForge 1.8.9 Client

ViaForge from the [ver/1.8-1.12](https://github.com/ViaVersion/ViaForge/tree/ver/1.8-1.12) branch,
adapted to target **Minecraft 1.8.9 / Forge 11.15.1.2318** only.
Developer launches include account management adapted from [Vibe](https://codeberg.org/SkidderClub/Vibe).

## Running and building (Windows)

- **`run.bat`** launches Minecraft with ViaForge and the developer account manager.
- **`build.bat`** runs checks and builds the mod JAR without the account manager.
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
- **More → Offline profile**: create a profile for singleplayer and offline servers.
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

## Development and attribution

This is a single Gradle project for Minecraft 1.8.9:

| Path | Contents |
| --- | --- |
| `src/main/` | ViaForge, Forge integration and release resources |
| `src/development/` | Developer account manager, screens and menu integration |
| `src/test/` | Account tests |
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
