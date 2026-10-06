package com.unnameduser.bulletinboard.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public final class UpdateNoteNbtC2SPacket {
    private final int slot;
    private final CompoundTag nbt;

    public UpdateNoteNbtC2SPacket(int slot, CompoundTag nbt) {
        this.slot = slot;
        this.nbt = nbt;
    }

    public UpdateNoteNbtC2SPacket(FriendlyByteBuf buf) {
        this(buf.readInt(), buf.readNbt());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(slot);
        buf.writeNbt(nbt);
    }

    public int slot() {
        return slot;
    }

    public CompoundTag nbt() {
        return nbt;
    }
}
