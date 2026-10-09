# Aventura em Aurora — caminho até o lançamento

Prioridades: **P0** impede o lançamento · **P1** deixa o jogo competitivo · **P2** extras que encantam.
✅ feito · ⏳ a fazer · 🧑 precisa de você (decisão, pessoa ou conta externa).
As ideias vindas dos clássicos estão explicadas em [ESTUDO-DE-DESIGN.md](ESTUDO-DE-DESIGN.md).

## 1. Preparar para publicar (P0)

- ✅ Pacote distribuível (`sh empacotar.sh`: jar, .zip, lançadores Linux/Windows; `--instalador` com Java embutido).
- ✅ Saves e preferências na pasta de dados do usuário na versão distribuída.
- ✅ Janela ampliada, redimensionável, tela cheia (F11, lembrada) e ícone.
- ✅ `testar.sh` roda toda a suíte; `.gitignore` sem compilados nem saves pessoais.
- ✅ Atalho T (+2h) só no modo desenvolvedor (`-Daurora.dev=true`; `jogar.sh` liga).
- ✅ Textos de fim de capítulo corrigidos (sem "Obrigado por jogar (Continua...)").
- ✅ Créditos com versão no título e rolagem ao fim do capítulo 5 (`Credits.java`; edite `AUTHOR`).
- ✅ `erros.log` na pasta de dados com aviso amigável; erros repetidos são gravados uma vez.
- ✅ Backup do save (`savegame.properties.bak`) recuperado automaticamente se o principal estiver ilegível.
- ✅ Nome e tratamento do jogador no novo jogo; as 4 falas com "(a)" agora seguem a escolha (ou ficam neutras).
- 🧑 **Arte final.** Os sprites ainda são provisórios (`MakeAssets.java`). É o que mais pesa na página da loja.
  Basta trocar os PNGs de `assets/` mantendo os nomes. Sprites novos que o jogo já aceita: `mascate`
  (folha 3x4 de personagem), `observatorio`, `luneta`, `caixa_correio` (hoje desenhados por código).
- 🧑 **Teste com jogadores reais**: 5 a 10 pessoas que nunca viram o jogo, só observando onde travam.
- ⏳ Assinatura de código dos instaladores (Windows/macOS mostram aviso sem ela). 🧑 exige certificado.

## 2. Qualidade de vida (P1)

- ✅ Texto em máquina de escrever com velocidade configurável (instantânea/rápida/normal).
- ✅ Indicador de interação ("Z" sobre o que está à frente; "~" na água com a vara).
- ✅ Correr segurando Shift.
- ✅ Dias da semana no HUD.
- ⏳ **Controle/gamepad.** Java não lê controles sem biblioteca nativa (SDL via LWJGL ou Jamepad).
  As ações já passam por `Accessibility.translate`, então o mapeamento entra num único lugar.
  Exige trocar o build para Gradle/Maven por causa das dependências.
- ⏳ Vários espaços de save (3 slots) com miniatura.
- ⏳ Dicas em camadas para quem travar (coruja/concha do eco: vaga → específica).

## 3. Conteúdo e profundidade (P1)

- ✅ **Conchas secretas** (12) com recompensas do Seu Marinho e a Concha que Escuta.
- ✅ **Corrente de trocas** de 6 passos, começando com o mascate de domingo.
- ✅ **Observatório** com 4 conjuntos de doação que mudam a vila e a tela de constelações.
- ✅ **Cartas** na caixa de correio (14 cartas ligadas a eventos, algumas com moedas).
- ✅ **Cartão do Explorador** com tempo de jogo e **16 conquistas** (prontas para virar conquistas da Steam).
- ⏳ **Estações do ano** a cada 28 dias: paleta, peixes, flores e festival diferentes (base em `Weather`).
  Exige revisar os testes estatísticos de pesca e clima.
- ⏳ **Culinária**: pratos com peixe e flores que dão amizade extra ou facilitam a pesca.
- ⏳ **Aniversários** dos moradores, com presente favorito em dobro.
- ⏳ **Capítulo 6** fechando o arco da Cintila e da Capitã, com nova região.
- ⏳ **Puzzles opcionais** com os itens da história (Lanterna revelando símbolos, blocos empurráveis na gruta).

## 4. Encantos (P2)

- ✅ Modo foto (F12), pose de "item obtido", créditos com resumo da jornada.
- ✅ Lanternas e canteiros na vila, luneta na praia, constelações no observatório.
- ⏳ Animais da vila (gato com rotina, gaivotas na praia).
- ⏳ Rádio com músicas e notícias da vila (estilo Pokégear).

## 5. Distribuição e divulgação 🧑

- **itch.io** primeiro (aceita o .zip e os instaladores; "pague quanto quiser"; coleta opiniões cedo).
- **Steam** depois: taxa por jogo, página com capturas e trailer; conquistas e nuvem pedem Steamworks (ex.: steamworks4j).
- **Página da loja**: capturas em 2x (F12 gera fotos em 2x), GIF da pesca e do festival, trailer de 30–60 s.
- **Tradução** (inglês, espanhol): exige tirar os textos do código para arquivos `textos_pt.properties` etc.

## 6. Arquitetura

Feito: `World` (mapas), `Story` (roteiro), `Inventory`, `Npc/GameMap/Warp`, `GameFiles` (arquivos),
`Feature` (interface dos sistemas novos: um sistema se registra em `Game.features` e recebe
reset/save/load/talk/draw/key sem mexer em seis lugares do `Game`).

Próximos passos:
1. Migrar os 13 sistemas antigos para `Feature`, preservando a ordem atual de `load` e `talk` (ela importa).
2. Estados de tela como `enum`.
3. Textos fora do código (tradução) e um `Renderer` separado para `tile`, `person` e `hud`.
4. Pacotes Java e Gradle quando entrar a primeira dependência (gamepad).
