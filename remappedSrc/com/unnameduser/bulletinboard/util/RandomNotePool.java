package com.unnameduser.bulletinboard.util;

import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Пул случайных записок для генерации на досках объявлений.
 * В будущем может быть расширено для квестов и взаимодействия жителей.
 */
public class RandomNotePool {
    
    private static final List<NoteTemplate> NOTES = new ArrayList<>();
    
    static {
        // === ОБЪЯВЛЕНИЯ ===
        NOTES.add(new NoteTemplate(
                "Пропала курица!",
                "Пропала рябая курица. Откликается на \"Кво-кво\". Нашедшему — 3 изумруда!",
                NoteCategory.ANNOUNCEMENT
        ));
        NOTES.add(new NoteTemplate(
                "Ищу шахтёров",
                "Требуются рабочие в новую шахту. Плата сдельная, изумрудами. Обращаться к старосте.",
                NoteCategory.ANNOUNCEMENT
        ));
        NOTES.add(new NoteTemplate(
                "Продам урожай",
                "Свежая пшеница и картофель. Урожай этого года. Цена договорная.",
                NoteCategory.ANNOUNCEMENT
        ));
        NOTES.add(new NoteTemplate(
                "Обмен вещей",
                "Поменяю книгу записок на изумруды или еду. Интересуют только целые книги.",
                NoteCategory.ANNOUNCEMENT
        ));
        NOTES.add(new NoteTemplate(
                "Потерялся кот",
                "Чёрный кот, отзывается на \"Уголёк\". Видели последний раз у колодца.",
                NoteCategory.ANNOUNCEMENT
        ));
        
        // === ПРЕДУПРЕЖДЕНИЯ ===
        NOTES.add(new NoteTemplate(
                "Осторожно, криперы!",
                "В последнее время участились случаи появления криперов near деревней. Будьте бдительны!",
                NoteCategory.WARNING
        ));
        NOTES.add(new NoteTemplate(
                "Шахта затоплена",
                "Вход в старую шахту затоплен. Не спускаться до особого распоряжения.",
                NoteCategory.WARNING
        ));
        NOTES.add(new NoteTemplate(
                "Карантин!",
                "Деревня на карантине. Не входить и не выходить без разрешения лекаря.",
                NoteCategory.WARNING
        ));
        NOTES.add(new NoteTemplate(
                "Ночная тревога",
                "Прошлой ночью были замечены зомби у стен. Усильте охрану!",
                NoteCategory.WARNING
        ));
        
        // === ЛИЧНЫЕ ===
        NOTES.add(new NoteTemplate(
                "С днём рождения!",
                "Дорогой Стив! Поздравляем с 30-летием! Ждём тебя на празднике у ратуши.",
                NoteCategory.PERSONAL
        ));
        NOTES.add(new NoteTemplate(
                "Кто видел моего кота?",
                "Рыжий, с белым пятном на груди. Очень скучаю...",
                NoteCategory.PERSONAL
        ));
        NOTES.add(new NoteTemplate(
                "Ищу друга",
                "Новенький в деревне. Не против пообщаться за кружкой эля.",
                NoteCategory.PERSONAL
        ));
        NOTES.add(new NoteTemplate(
                "Благодарность",
                "Спасибо тому герою, который очистил колодец от пауков! Вы — наше всё!",
                NoteCategory.PERSONAL
        ));
        
        // === КВЕСТЫ (задел на будущее) ===
        NOTES.add(new NoteTemplate(
                "Нужен уголь",
                "Срочно требуется 10 угля для кузницы. Награда: 5 изумрудов.",
                NoteCategory.QUEST
        ));
        NOTES.add(new NoteTemplate(
                "Защита деревни",
                "Требуется доброволец для охраны стен ночью. Плата: 3 изумруда за ночь.",
                NoteCategory.QUEST
        ));
        NOTES.add(new NoteTemplate(
                "Сбор трав",
                "Нужны лекарственные травы: 5 папоротников, 3 одуванчика. Для лекаря.",
                NoteCategory.QUEST
        ));
        NOTES.add(new NoteTemplate(
                "Доставка еды",
                "Отнести корзину с едой на ферму северо-восток. Награда на месте.",
                NoteCategory.QUEST
        ));
    }
    
    public enum NoteCategory {
        ANNOUNCEMENT(0xFFAA00),    // Оранжевый
        WARNING(0xFF5555),          // Красный
        PERSONAL(0x55FFFF),         // Голубой
        QUEST(0x55FF55);            // Зелёный
        
        public final int defaultBadgeColor;
        
        NoteCategory(int defaultBadgeColor) {
            this.defaultBadgeColor = defaultBadgeColor;
        }
    }
    
    public static class NoteTemplate {
        public final String title;
        public final String content;
        public final NoteCategory category;
        
        public NoteTemplate(String title, String content, NoteCategory category) {
            this.title = title;
            this.content = content;
            this.category = category;
        }
    }
    
    /**
     * Генерирует случайную записку из пула.
     * @param random Генератор случайных чисел
     * @return NoteData с заполненными полями
     */
    public static NoteData generateRandomNote(Random random) {
        NoteTemplate template = NOTES.get(random.nextInt(NOTES.size()));
        
        // Выбираем случайный цвет печати на основе категории
        int badgeColor = getRandomBadgeColorForCategory(template.category, random);
        
        NoteData note = new NoteData(
                template.title,
                template.content,
                "Аноним",
                badgeColor,
                System.currentTimeMillis(),
                true,  // hasSeal = true (с печатью)
                false  // isSmall = false (обычная записка)
        );
        
        return note;
    }
    
    /**
     * Генерирует случайную записку с маленьким размером.
     */
    public static NoteData generateRandomSmallNote(Random random) {
        NoteTemplate template = NOTES.get(random.nextInt(NOTES.size()));
        int badgeColor = getRandomBadgeColorForCategory(template.category, random);
        
        NoteData note = new NoteData(
                template.title,
                template.content.length() > 100 ? 
                        template.content.substring(0, 100) + "..." : template.content,
                "Аноним",
                badgeColor,
                System.currentTimeMillis(),
                true,
                true  // isSmall = true
        );
        
        return note;
    }
    
    private static int getRandomBadgeColorForCategory(NoteCategory category, Random random) {
        // Возвращаем цвет по умолчанию для категории с шансом 60%
        // Или случайный цвет из палитры с шансом 40%
        if (random.nextFloat() < 0.6f) {
            return category.defaultBadgeColor;
        }
        
        int[] colors = {
                0x1E1E1E, 0xFF5555, 0x55FF55, 0x8B4513,
                0x5555FF, 0xAA00AA, 0x00AAAA, 0xAAAAAA,
                0x555555, 0xFF55FF, 0x55FF55, 0xFFFF55,
                0x55FFFF, 0xFF55FF, 0xFFAA00, 0xFFFFFF
        };
        
        return colors[random.nextInt(colors.length)];
    }
    
    /**
     * Получение случайной записки только определённой категории.
     */
    public static NoteData generateNoteByCategory(NoteCategory category, Random random) {
        List<NoteTemplate> filtered = new ArrayList<>();
        for (NoteTemplate t : NOTES) {
            if (t.category == category) {
                filtered.add(t);
            }
        }
        
        if (filtered.isEmpty()) {
            return generateRandomNote(random);
        }
        
        NoteTemplate template = filtered.get(random.nextInt(filtered.size()));
        return new NoteData(
                template.title,
                template.content,
                "Аноним",
                category.defaultBadgeColor,
                System.currentTimeMillis(),
                true,
                false
        );
    }
}
