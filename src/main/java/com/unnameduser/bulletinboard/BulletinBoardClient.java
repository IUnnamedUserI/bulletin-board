package com.unnameduser.bulletinboard;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.ModBlockEntities;
import com.unnameduser.bulletinboard.renderer.BulletinBoardRenderer;
import com.unnameduser.bulletinboard.screen.NoteViewScreen;
import com.unnameduser.bulletinboard.util.NoteData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import static com.unnameduser.bulletinboard.BulletinBoardMod.MOD_ID;

public class BulletinBoardClient implements ClientModInitializer {

    private static final int COLOR_FREE = 0x00FF00;
    private static final int COLOR_OCCUPIED = 0xFF0000;
    private static final int COLOR_INTERACT = 0xFFFF00;

    @Override
    public void onInitializeClient() {
        net.minecraft.client.render.block.entity.BlockEntityRendererFactories.register(
                ModBlockEntities.BULLETIN_BOARD_ENTITY,
                BulletinBoardRenderer::new
        );

        // Исправленная регистрация глобального приемника пакетов
        ClientPlayNetworking.registerGlobalReceiver(new Identifier(MOD_ID, "open_note"),
                (client, handler, buf, responseSender) -> {
                    BlockPos pos = buf.readBlockPos();
                    int slot = buf.readInt();
                    client.execute(() -> {
                        if (client.world != null &&
                                client.world.getBlockEntity(pos) instanceof BulletinBoardBlockEntity boardEntity) {
                            NoteData note = boardEntity.getNoteAtPosition(slot);
                            if (note != null) {
                                client.setScreen(new NoteViewScreen(note, boardEntity, slot));
                            }
                        }
                    });
                });

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register((context) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null) return;

            ItemStack mainHand = client.player.getMainHandStack();
            ItemStack offHand = client.player.getOffHandStack();

            boolean hasNote = isSignedNote(mainHand) || isSignedNote(offHand);
            boolean hasEmptyHand = mainHand.isEmpty() && offHand.isEmpty();
            if (!hasNote && !hasEmptyHand) return;

            if (client.crosshairTarget instanceof BlockHitResult hitResult) {
                BlockPos pos = hitResult.getBlockPos();
                BlockState state = client.world.getBlockState(pos);

                if (state.getBlock() instanceof BulletinBoardBlock) {
                    var blockEntity = client.world.getBlockEntity(pos);
                    if (blockEntity instanceof BulletinBoardBlockEntity boardEntity) {
                        Direction facing = state.get(BulletinBoardBlock.FACING);

                        int slot = calculateSlot(hitResult, pos, facing);
                        if (slot < 0) return;

                        int color;
                        if (hasNote) {
                            color = boardEntity.isPositionFree(slot) ? COLOR_FREE : COLOR_OCCUPIED;
                        } else if (hasEmptyHand) {
                            color = !boardEntity.isPositionFree(slot) ? COLOR_INTERACT : -1;
                        } else {
                            color = -1;
                        }

                        if (color != -1) {
                            renderHighlight(context.matrixStack(), context.consumers(),
                                    pos, slot, facing, color, 0.7f);
                        }
                    }
                }
            }
        });
    }

    private boolean isSignedNote(ItemStack stack) {
        return stack.getItem() == BulletinBoardMod.NOTE_PAPER
                && stack.hasNbt() && stack.getNbt().contains("NoteData");
    }

    private int calculateSlot(BlockHitResult hit, BlockPos pos, Direction facing) {
        var local = hit.getPos().subtract(pos.getX(), pos.getY(), pos.getZ());
        double x = local.x, y = local.y, z = local.z;

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

    private void renderHighlight(MatrixStack matrices, VertexConsumerProvider consumers,
                                 BlockPos pos, int slot, Direction facing,
                                 int color, float alpha) {
        Box worldBox = getSlotWorldBox(slot, pos, facing);
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        var cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        drawBox(lines, matrices, worldBox, r, g, b, alpha);
        matrices.pop();
    }

    private Box getSlotWorldBox(int slot, BlockPos pos, Direction facing) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double slotX1, slotX2, slotY1, slotY2;

        switch (slot) {
            case 0: // Верхний левый
                slotX1 = 0.12; slotX2 = 0.42;
                slotY1 = 0.45; slotY2 = 0.75;
                break;
            case 1: // Нижний левый
                slotX1 = 0.12; slotX2 = 0.42;
                slotY1 = 0.12; slotY2 = 0.42;
                break;
            case 2: // Правый большой
                slotX1 = 0.46; slotX2 = 0.84;
                slotY1 = 0.29; slotY2 = 0.71;
                break;
            default:
                slotX1 = 0.12; slotX2 = 0.42;
                slotY1 = 0.58; slotY2 = 0.88;
        }

        return switch (facing) {
            case NORTH -> new Box(x + slotX1, y + slotY1, z + 0.93,
                    x + slotX2, y + slotY2, z + 0.95);
            case SOUTH -> new Box(x + slotX1, y + slotY1, z + 0.05,
                    x + slotX2, y + slotY2, z + 0.07);
            case WEST  -> new Box(x + 0.93, y + slotY1, z + slotX1,
                    x + 0.95, y + slotY2, z + slotX2);
            case EAST  -> new Box(x + 0.05, y + slotY1, z + slotX1,
                    x + 0.07, y + slotY2, z + slotX2);
            default    -> new Box(x + slotX1, y + slotY1, z + 0.93,
                    x + slotX2, y + slotY2, z + 0.95);
        };
    }

    private void drawBox(VertexConsumer lines, MatrixStack matrices, Box box,
                         float r, float g, float b, float a) {
        double minX = box.minX, maxX = box.maxX;
        double minY = box.minY, maxY = box.maxY;
        double minZ = box.minZ, maxZ = box.maxZ;
        line(lines, matrices, minX, minY, minZ, maxX, minY, minZ, r, g, b, a, 1, 0, 0);
        line(lines, matrices, minX, minY, minZ, minX, maxY, minZ, r, g, b, a, 0, 1, 0);
        line(lines, matrices, minX, minY, minZ, minX, minY, maxZ, r, g, b, a, 0, 0, 1);
        line(lines, matrices, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a, 0, 1, 0);
        line(lines, matrices, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a, 0, 0, 1);
        line(lines, matrices, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a, 1, 0, 0);
        line(lines, matrices, minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a, 0, 0, 1);
        line(lines, matrices, minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a, 1, 0, 0);
        line(lines, matrices, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        line(lines, matrices, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a, 0, 1, 0);
        line(lines, matrices, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a, 0, 0, 1);
        line(lines, matrices, minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a, 1, 0, 0);
    }

    private void line(VertexConsumer lines, MatrixStack matrices,
                      double x1, double y1, double z1, double x2, double y2, double z2,
                      float r, float g, float b, float a, int nx, int ny, int nz) {
        lines.vertex(matrices.peek().getPositionMatrix(), (float)x1, (float)y1, (float)z1)
                .color(r, g, b, a).normal(nx, ny, nz).next();
        lines.vertex(matrices.peek().getPositionMatrix(), (float)x2, (float)y2, (float)z2)
                .color(r, g, b, a).normal(nx, ny, nz).next();
    }
}