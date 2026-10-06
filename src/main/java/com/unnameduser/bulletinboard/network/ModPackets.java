package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.server.VillagerNameManager;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModPackets {
    private static final String PROTOCOL_VERSION = "1";

    // ============ КАНАЛ СЕТИ (замена Fabric-каналов по Identifier) ============

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(BulletinBoardMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    // ============ РЕГИСТРАЦИЯ ПАКЕТОВ ============

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(TakeNoteC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TakeNoteC2SPacket::write)
                .decoder(TakeNoteC2SPacket::new)
                .consumerMainThread(ModPackets::handleTakeNote)
                .add();

        CHANNEL.messageBuilder(UpdateNoteNbtC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateNoteNbtC2SPacket::write)
                .decoder(UpdateNoteNbtC2SPacket::new)
                .consumerMainThread(ModPackets::handleUpdateNoteNbt)
                .add();

        CHANNEL.messageBuilder(TakePlacedNoteC2SPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TakePlacedNoteC2SPacket::write)
                .decoder(TakePlacedNoteC2SPacket::new)
                .consumerMainThread(ModPackets::handleTakePlacedNote)
                .add();

        CHANNEL.messageBuilder(OpenNoteS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenNoteS2CPacket::write)
                .decoder(OpenNoteS2CPacket::new)
                .consumerMainThread((msg, ctx) -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        ModPacketsClient.handleOpenNote(msg);
                    }
                })
                .add();

        CHANNEL.messageBuilder(OpenPlacedNoteS2CPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenPlacedNoteS2CPacket::write)
                .decoder(OpenPlacedNoteS2CPacket::new)
                .consumerMainThread((msg, ctx) -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        ModPacketsClient.handleOpenPlacedNote(msg);
                    }
                })
                .add();

        CHANNEL.messageBuilder(VillagerNameSyncPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(VillagerNameSyncPacket::write)
                .decoder(VillagerNameSyncPacket::new)
                .consumerMainThread((msg, ctx) -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        ModPacketsClient.handleVillagerNames(msg);
                    }
                })
                .add();
    }

    // ============ ОБРАБОТЧИКИ СЕРВЕРНЫХ ПАКЕТОВ ============

    private static void handleTakeNote(TakeNoteC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        final ServerPlayer player = ctx.getSender();
        if (player == null) return;

        final BlockPos pos = msg.pos();
        final int position = msg.noteIndex();

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
    }

    private static void handleUpdateNoteNbt(UpdateNoteNbtC2SPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        final ServerPlayer player = ctx.getSender();
        if (player == null) return;

        final int slot = packet.slot();
        final CompoundTag nbt = packet.nbt();

        if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof NotePaperItem) {
                stack.setTag(nbt.copy());
                player.getInventory().setChanged();
            }
        }
    }

    private static void handleTakePlacedNote(TakePlacedNoteC2SPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        final ServerPlayer player = ctx.getSender();
        if (player == null) return;

        final BlockPos pos = msg.pos();

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

        VillagerNameSyncPacket packet = new VillagerNameSyncPacket(allNames);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendOpenNoteScreenToClient(ServerPlayer player, BlockPos pos, int slot) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenNoteS2CPacket(pos, slot));
    }

    public static void sendOpenPlacedNoteScreen(ServerPlayer player, BlockPos pos) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenPlacedNoteS2CPacket(pos));
    }
}
