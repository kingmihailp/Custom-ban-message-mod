package com.example.custombans;

import com.example.custombans.command.BanCommand;
import com.example.custombans.command.BanIpCommand;
import com.example.custombans.command.UnbanCommand;
import com.example.custombans.command.UnbanIpCommand;
import com.example.custombans.config.BanConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CustomBansMod.MOD_ID)
public class CustomBansMod {

    public static final String MOD_ID = "custombans";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Singleton config instance, accessible from anywhere in the mod. */
    public static BanConfig CONFIG;

    public CustomBansMod(IEventBus modEventBus, ModContainer modContainer) {
        CONFIG = new BanConfig();
        modContainer.registerConfig(ModConfig.Type.SERVER, CONFIG.SPEC, "custombans-server.toml");

        // Register game-level events (commands, etc.)
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("[CustomBans] Mod loaded. Ban screen and chat notifications are configurable in custombans-server.toml");
    }

    // ── Command Registration ──────────────────────────────────────────────────

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        BanCommand.register(event.getDispatcher());
        BanIpCommand.register(event.getDispatcher());
        UnbanCommand.register(event.getDispatcher());
        UnbanIpCommand.register(event.getDispatcher());
        LOGGER.info("[CustomBans] Registered /ban, /ban-ip, /unban, /unban-ip commands with time support.");
    }
}
