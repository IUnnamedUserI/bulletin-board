package com.unnameduser.bulletinboard.util;

import net.minecraft.util.StringRepresentable;

public enum NoteBadge implements StringRepresentable {
    NONE("none"),
    RED("red"),
    BLUE("blue"),
    GREEN("green");

    private final String name;

    NoteBadge(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static NoteBadge byName(String name) {
        for (NoteBadge badge : values()) {
            if (badge.name.equals(name)) {
                return badge;
            }
        }
        return NONE;
    }
}
