package com.josebieco.dirtcheapgolems.datagen;

import com.google.gson.JsonObject;
import com.josebieco.dirtcheapgolems.gametest.ModGameTests;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/** Writes one function test instance per {@code @GameTest} method of the mod. */
public final class GameTestInstanceProvider implements DataProvider {
    private final PackOutput.PathProvider paths;

    public GameTestInstanceProvider(PackOutput output) {
        this.paths = output.createRegistryElementsPathProvider(Registries.TEST_INSTANCE);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var futures = new ArrayList<CompletableFuture<?>>();
        ModGameTests.gather().forEach((id, ref) -> {
            var data = ref.data();
            var json = new JsonObject();
            json.addProperty("type", "minecraft:function");
            json.addProperty("function", id.toString());
            json.addProperty("environment", data.environment().toString());
            json.addProperty("dimension", data.dimension().identifier().toString());
            json.addProperty("structure", data.structure().toString());
            json.addProperty("max_ticks", data.maxTicks());
            json.addProperty("setup_ticks", data.setupTicks());
            json.addProperty("required", data.required());
            json.addProperty("rotation", data.rotation().getSerializedName());
            json.addProperty("manual_only", data.manualOnly());
            json.addProperty("max_attempts", data.maxAttempts());
            json.addProperty("required_successes", data.requiredSuccesses());
            json.addProperty("sky_access", data.skyAccess());
            json.addProperty("padding", data.padding());
            futures.add(DataProvider.saveStable(cache, json, paths.json(id)));
        });
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Dirt Cheap Golems game test instances";
    }
}
