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
 *
 * *** VERIFY BEFORE COMPILING ***
 * This targets the `discard()` invocation inside `ItemEntity#tick()` - discard() itself is
 * a long-stable, public Entity API method, but I cannot confirm tick() only calls it once
 * (for expiry) in 26.2; if merging with a nearby stack or another cleanup path also calls
 * discard() in the same method, you'll need to narrow this with a @Slice or an ordinal to
 * only match the expiry call. Check your deobfuscated ItemEntity#tick() source and adjust
 * before relying on this.
 */
@Mixin(ItemEntity.class)
public class ItemEntityDespawnMixin {

    @Inject(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"),
            cancellable = true
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