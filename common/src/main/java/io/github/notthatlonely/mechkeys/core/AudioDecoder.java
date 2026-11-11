package io.github.notthatlonely.mechkeys.core;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Locale;

/** Decodes WAV (JavaSound) and OGG Vorbis (JOrbis, bundled with Minecraft 1.8.9) into {@link Pcm}. MP3 goes through {@link Mp3Converter}. */
public final class AudioDecoder {
    private AudioDecoder() {}
    public static final long MAX_FILE_BYTES = 64L * 1024 * 1024;

    public static boolean supported(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".wav") || n.endsWith(".ogg") || n.endsWith(".mp3");
    }

    public static Pcm decode(File f) throws IOException {
        if (!f.isFile()) throw new FileNotFoundException(f.getPath());
        if (f.length() > MAX_FILE_BYTES) throw new IOException("Audio file too large: " + f.getName());
        String n = f.getName().toLowerCase(Locale.ROOT);
        if (n.endsWith(".mp3")) return decode(Mp3Converter.convert(f)); // converted .wav sibling (needs ffmpeg once)
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        try {
            if (n.endsWith(".ogg")) return OggDecoder.decode(in);
            if (n.endsWith(".wav")) return decodeWav(in);
        } finally { try { in.close(); } catch (IOException ignored) {} }
        throw new IOException("Unsupported audio format (use .ogg or .wav): " + f.getName());
    }

    static Pcm decodeWav(InputStream in) throws IOException {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(in);
            AudioFormat src = ais.getFormat();
            AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16,
                    src.getChannels(), src.getChannels() * 2, src.getSampleRate(), false);
            AudioInputStream conv = AudioSystem.getAudioInputStream(pcm, ais);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[16384]; int r;
            while ((r = conv.read(buf)) > 0) bo.write(buf, 0, r);
            byte[] b = bo.toByteArray();
            short[] s = new short[b.length / 2];
            for (int i = 0; i < s.length; i++) s[i] = (short) ((b[2 * i] & 0xff) | (b[2 * i + 1] << 8));
            return Pcm.normalize(s, src.getChannels(), Math.round(src.getSampleRate()));
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Not a valid WAV file: " + e.getMessage(), e);
        }
    }
}
