package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.client.VillagerNameClientCache;
import com.unnameduser.bulletinboard.screen.NoteViewScreen;
import com.unnameduser.bulletinboard.util.NoteData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class ModPacketsClient {

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(VillagerNameSyncPacket.ID, (client, handler, buf, responseSender) -> {
            VillagerNameSyncPacket packet = VillagerNameSyncPacket.read(buf);
            client.execute(() -> {
                VillagerNameClientCache.updateNames(packet.getNames());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.OPEN_PLACED_NOTE, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            client.execute(() -> {
                if (client.world != null
                        && client.world.getBlockEntity(pos) instanceof PlacedNoteBlockEntity noteEntity) {
                    NoteData note = noteEntity.getNoteData();
                    if (note != null) {
                        client.setScreen(new NoteViewScreen(note, pos));
                    }
                }
            });
        });
    }

    public static void sendTakeNote(BlockPos pos, int noteIndex) {
        if (!ClientPlayNetworking.canSend(ModPackets.TAKE_NOTE)) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(noteIndex);
        ClientPlayNetworking.send(ModPackets.TAKE_NOTE, buf);
    }

    public static void sendUpdateNoteNbt(int slot, net.minecraft.nbt.NbtCompound nbt) {
        if (!ClientPlayNetworking.canSend(ModPackets.UPDATE_NOTE_NBT)) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(slot);
        buf.writeNbt(nbt);
        ClientPlayNetworking.send(ModPackets.UPDATE_NOTE_NBT, buf);
    }

    public static void sendTakePlacedNote(BlockPos pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ClientPlayNetworking.send(ModPackets.TAKE_PLACED_NOTE, buf);
    }
}