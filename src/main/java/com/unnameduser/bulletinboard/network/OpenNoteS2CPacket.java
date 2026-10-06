package com.unnameduser.bulletinboard.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class OpenNoteS2CPacket {
    private final BlockPos pos;
    private final int slot;

    public OpenNoteS2CPacket(BlockPos pos, int slot) {
        this.pos = pos;
        this.slot = slot;
    }

    public OpenNoteS2CPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readInt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(slot);
    }

    public BlockPos pos() {
        return pos;
    }

    public int slot() {
        return slot;
    }
}
