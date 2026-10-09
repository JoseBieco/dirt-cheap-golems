package com.josebieco.dirtcheapgolems.datagen;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import com.josebieco.dirtcheapgolems.registry.ModEffects;
import com.josebieco.dirtcheapgolems.registry.ModEntities;
import com.josebieco.dirtcheapgolems.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/** en_us and pt_br translations. Every key must exist in both. */
public final class ModLanguageProvider extends LanguageProvider {
    private final boolean portuguese;

    private ModLanguageProvider(PackOutput output, String locale) {
        super(output, DirtCheapGolems.MOD_ID, locale);
        this.portuguese = locale.equals("pt_br");
    }

    public static ModLanguageProvider english(PackOutput output) {
        return new ModLanguageProvider(output, "en_us");
    }

    public static ModLanguageProvider portuguese(PackOutput output) {
        return new ModLanguageProvider(output, "pt_br");
    }

    private String t(String english, String portuguese) {
        return this.portuguese ? portuguese : english;
    }

    @Override
    protected void addTranslations() {
        addItem(ModItems.GOLEM_CORE, t("Golem Core", "Núcleo de Golem"));
        addEntityType(ModEntities.DIRT_GOLEM, t("Dirt Golem", "Golem de Terra"));
        addItem(ModItems.DIRT_GOLEM_SPAWN_EGG, t("Dirt Golem Spawn Egg", "Ovo Gerador de Golem de Terra"));
        addEntityType(ModEntities.SAND_GOLEM, t("Sand Golem", "Golem de Areia"));
        addItem(ModItems.SAND_GOLEM_SPAWN_EGG, t("Sand Golem Spawn Egg", "Ovo Gerador de Golem de Areia"));
        addEntityType(ModEntities.NETHERRACK_GOLEM, t("Netherrack Golem", "Golem de Netherrack"));
        addItem(ModItems.NETHERRACK_GOLEM_SPAWN_EGG, t("Netherrack Golem Spawn Egg", "Ovo Gerador de Golem de Netherrack"));
        addEntityType(ModEntities.END_STONE_GOLEM, t("End Stone Golem", "Golem de End Stone"));
        addItem(ModItems.END_STONE_GOLEM_SPAWN_EGG, t("End Stone Golem Spawn Egg", "Ovo Gerador de Golem de End Stone"));
        add("message.dirtcheapgolems.end_stone.linked", t("Linked", "Vinculado"));
        add("message.dirtcheapgolems.end_stone.too_far", t("Too far from home", "Muito longe da casa"));
        add("message.dirtcheapgolems.end_stone.other_dimension", t("Another dimension", "Outra dimensão"));
        add("message.dirtcheapgolems.end_stone.no_inventory", t("No inventory", "Sem inventário"));
        add("message.dirtcheapgolems.core.linked", t("Linked to %s", "Vinculado a %s"));
        add("message.dirtcheapgolems.core.unlinked", t("Link cleared", "V\u00ednculo removido"));
        add("tooltip.dirtcheapgolems.core.linked", t("Linked to: %s, %s, %s (%s)", "Vinculado a: %s, %s, %s (%s)"));
        addEntityType(ModEntities.GRAVEL_GOLEM, t("Gravel Golem", "Golem de Cascalho"));
        addItem(ModItems.GRAVEL_GOLEM_SPAWN_EGG, t("Gravel Golem Spawn Egg", "Ovo Gerador de Golem de Cascalho"));
        addEntityType(ModEntities.FLINT_SHARD, t("Flint Shard", "Estilha\u00e7o de S\u00edlex"));
        addEntityType(ModEntities.SAND_BLAST, t("Sand Blast", "Rajada de Areia"));
        addEffect(ModEffects.SAND_IN_EYES, t("Sand in Eyes", "Areia nos Olhos"));
    }
}
