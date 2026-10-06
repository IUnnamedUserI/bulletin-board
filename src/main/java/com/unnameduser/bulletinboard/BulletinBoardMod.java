package com.unnameduser.bulletinboard;

import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.block.ModBlockEntities;
import com.unnameduser.bulletinboard.block.PlacedNoteBlock;
import com.unnameduser.bulletinboard.command.BulletinBoardCommand;
import com.unnameduser.bulletinboard.config.ModConfig;
import com.unnameduser.bulletinboard.config.NoteConfigLoader;
import com.unnameduser.bulletinboard.config.VillagerNameConfig;
import com.unnameduser.bulletinboard.event.VillageDiscountEvent;
import com.unnameduser.bulletinboard.integration.TradeOverhaulIntegration;
import com.unnameduser.bulletinboard.item.BadgeItem;
import com.unnameduser.bulletinboard.item.NotePaperItem;
import com.unnameduser.bulletinboard.network.ModPackets;
import com.unnameduser.bulletinboard.server.VillagerNameManager;
import com.unnameduser.bulletinboard.util.AutoNoteScheduler;
import com.unnameduser.bulletinboard.world.StructureRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod(BulletinBoardMod.FORGE_MOD_ID)
public class BulletinBoardMod {
	/** Namespace ресурсов мода (assets/bulletin-board, data/bulletin-board). */
	public static final String MOD_ID = "bulletin-board";

	/** Идентификатор мода для Forge (в mods.toml и в аннотации @Mod). */
	public static final String FORGE_MOD_ID = "bulletin_board";

	// В Forge реестры заморожены в момент загрузки класса мода, поэтому предметы и блоки
	// нельзя создавать в статических инициализаторах: конструкторы Item/Block регистрируют
	// «intrusive holder» и требуют размороженного реестра. Все объекты создаются
	// в обработчике RegisterEvent, где Forge реестр размораживает.
	public static Item NOTE_PAPER;
	public static Item SMALL_NOTE_PAPER;

	public static Item BLACK_BADGE;
	public static Item RED_BADGE;
	public static Item GREEN_BADGE;
	public static Item BROWN_BADGE;
	public static Item BLUE_BADGE;
	public static Item PURPLE_BADGE;
	public static Item CYAN_BADGE;
	public static Item LIGHT_GRAY_BADGE;
	public static Item GRAY_BADGE;
	public static Item PINK_BADGE;
	public static Item LIME_BADGE;
	public static Item YELLOW_BADGE;
	public static Item LIGHT_BLUE_BADGE;
	public static Item MAGENTA_BADGE;
	public static Item ORANGE_BADGE;
	public static Item WHITE_BADGE;

	public static Block BULLETIN_BOARD;
	public static Block PLACED_NOTE;

	public static CreativeModeTab BULLETIN_BOARD_GROUP;

	public BulletinBoardMod() {
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(this::onRegister);

		// Сетевой канал и конфиги не зависят от реестров
		ModPackets.register();

		ModConfig.load();
		NoteConfigLoader.load();
		VillagerNameConfig.load();
		StructureRegistry.register();

		registerServerEvents();
	}

	// ============ РЕГИСТРАЦИЯ КОНТЕНТА ============

	private void onRegister(RegisterEvent event) {
		ResourceKey<? extends Registry<?>> key = event.getRegistryKey();

		if (key.equals(Registries.ITEM)) {
			registerItems(event);
		} else if (key.equals(Registries.BLOCK)) {
			registerBlocks(event);
		} else if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
			ModBlockEntities.register(event);
		} else if (key.equals(Registries.CREATIVE_MODE_TAB)) {
			registerCreativeTab(event);
		}
	}

	/**
	 * Регистрирует объект через Forge-метод {@code RegisterEvent#register} и запоминает его
	 * в статическом поле мода. Прямой вызов {@code Registry.register} здесь недопустим:
	 * во время рассылки события реестр заблокирован.
	 */
	private static <T> void reg(RegisterEvent event, ResourceKey<? extends Registry<T>> registryKey,
	                            String name, Supplier<T> supplier, Consumer<T> assign) {
		event.register(registryKey, new ResourceLocation(MOD_ID, name), () -> {
			T value = supplier.get();
			assign.accept(value);
			return value;
		});
	}

	private void registerItems(RegisterEvent event) {
		reg(event, Registries.ITEM, "note_paper",
				() -> new NotePaperItem(new Item.Properties().stacksTo(1), false), v -> NOTE_PAPER = v);
		reg(event, Registries.ITEM, "small_note_paper",
				() -> new NotePaperItem(new Item.Properties().stacksTo(1), true), v -> SMALL_NOTE_PAPER = v);

		reg(event, Registries.ITEM, "black_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x1E1E1E), v -> BLACK_BADGE = v);
		reg(event, Registries.ITEM, "red_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xFF5555), v -> RED_BADGE = v);
		reg(event, Registries.ITEM, "green_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x55FF55), v -> GREEN_BADGE = v);
		reg(event, Registries.ITEM, "brown_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x8B4513), v -> BROWN_BADGE = v);
		reg(event, Registries.ITEM, "blue_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x5555FF), v -> BLUE_BADGE = v);
		reg(event, Registries.ITEM, "purple_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xAA00AA), v -> PURPLE_BADGE = v);
		reg(event, Registries.ITEM, "cyan_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x00AAAA), v -> CYAN_BADGE = v);
		reg(event, Registries.ITEM, "light_gray_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xAAAAAA), v -> LIGHT_GRAY_BADGE = v);
		reg(event, Registries.ITEM, "gray_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x555555), v -> GRAY_BADGE = v);
		reg(event, Registries.ITEM, "pink_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xFF55FF), v -> PINK_BADGE = v);
		reg(event, Registries.ITEM, "lime_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x55FF55), v -> LIME_BADGE = v);
		reg(event, Registries.ITEM, "yellow_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xFFFF55), v -> YELLOW_BADGE = v);
		reg(event, Registries.ITEM, "light_blue_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0x55FFFF), v -> LIGHT_BLUE_BADGE = v);
		reg(event, Registries.ITEM, "magenta_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xFF55FF), v -> MAGENTA_BADGE = v);
		reg(event, Registries.ITEM, "orange_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xFFAA00), v -> ORANGE_BADGE = v);
		reg(event, Registries.ITEM, "white_badge",
				() -> new BadgeItem(new Item.Properties().stacksTo(16), 0xFFFFFF), v -> WHITE_BADGE = v);

		// BlockItem'ы блоков регистрируются именно здесь: во время события BLOCK реестр
		// предметов ещё заблокирован. Реестр BLOCK обрабатывается Forge раньше реестра ITEM.
		reg(event, Registries.ITEM, "bulletin_board",
				() -> new BlockItem(BULLETIN_BOARD, new Item.Properties()), v -> { });
		reg(event, Registries.ITEM, "placed_note",
				() -> new BlockItem(PLACED_NOTE, new Item.Properties()), v -> { });
	}

	private void registerBlocks(RegisterEvent event) {
		reg(event, Registries.BLOCK, "bulletin_board",
				() -> new BulletinBoardBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()),
				v -> BULLETIN_BOARD = v);

		reg(event, Registries.BLOCK, "placed_note",
				() -> new PlacedNoteBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS)
						.noOcclusion().noCollission().instabreak()),
				v -> PLACED_NOTE = v);
	}

	private void registerCreativeTab(RegisterEvent event) {
		reg(event, Registries.CREATIVE_MODE_TAB, "general", () -> CreativeModeTab.builder()
				.icon(() -> new ItemStack(NOTE_PAPER))
				.title(Component.literal("Bulletin Boards"))
				.displayItems((params, entries) -> {
					entries.accept(NOTE_PAPER);
					entries.accept(SMALL_NOTE_PAPER);
					entries.accept(BLACK_BADGE);
					entries.accept(RED_BADGE);
					entries.accept(GREEN_BADGE);
					entries.accept(BROWN_BADGE);
					entries.accept(BLUE_BADGE);
					entries.accept(PURPLE_BADGE);
					entries.accept(CYAN_BADGE);
					entries.accept(LIGHT_GRAY_BADGE);
					entries.accept(GRAY_BADGE);
					entries.accept(PINK_BADGE);
					entries.accept(LIME_BADGE);
					entries.accept(YELLOW_BADGE);
					entries.accept(LIGHT_BLUE_BADGE);
					entries.accept(MAGENTA_BADGE);
					entries.accept(ORANGE_BADGE);
					entries.accept(WHITE_BADGE);
					entries.accept(BULLETIN_BOARD);
				})
				.build(), v -> BULLETIN_BOARD_GROUP = v);
	}

	// ============ СОБЫТИЯ СЕРВЕРА (игровая шина) ============

	private void registerServerEvents() {
		MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
		MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
		MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
		MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
	}

	private void onServerStopped(ServerStoppedEvent event) {
		AutoNoteScheduler.reset();
	}

	private void onRegisterCommands(RegisterCommandsEvent event) {
		BulletinBoardCommand.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
	}

	private void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;

		MinecraftServer server = event.getServer();

		if (AutoNoteScheduler.getInstance() == null && server.getTickCount() > 200) {
			AutoNoteScheduler.init(server);
		}
		if (AutoNoteScheduler.getInstance() != null) {
			AutoNoteScheduler.tick();
		}
		if (VillageDiscountEvent.getInstance() == null) {
			VillageDiscountEvent.init(server);
		}
		VillageDiscountEvent.getInstance().tick();
		TradeOverhaulIntegration.tick();

		// Синхронизация имён жителей (раз в секунду)
		if (server.isStopped()) return;
		if (server.getTickCount() % 20 == 0) {
			VillagerNameManager manager = VillagerNameManager.get(server);
			ServerLevel world = server.overworld();
			if (world == null) return;

			boolean hasNewNames = false;
			for (Entity entity : world.getAllEntities()) {
				if (entity instanceof Villager villager) {
					String nameKey = manager.getOrCreateNameKey(villager.getUUID());
					if (nameKey != null) {
						hasNewNames = true;
					}
				}
			}

			if (hasNewNames) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					ModPackets.sendVillagerNames(player, server);
				}
			}
		}
	}

	private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			MinecraftServer server = player.getServer();
			if (server != null) {
				ModPackets.sendVillagerNames(player, server);
			}
		}
	}
}
