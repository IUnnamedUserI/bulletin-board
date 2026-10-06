# Технические заметки о портировании Fabric → Forge

Порт выполнен из каталога `Bulletin-board` (Fabric, Yarn mappings) в этот проект
(Forge 1.20.1, Mojang official mappings). Ниже — что именно пришлось заменить и почему.

## 1. Загрузчик и точка входа

| Fabric | Forge |
|---|---|
| `implements ModInitializer` + `onInitialize()` | `@Mod(BulletinBoardMod.FORGE_MOD_ID)` + конструктор класса |
| `implements ClientModInitializer` + `onInitializeClient()` | `@Mod.EventBusSubscriber(bus = Bus.MOD, value = Dist.CLIENT)` + `@SubscribeEvent onClientSetup(FMLClientSetupEvent)` |
| `fabric.mod.json` | `META-INF/mods.toml` (+ обязательный `pack.mcmeta`) |
| `@Environment(EnvType.CLIENT)` | `@OnlyIn(Dist.CLIENT)` |

**Идентификаторы.** Forge не допускает дефис в modId, поэтому:
- `BulletinBoardMod.FORGE_MOD_ID = "bulletin_board"` — для `@Mod` и `mods.toml`;
- `BulletinBoardMod.MOD_ID = "bulletin-board"` — namespace всех `ResourceLocation`.

Благодаря этому пути `assets/bulletin-board/**` и `data/bulletin-board/**`, а также рецепты
и ссылки внутри JSON остались без изменений (`ResourceLocation` дефис в namespace допускает).

## 2. Регистрация контента

**Важное отличие от Fabric.** В Fabric объекты можно создавать прямо в статических полях
класса мода. В Forge так делать нельзя: конструкторы `Item` и `Block` регистрируют
«intrusive holder» и требуют **размороженного** реестра, а на момент загрузки класса мода
реестры заморожены. Поэтому статический инициализатор вида

```java
public static final Item NOTE_PAPER = new NotePaperItem(...);   // ❌ IllegalStateException: Registry is already frozen
```

падает с `IllegalStateException: Registry is already frozen`, а объекты создаются
в обработчике `RegisterEvent`, где Forge реестр размораживает:

```java
public BulletinBoardMod() {
    FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegister);
}

private void onRegister(RegisterEvent event) {
    ResourceKey<? extends Registry<?>> key = event.getRegistryKey();
    if (key.equals(Registries.ITEM)) { ... }
    else if (key.equals(Registries.BLOCK)) { ... }
    else if (key.equals(Registries.BLOCK_ENTITY_TYPE)) { ... }
    else if (key.equals(Registries.CREATIVE_MODE_TAB)) { ... }
}
```

Из этого следуют три правила, которые важно не нарушить:

1. **Регистрировать нужно через `event.register(...)`, а не через `Registry.register(...)`.**
   Во время рассылки события реестр заблокирован, и прямой вызов падает с
   `IllegalStateException: Can not register to a locked registry. Modder should use Forge Register methods.`
   Поэтому в коде используется вспомогательный метод, который и регистрирует объект,
   и запоминает его в статическом поле мода:

   ```java
   private static <T> void reg(RegisterEvent event, ResourceKey<? extends Registry<T>> registryKey,
                               String name, Supplier<T> supplier, Consumer<T> assign) {
       event.register(registryKey, new ResourceLocation(MOD_ID, name), () -> {
           T value = supplier.get();
           assign.accept(value);
           return value;
       });
   }
   ```
2. `BlockItem` для блока регистрируется в событии `ITEM`, а не `BLOCK`: во время события
   `BLOCK` реестр предметов ещё заблокирован. Forge обрабатывает реестр `BLOCK` раньше `ITEM`,
   поэтому к моменту события `ITEM` блок уже зарегистрирован.
3. Поля предметов/блоков стали не `final` — они присваиваются во время события.

| Fabric | Forge |
|---|---|
| `FabricItemSettings().maxCount(n)` | `new Item.Properties().stacksTo(n)` |
| `FabricBlockSettings.copyOf(x).nonOpaque()` | `BlockBehaviour.Properties.copy(x).noOcclusion()` |
| `.noCollision()` | `.noCollission()` (написание Forge) |
| `.breakInstantly()` | `.instabreak()` |
| `FabricItemGroup.builder().displayName(..).entries(..)` | `CreativeModeTab.builder().title(..).displayItems(..)` |
| `FabricBlockEntityTypeBuilder.create(f, blocks).build()` | `BlockEntityType.Builder.of(f, blocks).build(null)` |

## 3. События

| Fabric API | Forge |
|---|---|
| `ServerTickEvents.START_SERVER_TICK` | `TickEvent.ServerTickEvent` (проверка `Phase.START`) |
| `ServerLifecycleEvents.SERVER_STOPPED` | `ServerStoppedEvent` |
| `ServerLifecycleEvents.SERVER_STARTING` | `ServerAboutToStartEvent` |
| `CommandRegistrationCallback.EVENT` | `RegisterCommandsEvent` |
| `ServerPlayConnectionEvents.JOIN` | `PlayerEvent.PlayerLoggedInEvent` |
| `WorldRenderEvents.END` | `RenderLevelStageEvent`, стадия `AFTER_ENTITIES` |
| `WorldRenderEvents.BEFORE_DEBUG_RENDER` | `RenderLevelStageEvent`, стадия `AFTER_PARTICLES` |

`MultiBufferSource` в `RenderLevelStageEvent` не передаётся, поэтому буфер берётся из
`Minecraft.getInstance().renderBuffers().bufferSource()`.

## 4. Сеть

Fabric-каналы по `Identifier` заменены на один `SimpleChannel`:

```java
public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(new ResourceLocation(MOD_ID, "main"))
        .networkProtocolVersion(() -> "1")
        .clientAcceptedVersions("1"::equals)
        .serverAcceptedVersions("1"::equals)
        .simpleChannel();
```

Идентификаторы сообщений (фиксированные, одинаковые на клиенте и сервере):
`0..2` — клиент→сервер, `3..5` — сервер→клиент.

Особенности:
- Пакеты `TakeNoteC2SPacket` и `UpdateNoteNbtC2SPacket` были `record` — превращены в обычные
  классы с теми же аксессорами, потому что Forge требует конструктор `(FriendlyByteBuf)`
  и однозначный метод-декодер.
- `buf.readString()/writeString()` → `readUtf()/writeUtf()`.
- Обработчики клиентских пакетов вызываются из общего класса через проверку
  `FMLEnvironment.dist == Dist.CLIENT`, чтобы клиентские классы не загружались на сервере
  (аналог `DistExecutor` без лишней зависимости).

## 5. Конфигурация

- `FabricLoader.getInstance().getConfigDir()` → `FMLPaths.CONFIGDIR.get()`.
- **Библиотека Jankson убрана.** В Fabric-версии JSON5-конфиг с комментариями разбирала
  Jankson, подключавшаяся как обычная библиотека. В Forge-порте внешних зависимостей нет:
  файл `config.json5` — это обычный JSON с комментариями `//`, поэтому комментарии снимаются
  собственным разбором (с учётом строковых литералов), а сам JSON разбирает **Gson**, который
  уже входит в состав Minecraft и потому гарантированно доступен загрузчику мода.

  Причины отказа от внешней библиотеки (проверено запуском сервера):
  1. **Forge JarJar** в dev-окружении не отдаёт вложенную библиотеку загрузчику класса мода:
     `NoClassDefFoundError: blue/endless/jankson/Jankson`, хотя jar присутствует в `-cp`.
     Дополнительно JarJar требует указывать **диапазон** версий (`[1.2.3,1.3)`), а не точную.
  2. Простая упаковка классов библиотеки внутрь jar (shade) проблему в dev тоже **не решает**:
     библиотека оказывается в `-cp`, но вне «legacy classpath» игрового слоя Forge.

  Формат файла при этом сохранён: старые `config.json5` читаются без изменений,
  а при создании нового файла в него по-прежнему записываются те же комментарии.

## 6. Генерация структур (замена мода `structure-pool-api`)

В Fabric-версии доска внедрялась в пулы деревенских домов сторонним модом
`net.fabric_extras.structure-pool-api`. В Forge аналога нет, поэтому `StructureRegistry`
делает это сам:

1. по `ServerAboutToStartEvent` берёт `RegistryAccess` сервера;
2. читает те же файлы данных `data/bulletin-board/structure_pool_api/inject_village_*.json`;
3. разбирает элемент структуры через `StructurePoolElement.CODEC` с `RegistryOps`;
4. добавляет элемент в список шаблонов нужного `StructureTemplatePool`.

Поле со списком шаблонов приватное и в продакшене носит SRG-имя, поэтому оно ищется
**по типу**, а не по имени: список, параметризованный именно `StructurePoolElement`
(это `templates`; в `rawTemplates` лежат `Pair`). Такой поиск работает и в dev, и в продакшене.

## 7. Прочие точечные замены

- `SavedData`/`DimensionDataStorage`: `ServerLevel.getDataStorage().computeIfAbsent(factory, supplier, name)`.
  Важно: класса `SavedData.Factory` в 1.20.1 **не существует** (появился позже) —
  используется сигнатура с `Function<CompoundTag, T>` и `Supplier<T>`.
- `PersistentState` → `SavedData`, `markDirty()` → `setDirty()`, `writeNbt/readNbt` → `save/load`.
- `BlockEntity`: `markDirty`→`setChanged`, `writeNbt/readNbt`→`saveAdditional/load`,
  `toInitialChunkDataNbt`→`getUpdateTag`, `getCachedState`→`getBlockState`,
  `world`→`level`, `pos`→`worldPosition`.
- `BlockEntityUpdateS2CPacket.create` → `ClientboundBlockEntityDataPacket.create`.
- `Text` → `Component`, `Text.literal` → `Component.literal`.
- `DrawContext` → `GuiGraphics`; `drawBorder` → `renderOutline`.
- `MatrixStack` → `PoseStack`; `push()/pop()` → `pushPose()/popPose()`;
  `peek().getPositionMatrix()` → `last().pose()`.
- `VertexConsumer.next()` → `endVertex()`.
- `RotationAxis.POSITIVE_X` → `Axis.XP` (и аналогично `YP/ZP/XN/YN/ZN`).
- `ValueLists.createIdToValueFunction` → `ByIdMap.continuous(...)`.
- `StringIdentifiable.asString()` → `StringRepresentable.getSerializedName()`.
- `Entity.getUuid()` → `getUUID()`, `Entity.getBlockPos()` → `blockPosition()`,
  `BlockPos.add(...)` → `offset(...)`.
- `CommandManager` → `Commands`, `ServerCommandSource` → `CommandSourceStack`,
  `sendFeedback(..)` → `sendSuccess(..)`, `sendError(..)` → `sendFailure(..)`,
  `hasPermissionLevel(n)` → `hasPermission(n)`.
- `world.getEntitiesByType(...)` прямого аналога не имеет: используется
  `getEntitiesOfClass(Class, AABB, Predicate)` либо обход `ServerLevel.getAllEntities()`.

## 8. Отличия в поведении

- `BulletinBoardCommand`: в оригинале позиция бралась через `BlockPosArgumentType.getBlockPos`
  (не требует загруженного чанка) — в порте сохранено ровно это поведение
  (`BlockPosArgument.getBlockPos`).
- Синхронизация имён жителей: сервер по-прежнему рассылает имена всем игрокам раз в секунду,
  но обход сущностей идёт через `ServerLevel.getAllEntities()` с фильтром по `Villager`
  (в Mojmap нет `getEntitiesByType` без ограничивающего AABB).
- Двойное внедрение доски в пулы (`StructureRegistry` + JSON-файлы) в оригинале приводило
  к двум вставкам; в порте источником истины сделаны JSON-файлы, поэтому вставка одна —
  с весом, указанным в данных.
