package com.example.custombans.util;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing human-readable time durations.
 *
 * <p>Supported formats (case-insensitive, can be combined):
 * <pre>
 *   30s / 30sec / 30secs / 30second / 30seconds
 *   5m  / 5min  / 5mins  / 5minute  / 5minutes
 *   2h  / 2hr   / 2hrs   / 2hour    / 2hours
 *   7d  / 7day  / 7days
 *   2w  / 2week / 2weeks
 *   1mo / 1month/ 1months
 *   1y  / 1yr   / 1year  / 1years
 * </pre>
 *
 * <p>Examples: {@code 1d12h30m}, {@code 2w3d}, {@code 1y2mo}, {@code 45s}
 */
public final class TimeUtil {

    private TimeUtil() {}

    // Captures one numeric component + unit, e.g. "2h", "30seconds", "1mo"
    private static final Pattern COMPONENT =
            Pattern.compile("(\\d+)\\s*(y(?:ears?|rs?)?|mo(?:nths?)?|w(?:eeks?)?|d(?:ays?)?|h(?:ours?|rs?)?|m(?:in(?:utes?|s)?)?|s(?:ec(?:onds?|s?)?)?)",
                    Pattern.CASE_INSENSITIVE);

    // A complete valid time string: one or more components, nothing else
    private static final Pattern FULL_TIME =
            Pattern.compile("^(?:\\d+(?:y(?:ears?|rs?)?|mo(?:nths?)?|w(?:eeks?)?|d(?:ays?)?|h(?:ours?|rs?)?|m(?:in(?:utes?|s)?)?|s(?:ec(?:onds?|s?)?)?))+" ,
                    Pattern.CASE_INSENSITIVE);

    /**
     * Returns true if the string looks like a time duration (not a ban reason).
     */
    public static boolean isTimeDuration(String s) {
        return s != null && FULL_TIME.matcher(s).matches();
    }

    /**
     * Parses a time string into a {@link Duration}, or {@code null} if invalid.
     *
     * <p>Months are approximated as 30 days; years as 365 days.
     */
    @Nullable
    public static Duration parseDuration(String input) {
        if (input == null || input.isBlank()) return null;

        Matcher m = COMPONENT.matcher(input);
        long seconds = 0L;
        int lastEnd = 0;

        while (m.find()) {
            if (m.start() != lastEnd) return null; // gap → invalid
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2).toLowerCase();

            if (unit.startsWith("y")) {
                seconds += value * 365L * 24 * 3600;
            } else if (unit.startsWith("mo")) {
                seconds += value * 30L * 24 * 3600;
            } else if (unit.startsWith("w")) {
                seconds += value * 7L * 24 * 3600;
            } else if (unit.startsWith("d")) {
                seconds += value * 24L * 3600;
            } else if (unit.startsWith("h")) {
                seconds += value * 3600L;
            } else if (unit.startsWith("m")) {
                seconds += value * 60L;
            } else if (unit.startsWith("s")) {
                seconds += value;
            }
            lastEnd = m.end();
        }

        if (lastEnd != input.length() || seconds == 0) return null;
        return Duration.ofSeconds(seconds);
    }

    /**
     * Formats a {@link Duration} as a human-readable string.
     *
     * <p>E.g. {@code 2d 3h 15m 6s}, using the configured separator.
     *
     * @param d         duration to format
     * @param separator string to place between components (from config)
     */
    public static String formatDuration(Duration d, String separator) {
        if (d == null || d.isZero() || d.isNegative()) return "0s";

        long total = d.toSeconds();
        long years   = total / (365L * 24 * 3600);   total %= 365L * 24 * 3600;
        long months  = total / (30L  * 24 * 3600);   total %= 30L  * 24 * 3600;
        long weeks   = total / (7L   * 24 * 3600);   total %= 7L   * 24 * 3600;
        long days    = total / (24L  * 3600);          total %= 24L  * 3600;
        long hours   = total / 3600L;                  total %= 3600L;
        long minutes = total / 60L;
        long seconds = total % 60L;

        List<String> parts = new ArrayList<>();
        if (years   > 0) parts.add(years   + "y");
        if (months  > 0) parts.add(months  + "mo");
        if (weeks   > 0) parts.add(weeks   + "w");
        if (days    > 0) parts.add(days    + "d");
        if (hours   > 0) parts.add(hours   + "h");
        if (minutes > 0) parts.add(minutes + "m");
        if (seconds > 0) parts.add(seconds + "s");

        return parts.isEmpty() ? "0s" : String.join(separator, parts);
    }

    /**
     * Formats remaining time until {@code expiryMs} (epoch millis), or
     * {@code permanentText} if expiry is {@code null}.
     */
    public static String formatRemaining(long expiryMs, String separator, String permanentText) {
        long remaining = expiryMs - System.currentTimeMillis();
        if (remaining <= 0) return "0s";
        return formatDuration(Duration.ofMillis(remaining), separator);
    }
}
