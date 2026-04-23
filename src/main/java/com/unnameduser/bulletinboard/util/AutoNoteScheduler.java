package com.unnameduser.bulletinboard.util;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Автоматический планировщик записок на досках объявлений.
 * Раз в заданный интервал размещает случайную записку на случайной доске.
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
    private static final long DEFAULT_INTERVAL_TICKS = 300; // 15 секунд (20 тиков/сек)
    private static final int MAX_NOTES_PER_BOARD = 5;
    
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
    
    /**
     * Пытается разместить случайную записку на случайной доске.
     */
    private void tryPlaceRandomNote() {
        ServerWorld world = server.getOverworld();
        Random random = world.getRandom();
        
        // Обновляем кэш досок
        updateBoardCache(world);
        
        if (CACHED_BOARDS.isEmpty()) {
            LOGGER.debug("No bulletin boards found in loaded chunks");
            return;
        }
        
        // Выбираем случайную доску
        BlockPos boardPos = CACHED_BOARDS.get(random.nextInt(CACHED_BOARDS.size()));
        
        // Пытаемся разместить записку
        var blockEntity = world.getBlockEntity(boardPos);
        
        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            return;
        }
        
        // Проверяем, есть ли свободные слоты
        List<Integer> freeSlots = findFreeSlots(boardEntity);
        
        if (freeSlots.isEmpty()) {
            LOGGER.debug("Board at {} is full", boardPos.toShortString());
            return;
        }
        
        // Выбираем случайный слот
        int targetSlot = freeSlots.get(random.nextInt(freeSlots.size()));
        
        // Генерируем случайную записку
        NoteData note = RandomNotePool.generateRandomNote(random);
        
        // Размещаем
        if (boardEntity.addNoteAtPosition(note, targetSlot)) {
            LOGGER.info("Auto-placed note '{}' on board at {}", 
                    note.getTitle(), boardPos.toShortString());
        }
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
