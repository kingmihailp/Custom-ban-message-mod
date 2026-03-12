package com.example.custombans.command;

import com.example.custombans.CustomBansMod;
import com.example.custombans.util.BanFormatter;
import com.example.custombans.util.TextUtil;
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
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.time.Duration;
import java.util.Collection;
import java.util.Date;

/**
 * Replaces (overrides) the vanilla {@code /ban} command with an enhanced version
 * that supports timed bans and custom ban-screen / chat-notification messages.
 *
 * <h3>Syntax</h3>
 * <pre>
 *   /ban &lt;player&gt;                       — permanent ban, no reason
 *   /ban &lt;player&gt; &lt;reason&gt;              — permanent ban with reason
 *   /ban &lt;player&gt; &lt;time&gt; &lt;reason&gt;       — temporary ban (time first)
 *   /ban &lt;player&gt; &lt;time&gt;               — temporary ban, no reason
 * </pre>
 *
 * <h3>Time format</h3>
 * <p>Time is the first word of the trailing arguments when it matches the
 * duration pattern, e.g. {@code 30s}, {@code 5m}, {@code 2h30m}, {@code 7d},
 * {@code 1mo}, {@code 1y2mo3d}.
 *
 * <p>If the first word does NOT match the pattern, the entire trailing string
 * is used as the ban reason (permanent ban).
 */
public final class BanCommand {

    private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED =
            new SimpleCommandExceptionType(Component.literal("That player is already banned."));

    private BanCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        int permLevel = CustomBansMod.CONFIG.banPermissionLevel.get();

        /*
         * We override Brigadier's /ban node. NeoForge calls RegisterCommandsEvent
         * after vanilla has already registered its commands, so we re-register the
         * literal "ban" — Brigadier merges nodes and the execution logic we provide
         * takes priority over the argument paths we define (greedy string swallows
         * the vanilla branch).
         */
        dispatcher.register(
            Commands.literal("ban")
                .requires(src -> src.hasPermission(permLevel))
                // /ban <targets>  — permanent, no reason
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .executes(ctx -> execute(ctx, ""))
                    // /ban <targets> <args>  — parse time + reason from <args>
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
        Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "targets");

        // Parse time and reason from the trailing arguments
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

        UserBanList banList = source.getServer().getPlayerList().getBans();
        String sourceName = source.getTextName();
        int count = 0;

        for (GameProfile profile : targets) {
            if (banList.isBanned(profile)) {
                throw ERROR_ALREADY_BANNED.create();
            }

            Date expires = (duration != null)
                    ? new Date(System.currentTimeMillis() + duration.toMillis())
                    : null; // null = permanent

            UserBanListEntry entry = new UserBanListEntry(
                    profile,
                    new Date(),     // created
                    sourceName,     // source
                    expires,        // expires
                    reason          // reason
            );
            banList.add(entry);

            // Kick the player if currently online
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(profile.getId());
            if (online != null) {
                Component kickMsg = BanFormatter.buildBanScreen(entry, profile.getName());
                online.connection.disconnect(kickMsg);
            }

            // Broadcast chat notification
            if (CustomBansMod.CONFIG.chatNotificationEnabled.get()) {
                Component notification = BanFormatter.buildChatNotification(entry, profile.getName());
                source.getServer().getPlayerList().broadcastSystemMessage(notification, false);
            }

            // Feedback to command sender
            String timeInfo = (duration != null)
                    ? " for " + TimeUtil.formatDuration(duration, " ")
                    : " permanently";
            source.sendSuccess(
                    () -> Component.literal("Banned " + profile.getName() + timeInfo + ". Reason: " + reason),
                    true
            );

            count++;
        }
        return count;
    }
}
