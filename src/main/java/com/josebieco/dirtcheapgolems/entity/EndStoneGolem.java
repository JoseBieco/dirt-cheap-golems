package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import com.josebieco.dirtcheapgolems.entity.ai.CollectItemsGoal;
import com.josebieco.dirtcheapgolems.entity.ai.HoveringStrollGoal;
import com.josebieco.dirtcheapgolems.item.GolemCoreItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * "Logistico" (spec v2 section 6.1): a flying golem that can be linked to a container.
 * Item collection is added on top of this class by the collection goal.
 */
public class EndStoneGolem extends AbstractCheapGolem {
    public enum LinkResult { LINKED, TOO_FAR, OTHER_DIMENSION, NO_INVENTORY }

    private static final String TAG_LINKED_CONTAINER = "LinkedContainer";
    private static final String TAG_ITEMS_CARRIED = "ItemsCarried";

    /** Flying speed attribute; the configured fly speed is the goal speed modifier on top of it. */
    private static final double BASE_FLYING_SPEED = 0.25;

    private @Nullable GlobalPos linkedContainer;
    private int itemsCarried;
    private boolean collecting;

    public EndStoneGolem(EntityType<? extends EndStoneGolem> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseAttributes().add(Attributes.FLYING_SPEED, BASE_FLYING_SPEED);
    }

    // ---- flight (same recipe as the Allay) ----

    @Override
    protected PathNavigation createNavigation(Level level) {
        var navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    public void travel(Vec3 input) {
        this.travelFlying(input, this.getSpeed());
    }

    /** The golem never falls, even if something resets the no-gravity flag. */
    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
    }

    @Override
    protected void registerGoals() {
        double speed = GolemConfig.endStoneFlySpeed();
        this.goalSelector.addGoal(4, new CollectItemsGoal(this, speed));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, speed));
        // The flying stroll respects the home restriction (HoverRandomPos / AirAndWaterRandomPos).
        this.goalSelector.addGoal(6, new HoveringStrollGoal(this, speed));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && !collecting) hoverNudge();
        if (this.level().isClientSide() && this.getRandom().nextInt(10) == 0)
            this.level().addParticle(ParticleTypes.PORTAL, this.getRandomX(0.5), this.getY(0.5), this.getRandomZ(0.5),
                0.0, 0.0, 0.0);
    }

    // ---- idle hover (spec 6.1: 0.5 to 1.5 blocks above the ground) ----

    public static final double HOVER_MIN = 0.5;
    public static final double HOVER_MAX = 1.5;
    private static final int GROUND_SCAN_DEPTH = 8;

    /** Set by the collect goal: while flying to an item the hover band is not enforced. */
    public void setCollecting(boolean collecting) {
        this.collecting = collecting;
    }

    /** Height of the top of the first solid block at or below (x, y, z), or NaN if none within 8 blocks. */
    public double groundHeightAt(double x, double y, double z) {
        var cursor = new BlockPos.MutableBlockPos();
        int bx = net.minecraft.util.Mth.floor(x);
        int bz = net.minecraft.util.Mth.floor(z);
        int by = net.minecraft.util.Mth.floor(y);
        for (int i = 0; i <= GROUND_SCAN_DEPTH; i++) {
            cursor.set(bx, by - i, bz);
            var shape = this.level().getBlockState(cursor).getCollisionShape(this.level(), cursor);
            if (!shape.isEmpty()) return cursor.getY() + shape.max(net.minecraft.core.Direction.Axis.Y);
        }
        return Double.NaN;
    }

    /** Gently pushes the golem back into the hover band when it is idle. */
    private void hoverNudge() {
        double ground = groundHeightAt(this.getX(), this.getY(), this.getZ());
        if (Double.isNaN(ground)) return;
        double height = this.getY() - ground;
        Vec3 motion = this.getDeltaMovement();
        if (height < HOVER_MIN + 0.1) this.setDeltaMovement(motion.x, Math.max(motion.y, 0.08), motion.z);
        else if (height > HOVER_MAX - 0.1) this.setDeltaMovement(motion.x, Math.min(motion.y, -0.08), motion.z);
    }

    // ---- container link ----

    public @Nullable GlobalPos getLinkedContainer() {
        return linkedContainer;
    }

    public void clearLink() {
        this.linkedContainer = null;
    }

    public int getItemsCarried() {
        return itemsCarried;
    }

    public void setItemsCarried(int itemsCarried) {
        this.itemsCarried = itemsCarried;
    }

    /** Stores the link if valid; on any refusal the previous link is kept. */
    public LinkResult tryLink(GlobalPos target) {
        if (!target.dimension().equals(this.level().dimension())) return LinkResult.OTHER_DIMENSION;
        BlockPos home = getHomePos() != null ? getHomePos() : this.blockPosition();
        double range = GolemConfig.endStoneLinkRange();
        if (target.pos().distSqr(home) > range * range) return LinkResult.TOO_FAR;
        if (!this.level().isLoaded(target.pos()) || !GolemCoreItem.hasInventory(this.level(), target.pos()))
            return LinkResult.NO_INVENTORY;
        this.linkedContainer = target;
        return LinkResult.LINKED;
    }

    @Override
    public InteractionResult receiveCommand(Player player, ItemStack core) {
        var link = GolemCoreItem.getLink(core);
        if (link == null) return InteractionResult.PASS;
        var result = tryLink(link);
        player.sendOverlayMessage(Component.translatable(
            "message.dirtcheapgolems.end_stone." + result.name().toLowerCase(Locale.ROOT)));
        if (result == LinkResult.LINKED && this.level() instanceof ServerLevel level)
            portalTrail(level, link.pos());
        return InteractionResult.SUCCESS;
    }

    /** Portal particles along the line between the golem and the container. */
    private void portalTrail(ServerLevel level, BlockPos container) {
        Vec3 from = this.position().add(0.0, 0.45, 0.0);
        Vec3 to = Vec3.atCenterOf(container);
        for (int i = 0; i <= 10; i++) {
            Vec3 p = from.lerp(to, i / 10.0);
            level.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 3, 0.1, 0.1, 0.1, 0.1);
        }
    }

    // ---- crumbling ----

    @Override
    protected void playCrumbleEffects(ServerLevel level) {
        level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 30, 0.25, 0.3, 0.25, 0.3);
        level.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.4F, 1.4F);
    }

    @Override
    protected BlockState getMaterialState() {
        return Blocks.END_STONE.defaultBlockState();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(Items.END_STONE);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.endStoneDropChance();
    }

    // ---- persistence ----

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (linkedContainer != null) output.store(TAG_LINKED_CONTAINER, GlobalPos.CODEC, linkedContainer);
        output.putInt(TAG_ITEMS_CARRIED, itemsCarried);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        linkedContainer = input.read(TAG_LINKED_CONTAINER, GlobalPos.CODEC).orElse(null);
        itemsCarried = input.getIntOr(TAG_ITEMS_CARRIED, 0);
    }
}
