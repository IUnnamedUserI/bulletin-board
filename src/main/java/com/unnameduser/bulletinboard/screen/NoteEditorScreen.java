package com.unnameduser.bulletinboard.screen;

import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.util.NoteConstants;
import com.unnameduser.bulletinboard.util.NoteData;
import com.unnameduser.bulletinboard.widget.AdaptiveRoundedTextFieldWidget;
import com.unnameduser.bulletinboard.widget.RoundedButtonWidget;
import com.unnameduser.bulletinboard.widget.RoundedTextFieldWidget;
import com.unnameduser.bulletinboard.widget.SwitchWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class NoteEditorScreen extends Screen {
    private AdaptiveRoundedTextFieldWidget titleField;
    private RoundedTextFieldWidget contentField;
    private final ItemStack notePaper;
    private boolean isAnonymous = false;
    private SwitchWidget anonymousSwitch;

    private final int titleMaxLength;
    private final int contentMaxLength;

    public NoteEditorScreen(ItemStack notePaper) {
        super(Text.translatable("gui.bulletin-board.note_editor.title"));
        this.notePaper = notePaper;

        // Определяем тип записки по NBT или по умолчанию (полноразмерная)
        boolean isSmall = false;
        if (notePaper.hasNbt() && notePaper.getNbt().contains("IsSmall")) {
            isSmall = notePaper.getNbt().getBoolean("IsSmall");
        }

        this.titleMaxLength = isSmall ? NoteConstants.SMALL_TITLE_MAX : NoteConstants.FULL_TITLE_MAX;
        this.contentMaxLength = isSmall ? NoteConstants.SMALL_CONTENT_MAX : NoteConstants.FULL_CONTENT_MAX;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int fieldWidth = 200;
        int fieldX = centerX - fieldWidth / 2;

        int contentHeight = 80;
        int contentY = centerY - contentHeight / 2;

        // === ПОЛЕ ЗАГОЛОВКА ===
        int titleY = contentY - 40;
        this.titleField = new AdaptiveRoundedTextFieldWidget(
                fieldX, titleY,
                fieldWidth,
                20, 60,
                titleMaxLength,
                Text.translatable("gui.bulletin-board.note_editor.title_placeholder")
        );
        this.addSelectableChild(this.titleField);

        // === ПОЛЕ СОДЕРЖИМОГО ===
        this.contentField = new RoundedTextFieldWidget(
                fieldX, contentY,
                fieldWidth, contentHeight,
                contentMaxLength,
                Text.translatable("gui.bulletin-board.note_editor.content_placeholder")
        );
        this.addSelectableChild(this.contentField);

        // === ПЕРЕКЛЮЧАТЕЛЬ АНОНИМНОСТИ ===
        int switchY = contentY + contentHeight + 10;
        this.anonymousSwitch = new SwitchWidget(
                fieldX, switchY,
                Text.translatable("gui.bulletin-board.note_editor.anonymous"),
                () -> {
                    isAnonymous = anonymousSwitch.getState();
                }
        );
        this.addDrawableChild(this.anonymousSwitch);

        // === КНОПКИ ===
        Text saveText = Text.translatable("gui.bulletin-board.note_editor.save");
        Text cancelText = Text.translatable("gui.bulletin-board.note_editor.cancel");

        int saveWidth = RoundedButtonWidget.calculateWidth(saveText);
        int cancelWidth = RoundedButtonWidget.calculateWidth(cancelText);

        int totalWidth = saveWidth + cancelWidth + 10;
        int buttonsStartX = centerX - totalWidth / 2;
        int buttonY = switchY + 30;

        this.addDrawableChild(new RoundedButtonWidget(
                buttonsStartX, buttonY,
                saveText,
                this::saveNote,
                0xFF55AA55
        ));

        this.addDrawableChild(new RoundedButtonWidget(
                buttonsStartX + saveWidth + 10, buttonY,
                cancelText,
                this::close,
                0xFFFF5555
        ));

        this.setInitialFocus(this.titleField);
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

            // Сохраняем тип записки
            boolean isSmall = titleMaxLength == NoteConstants.SMALL_TITLE_MAX;
            nbt.putBoolean("IsSmall", isSmall);

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
        int fieldWidth = 200;
        int fieldX = centerX - fieldWidth / 2;

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_editor.title"),
                centerX, 20, 0xFFFFFF);

        context.drawText(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_editor.title_label"),
                fieldX, this.titleField.getY() - 12, 0xFFFFFF, false);

        context.drawText(this.textRenderer,
                Text.translatable("gui.bulletin-board.note_editor.content_label"),
                fieldX, this.contentField.getY() - 12, 0xFFFFFF, false);

        this.titleField.render(context, mouseX, mouseY, delta);
        this.contentField.render(context, mouseX, mouseY, delta);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        super.tick();
        if (titleField != null) {
            titleField.tick();
        }
        if (contentField != null) {
            contentField.tick();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (this.titleField != null && this.titleField.isFocused()) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            if (this.contentField != null && this.contentField.isFocused()) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
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