package com.josebieco.dirtcheapgolems.datagen;

import net.minecraftforge.data.event.GatherDataEvent;

public final class DataGenerators {
    private DataGenerators() {}

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();

        generator.addProvider(event.includeServer(), new GameTestInstanceProvider(output));
        generator.addProvider(event.includeClient(), ModLanguageProvider.english(output));
        generator.addProvider(event.includeClient(), ModLanguageProvider.portuguese(output));
    }
}
