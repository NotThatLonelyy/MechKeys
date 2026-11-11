# Porting guide - adding a new version to this repo

MechKeys is one repository with one shared core. A new Minecraft version should only ever touch a few small files. The same notes are also written inside the source files that usually need changes (search for `PORTING NOTES`).

## How the repo is organised

```
common/        Minecraft-free core (package ...mechkeys.core): packs, importer, mixer, engine, bundled default packs, tests
fabric/        Fabric 26.x loader code (mod entry, 2 mixins, screens) + versions/<mc>.properties per Minecraft version
forge-1.8.9/   Forge 1.8.9 loader code (mod entry, input handler, screens)
version.properties            release version, read by every module
.github/workflows/build.yml   one job per toolchain; Fabric uses a matrix over Minecraft versions
```

Rules that keep every version working:
1. `common/` never imports Minecraft, Forge or Fabric classes.
2. `common/` stays **Java 8 source level** (the Forge 1.8.9 toolchain is JDK 8).
3. Loader modules only adapt input hooks, key mapping, command and GUI. Sound logic stays in `common/`.
4. A change in `common/` affects every version: let CI build all of them before you release.

## A. New Fabric Minecraft version (26.4, 27.x ...)

1. Copy the newest `fabric/versions/<mc>.properties` to `fabric/versions/<new>.properties` and set:
   * `minecraft_version` - exact game version
   * `fabric_api_version` - the matching Fabric API build
   * `mc_range` - accepted range, e.g. `~26.4`
2. In `.github/workflows/build.yml`, add `'<new>'` to the Fabric `matrix.mc` list.
3. Build: `cd fabric && gradle build -Pmc=<new>` (or push and let CI do it).
4. If it fails to compile or crashes, fix in this order (smallest blast radius first):
   * `Compat.java` - reflective shims for screen opening and key-mapping construction. Add the new variant, keep the old ones.
   * `Keys.java` - a renamed `InputConstants` key field. Add the new name.
   * `MKScreen.java` - renamed GUI graphics calls (`fill`, `text`). The other screens use its helpers.
   * `mixin/KeyboardHandlerMixin.java`, `mixin/MouseHandlerMixin.java` - changed target method name or parameters (`keyPress`, `onButton`). A non-matching mixin fails at start-up on purpose (`defaultRequire = 1`).
   * `MechKeysMod.java` - changed Fabric API event / command class names.
5. Keep the old versions building. Do not delete shims for an old version unless you drop that version on purpose.
6. If a new Loom / Gradle / JDK is required, change: `fabric/build.gradle` (plugin version, `VERSION_25`, `release = 25`), `fabric.mod.json` (`java`), and the workflow (`java-version`, `gradle-version`). This applies to all Fabric versions in the matrix, so if the old ones cannot use it, create a second Fabric folder (see C).

## B. New release of the same versions

Release checklist:
1. Bump `mod_version` in `version.properties`.
2. Bump `VERSION` in `fabric/.../MechKeysMod.java` and `forge-1.8.9/.../MechKeysMod.java` (they must match).
3. Add an entry to `CHANGELOG.md`.
4. If the bundled default packs changed, change `DEFAULTS_MARKER` (`.defaults-v2` -> `.defaults-v3`) in both `MechKeysMod` classes so existing players get the new defaults once.
5. Commit, then tag: `git tag v1.0.1 && git push --tags`. The workflow builds every jar and attaches all of them to one GitHub release.

## C. New loader or legacy Minecraft version (e.g. Forge 1.12.2, NeoForge, Quilt)

1. Copy the closest module folder to `<loader>-<mc>/` (for example `forge-1.12.2/`).
2. Edit its `build.gradle`: Minecraft version, mappings, `archivesBaseName`. Keep the `srcDir '../common/...'` lines - they are what shares the core.
3. Adapt only the loader classes: mod entry (`MechKeysMod`), input hook (`InputHandler` / mixins), key binding + command, and the GUI base class (`MKScreen`). Call the same core API: `SoundEngine.onKey(name, down, repeat)` and `SoundEngine.onMouse(name, down)`, with names produced the way `Keys.java` (Fabric) or `core.KeyCodes` (LWJGL 2) does.
4. Add a job for it to `.github/workflows/build.yml` (copy the `forge` or `fabric` job, change `working-directory`, cache key and artifact name) and add it to the `needs:` list of the `release` job.
5. Add a row to the tables in `README.md` and `docs/COMPATIBILITY.md`.

## D. Changing the core / pack format

* Edit `common/` and run its tests: they run as part of every module's `gradle build` (`SelfTest`, 30 checks, via JUnit).
* New Mechvibes pack format: extend `PackLoader` and add a case to `SelfTest`.
* New default pack: add its folder under `common/src/main/resources/assets/mechkeys/defaults/`, list its files in `index.txt`, update the profile ids in `MechKeysMod.installDefaults`, `NOTICE.md`, `docs/SOUNDPACKS.md`, and bump `DEFAULTS_MARKER`.

## Open items to verify on the next build (see TESTING.md)

* **JOrbis on Fabric.** `core.OggDecoder` uses JOrbis (`com.jcraft.*`). Minecraft 1.8.9 ships it; the Fabric 26.x build has never been confirmed to have it on the compile/run classpath. If CI reports `package com.jcraft.jorbis does not exist`, or OGG packs fail to decode in-game, add JOrbis as a dependency of `fabric/build.gradle` and bundle it with Loom's `include`.
* Everything marked "NOT verified" in `docs/TESTING.md`.
