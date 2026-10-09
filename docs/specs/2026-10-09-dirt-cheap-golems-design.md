# Dirt Cheap Golems — Spec de Design

- **Data:** 2026-10-09
- **Autor:** José Bieco
- **Alvo:** Minecraft 26.3 · Forge 66.0.9
- **Status:** Aprovado para planejamento de implementação (MVP)

---

## 1. Visão geral

**Conceito:** dar utilidade contínua a blocos abundantes e pouco úteis (terra, areia, netherrack, etc.) transformando-os em pequenos golems servos. Cada golem tem **uma função ligada ao seu material** e **uma forma de se desgastar**. Eles são baratos e temporários: o jogador precisa construir novos constantemente, o que gera consumo recorrente desses blocos.

**Critérios de sucesso do MVP**
1. O jogador consegue criar os 3 golems do MVP (Terra, Areia, Netherrack) com materiais baratos.
2. Cada golem executa sua função de forma útil e perceptível.
3. Todo golem eventualmente se desfaz; nenhum golem é permanente.
4. Todos os números de balanceamento são configuráveis sem recompilar.
5. Os GameTests automatizados da seção 9 passam.

**Fora do escopo do MVP:** reparo de golems, item de comando/cajado, posse por jogador, compatibilidade explícita com outros mods, golems da Fase 4 (seção 11).

---

## 2. Decisões de design (registro)

| # | Decisão | Alternativas descartadas |
|---|---|---|
| D1 | MVP = Terra + Areia + Netherrack; demais golems ficam como fase futura | Os 6 golems de uma vez; só Terra |
| D2 | Criação via item **Núcleo de Golem** usado em um pilar de 2 blocos | Estrutura estilo Iron Golem com abóbora; receita direta para item de spawn |
| D3 | Desgaste via **Integridade** (0–100), separada da vida, que cai por uso e por tempo | Só gatilhos; usar a própria vida (HP) |
| D4 | **Sem reparo.** Integridade só desce | Reparo parcial/total com o bloco |
| D5 | **Casa = posição de criação**, raio configurável | Cajado de comando; sem casa |
| D6 | Arquitetura: classe base abstrata + uma subclasse por golem | Entidade única data-driven; sistema de componentes |
| D7 | Netherrack em contato com água/chuva vira **`minecraft:stone`** (evita exploit de obsidiana barata) | Virar obsidiana; bloco-estátua customizado |
| D8 | "Cegueira" da Areia implementada como **efeito customizado** que afeta a IA de mobs (Cegueira vanilla quase não afeta mobs) | Usar `Blindness` vanilla |

---

## 3. Identidade técnica

- **Mod id:** `dirtcheapgolems`
- **Pacote raiz:** `com.josebieco.dirtcheapgolems`
- **Nomes:** usar os nomes oficiais da Mojang (o Minecraft 26.x não é ofuscado). Versão de Java: a exigida pelo MDK do Forge 66.0.9.
- **Idiomas:** `en_us` e `pt_br`.
- **Lado:** lógica de jogo no servidor; o cliente só renderiza e mostra partículas. Deve funcionar em singleplayer e em servidor dedicado.

> **Nota para a implementação:** as APIs do Forge e do Minecraft mudam entre versões. Antes de usar qualquer classe ou evento citado neste spec, verifique o nome e a assinatura corretos no MDK do Forge 66.0.9. Os nomes aqui descrevem a **intenção**, não garantem a assinatura exata.

---

## 4. Arquitetura

```
com.josebieco.dirtcheapgolems
├── DirtCheapGolems.java            # @Mod, registra tudo
├── registry/
│   ├── ModEntities.java            # tipos de entidade + atributos
│   ├── ModItems.java               # golem_core
│   ├── ModEffects.java             # sand_in_eyes
│   └── ModTags.java                # tags de materiais
├── config/
│   └── GolemConfig.java            # ForgeConfigSpec COMMON (seção 8)
├── item/
│   └── GolemCoreItem.java          # useOn → GolemActivation
├── golem/
│   ├── GolemType.java              # enum: DIRT, SAND, NETHERRACK (tag + EntityType)
│   └── GolemActivation.java        # valida pilar, consome, spawna
├── entity/
│   ├── AbstractCheapGolem.java     # integridade, casa, desmoronar, rachaduras
│   ├── DirtGolem.java
│   ├── SandGolem.java
│   ├── NetherrackGolem.java
│   ├── ai/
│   │   ├── HarvestAndReplantGoal.java
│   │   ├── SeekShelterFromRainGoal.java
│   │   ├── StationaryTurretGoal.java
│   │   └── KamikazeChargeGoal.java
│   └── projectile/
│       └── SandBlast.java
├── effect/
│   └── SandInEyesEffect.java
├── client/
│   ├── CheapGolemModel.java        # modelo único compartilhado
│   ├── CheapGolemRenderer.java     # textura por tipo + overlay de rachadura
│   └── SandBlastRenderer.java
└── datagen/                        # receitas, tags, lang, modelos de item
```

**Princípio:** a `AbstractCheapGolem` não conhece nenhum golem específico. As subclasses só adicionam suas metas de IA e seus gatilhos ambientais. Os golems da Fase 4 devem entrar como novas subclasses, sem alterar a base.

---

## 5. Sistemas comuns

### 5.1 Núcleo de Golem (`dirtcheapgolems:golem_core`)
- **Receita (shaped), rende 2:**
  ```
  . C .
  C R C
  . C .
  ```
  `C` = bola de argila (`minecraft:clay_ball`), `R` = pó de redstone.
- Empilha até 64.

### 5.2 Ativação (`GolemActivation`)
1. O jogador usa o núcleo (clique direito) em um bloco.
2. O bloco clicado (topo) e o bloco **diretamente abaixo** precisam pertencer à **mesma** tag de material (seção 5.3). O jogador também pode clicar no bloco de baixo: nesse caso, o sistema verifica o bloco de cima.
3. Se for válido (somente no servidor):
   - remove os 2 blocos (sem drops);
   - consome 1 núcleo (exceto no criativo);
   - spawna o golem centralizado na posição do bloco de baixo;
   - grava a **casa** = posição do bloco de baixo;
   - toca um som (reutilizar som vanilla, ex.: colocação do bloco do material) e emite partículas do bloco.
4. Se for inválido: nada acontece e o item não é consumido. Retorna `PASS` para não bloquear outras interações.
5. Um pilar misto (ex.: terra em cima de areia) **não** ativa.

### 5.3 Tags de material
| Tag | Conteúdo padrão | Golem |
|---|---|---|
| `#dirtcheapgolems:materials/dirt` | `dirt`, `coarse_dirt` | Terra |
| `#dirtcheapgolems:materials/sand` | `sand`, `red_sand` | Areia |
| `#dirtcheapgolems:materials/netherrack` | `netherrack` | Netherrack |

Modpacks podem alterar as tags via datapack.

### 5.4 Integridade
- Valor inteiro de **0 a 100**, começando em 100. **Separada da vida (HP).**
- Sincronizada com o cliente (`SynchedEntityData`) e salva em NBT (`Integrity`).
- **Perda passiva:** −1 a cada `passiveDecayTicks` ticks (padrão 600 = 30 s, ≈ 50 min de vida ociosa).
- **Perda por ação:** cada golem define seus custos (seção 6).
- **Perda ambiental:** cada golem define seus gatilhos (seção 6).
- **Nunca aumenta.** Não existe reparo (D4). Poções e regeneração afetam só a vida, nunca a integridade.
- **Integridade 0 ⇒ desmoronar** (5.6).

### 5.5 Rachaduras visuais
O renderer aplica overlays conforme a integridade:
| Integridade | Estágio |
|---|---|
| 67–100 | sem rachadura |
| 34–66 | rachadura leve |
| 1–33 | rachadura pesada |

Mesma ideia do Iron Golem vanilla, mas baseada na integridade, não na vida.

### 5.6 Desmoronar
Acontece quando a integridade chega a 0 **ou** quando a vida chega a 0.
- Emite partículas do bloco do material e o som de quebra do bloco.
- Dropa o item específico do golem (seção 6) com a chance configurada.
- Remove a entidade. Não dropa XP.
- Subclasses podem sobrescrever o resultado (ex.: Netherrack → cinzas/pedra).

### 5.7 Casa
- `BlockPos` salvo em NBT (`HomePos`).
- O golem fica restrito a um raio de `homeRadius` (padrão 16) em torno da casa.
- Se estiver fora do raio, a prioridade da IA passa a ser voltar.
- Se a casa estiver em outro chunk descarregado ou inacessível, o golem simplesmente fica parado (sem teleporte).

### 5.8 Regras gerais
- Base: estender `AbstractGolem` (vanilla). Não despawnam naturalmente.
- **Nunca** miram jogadores, golems do mod, Iron Golems, Snow Golems nem mobs passivos.
- "Hostil" = qualquer `Enemy`/`Monster`. Creepers estão incluídos.
- Hitbox ≈ 0,6 × 0,9 (pequeno).
- Atributos base: vida 10 (5 corações), velocidade 0,25, sem dano corpo a corpo, exceto quando indicado.

---

## 6. Golems do MVP

### 6.1 Golem de Terra — "Lavrador" (`dirtcheapgolems:dirt_golem`)
**Função:** colher e replantar plantações maduras dentro do raio da casa.

**Comportamento (`HarvestAndReplantGoal`)**
1. A cada `harvestScanInterval` ticks (padrão 40), procura a `CropBlock` madura (idade máxima) mais próxima dentro do raio da casa, limitada a `harvestScanRadius` (padrão 8) a partir da posição atual.
2. Anda até ficar adjacente à planta.
3. Quebra a planta e gera os drops normais (loot table vanilla).
4. Se entre os drops houver o item de semente/plantio correspondente, consome 1 e replanta na idade 0 no mesmo farmland. Se não houver, não replanta.
5. O restante dos drops fica no chão (sinergia futura com o Golem de End Stone).
6. Custo: −`harvestCost` (padrão 1) de integridade por colheita.
7. Não pisoteia farmland (garantir que andar e cair sobre farmland não o converta em terra).

**Plantações suportadas no MVP:** trigo, cenoura, batata, beterraba (qualquer subclasse de `CropBlock`).

**Gatilho ambiental: chuva e água**
- Exposto à chuva (`level.isRainingAt(pos acima)`): −`rainDecayPerSecond` (padrão 2) de integridade por segundo.
- Dentro da água: o dobro (padrão 4/s).
- Enquanto chove, `SeekShelterFromRainGoal` tem prioridade sobre colher: busca uma posição coberta dentro do raio da casa (lógica análoga a `FleeSunGoal`).
- Sem abrigo, derrete em ≈ 50 s.

**Desmoronar:** chance `dropChance` (padrão 0,5) de dropar 1 `minecraft:dirt`.

### 6.2 Golem de Areia — "Torre" (`dirtcheapgolems:sand_golem`)
**Função:** torre de defesa fixa que atrasa e desorienta inimigos.

**Comportamento (`StationaryTurretGoal`)**
- **Nunca se move:** velocidade 0, `knockbackResistance` = 1,0, sem metas de movimento. Pode girar a cabeça e o corpo para mirar.
- Alvo: o hostil mais próximo em até `turretRange` (padrão 12) blocos, **com linha de visão**.
- Dispara 1 `SandBlast` a cada `turretCooldown` ticks (padrão 40 = 2 s).
- Custo: −`shotCost` (padrão 2) de integridade por disparo, ≈ 45 tiros.
- Se cair (o bloco abaixo for removido), cai normalmente pela gravidade da entidade.

**Projétil `SandBlast`**
- Trajetória reta/levemente balística, com partículas de areia (`FALLING_DUST` com o bloco de areia).
- Ao acertar uma entidade viva:
  - dano `sandDamage` (padrão 1);
  - Lentidão II por `slownessTicks` (padrão 80 = 4 s);
  - efeito **Areia nos Olhos** por `sandInEyesTicks` (padrão 60 = 3 s).
- Ao acertar um bloco: some com partículas.
- Não fere jogadores nem golems (ignorar colisão com eles).

**Efeito "Areia nos Olhos" (`dirtcheapgolems:sand_in_eyes`)** (D8)
- `MobEffect` do tipo prejudicial, com ícone próprio.
- Enquanto ativo em um mob:
  - modificador de atributo `FOLLOW_RANGE` em −75% (multiplicativo);
  - ao ser aplicado, o mob perde o alvo atual (`setTarget(null)`);
  - o alvo só pode ser readquirido dentro do alcance reduzido.
- Afeta apenas mobs; jogadores nunca são alvo do Golem de Areia.

**Gatilho ambiental:** nenhum. É o golem "durável" do trio; o custo vem dos tiros e do tempo.

**Desmoronar:** chance `dropChance` (padrão 0,5) de dropar 1 `minecraft:sand`.

### 6.3 Golem de Netherrack — "Kamikaze Térmico" (`dirtcheapgolems:netherrack_golem`)
**Função:** luz ambulante e defesa suicida contra hostis.

**Luz ambulante**
- Enquanto vivo, mantém um bloco `minecraft:light` (nível `lightLevel`, padrão 12) na sua posição de cabeça, se ela estiver em ar.
- Ao mudar de bloco: remove a luz anterior e coloca a nova.
- Nunca substitui blocos que não sejam ar ou o próprio bloco de luz que ele colocou.
- **Limpeza obrigatória:** a posição da luz atual é salva em NBT (`LightPos`). A luz é removida ao morrer, ao virar pedra, ao ser descarregado (`remove`) e, ao carregar, qualquer `LightPos` salvo é limpo antes de colocar uma nova. Objetivo: zero blocos de luz órfãos.

**Combate (`KamikazeChargeGoal`)**
1. Vaga no raio da casa até detectar um hostil em até `kamikazeRange` (padrão 10).
2. Corre até o alvo (velocidade 1,3× a base).
3. Ao encostar (distância ≤ 1,5), inicia um **pavio** de `fuseTicks` (padrão 30 = 1,5 s): fica parado, emite partículas de fogo/fumaça e o som de chiado (reutilizar o som do pavio do creeper).
4. Se o alvo se afastar além de 3 blocos durante o pavio, cancela o pavio e volta a perseguir.
5. **Explosão térmica** (não é uma `Explosion` vanilla):
   - raio `blastRadius` (padrão 3);
   - todas as entidades vivas elegíveis no raio recebem `blastDamage` (padrão 4) de dano de fogo e ficam em chamas por `burnSeconds` (padrão 5);
   - **não** destrói blocos e **não** coloca fogo;
   - **não** afeta jogadores nem golems (configurável: `blastHurtsPlayers`, padrão `false`);
   - partículas de chama e lava, som de fogo.
6. Custo: −`blastCost` (padrão 34) de integridade, ≈ 3 explosões.
7. É imune a dano de fogo e lava.

**Gatilho ambiental: chuva ou água** (D7)
- Ao ser exposto à chuva **ou** tocar água: **instantaneamente** remove sua luz, coloca `minecraft:stone` na posição dos pés (só se for ar ou um bloco substituível; se não for possível, apenas desmorona) e é removido.
- Partículas de vapor (`CLOUD`) e som de extinção (`fire.extinguish`).
- Não dropa itens nesse caso; a pedra é o resultado.

**Desmoronar normal** (integridade 0 por explosão ou tempo, ou vida 0): vira cinzas, com partículas de fumaça e dropa 1 `minecraft:charcoal` com chance `dropChance` (padrão 1,0).

---

## 7. Cliente e assets

- **Modelo:** um único `CheapGolemModel` para os 3 tipos. É um golem pequeno e atarracado: corpo, cabeça e dois braços grossos, sem pernas separadas ou com pernas curtas. Animações simples de andar e balançar os braços.
- **Texturas:** `textures/entity/{dirt,sand,netherrack}_golem.png` (64×64). Na primeira versão, podem ser derivadas visualmente das texturas dos blocos vanilla (arte original/placeholder do autor).
- **Overlays de rachadura:** `textures/entity/cracks_{low,high}.png`, compartilhados.
- **Netherrack:** camada emissiva (olhos e rachaduras brilhando) usando render type emissivo.
- **Item:** `golem_core` com textura própria 16×16.
- **Projétil:** `SandBlast` renderizado como pequeno item/partícula (pode usar `ThrownItemRenderer` com um item interno ou só partículas).
- **Ovos de spawn** para os 3 golems (úteis para testes e criativo).

---

## 8. Configuração (`config/dirtcheapgolems-common.toml`)

| Seção | Chave | Padrão | Descrição |
|---|---|---|---|
| general | `passiveDecayTicks` | 600 | Ticks por −1 de integridade passiva |
| general | `homeRadius` | 16 | Raio da casa |
| dirt | `harvestCost` | 1 | Integridade por colheita |
| dirt | `harvestScanInterval` | 40 | Ticks entre buscas |
| dirt | `harvestScanRadius` | 8 | Raio de busca a partir do golem |
| dirt | `rainDecayPerSecond` | 2 | Perda na chuva (água = 2×) |
| dirt | `dropChance` | 0.5 | Chance de dropar terra |
| sand | `turretRange` | 12 | Alcance |
| sand | `turretCooldown` | 40 | Ticks entre tiros |
| sand | `shotCost` | 2 | Integridade por tiro |
| sand | `sandDamage` | 1.0 | Dano do projétil |
| sand | `slownessTicks` | 80 | Duração da Lentidão II |
| sand | `sandInEyesTicks` | 60 | Duração do Areia nos Olhos |
| sand | `dropChance` | 0.5 | Chance de dropar areia |
| netherrack | `lightLevel` | 12 | Nível da luz ambulante (0–15) |
| netherrack | `kamikazeRange` | 10 | Alcance de detecção |
| netherrack | `fuseTicks` | 30 | Duração do pavio |
| netherrack | `blastRadius` | 3.0 | Raio da explosão |
| netherrack | `blastDamage` | 4.0 | Dano |
| netherrack | `burnSeconds` | 5 | Segundos em chamas |
| netherrack | `blastCost` | 34 | Integridade por explosão |
| netherrack | `blastHurtsPlayers` | false | Explosão fere jogadores |
| netherrack | `dropChance` | 1.0 | Chance de dropar carvão vegetal |

Valores devem ter limites (`defineInRange`) para evitar configurações inválidas.

---

## 9. Testes

### 9.1 GameTests automatizados (Forge GameTest)
| ID | Cenário | Resultado esperado |
|---|---|---|
| T1 | Pilar de 2 terras + núcleo | Golem de Terra spawna; os 2 blocos somem; 1 núcleo consumido |
| T2 | Pilar terra + areia + núcleo | Nada acontece; núcleo não é consumido |
| T3 | Golem com integridade 1 + 1 ciclo de perda passiva | Golem removido |
| T4 | Trigo maduro ao lado do Golem de Terra | Trigo colhido; trigo idade 0 replantado; integridade −1 |
| T5 | Golem de Terra em água | Integridade cai no ritmo dobrado |
| T6 | Golem de Areia + zumbi a 8 blocos com linha de visão | Zumbi recebe Lentidão II e Areia nos Olhos; integridade −2 |
| T7 | Golem de Areia + zumbi atrás de parede | Nenhum disparo |
| T8 | Golem de Netherrack + zumbi próximo | Após o pavio: zumbi em chamas e com dano; integridade −34; nenhum bloco destruído |
| T9 | Golem de Netherrack colocado em água | Vira `minecraft:stone`; nenhum bloco `light` restante na área |
| T10 | Golem de Netherrack morto/removido | Nenhum bloco `light` restante |
| T11 | Golem salvo e recarregado (NBT) | Integridade e casa preservadas |

### 9.2 Checklist manual
- [ ] Rachaduras aparecem nos estágios corretos.
- [ ] Golem de Terra procura abrigo quando começa a chover.
- [ ] Golem de Terra não pisoteia o farmland.
- [ ] Golem de Areia não se move ao ser empurrado ou atingido.
- [ ] Mobs com Areia nos Olhos perdem o alvo de forma visível.
- [ ] Luz do Netherrack acompanha o golem sem piscar excessivamente.
- [ ] Golems nunca atacam o jogador.
- [ ] Funciona em servidor dedicado (sem crash por classe de cliente no servidor).
- [ ] Traduções `pt_br` e `en_us` completas.

---

## 10. Fases de entrega

Cada fase deve compilar, rodar e passar seus testes antes da próxima.

1. **Fase 1: Base + Golem de Terra.** Setup do projeto, registros, config, Núcleo + ativação, `AbstractCheapGolem` (integridade, casa, desmoronar, rachaduras), modelo/renderer compartilhado, `DirtGolem` completo, datagen. Testes T1–T5, T11.
2. **Fase 2: Golem de Areia.** `SandGolem`, `SandBlast`, efeito Areia nos Olhos. Testes T6–T7.
3. **Fase 3: Golem de Netherrack.** `NetherrackGolem`, luz ambulante com limpeza, kamikaze, conversão em pedra. Testes T8–T10.
4. **Fase 4 (futuro, sem implementação agora):** seção 11. Cada golem precisará de um mini-spec próprio antes de ser implementado.

---

## 11. Golems futuros (conceito, não implementar no MVP)

Descrições preservadas da ideia original para orientar o design futuro. Os números serão definidos em specs próprios.

### 11.1 Golem de End Stone — "Manipulador Logístico"
- Vinculado a um baú via item específico.
- Flutua lentamente pela área sugando itens do chão e teleportando-os para o baú vinculado, mesmo através de paredes.
- **Desgaste:** cada item teleportado consome uma fração da integridade. Após algumas centenas de itens, desaparece em partículas de portal.
- **Sinergia:** recolhe os drops deixados pelo Golem de Terra.
- **Questões em aberto:** item de vínculo (provavelmente reaproveitar a ideia de cajado), limite de distância do baú, comportamento quando o baú está cheio, contagem por item vs. por stack.

### 11.2 Golem de Cascalho — "Artilheiro de Cerco"
- Pesado e lento, posicionado no topo de muralhas.
- Atira pedaços de si mesmo (sílex) em inimigos abaixo, causando lentidão.
- Se um monstro chegar muito perto, pula sobre ele, causando dano de sufocamento ("enterra" o monstro).
- **Desgaste:** cada tiro custa integridade; o pulo é fatal e ele desmorona em 3–4 blocos de cascalho espalhados.
- **Questões em aberto:** risco de griefing ao colocar cascalho no mundo; se os blocos devem ser colocados ou só dropados.

### 11.3 Golem de Basalto / Deepslate — "Escudo Móvel"
- Não ataca. Segue o jogador ou guarda um ponto, posicionando-se entre o protegido e as ameaças para interceptar flechas e bolas de fogo (Esqueletos, Ghasts, Blazes).
- **Desgaste:** cada impacto interceptado lasca o golem; não se regenera.
- **Questões em aberto:** exige noção de "dono"/"protegido" (hoje fora do escopo); cálculo de posição de interceptação; interação com projéteis de jogadores.

---

## 12. Riscos conhecidos

| Risco | Impacto | Mitigação |
|---|---|---|
| APIs do Forge 66/MC 26.3 diferentes do esperado | Código não compila | Verificar no MDK antes de usar cada API (seção 3) |
| Cegueira vanilla não afeta mobs | Efeito da Areia inútil | Efeito customizado Areia nos Olhos (D8) |
| Blocos de luz órfãos do Netherrack | Luz "fantasma" permanente no mundo | Rastreamento de `LightPos` em NBT + limpeza em todos os caminhos de remoção (T9, T10) |
| Colheita em loop consumindo muitos ticks em fazendas grandes | Lag | Intervalo de busca configurável; buscar só a planta mais próxima; raio limitado |
| Golem de Terra pisoteando farmland | Destrói a fazenda que deveria cuidar | Impedir o pisoteio explicitamente (checklist manual) |
| Explosão do Netherrack ferir aliados | Frustração do jogador | Filtro de alvos + `blastHurtsPlayers=false` |
| Balanceamento (golems fortes ou fracos demais) | Mod irrelevante ou quebrado | Todos os números em config; ajustar após playtest |
