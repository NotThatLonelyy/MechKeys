# Testing report (honest status)

## Verified locally (headless Linux sandbox, JDK 21 compiling the core at Java 8 source level)
`SelfTest` (30 checks) covers the shared **core** (`common/`): both bundled default packs load as keyboard / mouse and build banks;
import from flat zip, zip wrapped in a folder, deeply nested zip, backslash entry names, `__MACOSX` junk, BOM config, folder, `file://` link, quoted path;
zip-slip / non-zip / config-less / missing / empty input rejected with clear messages and no temp files left; MP3 pack gives a clear message without ffmpeg;
engine: keyboard and mouse slots fill independently, press/release routing, each device's switch, key-repeat suppression, delete + slot refill, settings round trip.

When the repositories were merged into this one, the core was re-compiled in its single shared location and `SelfTest` passed 30/30 (the OGG decoder was stubbed in that run because JOrbis was not available offline).
All 11 packs in `soundpacks/` and both bundled defaults were loaded through `PackLoader` and recognised with the correct type (keyboard / mouse).

## NOT verified (no Minecraft, no network, no audio device in the sandbox)
* **Loader layers** have never been compiled or run here: `fabric/` (mod entry, mixins, Compat, screens) and `forge-1.8.9/` (mod entry, input events, screens, the Browse... file dialog). Expect a few compile errors on the first CI run; send the Actions log and they get fixed.
* **The merged Gradle setup** (shared `../common` source directories, `../version.properties`, the combined workflow) is new and has not been run. Fabric Loom 1.16-SNAPSHOT / Gradle 9.4 and ForgeGradle 2.1 / Gradle 2.14.1 builds are CI-only.
* **OGG decoding** (JOrbis) is written but untested here; the CI test decodes `common/src/test/resources/test-tone.ogg`. Whether JOrbis is available to the Fabric 26.x build is unconfirmed (see PORTING.md, "Open items").
* **Real audio output / latency / mixer clipping**: never heard. Latency estimate from buffer size: ~25 ms.
* **FPS impact, startup/shutdown inside Minecraft, multiplayer, GUI appearance**: untested.

## Manual test checklist for the first in-game run (each loader)
1. Game starts; `logs/latest.log` shows "MechKeys 1.0.0 ready".
2. Press K: the screen opens; keys and clicks make sounds. Type in chat, hold W (single sound), mash keys.
3. Profiles: select, import a zip (path, Browse..., URL), import an extra pack from `soundpacks/`, delete.
4. Main screen: toggle keyboard/mouse, drag volumes, cycle profiles.
5. Restart: choices restored.
