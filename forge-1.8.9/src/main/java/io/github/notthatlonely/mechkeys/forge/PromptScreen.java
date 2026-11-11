package io.github.notthatlonely.mechkeys.forge;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/** Text prompt (when text != null) or yes/no confirmation (when text == null). A prompt can offer a "Browse..." file picker. */
public class PromptScreen extends MKScreen {
    public interface Callback { void done(String text); }
    private final String title, hint, initial; private final boolean confirm, browse; private final Callback cb; private GuiTextField field;
    private volatile String picked; private volatile boolean browsing;

    public PromptScreen(GuiScreen parent, String title, String hint, String initial, boolean browse, Callback cb) { super(parent); this.title = title; this.hint = hint; this.initial = initial; this.cb = cb; this.confirm = false; this.browse = browse; }
    public PromptScreen(GuiScreen parent, String title, String message, Callback cb) { super(parent); this.title = title; this.hint = message; this.initial = null; this.cb = cb; this.confirm = true; this.browse = false; }

    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true); buttonList.clear(); int cx = width / 2;
        String keep = field == null ? initial : field.getText();
        if (!confirm) { field = new GuiTextField(0, fontRendererObj, cx - 150, height / 2 - 4, 300, 18); field.setMaxStringLength(1000); field.setText(keep == null ? "" : keep); field.setFocused(true); }
        if (browse) { btn(1, cx - 152, height / 2 + 24, 98, 20, "Import"); btn(3, cx - 49, height / 2 + 24, 98, 20, "Browse..."); btn(2, cx + 54, height / 2 + 24, 98, 20, "Cancel"); }
        else { btn(1, cx - 102, height / 2 + 24, 100, 20, confirm ? "Yes" : "OK"); btn(2, cx + 2, height / 2 + 24, 100, 20, confirm ? "No" : "Cancel"); }
    }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }
    @Override public void updateScreen() {
        if (field != null) { field.updateCursorCounter(); String p = picked; if (p != null) { picked = null; field.setText(p); field.setFocused(true); } }
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 1) submit(); else if (b.id == 2) close(); else if (b.id == 3) openPicker();
    }
    /** Native "open file" dialog on its own thread so the game keeps rendering. The chosen path is put into the text box. */
    private void openPicker() {
        if (browsing) return; browsing = true;
        Thread t = new Thread(new Runnable() { public void run() {
            try {
                FileDialog fd = new FileDialog((Frame) null, "Choose a sound pack (.zip)", FileDialog.LOAD);
                fd.setFilenameFilter(new FilenameFilter() { public boolean accept(File d, String n) { return n.toLowerCase().endsWith(".zip"); } });
                fd.setVisible(true);
                if (fd.getFile() != null) picked = new File(fd.getDirectory(), fd.getFile()).getPath();
                fd.dispose();
            } catch (Throwable e) { MechKeysMod.LOG.warn("File picker unavailable (paste the path instead): " + e); }
            finally { browsing = false; }
        } }, "MechKeys-FilePicker"); t.setDaemon(true); t.start();
    }
    private void submit() { String t = confirm ? "yes" : field.getText().trim(); if (!confirm && t.length() == 0) return; mc.displayGuiScreen(parent); cb.done(t); }
    @Override protected void keyTyped(char c, int code) throws IOException {
        if (code == Keyboard.KEY_ESCAPE) { close(); return; }
        if (code == Keyboard.KEY_RETURN || code == Keyboard.KEY_NUMPADENTER) { submit(); return; }
        if (field != null) field.textboxKeyTyped(c, code);
    }
    @Override protected void mouseClicked(int mx, int my, int b) throws IOException { super.mouseClicked(mx, my, b); if (field != null) field.mouseClicked(mx, my, b); }
    @Override public void drawScreen(int mx, int my, float pt) {
        drawRect(0, 0, width, height, 0xD0000000);
        int cx = width / 2; drawRect(cx - 165, height / 2 - 52, cx + 165, height / 2 + 52, 0xFF000000); drawRect(cx - 164, height / 2 - 51, cx + 164, height / 2 + 51, PANEL);
        drawRect(cx - 164, height / 2 - 51, cx + 164, height / 2 - 49, ACCENT);
        drawCenteredString(fontRendererObj, title, cx, height / 2 - 44, WHITE);
        String h = fontRendererObj.trimStringToWidth(hint == null ? "" : hint, 320); drawCenteredString(fontRendererObj, h, cx, height / 2 - 28, DIM);
        if (field != null) field.drawTextBox();
        if (browsing) drawCenteredString(fontRendererObj, "Choose a file in the dialog window...", cx, height / 2 + 48, DIM);
        super.drawScreen(mx, my, pt);
    }
}
