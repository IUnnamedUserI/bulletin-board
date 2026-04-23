package com.unnameduser.bulletinboard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.BulletinBoardBlockEntity;
import com.unnameduser.bulletinboard.event.VillageDiscountEvent;
import com.unnameduser.bulletinboard.event.VillageDiscountEvent.DiscountData;
import com.unnameduser.bulletinboard.integration.TradeOverhaulIntegration;
import com.unnameduser.bulletinboard.util.AutoNoteScheduler;
import com.unnameduser.bulletinboard.util.NoteData;
import com.unnameduser.bulletinboard.util.RandomNotePool;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Команды для управления досками объявлений.
 *
 * Доступные команды:
 * - /bulletin trigger [radius] — случайная записка на ближайшей доске
 * - /bulletin trigger-at <pos> — записка на указанную доску
 * - /bulletin clear <pos> — очистить все записки на доске
 */
public class BulletinBoardCommand {

    private static final SimpleCommandExceptionType NO_BOARD_NEARBY =
            new SimpleCommandExceptionType(Text.translatable("command.bulletin.no_board_nearby"));

    private static final SimpleCommandExceptionType BOARD_EMPTY =
            new SimpleCommandExceptionType(Text.translatable("command.bulletin.board_empty"));

    private static final SimpleCommandExceptionType CLEARED =
            new SimpleCommandExceptionType(Text.translatable("command.bulletin.clear.success"));

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {

        LiteralCommandNode<ServerCommandSource> bulletinNode = CommandManager
                .literal("bulletin")
                .requires(source -> source.hasPermissionLevel(2))
                .build();

        // /bulletin trigger [radius]
        LiteralCommandNode<ServerCommandSource> triggerNode = CommandManager
                .literal("trigger")
                .executes(BulletinBoardCommand::triggerRandom)
                .then(CommandManager
                        .argument("radius", IntegerArgumentType.integer(1, 100))
                        .executes(BulletinBoardCommand::triggerRandomWithRadius)
                )
                .build();

        // /bulletin trigger-at <pos>
        LiteralCommandNode<ServerCommandSource> triggerAtNode = CommandManager
                .literal("trigger-at")
                .then(CommandManager
                        .argument("pos", BlockPosArgumentType.blockPos())
                        .executes(BulletinBoardCommand::triggerAtPosition)
                )
                .build();

        // /bulletin clear <pos>
        LiteralCommandNode<ServerCommandSource> clearNode = CommandManager
                .literal("clear")
                .then(CommandManager
                        .argument("pos", BlockPosArgumentType.blockPos())
                        .executes(BulletinBoardCommand::clearBoard)
                )
                .build();

        // /bulletin list <pos>
        LiteralCommandNode<ServerCommandSource> listNode = CommandManager
                .literal("list")
                .then(CommandManager
                        .argument("pos", BlockPosArgumentType.blockPos())
                        .executes(BulletinBoardCommand::listNotes)
                )
                .build();

        // /bulletin scheduler <enable|disable|trigger|status>
        LiteralCommandNode<ServerCommandSource> schedulerNode = CommandManager
                .literal("scheduler")
                .then(CommandManager
                        .literal("enable")
                        .executes(BulletinBoardCommand::schedulerEnable)
                )
                .then(CommandManager
                        .literal("disable")
                        .executes(BulletinBoardCommand::schedulerDisable)
                )
                .then(CommandManager
                        .literal("trigger")
                        .executes(BulletinBoardCommand::schedulerTrigger)
                )
                .then(CommandManager
                        .literal("status")
                        .executes(BulletinBoardCommand::schedulerStatus)
                )
                .build();

        bulletinNode.addChild(triggerNode);
        bulletinNode.addChild(triggerAtNode);
        bulletinNode.addChild(clearNode);
        bulletinNode.addChild(listNode);
        bulletinNode.addChild(schedulerNode);

        // /bulletin discount <trigger|list|clear>
        LiteralCommandNode<ServerCommandSource> discountNode = CommandManager
                .literal("discount")
                .then(CommandManager
                        .literal("trigger")
                        .executes(BulletinBoardCommand::discountTrigger)
                )
                .then(CommandManager
                        .literal("list")
                        .executes(BulletinBoardCommand::discountList)
                )
                .then(CommandManager
                        .literal("clear")
                        .executes(BulletinBoardCommand::discountClear)
                )
                .build();

        bulletinNode.addChild(discountNode);

        dispatcher.getRoot().addChild(bulletinNode);
    }

    private static int triggerRandom(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return triggerRandomWithRadius(context, 10);
    }

    private static int triggerRandomWithRadius(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        return triggerRandomWithRadius(context, IntegerArgumentType.getInteger(context, "radius"));
    }

    private static int triggerRandomWithRadius(CommandContext<ServerCommandSource> context, int radius) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            source.sendError(Text.translatable("command.bulletin.no_player"));
            return 0;
        }

        ServerWorld world = source.getWorld();
        BlockPos playerPos = player.getBlockPos();

        // Ищем ближайшую доску объявлений в радиусе
        BlockPos boardPos = findNearestBulletinBoard(world, playerPos, radius);

        if (boardPos == null) {
            throw NO_BOARD_NEARBY.create();
        }

        // Генерируем и размещаем записку
        int notesAdded = placeRandomNote(world, boardPos);

        source.sendFeedback(() -> Text.translatable("command.bulletin.trigger.success", notesAdded), true);
        return notesAdded;
    }

    private static int triggerAtPosition(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
        ServerWorld world = source.getWorld();

        // Проверяем, что это доска объявлений
        if (!(world.getBlockState(pos).getBlock() instanceof BulletinBoardBlock)) {
            source.sendError(Text.translatable("command.bulletin.not_a_board"));
            return 0;
        }

        int notesAdded = placeRandomNote(world, pos);
        source.sendFeedback(() -> Text.translatable("command.bulletin.trigger.success", notesAdded), true);
        return notesAdded;
    }

    /**
     * Размещает случайную записку на доске.
     */
    private static int placeRandomNote(ServerWorld world, BlockPos boardPos) {
        var blockEntity = world.getBlockEntity(boardPos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            return 0;
        }

        // Генерируем случайную записку
        NoteData note = RandomNotePool.generateRandomNote(world.getRandom());

        // Находим свободный слот
        List<Integer> freeSlots = findFreeSlots(boardEntity);
        if (freeSlots.isEmpty()) {
            return 0;
        }

        // Выбираем случайный слот
        int targetSlot = freeSlots.get(world.getRandom().nextInt(freeSlots.size()));

        // Размещаем записку
        if (boardEntity.addNoteAtPosition(note, targetSlot)) {
            return 1;
        }

        return 0;
    }

    private static List<Integer> findFreeSlots(BulletinBoardBlockEntity boardEntity) {
        List<Integer> freeSlots = new java.util.ArrayList<>();

        // Проверяем каждый слот (0-4)
        for (int i = 0; i < 5; i++) {
            if (boardEntity.isPositionFree(i)) {
                freeSlots.add(i);
            }
        }

        return freeSlots;
    }

    private static BlockPos findNearestBulletinBoard(ServerWorld world, BlockPos center, int radius) {
        // Ищем доски объявлений в радиусе
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.add(x, y, z);
                    if (world.getBlockState(pos).getBlock() instanceof BulletinBoardBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static int clearBoard(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
        ServerWorld world = source.getWorld();

        var blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            source.sendError(Text.translatable("command.bulletin.not_a_board"));
            return 0;
        }

        var notes = boardEntity.getNotes();
        if (notes.isEmpty()) {
            throw BOARD_EMPTY.create();
        }

        int count = notes.size();
        for (int i = count - 1; i >= 0; i--) {
            boardEntity.removeNote(i);
        }

        source.sendFeedback(() -> Text.translatable("command.bulletin.clear.success"), true);
        return count;
    }

    private static int listNotes(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
        ServerWorld world = source.getWorld();

        var blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            source.sendError(Text.translatable("command.bulletin.not_a_board"));
            return 0;
        }

        var notes = boardEntity.getNotes();
        if (notes.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("command.bulletin.list.empty"), true);
            return 0;
        }

        source.sendFeedback(() -> Text.translatable("command.bulletin.list.header", notes.size()), true);

        int index = 0;
        for (var note : notes) {
            final int currentIndex = index;
            source.sendFeedback(() -> Text.literal(
                    String.format("§6[%d] §r%s §7- %s",
                            currentIndex,
                            note.getTitle(),
                            note.getAuthor())
            ), false);
            index++;
        }

        return notes.size();
    }

    private static int schedulerEnable(CommandContext<ServerCommandSource> context) {
        AutoNoteScheduler.setEnabled(true);
        context.getSource().sendFeedback(() -> Text.translatable("command.bulletin.scheduler.enabled"), true);
        return 1;
    }

    private static int schedulerDisable(CommandContext<ServerCommandSource> context) {
        AutoNoteScheduler.setEnabled(false);
        context.getSource().sendFeedback(() -> Text.translatable("command.bulletin.scheduler.disabled"), true);
        return 1;
    }

    private static int schedulerTrigger(CommandContext<ServerCommandSource> context) {
        AutoNoteScheduler.triggerNow();
        context.getSource().sendFeedback(() -> Text.translatable("command.bulletin.scheduler.triggered"), true);
        return 1;
    }

    private static int schedulerStatus(CommandContext<ServerCommandSource> context) {
        boolean enabled = AutoNoteScheduler.isEnabled();
        Text status = enabled ? 
                Text.translatable("command.bulletin.scheduler.status.enabled") :
                Text.translatable("command.bulletin.scheduler.status.disabled");
        context.getSource().sendFeedback(() -> status, true);
        return 1;
    }

    private static int discountTrigger(CommandContext<ServerCommandSource> context) {
        VillageDiscountEvent event = VillageDiscountEvent.getInstance();
        
        if (event == null) {
            context.getSource().sendError(Text.translatable("command.bulletin.not_initialized"));
            return 0;
        }
        
        boolean success = event.triggerDiscountEvent();
        
        if (success) {
            context.getSource().sendFeedback(() -> Text.translatable("command.bulletin.discount.trigger.success"), true);
            return 1;
        } else {
            context.getSource().sendError(Text.translatable("command.bulletin.discount.trigger.failed"));
            return 0;
        }
    }

    private static int discountList(CommandContext<ServerCommandSource> context) {
        VillageDiscountEvent event = VillageDiscountEvent.getInstance();
        
        if (event == null) {
            context.getSource().sendError(Text.translatable("command.bulletin.not_initialized"));
            return 0;
        }
        
        var discounts = event.getActiveDiscounts();
        
        if (discounts.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.translatable("command.bulletin.discount.list.empty"), true);
            return 0;
        }
        
        // Добавляем информацию об интеграции
        boolean tradeOverhaulPresent = TradeOverhaulIntegration.isTradeOverhaulPresent();
        Text integrationStatus = tradeOverhaulPresent ?
                Text.literal("§a[Trade Overhaul ENABLED] §r") :
                Text.literal("§c[Trade Overhaul DISABLED] §r");
        
        context.getSource().sendFeedback(() -> integrationStatus, false);
        context.getSource().sendFeedback(() -> Text.translatable("command.bulletin.discount.list.header", discounts.size()), true);
        
        for (var discount : discounts) {
            long remainingMinutes = discount.getRemainingTime() / 60000;
            String discountType = tradeOverhaulPresent ? "§aREAL" : "§eINFO";
            Text message = Text.literal(String.format("  %s §6%s (%s) - §e%d мин. осталось",
                    discountType,
                    discount.villagerName, discount.profession, remainingMinutes));
            context.getSource().sendFeedback(() -> message, false);
        }
        
        return discounts.size();
    }

    private static int discountClear(CommandContext<ServerCommandSource> context) {
        VillageDiscountEvent event = VillageDiscountEvent.getInstance();
        
        if (event == null) {
            context.getSource().sendError(Text.translatable("command.bulletin.not_initialized"));
            return 0;
        }
        
        event.clearAllDiscounts();
        context.getSource().sendFeedback(() -> Text.translatable("command.bulletin.discount.clear.success"), true);
        return 1;
    }
}
