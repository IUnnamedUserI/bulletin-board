package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.server.VillagerNameManager;
import com.unnameduser.bulletinboard.util.NoteData;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class ModPackets {
    public static final Identifier TAKE_NOTE = new Identifier(BulletinBoardMod.MOD_ID, "take_note");
    public static final Identifier UPDATE_NOTE_NBT = new Identifier(BulletinBoardMod.MOD_ID, "update_note_nbt");
    public static final Identifier OPEN_PLACED_NOTE = new Identifier(BulletinBoardMod.MOD_ID, "open_placed_note");
    public static final Identifier TAKE_PLACED_NOTE = new Identifier(BulletinBoardMod.MOD_ID, "take_placed_note");

    // ============ РЕГИСТРАЦИЯ СЕРВЕРНЫХ ПАКЕТОВ ============

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TAKE_NOTE, ModPackets::handleTakeNote);
        ServerPlayNetworking.registerGlobalReceiver(UPDATE_NOTE_NBT, ModPackets::handleUpdateNoteNbt);
        ServerPlayNetworking.registerGlobalReceiver(TAKE_PLACED_NOTE, ModPackets::handleTakePlacedNote);
    }

    // ============ ОБРАБОТЧИКИ СЕРВЕРНЫХ ПАКЕТОВ ============

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

                    ItemStack noteStack = new ItemStack(
                            note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                            1
                    );
                    NbtCompound nbt = noteStack.getOrCreateNbt();
                    nbt.put("NoteData", note.toNbt());

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
                if (stack.getItem() instanceof NotePaperItem) {
                    stack.setNbt(nbt.copy());
                    player.getInventory().markDirty();
                }
            }
        });
    }

    private static void handleTakePlacedNote(MinecraftServer server, ServerPlayerEntity player,
                                             ServerPlayNetworkHandler handler, PacketByteBuf buf,
                                             PacketSender responseSender) {
        final BlockPos pos = buf.readBlockPos();

        server.execute(() -> {
            var world = player.getWorld();
            var blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                NoteData note = noteEntity.getNoteData();
                if (note != null) {
                    ItemStack noteStack = new ItemStack(
                            note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                            1
                    );
                    NbtCompound nbt = noteStack.getOrCreateNbt();
                    nbt.put("NoteData", note.toNbt());
                    player.getInventory().offerOrDrop(noteStack);
                }

                noteEntity.setTakenByPlayer(true);
                world.removeBlock(pos, false);
            }
        });
    }

    // ============ ОТПРАВКА ПАКЕТОВ С СЕРВЕРА ============

    public static void sendVillagerNames(ServerPlayerEntity player, MinecraftServer server) {
        VillagerNameManager manager = VillagerNameManager.get(server);
        Map<String, String> allNames = new HashMap<>();

        var world = server.getWorld(World.OVERWORLD);
        if (world != null) {
            for (VillagerEntity villager : world.getEntitiesByType(EntityType.VILLAGER, entity -> true)) {
                String nameKey = manager.getOrCreateNameKey(villager.getUuid());
                allNames.put(villager.getUuid().toString(), nameKey);
            }
        }

        VillagerNameSyncPacket packet = new VillagerNameSyncPacket(allNames);
        PacketByteBuf buf = PacketByteBufs.create();
        packet.write(buf);
        ServerPlayNetworking.send(player, VillagerNameSyncPacket.ID, buf);
    }

    public static void sendOpenNoteScreenToClient(ServerPlayerEntity player, BlockPos pos, int slot) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeInt(slot);
        ServerPlayNetworking.send(player, new Identifier(BulletinBoardMod.MOD_ID, "open_note"), buf);
    }

    public static void sendOpenPlacedNoteScreen(ServerPlayerEntity player, BlockPos pos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        ServerPlayNetworking.send(player, OPEN_PLACED_NOTE, buf);
    }
}