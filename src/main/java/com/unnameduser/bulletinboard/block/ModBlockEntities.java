package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegisterEvent;

public class ModBlockEntities {
    public static BlockEntityType<BulletinBoardBlockEntity> BULLETIN_BOARD_ENTITY;
    public static BlockEntityType<PlacedNoteBlockEntity> PLACED_NOTE_ENTITY;

    /**
     * Регистрация выполняется через Forge-метод {@code RegisterEvent#register}:
     * во время рассылки события реестр заблокирован, и прямой {@code Registry.register}
     * приводит к ошибке «Can not register to a locked registry».
     */
    public static void register(RegisterEvent event) {
        event.register(
                Registries.BLOCK_ENTITY_TYPE,
                new ResourceLocation(BulletinBoardMod.MOD_ID, "bulletin_board"),
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
                new ResourceLocation(BulletinBoardMod.MOD_ID, "placed_note"),
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
