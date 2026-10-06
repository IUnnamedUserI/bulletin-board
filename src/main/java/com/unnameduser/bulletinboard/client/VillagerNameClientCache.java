package com.unnameduser.bulletinboard.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerNameClientCache {
    private static final Map<String, String> NAME_KEYS = new HashMap<>();

    public static void updateNames(Map<String, String> nameKeys) {
        NAME_KEYS.clear();
        NAME_KEYS.putAll(nameKeys);
    }

    public static String getNameKey(UUID villagerUuid) {
        return NAME_KEYS.getOrDefault(villagerUuid.toString(), "villager.name.default");
    }

    public static void putNameKey(String uuid, String nameKey) {
        NAME_KEYS.put(uuid, nameKey);
    }
}
