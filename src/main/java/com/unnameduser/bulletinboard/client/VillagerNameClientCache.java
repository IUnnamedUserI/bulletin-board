package com.unnameduser.bulletinboard.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerNameClientCache {
    private static final Map<String, String> NAMES = new HashMap<>();

    public static void updateNames(Map<String, String> names) {
        NAMES.clear();
        NAMES.putAll(names);
    }

    public static String getName(UUID villagerUuid) {
        return NAMES.getOrDefault(villagerUuid.toString(), "Villager");
    }

    public static void putName(String uuid, String name) {
        NAMES.put(uuid, name);
    }
}