package com.unnameduser.bulletinboard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.ModBlockEntities;
import com.unnameduser.bulletinboard.client.VillagerNameClientCache;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.renderer.BulletinBoardRenderer;
import com.unnameduser.bulletinboard.renderer.PlacedNoteBlockRenderer;
import com.unnameduser.bulletinboard.renderer.VillagerNameRenderer;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BulletinBoardMod.FORGE_MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BulletinBoardClient {

    private static final int COLOR_FREE = 0x00FF00;
    private static final int COLOR_OCCUPIED = 0xFF0000;
    private static final int COLOR_INTERACT = 0xFFFF00;

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        BlockEntityRenderers.register(
                ModBlockEntities.BULLETIN_BOARD_ENTITY,
                BulletinBoardRenderer::new
        );

        BlockEntityRenderers.register(
                ModBlockEntities.PLACED_NOTE_ENTITY,
                PlacedNoteBlockRenderer::new
        );

        // Аналог Fabric WorldRenderEvents: имена жителей и подсветка слотов
        MinecraftForge.EVENT_BUS.addListener(BulletinBoardClient::onRenderLevelStage);
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            renderVillagerNames(event);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            renderBlockHighlight(event);
        }
    }

    private static void renderVillagerNames(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (client.player == null || level == null) return;

        MultiBufferSource.BufferSource buffers = client.renderBuffers().bufferSource();

        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Villager villager) {
                String nameKey = VillagerNameClientCache.getNameKey(villager.getUUID());
                VillagerNameRenderer.render(villager, nameKey, event.getPoseStack(), buffers, event.getPartialTick());
            }
        }
    }

    private static void renderBlockHighlight(RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (client.player == null || level == null) return;

        ItemStack mainHand = client.player.getMainHandItem();
        ItemStack offHand = client.player.getOffhandItem();

        boolean hasNote = isSignedNote(mainHand) || isSignedNote(offHand);
        boolean hasEmptyHand = mainHand.isEmpty() && offHand.isEmpty();
        if (!hasNote && !hasEmptyHand) return;

        if (client.hitResult instanceof BlockHitResult hitResult) {
            BlockPos pos = hitResult.getBlockPos();
            BlockState state = level.getBlockState(pos);

            if (state.getBlock() instanceof BulletinBoardBlock) {
                var blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof BulletinBoardBlockEntity boardEntity) {
                    Direction facing = state.getValue(BulletinBoardBlock.FACING);

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
                        renderHighlight(event, pos, slot, facing, color, 0.7f);
                    }
                }
            }
        }
    }

    private static boolean isSignedNote(ItemStack stack) {
        return stack.getItem() instanceof NotePaperItem
                && stack.hasTag() && stack.getTag().contains("NoteData");
    }

    private static int calculateSlot(BlockHitResult hit, BlockPos pos, Direction facing) {
        Vec3 local = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
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
            if (y < 0.28 && y > 0.12) return 3;
            if (y < 0.45 && y > 0.29) return 2;
            if (y < 0.63 && y > 0.47) return 1;
            if (y < 0.81 && y > 0.65) return 0;
            return -1;
        } else if (horizontal > 0.47 && horizontal < 0.83 && y < 0.7 && y > 0.3) {
            return 4;
        } else { return -1; }
    }

    private static void renderHighlight(RenderLevelStageEvent event, BlockPos pos, int slot,
                                        Direction facing, int color, float alpha) {

        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (client.player == null || level == null) return;

        PoseStack matrices = event.getPoseStack();
        MultiBufferSource.BufferSource consumers = client.renderBuffers().bufferSource();

        ItemStack mainHand = client.player.getMainHandItem();
        ItemStack offHand = client.player.getOffhandItem();

        int tempColor = color;

        boolean hasSmallNote = isSmallNote(mainHand) || isSmallNote(offHand);
        boolean hasNormalNote = isNormalNote(mainHand) || isNormalNote(offHand);
        boolean hasEmptyHand = mainHand.isEmpty() && offHand.isEmpty();

        AABB highlightBox;

        if (hasSmallNote) {
            if (slot >= 0 && slot <= 3) {
                highlightBox = getSlotWorldBox(slot, pos, facing);
            } else {
                return;
            }
        } else if (hasNormalNote) {
            var blockEntity = level.getBlockEntity(pos);
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
            } else {
                return;
            }
        } else if (hasEmptyHand && tempColor == COLOR_INTERACT) {
            var blockEntity = level.getBlockEntity(pos);
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

        VertexConsumer lines = consumers.getBuffer(RenderType.lines());
        Vec3 cam = client.gameRenderer.getMainCamera().getPosition();

        matrices.pushPose();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        float r = ((tempColor >> 16) & 0xFF) / 255f;
        float g = ((tempColor >> 8) & 0xFF) / 255f;
        float b = (tempColor & 0xFF) / 255f;
        drawBox(lines, matrices, highlightBox, r, g, b, alpha);
        matrices.popPose();
    }

    private static AABB getSlotWorldBox(int slot, BlockPos pos, Direction facing) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double slotX1, slotX2, slotY1, slotY2;

        if (slot >= 0 && slot <= 3) {
            slotX1 = 0.14; slotX2 = 0.42;
            switch (slot) {
                case 0: slotY1 = 0.64; slotY2 = 0.80; break;
                case 1: slotY1 = 0.47; slotY2 = 0.63; break;
                case 2: slotY1 = 0.29; slotY2 = 0.45; break;
                case 3: slotY1 = 0.12; slotY2 = 0.28; break;
                default: slotY1 = 0.04; slotY2 = 0.88;
            }
        } else {
            slotX1 = 0.47; slotX2 = 0.83;
            slotY1 = 0.3; slotY2 = 0.7;
        }

        return switch (facing) {
            case NORTH -> new AABB(x + slotX1, y + slotY1, z + 0.93, x + slotX2, y + slotY2, z + 0.95);
            case SOUTH -> new AABB(x + slotX1, y + slotY1, z + 0.05, x + slotX2, y + slotY2, z + 0.07);
            case WEST  -> new AABB(x + 0.93, y + slotY1, z + slotX1, x + 0.95, y + slotY2, z + slotX2);
            case EAST  -> new AABB(x + 0.05, y + slotY1, z + slotX1, x + 0.07, y + slotY2, z + slotX2);
            default    -> new AABB(x + slotX1, y + slotY1, z + 0.93, x + slotX2, y + slotY2, z + 0.95);
        };
    }

    private static void drawBox(VertexConsumer lines, PoseStack matrices, AABB box,
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

    private static void line(VertexConsumer lines, PoseStack matrices,
                             double x1, double y1, double z1, double x2, double y2, double z2,
                             float r, float g, float b, float a, int nx, int ny, int nz) {
        lines.vertex(matrices.last().pose(), (float) x1, (float) y1, (float) z1)
                .color(r, g, b, a).normal(nx, ny, nz).endVertex();
        lines.vertex(matrices.last().pose(), (float) x2, (float) y2, (float) z2)
                .color(r, g, b, a).normal(nx, ny, nz).endVertex();
    }

    private static boolean isSmallNote(ItemStack stack) {
        return stack.getItem() instanceof NotePaperItem notePaper &&
                notePaper.isSmall() &&
                stack.hasTag() && stack.getTag().contains("NoteData");
    }

    private static boolean isNormalNote(ItemStack stack) {
        return stack.getItem() instanceof NotePaperItem notePaper &&
                !notePaper.isSmall() &&
                stack.hasTag() && stack.getTag().contains("NoteData");
    }

    private static AABB getCombinedSlotWorldBox(int slot1, int slot2, BlockPos pos, Direction facing) {
        AABB box1 = getSlotWorldBox(slot1, pos, facing);
        AABB box2 = getSlotWorldBox(slot2, pos, facing);

        return new AABB(
                Math.min(box1.minX, box2.minX),
                Math.min(box1.minY, box2.minY),
                Math.min(box1.minZ, box2.minZ),
                Math.max(box1.maxX, box2.maxX),
                Math.max(box1.maxY, box2.maxY),
                Math.max(box1.maxZ, box2.maxZ)
        );
    }
}
