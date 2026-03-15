package com.unnameduser.bulletinboard.screen;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class NoteEditorScreen extends Screen {
    private TextFieldWidget titleField;
    private TextFieldWidget contentField;
    private final ItemStack notePaper;

    private boolean isAnonymous = false;
    private ButtonWidget anonymousButton;

    private static final int TITLE_MAX_LENGTH = 48;
    private static final int CONTENT_MAX_LENGTH = 386;

    public NoteEditorScreen(ItemStack notePaper) {
        super(Text.translatable("gui.bulletin-board.note_editor.title"));
        this.notePaper = notePaper;
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
        this.addSelectableChild(this.titleField);

        this.contentField = new TextFieldWidget(
                this.textRenderer,
                centerX - 100,
                startY + 35,
                200, 20,
                Text.translatable("gui.bulletin-board.note_editor.content_placeholder")
        );
        this.contentField.setMaxLength(CONTENT_MAX_LENGTH);
        this.contentField.setPlaceholder(Text.translatable("gui.bulletin-board.note_editor.content_placeholder"));
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
                    this.client.player.getName().getString();

            NoteData note = new NoteData(title, content, author, -1);

            NbtCompound nbt = new NbtCompound();
            nbt.put("NoteData", note.toNbt());

            int slot = findSlotIndex();

            this.notePaper.setNbt(nbt);

            if (slot >= 0) {
                ModPackets.sendUpdateNoteNbt(slot, nbt);
            }

            this.close();
        }
    }

    private int findSlotIndex() {
        if (this.client == null || this.client.player == null) return -1;
        var inventory = this.client.player.getInventory();

        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i) == this.notePaper) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

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
                this.contentField.getText().length(), CONTENT_MAX_LENGTH);
        context.drawText(this.textRenderer, Text.literal(contentCounter),
                centerX + 100 - this.textRenderer.getWidth(contentCounter), startY + 23,
                this.contentField.getText().length() >= CONTENT_MAX_LENGTH ? 0xFF5555 : 0xAAAAAA, false);

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
}