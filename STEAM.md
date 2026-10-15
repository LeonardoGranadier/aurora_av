# Aventura em Aurora na Steam

O que já está pronto no projeto e o que depende de você (🧑). A ordem segue o caminho da Steam:
conta → página "em breve" → build → revisão → lançamento.

## 1. Conta e app 🧑

1. Crie a conta no [Steamworks](https://partner.steamgames.com/) (dados fiscais, banco e verificação de identidade).
2. Pague a taxa do Steam Direct (US$ 100 por jogo; volta como receita depois de US$ 1.000 em vendas).
3. Anote o **App ID** e crie **três depots**: Windows, Linux e macOS.
4. Em *Installation → General*, as opções de inicialização:

| Sistema | Executável |
|---|---|
| Windows | `AventuraEmAurora.exe` |
| Linux (e Steam Deck) | `bin/AventuraEmAurora` |
| macOS | `AventuraEmAurora.app` |

O Java já vai embutido em cada pacote (jpackage), então não há dependência para instalar.

## 2. Página da loja

Imagens geradas por `Capturas.java` (rode `java -Daurora.dados=$(mktemp -d) -cp .build Capturas` depois de compilar):

| Arquivo em `loja/steam/` | Onde vai no Steamworks |
|---|---|
| `cabecalho-920x430.png` | Header Capsule |
| `capsula-pequena-462x174.png` | Small Capsule |
| `capsula-principal-1232x706.png` | Main Capsule |
| `capsula-vertical-748x896.png` | Vertical Capsule |
| `biblioteca-600x900.png` | Library Capsule |
| `heroi-3840x1240.png` | Library Hero (sem texto) |
| `logo-1280x720.png` | Library Logo (PNG transparente) |
| `fundo-1438x810.png` | Page Background (opcional) |
| `icone-comunidade-184x184.png` | Community Icon |

- **Capturas**: `loja/capturas/*-pt.png` e `*-en.png` (1280x960, mínimo de 5). Ordem sugerida: 10, 02, 01, 09, 11, 04, 03, 07, 05, 06, 08.
- **Textos**: reaproveite `loja/PAGINA-ITCH.md` (pt e en). Descrição curta com até 300 caracteres.
- **Idiomas**: Português (Brasil) e Inglês, interface e legendas (o jogo não tem vozes).
- **Etiquetas sugeridas**: Cozy, Adventure, Exploration, Pixel Graphics, Top-Down, Fishing, Cooking, Puzzle,
  Singleplayer, Relaxing, Family Friendly, Cute, Story Rich, Casual, 2D.
- **Classificação**: livre (sem violência). Preencha o questionário IARC no Steamworks.
- **Controle**: "Full controller support" (Xbox, PlayStation, Switch Pro e Steam Deck via Jamepad).
- 🧑 Publique a página como **"Em breve"** pelo menos duas semanas antes do lançamento, para juntar listas de desejos.

### Roteiro do trailer (45 s, capture com F12/OBS em tela cheia)

| Tempo | Cena | Texto na tela |
|---|---|---|
| 0–4 s | Praia à noite, o farol acende | *Uma vila pequena.* |
| 4–9 s | Vila de manhã, moradores conversando, Bolinha | *Vizinhos de verdade.* |
| 9–15 s | Pesca e culinária (fogão, lancheira) | *Pesque, cozinhe, presenteie.* |
| 15–21 s | Barco para a ilha, braseiros do templo | *Mistérios antigos.* |
| 21–27 s | Campo de gelo, Cintila na cratera | *Uma estrela caída.* |
| 27–33 s | Recife e baleia; sino da praça e Vale dos Ecos | *Canções que abrem caminhos.* |
| 33–40 s | Floresta Antiga: as quatro lanternas acendem | *Quatro estações, nove capítulos.* |
| 40–45 s | A aurora sobre a vila; título e "Lista de desejos" | **Aventura em Aurora** |

Use a música da própria cena (o jogo sintetiza o áudio). Grave em 1280x960 ou em tela cheia 16:9.

## 3. Build (SteamPipe)

1. Gere os pacotes com a steamworks4j:
   - pelo GitHub: *Actions → Testes e pacotes → Run workflow*; baixe os três artefatos `AventuraEmAurora-<sistema>-steam`;
   - ou em cada sistema: `sh empacotar.sh --steam --portatil`.
2. Coloque as três pastas em `dist/` (`dist/AventuraEmAurora-windows-steam`, `-linux-steam`, `-mac-steam`).
3. Edite `steam/app_build.vdf`: App ID e IDs dos três depots.
4. Envie: `steamcmd +login <usuario> +run_app_build "$(pwd)/steam/app_build.vdf" +quit`.
5. No Steamworks, ponha o build na branch `default` e teste pelo próprio cliente Steam antes de pedir revisão.

## 4. Conquistas

`Steam.java` envia as conquistas do Cartão do Explorador quando o jogo roda pela Steam (sem a biblioteca ou sem
a Steam aberta, não faz nada; a versão da itch.io não leva a biblioteca). Partidas antigas sincronizam ao abrir.
Cadastre em *Stats & Achievements* exatamente estes nomes de API. Os ícones 256x256 já estão em
`loja/steam/conquistas/`: `ACH_X.png` (conquistada) e `ACH_X_cinza.png` (bloqueada), gerados por `Capturas.java`
a partir de `AchievementIcons.java` (os mesmos brasões do Cartão do Explorador).

| Nome de API | Nome | Descrição |
|---|---|---|
| `ACH_FAROL` | Luz do Farol | Devolveu a Concha Brilhante ao farol. |
| `ACH_MAPA` | Cartógrafo da Capitã | Reuniu os três pedaços do Mapa Estrelado. |
| `ACH_MARUJO` | Marujo | Zarpou para a Ilha das Estrelas. |
| `ACH_ESTRELA_MAR` | Guardião do Mar | Levou a Estrela do Mar ao topo do farol. |
| `ACH_CINTILA` | Amizade Estelar | Ajudou a Cintila a voltar para o céu. |
| `ACH_JARDIM` | Laços entre Céu e Terra | Fez o Jardim das Constelações florescer. |
| `ACH_ALBUM` | Mestre Pescador | Registrou as oito espécies de Aurora. |
| `ACH_SINOS` | Ouvido de Ouro | Repetiu as cinco melodias dos sinos. |
| `ACH_FESTIVAL` | Estrela do Festival | Venceu as duas provas numa mesma festa. |
| `ACH_AMIZADE` | Coração da Vila | Chegou a cinco corações com um morador. |
| `ACH_PEDIDOS` | Mão Amiga | Concluiu os quatro pedidos dos moradores. |
| `ACH_ENCOMENDAS` | Vizinhança Prestativa | Entregou dez encomendas do mural. |
| `ACH_REGIOES` | Pés na Estrada | Visitou todas as regiões de Aurora. |
| `ACH_CONCHAS` | Ouvido de Concha | Encontrou todas as conchas secretas. |
| `ACH_TROCAS` | Rei das Trocas | Completou a corrente de trocas da vila. |
| `ACH_OBSERVATORIO` | Céu Restaurado | Restaurou o observatório do professor. |
| `ACH_MARE` | Canção da Maré | Trouxe a baleia guardiã de volta para a baía. |
| `ACH_FESTIVAL_CEU` | Festival do Céu | Reuniu Aurora inteira no alto do Pico Estrelado. |
| `ACH_MELODIA` | Melodia da Semana | Trouxe a Lia de volta para o Lucas. |
| `ACH_GUARDIAO` | Guardião da Aurora | Desfez o eclipse com a Grande Árvore. |
| `ACH_ENIGMAS` | Mestre dos Enigmas | Abriu os três baús escondidos de Aurora. |
| `ACH_ANO` | Um Ano em Aurora | Viveu as quatro estações da vila. |
| `ACH_CHEF` | Chef de Aurora | Cozinhou todas as receitas. |
| `ACH_ANIVERSARIO` | Quem Ama Lembra | Deu um presente de aniversário a um morador. |
| `ACH_LENDARIO` | Pescador Lendário | Pescou os oito peixes raros de Aurora. |
| `ACH_INSETOS` | Amigo dos Insetos | Pegou os dez insetos de Aurora. |
| `ACH_CURADOR` | Curador do Museu | Completou as duas alas do Museu de Aurora. |
| `ACH_GATO` | Amigo do Bolinha | Fez carinho no Bolinha em sete dias diferentes. |

O `SteamTest` garante que todo id vira um nome de API único e válido. Para testar fora da Steam, crie
`steam_appid.txt` (só o App ID) na pasta de onde o jogo é iniciado, com a Steam aberta (o arquivo é ignorado pelo git).

## 5. Steam Cloud (sem código)

Use o **Auto-Cloud** em *Steam Cloud → Settings*: cota de 10 MB e 50 arquivos, com estas raízes:

| Sistema | Raiz | Subpasta | Padrão |
|---|---|---|---|
| Windows | `WinAppDataRoaming` | `Aventura em Aurora` | `savegame*.properties*` |
| macOS | `MacAppSupport` | `Aventura em Aurora` | `savegame*.properties*` |
| Linux | `LinuxXdgDataHome` | `aventura-aurora` | `savegame*.properties*` |

Preferências de vídeo, idioma e acessibilidade ficam de fora de propósito (são de cada máquina).

## 6. Steam Deck

- Resolução do Deck (1280x800): o jogo escala em múltiplos inteiros e centraliza; teste em tela cheia (F11 fica salvo).
- Controle: funciona pelo Jamepad; no Steamworks, marque o layout "Gamepad" como padrão.
- Textos: a opção de texto grande da acessibilidade ajuda na tela pequena.

## 7. Antes de lançar 🧑

- [ ] Página "Em breve" publicada e aprovada.
- [ ] Build aprovado na revisão (a Valve leva alguns dias úteis).
- [ ] Conquistas cadastradas (ícones em `loja/steam/conquistas/`) e testadas pela Steam.
- [ ] Steam Cloud configurado e testado (jogar no PC, continuar em outro).
- [ ] Teste com 5 a 10 pessoas que nunca jogaram (ROADMAP, seção 1).
- [ ] Preço e desconto de lançamento definidos; data anunciada.
