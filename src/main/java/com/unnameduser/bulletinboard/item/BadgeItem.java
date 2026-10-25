package com.unnameduser.bulletinboard.item;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public class BadgeItem extends Item {
    private final int badgeColor;

    public BadgeItem(Item.Properties settings, int badgeColor) {
        super(settings);
        this.badgeColor = badgeColor;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack badgeStack = player.getItemInHand(hand);
        ItemStack noteStack = hand == InteractionHand.MAIN_HAND ?
                player.getOffhandItem() : player.getMainHandItem();

        // Проверяем, что во второй руке подписанная записка
        if ((noteStack.getItem() == BulletinBoardMod.NOTE_PAPER ||
                noteStack.getItem() == BulletinBoardMod.SMALL_NOTE_PAPER) &&
                hasNoteData(noteStack)) {

            CompoundTag noteTag = getCustomTag(noteStack);
            NoteData note = NoteData.fromNbt(noteTag.getCompound("NoteData"));
            note.setTagColor(badgeColor);
            note.setHasSeal(true);

            ItemStack newNote = new ItemStack(noteStack.getItem(), 1);
            CompoundTag rootTag = new CompoundTag();
            rootTag.put("NoteData", note.toNbt());
            newNote.set(DataComponents.CUSTOM_DATA, CustomData.of(rootTag));

            if (!world.isClientSide) {
                // Удаляем старую записку
                if (noteStack.getCount() > 1) {
                    noteStack.shrink(1);
                } else {
                    player.getInventory().removeItem(noteStack);
                }

                // Удаляем печать
                if (badgeStack.getCount() > 1) {
                    badgeStack.shrink(1);
                } else {
                    player.getInventory().removeItem(badgeStack);
                }

                // Добавляем новую записку
                if (!player.getInventory().add(newNote)) {
                    player.drop(newNote, false);
                }
            }

            return InteractionResultHolder.success(badgeStack);
        }

        return InteractionResultHolder.pass(badgeStack);
    }

    private boolean hasNoteData(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null && customData.contains("NoteData");
    }

    private CompoundTag getCustomTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        return customData != null ? customData.copyTag() : new CompoundTag();
    }

    public int getBadgeColor() {
        return badgeColor;
    }
}