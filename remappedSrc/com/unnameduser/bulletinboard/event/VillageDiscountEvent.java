package com.unnameduser.bulletinboard.event;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.integration.TradeOverhaulIntegration;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Система ивентов со скидками от жителей.
 * Создаёт временные скидки и размещает информирующие записки на досках.
 * 
 * ИНТЕГРАЦИЯ С TRADE OVERHAUL:
 * - Если установлен Trade Overhaul - применяется реальная скидка через NBT
 * - Если нет - создаётся только информационная записка
 */
public class VillageDiscountEvent {
    private static final Logger LOGGER = LoggerFactory.getLogger(VillageDiscountEvent.class);
    
    private static VillageDiscountEvent instance;
    
    private final MinecraftServer server;
    private final Map<UUID, DiscountData> activeDiscounts = new HashMap<>();
    
    // Длительность скидки в миллисекундах (1 час = 3600000 мс)
    private static final long DISCOUNT_DURATION = 3600000;
    private static final int DISCOUNT_PERCENT = 50; // 50% скидка
    
    private VillageDiscountEvent(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Инициализирует систему ивентов.
     */
    public static void init(MinecraftServer server) {
        instance = new VillageDiscountEvent(server);
        // Инициализируем интеграцию с Trade Overhaul
        TradeOverhaulIntegration.init(server);
        LOGGER.info("VillageDiscountEvent initialized");
        if (TradeOverhaulIntegration.isTradeOverhaulPresent()) {
            LOGGER.info("Trade Overhaul integration ENABLED - real discounts will be applied!");
        } else {
            LOGGER.info("Trade Overhaul integration DISABLED - informational discounts only");
        }
    }
    
    public static VillageDiscountEvent getInstance() {
        return instance;
    }
    
    /**
     * Данные о скидке.
     */
    public static class DiscountData {
        public final UUID villagerUuid;
        public final String villagerName;
        public final String profession;
        public final long startTime;
        public final long endTime;
        public final BlockPos boardPos;
        
        public DiscountData(UUID villagerUuid, String villagerName, String profession, 
                           long startTime, BlockPos boardPos) {
            this.villagerUuid = villagerUuid;
            this.villagerName = villagerName;
            this.profession = profession;
            this.startTime = startTime;
            this.endTime = startTime + DISCOUNT_DURATION;
            this.boardPos = boardPos;
        }
        
        public boolean isActive() {
            return System.currentTimeMillis() < endTime;
        }
        
        public long getRemainingTime() {
            return Math.max(0, endTime - System.currentTimeMillis());
        }
    }
    
    /**
     * Запускает ивент со скидкой.
     * @return true если успешно
     */
    public boolean triggerDiscountEvent() {
        ServerWorld world = server.getOverworld();
        Random random = world.getRandom();
        
        LOGGER.info("Starting discount event...");
        
        // Находим случайного жителя с профессией
        VillagerEntity villager = findRandomVillagerWithProfession(world, 1000);
        
        if (villager == null) {
            LOGGER.warn("No villagers with profession found in 1000 block radius");
            return false;
        }
        
        LOGGER.info("Found villager: {} at {}", villager.getName().getString(), villager.getPos());
        
        // Проверяем, есть ли у жителя уже скидка
        if (activeDiscounts.containsKey(villager.getUuid())) {
            LOGGER.debug("Villager {} already has active discount", villager.getName().getString());
            return false;
        }

        // Находим случайную доску в радиусе 100 блоков
        BlockPos boardPos = findRandomBulletinBoard(world, villager.getBlockPos(), 100);

        if (boardPos == null) {
            LOGGER.warn("No bulletin boards found in 100 block radius around villager");
            return false;
        }
        
        LOGGER.info("Found board at {}", boardPos.toShortString());

        // Проверяем, есть ли свободные слоты на доске
        var blockEntity = world.getBlockEntity(boardPos);
        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            LOGGER.error("Block entity at {} is not a BulletinBoardBlockEntity", boardPos.toShortString());
            return false;
        }

        List<Integer> freeSlots = findFreeSlots(boardEntity);
        if (freeSlots.isEmpty()) {
            LOGGER.warn("Board at {} is full", boardPos.toShortString());
            return false;
        }
        
        LOGGER.info("Board has {} free slots", freeSlots.size());

        // Применяем скидку к жителю
        applyDiscountToVillager(villager);

        // Создаём записку
        String profession = getProfessionName(villager);
        NoteData note = createDiscountNote(villager.getName().getString(), profession);

        // Размещаем записку на доске
        int targetSlot = freeSlots.get(random.nextInt(freeSlots.size()));
        if (boardEntity.addNoteAtPosition(note, targetSlot)) {
            // Сохраняем данные о скидке
            DiscountData discountData = new DiscountData(
                    villager.getUuid(),
                    villager.getName().getString(),
                    profession,
                    System.currentTimeMillis(),
                    boardPos
            );
            activeDiscounts.put(villager.getUuid(), discountData);

            LOGGER.info("Discount event started for {} ({}) at board {}", 
                    villager.getName().getString(), profession, boardPos.toShortString());

            return true;
        }
        
        LOGGER.error("Failed to add note to board");

        return false;
    }
    
    /**
     * Находит случайного жителя с профессией в радиусе.
     */
    private VillagerEntity findRandomVillagerWithProfession(ServerWorld world, int radius) {
        List<VillagerEntity> villagers = new ArrayList<>();
        
        // Получаем всех игроков для центра поиска
        var players = world.getPlayers();
        if (players.isEmpty()) {
            LOGGER.warn("No players online to search for villagers");
            return null;
        }
        
        // Ищем вокруг каждого игрока
        for (var player : players) {
            // Используем Box для поиска сущностей
            var box = player.getBoundingBox().expand(radius);
            List<VillagerEntity> nearby = world.getEntitiesByClass(
                    VillagerEntity.class,
                    box,
                    entity -> true
            );
            
            LOGGER.debug("Found {} villagers near player {}", nearby.size(), player.getName().getString());
            
            for (VillagerEntity villager : nearby) {
                var profession = villager.getVillagerData().getProfession();
                LOGGER.debug("Villager {} has profession {}", villager.getName().getString(), profession);
                
                if (profession != net.minecraft.village.VillagerProfession.NONE) {
                    villagers.add(villager);
                }
            }
        }
        
        LOGGER.info("Total villagers with profession found: {}", villagers.size());
        
        if (villagers.isEmpty()) {
            return null;
        }
        
        return villagers.get(world.getRandom().nextInt(villagers.size()));
    }
    
    /**
     * Находит случайную доску в радиусе от позиции.
     */
    private BlockPos findRandomBulletinBoard(ServerWorld world, BlockPos center, int radius) {
        List<BlockPos> boards = new ArrayList<>();
        
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (world.getBlockState(pos).getBlock() instanceof BulletinBoardBlock) {
                        boards.add(pos);
                    }
                }
            }
        }
        
        if (boards.isEmpty()) {
            return null;
        }
        
        return boards.get(world.getRandom().nextInt(boards.size()));
    }
    
    /**
     * Применяет скидку к торгам жителя.
     * Использует интеграцию с Trade Overhaul если доступна.
     */
    private void applyDiscountToVillager(VillagerEntity villager) {
        // Применяем скидку через интеграцию
        boolean applied = TradeOverhaulIntegration.applyDiscount(
                villager, 
                DISCOUNT_PERCENT, 
                DISCOUNT_DURATION
        );
        
        if (applied) {
            LOGGER.info("Real discount applied to villager {} via Trade Overhaul", 
                    villager.getName().getString());
        } else {
            LOGGER.info("Informational discount created for villager {} (Trade Overhaul not installed)", 
                    villager.getName().getString());
        }
    }
    
    /**
     * Создаёт записку о скидке.
     */
    private NoteData createDiscountNote(String villagerName, String profession) {
        String title = Text.translatable("event.bulletin-board.discount.title")
                .getString();
        
        String content = Text.translatable("event.bulletin-board.discount.content", 
                villagerName, DISCOUNT_PERCENT)
                .getString();
        
        return new NoteData(
                title,
                content,
                profession, // Профессия как "автор"
                -1, // Без печати
                System.currentTimeMillis(),
                false, // Без печати
                false // Обычная записка
        );
    }
    
    /**
     * Получает название профессии.
     */
    private String getProfessionName(VillagerEntity villager) {
        var profession = villager.getVillagerData().getProfession();
        Identifier id = net.minecraft.registry.Registries.VILLAGER_PROFESSION.getId(profession);
        
        if (id != null) {
            return Text.translatable("entity.minecraft.villager." + id.getPath())
                    .getString();
        }
        
        return "Неизвестный";
    }
    
    /**
     * Проверяет и обновляет активные скидки.
     */
    public void tick() {
        long currentTime = System.currentTimeMillis();
        
        // Удаляем истёкшие скидки
        Iterator<Map.Entry<UUID, DiscountData>> iterator = activeDiscounts.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, DiscountData> entry = iterator.next();
            DiscountData data = entry.getValue();
            
            if (!data.isActive()) {
                // Скидка истекла - восстанавливаем цены
                restoreVillagerPrices(data.villagerUuid);
                iterator.remove();
                
                LOGGER.info("Discount expired for {}", data.villagerName);
            }
        }
    }
    
    /**
     * Восстанавливает цены жителя (удаляет скидку).
     * Использует интеграцию с Trade Overhaul если доступна.
     */
    private void restoreVillagerPrices(UUID villagerUuid) {
        // Находим жителя и удаляем скидку
        ServerWorld world = server.getOverworld();
        var entity = world.getEntity(villagerUuid);
        
        if (entity instanceof VillagerEntity villager) {
            TradeOverhaulIntegration.removeDiscount(villager);
            LOGGER.debug("Discount removed from villager {}", villager.getUuid());
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
     * Возвращает список активных скидок.
     */
    public List<DiscountData> getActiveDiscounts() {
        return new ArrayList<>(activeDiscounts.values());
    }
    
    /**
     * Принудительно завершает все скидки.
     */
    public void clearAllDiscounts() {
        for (DiscountData data : activeDiscounts.values()) {
            restoreVillagerPrices(data.villagerUuid);
        }
        activeDiscounts.clear();
        LOGGER.info("All discounts cleared");
    }
}
