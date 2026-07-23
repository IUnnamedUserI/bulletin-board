package com.unnameduser.bulletinboard.renderer;

import com.unnameduser.bulletinboard.config.VillagerNameConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class VillagerNameRenderer {
    private static final float BASE_SCALE = 0.02f;

    public static void render(VillagerEntity villager, String name, MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (!client.player.canSee(villager)) return;

        double distance = client.player.squaredDistanceTo(villager);
        int radius = VillagerNameConfig.getDisplayRadius();
        if (distance > radius * radius) return;

        TextRenderer textRenderer = client.textRenderer;
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        Vec3d pos = villager.getLerpedPos(tickDelta).add(0, villager.getHeight() * 1.2, 0);

        matrices.push();
        matrices.translate(
                pos.x - dispatcher.camera.getPos().x,
                pos.y - dispatcher.camera.getPos().y,
                pos.z - dispatcher.camera.getPos().z
        );
        matrices.multiply(dispatcher.camera.getRotation());

        float scale = BASE_SCALE * 0.8f;
        matrices.scale(-scale, -scale, scale);

        // --- ИМЯ ---
        Text nameText = Text.literal(name);
        float nameWidth = textRenderer.getWidth(nameText) / 2f;

        float nameSize = VillagerNameConfig.getNameSize() * 0.8f;
        matrices.push();
        matrices.scale(nameSize, nameSize, nameSize);

        textRenderer.draw(
                nameText,
                -nameWidth,
                -6,
                VillagerNameConfig.getNameColor(),
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                15728880
        );

        matrices.pop();

        // --- ПРОФЕССИЯ ---
        String professionRaw = villager.getVillagerData().getProfession().toString();
        String profession = capitalizeProfession(professionRaw);

        Text professionText = Text.literal(profession);
        float professionWidth = textRenderer.getWidth(professionText) / 2f;

        float professionSize = VillagerNameConfig.getProfessionSize() * 0.8f;
        matrices.push();
        matrices.scale(professionSize, professionSize, professionSize);

        textRenderer.draw(
                professionText,
                -professionWidth,
                10,
                VillagerNameConfig.getProfessionColor(),
                false,
                matrices.peek().getPositionMatrix(),
                vertexConsumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                15728880
        );

        matrices.pop();
        matrices.pop();
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