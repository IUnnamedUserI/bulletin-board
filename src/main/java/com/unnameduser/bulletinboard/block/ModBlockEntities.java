package com.unnameduser.bulletinboard.block;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static BlockEntityType<BulletinBoardBlockEntity> BULLETIN_BOARD_ENTITY;

    public static void register() {
        BULLETIN_BOARD_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(BulletinBoardMod.MOD_ID, "bulletin_board"),
                FabricBlockEntityTypeBuilder.create(
                        BulletinBoardBlockEntity::new,
                        BulletinBoardMod.BULLETIN_BOARD
                ).build()
        );
    }
}