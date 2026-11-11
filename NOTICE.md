# Notices and third-party licenses

**MechKeys is an unofficial, independent project by NotThatLonely. It is not affiliated with, endorsed by, or sponsored by Mechvibes or its author.** "Mechvibes" is used only to describe file-format compatibility. No Mechvibes logos or artwork are included.

## What was reused / derived

* **Mechvibes** (https://github.com/hainguyents13/mechvibes, MIT) - the sound-pack file format (V2 `config.json`, and the legacy V1 format) and the V1 key-code to key-name table in `KeyCodes.java` are derived from its documentation and `config_converter.rs`. No Rust source is copied; the loader, converter and audio engine are independent Java implementations. `docs/MECHVIBES_EDITOR.md` reproduces the Mechvibes editor documentation for convenience; it remains Mechvibes' text.

The Mechvibes MIT notice is reproduced below as the license requires.

### Mechvibes

```
MIT License

Copyright (c) 2026 Hải Nguyễn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Bundled default sound packs (inside the mod jars)

The two default packs in `common/src/main/resources/assets/mechkeys/defaults/` were supplied by the project owner from the Mechvibes community library:

* **amazin keychron q3 bluex** by *bluecxmbo* (keyboard)
* **X2 SuperStrike MOUSE** by *m0uh* (mouse; the original MP3 was converted to WAV, same audio and timings)

Each ships with its own `LICENSE.txt`, which says **"Personal use only"** (authors may claim and relicense). Keep that in mind before publishing a jar that contains them. Packs you import yourself remain under their authors' licenses; MechKeys does not redistribute any beyond the ones listed here and in the next section.

## Optional sound packs in `soundpacks/`

The `soundpacks/` folder holds 11 optional packs from the Mechvibes library / community (7 keyboard, 4 mouse). They are **not** inside the mod jars. The seven keyboard packs carry a `LICENSE.txt` that says **"Personal use only"** (original author: Mechvibes; the original author can claim a pack and change its license). The four mouse packs include no license file; they remain under their authors' terms. See `docs/SOUNDPACKS.md` for names and authors. If you fork or republish this repository, check these terms or remove the folder.

## Runtime libraries

JOrbis (OGG decoding) is used by `OggDecoder`. On Forge 1.8.9 it is provided by Minecraft itself (`codecjorbis`); nothing is shaded or redistributed. See `docs/PORTING.md` for the Fabric note.
