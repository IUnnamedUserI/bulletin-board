package com.unnameduser.bulletinboard.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.unnameduser.bulletinboard.block.PlacedNoteBlock;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class PlacedNoteBlockRenderer implements BlockEntityRenderer<PlacedNoteBlockEntity> {

    private static final ResourceLocation NOTE_TEXTURE = new ResourceLocation("bulletin-board", "textures/block/note_paper.png");
    private static final ResourceLocation SMALL_NOTE_TEXTURE = new ResourceLocation("bulletin-board", "textures/block/small_note_paper.png");

    public PlacedNoteBlockRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(PlacedNoteBlockEntity entity, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {

        NoteData note = entity.getNoteData();
        if (note == null) return;

        var state = entity.getBlockState();
        if (!(state.getBlock() instanceof PlacedNoteBlock)) return;

        int rotation = state.getValue(PlacedNoteBlock.ROTATION);

        matrices.pushPose();

        // Центрируем на блоке
        matrices.translate(0.5, 0.0, 0.5);

        // Поворот по 8 позициям
        matrices.mulPose(Axis.YP.rotationDegrees(rotation * 45.0f));

        // Поднимаем на 1 пиксель над блоком
        matrices.translate(0, 0.001, 0);

        // Масштаб: 0.3 по X и Z
        float scale = 0.2f;
        matrices.scale(scale, 1, scale);

        // Определяем, нужно ли зеркалить UV
        boolean mirrorUV = (rotation == 0 || rotation == 6);

        // Рендер записки
        // Рендер записки
        ResourceLocation texture = note.isSmall() ? SMALL_NOTE_TEXTURE : NOTE_TEXTURE;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(texture));

        PoseStack.Pose entry = matrices.last();
        float nx = 0, ny = 1, nz = 0;

        // Отзеркаленная текстура (поворот на 180°)
        consumer.vertex(entry.pose(), -1.0f, 0, -1.0f)
                .color(255, 255, 255, 255).uv(1, 1).overlayCoords(overlay).uv2(light)
                .normal(entry.normal(), nx, ny, nz).endVertex();
        consumer.vertex(entry.pose(), 1.0f, 0, -1.0f)
                .color(255, 255, 255, 255).uv(0, 1).overlayCoords(overlay).uv2(light)
                .normal(entry.normal(), nx, ny, nz).endVertex();
        consumer.vertex(entry.pose(), 1.0f, 0, 1.0f)
                .color(255, 255, 255, 255).uv(0, 0).overlayCoords(overlay).uv2(light)
                .normal(entry.normal(), nx, ny, nz).endVertex();
        consumer.vertex(entry.pose(), -1.0f, 0, 1.0f)
                .color(255, 255, 255, 255).uv(1, 0).overlayCoords(overlay).uv2(light)
                .normal(entry.normal(), nx, ny, nz).endVertex();

        // Рендер печати
        // Рендер печати
        if (note.getTagColor() != -1) {
            ResourceLocation badgeTexture = new ResourceLocation("bulletin-board", "textures/block/badge.png");
            VertexConsumer badgeConsumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(badgeTexture));

            matrices.pushPose();

            // Позиция печати (в пределах записки)
            // Записка теперь scale = 0.3, значит её размер в блоках = 0.6
            // Смещаем печать в правый нижний угол записки
            float badgeX = 0.4f;
            float badgeZ = note.isSmall() ? -0.25f : -0.7f;

            matrices.translate(badgeX, 0.001f, badgeZ);

            // Масштаб печати
            float badgeScale = note.isSmall() ? 0.2f : 0.25f;
            matrices.scale(badgeScale, 1, badgeScale);

            PoseStack.Pose badgeEntry = matrices.last();
            float r = ((note.getTagColor() >> 16) & 0xFF) / 255f;
            float g = ((note.getTagColor() >> 8) & 0xFF) / 255f;
            float b = (note.getTagColor() & 0xFF) / 255f;

            // Отзеркаленная текстура (поворот на 180°)
            badgeConsumer.vertex(badgeEntry.pose(), -1.0f, 0, -1.0f)
                    .color(r, g, b, 1.0f).uv(1, 1).overlayCoords(overlay).uv2(light)
                    .normal(badgeEntry.normal(), nx, ny, nz).endVertex();
            badgeConsumer.vertex(badgeEntry.pose(), 1.0f, 0, -1.0f)
                    .color(r, g, b, 1.0f).uv(0, 1).overlayCoords(overlay).uv2(light)
                    .normal(badgeEntry.normal(), nx, ny, nz).endVertex();
            badgeConsumer.vertex(badgeEntry.pose(), 1.0f, 0, 1.0f)
                    .color(r, g, b, 1.0f).uv(0, 0).overlayCoords(overlay).uv2(light)
                    .normal(badgeEntry.normal(), nx, ny, nz).endVertex();
            badgeConsumer.vertex(badgeEntry.pose(), -1.0f, 0, 1.0f)
                    .color(r, g, b, 1.0f).uv(1, 0).overlayCoords(overlay).uv2(light)
                    .normal(badgeEntry.normal(), nx, ny, nz).endVertex();

            matrices.popPose();
        }

        matrices.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(PlacedNoteBlockEntity entity) {
        return false;
    }
}
