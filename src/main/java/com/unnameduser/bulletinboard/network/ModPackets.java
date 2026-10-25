package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.server.VillagerNameManager;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;

public class ModPackets {

    // ============ РЕГИСТРАЦИЯ (вызывается из BulletinBoardMod с передачей modEventBus) ============

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModPackets::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(BulletinBoardMod.MOD_ID);

        // C2S пакеты
        registrar.playToServer(
                TakeNoteC2SPacket.TYPE,
                TakeNoteC2SPacket.STREAM_CODEC,
                ModPackets::handleTakeNote
        );
        registrar.playToServer(
                UpdateNoteNbtC2SPacket.TYPE,
                UpdateNoteNbtC2SPacket.STREAM_CODEC,
                ModPackets::handleUpdateNoteNbt
        );
        registrar.playToServer(
                TakePlacedNoteC2SPacket.TYPE,
                TakePlacedNoteC2SPacket.STREAM_CODEC,
                ModPackets::handleTakePlacedNote
        );

        // S2C пакеты
        registrar.playToClient(
                OpenNoteS2CPacket.TYPE,
                OpenNoteS2CPacket.STREAM_CODEC,
                ModPacketsClient::handleOpenNote
        );
        registrar.playToClient(
                OpenPlacedNoteS2CPacket.TYPE,
                OpenPlacedNoteS2CPacket.STREAM_CODEC,
                ModPacketsClient::handleOpenPlacedNote
        );
        registrar.playToClient(
                VillagerNameSyncPacket.TYPE,
                VillagerNameSyncPacket.STREAM_CODEC,
                ModPacketsClient::handleVillagerNames
        );
    }

    // ============ ОБРАБОТЧИКИ СЕРВЕРНЫХ ПАКЕТОВ ============

    private static void handleTakeNote(TakeNoteC2SPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            BlockPos pos = msg.pos();
            int position = msg.noteIndex();

            ServerLevel world = player.serverLevel();
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
                    CompoundTag nbt = noteStack.getOrCreateTag();
                    nbt.put("NoteData", note.toNbt());

                    player.getInventory().placeItemBackInInventory(noteStack);
                }
            }
        });
    }

    private static void handleUpdateNoteNbt(UpdateNoteNbtC2SPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            int slot = packet.slot();
            CompoundTag nbt = packet.nbt();

            if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack.getItem() instanceof NotePaperItem) {
                    stack.setTag(nbt.copy());
                    player.getInventory().setChanged();
                }
            }
        });
    }

    private static void handleTakePlacedNote(TakePlacedNoteC2SPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            BlockPos pos = msg.pos();

            ServerLevel world = player.serverLevel();
            var blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                NoteData note = noteEntity.getNoteData();
                if (note != null) {
                    ItemStack noteStack = new ItemStack(
                            note.isSmall() ? BulletinBoardMod.SMALL_NOTE_PAPER : BulletinBoardMod.NOTE_PAPER,
                            1
                    );
                    CompoundTag nbt = noteStack.getOrCreateTag();
                    nbt.put("NoteData", note.toNbt());
                    player.getInventory().placeItemBackInInventory(noteStack);
                }

                noteEntity.setTakenByPlayer(true);
                world.removeBlock(pos, false);
            }
        });
    }

    // ============ ОТПРАВКА ПАКЕТОВ С СЕРВЕРА ============

    public static void sendVillagerNames(ServerPlayer player, MinecraftServer server) {
        VillagerNameManager manager = VillagerNameManager.get(server);
        Map<String, String> allNames = new HashMap<>();

        ServerLevel world = server.overworld();
        if (world != null) {
            for (Entity entity : world.getAllEntities()) {
                if (entity instanceof Villager villager) {
                    String nameKey = manager.getOrCreateNameKey(villager.getUUID());
                    allNames.put(villager.getUUID().toString(), nameKey);
                }
            }
        }

        PacketDistributor.sendToPlayer(player, new VillagerNameSyncPacket(allNames));
    }

    public static void sendOpenNoteScreenToClient(ServerPlayer player, BlockPos pos, int slot) {
        PacketDistributor.sendToPlayer(player, new OpenNoteS2CPacket(pos, slot));
    }

    public static void sendOpenPlacedNoteScreen(ServerPlayer player, BlockPos pos) {
        PacketDistributor.sendToPlayer(player, new OpenPlacedNoteS2CPacket(pos));
    }
}