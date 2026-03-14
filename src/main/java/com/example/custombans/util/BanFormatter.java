package com.example.custombans.util;

import com.example.custombans.CustomBansMod;
import com.example.custombans.config.BanConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.UserBanListEntry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Builds formatted {@link Component} objects for the ban screen and chat
 * notifications by applying config templates and replacing placeholders.
 */
public final class BanFormatter {

    private BanFormatter() {}

    // ── Player ban ────────────────────────────────────────────────────────────

    /**
     * Builds the full ban-screen component for a player ban entry.
     *
     * @param entry   the vanilla {@link UserBanListEntry} for the player
     * @param name    the player's display name
     * @return a multi-line {@link Component} ready to be sent as the disconnect reason
     */
    public static Component buildBanScreen(UserBanListEntry entry, String name) {
        BanConfig cfg = CustomBansMod.CONFIG;

        String dateStr    = formatDate(entry.getCreated(), cfg.dateFormat.get());
        String expiresStr = formatExpiry(entry.getExpires(), cfg);
        String durationStr = formatRemainingDuration(entry.getExpires(), cfg);
        String reason     = entry.getReason() != null ? entry.getReason() : "Banned by an operator.";
        String source     = entry.getSource() != null ? entry.getSource() : "Console";

        // Title line
        String title = applyPlaceholders(cfg.banScreenTitle.get(),
                name, source, reason, dateStr, expiresStr, durationStr);

        // Body lines
        List<? extends String> raw = cfg.banScreenLines.get();
        List<String> filled = raw.stream()
                .map(line -> applyPlaceholders(line, name, source, reason, dateStr, expiresStr, durationStr))
                .toList();

        // Combine: title + newline + body
        Component titleComp = TextUtil.parse(title);
        Component bodyComp  = TextUtil.joinLines(filled);

        net.minecraft.network.chat.MutableComponent root = Component.empty()
                .append(titleComp)
                .append(Component.literal("\n"))
                .append(bodyComp);

        // Embed the image filename as an invisible style marker so the client-side
        // mixin can read it from the disconnect packet without affecting rendered text.
        String imageName = CustomBansMod.CONFIG.banScreenImage.get().trim();
        if (!imageName.isEmpty()) {
            root.withStyle(s -> s.withInsertion("CUSTOMBANS_IMAGE:" + imageName));
        }

        return root;
    }

    /**
     * Builds the chat-notification component for a player ban.
     */
    public static Component buildChatNotification(UserBanListEntry entry, String name) {
        BanConfig cfg = CustomBansMod.CONFIG;

        String dateStr    = formatDate(entry.getCreated(), cfg.dateFormat.get());
        String expiresStr = formatExpiry(entry.getExpires(), cfg);
        String durationStr = formatRemainingDuration(entry.getExpires(), cfg);
        String reason     = entry.getReason() != null ? entry.getReason() : "Banned by an operator.";
        String source     = entry.getSource() != null ? entry.getSource() : "Console";

        List<String> filled = cfg.chatNotificationLines.get().stream()
                .map(line -> applyPlaceholders(line, name, source, reason, dateStr, expiresStr, durationStr))
                .toList();

        return TextUtil.joinLines(filled);
    }

    // ── IP ban ────────────────────────────────────────────────────────────────

    /**
     * Builds the full ban-screen component for an IP ban entry.
     */
    public static Component buildIpBanScreen(IpBanListEntry entry, String ip) {
        BanConfig cfg = CustomBansMod.CONFIG;

        String dateStr    = formatDate(entry.getCreated(), cfg.dateFormat.get());
        String expiresStr = formatExpiry(entry.getExpires(), cfg);
        String durationStr = formatRemainingDuration(entry.getExpires(), cfg);
        String reason     = entry.getReason() != null ? entry.getReason() : "Banned by an operator.";
        String source     = entry.getSource() != null ? entry.getSource() : "Console";

        String title = applyPlaceholders(cfg.banScreenTitle.get(),
                ip, source, reason, dateStr, expiresStr, durationStr);

        List<String> filled = cfg.banScreenLines.get().stream()
                .map(line -> applyPlaceholders(line, ip, source, reason, dateStr, expiresStr, durationStr))
                .toList();

        net.minecraft.network.chat.MutableComponent root = Component.empty()
                .append(TextUtil.parse(title))
                .append(Component.literal("\n"))
                .append(TextUtil.joinLines(filled));

        String imageName = CustomBansMod.CONFIG.banScreenImage.get().trim();
        if (!imageName.isEmpty()) {
            root.withStyle(s -> s.withInsertion("CUSTOMBANS_IMAGE:" + imageName));
        }

        return root;
    }

    /**
     * Builds the chat-notification component for an IP ban.
     */
    public static Component buildIpBanChatNotification(IpBanListEntry entry, String ip) {
        BanConfig cfg = CustomBansMod.CONFIG;

        String dateStr    = formatDate(entry.getCreated(), cfg.dateFormat.get());
        String expiresStr = formatExpiry(entry.getExpires(), cfg);
        String durationStr = formatRemainingDuration(entry.getExpires(), cfg);
        String reason     = entry.getReason() != null ? entry.getReason() : "Banned by an operator.";
        String source     = entry.getSource() != null ? entry.getSource() : "Console";

        List<String> filled = cfg.chatNotificationLines.get().stream()
                .map(line -> applyPlaceholders(line, ip, source, reason, dateStr, expiresStr, durationStr))
                .toList();

        return TextUtil.joinLines(filled);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Replaces all supported placeholders in a template string.
     *
     * @param template   raw config string (may contain color codes + placeholders)
     * @param player     player name or IP
     * @param source     banning admin
     * @param reason     ban reason
     * @param date       formatted ban creation date
     * @param expires    formatted expiry string
     * @param duration   remaining time string
     */
    private static String applyPlaceholders(String template,
                                            String player, String source, String reason,
                                            String date, String expires, String duration) {
        return template
                .replace("{player}",   player)
                .replace("{source}",   source)
                .replace("{reason}",   reason)
                .replace("{date}",     date)
                .replace("{expires}",  expires)
                .replace("{duration}", duration)
                .replace("{newline}",  "\n");
    }

    private static String formatDate(Date date, String pattern) {
        if (date == null) return "Unknown";
        try {
            return new SimpleDateFormat(pattern).format(date);
        } catch (Exception e) {
            return date.toString();
        }
    }

    private static String formatExpiry(Date expires, BanConfig cfg) {
        if (expires == null) {
            return TextUtil.stripCodes(cfg.permanentText.get());
        }
        return formatDate(expires, cfg.dateFormat.get());
    }

    private static String formatRemainingDuration(Date expires, BanConfig cfg) {
        if (expires == null) {
            return TextUtil.stripCodes(cfg.permanentDurationText.get());
        }
        long remaining = expires.getTime() - System.currentTimeMillis();
        if (remaining <= 0) return "0s";
        return TimeUtil.formatRemaining(expires.getTime(), cfg.durationSeparator.get(), "");
    }
}
