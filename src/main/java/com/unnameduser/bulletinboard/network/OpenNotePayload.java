package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenNotePayload(BlockPos pos, int slot) implements CustomPayload {
    public static final CustomPayload.Id<OpenNotePayload> ID =
            new CustomPayload.Id<>(Identifier.of(BulletinBoardMod.MOD_ID, "open_note"));

    public static final PacketCodec<RegistryByteBuf, OpenNotePayload> CODEC =
            PacketCodec.tuple(BlockPos.PACKET_CODEC, OpenNotePayload::pos,
                    PacketCodecs.INTEGER, OpenNotePayload::slot,
                    OpenNotePayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}