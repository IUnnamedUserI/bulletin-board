package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.util.NoteData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModPackets {
    public static final Identifier TAKE_NOTE = new Identifier(BulletinBoardMod.MOD_ID, "take_note");
    public static final Identifier UPDATE_NOTE_NBT = new Identifier(BulletinBoardMod.MOD_ID, "update_note_nbt");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TAKE_NOTE, ModPackets::handleTakeNote);
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_NOTE_NBT, ModPackets::handleUpdateNoteNbt);
    }

    private static void handleTakeNote(MinecraftServer server, ServerPlayerEntity player,
                                       ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                       PacketSender responseSender) {
        final BlockPos pos = buf.readBlockPos();
        final int position = buf.readInt();

        server.execute(() -> {
            var world = player.getWorld();
            var blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof BulletinBoardBlockEntity boardEntity) {
                var note = boardEntity.getNoteAtPosition(position);

                if (note != null && !boardEntity.isPositionFree(position)) {
                    int index = boardEntity.getNoteIndexByPosition(position);
                    if (index >= 0) {
                        boardEntity.removeNote(index);
                    }

                    // 🔧 ИСПРАВЛЕНО: правильный источник предметов + сохраняем isSmall
                    ItemStack noteStack = new ItemStack(
                            note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                            1
                    );
                    NbtCompound nbt = noteStack.getOrCreateNbt();
                    nbt.put("NoteData", note.toNbt());  // toNbt() уже содержит isSmall!

                    player.getInventory().offerOrDrop(noteStack);
                }
            }
        });
    }

    private static void handleUpdateNoteNbt(MinecraftServer server, ServerPlayerEntity player,
                                            ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                            PacketSender responseSender) {
        final UpdateNoteNbtC2SPacket packet = new UpdateNoteNbtC2SPacket(buf);
        final int slot = packet.slot();
        final NbtCompound nbt = packet.nbt();

        server.execute(() -> {
            if (slot >= 0 && slot < player.getInventory().size()) {
                ItemStack stack = player.getInventory().getStack(slot);

                // 🔧 ИСПРАВЛЕНО: проверка через instanceof для обоих типов записок
                if (stack.getItem() instanceof NotePaperItem) {
                    stack.setNbt(nbt.copy());  // Копируем ВСЕ поля, включая isSmall
                    player.getInventory().markDirty();
                }
            }
        });
    }

    public static void sendTakeNote(BlockPos pos, int noteIndex) {
        if (!ClientPlayNetworking.canSend(TAKE_NOTE)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(noteIndex);
        ClientPlayNetworking.send(TAKE_NOTE, buf);
    }

    public static void sendUpdateNoteNbt(int slot, NbtCompound nbt) {
        if (!ClientPlayNetworking.canSend(UPDATE_NOTE_NBT)) {
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(slot);
        buf.writeNbt(nbt);
        ClientPlayNetworking.send(UPDATE_NOTE_NBT, buf);
    }

    public static void sendOpenNoteScreenToClient(ServerPlayerEntity player, BlockPos pos, int slot) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(slot);
        ServerPlayNetworking.send(player, new Identifier(BulletinBoardMod.MOD_ID, "open_note"), buf);
    }
}