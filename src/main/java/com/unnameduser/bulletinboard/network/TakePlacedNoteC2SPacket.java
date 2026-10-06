package com.unnameduser.bulletinboard.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public final class TakePlacedNoteC2SPacket {
    private final BlockPos pos;

    public TakePlacedNoteC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public TakePlacedNoteC2SPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public BlockPos pos() {
        return pos;
    }
}
