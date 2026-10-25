package com.unnameduser.bulletinboard.screen;

import com.unnameduser.bulletinboard.network.ModPacketsClient;
import com.unnameduser.bulletinboard.util.NoteConstants;
import com.unnameduser.bulletinboard.util.NoteData;
import com.unnameduser.bulletinboard.widget.AdaptiveRoundedTextFieldWidget;
import com.unnameduser.bulletinboard.widget.RoundedButtonWidget;
import com.unnameduser.bulletinboard.widget.RoundedTextFieldWidget;
import com.unnameduser.bulletinboard.widget.SwitchWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
        super(Component.translatable("gui.bulletin_board.note_editor.title"));
        this.notePaper = notePaper;

        // В 1.21.1 hasTag() -> hasComponents() или проверка через getOrDefault,
        // но для кастомных данных в ComponentMap используем get(DataComponentType).
        // Однако если вы храните данные через legacy NBT (setTag), то в 1.21.1
        // это больше не работает напрямую. Нужно использовать CustomData component.
        // Для упрощения порта пока оставим проверку через components,
        // но ниже в saveNote нужно будет писать правильно.
        boolean isSmall = false;
        var customData = notePaper.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains("IsSmall")) {
            isSmall = customData.copyTag().getBoolean("IsSmall");
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
                Component.translatable("gui.bulletin_board.note_editor.title_placeholder")
        );
        this.addWidget(this.titleField);

        // === ПОЛЕ СОДЕРЖИМОГО ===
        this.contentField = new RoundedTextFieldWidget(
                fieldX, contentY,
                fieldWidth, contentHeight,
                contentMaxLength,
                Component.translatable("gui.bulletin_board.note_editor.content_placeholder")
        );
        this.addWidget(this.contentField);

        // === ПЕРЕКЛЮЧАТЕЛЬ АНОНИМНОСТИ ===
        int switchY = contentY + contentHeight + 10;
        this.anonymousSwitch = new SwitchWidget(
                fieldX, switchY,
                Component.translatable("gui.bulletin_board.note_editor.anonymous"),
                () -> {
                    isAnonymous = anonymousSwitch.getState();
                }
        );
        this.addRenderableWidget(this.anonymousSwitch);

        // === КНОПКИ ===
        Component saveText = Component.translatable("gui.bulletin_board.note_editor.save");
        Component cancelText = Component.translatable("gui.bulletin_board.note_editor.cancel");

        int saveWidth = RoundedButtonWidget.calculateWidth(saveText);
        int cancelWidth = RoundedButtonWidget.calculateWidth(cancelText);

        int totalWidth = saveWidth + cancelWidth + 10;
        int buttonsStartX = centerX - totalWidth / 2;
        int buttonY = switchY + 30;

        this.addRenderableWidget(new RoundedButtonWidget(
                buttonsStartX, buttonY,
                saveText,
                this::saveNote,
                0xFF55AA55
        ));

        this.addRenderableWidget(new RoundedButtonWidget(
                buttonsStartX + saveWidth + 10, buttonY,
                cancelText,
                this::onClose,
                0xFFFF5555
        ));

        this.setInitialFocus(this.titleField);
    }

    private void saveNote() {
        String title = titleField.getText().trim();
        String content = contentField.getText().trim();

        if (!title.isEmpty() && !content.isEmpty()) {
            String author = isAnonymous ?
                    Component.translatable("gui.bulletin_board.note_editor.anonymous_name").getString() :
                    this.minecraft.player.getName().getString();

            NoteData note = new NoteData(title, content, author, -1);

            CompoundTag noteNbt = note.toNbt();
            boolean isSmall = titleMaxLength == NoteConstants.SMALL_TITLE_MAX;

            // В 1.21.1 нельзя просто setTag(). Нужно использовать CustomData component.
            CompoundTag rootTag = new CompoundTag();
            rootTag.put("NoteData", noteNbt);
            rootTag.putBoolean("IsSmall", isSmall);

            this.notePaper.set(
                    net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(rootTag)
            );

            int slot = findSlotIndex();
            if (slot >= 0) {
                ModPacketsClient.sendUpdateNoteNbt(slot, rootTag);
            }

            this.onClose();
        }
    }

    private int findSlotIndex() {
        if (this.minecraft == null || this.minecraft.player == null) return -1;
        var inventory = this.minecraft.player.getInventory();

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i) == this.notePaper) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // В NeoForge 1.21.1 renderBackground требует 4 аргумента
        this.renderBackground(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        int fieldWidth = 200;
        int fieldX = centerX - fieldWidth / 2;

        context.drawCenteredString(this.font,
                Component.translatable("gui.bulletin_board.note_editor.title"),
                centerX, 20, 0xFFFFFF);

        context.drawString(this.font,
                Component.translatable("gui.bulletin_board.note_editor.title_label"),
                fieldX, this.titleField.getY() - 12, 0xFFFFFF, false);

        context.drawString(this.font,
                Component.translatable("gui.bulletin_board.note_editor.content_label"),
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Не вызываем super, чтобы избежать блюра
        // Можно залить затемнением вручную:
        guiGraphics.fill(0, 0, this.width, this.height, 0x00FFFFFF); // полупрозрачный чёрный
    }
}