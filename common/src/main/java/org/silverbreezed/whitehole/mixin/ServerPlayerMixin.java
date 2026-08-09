package org.silverbreezed.whitehole.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.silverbreezed.whitehole.event.VoidDeathHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    // Target langsung ke ServerPlayer, bukan LivingEntity
    @Inject(method = "die", at = @At("HEAD"))
    private void onPlayerDie(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        // handlePlayerVoidDeath akan mengamankan barang dan langsung mengosongkan inventory
        boolean secured = VoidDeathHandler.handlePlayerVoidDeath(player, source);

        if (secured) {
            // Karena inventory sudah dikosongkan (clearContent), vanilla drop routine tidak akan
            // membuang apa pun ke void setelah baris ini terlewati.
            player.sendSystemMessage(Component.literal("§5[§lWhite Hole§r§5] §dYou died in the void. You can bring back your items using the White Hole Altar in an Ancient City."));
        }
    }
}