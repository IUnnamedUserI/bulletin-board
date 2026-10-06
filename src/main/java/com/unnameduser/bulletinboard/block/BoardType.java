package com.unnameduser.bulletinboard.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.util.ByIdMap;
import java.util.function.IntFunction;

public enum BoardType implements StringRepresentable {
    SINGLE_WALL(0, "single_wall", false),
    DOUBLE_WALL(1, "double_wall", true);

    private static final IntFunction<BoardType> BY_ID = ByIdMap.continuous(
            BoardType::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO);

    private final int id;
    private final String name;
    public final boolean isDouble;

    BoardType(int id, String name, boolean isDouble) {
        this.id = id;
        this.name = name;
        this.isDouble = isDouble;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getId() {
        return this.id;
    }

    public static BoardType byId(int id) {
        return BY_ID.apply(id);
    }
}
