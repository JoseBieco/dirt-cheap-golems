package com.josebieco.dirtcheapgolems.entity;

import com.josebieco.dirtcheapgolems.config.GolemConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** "Kamikaze Térmico" (spec §6.3): a walking light that blows itself up against hostiles. */
public class NetherrackGolem extends AbstractCheapGolem {
    private final WalkingLight walkingLight = new WalkingLight();
    /** Light saved in NBT by a previous life of this golem; removed before any new light is placed. */
    private @Nullable BlockPos staleLight;

    public NetherrackGolem(EntityType<? extends NetherrackGolem> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) return;
        if (staleLight != null) {
            WalkingLight.removeLightAt(level, staleLight);
            staleLight = null;
        }
        walkingLight.update(level, this.blockPosition().above(), GolemConfig.netherrackLightLevel());
    }

    /** Every removal path (death, discard, chunk unload, dimension change) takes the light away. */
    @Override
    public void onRemoval(RemovalReason reason) {
        super.onRemoval(reason);
        if (!this.level().isClientSide()) walkingLight.clear(this.level());
    }

    /** Crumbles into ashes: smoke instead of material particles, drops charcoal. */
    @Override
    protected void crumble() {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) return;
        walkingLight.clear(level);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 20, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.6F, 1.2F);
        if (this.getRandom().nextDouble() < getCrumbleDropChance())
            this.spawnAtLocation(level, getCrumbleDrop());
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        walkingLight.save(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        staleLight = WalkingLight.load(input);
    }

    @Override
    protected BlockState getMaterialState() {
        return Blocks.NETHERRACK.defaultBlockState();
    }

    @Override
    protected ItemStack getCrumbleDrop() {
        return new ItemStack(Items.CHARCOAL);
    }

    @Override
    protected double getCrumbleDropChance() {
        return GolemConfig.netherrackDropChance();
    }
}
