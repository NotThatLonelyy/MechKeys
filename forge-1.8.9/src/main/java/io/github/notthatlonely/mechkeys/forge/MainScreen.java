package io.github.notthatlonely.mechkeys.forge;

import io.github.notthatlonely.mechkeys.core.SoundEngine;
import io.github.notthatlonely.mechkeys.core.SoundPack;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.List;

/** The whole settings UI: keyboard and mouse are independent - profile, on/off and volume each. */
public class MainScreen extends MKScreen {
    private static final int H = 168;
    private GuiButton kbToggle, mouseToggle;
    public MainScreen(GuiScreen parent) { super(parent); }

    private int top() { return Math.max(6, (height - H) / 2); }

    @Override public void initGui() {
        buttonList.clear(); if (eng() == null) return;
        int px = panelX(), pw = panelW(), half = (pw - 24) / 2, t = top();
        // Keyboard
        btn(1, px + 8, t + 34, 20, 20, "<"); btn(2, px + pw - 28, t + 34, 20, 20, ">");
        kbToggle = btn(3, px + 8, t + 58, half, 20, "");
        buttonList.add(new Slider(4, px + 16 + half, t + 58, half, 20, 0f, 2f, eng().settings.keyboardVolume, fmt("Volume"),
            new Slider.Change() { public void changed(float v) { eng().settings.keyboardVolume = v; } }));
        // Mouse
        btn(5, px + 8, t + 92, 20, 20, "<"); btn(6, px + pw - 28, t + 92, 20, 20, ">");
        mouseToggle = btn(7, px + 8, t + 116, half, 20, "");
        buttonList.add(new Slider(8, px + 16 + half, t + 116, half, 20, 0f, 2f, eng().settings.mouseVolume, fmt("Volume"),
            new Slider.Change() { public void changed(float v) { eng().settings.mouseVolume = v; } }));
        btn(9, px + 8, t + H - 26, half, 20, "Profiles / Import"); btn(10, px + 16 + half, t + H - 26, half, 20, "Done");
        refresh();
    }
    private static Slider.Fmt fmt(final String label) { return new Slider.Fmt() { public String text(float v) { return label + ": " + Math.round(v * 100) + "%"; } }; }
    private static String onoff(boolean b) { return b ? "§aON" : "§cOFF"; }
    private void refresh() {
        kbToggle.displayString = "Keyboard: " + onoff(eng().settings.keyboardEnabled);
        mouseToggle.displayString = "Mouse: " + onoff(eng().settings.mouseEnabled);
    }

    private void cycle(SoundEngine.Slot slot, int d) {
        List<SoundPack> l = eng().profiles.list(slot.mouse); if (l.isEmpty()) return;
        int cur = 0; SoundPack a = slot.pack(); for (int i = 0; i < l.size(); i++) if (a != null && l.get(i).id.equals(a.id)) cur = i;
        eng().select(l.get(((cur + d) % l.size() + l.size()) % l.size()).id, true);
    }
    @Override protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case 1: cycle(eng().keyboard, -1); break; case 2: cycle(eng().keyboard, 1); break;
            case 3: eng().settings.keyboardEnabled = !eng().settings.keyboardEnabled; refresh(); break;
            case 5: cycle(eng().mouse, -1); break; case 6: cycle(eng().mouse, 1); break;
            case 7: eng().settings.mouseEnabled = !eng().settings.mouseEnabled; refresh(); break;
            case 9: mc.displayGuiScreen(new ProfilesScreen(this)); break;
            case 10: close(); break;
        }
    }
    @Override public void onGuiClosed() { if (eng() != null) eng().saveSettings(); }

    private void section(String label, SoundEngine.Slot slot, int y) {
        int px = panelX(), pw = panelW();
        SoundPack p = slot.pack();
        drawString(fontRendererObj, label + (p == null ? "" : "  \u00b7  by " + p.author + "  \u00b7  " + p.keyCount() + " keys"), px + 8, y + 2, DIM);
        String name = p == null ? "No profile - use Profiles / Import" : p.name;
        drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(name, pw - 80), width / 2, y + 18, p == null ? BAD : WHITE);
    }
    @Override public void drawScreen(int mx, int my, float pt) {
        int t = top(); drawBox("MechKeys", "unofficial · not affiliated with Mechvibes", t, t + H);
        if (eng() == null) { drawCenteredString(fontRendererObj, "MechKeys failed to start - see logs/latest.log", width / 2, t + 60, BAD); return; }
        section("KEYBOARD", eng().keyboard, t + 22);
        section("MOUSE", eng().mouse, t + 80);
        if (!eng().mixer.isAvailable() && eng().mixer.getError() != null) drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(eng().mixer.getError(), panelW() - 20), width / 2, t + H - 40, BAD);
        super.drawScreen(mx, my, pt);
    }
}
