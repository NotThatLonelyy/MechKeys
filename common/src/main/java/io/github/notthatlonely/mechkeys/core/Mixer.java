package io.github.notthatlonely.mechkeys.core;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** Software mixer on one daemon thread -> one SourceDataLine. Never touches the game thread beyond a queue offer. */
public final class Mixer {
    private static final int CHUNK = 256; // frames per mix pass (~5.8 ms)
    private static final class Voice { final short[] d; double pos; final double step; final float gain;
        Voice(short[] d, double step, float gain) { this.d = d; this.step = step; this.gain = gain; } }

    private final ConcurrentLinkedQueue<Voice> pending = new ConcurrentLinkedQueue<Voice>();
    private final AtomicInteger queued = new AtomicInteger();
    private final Object wake = new Object();
    private volatile boolean running = true, available;
    private volatile int maxVoices = 24;
    private volatile float master = 1f;
    private volatile long droppedCount;
    /** Number of play() requests received (diagnostics/tests). */
    public volatile long requested;
    private Thread thread; private String error;

    public synchronized void start() {
        if (thread != null) return;
        thread = new Thread(new Runnable() { public void run() { loop(); } }, "MechKeys-Audio");
        thread.setDaemon(true); thread.setPriority(Thread.NORM_PRIORITY + 1); thread.start();
    }
    public void setMaxVoices(int n) { maxVoices = Math.max(1, Math.min(128, n)); }
    public void setMaster(float v) { master = PackLoader.clamp(v, 0f, 2f); }
    public boolean isAvailable() { return available; }
    public String getError() { return error; }
    public long dropped() { return droppedCount; }

    /** Thread-safe, non-blocking. pitch 1.0 = original. */
    public void play(Pcm p, float volume, float pitch) {
        if (p == null) return;
        requested++;
        if (!available) return;
        if (queued.get() > maxVoices * 2) { droppedCount++; return; } // flood guard
        queued.incrementAndGet();
        pending.add(new Voice(p.data, PackLoader.clamp(pitch, 0.25f, 4f), volume));
        synchronized (wake) { wake.notify(); }
    }
    public void stopAll() { Voice v; while ((v = pending.poll()) != null) queued.decrementAndGet(); clearFlag = true; }
    private volatile boolean clearFlag;

    public void shutdown() { running = false; synchronized (wake) { wake.notifyAll(); } }

    private void loop() {
        SourceDataLine line = null;
        try {
            AudioFormat fmt = new AudioFormat(Pcm.RATE, 16, 2, true, false);
            line = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, fmt));
            line.open(fmt, CHUNK * 4 * 4); // ~4 chunks of buffer => ~23 ms
            line.start(); available = true;
        } catch (Throwable t) { error = "No audio output: " + t; available = false; running = false; return; }

        List<Voice> active = new ArrayList<Voice>(); byte[] out = new byte[CHUNK * 4]; float[] acc = new float[CHUNK * 2];
        long idleSince = 0; boolean stopped = false;
        try {
            while (running) {
                Voice v;
                while ((v = pending.poll()) != null) { queued.decrementAndGet(); active.add(v); }
                if (clearFlag) { clearFlag = false; active.clear(); }
                while (active.size() > maxVoices) active.remove(0); // steal oldest
                if (active.isEmpty()) {
                    long now = System.nanoTime();
                    if (idleSince == 0) idleSince = now;
                    if (!stopped && now - idleSince > 1_000_000_000L) { line.drain(); line.stop(); stopped = true; }
                    if (stopped) { synchronized (wake) { if (pending.isEmpty() && running) wake.wait(500); } continue; }
                } else {
                    idleSince = 0;
                    if (stopped) { line.start(); stopped = false; }
                }
                Arrays.fill(acc, 0f);
                for (Iterator<Voice> it = active.iterator(); it.hasNext(); ) {
                    Voice x = it.next(); int frames = x.d.length / 2; boolean done = false;
                    float g = x.gain * master;
                    for (int i = 0; i < CHUNK; i++) {
                        int i0 = (int) x.pos;
                        if (i0 >= frames - 1) { done = true; break; }
                        float f = (float) (x.pos - i0);
                        float l = x.d[i0 * 2] + (x.d[i0 * 2 + 2] - x.d[i0 * 2]) * f;
                        float r = x.d[i0 * 2 + 1] + (x.d[i0 * 2 + 3] - x.d[i0 * 2 + 1]) * f;
                        acc[i * 2] += l * g; acc[i * 2 + 1] += r * g; x.pos += x.step;
                    }
                    if (done) it.remove();
                }
                for (int i = 0; i < acc.length; i++) {
                    float s = acc[i]; // soft clip
                    if (s > 24000f || s < -24000f) { float a = Math.abs(s) - 24000f; s = Math.signum(s) * (24000f + 8767f * (a / (a + 8767f))); }
                    int q = Math.round(s); if (q > 32767) q = 32767; if (q < -32768) q = -32768;
                    out[i * 2] = (byte) q; out[i * 2 + 1] = (byte) (q >> 8);
                }
                line.write(out, 0, out.length); // blocks at the audio rate: natural pacing
            }
        } catch (Throwable t) { error = "Audio thread stopped: " + t; available = false;
        } finally { try { line.stop(); line.close(); } catch (Throwable ignored) {} available = false; active.clear(); pending.clear(); }
    }
}
