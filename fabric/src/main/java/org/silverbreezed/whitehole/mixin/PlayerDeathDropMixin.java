package org.silverbreezed.whitehole.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.silverbreezed.whitehole.event.capture.DespawnCaptureTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric-only counterpart to NeoForgeDespawnListener#onDeathDrops. NeoForge gets a proper
 * LivingDropsEvent for this with the ItemEntity list ready-made; Fabric has no equivalent
 * event, so this tags each item as it's individually dropped instead.
 *
**/
@Mixin(LivingEntity.class)
public class PlayerDeathDropMixin {

    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"),
            require = 1
    )
    private void whitehole$onDrop(ItemStack itemStack, boolean randomly, boolean thrownFromHand,
                                  CallbackInfoReturnable<ItemEntity> cir) {
        Player self = (Player) (Object) this;

        // Only death drops matter here - a live player pressing Q also calls this method,
        // but isDeadOrDying() is only true once death-loot dropping is underway.
        if (!(self instanceof ServerPlayer serverPlayer) || !self.isDeadOrDying()) return;
        if (!DespawnCaptureTrigger.isActivelyDying(serverPlayer.getUUID())) return;

        ItemEntity result = cir.getReturnValue();
        if (result != null) {
            DespawnCaptureTrigger.markOwned(result, serverPlayer.getUUID());
        }
    }
}