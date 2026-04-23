package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UpdateNoteNbtPayload(int slot, NbtCompound nbt) implements CustomPayload {
    public static final CustomPayload.Id<UpdateNoteNbtPayload> ID =
            new CustomPayload.Id<>(Identifier.of(BulletinBoardMod.MOD_ID, "update_note_nbt"));

    public static final PacketCodec<RegistryByteBuf, UpdateNoteNbtPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.INTEGER, UpdateNoteNbtPayload::slot,
                    PacketCodecs.NBT_COMPOUND, UpdateNoteNbtPayload::nbt,
                    UpdateNoteNbtPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}