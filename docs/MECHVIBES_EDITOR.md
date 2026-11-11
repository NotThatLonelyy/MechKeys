# Mechvibes sound pack editor - documentation

> This page reproduces the documentation of the **Mechvibes editor** (a browser tool) so you can build packs for MechKeys. The editor and its docs belong to the [Mechvibes](https://github.com/hainguyents13/mechvibes) project (MIT, see [NOTICE.md](../NOTICE.md)). MechKeys is unofficial and not affiliated with Mechvibes.
> To use an exported pack in Minecraft, follow [SOUNDPACKS.md](SOUNDPACKS.md) (import the exported `.zip` in the MechKeys screen). References below to adding the zip to the Mechvibes app are about the Mechvibes desktop app, not MechKeys.

What the editor does and how a pack comes together.

The Mechvibes editor runs in your browser. It builds a keyboard or mouse sound pack from scratch or from an existing one, then exports a `.zip` for the app. New to it? **Tour** shows where to start.

* **Pack name** (clickable) — opens *Edit Pack Information*: name, author, description, tags, icon, recommended volume and random pitch.
* **New** — starts an empty pack.
* **Import** — opens a sound pack `.zip` to change it.
* **Preview** — plays the pack as you type or click.
* **Export** — downloads the pack as a `.zip`.
* **Sign in / My packs** — opens your packs from the library, and **Update** publishes your changes to one.

Above the keyboard, choose whether the pack is for a **Keyboard** or a **Mouse**, and for a keyboard its size: **Full size**, **TKL** or **Compact**.

## Selecting keys

Pick the keys a sound plays on.

* Click a key to select just that key.
* Drag to select an area of keys.
* Ctrl + Click (Windows/Linux), Cmd + Click (macOS) or Shift + Click to add or remove individual keys.
* Esc clears the whole selection.
* **Quick select** picks a whole group: letters, numbers, F keys or modifier keys.
* A key with a sound shows a dot: top-right for the press sound, bottom-right for the release sound.

## Assigning sounds

One recording sliced across the keys, or a sound for each key.

Drop audio (MP3, WAV, OGG, FLAC, M4A) or video in, or click to browse. Use one recording for every key, cutting a slice of it for each, or give keys their own files. There is no mode to choose: the editor works it out from the files your keys use.

1. Pick a sound: drag on the waveform (drag its edges to resize), type **Start (ms)** and **End (ms)**, or use **Whole file**.
2. Press **Test** to hear the selection.
3. Under **Plays on**, choose **Press** or **Release**.
4. Select the keys, then press **Apply to N keys** (or **Apply release to N keys**).

* **Zoom** enlarges the waveform.
* **Auto detect** highlights each sound in the recording, with a **Threshold** slider; click a highlighted region to pick it.
* **Remove release** and **Reset all** clear what you assigned.

## Using a video file

Cut key sounds straight from a typing video.

You can load a video (MP4, WebM) instead of an audio file. The editor uses the video's audio track.

1. Load the video: drag & drop or click to browse.
2. A video preview fills the source panel on the left. The waveform beside it shows the video's audio track.
3. Pick a sound as usual. The video seeks to the start of the selection so you can see the frame that matches each sound.
4. Apply it to the selected keys.

On **Export**, the editor extracts the audio track, cuts out the unselected parts, joins the selected sounds into one file, and encodes it to OGG Vorbis. Whether a video opens depends on the codecs your browser supports.

## Previewing

Hear the pack the way Mechvibes will play it, before you export.

Once a key has a sound, press **Preview** and type, or click and scroll for a mouse pack, to hear the pack as Mechvibes will play it. Press Esc or **Exit preview** to go back to editing.

## Importing

Open a pack you already have and keep working on it.

* Press **Import** and choose a `.zip` file. If you have unsaved work, the editor asks before replacing it.
* The editor reads `config.json`, loads the audio, and puts each key's sound back on the keyboard.
* The pack information and the keyboard or mouse type come with it.

The pack must include `config.json` and at least one audio file (`.mp3`, `.wav`, `.ogg`, `.flac`). Packs in the old V1 format do not open; Mechvibes converts them when it loads them.

## Exporting

Save the pack as a zip the app can load.

Press **Export**. The editor writes `config.json`, packs it with the audio into a `.zip` and downloads it as `<slug>-<uuid>.zip`. Audio that is not already OGG or MP3 is compressed to OGG Vorbis first. In the Mechvibes app, add the zip in *Sound packs*, then *Add*, on the Keyboard or Mouse tab.

> **For MechKeys:** import the exported zip from **K > Profiles / Import > Import...** instead. Exported audio is OGG (plays directly) or MP3 (needs ffmpeg, see [SOUNDPACKS.md](SOUNDPACKS.md)).

## Sharing and updating

Publish the pack, then send out new versions of it later.

* To share a new pack, export it, then choose **Share your pack** in the library and select the `.zip`. Choose its type, category and license, then publish.
* To change a published pack, press **Sign in**, open it from **My packs**, and edit the sounds.
* Press **Update**. It publishes as the pack's next revision.
* The name, description, tags, type, category and license of a shared pack can also be edited on the website, from your profile's *Sound Packs*.
