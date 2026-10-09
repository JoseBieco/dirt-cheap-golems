package com.josebieco.dirtcheapgolems.item;

import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import com.josebieco.dirtcheapgolems.golem.GolemActivation;
import com.josebieco.dirtcheapgolems.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Golem Core (spec §5.1): used on a material pillar to create a golem.
 * Sneaking turns it into a command tool (spec v2 §3.1): link a container, clear the link, command a golem.
 */
public class GolemCoreItem extends Item {
    public GolemCoreItem(Properties properties) {
        super(properties);
    }

    public static @Nullable GlobalPos getLink(ItemStack core) {
        return core.get(ModDataComponents.LINKED_CONTAINER.get());
    }

    /** Stores the link (and a glint); {@code null} removes both. */
    public static void setLink(ItemStack core, @Nullable GlobalPos pos) {
        if (pos == null) {
            core.remove(ModDataComponents.LINKED_CONTAINER.get());
            core.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        } else {
            core.set(ModDataComponents.LINKED_CONTAINER.get(), pos);
            core.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player.isShiftKeyDown()) {
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null || !blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent())
                return InteractionResult.PASS;
            if (!level.isClientSide()) {
                setLink(context.getItemInHand(), GlobalPos.of(level.dimension(), pos));
                player.sendOverlayMessage(Component.translatable("message.dirtcheapgolems.core.linked",
                    level.getBlockState(pos).getBlock().getName()));
            }
            return InteractionResult.SUCCESS;
        }
        return GolemActivation.tryActivate(level, pos, player, context.getItemInHand());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && getLink(stack) != null && isLookingAtAir(player)) {
            if (!level.isClientSide()) {
                setLink(stack, null);
                player.sendOverlayMessage(Component.translatable("message.dirtcheapgolems.core.unlinked"));
            }
            return InteractionResult.SUCCESS;
        }
        return super.use(level, player, hand);
    }

    /** Only a click on air clears the link; a pass-through block click (stone, pillar) must keep it. */
    private static boolean isLookingAtAir(Player player) {
        return player.pick(player.blockInteractionRange(), 1.0F, false).getType() == HitResult.Type.MISS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (player.isShiftKeyDown() && target instanceof AbstractCheapGolem golem) {
            if (player.level().isClientSide()) return InteractionResult.SUCCESS;
            return golem.receiveCommand(player, stack);
        }
        return InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        var link = getLink(stack);
        if (link != null)
            builder.accept(Component.translatable("tooltip.dirtcheapgolems.core.linked",
                link.pos().getX(), link.pos().getY(), link.pos().getZ(), link.dimension().identifier().toString())
                .withStyle(ChatFormatting.GRAY));
    }
}
