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
- ✅ Três espaços de save com nome, capítulo e estação de cada partida.
- ✅ Dicas em camadas (Concha do Eco): vaga, onde ir, o que fazer — no diário ou no menu de pausa.

## 3. Conteúdo e profundidade (P1)

- ✅ **Conchas secretas** (12) com recompensas do Seu Marinho e a Concha que Escuta.
- ✅ **Corrente de trocas** de 6 passos, começando com o mascate de domingo.
- ✅ **Observatório** com 4 conjuntos de doação que mudam a vila e a tela de constelações.
- ✅ **Cartas** na caixa de correio (14 cartas ligadas a eventos, algumas com moedas).
- ✅ **Cartão do Explorador** com tempo de jogo e **16 conquistas** (prontas para virar conquistas da Steam).
- ✅ **Estações do ano** (28 dias): paisagem, peixes favorecidos, flores por estação, neve, nomes do festival,
  falas e cartas de cada estação, calendário.
- ✅ **Culinária**: fogão em casa, 8 receitas ensinadas pelos moradores, lancheira (K), efeitos do dia e pratos favoritos.
- ✅ **Aniversários** dos moradores, com aviso e presentes em dobro.
- ✅ **Bolinha, o gato da vila**, com carinho diário e conquista. Conquistas agora são 20.
- ✅ **Capítulo 6, A Canção da Maré**: conchas cantoras, Recife de Coral (região nova), canção e a baleia guardiã.
- ✅ **Capítulo 7, O Festival do Céu**: convites, banquete com a culinária e festa no Pico; novo final com créditos.
- ✅ Partidas antigas: a Mamãe pergunta nome e tratamento uma vez.
- ✅ **Capítulo 8, A Melodia da Semana**: melodias por dia da semana, Vale dos Ecos e ponte de pedras; novo final.
- ✅ **Enigmas opcionais**: pedras empurráveis (Ruínas do Vale), pegadas reveladas pela Lanterna, baú que o Cristal derrete.

## 4. Encantos (P2)

- ✅ Modo foto (F12), pose de "item obtido", créditos com resumo da jornada.
- ✅ Lanternas e canteiros na vila, luneta na praia, constelações no observatório.
- ✅ Gato da vila. ⏳ Gaivotas na praia e outros animais.
- ✅ Música própria de cada estação na vila e na floresta.
- ⏳ Rádio com músicas e notícias da vila (estilo Pokégear).

## 5. Distribuição e divulgação 🧑

- **itch.io** primeiro (aceita o .zip e os instaladores; "pague quanto quiser"; coleta opiniões cedo).
- **Steam** depois: taxa por jogo, página com capturas e trailer; conquistas e nuvem pedem Steamworks (ex.: steamworks4j).
- **Página da loja**: capturas em 2x (F12 gera fotos em 2x), GIF da pesca e do festival, trailer de 30–60 s.
- ✅ **Tradução para inglês**, com troca no título e no menu, dicionário em `assets/lang/en.tsv` e `LangTest`
  garantindo que nenhum texto fique sem tradução. ⏳ Espanhol: basta criar `assets/lang/es.tsv` e acrescentar "es" em `Lang.CODES`.

## 6. Arquitetura

Feito: `World` (mapas), `Story` (roteiro), `Inventory`, `Npc/GameMap/Warp`, `GameFiles` (arquivos),
`Feature` (interface dos sistemas novos: um sistema se registra em `Game.features` e recebe
reset/save/load/talk/draw/key sem mexer em seis lugares do `Game`).

Próximos passos:
1. Migrar os 13 sistemas antigos para `Feature`, preservando a ordem atual de `load` e `talk` (ela importa).
2. Estados de tela como `enum`.
3. Textos fora do código (tradução) e um `Renderer` separado para `tile`, `person` e `hud`.
4. Pacotes Java e Gradle quando entrar a primeira dependência (gamepad).
