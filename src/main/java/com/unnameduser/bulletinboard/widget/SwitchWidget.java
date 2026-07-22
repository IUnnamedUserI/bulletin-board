package com.unnameduser.bulletinboard.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class SwitchWidget extends ClickableWidget {
    private boolean state = false;
    private final Text label;
    private final Runnable onToggle;
    private float animProgress = 0.0f;

    private static final int SWITCH_WIDTH = 30;
    private static final int SWITCH_HEIGHT = 16;
    private static final int HANDLE_SIZE = 10;
    private static final int HANDLE_OFFSET = 2;
    private static final int RADIUS = 8;
    private static final int BORDER_COLOR_OFF = 0xFF888888;
    private static final int BORDER_COLOR_ON = 0xFF55AAFF;
    private static final int BG_COLOR = 0xFF1A1A1A;
    private static final int HANDLE_COLOR_OFF = 0xFF888888;
    private static final int HANDLE_COLOR_ON = 0xFF55AAFF;
    private static final int HANDLE_SHADOW = 0xCC555555;
    private static final float ANIMATION_SPEED = 0.12f;

    public SwitchWidget(int x, int y, Text label, Runnable onToggle) {
        super(x, y, SWITCH_WIDTH + 40, SWITCH_HEIGHT, Text.empty());
        this.label = label;
        this.onToggle = onToggle;
    }

    public boolean getState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
        this.animProgress = state ? 1.0f : 0.0f;
    }

    @Override
    public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();

        float target = state ? 1.0f : 0.0f;
        if (animProgress < target) {
            animProgress = Math.min(animProgress + ANIMATION_SPEED, 1.0f);
        } else if (animProgress > target) {
            animProgress = Math.max(animProgress - ANIMATION_SPEED, 0.0f);
        }

        drawRoundRect(context, x, y, SWITCH_WIDTH, SWITCH_HEIGHT, RADIUS, BG_COLOR);

        int borderColor = state ? BORDER_COLOR_ON : BORDER_COLOR_OFF;
        drawRoundRectBorder(context, x, y, SWITCH_WIDTH, SWITCH_HEIGHT, RADIUS, borderColor, 1);

        int maxOffset = SWITCH_WIDTH - HANDLE_SIZE - HANDLE_OFFSET * 2 - 1;
        int handleX = x + HANDLE_OFFSET + (int) (maxOffset * animProgress);
        int handleY = y + (SWITCH_HEIGHT - HANDLE_SIZE) / 2 - 1;
        int handleColor = state ? HANDLE_COLOR_ON : HANDLE_COLOR_OFF;

        drawCircle(context, handleX + HANDLE_SIZE / 2 + 1, handleY + HANDLE_SIZE / 2 + 1, HANDLE_SIZE / 2, HANDLE_SHADOW);
        drawCircle(context, handleX + HANDLE_SIZE / 2, handleY + HANDLE_SIZE / 2, HANDLE_SIZE / 2, handleColor);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int labelX = x + SWITCH_WIDTH + 6;
        int labelY = y + (SWITCH_HEIGHT - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, label, labelX, labelY, 0xFFFFFFFF, false);
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
            state = !state;
            if (onToggle != null) {
                onToggle.run();
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

    private void drawCircle(DrawContext context, int cx, int cy, int r, int color) {
        for (int i = -r; i <= r; i++) {
            for (int j = -r; j <= r; j++) {
                if (i * i + j * j <= r * r) {
                    context.fill(cx + i, cy + j, cx + i + 1, cy + j + 1, color);
                }
            }
        }
    }

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE,
                Text.literal("Анонимность: " + (state ? "включена" : "выключена")));
    }
}