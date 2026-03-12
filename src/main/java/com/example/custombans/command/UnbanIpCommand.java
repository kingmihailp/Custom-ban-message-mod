package com.example.custombans.command;

import com.example.custombans.CustomBansMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.IpBanList;

/**
 * Provides {@code /unban-ip} (alias for vanilla {@code /pardon-ip}).
 */
public final class UnbanIpCommand {

    private static final SimpleCommandExceptionType ERROR_NOT_BANNED =
            new SimpleCommandExceptionType(Component.literal("That IP is not banned."));

    private UnbanIpCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Read config lazily inside requires() — SERVER config is not yet loaded at registration time.
        dispatcher.register(
            Commands.literal("unban-ip")
                .requires(src -> src.hasPermission(CustomBansMod.CONFIG.banIpPermissionLevel.get()))
                .then(Commands.argument("ip", StringArgumentType.word())
                    .executes(UnbanIpCommand::execute)
                )
        );

        dispatcher.register(
            Commands.literal("pardon-ip")
                .requires(src -> src.hasPermission(CustomBansMod.CONFIG.banIpPermissionLevel.get()))
                .then(Commands.argument("ip", StringArgumentType.word())
                    .executes(UnbanIpCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {

        CommandSourceStack source = ctx.getSource();
        String ip = StringArgumentType.getString(ctx, "ip");
        IpBanList banList = source.getServer().getPlayerList().getIpBans();

        if (!banList.isBanned(ip)) {
            throw ERROR_NOT_BANNED.create();
        }

        banList.remove(ip);
        source.sendSuccess(() -> Component.literal("Unbanned IP: " + ip + "."), true);
        return 1;
    }
}
