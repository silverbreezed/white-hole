package org.silverbreezed.whitehole.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.silverbreezed.whitehole.manager.ItemSnapshotManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerLoginMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onPlayerJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        ItemSnapshotManager.loadPlayerDataAsync(player);
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerLeave(ServerPlayer player, CallbackInfo ci) {
        ItemSnapshotManager.unloadPlayerData(player);
    }
}