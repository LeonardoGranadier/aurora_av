# Estudo de design: o que Aurora aprende com os clássicos

Aventura em Aurora é um jogo de exploração em grade, visto de cima, com história em capítulos, vida
de vila e atividades de coleção. É a mesma família de Pokémon (Game Boy), Zelda 2D, Harvest Moon,
Stardew Valley, Animal Crossing, EarthBound e Golden Sun. Este documento resume o que faz cada um
funcionar, a lição para Aurora e o que já virou código. ✅ indica feito; ⏳ indica ideia para o futuro.

---

## 1. Pokémon Red/Blue e Gold/Silver (Game Boy)

**O que funciona**
- **Caminho principal com desvios recompensados.** A Floresta de Viridian tem um caminho crítico
  estreito e becos sem saída com itens: ensina, sem dizer, que explorar compensa.
- **Áreas que se abrem com a história** (corte, surfe, força) dão sensação de progresso e motivam a
  **voltar a lugares conhecidos**, que agora revelam segredos novos.
- **Relógio e dias da semana (Gold/Silver).** Pokémon diferentes de dia e de noite, eventos semanais
  (Concurso de Insetos), personagens que só aparecem em certos dias.
- **Pokégear.** Mapa, rádio e telefone: os personagens **ligam** com dicas, revanches e avisos.
  O mundo parece vivo mesmo longe deles.
- **Cartão de treinador.** Nome, tempo de jogo, dinheiro e insígnias num só lugar: um retrato do seu
  progresso para mostrar aos amigos.
- **Escolha do personagem** ("Você é menino ou menina?") e o **nome** digitado no início criam vínculo.

**Em Aurora**
- ✅ Nome e tratamento escolhidos no novo jogo (`Explorer`), usados nas falas; opção "Prefiro não dizer"
  com texto neutro. Partidas antigas recebem o texto neutro.
- ✅ **Dias da semana** no HUD; o festival cai sempre no **sábado**; o **mascate** aparece só aos
  **domingos** na praia (`TradeQuest`).
- ✅ **Cartas** que chegam pela manhã depois de eventos, com dicas e presentes (`Mailbox`). É o papel do
  telefone do Pokégear.
- ✅ **Cartão do Explorador** (tecla C): nome, tempo de jogo, dia, capítulo, moedas, álbum, regiões,
  conchas e 16 medalhas de conquista.
- ✅ **Correr com Shift**, como os tênis de corrida.
- ✅ **Itens escondidos**: conchas secretas dentro de árvores e pedras, com brilho fraco quando se passa
  perto (`SecretShells`).
- ⏳ Rádio: estações com música e notícias da vila (o sintetizador de `Audio` já existe).

## 2. The Legend of Zelda: Link's Awakening e A Link to the Past

**O que funciona**
- **Secret Seashells.** 26 conchas escondidas na ilha, com pistas de geometria do cenário; a cada
  quantidade, um prêmio, e o maior deles é poderoso.
- **Sequência de trocas.** Um boneco Yoshi vira laço, vira comida de cachorro, vira bananas... cada troca
  apresenta um personagem e leva o jogador a revisitar regiões. Em Link's Awakening ela é obrigatória.
- **Dicas em camadas.** Estátuas de coruja, lajes de pedra (que precisam de um fragmento para serem lidas)
  e as **cabines telefônicas** do velho Ulrira dão pistas quando o jogador trava.
- **"Item get".** Link ergue o item sobre a cabeça e toca uma fanfarra. É o momento mais memorável da
  série: o jogo **para** para celebrar a conquista, e itens importantes ganham fanfarra maior.
- **Masmorras com chave e cadeado (ALttP)** permitem caminhos não lineares e um retorno motivado ao
  baú grande.

**Em Aurora**
- ✅ **Conchas secretas** (12): o Seu Marinho troca cada três por um prêmio. O primeiro é a **Concha
  que Escuta**, que mostra o brilho de longe e conta no Mapa de Aurora quantas faltam em cada região
  (o detector de itens dos Pokémon, também inspirado na crítica ao Minish Cap abaixo).
- ✅ **Corrente de trocas** de seis passos: mascate → Lenhador → Montanhista → Eremita → Capitã →
  Prof. Carvalho. Ela atravessa os capítulos e termina na Chave do Observatório. As trocas nunca roubam
  a vez de uma conversa da história principal.
- ✅ **Pose de "item obtido"**: o jogador vira para a tela e ergue o item com raios de brilho, em itens da
  história, conchas e trocas.
- ✅ Fins de capítulo com título e pista do próximo passo, em vez de "Obrigado por jogar".
- ⏳ **Blocos empurráveis** e puzzles de chave na Gruta do Eco e no Templo (o campo de gelo já é um puzzle
  à moda de Pokémon).
- ⏳ Coruja ou "concha do eco" com dicas em camadas para quem ficar travado (vaga → específica).

## 3. The Minish Cap (Kinstones)

**O que funciona**: fundir Kinstones com moradores transforma conversa à toa em **troca com
consequência**. A câmera mostra o efeito em outro lugar do mapa, o que desperta curiosidade.
**O que falhou**: faltava um **registro** de quem ainda tinha fusões e um contador do que restava.

**Em Aurora**
- ✅ Contadores por região (Concha que Escuta, no mapa) e a pista da troca atual sempre visível no
  Cartão do Explorador: o jogador nunca precisa adivinhar o que falta.
- ⏳ Mostrar com uma pequena animação de câmera a mudança na vila depois de cada doação ao observatório.

## 4. Harvest Moon e Stardew Valley

**O que funciona**
- **Ciclo diário** com escolhas: plantar, pescar, conversar, presentear. Cada dia pede uma coisa diferente.
- **Correio** pela manhã: receitas, pedidos, histórias.
- **Centro Comunitário (pacotes):** um objetivo de longo prazo que obriga a provar **todas** as
  atividades do jogo. Cada sala restaurada **muda o mundo** de forma permanente. Os pacotes foram
  pensados para não serem repetitivos, e sim para mostrar a variedade do jogo.
- **Aniversários e preferências** dos moradores.

**Em Aurora**
- ✅ **Observatório do avô do professor**: quatro conjuntos que pedem peixes da vila, flores do quintal,
  conchas e moedas, e a chave da corrente de trocas. Cada um muda a vila: **lanternas** à noite,
  **canteiros floridos**, uma **luneta pública** na praia e, por fim, a tela de **constelações**.
  Doações pedem confirmação e são desfeitas se o save falhar.
- ✅ Cartas com moedas anexas e dicas (Capitã, Mamãe, moradores).
- ✅ **Estações do ano** (28 dias): paisagem, peixes favorecidos, flores por estação, neve no inverno,
  festival com nome da estação e calendário com festivais, mascate e aniversários (como o de Stardew).
- ✅ **Aniversários** dos moradores: aviso, fala especial e presentes em dobro.
- ✅ **Culinária**: receitas ensinadas pelos moradores conforme a amizade, pratos com efeitos de um dia
  (como os buffs de comida de Stardew) e pratos favoritos de cada morador.

## 5. Animal Crossing

**O que funciona**: tempo que corre de verdade, rotina tranquila **sem punição**, **cartas** dos
vizinhos, **museu** com o curador que comenta cada doação e **fósseis diários** que dão um motivo para
voltar todo dia.

**Em Aurora**
- ✅ Nada se perde por demora: plantas pausam sem água, encomendas expiram sem penalidade e as cartas
  chegam no ritmo de duas por dia.
- ⏳ "Achado do dia": um objeto escondido sorteado por dia (ex.: concha comum na praia) para vender ou doar.

## 6. EarthBound (Mother 2)

**O que funciona**: falas **com personalidade e humor**. Os NPCs reagem ao que você faz, comentam
coisas estranhas e quebram a expectativa. O save é um telefonema para o pai, e a saudade de casa tem efeito.

**Em Aurora**
- ✅ Personagens novos com voz própria: o mascate que "troca coisas por histórias" e o Eremita que
  escreve "Rapaz, moça, quem quer que seja".
- ⏳ Falas que mudam conforme o clima, o dia da semana e as conquistas (ex.: Lucas provoca se você
  perdeu nos sinos).

## 7. Golden Sun

**O que funciona**: poderes usados também **fora de combate** para resolver quebra-cabeças do cenário
(mover pilares, apagar fogo, crescer plantas). Cada poder novo dá um novo olhar para mapas antigos.

**Em Aurora**
- ✅ A Lanterna (cap. 4) já amplia a luz em lugares escuros; o Cristal de Calor derrete o portão.
- ⏳ Usar os itens de história em puzzles opcionais: a Lanterna revelando símbolos na gruta, o Cristal
  derretendo gelo que esconde conchas, a Concha Brilhante "ouvindo" passagens secretas.

## 8. "Juice": sensação ao jogar

Pequenas respostas ao que o jogador faz, no décimo de segundo seguinte, fazem um jogo simples parecer
caprichado. Os Zelda são o exemplo clássico com o som e a pose ao abrir baús.

**Em Aurora**
- ✅ Texto em **máquina de escrever** (velocidade configurável: instantânea, rápida, normal;
  o primeiro toque mostra a página inteira).
- ✅ **Balão "Z"** sobre o que pode ser usado à frente do jogador (e "~" na água quando há vara).
- ✅ Bandeira vermelha na caixa de correio quando há carta nova.
- ✅ Avisos de **conquista**, fanfarra nas doações e nas recompensas.
- ✅ **Modo foto** (F12) e **créditos** ao fim do capítulo 5 com o resumo da jornada.

---

## Fontes consultadas

- RPGFan — *Pokémon Red & Blue as beginner-friendly RPGs*: https://www.rpgfan.com/feature/dungeon-crawlers-2-pokemon-red-blue-as-beginner-friendly-rpgs/
- Wikipedia — *Pokémon Gold and Silver*: https://en.wikipedia.org/wiki/Pok%C3%A9mon_Gold_and_Silver
- Bulbapedia — *Pokégear*: https://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9gear
- Wikipedia — *The Legend of Zelda: Link's Awakening*: https://en.wikipedia.org/wiki/The_Legend_of_Zelda:_Link%27s_Awakening
- Nintendo Life — *Link's Awakening trading sequence*: https://www.nintendolife.com/guides/zelda-links-awakening-trading-sequence-all-trade-items-and-locations
- TheGamer — *Secret Seashell rewards*: https://www.thegamer.com/links-awakening-secret-seashell-location-rewards-guide/
- Zelda Wiki — *Owl Statue*, *Stone Slab*, *Telephone Booth*: https://zeldawiki.wiki/wiki/Owl_Statue · https://zeldawiki.wiki/wiki/Stone_Slab · https://zeldawiki.wiki/wiki/Telephone_Booth
- Zelda Dungeon — *How Kinstones reimagined NPC interactivity*: https://zeldadungeon.net/?p=223573
- Nintendo Life — *The Minish Cap's worst sidequest isn't the Kinstones*: https://www.nintendolife.com/features/opinion-no-zelda-the-minish-caps-worst-sidequest-isnt-the-kinstones
- Boris the Brave — *Lock and Key Dungeons*: https://www.boristhebrave.com/2021/02/27/lock-and-key-dungeons
- Zelda Dungeon — *Rod of Seasons*: https://www.zeldadungeon.net/wiki/Rod_of_Seasons
- TV Tropes — *Item Get*: https://www.tvtropes.org/pmwiki/pmwiki.php/Main/ItemGet
- Designing Sound — *Juicy*: https://designingsound.org/2017/11/19/sunday-sound-thought-96-juicy/
- Stardew Valley — *Dev update 19* (Community Center) e wiki de *Bundles*: https://stardewvalley.net/dev-update-19/ · https://stardewvalleywiki.com/Bundles
- Wikipedia — *Animal Crossing*: https://en.wikipedia.org/wiki/Animal_Crossing
- Wikipedia — *EarthBound*: https://en.wikipedia.org/wiki/EarthBound
- Game Developer — *Pilgrimage through the classics: EarthBound*: https://gamedeveloper.com/design/pilgrimage-through-the-classics-earthbound
- Golden Sun Universe — *Psynergy Training Grounds*: https://goldensunwiki.net/wiki/Psynergy_Training_Grounds
