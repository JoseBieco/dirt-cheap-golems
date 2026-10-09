package com.josebieco.dirtcheapgolems.gametest;

import com.josebieco.dirtcheapgolems.DirtCheapGolems;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.gametest.ForgeGameTestHooks;
import net.minecraftforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Registers every {@code @GameTest} of this mod as a vanilla test function.
 * The matching test instances (data/dirtcheapgolems/test_instance/*.json) are produced by datagen,
 * see {@link com.josebieco.dirtcheapgolems.datagen.GameTestInstanceProvider}. See README.md in this package.
 */
public final class ModGameTests {
    /** Every class holding {@code @GameTest} methods. Add new test classes here. */
    public static final List<Class<?>> TEST_CLASSES = List.of(
        SmokeTests.class,
        CoreGolemTests.class,
        ActivationTests.class
    );

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, DirtCheapGolems.MOD_ID);

    private ModGameTests() {}

    /** All tests of the mod, sorted by id. */
    public static Map<Identifier, ForgeGameTestHooks.TestReference> gather() {
        var tests = new TreeMap<Identifier, ForgeGameTestHooks.TestReference>();
        for (var cls : TEST_CLASSES)
            tests.putAll(ForgeGameTestHooks.gatherTests(cls, null));
        return tests;
    }

    public static void register(BusGroup modBusGroup) {
        gather().forEach((id, ref) -> FUNCTIONS.register(id.getPath(), ref::consumer));
        FUNCTIONS.register(modBusGroup);
    }
}
