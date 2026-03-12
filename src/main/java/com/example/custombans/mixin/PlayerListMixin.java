package com.example.custombans.mixin;

import com.example.custombans.util.BanFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import com.mojang.authlib.GameProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.net.SocketAddress;

/**
 * Intercepts {@link PlayerList#canPlayerLogin(SocketAddress, GameProfile)} to
 * replace the vanilla ban disconnect message with the fully customized
 * multi-line, hex-colored message defined in the mod's config.
 *
 * <p>The vanilla method returns {@code null} when login is allowed and a
 * {@link Component} (the disconnect reason) when the player is blocked.
 * We only replace the component when it originates from a ban entry — not
 * from whitelist restrictions or other causes.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Shadow public abstract UserBanList getBans();
    @Shadow public abstract IpBanList getIpBans();

    /**
     * Called at the RETURN point of {@code canPlayerLogin}.
     *
     * <p>If the vanilla logic determined that the player is banned, we replace
     * the disconnect reason with our custom formatted component. For any other
     * reason (whitelist, etc.) we leave the component untouched.
     */
    @Inject(method = "canPlayerLogin", at = @At("RETURN"), cancellable = true)
    private void custombans$customizeBanScreen(SocketAddress socketAddress,
                                               GameProfile gameProfile,
                                               CallbackInfoReturnable<Component> cir) {
        Component original = cir.getReturnValue();
        if (original == null) return; // Login is allowed — nothing to do

        // Check if this is a player UUID ban
        UserBanList banList = getBans();
        if (banList.isBanned(gameProfile)) {
            UserBanListEntry entry = banList.get(gameProfile);
            if (entry != null) {
                Component custom = BanFormatter.buildBanScreen(entry, gameProfile.getName());
                cir.setReturnValue(custom);
                return;
            }
        }

        // Check if this is an IP ban
        //  socketAddress may be null in some offline/testing scenarios
        if (socketAddress != null) {
            String ip = extractIp(socketAddress);
            IpBanList ipBanList = getIpBans();
            if (ipBanList.isBanned(ip)) {
                IpBanListEntry entry = ipBanList.get(ip);
                if (entry != null) {
                    Component custom = BanFormatter.buildIpBanScreen(entry, ip);
                    cir.setReturnValue(custom);
                    return;
                }
            }
        }

        // Otherwise: whitelist rejection or other — leave the original component
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String extractIp(@Nullable SocketAddress addr) {
        if (addr == null) return "";
        if (addr instanceof java.net.InetSocketAddress inet) {
            java.net.InetAddress ia = inet.getAddress();
            return ia != null ? ia.getHostAddress() : inet.getHostString();
        }
        return addr.toString();
    }
}
