package io.github.notthatlonely.mechkeys.forge;

import io.github.notthatlonely.mechkeys.core.SoundEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;

/**
 * Base class: shared theme, panel drawing, and engine access.
 *
 * <p><b>PORTING NOTES (next update)</b>: written against Forge 1.8.9 / MCP stable_22 ({@code GuiScreen}, {@code drawRect}, {@code fontRendererObj}).
 * Another legacy version only needs the helper methods here adapted; the screens use the helpers.
 */
public abstract class MKScreen extends GuiScreen {
    public static final int BG = 0xB00B0B12, PANEL = 0xF0191923, PANEL2 = 0xFF242434, ACCENT = 0xFFFF9E3D, WHITE = 0xFFFFFFFF,
            DIM = 0xFF9A9AB0, OK = 0xFF3DDC84, BAD = 0xFFE5484D, KEY_ON = 0xFF3A3A52, KEY_OFF = 0xFF2A1F26, KEY_SEL = 0xFFFF9E3D, KEY_HOVER = 0xFF55557A;
    protected final GuiScreen parent;
    protected MKScreen(GuiScreen parent) { this.parent = parent; }
    protected SoundEngine eng() { return MechKeysMod.engine; }
    protected void close() { mc.displayGuiScreen(parent); }

    protected int panelW() { return Math.min(width - 12, 420); }
    protected int panelX() { return (width - panelW()) / 2; }

    protected void drawPanel(String title, String subtitle) {
        drawRect(0, 0, width, height, BG);
        int px = panelX(), pw = panelW();
        drawRect(px - 1, 5, px + pw + 1, height - 5, 0xFF000000);
        drawRect(px, 6, px + pw, height - 6, PANEL);
        drawRect(px, 6, px + pw, 8, ACCENT);
        drawString(fontRendererObj, title, px + 8, 13, WHITE);
        if (subtitle != null) drawString(fontRendererObj, subtitle, px + 8 + fontRendererObj.getStringWidth(title) + 6, 13, DIM);
    }
    /** Compact panel that only spans [top, bottom) - used by the small main screen. */
    protected void drawBox(String title, String subtitle, int top, int bottom) {
        drawRect(0, 0, width, height, BG);
        int px = panelX(), pw = panelW();
        drawRect(px - 1, top - 1, px + pw + 1, bottom + 1, 0xFF000000);
        drawRect(px, top, px + pw, bottom, PANEL);
        drawRect(px, top, px + pw, top + 2, ACCENT);
        drawString(fontRendererObj, title, px + 8, top + 7, WHITE);
        if (subtitle != null) drawString(fontRendererObj, subtitle, px + 8 + fontRendererObj.getStringWidth(title) + 6, top + 7, DIM);
    }
    protected void text(String s, int x, int y, int color) { drawString(fontRendererObj, fontRendererObj.trimStringToWidth(s, Math.max(10, panelX() + panelW() - x - 6)), x, y, color); }
    protected void small(String s, int x, int y, int color) {
        GlStateManager.pushMatrix(); GlStateManager.scale(0.75f, 0.75f, 1f);
        drawString(fontRendererObj, s, (int) (x / 0.75f), (int) (y / 0.75f), color); GlStateManager.popMatrix();
    }
    @Override public boolean doesGuiPauseGame() { return false; }

    /** Horizontal slider using vanilla button textures. */
    public static class Slider extends GuiButton {
        public interface Fmt { String text(float v); }
        public interface Change { void changed(float v); }
        private final float min, max; private final Fmt fmt; private final Change cb; public float value; private boolean dragging;
        public Slider(int id, int x, int y, int w, int h, float min, float max, float init, Fmt fmt, Change cb) {
            super(id, x, y, w, h, ""); this.min = min; this.max = max; this.fmt = fmt; this.cb = cb; set(init); }
        public void set(float v) { value = Math.max(min, Math.min(max, v)); displayString = fmt.text(value); }
        private float norm() { return (value - min) / (max - min); }
        @Override protected int getHoverState(boolean over) { return 0; }
        private void drag(int mx) {
            float n = (float) (mx - (xPosition + 4)) / (float) (width - 8); n = Math.max(0f, Math.min(1f, n));
            set(min + n * (max - min)); cb.changed(value);
        }
        @Override protected void mouseDragged(Minecraft mc, int mx, int my) {
            if (!visible) return;
            if (dragging) drag(mx);
            mc.getTextureManager().bindTexture(buttonTextures); GlStateManager.color(1f, 1f, 1f, 1f);
            int kx = xPosition + (int) (norm() * (width - 8));
            drawTexturedModalRect(kx, yPosition, 0, 66, 4, 20); drawTexturedModalRect(kx + 4, yPosition, 196, 66, 4, 20);
        }
        @Override public boolean mousePressed(Minecraft mc, int mx, int my) { if (super.mousePressed(mc, mx, my)) { dragging = true; drag(mx); return true; } return false; }
        @Override public void mouseReleased(int mx, int my) { dragging = false; }
    }

    /** Fixed-height text button helper. */
    protected GuiButton btn(int id, int x, int y, int w, int h, String s) { GuiButton b = new GuiButton(id, x, y, w, h, s); buttonList.add(b); return b; }
}
