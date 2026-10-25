package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TakePlacedNoteC2SPacket(BlockPos pos) implements CustomPacketPayload {

    public static final Type<TakePlacedNoteC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "take_placed_note"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakePlacedNoteC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TakePlacedNoteC2SPacket::pos,
                    TakePlacedNoteC2SPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}