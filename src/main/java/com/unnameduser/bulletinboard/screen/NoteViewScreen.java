package com.unnameduser.bulletinboard.screen;

import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class NoteViewScreen extends Screen {
    private final NoteData note;
    private final BulletinBoardBlockEntity boardEntity;
    private final int notePosition;
    private static final int TEXT_COLOR = 0x3F3F3F;
    private static final int PARCHMENT_COLOR = 0xFFF5E6D3;
    private static final int BORDER_COLOR = 0xFF8B6B4D;
    private static final int TITLE_COLOR = 0xFF4A3C31;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private List<String> wrappedLines = new ArrayList<>();
    private int parchmentX, parchmentY, parchmentWidth, parchmentHeight;
    private int textStartY, textEndY;
    private boolean isSmall;

    public NoteViewScreen(NoteData note) {
        this(note, null, -1);
    }

    public NoteViewScreen(NoteData note, BulletinBoardBlockEntity boardEntity, int notePosition) {
        super(Text.translatable("gui.bulletin-board.note_view.title"));
        this.note = note;
        this.boardEntity = boardEntity;
        this.notePosition = notePosition;
        this.isSmall = note.isSmall();
    }

    @Override
    protected void init() {
        super.init();

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("gui.bulletin-board.note_view.close"),
                                button -> this.close()
                        )
                        .dimensions(this.width / 2 - 50, this.height - 40, 100, 20)
                        .build()
        );

        if (boardEntity != null && notePosition >= 0) {
            this.addDrawableChild(
                    ButtonWidget.builder(
                                    Text.translatable("gui.bulletin-board.note_view.take"),
                                    button -> this.takeNote()
                            )
                            .dimensions(this.width / 2 + 60, this.height - 40, 80, 20)
                            .build()
            );
        }

        parchmentWidth = 300;
        parchmentHeight = isSmall ? 160 : 230;
        parchmentX = this.width / 2 - parchmentWidth / 2;
        parchmentY = this.height / 2 - parchmentHeight / 2;
        textStartY = parchmentY + 57;
        textEndY = parchmentY + parchmentHeight - 50;

        wrapText(note.getContent());
        maxScroll = Math.max(0, wrappedLines.size() * 12 - (textEndY - textStartY));
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
    }

    private void wrapText(String text) {
        wrappedLines.clear();
        String[] paragraphs = text.split("\n", -1);

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                wrappedLines.add("");
                continue;
            }

            String[] words = paragraph.split(" ", -1);
            StringBuilder currentLine = new StringBuilder();
            int maxWidth = parchmentWidth - 40;

            for (String word : words) {
                if (word.isEmpty()) {
                    currentLine.append(" ");
                    continue;
                }

                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                int lineWidth = this.textRenderer.getWidth(testLine);

                if (lineWidth <= maxWidth) {
                    if (currentLine.length() == 0) {
                        currentLine = new StringBuilder(word);
                    } else {
                        currentLine.append(" ").append(word);
                    }
                } else {
                    wrappedLines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
            if (currentLine.length() > 0) {
                wrappedLines.add(currentLine.toString());
            }
        }
    }

    private void takeNote() {
        if (boardEntity != null && notePosition >= 0 && this.client != null) {
            com.unnameduser.bulletinboard.network.ModPackets.sendTakeNote(
                    boardEntity.getPos(), notePosition);
            this.close();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (boardEntity != null && notePosition >= 0) {
            NoteData currentNote = boardEntity.getNoteAtPosition(notePosition);
            if (currentNote == null || !currentNote.equals(note)) {
                this.close();
                return;
            }
        }

        this.renderBackground(context);

        context.fill(parchmentX, parchmentY,
                parchmentX + parchmentWidth, parchmentY + parchmentHeight,
                PARCHMENT_COLOR);
        context.drawBorder(parchmentX, parchmentY, parchmentWidth, parchmentHeight, BORDER_COLOR);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("§l" + note.getTitle()),
                this.width / 2, parchmentY + 20, TITLE_COLOR);

        context.fill(parchmentX + 20, parchmentY + 35,
                parchmentX + parchmentWidth - 20, parchmentY + 36,
                BORDER_COLOR);

        int textY = textStartY - scrollOffset;
        int visibleStart = Math.max(0, (parchmentY + 57 - textY) / 12);
        int visibleEnd = Math.min(wrappedLines.size(), (parchmentY + parchmentHeight - 50 - textY) / 12 + 1);

        for (int i = visibleStart; i < visibleEnd; i++) {
            int y = textY + i * 12;
            if (y >= parchmentY + 57 && y <= parchmentY + parchmentHeight - 50) {
                context.drawText(this.textRenderer,
                        Text.literal(wrappedLines.get(i)),
                        parchmentX + 20, y, TEXT_COLOR, false);
            }
        }

        context.drawText(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_view.from", note.getAuthor()),
                parchmentX + parchmentWidth - 100, parchmentY + parchmentHeight - 25,
                0xFF6B5E4A, false);

        // === СТРЕЛКИ ПРОКРУТКИ ===
        boolean canScrollUp = scrollOffset > 0;
        boolean canScrollDown = scrollOffset < maxScroll;

        if (canScrollUp) {
            int arrowY = parchmentY + 37;
            drawArrow(context, parchmentX + parchmentWidth / 2 - 8, arrowY, true, mouseX, mouseY);
        }

        if (canScrollDown) {
            int arrowY = parchmentY + parchmentHeight - 20;
            drawArrow(context, parchmentX + parchmentWidth / 2 - 8, arrowY, false, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawArrow(DrawContext context, int x, int y, boolean up, int mouseX, int mouseY) {
        int alpha = 150;
        int size = 16;

        boolean hovered = mouseX >= x && mouseX <= x + size &&
                mouseY >= y && mouseY <= y + size;

        if (hovered) {
            alpha = 255;
            if (up) {
                scrollOffset = Math.max(0, scrollOffset - 2);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 2);
            }
        }

        int color = (alpha << 24) | 0x888888;

        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int halfSize = 5;

        if (up) {
            for (int i = -halfSize; i <= halfSize; i++) {
                for (int j = -halfSize; j <= halfSize; j++) {
                    if (Math.abs(i) + Math.abs(j) <= halfSize && j <= 0) {
                        context.fill(centerX + i, centerY + j, centerX + i + 1, centerY + j + 1, color);
                    }
                }
            }
        } else {
            for (int i = -halfSize; i <= halfSize; i++) {
                for (int j = -halfSize; j <= halfSize; j++) {
                    if (Math.abs(i) + Math.abs(j) <= halfSize && j >= 0) {
                        context.fill(centerX + i, centerY + j, centerX + i + 1, centerY + j + 1, color);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (mouseX >= parchmentX && mouseX <= parchmentX + parchmentWidth &&
                mouseY >= parchmentY && mouseY <= parchmentY + parchmentHeight) {
            scrollOffset = MathHelper.clamp(scrollOffset - (int) (amount * 15), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}