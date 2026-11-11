package io.github.notthatlonely.mechkeys.forge;

import io.github.notthatlonely.mechkeys.core.PackException;
import io.github.notthatlonely.mechkeys.core.SoundPack;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Installed profiles: select (goes to keyboard or mouse automatically), import (zip / folder / link), delete. */
public class ProfilesScreen extends MKScreen {
    private List<SoundPack> all = new ArrayList<SoundPack>(), shown = new ArrayList<SoundPack>(); private List<String> errors = new ArrayList<String>();
    private GuiTextField search; private int scroll, selected = -1, listX, listY, listW, listH; private static final int ROW = 24;
    private volatile String message = ""; private volatile int messageColor = DIM; private volatile boolean refresh, busy;

    public ProfilesScreen(GuiScreen parent) { super(parent); }

    static void openFolder(File f) {
        try { f.mkdirs(); if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f); else MechKeysMod.LOG.info("Folder: " + f); }
        catch (Throwable t) { MechKeysMod.LOG.warn("Could not open folder " + f + ": " + t); }
    }

    @Override public void initGui() {
        Keyboard.enableRepeatEvents(true); buttonList.clear(); if (eng() == null) return;
        int px = panelX(), pw = panelW(); int colW = 100;
        listX = px + 8; listY = 54; listW = pw - colW - 24; listH = height - listY - 42;
        search = new GuiTextField(0, fontRendererObj, listX, 32, listW, 16); search.setMaxStringLength(40);
        int bx = px + pw - colW - 8, y = 32;
        btn(10, bx, y, colW, 20, "Select"); btn(13, bx, y + 24, colW, 20, "Import..."); btn(19, bx, y + 48, colW, 20, "Delete"); btn(20, bx, y + 72, colW, 20, "Open folder");
        btn(30, bx, height - 30, colW, 20, "Back"); reload();
    }
    private void reload() {
        String keep = selected >= 0 && selected < shown.size() ? shown.get(selected).id : null;
        errors = new ArrayList<String>(); all = eng().profiles.list(errors); applyFilter();
        selected = -1; if (keep != null) for (int i = 0; i < shown.size(); i++) if (shown.get(i).id.equals(keep)) selected = i;
    }
    private void applyFilter() {
        String q = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT); shown = new ArrayList<SoundPack>();
        for (SoundPack p : all) if (q.length() == 0 || p.name.toLowerCase(Locale.ROOT).contains(q) || p.author.toLowerCase(Locale.ROOT).contains(q) || p.id.toLowerCase(Locale.ROOT).contains(q)) shown.add(p);
        scroll = 0; selected = -1;
    }
    private SoundPack sel() { return selected >= 0 && selected < shown.size() ? shown.get(selected) : null; }
    private void msg(String m, boolean ok) { message = m; messageColor = ok ? OK : BAD; }

    private interface Job { String run() throws PackException; }
    private void async(final String what, final Job j) {
        if (busy) { msg("Please wait - another operation is running", false); return; }
        busy = true; msg(what + "...", true);
        Thread t = new Thread(new Runnable() { public void run() {
            try { String r = j.run(); msg(r, true); } catch (PackException e) { msg(e.getMessage(), false); MechKeysMod.LOG.warn(what + " failed: " + e.getMessage()); }
            catch (Throwable e) { msg(what + " failed: " + e, false); MechKeysMod.LOG.error(what + " failed", e); }
            finally { busy = false; refresh = true; }
        } }, "MechKeys-Profile-Job"); t.setDaemon(true); t.start();
    }

    @Override protected void actionPerformed(GuiButton b) throws IOException {
        final SoundPack p = sel();
        if (b.id == 30) { close(); return; }
        if (b.id == 20) { openFolder(new File(eng().root, "profiles")); return; }
        if (b.id == 13) {
            mc.displayGuiScreen(new PromptScreen(this, "Import profile", "Paste a path to a .zip or folder, or a link to a .zip - or use Browse", "", true, new PromptScreen.Callback() {
                public void done(final String t) { async("Importing", new Job() { public String run() throws PackException {
                    String id = eng().profiles.importAny(t, 128L * 1024 * 1024);
                    SoundPack ip = eng().profiles.get(id);
                    if (ip != null) { eng().select(id, true); return "Imported '" + ip.name + "' as your " + (ip.mousePack ? "mouse" : "keyboard") + " sound"; }
                    return "Imported as '" + id + "'"; } }); } }));
            return;
        }
        if (p == null) { msg("Select a profile in the list first", false); return; }
        final String id = p.id;
        switch (b.id) {
            case 10: eng().select(id, true); msg(p.name + " is now your " + (p.mousePack ? "mouse" : "keyboard") + " sound", true); break;
            case 19: mc.displayGuiScreen(new PromptScreen(this, "Delete '" + p.name + "'?", "This permanently deletes the profile folder.", new PromptScreen.Callback() {
                public void done(String t) { async("Deleting", new Job() { public String run() throws PackException {
                    eng().profiles.delete(id); eng().ensureSlots(); return "Deleted " + id; } }); } })); break;
        }
    }

    @Override public void updateScreen() { if (search != null) search.updateCursorCounter(); if (refresh) { refresh = false; reload(); } }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); if (eng() != null) eng().saveSettings(); }
    @Override protected void keyTyped(char c, int code) throws IOException {
        if (search != null && search.isFocused()) { String before = search.getText(); search.textboxKeyTyped(c, code); if (code == Keyboard.KEY_ESCAPE) { close(); return; } if (!before.equals(search.getText())) applyFilter(); return; }
        super.keyTyped(c, code);
    }
    @Override protected void mouseClicked(int mx, int my, int btn) throws IOException {
        super.mouseClicked(mx, my, btn); if (search != null) search.mouseClicked(mx, my, btn);
        if (btn == 0 && mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            int i = scroll + (my - listY) / ROW; if (i >= 0 && i < shown.size()) selected = i;
        }
    }
    @Override public void handleMouseInput() throws IOException {
        super.handleMouseInput(); int d = Mouse.getEventDWheel();
        if (d != 0) { int max = Math.max(0, shown.size() - listH / ROW); scroll = Math.max(0, Math.min(max, scroll + (d > 0 ? -1 : 1))); }
    }
    @Override public void drawScreen(int mx, int my, float pt) {
        drawPanel("Profiles", shown.size() + " installed"); if (eng() == null) return;
        search.drawTextBox(); if (search.getText().length() == 0 && !search.isFocused()) drawString(fontRendererObj, "Search...", listX + 4, 36, DIM);
        drawRect(listX, listY, listX + listW, listY + listH, 0xFF101018);
        int rows = listH / ROW; SoundPack ak = eng().keyboard.pack(), am = eng().mouse.pack();
        for (int r = 0; r < rows && scroll + r < shown.size(); r++) {
            SoundPack p = shown.get(scroll + r); int y = listY + r * ROW; boolean isSel = scroll + r == selected;
            drawRect(listX + 1, y + 1, listX + listW - 1, y + ROW - 1, isSel ? 0xFF3A3A55 : PANEL2);
            if (isSel) drawRect(listX + 1, y + 1, listX + 3, y + ROW - 1, ACCENT);
            boolean isAct = (ak != null && ak.id.equals(p.id)) || (am != null && am.id.equals(p.id));
            String tag = (isAct ? "§aACTIVE " : "") + (p.mousePack ? "§bMOUSE" : "§eKEYBOARD");
            drawString(fontRendererObj, fontRendererObj.trimStringToWidth(p.name, listW - fontRendererObj.getStringWidth(tag) - 14), listX + 7, y + 3, WHITE);
            drawString(fontRendererObj, tag, listX + listW - fontRendererObj.getStringWidth(tag) - 4, y + 3, WHITE);
            small("by " + p.author + " · " + p.keyCount() + " keys" + (p.warnings.isEmpty() ? "" : " · " + p.warnings.size() + " warning(s)"), listX + 7, y + 14, DIM);
        }
        if (shown.isEmpty()) drawCenteredString(fontRendererObj, all.isEmpty() ? "No profiles - use Import" : "No match", listX + listW / 2, listY + 12, DIM);
        String m = message; int mc2 = messageColor; if (m.length() == 0 && !errors.isEmpty()) { m = "Skipped invalid: " + errors.get(0); mc2 = BAD; }
        if (m.length() > 0) { List<String> lines = fontRendererObj.listFormattedStringToWidth(m, listW); for (int i = 0; i < Math.min(2, lines.size()); i++) drawString(fontRendererObj, lines.get(i), listX, height - 38 + i * 10, mc2); }
        super.drawScreen(mx, my, pt);
    }
}
