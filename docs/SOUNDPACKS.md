# Sound packs

MechKeys plays **Mechvibes-format sound packs**. This page lists the packs that come with MechKeys and explains, step by step, how to add any other pack.

## 1. Default sound packs (installed automatically)

Both are inside the mod jar and are installed to `.minecraft/config/mechkeys/profiles/` the first time the mod runs, then selected as the active keyboard and mouse sound.

| Profile id | Name | Type | Author | Audio |
|---|---|---|---|---|
| `keychron-q3-bluex` | amazin keychron q3 bluex | Keyboard | bluecxmbo | WAV |
| `superstrike-mouse` | X2 SuperStrike MOUSE (My First Mouse SP) | Mouse | m0uh | WAV |

* Deleting a default pack in the Profiles screen is respected: it is not installed again.
* Both are archived in the Mechvibes library with a **personal use only** license, see [NOTICE.md](../NOTICE.md).
* Source of the bundled files: `common/src/main/resources/assets/mechkeys/defaults/` (listed in `index.txt`).

## 2. Extra packs shipped in this repository (`soundpacks/`)

Optional. Not inside the jar; add them like any other pack (section 3).

**Keyboard**

| File | Pack name | Author | Audio |
|---|---|---|---|
| `cherrymx-blue-abs.zip` | CherryMX Blue - ABS keycaps | Mechvibes | OGG |
| `cherrymx-red-abs.zip` | CherryMX Red - ABS keycaps | Mechvibes | OGG |
| `cherrymx-brown-pbt.zip` | CherryMX Brown - PBT keycaps | Mechvibes | OGG |
| `cherrymx-black-pbt.zip` | CherryMX Black - PBT keycaps | Mechvibes | OGG |
| `eg-oreo.zip` | EG Oreo | Mechvibes | OGG |
| `eg-crystal-purple.zip` | EG Crystal Purple | Mechvibes | OGG |
| `topre-purple-hybrid-pbt.zip` | Topre Purple Hybrid - PBT keycaps | Mechvibes | OGG |

**Mouse**

| File | Pack name | Author | Audio |
|---|---|---|---|
| `incott-g23-v2.zip` | incott g23 v2 | fastglocks | WAV |
| `royal-kludge-m3.zip` | Royal Kludge M3 | Laika/Raika | WAV |
| `asmr-mouse.zip` | ASMR MOUSE | Angel Lozano | OGG |
| `g-pro.zip` | G Pro | Zen Zen | MP3 (needs ffmpeg, see below) |

All eleven were checked to load with MechKeys' pack loader (config valid, audio files present, correct keyboard/mouse type).

## 3. How to add a sound pack (full steps)

### Step 1 - Get a pack
Any of these:
* a `.zip` from this repository's `soundpacks/` folder,
* a pack downloaded from the Mechvibes library or community,
* a pack you made yourself in the Mechvibes editor (section 5).

A valid pack is a `.zip` (or folder) with a **`config.json`** and at least one audio file (`.wav`, `.ogg`, or `.mp3` with ffmpeg). The zip may keep the pack inside a sub-folder; `__MACOSX` junk is ignored.

### Step 2 - Open MechKeys
Start Minecraft and press **K** (rebind in *Controls > MechKeys*), or type `/mechkeys` in chat.

### Step 3 - Open Profiles / Import
Click **Profiles / Import**. You see the installed packs and an **Import...** button.

### Step 4 - Import
Click **Import...** and give MechKeys the pack in any one of these ways:
1. **Path** - paste the path to the `.zip` or to an unpacked folder. Quotes (Windows "Copy as path") are fine.
2. **Link** - paste a direct `https://...` link to the `.zip`.
3. **Browse...** - pick the `.zip` in the normal file dialog. If the dialog does not appear on your system, paste the path instead.

MechKeys reads `config.json`, checks the audio, copies the pack to `.minecraft/config/mechkeys/profiles/<name>/` and tells you if anything is wrong.

### Step 5 - It goes to the right device automatically
* A **keyboard** pack is assigned to the keyboard.
* A **mouse** pack (all keys are `Mouse...`) is assigned to the mouse.

### Step 6 - Choose and test
Back on the main screen use the **[<] [>]** arrows next to KEYBOARD / MOUSE to pick the profile, make sure the device switch says **ON**, adjust the volume, then type / click.

### Manual install (no in-game import)
1. Unzip the pack so that `config.json` and the audio sit directly inside one folder.
2. Move that folder to `.minecraft/config/mechkeys/profiles/<any-name>/`.
3. Reopen the Profiles screen (or restart the game). Profiles are read from disk each time the screen opens.

### Removing a pack
Profiles screen > select the pack > delete. The folder under `profiles/` is removed.

## 4. Audio format notes

| Format | Support |
|---|---|
| `.wav`, `.ogg` | Play directly. |
| `.mp3` | Converted **once** to a sibling `.mkconv.wav` using `ffmpeg`, if ffmpeg is installed and on your PATH. Otherwise the import stops with a message: convert the audio to `.wav`/`.ogg` or install ffmpeg and import again. |
| `.flac`, `.m4a`, video | Not supported. Convert to OGG or WAV first (for example with ffmpeg). |

Limits: 64 MB per audio file, 256 MB per archive. Supported pack formats: Mechvibes V2 (`single`, `multi`) and legacy V1.
Not supported: Mechvibes themes and the Mechvibes online library API (download packs yourself, then import the zip).

Convert example (ffmpeg): `ffmpeg -i sound.mp3 -c:a libvorbis sound.ogg` - then update `audio_file` in `config.json` (single mode) or each file name (multi mode).

## 5. Make your own pack

Use the Mechvibes editor (runs in the browser, no install) and export a `.zip`. Its documentation is reproduced in [MECHVIBES_EDITOR.md](MECHVIBES_EDITOR.md). Then import the exported zip into MechKeys with section 3. Tips:
* Choose **Keyboard** or **Mouse** at the top of the editor first - MechKeys decides the device from the keys in the pack.
* Prefer OGG/WAV audio so you do not need ffmpeg.
* Assign **Release** sounds too for a more realistic feel; MechKeys plays press and release separately when the pack has both.
* Packs in the old V1 format cannot be opened by the editor, but MechKeys itself can still import V1 packs.

## 6. Troubleshooting

| Symptom | Fix |
|---|---|
| "No config.json" | The zip is not a pack, or `config.json` is missing. Re-download or re-export. |
| "MP3 can't be decoded" | Install ffmpeg or convert the audio to `.wav`/`.ogg`. |
| "Skipped invalid: ..." | That folder's `config.json` is broken. Details are in `logs/latest.log` (search `MechKeys`). |
| Pack imported but silent | Is the device switch ON and the pack selected? Is the pack type right (keyboard pack on the keyboard)? |
| Some keys silent | The pack simply has no sound defined for that key. |
