# Development

This workspace targets Minecraft Forge 1.8.9 only. Open the root directory as a
Gradle project using JDK 21. The Gradle wrapper is included.

- `build.bat`: compile, run account tests on Java 8, verify and package the release.
- `run.bat`: build the Java 8 development JAR and launch Forge 1.8.9.
- `build.bat test`: run the account tests.

All client code and resources live in `src/main`; account tests live in `src/test`.
There are no version subprojects. Build outputs are in `build/libs`. The source
ZIP contains the matching source files and build scripts. Put manual verification
logs in `build/logs` rather than the project root.

The release pipeline shades dependencies, remaps MCP names to Forge SRG names,
downgrades modern bytecode to Java 8 and bundles the necessary compatibility API.
The development pipeline keeps MCP names and installs its JAR in `run/mods` so
Forge discovers both the Mixin bootstrap and the `@Mod` entry point.

Preserve upstream notices when changing copied files and record substantial
adaptations in `docs/THIRD_PARTY_NOTICES.md`. Keep account credentials and all `run/`
data out of version control and source archives.
