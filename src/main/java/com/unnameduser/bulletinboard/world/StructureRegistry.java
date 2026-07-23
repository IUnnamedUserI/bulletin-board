package com.unnameduser.bulletinboard.world;

import com.mojang.datafixers.util.Pair;
import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.structure.processor.StructureProcessorList;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class StructureRegistry {

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(StructureRegistry::addBulletinBoardToVillages);
    }

    private static void addBulletinBoardToVillages(MinecraftServer server) {
        Registry<StructurePool> templatePoolRegistry = server.getRegistryManager().get(RegistryKeys.TEMPLATE_POOL);
        Registry<StructureProcessorList> processorListRegistry = server.getRegistryManager().get(RegistryKeys.PROCESSOR_LIST);

        RegistryKey<StructureProcessorList> emptyProcessorListKey = RegistryKey.of(RegistryKeys.PROCESSOR_LIST, new Identifier("minecraft", "empty"));
        RegistryEntry<StructureProcessorList> emptyProcessorList = processorListRegistry.getEntry(emptyProcessorListKey).orElseThrow();

        Identifier nbtId = new Identifier(BulletinBoardMod.MOD_ID, "bulletin_board");

        addBuildingToPool(templatePoolRegistry, emptyProcessorList, new Identifier("minecraft:village/plains/houses"), nbtId, 1);
        addBuildingToPool(templatePoolRegistry, emptyProcessorList, new Identifier("minecraft:village/snowy/houses"), nbtId, 1);
        addBuildingToPool(templatePoolRegistry, emptyProcessorList, new Identifier("minecraft:village/savanna/houses"), nbtId, 1);
        addBuildingToPool(templatePoolRegistry, emptyProcessorList, new Identifier("minecraft:village/taiga/houses"), nbtId, 1);
        addBuildingToPool(templatePoolRegistry, emptyProcessorList, new Identifier("minecraft:village/desert/houses"), nbtId, 1);
    }

    private static void addBuildingToPool(Registry<StructurePool> templatePoolRegistry,
                                          RegistryEntry<StructureProcessorList> emptyProcessorList,
                                          Identifier poolId,
                                          Identifier nbtId,
                                          int weight) {
        StructurePool pool = templatePoolRegistry.get(poolId);
        if (pool == null) {
            System.out.println("[Bulletin Board] Pool not found: " + poolId);
            return;
        }

        StructurePoolElement piece = SinglePoolElement.ofProcessedSingle(nbtId.toString(), emptyProcessorList)
                .apply(StructurePool.Projection.RIGID);

        try {
            // --- Работа со списком элементов (elements) ---
            Field elementsField = StructurePool.class.getDeclaredField("elements");
            elementsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<StructurePoolElement> elements = (List<StructurePoolElement>) elementsField.get(pool);

            for (int i = 0; i < weight; i++) {
                elements.add(piece);
            }

            // --- Работа со списком весов (elementCounts) ---
            Field elementCountsField = StructurePool.class.getDeclaredField("elementCounts");
            elementCountsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Pair<StructurePoolElement, Integer>> originalCounts = (List<Pair<StructurePoolElement, Integer>>) elementCountsField.get(pool);

            // ИСПРАВЛЕНО 3: Создаем НОВЫЙ список, копируем старые элементы и добавляем наш
            List<Pair<StructurePoolElement, Integer>> newElementCounts = new ArrayList<>(originalCounts);
            newElementCounts.add(new Pair<>(piece, weight));

            // Перезаписываем ссылку в поле final-класса
            elementCountsField.set(pool, newElementCounts);

            System.out.println("[Bulletin Board] Successfully injected " + nbtId + " into " + poolId + " (Total pieces in pool: " + newElementCounts.size() + ")");

        } catch (Exception e) {
            System.err.println("[Bulletin Board] Reflection failed for " + poolId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}