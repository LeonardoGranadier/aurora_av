import java.util.Properties;

/**
 * Concha do Eco: dicas em três camadas para o objetivo atual, como as corujas e o telefone de Link's Awakening.
 * A primeira é vaga, a segunda diz onde ir e a terceira diz o que fazer. Cada pedido revela a próxima camada;
 * a contagem recomeça quando a história avança. Peça pelo diário (Z) ou pelo menu de pausa.
 */
final class Hints implements Feature {
    /** HINTS[etapa] = {vaga, onde, como}. */
    static final String[][] HINTS = {
        {"Alguém em casa quer falar com você.", "A Mamãe está dentro de casa.", "Fique de frente para a Mamãe e aperte Z."},
        {"A carta da Mamãe tem um destinatário à beira-mar.", "A Praia Azul fica ao sul da vila: siga a trilha de terra para baixo.", "O Seu Marinho fica perto do farol, na Praia Azul. Fale com ele."},
        {"O farol perdeu algo que brilha.", "A Floresta Verde fica a leste da vila.", "Siga a trilha da floresta até a clareira no nordeste: a concha dourada está no chão."},
        {"O Seu Marinho espera pela concha.", "Volte à Praia Azul.", "Fale com o Seu Marinho levando a Concha Brilhante."},
        {"O mar traz surpresas depois que escurece.", "Durma na cama de casa para chegar a noite e vá à Praia Azul.", "À noite, uma garrafa aparece na areia, no meio da praia. Pegue-a com Z."},
        {"O bilhete fala de um lugar que repete os sons.", "A Gruta do Eco fica ao norte da Floresta Verde.", "Entre pela abertura nas rochas ao norte da floresta e fale com a Capitã no salão."},
        {"Os pedaços do mapa aparecem em horas diferentes.", "Um na vila à noite, outro na praia de dia e o último no fundo da gruta.", "Vila: perto do lago, à noite. Praia: na areia, de dia. Gruta: na câmara a leste do salão."},
        {"A Capitã espera pelo mapa completo.", "Ela continua na Gruta do Eco.", "Volte ao salão da Gruta do Eco e fale com a Capitã."},
        {"A Capitã mudou de lugar.", "Ela espera no cais da Praia Azul, durante o dia.", "Vá ao píer de madeira da Praia Azul de dia e fale com ela."},
        {"Antes de viajar, é bom se despedir.", "A Mamãe e o Seu Marinho querem falar com você.", "Fale com a Mamãe, em casa, e com o Seu Marinho, na praia."},
        {"Tudo pronto para a viagem.", "A Capitã espera no cais.", "Fale com a Capitã no cais da Praia Azul para zarpar."},
        {"O portão do templo só se abre sob as estrelas.", "A fogueira da ilha permite descansar até a noite.", "Descanse na fogueira e, à noite, entre no templo a leste da ilha."},
        {"O mural do templo conta uma ordem.", "Sol, Lua e Estrela, nessa ordem.", "Acenda o braseiro do Sol (leste), depois o da Lua (oeste) e por fim o da Estrela (norte)."},
        {"Algo se abriu no fundo do templo.", "O portão interno está aberto.", "Siga pelo corredor ao norte do salão e examine o altar."},
        {"A Estrela do Mar brilha melhor no escuro.", "Volte de barco à Praia Azul e espere a noite.", "À noite, fale com o Seu Marinho na Praia Azul."},
        {"Alguém na vila viu algo no céu.", "O Prof. Carvalho anda pela Vila Aurora.", "Fale com o Prof. Carvalho na vila."},
        {"A estrela caiu na montanha.", "A Trilha da Montanha sai pelo norte da vila.", "No gelo você desliza até bater em algo: use as pedras para parar e chegue à cabana do Eremita."},
        {"O fogão da cabana precisa de fogo.", "A Lanterna do professor pode acendê-lo.", "Examine o fogão dentro da cabana do Eremita."},
        {"Algo quente pode derreter o gelo.", "O portão de gelo fica ao norte, depois da cabana.", "Leve o Cristal de Calor até o bloco de gelo na trilha e aperte Z."},
        {"Algo brilha lá no alto.", "O Pico Estrelado fica no topo da trilha.", "Suba pelo caminho depois do portão de gelo e examine o brilho na cratera."},
        {"A Cintila precisa da luz do farol.", "O trenó do pico desce rápido; o farol fica na Praia Azul.", "À noite, examine a porta do farol na Praia Azul."},
        {"O farol guarda uma mensagem.", "Volte à porta do farol depois que escurecer.", "Na Praia Azul, à noite, examine a porta do farol."},
        {"O professor conhece o caminho do jardim.", "Ele anda pela Vila Aurora.", "Fale com o Prof. Carvalho na vila."},
        {"Três luzes estão escondidas na trilha.", "A Trilha dos Vagalumes começa a leste da Floresta Verde.", "Há sementes no ramo oeste, no ramo leste e perto do portão ao norte."},
        {"As sementes abrem uma passagem.", "O portão fica ao norte da Trilha dos Vagalumes.", "Examine o portão de raízes no norte da trilha."},
        {"A inscrição do jardim explica o enigma.", "Cada pedra mostra um símbolo; Z gira a pedra.", "Oeste: Lua. Norte: Estrela. Leste: Sol. Depois, à noite, examine a fonte."},
        {"Alguém apareceu no jardim.", "A Cintila está junto à fonte.", "Fale com a Cintila no centro do jardim."},
        {"A flor deve ir para casa.", "A Mamãe está em casa.", "Entre em casa e fale com a Mamãe."},
        {"O mar está cantando à noite.", "A Capitã está no cais da Praia Azul.", "Depois que escurecer, fale com a Capitã no cais."},
        {"Três conchas guardam uma canção.", "Gruta do Eco, Ilha das Estrelas e Recife de Coral.", "Gruta: câmara do fundo, a leste. Ilha: praia leste. Recife: pegue o bote na praia leste da ilha."},
        {"A canção deve ser tocada diante do mar aberto.", "Vá à ponta do Recife de Coral, à noite.", "Na ponta do recife, toque MI, DÓ, SOL, RÉ, MI (setas ou 1 a 4)."},
        {"Alguém vai querer saber da Maré.", "A Capitã está no cais da Praia Azul.", "Fale com a Capitã no cais."},
        {"Cintila deixou um recado.", "A porta do farol brilha à noite.", "Na Praia Azul, à noite, examine a porta do farol."},
        {"Sete amigos precisam do convite.", "Mamãe (casa), Seu Marinho e Capitã (praia), Professor, Tio Beto, Dona Rosa e Lucas (vila).", "Fale com cada um. O diário (L) mostra quem já foi convidado."},
        {"Uma festa precisa de comida.", "Leve um prato na lancheira (K); a Mamãe ajuda se faltar.", "Com um prato, suba ao Pico Estrelado à noite e fale com a Cintila."},
        {"A festa está acontecendo.", "Converse com os amigos no Pico Estrelado.", "Fale com a Cintila no centro da cratera."},
        {"O Lucas anda inquieto.", "Ele passa o dia pela praça da vila.", "Fale com o Lucas na Vila Aurora."},
        {"Cada dia tem a sua melodia.", "Toque no sino da praça; amanhã será outra.", "Toque a melodia do dia em 3 dias diferentes. A cama de casa faz o tempo passar."},
        {"O eco abriu uma passagem.", "Ela fica no fundo da Gruta do Eco.", "Vá à câmara leste da gruta e siga para o norte."},
        {"As placas pedem peso.", "Empurre as pedras andando contra elas.", "Duas pedras vão primeiro para o lado e depois para cima; a do caminho só sobe. A pedra do eco recomeça."},
        {"Alguém toca música no coreto.", "Atravesse a ponte do riacho.", "Fale com a Lia no coreto, ao norte do riacho."},
        {"O Lucas precisa saber da irmã.", "Ele está na praça da vila.", "Fale com o Lucas na Vila Aurora."},
        {"Aurora ainda tem muito a explorar.", "Procure conchas secretas, trocas, enigmas e receitas.", "O Cartão do Explorador (C) mostra as conquistas que faltam."}
    };

    final Game game;
    int stage = -1, level;   // camadas já reveladas para a etapa atual

    Hints(Game game) { this.game = game; }

    /** Revela a próxima camada de dica e a mostra num diálogo. */
    void ask() {
        if (stage != game.stage) { stage = game.stage; level = 0; }
        String[] layers = HINTS[Math.min(game.stage, HINTS.length - 1)];
        level = Math.min(layers.length, level + 1);
        String[] lines = new String[level < layers.length ? level + 1 : level];
        for (int i = 0; i < level; i++) lines[i] = layers[i];
        if (level < layers.length) lines[level] = "(Peça de novo para uma dica mais clara.)";
        game.audio.sfx("twinkle");
        game.state = Game.PLAY; game.down.clear();
        game.say("Concha do Eco", lines);
    }

    @Override public void reset() { stage = -1; level = 0; }
    @Override public void save(Properties p) { p.setProperty("dicas.etapa", "" + stage); p.setProperty("dicas.nivel", "" + level); }
    @Override public void load(Properties p) {
        try { stage = Integer.parseInt(p.getProperty("dicas.etapa", "-1")); level = Integer.parseInt(p.getProperty("dicas.nivel", "0")); }
        catch (NumberFormatException e) { stage = -1; level = 0; }
        level = Math.max(0, Math.min(3, level));
    }
}
