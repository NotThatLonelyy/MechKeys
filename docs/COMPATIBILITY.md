# Compatibility

| Target | Module | Status |
|---|---|---|
| Fabric 26.1 (26.1.2) | `fabric/` | Implemented. Build verified by CI only (see TESTING.md). |
| Fabric 26.2 | `fabric/` | Implemented. Build verified by CI only. |
| Fabric 26.3 | `fabric/` | Implemented. Build verified by CI only. |
| Forge 1.8.9 (11.15.1.2318) | `forge-1.8.9/` | Implemented. Client-only. Build verified by CI only. |
| Other 1.8.9 loaders (Lunar, Badlion, Vanilla+Forge) | - | Not tested. |
| Newer Minecraft / other loaders | - | Planned; see [PORTING.md](PORTING.md). |

Requirements: Fabric needs Java 25, Fabric Loader 0.19.5+ and Fabric API. Forge 1.8.9 runs on Java 8.

## Design notes
* `common/` (package `...mechkeys.core`) is Java 8, dependency-free (own JSON parser) except JOrbis for OGG, and has no Minecraft dependency. Both loaders share it.
* Audio uses `javax.sound`, independent of Minecraft's OpenAL, so it does not use Minecraft's sound system or its volume sliders; use the MechKeys volume sliders.
* Keyboard and mouse sounds are independent (profile, switch, volume each).

## Mechvibes pack compatibility
* Supported: V2 `single` / `multi`, V1 `single` / `multi`.
* MP3 is converted once via an installed ffmpeg (the game cannot decode MP3).
* Not supported: FLAC / M4A audio, Mechvibes themes, the Mechvibes online library API.
