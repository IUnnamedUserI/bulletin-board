package com.unnameduser.bulletinboard.util;

import com.unnameduser.bulletinboard.config.ModConfig;
import com.unnameduser.bulletinboard.config.NoteConfigLoader;
import net.minecraft.util.RandomSource;

import java.util.List;
import java.util.stream.Collectors;

public class RandomNotePool {

    public enum NoteCategory {
        ANNOUNCEMENT(0xFFAA00), WARNING(0xFF5555), PERSONAL(0x55FFFF), QUEST(0x55FF55);
        public final int defaultBadgeColor;
        NoteCategory(int defaultBadgeColor) { this.defaultBadgeColor = defaultBadgeColor; }
    }

    public static NoteData generateRandomNote(RandomSource random, String author, String authorUuid, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotes();
        if (notes.isEmpty()) return createFallbackNote(author, authorUuid, hasSeal);
        var template = notes.get(random.nextInt(notes.size()));
        return createNoteFromTemplate(template, template.getDefaultContent(), author, authorUuid, template.isSmall(), hasSeal);
    }

    public static NoteData generateRandomNoteForProfession(RandomSource random, String author, String authorUuid,
                                                           String professionId, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotesForProfession(professionId);
        if (notes.isEmpty()) notes = NoteConfigLoader.getNotesForProfession(null);
        if (notes.isEmpty()) return createFallbackNote(author, authorUuid, hasSeal);

        var template = notes.get(random.nextInt(notes.size()));
        boolean isSmall = template.isSmall();
        String content = template.getDefaultContent();
        if (isSmall && content.length() > 150) content = content.substring(0, 150) + "...";

        return createNoteFromTemplate(template, content, author, authorUuid, isSmall, hasSeal);
    }

    private static NoteData createFallbackNote(String author, String authorUuid, boolean hasSeal) {
        // Серверная заглушка — всегда en_us, перевод на клиенте
        NoteData note = new NoteData(null, "No announcements",
                "Admin has not added announcements to config/bulletin-board/notes/",
                author != null ? author : "Anonymous", -1,
                System.currentTimeMillis(), hasSeal, false);
        if (authorUuid != null && !authorUuid.isEmpty()) note.setAuthorUuid(authorUuid);
        return note;
    }

    public static NoteData generateRandomSmallNote(RandomSource random, String author, String authorUuid, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotes();
        if (notes.isEmpty()) return createFallbackNote(author, authorUuid, hasSeal);
        var template = notes.get(random.nextInt(notes.size()));
        String content = template.getDefaultContent();
        if (content.length() > 100) content = content.substring(0, 100) + "...";
        return createNoteFromTemplate(template, content, author, authorUuid, true, hasSeal);
    }

    public static NoteData generateNoteByCategory(NoteCategory category, RandomSource random, String author, String authorUuid, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotesByCategory(category);
        if (notes.isEmpty()) return generateRandomNote(random, author, authorUuid, hasSeal);
        var template = notes.get(random.nextInt(notes.size()));
        return createNoteFromTemplate(template, template.getDefaultContent(), author, authorUuid, template.isSmall(), hasSeal);
    }

    public static NoteData generateRandomSmallNoteForProfession(RandomSource random, String author, String authorUuid,
                                                                String professionId, boolean hasSeal) {
        var notes = NoteConfigLoader.getNotesForProfession(professionId);
        if (notes.isEmpty()) notes = NoteConfigLoader.getNotesForProfession(null);
        if (notes.isEmpty()) return createFallbackNote(author, authorUuid, hasSeal);

        var smallNotes = notes.stream().filter(NoteConfigLoader.NoteTemplate::isSmall).collect(Collectors.toList());
        if (!smallNotes.isEmpty()) {
            var template = smallNotes.get(random.nextInt(smallNotes.size()));
            return createNoteFromTemplate(template, template.getDefaultContent(), author, authorUuid, true, hasSeal);
        }

        var template = notes.get(random.nextInt(notes.size()));
        String content = template.getDefaultContent();
        if (content.length() > ModConfig.getSmallContentMax()) content = content.substring(0, ModConfig.getSmallContentMax()) + "...";
        return createNoteFromTemplate(template, content, author, authorUuid, true, hasSeal);
    }

    private static NoteData createNoteFromTemplate(NoteConfigLoader.NoteTemplate template, String content,
                                                   String author, String authorUuid, boolean isSmall, boolean hasSeal) {
        NoteData note = new NoteData(template.id, template.getDefaultTitle(), content,
                author != null ? author : "Anonymous", -1,
                System.currentTimeMillis(), hasSeal, isSmall);
        if (authorUuid != null && !authorUuid.isEmpty()) note.setAuthorUuid(authorUuid);
        return note;
    }
}
