package org.silverbreezed.whitehole.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.silverbreezed.whitehole.event.VoidDeathHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onPlayerTakeDamageServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player) {
            if (player.getHealth() - amount <= 0.0F) {
                boolean secured = VoidDeathHandler.handlePlayerVoidDeath(player, source);

                if (secured) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
