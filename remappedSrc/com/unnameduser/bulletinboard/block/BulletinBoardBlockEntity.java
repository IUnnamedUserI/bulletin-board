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
    private List<NoteSlot> slots = new ArrayList<>();
    private static final int BIG_SLOT = 4;
    private static final long NOTE_LIFETIME = 3600000; // 1 час

    public BulletinBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BULLETIN_BOARD_ENTITY, pos, state);
    }

    public static class NoteSlot {
        public final NoteData note;
        public final int startSlot;
        public final int slotCount;

        public NoteSlot(NoteData note, int startSlot) {
            this.note = note;
            this.startSlot = startSlot;
            this.slotCount = note.isSmall() ? 1 : 2;
        }

        public boolean occupies(int slot) {
            return slot >= startSlot && slot < startSlot + slotCount;
        }
    }

    // Добавленный метод
    private boolean isSlotFree(int slot) {
        for (NoteSlot s : slots) {
            if (s.occupies(slot)) return false;
        }
        return true;
    }

    public boolean canPlaceNote(NoteData note, int targetSlot) {
        if (targetSlot == BIG_SLOT) {
            return !note.isSmall() && isSlotFree(BIG_SLOT);
        }

        if (note.isSmall()) {
            return isSlotFree(targetSlot);
        } else {
            if (targetSlot == 0 || targetSlot == 1) {
                return isSlotFree(0) && isSlotFree(1);
            } else if (targetSlot == 2 || targetSlot == 3) {
                return isSlotFree(2) && isSlotFree(3);
            }
        }
        return false;
    }

    public boolean addNoteAtPosition(NoteData note, int targetSlot) {
        if (!canPlaceNote(note, targetSlot)) return false;

        int startSlot;
        if (targetSlot == BIG_SLOT) {
            startSlot = BIG_SLOT;
        } else if (note.isSmall()) {
            startSlot = targetSlot;
        } else {
            startSlot = (targetSlot == 0 || targetSlot == 1) ? 0 : 2;
        }

        slots.add(new NoteSlot(note, startSlot));
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
        return true;
    }

    public void removeNote(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < slots.size()) {
            slots.remove(slotIndex);
            markDirty();
            if (world != null && !world.isClient) {
                world.updateListeners(pos, getCachedState(), getCachedState(), 3);
            }
        }
    }

    public NoteData getNoteAtPosition(int slot) {
        for (NoteSlot s : slots) {
            if (s.occupies(slot)) {
                return s.note;
            }
        }
        return null;
    }

    public int getNoteIndexByPosition(int slot) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).occupies(slot)) {
                return i;
            }
        }
        return -1;
    }

    public List<NoteData> getNotes() {
        List<NoteData> notes = new ArrayList<>();
        for (NoteSlot s : slots) {
            notes.add(s.note);
        }
        return notes;
    }

    public List<Integer> getNotePositions() {
        List<Integer> positions = new ArrayList<>();
        for (NoteSlot s : slots) {
            positions.add(s.startSlot);
        }
        return positions;
    }

    public boolean isPositionFree(int slot) {
        return isSlotFree(slot);
    }

    public void tick() {
        if (world == null || world.isClient) return;

        boolean changed = false;
        long currentTime = System.currentTimeMillis();

        for (int i = slots.size() - 1; i >= 0; i--) {
            NoteSlot slot = slots.get(i);
            NoteData note = slot.note;
            if (!note.hasSeal() && (currentTime - note.getCreationTime() > NOTE_LIFETIME)) {
                slots.remove(i);
                changed = true;
            }
        }

        if (changed) {
            markDirty();
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public boolean isNoteStillValid(NoteData note) {
        if (note.hasSeal()) return true;
        long currentTime = System.currentTimeMillis();
        return (currentTime - note.getCreationTime() <= NOTE_LIFETIME);
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        NbtList slotsList = new NbtList();

        for (NoteSlot slot : slots) {
            NbtCompound slotNbt = new NbtCompound();
            slotNbt.put("Note", slot.note.toNbt());
            slotNbt.putInt("StartSlot", slot.startSlot);
            slotsList.add(slotNbt);
        }

        nbt.put("Slots", slotsList);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        slots.clear();

        NbtList slotsList = nbt.getList("Slots", 10);

        for (int i = 0; i < slotsList.size(); i++) {
            NbtCompound slotNbt = slotsList.getCompound(i);
            NoteData note = NoteData.fromNbt(slotNbt.getCompound("Note"));
            int startSlot = slotNbt.getInt("StartSlot");
            slots.add(new NoteSlot(note, startSlot));
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
}