package io.github.notthatlonely.mechkeys.fabric;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;

/** Text prompt (when text != null) or yes/no confirmation (when text == null). A prompt can offer a "Browse..." file picker. */
public class PromptScreen extends MKScreen {
    public interface Callback { void done(String text); }
    private final String title, hint, initial; private final boolean confirm, browse; private final Callback cb; private EditBox field;
    private volatile String picked; private volatile boolean browsing;

    public PromptScreen(Screen parent, String title, String hint, String initial, boolean browse, Callback cb) { super(parent, title); this.title = title; this.hint = hint; this.initial = initial; this.cb = cb; this.confirm = false; this.browse = browse; }
    public PromptScreen(Screen parent, String title, String message, Callback cb) { super(parent, title); this.title = title; this.hint = message; this.initial = null; this.cb = cb; this.confirm = true; this.browse = false; }

    @Override protected void init() {
        int cx = width / 2;
        String keep = field == null ? initial : field.getValue();
        if (!confirm) {
            field = new EditBox(font, cx - 150, height / 2 - 4, 300, 18, Component.empty()); field.setMaxLength(1000);
            field.setValue(keep == null ? "" : keep); addRenderableWidget(field); setFocused(field);
        }
        if (browse) { btn(cx - 152, height / 2 + 24, 98, 20, "Import", b -> submit()); btn(cx - 49, height / 2 + 24, 98, 20, "Browse...", b -> openPicker()); btn(cx + 54, height / 2 + 24, 98, 20, "Cancel", b -> close()); }
        else { btn(cx - 102, height / 2 + 24, 100, 20, confirm ? "Yes" : "OK", b -> submit()); btn(cx + 2, height / 2 + 24, 100, 20, confirm ? "No" : "Cancel", b -> close()); }
    }
    @Override public void tick() {
        if (field != null) { String p = picked; if (p != null) { picked = null; field.setValue(p); setFocused(field); } }
    }
    /** Native "open file" dialog on its own thread so the game keeps rendering. The chosen path is put into the text box. */
    private void openPicker() {
        if (browsing) return; browsing = true;
        Thread t = new Thread(() -> {
            try {
                FileDialog fd = new FileDialog((Frame) null, "Choose a sound pack (.zip)", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter() { public boolean accept(File d, String n) { return n.toLowerCase().endsWith(".zip"); } });
                fd.setVisible(true);
                if (fd.getFile() != null) picked = new File(fd.getDirectory(), fd.getFile()).getPath();
                fd.dispose();
            } catch (Throwable e) { MechKeysMod.LOG.warn("File picker unavailable (paste the path instead): " + e); }
            finally { browsing = false; }
        }, "MechKeys-FilePicker"); t.setDaemon(true); t.start();
    }
    private void submit() { String t = confirm ? "yes" : field.getValue().trim(); if (!confirm && t.length() == 0) return; Compat.setScreen(minecraft, parent); cb.done(t); }
    @Override public boolean keyPressed(KeyEvent e) {
        if (e.key() == InputConstants.KEY_RETURN || e.key() == InputConstants.KEY_NUMPADENTER) { submit(); return true; }
        return super.keyPressed(e);   // Escape -> onClose() -> close()
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xD0000000);
        int cx = width / 2; g.fill(cx - 165, height / 2 - 52, cx + 165, height / 2 + 52, 0xFF000000); g.fill(cx - 164, height / 2 - 51, cx + 164, height / 2 + 51, PANEL);
        g.fill(cx - 164, height / 2 - 51, cx + 164, height / 2 - 49, ACCENT);
        center(g, title, cx, height / 2 - 44, WHITE);
        center(g, font.plainSubstrByWidth(hint == null ? "" : hint, 320), cx, height / 2 - 28, DIM);
        if (browsing) center(g, "Choose a file in the dialog window...", cx, height / 2 + 48, DIM);
        super.extractRenderState(g, mx, my, pt);
    }
}
