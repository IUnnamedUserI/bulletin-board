package com.unnameduser.bulletinboard.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class RoundedButtonWidget extends ClickableWidget {
    private final Runnable onPress;
    private final int hoverColor;
    private boolean isHovered = false;
    private float glowAlpha = 0.0f;

    private static final int MIN_WIDTH = 95;
    private static final int HEIGHT = 24;
    private static final int RADIUS = 8;
    private static final int PADDING = 16;
    private static final int BG_COLOR = 0xFF1A1A1A;
    private static final int BORDER_COLOR = 0xFF888888;

    public RoundedButtonWidget(int x, int y, Text message, Runnable onPress, int hoverColor) {
        super(x, y, calculateWidth(message), HEIGHT, message);
        this.onPress = onPress;
        this.hoverColor = hoverColor;
    }

    public static int calculateWidth(Text message) {
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textWidth = textRenderer.getWidth(message);
        return Math.max(MIN_WIDTH, textWidth + PADDING * 2);
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        isHovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;

        float target = isHovered ? 1.0f : 0.0f;
        if (glowAlpha < target) {
            glowAlpha = Math.min(glowAlpha + 0.08f, 1.0f);
        } else if (glowAlpha > target) {
            glowAlpha = Math.max(glowAlpha - 0.08f, 0.0f);
        }

        drawRoundRect(context, x, y, w, h, RADIUS, BG_COLOR);

        if (glowAlpha > 0.01f) {
            drawGlow(context, x, y, w, h, RADIUS, hoverColor);
        }

        int borderColor = isHovered ? hoverColor : BORDER_COLOR;
        drawRoundRectBorder(context, x, y, w, h, RADIUS, borderColor, 1);

        int color = isHovered ? hoverColor : 0xFFFFFFFF;
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        Text message = getMessage();
        int textX = x + (w - textRenderer.getWidth(message)) / 2;
        int textY = y + (h - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, message, textX, textY, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        if (mouseX >= x && mouseX <= x + w &&
                mouseY >= y && mouseY <= y + h) {
            if (onPress != null) {
                onPress.run();
            }
            return true;
        }
        return false;
    }

    private void drawRoundRect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        drawCornerFilled(context, x + w - r, y + h - r, r, color, 0);
        drawCornerFilled(context, x, y + h - r, r, color, 1);
        drawCornerFilled(context, x + w - r, y, r, color, 2);
        drawCornerFilled(context, x, y, r, color, 3);
        context.fill(x + r - 1, y, x + w - r + 1, y + h, color);
        context.fill(x, y + r - 1, x + w, y + h - r + 1, color);
    }

    private void drawCornerFilled(DrawContext context, int x, int y, int r, int color, int corner) {
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                if (i * i + j * j <= r * r) {
                    int px, py;
                    switch (corner) {
                        case 0: px = x + i; py = y + j; break;
                        case 1: px = x + (r - 1 - i); py = y + j; break;
                        case 2: px = x + i; py = y + (r - 1 - j); break;
                        case 3: px = x + (r - 1 - i); py = y + (r - 1 - j); break;
                        default: return;
                    }
                    context.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }

    private void drawRoundRectBorder(DrawContext context, int x, int y, int w, int h, int r, int color, int thickness) {
        drawCornerBorder(context, x + w - r, y + h - r, r, color, thickness, 0);
        drawCornerBorder(context, x, y + h - r, r, color, thickness, 1);
        drawCornerBorder(context, x + w - r, y, r, color, thickness, 2);
        drawCornerBorder(context, x, y, r, color, thickness, 3);
        context.fill(x + r - 1, y, x + w - r + 1, y + thickness, color);
        context.fill(x + r - 1, y + h - thickness, x + w - r + 1, y + h, color);
        context.fill(x, y + r - 1, x + thickness, y + h - r + 1, color);
        context.fill(x + w - thickness, y + r - 1, x + w, y + h - r + 1, color);
    }

    private void drawCornerBorder(DrawContext context, int x, int y, int r, int color, int thickness, int corner) {
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < r; j++) {
                double dist = Math.sqrt(i * i + j * j);
                if (dist <= r && dist > r - thickness) {
                    int px, py;
                    switch (corner) {
                        case 0: px = x + i; py = y + j; break;
                        case 1: px = x + (r - 1 - i); py = y + j; break;
                        case 2: px = x + i; py = y + (r - 1 - j); break;
                        case 3: px = x + (r - 1 - i); py = y + (r - 1 - j); break;
                        default: return;
                    }
                    context.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }

    private void drawGlow(DrawContext context, int x, int y, int w, int h, int r, int color) {
        int layers = 6;
        for (int i = 0; i < layers; i++) {
            float alpha = glowAlpha * (1.0f - (float) i / layers) * 0.3f;
            int alphaInt = (int) (alpha * 255);
            int glowColor = (alphaInt << 24) | (color & 0x00FFFFFF);
            int offset = i / 2;
            drawRoundRectBorder(context, x - offset, y - offset, w + offset * 2, h + offset * 2, r + offset, glowColor, 1);
        }
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE, getMessage());
    }
}