package com.example.guitarmod.client.integration;

import com.example.guitarmod.GuitarMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * playerAnimator（KosmX）可选集成桥。该类引用了 playerAnimator 的 API，
 * 只能在确认前置已安装（{@link #isAvailable()}）后触碰，否则 NoClassDefFoundError。
 */
public final class PlayerAnimatorBridge {

    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "guitar_play");
    private static final ResourceLocation ANIMATION_ID =
            ResourceLocation.fromNamespaceAndPath(GuitarMod.MODID, "guitar_play");

    private static Boolean available;
    private static boolean layerHookRegistered = false;
    private static boolean missingAnimLogged = false;

    private PlayerAnimatorBridge() {}

    public static boolean isAvailable() {
        if (available == null) {
            available = ModList.get().isLoaded("playeranimator");
            if (available) {
                registerLayerHook();
            } else {
                GuitarMod.LOGGER.info("[GuitarMod] playerAnimator not installed, strumming animation disabled");
            }
        }
        return available;
    }

    /** 给每个玩家挂一个动画层，供后续 setAnimation。 */
    private static void registerLayerHook() {
        if (layerHookRegistered) return;
        layerHookRegistered = true;
        try {
            PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, animationStack) -> {
                ModifierLayer<IAnimation> layer = new ModifierLayer<>();
                animationStack.addAnimLayer(1000, layer);
                PlayerAnimationAccess.getPlayerAssociatedData(player).set(LAYER_ID, layer);
            });
            GuitarMod.LOGGER.info("[GuitarMod] playerAnimator layer registered");
        } catch (Throwable t) {
            GuitarMod.LOGGER.warn("[GuitarMod] failed to hook playerAnimator", t);
            available = false;
        }
    }

    @SuppressWarnings("unchecked")
    public static void play(AbstractClientPlayer player) {
        try {
            IAnimation raw = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
            if (!(raw instanceof ModifierLayer)) return;
            ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) raw;

            KeyframeAnimation animation = PlayerAnimationRegistry.getAnimation(ANIMATION_ID) instanceof KeyframeAnimation kfa
                    ? kfa : null;
            if (animation == null) {
                if (!missingAnimLogged) {
                    missingAnimLogged = true;
                    GuitarMod.LOGGER.info("[GuitarMod] animation {} not found; add a player_animation/guitar_play.json resource",
                            ANIMATION_ID);
                }
                return;
            }
            layer.setAnimation(new KeyframeAnimationPlayer(animation));
        } catch (Throwable t) {
            GuitarMod.LOGGER.warn("[GuitarMod] play animation failed", t);
        }
    }

    @SuppressWarnings("unchecked")
    public static void stop(AbstractClientPlayer player) {
        try {
            IAnimation raw = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LAYER_ID);
            if (raw instanceof ModifierLayer) {
                ((ModifierLayer<IAnimation>) raw).setAnimation(null);
            }
        } catch (Throwable t) {
            GuitarMod.LOGGER.warn("[GuitarMod] stop animation failed", t);
        }
    }
}
