package org.silverbreezed.whitehole.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.silverbreezed.whitehole.event.VoidDeathHandler;
import org.silverbreezed.whitehole.item.ModItems;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class WhiteHoleAltarBlock extends Block {

    // 1. Membuat properti status AKTIF (bawaan vanilla Mojang)
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    private static UUID lastPlacerUUID = null;

    // Kamus data untuk mencatat waktu cooldown pemain (1 menit = 1200 tick game)
    private static final HashMap<UUID, Long> ALTAR_COOLDOWN = new HashMap<>();

    public WhiteHoleAltarBlock(Properties properties) {
        super(properties);
        // Default saat ditaruh atau ditemukan di dunia adalah MATI / RUSAK
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ACTIVE, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NonNull InteractionResult useWithoutItem(BlockState state, Level level, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hitResult) {
        ItemStack heldItem = player.getMainHandItem();
        boolean hasEye = state.getValue(ACTIVE);
        UUID playerUUID = player.getUUID();
        long gameTime = level.getGameTime();

        // 1. CEK COOLDOWN 1 MENIT
        if (ALTAR_COOLDOWN.containsKey(playerUUID)) {
            long timePassed = gameTime - ALTAR_COOLDOWN.get(playerUUID);
            if (timePassed < 1200) {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("§6[White Hole] §cAltar sedang menstabilkan energi fusi. Tunggu §e" + ((1200 - timePassed) / 20) + " §cdetik."));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // --- RITUAL DIMULAI: PEMASANGAN MATA KOSMIK ---
        if (!hasEye) {
            if (heldItem.is(ModItems.COSMIC_EYE)) {

                // Nyalakan mata di pilar secara visual (LIT = true)
                level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }

                if (!level.isClientSide()) {
                    lastPlacerUUID = playerUUID; // Kunci identitas pemain
                    player.sendSystemMessage(Component.literal("§5[White Hole] §dMata Kosmik terpasang. Membuka gerbang singularitas hampa..."));
                    level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.8F, 1.10F);
                    level.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 0.8F);
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 1.10F);

                    // Jadwalkan waktu tunggu otomatis selama 3 detik (60 ticks)
                    level.scheduleTick(pos, this, 60);
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("§7Pilar Altar ini memiliki rongga kosong berbentuk mata. Carilah Cosmic Eye untuk memulai ritual."));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * JEDA WAKTU 3 DETIK HABIS: ALUR MUNTAH OTOMATIS DIEKSEKUSI DI SINI
     */
    @Override
    protected void tick(@NonNull BlockState state, @NonNull ServerLevel serverLevel, @NonNull BlockPos pos, @NonNull RandomSource random) {
        super.tick(state, serverLevel, pos, random);

        if (lastPlacerUUID == null) return;
        Player player = serverLevel.getPlayerByUUID(lastPlacerUUID);
        long gameTime = serverLevel.getGameTime();

        double spawnX = pos.getX() + 0.5;
        double spawnY = pos.getY() + 1.2;
        double spawnZ = pos.getZ() + 0.5;

        // --- SKENARIO A: BARANG ADA DI VOID (MUNTAHKAN BARANG SURVIVAL!) ---
        if (VoidDeathHandler.hasSavedItems(serverLevel, lastPlacerUUID)) {
            List<ItemStack> savedItems = VoidDeathHandler.getAndClearSavedItems(serverLevel, lastPlacerUUID);

            if (savedItems != null) {
                // Semburkan semua zirah dan senjata melayang ke atas
                for (ItemStack stack : savedItems) {
                    ItemEntity itemEntity = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, stack);
                    itemEntity.setDeltaMovement((random.nextDouble() - 0.5) * 0.2, 0.35, (random.nextDouble() - 0.5) * 0.2);
                    serverLevel.addFreshEntity(itemEntity);
                }

                // Matikan kembali kelopak mata pilar (Mata hancur melebur jadi energi fusi)
                serverLevel.setBlock(pos, state.setValue(ACTIVE, false), 3);

                // Aktifkan cooldown 1 menit agar tidak bisa dispam
                ALTAR_COOLDOWN.put(lastPlacerUUID, gameTime);

                // Audio ledakan kosmik sip
                serverLevel.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.0F, 1.1F);

                if (player != null) {
                    player.sendSystemMessage(Component.literal("§6[White Hole] §aSingularitas pecah! Seluruh materi Anda berhasil direkonstruksi!"));
                }
            }
        }
        // --- SKENARIO B: BARANG KOSONG (MUNTAHKAN KEMBALI MATANYA!) ---
        else {
            // Matikan kembali kelopak mata pilar menjadi terpejam
            serverLevel.setBlock(pos, state.setValue(ACTIVE, false), 3);

            // Melempar kembali item Cosmic Eye fisik ke lantai
            ItemEntity eyeDrop = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, new ItemStack(ModItems.COSMIC_EYE));
            eyeDrop.setDeltaMovement((random.nextDouble() - 0.5) * 0.1, 0.2, (random.nextDouble() - 0.5) * 0.1);
            serverLevel.addFreshEntity(eyeDrop);

            // Audio penolakan energi tersendat
            serverLevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2F, 1.0F);

            if (player != null) {
                player.sendSystemMessage(Component.literal("§6[White Hole] §cGerbang menolak masuk! Tidak ada jalinan jiwa Anda yang tertinggal di dasar Void."));
            }
        }

        lastPlacerUUID = null; // Riset pelacak pemain untuk ritual berikutnya
    }

    @Override
    protected float getShadeBrightness(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos) {
        return 1.0F; // Mencegah bayangan hitam aneh di bawah celah Altar
    }

    // 1. Daftarkan properti arah hadap horizontal (North, South, East, West)
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // 2. Kode ajaib agar blok otomatis menghadap ke pemain saat ditaruh di survival
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // 3. Wajib daftarkan FACING ke dalam sistem mesin state Minecraft
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
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

        boolean isEyePlaced = state.getValue(ACTIVE);

        // Jika mata sudah terpasang (isEyePlaced = true), barulah badai partikel kosmik di atas mangkuk menyala berputar!
        if (isEyePlaced) {
            // ... kode perulangan partikel REVERSE_PORTAL dan FIREWORK Anda kemarin ...
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
}
