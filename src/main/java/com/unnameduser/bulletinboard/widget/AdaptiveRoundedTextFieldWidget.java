package com.unnameduser.bulletinboard.widget;

import net.minecraft.text.Text;

import java.util.List;

public class AdaptiveRoundedTextFieldWidget extends RoundedTextFieldWidget {
    private final int minHeight;
    private final int maxHeight;
    private final int lineHeight = 10;

    public AdaptiveRoundedTextFieldWidget(int x, int y, int width, int minHeight, int maxHeight, int maxLength, Text placeholder) {
        super(x, y, width, minHeight, maxLength, placeholder);
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        boolean result = super.charTyped(chr, modifiers);
        if (result) {
            updateHeight();
        }
        return result;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
        if (result) {
            updateHeight();
        }
        return result;
    }

    @Override
    public void setText(String text) {
        super.setText(text);
        updateHeight();
    }

    protected void updateHeight() {
        String currentText = getText();
        int maxWidth = getWidth() - PADDING * 2;

        List<String> lines = wrapText(currentText, maxWidth);
        int lineCount = Math.max(1, lines.size());

        int newHeight = Math.min(
                Math.max(minHeight, lineCount * lineHeight + PADDING + PADDING_BOTTOM),
                maxHeight
        );

        int oldHeight = this.height;
        int deltaY = oldHeight - newHeight;

        this.height = newHeight;

        if (deltaY != 0) {
            this.setY(this.getY() + deltaY);
        }
    }
}