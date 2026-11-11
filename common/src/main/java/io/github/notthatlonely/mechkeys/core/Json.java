package io.github.notthatlonely.mechkeys.core;

import java.util.*;

/** Minimal dependency-free JSON parser/writer (maps -> LinkedHashMap, arrays -> ArrayList, numbers -> Double). */
public final class Json {
    private final String s; private int p;
    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json j = new Json(text);
        j.ws();
        Object v = j.value();
        j.ws();
        if (j.p != j.s.length()) throw new IllegalArgumentException("Trailing data at " + j.p);
        return v;
    }
    private void ws() { while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++; if (p < s.length() && s.charAt(p) == '\uFEFF') { p++; ws(); } }
    private Object value() {
        if (p >= s.length()) throw new IllegalArgumentException("Unexpected end");
        char c = s.charAt(p);
        if (c == '{') {
            p++; Map<String, Object> m = new LinkedHashMap<String, Object>(); ws();
            if (s.charAt(p) == '}') { p++; return m; }
            while (true) {
                ws(); String k = str(); ws(); expect(':'); ws(); m.put(k, value()); ws();
                if (s.charAt(p) == ',') { p++; continue; }
                expect('}'); return m;
            }
        }
        if (c == '[') {
            p++; List<Object> l = new ArrayList<Object>(); ws();
            if (s.charAt(p) == ']') { p++; return l; }
            while (true) {
                ws(); l.add(value()); ws();
                if (s.charAt(p) == ',') { p++; continue; }
                expect(']'); return l;
            }
        }
        if (c == '"') return str();
        if (s.startsWith("true", p)) { p += 4; return Boolean.TRUE; }
        if (s.startsWith("false", p)) { p += 5; return Boolean.FALSE; }
        if (s.startsWith("null", p)) { p += 4; return null; }
        int st = p;
        while (p < s.length() && "+-0123456789.eE".indexOf(s.charAt(p)) >= 0) p++;
        if (st == p) throw new IllegalArgumentException("Bad token at " + p);
        return Double.valueOf(s.substring(st, p));
    }
    private void expect(char c) { if (p >= s.length() || s.charAt(p) != c) throw new IllegalArgumentException("Expected '" + c + "' at " + p); p++; }
    private String str() {
        expect('"'); StringBuilder b = new StringBuilder();
        while (true) {
            char c = s.charAt(p++);
            if (c == '"') return b.toString();
            if (c == '\\') {
                char e = s.charAt(p++);
                switch (e) {
                    case 'n': b.append('\n'); break; case 't': b.append('\t'); break; case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break; case 'f': b.append('\f'); break;
                    case 'u': b.append((char) Integer.parseInt(s.substring(p, p + 4), 16)); p += 4; break;
                    default: b.append(e);
                }
            } else b.append(c);
        }
    }

    public static String write(Object o) { StringBuilder b = new StringBuilder(); w(o, b, 0); return b.toString(); }
    @SuppressWarnings("unchecked")
    private static void w(Object o, StringBuilder b, int ind) {
        if (o == null) b.append("null");
        else if (o instanceof String) q((String) o, b);
        else if (o instanceof Boolean) b.append(o);
        else if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d); else b.append(d);
        } else if (o instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) o;
            if (m.isEmpty()) { b.append("{}"); return; }
            b.append("{\n"); int i = 0;
            for (Map.Entry<String, Object> e : m.entrySet()) {
                pad(b, ind + 1); q(e.getKey(), b); b.append(": "); w(e.getValue(), b, ind + 1);
                if (++i < m.size()) b.append(','); b.append('\n');
            }
            pad(b, ind); b.append('}');
        } else if (o instanceof List) {
            List<Object> l = (List<Object>) o; b.append('[');
            for (int i = 0; i < l.size(); i++) { if (i > 0) b.append(", "); w(l.get(i), b, ind); }
            b.append(']');
        } else throw new IllegalArgumentException("Unsupported " + o.getClass());
    }
    private static void pad(StringBuilder b, int n) { for (int i = 0; i < n; i++) b.append("  "); }
    private static void q(String s, StringBuilder b) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c == '\n') b.append("\\n"); else if (c == '\r') b.append("\\r"); else if (c == '\t') b.append("\\t");
            else if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c);
        }
        b.append('"');
    }
}
