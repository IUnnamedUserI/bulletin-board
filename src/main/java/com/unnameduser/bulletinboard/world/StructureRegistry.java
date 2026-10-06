package com.unnameduser.bulletinboard.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import com.unnameduser.bulletinboard.config.ModConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Внедрение доски объявлений в пулы деревенских домов.
 * <p>
 * В Fabric-версии это делал сторонний мод {@code structure-pool-api}. В Forge такого API нет,
 * поэтому инъекция выполняется напрямую: описания берутся из тех же файлов данных
 * {@code data/bulletin-board/structure_pool_api/inject_village_*.json}, которые уже есть в моде.
 */
public class StructureRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("BulletinBoard/Structures");

    private static final String INJECTION_DIR = "/data/bulletin-board/structure_pool_api/";

    private static final String[] INJECTION_FILES = {
            "inject_village_houses.json",
            "inject_village_snowy_houses.json",
            "inject_village_savanna_houses.json",
            "inject_village_taiga_houses.json",
            "inject_village_desert_houses.json"
    };

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(StructureRegistry::onServerAboutToStart);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        if (!ModConfig.isGenerateBulletinBoard()) {
            System.out.println("[Bulletin Board] Bulletin board generation disabled in config");
            return;
        }

        MinecraftServer server = event.getServer();
        RegistryAccess registryAccess = server.registryAccess();
        Registry<StructureTemplatePool> poolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL);
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

        int injected = 0;
        for (String fileName : INJECTION_FILES) {
            injected += applyInjection(poolRegistry, ops, fileName);
        }

        System.out.println("[Bulletin Board] Structures registered: " + injected + " pool element(s) injected");
    }

    private static int applyInjection(Registry<StructureTemplatePool> poolRegistry,
                                     RegistryOps<JsonElement> ops,
                                     String fileName) {
        JsonObject root = readJson(INJECTION_DIR + fileName);
        if (root == null) {
            return 0;
        }

        if (!root.has("target_pool") || !root.has("elements")) {
            LOGGER.warn("Некорректный файл инъекции структур: {}", fileName);
            return 0;
        }

        ResourceLocation poolId = new ResourceLocation(root.get("target_pool").getAsString());
        StructureTemplatePool pool = poolRegistry.get(poolId);
        if (pool == null) {
            LOGGER.warn("Пул структур не найден: {}", poolId);
            return 0;
        }

        int injected = 0;
        JsonArray elements = root.getAsJsonArray("elements");
        for (JsonElement entry : elements) {
            if (!entry.isJsonObject()) continue;
            JsonObject entryObject = entry.getAsJsonObject();
            if (!entryObject.has("element")) continue;

            int weight = entryObject.has("weight") ? entryObject.get("weight").getAsInt() : 1;

            StructurePoolElement element = StructurePoolElement.CODEC
                    .parse(ops, entryObject.get("element"))
                    .result()
                    .orElse(null);

            if (element == null) {
                LOGGER.warn("Не удалось разобрать элемент структуры в файле {}", fileName);
                continue;
            }

            if (appendToPool(pool, element, weight)) {
                injected += weight;
            }
        }

        return injected;
    }

    private static JsonObject readJson(String resourcePath) {
        try (InputStream in = StructureRegistry.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                LOGGER.warn("Ресурс не найден: {}", resourcePath);
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                return parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось прочитать {}: {}", resourcePath, e.getMessage());
            return null;
        }
    }

    /**
     * Добавляет элемент в список шаблонов пула.
     * <p>
     * Поле приватное и в продакшене носит SRG-имя, поэтому ищем его не по имени, а по типу:
     * список, параметризованный именно {@link StructurePoolElement} (это {@code templates},
     * в отличие от {@code rawTemplates}, где лежат {@code Pair}).
     */
    private static boolean appendToPool(StructureTemplatePool pool, StructurePoolElement element, int weight) {
        for (Field field : StructureTemplatePool.class.getDeclaredFields()) {
            if (!List.class.isAssignableFrom(field.getType())) continue;

            Type genericType = field.getGenericType();
            if (!(genericType instanceof ParameterizedType parameterized)) continue;

            Type[] arguments = parameterized.getActualTypeArguments();
            if (arguments.length != 1 || arguments[0] != StructurePoolElement.class) continue;

            try {
                field.setAccessible(true);
                Object value = field.get(pool);
                if (value instanceof List<?> rawList) {
                    @SuppressWarnings("unchecked")
                    List<StructurePoolElement> templates = (List<StructurePoolElement>) rawList;
                    for (int i = 0; i < weight; i++) {
                        templates.add(element);
                    }
                    return true;
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.error("Не удалось добавить элемент в пул структур: {}", e.getMessage());
            }
        }
        return false;
    }

    /** Не используется напрямую, но сохраняет совместимость с прежним API (rawTemplates). */
    @SuppressWarnings("unused")
    private static Pair<StructurePoolElement, Integer> rawEntry(StructurePoolElement element, int weight) {
        return Pair.of(element, weight);
    }
}
