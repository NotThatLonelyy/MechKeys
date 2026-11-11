package io.github.notthatlonely.mechkeys.core;

/** Decoded audio: interleaved stereo signed 16-bit at {@link #RATE} Hz. */
public final class Pcm {
    public static final int RATE = 44100;
    public final short[] data; // interleaved L,R
    public Pcm(short[] data) { this.data = data; }
    public int frames() { return data.length / 2; }

    /** Cuts [startMs,endMs) into a new Pcm (clamped to bounds). */
    public Pcm slice(double startMs, double endMs) {
        int a = clamp((int) Math.round(startMs * RATE / 1000.0), 0, frames());
        int b = clamp((int) Math.round(endMs * RATE / 1000.0), a, frames());
        short[] out = new short[(b - a) * 2];
        System.arraycopy(data, a * 2, out, 0, out.length);
        return new Pcm(out);
    }
    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Converts arbitrary interleaved 16-bit PCM to stereo @ RATE with linear resampling. */
    public static Pcm normalize(short[] src, int channels, int rate) {
        if (channels < 1) throw new IllegalArgumentException("channels");
        int inFrames = src.length / channels;
        long outFrames = Math.max(0L, (long) inFrames * RATE / rate);
        if (outFrames > 200L * RATE) throw new IllegalArgumentException("Audio too long (>200s)");
        short[] out = new short[(int) outFrames * 2];
        for (int i = 0; i < (int) outFrames; i++) {
            double pos = (double) i * rate / RATE;
            int i0 = (int) pos; int i1 = Math.min(i0 + 1, inFrames - 1); double f = pos - i0;
            for (int ch = 0; ch < 2; ch++) {
                int c = channels == 1 ? 0 : Math.min(ch, channels - 1);
                double v0 = src[i0 * channels + c], v1 = src[i1 * channels + c];
                out[i * 2 + ch] = (short) Math.round(v0 + (v1 - v0) * f);
            }
        }
        return new Pcm(out);
    }
}
