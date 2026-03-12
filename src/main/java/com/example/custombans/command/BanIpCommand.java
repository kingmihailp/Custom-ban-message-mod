package com.example.custombans.command;

import com.example.custombans.CustomBansMod;
import com.example.custombans.util.BanFormatter;
import com.example.custombans.util.TimeUtil;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;

/**
 * Replaces the vanilla {@code /ban-ip} command with an enhanced version that
 * supports timed IP bans and custom notifications.
 *
 * <h3>Syntax</h3>
 * <pre>
 *   /ban-ip &lt;player|ip&gt;                  — permanent IP ban, no reason
 *   /ban-ip &lt;player|ip&gt; &lt;reason&gt;         — permanent IP ban with reason
 *   /ban-ip &lt;player|ip&gt; &lt;time&gt; [reason]  — temporary IP ban
 * </pre>
 *
 * <p>Accepts either a player name (online or offline) or a raw IP address string
 * as the target. When a player name is given the IP is resolved from the current
 * or last-known connection.
 */
public final class BanIpCommand {

    private static final SimpleCommandExceptionType ERROR_INVALID_IP =
            new SimpleCommandExceptionType(Component.literal("Invalid IP address."));
    private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED =
            new SimpleCommandExceptionType(Component.literal("That IP is already banned."));

    private BanIpCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Read config lazily inside requires() — SERVER config is not yet loaded at registration time.
        dispatcher.register(
            Commands.literal("ban-ip")
                .requires(src -> src.hasPermission(CustomBansMod.CONFIG.banIpPermissionLevel.get()))
                // /ban-ip <target>
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(ctx -> execute(ctx, ""))
                    // /ban-ip <target> <args>
                    .then(Commands.argument("args", StringArgumentType.greedyString())
                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "args")))
                    )
                )
        );
    }

    // ── Execution ─────────────────────────────────────────────────────────────

    private static int execute(CommandContext<CommandSourceStack> ctx, String args)
            throws CommandSyntaxException {

        CommandSourceStack source = ctx.getSource();
        String target = StringArgumentType.getString(ctx, "target");

        // Resolve IP: either a raw address or a player name
        String ip = resolveIp(source, target);
        if (ip == null || !isValidIp(ip)) {
            throw ERROR_INVALID_IP.create();
        }

        // Parse time and reason
        Duration duration = null;
        String reason = "Banned by an operator.";

        if (!args.isBlank()) {
            String[] parts = args.split("\\s+", 2);
            if (TimeUtil.isTimeDuration(parts[0])) {
                duration = TimeUtil.parseDuration(parts[0]);
                reason = (parts.length > 1 && !parts[1].isBlank()) ? parts[1] : "Banned by an operator.";
            } else {
                reason = args;
            }
        }

        IpBanList banList = source.getServer().getPlayerList().getIpBans();

        if (banList.isBanned(ip)) {
            throw ERROR_ALREADY_BANNED.create();
        }

        Date expires = (duration != null)
                ? new Date(System.currentTimeMillis() + duration.toMillis())
                : null;

        IpBanListEntry entry = new IpBanListEntry(
                ip,
                new Date(),
                source.getTextName(),
                expires,
                reason
        );
        banList.add(entry);

        // Kick all online players with that IP
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            SocketAddress addr = player.connection.connection.getRemoteAddress();
            String playerIp = extractIp(addr);
            if (ip.equals(playerIp)) {
                Component kickMsg = BanFormatter.buildIpBanScreen(entry, ip);
                player.connection.disconnect(kickMsg);
            }
        }

        // Broadcast chat notification
        if (CustomBansMod.CONFIG.chatNotificationEnabled.get()
                && CustomBansMod.CONFIG.chatNotificationOnIpBan.get()) {
            Component notification = BanFormatter.buildIpBanChatNotification(entry, ip);
            source.getServer().getPlayerList().broadcastSystemMessage(notification, false);
        }

        String timeInfo = (duration != null)
                ? " for " + TimeUtil.formatDuration(duration, " ")
                : " permanently";
        final String finalTimeInfo = timeInfo;
        final String finalReason   = reason;
        source.sendSuccess(
                () -> Component.literal("IP-banned " + ip + finalTimeInfo + ". Reason: " + finalReason),
                true
        );
        return 1;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Resolves an IP string from either a raw IP input or an online player name.
     */
    private static String resolveIp(CommandSourceStack source, String target) {
        if (isValidIp(target)) return target;

        // Try to find a matching online player
        ServerPlayer player = source.getServer().getPlayerList().getPlayerByName(target);
        if (player != null) {
            return extractIp(player.connection.connection.getRemoteAddress());
        }
        return null;
    }

    private static String extractIp(SocketAddress addr) {
        if (addr instanceof InetSocketAddress inet) {
            InetAddress ia = inet.getAddress();
            return ia != null ? ia.getHostAddress() : inet.getHostString();
        }
        return addr.toString();
    }

    private static boolean isValidIp(String s) {
        // Accepts IPv4 and IPv6 patterns
        return s != null && s.matches(
                "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.){3}(25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)$"
                + "|^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$");
    }
}
