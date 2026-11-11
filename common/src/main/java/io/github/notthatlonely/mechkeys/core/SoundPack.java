package io.github.notthatlonely.mechkeys.core;

import java.io.File;
import java.util.*;

/** Metadata + key definitions of one profile (a folder containing config.json and audio). */
public final class SoundPack {
    public static final class Segment {
        public final String file; // relative to pack dir; null = main audio file
        public final double startMs, endMs; // endMs < 0 => whole file
        public Segment(String file, double s, double e) { this.file = file; this.startMs = s; this.endMs = e; }
    }

    public String id, name = "Unnamed", author = "Unknown", description = "", version = "1.0.0";
    public String mainAudio; public boolean multi; public boolean randomPitch; public float recommendedVolume = 1f;
    /** True when every defined key is a mouse button: the pack goes to the mouse slot, everything else to the keyboard slot. */
    public boolean mousePack; public String icon;
    public File dir;
    public final Map<String, List<Segment>> defs = new LinkedHashMap<String, List<Segment>>();
    public final List<String> warnings = new ArrayList<String>();

    public int keyCount() { return defs.size(); }
    @Override public String toString() { return name + " (" + id + ")"; }
}
