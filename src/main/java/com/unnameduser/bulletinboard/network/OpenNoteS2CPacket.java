package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenNoteS2CPacket(BlockPos pos, int slot) implements CustomPacketPayload {

    public static final Type<OpenNoteS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "open_note"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNoteS2CPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenNoteS2CPacket::pos,
                    ByteBufCodecs.INT, OpenNoteS2CPacket::slot,
                    OpenNoteS2CPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}