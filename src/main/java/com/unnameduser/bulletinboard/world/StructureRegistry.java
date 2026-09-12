package com.unnameduser.bulletinboard.world;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.config.ModConfig;
import net.fabric_extras.structure_pool.api.FabricStructurePoolRegistry;
import net.minecraft.util.Identifier;

public class StructureRegistry {

    public static void register() {
        if (!ModConfig.isGenerateBulletinBoard()) {
            System.out.println("[Bulletin Board] Bulletin board generation disabled in config");
            return;
        }

        Identifier nbtId = new Identifier(BulletinBoardMod.MOD_ID, "bulletin_board");

        FabricStructurePoolRegistry.registerSimple(
                new Identifier("minecraft:village/plains/houses"),
                nbtId,
                1
        );
        FabricStructurePoolRegistry.registerSimple(
                new Identifier("minecraft:village/snowy/houses"),
                nbtId,
                1
        );
        FabricStructurePoolRegistry.registerSimple(
                new Identifier("minecraft:village/savanna/houses"),
                nbtId,
                1
        );
        FabricStructurePoolRegistry.registerSimple(
                new Identifier("minecraft:village/taiga/houses"),
                nbtId,
                1
        );
        FabricStructurePoolRegistry.registerSimple(
                new Identifier("minecraft:village/desert/houses"),
                nbtId,
                1
        );

        System.out.println("[Bulletin Board] Bulletin board registered in village pools");
    }
}