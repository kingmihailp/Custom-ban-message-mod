package com.example.custombans.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses color-coded strings into Minecraft {@link Component} objects.
 *
 * <h3>Supported syntax</h3>
 * <ul>
 *   <li>{@code &#RRGGBB} — hex color (24-bit)</li>
 *   <li>{@code &0-9}, {@code &a-f} — standard legacy colors</li>
 *   <li>{@code &l} bold, {@code &o} italic, {@code &n} underline,
 *       {@code &m} strikethrough, {@code &k} obfuscated</li>
 *   <li>{@code &r} — reset all formatting</li>
 * </ul>
 */
public final class TextUtil {

    private TextUtil() {}

    /** Matches &amp;#RRGGBB or &amp;X (legacy color/format code). */
    private static final Pattern CODE = Pattern.compile("&#([0-9A-Fa-f]{6})|&([0-9a-fA-FrRlLoOnNmMkK])");

    // Legacy color code → RGB
    private static final int[] LEGACY_COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    /**
     * Parses a color-coded string into a single {@link Component}.
     * Newlines in the text produce line-break literals.
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // Split on actual newline characters so we can handle them properly
        // Then process each segment
        return parseSegment(text, Style.EMPTY);
    }

    private static MutableComponent parseSegment(String text, Style baseStyle) {
        MutableComponent root = Component.empty();
        Matcher m = CODE.matcher(text);

        int cursor = 0;
        Style current = baseStyle;

        while (m.find()) {
            // Append plain text before this code
            if (m.start() > cursor) {
                String plain = text.substring(cursor, m.start());
                appendPlain(root, plain, current);
            }
            cursor = m.end();

            // Determine new style
            if (m.group(1) != null) {
                // Hex color &#RRGGBB
                int rgb = Integer.parseInt(m.group(1), 16);
                current = current.withColor(TextColor.fromRgb(rgb));
            } else {
                char code = Character.toLowerCase(m.group(2).charAt(0));
                current = applyLegacyCode(current, code);
            }
        }

        // Remaining text after last code
        if (cursor < text.length()) {
            appendPlain(root, text.substring(cursor), current);
        }

        return root;
    }

    /** Appends a plain text segment, respecting embedded \n characters. */
    private static void appendPlain(MutableComponent root, String text, Style style) {
        // Handle explicit \n within the text (from newline characters)
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) root.append(Component.literal("\n"));
            if (!lines[i].isEmpty()) {
                root.append(Component.literal(lines[i]).withStyle(style));
            }
        }
    }

    private static Style applyLegacyCode(Style style, char code) {
        return switch (code) {
            case '0' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[0]));
            case '1' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[1]));
            case '2' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[2]));
            case '3' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[3]));
            case '4' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[4]));
            case '5' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[5]));
            case '6' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[6]));
            case '7' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[7]));
            case '8' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[8]));
            case '9' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[9]));
            case 'a' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[10]));
            case 'b' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[11]));
            case 'c' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[12]));
            case 'd' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[13]));
            case 'e' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[14]));
            case 'f' -> Style.EMPTY.withColor(TextColor.fromRgb(LEGACY_COLORS[15]));
            case 'l' -> style.withBold(true);
            case 'o' -> style.withItalic(true);
            case 'n' -> style.withUnderlined(true);
            case 'm' -> style.withStrikethrough(true);
            case 'k' -> style.withObfuscated(true);
            case 'r' -> Style.EMPTY;
            default  -> style;
        };
    }

    /**
     * Joins a list of line strings into a single {@link Component} separated by
     * newlines. Each line is parsed for color codes.
     */
    public static Component joinLines(List<? extends String> lines) {
        if (lines == null || lines.isEmpty()) return Component.empty();

        MutableComponent result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) result.append(Component.literal("\n"));
            result.append(parse(lines.get(i)));
        }
        return result;
    }

    /**
     * Strips all color/format codes from a string, returning plain text.
     */
    public static String stripCodes(String text) {
        if (text == null) return "";
        return CODE.matcher(text).replaceAll("");
    }
}
