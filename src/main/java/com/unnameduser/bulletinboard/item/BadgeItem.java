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
        ItemStack stack = player.getStackInHand(hand);
        ItemStack otherHand = hand == Hand.MAIN_HAND ?
                player.getOffHandStack() : player.getMainHandStack();

        if (otherHand.getItem() == BulletinBoardMod.NOTE_PAPER &&
                otherHand.hasNbt() && otherHand.getNbt().contains("NoteData")) {

            NoteData note = NoteData.fromNbt(otherHand.getNbt().getCompound("NoteData"));
            note.setTagColor(badgeColor);

            note.setHasSeal(true);

            ItemStack newNote = new ItemStack(BulletinBoardMod.NOTE_PAPER, 1);
            NbtCompound nbt = newNote.getOrCreateNbt();
            nbt.put("NoteData", note.toNbt());

            if (!world.isClient) {
                otherHand.decrement(1);
                stack.decrement(1);

                if (!player.getInventory().insertStack(newNote)) {
                    player.dropItem(newNote, false);
                }
            }

            return TypedActionResult.success(stack);
        }

        return TypedActionResult.pass(stack);
    }

    public int getBadgeColor() {
        return badgeColor;
    }
}