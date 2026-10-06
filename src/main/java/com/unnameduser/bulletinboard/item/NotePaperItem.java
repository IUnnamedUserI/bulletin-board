package com.unnameduser.bulletinboard.item;

import com.unnameduser.bulletinboard.BulletinBoardMod;
import com.unnameduser.bulletinboard.block.PlacedNoteBlock;
import com.unnameduser.bulletinboard.block.PlacedNoteBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.unnameduser.bulletinboard.block.BulletinBoardBlock;
import com.unnameduser.bulletinboard.screen.NoteEditorScreen;
import com.unnameduser.bulletinboard.screen.NoteViewScreen;
import com.unnameduser.bulletinboard.util.NoteData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NotePaperItem extends Item {
    private final boolean isSmall;

    public NotePaperItem(Item.Properties settings, boolean isSmall) {
        super(settings);
        this.isSmall = isSmall;
    }

    public boolean isSmall() {
        return isSmall;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        // Если записка уже подписана (есть NoteData) и игрок зажимает Shift — кладём на блок
        if (user.isShiftKeyDown() && hasNoteData(stack)) {
            HitResult hit = user.pick(5.0, 0.0f, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;

                // Проверяем, что клик по верхней грани
                if (blockHit.getDirection() == Direction.UP) {
                    BlockPos belowPos = blockHit.getBlockPos();
                    BlockPos placePos = belowPos.above();

                    // Проверяем, можно ли поставить записку
                    if (world.getBlockState(placePos).isAir()
                            && BulletinBoardMod.PLACED_NOTE.defaultBlockState()
                            .canSurvive(world, placePos)) {

                        if (!world.isClientSide) {
                            // Ставим блок
                            world.setBlockAndUpdate(placePos,
                                    BulletinBoardMod.PLACED_NOTE.defaultBlockState()
                                            .setValue(PlacedNoteBlock.ROTATION, getRotation(user)));

                            // Сохраняем данные записки в BlockEntity
                            BlockEntity blockEntity = world.getBlockEntity(placePos);
                            if (blockEntity instanceof PlacedNoteBlockEntity noteEntity) {
                                NoteData note = NoteData.fromNbt(stack.getTag().getCompound("NoteData"));
                                noteEntity.setNoteData(note);
                            }

                            // Уменьшаем стак
                            stack.shrink(1);
                        }

                        user.swing(hand);
                        return InteractionResultHolder.success(stack);
                    }
                }
            }
        }

        // Обычное поведение (открытие редактора/просмотра)
        HitResult hit = user.pick(5.0, 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            if (world.getBlockState(pos).getBlock() instanceof BulletinBoardBlock) {
                return InteractionResultHolder.pass(stack);
            }
        }

        if (world.isClientSide) {
            openScreen(stack, hasNoteData(stack));
        }

        return InteractionResultHolder.success(stack);
    }

    private int getRotation(Player player) {
        float yaw = player.getYRot();
        yaw = ((yaw % 360) + 360) % 360;
        // Инвертируем yaw: 360 - yaw
        float invertedYaw = 360 - yaw;
        return Math.round(invertedYaw / 45.0f) & 7;
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreen(ItemStack stack, boolean hasNote) {
        if (hasNote) {
            NoteData note = NoteData.fromNbt(stack.getTag().getCompound("NoteData"));
            Minecraft.getInstance().setScreen(new NoteViewScreen(note));
        } else {
            // Передаём тип записки через NBT
            if (!stack.hasTag()) {
                stack.setTag(new net.minecraft.nbt.CompoundTag());
            }
            stack.getTag().putBoolean("IsSmall", isSmall);
            Minecraft.getInstance().setScreen(new NoteEditorScreen(stack));
        }
    }

    private boolean hasNoteData(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("NoteData");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
        if (hasNoteData(stack)) {
            NoteData note = NoteData.fromNbt(stack.getTag().getCompound("NoteData"));

            // Используем переведённый заголовок
            String translatedTitle = note.getTranslatedTitle();
            tooltip.add(Component.literal("§6" + translatedTitle).withStyle(ChatFormatting.GOLD));

            // Автор уже переводится
            tooltip.add(Component.translatable("item.bulletin-board.note_paper.tooltip.author",
                    Component.translatable(note.getAuthor())).withStyle(ChatFormatting.GRAY));

            if (note.getTagColor() != -1) {
                String badgeId = getBadgeIdByColor(note.getTagColor());
                if (badgeId != null) {
                    ChatFormatting colorFormatting = getFormattingFromColor(note.getTagColor());
                    tooltip.add(Component.translatable("item.bulletin-board." + badgeId).withStyle(colorFormatting));
                }
            }
        } else {
            tooltip.add(Component.translatable("item.bulletin-board.note_paper.tooltip.empty")
                    .withStyle(ChatFormatting.GRAY));
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

    private ChatFormatting getFormattingFromColor(int color) {
        return switch (color) {
            case 0x000000 -> ChatFormatting.BLACK;
            case 0xFF5555 -> ChatFormatting.RED;
            case 0x55FF55 -> ChatFormatting.GREEN;
            case 0x8B4513 -> ChatFormatting.GOLD;
            case 0x5555FF -> ChatFormatting.BLUE;
            case 0xAA00AA -> ChatFormatting.DARK_PURPLE;
            case 0x00AAAA -> ChatFormatting.AQUA;
            case 0xAAAAAA -> ChatFormatting.GRAY;
            case 0x555555 -> ChatFormatting.DARK_GRAY;
            case 0xFFAAFF -> ChatFormatting.LIGHT_PURPLE;
            case 0xAAFF55 -> ChatFormatting.GREEN;
            case 0xFFFF55 -> ChatFormatting.YELLOW;
            case 0x55FFFF -> ChatFormatting.AQUA;
            case 0xFF55FF -> ChatFormatting.LIGHT_PURPLE;
            case 0xFFAA00 -> ChatFormatting.GOLD;
            case 0xFFFFFF -> ChatFormatting.WHITE;
            default -> ChatFormatting.WHITE;
        };
    }
}
