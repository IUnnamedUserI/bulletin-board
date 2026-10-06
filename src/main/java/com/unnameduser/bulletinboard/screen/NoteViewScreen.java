package com.unnameduser.bulletinboard.screen;

import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.network.ModPacketsClient;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class NoteViewScreen extends Screen {
    private final NoteData note;
    private final BulletinBoardBlockEntity boardEntity;
    private final int notePosition;
    private final BlockPos placedPos;

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
        this(note, null, -1, null);
    }

    public NoteViewScreen(NoteData note, BulletinBoardBlockEntity boardEntity, int notePosition) {
        this(note, boardEntity, notePosition, null);
    }

    public NoteViewScreen(NoteData note, BlockPos placedPos) {
        this(note, null, -1, placedPos);
    }

    private NoteViewScreen(NoteData note, BulletinBoardBlockEntity boardEntity, int notePosition, BlockPos placedPos) {
        super(Component.translatable("gui.bulletin-board.note_view.title"));
        this.note = note;
        this.boardEntity = boardEntity;
        this.notePosition = notePosition;
        this.placedPos = placedPos;
        this.isSmall = note.isSmall();
    }

    @Override
    protected void init() {
        super.init();

        // Кнопка "Закрыть"
        this.addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.bulletin-board.note_view.close"),
                                button -> this.onClose()
                        )
                        .bounds(this.width / 2 - 50, this.height - 40, 100, 20)
                        .build()
        );

        // Кнопка "Забрать" для записки на доске
        if (boardEntity != null && notePosition >= 0) {
            this.addRenderableWidget(
                    Button.builder(
                                    Component.translatable("gui.bulletin-board.note_view.take"),
                                    button -> this.takeNote()
                            )
                            .bounds(this.width / 2 + 60, this.height - 40, 80, 20)
                            .build()
            );
        }

        // Кнопка "Забрать" для записки на блоке
        if (placedPos != null) {
            this.addRenderableWidget(
                    Button.builder(
                                    Component.translatable("gui.bulletin-board.note_view.take"),
                                    button -> this.takePlacedNote()
                            )
                            .bounds(this.width / 2 + 60, this.height - 40, 80, 20)
                            .build()
            );
        }

        parchmentWidth = 300;
        parchmentHeight = isSmall ? 160 : 230;
        parchmentX = this.width / 2 - parchmentWidth / 2;
        parchmentY = this.height / 2 - parchmentHeight / 2;
        textStartY = parchmentY + 57;
        textEndY = parchmentY + parchmentHeight - 50;

        wrapText(note.getTranslatedContent());
        maxScroll = Math.max(0, wrappedLines.size() * 12 - (textEndY - textStartY));
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);
    }

    private void wrapText(String text) {
        wrappedLines.clear();
        String[] paragraphs = text.split("\n", -1);
        int maxWidth = parchmentWidth - 40;

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                wrappedLines.add("");
                continue;
            }

            String[] words = paragraph.split(" ", -1);
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                if (word.isEmpty()) {
                    // Пустое слово = лишний пробел, добавляем как есть
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    continue;
                }

                int wordWidth = this.font.width(word);

                // СЛОВО ШИРЕ МАКСИМАЛЬНОЙ ДОПУСТИМОЙ ШИРИНЫ — разбиваем посимвольно
                if (wordWidth > maxWidth) {
                    // Сначала сохраняем текущую накопленную строку (если есть)
                    if (currentLine.length() > 0) {
                        wrappedLines.add(currentLine.toString());
                        currentLine.setLength(0);
                    }

                    // Разбиваем длинное слово на части по пикселям
                    StringBuilder chunk = new StringBuilder();
                    for (int i = 0; i < word.length(); i++) {
                        char c = word.charAt(i);
                        String testChunk = chunk.toString() + c;
                        if (this.font.width(testChunk) > maxWidth) {
                            // Текущий кусок заполнен, сохраняем и начинаем новый
                            wrappedLines.add(chunk.toString());
                            chunk.setLength(0);
                        }
                        chunk.append(c);
                    }
                    // Остаток слова становится началом следующей строки
                    currentLine = chunk;
                    continue;
                }

                // Обычная логика: слово помещается или нет
                String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
                int lineWidth = this.font.width(testLine);

                if (lineWidth <= maxWidth) {
                    if (currentLine.length() == 0) {
                        currentLine = new StringBuilder(word);
                    } else {
                        currentLine.append(" ").append(word);
                    }
                } else {
                    // Слово не влезает в текущую строку, но само по себе короче maxWidth
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
        if (boardEntity != null && notePosition >= 0 && this.minecraft != null) {
            ModPacketsClient.sendTakeNote(boardEntity.getBlockPos(), notePosition);
            this.onClose();
        }
    }

    private void takePlacedNote() {
        if (placedPos != null && this.minecraft != null) {
            ModPacketsClient.sendTakePlacedNote(placedPos);
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Проверка актуальности записки на доске
        if (boardEntity != null && notePosition >= 0) {
            NoteData currentNote = boardEntity.getNoteAtPosition(notePosition);
            if (currentNote == null || !currentNote.equals(note)) {
                this.onClose();
                return;
            }
        }

        this.renderBackground(context);

        context.fill(parchmentX, parchmentY,
                parchmentX + parchmentWidth, parchmentY + parchmentHeight,
                PARCHMENT_COLOR);
        context.renderOutline(parchmentX, parchmentY, parchmentWidth, parchmentHeight, BORDER_COLOR);

        // === ОТРИСОВКА ЗАГОЛОВКА С ГИБРИДНЫМ ПЕРЕНОСОМ ===
        String rawTitle = "§l" + note.getTranslatedTitle();
        int titleMaxWidth = parchmentWidth - 40;
        List<String> titleLines = new ArrayList<>();

        String[] words = rawTitle.split(" ", -1);
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                continue;
            }

            int wordWidth = this.font.width(word);

            // Слово шире максимальной ширины — разбиваем посимвольно
            if (wordWidth > titleMaxWidth) {
                if (currentLine.length() > 0) {
                    titleLines.add(currentLine.toString());
                    currentLine.setLength(0);
                }

                StringBuilder chunk = new StringBuilder();
                for (int i = 0; i < word.length(); i++) {
                    char c = word.charAt(i);
                    String testChunk = chunk.toString() + c;
                    if (this.font.width(testChunk) > titleMaxWidth) {
                        titleLines.add(chunk.toString());
                        chunk.setLength(0);
                    }
                    chunk.append(c);
                }
                currentLine = chunk;
                continue;
            }

            // Обычная логика: проверяем, влезает ли слово с пробелом
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            int lineWidth = this.font.width(testLine);

            if (lineWidth <= titleMaxWidth) {
                if (currentLine.length() == 0) {
                    currentLine = new StringBuilder(word);
                } else {
                    currentLine.append(" ").append(word);
                }
            } else {
                // Слово не влезает в текущую строку, но само короче maxWidth
                // Переносим целое слово на следующую строку
                titleLines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            titleLines.add(currentLine.toString());
        }

        // Отрисовываем каждую строку заголовка по центру
        int titleLineHeight = 12;
        int totalTitleHeight = titleLines.size() * titleLineHeight;
        int titleStartY = parchmentY + 20 - (totalTitleHeight / 2) + (titleLineHeight / 2);

        for (int i = 0; i < titleLines.size(); i++) {
            context.drawCenteredString(
                    this.font,
                    Component.literal(titleLines.get(i)),
                    this.width / 2,
                    titleStartY + i * titleLineHeight,
                    TITLE_COLOR
            );
        }

        // Разделительная линия под заголовком
        context.fill(parchmentX + 20, parchmentY + 35,
                parchmentX + parchmentWidth - 20, parchmentY + 36,
                BORDER_COLOR);

        // === ОТРИСОВКА КОНТЕНТА ===
        int textY = textStartY - scrollOffset;
        int visibleStart = Math.max(0, (parchmentY + 57 - textY) / 12);
        int visibleEnd = Math.min(wrappedLines.size(), (parchmentY + parchmentHeight - 50 - textY) / 12 + 1);

        for (int i = visibleStart; i < visibleEnd; i++) {
            int y = textY + i * 12;
            if (y >= parchmentY + 57 && y <= parchmentY + parchmentHeight - 50) {
                context.drawString(this.font,
                        Component.literal(wrappedLines.get(i)),
                        parchmentX + 20, y, TEXT_COLOR, false);
            }
        }

        // Автор
        Component authorText = Component.translatable("gui.bulletin-board.note_view.from",
                Component.translatable(note.getAuthor()));
        context.drawString(this.font,
                authorText,
                parchmentX + parchmentWidth - 100, parchmentY + parchmentHeight - 25,
                0xFF6B5E4A, false);

        // Стрелки прокрутки
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

    private void drawArrow(GuiGraphics context, int x, int y, boolean up, int mouseX, int mouseY) {
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
            scrollOffset = Mth.clamp(scrollOffset - (int) (amount * 15), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
