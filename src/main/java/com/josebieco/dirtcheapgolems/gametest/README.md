# GameTests (Forge 66.0.9 / Minecraft 26.3)

Since 1.21.5 vanilla splits a game test into two registry entries:

- a **test function** (`Registries.TEST_FUNCTION`, static registry) — the code, a `Consumer<GameTestHelper>`;
- a **test instance** (`Registries.TEST_INSTANCE`, datapack registry) — JSON in
  `data/<ns>/test_instance/<path>.json` pointing at the function, structure, max ticks, etc.

Forge 66 provides the `@GameTest` annotation (`net.minecraftforge.gametest`) and
`ForgeGameTestHooks.gatherTests(Class, instance)` but does **not** register them automatically.
This mod glues them together:

1. Write a class with `@GameTestNamespace("dirtcheapgolems")` + `@GameTestPrefix("<group>")` and
   `public static void someTest(GameTestHelper helper)` methods annotated with
   `@GameTest(structure = Arena.STRUCTURE, maxTicks = ...)`. Test id = `dirtcheapgolems:<group>/some_test`.
2. Add the class to `ModGameTests.TEST_CLASSES`. `ModGameTests.register` registers each method
   as a test function through a `DeferredRegister`.
3. Run `gradlew runData`: `datagen/GameTestInstanceProvider` writes one test instance JSON per method
   into `src/generated/resources/data/dirtcheapgolems/test_instance/`. **Re-run datagen whenever a
   test is added, renamed or its annotation changes.**
4. `gradlew runGameTestServer` runs every registered test instance and exits non-zero on failure.

Arena: `Arena.STRUCTURE` = `forge:empty9x5x9` — Forge generates `forge:emptyXxYxZ` structures (all air)
on the fly. The arena has no floor of its own; call `helper.makeFloor()` (stone at relative y=0)
when the test needs solid ground, and place things at y=1.
