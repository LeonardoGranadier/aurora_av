/** Roteiro da campanha principal: objetivos, faixas de capítulo e diálogos dos capítulos 1 a 4. */
final class Story {
    static final String[] OBJECTIVES = {
        "Fale com a mamãe aqui em casa.",
        "Leve a carta ao Seu Marinho (Praia Azul, ao sul da vila).",
        "Ache a Concha Brilhante (Floresta Verde, a leste da vila).",
        "Leve a concha de volta ao Seu Marinho.",
        "Volte à Praia Azul depois que anoitecer (durma na cama de casa).",
        "Encontre a Capitã Estrela na Gruta do Eco (norte da floresta).",
        "Ache os 3 pedaços do mapa: vila à noite, praia de dia, gruta.",
        "Leve o Mapa Estrelado completo à Capitã Estrela.",
        "Encontre a Capitã no cais da Praia Azul (de dia).",
        "Despeça-se da mamãe e do Seu Marinho antes de zarpar.",
        "Volte ao cais e zarpe com a Capitã.",
        "Ache o Templo Estrelado: o portão só abre à noite (fogueira).",
        "Acenda os 3 braseiros na ordem do mural.",
        "O altar se revelou! Pegue a Estrela do Mar.",
        "Volte de barco e mostre a Estrela ao Seu Marinho, à noite.",
        "Fale com o Prof. Carvalho na vila: ele viu algo no céu.",
        "Suba a Montanha Névoa (saída norte da vila) e ache o Eremita.",
        "Acenda o fogão do Eremita com a Lanterna.",
        "Derreta o portão de gelo da trilha com o Cristal de Calor.",
        "Suba ao Pico Estrelado: algo brilha na cratera.",
        "Leve a Cintila ao farol, à noite (o trenó desce a montanha).",
        "Volte à porta do farol à noite: Cintila tem uma mensagem.",
        "Mostre a mensagem de Cintila ao Prof. Carvalho.",
        "Busque 3 sementes na Trilha dos Vagalumes, a leste da floresta.",
        "Abra o portão ao norte da Trilha dos Vagalumes.",
        "Alinhe as pedras do jardim e visite a fonte à noite.",
        "Fale com Cintila junto à fonte do jardim.",
        "Leve a Flor-Constelação à Mamãe, em casa.",
        "Capítulo 5 concluído! À noite, visite a Capitã no cais: o mar anda cantando.",
        "Ouça as 3 conchas cantoras: Gruta do Eco, Ilha e Recife de Coral.",
        "Na ponta do Recife de Coral, à noite, toque a canção da baleia.",
        "Conte à Capitã Estrela, no cais, o que aconteceu no recife.",
        "Capítulo 6 concluído! Numa noite clara, a porta do farol guarda um convite.",
        "Convide os amigos para o Festival do Céu.",
        "Leve um prato na lancheira (K) e suba ao Pico Estrelado à noite.",
        "Celebre com os amigos e fale com Cintila no alto do Pico Estrelado.",
        "Capítulo 7 concluído! O Lucas anda inquieto: fale com ele na vila.",
        "Toque a melodia do dia no sino da praça, em 3 dias diferentes.",
        "Uma passagem se abriu no fundo da Gruta do Eco. Siga até o Vale dos Ecos.",
        "Empurre as pedras sobre as placas para erguer a ponte até o coreto.",
        "Atravesse a ponte e fale com Lia no coreto do Vale dos Ecos.",
        "Encontre o Lucas na praça da vila: a Lia voltou!",
        "Capítulo 8 concluído! À noite, a Cintila chama do alto do Pico Estrelado.",
        "Atravesse as raízes ao norte do Jardim das Constelações.",
        "Acenda as 4 lanternas da Floresta Antiga: flor, peixe, prato e canção.",
        "À noite, fale com a Grande Árvore no centro da Floresta Antiga.",
        "Volte para casa e conte tudo à Mamãe.",
        "Fim da jornada! Aurora continua esperando por você."
    };
    /** Última etapa da campanha: ao chegar nela, os créditos rolam. */
    static final int FINAL = ChapterNine.FINAL;

    final Game game;
    Story(Game game) { this.game = game; }

    /** Texto exibido na faixa quando a etapa começa. */
    static String banner(int st) {
        return switch (st) {
        case 4 -> "Capítulo 1 concluído: A Luz do Farol";
        case 5 -> "Capítulo 2: O Chamado da Capitã";
        case 7 -> "Mapa Estrelado completo!";
        case 8 -> "Capítulo 3: A Ilha das Estrelas";
        case 10 -> "Despedidas feitas! Volte ao cais.";
        case 11 -> "Ilha das Estrelas";
        case 12 -> "O Templo Estrelado";
        case 13 -> "O altar se revelou!";
        case 14 -> "A Estrela do Mar é sua!";
        case 15 -> "Capítulo 3 concluído: A Ilha das Estrelas";
        case 16 -> "Capítulo 4: A Estrela Cadente";
        case 18 -> "Cristal de Calor obtido!";
        case 19 -> "O portão de gelo derreteu!";
        case 20 -> "A Cintila vai com você!";
        case 21 -> "Capítulo 4 concluído: A Estrela Cadente";
        case 22 -> "Capítulo 5: O Jardim das Constelações";
        case 23 -> "A Trilha dos Vagalumes está aberta";
        case 24 -> "Três sementes de luz reunidas";
        case 25 -> "O Jardim das Constelações";
        case 26 -> "O jardim despertou!";
        case 27 -> "Uma flor para levar para casa";
        case 28 -> "Capítulo 5 concluído: Laços entre céu e terra";
        case 29 -> "Capítulo 6: A Canção da Maré";
        case 30 -> "A canção está completa!";
        case 31 -> "A baleia Maré voltou!";
        case 32 -> "Capítulo 6 concluído: A Canção da Maré";
        case 33 -> "Capítulo 7: O Festival do Céu";
        case 34 -> "Todos os amigos foram convidados!";
        case 35 -> "O Festival do Céu começou!";
        case 36 -> "Capítulo 7 concluído: O Festival do Céu";
        case 37 -> "Capítulo 8: A Melodia da Semana";
        case 38 -> "O eco abriu um caminho!";
        case 39 -> "O Vale dos Ecos";
        case 40 -> "A ponte subiu!";
        case 41 -> "Lia foi encontrada!";
        case 42 -> "Capítulo 8 concluído: A Melodia da Semana";
        case 43 -> "Capítulo 9: O Eclipse de Aurora";
        case 44 -> "A Floresta Antiga";
        case 45 -> "As quatro lanternas brilham!";
        case 46 -> "A aurora tomou o céu!";
        case 47 -> "Capítulo 9 concluído: O Eclipse de Aurora";
        default -> "Objetivo atualizado";
        };
    }

    /** Etapas que abrem ou fecham capítulos tocam a fanfarra. */
    static boolean fanfare(int st) {
        return st == 4 || st == 5 || st == 7 || st == 8 || st == 13 || st == 14 || st == 15 || st == 16 || st == 20 || st == 21 || st >= 22 && st != 30 && st != 34;
    }

    void checkBye() { if (game.bye == 3) game.setStage(10); }

    void talk(Npc n) {
        if (game.chapter.talk(n)) return;
        if (game.life.talk(n)) return;
        boolean noite = game.isNight();
        switch (n.id) {
            case "mae" -> {
                if (game.stage == 0) {
                    game.setStage(1);
                    game.say(n.name, "Bom dia, " + game.explorer.nameOr("meu bem") + "! Hoje é um dia especial na Vila Aurora.",
                        "Preciso que leve esta carta ao Seu Marinho, na Praia Azul.",
                        "Siga a trilha ao sul da vila. Boa viagem!");
                } else if (game.stage == 1) game.say(n.name, noite ? "Já está escuro... leve cuidado!" : "Seu Marinho mora na praia, ao sul da vila. Vá com calma!");
                else if (game.stage < 4) game.say(n.name, "Uma concha mágica? Cuidado na floresta, e volte antes de escurecer!");
                else if (game.stage == 4) game.say(n.name, "Vi o farol acendendo daqui de casa! Que orgulho!",
                    "Dizem que essa luz atrai visitantes misteriosos à noite. Vá ver, mas agasalhe-se!");
                else if (game.stage <= 7) game.say(n.name, "Uma capitã numa gruta? Leve cuidado com o escuro e volte para o jantar!");
                else if (game.stage == 8) game.say(n.name, "Uma viagem de barco?! Que emoção! Mas fale primeiro com a Capitã, no cais da praia, de dia.");
                else if (game.stage == 9 && (game.bye & 1) == 0) {
                    game.bye |= 1;
                    game.say(n.name, "Então vai mesmo velejar? Meu coração aperta... mas sei que você tem coragem de sobra.",
                        "Leve este lanche de pão com queijo e um agasalho. E volte " + game.explorer.w("inteirinho", "inteirinha", "sem nenhum arranhão") + "!");
                    checkBye();
                } else if (game.stage <= 13) game.say(n.name, "Boa viagem! Eu espero aqui, com o jantar quentinho.");
                else if (game.stage == 14) game.say(n.name, "Voltou! Depois você me conta tudo. Mostre a Estrela ao Seu Marinho, à noite!");
                else if (game.stage == 15) game.say(n.name, game.explorer.w("Meu explorador", "Minha exploradora", "Minha estrela") + " das estrelas! O farol nunca brilhou tanto!");
                else if (game.stage <= 19) game.say(n.name, "Vai subir a montanha? Leve um cachecol e cuidado com o gelo!");
                else if (game.stage == 20) game.say(n.name, "Uma estrelinha que fala?! Que fofa! Leve-a ao farol, antes que ela fique com frio.");
                else game.say(n.name, "O céu inteiro brilhou ontem à noite. Foi você, não foi?");
            }
            case "marinho" -> {
                switch (game.stage) {
                    case 0 -> game.say(n.name, "Olá, jovem! Estou só olhando o mar.");
                    case 1 -> {
                        game.setStage(2);
                        game.say(n.name, "Uma carta da sua mãe? Obrigado!",
                            "O farol apagou: a Concha Brilhante, o coração dele, sumiu na Floresta Verde.",
                            "Pode buscá-la? Fica numa clareira, no fim da trilha da floresta.");
                    }
                    case 2 -> game.say(n.name, "A floresta fica a leste da vila. Procure uma luz dourada.");
                    case 3 -> {
                        game.setStage(4);
                        game.say(n.name, "A Concha Brilhante! Você conseguiu!", "*clique* ...O farol voltou a brilhar!",
                            "Sabe de uma coisa? Luz assim atrai visitantes. Volte à noite e fique de olho no mar.");
                    }
                    case 4 -> game.say(n.name, noite ? "Olhe o mar... a maré trouxe algo. Procure pela praia!" : "O farol faz sua mágica à noite. Volte depois que escurecer!");
                    case 5 -> game.say(n.name, "Capitã Estrela?! Há anos não ouço esse nome... Fomos tripulantes juntos. Cuide dela.");
                    case 6 -> game.say(n.name, "Pedaços de mapa? A maré baixa, de dia, deixa a praia cheia de segredos. Procure com atenção.");
                    case 7 -> game.say(n.name, "Então o mapa está completo! Leve à Capitã. O mar chama...");
                    case 8 -> game.say(n.name, "A Capitã está no cais? Ela sempre foi teimosa... mas é a melhor navegadora que conheço.");
                    case 9 -> {
                        if ((game.bye & 2) == 0) {
                            game.bye |= 2;
                            game.say(n.name, "Vai zarpar com a Capitã? Então leve esta bússola velha: ela sempre aponta para casa.",
                                "Cuide dela... e de você, marujo.");
                            checkBye();
                        } else game.say(n.name, "Boa viagem, marujo. O mar é generoso com quem o respeita.");
                    }
                    case 10, 11, 12, 13 -> game.say(n.name, "Boa viagem, marujo. O mar é generoso com quem o respeita.");
                    case 14 -> {
                        if (noite) {
                            game.setStage(15);
                            game.say(n.name, "Isso é... a Estrela do Mar?! Ela brilha como o próprio mar!",
                                "Venha, vamos colocá-la no topo do farol, junto da Concha Brilhante.",
                                "*clique* ...Olhe! A baía inteira se iluminou!",
                                "Obrigado, marujo. Você devolveu a luz a Aurora.",
                                "Capítulo 3 concluído: A Ilha das Estrelas.",
                                "Seu Marinho: \"Descanse, marujo. Amanhã, passe na vila: o Prof. Carvalho anda olhando muito para o céu...\"");
                        } else game.say(n.name, "Uma estrela do mar de cristal?! Ela brilha melhor no escuro. Volte depois que anoitecer!");
                    }
                    case 15 -> game.say(n.name, "O farol e a Estrela juntos... a baía inteira brilha! Obrigado, marujo.");
                    case 16, 17, 18, 19 -> game.say(n.name, "Uma estrela caiu na Montanha Névoa? O farol vai ficar curioso. Boa subida, marujo!");
                    case 20 -> game.say(n.name, noite ? "Essa luzinha no seu ombro... Vá até a porta do farol, ao pé da torre!" : "Uma estrelinha? Traga-a à noite: é quando o farol chama quem vem do céu.");
                    default -> game.say(n.name, "O farol nunca brilhou tanto. Uma estrela pousou nele... jamais vou esquecer.");
                }
            }
            case "concha" -> {
                if (game.stage == 2) {
                    game.setStage(3);
                    game.take(n);
                    game.say("", "Uma concha dourada brilha entre as folhas...", "Você pegou a Concha Brilhante!");
                } else game.say("", "Uma concha dourada. Parece pertencer a alguém... melhor deixar aí por enquanto.");
            }
            case "garrafa" -> {
                game.take(n);
                game.setStage(5);
                game.say("", "Uma garrafa trazida pela maré... há um bilhete dentro.",
                    "\"Vi o farol acender depois de tantos anos. Preciso de ajuda. Estou na Gruta do Eco, ao norte da Floresta Verde. — Capitã Estrela\"");
            }
            case "frag1", "frag2", "frag3" -> {
                game.frags |= 1 << (n.id.charAt(4) - '1');
                game.take(n);
                int cnt = Integer.bitCount(game.frags);
                String txt = n.id.equals("frag1") ? "Um pedaço de pergaminho brilha ao luar..."
                    : n.id.equals("frag2") ? "Com a maré baixa, algo brilha na areia seca..."
                    : "Sobre uma pedra, no fundo da gruta, repousa um pedaço de mapa.";
                if (cnt == 3) {
                    game.setStage(7);
                    game.say("", txt, "Fragmento do Mapa Estrelado (3/3)!", "Os três pedaços se encaixam sozinhos. Leve o mapa à Capitã Estrela!");
                } else game.say("", txt, "Fragmento do Mapa Estrelado (" + cnt + "/3)!");
            }
            case "capita" -> {
                switch (game.stage) {
                    case 5 -> {
                        game.setStage(6);
                        game.say(n.name, "Você veio! Foi você quem acendeu o farol, não foi? Eu sou a Capitã Estrela.",
                            "Meu navio naufragou há anos e o Mapa Estrelado se partiu em três pedaços.",
                            "Um brilha à noite, na vila, perto do lago. Outro foi enterrado na praia: só aparece na maré baixa, de dia.",
                            "O terceiro está no fundo desta gruta, ao leste. Me traga todos!");
                    }
                    case 6 -> game.say(n.name, "Faltam " + (3 - Integer.bitCount(game.frags)) + " pedaço(s). Vila à noite, praia de dia, e o fundo desta gruta.");
                    case 7 -> {
                        game.setStage(8);
                        game.say(n.name, "O Mapa Estrelado inteiro! Há anos eu sonhava com isso.",
                            "Ele aponta para a Ilha das Estrelas, onde repousa a lendária Estrela do Mar.",
                            "Meu barco, o Estrela do Norte, ainda está inteiro no cais da Praia Azul.",
                            "Sem você eu ainda estaria perdida no escuro. Vamos velejar! Me encontre no cais, de dia.");
                    }
                    default -> game.say(n.name, "Shh! Esta gruta ecoa tudo... Volte quando a luz do farol me chamar.");
                }
            }
            case "capita_cais" -> {
                switch (game.stage) {
                    case 8 -> {
                        game.setStage(9);
                        game.say(n.name, "Chegou! Este é o Estrela do Norte, meu barco. Casco firme e vela nova!",
                            "A Ilha das Estrelas fica mar adentro. Antes de zarpar, despeça-se da sua mãe e do Seu Marinho.",
                            "Quem parte sem se despedir não volta com o mesmo sorriso!");
                    }
                    case 9 -> game.say(n.name, "Já se despediu? Fale com sua mãe e com o Seu Marinho (" + Integer.bitCount(game.bye) + "/2).");
                    default -> {
                        game.say(n.name, "Tudo pronto? Então, ao mar!",
                            "...O Estrela do Norte corta as ondas, e o farol vai ficando pequeno no horizonte.",
                            "Lá está! A Ilha das Estrelas surge entre as nuvens!");
                        game.afterDlg = () -> { game.setStage(11); game.time += 90; game.travel("Ilha das Estrelas", 4, 10); };
                    }
                }
            }
            case "capita_ilha" -> {
                if (game.stage == 11) {
                    if (noite) game.say(n.name, "Olhe! O portão do templo sumiu! Vamos logo, ele fica a leste.");
                    else game.say(n.name, "Chegamos! O templo fica a leste, mas dizem que o portão só some sob as estrelas.",
                        "Descanse na fogueira até anoitecer. Eu espero aqui.");
                } else if (game.stage == 12) game.say(n.name, "O mural do salão deve dizer a ordem das chamas. Leia com atenção!");
                else if (game.stage == 13) game.say(n.name, "O portão interno abriu! O altar está logo ali!");
                else game.say(n.name, "Vamos para casa! O barco está no cais, a oeste.");
            }
            case "placa" -> game.say("Placa", "\"Trilha da Montanha Névoa: FECHADA. Neve forte e gelo.\"");
            case "montanhista" -> game.say(n.name, game.stage >= 19 ? "Soube que o gelo do portão derreteu? Quem diria!"
                : "A trilha congelada é traiçoeira: você desliza até bater em alguma coisa. Procure as pedras: elas fazem você parar no lugar certo!");
            case "eremita" -> {
                switch (game.stage) {
                    case 16 -> {
                        game.setStage(17);
                        game.say(n.name, "Visitas! Você veio por causa da estrela cadente, não foi? Eu vi: ela caiu lá no Pico!",
                            "Mas o portão da trilha está preso num bloco de gelo. Só o Cristal de Calor, que guardo aqui, o derrete.",
                            "O problema: meu fogão apagou e o cristal só desperta com o calor do fogo. Acenda-o com a sua lanterna!");
                    }
                    case 17 -> game.say(n.name, "Meu fogão está apagado. Acenda-o com a lanterna e o Cristal de Calor desperta!");
                    case 18 -> game.say(n.name, "Use o cristal no portão de gelo, mais ao norte na trilha. E cuide da estrela: ela deve estar frágil.");
                    case 19 -> game.say(n.name, "Boa sorte lá em cima. O Pico é lindo à noite.");
                    case 20 -> game.say(n.name, "A estrelinha está com você? Que bom que alguém a encontrou. Desça de trenó, é mais rápido!");
                    default -> game.say(n.name, game.stage >= 21 ? "O céu dessa noite... nunca vi nada igual. Obrigado, jovem." : "Hm? Não esperava visita...");
                }
            }
            case "fogao" -> {
                if (game.stage == 17) {
                    game.setStage(18);
                    game.audio.sfx("fire");
                    game.say("", "Você acende o fogão com a Lanterna. A lenha estala e esquenta a cabana toda.",
                        "Na prateleira, o Cristal de Calor começa a brilhar em tons de laranja.",
                        "O Eremita sorri e entrega o Cristal de Calor a você!");
                } else if (game.stage < 17) game.say("", "O fogão está apagado e frio. Melhor falar com o dono da cabana.");
                else game.say("", "O fogão crepita, quentinho.");
            }
            case "gelo" -> {
                if (game.stage == 18) {
                    game.setStage(19);
                    game.audio.sfx("fire");
                    game.say("", "Você aproxima o Cristal de Calor do bloco de gelo...", "O gelo brilha em vermelho, racha e derrete em segundos!", "A trilha até o Pico Estrelado está livre.");
                } else game.say("", "Um bloco de gelo maciço bloqueia a trilha. Dizem que o Eremita sabe como derretê-lo.");
            }
            case "treno" -> {
                if (game.stage >= 20) {
                    game.say("", "Você se senta no trenó com a Cintila no colo.", "Ele desce a montanha em disparada, levantando neve!");
                    game.afterDlg = () -> { game.time += 20; game.travel("Vila Aurora", 15, 2); };
                } else game.say("", "Um trenó de madeira, deixado pelo Eremita. Primeiro veja o que brilha na cratera.");
            }
            case "cintila" -> {
                game.take(n);
                game.setStage(20);
                game.audio.sfx("twinkle");
                game.say("Cintila", "Tin... tin... (uma estrelinha de olhos grandes pisca fraquinho na neve)",
                    "\"Você... acendeu aquele farol? Vi a luz lá do céu e quis chegar mais perto... e caí.\"",
                    "\"Estou fraca demais para voltar. Só a luz do farol, à noite, me dá força. Pode me levar até lá?\"",
                    "A Cintila pula para perto de você e passa a seguir seus passos!");
            }
            case "porta_farol" -> {
                if (game.stage == 20 && noite) {
                    game.say("", "Você encosta na porta do farol e a Cintila começa a brilhar, cada vez mais forte...",
                        "Cintila: \"A luz do farol... é quentinha. Obrigada, " + game.explorer.nameOr(game.explorer.w("amigo", "amiga", "minha luz")) + "!\"",
                        "Ela sobe voando até a lente do farol, e o feixe de luz se enche de estrelas!",
                        "Cintila: \"Agora posso voltar para casa, no céu. Procure por mim nas noites claras!\"",
                        "Capítulo 4 concluído: A Estrela Cadente.",
                        "Numa noite clara, volte à porta do farol. Talvez a Cintila ainda tenha algo a dizer.");
                    game.afterDlg = () -> game.setStage(21);
                } else if (game.stage == 20) game.say("Cintila", "Tin! Só consigo subir à noite, quando a luz do farol me chama. Vamos esperar escurecer?");
                else if (game.stage >= 21) game.say("", "A porta do farol. Lá em cima, uma estrelinha cintila a noite toda.");
                else game.say("", "A porta do farol está trancada. O Seu Marinho cuida dele com carinho.");
            }
            case "capita_fim" -> {
                if (game.stage == 14) game.say(n.name, "Vá mostrar a Estrela ao Seu Marinho. À noite, o farol vai brilhar como nunca!");
                else if (game.stage >= 21) game.say(n.name, "Uma estrela voltou para o céu... Já vi muito mar, mas nada tão lindo.");
                else game.say(n.name, "Minha vida inteira procurando... e foi você quem a encontrou. Obrigada.", "O mar tem um novo guardião!");
            }
            case "barco" -> game.say("", game.stage <= 10 ? "O Estrela do Norte, o barco da Capitã. Casco firme e vela nova." : "O Estrela do Norte balança suavemente no cais.");
            case "barco_ilha" -> {
                if (game.stage >= 14) {
                    game.say("", "Hora de voltar para casa.", "O Estrela do Norte singra as ondas, com a Estrela do Mar brilhando no convés...");
                    game.afterDlg = () -> { game.time += 90; game.travel("Praia Azul", 5, 15); };
                } else game.say("", "O barco da Capitã. Ainda temos trabalho nesta ilha.");
            }
            case "cama", "fogueira" -> {
                boolean paraNoite = !noite;
                game.say("", (n.id.equals("cama") ? "Você se deita na cama" : "Você se senta junto à fogueira")
                    + (paraNoite ? " e cochila até o entardecer..." : " e descansa até o amanhecer..."));
                game.afterDlg = () -> game.rest(paraNoite);
            }
            case "estatua" -> game.say("Estátua", noite ? "As estrelas despertaram. Entre, viajante." : "Somente quem segue as estrelas cruza o portão.");
            case "portao" -> game.say("", "Um portão de pedra com uma estrela gravada. Está selado... talvez as estrelas saibam abri-lo.");
            case "portao2" -> game.say("", "Um segundo portão selado. Ele parece reagir às chamas do salão.");
            case "mural" -> game.say("", "Um mural antigo com três símbolos: o Sol, a Lua e uma Estrela.",
                "\"O Sol nasce primeiro, a Lua o segue, e a Estrela fecha a noite.\"",
                "Os braseiros do salão devem ser acesos nessa ordem.");
            case "br1", "br2", "br3" -> {
                int idx = n.id.charAt(2) - '0';
                String de = new String[]{"do Sol", "da Lua", "da Estrela"}[idx - 1];
                if (idx <= game.brasas) game.say("", "O braseiro " + de + " já está aceso.");
                else if (idx == game.brasas + 1) {
                    game.brasas++;
                    game.audio.sfx("fire");
                    game.refreshObjects();
                    if (game.brasas == 3) {
                        game.setStage(13);
                        game.say("", "A chama " + de + " se acende!", "As três chamas brilham juntas... com um ronco, o portão interno se abre!");
                    } else game.say("", "A chama " + de + " se acende!");
                } else {
                    game.brasas = 0;
                    game.audio.sfx("bad");
                    game.refreshObjects();
                    game.say("", "A chama " + de + " se acende... mas logo se apaga, e as outras também!", "A ordem estava errada. Releia o mural.");
                }
            }
            case "altar" -> {
                if (game.stage == 13) {
                    game.setStage(14);
                    game.say("", "No altar repousa a Estrela do Mar: uma estrela-do-mar de cristal que pulsa com luz própria.",
                        "Você a pegou! Ela está morna e parece sussurrar o som do mar.",
                        "Vamos voltar para casa! Leve a Estrela ao Seu Marinho, à noite.");
                } else if (game.stage > 13) game.say("", "O altar está vazio, mas ainda ecoa o som do mar.");
                else game.say("", "Um altar de pedra, coberto por uma luz fraca. Ainda está selado.");
            }
            case "prof" -> {
                if (game.stage <= 3) {
                    if (noite) game.say(n.name, "Já escureceu... a vila fica bonita à noite, mas cuidado na floresta.");
                    else game.say(n.name, "Sou o Prof. Carvalho. Estudo as regiões daqui: Vila, Floresta e Praia.",
                        game.stage < 2 ? "Dizem que o Seu Marinho anda precisando de ajuda." : "Boa sorte na sua missão!");
                } else if (game.stage == 4) game.say(n.name, "O farol voltou a brilhar! Dizem que à noite a maré traz surpresas.");
                else if (game.stage == 5) game.say(n.name, "Uma capitã na Gruta do Eco? Eu achava que era lenda!");
                else if (game.stage == 6) game.say(n.name, "Um mapa em pedaços? Os fragmentos brilham à noite, perto da água. Procure ao redor do lago da vila!");
                else if (game.stage == 7) game.say(n.name, "Mapa completo? Fascinante! Mostre à Capitã.");
                else if (game.stage <= 10) game.say(n.name, "Uma viagem à Ilha das Estrelas?! Anote tudo! Eu daria meu microscópio para ir junto!");
                else if (game.stage <= 13) game.say(n.name, "Quando voltar, conte cada detalhe. Vou deixar o caderno pronto!");
                else if (game.stage == 14) game.say(n.name, "Uma estrela-do-mar de cristal? Isso muda os livros de ciência!");
                else if (game.stage == 15) {
                    game.setStage(16);
                    game.say(n.name, "Você viu o céu ontem à noite? Uma estrela cadente caiu na Montanha Névoa, ao norte!",
                        "Meu avô tinha um observatório lá em cima e uma trilha que só os corajosos cruzam.",
                        "Leve esta lanterna dele: ela ilumina mais longe no escuro. A trilha começa na saída norte da vila.",
                        "Procure o Eremita da cabana. Ele conhece a montanha melhor que ninguém!");
                } else if (game.stage <= 19) game.say(n.name, "A Montanha Névoa fica ao norte da vila. Cuidado com o gelo: ele escorrega!");
                else if (game.stage == 20) game.say(n.name, "Uma estrelinha falante?! Leve-a ao farol, à noite. Ela vai se sentir em casa.");
                else game.say(n.name, noite ? "Olhe para o céu! Uma estrela nova brilha sobre o farol!" : "Dizem que o céu da vila brilhou diferente ontem à noite. Vou anotar tudo!");
            }
            case "lenhador" -> {
                if (game.stage >= 16) game.say(n.name, "Dizem que a estrela caiu na montanha. Eu ouvi o estrondo daqui da floresta!");
                else if (game.stage >= 8) game.say(n.name, "Ouvi dizer que vão velejar. Leve um chapéu, o sol do mar queima!");
                else if (game.stage >= 5) game.say(n.name, "A gruta fica no fim da trilha ao norte. Dizem que ela ecoa até de dia.");
                else game.say(n.name, noite ? "À noite essas árvores parecem sussurrar..." : "Esta floresta é enorme. Dizem que há uma clareira mágica ao leste.");
            }
            default -> game.say(n.name, "...");
        }
    }
}
