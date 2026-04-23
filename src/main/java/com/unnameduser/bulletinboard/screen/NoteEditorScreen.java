package com.unnameduser.bulletinboard.screen;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class NoteEditorScreen extends Screen {
    private TextFieldWidget titleField;
    private TextFieldWidget contentField;
    private final ItemStack notePaper;
    private final boolean isSmall;

    private boolean isAnonymous = false;
    private ButtonWidget anonymousButton;

    private static final int TITLE_MAX_LENGTH = 40;
    private static final int CONTENT_MAX_LENGTH_NORMAL = 512;
    private static final int CONTENT_MAX_LENGTH_SMALL = 256;

    private final int contentMaxLength;

    public NoteEditorScreen(ItemStack notePaper) {
        super(Text.translatable("gui.bulletin-board.note_editor.title"));
        this.notePaper = notePaper;
        this.isSmall = notePaper.getItem() instanceof NotePaperItem noteItem && noteItem.isSmall();
        this.contentMaxLength = isSmall ? CONTENT_MAX_LENGTH_SMALL : CONTENT_MAX_LENGTH_NORMAL;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        this.titleField = new TextFieldWidget(
                this.textRenderer,
                centerX - 100,
                startY,
                200, 20,
                Text.translatable("gui.bulletin-board.note_editor.title_placeholder")
        );
        this.titleField.setMaxLength(TITLE_MAX_LENGTH);
        this.titleField.setPlaceholder(Text.translatable("gui.bulletin-board.note_editor.title_placeholder"));
        // ✅ Устанавливаем непрозрачный фон для текстового поля
        this.titleField.setDrawsBackground(true);
        this.addSelectableChild(this.titleField);

        this.contentField = new TextFieldWidget(
                this.textRenderer,
                centerX - 100,
                startY + 35,
                200, 20,
                Text.translatable("gui.bulletin-board.note_editor.content_placeholder")
        );
        this.contentField.setMaxLength(contentMaxLength);
        this.contentField.setPlaceholder(Text.translatable("gui.bulletin-board.note_editor.content_placeholder"));
        // ✅ Устанавливаем непрозрачный фон для текстового поля
        this.contentField.setDrawsBackground(true);
        this.addSelectableChild(this.contentField);

        this.anonymousButton = ButtonWidget.builder(
                        getAnonymousButtonText(),
                        button -> {
                            isAnonymous = !isAnonymous;
                            anonymousButton.setMessage(getAnonymousButtonText());
                        }
                )
                .dimensions(centerX - 100, startY + 60, 200, 20)
                .build();
        this.addDrawableChild(this.anonymousButton);

        this.setInitialFocus(this.titleField);

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("gui.bulletin-board.note_editor.save"),
                                button -> this.saveNote()
                        )
                        .dimensions(centerX - 100, startY + 90, 95, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(
                                Text.translatable("gui.bulletin-board.note_editor.cancel"),
                                button -> this.close()
                        )
                        .dimensions(centerX + 5, startY + 90, 95, 20)
                        .build()
        );
    }

    private Text getAnonymousButtonText() {
        String checkBox = isAnonymous ? "[✓]" : "[ ]";
        return Text.literal(checkBox + " " +
                Text.translatable("gui.bulletin-board.note_editor.anonymous").getString());
    }

    private void saveNote() {
        String title = titleField.getText().trim();
        String content = contentField.getText().trim();

        if (!title.isEmpty() && !content.isEmpty()) {
            String author = isAnonymous ?
                    Text.translatable("gui.bulletin-board.note_editor.anonymous_name").getString() :
                    (this.client != null && this.client.player != null ? this.client.player.getName().getString() : "Unknown");

            NoteData note = new NoteData(title, content, author, -1, System.currentTimeMillis(), false, isSmall);

            // ✅ СОХРАНЯЕМ ДАННЫЕ ПРЯМО В ПРЕДМЕТ
            NbtCompound noteNbt = note.toNbt();
            NbtCompound rootNbt = new NbtCompound();
            rootNbt.put(BulletinBoardMod.NOTE_DATA_NBT_KEY, noteNbt);

            NbtComponent nbtComponent = NbtComponent.of(rootNbt);
            notePaper.set(DataComponentTypes.CUSTOM_DATA, nbtComponent);

            // Отправляем на сервер для синхронизации
            int slot = findSlotIndex();
            if (slot >= 0) {
                ModPackets.sendUpdateNoteNbt(slot, rootNbt);
            }

            this.close();
        }
    }

    private int findSlotIndex() {
        if (this.client == null || this.client.player == null) return -1;
        var inventory = this.client.player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            if (ItemStack.areEqual(inventory.getStack(i), this.notePaper)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_editor.title"),
                this.width / 2, 20, 0xFFFFFF);

        context.drawText(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_editor.title_label"),
                centerX - 100, startY - 12, 0xFFFFFF, false);
        context.drawText(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_editor.content_label"),
                centerX - 100, startY + 23, 0xFFFFFF, false);

        String titleCounter = String.format("%d/%d",
                this.titleField.getText().length(), TITLE_MAX_LENGTH);
        context.drawText(this.textRenderer, Text.literal(titleCounter),
                centerX + 100 - this.textRenderer.getWidth(titleCounter), startY - 12,
                this.titleField.getText().length() >= TITLE_MAX_LENGTH ? 0xFF5555 : 0xAAAAAA, false);

        String contentCounter = String.format("%d/%d",
                this.contentField.getText().length(), contentMaxLength);
        context.drawText(this.textRenderer, Text.literal(contentCounter),
                centerX + 100 - this.textRenderer.getWidth(contentCounter), startY + 23,
                this.contentField.getText().length() >= contentMaxLength ? 0xFF5555 : 0xAAAAAA, false);

        this.titleField.render(context, mouseX, mouseY, delta);
        this.contentField.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            this.saveNote();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Рисуем непрозрачный фон
        context.fill(0, 0, this.width, this.height, 0x0F000000);
    }
}