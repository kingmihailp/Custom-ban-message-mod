package com.example.custombans.command;

import com.example.custombans.CustomBansMod;
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
import net.minecraft.server.players.UserBanList;

import java.util.Collection;

/**
 * Provides {@code /unban} (alias for vanilla {@code /pardon}) with feedback
 * messages styled to match the rest of the mod.
 */
public final class UnbanCommand {

    private static final SimpleCommandExceptionType ERROR_NOT_BANNED =
            new SimpleCommandExceptionType(Component.literal("That player is not banned."));

    private UnbanCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Read config lazily inside requires() — SERVER config is not yet loaded at registration time.
        dispatcher.register(
            Commands.literal("unban")
                .requires(src -> src.hasPermission(CustomBansMod.CONFIG.banPermissionLevel.get()))
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .executes(UnbanCommand::execute)
                )
        );

        // Also keep vanilla /pardon but with our permission level
        dispatcher.register(
            Commands.literal("pardon")
                .requires(src -> src.hasPermission(CustomBansMod.CONFIG.banPermissionLevel.get()))
                .then(Commands.argument("targets", GameProfileArgument.gameProfile())
                    .executes(UnbanCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {

        CommandSourceStack source = ctx.getSource();
        Collection<GameProfile> targets = GameProfileArgument.getGameProfiles(ctx, "targets");
        UserBanList banList = source.getServer().getPlayerList().getBans();
        int count = 0;

        for (GameProfile profile : targets) {
            if (!banList.isBanned(profile)) {
                throw ERROR_NOT_BANNED.create();
            }
            banList.remove(profile);
            source.sendSuccess(
                    () -> Component.literal("Unbanned " + profile.getName() + "."),
                    true
            );
            count++;
        }
        return count;
    }
}
