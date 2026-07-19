package com.unnameduser.bulletinboard.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class RoundedTextFieldWidget extends ClickableWidget {
    protected final TextRenderer textRenderer;
    protected String text = "";
    protected final int maxLength;
    protected final Text placeholder;
    protected boolean focused = false;
    protected int cursorPos = 0;
    protected int firstLineIndex = 0;

    // Настройки внешнего вида
    protected static final int RADIUS = 8;
    protected static final int BORDER_COLOR = 0xFF888888;
    protected static final int BORDER_COLOR_FOCUSED = 0xFF55AAFF;
    protected static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    protected static final int TEXT_COLOR = 0xFFFFFFFF;
    protected static final int PLACEHOLDER_COLOR = 0xFF888888;
    protected static final int LINE_HEIGHT = 10;
    protected static final int PADDING = 6;
    protected static final int PADDING_BOTTOM = 14;

    // Анимация свечения
    private float glowAlpha = 0.0f;

    public RoundedTextFieldWidget(int x, int y, int width, int height, int maxLength, Text placeholder) {
        super(x, y, width, height, Text.empty());
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        this.maxLength = maxLength;
        this.placeholder = placeholder;
        this.cursorPos = 0;
    }

    // ============ ГЕТТЕРЫ И СЕТТЕРЫ ============

    public String getText() { return text; }
    public void setText(String text) {
        this.text = text != null ? text : "";
        this.cursorPos = MathHelper.clamp(cursorPos, 0, this.text.length());
    }
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) { this.focused = focused; }
    public void setCursorPos(int pos) {
        this.cursorPos = MathHelper.clamp(pos, 0, text.length());
    }

    // ============ МЕТОДЫ ДЛЯ NARRATOR ============

    @Override
    protected void appendClickableNarrations(net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
        builder.put(net.minecraft.client.gui.screen.narration.NarrationPart.TITLE,
                Text.literal("Текстовое поле: " + text));
    }

    @Override
    public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        this.render(context, mouseX, mouseY, delta);
    }

    // ============ АНИМАЦИЯ СВЕЧЕНИЯ ============
    public void tick() {
        if (focused && glowAlpha < 1.0f) {
            glowAlpha += 0.08f;
        } else if (!focused && glowAlpha > 0.0f) {
            glowAlpha -= 0.08f;
        }
        glowAlpha = MathHelper.clamp(glowAlpha, 0.0f, 1.0f);
    }

    private void drawGlow(DrawContext context, int x, int y, int w, int h, int r) {
        if (glowAlpha < 0.01f) return;

        int color = 0x55AAFF;
        int layers = 6;

        for (int i = 0; i < layers; i++) {
            float alpha = glowAlpha * (1.0f - (float) i / layers) * 0.3f;
            int alphaInt = (int) (alpha * 255);
            int glowColor = (alphaInt << 24) | (color & 0x00FFFFFF);
            int offset = i / 2;

            drawRoundRectBorder(
                    context,
                    x - offset,
                    y - offset,
                    w + offset * 2,
                    h + offset * 2,
                    r + offset,
                    glowColor,
                    1
            );
        }
    }

    // ============ ОТРИСОВКА ============

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!this.visible) return;

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        // 1. Фон с закруглениями
        drawRoundRect(context, x, y, w, h, RADIUS, BACKGROUND_COLOR);

        // 2. Свечение (если есть фокус)
        if (focused) {
            drawGlow(context, x, y, w, h, RADIUS);
        }

        // 3. Граница
        int borderColor = focused ? BORDER_COLOR_FOCUSED : BORDER_COLOR;
        drawRoundRectBorder(context, x, y, w, h, RADIUS, borderColor, 1);

        // 4. Обрезка области для текста
        int textX = x + PADDING;
        int textY = y + PADDING;
        int maxTextWidth = w - PADDING * 2;
        int maxTextHeight = h - PADDING - PADDING_BOTTOM;

        // 5. Текст или плейсхолдер
        if (text.isEmpty() && !focused) {
            context.drawText(textRenderer, placeholder, textX, textY, PLACEHOLDER_COLOR, false);
        } else {
            List<String> lines = wrapText(text, maxTextWidth);
            if (lines.isEmpty()) lines.add("");

            int maxLines = maxTextHeight / LINE_HEIGHT;
            int maxScroll = Math.max(0, lines.size() - maxLines);
            firstLineIndex = MathHelper.clamp(firstLineIndex, 0, maxScroll);

            int startLine = firstLineIndex;
            int endLine = Math.min(lines.size(), startLine + maxLines);

            for (int i = startLine; i < endLine; i++) {
                int lineY = textY + (i - startLine) * LINE_HEIGHT;
                context.drawText(textRenderer, Text.literal(lines.get(i)), textX, lineY, TEXT_COLOR, false);
            }

            // 6. Курсор
            if (focused && (System.currentTimeMillis() / 500 % 2 == 0)) {
                int cursorLine = getLineIndexForCursor(lines);
                int posInLine = getPositionInLineForCursor(cursorLine, lines);
                String line = lines.get(cursorLine);
                String beforeCursor = line.substring(0, Math.min(posInLine, line.length()));

                int cursorX = textX + textRenderer.getWidth(beforeCursor);
                int cursorY = textY + (cursorLine - firstLineIndex) * LINE_HEIGHT;

                if (cursorX > textX + maxTextWidth) {
                    cursorX = textX + maxTextWidth;
                }

                if (cursorLine < firstLineIndex) {
                    firstLineIndex = cursorLine;
                } else if (cursorLine >= firstLineIndex + maxLines) {
                    firstLineIndex = cursorLine - maxLines + 1;
                }
                maxScroll = Math.max(0, lines.size() - maxLines);
                firstLineIndex = MathHelper.clamp(firstLineIndex, 0, maxScroll);

                cursorY = textY + (cursorLine - firstLineIndex) * LINE_HEIGHT;

                if (cursorLine >= firstLineIndex && cursorLine < firstLineIndex + maxLines) {
                    context.fill(cursorX, cursorY, cursorX + 1, cursorY + LINE_HEIGHT, 0xFFFFFFFF);
                }
            }
        }

        // 7. Счётчик символов
        String counter = text.length() + "/" + maxLength;
        int counterWidth = textRenderer.getWidth(counter);
        int counterX = x + w - PADDING - counterWidth;
        int counterY = y + h - PADDING_BOTTOM + 2;
        boolean isFull = text.length() >= maxLength;
        int counterColor = isFull ? 0xFFFF5555 : 0xFF888888;

        context.fill(
                counterX - 3,
                counterY - 1,
                counterX + counterWidth + 3,
                counterY + textRenderer.fontHeight + 1,
                0xCC1A1A1A
        );

        context.drawText(
                textRenderer,
                Text.literal(counter),
                counterX,
                counterY,
                counterColor,
                false
        );
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============

    protected List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] paragraphs = text.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }

            String[] words = paragraph.split(" ", -1);
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.isEmpty()) {
                    currentLine.append(" ");
                    continue;
                }

                if (textRenderer.getWidth(word) <= maxWidth) {
                    String testLine = currentLine + (currentLine.isEmpty() ? "" : " ") + word;
                    if (textRenderer.getWidth(testLine) <= maxWidth) {
                        if (!currentLine.isEmpty()) currentLine.append(" ");
                        currentLine.append(word);
                    } else {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder(word);
                    }
                } else {
                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                        currentLine = new StringBuilder();
                    }
                    for (int i = 0; i < word.length(); i++) {
                        char c = word.charAt(i);
                        String testLine = currentLine.toString() + c;
                        if (textRenderer.getWidth(testLine) <= maxWidth) {
                            currentLine.append(c);
                        } else {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder();
                            currentLine.append(c);
                        }
                    }
                }
            }
            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }
        }
        return lines;
    }

    private int getLineIndexForCursor(List<String> lines) {
        int charCount = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (cursorPos <= charCount + line.length()) {
                return i;
            }
            charCount += line.length() + 1;
        }
        return lines.size() - 1;
    }

    private int getPositionInLineForCursor(int lineIndex, List<String> lines) {
        int charCount = 0;
        for (int i = 0; i < lineIndex; i++) {
            charCount += lines.get(i).length() + 1;
        }
        return Math.min(cursorPos - charCount, lines.get(lineIndex).length());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) return false;
        focused = isInside(mouseX, mouseY);
        if (!focused) return false;

        int clickX = (int) (mouseX - getX() - PADDING);
        int clickY = (int) (mouseY - getY() - PADDING);

        List<String> lines = wrapText(text, getWidth() - PADDING * 2);
        int lineIndex = clickY / LINE_HEIGHT + firstLineIndex;
        if (lineIndex >= 0 && lineIndex < lines.size()) {
            String line = lines.get(lineIndex);
            int posInLine = 0;
            int bestWidth = 0;
            for (int i = 0; i <= line.length(); i++) {
                String sub = line.substring(0, i);
                int w = textRenderer.getWidth(sub);
                if (Math.abs(w - clickX) < Math.abs(bestWidth - clickX)) {
                    bestWidth = w;
                    posInLine = i;
                }
            }
            int charCount = 0;
            for (int i = 0; i < lineIndex; i++) {
                charCount += lines.get(i).length() + 1;
            }
            cursorPos = MathHelper.clamp(charCount + posInLine, 0, text.length());
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        boolean ctrlDown = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE:
                if (!text.isEmpty() && cursorPos > 0) {
                    if (shiftDown) {
                        text = "";
                        cursorPos = 0;
                    } else {
                        text = text.substring(0, cursorPos - 1) + text.substring(cursorPos);
                        cursorPos--;
                    }
                }
                return true;

            case GLFW.GLFW_KEY_DELETE:
                if (!text.isEmpty() && cursorPos < text.length()) {
                    text = text.substring(0, cursorPos) + text.substring(cursorPos + 1);
                }
                return true;

            case GLFW.GLFW_KEY_ENTER:
                if (text.length() < maxLength) {
                    text = text.substring(0, cursorPos) + "\n" + text.substring(cursorPos);
                    cursorPos++;
                }
                return true;

            case GLFW.GLFW_KEY_LEFT:
                if (ctrlDown) {
                    cursorPos = findPreviousWordBoundary(cursorPos);
                } else {
                    cursorPos = Math.max(0, cursorPos - 1);
                }
                return true;

            case GLFW.GLFW_KEY_RIGHT:
                if (ctrlDown) {
                    cursorPos = findNextWordBoundary(cursorPos);
                } else {
                    cursorPos = Math.min(text.length(), cursorPos + 1);
                }
                return true;

            case GLFW.GLFW_KEY_UP:
                moveCursorUp();
                return true;

            case GLFW.GLFW_KEY_DOWN:
                moveCursorDown();
                return true;

            case GLFW.GLFW_KEY_HOME:
                cursorPos = 0;
                return true;

            case GLFW.GLFW_KEY_END:
                cursorPos = text.length();
                return true;

            case GLFW.GLFW_KEY_A:
                if (ctrlDown) {
                    cursorPos = text.length();
                }
                return true;

            case GLFW.GLFW_KEY_V:
                if (ctrlDown) {
                    String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
                    if (clipboard != null) {
                        for (char c : clipboard.toCharArray()) {
                            if (text.length() < maxLength) {
                                text = text.substring(0, cursorPos) + c + text.substring(cursorPos);
                                cursorPos++;
                            } else break;
                        }
                    }
                    return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused || chr < 32 || text.length() >= maxLength) return false;
        text = text.substring(0, cursorPos) + chr + text.substring(cursorPos);
        cursorPos++;
        return true;
    }

    // ============ НАВИГАЦИЯ ============

    private void moveCursorUp() {
        List<String> lines = wrapText(text, getWidth() - PADDING * 2);
        if (lines.isEmpty()) return;

        int lineIndex = getLineIndexForCursor(lines);
        if (lineIndex > 0) {
            String prevLine = lines.get(lineIndex - 1);
            int posInLine = getPositionInLineForCursor(lineIndex, lines);
            int newPos = Math.min(posInLine, prevLine.length());

            int charCount = 0;
            for (int i = 0; i < lineIndex - 1; i++) {
                charCount += lines.get(i).length() + 1;
            }
            cursorPos = charCount + newPos;
        } else {
            cursorPos = 0;
        }
    }

    private void moveCursorDown() {
        List<String> lines = wrapText(text, getWidth() - PADDING * 2);
        if (lines.isEmpty()) return;

        int lineIndex = getLineIndexForCursor(lines);
        if (lineIndex < lines.size() - 1) {
            String nextLine = lines.get(lineIndex + 1);
            int posInLine = getPositionInLineForCursor(lineIndex, lines);
            int newPos = Math.min(posInLine, nextLine.length());

            int charCount = 0;
            for (int i = 0; i < lineIndex + 1; i++) {
                charCount += lines.get(i).length() + 1;
            }
            cursorPos = charCount + newPos;
        } else {
            cursorPos = text.length();
        }
    }

    private int findPreviousWordBoundary(int pos) {
        if (pos <= 0) return 0;
        int i = pos - 1;
        while (i > 0 && text.charAt(i) != ' ' && text.charAt(i) != '\n') i--;
        while (i > 0 && (text.charAt(i) == ' ' || text.charAt(i) == '\n')) i--;
        return Math.max(0, i + 1);
    }

    private int findNextWordBoundary(int pos) {
        if (pos >= text.length()) return text.length();
        int i = pos;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\n')) i++;
        while (i < text.length() && text.charAt(i) != ' ' && text.charAt(i) != '\n') i++;
        return Math.min(text.length(), i);
    }

    private boolean isInside(double x, double y) {
        return x >= getX() && x <= getX() + getWidth() &&
                y >= getY() && y <= getY() + getHeight();
    }

    // ============ МЕТОДЫ ОТРИСОВКИ ФИГУР ============

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
}