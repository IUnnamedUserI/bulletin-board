package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class BulletinBoardBlock extends Block implements BlockEntityProvider {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<BoardType> BOARD_TYPE = EnumProperty.of("type", BoardType.class);

    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 1);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(15, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 1, 16, 16);

    private static final VoxelShape NORTH_DOUBLE_SHAPE = Block.createCuboidShape(0, 0, 15, 32, 16, 16);
    private static final VoxelShape SOUTH_DOUBLE_SHAPE = Block.createCuboidShape(0, 0, 0, 32, 16, 1);
    private static final VoxelShape WEST_DOUBLE_SHAPE = Block.createCuboidShape(15, 0, 0, 16, 16, 32);
    private static final VoxelShape EAST_DOUBLE_SHAPE = Block.createCuboidShape(0, 0, 0, 1, 16, 32);

    public BulletinBoardBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(BOARD_TYPE, BoardType.SINGLE_WALL));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        BoardType type = state.get(BOARD_TYPE);
        Direction facing = state.get(FACING);

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
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        if (side == Direction.UP || side == Direction.DOWN) return null;

        Direction playerFacing = ctx.getHorizontalPlayerFacing();
        Direction facing = playerFacing.getOpposite();

        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        BoardType baseType = BoardType.SINGLE_WALL;

        Direction.Axis axis = facing.getAxis();
        BlockPos neighborPos = pos.offset(axis == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH);
        BlockState neighbor = world.getBlockState(neighborPos);

        if (neighbor.getBlock() == this &&
                neighbor.get(BOARD_TYPE) == BoardType.SINGLE_WALL &&
                neighbor.get(FACING) == facing) {
            baseType = BoardType.DOUBLE_WALL;
        }

        return this.getDefaultState()
                .with(FACING, facing)
                .with(BOARD_TYPE, baseType);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, BOARD_TYPE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BulletinBoardBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {

        BlockEntity blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            return ActionResult.PASS;
        }

        ItemStack heldItem = player.getStackInHand(hand);
        int hitPosition = getHitPosition(hit, pos, state);

        if (heldItem.getItem() == BulletinBoardMod.NOTE_PAPER &&
                heldItem.hasNbt() && heldItem.getNbt().contains("NoteData")) {

            if (!world.isClient) {
                if (hitPosition >= 0 && boardEntity.isPositionFree(hitPosition)) {
                    NoteData note = NoteData.fromNbt(heldItem.getNbt().getCompound("NoteData"));

                    boolean added = boardEntity.addNoteAtPosition(note, hitPosition);

                    if (added) {
                        heldItem.decrement(1);
                        return ActionResult.CONSUME;
                    }
                } else if (hitPosition >= 0) {
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.SUCCESS;
        }

        if (hitPosition >= 0) {
            NoteData note = boardEntity.getNoteAtPosition(hitPosition);
            if (note != null) {
                if (!note.hasSeal() && !boardEntity.isNoteStillValid(note)) {
                    int index = boardEntity.getNoteIndexByPosition(hitPosition);
                    if (index >= 0) {
                        boardEntity.removeNote(index);
                    }
                    return ActionResult.SUCCESS;
                }

                if (!world.isClient) {
                    ModPackets.sendOpenNoteScreenToClient((ServerPlayerEntity) player, pos, hitPosition);
                }
                return ActionResult.SUCCESS;
            }
        }

        if (!world.isClient && player.isSneaking()) {
            showNotes(player, boardEntity);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private int getHitPosition(BlockHitResult hit, BlockPos pos, BlockState state) {
        Vec3d hitPos = hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
        Direction facing = state.get(FACING);
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

        if (horizontal < 0.5) {
            if (y > 0.5) return 0;
            return 1;
        } else {
            return 2;
        }
    }

    private void showNotes(PlayerEntity player, BulletinBoardBlockEntity boardEntity) {
        var notes = boardEntity.getNotes();
        if (notes.isEmpty()) {
            return;
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : (world1, pos, state1, be) -> {
            if (be instanceof BulletinBoardBlockEntity boardBe) {
                boardBe.tick();
            }
        };
    }
}