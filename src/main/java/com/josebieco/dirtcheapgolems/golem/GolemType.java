package com.josebieco.dirtcheapgolems.golem;

import com.josebieco.dirtcheapgolems.entity.AbstractCheapGolem;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import com.josebieco.dirtcheapgolems.registry.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

/** Maps a material tag to the golem built from it. */
public enum GolemType {
    DIRT(ModTags.Blocks.MATERIAL_DIRT, () -> ModEntities.DIRT_GOLEM.get()),
    SAND(ModTags.Blocks.MATERIAL_SAND, () -> ModEntities.SAND_GOLEM.get()),
    NETHERRACK(ModTags.Blocks.MATERIAL_NETHERRACK, () -> ModEntities.NETHERRACK_GOLEM.get()),
    END_STONE(ModTags.Blocks.MATERIAL_END_STONE, () -> ModEntities.END_STONE_GOLEM.get()),
    GRAVEL(ModTags.Blocks.MATERIAL_GRAVEL, () -> ModEntities.GRAVEL_GOLEM.get());

    private final TagKey<Block> material;
    private final Supplier<? extends @Nullable EntityType<? extends AbstractCheapGolem>> entityType;

    GolemType(TagKey<Block> material, Supplier<? extends @Nullable EntityType<? extends AbstractCheapGolem>> entityType) {
        this.material = material;
        this.entityType = entityType;
    }

    public TagKey<Block> material() {
        return material;
    }

    public @Nullable EntityType<? extends AbstractCheapGolem> entityType() {
        return entityType.get();
    }

    /** The golem type whose material tag contains both blocks; types without an entity yet are ignored. */
    public static Optional<GolemType> fromPillar(BlockState top, BlockState bottom) {
        for (var type : values()) {
            if (type.entityType() != null && top.is(type.material) && bottom.is(type.material))
                return Optional.of(type);
        }
        return Optional.empty();
    }
}
