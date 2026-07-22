package com.unnameduser.bulletinboard.integration;

import com.unnameduser.bulletinboard.event.VillageDiscountEvent;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Интеграция с модом Trade Overhaul.
 * Обеспечивает применение реальных скидок к жителям при ивентах.
 * 
 * Работает ТОЛЬКО если установлен мод tradeoverhaul.
 */
public class TradeOverhaulIntegration {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeOverhaulIntegration.class);
    
    private static boolean tradeOverhaulPresent = false;
    private static final Map<UUID, DiscountData> activeDiscounts = new HashMap<>();
    
    /**
     * Данные о применённой скидке.
     */
    public static class DiscountData {
        public final UUID villagerUuid;
        public final int discountPercent;
        public final long startTime;
        public final long endTime;
        
        public DiscountData(UUID villagerUuid, int discountPercent, long durationMs) {
            this.villagerUuid = villagerUuid;
            this.discountPercent = discountPercent;
            this.startTime = System.currentTimeMillis();
            this.endTime = this.startTime + durationMs;
        }
        
        public boolean isActive() {
            return System.currentTimeMillis() < endTime;
        }
    }
    
    /**
     * Инициализирует интеграцию. Проверяет наличие Trade Overhaul.
     */
    public static void init(MinecraftServer server) {
        // Проверяем наличие мода через Fabric Loader
        try {
            // Пытаемся загрузить класс из Trade Overhaul
            Class.forName("com.unnameduser.tradeoverhaul.TradeOverhaulMod");
            tradeOverhaulPresent = true;
            LOGGER.info("Trade Overhaul detected! Discount integration enabled.");
        } catch (ClassNotFoundException e) {
            tradeOverhaulPresent = false;
            LOGGER.info("Trade Overhaul not found. Discounts will be informational only.");
        }
    }
    
    /**
     * Проверяет, установлен ли Trade Overhaul.
     */
    public static boolean isTradeOverhaulPresent() {
        return tradeOverhaulPresent;
    }
    
    /**
     * Применяет скидку к жителю.
     * Если Trade Overhaul установлен - применяет реальную скидку через NBT.
     * Если нет - только регистрирует скидку для информационных целей.
     * 
     * @param villager Житель
     * @param discountPercent Процент скидки (50 = 50%)
     * @param durationMs Длительность в миллисекундах
     * @return true если скидка применена успешно
     */
    public static boolean applyDiscount(VillagerEntity villager, int discountPercent, long durationMs) {
        UUID villagerUuid = villager.getUuid();
        
        // Сохраняем данные о скидке
        DiscountData data = new DiscountData(villagerUuid, discountPercent, durationMs);
        activeDiscounts.put(villagerUuid, data);
        
        if (tradeOverhaulPresent) {
            // Получаем существующий NBT жителя
            NbtCompound nbt = new NbtCompound();
            villager.writeCustomDataToNbt(nbt);
            
            // Добавляем теги скидки
            nbt.putLong("BulletinBoardDiscountEndTime", data.endTime);
            nbt.putInt("BulletinBoardDiscountPercent", discountPercent);
            
            // Записываем обратно
            villager.readCustomDataFromNbt(nbt);
            
            LOGGER.info("Applied {}% discount to villager {} for {} minutes",
                    discountPercent, villager.getName().getString(), durationMs / 60000);
            
            // Проверяем, что данные записались
            NbtCompound checkNbt = new NbtCompound();
            villager.writeCustomDataToNbt(checkNbt);
            LOGGER.debug("NBT check - EndTime: {}, Percent: {}", 
                    checkNbt.getLong("BulletinBoardDiscountEndTime"),
                    checkNbt.getInt("BulletinBoardDiscountPercent"));
            
            return true;
        } else {
            // Trade Overhaul не установлен - только информационная скидка
            LOGGER.info("Created informational discount for villager {} (Trade Overhaul not installed)",
                    villager.getName().getString());
            return false;
        }
    }
    
    /**
     * Удаляет скидку у жителя.
     */
    public static void removeDiscount(VillagerEntity villager) {
        UUID villagerUuid = villager.getUuid();
        activeDiscounts.remove(villagerUuid);
        
        if (tradeOverhaulPresent) {
            // Удаляем данные из NBT
            NbtCompound nbt = villager.writeNbt(new NbtCompound());
            nbt.remove("BulletinBoardDiscountEndTime");
            nbt.remove("BulletinBoardDiscountPercent");
            villager.readNbt(nbt);
        }
    }
    
    /**
     * Проверяет, есть ли у жителя активная скидка.
     */
    public static boolean hasActiveDiscount(VillagerEntity villager) {
        UUID villagerUuid = villager.getUuid();
        DiscountData data = activeDiscounts.get(villagerUuid);
        
        if (data == null) {
            return false;
        }
        
        if (!data.isActive()) {
            // Скидка истекла - удаляем
            activeDiscounts.remove(villagerUuid);
            return false;
        }
        
        return true;
    }
    
    /**
     * Получает процент скидки у жителя.
     */
    public static int getDiscountPercent(VillagerEntity villager) {
        UUID villagerUuid = villager.getUuid();
        DiscountData data = activeDiscounts.get(villagerUuid);
        
        if (data != null && data.isActive()) {
            return data.discountPercent;
        }
        
        return 0;
    }
    
    /**
     * Очищает все истёкшие скидки.
     */
    public static void tick() {
        long currentTime = System.currentTimeMillis();
        
        activeDiscounts.entrySet().removeIf(entry -> {
            if (!entry.getValue().isActive()) {
                LOGGER.debug("Discount expired for villager {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Возвращает количество активных скидок.
     */
    public static int getActiveDiscountCount() {
        return activeDiscounts.size();
    }
}
