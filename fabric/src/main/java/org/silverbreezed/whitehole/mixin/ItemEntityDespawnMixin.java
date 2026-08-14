package org.silverbreezed.whitehole.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import org.silverbreezed.whitehole.event.capture.DespawnCaptureTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric-only counterpart to NeoForgeDespawnListener#onItemExpire. NeoForge exposes a
 * cancelable ItemExpireEvent for this; Fabric has no equivalent, so this intercepts the
 * actual discard() call inside ItemEntity's own tick instead.
 **
**/
@Mixin(ItemEntity.class)
public class ItemEntityDespawnMixin {

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"),
            cancellable = true,
            require = 1
    )
    private void whitehole$onExpire(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!(self.level() instanceof ServerLevel serverLevel)) return;

        if (DespawnCaptureTrigger.tryIntercept(serverLevel, self)) {
            // We've safely stored the items ourselves - stop vanilla's own discard() call
            // (which sits right after this injection point) from running a second time.
            self.discard();
            ci.cancel();
        }
    }
}