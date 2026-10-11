package com.josebieco.dirtcheapgolems.entity.ai;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.EndStoneGolem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

/**
 * End Stone Golem job (spec 6.1): every second, fly to the nearest dropped item near home and teleport it into the
 * linked container. A vanished container clears the link; an unloaded chunk keeps it (golem idles).
 */
public class CollectItemsGoal extends Goal {
    private static final int SCAN_INTERVAL = 20;
    private static final int GIVE_UP_TICKS = 200;

    private final EndStoneGolem golem;
    private final double speed;
    private int scanCooldown;
    private @Nullable ItemEntity target;
    private int ticksRunning;

    public CollectItemsGoal(EndStoneGolem golem, double speed) {
        this.golem = golem;
        this.speed = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--scanCooldown > 0) return false;
        scanCooldown = SCAN_INTERVAL;
        var link = golem.getLinkedContainer();
        if (link == null || !(golem.level() instanceof ServerLevel level)) return false;
        if (!link.dimension().equals(level.dimension())) return false;   // linked in another dimension: idle, keep the link
        if (!level.isLoaded(link.pos())) return false;
        var handler = handlerAt(level, link.pos());
        if (handler == null) {
            golem.clearLink();
            level.sendParticles(ParticleTypes.SMOKE, golem.getX(), golem.getY(0.5), golem.getZ(), 12, 0.25, 0.3, 0.25, 0.02);
            return false;
        }
        target = findTarget(level, handler);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && !target.hasPickUpDelay() && ticksRunning < GIVE_UP_TICKS
            && golem.getLinkedContainer() != null
            && golem.getLinkedContainer().dimension().equals(golem.level().dimension());
    }

    @Override
    public void start() {
        ticksRunning = 0;
        golem.setCollecting(true);
        moveToTarget();
    }

    @Override
    public void stop() {
        target = null;
        golem.setCollecting(false);
        golem.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null || !(golem.level() instanceof ServerLevel level)) return;
        ticksRunning++;
        golem.getLookControl().setLookAt(target);
        if (golem.position().closerThan(target.position(), GolemConfig.endStonePickupRange())) {
            teleport(level, target);
            target = null;
        } else if (ticksRunning % 10 == 0 || golem.getNavigation().isDone()) {
            moveToTarget();
        }
    }

    private void moveToTarget() {
        if (target != null) golem.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), speed);
    }

    private @Nullable ItemEntity findTarget(ServerLevel level, IItemHandler handler) {
        BlockPos center = golem.getHomePos() != null ? golem.getHomePos() : golem.blockPosition();
        double radius = golem.effectiveHomeRadius();
        AABB area = new AABB(center).inflate(radius);
        return level.getEntitiesOfClass(ItemEntity.class, area, item ->
                item.isAlive() && !item.hasPickUpDelay() && item.distanceToSqr(Vec3.atCenterOf(center)) <= radius * radius
                    && insert(handler, item.getItem(), true).getCount() < item.getItem().getCount())
            .stream().min(Comparator.comparingDouble(golem::distanceToSqr)).orElse(null);
    }

    private void teleport(ServerLevel level, ItemEntity item) {
        var link = golem.getLinkedContainer();
        if (link == null || !link.dimension().equals(level.dimension())) return;
        var handler = handlerAt(level, link.pos());
        if (handler == null) return;
        ItemStack stack = item.getItem();
        ItemStack remaining = insert(handler, stack, false);
        int inserted = stack.getCount() - remaining.getCount();
        if (inserted <= 0) return;
        Vec3 from = item.position();
        Vec3 to = Vec3.atCenterOf(link.pos());
        if (remaining.isEmpty()) item.discard();
        else item.setItem(remaining);
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 0.2, from.z, 12, 0.2, 0.2, 0.2, 0.2);
        level.sendParticles(ParticleTypes.PORTAL, to.x, to.y, to.z, 12, 0.3, 0.3, 0.3, 0.2);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.3F, 1.2F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.3F, 1.2F);

        int perIntegrity = GolemConfig.endStoneItemsPerIntegrity();
        int carried = golem.getItemsCarried() + inserted;
        int cost = 0;
        while (carried >= perIntegrity) {
            carried -= perIntegrity;
            cost++;
        }
        golem.setItemsCarried(carried);
        if (cost > 0) golem.consumeIntegrity(cost);
    }

    private static @Nullable IItemHandler handlerAt(ServerLevel level, BlockPos pos) {
        var blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return null;
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    /** Inserts into every slot of the handler (a double chest capability is already the combined inventory). */
    static ItemStack insert(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++)
            remaining = handler.insertItem(slot, remaining, simulate);
        return remaining;
    }
}
