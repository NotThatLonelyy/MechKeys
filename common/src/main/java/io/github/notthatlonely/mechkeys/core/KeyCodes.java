package io.github.notthatlonely.mechkeys.core;

import java.util.*;

/**
 * Maps LWJGL 2 keycodes (Minecraft 1.8.9 / Forge) to Mechvibes V2 key names (W3C KeyboardEvent.code style),
 * and legacy Mechvibes V1 (uiohook) codes to the same names.
 * LWJGL2 codes equal DirectInput scancodes, which match uiohook codes for the base range 1..88.
 */
public final class KeyCodes {
    private KeyCodes() {}
    private static final Map<Integer, String> LWJGL = new HashMap<Integer, String>();
    private static final Map<String, Integer> NAME_TO_LWJGL = new LinkedHashMap<String, Integer>();
    private static final Map<Integer, String> V1 = new HashMap<Integer, String>();
    public static final String[] ALL_KEYS;

    private static void k(int code, String name) { LWJGL.put(code, name); NAME_TO_LWJGL.put(name, code); V1.put(code, name); }

    static {
        k(1, "Escape");
        String[] digits = {"Digit1","Digit2","Digit3","Digit4","Digit5","Digit6","Digit7","Digit8","Digit9","Digit0"};
        for (int i = 0; i < 10; i++) k(2 + i, digits[i]);
        k(12, "Minus"); k(13, "Equal"); k(14, "Backspace"); k(15, "Tab");
        String q = "QWERTYUIOP"; for (int i = 0; i < q.length(); i++) k(16 + i, "Key" + q.charAt(i));
        k(26, "BracketLeft"); k(27, "BracketRight"); k(28, "Enter"); k(29, "ControlLeft");
        String a = "ASDFGHJKL"; for (int i = 0; i < a.length(); i++) k(30 + i, "Key" + a.charAt(i));
        k(39, "Semicolon"); k(40, "Quote"); k(41, "Backquote"); k(42, "ShiftLeft"); k(43, "Backslash");
        String z = "ZXCVBNM"; for (int i = 0; i < z.length(); i++) k(44 + i, "Key" + z.charAt(i));
        k(51, "Comma"); k(52, "Period"); k(53, "Slash"); k(54, "ShiftRight"); k(55, "NumpadMultiply");
        k(56, "AltLeft"); k(57, "Space"); k(58, "CapsLock");
        for (int i = 1; i <= 10; i++) k(58 + i, "F" + i);
        k(69, "NumLock"); k(70, "ScrollLock");
        k(71, "Numpad7"); k(72, "Numpad8"); k(73, "Numpad9"); k(74, "NumpadSubtract");
        k(75, "Numpad4"); k(76, "Numpad5"); k(77, "Numpad6"); k(78, "NumpadAdd");
        k(79, "Numpad1"); k(80, "Numpad2"); k(81, "Numpad3"); k(82, "Numpad0"); k(83, "NumpadDecimal");
        k(87, "F11"); k(88, "F12");
        // LWJGL2 extended codes
        k(156, "NumpadEnter"); k(157, "ControlRight"); k(181, "NumpadDivide"); k(183, "PrintScreen");
        k(184, "AltRight"); k(197, "Pause"); k(199, "Home"); k(200, "ArrowUp"); k(201, "PageUp");
        k(203, "ArrowLeft"); k(205, "ArrowRight"); k(207, "End"); k(208, "ArrowDown"); k(209, "PageDown");
        k(210, "Insert"); k(211, "Delete"); k(219, "MetaLeft"); k(220, "MetaRight"); k(221, "ContextMenu");
        // V1 (Mechvibes 2 / uiohook) extended codes differ from LWJGL's; they only apply when reading V1 packs.
        int[][] v1ext = {{3597,0},{3612,0},{3613,0},{3637,0},{3639,0},{3640,0},{3653,0},{3655,0},{3657,0},{3663,0},{3665,0},{3666,0},{3667,0},{3675,0},{3676,0},{3677,0}};
        String[] v1n = {"NumpadEqual","NumpadEnter","ControlRight","NumpadDivide","PrintScreen","AltRight","Pause","Home","PageUp","End","PageDown","Insert","Delete","MetaLeft","MetaRight","ContextMenu"};
        for (int i = 0; i < v1ext.length; i++) V1.put(v1ext[i][0], v1n[i]);
        // 0xE0xx range some builds report
        V1.put(57416, "ArrowUp"); V1.put(57419, "ArrowLeft"); V1.put(57421, "ArrowRight"); V1.put(57424, "ArrowDown");
        V1.put(57399, "PrintScreen"); V1.put(57415, "Home"); V1.put(57417, "PageUp"); V1.put(57423, "End");
        V1.put(57425, "PageDown"); V1.put(57426, "Insert"); V1.put(57427, "Delete");
        V1.put(61000, "ArrowUp"); V1.put(61003, "ArrowLeft"); V1.put(61005, "ArrowRight"); V1.put(61008, "ArrowDown");
        List<String> all = new ArrayList<String>(NAME_TO_LWJGL.keySet());
        all.add("MouseLeft"); all.add("MouseRight"); all.add("MouseMiddle"); all.add("MouseWheelUp"); all.add("MouseWheelDown");
        ALL_KEYS = all.toArray(new String[0]);
    }

    /** LWJGL keycode -> pack key name, or null. */
    public static String fromLwjgl(int code) { return LWJGL.get(code); }
    /** V1 define key (e.g. "30" or "01") -> pack key name; null if unknown. */
    public static String fromV1(int code) { return V1.get(code); }
    public static String mouse(int button) {
        switch (button) { case 0: return "MouseLeft"; case 1: return "MouseRight"; case 2: return "MouseMiddle"; default: return null; }
    }
    public static boolean isMouseName(String n) { return n.startsWith("Mouse"); }
}
