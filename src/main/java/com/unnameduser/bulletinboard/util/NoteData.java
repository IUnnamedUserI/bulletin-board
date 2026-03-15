package com.unnameduser.bulletinboard.util;

import net.minecraft.nbt.NbtCompound;

public class NoteData {
    private String title;
    private String content;
    private String author;
    private int tagColor = -1;  // 🔧 Цвет метки (RGB)

    public NoteData(String title, String content, String author) {
        this(title, content, author, -1);
    }

    public NoteData(String title, String content, String author, int tagColor) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.tagColor = tagColor;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public int getTagColor() {
        return tagColor;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setTagColor(int tagColor) {
        this.tagColor = tagColor;
    }

    // 🔧 Сохранение в NBT
    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Title", title);
        nbt.putString("Content", content);
        nbt.putString("Author", author);
        nbt.putInt("TagColor", tagColor);
        return nbt;
    }

    // 🔧 Загрузка из NBT
    public static NoteData fromNbt(NbtCompound nbt) {
        String title = nbt.getString("Title");
        String content = nbt.getString("Content");
        String author = nbt.getString("Author");
        int tagColor = nbt.contains("TagColor", 3) ? nbt.getInt("TagColor") : -1;

        return new NoteData(title, content, author, tagColor);
    }

    // 🔧 Для отладки
    @Override
    public String toString() {
        return String.format("NoteData{title='%s', author='%s', tagColor=0x%X}",
                title, author, tagColor);
    }

    // 🔧 Равенство (для сравнения записок)
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