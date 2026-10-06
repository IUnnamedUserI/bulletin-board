package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BulletinBoardBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BoardType> BOARD_TYPE = EnumProperty.create("type", BoardType.class);

    private static final VoxelShape NORTH_SHAPE = Block.box(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0, 0, 0, 16, 16, 1);
    private static final VoxelShape WEST_SHAPE = Block.box(15, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 0, 0, 1, 16, 16);

    private static final VoxelShape NORTH_DOUBLE_SHAPE = Block.box(0, 0, 15, 32, 16, 16);
    private static final VoxelShape SOUTH_DOUBLE_SHAPE = Block.box(0, 0, 0, 32, 16, 1);
    private static final VoxelShape WEST_DOUBLE_SHAPE = Block.box(15, 0, 0, 16, 16, 32);
    private static final VoxelShape EAST_DOUBLE_SHAPE = Block.box(0, 0, 0, 1, 16, 32);

    public BulletinBoardBlock(BlockBehaviour.Properties settings) {
        super(settings);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BOARD_TYPE, BoardType.SINGLE_WALL));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        BoardType type = state.getValue(BOARD_TYPE);
        Direction facing = state.getValue(FACING);

        if (type == BoardType.DOUBLE_WALL) {
            return switch (facing) {
                case NORTH -> NORTH_DOUBLE_SHAPE;
                case SOUTH -> SOUTH_DOUBLE_SHAPE;
                case WEST -> WEST_DOUBLE_SHAPE;
                case EAST -> EAST_DOUBLE_SHAPE;
                default -> NORTH_DOUBLE_SHAPE;
            };
        } else {
            return switch (facing) {
                case NORTH -> NORTH_SHAPE;
                case SOUTH -> SOUTH_SHAPE;
                case WEST -> WEST_SHAPE;
                case EAST -> EAST_SHAPE;
                default -> NORTH_SHAPE;
            };
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        if (side == Direction.UP || side == Direction.DOWN) return null;

        Direction playerFacing = ctx.getHorizontalDirection();
        Direction facing = playerFacing.getOpposite();

        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();

        BoardType baseType = BoardType.SINGLE_WALL;

        Direction.Axis axis = facing.getAxis();
        BlockPos neighborPos = pos.relative(axis == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH);
        BlockState neighbor = world.getBlockState(neighborPos);

        if (neighbor.getBlock() == this &&
                neighbor.getValue(BOARD_TYPE) == BoardType.SINGLE_WALL &&
                neighbor.getValue(FACING) == facing) {
            baseType = BoardType.DOUBLE_WALL;
        }

        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(BOARD_TYPE, baseType);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BOARD_TYPE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BulletinBoardBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack heldItem = player.getItemInHand(hand);
        int hitPosition = getHitPosition(hit, pos, state);

        if (heldItem.getItem() instanceof NotePaperItem notePaper &&
                heldItem.hasTag() && heldItem.getTag().contains("NoteData")) {

            if (!world.isClientSide) {
                if (hitPosition >= 0) {
                    NoteData note = NoteData.fromNbt(heldItem.getTag().getCompound("NoteData"));

                    boolean canPlace = false;
                    if (note.isSmall()) {
                        canPlace = hitPosition >= 0 && hitPosition <= 3;
                    } else {
                        canPlace = (hitPosition == 4) ||
                                (hitPosition == 0) || (hitPosition == 1) ||
                                (hitPosition == 2) || (hitPosition == 3);
                    }
                    if (canPlace && boardEntity.canPlaceNote(note, hitPosition)) {
                        boolean added = boardEntity.addNoteAtPosition(note, hitPosition);
                        if (added) {
                            heldItem.shrink(1);
                            return InteractionResult.CONSUME;
                        }
                    }
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (hitPosition >= 0) {
            NoteData note = boardEntity.getNoteAtPosition(hitPosition);
            if (note != null) {
                if (!note.hasSeal() && !boardEntity.isNoteStillValid(note)) {
                    int index = boardEntity.getNoteIndexByPosition(hitPosition);
                    if (index >= 0) {
                        boardEntity.removeNote(index);
                    }
                    return InteractionResult.SUCCESS;
                }

                if (!world.isClientSide) {
                    ModPackets.sendOpenNoteScreenToClient((ServerPlayer) player, pos, hitPosition);
                }
                return InteractionResult.SUCCESS;
            }
        }

        if (!world.isClientSide && player.isShiftKeyDown()) {
            showNotes(player, boardEntity);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private int getHitPosition(BlockHitResult hit, BlockPos pos, BlockState state) {
        Vec3 hitPos = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction facing = state.getValue(FACING);
        double x = hitPos.x, y = hitPos.y, z = hitPos.z;

        boolean hitFront = switch (facing) {
            case NORTH -> z > 0.93;
            case SOUTH -> z < 0.07;
            case WEST -> x > 0.93;
            case EAST -> x < 0.07;
            default -> false;
        };
        if (!hitFront) return -1;
        if (y < 0.0 || y > 1.0) return -1;

        double horizontal = (facing == Direction.NORTH || facing == Direction.SOUTH) ? x : z;

        if (horizontal > 0.15 && horizontal < 0.4) {
            if (y > 0.12 && y < 0.28) return 3;
            if (y > 0.29 && y < 0.45) return 2;
            if (y > 0.47 && y < 0.63) return 1;
            if (y > 0.65 && y < 0.81) return 0;
            return -1; // Попал в левую колонку, но между слотами
        }
        // Правая колонка: большой слот 4
        else if (horizontal > 0.47 && horizontal < 0.83 && y > 0.3 && y < 0.7) {
            return 4;
        }

        return -1; // Попал в доску, но не в слот
    }

    private void showNotes(Player player, BulletinBoardBlockEntity boardEntity) {
        var notes = boardEntity.getNotes();
        if (notes.isEmpty()) {
            return;
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return world.isClientSide ? null : (world1, pos, state1, be) -> {
            if (be instanceof BulletinBoardBlockEntity boardBe) {
                boardBe.tick();
            }
        };
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
