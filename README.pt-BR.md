# Dirt Cheap Golems

![Dirt Cheap Golems Banner](docs/assets/banner.jpg)

[English](README.md)

> Transforme os blocos que ninguém quer (terra, areia, netherrack, cascalho, basalto, end stone) em pequenos golems servos. Eles trabalham, se desgastam, e você constrói mais.

**Minecraft 26.3 · Forge 66.0.9 · v2.0.0**

Todo mundo de sobrevivência acaba com baús cheios de terra, areia, netherrack, cascalho, basalto e pedra do End. O Dirt Cheap Golems dá um emprego a esses blocos: dois blocos e um núcleo barato viram um pequeno golem que cuida da plantação, defende ou ilumina a sua base. O porém: **os golems são descartáveis**. Eles se desgastam ao trabalhar, com o passar do tempo e quando o clima não ajuda, e por isso você sempre volta a precisar de mais blocos.

---

## Núcleo de Golem

O único item craftável do mod. É ele que dá vida a um pilar de blocos.

![Receita de Crafting](docs/assets/crafting.png)

| | | |
|:-:|:-:|:-:|
| | Bola de Argila | |
| Bola de Argila | Pó de Redstone | Bola de Argila |
| | Bola de Argila | |

**Rende 2 Núcleos de Golem.**

## Como criar um golem

1. Empilhe **dois blocos do mesmo material**, um em cima do outro.
2. Use um **Núcleo de Golem** em qualquer um dos dois blocos.
3. O pilar some e o golem aparece onde estava o bloco de baixo. Esse ponto vira a **casa** dele.

- Pilares mistos (ex.: terra em cima de areia) não fazem nada, e o núcleo não é gasto. Use o clique normal: **agachado nunca cria golem** (veja [Dando ordens aos golems](#dando-ordens-aos-golems)).
- No modo criativo, o núcleo não é consumido.
- Quem não pode construir naquele lugar (modo aventura, proteção de spawn) não consegue criar golems.

| Material | Golem |
|---|---|
| Terra, Terra Grossa | Golem de Terra |
| Areia, Areia Vermelha | Golem de Areia |
| Netherrack | Golem de Netherrack |
| End Stone | Golem de End Stone |
| Cascalho | Golem de Cascalho |
| Basalto, Basalto Polido, Basalto Liso, Deepslate, Deepslate Lapidado (misturáveis) | Golem de Basalto |

## Dando ordens aos golems

O Núcleo de Golem também tem um **modo de comando**, usado **agachado**:

| Ação | Resultado |
|---|---|
| Clicar num bloco (sem agachar) | **Ativação**: cria um golem a partir de um pilar |
| Agachado + clicar num bloco **com inventário** (baú, barril, caixa de shulker, armazenamento de outros mods) | **Vincula** o bloco ao núcleo (brilho de encantamento e tooltip mostram o alvo) |
| Agachado + clicar em qualquer outro bloco | Nada. Nunca cria golem |
| Agachado + clicar num golem | Dá uma **ordem** ao golem (ex.: troca o modo do Golem de Basalto) |
| Agachado + clicar no ar | **Limpa** o vínculo |

> **Mudou na 2.0.0:** clicar agachado num pilar **não cria mais golem**. Use o clique normal.

Comandos nunca consomem o núcleo. O vínculo fica na pilha de itens, então vale para todos os núcleos daquela pilha. A receita do Núcleo de Golem também aparece no livro de receitas.

---

## Os golems

### Golem de Terra: o Lavrador

![Golem de Terra](docs/assets/dirt_golem.jpg)

Anda em volta da casa procurando **plantações maduras** (trigo, cenoura, batata, beterraba), colhe e **replanta** usando uma semente dos próprios drops. O resto fica no chão para você recolher. Ele nunca pisoteia a terra arada.

- **Desgaste:** 1 de integridade por colheita.
- **Fraqueza:** **derrete na chuva** (−2 de integridade/s) e na água (−4/s). Quando começa a chover, procura um lugar coberto por perto e para de colher até a chuva passar.
- **Ao desmoronar:** 50% de chance de dropar 1 Terra.

### Golem de Areia: a Torre

![Golem de Areia](docs/assets/sand_golem.jpg)

Uma torre de defesa fixa. **Nunca se move** e não sofre repulsão. Dispara rajadas de areia em **mobs hostis** que esteja vendo, a até 12 blocos. As rajadas atravessam jogadores, aldeões e outros mobs pacíficos.

Cada acerto causa 1 de dano e aplica:
- **Lentidão II** por 4 s.
- **Areia nos Olhos** por 3 s: o mob **perde o alvo** e o alcance de perseguição cai **75%**.

- **Desgaste:** 2 de integridade por tiro (cerca de 45 tiros).
- **Fraqueza:** nenhuma. É o golem mais resistente; o preço está em cada tiro.
- **Ao desmoronar:** 50% de chance de dropar 1 Areia.

### Golem de Netherrack: o Kamikaze Térmico

![Golem de Netherrack](docs/assets/netherrack_golem.jpg)

Uma **luz ambulante** (nível de luz 12) que patrulha a sua base. Quando um mob hostil chega a até 10 blocos, ele corre, abraça o mob e, depois de um chiado curto, libera uma **explosão térmica**: 4 de dano de fogo e 5 s em chamas para todo mob hostil a até 3 blocos.

- A explosão **nunca quebra blocos** nem põe fogo no mundo, e nunca fere jogadores, golems ou mobs pacíficos.
- O golem é imune a fogo e lava.
- **Desgaste:** 34 de integridade por explosão. **Três explosões e ele vira cinzas.**
- **Fraqueza:** se tocar em **água ou chuva**, vira na hora um bloco de **Pedra**.
- **Ao desmoronar:** 1 Carvão Vegetal.

### Golem de End Stone: o Coletor

![Golem de End Stone](docs/assets/end_stone_golem.jpg)

Um golem **voador** que paira de 0,5 a 1,5 bloco acima do chão quando está parado. Vincule um contêiner a um núcleo (agachado + clicar num baú) e crie o golem com um núcleo da mesma pilha: ele **coleta itens caídos dentro do raio da casa e os teleporta para o contêiner vinculado**.

- O contêiner precisa estar a até **32 blocos da casa do golem**, na mesma dimensão.
- Ignora itens que ainda têm atraso de coleta (como os que um jogador acabou de jogar).
- Se o contêiner for quebrado, o vínculo é limpo e ele para de coletar.
- **Desgaste:** 1 de integridade a cada **4 itens**.
- **Ao desmoronar:** 50% de chance de dropar 1 End Stone.

### Golem de Cascalho: a Artilharia

![Golem de Cascalho](docs/assets/gravel_golem.jpg)

Uma peça de artilharia móvel. Arremessa **estilhaços de sílex** em **mobs hostis** a até **16 blocos**, preferindo alvos **mais baixos** que ele (muralhas e torres são o território dele). Os estilhaços atravessam jogadores, aldeões e outros mobs não hostis.

Cada acerto causa 3 de dano e aplica **Lentidão I** por 3 s.

- **Salto:** quando um hostil chega perto, ou está abaixo e ao alcance, o golem **salta** sobre ele e o **enterra sob cascalho caindo**. O cascalho só é colocado em ar e respeita a regra `mobGriefing` e a opção `leapPlacesBlocks`; caso contrário, solta itens de cascalho.
- **Desgaste:** 3 de integridade por tiro.
- **Ao desmoronar:** 50% de chance de dropar 1 Cascalho.

### Golem de Basalto / Deepslate: o Escudo

![Golem de Basalto](docs/assets/basalt_golem.jpg)

Um escudo vivo para o **dono**, o jogador que o criou. Monte um pilar com qualquer mistura de basalto, basalto polido, basalto liso, deepslate ou deepslate lapidado. Golems de ovo de spawn não têm dono e ficam em GUARD.

Dois modos, alternados por **agachado + núcleo no golem** (**só o dono** consegue):
- **FOLLOW:** segue o dono e se posiciona para interceptar ameaças à distância apontadas para ele.
- **GUARD:** mantém a posição perto da casa.

Ele **absorve projéteis de mobs hostis** que chegam a até 2,5 blocos. Custo por projétil: flecha ou tridente 4, bola de fogo pequena 6, bola de fogo de ghast 15 (**não** explode), qualquer outro 4. Um golpe corpo a corpo custa 2.

- **Ao desmoronar:** 50% de chance de dropar o **bloco de origem** (o de baixo do pilar).

---

## Integridade: por que os golems não duram

Cada golem tem **integridade** (0–100), separada da vida.

- Ela **só diminui**: ao trabalhar, pela fraqueza do golem e com o tempo (−1 a cada 30 s, cerca de 50 minutos parado).
- Cura e poções recuperam a vida, **nunca** a integridade. Não existe reparo.
- **Rachaduras** aparecem no golem com 66 e com 33 de integridade.
- Com integridade 0, ou quando a vida acaba, o golem **desmorona** e pode dropar parte do material.

Os golems ficam a até **16 blocos da casa** e nunca atacam jogadores, outros golems ou mobs pacíficos.

## Configuração

Todos os números de balanceamento estão em `config/dirtcheapgolems-common.toml`, com limites seguros: desgaste passivo, raio da casa, custo de cada ação, alcances, dano, duração dos efeitos e chances de drop.

Quais blocos criam cada golem é definido por tags de bloco, alteráveis por datapack:

| Tag | Padrão |
|---|---|
| `#dirtcheapgolems:materials/dirt` | `dirt`, `coarse_dirt` |
| `#dirtcheapgolems:materials/sand` | `sand`, `red_sand` |
| `#dirtcheapgolems:materials/netherrack` | `netherrack` |
| `#dirtcheapgolems:materials/end_stone` | `end_stone` |
| `#dirtcheapgolems:materials/gravel` | `gravel` |
| `#dirtcheapgolems:materials/basalt` | `basalt`, `polished_basalt`, `smooth_basalt`, `deepslate`, `cobbled_deepslate` |

---

## Desenvolvimento

O Gradle precisa do JDK 21+ para iniciar; o build usa um toolchain Java 25 (baixado automaticamente). No Windows, use `gradlew.bat`.

| Comando | O que faz |
|---|---|
| `gradlew build` | Compila e gera o jar |
| `gradlew test` | Testes JUnit (lógica pura) |
| `gradlew runData` | Datagen: traduções e instâncias de GameTest em `src/generated/resources` |
| `gradlew runGameTestServer` | Roda todos os GameTests num servidor dedicado; sai com erro se algum falhar |
| `gradlew runClient` / `gradlew runServer` | Cliente / servidor dedicado de desenvolvimento |

**Rode `gradlew runData` antes de `runGameTestServer` sempre que um GameTest for criado ou renomeado.** Veja [`gametest/README.md`](src/main/java/com/josebieco/dirtcheapgolems/gametest/README.md) para entender como os testes são registrados.

As texturas provisórias são geradas por `python tools/gen_textures.py`.

Specs de design: [v1](docs/specs/2026-10-09-dirt-cheap-golems-design.md), [v2](docs/specs/2026-10-09-dirt-cheap-golems-v2-design.md)

## Autor

José Eduardo Saroba Bieco

## Licença

[MIT](LICENSE.txt)
