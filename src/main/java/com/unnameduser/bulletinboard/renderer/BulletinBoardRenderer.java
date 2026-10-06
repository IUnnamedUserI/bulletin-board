package com.unnameduser.bulletinboard.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

public class BulletinBoardRenderer implements BlockEntityRenderer<BulletinBoardBlockEntity> {

    private static final ResourceLocation NOTE_TEXTURE = new ResourceLocation("bulletin-board", "textures/block/note_paper.png");
    private static final ResourceLocation SMALL_NOTE_TEXTURE = new ResourceLocation("bulletin-board", "textures/block/small_note_paper.png");
    private static final ResourceLocation BADGE_TEXTURE = new ResourceLocation("bulletin-board", "textures/block/badge.png");

    private static final Map<Direction, BadgeConfig> BADGE_CONFIGS = new EnumMap<>(Direction.class);
    private static final Map<Direction, BadgeConfig> SMALL_BADGE_CONFIGS = new EnumMap<>(Direction.class);

    static {
        BADGE_CONFIGS.put(Direction.NORTH, new BadgeConfig(-0.225, -0.35, 0.01, false, 0.25f));
        BADGE_CONFIGS.put(Direction.SOUTH, new BadgeConfig(0.225, -0.35, 0.01, true, 0.25f));
        BADGE_CONFIGS.put(Direction.WEST,  new BadgeConfig(0.225, -0.35, 0.01, false, 0.25f));
        BADGE_CONFIGS.put(Direction.EAST,  new BadgeConfig(-0.225, -0.35, -0.01, true, 0.25f));
    }

    static {
        SMALL_BADGE_CONFIGS.put(Direction.NORTH, new BadgeConfig(-0.225, -0.125, 0.01, false, 0.2f));
        SMALL_BADGE_CONFIGS.put(Direction.SOUTH, new BadgeConfig(0.225, -0.125, 0.01, true, 0.2f));
        SMALL_BADGE_CONFIGS.put(Direction.WEST,  new BadgeConfig(0.225, -0.125, 0.01, false, 0.2f));
        SMALL_BADGE_CONFIGS.put(Direction.EAST,  new BadgeConfig(-0.225, -0.125, -0.01, true, 0.2f));
    }

    public BulletinBoardRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(BulletinBoardBlockEntity entity, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {

        if (entity.getLevel() == null) return;

        var state = entity.getBlockState();
        if (!(state.getBlock() instanceof BulletinBoardBlock)) return;

        Direction facing = state.getValue(BulletinBoardBlock.FACING);
        var notes = entity.getNotes();
        var positions = entity.getNotePositions();

        if (notes.isEmpty()) return;

        matrices.pushPose();

        // 🔧 Стандартное центрирование (как в рабочем коде)
        matrices.translate(0.5, 0.5, 0.5);

        // 🔧 Поворот и смещение по направлению (как в рабочем коде)
        switch (facing) {
            case NORTH:
                matrices.mulPose(Axis.YP.rotationDegrees(180));
                matrices.translate(0, 0, -0.45);
                break;
            case SOUTH:
                matrices.mulPose(Axis.YP.rotationDegrees(0));
                matrices.translate(0, 0, -0.45);
                matrices.scale(-1, 1, 1);
                break;
            case WEST:
                matrices.mulPose(Axis.YP.rotationDegrees(-90));
                matrices.translate(0, 0, -0.45);
                break;
            case EAST:
                matrices.mulPose(Axis.YP.rotationDegrees(-90));
                matrices.translate(0, 0, 0.45);
                matrices.scale(-1, 1, 1);
                break;
        }

        boolean mirrorTexture = (facing == Direction.SOUTH || facing == Direction.WEST);

        for (int i = 0; i < notes.size(); i++) {
            NoteData note = notes.get(i);
            int position = positions.get(i);

            // 🔧 Получаем координаты слота (адаптировано под 5 слотов)
            double[] slotCoords;
            if (!note.isSmall() && (position == 0 || position == 2)) {
                double[] top = getSlotCoordinates(position, facing);     // Слот 0 или 2
                double[] bottom = getSlotCoordinates(position + 1, facing); // Слот 1 или 3
                // Среднее между верхним и нижним
                slotCoords = new double[]{ top[0], (top[1] + bottom[1]) / 2.0 };
            } else {
                slotCoords = getSlotCoordinates(position, facing);
            }

            matrices.pushPose();
            matrices.translate(slotCoords[0], slotCoords[1], 0.01);

            // 🔧 Масштаб: 0.3 для малых, 0.4 для большой
            float scale = (position == 4) ? 0.4f : 0.3f;
            matrices.scale(scale, scale, 1);

            renderNote(matrices, vertexConsumers, light, overlay, mirrorTexture, note.isSmall());

            if (note.getTagColor() != -1) {
                renderBadge(matrices, vertexConsumers, light, overlay, facing, note.getTagColor(), note.isSmall());
            }

            matrices.popPose();
        }

        matrices.popPose();
    }

    // 🔧 АДАПТИРОВАНО: координаты как в рабочем коде + расширено на 5 слотов
    private double[] getSlotCoordinates(int position, Direction facing) {
        double x, y;

        switch (position) {
            // 🔧 Малые слоты: ВСЕ в левом столбце (x = -0.23), stacked по Y
            case 0: // Самый верхний малый
                x = -0.22;
                y = 0.22;
                break;
            case 1: // Второй сверху малый
                x = -0.22;
                y = 0.05;
                break;
            case 2: // Третий сверху малый
                x = -0.22;
                y = -0.13;
                break;
            case 3: // Самый нижний малый
                x = -0.22;
                y = -0.3;
                break;
            // 🔧 Большой слот: правый столбец, центр по Y
            case 4:
                x = 0.15;
                y = 0.0;
                break;
            default:
                return null;
        }

        // 🔧 Зеркалирование (как в рабочем коде)
        if (facing == Direction.SOUTH || facing == Direction.EAST || facing == Direction.NORTH) {
            x = -x;
        }

        return new double[]{x, y};
    }

    private void renderNote(PoseStack matrices, MultiBufferSource vertexConsumers,
                            int light, int overlay, boolean mirror, boolean isSmall) {

        ResourceLocation texture = isSmall ? SMALL_NOTE_TEXTURE : NOTE_TEXTURE;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose entry = matrices.last();
        float nx = 0, ny = 0, nz = 1;

        if (!mirror) {
            consumer.vertex(entry.pose(), -0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255).uv(0, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255).uv(1, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255).uv(1, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), -0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255).uv(0, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
        } else {
            consumer.vertex(entry.pose(), -0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255).uv(1, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, -0.5f, 0)
                    .color(255, 255, 255, 255).uv(0, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255).uv(0, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), -0.5f, 0.5f, 0)
                    .color(255, 255, 255, 255).uv(1, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
        }
    }

    private void renderBadge(PoseStack matrices, MultiBufferSource vertexConsumers,
                             int light, int overlay, Direction facing, int color, boolean isSmall) {

        if (color == -1) return;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(BADGE_TEXTURE));
        BadgeConfig config = isSmall ? SMALL_BADGE_CONFIGS.get(facing) : BADGE_CONFIGS.get(facing);
        if (config == null) return;

        matrices.pushPose();
        matrices.translate(config.x, config.y, config.z);
        matrices.scale(config.scale, config.scale, 1);

        PoseStack.Pose entry = matrices.last();
        float nx = 0, ny = 0, nz = 1;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (!config.mirror) {
            consumer.vertex(entry.pose(), -0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f).uv(0, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f).uv(1, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f).uv(1, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), -0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f).uv(0, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
        } else {
            consumer.vertex(entry.pose(), -0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f).uv(1, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, -0.5f, 0)
                    .color(r, g, b, 1.0f).uv(0, 1).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), 0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f).uv(0, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
            consumer.vertex(entry.pose(), -0.5f, 0.5f, 0)
                    .color(r, g, b, 1.0f).uv(1, 0).overlayCoords(overlay).uv2(light)
                    .normal(entry.normal(), nx, ny, nz).endVertex();
        }

        matrices.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BulletinBoardBlockEntity entity) {
        return true;
    }

    private static class BadgeConfig {
        final double x, y, z;
        final boolean mirror;
        final float scale;
        BadgeConfig(double x, double y, double z, boolean mirror, float scale) {
            this.x = x; this.y = y; this.z = z; this.mirror = mirror; this.scale = scale;
        }
    }
}
