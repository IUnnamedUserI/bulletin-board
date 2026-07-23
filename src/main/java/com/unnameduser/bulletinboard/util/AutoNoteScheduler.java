package com.unnameduser.bulletinboard.util;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.server.VillagerNameManager;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Автоматический планировщик записок на досках объявлений.
 * Раз в заданный интервал размещает случайную записку от имени реального жителя.
 */
public class AutoNoteScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoNoteScheduler.class);

    private static AutoNoteScheduler instance;

    private final MinecraftServer server;
    private final long intervalTicks;
    private long lastRunTime = 0;
    private boolean enabled = true;

    // Кэш досок — обновляется только при загрузке чанков
    private static final List<BlockPos> CACHED_BOARDS = new CopyOnWriteArrayList<>();

    // Конфигурируемые параметры
    private static final long DEFAULT_INTERVAL_TICKS = 12000; // 15 секунд (20 тиков/сек)
    private static final int VILLAGER_SEARCH_RADIUS = 50; // Блоков

    private AutoNoteScheduler(MinecraftServer server, long intervalTicks) {
        this.server = server;
        this.intervalTicks = intervalTicks;
    }

    /**
     * Возвращает экземпляр планировщика.
     */
    public static AutoNoteScheduler getInstance() {
        return instance;
    }

    /**
     * Инициализирует планировщик.
     */
    public static void init(MinecraftServer server) {
        instance = new AutoNoteScheduler(server, DEFAULT_INTERVAL_TICKS);
        LOGGER.info("AutoNoteScheduler initialized with {} ticks interval ({} seconds)",
                DEFAULT_INTERVAL_TICKS, DEFAULT_INTERVAL_TICKS / 20);
    }

    /**
     * Вызывается каждый тик сервера.
     */
    public static void tick() {
        if (instance == null || !instance.enabled) {
            return;
        }

        long currentTime = instance.server.getOverworld().getTime();

        if (currentTime - instance.lastRunTime >= instance.intervalTicks) {
            instance.tryPlaceRandomNote();
            instance.lastRunTime = currentTime;
        }
    }

    private void tryPlaceRandomNote() {
        ServerWorld world = server.getOverworld();
        Random random = world.getRandom();

        updateBoardCache(world);

        if (CACHED_BOARDS.isEmpty()) {
            LOGGER.debug("No bulletin boards found in loaded chunks");
            return;
        }

        BlockPos boardPos = CACHED_BOARDS.get(random.nextInt(CACHED_BOARDS.size()));

        var blockEntity = world.getBlockEntity(boardPos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            return;
        }

        List<Integer> freeSlots = findFreeSlots(boardEntity);

        if (freeSlots.isEmpty()) {
            LOGGER.debug("Board at {} is full", boardPos.toShortString());
            return;
        }

        // Получаем живых жителей
        List<VillagerEntity> villagers = getNearbyVillagers(world, boardPos, VILLAGER_SEARCH_RADIUS);

        // Если жителей нет — выходим, не создаём записку
        if (villagers.isEmpty()) {
            LOGGER.debug("No villagers found near board at {}, skipping", boardPos.toShortString());
            return;
        }

        // Фильтруем живых
        List<VillagerEntity> aliveVillagers = villagers.stream()
                .filter(entity -> entity.isAlive() && !entity.isRemoved())
                .collect(Collectors.toList());

        if (aliveVillagers.isEmpty()) {
            LOGGER.debug("No alive villagers found near board at {}, skipping", boardPos.toShortString());
            return;
        }

        // Выбираем случайного жителя
        VillagerEntity author = aliveVillagers.get(random.nextInt(aliveVillagers.size()));
        String authorUuid = author.getUuid().toString();

        // Получаем имя из кэша
        String authorName = VillagerNameManager.get(server).getName(author.getUuid());
        if (authorName == null || authorName.equals("Villager") || authorName.equals("Аноним")) {
            authorName = "Житель-" + authorUuid.substring(0, 8);
        }

        // Выбираем слот
        int targetSlot = freeSlots.get(random.nextInt(freeSlots.size()));

        // Создаём записку с именем жителя (без печати)
        NoteData note = RandomNotePool.generateRandomNote(random, authorName, authorUuid, false);

        if (boardEntity.addNoteAtPosition(note, targetSlot)) {
            LOGGER.info("Auto-placed note '{}' by villager {} (UUID: {}) on board at {}",
                    note.getTitle(), authorName, authorUuid, boardPos.toShortString());
        }
    }

    /**
     * Находит всех живых жителей в радиусе от заданной позиции.
     */
    private List<VillagerEntity> getNearbyVillagers(ServerWorld world, BlockPos pos, int radius) {
        Box area = new Box(pos).expand(radius);
        return world.getEntitiesByClass(VillagerEntity.class, area, entity ->
                entity.isAlive() && !entity.isRemoved()
        );
    }

    /**
     * Обновляет кэш досок из загруженных чанков.
     * Проверяет только чанки вокруг игроков — это быстро.
     */
    private void updateBoardCache(ServerWorld world) {
        CACHED_BOARDS.clear();

        // Сканируем только загруженные чанки вокруг игроков
        var players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }

        int scanRadius = 10; // Чанков (160 блоков)

        for (var player : players) {
            int playerChunkX = player.getBlockX() >> 4;
            int playerChunkZ = player.getBlockZ() >> 4;

            for (int cx = -scanRadius; cx <= scanRadius; cx++) {
                for (int cz = -scanRadius; cz <= scanRadius; cz++) {
                    int chunkX = playerChunkX + cx;
                    int chunkZ = playerChunkZ + cz;

                    // Проверяем, загружен ли чанк
                    var chunk = world.getChunk(chunkX, chunkZ);
                    if (chunk.isEmpty()) {
                        continue;
                    }

                    // Сканируем блоки в чанке
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            int worldX = (chunkX << 4) + x;
                            int worldZ = (chunkZ << 4) + z;

                            for (int y = world.getBottomY(); y < world.getTopY(); y++) {
                                if (chunk.getBlockState(new BlockPos(worldX, y, worldZ)).getBlock() instanceof BulletinBoardBlock) {
                                    CACHED_BOARDS.add(new BlockPos(worldX, y, worldZ));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Находит свободные слоты на доске.
     */
    private List<Integer> findFreeSlots(BulletinBoardBlockEntity boardEntity) {
        List<Integer> freeSlots = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            if (boardEntity.isPositionFree(i)) {
                freeSlots.add(i);
            }
        }

        return freeSlots;
    }

    /**
     * Включает или выключает планировщик.
     */
    public static void setEnabled(boolean enabled) {
        if (instance != null) {
            instance.enabled = enabled;
            LOGGER.info("AutoNoteScheduler {}", enabled ? "enabled" : "disabled");
        }
    }

    /**
     * Проверяет, включён ли планировщик.
     */
    public static boolean isEnabled() {
        return instance != null && instance.enabled;
    }

    /**
     * Принудительно запускает размещение записки (для тестов).
     */
    public static void triggerNow() {
        if (instance != null) {
            instance.tryPlaceRandomNote();
            instance.lastRunTime = instance.server.getOverworld().getTime();
        }
    }
}