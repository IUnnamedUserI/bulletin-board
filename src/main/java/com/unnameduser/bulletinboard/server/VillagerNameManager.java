package com.unnameduser.bulletinboard.server;

import com.unnameduser.bulletinboard.config.VillagerNameConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerNameManager extends PersistentState {
    private static final String NAME = "bulletin_board_villager_names";
    // Храним UUID → КЛЮЧ перевода (а не само имя)
    private final Map<String, String> villagerNameKeys = new HashMap<>();

    public String getOrCreateNameKey(UUID villagerUuid) {
        String uuid = villagerUuid.toString();
        if (villagerNameKeys.containsKey(uuid)) {
            return villagerNameKeys.get(uuid);
        }

        String nameKey = VillagerNameConfig.getRandomNameKey();
        villagerNameKeys.put(uuid, nameKey);
        markDirty();
        return nameKey;
    }

    public String getNameKey(UUID villagerUuid) {
        return villagerNameKeys.getOrDefault(villagerUuid.toString(), "villager.name.default");
    }

    public Map<String, String> getAllNameKeys() {
        return new HashMap<>(villagerNameKeys);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound namesNbt = new NbtCompound();
        for (Map.Entry<String, String> entry : villagerNameKeys.entrySet()) {
            namesNbt.putString(entry.getKey(), entry.getValue());
        }
        nbt.put("VillagerNames", namesNbt);
        return nbt;
    }

    public static VillagerNameManager fromNbt(NbtCompound nbt) {
        VillagerNameManager manager = new VillagerNameManager();
        NbtCompound namesNbt = nbt.getCompound("VillagerNames");
        for (String key : namesNbt.getKeys()) {
            manager.villagerNameKeys.put(key, namesNbt.getString(key));
        }
        return manager;
    }

    public static VillagerNameManager get(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return manager.getOrCreate(
                VillagerNameManager::fromNbt,
                VillagerNameManager::new,
                NAME
        );
    }
}