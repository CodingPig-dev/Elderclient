package me.eldergodtactics.elderclient.mixin;

import me.eldergodtactics.elderclient.CapeManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void injectCape(CallbackInfoReturnable<PlayerSkin> cir) {
        PlayerSkin original = cir.getReturnValue();
        if (CapeManager.capeTextureLocation != null && original != null) {
            PlayerSkin custom = new PlayerSkin(
                    original.texture(),
                    original.textureUrl(),
                    CapeManager.capeTextureLocation,
                    original.elytraTexture(),
                    original.model(),
                    original.secure()
            );
            cir.setReturnValue(custom);
        }
    }
}
