package io.github.notthatlonely.mechkeys.fabric;

import io.github.notthatlonely.mechkeys.core.PackException;
import io.github.notthatlonely.mechkeys.core.SoundPack;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.awt.Desktop;
import java.io.File;
import java.util.*;

/** Installed profiles: select (goes to keyboard or mouse automatically), import (zip / folder / link), delete. */
public class ProfilesScreen extends MKScreen {
    private List<SoundPack> all = new ArrayList<SoundPack>(), shown = new ArrayList<SoundPack>(); private List<String> errors = new ArrayList<String>();
    private EditBox search; private int scroll, selected = -1, listX, listY, listW, listH; private static final int ROW = 24;
    private volatile String message = ""; private volatile int messageColor = DIM; private volatile boolean refresh, busy;

    public ProfilesScreen(Screen parent) { super(parent, "Profiles"); }

    static void openFolder(File f) {
        try { f.mkdirs(); if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(f); else MechKeysMod.LOG.info("Folder: " + f); }
        catch (Throwable t) { MechKeysMod.LOG.warn("Could not open folder " + f + ": " + t); }
    }

    @Override protected void init() {
        if (eng() == null) return;
        int px = panelX(), pw = panelW(); int colW = 100;
        listX = px + 8; listY = 54; listW = pw - colW - 24; listH = height - listY - 42;
        String keepText = search == null ? "" : search.getValue();
        search = new EditBox(font, listX, 32, listW, 16, Component.empty()); search.setMaxLength(40); search.setValue(keepText);
        search.setResponder(s -> applyFilter()); addRenderableWidget(search);
        int bx = px + pw - colW - 8, y = 32;
        btn(bx, y, colW, 20, "Select", b -> onSelect()); btn(bx, y + 24, colW, 20, "Import...", b -> onImport());
        btn(bx, y + 48, colW, 20, "Delete", b -> onDelete()); btn(bx, y + 72, colW, 20, "Open folder", b -> openFolder(new File(eng().root, "profiles")));
        btn(bx, height - 30, colW, 20, "Back", b -> close()); reload();
    }
    private void reload() {
        String keep = selected >= 0 && selected < shown.size() ? shown.get(selected).id : null;
        errors = new ArrayList<String>(); all = eng().profiles.list(errors); applyFilter();
        selected = -1; if (keep != null) for (int i = 0; i < shown.size(); i++) if (shown.get(i).id.equals(keep)) selected = i;
    }
    private void applyFilter() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT); shown = new ArrayList<SoundPack>();
        for (SoundPack p : all) if (q.length() == 0 || p.name.toLowerCase(Locale.ROOT).contains(q) || p.author.toLowerCase(Locale.ROOT).contains(q) || p.id.toLowerCase(Locale.ROOT).contains(q)) shown.add(p);
        scroll = 0; selected = -1;
    }
    private SoundPack sel() { return selected >= 0 && selected < shown.size() ? shown.get(selected) : null; }
    private void msg(String m, boolean ok) { message = m; messageColor = ok ? OK : BAD; }

    private interface Job { String run() throws PackException; }
    private void async(final String what, final Job j) {
        if (busy) { msg("Please wait - another operation is running", false); return; }
        busy = true; msg(what + "...", true);
        Thread t = new Thread(() -> {
            try { String r = j.run(); msg(r, true); } catch (PackException e) { msg(e.getMessage(), false); MechKeysMod.LOG.warn(what + " failed: " + e.getMessage()); }
            catch (Throwable e) { msg(what + " failed: " + e, false); MechKeysMod.LOG.error(what + " failed", e); }
            finally { busy = false; refresh = true; }
        }, "MechKeys-Profile-Job"); t.setDaemon(true); t.start();
    }

    private void onImport() {
        Compat.setScreen(minecraft, new PromptScreen(this, "Import profile", "Paste a path to a .zip or folder, or a link to a .zip - or use Browse", "", true,
            t -> async("Importing", () -> {
                String id = eng().profiles.importAny(t, 128L * 1024 * 1024);
                SoundPack ip = eng().profiles.get(id);
                if (ip != null) { eng().select(id, true); return "Imported '" + ip.name + "' as your " + (ip.mousePack ? "mouse" : "keyboard") + " sound"; }
                return "Imported as '" + id + "'"; })));
    }
    private void onSelect() {
        SoundPack p = sel(); if (p == null) { msg("Select a profile in the list first", false); return; }
        eng().select(p.id, true); msg(p.name + " is now your " + (p.mousePack ? "mouse" : "keyboard") + " sound", true);
    }
    private void onDelete() {
        final SoundPack p = sel(); if (p == null) { msg("Select a profile in the list first", false); return; }
        final String id = p.id;
        Compat.setScreen(minecraft, new PromptScreen(this, "Delete '" + p.name + "'?", "This permanently deletes the profile folder.",
            t -> async("Deleting", () -> { eng().profiles.delete(id); eng().ensureSlots(); return "Deleted " + id; })));
    }

    @Override public void tick() { if (refresh) { refresh = false; reload(); } }
    @Override public void removed() { if (eng() != null) eng().saveSettings(); }
    @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
        boolean handled = super.mouseClicked(e, dbl);
        double mx = e.x(), my = e.y();
        if (e.button() == InputConstants.MOUSE_BUTTON_LEFT && mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            int i = scroll + (int) (my - listY) / ROW; if (i >= 0 && i < shown.size()) selected = i;
            return true;
        }
        return handled;
    }
    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy != 0) { int max = Math.max(0, shown.size() - listH / ROW); scroll = Math.max(0, Math.min(max, scroll + (sy > 0 ? -1 : 1))); return true; }
        return super.mouseScrolled(mx, my, sx, sy);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        drawPanel(g, "Profiles", shown.size() + " installed"); if (eng() == null) return;
        if (search.getValue().length() == 0 && !search.isFocused()) str(g, "Search...", listX + 4, 36, DIM);
        g.fill(listX, listY, listX + listW, listY + listH, 0xFF101018);
        int rows = listH / ROW; SoundPack ak = eng().keyboard.pack(), am = eng().mouse.pack();
        for (int r = 0; r < rows && scroll + r < shown.size(); r++) {
            SoundPack p = shown.get(scroll + r); int y = listY + r * ROW; boolean isSel = scroll + r == selected;
            g.fill(listX + 1, y + 1, listX + listW - 1, y + ROW - 1, isSel ? 0xFF3A3A55 : PANEL2);
            if (isSel) g.fill(listX + 1, y + 1, listX + 3, y + ROW - 1, ACCENT);
            boolean isAct = (ak != null && ak.id.equals(p.id)) || (am != null && am.id.equals(p.id));
            MutableComponent tag = Component.empty();
            if (isAct) tag.append(Component.literal("ACTIVE ").withStyle(ChatFormatting.GREEN));
            tag.append(Component.literal(p.mousePack ? "MOUSE" : "KEYBOARD").withStyle(p.mousePack ? ChatFormatting.AQUA : ChatFormatting.YELLOW));
            int tw = font.width(tag);
            str(g, font.plainSubstrByWidth(p.name, listW - tw - 14), listX + 7, y + 3, WHITE);
            g.text(font, tag, listX + listW - tw - 4, y + 3, WHITE);
            small(g, "by " + p.author + " \u00b7 " + p.keyCount() + " keys" + (p.warnings.isEmpty() ? "" : " \u00b7 " + p.warnings.size() + " warning(s)"), listX + 7, y + 14, DIM);
        }
        if (shown.isEmpty()) center(g, all.isEmpty() ? "No profiles - use Import" : "No match", listX + listW / 2, listY + 12, DIM);
        String m = message; int mc2 = messageColor; if (m.length() == 0 && !errors.isEmpty()) { m = "Skipped invalid: " + errors.get(0); mc2 = BAD; }
        if (m.length() > 0) { List<FormattedCharSequence> lines = font.split(Component.literal(m), listW); for (int i = 0; i < Math.min(2, lines.size()); i++) g.text(font, lines.get(i), listX, height - 38 + i * 10, mc2); }
        super.extractRenderState(g, mx, my, pt);
    }
}
