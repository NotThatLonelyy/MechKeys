package io.github.notthatlonely.mechkeys.fabric;

import io.github.notthatlonely.mechkeys.core.SoundEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mojang.blaze3d.platform.InputConstants;

import java.io.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * MechKeys - mechanical keyboard and mouse sounds for Minecraft (Fabric 26.x). Unofficial; not affiliated with Mechvibes. By NotThatLonely.
 *
 * <p><b>PORTING NOTES (next update)</b> - full guide: docs/PORTING.md
 * <ul>
 *  <li>This class is the only place that wires the shared engine ({@code ../common}, package {@code core}) into Fabric:
 *      engine start/stop, K keybind, /mechkeys command, first-run default packs.</li>
 *  <li>New Minecraft version: usually nothing here changes. If it does not compile, the break is almost always
 *      {@code KeyMapping.Category}, {@code Identifier}, or the Fabric API event/command classes imported above - fix that import/call.</li>
 *  <li>{@code VERSION} must equal {@code mod_version} in ../version.properties (bump both on release).</li>
 *  <li>{@code DEFAULTS_MARKER} below: change {@code ".defaults-v2"} to {@code ".defaults-v3"} whenever the bundled default packs change,
 *      so existing players get the new defaults installed once.</li>
 * </ul>
 */
public class MechKeysMod implements ClientModInitializer {
    public static final String MODID = "mechkeys", VERSION = "1.0.0";
    public static final Logger LOG = LogManager.getLogger("MechKeys");
    public static SoundEngine engine;
    public static KeyMapping openGui;
    static volatile boolean pendingOpen;

    @Override
    public void onInitializeClient() {
        // Bridge the engine's java.util.logging into the game log (log4j).
        java.util.logging.Logger jul = SoundEngine.LOG; jul.setUseParentHandlers(false);
        jul.addHandler(new Handler() {
            public void publish(LogRecord r) {
                Level l = r.getLevel(); String m = r.getMessage();
                if (l.intValue() >= Level.SEVERE.intValue()) LOG.error(m, r.getThrown());
                else if (l.intValue() >= Level.WARNING.intValue()) LOG.warn(m); else LOG.info(m);
            }
            public void flush() {} public void close() {}
        });

        File root = FabricLoader.getInstance().getGameDir().resolve("config").resolve("mechkeys").toFile();
        try {
            engine = new SoundEngine(root);
            installDefaults(root);
            engine.start();
            if (!engine.mixer.isAvailable() && engine.mixer.getError() != null) LOG.warn(engine.mixer.getError());
        } catch (Throwable t) { LOG.error("MechKeys failed to start; sounds are disabled. Reason: " + t, t); engine = null; return; }

        openGui = KeyMappingHelper.registerKeyMapping(Compat.newKeyMapping("key.mechkeys.open", InputConstants.KEY_K,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MODID, "main"))));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (pendingOpen) { pendingOpen = false; Compat.setScreen(client, new MainScreen(null)); }
            while (openGui.consumeClick()) Compat.setScreen(client, new MainScreen(null));   // only fires with no screen open
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommands.literal("mechkeys").executes(ctx -> { pendingOpen = true; return 1; })));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { if (engine != null) engine.shutdown(); }, "MechKeys-Shutdown"));
        LOG.info("MechKeys " + VERSION + " ready (unofficial; not affiliated with Mechvibes). Folder: " + root);
    }

    /** Called from the input mixins (game window focused only; never cancels or changes input). */
    public static void onKey(int keyCode, int action) {
        SoundEngine en = engine; if (en == null) return;
        String name = Keys.key(keyCode); if (name == null) return;
        en.onKey(name, action != InputConstants.RELEASE, action == InputConstants.REPEAT);
    }
    public static void onMouse(int button, int action) {
        SoundEngine en = engine; if (en == null) return;
        String name = Keys.mouse(button); if (name == null) return;
        en.onMouse(name, action != InputConstants.RELEASE);
    }

    private static final String DEFAULTS_MARKER = ".defaults-v2";
    /** Old built-in example profiles that earlier versions installed; removed once when upgrading. */
    private static final String[] LEGACY = {"synth-clicky", "soft-thock"};

    /**
     * First run of this version: remove the old example profiles and install the two bundled default packs
     * (one keyboard, one mouse), then make them the active sounds. Deleting them later is respected.
     */
    private static void installDefaults(File root) {
        File marker = new File(root, DEFAULTS_MARKER); if (marker.exists()) return;
        for (String id : LEGACY) { try { engine.profiles.delete(id); } catch (Exception ignored) {} }
        for (File old : new File[]{new File(root, ".examples-installed")}) old.delete();
        InputStream idx = MechKeysMod.class.getResourceAsStream("/assets/mechkeys/defaults/index.txt");
        if (idx == null) { LOG.warn("Bundled default profiles are missing from the jar"); return; }
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(idx, "UTF-8")); String line;
            while ((line = br.readLine()) != null) {
                line = line.trim(); if (line.length() == 0 || line.contains("..")) continue;
                File out = new File(root, "profiles/" + line); out.getParentFile().mkdirs();
                InputStream in = MechKeysMod.class.getResourceAsStream("/assets/mechkeys/defaults/" + line);
                if (in == null) continue; OutputStream o = new FileOutputStream(out);
                try { byte[] b = new byte[16384]; int r; while ((r = in.read(b)) > 0) o.write(b, 0, r); } finally { in.close(); o.close(); }
            }
            br.close();
            engine.settings.keyboardProfile = "keychron-q3-bluex"; engine.settings.mouseProfile = "superstrike-mouse";
            engine.saveSettings();
            try { marker.createNewFile(); } catch (IOException ignored) {}
        } catch (IOException ex) { LOG.warn("Could not install default profiles: " + ex); }
    }
}
