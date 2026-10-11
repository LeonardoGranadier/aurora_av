# Testes de Aventura em Aurora

## Rodar tudo

```sh
sh testar.sh                      # compila e roda todos os testes
sh testar.sh WorldMapTest BondsTest
```

Cada teste roda numa pasta temporaria nova com o atalho `assets`. Em caso de falha,
o script mostra o fim da saida e mantem a pasta (com saves e imagens) para inspecao.
As secoes abaixo descrevem a cobertura e o procedimento manual equivalente.

## Sistemas inspirados nos classicos

`ClassicFeaturesTest` cobre conchas secretas (esconderijos solidos e alcancaveis, achado unico,
recompensas acumuladas do Seu Marinho, contador no mapa), corrente de trocas (mascate aos
domingos, prioridade da historia, persistencia), observatorio (conjuntos, confirmacao, rollback
em falha de save, mudancas na vila, ceu a noite), cartas (chegada pela manha, limite diario,
moedas uma vez), nome e tratamento, conquistas, cartao, creditos, texto letra a letra, correr,
modo desenvolvedor, foto, backup do save e registro de erros. Renderiza as telas novas.

## Controle e vida no mundo

`GamepadTest` usa um controle simulado (sem hardware): segurar e soltar direcional, A em
dialogos sem repetir, Start/B na pausa, X/Y/RB/LB/Select abrindo telas, RT correndo, teclado
e controle juntos, remapeamento, segurar A na pesca e desconexao soltando tudo.
`WorldLifeTest` renderiza as estacoes com efeitos, testa agua para os peixes, conversa entre
moradores (fala, resposta, pausa, silencio longe do jogador), traducoes e "reduzir movimento".

## Idiomas

`LangTest` liga o ingles e percorre o jogo: conversa com todos os personagens e objetos
de todos os mapas em cada etapa da historia, de dia e de noite, le os dialogos inteiros e
abre todas as telas (itens, cartao, diario, calendario, mapa, cozinha, cartas, decoracao,
observatorio, creditos, nome, saves, menus e dicas). Falha listando em `faltando.txt` todo
texto desenhado sem traducao. Uma frase de controle confirma que a varredura enxerga faltas.

Para auditar os demais testes em ingles, rode-os com `-Daurora.idioma=en` e
`-Daurora.idioma.faltando=arquivo.txt`: ao sair, o arquivo lista o que ficou sem traducao.

## Capitulo 8, enigmas, saves e musica

`ChapterEightTest` joga o capitulo 8 (Lucas, melodias em tres dias da semana com erro e
repeticao, passagem da gruta, pedras empurradas, reinicio ao sair, ponte, Lia, creditos do
novo final, compatibilidade dos creditos ja vistos), os tres enigmas opcionais (ruinas com
pedra do eco, pegadas que exigem a Lanterna e respeitam a ordem, bau que exige o Cristal),
a musica de cada estacao e os tres espacos de save (novo jogo em espaco livre, ultimo jogado,
carregar, substituir com confirmacao, apagar um so espaco).

## Capitulos 6 e 7

`NewChaptersTest` joga os dois capitulos interagindo como o jogador (ficando ao lado e
apertando Z): capita a noite, bote para o recife, tres conchas cantoras, cancao com erro e
acerto, baleia, persistencia, convites (sopa da Mamae, persistencia no meio), Cintila a noite,
festa com os sete convidados, creditos finais, conquistas, decoracoes e destinos do mapa.
Tambem cobre a pergunta do nome em partidas antigas (responder, nao repetir, pular com Esc).

## Estacoes e culinaria

`SeasonsCookingTest` cobre a ordem das estacoes e o novo ano, peixes favorecidos sem
eliminar especies, neve no inverno (fora da praia e da ilha), flores por estacao, anuncios,
aniversarios (presente em dobro, persistencia), calendario, fogao na parede sem bloquear a
casa, receitas, confirmacao, rollback de ingredientes, efeitos do dia, lancheira diante de
morador, limite diario de presentes, gato (area, noite, carinho diario) e as 20 conquistas.
Renderiza a vila nas quatro estacoes, calendario, cozinha, lancheira, cartao e o gato.

## Arquivos e janela

`GameFilesTest` verifica que o desenvolvimento usa a pasta atual, a pasta de dados
configuravel da versao distribuida, a localizacao dos assets e a escala da janela
(2x identica a cena original, faixas pretas centralizadas e reducao abaixo de 1x).

## Acessibilidade

Execute `AccessibilityTest` em pasta temporaria sem save nem accessibility.properties.
Verifica remapeamento, teclas reservadas/conflitos, acao mantida e solta, preferencias,
texto ampliado paginado, diario com rolagem, relogio preservado, captura cancelavel,
confirmacoes de presentes/encomendas e protecao da partida existente. Renderiza as telas.

## Encomendas da Vila

Execute `VillageOrdersTest` em pasta temporaria vazia com assets. Verifica acesso,
pedidos compativeis, selecao diaria persistente, estoque compartilhado, album preservado,
expiracao sem penalidade, flores, moedas, amizade, duplicatas, rollback em falha de save,
premio de dez entregas, colocacao/reload da decoracao, painel, saves antigos e reinicio.

## Jardinagem

Execute `GardeningTest` em pasta temporaria vazia com assets. Cobre acesso ao quintal
e canteiros, regador, plantio, rega unica, dias secos/chuvosos, viagens longas, colheita,
desbloqueio especial, estoque, vendas, presentes compartilhados com peixes, persistencia,
rollback em falhas de gravacao, cesta, horarios, saves antigos e reinicio.

## Sons ambientes

Execute `SoundscapeTest` em pasta temporaria sem save nem config.properties, com assets.
Verifica selecao por lugar/horario/clima, silencio em interiores/titulo, oito loops
distintos, limites do PCM, silencio em volume zero, volumes independentes, persistencia,
configuracao invalida, painel e retorno. Renderiza painel e menu. A avaliacao auditiva
e a reproducao em hardware real precisam ser feitas com alto-falantes ou fones.

## Diario de Aventuras

Execute `AdventureJournalTest` em pasta temporaria vazia com o atalho `assets`.
Cobre atalhos, pausa, retorno ao menu, capitulos ocultos, registro de inscricoes,
autosave sem mudanca de etapa, persistencia, rolagem, resumos, entrega final,
partidas antigas e reinicio. Renderiza inicio, pistas, historico e menu completo.

## Mapa de Aurora

Execute `WorldMapTest` depois de compilar, em uma pasta temporaria vazia com o
atalho `assets`, seguindo o procedimento abaixo. Cobre Tab, ESC, retorno ao menu,
pausa do relogio, bloqueio durante dialogo/movimento, descoberta em warps e viagens,
persistencia, saves antigos, destinos por etapa e reinicio. Renderiza mapa inicial,
mapa completo e menu de pausa dentro de casa.

Requer JDK 17 ou superior. Na pasta do projeto, execute:

```sh
javac *.java
aurora_project="$PWD"
aurora_test_dir=$(mktemp -d)
cd "$aurora_test_dir"
ln -s "$aurora_project/assets" assets
java -Djava.awt.headless=true -cp "$aurora_project" VillageLifeTest
```

Use uma pasta temporaria nova em cada execucao. O teste recusa iniciar se encontrar
`savegame.properties`, para proteger uma partida existente. Os saves de teste e as
imagens renderizadas ficam nessa pasta temporaria.

Cobertura: trajetos e horarios dos moradores, reservas de tiles e colisoes,
acesso aos pontos de pesca e interacoes da vila, vara, habitats, horario e progresso
das oito especies, captura, falhas e cancelamento, album e trofeu, sequencias dos
sinos, erros, repeticao, medalha e quatro sons distintos, save/load, saves antigos,
conclusao da campanha e protecao contra erros de gravacao.

As imagens incluem vila, album, pesca, sinos e inventario. O teste de captura usa
um controlador automatico; a dificuldade e a experiencia sonora ainda devem ser
avaliadas jogando com teclado e alto-falantes.

## Lacos de Aurora

Compile com o mesmo `javac *.java`. Em outra pasta temporaria vazia, execute
`java -Djava.awt.headless=true -cp "$aurora_project" BondsTest`.

Este teste cobre as quatro missoes, acesso real aos objetos da floresta e praia,
horarios das observacoes, limite diario de amizade e presentes, estoque separado
do album, preferencias, historias pessoais, recompensa de pesca, persistencia,
compatibilidade com saves anteriores e preservacao da missao principal do professor.
Tambem renderiza o diario dos quatro moradores, a selecao de presentes e o menu.

## Festival de Aurora

Depois de compilar, em mais uma pasta temporaria vazia, execute
`java -Djava.awt.headless=true -cp "$aurora_project" FestivalTest`.

Cobertura: calendario e horarios de abertura, encontro na praca e retorno as rotinas,
colisoes e acesso ao mural/lago/saidas, dialogos por amizade, inscricao, vara obrigatoria,
cronometro e pausas, captura real pelo minijogo, encerramento, sete rodadas especiais,
premio unico por edicao, recorrencia, save/load de tentativa em andamento e saves antigos.
Renderiza praca decorada, mural, pesca com cronometro, sinos especiais e lembranca em casa.

## Meu Cantinho em Aurora

Depois de compilar, em outra pasta temporaria vazia, execute
`java -Djava.awt.headless=true -cp "$aurora_project" HomeDecorTest`.

Cobertura: venda por quantidade, estoque compartilhado com presentes e preservacao do album,
precos e saldo, limite de moedas, compra unica dos oito moveis, colisao e rotacao,
porta e pontos de entrada, acesso a cama e a Mamae, tapete sob moveis, mover e guardar,
decoracoes conquistadas, migracao da estrela do festival, persistencia do layout completo,
atalhos e horarios das lojas, save antigo e rollback de transacoes com falha de gravacao.
Renderiza loja, venda, casa mobiliada, editor e previa de colocacao invalida.

## O Tempo em Aurora

Depois de compilar, em outra pasta temporaria vazia, execute
`java -Djava.awt.headless=true -cp "$aurora_project" WeatherTest`.

Cobertura: primeiro dia, previsao deterministica, passagem da meia-noite, save/load sem
novo sorteio, compatibilidade com saves antigos, todos os moradores saindo de casa e
alcancando o abrigo sem colisoes, retorno as rotinas, acesso aos NPCs e prioridade do festival.
Confere os pesos da pesca com amostragem reproduzivel e disponibilidade de todas as especies
em cada clima. Verifica pixels da chuva e neblina em movimento, ausencia de efeitos em
interiores e preservacao da neve. Renderiza chuva, neblina, previsao e menu de pausa.

## O Jardim das Constelacoes

Depois de compilar, em outra pasta temporaria vazia, execute
`java -Djava.awt.headless=true -cp "$aurora_project" ChapterFiveTest`.

Cobertura: mensagem noturna, professor, bloqueios da trilha e jardim, acesso aos
colecionaveis, sementes e enigma parcialmente salvos, simbolos incorretos, fogueira,
florescimento noturno, reencontro com Cintila, caminhos de retorno, entrega a Mamae,
autosave depois do dialogo final, decoracao desbloqueada, partida antiga e reinicio.
Renderiza a trilha e o jardim florescido. Os testes nao substituem jogar com teclado
e ouvir a trilha em um dispositivo de audio.
