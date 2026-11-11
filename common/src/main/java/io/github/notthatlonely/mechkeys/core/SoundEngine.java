package io.github.notthatlonely.mechkeys.core;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Facade used by the Minecraft layer. Keyboard and mouse are two independent slots, like Mechvibes:
 * each has its own profile, on/off switch and volume. A profile whose keys are all mouse buttons loads into the
 * mouse slot; any other profile loads into the keyboard slot.
 */
public final class SoundEngine {
    public static final Logger LOG = Logger.getLogger("MechKeys");

    /** One independent sound source (keyboard or mouse). */
    public static final class Slot {
        public final boolean mouse;
        volatile Bank bank; volatile SoundPack pack; volatile String status = "No profile";
        final AtomicInteger version = new AtomicInteger();
        Slot(boolean mouse) { this.mouse = mouse; }
        public SoundPack pack() { return pack; }
        public String status() { return status; }
        public boolean loaded() { return bank != null; }
    }

    public final File root; public final ProfileManager profiles; public final Settings settings; public final Mixer mixer = new Mixer();
    public final Slot keyboard = new Slot(false), mouse = new Slot(true);
    private final File settingsFile;
    private final ExecutorService loader = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) { Thread t = new Thread(r, "MechKeys-Loader"); t.setDaemon(true); return t; } });
    private final Set<String> held = Collections.synchronizedSet(new HashSet<String>());
    private final Map<String, Long> lastPlay = new HashMap<String, Long>();
    private final Random rnd = new Random();

    public SoundEngine(File root) {
        this.root = root; root.mkdirs(); this.settingsFile = new File(root, "settings.json");
        this.profiles = new ProfileManager(root); this.settings = Settings.load(settingsFile);
    }

    public void start() {
        mixer.setMaxVoices(Settings.MAX_VOICES); mixer.setMaster(1f); mixer.start();
        restoreSlot(keyboard, settings.keyboardProfile); restoreSlot(mouse, settings.mouseProfile);
    }
    /** Loads the saved profile of a slot; if it is gone, falls back to the first installed profile of that kind. */
    private void restoreSlot(Slot s, String savedId) {
        SoundPack p = profiles.get(savedId);
        if (p == null || p.mousePack != s.mouse) { List<SoundPack> l = profiles.list(s.mouse); p = l.isEmpty() ? null : l.get(0); }
        if (p != null) select(p.id, false); else s.status = "No " + (s.mouse ? "mouse" : "keyboard") + " profile - import one";
    }
    /** After a delete/import: refill empty slots. */
    public void ensureSlots() {
        for (Slot s : new Slot[]{keyboard, mouse}) if (s.pack == null || profiles.get(s.pack.id) == null) { s.bank = null; s.pack = null; restoreSlot(s, ""); }
    }

    public void shutdown() {
        try { settings.save(settingsFile); } catch (Exception e) { LOG.warning("Could not save settings: " + e); }
        mixer.shutdown(); loader.shutdownNow(); keyboard.bank = null; mouse.bank = null;
    }
    public synchronized void saveSettings() {
        try { settings.save(settingsFile); } catch (Exception e) { LOG.warning("Could not save settings: " + e); }
    }

    /** Selects and (asynchronously) loads a profile into the right slot. The old bank is released once the new one is ready. */
    public void select(final String id, final boolean persist) {
        loader.execute(new Runnable() { public void run() {
            Slot slot = null; int ver = 0;
            try {
                SoundPack p = profiles.get(id);
                if (p == null) { keyboard.status = "Profile not found or invalid: " + id; return; }
                slot = p.mousePack ? mouse : keyboard; ver = slot.version.incrementAndGet(); slot.status = "Loading " + p.name + "...";
                Bank b = Bank.build(p);
                if (ver != slot.version.get()) return;
                if (b.size() == 0) { slot.status = "Profile has no playable sounds: " + (b.problems.isEmpty() ? "" : b.problems.get(0)); LOG.warning(slot.status); return; }
                held.clear(); slot.pack = p; slot.bank = b;
                slot.status = "Loaded " + p.name + " (" + b.size() + " keys)";
                if (persist) { if (p.mousePack) settings.mouseProfile = p.id; else settings.keyboardProfile = p.id; saveSettings(); }
                for (String w : p.warnings) LOG.warning(p.id + ": " + w);
                for (String w : b.problems) LOG.warning(p.id + ": " + w);
                LOG.info(slot.status);
            } catch (Throwable t) { if (slot != null) slot.status = "Failed to load " + id + ": " + t.getMessage(); LOG.severe("Failed to load " + id + ": " + t); }
        } });
    }

    // ---------------- input ----------------
    /** key = pack key name (e.g. KeyW). Call from the client thread; cheap and non-blocking. */
    public void onKey(String key, boolean down, boolean isRepeat) {
        if (key == null || !settings.keyboardEnabled) return;
        trigger(keyboard, settings.keyboardVolume, key, down, isRepeat);
    }
    public void onMouse(String key, boolean down) {
        if (key == null || !settings.mouseEnabled) return;
        trigger(mouse, settings.mouseVolume, key, down, false);
    }

    private void trigger(Slot s, float vol, String key, boolean down, boolean ignoredRepeat) {
        Bank b = s.bank; SoundPack p = s.pack; if (b == null || p == null) return;
        if (down) {
            if (!held.add(key)) return; // already held (key repeat): stay quiet
            long now = System.nanoTime() / 1000000; Long last = lastPlay.get(key);
            if (last != null && now - last < Settings.MIN_KEY_INTERVAL_MS) return; lastPlay.put(key, now);
        } else {
            if (!held.remove(key)) return; // release without a tracked press
        }
        play(b, p, vol, key, down);
    }
    private void play(Bank b, SoundPack p, float vol, String key, boolean down) {
        Pcm s = down ? b.pickDown(key, rnd) : b.up(key);
        if (s == null) return;
        float pitch = 1f; if (p.randomPitch) pitch *= 1f + (rnd.nextFloat() - 0.5f) * 0.12f;
        mixer.play(s, vol * (down ? 1f : 0.85f), pitch);
    }

    /** Plays a representative sound of a slot (used when the volume slider or profile changes). */
    public void preview(Slot s) {
        Bank b = s.bank; SoundPack p = s.pack; if (b == null || p == null) return;
        String[] keys = s.mouse ? new String[]{"MouseLeft", "MouseRight"} : new String[]{"KeyH", "KeyE", "KeyL", "KeyO", "Space", "Enter"};
        String k = keys[rnd.nextInt(keys.length)]; if (!b.has(k)) { Iterator<String> it = p.defs.keySet().iterator(); if (!it.hasNext()) return; k = it.next(); }
        play(b, p, s.mouse ? settings.mouseVolume : settings.keyboardVolume, k, true);
    }
}
