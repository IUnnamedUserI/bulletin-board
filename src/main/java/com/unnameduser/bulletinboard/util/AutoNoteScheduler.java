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
import com.unnameduser.bulletinboard.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class AutoNoteScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AutoNoteScheduler.class);

    private static AutoNoteScheduler instance;

    private final MinecraftServer server;
    private final long intervalTicks;
    private long lastRunTime = 0;
    private boolean enabled = true;

    private static final List<BlockPos> CACHED_BOARDS = new CopyOnWriteArrayList<>();
    private static final int VILLAGER_SEARCH_RADIUS = 50;

    private AutoNoteScheduler(MinecraftServer server, long intervalTicks) {
        this.server = server;
        this.intervalTicks = intervalTicks;
    }

    public static AutoNoteScheduler getInstance() {
        return instance;
    }

    public static void init(MinecraftServer server) {
        int interval = ModConfig.getIntervalTicks();
        instance = new AutoNoteScheduler(server, interval);
        LOGGER.info("AutoNoteScheduler initialized with {} ticks interval ({} seconds)",
                interval, interval / 20);
    }

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

        int totalBoards = CACHED_BOARDS.size();
        int checkedBoards = 0;
        int placedNotes = 0;

        for (BlockPos boardPos : CACHED_BOARDS) {
            if (random.nextFloat() > 0.3f) {
                continue;
            }

            List<VillagerEntity> villagers = getNearbyVillagers(world, boardPos, VILLAGER_SEARCH_RADIUS);
            if (villagers.isEmpty()) {
                LOGGER.debug("No villagers near board at {}, skipping", boardPos.toShortString());
                continue;
            }

            List<VillagerEntity> aliveVillagers = villagers.stream()
                    .filter(entity -> entity.isAlive() && !entity.isRemoved())
                    .collect(Collectors.toList());

            if (aliveVillagers.isEmpty()) {
                LOGGER.debug("No alive villagers near board at {}, skipping", boardPos.toShortString());
                continue;
            }

            var blockEntity = world.getBlockEntity(boardPos);
            if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
                continue;
            }

            List<Integer> freeSlots = findFreeSlots(boardEntity);
            if (freeSlots.isEmpty()) {
                LOGGER.debug("Board at {} is full", boardPos.toShortString());
                continue;
            }

            checkedBoards++;

            VillagerEntity author = aliveVillagers.get(random.nextInt(aliveVillagers.size()));
            String authorUuid = author.getUuid().toString();
            String authorNameKey = VillagerNameManager.get(server).getNameKey(author.getUuid());
            String professionId = author.getVillagerData().getProfession().toString();

            int targetSlot = freeSlots.get(random.nextInt(freeSlots.size()));

            // Генерируем записку из JSON-конфига (БЕЗ инициализации досок)
            NoteData note = RandomNotePool.generateRandomNoteForProfession(
                    random,
                    authorNameKey,
                    authorUuid,
                    professionId,
                    false
            );

            if (boardEntity.addNoteAtPosition(note, targetSlot)) {
                placedNotes++;
                LOGGER.info("Auto-placed note '{}' by {} ({}) on board at {}",
                        note.getTitle(), authorNameKey, professionId, boardPos.toShortString());
            }
        }

        if (placedNotes > 0) {
            LOGGER.info("Auto-placed {} notes on {} boards (out of {} total)", placedNotes, checkedBoards, totalBoards);
        } else {
            LOGGER.debug("No notes placed this tick (checked {} boards)", checkedBoards);
        }
    }

    private List<VillagerEntity> getNearbyVillagers(ServerWorld world, BlockPos pos, int radius) {
        Box area = new Box(pos).expand(radius);
        return world.getEntitiesByClass(VillagerEntity.class, area, entity ->
                entity.isAlive() && !entity.isRemoved()
        );
    }

    private void updateBoardCache(ServerWorld world) {
        CACHED_BOARDS.clear();

        var players = world.getPlayers();
        if (players.isEmpty()) {
            return;
        }

        int scanRadius = 10;

        for (var player : players) {
            int playerChunkX = player.getBlockX() >> 4;
            int playerChunkZ = player.getBlockZ() >> 4;

            for (int cx = -scanRadius; cx <= scanRadius; cx++) {
                for (int cz = -scanRadius; cz <= scanRadius; cz++) {
                    int chunkX = playerChunkX + cx;
                    int chunkZ = playerChunkZ + cz;

                    var chunk = world.getChunk(chunkX, chunkZ);
                    if (chunk.isEmpty()) {
                        continue;
                    }

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

    private List<Integer> findFreeSlots(BulletinBoardBlockEntity boardEntity) {
        List<Integer> freeSlots = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (boardEntity.isPositionFree(i)) {
                freeSlots.add(i);
            }
        }
        return freeSlots;
    }

    public static void setEnabled(boolean enabled) {
        if (instance != null) {
            instance.enabled = enabled;
            LOGGER.info("AutoNoteScheduler {}", enabled ? "enabled" : "disabled");
        }
    }

    public static boolean isEnabled() {
        return instance != null && instance.enabled;
    }

    public static void triggerNow() {
        if (instance != null) {
            instance.tryPlaceRandomNote();
            instance.lastRunTime = instance.server.getOverworld().getTime();
        }
    }

    public static void reset() {
        if (instance != null) {
            instance.enabled = false;
            instance = null;
            CACHED_BOARDS.clear();
            LOGGER.info("AutoNoteScheduler reset");
        }
    }
}