package com.unnameduser.bulletinboard.config;

import blue.endless.jankson.Jankson;
import blue.endless.jankson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ModConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("bulletin-board/config.json5");
    private static final Jankson JANKSON = Jankson.builder().build();

    // ============ СТРУКТУРА КОНФИГА ============

    public static class NoteLimits {
        public int full_title_max = 48;
        public int full_content_max = 512;
        public int small_title_max = 24;
        public int small_content_max = 300;
    }

    public static class SchedulerConfig {
        public int interval_ticks = 24000;
        public double spawn_chance = 0.3;
    }

    public static class StructureConfig {
        public boolean generate_bulletin_board = true;
    }

    public static class VillagerDisplayConfig {
        public boolean show_villager_names = true;
    }

    public NoteLimits note_limits = new NoteLimits();
    public SchedulerConfig scheduler = new SchedulerConfig();
    public StructureConfig structure = new StructureConfig();
    public VillagerDisplayConfig villager_display = new VillagerDisplayConfig();

    private static ModConfig INSTANCE;

    // ============ ЗАГРУЗКА / СОХРАНЕНИЕ ============

    public static ModConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        try {
            CONFIG_PATH.getParent().toFile().mkdirs();
            if (!CONFIG_PATH.toFile().exists()) {
                INSTANCE = new ModConfig();
                save();
                System.out.println("[Bulletin Board] Created default config.json5");
                return;
            }

            JsonObject json = JANKSON.load(CONFIG_PATH.toFile());
            INSTANCE = JANKSON.fromJson(json, ModConfig.class);
            System.out.println("[Bulletin Board] Loaded config.json5");
        } catch (Exception e) {
            System.err.println("[Bulletin Board] Failed to load config: " + e.getMessage());
            INSTANCE = new ModConfig();
        }
    }

    public static void save() {
        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            JsonObject json = (JsonObject) JANKSON.toJson(INSTANCE);
            String output = json.toJson(true, true);

            // Вставляем комментарии на английском
            output = output.replace("\"full_title_max\"",
                    "// Maximum characters in the title of a full-size note\n  \"full_title_max\"");
            output = output.replace("\"full_content_max\"",
                    "// Maximum characters in the content of a full-size note\n  \"full_content_max\"");
            output = output.replace("\"small_title_max\"",
                    "// Maximum characters in the title of a small note\n  \"small_title_max\"");
            output = output.replace("\"small_content_max\"",
                    "// Maximum characters in the content of a small note\n  \"small_content_max\"");
            output = output.replace("\"interval_ticks\"",
                    "// Interval between note generation attempts in ticks (20 ticks = 1 second). 24000 = 20 minutes\n  \"interval_ticks\"");
            output = output.replace("\"spawn_chance\"",
                    "// Chance for a note to appear on a board (0.0 - 1.0). Recommended: 0.1 - 1.0.\n  // Set to 0 to completely disable note generation by villagers\n  \"spawn_chance\"");
            output = output.replace("\"generate_bulletin_board\"",
                    "// Whether to generate bulletin boards in villages. true = yes, false = no\n  \"generate_bulletin_board\"");
            output = output.replace("\"show_villager_names\"",
                    "// Whether to show villager names and professions above their heads. true = yes, false = no\n  \"show_villager_names\"");

            writer.write(output);
        } catch (IOException e) {
            System.err.println("[Bulletin Board] Failed to save config: " + e.getMessage());
        }
    }

    // ============ ГЕТТЕРЫ ============

    public static int getFullTitleMax() { return get().note_limits.full_title_max; }
    public static int getFullContentMax() { return get().note_limits.full_content_max; }
    public static int getSmallTitleMax() { return get().note_limits.small_title_max; }
    public static int getSmallContentMax() { return get().note_limits.small_content_max; }
    public static int getIntervalTicks() { return get().scheduler.interval_ticks; }
    public static double getSpawnChance() { return get().scheduler.spawn_chance; }
    public static boolean isGenerateBulletinBoard() { return get().structure.generate_bulletin_board; }
    public static boolean isShowVillagerNames() { return get().villager_display.show_villager_names; }
}