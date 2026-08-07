package org.silverbreezed.whitehole.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        UUID playerUUID = player.getUUID();s
        long gameTime = level.getGameTime(); // Mengambil waktu internal dunia saat ini

        // --- SISTEM PENGECEKAN COOLDOWN 1 MENIT ---
        if (ALTAR_COOLDOWN.containsKey(playerUUID)) {
            long lastUsedTime = ALTAR_COOLDOWN.get(playerUUID);
            long timePassed = gameTime - lastUsedTime;

            if (timePassed < 1200) { // 1200 tick = 60 detik (1 menit)
                long secondsLeft = (1200 - timePassed) / 20;
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("§6[White Hole] §cAltar sedang mengumpulkan energi kosmik kembali. Tunggu §e" + secondsLeft + " §cdetik lagi."));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // --- TAHAP 1: MEMASANG MATA (JIKA PILAR MASIH KOSONG) ---
        if (!hasEye) {
            if (heldItem.is(ModItems.COSMIC_EYE)) {
                if (!level.isClientSide()) {
                    // Cek apakah pemain punya barang di Void SEBELUM mengonsumsi Mata
                    if (VoidDeathHandler.hasSavedItems(playerUUID)) {

                        // KONDISI A: BARANG ADA -> Mata sukses terpasang dan dikonsumsi!
                        level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }
                        player.sendSystemMessage(Component.literal("§5[White Hole] §dMata Kosmik terpasang pas pada pilar kuno..."));
                    } else {
                        // KONDISI B: BARANG KOSONG -> Mata TIDAK dikonsumsi, dikembalikan ke pemain!
                        player.sendSystemMessage(Component.literal("§6[White Hole] §cMata Kosmik menolak masuk. Jiwa Anda aman, tidak ada materi yang tertinggal di Void."));
                    }
                } else {
                    // Hanya bunyikan suara jika pemain memang punya barang di Void
                    if (VoidDeathHandler.hasSavedItems(playerUUID)) {
                        level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                    }
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("§7Pilar Altar ini memiliki rongga kosong berbentuk mata. Carilah Cosmic Eye untuk mengisinya."));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // --- TAHAP 2: MENYALAKAN KUNCI UTAMA (MATA SUDAH TERPASANG) ---
        if (heldItem.is(Items.ECHO_SHARD)) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {

                // Ambil daftar item dari memori Void
                List<ItemStack> savedItems = VoidDeathHandler.getAndClearSavedItems(playerUUID);

                if (savedItems != null) {
                    double spawnX = pos.getX() + 0.5;
                    double spawnY = pos.getY() + 1.2;
                    double spawnZ = pos.getZ() + 0.5;

                    // Muntahkan seluruh item dari Void ke atas Altar
                    for (ItemStack stack : savedItems) {
                        ItemEntity itemEntity = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, stack);
                        itemEntity.setDeltaMovement((serverLevel.getRandom().nextDouble() - 0.5) * 0.2, 0.3, (serverLevel.getRandom().nextDouble() - 0.5) * 0.2);
                        serverLevel.addFreshEntity(itemEntity);
                    }

                    // Konsumsi Echo Shard pemicu kunci
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }

                    // Kembalikan status Altar menjadi Kosong/Mati (LIT=false) agar Mata bisa dipasang lagi nanti
                    level.setBlock(pos, state.setValue(ACTIVE, false), 3);

                    // DAFTARKAN WAKTU COOLDOWN (Pemain harus menunggu 1 menit sebelum bisa memakai Altar lagi)
                    ALTAR_COOLDOWN.put(playerUUID, gameTime);

                    player.sendSystemMessage(Component.literal("§6[White Hole] §aGema Echo Shard membuka jalinan ruang waktu!"));
                }
            } else if (level.isClientSide()) {
                // Efek visual ledakan partikel portal kosmik megah di sisi Client
                for (int i = 0; i < 60; i++) {
                    level.addParticle(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                            (level.getRandom().nextDouble() - 0.5) * 0.8, level.getRandom().nextDouble() * 0.6, (level.getRandom().nextDouble() - 0.5) * 0.8);
                }
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.0F, 1.1F, false);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
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
