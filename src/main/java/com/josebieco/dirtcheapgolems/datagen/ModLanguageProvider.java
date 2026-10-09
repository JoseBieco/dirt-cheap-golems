package com.josebieco.dirtcheapgolems.datagen;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
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
    }
}
