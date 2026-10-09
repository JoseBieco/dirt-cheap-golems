# Dirt Cheap Golems — MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar o MVP do mod Dirt Cheap Golems (Golems de Terra, Areia e Netherrack) para Minecraft 26.3 / Forge 66.0.9.

**Architecture:** Uma classe base `AbstractCheapGolem` (estende `AbstractGolem`) concentra integridade, casa, desmoronar e rachaduras; cada golem é uma subclasse que só adiciona metas de IA e gatilhos ambientais. Lógica pura (integridade, estágios) fica em classes sem dependência de mundo, testadas com JUnit; comportamento em mundo é testado com GameTests do Forge.

**Tech Stack:** Java (versão exigida pelo MDK), Gradle (ForgeGradle do MDK 66.0.9), Forge 66.0.9, JUnit 5, Forge GameTest.

**Spec:** `docs/specs/2026-10-09-dirt-cheap-golems-design.md` — leia antes de cada task. Seções citadas como "spec §X".

## Global Constraints

- Minecraft **26.3**, Forge **66.0.9**. Nomes oficiais da Mojang (26.x não é ofuscado).
- Mod id `dirtcheapgolems`; pacote raiz `com.josebieco.dirtcheapgolems`.
- **Antes de usar qualquer classe/evento/método do Forge ou do Minecraft, confirme nome e assinatura no MDK/fontes decompiladas do 26.3.** Este plano descreve intenção; se uma API citada não existir, use a equivalente atual e registre a troca na mensagem de commit.
- Toda lógica de jogo roda só no servidor (`!level.isClientSide()`); nenhuma classe de `client/` pode ser referenciada por código comum (servidor dedicado não pode crashar).
- Todo número de balanceamento vem de `GolemConfig` (spec §8), nunca hard-coded.
- Golems **nunca** miram/ferem jogadores, outros golems do mod, Iron/Snow Golems ou mobs passivos (spec §5.8).
- Integridade: inteiro 0–100, começa em 100, **nunca aumenta** (spec §5.4).
- Traduções completas em `en_us` e `pt_br`.
- Windows: onde estiver `./gradlew`, use `gradlew.bat`.

## Review Focus

1. **Jogador sem permissão de construir** (modo aventura, proteção de spawn) usando o núcleo → não deve ativar nem consumir. Teste em Task 4.
2. **Golem criado por ovo de spawn** (sem pilar) → casa = posição de spawn; não pode ficar sem casa. Teste em Task 3.
3. **Planta madura dentro do `harvestScanRadius` mas fora do `homeRadius`** → não deve ser colhida. Teste em Task 6.
4. **Dois Golems de Terra mirando a mesma planta** → só um colhe e só um perde integridade. Teste em Task 6.
5. **Chunk descarregado/recarregado e servidor reiniciado com Golem de Netherrack** → nenhum bloco de luz órfão. Teste em Task 10.

## Comandos

- Testes unitários: `./gradlew test`
- GameTests: `./gradlew runGameTestServer` (sai com código ≠ 0 se algum teste falhar)
- Cliente para verificação manual: `./gradlew runClient`
- Servidor dedicado: `./gradlew runServer`

> **Atenção GameTest:** o framework de GameTest mudou a partir do 1.21.5 (test instances/funções registradas). Na Task 1, descubra no MDK 66.0.9 como registrar um GameTest e um template de estrutura vazio, e documente o padrão em `src/main/java/.../gametest/README.md` para as tasks seguintes. Todos os GameTests deste plano vivem em `com.josebieco.dirtcheapgolems.gametest` e usam uma arena plana vazia (ex.: 9×5×9) salvo indicação.

---

### Task 1: Scaffold do projeto, config e infraestrutura de testes

**Files:**
- Create: projeto a partir do MDK Forge 66.0.9 (`build.gradle`, `settings.gradle`, `gradle.properties`, wrapper)
- Create: `src/main/java/com/josebieco/dirtcheapgolems/DirtCheapGolems.java`
- Create: `src/main/java/com/josebieco/dirtcheapgolems/config/GolemConfig.java`
- Create: `src/main/resources/META-INF/mods.toml` (ou o arquivo de metadados que o MDK usar)
- Create: `src/main/java/com/josebieco/dirtcheapgolems/gametest/SmokeTests.java`, `gametest/README.md`
- Create: `.gitignore`

**Interfaces:**
- Produces: `DirtCheapGolems.MOD_ID = "dirtcheapgolems"`; `GolemConfig.SPEC` (COMMON) com getters estáticos para **todas** as chaves da spec §8, ex.: `GolemConfig.passiveDecayTicks()`, `GolemConfig.dirtHarvestCost()`, `GolemConfig.sandTurretRange()`, `GolemConfig.netherrackBlastHurtsPlayers()`. Convenção de nome: `<golem><Chave>()`, `general` sem prefixo.

- [ ] **Step 1:** `git init`; baixar o MDK 66.0.9, configurar `mod_id=dirtcheapgolems`, `mod_group_id=com.josebieco.dirtcheapgolems`, nome "Dirt Cheap Golems", autor "José Bieco". Adicionar JUnit 5 ao `build.gradle` (`testImplementation`, `useJUnitPlatform()`).
- [ ] **Step 2:** Implementar `GolemConfig` com `defineInRange` para todas as chaves/padrões da spec §8 (ex.: `passiveDecayTicks` 600 em [20, 72000]; `dropChance` em [0.0, 1.0]; `lightLevel` em [0, 15]). Registrar como COMMON em `DirtCheapGolems`.
- [ ] **Step 3:** Escrever GameTest `SmokeTests.arenaLoads` que só chama `helper.succeed()`. Documentar o padrão de registro no `gametest/README.md`.
- [ ] **Step 4:** Rodar `./gradlew build` → BUILD SUCCESSFUL. Rodar `./gradlew runGameTestServer` → 1 teste, todos passam. Rodar `./gradlew runServer` uma vez e confirmar que `config/dirtcheapgolems-common.toml` é gerado com as chaves da spec §8.
- [ ] **Step 5:** Commit `chore: scaffold Forge 66 project with config and gametest setup`.

---

### Task 2: Lógica pura de integridade

**Files:**
- Create: `src/main/java/com/josebieco/dirtcheapgolems/entity/Integrity.java`
- Create: `src/main/java/com/josebieco/dirtcheapgolems/entity/CrackStage.java`
- Test: `src/test/java/com/josebieco/dirtcheapgolems/entity/IntegrityTest.java`

**Interfaces:**
- Produces:
  - `enum CrackStage { NONE, LOW, HIGH }` com `static CrackStage forIntegrity(int value)`.
  - `final class Integrity` — `static final int MAX = 100`; `Integrity()` (100); `Integrity(int value)` (clamp 0..100); `int value()`; `boolean isDepleted()`; `void consume(int amount)` (ignora amount ≤ 0; clamp em 0); `boolean tickPassive(int intervalTicks)` — conta ticks internamente, a cada `intervalTicks` consome 1 e retorna `true` naquele tick; `int ticksSinceDecay()` e `void setTicksSinceDecay(int)` para NBT.
  - Não há método que aumente o valor.

- [ ] **Step 1: Testes que falham**

```java
@Test void startsAtMax() { assertEquals(100, new Integrity().value()); }
@Test void consumeClampsAtZero() { var i = new Integrity(5); i.consume(34); assertEquals(0, i.value()); assertTrue(i.isDepleted()); }
@Test void consumeIgnoresNonPositive() { var i = new Integrity(); i.consume(-10); i.consume(0); assertEquals(100, i.value()); }
@Test void constructorClamps() { assertEquals(100, new Integrity(250).value()); assertEquals(0, new Integrity(-3).value()); }
@Test void passiveDecaysOncePerInterval() {
  var i = new Integrity(); int decays = 0;
  for (int t = 0; t < 1200; t++) if (i.tickPassive(600)) decays++;
  assertEquals(2, decays); assertEquals(98, i.value()); }
@Test void crackStages() {
  assertEquals(CrackStage.NONE, CrackStage.forIntegrity(100)); assertEquals(CrackStage.NONE, CrackStage.forIntegrity(67));
  assertEquals(CrackStage.LOW, CrackStage.forIntegrity(66)); assertEquals(CrackStage.LOW, CrackStage.forIntegrity(34));
  assertEquals(CrackStage.HIGH, CrackStage.forIntegrity(33)); assertEquals(CrackStage.HIGH, CrackStage.forIntegrity(1)); }
```

- [ ] **Step 2:** `./gradlew test` → FAIL (classes inexistentes).
- [ ] **Step 3:** Implementar `Integrity` e `CrackStage`.
- [ ] **Step 4:** `./gradlew test` → PASS.
- [ ] **Step 5:** Commit `feat: add integrity value logic and crack stages`.

---

### Task 3: `AbstractCheapGolem` + entidade `DirtGolem` (sem IA de colheita)

**Files:**
- Create: `entity/AbstractCheapGolem.java`, `entity/DirtGolem.java`
- Create: `registry/ModEntities.java`
- Modify: `DirtCheapGolems.java` (registrar `ModEntities`, atributos via evento de atributos)
- Test: `gametest/CoreGolemTests.java`

**Interfaces:**
- Consumes: `Integrity`, `CrackStage`, `GolemConfig`.
- Produces (`AbstractCheapGolem extends AbstractGolem`):
  - `int getIntegrity()`; `CrackStage getCrackStage()`; `void consumeIntegrity(int amount)` (se zerar, chama `crumble()` no mesmo tick).
  - `BlockPos getHomePos()`; `void setHomePos(BlockPos pos)` — também chama `restrictTo(pos, GolemConfig.homeRadius())`.
  - `protected void crumble()` — partículas/som do material, drop via `getCrumbleDrop()` com chance `getCrumbleDropChance()`, `discard()`. Subclasses podem sobrescrever.
  - `protected abstract BlockState getMaterialState()`; `protected abstract ItemStack getCrumbleDrop()`; `protected abstract double getCrumbleDropChance()`.
  - `protected void environmentTick()` — no-op na base; chamado a cada tick de servidor, antes da decadência passiva.
  - `static AttributeSupplier.Builder createBaseAttributes()` — vida 10, velocidade 0.25 (spec §5.8).
  - NBT: `Integrity` (int), `IntegrityTicks` (int), `HomePos` (BlockPos). Integridade sincronizada via `SynchedEntityData` (`DATA_INTEGRITY`).
  - Morte por vida 0 → `crumble()` (sem XP, sem loot table de morte).
  - Sem casa salva ao entrar no mundo (ex.: ovo de spawn) → casa = `blockPosition()` (em `finalizeSpawn` e fallback no primeiro tick).
  - Metas base: `RandomStrollGoal` restrito à casa, `MoveTowardsRestrictionGoal` (volta para casa), `LookAtPlayerGoal`.
  - Não despawna (`removeWhenFarAway` → false).
- `ModEntities.DIRT_GOLEM` (`dirtcheapgolems:dirt_golem`, hitbox 0.6×0.9, `MobCategory.MISC`).
- `DirtGolem`: material `Blocks.DIRT`, drop `Items.DIRT`, chance `GolemConfig.dirtDropChance()`.

- [ ] **Step 1: GameTests que falham** em `CoreGolemTests`:
  - `integrityDepletionCrumbles` (T3): spawna `DirtGolem`, `consumeIntegrity(99)`, força 1 ciclo de passiva (`passiveDecayTicks`) → entidade removida.
  - `consumeToZeroCrumblesImmediately`: `consumeIntegrity(100)` → removida no mesmo tick.
  - `nbtRoundTrip` (T11): golem com integridade 57 e casa (2,1,2) relativa; salvar em NBT, criar nova entidade e carregar → `getIntegrity()==57`, `getHomePos()` igual.
  - `spawnEggSetsHome` (Review Focus 2): spawn sem casa via `EntityType.spawn` com motivo SPAWN_EGG → `getHomePos()` == posição de spawn.
  - `healingDoesNotRestoreIntegrity`: `consumeIntegrity(30)`, `heal(10)` → integridade continua 70.
- [ ] **Step 2:** `./gradlew runGameTestServer` → FAIL.
- [ ] **Step 3:** Implementar `AbstractCheapGolem`, `DirtGolem`, `ModEntities`, registro de atributos.
- [ ] **Step 4:** `./gradlew runGameTestServer` → todos PASS.
- [ ] **Step 5:** Commit `feat: add AbstractCheapGolem base and DirtGolem entity`.

---

### Task 4: Núcleo de Golem, tags e ativação

**Files:**
- Create: `registry/ModItems.java`, `registry/ModTags.java`, `item/GolemCoreItem.java`, `golem/GolemType.java`, `golem/GolemActivation.java`
- Create (datagen): `datagen/ModRecipeProvider.java`, `datagen/ModBlockTagsProvider.java`, `datagen/DataGenerators.java`
- Test: `gametest/ActivationTests.java`

**Interfaces:**
- Consumes: `ModEntities.DIRT_GOLEM`, `AbstractCheapGolem.setHomePos`.
- Produces:
  - `ModItems.GOLEM_CORE` (`dirtcheapgolems:golem_core`, stack 64).
  - `ModTags.Blocks.MATERIAL_DIRT|MATERIAL_SAND|MATERIAL_NETHERRACK` = `dirtcheapgolems:materials/{dirt,sand,netherrack}` com conteúdo da spec §5.3.
  - `enum GolemType` — `DIRT(MATERIAL_DIRT, () -> ModEntities.DIRT_GOLEM)`, `SAND`, `NETHERRACK` (as duas últimas com supplier que as Tasks 9 e 10 preenchem; até lá, `SAND`/`NETHERRACK` retornam `null` e são ignorados por `fromPillar`). `static Optional<GolemType> fromPillar(BlockState top, BlockState bottom)` — ambos na mesma tag.
  - `static InteractionResult GolemActivation.tryActivate(Level level, BlockPos clicked, Player player, ItemStack stack)` — se `clicked` for o bloco de baixo de um pilar, usa `clicked.above()` como topo; senão usa `clicked` como topo e `clicked.below()` como base. Exige `player.mayUseItemAt(...)` para os dois blocos. Válido → remove os 2 blocos sem drops, spawna o golem centralizado na base, `setHomePos(base)`, consome 1 núcleo se não for criativo, emite partículas do bloco e o som de colocação do material, retorna `SUCCESS`. Inválido → `PASS` sem efeitos.
  - Receita shaped: argila em cruz + redstone no centro → 2 núcleos (spec §5.1).

- [ ] **Step 1: GameTests que falham** em `ActivationTests`:
  - `dirtPillarCreatesGolem` (T1): dois `dirt` empilhados, jogador mock sobrevivência com 1 núcleo usa no topo → 1 `DirtGolem` na posição de baixo, ambos os blocos viram ar, stack do núcleo = 0, `getHomePos()` = pos de baixo.
  - `clickingBottomBlockAlsoWorks`: mesmo cenário clicando no bloco de baixo → golem criado.
  - `coarseDirtMixedWithDirtWorks`: `coarse_dirt` sobre `dirt` → golem criado (mesma tag).
  - `mixedPillarDoesNothing` (T2): `dirt` sobre `sand` → nenhum golem, blocos intactos, núcleo não consumido.
  - `creativeDoesNotConsumeCore`: jogador criativo → golem criado, stack continua 1.
  - `adventureModeCannotActivate` (Review Focus 1): jogador em modo aventura → nada acontece, núcleo intacto.
- [ ] **Step 2:** `./gradlew runGameTestServer` → FAIL.
- [ ] **Step 3:** Implementar itens, tags, `GolemType`, `GolemActivation`, `GolemCoreItem.useOn` delegando a `tryActivate`; datagen de receita e tags; rodar `./gradlew runData` e commitar os JSON gerados.
- [ ] **Step 4:** `./gradlew runGameTestServer` → PASS. Verificar `src/generated/resources/data/dirtcheapgolems/recipe/golem_core.json` (ou caminho equivalente do 26.3) com resultado `count: 2`.
- [ ] **Step 5:** Commit `feat: add golem core item, material tags and pillar activation`.

---

### Task 5: Cliente — modelo, renderer, rachaduras, ovos de spawn e traduções

**Files:**
- Create: `client/CheapGolemModel.java`, `client/CheapGolemRenderer.java`, `client/ClientSetup.java` (só registrado em `Dist.CLIENT`)
- Create: `src/main/resources/assets/dirtcheapgolems/textures/entity/dirt_golem.png`, `cracks_low.png`, `cracks_high.png`, `textures/item/golem_core.png` (64×64 / 16×16, placeholders aceitáveis)
- Create (datagen): `datagen/ModLanguageProvider` para `en_us` e `pt_br`, `datagen/ModItemModelProvider`
- Modify: `ModItems.java` (ovo de spawn `dirt_golem_spawn_egg`)

**Interfaces:**
- Consumes: `AbstractCheapGolem.getCrackStage()`.
- Produces: `CheapGolemRenderer<T extends AbstractCheapGolem>(context, ResourceLocation texture)` — reutilizável pelas Tasks 9 e 10 só trocando a textura; aplica overlay `cracks_low`/`cracks_high` por `CrackStage`. `CheapGolemModel` com cabeça, corpo, dois braços, pernas curtas; animação de andar e balanço de braços.
- Chaves de tradução: `item.dirtcheapgolems.golem_core` = "Golem Core" / "Núcleo de Golem"; `entity.dirtcheapgolems.dirt_golem` = "Dirt Golem" / "Golem de Terra"; ovo de spawn idem.

- [ ] **Step 1:** Implementar modelo, renderer, layer de rachaduras, registro de renderer e layer definition no `ClientSetup`.
- [ ] **Step 2:** Datagen de lang/modelos; `./gradlew runData`.
- [ ] **Step 3: Verificação manual** com `./gradlew runClient`: craftar núcleo, criar Golem de Terra, executar `/data merge entity @e[type=dirtcheapgolems:dirt_golem,limit=1] {Integrity:50}` → rachadura leve visível; `{Integrity:20}` → pesada. Nomes aparecem em pt_br com idioma Português (Brasil).
- [ ] **Step 4:** `./gradlew runServer` sobe sem crash (nenhuma classe de cliente carregada no servidor). `./gradlew runGameTestServer` continua PASS.
- [ ] **Step 5:** Commit `feat: add golem model, renderer with crack overlays, spawn egg and translations`.

---

### Task 6: Golem de Terra — colher e replantar

**Files:**
- Create: `entity/ai/HarvestAndReplantGoal.java`
- Modify: `entity/DirtGolem.java` (registrar meta; desativar pisoteio de farmland)
- Test: `gametest/DirtGolemTests.java`

**Interfaces:**
- Consumes: `AbstractCheapGolem.consumeIntegrity`, `getHomePos`, `GolemConfig.dirtHarvestCost()/dirtHarvestScanInterval()/dirtHarvestScanRadius()`, `GolemConfig.homeRadius()`.
- Produces: `HarvestAndReplantGoal(AbstractCheapGolem golem, double speed)`. Algoritmo:
  1. `canUse()` só a cada `scanInterval` ticks; procura a `CropBlock` com `isMaxAge(state)` mais próxima dentro de `scanRadius` do golem **e** dentro de `homeRadius` da casa.
  2. Navega até distância ≤ 1.5 do alvo.
  3. Ao chegar: **revalida** que o bloco ainda é planta madura (se não for, termina sem custo). Quebra com `level.destroyBlock(pos, false)` + drops via `Block.getDrops(state, serverLevel, pos, null, golem, ItemStack.EMPTY)`.
  4. Item de replantio = `state.getBlock().getCloneItemStack(...)` (o item que planta o crop). Se estiver nos drops: remove 1 e coloca `block.defaultBlockState()` (idade 0); senão não replanta.
  5. Spawna os drops restantes como `ItemEntity`. `consumeIntegrity(harvestCost)`.
- Pisoteio: sobrescrever o método de "pode pisotear farmland" (confirmar nome no 26.3, ex.: `canTrample`/checagem em `FarmBlock.fallOn`) para retornar `false`.

- [ ] **Step 1: GameTests que falham:**
  - `harvestsAndReplantsWheat` (T4): farmland + trigo idade 7 a 2 blocos do golem → após ≤ 200 ticks: bloco é trigo idade 0, existe `ItemEntity` de trigo no chão, integridade = 99.
  - `ignoresImmatureCrops`: trigo idade 3 → após 200 ticks, trigo continua idade 3 e integridade `>= 99` (tolerância para a perda passiva).
  - `replantsPotato`: batata madura → após ≤ 200 ticks é batata idade 0. (O ramo "sem semente nos drops" não é reproduzível com crops vanilla; fica coberto por revisão de código.)
  - `respectsHomeRadius` (Review Focus 3): casa definida a 0 blocos, `homeRadius` efetivo de teste = 3, trigo maduro a 5 blocos (dentro do scanRadius 8) → não colhido após 200 ticks.
  - `twoGolemsOneCrop` (Review Focus 4): dois golems equidistantes de um único trigo maduro → trigo colhido uma vez; soma das integridades perdidas por colheita = 1.
  - `doesNotTrampleFarmland`: golem cai de 2 blocos sobre farmland → continua farmland.
- [ ] **Step 2:** `./gradlew runGameTestServer` → FAIL.
- [ ] **Step 3:** Implementar `HarvestAndReplantGoal` e ajustes no `DirtGolem`.
- [ ] **Step 4:** `./gradlew runGameTestServer` → PASS.
- [ ] **Step 5:** Commit `feat: dirt golem harvests and replants mature crops`.

> Nota sobre `homeRadius` em testes: exponha um setter de teste `setHomeRadiusOverride(int)` em `AbstractCheapGolem` (package-private ou documentado como uso de teste) em vez de mexer na config global.

---

### Task 7: Golem de Terra — chuva, água e abrigo

**Files:**
- Create: `entity/ai/SeekShelterFromRainGoal.java`
- Modify: `entity/DirtGolem.java` (`environmentTick`, meta de abrigo com prioridade maior que a colheita)
- Test: `gametest/DirtGolemTests.java`

**Interfaces:**
- Consumes: `GolemConfig.dirtRainDecayPerSecond()`.
- Produces:
  - `DirtGolem.environmentTick()`: a cada 20 ticks, se `isInWater()` → `consumeIntegrity(2 × rate)`; senão se `level.isRainingAt(blockPosition().above())` → `consumeIntegrity(rate)`.
  - `SeekShelterFromRainGoal(PathfinderMob mob, double speed)`: ativo enquanto `isRainingAt(pos acima)`; procura (até 10 tentativas aleatórias, raio 10, dentro da casa) uma posição com `!level.canSeeSky(pos)` e navega até ela — análogo a `FleeSunGoal`.

- [ ] **Step 1: GameTests que falham:**
  - `waterDoublesDecay` (T5): golem em bloco de água por 40 ticks → integridade caiu 8 (±1 pela passiva).
  - `rainDecays`: forçar chuva (`serverLevel.setWeatherParameters(0, 6000, true, false)`), golem a céu aberto em bioma com chuva, 40 ticks → caiu 4 (±1). Restaurar o clima no fim do teste.
  - `shelteredFromRainDoesNotDecay`: mesmo cenário com um teto sólido acima → sem perda por chuva.
- [ ] **Step 2:** `./gradlew runGameTestServer` → FAIL.
- [ ] **Step 3:** Implementar.
- [ ] **Step 4:** `./gradlew runGameTestServer` → PASS. **Manual** (`runClient`, `/weather rain`): golem anda até uma cobertura próxima.
- [ ] **Step 5:** Commit `feat: dirt golem melts in rain and water and seeks shelter`.

---

### Task 8: Efeito "Areia nos Olhos"

**Files:**
- Create: `effect/SandInEyesEffect.java`, `registry/ModEffects.java`
- Create: `assets/dirtcheapgolems/textures/mob_effect/sand_in_eyes.png`
- Modify: datagen de lang (`effect.dirtcheapgolems.sand_in_eyes` = "Sand in Eyes" / "Areia nos Olhos")
- Test: `gametest/SandGolemTests.java`

**Interfaces:**
- Produces: `ModEffects.SAND_IN_EYES` (`MobEffectCategory.HARMFUL`), com modificador de atributo `FOLLOW_RANGE` −0.75 `ADD_MULTIPLIED_TOTAL` (id `dirtcheapgolems:sand_in_eyes`). Ao ser aplicado num `Mob` (hook de "on effect added"/`onEffectStarted` — confirmar no 26.3): `mob.setTarget(null)`.

- [ ] **Step 1: GameTests que falham:**
  - `clearsTargetOnApply`: zumbi com alvo (armor stand não serve; usar um villager como alvo) → aplicar efeito 60 ticks → `getTarget()==null` no tick seguinte.
  - `reducesFollowRange`: `getAttributeValue(FOLLOW_RANGE)` com efeito == 25% do valor base (tolerância 0.01).
  - `followRangeRestoredAfterExpiry`: após 61 ticks, valor == base.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat: add Sand in Eyes mob effect`.

---

### Task 9: Golem de Areia — torre e projétil

**Files:**
- Create: `entity/SandGolem.java`, `entity/projectile/SandBlast.java`, `entity/ai/StationaryTurretGoal.java`
- Create: `client/SandBlastRenderer.java`, textura `sand_golem.png`
- Modify: `ModEntities` (`SAND_GOLEM`, `SAND_BLAST`), `GolemType.SAND` supplier, `ModItems` (ovo), `ClientSetup`, lang
- Test: `gametest/SandGolemTests.java`

**Interfaces:**
- Consumes: `ModEffects.SAND_IN_EYES`, `CheapGolemRenderer`, `GolemConfig.sand*()`.
- Produces:
  - `SandGolem`: atributos base com velocidade 0 e `KNOCKBACK_RESISTANCE` 1.0; sem metas de movimento (remove stroll/retorno); metas: `StationaryTurretGoal`, `LookAtPlayerGoal`. Material `Blocks.SAND`, drop `Items.SAND`, chance `sandDropChance()`. Seletor de alvo: `NearestAttackableTargetGoal<Monster>` com `mustSee=true`, alcance `sandTurretRange()`.
  - `StationaryTurretGoal(SandGolem golem)`: com alvo válido e `hasLineOfSight`, a cada `sandTurretCooldown()` ticks dispara um `SandBlast` mirado no alvo e chama `consumeIntegrity(sandShotCost())`.
  - `SandBlast extends ThrowableItemProjectile` (ou `Projectile` — escolha a mais simples no 26.3), velocidade 1.5, pouca gravidade. `onHitEntity`: se o alvo for jogador ou `AbstractCheapGolem`/Iron/Snow Golem → ignora (sem dano/efeito); senão dano `sandDamage()` (fonte: projétil, dono = golem), Lentidão II por `sandSlownessTicks()`, `SAND_IN_EYES` por `sandInEyesTicks()`. `onHit` sempre: partículas `FALLING_DUST` de areia e `discard()`.

- [ ] **Step 1: GameTests que falham:**
  - `firesAtZombieInSight` (T6): golem e zumbi a 8 blocos, linha de visão livre → em ≤ 100 ticks o zumbi tem `MOVEMENT_SLOWDOWN` amplificador 1 e `SAND_IN_EYES`; integridade do golem ≤ 98.
  - `noFireThroughWall` (T7): parede de pedra entre eles → após 100 ticks, zumbi sem efeitos, integridade ≥ 99.
  - `doesNotMove`: aplicar `knockback(2.0, 1, 0)` e esperar 20 ticks → posição X/Z inalterada (±0.01).
  - `ignoresPlayers`: só um jogador mock no alcance → nenhum `SandBlast` existe após 100 ticks.
  - `golemFromSandPillar`: pilar `red_sand`+`sand` + núcleo → `SandGolem` criado.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS.
- [ ] **Step 5: Manual** (`runClient`): projétil visível com partículas; zumbi perde o foco ao ser atingido.
- [ ] **Step 6:** Commit `feat: add sand golem turret and sand blast projectile`.

---

### Task 10: Golem de Netherrack — entidade e luz ambulante

**Files:**
- Create: `entity/NetherrackGolem.java`, `entity/WalkingLight.java`, textura `netherrack_golem.png` (+ camada emissiva)
- Modify: `ModEntities` (`NETHERRACK_GOLEM`), `GolemType.NETHERRACK`, `ModItems` (ovo), `ClientSetup` (renderer com layer emissivo), lang
- Test: `gametest/NetherrackGolemTests.java`

**Interfaces:**
- Consumes: `GolemConfig.netherrackLightLevel()`.
- Produces:
  - `final class WalkingLight` — `@Nullable BlockPos current()`; `void update(Level level, BlockPos desired, int lightLevel)`: se `desired` == `current`, nada; remove a luz em `current` **só se** o bloco lá ainda for `Blocks.LIGHT`; coloca `Blocks.LIGHT` com `LEVEL` = lightLevel em `desired` **só se** for ar (caso contrário, `current = null`); `void clear(Level level)`; `void save(CompoundTag)` / `void load(CompoundTag)` com a chave `LightPos`.
  - `NetherrackGolem`: imune a fogo (`fireImmune()` no `EntityType`); a cada tick de servidor `walkingLight.update(level, blockPosition().above(), lightLevel)`; `clear` em `crumble()`, em `remove(RemovalReason)` (inclui descarregamento de chunk) e, ao carregar NBT, limpa o `LightPos` salvo antes de qualquer nova colocação. Material `Blocks.NETHERRACK`; drop `Items.CHARCOAL`, chance `netherrackDropChance()`; `crumble()` sobrescrito usa partículas de fumaça.

- [ ] **Step 1: GameTests que falham:**
  - `placesLightAtHead`: golem parado → bloco em `pos.above()` é `LIGHT` com nível 12.
  - `lightFollowsGolem`: teleportar o golem 3 blocos → luz antiga virou ar, nova existe.
  - `doesNotReplaceSolidBlocks`: bloco de vidro na posição da cabeça → vidro continua lá.
  - `noLightLeftAfterDeath` (T10): `kill()` → nenhum `LIGHT` na arena.
  - `noLightLeftAfterReload` (Review Focus 5): salvar NBT do golem, `discard()` com motivo `UNLOADED_TO_CHUNK`, verificar que a arena não tem `LIGHT`; criar nova entidade a partir do NBT em outra posição → só existe 1 `LIGHT` (na nova cabeça).
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat: add netherrack golem with walking light`.

---

### Task 11: Golem de Netherrack — kamikaze térmico

**Files:**
- Create: `entity/ai/KamikazeChargeGoal.java`, `entity/ThermalBlast.java`
- Modify: `entity/NetherrackGolem.java` (meta, estado de pavio sincronizado)
- Test: `gametest/NetherrackGolemTests.java`

**Interfaces:**
- Consumes: `GolemConfig.netherrackKamikazeRange()/FuseTicks()/BlastRadius()/BlastDamage()/BurnSeconds()/BlastCost()/BlastHurtsPlayers()`.
- Produces:
  - `static void ThermalBlast.detonate(ServerLevel level, AbstractCheapGolem source, double radius, float damage, int burnSeconds, boolean hurtPlayers)`: para cada `LivingEntity` em AABB inflado por `radius` e distância ≤ `radius`, exceto `source`, qualquer `AbstractCheapGolem`, Iron/Snow Golems e (se `!hurtPlayers`) jogadores: `hurt(fonte de fogo/onFire atribuída ao golem, damage)` e `igniteForSeconds(burnSeconds)`. Partículas `FLAME`/`LAVA`, som de fogo. **Não** cria `Explosion`, **não** altera blocos.
  - `NetherrackGolem`: `int getFuseTicks()` sincronizado (para partículas no cliente); `void startFuse()`, `void cancelFuse()`.
  - `KamikazeChargeGoal(NetherrackGolem golem, double speedModifier = 1.3)`: com alvo hostil (seletor `Monster` em `kamikazeRange`), persegue; distância ≤ 1.5 → `startFuse()` e para; durante o pavio, distância > 3 → `cancelFuse()` e volta a perseguir; pavio completo → `ThermalBlast.detonate(...)`, `cancelFuse()`, `consumeIntegrity(blastCost)`.

- [ ] **Step 1: GameTests que falham:**
  - `blastBurnsAndDamagesZombie` (T8): zumbi a 4 blocos (imobilizado com `setNoAi(true)`) → em ≤ 200 ticks o zumbi tem `isOnFire()` e vida < máxima; integridade do golem == 66 (±1); nenhum bloco da arena mudou (comparar contagem de blocos não-ar antes/depois, exceto o `LIGHT`).
  - `fuseCancelsWhenTargetLeaves`: iniciar pavio, teleportar o zumbi para 6 blocos antes do fim → zumbi sem fogo no tick em que o pavio teria acabado.
  - `blastSparesPlayersByDefault`: jogador mock a 1 bloco do zumbi no momento da explosão → jogador não está em chamas, vida cheia.
  - `threeBlastsCrumbleToCharcoal`: forçar 3 detonações via `consumeIntegrity(34)` ×3 com `netherrackDropChance=1.0` → golem removido, `ItemEntity` de `charcoal` presente.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS.
- [ ] **Step 5: Manual** (`runClient`): partículas e chiado do pavio visíveis; abraço → rajada de fogo.
- [ ] **Step 6:** Commit `feat: netherrack golem kamikaze thermal blast`.

---

### Task 12: Golem de Netherrack — água/chuva viram pedra

**Files:**
- Modify: `entity/NetherrackGolem.java` (`environmentTick`)
- Test: `gametest/NetherrackGolemTests.java`

**Interfaces:**
- Produces: `NetherrackGolem.environmentTick()`: se `isInWaterOrRain()` (água ou `isRainingAt` acima) → `walkingLight.clear(level)`; se o bloco em `blockPosition()` for ar ou `canBeReplaced()` (inclui água), coloca `Blocks.STONE`; partículas `CLOUD`, som `FIRE_EXTINGUISH`; `discard()` **sem** drop. Se não puder colocar pedra → `crumble()` normal.

- [ ] **Step 1: GameTests que falham:**
  - `waterTurnsIntoStone` (T9): golem dentro de água → em ≤ 5 ticks existe `stone` na posição, entidade removida, zero `LIGHT` e zero `ItemEntity` na arena.
  - `rainTurnsIntoStone`: forçar chuva com céu aberto → mesmo resultado; restaurar clima.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** PASS.
- [ ] **Step 5:** Commit `feat: netherrack golem turns to stone in water or rain`.

---

### Task 13: Verificação final

**Files:**
- Create: `README.md` (o que o mod faz, receita, como criar golems, como rodar testes)

- [ ] **Step 1:** `./gradlew clean build test runGameTestServer` → tudo PASS; anotar a contagem de testes.
- [ ] **Step 2:** `./gradlew runServer` sobe e para (`stop`) sem exceções no log.
- [ ] **Step 3:** Executar o **checklist manual da spec §9.2** em `runClient` e marcar cada item no PR/commit.
- [ ] **Step 4:** Conferir que `config/dirtcheapgolems-common.toml` tem todas as chaves da spec §8 e que alterar `sand.turretCooldown` para 10 e reiniciar muda a cadência de tiro.
- [ ] **Step 5:** Commit `docs: add README and finalize MVP`.
