package com.unnameduser.bulletinboard.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Map;

public class VillagerNameSyncPacket {

    private final Map<String, String> names;

    public VillagerNameSyncPacket(Map<String, String> names) {
        this.names = names;
    }

    public VillagerNameSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> read = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String uuid = buf.readUtf();
            String name = buf.readUtf();
            read.put(uuid, name);
        }
        this.names = read;
    }

    public static VillagerNameSyncPacket read(FriendlyByteBuf buf) {
        return new VillagerNameSyncPacket(buf);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeInt(names.size());
        for (Map.Entry<String, String> entry : names.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public Map<String, String> getNames() {
        return names;
    }
}
