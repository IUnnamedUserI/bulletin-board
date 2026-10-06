package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.client.VillagerNameClientCache;
import com.unnameduser.bulletinboard.screen.NoteViewScreen;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModPacketsClient {

    // ============ ОБРАБОТЧИКИ КЛИЕНТСКИХ ПАКЕТОВ ============

    public static void handleVillagerNames(VillagerNameSyncPacket packet) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            VillagerNameClientCache.updateNames(packet.getNames());
        });
    }

    public static void handleOpenPlacedNote(OpenPlacedNoteS2CPacket packet) {
        Minecraft client = Minecraft.getInstance();
        final BlockPos pos = packet.pos();
        client.execute(() -> {
            if (client.level != null
                    && client.level.getBlockEntity(pos) instanceof PlacedNoteBlockEntity noteEntity) {
                NoteData note = noteEntity.getNoteData();
                if (note != null) {
                    client.setScreen(new NoteViewScreen(note, pos));
                }
            }
        });
    }

    public static void handleOpenNote(OpenNoteS2CPacket packet) {
        Minecraft client = Minecraft.getInstance();
        final BlockPos pos = packet.pos();
        final int slot = packet.slot();
        client.execute(() -> {
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
        ModPackets.CHANNEL.sendToServer(new TakeNoteC2SPacket(pos, noteIndex));
    }

    public static void sendUpdateNoteNbt(int slot, CompoundTag nbt) {
        ModPackets.CHANNEL.sendToServer(new UpdateNoteNbtC2SPacket(slot, nbt));
    }

    public static void sendTakePlacedNote(BlockPos pos) {
        ModPackets.CHANNEL.sendToServer(new TakePlacedNoteC2SPacket(pos));
    }
}
