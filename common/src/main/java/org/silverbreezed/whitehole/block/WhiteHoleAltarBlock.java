package org.silverbreezed.whitehole.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.silverbreezed.whitehole.event.VoidDeathHandler;

import java.util.List;

public class WhiteHoleAltarBlock extends Block {

    // 1. Membuat properti status AKTIF (bawaan vanilla Mojang)
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public WhiteHoleAltarBlock(Properties properties) {
        super(properties);
        // Default saat ditaruh atau ditemukan di dunia adalah MATI / RUSAK
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected float getShadeBrightness(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos) {
        return 1.0F; // Mencegah bayangan hitam aneh di bawah celah Altar
    }

    @Override
    protected @NonNull VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos, @NonNull CollisionContext context) {
        VoxelShape base = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 3.0D, 16.0D);
        VoxelShape pillar = Block.box(3.0D, 3.0D, 3.0D, 13.0D, 12.0D, 13.0D);
        VoxelShape top = Block.box(0.0D, 12.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        return Shapes.or(base, Shapes.or(pillar, top));
    }

    @Override
    public void animateTick(BlockState state, @NonNull Level level, BlockPos pos, net.minecraft.util.@NonNull RandomSource random) {
        boolean isActive = state.getValue(ACTIVE);

        // Menentukan titik pusat tepat di atas mangkuk pedestal Altar (Y + 1.1)
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.1;
        double z = pos.getZ() + 0.5;

        // --- SITUASI 1: JIKA ALTAR MASIH RUSAK (Belum diberi Nether Star) ---
        if (!isActive) {
            // Hanya memunculkan asap hitam tipis sesekali sebagai tanda altar tertidur
            if (random.nextInt(8) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0D, 0.01D, 0.0D);
            }
        }

        // --- SITUASI 2: JIKA ALTAR SUDAH AKTIF (BADAI PUSARAN WHITE HOLE!) ---
        else {
            // Membuat pusaran bintang melingkar (*Cosmic Vortex Orbit*)
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * 2.0 * Math.PI;
                double radius = 0.25 + (random.nextDouble() * 0.2); // Jari-jari lingkaran pusaran

                // Menghitung koordinat partikel di sekeliling pusat altar
                double particleX = x + Math.cos(angle) * radius;
                double particleZ = z + Math.sin(angle) * radius;

                // Menggunakan REVERSE_PORTAL agar partikel bergerak memancar keluar (Konsep Lubang Putih)
                level.addParticle(ParticleTypes.REVERSE_PORTAL, particleX, y, particleZ,
                        (x - particleX) * -0.08, // Dorongan keluar horizontal
                        0.015D,                  // Melayang ke atas secara perlahan
                        (z - particleZ) * -0.08
                );
            }

            // Sesekali memunculkan kilatan bintang putih neon terang di pusat lubang
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.FIREWORK, x, y + 0.05, z,
                        (random.nextDouble() - 0.5) * 0.03,
                        0.01D,
                        (random.nextDouble() - 0.5) * 0.03
                );
            }
        }
    }



    @Override
    protected @NonNull InteractionResult useWithoutItem(BlockState state, @NonNull Level level, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hitResult) {
        ItemStack heldItem = player.getMainHandItem();
        boolean isActive = state.getValue(ACTIVE);

        // --- FASE 1: JIKA ALTAR MASIH RUSAK (TANTANGAN RESTORASI) ---
        if (!isActive) {
            // Pemain harus mengorbankan NETHER STAR untuk membangkitkan Altar Kuno ini
            if (heldItem.is(Items.NETHER_STAR)) {
                if (!level.isClientSide()) {
                    // Ubah status blok menjadi AKTIF secara permanen di server
                    level.setBlock(pos, state.setValue(ACTIVE, true), 3);

                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    player.sendSystemMessage(Component.literal("§5[White Hole] §dEnergi inti bintang menghidupkan kembali Altar Kuno yang mati!"));
                } else {
                    // Efek visual ledakan kebangkitan di sisi Client
                    level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 2.0F, 1.10F, false);
                    level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F, false);

                    for (int i = 0; i < 100; i++) {
                        level.addParticle(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.1, 0);
                    }
                }
                return InteractionResult.SUCCESS;
            } else {
                // Pesan jika diklik biasa saat masih rusak
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("§7Altar Kosmik ini telah hancur dan tertidur. Dibutuhkan inti energi murni (Nether Star) untuk merestorasinya."));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // --- FASE 2: JIKA ALTAR SUDAH AKTIF (FUNGSIONALITAS UTAMA) ---
        if (heldItem.is(Items.ECHO_SHARD)) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                double spawnX = pos.getX() + 0.5;
                double spawnY = pos.getY() + 1.2;
                double spawnZ = pos.getZ() + 0.5;

                if (VoidDeathHandler.hasSavedItems(player.getUUID())) {
                    List<ItemStack> savedItems = VoidDeathHandler.getAndClearSavedItems(player.getUUID());
                    if (savedItems != null) {
                        for (ItemStack stack : savedItems) {
                            ItemEntity itemEntity = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, stack);
                            itemEntity.setDeltaMovement((serverLevel.getRandom().nextDouble() - 0.5) * 0.2, 0.3, (serverLevel.getRandom().nextDouble() - 0.5) * 0.2);
                            serverLevel.addFreshEntity(itemEntity);
                        }

                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }

                        level.setBlock(pos, state.setValue(ACTIVE, false), 3);
                        player.sendSystemMessage(Component.literal("§6[White Hole] §aMateri ruang waktu berhasil direkonstruksi!"));
                    }
                } else {
                    ItemEntity returnedStar = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, Items.NETHER_STAR.getDefaultInstance());

                    returnedStar.setDeltaMovement((serverLevel.getRandom().nextDouble() - 0.5) * 0.2, 0.3, (serverLevel.getRandom().nextDouble() - 0.5) * 0.2);

                    serverLevel.addFreshEntity(returnedStar);

                    level.setBlock(pos, state.setValue(ACTIVE, false), 3);

                    player.sendSystemMessage(Component.literal("§6[White Hole] §cAltar tetap tenang. Tidak ada jalinan jiwa Anda yang tertinggal di Void."));
                }
            } else if (level.isClientSide()) {
                // Efek partikel pemulihan item biasa
                for (int i = 0; i < 50; i++) {
                    level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            (level.getRandom().nextDouble() - 0.5) * 0.8, level.getRandom().nextDouble() * 0.6, (level.getRandom().nextDouble() - 0.5) * 0.8);
                }
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.0F, 1.1F, false);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // 2. Wajib memberi tahu Minecraft untuk mendaftarkan properti ACTIVE ke dalam state engine game
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }
}
