package io.github.notthatlonely.mechkeys.core;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/** Reads Mechvibes V2 and legacy V1 config.json into {@link SoundPack}. */
public final class PackLoader {
    private PackLoader() {}
    static final Charset UTF8 = Charset.forName("UTF-8");

    public static String readText(File f) throws IOException {
        if (f.length() > 8L * 1024 * 1024) throw new IOException("config too large");
        FileInputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(); byte[] b = new byte[8192]; int r;
            while ((r = in.read(b)) > 0) bo.write(b, 0, r);
            return new String(bo.toByteArray(), UTF8);
        } finally { in.close(); }
    }
    public static void writeText(File f, String s) throws IOException {
        File tmp = new File(f.getPath() + ".tmp");
        FileOutputStream o = new FileOutputStream(tmp);
        try { o.write(s.getBytes(UTF8)); } finally { o.close(); }
        if (f.exists() && !f.delete()) throw new IOException("Cannot replace " + f);
        if (!tmp.renameTo(f)) throw new IOException("Cannot write " + f);
    }

    @SuppressWarnings("unchecked")
    public static SoundPack load(File dir) throws PackException {
        File cfg = new File(dir, "config.json");
        if (!cfg.isFile()) throw new PackException("No config.json in " + dir.getName());
        Map<String, Object> m;
        try {
            Object o = Json.parse(readText(cfg));
            if (!(o instanceof Map)) throw new PackException("config.json is not a JSON object");
            m = (Map<String, Object>) o;
        } catch (IOException e) { throw new PackException("Cannot read config.json: " + e.getMessage(), e);
        } catch (RuntimeException e) { throw new PackException("config.json is corrupted: " + e.getMessage(), e); }

        SoundPack p = new SoundPack(); p.dir = dir;
        p.id = str(m, "id", dir.getName());
        p.name = str(m, "name", dir.getName());
        p.author = str(m, "author", str(m, "m_author", "Unknown"));
        p.description = str(m, "description", ""); p.icon = str(m, "icon", null);
        Object ver = m.get("version"); if (ver instanceof String) p.version = (String) ver;
        Object opts = m.get("options");
        if (opts instanceof Map) {
            Map<String, Object> om = (Map<String, Object>) opts;
            if (om.get("recommended_volume") instanceof Number) p.recommendedVolume = ((Number) om.get("recommended_volume")).floatValue();
            if (Boolean.TRUE.equals(om.get("random_pitch"))) p.randomPitch = true;
        }
        boolean v2 = m.containsKey("definitions") || m.containsKey("defs") || "2".equals(String.valueOf(m.get("config_version")))
                || Double.valueOf(2).equals(m.get("config_version"));
        if (v2 && !m.containsKey("defines")) loadV2(p, m); else loadV1(p, m);
        if (p.defs.isEmpty()) throw new PackException("Profile defines no keys");
        validateFiles(p);
        return p;
    }

    @SuppressWarnings("unchecked")
    private static void loadV2(SoundPack p, Map<String, Object> m) throws PackException {
        Object cv = m.get("config_version");
        if (cv != null) { try { if (Double.parseDouble(String.valueOf(cv)) > 2) throw new PackException("Profile needs a newer config version (" + cv + ")"); } catch (NumberFormatException ignored) {} }
        String method = str(m, "definition_method", "single");
        p.multi = "multi".equals(method);
        p.mainAudio = str(m, "audio_file", null);
        Object d = m.containsKey("definitions") ? m.get("definitions") : m.get("defs");
        if (!(d instanceof Map)) throw new PackException("Missing 'definitions'");
        if (!p.multi && p.mainAudio == null) throw new PackException("Single-file profile has no 'audio_file'");
        for (Map.Entry<String, Object> e : ((Map<String, Object>) d).entrySet()) {
            if (!(e.getValue() instanceof Map)) { p.warnings.add("Skipped bad definition: " + e.getKey()); continue; }
            Map<String, Object> def = (Map<String, Object>) e.getValue();
            String file = p.multi ? str(def, "audio_file", null) : null;
            Object t = def.get("timing");
            List<SoundPack.Segment> segs = new ArrayList<SoundPack.Segment>();
            if (t instanceof List) {
                for (Object pair : (List<Object>) t) {
                    if (pair instanceof List && ((List<Object>) pair).size() >= 2
                            && ((List<Object>) pair).get(0) instanceof Number && ((List<Object>) pair).get(1) instanceof Number) {
                        List<Object> l = (List<Object>) pair;
                        double a = ((Number) l.get(0)).doubleValue(), b = ((Number) l.get(1)).doubleValue();
                        if (b <= a) { p.warnings.add("Empty timing for " + e.getKey()); continue; }
                        segs.add(new SoundPack.Segment(file, a, b));
                    }
                }
            } else if (p.multi && file != null) segs.add(new SoundPack.Segment(file, 0, -1));
            if (segs.isEmpty()) { p.warnings.add("No usable timing for " + e.getKey()); continue; }
            if (p.multi && file == null && p.mainAudio == null) { p.warnings.add("No audio for " + e.getKey()); continue; }
            p.defs.put(e.getKey(), segs);
        }
        p.mousePack = !p.defs.isEmpty() && allMouse(p.defs.keySet());
    }

    @SuppressWarnings("unchecked")
    private static void loadV1(SoundPack p, Map<String, Object> m) throws PackException {
        String kdt = str(m, "key_define_type", "single");
        p.multi = "multi".equals(kdt);
        p.mainAudio = str(m, "sound", null);
        p.mousePack = Boolean.TRUE.equals(m.get("mouse"));
        Object d = m.get("defines");
        if (!(d instanceof Map)) throw new PackException("Unrecognised config: neither V2 'definitions' nor V1 'defines'");
        if (!p.multi && p.mainAudio == null) throw new PackException("V1 profile has no 'sound' file");
        for (Map.Entry<String, Object> e : ((Map<String, Object>) d).entrySet()) {
            String k = e.getKey(); boolean release = k.length() > 1 && k.startsWith("0");
            int code; try { code = Integer.parseInt(k); } catch (NumberFormatException ex) { p.warnings.add("Bad key id " + k); continue; }
            String name = p.mousePack ? KeyCodes.mouse(code - 1) : KeyCodes.fromV1(code);
            if (name == null) { p.warnings.add("Unmapped V1 key " + k); continue; }
            SoundPack.Segment seg = null; Object v = e.getValue();
            if (p.multi && v instanceof String) seg = new SoundPack.Segment((String) v, 0, -1);
            else if (!p.multi && v instanceof List && ((List<Object>) v).size() >= 2
                    && ((List<Object>) v).get(0) instanceof Number && ((List<Object>) v).get(1) instanceof Number) {
                double s = ((Number) ((List<Object>) v).get(0)).doubleValue(), du = ((Number) ((List<Object>) v).get(1)).doubleValue();
                if (du > 0) seg = new SoundPack.Segment(null, s, s + du);
            }
            if (seg == null) { p.warnings.add("Bad define for " + k); continue; }
            List<SoundPack.Segment> l = p.defs.get(name);
            if (l == null) { l = new ArrayList<SoundPack.Segment>(); p.defs.put(name, l); }
            if (release) { if (l.size() == 1) l.add(seg); } else l.add(0, seg); // press first, release second
            if (l.size() > 2) l.remove(2);
        }
    }

    private static boolean allMouse(Set<String> keys) { for (String k : keys) if (!KeyCodes.isMouseName(k)) return false; return true; }

    static void validateFiles(SoundPack p) throws PackException {
        Set<String> files = new LinkedHashSet<String>();
        if (p.mainAudio != null) files.add(p.mainAudio);
        for (List<SoundPack.Segment> l : p.defs.values()) for (SoundPack.Segment s : l) if (s.file != null) files.add(s.file);
        int missing = 0;
        for (String f : files) {
            File t = safeFile(p.dir, f);
            if (t == null) throw new PackException("Unsafe file path in profile: " + f);
            if (!AudioDecoder.supported(f)) throw new PackException("Unsupported audio format (use .wav, .ogg or .mp3): " + f);
            if (!t.isFile()) { missing++; p.warnings.add("Missing audio file: " + f); continue; }
            if (Mp3Converter.isMp3(f)) { try { Mp3Converter.convert(t); } catch (IOException e) { throw new PackException(e.getMessage(), e); } }
        }
        if (!files.isEmpty() && missing == files.size()) throw new PackException("None of the profile's audio files exist");
        if (!p.multi && p.mainAudio != null) { File m = safeFile(p.dir, p.mainAudio); if (m == null || !m.isFile()) throw new PackException("Audio file missing: " + p.mainAudio); }
    }

    /** Resolves a relative path inside dir, rejecting traversal. */
    public static File safeFile(File dir, String rel) {
        if (rel == null || rel.length() == 0) return null;
        try {
            File base = dir.getCanonicalFile(); File t = new File(base, rel).getCanonicalFile();
            return t.getPath().startsWith(base.getPath() + File.separator) ? t : null;
        } catch (IOException e) { return null; }
    }

    public static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }

    static String str(Map<String, Object> m, String k, String def) { Object v = m.get(k); return v instanceof String && ((String) v).length() > 0 ? (String) v : def; }
}
