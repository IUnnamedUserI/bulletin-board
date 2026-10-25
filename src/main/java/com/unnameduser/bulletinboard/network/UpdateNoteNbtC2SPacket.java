package com.unnameduser.bulletinboard.network;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateNoteNbtC2SPacket(int slot, CompoundTag nbt) implements CustomPacketPayload {

    public static final Type<UpdateNoteNbtC2SPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "update_note_nbt"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateNoteNbtC2SPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, UpdateNoteNbtC2SPacket::slot,
                    ByteBufCodecs.COMPOUND_TAG, UpdateNoteNbtC2SPacket::nbt,
                    UpdateNoteNbtC2SPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}