package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class PlacedNoteBlockEntity extends BlockEntity {

    private NoteData noteData;
    private boolean takenByPlayer = false;

    public PlacedNoteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_NOTE_ENTITY, pos, state);
    }

    // ============ ГЕТТЕРЫ / СЕТТЕРЫ ============

    public NoteData getNoteData() {
        return noteData;
    }

    public void setNoteData(NoteData noteData) {
        this.noteData = noteData;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public boolean isTakenByPlayer() {
        return takenByPlayer;
    }

    public void setTakenByPlayer(boolean taken) {
        this.takenByPlayer = taken;
    }

    // ============ ТИК ============

    public void tick() {
        if (world == null || world.isClient) return;

        if (world.isRaining() || world.isThundering()) {
            if (world.isSkyVisible(pos.up())) {
                // Помечаем, что дроп не нужен
                this.takenByPlayer = true;
                world.removeBlock(pos, false);
            }
        }
    }

    // ============ NBT ============

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (noteData != null) {
            nbt.put("NoteData", noteData.toNbt());
        }
        nbt.putBoolean("TakenByPlayer", takenByPlayer);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.contains("NoteData")) {
            this.noteData = NoteData.fromNbt(nbt.getCompound("NoteData"));
        }
        this.takenByPlayer = nbt.getBoolean("TakenByPlayer");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}