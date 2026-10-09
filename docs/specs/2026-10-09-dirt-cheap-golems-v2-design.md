# Dirt Cheap Golems v2 — Spec de Design

- **Data:** 2026-10-09
- **Autor:** José Bieco
- **Alvo:** Minecraft 26.3 · Forge 66.0.9
- **Base:** v1 implementada conforme `docs/specs/2026-10-09-dirt-cheap-golems-design.md` (chamada aqui de "spec v1")
- **Status:** Aprovado para planejamento de implementação

---

## 1. Visão geral

A v2 adiciona os três golems que a spec v1 deixou como conceito (spec v1 §11): **End Stone**, **Cascalho** e **Basalto/Deepslate**. Eles seguem a mesma filosofia: são baratos, têm uma função ligada ao material e se desgastam até sumir.

Para viabilizá-los, a v2 também traz duas mudanças nos sistemas comuns:
- o Núcleo de Golem ganha um **modo de comando**;
- a base ganha ganchos genéricos de **criação por jogador** e de **ordem recebida**.

**Critérios de sucesso**
1. Os 3 novos golems podem ser criados com pilares de 2 blocos e o Núcleo, como na v1.
2. O End Stone recolhe itens para um baú vinculado; o Cascalho defende e enterra inimigos; o Basalto absorve projéteis hostis destinados a quem protege.
3. Os 3 golems se desgastam até desmoronar; nenhum é reparável.
4. Todos os números são configuráveis.
5. Os GameTests da seção 8 passam, e os da v1 continuam passando.

**Fora do escopo:** reparo, teleporte de golems, vínculo entre dimensões, filtros de itens no End Stone, compatibilidade explícita com mods específicos (além da capability de inventário do Forge).

---

## 2. Decisões de design (registro)

| # | Decisão | Alternativas descartadas |
|---|---|---|
| V1 | Ordens dadas com o **próprio Núcleo de Golem** (agachado) | Item novo "Cajado de Golem"; tudo automático |
| V2 | Desabamento do Cascalho coloca **cascalho real** no mundo (como blocos caindo) | Blocos temporários; só dano + itens |
| V3 | Basalto protege **o criador**, com modos **Seguir / Guardar** | Só Guardar; só Seguir |
| V4 | Interceptação = **posicionamento + ímã de projéteis** | Só posicionamento físico; escudo automático "mágico" |
| V5 | Desabamento respeita a regra `mobGriefing` e a config `leapPlacesBlocks` | Ignorar regras de griefing |
| V6 | Ovos dos golems (v1 e v2) usam o **modelo tingido único** (`golem_spawn_egg_base/overlay.png`) | Ovo desenhado individualmente por golem |

---

## 3. Mudanças nos sistemas comuns

### 3.1 Núcleo de Golem: modo de comando
O comportamento passa a depender de o jogador estar agachado (`isShiftKeyDown`/`isCrouching`, confirmar no 26.3):

| Ação | Resultado |
|---|---|
| Clique normal num bloco | **Ativação** (spec v1 §5.2), inalterada |
| Agachado + clique num bloco **com inventário** | **Memoriza** o bloco no núcleo |
| Agachado + clique num bloco sem inventário | Nada (`PASS`); **nunca** ativa golem |
| Agachado + clique num golem do mod | Entrega uma **ordem** ao golem (3.3) |
| Agachado + clique no ar | **Limpa** o vínculo memorizado, se houver |

- ⚠️ **Mudança de comportamento:** na v1, clicar agachado num pilar criava o golem. Na v2, **agachado nunca cria golem**. O README (en e pt-BR) deve ser atualizado.
- O núcleo **nunca é consumido** por comandos.
- "Bloco com inventário" = bloco cujo block entity expõe a capability de item handler do Forge (`IItemHandler`/`ForgeCapabilities.ITEM_HANDLER`, confirmar no 66.0.9). Isso cobre baús, barris, baús de shulker e inventários de outros mods.

**Vínculo memorizado no núcleo**
- Guardado como data component do item: `dirtcheapgolems:linked_container`, com valor `GlobalPos` (dimensão + posição).
- O núcleo com vínculo ganha brilho de encantamento (`enchantment_glint_override`).
- O tooltip mostra "Vinculado a: x, y, z (dimensão)".
- Como o componente fica na pilha, ele vale para todos os núcleos daquela pilha.
- Ao memorizar ou limpar, aparece uma mensagem na action bar, com tradução.

### 3.2 Gancho de criação por jogador
- `AbstractCheapGolem` ganha `protected void onCreatedBy(Player player)`, que não faz nada na base.
- `GolemActivation` chama esse método logo após spawnar o golem a partir de um pilar.
- Golems criados por ovo de spawn **não** recebem a chamada.
- `AbstractCheapGolem` também ganha `protected void onCreatedFrom(BlockState bottom)`, com o estado do bloco de baixo do pilar. O Basalto usa isso para guardar o bloco de origem (6.3).

### 3.3 Gancho de ordem
- `AbstractCheapGolem` ganha `public InteractionResult receiveCommand(Player player, ItemStack core)`, que retorna `PASS` na base.
- `GolemCoreItem` sobrescreve a interação com entidade (`interactLivingEntity`): se o jogador estiver agachado e o alvo for um `AbstractCheapGolem`, delega para `receiveCommand`.
- Toda lógica de ordem roda só no servidor.

### 3.4 Tags de material (novas)
| Tag | Conteúdo padrão | Golem |
|---|---|---|
| `#dirtcheapgolems:materials/end_stone` | `end_stone` | End Stone |
| `#dirtcheapgolems:materials/gravel` | `gravel` | Cascalho |
| `#dirtcheapgolems:materials/basalt` | `basalt`, `polished_basalt`, `smooth_basalt`, `deepslate`, `cobbled_deepslate` | Basalto |

Pilares mistos **dentro da mesma tag** são válidos (ex.: basalto sobre deepslate), como na v1.

### 3.5 Ovos de spawn (modelo tingido)
- Um único modelo `models/item/golem_spawn_egg.json`, com `layer0 = item/golem_spawn_egg_base` e `layer1 = item/golem_spawn_egg_overlay`.
- Os PNGs já existem e são gerados por `tools/gen_spawn_egg.py`.
- Cada `items/<golem>_spawn_egg.json` aponta para esse modelo, com `tints` do tipo `minecraft:constant` (confirmar o formato no 26.3).
- Os PNGs e modelos individuais antigos dos ovos são removidos.

| Golem | Casca | Manchas |
|---|---|---|
| Terra | `#866043` | `#6A9C3A` |
| Areia | `#E3D7A4` | `#A88A4C` |
| Netherrack | `#6F3534` | `#FF9A2E` |
| End Stone | `#DBDE9E` | `#9B5FC0` |
| Cascalho | `#857F7E` | `#3D3A3A` |
| Basalto | `#4E4D52` | `#9AA0A8` |

---

## 4. Arquitetura (adições)

```
com.josebieco.dirtcheapgolems
├── item/GolemCoreItem.java          # + modo de comando (3.1, 3.3)
├── registry/ModDataComponents.java  # linked_container (GlobalPos)
├── entity/
│   ├── AbstractCheapGolem.java      # + onCreatedBy, onCreatedFrom, receiveCommand
│   ├── EndStoneGolem.java
│   ├── GravelGolem.java
│   ├── BasaltGolem.java
│   ├── ShieldMode.java              # enum FOLLOW, GUARD
│   ├── ai/
│   │   ├── CollectItemsGoal.java
│   │   ├── GravelArtilleryGoal.java
│   │   ├── GravelLeapGoal.java
│   │   ├── FollowOwnerShieldGoal.java
│   │   └── InterceptThreatGoal.java
│   └── projectile/FlintShard.java
└── golem/GolemType.java             # + END_STONE, GRAVEL, BASALT
```

O princípio da v1 se mantém: a base não conhece golems específicos. As únicas mudanças na base são os 3 ganchos genéricos de 3.2 e 3.3.

---

## 5. Configuração (novas chaves em `dirtcheapgolems-common.toml`)

| Seção | Chave | Padrão | Faixa | Descrição |
|---|---|---|---|---|
| end_stone | `linkRange` | 32 | 1–128 | Distância máxima baú↔casa |
| end_stone | `itemsPerIntegrity` | 4 | 1–64 | Itens teleportados por −1 de integridade |
| end_stone | `pickupRange` | 1.5 | 0.5–8 | Distância para teleportar um item |
| end_stone | `flySpeed` | 0.4 | 0.05–2 | Velocidade de voo |
| end_stone | `dropChance` | 0.5 | 0–1 | Chance de dropar End Stone |
| gravel | `artilleryRange` | 16 | 1–64 | Alcance do tiro |
| gravel | `artilleryCooldown` | 60 | 1–1200 | Ticks entre tiros |
| gravel | `shotCost` | 3 | 0–100 | Integridade por tiro |
| gravel | `shardDamage` | 3.0 | 0–100 | Dano do Estilhaço |
| gravel | `slownessTicks` | 60 | 0–12000 | Duração de Lentidão I |
| gravel | `leapTriggerRange` | 2.5 | 0.5–8 | Distância que dispara o salto |
| gravel | `leapBelowHorizontal` | 2 | 0–8 | Alcance horizontal do salto "para baixo" |
| gravel | `leapBelowDepth` | 6 | 1–32 | Profundidade máxima do salto "para baixo" |
| gravel | `leapImpactDamage` | 4.0 | 0–100 | Dano de impacto do salto |
| gravel | `leapBlocks` | 4 | 1–9 | Blocos de cascalho gerados no desabamento |
| gravel | `leapPlacesBlocks` | true | — | Se false, o cascalho vira itens |
| gravel | `dropChance` | 0.5 | 0–1 | Chance de dropar cascalho no desmoronar normal |
| basalt | `followDistance` | 2.5 | 1–8 | Distância de acompanhamento do dono |
| basalt | `maxFollowDistance` | 24 | 4–128 | Além disso, para e espera |
| basalt | `threatRange` | 16 | 1–64 | Alcance para detectar atiradores |
| basalt | `magnetRadius` | 2.5 | 0.5–8 | Raio do ímã |
| basalt | `arrowCost` | 4 | 0–100 | Flecha / tridente |
| basalt | `smallFireballCost` | 6 | 0–100 | Bola de fogo pequena |
| basalt | `largeFireballCost` | 15 | 0–100 | Bola de fogo do ghast |
| basalt | `otherProjectileCost` | 4 | 0–100 | Outros projéteis hostis |
| basalt | `meleeHitCost` | 2 | 0–100 | Golpe corpo a corpo recebido |
| basalt | `dropChance` | 0.5 | 0–1 | Chance de dropar o bloco de origem |

---

## 6. Golems da v2

### 6.1 Golem de End Stone — "Logístico" (`dirtcheapgolems:end_stone_golem`)
**Corpo:** vida 10, hitbox 0,6 × 0,9, **sem gravidade**, voo lento (navegação de voo e controle de movimento de voo, como o Allay). Imune a dano de queda. Paira 0,5 a 1,5 bloco acima do chão quando ocioso.

**Vínculo (ordem via 3.3)**
- `receiveCommand`: se o núcleo tiver `linked_container`, o golem tenta se vincular a ele. O vínculo é aceito se:
  - o baú está na mesma dimensão do golem;
  - a distância do baú até a casa é ≤ `linkRange`;
  - o bloco ainda tem inventário.
- Aceito: grava `LinkedContainer` (GlobalPos) em NBT, emite partículas de portal entre golem e baú e mostra a mensagem "Vinculado".
- Recusado: mostra uma mensagem com o motivo ("Muito longe da casa", "Outra dimensão", "Sem inventário"). O vínculo anterior é mantido.
- Núcleo sem vínculo: nada (`PASS`).

**Comportamento (`CollectItemsGoal`)**
1. Só fica ativo com vínculo válido.
2. A cada 20 ticks, procura o `ItemEntity` mais próximo dentro do raio da casa que **caiba** no inventário vinculado. A checagem é uma simulação de inserção; itens que não cabem são ignorados.
3. Voa até ficar a ≤ `pickupRange` do item.
4. Insere o máximo possível via capability, a sobra fica como `ItemEntity`. Emite partículas `PORTAL` no item e no baú e o som `ENDERMAN_TELEPORT` em volume baixo.
5. **Custo:** um contador interno (`ItemsCarried`, salvo em NBT) soma os itens inseridos; a cada `itemsPerIntegrity`, −1 de integridade.
6. Itens com atraso de coleta (recém-jogados pelo jogador) são ignorados até o atraso acabar.

**Falhas**
| Situação | Comportamento |
|---|---|
| Baú cheio | Não procura itens que não cabem; sem custo |
| Bloco do vínculo quebrado ou sem inventário | Limpa o vínculo, emite partículas `SMOKE` e fica ocioso |
| Chunk do baú descarregado | Pausa a meta; mantém o vínculo |

**Ambiente:** nenhuma fraqueza.

**Desmoronar:** partículas `PORTAL` (em vez de partículas do bloco) e 50% de chance de dropar 1 `end_stone`.

### 6.2 Golem de Cascalho — "Artilheiro de Cerco" (`dirtcheapgolems:gravel_golem`)
**Corpo:** vida 16, hitbox 0,7 × 0,9, velocidade 0,15, resistência a repulsão 0,6. Fica restrito à casa, como na v1.

**Artilharia (`GravelArtilleryGoal`)**
- Alvo: hostil (`Enemy`) com linha de visão a até `artilleryRange`. Entre os candidatos, prefere o que estiver mais abaixo e, em caso de empate, o mais próximo.
- A cada `artilleryCooldown` ticks, dispara um **`FlintShard`**: projétil arremessado com aparência do item sílex e trajetória balística.
- **Efeito do `FlintShard`:**
  - dano `shardDamage`;
  - Lentidão I por `slownessTicks`;
  - atravessa jogadores, golems e mobs não hostis, como o `SandBlast` corrigido da v1;
  - ao acertar um bloco, some com partículas de cascalho.
- **Custo:** −`shotCost` de integridade por disparo.

**Desabamento (`GravelLeapGoal`, prioridade maior que a artilharia)**
- **Gatilho:** um hostil atende a uma destas condições:
  - está a ≤ `leapTriggerRange`;
  - está abaixo do golem, com distância horizontal ≤ `leapBelowHorizontal` e queda ≤ `leapBelowDepth`.
- **Salto:** impulso calculado para cair sobre o alvo.
- **Impacto:** acontece ao tocar o alvo ou ao aterrissar a até 1,5 bloco dele.
  1. Aplica `leapImpactDamage` ao alvo.
  2. Gera até `leapBlocks` blocos de cascalho:
     - 1 na coluna do alvo, 1 bloco acima da cabeça;
     - os demais nas colunas adjacentes (N/S/L/O, em ordem aleatória), na mesma altura.
  3. Os blocos são gerados como **blocos em queda**. Pela física vanilla, eles caem, atravessam o alvo e se assentam no espaço dele, enterrando-o com dano de sufocamento.
  4. O golem é removido **sem drop**, porque o cascalho já é o resultado. Partículas e som de cascalho.
- **Regras de segurança:**
  - um bloco só é gerado se a posição for **ar**; nunca substitui blocos;
  - se `mobGriefing = false` **ou** `leapPlacesBlocks = false`, nenhum bloco é gerado e o golem dropa `leapBlocks` itens de cascalho no ponto de impacto;
  - posições que não são ar são puladas, sem procurar outras.

**Desmoronar normal** (integridade ou vida a zero): 50% de chance de dropar 1 `gravel`.

**Ambiente:** nenhuma fraqueza (o custo vem dos tiros e do salto).

### 6.3 Golem de Basalto/Deepslate — "Escudo" (`dirtcheapgolems:basalt_golem`)
**Corpo:** vida 30, hitbox 0,8 × 1,2, velocidade 0,25, resistência a repulsão 0,8. **Não ataca.**

**Bloco de origem:** `onCreatedFrom` grava o bloco de baixo do pilar em NBT (`SourceBlock`, id do bloco). Golem criado por ovo usa `basalt`. A textura é a mesma para todos (uma só textura "basalto/deepslate" no MVP da v2).

**Dono e modos**
- `onCreatedBy(player)` grava o UUID do dono (`Owner`) em NBT.
- `ShieldMode` sincronizado com o cliente e salvo em NBT (`Mode`). Começa em `FOLLOW` se houver dono; senão, `GUARD`.
- `receiveCommand`:
  - só o dono troca o modo, e outros jogadores recebem a mensagem "Este golem não é seu";
  - `FOLLOW` → `GUARD`: a casa passa a ser a posição atual;
  - `GUARD` → `FOLLOW`: só se o dono estiver online e na mesma dimensão;
  - golem sem dono ignora o comando;
  - a mudança aparece numa mensagem na action bar.
- **Protegidos:**
  - em `FOLLOW`, só o dono;
  - em `GUARD`, todos os jogadores dentro do raio da casa.

**Seguir (`FollowOwnerShieldGoal`, só em FOLLOW)**
- Mantém cerca de `followDistance` do dono, sem a restrição de casa.
- Se o dono estiver a mais de `maxFollowDistance`, offline ou em outra dimensão, o golem para. **Não teleporta.**

**Posicionamento (`InterceptThreatGoal`, prioridade maior que Seguir)**
- **Ameaça** = mob hostil que ataca à distância (`RangedAttackMob`, `Ghast` ou `Blaze`), a até `threatRange`, cujo alvo atual é um protegido.
- Escolhe a ameaça mais próxima de qualquer protegido.
- **Ponto de interceptação** = protegido + 1,5 bloco na direção da ameaça (no plano horizontal).
- O golem anda até esse ponto e recalcula a cada 10 ticks.
- Em `GUARD`, o ponto fica limitado ao raio da casa.

**Ímã de projéteis** (verificado a cada tick no servidor)
- Afeta todo `Projectile` dentro de `magnetRadius` do golem cujo dono seja um mob `Enemy` (inclui Ghast). Projéteis sem dono ou de jogador **nunca** são afetados.
- O projétil é **absorvido**: `discard()` sem chamar o impacto. A bola de fogo do ghast **não explode**.
- Efeitos: partículas do bloco de origem e som de pedra quebrando.
- **Custo:**

  | Projétil | Config |
  |---|---|
  | Flecha / tridente | `arrowCost` |
  | Bola de fogo pequena | `smallFireballCost` |
  | Bola de fogo grande | `largeFireballCost` |
  | Outros projéteis hostis | `otherProjectileCost` |

- O ímã funciona em ambos os modos, mesmo sem ameaça detectada.

**Dano corpo a corpo recebido:** cada golpe de entidade viva custa `meleeHitCost` de integridade, além do dano normal na vida.

**Ambiente:** nenhuma fraqueza.

**Desmoronar:** 50% de chance de dropar 1 do bloco de origem (`SourceBlock`).

---

## 7. Cliente e assets

- **Modelo:**
  - End Stone e Cascalho reutilizam o `CheapGolemModel`;
  - o Basalto usa o mesmo modelo escalado em ~1,3× no renderer (hitbox 0,8 × 1,2), sem modelo novo.
- **Texturas:** `end_stone_golem.png`, `gravel_golem.png` e `basalt_golem.png` (64×64), geradas por `tools/gen_textures.py` com as paletas dos blocos vanilla.
- **End Stone:** o renderer adiciona uma leve oscilação vertical (bobbing) e partículas `PORTAL` ocasionais no cliente.
- **`FlintShard`:** renderizado como item arremessado (sílex).
- **Rachaduras:** as mesmas overlays da v1.
- **Traduções** `en_us`/`pt_br`:
  - nomes dos golems, ovos e `FlintShard`;
  - mensagens de vínculo e de modo;
  - tooltip do núcleo vinculado.

---

## 8. Testes

### 8.1 GameTests automatizados
| ID | Cenário | Resultado esperado |
|---|---|---|
| V2-T1 | Agachado + núcleo num pilar de terra | Nenhum golem; blocos intactos; núcleo intacto |
| V2-T2 | Agachado + núcleo num baú | Núcleo com `linked_container` = posição do baú |
| V2-T3 | Agachado + núcleo vinculado em End Stone a ≤ 32 da casa | Golem vinculado; núcleo não consumido |
| V2-T4 | Vincular baú a > `linkRange` da casa | Vínculo recusado; golem sem vínculo |
| V2-T5 | End Stone vinculado + 8 itens de pedra no chão | Após ≤ 400 ticks: 8 no baú, nenhum `ItemEntity`, integridade −2 (tolerância da passiva) |
| V2-T6 | Baú cheio + itens no chão | Itens continuam no chão; integridade sem perda por itens |
| V2-T7 | Quebrar o baú vinculado | Vínculo limpo; golem continua vivo; sem exceção |
| V2-T8 | Pilar basalto + deepslate | Golem de Basalto criado |
| V2-T9 | Cascalho + zumbi (sem IA) a 10 blocos, visível | Zumbi com Lentidão I; integridade −3 |
| V2-T10 | Cascalho + zumbi a 2 blocos, arena aberta | Golem removido; ≥ 1 bloco `gravel` na coluna do zumbi após assentar; zumbi recebeu dano |
| V2-T11 | Igual a V2-T10 com `mobGriefing=false` | Nenhum `gravel` colocado; `leapBlocks` itens de cascalho no chão |
| V2-T12 | Igual a V2-T10 com bloco sólido acima do zumbi | O bloco sólido continua lá (nada substituído) |
| V2-T13 | Basalto + flecha com dono esqueleto passando a 1,5 bloco | Flecha removida; integridade −4 |
| V2-T14 | Basalto + flecha com dono jogador passando a 1,5 bloco | Flecha não removida |
| V2-T15 | Basalto + bola de fogo grande com dono ghast entrando no raio | Removida sem explosão (blocos intactos); integridade −15 |
| V2-T16 | Outro jogador (não dono) dá a ordem | Modo inalterado |
| V2-T17 | Dono dá a ordem em FOLLOW | Modo = GUARD; casa = posição atual |
| V2-T18 | Basalto criado com `polished_basalt` embaixo, integridade → 0 com `dropChance=1` | Dropa `polished_basalt` |
| V2-T19 | Salvar e recarregar End Stone e Basalto | `LinkedContainer`, `ItemsCarried`, `Owner`, `Mode` e `SourceBlock` preservados |
| V2-T20 | Basalto sem dono (ovo) | Modo GUARD; ordem ignorada |

Todos os GameTests da v1 devem continuar passando. Atenção a `ActivationTests`, por causa da mudança de agachar.

### 8.2 Checklist manual
- [ ] Ovos dos 6 golems aparecem tingidos com as cores da 3.5.
- [ ] Núcleo vinculado mostra brilho e tooltip.
- [ ] End Stone flutua, recolhe itens de uma fazenda do Golem de Terra e mostra partículas de portal.
- [ ] Cascalho no alto de uma muralha atira em zumbis e enterra um que encosta no muro.
- [ ] Basalto em FOLLOW se posiciona entre o dono e esqueletos; flechas somem antes de acertar.
- [ ] Bola de fogo de ghast no Nether é absorvida sem cratera.
- [ ] Mensagens de vínculo e de modo em pt-BR.
- [ ] README atualizado: comandos do núcleo, os 3 golems novos e a mudança do clique agachado.

---

## 9. Fases de entrega

5. **Base da v2:**
   - modo de comando do núcleo, data component e ganchos `onCreatedBy`/`onCreatedFrom`/`receiveCommand`;
   - novas tags e chaves de config;
   - migração dos ovos da v1 para o modelo tingido;
   - atualização do README.

   Testes V2-T1, V2-T2 e regressão da v1.
6. **Golem de End Stone.** Testes V2-T3 a V2-T7 e parte de V2-T19.
7. **Golem de Cascalho.** Testes V2-T9 a V2-T12.
8. **Golem de Basalto.** Testes V2-T8, V2-T13 a V2-T18, V2-T20 e parte de V2-T19.

---

## 10. Riscos conhecidos

| Risco | Impacto | Mitigação |
|---|---|---|
| Mudança no clique agachado quebra o hábito de jogadores da v1 | Confusão | Documentar no README e no changelog; agachar num pilar continua sendo um `PASS` silencioso |
| APIs de capability/data component diferentes no 66.0.9 | Não compila | Confirmar no MDK antes de usar (mesma regra da v1) |
| Ímã absorve projéteis hostis que não iam acertar ninguém | Desgaste "desperdiçado" | Aceito por design; `magnetRadius` configurável |
| Cascalho enterra mobs do lado de dentro da base | Bloco no meio da base | Intencional; documentado no README; `leapPlacesBlocks=false` desliga |
| Cascalho gerado sobre plantações ou caminhos | Bagunça | Só gera em ar; respeita `mobGriefing` |
| Basalto em FOLLOW fica para trás quando o dono se afasta ou muda de dimensão | Golem "perdido" | Comportamento documentado (para e espera); sem teleporte por design |
| End Stone varrendo muitos itens em fazendas grandes | Lag | Busca a cada 20 ticks, só o item mais próximo, raio limitado à casa |
| Itens jogados pelo jogador sendo sugados na hora | Frustração | Respeitar o atraso de coleta do `ItemEntity` |
| Bola de fogo do ghast absorvida no Nether pode tornar ghasts triviais | Balanceamento | `largeFireballCost=15` (≈ 6 absorções por golem); ajustável |
