package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModBlockEntities {
    public static BlockEntityType<BulletinBoardBlockEntity> BULLETIN_BOARD_ENTITY;
    public static BlockEntityType<PlacedNoteBlockEntity> PLACED_NOTE_ENTITY;

    /**
     * Регистрация выполняется через NeoForge RegisterEvent:
     * во время рассылки события реестр заблокирован, и прямой Registry.register
     * приводит к ошибке «Can not register to a locked registry».
     */
    public static void register(RegisterEvent event) {
        event.register(
                Registries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "bulletin_board"),
                () -> {
                    BULLETIN_BOARD_ENTITY = BlockEntityType.Builder.of(
                            BulletinBoardBlockEntity::new,
                            BulletinBoardMod.BULLETIN_BOARD
                    ).build(null);
                    return BULLETIN_BOARD_ENTITY;
                }
        );

        event.register(
                Registries.BLOCK_ENTITY_TYPE,
                ResourceLocation.fromNamespaceAndPath(BulletinBoardMod.MOD_ID, "placed_note"),
                () -> {
                    PLACED_NOTE_ENTITY = BlockEntityType.Builder.of(
                            PlacedNoteBlockEntity::new,
                            BulletinBoardMod.PLACED_NOTE
                    ).build(null);
                    return PLACED_NOTE_ENTITY;
                }
        );
    }
}