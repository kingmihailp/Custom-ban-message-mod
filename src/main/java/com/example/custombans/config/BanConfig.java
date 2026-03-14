package com.example.custombans.config;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class BanConfig {

    // ── Ban Screen ────────────────────────────────────────────────────────────

    /** Lines shown on the disconnect/ban screen. Placeholders and hex supported. */
    public final ModConfigSpec.ConfigValue<List<? extends String>> banScreenLines;

    /** Title line (first bold line) on the ban screen. */
    public final ModConfigSpec.ConfigValue<String> banScreenTitle;

    /**
     * Filename of the image to display at the top of the ban screen.
     * The file must be placed in {@code config/custombans/assets/<filename>}.
     * Leave empty to disable the image. Supports PNG files.
     */
    public final ModConfigSpec.ConfigValue<String> banScreenImage;

    // ── Chat Notification ─────────────────────────────────────────────────────

    /** Whether to broadcast a chat message when a player is banned. */
    public final ModConfigSpec.BooleanValue chatNotificationEnabled;

    /** Lines to broadcast in chat when a player is banned. */
    public final ModConfigSpec.ConfigValue<List<? extends String>> chatNotificationLines;

    /** Whether to broadcast chat notification on /ban-ip as well. */
    public final ModConfigSpec.BooleanValue chatNotificationOnIpBan;

    // ── Formatting ────────────────────────────────────────────────────────────

    /** Format string for the expiry date, used in {expires} placeholder. */
    public final ModConfigSpec.ConfigValue<String> dateFormat;

    /** Text shown in {expires} when the ban is permanent. */
    public final ModConfigSpec.ConfigValue<String> permanentText;

    /** Text shown in {duration} when the ban is permanent. */
    public final ModConfigSpec.ConfigValue<String> permanentDurationText;

    /** Separator between duration components, e.g. " " or ", ". */
    public final ModConfigSpec.ConfigValue<String> durationSeparator;

    // ── Permission Levels ─────────────────────────────────────────────────────

    /** Required permission level to use /ban. Vanilla default is 3. */
    public final ModConfigSpec.IntValue banPermissionLevel;

    /** Required permission level to use /ban-ip. Vanilla default is 3. */
    public final ModConfigSpec.IntValue banIpPermissionLevel;

    // ─────────────────────────────────────────────────────────────────────────

    public final ModConfigSpec SPEC;

    public BanConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("""
                ╔══════════════════════════════════════════════════════════╗
                ║              Custom Ban Messages — Config                ║
                ╠══════════════════════════════════════════════════════════╣
                ║  Supported placeholders:                                 ║
                ║    {player}   — banned player's name                     ║
                ║    {source}   — who issued the ban (or Console)          ║
                ║    {reason}   — ban reason                               ║
                ║    {date}     — date the ban was issued                  ║
                ║    {expires}  — expiry date (or permanent text)          ║
                ║    {duration} — remaining time (e.g. 2d 3h 15m)         ║
                ║    {newline}  — explicit line break                      ║
                ║                                                          ║
                ║  Color codes:                                            ║
                ║    &#RRGGBB  — hex color  e.g. &#FF5555                  ║
                ║    &0-9a-f   — standard Minecraft colors                 ║
                ║    &l &o &n &m &k — bold italic underline strike obf     ║
                ║    &r        — reset                                     ║
                ╚══════════════════════════════════════════════════════════╝
                """);

        builder.push("ban_screen");

        banScreenTitle = builder
                .comment("Title displayed at the top of the ban screen.")
                .define("title", "&#FF4444&lYou have been banned!");

        banScreenLines = builder
                .comment("Lines shown on the ban/disconnect screen. Each entry is one visual line.",
                         "You can use placeholders listed above and hex/legacy color codes.")
                .defineListAllowEmpty("lines", List.of(
                        "",
                        "&#FFAA00&lReason: &r&#FFFFFF{reason}",
                        "&#FFAA00&lBanned by: &r&#FFFFFF{source}",
                        "&#FFAA00&lBan date: &r&#FFFFFF{date}",
                        "&#FFAA00&lExpires: &r&#FFFFFF{expires}",
                        "&#FFAA00&lTime remaining: &r&#FFFFFF{duration}",
                        "",
                        "&#AAAAAA&oAppeal at: discord.gg/example"
                ), e -> e instanceof String);

        banScreenImage = builder
                .comment("Image file to show at the top of the ban screen.",
                         "Place the file in config/custombans/assets/<filename> on the client machine.",
                         "Supports PNG format. Leave empty to disable.",
                         "Example: ban_logo.png")
                .define("image", "");

        builder.pop();

        // ── Chat Notification ─────────────────────────────────────────────────
        builder.push("chat_notification");

        chatNotificationEnabled = builder
                .comment("If true, a message is broadcast in chat when /ban is used.")
                .define("enabled", true);

        chatNotificationOnIpBan = builder
                .comment("If true, also broadcast when /ban-ip is used.")
                .define("on_ip_ban", true);

        chatNotificationLines = builder
                .comment("Lines broadcast to all players when someone is banned.")
                .defineListAllowEmpty("lines", List.of(
                        "&#FF4444&l[BAN] &r&#FFFFFF{player} &7has been banned!",
                        "&#7F7F7F  Reason: &f{reason} &7| Expires: &f{expires}"
                ), e -> e instanceof String);

        builder.pop();

        // ── Formatting ────────────────────────────────────────────────────────
        builder.push("formatting");

        dateFormat = builder
                .comment("Java SimpleDateFormat pattern used for {date} and {expires} placeholders.")
                .define("date_format", "dd.MM.yyyy HH:mm");

        permanentText = builder
                .comment("Text shown in {expires} when the ban is permanent.")
                .define("permanent_text", "&#FF4444Never");

        permanentDurationText = builder
                .comment("Text shown in {duration} when the ban is permanent.")
                .define("permanent_duration_text", "&#FF4444Forever");

        durationSeparator = builder
                .comment("Separator between duration units, e.g. \" \", \", \".")
                .define("duration_separator", " ");

        builder.pop();

        // ── Permissions ───────────────────────────────────────────────────────
        builder.push("permissions");

        banPermissionLevel = builder
                .comment("Required permission level to use /ban (vanilla default: 3).")
                .defineInRange("ban_permission_level", 3, 0, 4);

        banIpPermissionLevel = builder
                .comment("Required permission level to use /ban-ip (vanilla default: 3).")
                .defineInRange("ban_ip_permission_level", 3, 0, 4);

        builder.pop();

        SPEC = builder.build();
    }
}
