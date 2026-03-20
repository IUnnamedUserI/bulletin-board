package com.unnameduser.bulletinboard;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.ModBlockEntities;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.renderer.BulletinBoardRenderer;
import com.unnameduser.bulletinboard.screen.NoteViewScreen;
import com.unnameduser.bulletinboard.util.NoteData;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
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
        BlockEntityRendererFactories.register(
                ModBlockEntities.BULLETIN_BOARD_ENTITY,
                BulletinBoardRenderer::new
        );

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
        return stack.getItem() instanceof NotePaperItem  // ← Работает для NOTE_PAPER и SMALL_NOTE_PAPER
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

        if (horizontal < 0.4 && horizontal > 0.15) {
            if (y < 0.28 && y > 0.12) return 3;  // Между слотами 3 и 2: (0.22 + 0.26) / 2
            if (y < 0.45 && y > 0.29) return 2;  // Между слотами 2 и 1: (0.44 + 0.48) / 2
            if (y < 0.63 && y > 0.47) return 1;  // Между слотами 1 и 0: (0.66 + 0.70) / 2
            if (y < 0.81 && y > 0.65) return 0;
            return -1;
        } else if (horizontal > 0.47 && horizontal < 0.83 && y < 0.7 && y > 0.3) {
            return 4;
        } else { return -1; }
    }

    private void renderHighlight(MatrixStack matrices, VertexConsumerProvider consumers,
                                 BlockPos pos, int slot, Direction facing,
                                 int color, float alpha) {

        MinecraftClient client = MinecraftClient.getInstance();
        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();

        int tempColor = color;

        boolean hasSmallNote = isSmallNote(mainHand) || isSmallNote(offHand);
        boolean hasNormalNote = isNormalNote(mainHand) || isNormalNote(offHand);
        boolean hasEmptyHand = mainHand.isEmpty() && offHand.isEmpty();

        Box highlightBox = null;

        // 🔧 Логика для МАЛЫХ записок (зелёная подсветка)
        if (hasSmallNote) {
            // Малые записки можно вешать только в слоты 0-3
            if (slot >= 0 && slot <= 3) {
                highlightBox = getSlotWorldBox(slot, pos, facing);
            } else {
                return; // Невалидный слот для малой записки
            }
        }
        // 🔧 Логика для ОБЫЧНЫХ записок
        else if (hasNormalNote) {
            var blockEntity = client.world.getBlockEntity(pos);
            if (blockEntity instanceof BulletinBoardBlockEntity boardEntity) {
                if (slot == 0 || slot == 1) {
                    highlightBox = getCombinedSlotWorldBox(0, 1, pos, facing);
                    if (!boardEntity.isPositionFree(0) || !boardEntity.isPositionFree(1)) {
                        tempColor = COLOR_OCCUPIED;
                    }
                } else if (slot == 2 || slot == 3) {
                    highlightBox = getCombinedSlotWorldBox(2, 3, pos, facing);
                    if (!boardEntity.isPositionFree(2) || !boardEntity.isPositionFree(3)) {
                        tempColor = COLOR_OCCUPIED;
                    }
                } else if (slot == 4) {
                    highlightBox = getSlotWorldBox(4, pos, facing);
                } else {
                    return;
                }
            }
        }
        // 🔧 Пустая рука — жёлтая подсветка для взаимодействия
        else if (hasEmptyHand && tempColor == COLOR_INTERACT) {
            var blockEntity = client.world.getBlockEntity(pos);
            if (blockEntity instanceof BulletinBoardBlockEntity boardEntity) {
                NoteData note = boardEntity.getNoteAtPosition(slot);
                if (note != null && !note.isSmall()) {
                    if (slot == 0 || slot == 1) {
                        highlightBox = getCombinedSlotWorldBox(0, 1, pos, facing);
                    } else if (slot == 2 || slot == 3) {
                        highlightBox = getCombinedSlotWorldBox(2, 3, pos, facing);
                    } else {
                        highlightBox = getSlotWorldBox(slot, pos, facing);
                    }
                } else {
                    highlightBox = getSlotWorldBox(slot, pos, facing);
                }
            } else {
                highlightBox = getSlotWorldBox(slot, pos, facing);
            }
        } else {
            return;
        }

        // Отрисовка
        VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());
        var cam = client.gameRenderer.getCamera().getPos();

        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        float r = ((tempColor >> 16) & 0xFF) / 255f;
        float g = ((tempColor >> 8) & 0xFF) / 255f;
        float b = (tempColor & 0xFF) / 255f;
        drawBox(lines, matrices, highlightBox, r, g, b, alpha);
        matrices.pop();
    }

    private Box getSlotWorldBox(int slot, BlockPos pos, Direction facing) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double slotX1, slotX2, slotY1, slotY2;

        // Для маленьких слотов (0-3)
        if (slot >= 0 && slot <= 3) {
            slotX1 = 0.14; slotX2 = 0.42;
            // Высота зависит от слота
            switch (slot) {
                case 0:
                    slotY1 = 0.64; slotY2 = 0.80;
                    break;
                case 1:
                    slotY1 = 0.47; slotY2 = 0.63;
                    break;
                case 2:
                    slotY1 = 0.29; slotY2 = 0.45;
                    break;
                case 3:
                    slotY1 = 0.12; slotY2 = 0.28;
                    break;
                default:
                    slotY1 = 0.04; slotY2 = 0.88;
            }
        } else { // Большой слот (4)
            slotX1 = 0.47; slotX2 = 0.83;
            slotY1 = 0.3; slotY2 = 0.7;
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

    private boolean isSmallNote(ItemStack stack) {
        return stack.getItem() instanceof NotePaperItem &&
                ((NotePaperItem) stack.getItem()).isSmall() &&
                stack.hasNbt() && stack.getNbt().contains("NoteData");
    }

    private boolean isNormalNote(ItemStack stack) {
        return stack.getItem() instanceof NotePaperItem &&
                !((NotePaperItem) stack.getItem()).isSmall() &&
                stack.hasNbt() && stack.getNbt().contains("NoteData");
    }

    private Box getCombinedSlotWorldBox(int slot1, int slot2, BlockPos pos, Direction facing) {
        Box box1 = getSlotWorldBox(slot1, pos, facing);
        Box box2 = getSlotWorldBox(slot2, pos, facing);

        return new Box(
                Math.min(box1.minX, box2.minX),
                Math.min(box1.minY, box2.minY),
                Math.min(box1.minZ, box2.minZ),
                Math.max(box1.maxX, box2.maxX),
                Math.max(box1.maxY, box2.maxY),
                Math.max(box1.maxZ, box2.maxZ)
        );
    }
}