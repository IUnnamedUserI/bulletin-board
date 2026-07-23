package com.unnameduser.bulletinboard.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

import static com.unnameduser.bulletinboard.BulletinBoardMod.MOD_ID;

public class VillagerNameSyncPacket {
    public static final Identifier ID = new Identifier(MOD_ID, "villager_name_sync");

    private final Map<String, String> names;

    public VillagerNameSyncPacket(Map<String, String> names) {
        this.names = names;
    }

    public static VillagerNameSyncPacket read(PacketByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> names = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String uuid = buf.readString();
            String name = buf.readString();
            names.put(uuid, name);
        }
        return new VillagerNameSyncPacket(names);
    }

    public void write(PacketByteBuf buf) {
        buf.writeInt(names.size());
        for (Map.Entry<String, String> entry : names.entrySet()) {
            buf.writeString(entry.getKey());
            buf.writeString(entry.getValue());
        }
    }

    public Map<String, String> getNames() {
        return names;
    }
}