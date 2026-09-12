package com.unnameduser.bulletinboard.renderer;

import com.unnameduser.bulletinboard.block.PlacedNoteBlock;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class PlacedNoteBlockRenderer implements BlockEntityRenderer<PlacedNoteBlockEntity> {

    private static final Identifier NOTE_TEXTURE = new Identifier("bulletin-board", "textures/block/note_paper.png");
    private static final Identifier SMALL_NOTE_TEXTURE = new Identifier("bulletin-board", "textures/block/small_note_paper.png");

    public PlacedNoteBlockRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(PlacedNoteBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        NoteData note = entity.getNoteData();
        if (note == null) return;

        var state = entity.getCachedState();
        if (!(state.getBlock() instanceof PlacedNoteBlock)) return;

        int rotation = state.get(PlacedNoteBlock.ROTATION);

        matrices.push();

        // Центрируем на блоке
        matrices.translate(0.5, 0.0, 0.5);

        // Поворот по 8 позициям
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation * 45.0f));

        // Поднимаем на 1 пиксель над блоком
        matrices.translate(0, 0.001, 0);

        // Масштаб: 0.3 по X и Z
        float scale = 0.2f;
        matrices.scale(scale, 1, scale);

        // Определяем, нужно ли зеркалить UV
        boolean mirrorUV = (rotation == 0 || rotation == 6);

        // Рендер записки
        // Рендер записки
        Identifier texture = note.isSmall() ? SMALL_NOTE_TEXTURE : NOTE_TEXTURE;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));

        MatrixStack.Entry entry = matrices.peek();
        float nx = 0, ny = 1, nz = 0;

        // Отзеркаленная текстура (поворот на 180°)
        consumer.vertex(entry.getPositionMatrix(), -1.0f, 0, -1.0f)
                .color(255, 255, 255, 255).texture(1, 1).overlay(overlay).light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz).next();
        consumer.vertex(entry.getPositionMatrix(), 1.0f, 0, -1.0f)
                .color(255, 255, 255, 255).texture(0, 1).overlay(overlay).light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz).next();
        consumer.vertex(entry.getPositionMatrix(), 1.0f, 0, 1.0f)
                .color(255, 255, 255, 255).texture(0, 0).overlay(overlay).light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz).next();
        consumer.vertex(entry.getPositionMatrix(), -1.0f, 0, 1.0f)
                .color(255, 255, 255, 255).texture(1, 0).overlay(overlay).light(light)
                .normal(entry.getNormalMatrix(), nx, ny, nz).next();

        // Рендер печати
        // Рендер печати
        if (note.getTagColor() != -1) {
            Identifier badgeTexture = new Identifier("bulletin-board", "textures/block/badge.png");
            VertexConsumer badgeConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(badgeTexture));

            matrices.push();

            // Позиция печати (в пределах записки)
            // Записка теперь scale = 0.3, значит её размер в блоках = 0.6
            // Смещаем печать в правый нижний угол записки
            float badgeX = 0.4f;
            float badgeZ = note.isSmall() ? -0.25f : -0.7f;

            matrices.translate(badgeX, 0.001f, badgeZ);

            // Масштаб печати
            float badgeScale = note.isSmall() ? 0.2f : 0.25f;
            matrices.scale(badgeScale, 1, badgeScale);

            MatrixStack.Entry badgeEntry = matrices.peek();
            float r = ((note.getTagColor() >> 16) & 0xFF) / 255f;
            float g = ((note.getTagColor() >> 8) & 0xFF) / 255f;
            float b = (note.getTagColor() & 0xFF) / 255f;

            // Отзеркаленная текстура (поворот на 180°)
            badgeConsumer.vertex(badgeEntry.getPositionMatrix(), -1.0f, 0, -1.0f)
                    .color(r, g, b, 1.0f).texture(1, 1).overlay(overlay).light(light)
                    .normal(badgeEntry.getNormalMatrix(), nx, ny, nz).next();
            badgeConsumer.vertex(badgeEntry.getPositionMatrix(), 1.0f, 0, -1.0f)
                    .color(r, g, b, 1.0f).texture(0, 1).overlay(overlay).light(light)
                    .normal(badgeEntry.getNormalMatrix(), nx, ny, nz).next();
            badgeConsumer.vertex(badgeEntry.getPositionMatrix(), 1.0f, 0, 1.0f)
                    .color(r, g, b, 1.0f).texture(0, 0).overlay(overlay).light(light)
                    .normal(badgeEntry.getNormalMatrix(), nx, ny, nz).next();
            badgeConsumer.vertex(badgeEntry.getPositionMatrix(), -1.0f, 0, 1.0f)
                    .color(r, g, b, 1.0f).texture(1, 0).overlay(overlay).light(light)
                    .normal(badgeEntry.getNormalMatrix(), nx, ny, nz).next();

            matrices.pop();
        }

        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox(PlacedNoteBlockEntity entity) {
        return false;
    }
}