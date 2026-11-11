package io.github.notthatlonely.mechkeys.forge;

import io.github.notthatlonely.mechkeys.core.KeyCodes;
import io.github.notthatlonely.mechkeys.core.SoundEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

/**
 * Listens to Minecraft's own input events (client side only). It never reads input outside the game window,
 * never consumes or cancels events, and sends nothing over the network.
 */
// PORTING NOTES (next update): LWJGL 2 polling of keyboard/mouse events (Forge 1.8.9). Key codes are translated by the shared
// core.KeyCodes (LWJGL 2 -> Mechvibes names). A newer Forge version that uses GLFW needs its own handler, not a change to KeyCodes.
public class InputHandler {
    private SoundEngine eng() { return MechKeysMod.engine; }

    /** In-world keys (no screen open). */
    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent e) {
        SoundEngine en = eng(); if (en == null || !Display.isActive()) return;
        handleKey(en);
        if (MechKeysMod.openGui != null && MechKeysMod.openGui.isPressed()) Minecraft.getMinecraft().displayGuiScreen(new MainScreen(null));
    }

    /** Keys while a GUI (inventory, chat, menus) is open. */
    @SubscribeEvent
    public void onGuiKey(GuiScreenEvent.KeyboardInputEvent.Pre e) {
        SoundEngine en = eng(); if (en == null || !Display.isActive()) return;
        handleKey(en);
    }

    private void handleKey(SoundEngine en) {
        int code = Keyboard.getEventKey(); if (code == 0) return;
        en.onKey(KeyCodes.fromLwjgl(code), Keyboard.getEventKeyState(), Keyboard.isRepeatEvent());
    }

    /** In-world mouse buttons. */
    @SubscribeEvent
    public void onMouse(MouseEvent e) {
        SoundEngine en = eng(); if (en == null || !Display.isActive()) return;
        if (e.button >= 0) en.onMouse(KeyCodes.mouse(e.button), e.buttonstate);
    }

    @SubscribeEvent
    public void onGuiMouse(GuiScreenEvent.MouseInputEvent.Pre e) {
        SoundEngine en = eng(); if (en == null || !Display.isActive()) return;
        int b = Mouse.getEventButton(); if (b >= 0) en.onMouse(KeyCodes.mouse(b), Mouse.getEventButtonState());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (MechKeysMod.pendingOpen) { MechKeysMod.pendingOpen = false; Minecraft.getMinecraft().displayGuiScreen(new MainScreen(null)); }
    }
}
