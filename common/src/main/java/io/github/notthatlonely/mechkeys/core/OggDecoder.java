package io.github.notthatlonely.mechkeys.core;

import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;
import java.io.*;

/** Ogg Vorbis decoder on top of JOrbis (ships inside Minecraft 1.8.9's codecjorbis library). */
final class OggDecoder {
    private OggDecoder() {}

    static Pcm decode(InputStream in) throws IOException {
        SyncState oy = new SyncState(); StreamState os = new StreamState();
        Page og = new Page(); Packet op = new Packet();
        Info vi = new Info(); Comment vc = new Comment(); DspState vd = new DspState(); Block vb = new Block(vd);
        oy.init();
        ShortSink sink = new ShortSink();
        int convsize = 4096; byte[] buffer; int bytes;

        // --- headers ---
        int idx = oy.buffer(convsize); buffer = oy.data;
        bytes = in.read(buffer, idx, convsize);
        if (bytes <= 0) throw new IOException("Empty OGG file");
        oy.wrote(bytes);
        if (oy.pageout(og) != 1) throw new IOException("Not an Ogg Vorbis stream");
        os.init(og.serialno());
        vi.init(); vc.init();
        if (os.pagein(og) < 0) throw new IOException("Error reading first page");
        if (os.packetout(op) != 1) throw new IOException("Error reading initial header packet");
        if (vi.synthesis_headerin(vc, op) < 0) throw new IOException("Not a Vorbis audio stream");
        int i = 0;
        while (i < 2) {
            while (i < 2) {
                int result = oy.pageout(og);
                if (result == 0) break;
                if (result == 1) {
                    os.pagein(og);
                    while (i < 2) {
                        result = os.packetout(op);
                        if (result == 0) break;
                        if (result == -1) throw new IOException("Corrupt secondary header");
                        vi.synthesis_headerin(vc, op); i++;
                    }
                }
            }
            idx = oy.buffer(convsize); buffer = oy.data;
            bytes = in.read(buffer, idx, convsize);
            if (bytes <= 0 && i < 2) throw new IOException("Truncated OGG headers");
            oy.wrote(bytes);
        }
        int channels = vi.channels, rate = vi.rate;
        if (channels < 1 || channels > 8 || rate < 8000 || rate > 192000) throw new IOException("Unsupported OGG layout");
        vd.synthesis_init(vi); vb.init(vd);
        float[][][] pcmIndex = new float[1][][]; int[] pcmOffs = new int[channels];

        boolean eos = false;
        while (!eos) {
            while (!eos) {
                int result = oy.pageout(og);
                if (result == 0) break;
                if (result == -1) continue; // missing/corrupt page: skip
                os.pagein(og);
                while (true) {
                    result = os.packetout(op);
                    if (result == 0) break;
                    if (result == -1) continue;
                    if (vb.synthesis(op) == 0) vd.synthesis_blockin(vb);
                    int samples;
                    while ((samples = vd.synthesis_pcmout(pcmIndex, pcmOffs)) > 0) {
                        float[][] pcm = pcmIndex[0];
                        for (int j = 0; j < samples; j++)
                            for (int ch = 0; ch < channels; ch++) {
                                int v = (int) (pcm[ch][pcmOffs[ch] + j] * 32767.0f);
                                if (v > 32767) v = 32767; if (v < -32768) v = -32768;
                                sink.add((short) v);
                            }
                        vd.synthesis_read(samples);
                        if (sink.size() > 200L * rate * channels) throw new IOException("Audio too long (>200s)");
                    }
                }
                if (og.eos() != 0) eos = true;
            }
            if (!eos) {
                idx = oy.buffer(convsize); buffer = oy.data;
                bytes = in.read(buffer, idx, convsize);
                if (bytes <= 0) break;
                oy.wrote(bytes);
            }
        }
        if (sink.size() == 0) throw new IOException("OGG contained no audio");
        return Pcm.normalize(sink.toArray(), channels, rate);
    }

    private static final class ShortSink {
        private short[] a = new short[1 << 16]; private int n;
        void add(short v) { if (n == a.length) a = java.util.Arrays.copyOf(a, n * 2); a[n++] = v; }
        int size() { return n; }
        short[] toArray() { return java.util.Arrays.copyOf(a, n); }
    }
}
