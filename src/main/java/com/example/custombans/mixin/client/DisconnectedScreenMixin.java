package com.example.custombans.mixin.client;

import com.example.custombans.CustomBansMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-side mixin for {@link DisconnectedScreen}.
 *
 * Reads the image filename embedded by the server in the disconnect component's
 * {@code insertion} style field (marker: {@code "CUSTOMBANS_IMAGE:<filename>"}),
 * loads the PNG from {@code config/custombans/assets/<filename>} on the client
 * machine, and renders it above the screen title.
 *
 * This class is in the "client" mixin array and is never applied on dedicated servers.
 */
@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {

    @Shadow @Final private Component reason;

    /** Registered texture location, or {@code null} when no image is active. */
    @Unique private ResourceLocation custombans$texture;
    @Unique private int custombans$texW;
    @Unique private int custombans$texH;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Inject(method = "init", at = @At("TAIL"))
    private void custombans$init(CallbackInfo ci) {
        // Release any previously loaded texture (e.g. screen re-init).
        custombans$releaseTexture();

        String filename = custombans$findImageMarker(this.reason);
        if (filename == null || filename.isBlank()) return;

        Path assetsDir = FMLPaths.CONFIGDIR.get()
                .resolve("custombans")
                .resolve("assets");

        // Auto-create the assets folder so users know where to put images.
        try {
            Files.createDirectories(assetsDir);
        } catch (Exception ignored) {}

        Path imageFile = assetsDir.resolve(filename);
        if (!Files.isRegularFile(imageFile)) {
            CustomBansMod.LOGGER.warn("[CustomBans] Ban screen image not found: {}", imageFile);
            return;
        }

        try (InputStream is = Files.newInputStream(imageFile)) {
            NativeImage img = NativeImage.read(is);
            DynamicTexture tex = new DynamicTexture(img);
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    CustomBansMod.MOD_ID, "dynamic/ban_screen_image");
            Minecraft.getInstance().getTextureManager().register(loc, tex);
            custombans$texture = loc;
            custombans$texW = img.getWidth();
            custombans$texH = img.getHeight();
        } catch (Exception e) {
            CustomBansMod.LOGGER.warn("[CustomBans] Failed to load ban screen image '{}': {}",
                    filename, e.getMessage());
        }
    }

    /**
     * Renders the ban image after the screen background is drawn but before
     * the title / reason text is drawn, so the text appears on top.
     */
    @Inject(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            shift = At.Shift.AFTER
    ))
    private void custombans$render(GuiGraphics graphics, int mouseX, int mouseY,
                                   float delta, CallbackInfo ci) {
        if (custombans$texture == null) return;

        // Scale proportionally to fit within (screen_width - 20) × 60 px.
        int maxW = this.width - 20;
        int maxH = 60;
        float scale = Math.min((float) maxW / custombans$texW, (float) maxH / custombans$texH);
        int renderW = Math.max(1, (int) (custombans$texW * scale));
        int renderH = Math.max(1, (int) (custombans$texH * scale));

        int x = (this.width - renderW) / 2;
        // Render at the very top of the screen (y=5).  The title text appears at
        // y=30, so images up to ~24 px tall will sit cleanly above it; larger
        // images will overlap the title, which is acceptable for banner graphics.
        int y = 5;

        graphics.blit(custombans$texture, x, y, 0.0f, 0.0f,
                renderW, renderH, custombans$texW, custombans$texH);
    }

    /** Free the GPU texture when the screen is removed from the stack. */
    @Inject(method = "removed", at = @At("TAIL"))
    private void custombans$removed(CallbackInfo ci) {
        custombans$releaseTexture();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @Unique
    private void custombans$releaseTexture() {
        if (custombans$texture != null) {
            Minecraft.getInstance().getTextureManager().release(custombans$texture);
            custombans$texture = null;
        }
    }

    /**
     * Recursively searches {@code component} and its siblings for the
     * {@code CUSTOMBANS_IMAGE:<filename>} marker stored in the {@code insertion}
     * style field.  Returns the filename, or {@code null} if not found.
     */
    @Unique
    private static String custombans$findImageMarker(Component component) {
        if (component == null) return null;
        String ins = component.getStyle().getInsertion();
        if (ins != null && ins.startsWith("CUSTOMBANS_IMAGE:")) {
            return ins.substring("CUSTOMBANS_IMAGE:".length()).trim();
        }
        for (Component sibling : component.getSiblings()) {
            String result = custombans$findImageMarker(sibling);
            if (result != null) return result;
        }
        return null;
    }
}
