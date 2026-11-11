package io.github.notthatlonely.mechkeys.fabric;

import io.github.notthatlonely.mechkeys.core.SoundEngine;
import io.github.notthatlonely.mechkeys.core.SoundPack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** The whole settings UI: keyboard and mouse are independent - profile, on/off and volume each. */
public class MainScreen extends MKScreen {
    private static final int H = 168;
    private Button kbToggle, mouseToggle;
    public MainScreen(Screen parent) { super(parent, "MechKeys"); }

    private int top() { return Math.max(6, (height - H) / 2); }

    @Override protected void init() {
        if (eng() == null) return;
        int px = panelX(), pw = panelW(), half = (pw - 24) / 2, t = top();
        // Keyboard
        btn(px + 8, t + 34, 20, 20, "<", b -> cycle(eng().keyboard, -1)); btn(px + pw - 28, t + 34, 20, 20, ">", b -> cycle(eng().keyboard, 1));
        kbToggle = btn(px + 8, t + 58, half, 20, "", b -> { eng().settings.keyboardEnabled = !eng().settings.keyboardEnabled; refresh(); });
        addRenderableWidget(new Slider(px + 16 + half, t + 58, half, 20, 0f, 2f, eng().settings.keyboardVolume, fmt("Volume"), v -> eng().settings.keyboardVolume = v));
        // Mouse
        btn(px + 8, t + 92, 20, 20, "<", b -> cycle(eng().mouse, -1)); btn(px + pw - 28, t + 92, 20, 20, ">", b -> cycle(eng().mouse, 1));
        mouseToggle = btn(px + 8, t + 116, half, 20, "", b -> { eng().settings.mouseEnabled = !eng().settings.mouseEnabled; refresh(); });
        addRenderableWidget(new Slider(px + 16 + half, t + 116, half, 20, 0f, 2f, eng().settings.mouseVolume, fmt("Volume"), v -> eng().settings.mouseVolume = v));
        btn(px + 8, t + H - 26, half, 20, "Profiles / Import", b -> Compat.setScreen(minecraft, new ProfilesScreen(this)));
        btn(px + 16 + half, t + H - 26, half, 20, "Done", b -> close());
        refresh();
    }
    private static Slider.Fmt fmt(final String label) { return v -> label + ": " + Math.round(v * 100) + "%"; }
    private static Component onoff(String label, boolean on) {
        return Component.literal(label + ": ").append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
    private void refresh() {
        kbToggle.setMessage(onoff("Keyboard", eng().settings.keyboardEnabled));
        mouseToggle.setMessage(onoff("Mouse", eng().settings.mouseEnabled));
    }

    private void cycle(SoundEngine.Slot slot, int d) {
        List<SoundPack> l = eng().profiles.list(slot.mouse); if (l.isEmpty()) return;
        int cur = 0; SoundPack a = slot.pack(); for (int i = 0; i < l.size(); i++) if (a != null && l.get(i).id.equals(a.id)) cur = i;
        eng().select(l.get(((cur + d) % l.size() + l.size()) % l.size()).id, true);
    }
    @Override public void removed() { if (eng() != null) eng().saveSettings(); }

    private void section(GuiGraphicsExtractor g, String label, SoundEngine.Slot slot, int y) {
        int px = panelX(), pw = panelW();
        SoundPack p = slot.pack();
        str(g, label + (p == null ? "" : "  \u00b7  by " + p.author + "  \u00b7  " + p.keyCount() + " keys"), px + 8, y + 2, DIM);
        String name = p == null ? "No profile - use Profiles / Import" : p.name;
        center(g, font.plainSubstrByWidth(name, pw - 80), width / 2, y + 18, p == null ? BAD : WHITE);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        int t = top(); drawBox(g, "MechKeys", "unofficial \u00b7 not affiliated with Mechvibes", t, t + H);
        if (eng() == null) { center(g, "MechKeys failed to start - see logs/latest.log", width / 2, t + 60, BAD); return; }
        section(g, "KEYBOARD", eng().keyboard, t + 22);
        section(g, "MOUSE", eng().mouse, t + 80);
        if (!eng().mixer.isAvailable() && eng().mixer.getError() != null) center(g, font.plainSubstrByWidth(eng().mixer.getError(), panelW() - 20), width / 2, t + H - 40, BAD);
        super.extractRenderState(g, mx, my, pt);
    }
}
