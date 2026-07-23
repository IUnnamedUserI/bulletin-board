package com.unnameduser.bulletinboard.world;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;

public class StructureRegistry {
    private static final Identifier BULLETIN_BOARD_STRUCTURE = new Identifier(BulletinBoardMod.MOD_ID, "bulletin_board");

    public static void register() {
        // Регистрация структуры будет происходить через datapack
        // Файл структуры уже лежит в правильной папке
        System.out.println("[Bulletin Board] Structure registered: " + BULLETIN_BOARD_STRUCTURE);
    }
}