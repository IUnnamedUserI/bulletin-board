package com.unnameduser.bulletinboard.util;

import com.unnameduser.bulletinboard.config.ModConfig;

public class NoteConstants {
    public static int FULL_TITLE_MAX = ModConfig.getFullTitleMax();
    public static int FULL_CONTENT_MAX = ModConfig.getFullContentMax();
    public static int SMALL_TITLE_MAX = ModConfig.getSmallTitleMax();
    public static int SMALL_CONTENT_MAX = ModConfig.getSmallContentMax();

    // Перезагрузить лимиты из конфига (вызывать после изменения config.json)
    public static void reload() {
        ModConfig.load();
        FULL_TITLE_MAX = ModConfig.getFullTitleMax();
        FULL_CONTENT_MAX = ModConfig.getFullContentMax();
        SMALL_TITLE_MAX = ModConfig.getSmallTitleMax();
        SMALL_CONTENT_MAX = ModConfig.getSmallContentMax();
    }
}