import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/** Amizades, pedidos e presentes; o album conserva o historico das capturas. */
final class Bonds {
    static final String[] IDS = {"prof", "beto", "rosa", "lucas"};
    static final String[] NAMES = {"Prof. Carvalho", "Tio Beto", "Dona Rosa", "Lucas"};
    static final String[] TITLES = {"Caderno de campo", "Paciência de pescador", "Flores para a praça", "O apito perdido"};
    static final String[] REWARDS = {"Caderno autografado", "Boia de cortiça", "Flor prensada", "Apito da amizade"};
    static final String[] REQUESTS = {
        "Observe a árvore marcada na floresta de dia e o farol na praia à noite. Depois me conte o que viu!",
        "Pesque três lambaris no lago da vila a partir de agora. Volte para me contar: tenho uma boia especial para você.",
        "Preciso de três flores da trilha da floresta para enfeitar a praça. Pode trazê-las para mim?",
        "Perdi meu apito na trilha da floresta, perto da clareira a leste. Pode procurar para mim?"
    };
    static final String[][] STORIES = {
        {"Meu primeiro caderno era um presente do meu avô. Eu desenhava estrelas antes de saber escrever o nome delas.",
         "Durante anos achei que estudar era trabalhar sozinho. Você me lembrou de como é bom dividir uma descoberta."},
        {"Foi o Seu Marinho quem me ensinou a pescar. No primeiro dia só trouxe um sapato... nós rimos até o pôr do sol.",
         "Esta vila me acolheu quando eu não tinha nada. Agora posso passar adiante o que aprendi. Obrigado por sentar comigo."},
        {"Plantei minha primeira flor aqui no dia em que cheguei à vila. Ainda guardo uma pétala dentro de um livro.",
         "Às vezes tenho saudade de casa. Mas quando vejo vocês passando pela praça, percebo que também criei raízes aqui."},
        {"Aprendi as notas dos sinos ouvindo minha irmã. Ela viajou, mas deixou uma música para cada dia da semana.",
         "Quando ela voltar, quero apresentar meu melhor amigo de aventuras. Você vai estar lá, né?"}
    };
    final Game game;
    final int[] friendship = new int[4], quests = new int[4], talked = new int[4], gifted = new int[4], stories = new int[4];
    final int[] spent = new int[8];
    int betoStart, selection, returnState, giftTarget = -1;

    Bonds(Game game) { this.game = game; reset(); }
    void reset() {
        Arrays.fill(friendship, 0); Arrays.fill(quests, 0); Arrays.fill(talked, -1);
        Arrays.fill(gifted, -1); Arrays.fill(stories, 0); Arrays.fill(spent, 0);
        betoStart = 0; giftTarget = -1; selection = 0;
    }
    int residentIndex(String id) { return Arrays.asList(IDS).indexOf(id); }
    int day() { return (int) Math.floor(game.time / 1440); }
    void addFriendship(int who, int points) { friendship[who] = Math.min(100, friendship[who] + points); }

    void buildWorld() {
        GameMap forest = game.maps.get("Floresta Verde");
        add(forest, "bond_flower0", 13, 5, "Flor silvestre");
        add(forest, "bond_flower1", 16, 8, "Flor silvestre");
        add(forest, "bond_flower2", 23, 7, "Flor silvestre");
        add(forest, "bond_whistle", 26, 8, "Apito de Lucas");
        add(forest, "bond_tree", 13, 7, "Árvore marcada");
        add(game.maps.get("Praia Azul"), "bond_light", 20, 9, "Observação do farol");
        for (Npc n : forest.npcs) if (n.id.equals("bond_tree")) n.when = "dia";
        for (Npc n : game.maps.get("Praia Azul").npcs) if (n.id.equals("bond_light")) n.when = "noite";
    }
    void add(GameMap map, String id, int x, int y, String name) {
        map.t[y][x] = map.name.equals("Praia Azul") ? 's' : '.';
        map.addNpc(new Npc(id, name, x, y, new Color(236, 149, 178)).obj(id).still());
    }
    boolean visible(String id) {
        if (id.startsWith("bond_flower")) return quests[2] == 1;
        if (id.equals("bond_whistle")) return quests[3] == 1;
        if (id.equals("bond_tree") || id.equals("bond_light")) return quests[0] == 1;
        return true;
    }
    int progress(int who) {
        return switch (who) {
            case 0 -> (game.taken.contains("bond_tree") ? 1 : 0) + (game.taken.contains("bond_light") ? 1 : 0);
            case 1 -> Math.min(3, Math.max(0, game.life.catches[0] - betoStart));
            case 2 -> (int) game.taken.stream().filter(id -> id.startsWith("bond_flower")).count();
            default -> game.taken.contains("bond_whistle") ? 1 : 0;
        };
    }
    int required(int who) { return who == 0 ? 2 : who == 3 ? 1 : 3; }

    void afterTalk(Npc npc) {
        int who = residentIndex(npc.id);
        if (who < 0 || game.dlg == null) return;
        if (talked[who] != day()) { talked[who] = day(); addFriendship(who, 5 + game.cooking.talkBonus()); }
        List<String> extra = new ArrayList<>();
        if (quests[who] == 0) {
            quests[who] = 1;
            if (who == 1) betoStart = game.life.catches[0];
            extra.add(REQUESTS[who]);
        } else if (quests[who] == 1 && progress(who) >= required(who)) {
            quests[who] = 2; addFriendship(who, 25);
            extra.add(switch (who) {
                case 0 -> "Que anotações bonitas! Este caderno é seu. Ainda vamos descobrir muita coisa juntos.";
                case 1 -> "Três lambaris! Fique com esta boia de cortiça: ela torna a faixa de captura um pouco maior.";
                case 2 -> "As flores estão lindas! Guarde esta flor prensada como lembrança da nossa praça.";
                default -> "Meu apito! Obrigado! Vou ficar com ele, mas fiz outro igual para você. Agora temos um sinal de amizade.";
            });
            extra.add("Lembrança recebida: " + REWARDS[who] + ".");
            game.audio.sfx("pickup");
        } else if (quests[who] == 1) {
            extra.add(TITLES[who] + ": " + progress(who) + "/" + required(who) + ". " + REQUESTS[who]);
        }
        for (int level = 0; level < 2; level++) {
            int bit = 1 << level;
            if (friendship[who] >= (level + 1) * 40 && (stories[who] & bit) == 0) {
                stories[who] |= bit; extra.add(STORIES[who][level]);
            }
        }
        List<String> lines = new ArrayList<>(Arrays.asList(game.dlg)); lines.addAll(extra);
        game.dlg = lines.toArray(String[]::new);
    }

    boolean collect(Npc npc) {
        if (!npc.id.startsWith("bond_")) return false;
        if (!visible(npc.id)) return true;
        game.take(npc);
        game.say("", switch (npc.id) {
            case "bond_tree" -> "Você observa os anéis e as folhas da árvore. Pequenos brotos apontam para o sol. Anotação feita!";
            case "bond_light" -> "Você desenha o caminho da luz sobre a água e registra as estrelas ao redor do farol. Anotação feita!";
            case "bond_whistle" -> "Entre as folhas, um apito de madeira! As iniciais de Lucas estão gravadas nele.";
            default -> "Você colhe uma flor da trilha com cuidado. Dona Rosa vai gostar!";
        });
        game.audio.sfx("pickup"); return true;
    }

    int stock(int fish) { return Math.max(0, game.life.catches[fish] - spent[fish] - game.home.sold[fish]); }
    boolean favorite(int who, int fish) {
        return switch (who) { case 0 -> fish == 5 || fish == 7; case 1 -> fish == 0 || fish == 2;
            case 2 -> fish == 1 || fish == 3; default -> fish == 4 || fish == 6; };
    }
    void openJournal(int from) { returnState = from; giftTarget = -1; selection = 0; game.state = Game.BONDS; }
    void openGift() {
        int x = game.tx + Game.DX[game.dir], y = game.ty + Game.DY[game.dir];
        for (Npc n : game.map.npcs) {
            int who = residentIndex(n.id);
            if (who < 0 || !game.vis(n) || n.x != x || n.y != y) continue;
            if (gifted[who] == day()) { game.say(n.name, "Seu presente de hoje já fez meu dia. Obrigado!"); return; }
            giftTarget = who; returnState = Game.PLAY; selection = 0; game.state = Game.BONDS; return;
        }
        game.toast("Nenhum morador por perto.");
    }
    boolean give(int who, int fish) {
        if (stock(fish) == 0 || gifted[who] == day()) return false;
        int oldSpent = spent[fish], oldGifted = gifted[who], oldFriendship = friendship[who];
        spent[fish]++; gifted[who] = day(); addFriendship(who, game.giftPoints(who, favorite(who, fish) ? 15 : 8));
        if (!game.save(false)) { spent[fish] = oldSpent; gifted[who] = oldGifted; friendship[who] = oldFriendship; return false; }
        game.state = Game.PLAY;
        game.say(NAMES[who], favorite(who, fish) ? "Você lembrou do meu favorito! Que presente especial. Obrigado!" : "Que gentileza! Vou guardar com carinho esse momento.");
        game.audio.sfx("pickup"); return true;
    }
    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_J) { game.state = returnState; game.down.clear(); return; }
        int count = giftTarget < 0 ? 4 : 8;
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) selection = (selection + count - 1) % count;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) selection = (selection + 1) % count;
        if (giftTarget >= 0 && (key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER)) {
            int who = giftTarget, fish = selection;
            game.access.ask("Entregar 1 " + VillageLife.FISH[fish].name() + " para " + NAMES[who] + "?", () -> {
                if (!give(who, fish)) game.toast("Presente indisponível. Confira estoque e limite diário.");
            });
        }
    }

    String signature() {
        return Arrays.toString(friendship) + Arrays.toString(quests) + Arrays.toString(talked)
            + Arrays.toString(gifted) + Arrays.toString(stories) + Arrays.toString(spent) + betoStart;
    }
    void save(Properties p) {
        int[][] fields = {friendship, quests, talked, gifted, stories, spent};
        for (int f = 0; f < fields.length; f++) for (int i = 0; i < fields[f].length; i++)
            p.setProperty("lacos." + f + "." + i, Integer.toString(fields[f][i]));
        p.setProperty("lacos.betoInicio", Integer.toString(betoStart));
    }
    void load(Properties p) {
        int[][] fields = {friendship, quests, talked, gifted, stories, spent};
        int[] max = {100, 2, Integer.MAX_VALUE, Integer.MAX_VALUE, 3, 999999};
        for (int f = 0; f < fields.length; f++) for (int i = 0; i < fields[f].length; i++) {
            int fallback = f == 2 || f == 3 ? -1 : 0;
            try { fields[f][i] = Math.max(fallback, Math.min(max[f], Integer.parseInt(p.getProperty("lacos." + f + "." + i, "" + fallback)))); }
            catch (NumberFormatException e) { fields[f][i] = fallback; }
        }
        betoStart = game.life.number(p, "lacos.betoInicio", 0, 999999);
        for (int i = 0; i < spent.length; i++) spent[i] = Math.min(spent[i], game.life.catches[i]);
    }

    boolean drawObject(Graphics2D g, Npc npc, int x, int y) {
        if (!npc.id.startsWith("bond_")) return false;
        g.setColor(new Color(255, 245, 177)); g.drawOval(x + 2, y + 3, 28, 28);
        if (npc.id.startsWith("bond_flower")) {
            g.setColor(new Color(55, 131, 86)); g.fillRect(x + 15, y + 16, 3, 15);
            g.setColor(new Color(236, 126, 168));
            for (int i = 0; i < 5; i++) g.fillOval(x + 11 + (int) (Math.cos(i * 1.257) * 7), y + 10 + (int) (Math.sin(i * 1.257) * 7), 9, 9);
            g.setColor(new Color(255, 224, 100)); g.fillOval(x + 13, y + 12, 7, 7);
        } else if (npc.id.equals("bond_whistle")) {
            g.setColor(new Color(189, 134, 68)); g.fillRoundRect(x + 5, y + 13, 24, 11, 5, 5);
            g.setColor(VillageLife.INK); g.fillOval(x + 9, y + 16, 5, 5);
        } else {
            g.setColor(new Color(244, 244, 224)); g.fillRect(x + 8, y + 8, 17, 21);
            g.setColor(new Color(60, 123, 139)); g.drawLine(x + 11, y + 14, x + 22, y + 14);
            g.drawLine(x + 11, y + 19, x + 22, y + 19); g.drawLine(x + 11, y + 24, x + 18, y + 24);
        }
        return true;
    }
    void heart(Graphics2D g, int x, int y, boolean filled) {
        g.setColor(filled ? new Color(214, 90, 123) : new Color(194, 210, 203));
        g.fillOval(x, y, 9, 9); g.fillOval(x + 7, y, 9, 9);
        g.fillPolygon(new int[]{x, x + 16, x + 8}, new int[]{y + 5, y + 5, y + 16}, 3);
    }
    void draw(Graphics2D g) {
        if (giftTarget >= 0) { drawGifts(g); return; }
        game.life.panel(g, "Laços de Aurora", "Amizades e lembranças da vila");
        for (int i = 0; i < 4; i++) {
            int y = 115 + i * 69;
            if (selection == i) { g.setColor(new Color(205, 231, 220)); g.fillRoundRect(38, y, 194, 61, 6, 6); }
            game.life.text(g, NAMES[i], 49, y + 23, 177, 15);
            for (int h = 0; h < 5; h++) heart(g, 50 + h * 23, y + 33, friendship[i] >= (h + 1) * 20);
        }
        int who = selection;
        game.life.text(g, TITLES[who], 254, 133, 334, 20);
        String status = quests[who] == 0 ? "Ainda não conversaram sobre um pedido." : quests[who] == 2 ? "Pedido concluído" : progress(who) >= required(who) ? "Tudo pronto! Volte para conversar." : "Em andamento · " + progress(who) + "/" + required(who);
        game.life.text(g, status, 254, 161, 332, 14);
        game.life.text(g, quests[who] == 0 ? "Uma conversa pode ser o começo de uma amizade." : REQUESTS[who], 254, 200, 328, 15);
        game.life.text(g, "Amizade: " + friendship[who] + "/100", 254, 306, 300, 14);
        game.life.text(g, "Lembrança: " + (quests[who] == 2 ? REWARDS[who] : "a descobrir"), 254, 334, 328, 14);
        String favorites = switch (who) { case 0 -> "Peixe-Lanterna e Peixe Estelar"; case 1 -> "Lambari e Bagre da Lua"; case 2 -> "Carpa Dourada e Truta Verde"; default -> "Sardinha Azul e Peixe-Coral"; };
        game.life.text(g, "Favoritos: " + favorites, 254, 365, 326, 13);
        game.life.text(g, "Histórias: " + Integer.bitCount(stories[who]) + "/2", 254, 412, 320, 13);
        game.life.text(g, "↑ ↓", 48, 437, 90, 15); game.life.text(g, "ESC", 548, 437, 55, 13);
    }
    void drawGifts(Graphics2D g) {
        game.life.panel(g, "Um presente", "Para " + NAMES[giftTarget]);
        for (int i = 0; i < 8; i++) {
            int y = 114 + i * 36;
            if (selection == i) { g.setColor(new Color(205, 231, 220)); g.fillRoundRect(38, y, 560, 33, 6, 6); }
            game.life.drawFish(g, 48, y + 10, 24, VillageLife.FISH[i].color());
            game.life.text(g, VillageLife.FISH[i].name(), 88, y + 23, 255, 15);
            if (favorite(giftTarget, i)) heart(g, 390, y + 8, true);
            game.life.text(g, "× " + stock(i), 486, y + 23, 100, 15);
        }
        game.life.text(g, "↑ ↓    Z  ↵", 48, 437, 240, 15); game.life.text(g, "ESC", 548, 437, 55, 13);
    }
}
