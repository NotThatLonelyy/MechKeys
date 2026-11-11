package io.github.notthatlonely.mechkeys.core;

import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Headless checks of the core package (no Minecraft needed). run() returns the number of failures. */
public final class SelfTest {
    static int fails, checks;
    static void check(boolean ok, String what) { checks++; if (!ok) { fails++; System.out.println("FAIL: " + what); } }

    static File tmp(String n) throws IOException { File f = File.createTempFile("mk-" + n, ""); f.delete(); f.mkdirs(); return f; }
    static void wav(File f, int ms) throws IOException {
        int n = 44100 * ms / 1000; ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream d = new DataOutputStream(b);
        for (int i = 0; i < n; i++) { short v = (short) (Math.sin(i / 20.0) * 8000); d.writeByte(v & 255); d.writeByte(v >> 8); d.writeByte(v & 255); d.writeByte(v >> 8); }
        byte[] pcm = b.toByteArray(); DataOutputStream o = new DataOutputStream(new FileOutputStream(f));
        o.writeBytes("RIFF"); o.writeInt(Integer.reverseBytes(36 + pcm.length)); o.writeBytes("WAVEfmt "); o.writeInt(Integer.reverseBytes(16));
        o.writeShort(Short.reverseBytes((short) 1)); o.writeShort(Short.reverseBytes((short) 2)); o.writeInt(Integer.reverseBytes(44100)); o.writeInt(Integer.reverseBytes(44100 * 4));
        o.writeShort(Short.reverseBytes((short) 4)); o.writeShort(Short.reverseBytes((short) 16)); o.writeBytes("data"); o.writeInt(Integer.reverseBytes(pcm.length)); o.write(pcm); o.close();
    }
    static String cfg(String audio, String keys) {
        return "{\"id\":\"t\",\"name\":\"Test\",\"config_version\":\"2\",\"definition_method\":\"single\",\"audio_file\":\"" + audio + "\",\"definitions\":{" + keys + "}}";
    }
    static void write(File f, String s) throws IOException { f.getParentFile().mkdirs(); FileOutputStream o = new FileOutputStream(f); o.write(s.getBytes("UTF-8")); o.close(); }
    static File zip(File out, Map<String, File> entries, Map<String, String> text) throws IOException {
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(out));
        for (Map.Entry<String, File> e : entries.entrySet()) { z.putNextEntry(new ZipEntry(e.getKey())); FileInputStream in = new FileInputStream(e.getValue()); byte[] b = new byte[8192]; int r; while ((r = in.read(b)) > 0) z.write(b, 0, r); in.close(); z.closeEntry(); }
        for (Map.Entry<String, String> e : text.entrySet()) { z.putNextEntry(new ZipEntry(e.getKey())); z.write(e.getValue().getBytes("UTF-8")); z.closeEntry(); }
        z.close(); return out;
    }
    static boolean exists(File root, String id) { return new File(root, "profiles/" + id).isDirectory(); }

    public static int run() throws Exception {
        fails = 0; checks = 0;
        String KEYS = "\"KeyA\":{\"timing\":[[0,100]]},\"KeyB\":{\"timing\":[[100,200]]}", MKEYS = "\"MouseLeft\":{\"timing\":[[0,100],[100,200]]},\"MouseRight\":{\"timing\":[[200,300]]}";
        File w = tmp("w"); File a = new File(w, "a.wav"); wav(a, 400);

        // --- bundled defaults load, and go to the right slot ---
        File root = tmp("root"); ProfileManager pm = new ProfileManager(root);
        String idx = readRes("/assets/mechkeys/defaults/index.txt");
        check(idx != null, "defaults index present");
        if (idx != null) for (String line : idx.split("\\r?\\n")) { line = line.trim(); if (line.length() == 0) continue; File o = new File(root, "profiles/" + line); o.getParentFile().mkdirs(); copyRes("/assets/mechkeys/defaults/" + line, o); }
        SoundPack kb = pm.get("keychron-q3-bluex"), ms = pm.get("superstrike-mouse");
        check(kb != null && !kb.mousePack && kb.keyCount() > 90, "keyboard default loads as keyboard (" + (kb == null ? "null" : kb.keyCount()) + " keys)");
        check(ms != null && ms.mousePack && ms.keyCount() == 2, "mouse default loads as mouse");
        if (kb != null) { Bank b = Bank.build(kb); check(b.size() == kb.keyCount() && b.problems.isEmpty(), "keyboard bank builds"); check(b.has("ShiftRight") && b.has("KeyW"), "alias + keys"); }
        if (ms != null) { Bank b = Bank.build(ms); check(b.has("MouseLeft") && b.up("MouseLeft") != null && b.up("MouseRight") != null, "mouse press+release segments"); }
        check(pm.list(true).size() == 1 && pm.list(false).size() == 1, "list by kind");

        // --- import: zip, wrapped zip, backslash entries, nested folder, folder, quoted path ---
        File r2 = tmp("r2"); ProfileManager p2 = new ProfileManager(r2);
        Map<String, File> f1 = new LinkedHashMap<String, File>(); f1.put("a.wav", a);
        Map<String, String> t1 = new LinkedHashMap<String, String>(); t1.put("config.json", cfg("a.wav", KEYS));
        String id1 = p2.importFrom(zip(new File(w, "flat.zip"), f1, t1)); check(p2.get(id1) != null, "flat zip imports");
        Map<String, File> f2 = new LinkedHashMap<String, File>(); f2.put("Wrapped Pack/a.wav", a);
        Map<String, String> t2 = new LinkedHashMap<String, String>(); t2.put("Wrapped Pack/config.json", cfg("a.wav", MKEYS));
        String id2 = p2.importFrom(zip(new File(w, "wrapped.zip"), f2, t2)); check(p2.get(id2) != null && p2.get(id2).mousePack, "zip wrapped in a folder imports (mouse)");
        Map<String, File> f3 = new LinkedHashMap<String, File>(); f3.put("pack\\a.wav", a);
        Map<String, String> t3 = new LinkedHashMap<String, String>(); t3.put("pack\\config.json", cfg("a.wav", KEYS));
        String id3 = p2.importFrom(zip(new File(w, "bs.zip"), f3, t3)); check(p2.get(id3) != null, "backslash entry names import");
        Map<String, File> f4 = new LinkedHashMap<String, File>(); f4.put("x/y/a.wav", a);
        Map<String, String> t4 = new LinkedHashMap<String, String>(); t4.put("x/y/config.json", cfg("a.wav", KEYS)); t4.put("__MACOSX/x/._config.json", "junk");
        String id4 = p2.importAny("\"" + zip(new File(w, "deep.zip"), f4, t4).getPath() + "\"", 1 << 20); check(p2.get(id4) != null, "deep zip + quoted path + __MACOSX");
        File fol = new File(w, "folder"); fol.mkdirs(); copy(a, new File(fol, "a.wav")); write(new File(fol, "config.json"), cfg("a.wav", KEYS));
        check(p2.get(p2.importFrom(fol)) != null, "folder imports");
        check(p2.get(p2.importAny("file://" + fol.getPath(), 1 << 20)) != null, "file:// link imports");
        // BOM config
        File bom = new File(w, "bom"); bom.mkdirs(); copy(a, new File(bom, "a.wav")); write(new File(bom, "config.json"), "﻿" + cfg("a.wav", KEYS));
        check(p2.get(p2.importFrom(bom)) != null, "BOM config imports");

        // --- rejections leave nothing behind ---
        int before = p2.list(null).size();
        Map<String, File> ev = new LinkedHashMap<String, File>(); Map<String, String> te = new LinkedHashMap<String, String>(); te.put("../evil.txt", "x"); te.put("config.json", "{}");
        try { p2.importFrom(zip(new File(w, "slip.zip"), ev, te)); check(false, "zip-slip rejected"); } catch (PackException e) { check(true, ""); }
        write(new File(w, "notzip.zip"), "hello"); try { p2.importFrom(new File(w, "notzip.zip")); check(false, "non-zip rejected"); } catch (PackException e) { check(e.getMessage().contains("zip"), "non-zip message: " + e.getMessage()); }
        Map<String, String> tn = new LinkedHashMap<String, String>(); tn.put("readme.txt", "x");
        try { p2.importFrom(zip(new File(w, "nocfg.zip"), new LinkedHashMap<String, File>(), tn)); check(false, "no config rejected"); } catch (PackException e) { check(e.getMessage().contains("config.json"), "no-config message"); }
        try { p2.importAny("", 1); check(false, "empty rejected"); } catch (PackException e) { check(true, ""); }
        try { p2.importAny(new File(w, "nope.zip").getPath(), 1); check(false, "missing rejected"); } catch (PackException e) { check(e.getMessage().startsWith("Not found"), "missing message"); }
        File dm = new File(w, "mp3pack"); dm.mkdirs(); write(new File(dm, "s.mp3"), "ID3 not real audio"); write(new File(dm, "config.json"), cfg("s.mp3", KEYS));
        try { p2.importFrom(dm); check(false, "undecodable mp3 rejected"); } catch (PackException e) { check(e.getMessage().toLowerCase().contains("mp3"), "mp3 message is clear: " + e.getMessage()); }
        check(p2.list(null).size() == before, "failed imports leave no profile");
        String[] leftovers = p2.profilesDir.list(); int dots = 0; for (String s : leftovers) if (s.startsWith(".")) dots++; check(dots == 0, "no temp dirs left");

        // --- engine: two independent slots ---
        File r3 = tmp("r3"); SoundEngine en = new SoundEngine(r3);
        en.profiles.importFrom(fol); String mid = en.profiles.importFrom(dirWith(w, "mouse", a, MKEYS));
        en.settings.keyboardProfile = ""; en.start();
        for (int i = 0; i < 50 && (!en.keyboard.loaded() || !en.mouse.loaded()); i++) Thread.sleep(100);
        check(en.keyboard.loaded() && !en.keyboard.pack().mousePack, "keyboard slot filled with keyboard pack");
        check(en.mouse.loaded() && en.mouse.pack().mousePack, "mouse slot filled with mouse pack");
        long r0 = en.mixer.requested;
        en.onKey("KeyA", true, false); en.onKey("KeyA", false, false); en.onMouse("MouseLeft", true); en.onMouse("MouseLeft", false);
        check(en.mixer.requested - r0 == 3, "key press + mouse press/release routed (" + (en.mixer.requested - r0) + ")");
        en.settings.keyboardEnabled = false; r0 = en.mixer.requested; en.onKey("KeyB", true, false); en.onMouse("MouseRight", true);
        check(en.mixer.requested - r0 == 1, "keyboard off does not silence mouse");
        en.settings.mouseEnabled = false; r0 = en.mixer.requested; en.onMouse("MouseRight", false); en.onMouse("MouseRight", true);
        check(en.mixer.requested == r0, "mouse off silences mouse");
        en.settings.keyboardEnabled = true; en.settings.mouseEnabled = true; r0 = en.mixer.requested; en.onKey("KeyA", true, false); en.onKey("KeyA", true, true); check(en.mixer.requested - r0 <= 1, "held key repeat ignored");
        en.profiles.delete(en.mouse.pack().id); en.ensureSlots(); Thread.sleep(300);
        check(en.mouse.pack() == null || !en.mouse.pack().id.equals(mid), "deleted profile no longer active");
        en.saveSettings(); Settings s2 = Settings.load(new File(r3, "settings.json")); check(s2.keyboardEnabled && s2.mouseEnabled && s2.keyboardVolume == 1f, "settings round trip");
        en.shutdown();
        System.out.println("SelfTest: " + (checks - fails) + "/" + checks + " checks passed");
        return fails;
    }
    static File dirWith(File w, String n, File audio, String keys) throws IOException { File d = new File(w, n); d.mkdirs(); copy(audio, new File(d, "a.wav")); write(new File(d, "config.json"), cfg("a.wav", keys)); return d; }
    static void copy(File s, File d) throws IOException { FileInputStream i = new FileInputStream(s); FileOutputStream o = new FileOutputStream(d); byte[] b = new byte[8192]; int r; while ((r = i.read(b)) > 0) o.write(b, 0, r); i.close(); o.close(); }
    static String readRes(String p) throws IOException { InputStream in = SelfTest.class.getResourceAsStream(p); if (in == null) return null; ByteArrayOutputStream b = new ByteArrayOutputStream(); byte[] x = new byte[4096]; int r; while ((r = in.read(x)) > 0) b.write(x, 0, r); in.close(); return new String(b.toByteArray(), "UTF-8"); }
    static void copyRes(String p, File d) throws IOException { InputStream in = SelfTest.class.getResourceAsStream(p); FileOutputStream o = new FileOutputStream(d); byte[] x = new byte[8192]; int r; while ((r = in.read(x)) > 0) o.write(x, 0, r); in.close(); o.close(); }
    public static void main(String[] a) throws Exception { System.exit(run() == 0 ? 0 : 1); }
}
