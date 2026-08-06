package org.silverbreezed.whitehole.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.silverbreezed.whitehole.event.VoidDeathHandler;

import java.util.List;

public class WhiteHoleAltarBlock extends Block {

    public WhiteHoleAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        ItemStack heldItem = player.getMainHandItem();

        // Memeriksa apakah ritual dipicu menggunakan item pengorbanan (Echo Shard)
        if (heldItem.is(Items.ECHO_SHARD)) {

            // Logika Sisi Server (Logika Data Game)
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {

                // 1. Memeriksa apakah pemain ini punya riwayat barang hilang di Void
                if (VoidDeathHandler.hasSavedItems(player.getUUID())) {

                    // Ambil daftar item dan hapus dari memori server agar tidak bisa diduplikasi
                    List<ItemStack> savedItems = VoidDeathHandler.getAndClearSavedItems(player.getUUID());

                    if (savedItems != null) {
                        double spawnX = pos.getX() + 0.5;
                        double spawnY = pos.getY() + 1.2; // Dimuntahkan sedikit di atas altar
                        double spawnZ = pos.getZ() + 0.5;

                        // 2. Muntahkan semua item kembali ke dunia nyata sebagai drop entitas
                        for (ItemStack stack : savedItems) {
                            ItemEntity itemEntity = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, stack);

                            // Berikan sedikit efek dorongan acak ke atas agar terlihat seperti memancar keluar
                            itemEntity.setDeltaMovement(
                                    (serverLevel.getRandom().nextDouble() - 0.5) * 0.2,
                                    0.3,
                                    (serverLevel.getRandom().nextDouble() - 0.5) * 0.2
                            );
                            serverLevel.addFreshEntity(itemEntity);
                        }

                        // Konsumsi 1 Echo Shard dari tangan pemain (Kecuali Creative Mode)
                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }

                        System.out.println("§6[White Hole] §aMateri ruang waktu berhasil direkonstruksi!");
                    }
                } else {
                    System.out.println("§6[White Hole] §cAltar tetap tenang. Tidak ada jalinan jiwa Anda yang tertinggal di Void.");
                }
            }

            // Logika Sisi Client (Visual Efek Tanpa Lag)
            else if (level.isClientSide()) {
                double x = pos.getX() + 0.5;
                double y = pos.getY() + 1.2;
                double z = pos.getZ() + 0.5;

                // Memunculkan badai partikel kosmik saat ritual berhasil
                for (int i = 0; i < 50; i++) {
                    level.addParticle(ParticleTypes.PORTAL, x, y, z,
                            (level.getRandom().nextDouble() - 0.5) * 0.8,
                            level.getRandom().nextDouble() * 0.6,
                            (level.getRandom().nextDouble() - 0.5) * 0.8
                    );
                }

                // Suara kosmik Warden menggema di area altar
                level.playLocalSound(x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.0F, 1.1F, false);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
