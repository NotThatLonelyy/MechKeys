package io.github.notthatlonely.mechkeys.core;

import java.io.File;
import java.util.*;

/** Deliberately tiny: keyboard and mouse are independent (own profile, on/off, volume). Everything else is fixed. */
public final class Settings {
    public boolean keyboardEnabled = true, mouseEnabled = true;
    public float keyboardVolume = 1f, mouseVolume = 1f;   // 0..2
    public String keyboardProfile = "", mouseProfile = "";

    // Fixed behaviour (not user-configurable any more)
    public static final int MAX_VOICES = 24, MIN_KEY_INTERVAL_MS = 8;

    public static Settings load(File f) {
        Settings s = new Settings(); if (!f.isFile()) return s;
        try {
            @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) Json.parse(PackLoader.readText(f));
            s.keyboardEnabled = b(m, "keyboardEnabled", s.keyboardEnabled); s.mouseEnabled = b(m, "mouseEnabled", s.mouseEnabled);
            s.keyboardVolume = PackLoader.clamp(n(m, "keyboardVolume", s.keyboardVolume), 0f, 2f);
            s.mouseVolume = PackLoader.clamp(n(m, "mouseVolume", s.mouseVolume), 0f, 2f);
            if (m.get("keyboardProfile") instanceof String) s.keyboardProfile = (String) m.get("keyboardProfile");
            if (m.get("mouseProfile") instanceof String) s.mouseProfile = (String) m.get("mouseProfile");
        } catch (Exception e) { /* corrupted: fall back to defaults */ }
        return s;
    }
    public void save(File f) throws java.io.IOException {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("keyboardEnabled", keyboardEnabled); m.put("mouseEnabled", mouseEnabled);
        m.put("keyboardVolume", (double) keyboardVolume); m.put("mouseVolume", (double) mouseVolume);
        m.put("keyboardProfile", keyboardProfile); m.put("mouseProfile", mouseProfile);
        PackLoader.writeText(f, Json.write(m));
    }
    private static boolean b(Map<String, Object> m, String k, boolean d) { Object v = m.get(k); return v instanceof Boolean ? (Boolean) v : d; }
    private static float n(Map<String, Object> m, String k, float d) { Object v = m.get(k); return v instanceof Number ? ((Number) v).floatValue() : d; }
}
