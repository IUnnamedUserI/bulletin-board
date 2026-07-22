package com.unnameduser.bulletinboard.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.screen.NoteEditorScreen;
import com.unnameduser.bulletinboard.screen.NoteViewScreen;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;

public class NotePaperItem extends Item {
    private final boolean isSmall;

    public NotePaperItem(Settings settings, boolean isSmall) {
        super(settings);
        this.isSmall = isSmall;
    }

    public boolean isSmall() {
        return isSmall;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        HitResult hit = user.raycast(5.0, 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            if (world.getBlockState(pos).getBlock() instanceof BulletinBoardBlock) {
                return TypedActionResult.pass(stack);
            }
        }

        if (world.isClient) {
            openScreen(stack, hasNoteData(stack));
        }

        return TypedActionResult.success(stack);
    }

    @Environment(EnvType.CLIENT)
    private void openScreen(ItemStack stack, boolean hasNote) {
        if (hasNote) {
            // Получаем NbtComponent
            NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (nbtComponent != null) {
                // Копируем в NbtCompound для удобной работы
                NbtCompound nbt = nbtComponent.copyNbt();
                if (nbt.contains(BulletinBoardMod.NOTE_DATA_NBT_KEY)) {
                    NbtCompound noteNbt = nbt.getCompound(BulletinBoardMod.NOTE_DATA_NBT_KEY);
                    NoteData note = NoteData.fromNbt(noteNbt);
                    MinecraftClient.getInstance().setScreen(new NoteViewScreen(note));
                    return;
                }
            }
        }
        // Если нет данных - открываем редактор
        MinecraftClient.getInstance().setScreen(new NoteEditorScreen(stack));
    }

    private boolean hasNoteData(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null) return false;
        NbtCompound nbt = nbtComponent.copyNbt();
        return nbt.contains(BulletinBoardMod.NOTE_DATA_NBT_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        NoteData note = null;

        if (nbtComponent != null) {
            NbtCompound nbt = nbtComponent.copyNbt();
            if (nbt.contains(BulletinBoardMod.NOTE_DATA_NBT_KEY)) {
                NbtCompound noteNbt = nbt.getCompound(BulletinBoardMod.NOTE_DATA_NBT_KEY);
                note = NoteData.fromNbt(noteNbt);
            }
        }

        if (note != null) {
            tooltip.add(Text.literal("§6" + note.getTitle()).formatted(Formatting.GOLD));
            tooltip.add(Text.translatable("item.bulletin-board.note_paper.tooltip.author",
                    note.getAuthor()).formatted(Formatting.GRAY));

            if (note.getTagColor() != -1) {
                String badgeId = getBadgeIdByColor(note.getTagColor());
                if (badgeId != null) {
                    Formatting colorFormatting = getFormattingFromColor(note.getTagColor());
                    tooltip.add(Text.translatable("item.bulletin-board." + badgeId).formatted(colorFormatting));
                }
            }
        } else {
            tooltip.add(Text.translatable("item.bulletin-board.note_paper.tooltip.empty")
                    .formatted(Formatting.GRAY));
        }
    }

    private String getBadgeIdByColor(int color) {
        return switch (color) {
            case 0x000000 -> "black_badge";
            case 0xFF5555 -> "red_badge";
            case 0x55FF55 -> "green_badge";
            case 0x8B4513 -> "brown_badge";
            case 0x5555FF -> "blue_badge";
            case 0xAA00AA -> "purple_badge";
            case 0x00AAAA -> "cyan_badge";
            case 0xAAAAAA -> "light_gray_badge";
            case 0x555555 -> "gray_badge";
            case 0xFFAAFF -> "pink_badge";
            case 0xAAFF55 -> "lime_badge";
            case 0xFFFF55 -> "yellow_badge";
            case 0x55FFFF -> "light_blue_badge";
            case 0xFF55FF -> "magenta_badge";
            case 0xFFAA00 -> "orange_badge";
            case 0xFFFFFF -> "white_badge";
            default -> null;
        };
    }

    private Formatting getFormattingFromColor(int color) {
        return switch (color) {
            case 0x000000 -> Formatting.BLACK;
            case 0xFF5555 -> Formatting.RED;
            case 0x55FF55 -> Formatting.GREEN;
            case 0x8B4513 -> Formatting.GOLD;
            case 0x5555FF -> Formatting.BLUE;
            case 0xAA00AA -> Formatting.DARK_PURPLE;
            case 0x00AAAA -> Formatting.AQUA;
            case 0xAAAAAA -> Formatting.GRAY;
            case 0x555555 -> Formatting.DARK_GRAY;
            case 0xFFAAFF -> Formatting.LIGHT_PURPLE;
            case 0xAAFF55 -> Formatting.GREEN;
            case 0xFFFF55 -> Formatting.YELLOW;
            case 0x55FFFF -> Formatting.AQUA;
            case 0xFF55FF -> Formatting.LIGHT_PURPLE;
            case 0xFFAA00 -> Formatting.GOLD;
            case 0xFFFFFF -> Formatting.WHITE;
            default -> Formatting.WHITE;
        };
    }
}