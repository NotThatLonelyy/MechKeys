package io.github.notthatlonely.mechkeys.core;

import java.io.File;
import java.util.*;

/** Fully decoded, sliced samples for a profile. Immutable after build; dropped (GC'd) when the profile changes. */
public final class Bank {
    public final SoundPack pack;
    private final Map<String, Pcm[]> down = new HashMap<String, Pcm[]>();
    private final Map<String, Pcm> up = new HashMap<String, Pcm>();
    public final List<String> problems = new ArrayList<String>();
    private Bank(SoundPack p) { pack = p; }

    public static Bank build(SoundPack p) {
        Bank b = new Bank(p);
        Map<String, Pcm> cache = new HashMap<String, Pcm>();
        for (Map.Entry<String, List<SoundPack.Segment>> e : p.defs.entrySet()) {
            String key = e.getKey(); List<SoundPack.Segment> segs = e.getValue();
            if (segs.size() > 2) { b.problems.add(key + ": >2 segments, ignored"); continue; }
            List<Pcm> downs = new ArrayList<Pcm>();
            Pcm d = b.cut(p, segs.get(0), cache); if (d != null) downs.add(d);
            if (!downs.isEmpty()) b.down.put(key, downs.toArray(new Pcm[0]));
            if (segs.size() == 2) { Pcm u = b.cut(p, segs.get(1), cache); if (u != null) b.up.put(key, u); }
        }
        return b;
    }

    private Pcm cut(SoundPack p, SoundPack.Segment s, Map<String, Pcm> cache) {
        String f = s.file != null ? s.file : p.mainAudio;
        if (f == null) return null;
        Pcm full = cache.get(f);
        if (full == null && !cache.containsKey(f)) {
            try {
                File t = PackLoader.safeFile(p.dir, f);
                full = t == null ? null : AudioDecoder.decode(t);
            } catch (Exception | OutOfMemoryError ex) { problems.add(f + ": " + ex.getMessage()); }
            cache.put(f, full);
        }
        if (full == null) return null;
        Pcm out = s.endMs < 0 ? full : full.slice(s.startMs, s.endMs);
        return out.frames() < 8 ? null : out;
    }

    /** Keys a pack does not define fall back to their usual twin (e.g. right Shift -> left Shift) so nothing is silent. */
    private static final Map<String, String> ALIAS = new HashMap<String, String>();
    static {
        ALIAS.put("ShiftRight", "ShiftLeft"); ALIAS.put("ControlRight", "ControlLeft"); ALIAS.put("AltRight", "AltLeft");
        ALIAS.put("MetaLeft", "ControlLeft"); ALIAS.put("MetaRight", "ControlLeft"); ALIAS.put("ContextMenu", "ControlLeft");
        ALIAS.put("NumpadSubtract", "NumpadAdd"); ALIAS.put("NumpadDivide", "NumpadMultiply"); ALIAS.put("NumLock", "CapsLock");
        ALIAS.put("Backslash", "Backspace"); ALIAS.put("MouseMiddle", "MouseLeft");
    }
    private String resolve(String key) { if (down.containsKey(key)) return key; String a = ALIAS.get(key); return a != null && down.containsKey(a) ? a : key; }

    public Pcm pickDown(String key, Random r) { Pcm[] a = down.get(resolve(key)); return a == null ? null : a[a.length == 1 ? 0 : r.nextInt(a.length)]; }
    public Pcm up(String key) { return up.get(resolve(key)); }
    public boolean has(String key) { return down.containsKey(resolve(key)); }
    public int size() { return down.size(); }
    public long bytes() { long n = 0; for (Pcm[] a : down.values()) for (Pcm x : a) n += x.data.length * 2L; for (Pcm x : up.values()) n += x.data.length * 2L; return n; }
}
