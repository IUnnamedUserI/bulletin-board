package com.unnameduser.bulletinboard.util;

import net.minecraft.nbt.NbtCompound;

public class NoteData {
    private String title;
    private String content;
    private String author;
    private int tagColor = -1;
    private long creationTime;
    private boolean hasSeal;
    private boolean isSmall;

    public NoteData(String title, String content, String author, boolean isSmall) {
        this(title, content, author, -1, System.currentTimeMillis(), false, isSmall);
    }

    public NoteData(String title, String content, String author, int tagColor) {
        this(title, content, author, tagColor, System.currentTimeMillis(), tagColor != -1, false);
    }

    public NoteData(String title, String content, String author, int tagColor, long creationTime, boolean hasSeal, boolean isSmall) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.tagColor = tagColor;
        this.creationTime = creationTime;
        this.hasSeal = hasSeal;
        this.isSmall = isSmall;
    }

    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getAuthor() { return author; }
    public int getTagColor() { return tagColor; }
    public long getCreationTime() { return creationTime; }
    public boolean hasSeal() { return hasSeal; }
    public boolean isSmall() { return isSmall; }

    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public void setAuthor(String author) { this.author = author; }
    public void setTagColor(int tagColor) { this.tagColor = tagColor; }
    public void setHasSeal(boolean hasSeal) { this.hasSeal = hasSeal; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Title", title);
        nbt.putString("Content", content);
        nbt.putString("Author", author);
        nbt.putInt("TagColor", tagColor);
        nbt.putLong("CreationTime", creationTime);
        nbt.putBoolean("HasSeal", hasSeal);
        nbt.putBoolean("IsSmall", isSmall);
        return nbt;
    }

    public static NoteData fromNbt(NbtCompound nbt) {
        String title = nbt.getString("Title");
        String content = nbt.getString("Content");
        String author = nbt.getString("Author");
        int tagColor = nbt.contains("TagColor", 3) ? nbt.getInt("TagColor") : -1;
        long creationTime = nbt.contains("CreationTime") ? nbt.getLong("CreationTime") : System.currentTimeMillis();
        boolean hasSeal = nbt.contains("HasSeal") ? nbt.getBoolean("HasSeal") : (tagColor != -1);
        boolean isSmall = nbt.contains("IsSmall") && nbt.getBoolean("IsSmall");
        return new NoteData(title, content, author, tagColor, creationTime, hasSeal, isSmall);
    }

    @Override
    public String toString() {
        return String.format("NoteData{title='%s', author='%s', tagColor=0x%X}",
                title, author, tagColor);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NoteData)) return false;
        NoteData other = (NoteData) obj;
        return title.equals(other.title) &&
                content.equals(other.content) &&
                author.equals(other.author) &&
                tagColor == other.tagColor;
    }

    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + content.hashCode();
        result = 31 * result + author.hashCode();
        result = 31 * result + tagColor;
        return result;
    }
}