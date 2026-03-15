package com.unnameduser.bulletinboard.network;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;

public record UpdateNoteNbtC2SPacket(int slot, NbtCompound nbt) {

    public UpdateNoteNbtC2SPacket(PacketByteBuf buf) {
        this(buf.readInt(), buf.readNbt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(slot);
        buf.writeNbt(nbt);
    }
}