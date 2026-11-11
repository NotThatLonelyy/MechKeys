package io.github.notthatlonely.mechkeys.fabric;

import com.mojang.blaze3d.platform.InputConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Game key / mouse-button codes -> sound-pack key names. Built from InputConstants at startup, so it is right for GLFW codes
 * (26.1, 26.2) and SDL scancodes (26.3) alike. Constants missing in a given version are simply skipped.
 *
 * <p><b>PORTING NOTES (next update)</b>: if a key stops making sound on a new version, a constant was probably renamed in
 * {@code InputConstants}; add the new field name to the table below (the old one can stay, missing fields are ignored).
 * Pack key names are Mechvibes V2 / W3C style ({@code KeyA}, {@code ShiftLeft}, {@code MouseLeft} ...).
 */
final class Keys {
    private Keys() {}
    private static final Map<Integer, String> MAP = new HashMap<Integer, String>();
    private static final int LEFT = c("MOUSE_BUTTON_LEFT", 0), RIGHT = c("MOUSE_BUTTON_RIGHT", 1), MIDDLE = c("MOUSE_BUTTON_MIDDLE", 2);

    private static int c(String field, int dflt) { try { return InputConstants.class.getField(field).getInt(null); } catch (Throwable t) { return dflt; } }
    private static void put(String field, String name) {
        try { MAP.putIfAbsent(InputConstants.class.getField(field).getInt(null), name); } catch (Throwable ignored) {}
    }

    static {
        for (char ch = 'A'; ch <= 'Z'; ch++) put("KEY_" + ch, "Key" + ch);
        for (int i = 0; i <= 9; i++) { put("KEY_" + i, "Digit" + i); put("KEY_NUMPAD" + i, "Numpad" + i); }
        for (int i = 1; i <= 12; i++) put("KEY_F" + i, "F" + i);
        String[][] t = {
            {"KEY_ESCAPE","Escape"},{"KEY_MINUS","Minus"},{"KEY_EQUAL","Equal"},{"KEY_BACKSPACE","Backspace"},{"KEY_TAB","Tab"},
            {"KEY_LBRACKET","BracketLeft"},{"KEY_RBRACKET","BracketRight"},{"KEY_RETURN","Enter"},{"KEY_LCONTROL","ControlLeft"},
            {"KEY_SEMICOLON","Semicolon"},{"KEY_APOSTROPHE","Quote"},{"KEY_GRAVE","Backquote"},{"KEY_LSHIFT","ShiftLeft"},
            {"KEY_BACKSLASH","Backslash"},{"KEY_COMMA","Comma"},{"KEY_PERIOD","Period"},{"KEY_SLASH","Slash"},{"KEY_RSHIFT","ShiftRight"},
            {"KEY_MULTIPLY","NumpadMultiply"},{"KEY_LALT","AltLeft"},{"KEY_SPACE","Space"},{"KEY_CAPSLOCK","CapsLock"},
            {"KEY_NUMLOCK","NumLock"},{"KEY_SCROLLLOCK","ScrollLock"},{"KEY_SUBTRACT","NumpadSubtract"},{"KEY_ADD","NumpadAdd"},
            {"KEY_DECIMAL","NumpadDecimal"},{"KEY_NUMPADENTER","NumpadEnter"},{"KEY_RCONTROL","ControlRight"},{"KEY_DIVIDE","NumpadDivide"},
            {"KEY_PRINTSCREEN","PrintScreen"},{"KEY_RALT","AltRight"},{"KEY_PAUSE","Pause"},{"KEY_HOME","Home"},{"KEY_UP","ArrowUp"},
            {"KEY_PAGEUP","PageUp"},{"KEY_LEFT","ArrowLeft"},{"KEY_RIGHT","ArrowRight"},{"KEY_END","End"},{"KEY_DOWN","ArrowDown"},
            {"KEY_PAGEDOWN","PageDown"},{"KEY_INSERT","Insert"},{"KEY_DELETE","Delete"},{"KEY_LWIN","MetaLeft"},{"KEY_RWIN","MetaRight"},
            {"KEY_MENU","ContextMenu"}};
        for (String[] e : t) put(e[0], e[1]);
    }

    static String key(int code) { return MAP.get(code); }
    static String mouse(int b) { return b == LEFT ? "MouseLeft" : b == RIGHT ? "MouseRight" : b == MIDDLE ? "MouseMiddle" : null; }
}
