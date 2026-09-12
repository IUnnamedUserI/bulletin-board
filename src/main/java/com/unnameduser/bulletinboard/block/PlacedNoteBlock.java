package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.WorldView;
import net.minecraft.block.entity.BlockEntity;

public class PlacedNoteBlock extends BlockWithEntity {

    public static final IntProperty ROTATION = IntProperty.of("rotation", 0, 7);
    private static final VoxelShape SHAPE = Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 1.0, 14.0);

    public PlacedNoteBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ROTATION, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedNoteBlockEntity(pos, state);
    }

    // ============ РАЗМЕЩЕНИЕ ============

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        if (ctx.getSide() != Direction.UP) {
            return null;
        }

        if (!canPlaceOn(belowState, world, belowPos)) {
            return null;
        }

        int rotation = getRotationFromPlayer(ctx.getPlayer());
        return this.getDefaultState().with(ROTATION, rotation);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);
        return canPlaceOn(belowState, world, belowPos);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canPlaceOn(neighborState, world, neighborPos)) {
            return net.minecraft.block.Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    private boolean canPlaceOn(BlockState belowState, BlockView world, BlockPos belowPos) {
        // Проверяем, твёрдый ли блок (с учётом ступеней/плит)
        if (!isSolidForNote(belowState, world, belowPos)) {
            return false;
        }

        Block block = belowState.getBlock();

        // Чёрный список
        if (block instanceof DoorBlock
                || block instanceof TrapdoorBlock
                || block instanceof FenceBlock
                || block instanceof FenceGateBlock
                || block instanceof CarpetBlock
                || block instanceof TorchBlock
                || block instanceof WallTorchBlock
                || block instanceof CandleBlock
                || block instanceof CandleCakeBlock
                || block instanceof FlowerBlock
                || block instanceof FlowerPotBlock
                || block instanceof ButtonBlock
                || block instanceof LeverBlock
                || block instanceof AbstractRailBlock
                || block == BulletinBoardMod.BULLETIN_BOARD
                || block instanceof PlacedNoteBlock) {
            return false;
        }

        return true;
    }

    private boolean isSolidForNote(BlockState state, BlockView world, BlockPos pos) {
        // Обычный твёрдый блок
        if (state.isSolidBlock(world, pos)) {
            return true;
        }

        // Ступень: если half = BOTTOM (верхняя грань на полной высоте)
        if (state.getBlock() instanceof StairsBlock) {
            return state.get(StairsBlock.HALF) == BlockHalf.TOP;
        }

        // Плита: если DOUBLE (полный блок) или TOP (верхняя грань на полной высоте)
        if (state.getBlock() instanceof SlabBlock) {
            SlabType type = state.get(SlabBlock.TYPE);
            return type == SlabType.DOUBLE || type == SlabType.TOP;
        }

        return false;
    }

    private int getRotationFromPlayer(@Nullable PlayerEntity player) {
        if (player == null) return 0;

        float yaw = player.getYaw();
        // Нормализуем yaw
        yaw = ((yaw % 360) + 360) % 360;

        // 8 позиций по 45 градусов
        int rotation = Math.round(yaw / 45.0f) & 7;

        // Инвертируем для правильного отображения
        return (rotation + 4) & 7;
    }

    // ============ ВЗАИМОДЕЙСТВИЕ ============

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                com.unnameduser.bulletinboard.network.ModPackets.sendOpenPlacedNoteScreen(serverPlayer, pos);
            }
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    // ============ ДРОП ПРИ РАЗРУШЕНИИ ============

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                // Если записка уже забрана игроком или уничтожена дождём — не дропаем
                if (!noteEntity.isTakenByPlayer()) {
                    NoteData note = noteEntity.getNoteData();
                    if (note != null && !world.isClient) {
                        ItemStack stack = new ItemStack(
                                note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                                1
                        );
                        NbtCompound nbt = stack.getOrCreateNbt();
                        nbt.put("NoteData", note.toNbt());

                        ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                    }
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    // ============ ТИК ============

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : (BlockEntityTicker<T>) (world1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                noteEntity.tick();
            }
        };
    }

    // ============ РЕНДЕР ============

    @Override
    public net.minecraft.block.BlockRenderType getRenderType(BlockState state) {
        return net.minecraft.block.BlockRenderType.INVISIBLE;
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
            NoteData note = noteEntity.getNoteData();
            if (note != null) {
                ItemStack stack = new ItemStack(
                        note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                        1
                );
                NbtCompound nbt = stack.getOrCreateNbt();
                nbt.put("NoteData", note.toNbt());
                return stack;
            }
        }
        // Если данных нет, возвращаем пустую записку
        return new ItemStack(BulletinBoardMod.NOTE_PAPER);
    }
}