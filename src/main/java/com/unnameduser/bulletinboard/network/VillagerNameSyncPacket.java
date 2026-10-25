package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public record VillagerNameSyncPacket(Map<String, String> names) implements CustomPacketPayload {

    public static final Type<VillagerNameSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "villager_name_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerNameSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
                    VillagerNameSyncPacket::names,
                    VillagerNameSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}