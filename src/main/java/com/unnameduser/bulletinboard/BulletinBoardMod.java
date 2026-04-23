package com.unnameduser.bulletinboard;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.ModBlockEntities;
import com.unnameduser.bulletinboard.command.BulletinBoardCommand;
import com.unnameduser.bulletinboard.event.VillageDiscountEvent;
import com.unnameduser.bulletinboard.integration.TradeOverhaulIntegration;
import com.unnameduser.bulletinboard.item.BadgeItem;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.util.AutoNoteScheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BulletinBoardMod implements ModInitializer {
	public static final String MOD_ID = "bulletin-board";

	public static final Item NOTE_PAPER = new NotePaperItem(
			new FabricItemSettings().maxCount(1), false
	);

	public static final Item SMALL_NOTE_PAPER = new NotePaperItem(
			new FabricItemSettings().maxCount(1), true
	);

	public static final Item BLACK_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x1E1E1E);
	public static final Item RED_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xFF5555);
	public static final Item GREEN_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x55FF55);
	public static final Item BROWN_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x8B4513);
	public static final Item BLUE_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x5555FF);
	public static final Item PURPLE_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xAA00AA);
	public static final Item CYAN_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x00AAAA);
	public static final Item LIGHT_GRAY_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xAAAAAA);
	public static final Item GRAY_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x555555);
	public static final Item PINK_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xFF55FF);
	public static final Item LIME_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x55FF55);
	public static final Item YELLOW_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xFFFF55);
	public static final Item LIGHT_BLUE_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0x55FFFF);
	public static final Item MAGENTA_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xFF55FF);
	public static final Item ORANGE_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xFFAA00);
	public static final Item WHITE_BADGE = new BadgeItem(new FabricItemSettings().maxCount(16), 0xFFFFFF);

	public static final Block BULLETIN_BOARD = new BulletinBoardBlock(
			FabricBlockSettings.copyOf(Blocks.OAK_PLANKS).nonOpaque()
	);

	public static final ItemGroup BULLETIN_BOARD_GROUP = FabricItemGroup.builder()
			.icon(() -> new ItemStack(NOTE_PAPER))
			.displayName(Text.literal("Bulletin Boards"))
			.entries((context, entries) -> {
				entries.add(NOTE_PAPER);
				entries.add(SMALL_NOTE_PAPER);
				entries.add(BLACK_BADGE);
				entries.add(RED_BADGE);
				entries.add(GREEN_BADGE);
				entries.add(BROWN_BADGE);
				entries.add(BLUE_BADGE);
				entries.add(PURPLE_BADGE);
				entries.add(CYAN_BADGE);
				entries.add(LIGHT_GRAY_BADGE);
				entries.add(GRAY_BADGE);
				entries.add(PINK_BADGE);
				entries.add(LIME_BADGE);
				entries.add(YELLOW_BADGE);
				entries.add(LIGHT_BLUE_BADGE);
				entries.add(MAGENTA_BADGE);
				entries.add(ORANGE_BADGE);
				entries.add(WHITE_BADGE);
				entries.add(BULLETIN_BOARD);
			})
			.build();

	@Override
	public void onInitialize() {
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "note_paper"), NOTE_PAPER);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "small_note_paper"), SMALL_NOTE_PAPER);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "black_badge"), BLACK_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "red_badge"), RED_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "green_badge"), GREEN_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "brown_badge"), BROWN_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "blue_badge"), BLUE_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "purple_badge"), PURPLE_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "cyan_badge"), CYAN_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "light_gray_badge"), LIGHT_GRAY_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "gray_badge"), GRAY_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "pink_badge"), PINK_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "lime_badge"), LIME_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "yellow_badge"), YELLOW_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "light_blue_badge"), LIGHT_BLUE_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "magenta_badge"), MAGENTA_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "orange_badge"), ORANGE_BADGE);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, "white_badge"), WHITE_BADGE);

		registerBlock("bulletin_board", BULLETIN_BOARD);

		ModBlockEntities.register();
		ModPackets.register();
		registerCommands();
		registerServerTickEvents();

		Registry.register(Registries.ITEM_GROUP,
				new Identifier(MOD_ID, "general"),
				BULLETIN_BOARD_GROUP);
	}

	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register(BulletinBoardCommand::register);
	}

	private void registerServerTickEvents() {
		ServerTickEvents.START_SERVER_TICK.register(server -> {
			// Инициализируем планировщик при первом тике сервера
			if (AutoNoteScheduler.getInstance() == null) {
				AutoNoteScheduler.init(server);
			}
			AutoNoteScheduler.tick();
			
			// Инициализируем ивенты скидок при первом тике
			if (VillageDiscountEvent.getInstance() == null) {
				VillageDiscountEvent.init(server);
			}
			VillageDiscountEvent.getInstance().tick();
			
			// Тикаем интеграцию с Trade Overhaul
			TradeOverhaulIntegration.tick();
		});
	}

	private static void registerBlock(String name, Block block) {
		Registry.register(Registries.BLOCK, new Identifier(MOD_ID, name), block);
		Registry.register(Registries.ITEM, new Identifier(MOD_ID, name),
				new BlockItem(block, new FabricItemSettings()));
	}
}