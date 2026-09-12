package com.unnameduser.bulletinboard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.unnameduser.bulletinboard.util.RandomNotePool;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class NoteConfigLoader {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("bulletin-board/notes");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, List<NoteTemplate>> PROFESSION_NOTES = new HashMap<>();
    private static List<NoteTemplate> commonNotes = new ArrayList<>();

    private static final String[] PROFESSIONS = {
            "farmer", "fisherman", "shepherd", "fletcher", "librarian",
            "cartographer", "cleric", "armorer", "weapon_smith", "tool_smith",
            "butcher", "leatherworker", "mason", "nitwit"
    };

    public static class NoteTemplate {
        public String id;
        public String category;
        public String noteType;
        public Map<String, String> title;
        public Map<String, String> content;

        public String getTitle() {
            String lang = getCurrentLanguage();
            return title.getOrDefault(lang, title.getOrDefault("en_us", "Unknown"));
        }

        public String getContent() {
            String lang = getCurrentLanguage();
            return content.getOrDefault(lang, content.getOrDefault("en_us", ""));
        }

        public boolean isSmall() {
            return "small".equalsIgnoreCase(noteType);
        }

        private String getCurrentLanguage() {
            try {
                return MinecraftClient.getInstance().getLanguageManager().getLanguage();
            } catch (Exception e) {
                return "en_us";
            }
        }

        public RandomNotePool.NoteCategory getCategory() {
            try {
                return RandomNotePool.NoteCategory.valueOf(category);
            } catch (IllegalArgumentException e) {
                return RandomNotePool.NoteCategory.ANNOUNCEMENT;
            }
        }
    }

    public static void load() {
        System.out.println("[Bulletin Board] Loading notes config from: " + CONFIG_DIR);

        CONFIG_DIR.toFile().mkdirs();

        // Копируем дефолтные файлы из ресурсов, если их нет
        copyDefaultIfMissing("common.json");
        for (String profession : PROFESSIONS) {
            copyDefaultIfMissing(profession + ".json");
        }

        // Загружаем общие записки
        commonNotes = loadFile(CONFIG_DIR.resolve("common.json"));

        // Загружаем записки по профессиям
        for (String profession : PROFESSIONS) {
            Path profPath = CONFIG_DIR.resolve(profession + ".json");
            if (profPath.toFile().exists()) {
                PROFESSION_NOTES.put(profession, loadFile(profPath));
            }
        }

        System.out.println("[Bulletin Board] Notes config loaded successfully!");

        System.out.println("[Bulletin Board] commonNotes size: " + commonNotes.size());
        for (Map.Entry<String, List<NoteTemplate>> entry : PROFESSION_NOTES.entrySet()) {
            System.out.println("[Bulletin Board] Profession " + entry.getKey() + ": " + entry.getValue().size() + " notes");
        }
    }

    private static void copyDefaultIfMissing(String fileName) {
        Path target = CONFIG_DIR.resolve(fileName);
        if (target.toFile().exists()) {
            return; // Файл уже есть — не перезаписываем
        }

        // Пытаемся скопировать из ресурсов мода
        try (InputStream in = NoteConfigLoader.class.getResourceAsStream("/default_notes/" + fileName)) {
            if (in != null) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[Bulletin Board] Created default: " + fileName);
            } else {
                System.err.println("[Bulletin Board] Default file not found in resources: " + fileName);
                // Создаём пустой файл-заглушку, чтобы не было ошибок
                Files.createFile(target);
                System.err.println("[Bulletin Board] Created empty fallback file: " + fileName);
            }
        } catch (Exception e) {
            System.err.println("[Bulletin Board] Failed to copy default notes: " + fileName + " — " + e.getMessage());
        }
    }

    private static List<NoteTemplate> loadFile(Path path) {
        try (Reader reader = new FileReader(path.toFile())) {
            List<Map<String, Object>> data = GSON.fromJson(reader, new TypeToken<List<Map<String, Object>>>(){}.getType());
            List<NoteTemplate> notes = new ArrayList<>();

            for (var noteData : data) {
                NoteTemplate note = new NoteTemplate();
                note.id = (String) noteData.get("id");
                note.category = (String) noteData.get("category");
                note.noteType = (String) noteData.get("note_type");
                note.title = (Map<String, String>) noteData.get("title");
                note.content = (Map<String, String>) noteData.get("content");
                notes.add(note);
            }
            return notes;
        } catch (Exception e) {
            System.err.println("[Bulletin Board] Failed to load notes from " + path + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // === ГЕТТЕРЫ ===

    public static List<NoteTemplate> getNotes() {
        List<NoteTemplate> allNotes = new ArrayList<>();
        allNotes.addAll(commonNotes);
        for (List<NoteTemplate> notes : PROFESSION_NOTES.values()) {
            allNotes.addAll(notes);
        }
        return allNotes;
    }

    public static List<NoteTemplate> getNotesForProfession(String professionId) {
        List<NoteTemplate> notes = new ArrayList<>();
        notes.addAll(commonNotes);

        if (professionId != null) {
            String profession = professionId.replace("minecraft:", "");
            if (PROFESSION_NOTES.containsKey(profession)) {
                notes.addAll(PROFESSION_NOTES.get(profession));
            }
        }

        return notes;
    }

    public static NoteTemplate getNoteById(String id) {
        for (NoteTemplate note : commonNotes) {
            if (note.id.equals(id)) return note;
        }
        for (List<NoteTemplate> notes : PROFESSION_NOTES.values()) {
            for (NoteTemplate note : notes) {
                if (note.id.equals(id)) return note;
            }
        }
        return null;
    }

    public static List<NoteTemplate> getNotesByCategory(RandomNotePool.NoteCategory category) {
        List<NoteTemplate> result = new ArrayList<>();
        for (NoteTemplate note : commonNotes) {
            if (note.getCategory() == category) result.add(note);
        }
        for (List<NoteTemplate> notes : PROFESSION_NOTES.values()) {
            for (NoteTemplate note : notes) {
                if (note.getCategory() == category) result.add(note);
            }
        }
        return result;
    }
}