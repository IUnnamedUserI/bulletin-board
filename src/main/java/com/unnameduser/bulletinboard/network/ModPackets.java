package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.util.NoteData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class ModPackets {

    public static void register() {
        // Обработчик взятия записки с доски
        ServerPlayNetworking.registerGlobalReceiver(TakeNotePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                var world = player.getWorld();
                var blockEntity = world.getBlockEntity(payload.pos());

                if (blockEntity instanceof BulletinBoardBlockEntity boardEntity) {
                    var note = boardEntity.getNoteAtPosition(payload.slot());

                    if (note != null && !boardEntity.isPositionFree(payload.slot())) {
                        int index = boardEntity.getNoteIndexByPosition(payload.slot());
                        if (index >= 0) {
                            boardEntity.removeNote(index);
                        }

                        ItemStack noteStack = new ItemStack(
                                note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                                1
                        );

                        // Правильная структура NBT
                        NbtCompound noteNbt = note.toNbt();
                        NbtCompound rootNbt = new NbtCompound();
                        rootNbt.put(BulletinBoardMod.NOTE_DATA_NBT_KEY, noteNbt);

                        NbtComponent nbtComponent = NbtComponent.of(rootNbt);
                        noteStack.set(DataComponentTypes.CUSTOM_DATA, nbtComponent);

                        if (!player.getInventory().insertStack(noteStack)) {
                            player.dropItem(noteStack, false);
                        }

                        player.playerScreenHandler.sendContentUpdates();
                    }
                }
            });
        });

        // Обработчик обновления записки
        ServerPlayNetworking.registerGlobalReceiver(UpdateNoteNbtPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                int slot = payload.slot();
                NbtCompound nbt = payload.nbt();

                if (slot >= 0 && slot < player.getInventory().size()) {
                    ItemStack stack = player.getInventory().getStack(slot);

                    if (stack.getItem() instanceof NotePaperItem) {
                        NbtComponent nbtComponent = NbtComponent.of(nbt);
                        stack.set(DataComponentTypes.CUSTOM_DATA, nbtComponent);
                        player.getInventory().markDirty();
                        player.playerScreenHandler.sendContentUpdates();
                    }
                }
            });
        });
    }

    public static void sendTakeNote(BlockPos pos, int noteIndex) {
        ClientPlayNetworking.send(new TakeNotePayload(pos, noteIndex));
    }

    public static void sendUpdateNoteNbt(int slot, NbtCompound nbt) {
        ClientPlayNetworking.send(new UpdateNoteNbtPayload(slot, nbt));
    }

    public static void sendOpenNoteScreenToClient(ServerPlayerEntity player, BlockPos pos, int slot) {
        ServerPlayNetworking.send(player, new OpenNotePayload(pos, slot));
    }
}