package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenPlacedNoteS2CPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<OpenPlacedNoteS2CPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "open_placed_note"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPlacedNoteS2CPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, OpenPlacedNoteS2CPacket::pos,
                    OpenPlacedNoteS2CPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}