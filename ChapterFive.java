import java.awt.*;
import java.util.Arrays;
import java.util.Properties;

/** Continuação da campanha: caminhos, sementes e um enigma de símbolos. */
final class ChapterFive {
    static final String TRAIL = "Trilha dos Vagalumes", GARDEN = "Jardim das Constelações";
    static final String[] SYMBOLS = {"Sol", "Lua", "Estrela", "Semente"};
    static final int[] TARGET = {1, 2, 0}; // oeste, norte, leste
    static final int[][] STONES = {{4, 8}, {10, 4}, {15, 8}};
    final Game game;
    int seeds;
    final int[] stones = {3, 3, 3};

    ChapterFive(Game game) { this.game = game; }
    void reset() { seeds = 0; Arrays.fill(stones, 3); }
    void buildWorld() {
        GameMap forest = game.maps.get("Floresta Verde");
        forest.rect(25, 6, 5, 1, '=');
        forest.warps.add(new Warp(29, 6, TRAIL, 1, 14));
        forest.addNpc(new Npc("garden_trail_gate", "Passagem coberta de raízes", 29, 6,
            new Color(101, 145, 95)).obj("portao").still().stages(0, 22));

        GameMap trail = new GameMap(TRAIL, 24, 20, '#', true);
        trail.rect(1, 3, 22, 16, '.');
        trail.rect(12, 0, 2, 17, '=');
        trail.rect(0, 14, 14, 2, '=');
        trail.rect(4, 6, 1, 9, '='); trail.rect(4, 7, 10, 1, '=');
        trail.rect(13, 11, 8, 1, '='); trail.rect(13, 4, 5, 1, '=');
        trail.rect(17, 14, 5, 4, '~');
        for (int[] tree : new int[][]{{2, 4}, {7, 5}, {8, 10}, {2, 11}, {16, 9}, {21, 5}, {6, 17}, {15, 17}})
            trail.t[tree[1]][tree[0]] = '#';
        trail.warps.add(new Warp(0, 14, "Floresta Verde", 28, 6));
        trail.warps.add(new Warp(0, 15, "Floresta Verde", 28, 6));
        trail.warps.add(new Warp(12, 0, GARDEN, 9, 16));
        trail.warps.add(new Warp(13, 0, GARDEN, 10, 16));
        for (int i = 0; i < 3; i++) {
            int[][] places = {{4, 6}, {20, 11}, {17, 4}};
            trail.addNpc(new Npc("garden_seed" + i, "Semente de luz", places[i][0], places[i][1],
                new Color(255, 222, 105)).obj("garden_seed").stages(23, 23));
        }
        for (int x = 12; x <= 13; x++) trail.addNpc(new Npc("garden_gate", "Portão do jardim", x, 2,
            new Color(159, 180, 165)).obj("portao").still().stages(0, 24));
        trail.addNpc(new Npc("garden_sign", "Marco da trilha", 10, 14,
            new Color(150, 155, 155)).obj("mural").still());
        game.maps.put(trail.name, trail);

        GameMap garden = new GameMap(GARDEN, 20, 18, '.', true);
        garden.border('#'); garden.rect(6, 5, 8, 7, 'm');
        garden.rect(9, 1, 2, 17, 'm'); garden.rect(4, 8, 12, 1, 'm');
        garden.rect(2, 2, 3, 3, '~'); garden.rect(15, 2, 3, 3, '~');
        garden.warps.add(new Warp(9, 17, TRAIL, 12, 1));
        garden.warps.add(new Warp(10, 17, TRAIL, 13, 1));
        for (int i = 0; i < 3; i++) garden.addNpc(new Npc("garden_stone" + i, "Pedra celeste",
            STONES[i][0], STONES[i][1], new Color(150, 181, 188)).obj("garden_stone").still());
        garden.addNpc(new Npc("garden_fountain", "Fonte das Constelações", 9, 8,
            new Color(112, 202, 220)).obj("garden_fountain").still());
        garden.addNpc(new Npc("garden_mural", "Inscrição do jardim", 6, 12,
            new Color(151, 170, 176)).obj("mural").still());
        garden.addNpc(new Npc("garden_cintila", "Cintila", 10, 6,
            new Color(255, 231, 121)).obj("cintila").stages(26, 99));
        garden.addNpc(new Npc("fogueira", "", 14, 14, new Color(244, 152, 64)).obj("fogueira").still());
        game.maps.put(garden.name, garden);
    }
    boolean allowWarp(String to) {
        return (!to.equals(TRAIL) || game.stage >= 23) && (!to.equals(GARDEN) || game.stage >= 25);
    }
    boolean visible(String id) {
        if (id.startsWith("garden_seed") && id.length() == 12) return (seeds & (1 << (id.charAt(11) - '0'))) == 0;
        return true;
    }
    boolean solved() { return Arrays.equals(stones, TARGET); }
    boolean talk(Npc npc) {
        String id = npc.id;
        if (id.equals("porta_farol") && game.stage == 21) {
            if (!game.isNight()) game.say("", "Uma pequena luz pisca na lente do farol. Talvez a mensagem fique visível à noite.");
            else {
                game.setStage(22);
                game.say("Cintila", "Uma linha de estrelas desenha palavras na porta: \"Amigo, preciso da sua ajuda outra vez.\"",
                    "\"O Jardim das Constelações está se apagando. Suas flores guardam as lembranças que unem a terra ao céu.\"",
                    "\"O professor conhece o caminho. Procure por três sementes de luz. Eu estarei esperando.\"");
            }
            return true;
        }
        if (id.equals("prof") && game.stage == 22) {
            game.setStage(23);
            game.say(npc.name, "O jardim do meu avô! Ele dizia que as estrelas não vivem só de luz: vivem de lembranças compartilhadas.",
                "Há uma passagem a leste da clareira da Floresta Verde. Com essa mensagem, as raízes vão deixar você passar.",
                "Reúna três sementes na Trilha dos Vagalumes. Elas abrem o portão ao norte. E leia as inscrições do jardim!");
            return true;
        }
        if (id.equals("mae") && game.stage == 27) {
            game.say(npc.name, "Uma Flor-Constelação... Quando eu era pequena, sua avó me contou sobre esse jardim.",
                "Achava que era só uma história. Mas as histórias que dividimos também criam raízes, não é?",
                "Vamos cuidar desta flor juntos. Sempre haverá uma luz aqui para você voltar.",
                "Lá no alto, Cintila responde com um brilho. O jardim, a vila e o céu agora compartilham a mesma lembrança.",
                "Capítulo 5 concluído: Laços entre céu e terra.",
                "A Flor-Constelação já pode enfeitar a sua casa (tecla H).");
            game.afterDlg = () -> game.setStage(28);
            return true;
        }
        if (id.startsWith("garden_seed") && id.length() == 12) {
            if (game.stage != 23) return true;
            int bit = 1 << (id.charAt(11) - '0'); seeds |= bit;
            game.audio.sfx("pickup");
            if (seeds == 7) game.setStage(24);
            game.say("", "A semente repousa na sua mão, morna como um raio de sol. Sementes reunidas: " + Integer.bitCount(seeds) + "/3.",
                seeds == 7 ? "As três sementes apontam para o portão ao norte da trilha." : "Ainda há luzes escondidas nos outros caminhos da trilha.");
            return true;
        }
        if (id.startsWith("garden_stone") && id.length() == 13) {
            int index = id.charAt(12) - '0';
            if (game.stage == 25) {
                stones[index] = (stones[index] + 1) % 4; game.audio.sfx("bell" + stones[index]);
                game.say("", "A pedra gira e revela o símbolo: " + SYMBOLS[stones[index]] + ".",
                    solved() ? "As três pedras vibram juntas. A fonte espera pelo céu da noite." : "A inscrição junto à entrada explica o lugar de cada símbolo.");
            } else game.say("", "O símbolo da " + SYMBOLS[stones[index]] + " brilha. O jardim encontrou seu equilíbrio.");
            return true;
        }
        switch (id) {
            case "garden_trail_gate" -> game.say("", "As raízes guardam uma trilha esquecida. Uma mensagem das estrelas pode acordá-las.");
            case "garden_sign" -> game.say("", "Três sementes guardam o caminho: uma no ramo oeste, outra no ramo leste e a última perto do portão ao norte.");
            case "garden_gate" -> {
                if (game.stage == 24 && seeds == 7) {
                    game.setStage(25);
                    game.say("", "Você aproxima as três sementes. As raízes do portão se desenrolam, abrindo a passagem.",
                        "Além da trilha, um jardim antigo espera em silêncio. Guarde as sementes para a fonte.");
                } else game.say("", "Três cavidades brilham no portão. Faltam sementes de luz: " + Integer.bitCount(seeds) + "/3.");
            }
            case "garden_mural" -> game.say("", "\"No poente, a Lua repousa. Ao norte, a Estrela orienta. No nascente, o Sol desperta.\"",
                "\"Quando as três pedras concordarem, ofereça as sementes à fonte sob o céu da noite.\"",
                "Uma fogueira ao sul ainda está acesa. Viajantes descansavam aqui à espera das estrelas.");
            case "garden_fountain" -> {
                if (game.stage >= 26) game.say("", "A fonte canta baixinho. As sementes criaram raízes, e o jardim guarda uma nova lembrança.");
                else if (!solved()) game.say("", "A água reflete símbolos desencontrados. Primeiro alinhe as pedras conforme a inscrição.");
                else if (!game.isNight()) game.say("", "Os símbolos estão certos, mas a fonte precisa da noite. A fogueira permite descansar até as estrelas aparecerem.");
                else {
                    game.setStage(26);
                    game.say("", "Você entrega as sementes à água. Três raízes luminosas se espalham pelo jardim.",
                        "Flores se abrem como pequenas constelações. Entre elas, uma luz conhecida toma forma: Cintila!");
                }
            }
            case "garden_cintila" -> {
                if (game.stage == 26) {
                    game.setStage(27);
                    game.say("Cintila", "Você veio! O jardim estava se apagando porque ninguém mais contava suas histórias.",
                        "As sementes guardavam caminhos antigos. Mas foi você, voltando para ajudar, quem deu a elas uma nova lembrança.",
                        "Leve esta Flor-Constelação para sua mãe. Assim, o jardim terá raízes na vila também.",
                        "Eu posso visitar este lugar sempre que você quiser. Uma amizade não precisa morar no mesmo céu.");
                } else game.say("Cintila", game.stage == 27 ? "Sua mãe vai gostar da flor. Eu espero aqui, entre as constelações!"
                    : "Tin! O jardim floresce toda noite. Obrigada por não deixar nossa história se apagar.");
            }
            default -> { return false; }
        }
        return true;
    }
    String signature() { return seeds + Arrays.toString(stones); }
    void save(Properties p) {
        p.setProperty("jardim.sementes", Integer.toString(seeds));
        for (int i = 0; i < 3; i++) p.setProperty("jardim.pedra." + i, Integer.toString(stones[i]));
    }
    void load(Properties p) {
        seeds = game.stage >= 24 ? 7 : game.stage == 23 ? game.life.number(p, "jardim.sementes", 0, 7) : 0;
        for (int i = 0; i < 3; i++) stones[i] = game.stage >= 26 ? TARGET[i] : game.life.number(p, "jardim.pedra." + i, 3, 3);
    }
    void star(Graphics2D g, int x, int y, int radius, Color color) {
        int[] xx = new int[10], yy = new int[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5; double r = i % 2 == 0 ? radius : radius * 0.45;
            xx[i] = x + (int) (Math.cos(angle) * r); yy[i] = y + (int) (Math.sin(angle) * r);
        }
        g.setColor(color); g.fillPolygon(xx, yy, 10);
    }
    boolean drawObject(Graphics2D g, Npc npc, int x, int y) {
        if (npc.id.startsWith("garden_seed")) {
            int bob = (int) (Math.sin(game.frame / 15.0 + npc.x) * 2);
            star(g, x + 16, y + 17 + bob, 11, new Color(254, 219, 109)); return true;
        }
        if (npc.id.startsWith("garden_stone")) {
            int index = npc.id.charAt(12) - '0', symbol = stones[index];
            g.setColor(new Color(97, 134, 145)); g.fillRoundRect(x + 2, y + 5, 28, 28, 6, 6);
            g.setColor(new Color(198, 226, 221)); g.fillRoundRect(x + 5, y + 7, 22, 22, 4, 4);
            if (symbol == 2) star(g, x + 16, y + 18, 9, new Color(184, 117, 167));
            else if (symbol == 0) { g.setColor(new Color(225, 164, 39)); g.fillOval(x + 9, y + 11, 14, 14); }
            else if (symbol == 1) {
                g.setColor(new Color(66, 116, 174)); g.fillOval(x + 9, y + 11, 14, 14);
                g.setColor(new Color(198, 226, 221)); g.fillOval(x + 14, y + 9, 12, 13);
            } else { g.setColor(new Color(91, 148, 102)); g.fillOval(x + 12, y + 12, 9, 14); }
            return true;
        }
        if (!npc.id.equals("garden_fountain")) return false;
        g.setColor(new Color(142, 161, 169)); g.fillOval(x, y + 8, 32, 24);
        g.setColor(game.stage >= 26 ? new Color(106, 219, 213) : new Color(71, 131, 165)); g.fillOval(x + 4, y + 11, 24, 16);
        star(g, x + 16, y + 17, 7, new Color(246, 220, 132)); return true;
    }
    void drawWorld(Graphics2D g, int camX, int camY) {
        if (game.map.name.equals(TRAIL) && game.isNight()) {
            for (int i = 0; i < 32; i++) {
                int x = 40 + (i * 127 % 660) + (int) (Math.sin(game.frame / 40.0 + i) * 8) - camX;
                int y = 110 + (i * 73 % 465) - camY;
                g.setColor(new Color(228, 238, 113, 130 + (int) (Math.sin(game.frame / 18.0 + i) * 80)));
                g.fillRect(x, y, 2, 2);
            }
        }
        if (!game.map.name.equals(GARDEN)) return;
        if (game.stage >= 26 || solved()) {
            g.setColor(new Color(101, 181, 186, game.isNight() ? 160 : 85));
            for (int[] stone : STONES) g.drawLine(stone[0] * 32 + 16 - camX, stone[1] * 32 + 16 - camY, 9 * 32 + 16 - camX, 8 * 32 + 16 - camY);
        }
        for (int[] flower : new int[][]{{5, 5}, {14, 5}, {4, 11}, {15, 11}, {7, 3}, {12, 3}, {6, 14}, {12, 12}}) {
            int x = flower[0] * 32 + 16 - camX, y = flower[1] * 32 + 16 - camY;
            g.setColor(new Color(61, 127, 89)); g.fillRect(x - 1, y, 3, 12);
            star(g, x, y, game.stage >= 26 ? 10 : 5, game.stage >= 26 ? new Color(239, 189, 221) : new Color(126, 154, 139));
            if (game.stage >= 26) star(g, x, y, 4, new Color(255, 236, 130));
        }
    }
}
