package com.unnameduser.bulletinboard.block;

import com.mojang.serialization.MapCodec;
import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PlacedNoteBlock extends BaseEntityBlock {

    public static final MapCodec<PlacedNoteBlock> CODEC = simpleCodec(PlacedNoteBlock::new);

    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 7);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 1.0, 14.0);

    public PlacedNoteBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any().setValue(ROTATION, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedNoteBlockEntity(pos, state);
    }

    // ============ РАЗМЕЩЕНИЕ ============

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);

        if (ctx.getClickedFace() != Direction.UP) {
            return null;
        }

        if (!canPlaceOn(belowState, world, belowPos)) {
            return null;
        }

        int rotation = getRotationFromPlayer(ctx.getPlayer());
        return this.defaultBlockState().setValue(ROTATION, rotation);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = world.getBlockState(belowPos);
        return canPlaceOn(belowState, world, belowPos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canPlaceOn(neighborState, world, neighborPos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    private boolean canPlaceOn(BlockState belowState, BlockGetter world, BlockPos belowPos) {
        if (!isSolidForNote(belowState, world, belowPos)) {
            return false;
        }

        Block block = belowState.getBlock();

        if (block instanceof DoorBlock
                || block instanceof TrapDoorBlock
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
                || block instanceof BaseRailBlock
                || block == BulletinBoardMod.BULLETIN_BOARD
                || block instanceof PlacedNoteBlock) {
            return false;
        }

        return true;
    }

    private boolean isSolidForNote(BlockState state, BlockGetter world, BlockPos pos) {
        if (state.isSolidRender(world, pos)) {
            return true;
        }

        if (state.getBlock() instanceof StairBlock) {
            return state.getValue(StairBlock.HALF) == Half.TOP;
        }

        if (state.getBlock() instanceof SlabBlock) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            return type == SlabType.DOUBLE || type == SlabType.TOP;
        }

        return false;
    }

    private int getRotationFromPlayer(@Nullable Player player) {
        if (player == null) return 0;

        float yaw = player.getYRot();
        yaw = ((yaw % 360) + 360) % 360;

        int rotation = Math.round(yaw / 45.0f) & 7;
        return (rotation + 4) & 7;
    }

    // ============ ВЗАИМОДЕЙСТВИЕ ============

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!world.isClientSide) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                if (player instanceof ServerPlayer serverPlayer) {
                    ModPackets.sendOpenPlacedNoteScreen(serverPlayer, pos);
                }
                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    // ============ ДРОП ПРИ РАЗРУШЕНИИ ============

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                if (!noteEntity.isTakenByPlayer()) {
                    NoteData note = noteEntity.getNoteData();
                    if (note != null && !world.isClientSide) {
                        ItemStack stack = new ItemStack(
                                note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                                1
                        );
                        CompoundTag rootTag = new CompoundTag();
                        rootTag.put("NoteData", note.toNbt());
                        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));

                        Containers.dropItemStack(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                    }
                }
            }
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    // ============ ТИК ============

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide ? null : (world1, pos1, state1, blockEntity) -> {
            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                noteEntity.tick();
            }
        };
    }

    // ============ РЕНДЕР ============

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    public ItemStack getCloneItemStack(LevelReader world, BlockPos pos, BlockState state, boolean includeData) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
            NoteData note = noteEntity.getNoteData();
            if (note != null) {
                ItemStack stack = new ItemStack(
                        note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                        1
                );
                CompoundTag rootTag = new CompoundTag();
                rootTag.put("NoteData", note.toNbt());
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));
                return stack;
            }
        }
        return new ItemStack(BulletinBoardMod.NOTE_PAPER);
    }
}