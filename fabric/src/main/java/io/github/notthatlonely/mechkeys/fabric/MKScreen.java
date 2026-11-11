package io.github.notthatlonely.mechkeys.fabric;

import io.github.notthatlonely.mechkeys.core.SoundEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base class: shared theme, panel drawing, and engine access.
 *
 * <p><b>PORTING NOTES (next update)</b>: all drawing goes through {@code GuiGraphicsExtractor} ({@code fill}, {@code text}).
 * If a new version renames the graphics class or these calls, fix them HERE first - MainScreen / ProfilesScreen / PromptScreen
 * only use the helpers {@code str}, {@code center}, {@code drawPanel}, {@code drawBox}, so most UI code does not need to change.
 */
public abstract class MKScreen extends Screen {
    public static final int BG = 0xB00B0B12, PANEL = 0xF0191923, PANEL2 = 0xFF242434, ACCENT = 0xFFFF9E3D, WHITE = 0xFFFFFFFF,
            DIM = 0xFF9A9AB0, OK = 0xFF3DDC84, BAD = 0xFFE5484D, KEY_ON = 0xFF3A3A52, KEY_OFF = 0xFF2A1F26, KEY_SEL = 0xFFFF9E3D, KEY_HOVER = 0xFF55557A;
    protected final Screen parent;
    protected MKScreen(Screen parent, String title) { super(Component.literal(title)); this.parent = parent; }
    protected SoundEngine eng() { return MechKeysMod.engine; }
    protected void close() { Compat.setScreen(minecraft, parent); }
    @Override public void onClose() { close(); }
    @Override public boolean isPauseScreen() { return false; }
    /** The screens paint their own backdrop, so skip the vanilla blur/darkening. */
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float pt) {}

    protected int panelW() { return Math.min(width - 12, 420); }
    protected int panelX() { return (width - panelW()) / 2; }

    protected void str(GuiGraphicsExtractor g, String s, int x, int y, int color) { g.text(font, s, x, y, color); }
    protected void center(GuiGraphicsExtractor g, String s, int cx, int y, int color) { g.text(font, s, cx - font.width(s) / 2, y, color); }

    protected void drawPanel(GuiGraphicsExtractor g, String title, String subtitle) {
        g.fill(0, 0, width, height, BG);
        int px = panelX(), pw = panelW();
        g.fill(px - 1, 5, px + pw + 1, height - 5, 0xFF000000);
        g.fill(px, 6, px + pw, height - 6, PANEL);
        g.fill(px, 6, px + pw, 8, ACCENT);
        str(g, title, px + 8, 13, WHITE);
        if (subtitle != null) str(g, subtitle, px + 8 + font.width(title) + 6, 13, DIM);
    }
    /** Compact panel that only spans [top, bottom) - used by the small main screen. */
    protected void drawBox(GuiGraphicsExtractor g, String title, String subtitle, int top, int bottom) {
        g.fill(0, 0, width, height, BG);
        int px = panelX(), pw = panelW();
        g.fill(px - 1, top - 1, px + pw + 1, bottom + 1, 0xFF000000);
        g.fill(px, top, px + pw, bottom, PANEL);
        g.fill(px, top, px + pw, top + 2, ACCENT);
        str(g, title, px + 8, top + 7, WHITE);
        if (subtitle != null) str(g, subtitle, px + 8 + font.width(title) + 6, top + 7, DIM);
    }
    protected void small(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.pose().pushMatrix(); g.pose().scale(0.75f, 0.75f);
        g.text(font, s, (int) (x / 0.75f), (int) (y / 0.75f), color); g.pose().popMatrix();
    }

    /** Horizontal slider (vanilla slider look). */
    public static class Slider extends AbstractSliderButton {
        public interface Fmt { String text(float v); }
        public interface Change { void changed(float v); }
        private final float min, max; private final Fmt fmt; private final Change cb;
        public Slider(int x, int y, int w, int h, float min, float max, float init, Fmt fmt, Change cb) {
            super(x, y, w, h, Component.empty(), (Math.max(min, Math.min(max, init)) - min) / (max - min));
            this.min = min; this.max = max; this.fmt = fmt; this.cb = cb; updateMessage();
        }
        private float val() { return min + (float) value * (max - min); }
        @Override protected void updateMessage() { if (fmt != null) setMessage(Component.literal(fmt.text(val()))); }
        @Override protected void applyValue() { cb.changed(val()); }
    }

    /** Fixed-height text button helper. */
    protected Button btn(int x, int y, int w, int h, String s, Button.OnPress press) {
        Button b = Button.builder(Component.literal(s), press).bounds(x, y, w, h).build(); addRenderableWidget(b); return b;
    }
}
