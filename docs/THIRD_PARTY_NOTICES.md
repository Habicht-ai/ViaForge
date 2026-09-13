# Source and attribution

This is a modified ViaForge client for Minecraft Forge 1.8.9.

- ViaForge: https://github.com/ViaVersion/ViaForge/tree/ver/1.8-1.12,
  revision `2eab6aa82632405758b5871d6b55d5537d6d522e`.
  Copyright (C) 2021-2026 Florian Reuth and contributors; GPL-3.0-or-later.
- Vibe: https://codeberg.org/SkidderClub/Vibe,
  revision `4d73c27fb52889bbe0d0a70a93f938306f4af7fb`.
  Account implementation, account tests, account screens, rounded panel renderer,
  skin head renderer and skin profile lookup by SkidderClub/Vibe contributors,
  under Vibe's GPLv3 license. The Windows builder is adapted from Vibe too.
  The complete GPLv3 text is in the project root's `LICENSE`.

The account manager and screens are development-only (`src/development`) and
are not included in the release mod JAR. They are available when using `run.bat`
or Gradle `runClient` and remain included in the project source archive.

Changes to the Vibe files: Java package names, development-only Forge initialization
and menu event integration, account directory, fixed account theme and vanilla background in
place of Vibe's shader/language/theme infrastructure. Account screen clicks are
dispatched once to prevent a newly displayed button receiving the same click.
The encrypted file format
and `.vibeaccounts` backup format remain compatible. A backup requires the key
from the installation that created it.

As in Vibe, Microsoft device authorization uses the public In-Game Account
Switcher application registration; Microsoft's consent screen identifies it as
**In-Game Account Switcher**. Cookie authorization uses the Minecraft application
registration. Vibe credits IAS (The_Fireplace, VidTu and contributors) and
MinecraftAuth (Lenni0451 and contributors) as implementation references; neither
library is bundled here. No Microsoft password is collected by the client.

The JAR includes ViaVersion, ViaBackwards, ViaRewind, ViaAprilFools, ViaLegacy,
Mixin, SLF4J and the required JVM Downgrader compatibility code. Embedded
dependency license notices are retained where provided upstream.
`build/libs/*-sources.zip` contains the matching project sources and build scripts.

The versioned-block resource catalog records Mojang's official client download
URLs, sizes and SHA-1 hashes from
https://piston-meta.mojang.com/mc/game/version_manifest_v2.json (retrieved
2026-09-12). Vanilla block resources are downloaded and converted locally at
runtime. Minecraft artwork, models and client archives are not redistributed
inside the mod JAR or its source ZIP. The block implementation uses the existing
ViaVersion chunk codecs, reversible ViaBackwards/ViaRewind item mappings and Forge
block/rendering APIs. The locally read assets now include block-item models and
the specific entity/icon textures used by shulker boxes, colored beds, gateways and block seeds.
