package com.josebieco.dirtcheapgolems.registry;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    private ModTags() {}

    /** Golem material tags (spec §5.3). Modpacks may change them via datapack. */
    public static final class Blocks {
        public static final TagKey<Block> MATERIAL_DIRT = material("dirt");
        public static final TagKey<Block> MATERIAL_SAND = material("sand");
        public static final TagKey<Block> MATERIAL_NETHERRACK = material("netherrack");
        public static final TagKey<Block> MATERIAL_END_STONE = material("end_stone");
        public static final TagKey<Block> MATERIAL_GRAVEL = material("gravel");

        private Blocks() {}

        private static TagKey<Block> material(String name) {
            return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(DirtCheapGolems.MOD_ID, "materials/" + name));
        }
    }
}
