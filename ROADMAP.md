# Aventura em Aurora — ideias e caminho até o lançamento

Prioridades: **P0** impede o lançamento · **P1** deixa o jogo competitivo · **P2** extras que encantam.
Itens marcados com ✅ já foram feitos.

## 1. Preparar para publicar (P0)

- ✅ Pacote distribuível: `sh empacotar.sh` gera jar + assets + lançadores (Linux/Windows) e .zip;
  `--instalador` gera instalador nativo com Java embutido (jpackage), sem exigir Java instalado.
- ✅ Saves e preferências na pasta de dados do usuário na versão distribuída (pasta do jogo pode ser somente leitura).
- ✅ Janela ampliada conforme a tela, redimensionável, tela cheia (F11) e ícone da janela.
- ✅ `testar.sh` roda toda a suíte; `.gitignore` sem compilados nem saves pessoais.
- **Arte final.** Os sprites atuais são provisórios (gerados por `MakeAssets.java`). É o item que mais pesa
  na primeira impressão (página da loja, capturas, trailer). O sistema de troca por PNG já está pronto:
  basta substituir os arquivos de `assets/` mantendo os nomes. Considere contratar um artista de pixel art
  ou usar um pacote com licença comercial (verifique a licença de cada asset).
- **Atalho de teste T (+2h).** Hoje qualquer jogador pode avançar o relógio. Decidir: remover, esconder atrás
  de uma opção de desenvolvedor (`-Daurora.dev=true`) ou transformar numa mecânica (ex.: "esperar" no banco da praça).
- **Textos de fim de capítulo desatualizados.** Os capítulos 3 e 4 terminam com "Obrigado por jogar! (Continua...)"
  (`Story.java`), mas a história segue até o capítulo 5. Trocar por uma transição de capítulo.
- **Tela de créditos e versão** no título (autoria, ferramentas, licenças de fontes/sons), e uma tela de "fim"
  após o capítulo 5.
- **Registro de erros.** Uma exceção inesperada hoje só aparece no terminal. Gravar um `erros.log` na pasta de dados
  e mostrar uma mensagem amigável ajuda a receber relatos dos jogadores.
- **Backup de save.** Antes de gravar, manter a versão anterior como `savegame.bak` e oferecer recuperá-la se a
  principal estiver corrompida. Incrementar o campo `versao` quando o formato mudar.
- **Escolha do personagem.** São 14 textos com "(a)" (ex.: "corajoso(a)"). Perguntar nome e forma de tratamento
  no novo jogo deixa os diálogos naturais.
- **Teste com jogadores reais.** Uma rodada com 5–10 pessoas que nunca viram o jogo, só observando,
  revela onde travam (o mapa e o diário ajudam, mas os objetivos noturnos e o gelo costumam confundir).

## 2. Qualidade de vida (P1)

- **Controle/gamepad.** Essencial para Steam Deck e público de jogos aconchegantes. Java não lê controles nativamente;
  bibliotecas como SDL via LWJGL ou Jamepad resolvem. As ações já passam por `Accessibility.translate`,
  então o mapeamento entra num único lugar.
- **Velocidade do texto** com efeito de máquina de escrever e opção "instantâneo" (acessibilidade).
- **Vários espaços de save** (3 slots) com miniatura, dia e capítulo.
- **Tutorial em contexto** nas primeiras horas: dicas curtas na primeira pesca, no primeiro canteiro, no primeiro festival.
- **Indicador de interação** (ícone "Z" sobre o que pode ser usado à frente do jogador).
- **Correr** (segurar Shift) para trajetos longos entre vila, praia e montanha.
- **Opções de vídeo**: escala inteira vs. preencher a tela, e lembrar o modo tela cheia.

## 3. Conteúdo e profundidade (P1)

- **Estações do ano** a cada 28 dias: muda a paleta, os peixes, as flores e o festival (já existe a base em `Weather`).
- **Culinária**: combinar peixes e flores/colheitas em pratos que dão amizade extra ou efeitos (pesca mais fácil).
- **Museu ou coleção na casa do professor** para doar peixes e flores — dá um objetivo de longo prazo às coleções.
- **Aniversários dos moradores** no calendário, com presentes favoritos e diálogos especiais.
- **Capítulo 6** fechando o arco da Cintila e da Capitã, com uma nova região (ex.: ilha submersa ou observatório do avô do professor).
- **Mais moradores** na vila, cada um com rotina e pedido próprio (o sistema `Bonds` já suporta isso).
- **Melhorias na casa**: ampliar cômodos com moedas, papel de parede e piso.
- **Conquistas** ligadas ao que já existe (troféu de pesca, medalha dos sinos, estrela do festival, 10 encomendas),
  prontas para virar conquistas da Steam.

## 4. Encantos (P2)

- **Modo foto** que esconde a interface e salva um PNG (o código já renderiza telas em imagem nos testes).
- **Diário ilustrado** com desenhos que aparecem conforme a história avança.
- **Céu noturno com constelações** que o jogador descobre com a luneta do professor.
- **Animais da vila** (gato que segue rotinas, gaivotas na praia) para dar vida aos mapas.
- **Trilhas sonoras por estação** usando o sintetizador de `Audio.java`.

## 5. Distribuição e divulgação

- **itch.io** primeiro: aceita o .zip e os instaladores de `empacotar.sh`, permite preço "pague quanto quiser"
  e coleta opiniões cedo. Depois **Steam** (taxa de inscrição por jogo; precisa de página com capturas e trailer
  e, para conquistas e nuvem, integração Steamworks, ex.: steamworks4j).
- **Página da loja**: 5+ capturas em 2x (o `GameFilesTest` já gera uma), GIF da pesca e do festival, trailer de 30–60 s.
- **Tradução** para inglês e espanhol multiplica o público. Exige tirar os textos do código para arquivos
  `textos_pt.properties` / `textos_en.properties` (cerca de 150 falas só em `Story.java`).
- **Assinatura de código** dos instaladores (Windows e macOS mostram aviso de segurança sem ela).

## 6. Arquitetura (próximos passos)

Já feito: mapas em `World`, roteiro em `Story`, inventário em `Inventory`, tipos do mundo em arquivos próprios,
arquivos em `GameFiles`. `Game.java` caiu de 1.782 para cerca de 1.070 linhas, com todos os testes passando.

Sugestões seguintes, em ordem de retorno:
1. **Interface comum para os sistemas** (`reset`, `buildWorld`, `save`, `load`, `talk`, `key`, `draw`). Hoje
   `Game.newGame/save/loadGame/talk/keyPressed` repetem a lista de 13 sistemas; com uma interface, adicionar um
   sistema novo passa a ser uma linha. Atenção: a ordem atual de `load` e `talk` importa e deve ser preservada.
2. **Estados de tela como `enum`** em vez de constantes `int` (`TITLE`, `PLAY`, ... `HANDOFF`).
3. **Textos fora do código** (ver Tradução) e um `Renderer` separado para `tile`, `person` e `hud`.
4. **Pacotes Java** (`aurora.mundo`, `aurora.sistemas`, `aurora.ui`) quando o número de arquivos crescer,
   junto com um build em Gradle ou Maven para dependências (ex.: biblioteca de gamepad).
