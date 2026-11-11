package io.github.notthatlonely.mechkeys.core;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.*;

/** Profile folder management: list, import (zip / folder / URL), delete. */
public final class ProfileManager {
    public static final long MAX_ZIP_TOTAL = 256L * 1024 * 1024;
    public static final int MAX_ENTRIES = 3000;
    private static final Pattern BAD_ID = Pattern.compile("[^A-Za-z0-9._-]");
    public final File root, profilesDir;

    public ProfileManager(File root) {
        this.root = root; this.profilesDir = new File(root, "profiles");
        if (!profilesDir.isDirectory() && !profilesDir.mkdirs()) throw new IllegalStateException("Cannot create " + profilesDir);
    }

    /** Lists all valid profiles; invalid ones are reported in {@code errors} (never thrown). */
    public List<SoundPack> list(List<String> errors) {
        List<SoundPack> out = new ArrayList<SoundPack>();
        File[] dirs = profilesDir.listFiles();
        if (dirs == null) return out;
        Arrays.sort(dirs, new Comparator<File>() { public int compare(File a, File b) { return a.getName().compareToIgnoreCase(b.getName()); } });
        for (File d : dirs) {
            if (!d.isDirectory() || d.getName().startsWith(".")) continue;
            try { SoundPack p = PackLoader.load(d); p.id = d.getName(); out.add(p); }
            catch (PackException e) { if (errors != null) errors.add(d.getName() + ": " + e.getMessage()); }
        }
        return out;
    }
    /** Valid profiles of one kind (mouse or keyboard). */
    public List<SoundPack> list(boolean mouse) {
        List<SoundPack> out = new ArrayList<SoundPack>();
        for (SoundPack p : list(null)) if (p.mousePack == mouse) out.add(p);
        return out;
    }
    public SoundPack get(String id) {
        if (id == null || id.length() == 0) return null;
        File d = new File(profilesDir, sanitize(id)); if (!d.isDirectory()) return null;
        try { SoundPack p = PackLoader.load(d); p.id = d.getName(); return p; } catch (PackException e) { return null; }
    }

    public static String sanitize(String s) {
        String t = BAD_ID.matcher(s == null ? "" : s.trim()).replaceAll("-");
        while (t.startsWith(".") || t.startsWith("-")) t = t.substring(1);
        if (t.length() > 64) t = t.substring(0, 64);
        String low = t.toLowerCase(Locale.ROOT);
        if (low.length() == 0 || low.matches("(con|prn|aux|nul|com[1-9]|lpt[1-9])(\\..*)?")) t = "profile-" + t;
        return t;
    }
    private File uniqueDir(String base) {
        String id = sanitize(base); File d = new File(profilesDir, id); int i = 2;
        while (d.exists()) d = new File(profilesDir, id + "-" + (i++));
        return d;
    }

    // ---------------- import ----------------
    /** Turns whatever the user typed or pasted into an import: URL, file:// link, quoted/Windows/~ path, zip or folder. */
    public String importAny(String input, long maxDownloadBytes) throws PackException {
        String s = input == null ? "" : input.trim();
        while (s.length() > 1 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) s = s.substring(1, s.length() - 1).trim();
        if (s.length() == 0) throw new PackException("Enter a path to a .zip / folder, or a link to a .zip");
        String low = s.toLowerCase(Locale.ROOT);
        if (low.startsWith("http://") || low.startsWith("https://")) return importUrl(s, maxDownloadBytes);
        if (low.startsWith("file:")) { try { s = new File(new URI(s.replace(" ", "%20"))).getPath(); } catch (Exception e) { throw new PackException("Invalid file link"); } }
        if (s.equals("~") || s.startsWith("~/") || s.startsWith("~\\")) s = System.getProperty("user.home") + s.substring(1);
        File f = new File(s);
        if (!f.exists()) throw new PackException("Not found: " + s);
        return importFrom(f);
    }

    /** Imports a .zip or a folder. Returns the installed profile id. Atomic: nothing remains if validation fails. */
    public String importFrom(File src) throws PackException {
        File tmp = new File(profilesDir, ".import-" + System.nanoTime());
        try {
            if (src.isDirectory()) copyDir(src, tmp);
            else if (src.isFile()) unzip(src, tmp);
            else throw new PackException("Not found: " + src);
            File packDir = findPackDir(tmp, 0);
            if (packDir == null) throw new PackException("No config.json found in " + src.getName());
            SoundPack p = PackLoader.load(packDir); // validates fully (and converts .mp3 where possible)
            String base = p.id != null && !p.id.equals(packDir.getName()) ? p.id : (src.isFile() ? stripExt(src.getName()) : src.getName());
            File dest = uniqueDir(base);
            if (!packDir.renameTo(dest)) { copyDir(packDir, dest); }
            return dest.getName();
        } catch (IOException e) { throw new PackException("Import failed: " + e.getMessage(), e);
        } finally { deleteRec(tmp); }
    }

    /** Downloads a zip from an http(s) URL (size capped) and imports it. */
    public String importUrl(String url, long maxBytes) throws PackException {
        File dl = new File(profilesDir, ".download-" + System.nanoTime() + ".zip");
        try {
            URL u = new URL(url.trim());
            if (!u.getProtocol().equals("https") && !u.getProtocol().equals("http")) throw new PackException("Only http(s) links are supported");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent", "MechKeys-Minecraft (unofficial)");
            if (c.getResponseCode() / 100 != 2) throw new PackException("Download failed: HTTP " + c.getResponseCode());
            InputStream in = c.getInputStream(); OutputStream o = new FileOutputStream(dl);
            try { byte[] b = new byte[16384]; long n = 0; int r;
                while ((r = in.read(b)) > 0) { n += r; if (n > maxBytes) throw new PackException("Download exceeds size limit"); o.write(b, 0, r); }
            } finally { in.close(); o.close(); }
            return importFrom(dl);
        } catch (MalformedURLException e) { throw new PackException("Invalid URL", e);
        } catch (IOException e) { throw new PackException("Download failed: " + e.getMessage(), e);
        } finally { dl.delete(); }
    }

    private static ZipFile openZip(File zip) throws PackException, IOException {
        // Zips made by some Windows tools use legacy code pages for names; UTF-8 then fails with IllegalArgumentException.
        try { return new ZipFile(zip); }
        catch (ZipException e) { throw new PackException("Not a valid .zip file: " + zip.getName()); }
        catch (IllegalArgumentException e) {
            try { return new ZipFile(zip, Charset.forName("ISO-8859-1")); } catch (ZipException e2) { throw new PackException("Not a valid .zip file: " + zip.getName()); }
        }
    }

    private void unzip(File zip, File dest) throws IOException, PackException {
        ZipFile z = openZip(zip);
        try {
            if (!dest.mkdirs()) throw new IOException("Cannot create temp dir");
            String base = dest.getCanonicalPath() + File.separator; long total = 0; int n = 0;
            for (Enumeration<? extends ZipEntry> en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (++n > MAX_ENTRIES) throw new PackException("Archive has too many files");
                String name = e.getName().replace('\\', '/'); // some tools write backslashes
                if (name.startsWith("__MACOSX/") || name.contains("/__MACOSX/") || name.endsWith(".DS_Store") || name.endsWith("Thumbs.db")) continue;
                File t = new File(dest, name); String canon = t.getCanonicalPath();
                if (!(canon + File.separator).startsWith(base) && !canon.startsWith(base)) throw new PackException("Unsafe path in archive: " + name);
                if (e.isDirectory() || name.endsWith("/")) { t.mkdirs(); continue; }
                t.getParentFile().mkdirs();
                InputStream in = z.getInputStream(e); OutputStream o = new FileOutputStream(t);
                try { byte[] b = new byte[16384]; int r;
                    while ((r = in.read(b)) > 0) { total += r; if (total > MAX_ZIP_TOTAL) throw new PackException("Archive too large when extracted"); o.write(b, 0, r); }
                } finally { in.close(); o.close(); }
            }
        } finally { z.close(); }
    }

    /** Finds the folder holding config.json: the root, or a nested folder (zips often wrap the pack in a folder). */
    private File findPackDir(File d, int depth) {
        if (new File(d, "config.json").isFile()) return d;
        if (depth >= 3) return null;
        File[] kids = d.listFiles(); if (kids == null) return null;
        Arrays.sort(kids);
        for (File k : kids) if (k.isDirectory()) { File r = findPackDir(k, depth + 1); if (r != null) return r; }
        return null;
    }

    public void delete(String id) throws PackException {
        File d = new File(profilesDir, sanitize(id)); if (!d.isDirectory()) throw new PackException("No such profile");
        try { if (!d.getCanonicalFile().getParentFile().equals(profilesDir.getCanonicalFile())) throw new PackException("Refusing to delete outside profiles folder"); }
        catch (IOException e) { throw new PackException("Bad path"); }
        deleteRec(d); if (d.exists()) throw new PackException("Could not delete all files");
    }

    // ---------------- util ----------------
    static String stripExt(String n) { int i = n.lastIndexOf('.'); return i > 0 ? n.substring(0, i) : n; }
    static void copyDir(File s, File d) throws IOException {
        if (!d.mkdirs() && !d.isDirectory()) throw new IOException("mkdir " + d);
        File[] fs = s.listFiles(); if (fs == null) return;
        for (File f : fs) { if (f.getName().startsWith(".")) continue; File t = new File(d, f.getName()); if (f.isDirectory()) copyDir(f, t); else copyFile(f, t); }
    }
    static void copyFile(File s, File d) throws IOException {
        InputStream in = new FileInputStream(s); OutputStream o = new FileOutputStream(d);
        try { byte[] b = new byte[16384]; int r; while ((r = in.read(b)) > 0) o.write(b, 0, r); } finally { in.close(); o.close(); }
    }
    static void deleteRec(File f) {
        if (f == null || !f.exists()) return;
        File[] k = f.isDirectory() ? f.listFiles() : null;
        if (k != null) for (File c : k) deleteRec(c);
        f.delete();
    }
}
