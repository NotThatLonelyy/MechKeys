package io.github.notthatlonely.mechkeys.core;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Minecraft 1.8.9 ships no MP3 decoder. When a pack uses .mp3 we convert it once to a sibling
 * "<name>.mkconv.wav" using an installed ffmpeg (if any); the converted file is what gets played from then on.
 */
final class Mp3Converter {
    private Mp3Converter() {}
    private static volatile boolean ffmpegMissing;

    static File wavFor(File mp3) {
        String n = mp3.getName(); int i = n.lastIndexOf('.');
        return new File(mp3.getParentFile(), (i > 0 ? n.substring(0, i) : n) + ".mkconv.wav");
    }
    static boolean isMp3(String name) { return name.toLowerCase(java.util.Locale.ROOT).endsWith(".mp3"); }

    /** Returns the converted wav (converting if needed) or throws a message that tells the user what to do. */
    static File convert(File mp3) throws IOException {
        File out = wavFor(mp3);
        if (out.isFile() && out.length() > 44) return out;
        final String help = mp3.getName() + " is MP3 and Minecraft 1.8.9 cannot decode MP3. Convert it to .wav or .ogg (or install ffmpeg and re-import).";
        if (ffmpegMissing) throw new IOException(help);
        File tmp = new File(out.getPath() + ".tmp");
        ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-y", "-v", "error", "-i", mp3.getPath(), "-vn", "-c:a", "pcm_s16le", "-f", "wav", tmp.getPath());
        pb.redirectErrorStream(true);
        Process p;
        try { p = pb.start(); } catch (IOException e) { ffmpegMissing = true; throw new IOException(help); }
        final Process fp = p;
        Thread drain = new Thread(new Runnable() { public void run() { try { InputStream in = fp.getInputStream(); byte[] b = new byte[4096]; while (in.read(b) >= 0) { } } catch (IOException ignored) {} } }, "MechKeys-ffmpeg");
        drain.setDaemon(true); drain.start();
        try {
            if (!p.waitFor(60, TimeUnit.SECONDS)) { p.destroyForcibly(); tmp.delete(); throw new IOException("ffmpeg timed out converting " + mp3.getName()); }
        } catch (InterruptedException e) { p.destroyForcibly(); tmp.delete(); Thread.currentThread().interrupt(); throw new IOException("Interrupted"); }
        if (p.exitValue() != 0 || !tmp.isFile() || tmp.length() <= 44) { tmp.delete(); throw new IOException("ffmpeg could not convert " + mp3.getName()); }
        if (out.exists()) out.delete();
        if (!tmp.renameTo(out)) { tmp.delete(); throw new IOException("Cannot write " + out.getName()); }
        return out;
    }
}
