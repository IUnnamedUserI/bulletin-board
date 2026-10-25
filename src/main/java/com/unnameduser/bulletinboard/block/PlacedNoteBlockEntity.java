package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PlacedNoteBlockEntity extends BlockEntity {

    private NoteData noteData;
    private boolean takenByPlayer = false;

    public PlacedNoteBlockEntity(BlockPos pos, BlockState state) {
        // В NeoForge 1.21.1 PLACED_NOTE_ENTITY уже имеет тип BlockEntityType,
        // поэтому не нужно вызывать .get(), передаём напрямую
        super(ModBlockEntities.PLACED_NOTE_ENTITY, pos, state);
    }

    // ============ ГЕТТЕРЫ / СЕТТЕРЫ ============

    public NoteData getNoteData() {
        return noteData;
    }

    public void setNoteData(NoteData noteData) {
        this.noteData = noteData;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
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
        if (level == null || level.isClientSide) return;

        if (level.isRaining() || level.isThundering()) {
            if (level.canSeeSky(worldPosition.above())) {
                this.takenByPlayer = true;
                level.removeBlock(worldPosition, false);
            }
        }
    }

    // ============ NBT ============

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        if (noteData != null) {
            nbt.put("NoteData", noteData.toNbt());
        }
        nbt.putBoolean("TakenByPlayer", takenByPlayer);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        if (nbt.contains("NoteData")) {
            this.noteData = NoteData.fromNbt(nbt.getCompound("NoteData"));
        }
        this.takenByPlayer = nbt.getBoolean("TakenByPlayer");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}