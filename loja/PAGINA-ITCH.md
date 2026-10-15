# Página da itch.io — Aventura em Aurora

Tudo o que a página precisa está nesta pasta. Imagens geradas por `Capturas.java` (veja o fim do arquivo).

| Campo da itch.io | Valor |
|---|---|
| Título | Aventura em Aurora |
| Endereço sugerido | `aventura-em-aurora` |
| Tipo | Downloadable · Game |
| Gênero | Adventure (secundário: Role Playing / Simulation) |
| Preço | "No payments" para o lançamento, ou "Pay what you want" com mínimo 0 |
| Capa | `capa.png` (630 × 500) |
| Ícone / avatar | `icone.png` (512 × 512) |
| Capturas | `capturas/*-pt.png` ou `*-en.png` (1280 × 960). Ordem sugerida: 10, 02, 12, 01, 09, 11, 04, 03, 07, 05, 06, 08 |
| Plataformas | Windows, macOS, Linux (pacotes portáteis, Java embutido) |
| Tags | pixel-art, cozy, exploration, story-rich, fishing, farming, cooking, singleplayer, 2d, relaxing, top-down, wholesome |
| Idiomas | Português (Brasil), English |
| Entradas | Keyboard, Xbox controller, Playstation controller |
| Acessibilidade | Remapeamento de teclas, texto grande, reduzir movimento, velocidade do texto, jogável sem som (notas em cores) |
| Tempo médio | 6 a 10 horas na campanha; mais com coleções |

## Frase curta (tagline)
- **PT:** Uma pequena história de exploração entre um farol, uma vila e as estrelas.
- **EN:** A small exploration story between a lighthouse, a village and the stars.

## Descrição (português)

**Aventura em Aurora** é um jogo de exploração tranquilo, em pixel art, sobre uma vila à beira-mar, um farol apagado e uma amizade que começa no céu.

Tudo começa com uma carta da Mamãe. Logo você está devolvendo a luz ao farol, decifrando o mapa de uma capitã, velejando até uma ilha de estrelas e ajudando a **Cintila**, uma estrelinha que caiu na montanha, a voltar para casa.

**O que você encontra em Aurora**
- **9 capítulos** de história, com quebra-cabeças de cenário, campos de gelo, braseiros, um jardim de constelações, uma baleia que canta e uma floresta antiga sob o eclipse.
- **Uma vila viva**: moradores com rotinas, conversas, aniversários, pedidos e cinco corações de amizade.
- **Estações do ano** que mudam a paisagem, os peixes, as flores, a música e o festival de sábado.
- **Pesca** com álbum de 8 espécies e **12 peixes raros**, **10 insetos** para pegar com a rede, **8 fósseis** para desenterrar, um **museu** para completar, um **barco** para navegar até o alto-mar e ilhas pequenas, **jardinagem** no quintal, **culinária** com receitas ensinadas pelos amigos e uma **casa** para decorar.
- **Segredos**: 12 conchas escondidas, uma corrente de trocas, enigmas opcionais e um observatório para restaurar.
- **Cartas** que chegam pela manhã, um **diário** com dicas em camadas e um **Cartão do Explorador** com 30 conquistas.
- Um **rádio** em casa com as músicas de cada região e o jornal do dia (clima, festas, aniversários e dicas).
- Música e efeitos **sintetizados por código**, com trilhas de dia, de noite e de cada estação.

**Acessível de verdade**: teclas remapeáveis, controle, texto grande, velocidade do texto, opção de reduzir movimento e minijogos que mostram as notas em cores para jogar sem som.

Feito com carinho, inspirado em Pokémon de Game Boy, Zelda, Harvest Moon, Stardew Valley, Animal Crossing e EarthBound.

## Description (English)

**Aventura em Aurora** is a cozy pixel-art exploration game about a seaside village, a dark lighthouse and a friendship that begins in the sky.

It all starts with a letter from Mom. Soon you're bringing light back to the lighthouse, piecing together a captain's map, sailing to an island of stars and helping **Cintila**, a little star who fell on the mountain, find her way home.

**What you'll find in Aurora**
- **9 story chapters** with environmental puzzles, ice fields, braziers, a constellation garden, a singing whale and an ancient forest under an eclipse.
- **A living village**: villagers with routines, chats, birthdays, requests and five hearts of friendship.
- **Seasons** that change the landscape, the fish, the flowers, the music and the Saturday festival.
- **Fishing** with an 8-species album and **12 rare fish**, **10 bugs** to catch with a net, **8 fossils** to dig up, a **museum** to complete, a **boat** to sail to the open sea and little islands, **gardening** in your backyard, **cooking** with recipes taught by friends, and a **home** to decorate.
- **Secrets**: 12 hidden shells, a trading chain, optional puzzles and an observatory to restore.
- **Letters** that arrive in the morning, a **journal** with layered hints and an **Explorer Card** with 30 achievements.
- A **radio** at home with every region's music and the daily news (weather, festivals, birthdays and tips).
- Music and sound effects **synthesized in code**, with day, night and seasonal tracks.

**Truly accessible**: remappable keys, controller support, large text, text speed, reduced motion, and minigames that show notes in colors so you can play without sound.

Made with care, inspired by Game Boy Pokémon, Zelda, Harvest Moon, Stardew Valley, Animal Crossing and EarthBound.

## Controles / Controls
| Ação | Teclado | Controle |
|---|---|---|
| Mover | Setas / WASD | Direcional / analógico esquerdo |
| Falar, escolher (segurar na pesca) | Z, Enter, Espaço | A |
| Voltar / pausa | Esc | B / Start |
| Correr | Shift | RT |
| Itens · Cartão · Diário | I · C · L | X · Y · RB |
| Lancheira · Mapa · Flores | K · Tab · V | LB · Select · LT |
| Tela cheia · Foto | F11 · F12 | — |

## Enviar os arquivos
1. Gere os pacotes portáteis: o GitHub faz isso sozinho a cada envio para o `main` (aba **Actions** › execução mais recente › **Artifacts**: `AventuraEmAurora-windows`, `-mac`, `-linux`). Localmente: `sh empacotar.sh --portatil` gera o do seu sistema.
2. Na página do jogo na itch.io, em **Uploads**, envie cada zip e marque a plataforma correspondente.
3. (Opcional) Envio automático: crie uma chave em itch.io › Settings › API keys, salve no GitHub como segredo `BUTLER_API_KEY` e crie a variável `ITCH_PROJECT` com `seu-usuario/aventura-em-aurora`. A partir daí, cada envio para o `main` atualiza os três canais (`windows`, `mac`, `linux`).
4. macOS: o pacote não é assinado; na primeira vez o jogador abre com clique direito › Abrir. Vale avisar na página.

## Regerar as imagens
```
sh jogar.sh --compilar
java -Djava.awt.headless=true -Daurora.dados=$(mktemp -d) -cp ".build" Capturas
```
