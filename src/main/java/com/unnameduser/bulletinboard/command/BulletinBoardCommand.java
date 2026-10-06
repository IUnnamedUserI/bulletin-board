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
import com.unnameduser.bulletinboard.integration.TradeOverhaulIntegration;
import com.unnameduser.bulletinboard.util.AutoNoteScheduler;
import com.unnameduser.bulletinboard.util.NoteData;
import com.unnameduser.bulletinboard.util.RandomNotePool;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

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
            new SimpleCommandExceptionType(Component.translatable("command.bulletin.no_board_nearby"));

    private static final SimpleCommandExceptionType BOARD_EMPTY =
            new SimpleCommandExceptionType(Component.translatable("command.bulletin.board_empty"));

    private static final SimpleCommandExceptionType CLEARED =
            new SimpleCommandExceptionType(Component.translatable("command.bulletin.clear.success"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                CommandBuildContext ctx,
                                Commands.CommandSelection selection) {

        LiteralCommandNode<CommandSourceStack> bulletinNode = Commands
                .literal("bulletin")
                .requires(source -> source.hasPermission(2))
                .build();

        // /bulletin trigger [radius]
        LiteralCommandNode<CommandSourceStack> triggerNode = Commands
                .literal("trigger")
                .executes(BulletinBoardCommand::triggerRandom)
                .then(Commands
                        .argument("radius", IntegerArgumentType.integer(1, 100))
                        .executes(BulletinBoardCommand::triggerRandomWithRadius)
                )
                .build();

        // /bulletin trigger-at <pos>
        LiteralCommandNode<CommandSourceStack> triggerAtNode = Commands
                .literal("trigger-at")
                .then(Commands
                        .argument("pos", BlockPosArgument.blockPos())
                        .executes(BulletinBoardCommand::triggerAtPosition)
                )
                .build();

        // /bulletin clear <pos>
        LiteralCommandNode<CommandSourceStack> clearNode = Commands
                .literal("clear")
                .then(Commands
                        .argument("pos", BlockPosArgument.blockPos())
                        .executes(BulletinBoardCommand::clearBoard)
                )
                .build();

        // /bulletin list <pos>
        LiteralCommandNode<CommandSourceStack> listNode = Commands
                .literal("list")
                .then(Commands
                        .argument("pos", BlockPosArgument.blockPos())
                        .executes(BulletinBoardCommand::listNotes)
                )
                .build();

        // /bulletin scheduler <enable|disable|trigger|status>
        LiteralCommandNode<CommandSourceStack> schedulerNode = Commands
                .literal("scheduler")
                .then(Commands
                        .literal("enable")
                        .executes(BulletinBoardCommand::schedulerEnable)
                )
                .then(Commands
                        .literal("disable")
                        .executes(BulletinBoardCommand::schedulerDisable)
                )
                .then(Commands
                        .literal("trigger")
                        .executes(BulletinBoardCommand::schedulerTrigger)
                )
                .then(Commands
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
        LiteralCommandNode<CommandSourceStack> discountNode = Commands
                .literal("discount")
                .then(Commands
                        .literal("trigger")
                        .executes(BulletinBoardCommand::discountTrigger)
                )
                .then(Commands
                        .literal("list")
                        .executes(BulletinBoardCommand::discountList)
                )
                .then(Commands
                        .literal("clear")
                        .executes(BulletinBoardCommand::discountClear)
                )
                .build();

        bulletinNode.addChild(discountNode);

        dispatcher.getRoot().addChild(bulletinNode);
    }

    private static int triggerRandom(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return triggerRandomWithRadius(context, 10);
    }

    private static int triggerRandomWithRadius(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return triggerRandomWithRadius(context, IntegerArgumentType.getInteger(context, "radius"));
    }

    private static int triggerRandomWithRadius(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.translatable("command.bulletin.no_player"));
            return 0;
        }

        ServerLevel world = source.getLevel();
        BlockPos playerPos = player.blockPosition();

        // Ищем ближайшую доску объявлений в радиусе
        BlockPos boardPos = findNearestBulletinBoard(world, playerPos, radius);

        if (boardPos == null) {
            throw NO_BOARD_NEARBY.create();
        }

        // Генерируем и размещаем записку
        int notesAdded = placeRandomNote(world, boardPos);

        source.sendSuccess(() -> Component.translatable("command.bulletin.trigger.success", notesAdded), true);
        return notesAdded;
    }

    private static int triggerAtPosition(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        ServerLevel world = source.getLevel();

        // Проверяем, что это доска объявлений
        if (!(world.getBlockState(pos).getBlock() instanceof BulletinBoardBlock)) {
            source.sendFailure(Component.translatable("command.bulletin.not_a_board"));
            return 0;
        }

        int notesAdded = placeRandomNote(world, pos);
        source.sendSuccess(() -> Component.translatable("command.bulletin.trigger.success", notesAdded), true);
        return notesAdded;
    }

    /**
     * Размещает случайную записку на доске.
     */
    private static int placeRandomNote(ServerLevel world, BlockPos boardPos) {
        var blockEntity = world.getBlockEntity(boardPos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            return 0;
        }

        NoteData note = RandomNotePool.generateRandomNote(
                world.getRandom(),
                "Команда",
                "command",
                false
        );

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

    private static BlockPos findNearestBulletinBoard(ServerLevel world, BlockPos center, int radius) {
        // Ищем доски объявлений в радиусе
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (world.getBlockState(pos).getBlock() instanceof BulletinBoardBlock) {
                        return pos;
                    }
                }
            }
        }
        return null;
    }

    private static int clearBoard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        ServerLevel world = source.getLevel();

        var blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            source.sendFailure(Component.translatable("command.bulletin.not_a_board"));
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

        source.sendSuccess(() -> Component.translatable("command.bulletin.clear.success"), true);
        return count;
    }

    private static int listNotes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");
        ServerLevel world = source.getLevel();

        var blockEntity = world.getBlockEntity(pos);

        if (!(blockEntity instanceof BulletinBoardBlockEntity boardEntity)) {
            source.sendFailure(Component.translatable("command.bulletin.not_a_board"));
            return 0;
        }

        var notes = boardEntity.getNotes();
        if (notes.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("command.bulletin.list.empty"), true);
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("command.bulletin.list.header", notes.size()), true);

        int index = 0;
        for (var note : notes) {
            final int currentIndex = index;
            source.sendSuccess(() -> Component.literal(
                    String.format("§6[%d] §r%s §7- %s",
                            currentIndex,
                            note.getTitle(),
                            note.getAuthor())
            ), false);
            index++;
        }

        return notes.size();
    }

    private static int schedulerEnable(CommandContext<CommandSourceStack> context) {
        AutoNoteScheduler.setEnabled(true);
        context.getSource().sendSuccess(() -> Component.translatable("command.bulletin.scheduler.enabled"), true);
        return 1;
    }

    private static int schedulerDisable(CommandContext<CommandSourceStack> context) {
        AutoNoteScheduler.setEnabled(false);
        context.getSource().sendSuccess(() -> Component.translatable("command.bulletin.scheduler.disabled"), true);
        return 1;
    }

    private static int schedulerTrigger(CommandContext<CommandSourceStack> context) {
        AutoNoteScheduler.triggerNow();
        context.getSource().sendSuccess(() -> Component.translatable("command.bulletin.scheduler.triggered"), true);
        return 1;
    }

    private static int schedulerStatus(CommandContext<CommandSourceStack> context) {
        boolean enabled = AutoNoteScheduler.isEnabled();
        Component status = enabled ?
                Component.translatable("command.bulletin.scheduler.status.enabled") :
                Component.translatable("command.bulletin.scheduler.status.disabled");
        context.getSource().sendSuccess(() -> status, true);
        return 1;
    }

    private static int discountTrigger(CommandContext<CommandSourceStack> context) {
        VillageDiscountEvent event = VillageDiscountEvent.getInstance();

        if (event == null) {
            context.getSource().sendFailure(Component.translatable("command.bulletin.not_initialized"));
            return 0;
        }

        boolean success = event.triggerDiscountEvent();

        if (success) {
            context.getSource().sendSuccess(() -> Component.translatable("command.bulletin.discount.trigger.success"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.translatable("command.bulletin.discount.trigger.failed"));
            return 0;
        }
    }

    private static int discountList(CommandContext<CommandSourceStack> context) {
        VillageDiscountEvent event = VillageDiscountEvent.getInstance();

        if (event == null) {
            context.getSource().sendFailure(Component.translatable("command.bulletin.not_initialized"));
            return 0;
        }

        var discounts = event.getActiveDiscounts();

        if (discounts.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatable("command.bulletin.discount.list.empty"), true);
            return 0;
        }

        // Добавляем информацию об интеграции
        boolean tradeOverhaulPresent = TradeOverhaulIntegration.isTradeOverhaulPresent();
        Component integrationStatus = tradeOverhaulPresent ?
                Component.literal("§a[Trade Overhaul ENABLED] §r") :
                Component.literal("§c[Trade Overhaul DISABLED] §r");

        context.getSource().sendSuccess(() -> integrationStatus, false);
        context.getSource().sendSuccess(() -> Component.translatable("command.bulletin.discount.list.header", discounts.size()), true);

        for (var discount : discounts) {
            long remainingMinutes = discount.getRemainingTime() / 60000;
            String discountType = tradeOverhaulPresent ? "§aREAL" : "§eINFO";
            Component message = Component.literal(String.format("  %s §6%s (%s) - §e%d мин. осталось",
                    discountType,
                    discount.villagerName, discount.profession, remainingMinutes));
            context.getSource().sendSuccess(() -> message, false);
        }

        return discounts.size();
    }

    private static int discountClear(CommandContext<CommandSourceStack> context) {
        VillageDiscountEvent event = VillageDiscountEvent.getInstance();

        if (event == null) {
            context.getSource().sendFailure(Component.translatable("command.bulletin.not_initialized"));
            return 0;
        }

        event.clearAllDiscounts();
        context.getSource().sendSuccess(() -> Component.translatable("command.bulletin.discount.clear.success"), true);
        return 1;
    }
}
