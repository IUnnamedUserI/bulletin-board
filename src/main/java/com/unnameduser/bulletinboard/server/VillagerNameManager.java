package com.unnameduser.bulletinboard.server;

import com.unnameduser.bulletinboard.config.VillagerNameConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerNameManager extends SavedData {
    private static final String NAME = "bulletin_board_villager_names";

    // Фабрика для SavedData в 1.21.1
    private static final SavedData.Factory<VillagerNameManager> FACTORY = new SavedData.Factory<>(
            VillagerNameManager::new,
            VillagerNameManager::load
    );

    // Храним UUID → КЛЮЧ перевода (а не само имя)
    private final Map<String, String> villagerNameKeys = new HashMap<>();

    public VillagerNameManager() {
    }

    public String getOrCreateNameKey(UUID villagerUuid) {
        String uuid = villagerUuid.toString();
        if (villagerNameKeys.containsKey(uuid)) {
            return villagerNameKeys.get(uuid);
        }

        String nameKey = VillagerNameConfig.getRandomNameKey();
        villagerNameKeys.put(uuid, nameKey);
        setDirty();
        return nameKey;
    }

    public String getNameKey(UUID villagerUuid) {
        return villagerNameKeys.getOrDefault(villagerUuid.toString(), "villager.name.default");
    }

    public Map<String, String> getAllNameKeys() {
        return new HashMap<>(villagerNameKeys);
    }

    @Override
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag namesNbt = new CompoundTag();
        for (Map.Entry<String, String> entry : villagerNameKeys.entrySet()) {
            namesNbt.putString(entry.getKey(), entry.getValue());
        }
        nbt.put("VillagerNames", namesNbt);
        return nbt;
    }

    /**
     * В 1.21.1 статический метод загрузки должен принимать два аргумента:
     * CompoundTag и HolderLookup.Provider.
     */
    public static VillagerNameManager load(CompoundTag nbt, HolderLookup.Provider registries) {
        VillagerNameManager manager = new VillagerNameManager();
        CompoundTag namesNbt = nbt.getCompound("VillagerNames");
        for (String key : namesNbt.getAllKeys()) {
            manager.villagerNameKeys.put(key, namesNbt.getString(key));
        }
        return manager;
    }

    public static VillagerNameManager get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }
}