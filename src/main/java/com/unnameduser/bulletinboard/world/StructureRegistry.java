package com.unnameduser.bulletinboard.world;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.config.ModConfig;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;

public class StructureRegistry {
    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(StructureRegistry::onServerStarting);
    }

    private static void onServerStarting(MinecraftServer server) {
        if (!ModConfig.isGenerateBulletinBoard()) {
            System.out.println("[Bulletin Board] Bulletin board generation disabled in config");
            return;
        }

        Identifier nbtId = new Identifier(BulletinBoardMod.MOD_ID, "bulletin_board");

        inject(server, nbtId, "minecraft:village/plains/houses", 1);
        inject(server, nbtId, "minecraft:village/snowy/houses", 1);
        inject(server, nbtId, "minecraft:village/savanna/houses", 1);
        inject(server, nbtId, "minecraft:village/taiga/houses", 1);
        inject(server, nbtId, "minecraft:village/desert/houses", 1);

        System.out.println("[Bulletin Board] Structures registered via Structure Pool API");
    }

    private static void inject(MinecraftServer server, Identifier nbtId, String pool, int weight) {
        StructurePoolAPI.injectIntoStructurePool(
                server,
                new Identifier(pool),
                nbtId,
                weight
        );
    }
}