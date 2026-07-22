package com.unnameduser.bulletinboard.screen;

import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;

public class NoteViewScreen extends Screen {
    private final NoteData note;
    private final BulletinBoardBlockEntity boardEntity;
    private final int notePosition;
    private static final int TEXT_COLOR = 0x3F3F3F;
    private static final int PARCHMENT_COLOR = 0xFFF5E6D3;
    private static final int BORDER_COLOR = 0xFF8B6B4D;
    private static final int TITLE_COLOR = 0xFF4A3C31;

    public NoteViewScreen(NoteData note) {
        this(note, null, -1);
    }

    public NoteViewScreen(NoteData note, BulletinBoardBlockEntity boardEntity, int notePosition) {
        super(Text.translatable("gui.bulletin-board.note_view.title"));
        this.note = note;
        this.boardEntity = boardEntity;
        this.notePosition = notePosition;
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

        boolean isSmall = note.isSmall();

        int parchmentX = this.width / 2 - 150;
        int parchmentY = this.height / 2 - 120;
        int parchmentWidth = 300;
        int parchmentHeight = isSmall ? 160 : 230;

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

        renderWrappedText(context, note.getContent(),
                parchmentX + 20, parchmentY + 45,
                parchmentWidth - 40, parchmentY + parchmentHeight - 50);

        context.drawText(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_view.from", note.getAuthor()),
                parchmentX + parchmentWidth - 100, parchmentY + parchmentHeight - 25,
                0xFF6B5E4A, false);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderWrappedText(DrawContext context, String text, int x, int y, int maxWidth, int maxY) {
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int currentY = y;

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int lineWidth = this.textRenderer.getWidth(testLine);

            if (lineWidth <= maxWidth) {
                if (currentLine.length() == 0) {
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            } else {
                if (currentY <= maxY) {
                    context.drawText(this.textRenderer,
                            Text.literal(currentLine.toString()),
                            x, currentY, TEXT_COLOR, false);
                    currentY += 12;
                }
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0 && currentY <= maxY) {
            context.drawText(this.textRenderer,
                    Text.literal(currentLine.toString()),
                    x, currentY, TEXT_COLOR, false);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}