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

    // KUNCI UTAMA 26.2: Menyuntikkan pencegat tepat di kepala metode "hurtServer"
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onPlayerTakeDamageServer(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // Melakukan pengecekan apakah entitas yang diserang di server adalah pemain
        if ((Object) this instanceof ServerPlayer player) {

            // Memeriksa kondisi apakah damage mematikan akan membuat darah pemain habis
            if (player.getHealth() - amount <= 0.0F) {

                // Memicu fungsi penyelamat GSON mandiri dari folder Common
                boolean secured = VoidDeathHandler.handlePlayerVoidDeath(player, source);

                if (secured) {
                    // Jika sukses disimpan ke JSON, gagalkan kematian vanilla dan kembalikan nilai false
                    cir.setReturnValue(false);
                }
            }
        }
    }
}
