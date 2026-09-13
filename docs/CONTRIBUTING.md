# Development

This workspace targets Minecraft Forge 1.8.9 only. Open the root directory as a
Gradle project using JDK 21. The Gradle wrapper is included.

- `build.bat`: compile, run tests on Java 8, verify and package the release.
- `run.bat`: build the Java 8 development JAR with account login support and launch Forge 1.8.9.
- `build.bat test`: run the account and block tests.

Release code and resources live in `src/main`; the account manager, its screens
and Forge event integration live in the separate `src/development` source set.
Account and block compatibility tests live in `src/test` and run against the
downgraded development JAR. The opt-in client renderer/pipeline smoke test is
documented in [BLOCKS.md](BLOCKS.md).
There are no version subprojects. Release outputs are in `build/libs`; the
development JAR is in `build/development`. The source
ZIP contains the matching source files and build scripts. Put manual verification
logs in `build/logs` rather than the project root.

The release pipeline shades dependencies, remaps MCP names to Forge SRG names,
downgrades modern bytecode to Java 8 and bundles the necessary compatibility API.
The development pipeline adds `src/development` to the shaded main JAR, keeps
MCP names and installs its JAR in `run/mods` so Forge discovers the Mixin bootstrap,
ViaForge and the development account mod. The release pipeline only uses
`src/main`, so it has no account manager or Accounts button. `check` verifies that
account classes are absent from the release and present in the development JAR.

Preserve upstream notices when changing copied files and record substantial
adaptations in `docs/THIRD_PARTY_NOTICES.md`. Keep account credentials and all `run/`
data out of version control and source archives.
