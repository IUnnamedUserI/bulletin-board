package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TakeNoteC2SPacket(BlockPos pos, int noteIndex) implements CustomPacketPayload {

    public static final Type<TakeNoteC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "take_note"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TakeNoteC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, TakeNoteC2SPacket::pos,
                    ByteBufCodecs.INT, TakeNoteC2SPacket::noteIndex,
                    TakeNoteC2SPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}