package com.unnameduser.bulletinboard.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class OpenPlacedNoteS2CPacket {
    private final BlockPos pos;

    public OpenPlacedNoteS2CPacket(BlockPos pos) {
        this.pos = pos;
    }

    public OpenPlacedNoteS2CPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public BlockPos pos() {
        return pos;
    }
}
