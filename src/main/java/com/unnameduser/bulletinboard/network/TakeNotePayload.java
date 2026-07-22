package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record TakeNotePayload(BlockPos pos, int slot) implements CustomPayload {
    public static final CustomPayload.Id<TakeNotePayload> ID =
            new CustomPayload.Id<>(Identifier.of(BulletinBoardMod.MOD_ID, "take_note"));

    public static final PacketCodec<RegistryByteBuf, TakeNotePayload> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, TakeNotePayload::pos,
                    PacketCodecs.INTEGER, TakeNotePayload::slot,
                    TakeNotePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}