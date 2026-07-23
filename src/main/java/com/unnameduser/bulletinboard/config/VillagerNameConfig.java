package com.unnameduser.bulletinboard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

public class VillagerNameConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("bulletin-board/villager_names.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, String> NAMES = new HashMap<>();
    private static final List<String> RANDOM_NAMES = new ArrayList<>();
    private static int DISPLAY_RADIUS = 8;
    private static float NAME_SIZE = 1.0f;
    private static float PROFESSION_SIZE = 0.8f;
    private static int NAME_COLOR = 0xE8D0A0;
    private static int PROFESSION_COLOR = 0x55AAFF;

    static {
        RANDOM_NAMES.addAll(Arrays.asList(
                "Aethelred", "Beowulf", "Cedric", "Dunstan", "Eadric",
                "Godric", "Hrothgar", "Leofric", "Osric", "Wulfstan",
                "Ethelbert", "Aldric", "Baldwin", "Cuthbert", "Edmund",
                "Geoffrey", "Harold", "Leofwine", "Ordric", "Siward"
        ));
    }

    public static void load() {
        if (!CONFIG_PATH.toFile().exists()) {
            createDefaultConfig();
        }

        try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
            Map<String, Object> data = GSON.fromJson(reader, Map.class);
            if (data == null) return;

            if (data.containsKey("names")) {
                Map<String, String> namesMap = (Map<String, String>) data.get("names");
                NAMES.clear();
                NAMES.putAll(namesMap);
            }

            if (data.containsKey("random_names")) {
                List<String> randomNames = (List<String>) data.get("random_names");
                RANDOM_NAMES.clear();
                RANDOM_NAMES.addAll(randomNames);
            }

            if (data.containsKey("display_radius")) {
                DISPLAY_RADIUS = ((Number) data.get("display_radius")).intValue();
            }
            if (data.containsKey("display_name_size")) {
                NAME_SIZE = ((Number) data.get("display_name_size")).floatValue();
            }
            if (data.containsKey("display_profession_size")) {
                PROFESSION_SIZE = ((Number) data.get("display_profession_size")).floatValue();
            }
            if (data.containsKey("display_name_color")) {
                NAME_COLOR = parseColor((String) data.get("display_name_color"));
            }
            if (data.containsKey("display_profession_color")) {
                PROFESSION_COLOR = parseColor((String) data.get("display_profession_color"));
            }

        } catch (Exception e) {
            System.err.println("Failed to load villager names config: " + e.getMessage());
        }
    }

    private static void createDefaultConfig() {
        try {
            CONFIG_PATH.getParent().toFile().mkdirs();
            Map<String, Object> config = new LinkedHashMap<>();

            Map<String, String> names = new LinkedHashMap<>();
            names.put("minecraft:farm", "Farmer");
            names.put("minecraft:fisherman", "Fisherman");
            names.put("minecraft:shepherd", "Shepherd");
            names.put("minecraft:fletcher", "Fletcher");
            names.put("minecraft:librarian", "Librarian");
            names.put("minecraft:cartographer", "Cartographer");
            names.put("minecraft:cleric", "Cleric");
            names.put("minecraft:armorer", "Armorer");
            names.put("minecraft:weapon_smith", "Weaponsmith");
            names.put("minecraft:tool_smith", "Toolsmith");
            names.put("minecraft:butcher", "Butcher");
            names.put("minecraft:leatherworker", "Leatherworker");
            names.put("minecraft:mason", "Mason");
            names.put("minecraft:nitwit", "Nitwit");
            config.put("names", names);

            List<String> randomNames = Arrays.asList(
                    "Aethelred", "Beowulf", "Cedric", "Dunstan", "Eadric",
                    "Godric", "Hrothgar", "Leofric", "Osric", "Wulfstan",
                    "Ethelbert", "Aldric", "Baldwin", "Cuthbert", "Edmund",
                    "Geoffrey", "Harold", "Leofwine", "Ordric", "Siward"
            );
            config.put("random_names", randomNames);

            config.put("display_name_color", "#E8D0A0");
            config.put("display_profession_color", "#55AAFF");
            config.put("display_name_size", 1.0f);
            config.put("display_profession_size", 0.8f);
            config.put("display_radius", 8);

            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            System.err.println("Failed to create default villager names config: " + e.getMessage());
        }
    }

    private static int parseColor(String hex) {
        try {
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            return Integer.parseInt(hex, 16);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    public static String getProfessionName(String professionId) {
        return NAMES.getOrDefault(professionId, professionId);
    }

    public static String getRandomName() {
        if (RANDOM_NAMES.isEmpty()) {
            return "Villager";
        }
        return RANDOM_NAMES.get(new Random().nextInt(RANDOM_NAMES.size()));
    }

    public static int getDisplayRadius() {
        return DISPLAY_RADIUS;
    }

    public static float getNameSize() {
        return NAME_SIZE;
    }

    public static float getProfessionSize() {
        return PROFESSION_SIZE;
    }

    public static int getNameColor() {
        return NAME_COLOR;
    }

    public static int getProfessionColor() {
        return PROFESSION_COLOR;
    }
}