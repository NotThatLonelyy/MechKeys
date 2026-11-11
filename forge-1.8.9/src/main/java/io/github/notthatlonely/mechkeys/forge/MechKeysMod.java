package io.github.notthatlonely.mechkeys.forge;

import io.github.notthatlonely.mechkeys.core.SoundEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * MechKeys - mechanical keyboard and mouse sounds for Minecraft (Forge 1.8.9). Unofficial; not affiliated with Mechvibes. By NotThatLonely.
 *
 * <p><b>PORTING NOTES (next update)</b> - full guide: docs/PORTING.md
 * <ul>
 *  <li>This class wires the shared engine ({@code ../common}, package {@code core}) into Forge 1.8.9: engine start/stop, K key binding,
 *      /mechkeys client command, first-run default packs. To support another legacy Forge version, copy this module to
 *      {@code forge-<mc>/} and adapt only this class, InputHandler and the screens - never the core.</li>
 *  <li>{@code VERSION} must equal {@code mod_version} in ../version.properties (bump both on release).</li>
 *  <li>{@code DEFAULTS_MARKER} below: change {@code ".defaults-v2"} to {@code ".defaults-v3"} whenever the bundled default packs change,
 *      so existing players get the new defaults installed once.</li>
 * </ul>
 */
@Mod(modid = MechKeysMod.MODID, name = "MechKeys", version = MechKeysMod.VERSION, clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class MechKeysMod {
    public static final String MODID = "mechkeys", VERSION = "1.0.0";
    public static final Logger LOG = LogManager.getLogger("MechKeys");
    public static SoundEngine engine;
    public static KeyBinding openGui;
    static volatile boolean pendingOpen;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
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
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        File root = new File(Minecraft.getMinecraft().mcDataDir, "config/mechkeys");
        try {
            engine = new SoundEngine(root);
            installDefaults(root);
            engine.start();
            if (!engine.mixer.isAvailable() && engine.mixer.getError() != null) LOG.warn(engine.mixer.getError());
        } catch (Throwable t) { LOG.error("MechKeys failed to start; sounds are disabled. Reason: " + t, t); engine = null; return; }
        openGui = new KeyBinding("Open MechKeys", Keyboard.KEY_K, "MechKeys");
        ClientRegistry.registerKeyBinding(openGui);
        InputHandler ih = new InputHandler();
        MinecraftForge.EVENT_BUS.register(ih);                              // GuiScreenEvent, MouseEvent
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(ih); // KeyInputEvent, ClientTickEvent
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            public String getCommandName() { return "mechkeys"; }
            public String getCommandUsage(ICommandSender s) { return "/mechkeys - opens the MechKeys screen"; }
            public void processCommand(ICommandSender s, String[] a) { pendingOpen = true; }
            public int getRequiredPermissionLevel() { return 0; }
            public boolean canCommandSenderUseCommand(ICommandSender s) { return true; }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { public void run() { if (engine != null) engine.shutdown(); } }, "MechKeys-Shutdown"));
        LOG.info("MechKeys " + VERSION + " ready (unofficial; not affiliated with Mechvibes). Folder: " + root);
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
