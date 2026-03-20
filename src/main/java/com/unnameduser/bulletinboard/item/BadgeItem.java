package com.unnameduser.bulletinboard.item;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class BadgeItem extends Item {
    private final int badgeColor;

    public BadgeItem(Settings settings, int badgeColor) {
        super(settings);
        this.badgeColor = badgeColor;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack badgeStack = player.getStackInHand(hand);
        ItemStack noteStack = hand == Hand.MAIN_HAND ?
                player.getOffHandStack() : player.getMainHandStack();

        // Проверяем, что во второй руке подписанная записка
        if ((noteStack.getItem() == BulletinBoardMod.NOTE_PAPER ||
                noteStack.getItem() == BulletinBoardMod.SMALL_NOTE_PAPER) &&
                noteStack.hasNbt() && noteStack.getNbt().contains("NoteData")) {

            NoteData note = NoteData.fromNbt(noteStack.getNbt().getCompound("NoteData"));
            note.setTagColor(badgeColor);
            note.setHasSeal(true);

            ItemStack newNote = new ItemStack(noteStack.getItem(), 1);
            NbtCompound nbt = newNote.getOrCreateNbt();
            nbt.put("NoteData", note.toNbt());

            if (!world.isClient) {
                // Удаляем старую записку
                if (noteStack.getCount() > 1) {
                    noteStack.decrement(1);
                } else {
                    player.getInventory().removeOne(noteStack);
                }

                // Удаляем печать
                if (badgeStack.getCount() > 1) {
                    badgeStack.decrement(1);
                } else {
                    player.getInventory().removeOne(badgeStack);
                }

                // Добавляем новую записку
                if (!player.getInventory().insertStack(newNote)) {
                    player.dropItem(newNote, false);
                }
            }

            return TypedActionResult.success(badgeStack);
        }

        return TypedActionResult.pass(badgeStack);
    }

    public int getBadgeColor() {
        return badgeColor;
    }
}