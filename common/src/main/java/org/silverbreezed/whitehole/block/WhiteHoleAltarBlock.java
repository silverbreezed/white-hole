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

import java.util.List;

public class WhiteHoleAltarBlock extends Block {

    // 1. Membuat properti status AKTIF (bawaan vanilla Mojang)
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public WhiteHoleAltarBlock(Properties properties) {
        super(properties);
        // Default saat ditaruh atau ditemukan di dunia adalah MATI / RUSAK
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(ACTIVE, false)
                .setValue(FACING, Direction.NORTH));
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

    @Override
    protected @NonNull InteractionResult useWithoutItem(BlockState state, @NonNull Level level, @NonNull BlockPos pos, Player player, @NonNull BlockHitResult hitResult) {
        ItemStack heldItem = player.getMainHandItem();
        boolean hasEye = state.getValue(ACTIVE); // Kita gunakan properti ACTIVE sebagai penanda mata terpasang

        // --- TAHAP 1: MEMASANG MATA (JIKA PILAR MASIH KOSONG) ---
        if (!hasEye) {
            if (heldItem.is(ModItems.COSMIC_EYE)) {
                if (!level.isClientSide()) {
                    // Pasang mata, ubah tekstur menjadi Terbuka/Menyala (LIT=true)
                    level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                    if (!player.getAbilities().instabuild) heldItem.shrink(1);

                    player.sendSystemMessage(Component.literal("§5[White Hole] §dMata Kosmik terpasang pas pada pilar kuno..."));
                } else {
                    level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);
                    level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 2.0F, 1.10F, false);
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
                if (VoidDeathHandler.hasSavedItems(player.getUUID())) {
                    List<ItemStack> savedItems = VoidDeathHandler.getAndClearSavedItems(player.getUUID());
                    if (savedItems != null) {
                        // Muntahkan item (Logika pemulihan dari Void kemarin)
                        double spawnX = pos.getX() + 0.5;
                        double spawnY = pos.getY() + 1.2;
                        double spawnZ = pos.getZ() + 0.5;
                        for (ItemStack stack : savedItems) {
                            ItemEntity itemEntity = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, stack);
                            itemEntity.setDeltaMovement((serverLevel.getRandom().nextDouble() - 0.5) * 0.2, 0.3, (serverLevel.getRandom().nextDouble() - 0.5) * 0.2);
                            serverLevel.addFreshEntity(itemEntity);
                        }

                        if (!player.getAbilities().instabuild) heldItem.shrink(1);
                        player.sendSystemMessage(Component.literal("§6[White Hole] §aGema Echo Shard membuka jalinan ruang waktu!"));
                    }
                } else {
                    player.sendSystemMessage(Component.literal("§6[White Hole] §cMata Kosmik berkedip tenang. Jiwa Anda aman di dunia ini."));
                }
            } else if (level.isClientSide()) {
                // Efek badai partikel yang memancar megah (Metode partikel kemarin)
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
}
