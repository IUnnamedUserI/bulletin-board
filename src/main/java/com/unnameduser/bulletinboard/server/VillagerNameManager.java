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
    private final Map<String, String> villagerNames = new HashMap<>();

    public String getOrCreateName(UUID villagerUuid) {
        String uuid = villagerUuid.toString();
        if (villagerNames.containsKey(uuid)) {
            return villagerNames.get(uuid);
        }

        String name = VillagerNameConfig.getRandomName();
        villagerNames.put(uuid, name);
        markDirty();
        return name;
    }

    public String getName(UUID villagerUuid) {
        return villagerNames.getOrDefault(villagerUuid.toString(), "Villager");
    }

    public Map<String, String> getAllNames() {
        return new HashMap<>(villagerNames);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound namesNbt = new NbtCompound();
        for (Map.Entry<String, String> entry : villagerNames.entrySet()) {
            namesNbt.putString(entry.getKey(), entry.getValue());
        }
        nbt.put("VillagerNames", namesNbt);
        return nbt;
    }

    public static VillagerNameManager fromNbt(NbtCompound nbt) {
        VillagerNameManager manager = new VillagerNameManager();
        NbtCompound namesNbt = nbt.getCompound("VillagerNames");
        for (String key : namesNbt.getKeys()) {
            manager.villagerNames.put(key, namesNbt.getString(key));
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