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
    private static final List<String> RANDOM_NAME_KEYS = new ArrayList<>();
    private static int DISPLAY_RADIUS = 8;
    private static float NAME_SIZE = 1.0f;
    private static float PROFESSION_SIZE = 0.8f;
    private static int NAME_COLOR = 0xE8D0A0;
    private static int PROFESSION_COLOR = 0x55AAFF;

    static {
        // Ключи перевода для имён (будут использоваться в lang-файлах)
        RANDOM_NAME_KEYS.addAll(Arrays.asList(
                "villager.name.aethelred",
                "villager.name.beowulf",
                "villager.name.cedric",
                "villager.name.dunstan",
                "villager.name.eadric",
                "villager.name.godric",
                "villager.name.hrothgar",
                "villager.name.leofric",
                "villager.name.osric",
                "villager.name.wulfstan",
                "villager.name.ethelbert",
                "villager.name.aldric",
                "villager.name.baldwin",
                "villager.name.cuthbert",
                "villager.name.edmund",
                "villager.name.geoffrey",
                "villager.name.harold",
                "villager.name.leofwine",
                "villager.name.ordric",
                "villager.name.siward"
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
                RANDOM_NAME_KEYS.clear();
                RANDOM_NAME_KEYS.addAll(randomNames);
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

            // НОВЫЙ СПИСОК ИМЁН (70 штук)
            List<String> randomNameKeys = Arrays.asList(
                    "villager.name.william", "villager.name.richard", "villager.name.robert",
                    "villager.name.john", "villager.name.thomas", "villager.name.henry",
                    "villager.name.walter", "villager.name.ralph", "villager.name.hugh",
                    "villager.name.roger", "villager.name.geoffrey", "villager.name.adam",
                    "villager.name.simon", "villager.name.nicholas", "villager.name.peter",
                    "villager.name.alan", "villager.name.gilbert", "villager.name.edmund",
                    "villager.name.godfrey", "villager.name.aldric", "villager.name.osric",
                    "villager.name.wulfstan", "villager.name.leofric", "villager.name.earl",
                    "villager.name.martin", "villager.name.philip", "villager.name.stephen",
                    "villager.name.david", "villager.name.alexander", "villager.name.andrew",
                    "villager.name.benedict", "villager.name.christopher", "villager.name.constantin",
                    "villager.name.gregory", "villager.name.leonard", "villager.name.norman",
                    "villager.name.ranulf", "villager.name.reynold", "villager.name.reginald",
                    "villager.name.ambrose", "villager.name.archibald", "villager.name.barnaby",
                    "villager.name.bartholomew", "villager.name.cyril", "villager.name.dominic",
                    "villager.name.edgar", "villager.name.eric", "villager.name.everard",
                    "villager.name.frederick", "villager.name.giles", "villager.name.guy",
                    "villager.name.harold", "villager.name.herbert", "villager.name.horace",
                    "villager.name.humphrey", "villager.name.ivor", "villager.name.jasper",
                    "villager.name.jerome", "villager.name.jocelyn", "villager.name.julian",
                    "villager.name.lawrence", "villager.name.leander", "villager.name.leopold",
                    "villager.name.lionel", "villager.name.lucian", "villager.name.malcolm",
                    "villager.name.maurice", "villager.name.maximilian", "villager.name.miles",
                    "villager.name.morris", "villager.name.oliver"
            );
            config.put("random_names", randomNameKeys);

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

    /**
     * Возвращает случайный КЛЮЧ перевода для имени жителя.
     */
    public static String getRandomNameKey() {
        if (RANDOM_NAME_KEYS.isEmpty()) {
            return "villager.name.default";
        }
        return RANDOM_NAME_KEYS.get(new Random().nextInt(RANDOM_NAME_KEYS.size()));
    }

    @Deprecated
    public static String getRandomName() {
        if (RANDOM_NAME_KEYS.isEmpty()) {
            return "Villager";
        }
        // Просто возвращаем ключ, который потом будет переведён
        return RANDOM_NAME_KEYS.get(new Random().nextInt(RANDOM_NAME_KEYS.size()));
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