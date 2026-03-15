package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BulletinBoardBlockEntity extends BlockEntity {
    private List<NoteData> notes = new ArrayList<>();
    private List<Integer> notePositions = new ArrayList<>();
    private static final int MAX_NOTES = 3;
    private static final long NOTE_LIFETIME = 3600000;

    public BulletinBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BULLETIN_BOARD_ENTITY, pos, state);
    }

    public void tick() {
        if (world == null || world.isClient) return;

        boolean changed = false;
        long currentTime = System.currentTimeMillis();

        for (int i = notes.size() -1; i >= 0; i--) {
            NoteData note = notes.get(i);
            if (!note.hasSeal() && (currentTime - note.getCreationTime()) > NOTE_LIFETIME) {
                notes.remove(i);
                notePositions.remove(i);
                changed = true;
            }
        }

        if (changed) {
            markDirty();
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public boolean addNoteAtPosition(NoteData note, int position) {
        if (notes.size() < MAX_NOTES && position >= 0 && position < MAX_NOTES && !notePositions.contains(position)) {
            notes.add(note);
            notePositions.add(position);

            markDirty();

            if (world != null && !world.isClient) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            }

            return true;
        }

        return false;
    }

    public void removeNote(int index) {

        if (index >= 0 && index < notes.size()) {
            notes.remove(index);
            notePositions.remove(index);

            markDirty();
            if (world != null && !world.isClient) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            }
        }
    }

    public NoteData getNoteAtPosition(int position) {
        int idx = notePositions.indexOf(position);
        return idx >= 0 ? notes.get(idx) : null;
    }

    public int getNoteIndexByPosition(int position) {
        return notePositions.indexOf(position);
    }

    public List<NoteData> getNotes() {
        return notes;
    }

    public List<Integer> getNotePositions() {
        return notePositions;
    }

    public boolean isPositionFree(int position) {
        return !notePositions.contains(position);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        NbtList notesList = new NbtList();
        NbtList positionsList = new NbtList();

        for (int i = 0; i < notes.size(); i++) {
            notesList.add(notes.get(i).toNbt());
            positionsList.add(NbtInt.of(notePositions.get(i)));
        }

        nbt.put("Notes", notesList);
        nbt.put("Positions", positionsList);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        notes.clear();
        notePositions.clear();

        NbtList notesList = nbt.getList("Notes", 10);
        NbtList positionsList = nbt.getList("Positions", 3);

        for (int i = 0; i < notesList.size(); i++) {
            notes.add(NoteData.fromNbt(notesList.getCompound(i)));
            if (i < positionsList.size()) {
                notePositions.add(positionsList.getInt(i));
            }
        }
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

    public boolean isNoteStillValid(NoteData note) {
        if (note.hasSeal()) return  true;
        long currentTime = System.currentTimeMillis();
        return (currentTime - note.getCreationTime() <= NOTE_LIFETIME);
    }
}