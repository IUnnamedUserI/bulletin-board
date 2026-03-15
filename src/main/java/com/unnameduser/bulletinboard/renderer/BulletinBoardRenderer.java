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

    // Конфигурация для каждого направления
    private static final Map<Direction, BadgeConfig> BADGE_CONFIGS = new EnumMap<>(Direction.class);

    static {
        // Настройки для значка: x, y, z, mirror, scale. Стандартные значения - (0.15, 0.25, 0.01)
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

        // Центрируем на блоке
        matrices.translate(0.5, 0.5, 0.5); // Стандартные значения - (0.5, 0.5, 0.5)

        // Поворачиваем и позиционируем в зависимости от направления
        switch (facing) {
            case NORTH:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
                matrices.translate(0, 0, -0.45);
                break;

            case SOUTH:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0));
                matrices.translate(0, 0, -0.45);
                // Зеркалим по X
                matrices.scale(-1, 1, 1);
                break;

            case WEST:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90));
                matrices.translate(0, 0, 0.45);
                break;

            case EAST:
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90));
                matrices.translate(0, 0, 0.45);
                // Зеркалим по X
                matrices.scale(-1, 1, 1);
                break;
        }

        // Определяем, нужно ли зеркалить текстуру
        boolean mirrorTexture = (facing == Direction.SOUTH || facing == Direction.WEST);

        // Рисуем все записки
        for (int i = 0; i < notes.size(); i++) {
            NoteData note = notes.get(i);
            int position = positions.get(i);
            double[] slotCoords = getSlotCoordinates(position, facing);

            matrices.push();
            matrices.translate(slotCoords[0], slotCoords[1], 0.01);

            float scale = 0.3f;
            if (position == 2) scale = 0.4f;
            matrices.scale(scale, scale, 1);

            // Рисуем саму записку
            renderNote(matrices, vertexConsumers, light, overlay, mirrorTexture);

            // Рисуем значок, если цвет не белый (0xFFFFFF)
            if (note.getTagColor() != -1) {
                renderBadge(matrices, vertexConsumers, light, overlay, facing, note.getTagColor());
            }

            matrices.pop();
        }

        matrices.pop();
    }

    private double[] getSlotCoordinates(int position, Direction facing) {
        // Базовые координаты
        double x, y;

        switch (position) {
            case 0: // Верхний левый
                x = -0.23;
                y = 0.1;
                break;
            case 1: // Нижний левый
                x = -0.23;
                y = -0.23;
                break;
            case 2: // Правый центральный
                x = 0.15;
                y = 0.0;
                break;
            default:
                return new double[]{0, 0};
        }

        // Корректировка для разных направлений
        switch (facing) {
            case SOUTH:
            case EAST:
                // Для южной и восточной сторон: зеркалим по X
                x = -x;
                break;
            case NORTH:
                x = -x;
                break;
            case WEST:
                x = -x;
                // Для северной и западной: оставляем как есть
                break;
            default:
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
            // Нормальное отображение
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
            // Зеркальное отображение
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

        // Получаем конфигурацию для текущего направления
        BadgeConfig config = BADGE_CONFIGS.get(facing);
        if (config == null) return;

        matrices.push();

        // Применяем индивидуальные настройки
        matrices.translate(config.x, config.y, config.z);
        matrices.scale(config.scale, config.scale, 1);

        MatrixStack.Entry entry = matrices.peek();
        float nx = 0, ny = 0, nz = 1;

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        if (!config.mirror) {
            // Нормальное отображение
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
            // Зеркальное отображение
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

    // Внутренний класс для хранения конфигурации значка
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