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

    private static final ResourceLocation NOTE_TEXTURE = ResourceLocation.fromNamespaceAndPath("bulletin-board", "textures/block/note_paper.png");
    private static final ResourceLocation SMALL_NOTE_TEXTURE = ResourceLocation.fromNamespaceAndPath("bulletin-board", "textures/block/small_note_paper.png");
    private static final ResourceLocation BADGE_TEXTURE = ResourceLocation.fromNamespaceAndPath("bulletin-board", "textures/block/badge.png");

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
        matrices.translate(0.5, 0.0, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(rotation * 45.0f));
        matrices.translate(0, 0.001, 0);

        float scale = 0.2f;
        matrices.scale(scale, 1, scale);

        ResourceLocation texture = note.isSmall() ? SMALL_NOTE_TEXTURE : NOTE_TEXTURE;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(texture));

        float nx = 0, ny = 1, nz = 0;

        // Вершина 1
        consumer.addVertex(-1.0f, 0, -1.0f);
        consumer.setColor(255, 255, 255, 255);
        consumer.setUv(1, 1);
        consumer.setOverlay(overlay);
        consumer.setLight(light);
        consumer.setNormal(nx, ny, nz);

        // Вершина 2
        consumer.addVertex(1.0f, 0, -1.0f);
        consumer.setColor(255, 255, 255, 255);
        consumer.setUv(0, 1);
        consumer.setOverlay(overlay);
        consumer.setLight(light);
        consumer.setNormal(nx, ny, nz);

        // Вершина 3
        consumer.addVertex(1.0f, 0, 1.0f);
        consumer.setColor(255, 255, 255, 255);
        consumer.setUv(0, 0);
        consumer.setOverlay(overlay);
        consumer.setLight(light);
        consumer.setNormal(nx, ny, nz);

        // Вершина 4
        consumer.addVertex(-1.0f, 0, 1.0f);
        consumer.setColor(255, 255, 255, 255);
        consumer.setUv(1, 0);
        consumer.setOverlay(overlay);
        consumer.setLight(light);
        consumer.setNormal(nx, ny, nz);

        if (note.getTagColor() != -1) {
            VertexConsumer badgeConsumer = vertexConsumers.getBuffer(RenderType.entityCutoutNoCull(BADGE_TEXTURE));

            matrices.pushPose();

            float badgeX = 0.4f;
            float badgeZ = note.isSmall() ? -0.25f : -0.7f;

            matrices.translate(badgeX, 0.001f, badgeZ);

            float badgeScale = note.isSmall() ? 0.2f : 0.25f;
            matrices.scale(badgeScale, 1, badgeScale);

            float r = ((note.getTagColor() >> 16) & 0xFF) / 255f;
            float g = ((note.getTagColor() >> 8) & 0xFF) / 255f;
            float b = (note.getTagColor() & 0xFF) / 255f;

            // Вершина 1
            badgeConsumer.addVertex(-1.0f, 0, -1.0f);
            badgeConsumer.setColor(r, g, b, 1.0f);
            badgeConsumer.setUv(1, 1);
            badgeConsumer.setOverlay(overlay);
            badgeConsumer.setLight(light);
            badgeConsumer.setNormal(nx, ny, nz);

            // Вершина 2
            badgeConsumer.addVertex(1.0f, 0, -1.0f);
            badgeConsumer.setColor(r, g, b, 1.0f);
            badgeConsumer.setUv(0, 1);
            badgeConsumer.setOverlay(overlay);
            badgeConsumer.setLight(light);
            badgeConsumer.setNormal(nx, ny, nz);

            // Вершина 3
            badgeConsumer.addVertex(1.0f, 0, 1.0f);
            badgeConsumer.setColor(r, g, b, 1.0f);
            badgeConsumer.setUv(0, 0);
            badgeConsumer.setOverlay(overlay);
            badgeConsumer.setLight(light);
            badgeConsumer.setNormal(nx, ny, nz);

            // Вершина 4
            badgeConsumer.addVertex(-1.0f, 0, 1.0f);
            badgeConsumer.setColor(r, g, b, 1.0f);
            badgeConsumer.setUv(1, 0);
            badgeConsumer.setOverlay(overlay);
            badgeConsumer.setLight(light);
            badgeConsumer.setNormal(nx, ny, nz);

            matrices.popPose();
        }

        matrices.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(PlacedNoteBlockEntity entity) {
        return false;
    }
}