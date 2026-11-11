package io.github.notthatlonely.mechkeys.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Small shims for the few client calls that differ between Minecraft 26.1 / 26.2 / 26.3, resolved by name at runtime
 * (the game is unobfuscated, so names are stable) so the same source builds for all three versions.
 *
 * <p><b>PORTING NOTES (next update)</b>: this file is where version-to-version Minecraft API drift is absorbed.
 * When a new version (26.4, 27.x ...) fails to compile or crashes on opening the screen / registering the key:
 * <ol>
 *  <li>Find what moved (Minecraft sources via {@code gradle genSources}, or the Fabric / Yarn-free "unobfuscated" class names).</li>
 *  <li>Add the new variant as one more reflective fallback here, keeping the old ones - the same jar source must keep building 26.1 / 26.2 / 26.3.</li>
 *  <li>Only if a change cannot be done reflectively (signature of a mixin target), put it in the mixins instead - see the notes there.</li>
 * </ol>
 * Known history: setScreen -> setScreenAndShow (26.2) -> Minecraft.gui (26.3); InputConstants.Type.KEYSYM -> KEYBOARD (26.3).
 */
final class Compat {
    private Compat() {}
    private static Object target; private static Method method;

    /** 26.1: Minecraft.setScreen; 26.2: Minecraft.setScreenAndShow; 26.3: lives on Minecraft.gui. */
    static synchronized void setScreen(Minecraft mc, Screen s) {
        try {
            if (method == null) resolve(mc);
            method.invoke(target, s);
        } catch (Throwable t) { MechKeysMod.LOG.error("Could not open screen: " + t, t); }
    }
    private static void resolve(Minecraft mc) throws Exception {
        Object gui = null;
        try { gui = mc.getClass().getField("gui").get(mc); } catch (Throwable ignored) {}
        Object[] targets = {mc, gui};
        String[] names = {"setScreenAndShow", "setScreen"};
        for (Object t : targets) {
            if (t == null) continue;
            for (String n : names) for (Method m : t.getClass().getMethods())
                if (m.getName().equals(n) && m.getParameterCount() == 1 && m.getParameterTypes()[0].isAssignableFrom(Screen.class)) { target = t; method = m; return; }
        }
        throw new NoSuchMethodException("no setScreen variant found on Minecraft or Minecraft.gui");
    }

    /** 26.1/26.2: (name, key, category). 26.3 renamed InputConstants.Type.KEYSYM -> KEYBOARD, so fall back to the typed constructor. */
    static KeyMapping newKeyMapping(String name, int key, KeyMapping.Category cat) {
        try {
            try { return KeyMapping.class.getConstructor(String.class, int.class, KeyMapping.Category.class).newInstance(name, key, cat); }
            catch (NoSuchMethodException e) {
                Object type = null;
                for (Object c : InputConstants.Type.class.getEnumConstants()) if (c.toString().equals("KEYBOARD") || c.toString().equals("KEYSYM")) type = c;
                Constructor<KeyMapping> k = KeyMapping.class.getConstructor(String.class, InputConstants.Type.class, int.class, KeyMapping.Category.class);
                return k.newInstance(name, type, key, cat);
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot create key mapping: " + e, e); }
    }
}
