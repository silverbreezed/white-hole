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
import net.minecraft.world.level.ItemLike;
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
import org.silverbreezed.whitehole.config.ModConfig;
import org.silverbreezed.whitehole.event.VoidDeathHandler;
import org.silverbreezed.whitehole.item.ModItems;
import org.silverbreezed.whitehole.manager.ConfigManager;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class WhiteHoleAltarBlock extends Block {

    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    private static UUID lastPlacerUUID = null;

    private static final HashMap<UUID, Long> ALTAR_COOLDOWN = new HashMap<>();

    public WhiteHoleAltarBlock(Properties properties) {
        super(properties);
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

        if (ALTAR_COOLDOWN.containsKey(playerUUID)) {
            long timePassed = gameTime - ALTAR_COOLDOWN.get(playerUUID);
            ModConfig modConfig = ConfigManager.getModConfig();

            if (modConfig.altarCooldown && timePassed < modConfig.defaultAltarCooldown) {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("§f[White Hole] §cThe altar is cooldown. Wait §e" + ((1200 - timePassed) / 20) + " §cs."));
                }
                return InteractionResult.SUCCESS;
            }
        }

        // ---  BEGINNING: COSMIC EYE ---
        if (!hasEye) {
            if (heldItem.is(ModItems.COSMIC_EYE)) {
                level.setBlock(pos, state.setValue(ACTIVE, true), 3);
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }

                if (!level.isClientSide()) {
                    lastPlacerUUID = playerUUID; // Kunci identitas pemain
                    player.sendSystemMessage(Component.literal("§5[§lWhite Hole§r§5] §dCosmic Eye has installed. Opening the gate of the void singularity..."));
                    level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.BLOCKS, 0.3F, 1.05F);
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 1.10F);

                    level.scheduleTick(pos, this, 60);
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.literal("§7This altar pillar has an empty cavity in the shape of an eye. Use Cosmic Eye item to begin restoration."));
                }
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * 3 SECONDS DELAY TO SHOW OUTPUT
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

        org.silverbreezed.whitehole.event.DeathRecord lastDeath = org.silverbreezed.whitehole.event.VoidDeathHandler.popLastDeathRecord(serverLevel, lastPlacerUUID);

        // --- IF ITEMS EXISTS ---
        if (lastDeath != null) {
            List<ItemStack> savedItems = lastDeath.getItems();

            for (ItemStack stack : savedItems) {
                ItemEntity itemEntity = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, stack);
                itemEntity.setDeltaMovement((random.nextDouble() - 0.5) * 0.2, 0.35, (random.nextDouble() - 0.5) * 0.2);
                serverLevel.addFreshEntity(itemEntity);
            }

            serverLevel.setBlock(pos, state.setValue(ACTIVE, false), 3);
            ALTAR_COOLDOWN.put(lastPlacerUUID, gameTime);

            serverLevel.playSound(null, pos, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 1.0F, 1.1F);

            if (player != null) {
                player.sendSystemMessage(Component.literal("§f[§lWhite Hole§r] §7The singularity broke! All your materials have been successfully reconstructed."));
            }
        }
        // --- IF NO ITEMS ---
        else {
            serverLevel.setBlock(pos, state.setValue(ACTIVE, false), 3);

            ItemEntity eyeDrop = new ItemEntity(serverLevel, spawnX, spawnY, spawnZ, new ItemStack(ModItems.COSMIC_EYE));
            eyeDrop.setDeltaMovement((random.nextDouble() - 0.5) * 0.1, 0.2, (random.nextDouble() - 0.5) * 0.1);
            serverLevel.addFreshEntity(eyeDrop);

            serverLevel.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.2F, 1.0F);

            if (player != null) {
                player.sendSystemMessage(Component.literal(
                        "§4[§lWhite Hole§r§4] §cThe gate refuses entry! No such materials or items on the last void death"
                ));
            }
        }

        lastPlacerUUID = null;
    }

    @Override
    protected float getShadeBrightness(@NonNull BlockState state, @NonNull BlockGetter level, @NonNull BlockPos pos) {
        return 1.0F;
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

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

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.1;
        double z = pos.getZ() + 0.5;

        boolean isEyePlaced = state.getValue(ACTIVE);

        if (isEyePlaced) {
            // Particles
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * 2.0 * Math.PI;
                double radius = 0.25 + (random.nextDouble() * 0.2);

                double particleX = x + Math.cos(angle) * radius;
                double particleZ = z + Math.sin(angle) * radius;

                level.addParticle(ParticleTypes.REVERSE_PORTAL, particleX, y, particleZ,
                        (x - particleX) * -0.08,
                        0.015D,
                        (z - particleZ) * -0.08
                );
            }

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
