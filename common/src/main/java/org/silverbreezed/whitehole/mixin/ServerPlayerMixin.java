package org.silverbreezed.whitehole.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.silverbreezed.whitehole.event.capture.DespawnCaptureTrigger;
import org.silverbreezed.whitehole.event.capture.VoidDeathCaptureTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void onPlayerDie(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;

        boolean voidDeathsecured = VoidDeathCaptureTrigger.tryCapture(player, source);
        // Opens the eligibility window used by loaders whose death-drop hook can't see the
        // DamageSource directly (Fabric's PlayerDeathDropMixin). Harmless on NeoForge, which
        // gets the source for free from LivingDropsEvent and doesn't consult this.
        DespawnCaptureTrigger.beginDeath(player.getUUID());

        if (voidDeathsecured) {
            player.displayClientMessage(Component.literal("§5[§lWhite Hole§r§5] §dYou died in the void. You can bring back your items using the White Hole Altar in an Ancient City."), false);
        }

        if (!source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            player.displayClientMessage(Component.literal("§5[§lWhite Hole§r§5] §dYou died. If your items despawned, you can bring back your items using the White Hole Altar in an Ancient City."), false);
        }
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void onPlayerDieEnd(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        DespawnCaptureTrigger.endDeath(player.getUUID());
    }
}