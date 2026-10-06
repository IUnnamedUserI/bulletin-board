package com.unnameduser.bulletinboard.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.unnameduser.bulletinboard.config.ModConfig;
import com.unnameduser.bulletinboard.config.VillagerNameConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

public class VillagerNameRenderer {
    private static final float BASE_SCALE = 0.02f;

    public static void render(Villager villager, String nameKey, PoseStack matrices, MultiBufferSource vertexConsumers, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        if (!ModConfig.isShowVillagerNames()) {
            return;
        }

        if (!client.player.hasLineOfSight(villager)) return;

        double distance = client.player.distanceToSqr(villager);
        int radius = VillagerNameConfig.getDisplayRadius();
        if (distance > radius * radius) return;

        Font textRenderer = client.font;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        Vec3 pos = villager.getPosition(tickDelta).add(0, villager.getBbHeight() * 1.2, 0);

        matrices.pushPose();
        matrices.translate(
                pos.x - dispatcher.camera.getPosition().x,
                pos.y - dispatcher.camera.getPosition().y,
                pos.z - dispatcher.camera.getPosition().z
        );
        matrices.mulPose(dispatcher.camera.rotation());

        float scale = BASE_SCALE * 0.8f;
        matrices.scale(-scale, -scale, scale);

        // --- ИМЯ (переводим через Text.translatable) ---
        Component nameText = Component.translatable(nameKey);
        float nameWidth = textRenderer.width(nameText) / 2f;

        float nameSize = VillagerNameConfig.getNameSize() * 0.8f;
        matrices.pushPose();
        matrices.scale(nameSize, nameSize, nameSize);

        textRenderer.drawInBatch(
                nameText,
                -nameWidth,
                -6,
                VillagerNameConfig.getNameColor(),
                false,
                matrices.last().pose(),
                vertexConsumers,
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );

        matrices.popPose();

        // --- ПРОФЕССИЯ ---
        String professionRaw = BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).toString();
        String professionKey = "villager.profession." + professionRaw.replace("minecraft:", "");

        Component professionText = Component.translatable(professionKey);
        float professionWidth = textRenderer.width(professionText) / 2f;

        float professionSize = VillagerNameConfig.getProfessionSize() * 0.8f;
        matrices.pushPose();
        matrices.scale(professionSize, professionSize, professionSize);

        textRenderer.drawInBatch(
                professionText,
                -professionWidth,
                10,
                VillagerNameConfig.getProfessionColor(),
                false,
                matrices.last().pose(),
                vertexConsumers,
                Font.DisplayMode.NORMAL,
                0,
                15728880
        );

        matrices.popPose();
        matrices.popPose();
    }

    private static String capitalizeProfession(String profession) {
        String cleaned = profession.replace("minecraft:", "");
        String[] parts = cleaned.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.length() > 0) {
                result.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}
