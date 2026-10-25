package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.client.VillagerNameClientCache;
import com.unnameduser.bulletinboard.screen.NoteViewScreen;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ModPacketsClient {

    // ============ ОБРАБОТЧИКИ КЛИЕНТСКИХ ПАКЕТОВ (вызываются из ModPackets.onRegisterPayloadHandlers) ============

    public static void handleVillagerNames(VillagerNameSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            VillagerNameClientCache.updateNames(packet.names());
        });
    }

    public static void handleOpenPlacedNote(OpenPlacedNoteS2CPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            BlockPos pos = packet.pos();
            if (client.level != null
                    && client.level.getBlockEntity(pos) instanceof PlacedNoteBlockEntity noteEntity) {
                NoteData note = noteEntity.getNoteData();
                if (note != null) {
                    client.setScreen(new NoteViewScreen(note, pos));
                }
            }
        });
    }

    public static void handleOpenNote(OpenNoteS2CPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft client = Minecraft.getInstance();
            BlockPos pos = packet.pos();
            int slot = packet.slot();
            if (client.level != null
                    && client.level.getBlockEntity(pos) instanceof BulletinBoardBlockEntity boardEntity) {
                NoteData note = boardEntity.getNoteAtPosition(slot);
                if (note != null) {
                    client.setScreen(new NoteViewScreen(note, boardEntity, slot));
                }
            }
        });
    }

    // ============ ОТПРАВКА ПАКЕТОВ НА СЕРВЕР ============

    public static void sendTakeNote(BlockPos pos, int noteIndex) {
        PacketDistributor.sendToServer(new TakeNoteC2SPacket(pos, noteIndex));
    }

    public static void sendUpdateNoteNbt(int slot, CompoundTag nbt) {
        PacketDistributor.sendToServer(new UpdateNoteNbtC2SPacket(slot, nbt));
    }

    public static void sendTakePlacedNote(BlockPos pos) {
        PacketDistributor.sendToServer(new TakePlacedNoteC2SPacket(pos));
    }
}