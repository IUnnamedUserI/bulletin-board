package com.unnameduser.bulletinboard.renderer;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.EnumMap;
import java.util.Map;

public class BulletinBoardRenderer implements BlockEntityRenderer<BulletinBoardBlockEntity> {

    private static final Identifier NOTE_TEXTURE = new Identifier("bulletin-board", "textures/block/note_paper.png");
    private static final Identifier BADGE_TEXTURE = new Identifier("bulletin-board", "textures/block/badge.png");

    private static final Map<Direction, BadgeConfig> BADGE_CONFIGS = new EnumMap<>(Direction.class);

    static {
        BADGE_CONFIGS.put(Direction.NORTH, new BadgeConfig(-0.225, -0.35, 0.01, false, 0.25f));
        BADGE_CONFIGS.put(Direction.SOUTH, new BadgeConfig(0.225, -0.35, 0.01, true, 0.25f));
        BADGE_CONFIGS.put(Direction.WEST,  new BadgeConfig(0.225, -0.35, -0.01, false, 0.25f));
        BADGE_CONFIGS.put(Direction.EAST,  new BadgeConfig(-0.225, -0.35, -0.01, true, 0.25f));
    }

    public BulletinBoardRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(BulletinBoardBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        if (entity.getWorld() == null) return;

        var state = entity.getCachedState();
        if (!(state.getBlock() instanceof BulletinBoardBlock)) return;

        Direction facing = state.get(BulletinBoardBlock.FACING);
        var notes = entity.getNotes();
        var positions = entity.getNotePositions();

        if (notes.isEmpty()) return;

        matrices.push();

        matrices.translate(0.5, 0.5, 0.5);

        switch (facing) {
            case NORTH:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                matrices.translate(0, 0, -0.45);
                break;

            case SOUTH:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0));
                matrices.translate(0, 0, -0.45);
                matrices.scale(-1, 1, 1);
                break;

            case WEST:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                matrices.translate(0, 0, 0.45);
                break;

            case EAST:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
                matrices.translate(0, 0, 0.45);
                matrices.scale(-1, 1, 1);
                break;
        }

        boolean mirrorTexture = (facing == Direction.SOUTH || facing == Direction.WEST);

        for (int i = 0; i < notes.size(); i++) {
            NoteData note = notes.get(i);
            int position = positions.get(i);
            double[] slotCoords = getSlotCoordinates(position, facing);

            matrices.push();
            matrices.translate(slotCoords[0], slotCoords[1], 0.01);

            float scale = 0.3f;
            if (position == 2) scale = 0.4f;
            matrices.scale(scale, scale, 1);

            renderNote(matrices, vertexConsumers, light, overlay, mirrorTexture);

            if (note.getTagColor() != -1) {
                renderBadge(matrices, vertexConsumers, light, overlay, facing, note.getTagColor());
            }

            matrices.pop();
        }

        matrices.pop();
    }

    private double[] getSlotCoordinates(int position, Direction facing) {
        double x, y;

        switch (position) {
            case 0:
                x = -0.23;
                y = 0.1;
                break;
            case 1:
                x = -0.23;
                y = -0.23;
                break;
            case 2:
                x = 0.15;
                y = 0.0;
                break;
            default:
                return new double[]{0, 0};
        }

        switch (facing) {
            case SOUTH:
            case EAST:
                x = -x;
                break;
            case NORTH:
                x = -x;
                break;
            case WEST:
                x = -x;
                break;
        }

        return new double[]{x, y};
    }

    private void renderNote(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                            int light, int overlay, boolean mirror) {

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(NOTE_TEXTURE));
        MatrixStack.Entry entry = matrices.peek();

        float nx = 0, ny = 0, nz = 1;

        if (!mirror) {
            consumer.vertex(entry.getPositionMatrix(), -0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(0, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(1, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(1, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), -0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(0, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();
        } else {
            consumer.vertex(entry.getPositionMatrix(), -0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(1, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(0, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(0, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), -0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255)
                    .texture(1, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();
        }
    }

    private void renderBadge(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                             int light, int overlay, Direction facing, int color) {

        if (color == -1) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(BADGE_TEXTURE));

        BadgeConfig config = BADGE_CONFIGS.get(facing);
        if (config == null) return;

        matrices.push();

        matrices.translate(config.x, config.y, config.z);
        matrices.scale(config.scale, config.scale, 1);

        MatrixStack.Entry entry = matrices.peek();
        float nx = 0, ny = 0, nz = 1;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (!config.mirror) {
            consumer.vertex(entry.getPositionMatrix(), -0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(0, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(1, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(1, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), -0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(0, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();
        } else {
            consumer.vertex(entry.getPositionMatrix(), -0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(1, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(0, 1)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), 0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(0, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();

            consumer.vertex(entry.getPositionMatrix(), -0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f)
                    .texture(1, 0)
                    .overlay(overlay)
                    .light(light)
                    .normal(entry.getNormalMatrix(), nx, ny, nz)
                    .next();
        }

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(BulletinBoardBlockEntity entity) {
        return true;
    }

    private static class BadgeConfig {
        final double x, y, z;
        final boolean mirror;
        final float scale;

        BadgeConfig(double x, double y, double z, boolean mirror, float scale) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.mirror = mirror;
            this.scale = scale;
        }
    }
}