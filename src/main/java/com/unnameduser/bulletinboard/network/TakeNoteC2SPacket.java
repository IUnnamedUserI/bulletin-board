package com.unnameduser.bulletinboard.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public record TakeNoteC2SPacket(BlockPos pos, int noteIndex) {

    public TakeNoteC2SPacket(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readInt());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(noteIndex);
    }
}