# Dirt Cheap Golems v2 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar os Golems de End Stone, Cascalho e Basalto/Deepslate, o modo de comando do Núcleo e os ovos tingidos, sem quebrar a v1.

**Architecture:** Mesma arquitetura da v1: cada golem é uma subclasse de `AbstractCheapGolem`. A base ganha só três ganchos genéricos: `onCreatedBy`, `onCreatedFrom` e `receiveCommand`. O Núcleo ganha um modo de comando (agachado) e um data component de vínculo.

**Tech Stack:** Java (toolchain 25 do MDK), Forge 66.0.9, Minecraft 26.3, JUnit 5, Forge GameTest (padrão em `src/main/java/com/josebieco/dirtcheapgolems/gametest/README.md`).

**Spec:** `docs/specs/2026-10-09-dirt-cheap-golems-v2-design.md` (chamada aqui de "spec v2 §X"). Ela se apoia na spec v1, `docs/specs/2026-10-09-dirt-cheap-golems-design.md`.

## Global Constraints

- Minecraft **26.3**, Forge **66.0.9**, mod id `dirtcheapgolems`, pacote `com.josebieco.dirtcheapgolems`.
- **Confirme toda API do Forge/Minecraft no MDK antes de usar.** Isso vale especialmente para: capability de item handler, data components, `interactLivingEntity`, navegação de voo, `FallingBlockEntity`, game rule `mobGriefing`, `hurtServer` e `tints` de item model. Se trocar uma API, registre a troca no commit.
- Lógica só no servidor. Nada de `client/` no código comum.
- Todo número vem de `GolemConfig` (spec v2 §5). Convenção de getter: `<golem><Chave>()`, por exemplo `endStoneLinkRange()`, `gravelArtilleryRange()`, `basaltMagnetRadius()`.
- Integridade nunca aumenta. Golems nunca ferem nem miram jogadores, outros golems do mod, Iron/Snow Golems ou mobs passivos.
- **Agachado nunca cria golem** (spec v2 §3.1).
- Traduções completas em `en_us` e `pt_br` via `datagen/ModLanguageProvider`.
- Novo GameTest ou GameTest renomeado → rodar `gradlew runData` antes de `gradlew runGameTestServer`.
- Toda task termina com **todos** os GameTests (v1 + v2) passando, não só os novos.
- Testes que precisam de valores de config diferentes usam overrides na entidade, como o `setHomeRadiusOverride` da v1, nunca a config global. Game rules alteradas em teste são restauradas no fim.

## Review Focus

1. **Baú duplo vinculado por uma das metades:** a inserção deve usar o inventário combinado (54 slots). Teste na Task 4.
2. **Item recém-jogado pelo jogador** (com atraso de coleta): o End Stone não deve sugá-lo antes do atraso acabar. Teste na Task 4.
3. **Agachado + núcleo vinculado num golem que não usa ordens** (ex.: Terra): nada acontece, e o núcleo e o vínculo ficam intactos. Teste na Task 2.
4. **Dono do Basalto sai do servidor em FOLLOW:** o golem para, não crasha e não muda de modo. Teste na Task 9.
5. **Projétil de um mob não hostil** (bola de neve de Snow Golem) passando pelo ímã: não é absorvido. Teste na Task 8.

## Comandos

`gradlew test` · `gradlew runData` · `gradlew runGameTestServer` · `gradlew runClient` · `gradlew runServer` (no Windows, `gradlew.bat`).

---

### Task 1: Ovos tingidos (migração da v1)

**Files:**
- Create: `src/main/resources/assets/dirtcheapgolems/models/item/golem_spawn_egg.json`
- Modify: `assets/dirtcheapgolems/items/{dirt,sand,netherrack}_golem_spawn_egg.json`
- Delete: `textures/item/{dirt,sand,netherrack}_golem_spawn_egg.png`, `models/item/{dirt,sand,netherrack}_golem_spawn_egg.json`

**Interfaces:**
- Produces: modelo `dirtcheapgolems:item/golem_spawn_egg` (parent `minecraft:item/generated`, `layer0` = `dirtcheapgolems:item/golem_spawn_egg_base`, `layer1` = `dirtcheapgolems:item/golem_spawn_egg_overlay`). Os PNGs já existem (`tools/gen_spawn_egg.py`). Todo ovo, incluindo os das Tasks 3, 5 e 7, segue este formato:

```json
{ "model": { "type": "minecraft:model", "model": "dirtcheapgolems:item/golem_spawn_egg",
  "tints": [ { "type": "minecraft:constant", "value": <casca> }, { "type": "minecraft:constant", "value": <manchas> } ] } }
```

Cores (spec v2 §3.5), em decimal:

| Golem | Casca | Manchas |
|---|---|---|
| Terra | 8806467 | 6986810 |
| Areia | 14931876 | 11045452 |
| Netherrack | 7288116 | 16751150 |
| End Stone | 14409374 | 10182592 |
| Cascalho | 8748926 | 4012602 |
| Basalto | 5131602 | 10133672 |

- [ ] **Step 1:** Criar o modelo compartilhado, reescrever os 3 `items/*.json` e apagar os PNGs e modelos antigos.
- [ ] **Step 2:** `gradlew runClient` → no inventário criativo, os 3 ovos aparecem tingidos com as cores certas. No log, nenhum aviso de textura ou modelo ausente. Se o formato de `tints` diferir no 26.3, ajustar e anotar no commit.
- [ ] **Step 3:** `gradlew runGameTestServer` → todos PASS.
- [ ] **Step 4:** Commit `feat: switch spawn eggs to the shared tinted template`.

---

### Task 2: Modo de comando do Núcleo e ganchos da base

**Files:**
- Create: `registry/ModDataComponents.java`
- Modify: `item/GolemCoreItem.java`, `golem/GolemActivation.java`, `entity/AbstractCheapGolem.java`, `DirtCheapGolems.java`, `datagen/ModLanguageProvider.java`
- Test: `gametest/CommandTests.java` (registrar em `ModGameTests.TEST_CLASSES`, prefixo `command`)

**Interfaces:**
- Produces:
  - `ModDataComponents.LINKED_CONTAINER : DataComponentType<GlobalPos>` (`dirtcheapgolems:linked_container`, persistente e sincronizado na rede).
  - `GolemCoreItem`:
    - `static @Nullable GlobalPos getLink(ItemStack core)`;
    - `static void setLink(ItemStack core, @Nullable GlobalPos pos)`, onde `null` remove o componente e o glint;
    - com link, também aplica `DataComponents.ENCHANTMENT_GLINT_OVERRIDE = true`.
  - Regras de `GolemCoreItem.useOn` (spec v2 §3.1):
    - agachado + bloco com capability de item handler → `setLink`, action bar `message.dirtcheapgolems.core.linked` ("Linked to %s" / "Vinculado a %s"), `SUCCESS`;
    - agachado + qualquer outro bloco → `PASS`;
    - não agachado → `GolemActivation.tryActivate` (inalterado).
  - `GolemCoreItem.use` (no ar), agachado e com link → `setLink(stack, null)`, action bar `message.dirtcheapgolems.core.unlinked` ("Link cleared" / "Vínculo removido").
  - `GolemCoreItem.interactLivingEntity`: agachado e alvo `AbstractCheapGolem` → `golem.receiveCommand(player, stack)` no servidor; senão `PASS`.
  - Tooltip com link: `tooltip.dirtcheapgolems.core.linked` = "Linked to: %s, %s, %s (%s)" / "Vinculado a: %s, %s, %s (%s)".
  - `AbstractCheapGolem`:
    - `protected void onCreatedBy(Player player) {}`;
    - `protected void onCreatedFrom(BlockState bottom) {}`;
    - `public InteractionResult receiveCommand(Player player, ItemStack core) { return InteractionResult.PASS; }`;
    - `public void setCrumbleDropChanceOverride(@Nullable Double chance)`: quando não nulo, é usado no lugar de `getCrumbleDropChance()` em `crumble()` (só para testes).
  - `GolemActivation.tryActivate`: após o spawn e `setHomePos`, chama `golem.onCreatedFrom(bottomState)` e depois `golem.onCreatedBy(player)`. O `bottomState` precisa ser capturado **antes** de remover o pilar.

- [ ] **Step 1: GameTests que falham** (`CommandTests`):
  - `sneakingOnPillarDoesNothing` (V2-T1): jogador mock agachado (`setShiftKeyDown(true)`) usa o núcleo em pilar de terra → nenhum golem, os 2 blocos intactos, contagem do núcleo = 1.
  - `sneakingOnChestStoresLink` (V2-T2): baú em (4,1,4), uso agachado → `GolemCoreItem.getLink(stack)` = `GlobalPos.of(level.dimension(), absolutePos(4,1,4))`, contagem = 1.
  - `sneakingOnStoneDoesNotLink`: uso agachado em pedra → `getLink` == null.
  - `sneakingInAirClearsLink`: núcleo com link, `use` agachado → `getLink` == null.
  - `commandOnDirtGolemIsIgnored` (Review Focus 3): núcleo com link, `interactOn` agachado num `DirtGolem` → `getLink` inalterado, contagem = 1, integridade do golem ≥ 99.
  - `normalClickStillActivates`: regressão. Não agachado no pilar → golem criado.
- [ ] **Step 2:** `gradlew runData && gradlew runGameTestServer` → os novos testes FAIL.
- [ ] **Step 3:** Implementar o data component, as mudanças no `GolemCoreItem`, os ganchos e as chamadas em `GolemActivation`.
- [ ] **Step 4:** `gradlew runGameTestServer` → todos PASS, incluindo `ActivationTests` da v1.
- [ ] **Step 5:** Commit `feat: golem core command mode with container link and golem hooks`.

---

### Task 3: Golem de End Stone — entidade, voo e vínculo

**Files:**
- Create: `entity/EndStoneGolem.java`, `assets/.../items/end_stone_golem_spawn_egg.json`, `data/dirtcheapgolems/tags/block/materials/end_stone.json`
- Modify: `registry/ModEntities.java`, `registry/ModItems.java`, `registry/ModTags.java`, `golem/GolemType.java`, `config/GolemConfig.java` (seção `end_stone`, spec v2 §5), `client/ClientSetup.java`, `tools/gen_textures.py` (paleta `end_stone`; gerar `end_stone_golem.png`), lang
- Test: `gametest/EndStoneGolemTests.java` (prefixo `end_stone`)

**Interfaces:**
- Consumes: `GolemCoreItem.getLink`, `AbstractCheapGolem.receiveCommand`.
- Produces:
  - `ModEntities.END_STONE_GOLEM` (0,6 × 0,9).
  - `GolemType.END_STONE(MATERIAL_END_STONE, ...)`.
  - `EndStoneGolem`:
    - sem gravidade, imune a dano de queda;
    - movimento e navegação de voo (padrão do Allay), velocidade `endStoneFlySpeed()`;
    - stroll aéreo restrito à casa;
    - drop `Items.END_STONE` com chance `endStoneDropChance()`;
    - `crumble()` com partículas `PORTAL`.
  - `@Nullable GlobalPos getLinkedContainer()`; `void clearLink()`.
  - `enum LinkResult { LINKED, TOO_FAR, OTHER_DIMENSION, NO_INVENTORY }` e `LinkResult tryLink(GlobalPos pos)`:
    - outra dimensão → `OTHER_DIMENSION`;
    - distância do bloco até a casa > `endStoneLinkRange()` → `TOO_FAR`;
    - bloco sem capability de item handler → `NO_INVENTORY`;
    - senão grava o vínculo e retorna `LINKED`.
    - Em qualquer recusa, mantém o vínculo anterior.
  - `receiveCommand`: sem link no núcleo → `PASS`. Com link → `tryLink`, envia a mensagem `message.dirtcheapgolems.end_stone.<resultado_em_minúsculas>` na action bar e retorna `SUCCESS`.
  - Traduções:

    | Chave | en_us | pt_br |
    |---|---|---|
    | `.linked` | "Linked" | "Vinculado" |
    | `.too_far` | "Too far from home" | "Muito longe da casa" |
    | `.other_dimension` | "Another dimension" | "Outra dimensão" |
    | `.no_inventory` | "No inventory" | "Sem inventário" |

  - NBT: `LinkedContainer` (GlobalPos) e `ItemsCarried` (int; usado na Task 4).
  - Renderer: `CheapGolemRenderer` com `end_stone_golem.png`, bobbing vertical no cliente (seno, amplitude 0,1).

- [ ] **Step 1: GameTests que falham:**
  - `endStonePillarCreatesGolem`: pilar de `end_stone` + núcleo → `EndStoneGolem` criado.
  - `linksToChestNearHome` (V2-T3): baú a 3 blocos da casa, núcleo com link → `interactOn` agachado → `getLinkedContainer()` == link; contagem do núcleo = 1.
  - `refusesChestTooFar` (V2-T4): `tryLink` com um `GlobalPos` a 40 blocos da casa → `TOO_FAR` e `getLinkedContainer()` == null.
  - `refusesBlockWithoutInventory`: `tryLink` em pedra → `NO_INVENTORY`.
  - `doesNotFall`: golem spawnado a y=3 sobre ar, após 40 ticks y ≥ 2,5 (relativo).
  - `linkSurvivesReload` (parte de V2-T19): salvar, carregar em nova entidade → mesmo `getLinkedContainer()`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar e rodar `python tools/gen_textures.py`. **Step 4:** Todos PASS.
- [ ] **Step 5:** Commit `feat: add end stone golem with flight and container link`.

---

### Task 4: Golem de End Stone — coleta e teleporte de itens

**Files:**
- Create: `entity/ai/CollectItemsGoal.java`
- Modify: `entity/EndStoneGolem.java`
- Test: `gametest/EndStoneGolemTests.java`

**Interfaces:**
- Consumes: `getLinkedContainer`, `clearLink`, `GolemConfig.endStonePickupRange()/endStoneItemsPerIntegrity()`, `consumeIntegrity`.
- Produces: `CollectItemsGoal(EndStoneGolem golem)`. Algoritmo, a cada 20 ticks:
  1. Sem link → inativo.
  2. Bloco vinculado sem capability, com o chunk carregado → `clearLink()` + partículas `SMOKE`, termina.
  3. Chunk do vínculo não carregado → inativo, mantém o link.
  4. Alvo = `ItemEntity` mais próximo dentro do raio da casa que **não** tenha atraso de coleta (`hasPickUpDelay()`) e cuja inserção simulada aceite ≥ 1 item.
  5. Voa até ≤ `pickupRange`. Insere de verdade e atualiza ou remove o `ItemEntity` com a sobra.
  6. Efeitos: partículas `PORTAL` nas duas pontas e `ENDERMAN_TELEPORT` com volume 0,3.
  7. Custo: `ItemsCarried += inseridos`; enquanto `ItemsCarried >= itemsPerIntegrity`, subtrai `itemsPerIntegrity` e chama `consumeIntegrity(1)`.

```java
// Inserção em todos os slots do IItemHandler (a capability do baú duplo já é o inventário combinado):
ItemStack remaining = stack.copy();
for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++)
    remaining = handler.insertItem(slot, remaining, simulate);
```

- [ ] **Step 1: GameTests que falham:**
  - `teleportsItemsIntoChest` (V2-T5): baú vinculado; 8 `ItemEntity` de pedra (1 cada) espalhados a ≤ 4 blocos, com `setNoPickUpDelay()` → em ≤ 400 ticks o baú tem 8 de pedra, não há `ItemEntity` na arena e a integridade está entre 97 e 98.
  - `fullChestLeavesItems` (V2-T6): baú cheio de terra (27 × 64) e 4 itens de pedra → após 200 ticks os 4 continuam no chão; integridade ≥ 99.
  - `brokenChestClearsLink` (V2-T7): após o vínculo, `destroyBlock` no baú → em ≤ 40 ticks `getLinkedContainer()` == null e o golem está vivo.
  - `doubleChestUsesBothHalves` (Review Focus 1): baú duplo vinculado pela metade A; metade A cheia; 4 pedras no chão → em ≤ 400 ticks as 4 pedras estão na metade B.
  - `ignoresItemsWithPickupDelay` (Review Focus 2): `ItemEntity` com `setPickUpDelay(200)` → após 100 ticks continua no chão; antes de 400 ticks está no baú (`maxTicks` 450).
  - `itemsCarriedSurvivesReload`: `ItemsCarried` = 3 → salvar e carregar → 3.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Todos PASS.
- [ ] **Step 5:** Manual (`runClient`): End Stone vinculado ao lado de uma fazenda do Golem de Terra recolhe a colheita; partículas de portal visíveis.
- [ ] **Step 6:** Commit `feat: end stone golem teleports dropped items into its linked container`.

---

### Task 5: Golem de Cascalho — entidade e artilharia

**Files:**
- Create: `entity/GravelGolem.java`, `entity/projectile/FlintShard.java`, `entity/ai/GravelArtilleryGoal.java`, `items/gravel_golem_spawn_egg.json`, `tags/block/materials/gravel.json`
- Modify: `ModEntities` (`GRAVEL_GOLEM` 0,7 × 0,9; `FLINT_SHARD`), `ModItems`, `ModTags`, `GolemType.GRAVEL`, `GolemConfig` (seção `gravel`), `ClientSetup` (renderer do golem + `ThrownItemRenderer` para o shard), `gen_textures.py` (paleta `gravel`), lang (`entity.dirtcheapgolems.flint_shard` = "Flint Shard" / "Estilhaço de Sílex")
- Test: `gametest/GravelGolemTests.java` (prefixo `gravel`)

**Interfaces:**
- Produces:
  - `GravelGolem`: vida 16, velocidade 0,15, `KNOCKBACK_RESISTANCE` 0,6; drop `Items.GRAVEL` com `gravelDropChance()`.
  - Alvo: `NearestAttackableTargetGoal<Mob>` com predicado `Enemy` e linha de visão, alcance `gravelArtilleryRange()`, ordenado por (menor Y, depois menor distância).
  - `GravelArtilleryGoal(GravelGolem)`: a cada `gravelArtilleryCooldown()` ticks, com alvo visível, dispara um `FlintShard` (balístico, velocidade 1,4, mira com compensação de altura como o esqueleto) e chama `consumeIntegrity(gravelShotCost())`.
  - `FlintShard extends ThrowableItemProjectile`, item exibido `Items.FLINT`:
    - colide só com `Enemy` e atravessa o resto (mesmo padrão do `SandBlast` corrigido);
    - ao acertar: dano `gravelShardDamage()` e Lentidão I (amplificador 0) por `gravelSlownessTicks()`;
    - `onHit`: partículas de bloco de cascalho e `discard()`.

- [ ] **Step 1: GameTests que falham:**
  - `firesAtVisibleZombie` (V2-T9): zumbi `setNoAi` a 6 blocos, visível → em ≤ 120 ticks tem `MOVEMENT_SLOWDOWN` amplificador 0; integridade do golem ≤ 97.
  - `shardPassesThroughVillager`: aldeão entre o golem e o zumbi → aldeão com vida cheia e sem efeitos.
  - `prefersLowerTarget`: golem a y=3 sobre uma coluna de pedra; zumbi A a y=1 (6 blocos de distância) e zumbi B a y=3 (4 blocos) → após 40 ticks, `getTarget()` == A.
  - `gravelPillarCreatesGolem`: pilar de cascalho sobre piso de pedra + núcleo → `GravelGolem`.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Todos PASS.
- [ ] **Step 5:** Commit `feat: add gravel golem with flint shard artillery`.

---

### Task 6: Golem de Cascalho — desabamento

**Files:**
- Create: `entity/ai/GravelLeapGoal.java`
- Modify: `entity/GravelGolem.java`
- Test: `gametest/GravelGolemTests.java`

**Interfaces:**
- Consumes: `GolemConfig.gravelLeapTriggerRange()/gravelLeapBelowHorizontal()/gravelLeapBelowDepth()/gravelLeapImpactDamage()/gravelLeapBlocks()/gravelLeapPlacesBlocks()`.
- Produces:
  - `GravelLeapGoal(GravelGolem)`, com prioridade maior que a artilharia. Dispara se o alvo estiver a ≤ `leapTriggerRange` **ou** abaixo do golem com distância horizontal ≤ `leapBelowHorizontal` e `golem.y - alvo.y` ≤ `leapBelowDepth`. Aplica um impulso balístico em direção ao alvo.
  - `void GravelGolem.collapseOnto(LivingEntity target)`, chamado ao tocar o alvo ou ao aterrissar a ≤ 1,5 bloco dele (os testes podem chamar direto):
    1. `target.hurt(dano de impacto atribuído ao golem, leapImpactDamage)`.
    2. Posições, em ordem: `head` = bloco logo acima da cabeça do alvo; depois `head` deslocado N/S/L/O, embaralhado; pega as primeiras `leapBlocks`.
    3. Se `mobGriefing` e `leapPlacesBlocks`: para cada posição que seja **ar**, cria `FallingBlockEntity.fall(level, pos, Blocks.GRAVEL.defaultBlockState())`; as demais são puladas.
    4. Senão: spawna `leapBlocks` itens de cascalho (um `ItemEntity` com essa contagem) na posição do alvo.
    5. Partículas e som de cascalho; `discard()` **sem** passar por `crumble()` (sem drop extra).

- [ ] **Step 1: GameTests que falham:**
  - `collapseBuriesZombie` (V2-T10): piso de pedra, zumbi `setNoAi` em (4,1,4), golem em (4,1,2) → em ≤ 200 ticks: golem removido; `(4,1,4)` ou `(4,2,4)` é `gravel`; vida do zumbi < máxima.
  - `noBlocksWithoutMobGriefing` (V2-T11): `mobGriefing=false` (restaurar ao fim) → nenhum `gravel` na arena; itens de cascalho no chão somando 4.
  - `neverReplacesSolidBlocks` (V2-T12): vidro em `head` e nas 4 vizinhas → após `collapseOnto`, os 5 vidros intactos.
  - `leapsDownFromWall`: golem a y=4 numa coluna de pedra; zumbi `setNoAi` a y=1, 1 bloco ao lado na horizontal → o golem sai da coluna em ≤ 60 ticks.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Todos PASS.
- [ ] **Step 5:** Manual: um Cascalho no alto de uma muralha de 4 blocos enterra um zumbi que encosta no muro.
- [ ] **Step 6:** Commit `feat: gravel golem leaps and buries enemies under falling gravel`.

---

### Task 7: Golem de Basalto — entidade, dono, modos e bloco de origem

**Files:**
- Create: `entity/BasaltGolem.java`, `entity/ShieldMode.java`, `items/basalt_golem_spawn_egg.json`, `tags/block/materials/basalt.json`
- Modify: `ModEntities` (`BASALT_GOLEM` 0,8 × 1,2), `ModItems`, `ModTags`, `GolemType.BASALT`, `GolemConfig` (seção `basalt`), `ClientSetup` (renderer com escala 1,3), `gen_textures.py` (paleta `basalt`), lang
- Test: `gametest/BasaltGolemTests.java` (prefixo `basalt`)

**Interfaces:**
- Consumes: `onCreatedBy`, `onCreatedFrom`, `receiveCommand`, `setCrumbleDropChanceOverride`.
- Produces:
  - `enum ShieldMode { FOLLOW, GUARD }`.
  - `BasaltGolem`: vida 30, velocidade 0,25, `KNOCKBACK_RESISTANCE` 0,8, sem ataque.
  - `@Nullable UUID getOwnerId()`, `@Nullable Player getOwner()` (online e na mesma dimensão), `ShieldMode getMode()` (sincronizado), `void setMode(ShieldMode)`, `BlockState getSourceBlock()`.
  - `onCreatedBy(player)` → dono = UUID, modo = `FOLLOW`. Sem dono → `GUARD`.
  - `onCreatedFrom(bottom)` → `SourceBlock` = bloco de `bottom`; padrão `Blocks.BASALT`.
  - `receiveCommand(player, core)`:
    - sem dono → `PASS`;
    - outro jogador → action bar `message.dirtcheapgolems.basalt.not_yours` ("This golem isn't yours" / "Este golem não é seu"), `SUCCESS`;
    - dono em `FOLLOW` → `GUARD` + `setHomePos(blockPosition())`;
    - dono em `GUARD` → `FOLLOW` só se `getOwner()` != null;
    - action bar `message.dirtcheapgolems.basalt.mode.follow|guard` ("Following" / "Seguindo", "Guarding" / "Guardando").
  - Em `FOLLOW`, a restrição de casa fica desligada (`clearRestriction()`). Em `GUARD`, é reaplicada.
  - `getCrumbleDrop()` = `new ItemStack(sourceBlock.getBlock())` com chance `basaltDropChance()`; `crumble()` usa partículas do `sourceBlock`.
  - NBT: `Owner` (UUID), `Mode` (string), `SourceBlock` (id do bloco).

- [ ] **Step 1: GameTests que falham:**
  - `mixedBasaltDeepslatePillar` (V2-T8): `basalt` sobre `deepslate` + núcleo → `BasaltGolem` com `getOwnerId()` = jogador, `getMode()` = FOLLOW, `getSourceBlock()` = deepslate.
  - `strangerCannotChangeMode` (V2-T16): segundo jogador mock agachado dá a ordem → modo continua FOLLOW.
  - `ownerSwitchesToGuard` (V2-T17): o dono dá a ordem → GUARD e `getHomePos()` = posição atual.
  - `dropsSourceBlock` (V2-T18): criado sobre `polished_basalt`, `setCrumbleDropChanceOverride(1.0)`, `consumeIntegrity(100)` → `ItemEntity` de `polished_basalt`.
  - `eggGolemIsGuardAndIgnoresCommands` (V2-T20): spawn por ovo → GUARD, sem dono; ordem de um jogador → GUARD.
  - `ownerModeSourceSurviveReload` (parte de V2-T19).
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Todos PASS.
- [ ] **Step 5:** Commit `feat: add basalt golem with owner, shield modes and source block`.

---

### Task 8: Golem de Basalto — ímã de projéteis e golpes recebidos

**Files:**
- Modify: `entity/BasaltGolem.java`
- Test: `gametest/BasaltGolemTests.java`

**Interfaces:**
- Consumes: `GolemConfig.basaltMagnetRadius()/basaltArrowCost()/basaltSmallFireballCost()/basaltLargeFireballCost()/basaltOtherProjectileCost()/basaltMeleeHitCost()`.
- Produces:
  - `int BasaltGolem.absorbCost(Projectile p)`:
    - `AbstractArrow` (inclui tridente) → `arrowCost`;
    - `SmallFireball` → `smallFireballCost`;
    - `LargeFireball` → `largeFireballCost`;
    - outro → `otherProjectileCost`.
  - Ímã em `environmentTick` (todo tick):
    - candidatos: todo `Projectile` vivo em AABB inflada por `magnetRadius`, com distância ≤ `magnetRadius`, cujo `getOwner()` seja um `Mob` que implementa `Enemy`;
    - para cada um: `discard()` (sem `onHit`, logo sem explosão), partículas do `sourceBlock`, som `STONE_BREAK`, `consumeIntegrity(absorbCost(p))`;
    - para no meio da lista se o golem for removido.
  - Golpe recebido: override de `hurtServer` (confirmar o nome no 26.3). Se `source.getEntity()` for `LivingEntity` e `!source.is(DamageTypeTags.IS_PROJECTILE)`, chama `consumeIntegrity(meleeHitCost)` antes de delegar.

- [ ] **Step 1: GameTests que falham:**
  - `absorbsSkeletonArrow` (V2-T13): esqueleto `setNoAi`; flecha com dono = esqueleto a 1,5 bloco do golem, sem velocidade → em 2 ticks a flecha sumiu e a integridade está entre 95 e 96.
  - `ignoresPlayerArrow` (V2-T14): mesma flecha com dono = jogador mock → ainda existe após 5 ticks.
  - `absorbsGhastFireballWithoutExplosion` (V2-T15): `LargeFireball` com dono ghast a 2 blocos, movendo-se para o golem → some; os blocos do piso continuam intactos; integridade entre 84 e 85.
  - `ignoresSnowGolemSnowball` (Review Focus 5): `Snowball` com dono `SnowGolem` a 1,5 bloco → ainda existe após 5 ticks.
  - `meleeHitCostsIntegrity`: `hurtServer` com `damageSources().mobAttack(zombie)` e dano 1 → integridade entre 97 e 98.
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Todos PASS.
- [ ] **Step 5:** Commit `feat: basalt golem absorbs hostile projectiles`.

---

### Task 9: Golem de Basalto — seguir e interceptar

**Files:**
- Create: `entity/ai/FollowOwnerShieldGoal.java`, `entity/ai/InterceptThreatGoal.java`
- Modify: `entity/BasaltGolem.java`
- Test: `gametest/BasaltGolemTests.java`

**Interfaces:**
- Consumes: `getOwner`, `getMode`, `GolemConfig.basaltFollowDistance()/basaltMaxFollowDistance()/basaltThreatRange()`.
- Produces:
  - `List<Player> BasaltGolem.protectedPlayers()`: em FOLLOW, o dono (se `getOwner()` != null); em GUARD, todos os jogadores a ≤ `effectiveHomeRadius()` da casa.
  - `FollowOwnerShieldGoal(BasaltGolem)`: só em FOLLOW, com dono presente e distância ≤ `maxFollowDistance`. Navega até ficar a ≤ `followDistance`. Sem dono presente ou além do máximo → `navigation.stop()`. Nunca teleporta.
  - `InterceptThreatGoal(BasaltGolem)`, com prioridade maior que Seguir:
    - ameaça = `Mob` que seja `RangedAttackMob`, `Ghast` ou `Blaze`, a ≤ `threatRange`, cujo `getTarget()` esteja em `protectedPlayers()`; pega a mais próxima de seu alvo;
    - ponto = alvo + 1,5 × (vetor horizontal normalizado alvo→ameaça);
    - em GUARD, o ponto é limitado ao raio da casa;
    - recalcula a cada 10 ticks.

- [ ] **Step 1: GameTests que falham:**
  - `followsOwner`: piso de pedra; dono mock a 6 blocos → em ≤ 100 ticks a distância golem↔dono é ≤ 3,5.
  - `stopsWhenOwnerTooFar`: dono posicionado a 30 blocos (coordenadas absolutas, fora da arena) → após 60 ticks o golem andou < 1 bloco.
  - `ownerLeavesWhileFollowing` (Review Focus 4): `Arena.removePlayer(dono)` → após 40 ticks o golem está vivo, `getMode()` = FOLLOW, sem exceções.
  - `positionsBetweenOwnerAndSkeleton`: dono em (1,1,4), esqueleto `setNoAi` em (7,1,4) com `setTarget(dono)` → em ≤ 120 ticks o golem está com x em [2, 4] e z em [3,5; 4,5] (relativos).
- [ ] **Step 2:** FAIL. **Step 3:** Implementar. **Step 4:** Todos PASS.
- [ ] **Step 5:** Manual (`runClient`): em FOLLOW, o golem se coloca entre você e os esqueletos e as flechas somem; no Nether, uma bola de fogo de ghast é absorvida sem cratera.
- [ ] **Step 6:** Commit `feat: basalt golem follows its owner and intercepts ranged threats`.

---

### Task 10: README, changelog e verificação final

**Files:**
- Modify: `README.md`, `README.pt-BR.md`, `changelog.txt`, `src/main/resources/META-INF/mods.toml` e/ou `gradle.properties` (versão `2.0.0`, onde o projeto a define)

- [ ] **Step 1:** READMEs (en e pt-BR):
  - nova seção "Commanding golems" / "Dando ordens aos golems" com a tabela da spec v2 §3.1;
  - os 3 golems novos no mesmo formato dos da v1;
  - tags novas na tabela de configuração;
  - remover os 3 golems novos de "Roadmap" / "Próximos golems";
  - aviso destacado: "Sneak-clicking a pillar no longer creates a golem" / "Clicar agachado num pilar não cria mais golem".
- [ ] **Step 2:** `changelog.txt`: entrada 2.0.0 com os golems, o modo de comando, os ovos tingidos e a mudança do clique agachado.
- [ ] **Step 3:** `gradlew clean build test runGameTestServer` → tudo PASS; anotar a contagem de testes no commit.
- [ ] **Step 4:** `gradlew runServer` sobe e para (`stop`) sem exceções.
- [ ] **Step 5:** Executar o checklist manual da spec v2 §8.2 em `runClient`.
- [ ] **Step 6:** Commit `docs: v2 README, changelog and version bump`.
