package com.unnameduser.bulletinboard.util;

import com.unnameduser.bulletinboard.config.ModConfig;
import com.unnameduser.bulletinboard.config.NoteConfigLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.random.Random;

import java.util.List;
import java.util.stream.Collectors;

public class RandomNotePool {

    public enum NoteCategory {
        ANNOUNCEMENT(0xFFAA00),
        WARNING(0xFF5555),
        PERSONAL(0x55FFFF),
        QUEST(0x55FF55);

        public final int defaultBadgeColor;

        NoteCategory(int defaultBadgeColor) {
            this.defaultBadgeColor = defaultBadgeColor;
        }
    }

    /**
     * Генерирует случайную записку из конфига (без учёта профессии).
     */
    public static NoteData generateRandomNote(Random random, String author, String authorUuid, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotes();
        if (notes.isEmpty()) {
            return createFallbackNote(author, authorUuid, hasSeal);
        }

        var template = notes.get(random.nextInt(notes.size()));
        int badgeColor = -1;

        NoteData note = new NoteData(
                template.id,
                template.getTitle(),
                template.getContent(),
                author != null ? author : "Аноним",
                badgeColor,
                System.currentTimeMillis(),
                hasSeal,
                false
        );

        if (authorUuid != null && !authorUuid.isEmpty()) {
            note.setAuthorUuid(authorUuid);
        }

        return note;
    }

    /**
     * Генерирует случайную записку с учётом профессии жителя.
     */
    public static NoteData generateRandomNoteForProfession(Random random, String author, String authorUuid,
                                                           String professionId, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotesForProfession(professionId);

        if (notes.isEmpty()) {
            notes = NoteConfigLoader.getNotesForProfession(null);
        }

        if (notes.isEmpty()) {
            return createFallbackNote(author, authorUuid, hasSeal);
        }

        var template = notes.get(random.nextInt(notes.size()));
        int badgeColor = -1;

        // Определяем тип записки
        boolean isSmall = template.isSmall();

        // Если это маленькая записка — урезаем содержимое, если оно слишком длинное
        String content = template.getContent();
        if (isSmall && content.length() > 150) {
            content = content.substring(0, 150) + "...";
        }

        NoteData note = new NoteData(
                template.id,
                template.getTitle(),
                content,
                author != null ? author : "Аноним",
                badgeColor,
                System.currentTimeMillis(),
                hasSeal,
                isSmall  // ← передаём тип
        );

        if (authorUuid != null && !authorUuid.isEmpty()) {
            note.setAuthorUuid(authorUuid);
        }

        return note;
    }

    /**
     * Создаёт записку-заглушку, если конфиг не загружен.
     */
    private static NoteData createFallbackNote(String author, String authorUuid, boolean hasSeal) {
        String lang = getCurrentLanguage();
        String title = lang.equals("ru_ru") ? "Нет объявлений" : "No announcements";
        String content = lang.equals("ru_ru")
                ? "Администратор ещё не добавил объявления в config/bulletin-board/notes/"
                : "Admin has not added announcements to config/bulletin-board/notes/";

        NoteData note = new NoteData(
                null,
                title,
                content,
                author != null ? author : "Аноним",
                -1,
                System.currentTimeMillis(),
                hasSeal,
                false
        );

        if (authorUuid != null && !authorUuid.isEmpty()) {
            note.setAuthorUuid(authorUuid);
        }

        return note;
    }

    private static String getCurrentLanguage() {
        try {
            return MinecraftClient.getInstance().getLanguageManager().getLanguage();
        } catch (Exception e) {
            return "en_us";
        }
    }

    /**
     * Генерирует случайную записку с маленьким размером.
     */
    public static NoteData generateRandomSmallNote(Random random, String author, String authorUuid, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotes();
        if (notes.isEmpty()) {
            return createFallbackNote(author, authorUuid, hasSeal);
        }

        var template = notes.get(random.nextInt(notes.size()));
        int badgeColor = -1;

        String content = template.getContent();
        if (content.length() > 100) {
            content = content.substring(0, 100) + "...";
        }

        NoteData note = new NoteData(
                template.id,
                template.getTitle(),
                content,
                author != null ? author : "Аноним",
                badgeColor,
                System.currentTimeMillis(),
                hasSeal,
                true
        );

        if (authorUuid != null && !authorUuid.isEmpty()) {
            note.setAuthorUuid(authorUuid);
        }

        return note;
    }

    /**
     * Получение случайной записки только определённой категории.
     */
    public static NoteData generateNoteByCategory(NoteCategory category, Random random, String author, String authorUuid, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotesByCategory(category);
        if (notes.isEmpty()) {
            return generateRandomNote(random, author, authorUuid, hasSeal);
        }

        var template = notes.get(random.nextInt(notes.size()));
        int badgeColor = -1;

        NoteData note = new NoteData(
                template.id,
                template.getTitle(),
                template.getContent(),
                author != null ? author : "Аноним",
                badgeColor,
                System.currentTimeMillis(),
                hasSeal,
                false
        );

        if (authorUuid != null && !authorUuid.isEmpty()) {
            note.setAuthorUuid(authorUuid);
        }

        return note;
    }

    public static NoteData generateRandomSmallNoteForProfession(Random random, String author, String authorUuid,
                                                                String professionId, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotesForProfession(professionId);

        if (notes.isEmpty()) {
            notes = NoteConfigLoader.getNotesForProfession(null);
        }

        if (notes.isEmpty()) {
            return createFallbackNote(author, authorUuid, hasSeal);
        }

        // Фильтруем только маленькие записки
        var smallNotes = notes.stream()
                .filter(NoteConfigLoader.NoteTemplate::isSmall)
                .collect(Collectors.toList());

        if (smallNotes.isEmpty()) {
            // Если нет маленьких — берём обычную, но урезаем
            var template = notes.get(random.nextInt(notes.size()));
            String content = template.getContent();
            if (content.length() > ModConfig.getSmallContentMax()) {
                content = content.substring(0, ModConfig.getSmallContentMax()) + "...";
            }
            return createNoteFromTemplate(template, content, author, authorUuid, true, hasSeal);
        }

        var template = smallNotes.get(random.nextInt(smallNotes.size()));
        return createNoteFromTemplate(template, template.getContent(), author, authorUuid, true, hasSeal);
    }

    private static NoteData createNoteFromTemplate(NoteConfigLoader.NoteTemplate template, String content,
                                                   String author, String authorUuid, boolean isSmall, boolean hasSeal) {
        int badgeColor = -1;
        NoteData note = new NoteData(
                template.id,
                template.getTitle(),
                content,
                author != null ? author : "Аноним",
                badgeColor,
                System.currentTimeMillis(),
                hasSeal,
                isSmall
        );
        if (authorUuid != null && !authorUuid.isEmpty()) {
            note.setAuthorUuid(authorUuid);
        }
        return note;
    }
}