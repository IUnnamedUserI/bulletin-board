package com.unnameduser.bulletinboard.item;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
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
        NbtComponent noteComponent = noteStack.get(DataComponentTypes.CUSTOM_DATA);
        if ((noteStack.getItem() == BulletinBoardMod.NOTE_PAPER ||
                noteStack.getItem() == BulletinBoardMod.SMALL_NOTE_PAPER) &&
                noteComponent != null) {

            NbtCompound noteRootNbt = noteComponent.copyNbt();
            if (noteRootNbt.contains(BulletinBoardMod.NOTE_DATA_NBT_KEY)) {
                NbtCompound noteNbt = noteRootNbt.getCompound(BulletinBoardMod.NOTE_DATA_NBT_KEY);
                NoteData note = NoteData.fromNbt(noteNbt);

                // Обновляем данные записки
                note.setTagColor(badgeColor);
                note.setHasSeal(true);

                // Создаём новую записку с обновлёнными данными
                ItemStack newNote = new ItemStack(noteStack.getItem(), 1);

                // Правильная структура NBT
                NbtCompound newNoteNbt = note.toNbt();
                NbtCompound newRootNbt = new NbtCompound();
                newRootNbt.put(BulletinBoardMod.NOTE_DATA_NBT_KEY, newNoteNbt);

                NbtComponent newComponent = NbtComponent.of(newRootNbt);
                newNote.set(DataComponentTypes.CUSTOM_DATA, newComponent);

                if (!world.isClient) {
                    // Удаляем старую записку
                    noteStack.decrement(1);

                    // Удаляем значок
                    badgeStack.decrement(1);

                    // Добавляем новую записку
                    if (!player.getInventory().insertStack(newNote)) {
                        player.dropItem(newNote, false);
                    }

                    player.getInventory().markDirty();
                }

                return TypedActionResult.success(badgeStack);
            }
        }

        return TypedActionResult.pass(badgeStack);
    }

    public int getBadgeColor() {
        return badgeColor;
    }
}