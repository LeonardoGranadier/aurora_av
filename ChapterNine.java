import java.awt.*;
import java.util.*;

/**
 * Capítulo 9, O Eclipse de Aurora: Cintila avisa que um eclipse vai apagar as estrelas. Ao norte do Jardim das
 * Constelações, a Floresta Antiga guarda quatro lanternas que só acendem com o que a vila tem de melhor:
 * uma flor, um peixe, um prato e uma canção. Acesas, elas despertam a Grande Árvore; à noite, o eclipse passa
 * e o céu de Aurora ganha uma aurora boreal, visível em todas as noites seguintes. Etapas 42 a 47.
 */
final class ChapterNine implements Feature {
    static final String FOREST = "Floresta Antiga";
    static final int START = 42, ENTER = 43, LANTERNS = 44, TREE = 45, HOME = 46, FINAL = 47;
    static final String[] LANTERN_NAMES = {"Lanterna da Flor", "Lanterna do Rio", "Lanterna do Lar", "Lanterna do Canto"};
    static final int[][] LANTERN_SPOTS = {{3, 9}, {20, 9}, {15, 2}, {6, 16}};
    static final int[] SONG = {0, 2, 1, 3, 0};   // DÓ, MI, RÉ, SOL, DÓ

    final Game game;
    int lit;              // bits das lanternas acesas
    boolean flowerTaken;  // a flor silvestre da floresta já foi colhida

    ChapterNine(Game game) { this.game = game; }

    int count() { return Integer.bitCount(lit); }
    boolean auroraSky() { return game.stage >= HOME; }

    @Override public void reset() { lit = 0; flowerTaken = false; }

    @Override public void buildWorld() {
        GameMap garden = game.maps.get(ChapterFive.GARDEN);
        garden.rect(9, 0, 2, 1, 'm');
        garden.warps.add(new Warp(9, 0, FOREST, 11, 18));
        garden.warps.add(new Warp(10, 0, FOREST, 12, 18));
        for (int x = 9; x <= 10; x++)
            garden.addNpc(new Npc("raizes_antigas", "Raízes antigas", x, 0, new Color(90, 70, 50)).obj("portao").still().stages(0, START));

        GameMap f = new GameMap(FOREST, 24, 20, '#', true);
        f.rect(2, 2, 20, 16, '.');
        f.rect(11, 10, 2, 10, '=');                                          // da entrada até a clareira
        f.rect(4, 9, 16, 1, '=');                                            // leste-oeste
        f.rect(15, 3, 1, 6, '=');                                            // para o norte
        f.rect(6, 10, 1, 6, '=');                                            // para o sudoeste
        // mata fechada: árvores em toda parte, menos junto às trilhas, às clareiras e a quem mora aqui
        int[][] clear = {{12, 6, 2}, {3, 9, 1}, {20, 9, 1}, {15, 2, 1}, {6, 16, 1}, {14, 17, 1}, {8, 16, 1}, {9, 11, 1}};
        for (int y = 2; y < 18; y++) for (int x = 2; x < 22; x++) {
            if (f.t[y][x] != '.') continue;
            boolean open = false;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) if (f.at(x + dx, y + dy) == '=') open = true;
            for (int[] c : clear) if (Math.abs(x - c[0]) <= c[2] && Math.abs(y - c[1]) <= c[2]) open = true;
            if (!open && (x * 7 + y * 13 + x * y) % 5 != 0) f.t[y][x] = '#';
            else if (!open && (x + y) % 3 == 0) f.t[y][x] = 'F';
        }
        f.warps.add(new Warp(11, 19, ChapterFive.GARDEN, 9, 1));
        f.warps.add(new Warp(12, 19, ChapterFive.GARDEN, 10, 1));
        f.addNpc(new Npc("grande_arvore", "Grande Árvore", 12, 6, new Color(90, 130, 80)).obj("grande_arvore").still());
        for (int i = 0; i < 4; i++)
            f.addNpc(new Npc("lanterna_antiga" + i, LANTERN_NAMES[i], LANTERN_SPOTS[i][0], LANTERN_SPOTS[i][1], new Color(200, 200, 150)).obj("lanterna_antiga").still());
        f.addNpc(new Npc("coruja", "Coruja Sábia", 14, 17, new Color(150, 120, 90)).obj("coruja").still());
        f.addNpc(new Npc("flor_antiga", "Flor silvestre", 8, 16, new Color(230, 220, 120)).obj("flor_antiga").still());
        f.addNpc(new Npc("fogueira", "", 9, 11, new Color(244, 152, 64)).obj("fogueira").still());
        game.maps.put(FOREST, f);
    }

    @Override public boolean visible(Npc n) { return !n.id.equals("flor_antiga") || !flowerTaken; }

    @Override public void arrive(GameMap map) {
        if (map.name.equals(FOREST) && game.stage == ENTER) game.setStage(LANTERNS);
    }

    boolean hasDish() { return Arrays.stream(game.cooking.dishes).anyMatch(d -> d > 0); }

    @Override public boolean talk(Npc n) {
        String id = n.id;
        if (id.equals("ceu_cintila") && game.stage == START && game.isNight()) {
            game.setStage(ENTER);
            game.say("Cintila", "Tin... você está vendo? As estrelas piscam mais fraco a cada noite.",
                "É o Eclipse de Aurora. Uma sombra antiga que, de cem em cem anos, tenta apagar o céu.",
                "Só a Grande Árvore, na Floresta Antiga, consegue segurá-la. Mas ela dorme, e suas quatro lanternas se apagaram.",
                "A passagem fica ao norte do Jardim das Constelações. As raízes vão deixar você passar agora.");
            return true;
        }
        if (id.equals("mae") && game.stage == LANTERNS && !hasDish() && (lit & 4) == 0) {
            game.cooking.dishes[0]++;
            game.say(n.name, "Uma lanterna que quer comida? Então leve esta Sopa de Lambari. Lanternas também ficam com fome!");
            return true;
        }
        if (id.equals("mae") && game.stage == HOME) {
            game.say(n.name, "Eu vi da janela! O céu inteiro dançando em verde e violeta... achei que estava sonhando.",
                "Então era você lá na floresta? Uma flor, um peixe, um prato e uma canção... é a cara de Aurora.",
                "Toma, guarde esta semente que caiu no quintal ontem à noite. Brilha igualzinho às raízes que você contou.");
            game.afterDlg = () -> { game.setStage(FINAL); game.itemGet("semente_arvore"); };
            return true;
        }
        if (id.startsWith("lanterna_antiga")) { lantern(id.charAt(15) - '0'); return true; }
        switch (id) {
            case "raizes_antigas" -> game.say("", "Raízes grossas e antigas fecham a passagem. Parecem esperar por um pedido do céu.");
            case "coruja" -> game.say("Coruja Sábia", game.stage < LANTERNS ? "Huu... poucos chegam até aqui. A floresta lembra de quem já cuidou da vila."
                : game.stage == LANTERNS ? "Huu. Flor, rio, lar e canto: quatro lanternas, quatro presentes. O que a vila tem de melhor."
                : game.stage == TREE ? "A Grande Árvore só fala sob as estrelas. Descanse na fogueira, se precisar."
                : "Huu! Que céu. Nem eu, com toda a minha idade, tinha visto uma aurora assim.");
            case "flor_antiga" -> {
                flowerTaken = true;
                game.gardening.stock[0]++;
                game.audio.sfx("pickup");
                game.say("", "Uma margarida silvestre cresce entre as raízes. Você a colhe com cuidado. (+1 Margarida na cesta)");
            }
            case "grande_arvore" -> tree();
            default -> { return false; }
        }
        return true;
    }

    void lantern(int i) {
        String name = LANTERN_NAMES[i];
        if ((lit & (1 << i)) != 0) { game.say(name, "A chama brilha calma. Esta lanterna já está acesa."); return; }
        if (game.stage != LANTERNS) { game.say(name, "Uma lanterna de pedra coberta de musgo. Está fria e apagada."); return; }
        switch (i) {
            case 0 -> {
                int flower = -1; for (int f = 0; f < 4; f++) if (game.gardening.stock[f] > 0) { flower = f; break; }
                if (flower < 0) { game.say(name, "Na pedra está gravada uma flor. Traga uma flor do quintal; há uma silvestre aqui perto, ao sul."); return; }
                game.gardening.stock[flower]--;
                light(i, "Você deixa uma " + Gardening.NAMES[flower] + " no altar da lanterna. As pétalas brilham e a chama acende!");
            }
            case 1 -> {
                int fish = -1; for (int f = 0; f < 8; f++) if (game.bonds.stock(f) > 0) { fish = f; break; }
                if (fish < 0) { game.say(name, "Na pedra está gravado um peixe. Traga um peixe do seu estoque (o álbum não perde a captura)."); return; }
                game.bonds.spent[fish]++;
                light(i, "Você oferece um " + VillageLife.FISH[fish].name() + ". A água da pedra ondula e a chama acende, azul como o rio!");
            }
            case 2 -> {
                int dish = -1; for (int d = 0; d < Cooking.RECIPES.length; d++) if (game.cooking.dishes[d] > 0) { dish = d; break; }
                if (dish < 0) { game.say(name, "Na pedra está gravada uma panela. Traga um prato da lancheira; a Mamãe sempre ajuda."); return; }
                game.cooking.dishes[dish]--;
                light(i, "Você serve " + Cooking.RECIPES[dish].name() + ". Um cheiro de casa enche a floresta e a chama acende, dourada!");
            }
            default -> game.melody.open(name, "A canção da floresta está gravada na pedra: DÓ, MI, RÉ, SOL, DÓ.", SONG,
                () -> light(3, "As notas sobem pelos galhos. A floresta inteira repete a canção, e a chama acende!"));
        }
    }

    void light(int i, String text) {
        lit |= 1 << i;
        game.audio.sfx("fire");
        if (lit == 15) {
            game.setStage(TREE);
            game.say(LANTERN_NAMES[i], text, "As quatro lanternas brilham juntas. No centro da floresta, a Grande Árvore suspira. Volte à noite.");
        } else game.say(LANTERN_NAMES[i], text, "Lanternas acesas: " + count() + "/4.");
        game.save(false);
    }

    void tree() {
        if (game.stage < TREE) { game.say("Grande Árvore", "Uma árvore enorme, mais velha que a vila. Ela dorme, e suas raízes estão frias."); return; }
        if (game.stage == TREE) {
            if (!game.isNight()) { game.say("Grande Árvore", "As folhas se mexem devagar, como se a árvore sonhasse. Ela só desperta sob as estrelas."); return; }
            game.setStage(HOME);
            game.audio.sfx("fanfare");
            game.say("Grande Árvore", "Huuummm... quanto tempo. Uma flor, um peixe, um prato e uma canção. A vila ainda se lembra.",
                "Eu seguro a sombra há mil anos, pequeno. Mas ninguém segura o céu sozinho. Por isso as lanternas: elas são vocês.",
                "As raízes da árvore brilham, sobem pelo tronco e explodem em luz pelo céu.",
                "O eclipse se desfaz. Em seu lugar, faixas verdes, azuis e violeta dançam sobre Aurora: uma aurora de verdade.",
                "Volte para casa. Há alguém que vai querer ver isso com você.");
            return;
        }
        game.say("Grande Árvore", "Huuum. Toda noite clara, a aurora vai lembrar a vila de que o céu é de todos.");
    }

    // ---------- Visual ----------
    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.startsWith("lanterna_antiga")) {
            int i = n.id.charAt(15) - '0';
            boolean on = (lit & (1 << i)) != 0;
            g.setColor(new Color(90, 96, 90)); g.fillRect(x + 11, y + 18, 10, 16);
            g.setColor(new Color(120, 128, 118)); g.fillRoundRect(x + 7, y + 6, 18, 14, 6, 6);
            g.setColor(new Color(70, 110, 60)); g.fillRect(x + 7, y + 6, 18, 3);
            Color[] flames = {new Color(250, 160, 200), new Color(120, 190, 255), new Color(255, 200, 90), new Color(190, 150, 255)};
            g.setColor(on ? flames[i] : new Color(50, 50, 56)); g.fillOval(x + 12, y + 10, 8, 8);
            return true;
        }
        switch (n.id) {
            case "grande_arvore" -> {
                g.setColor(new Color(0, 0, 0, 50)); g.fillOval(x - 20, y + 26, 72, 14);
                g.setColor(new Color(96, 66, 44)); g.fillRect(x + 8, y + 6, 16, 30);
                g.setColor(new Color(74, 50, 34)); g.fillRect(x + 8, y + 6, 4, 30);
                if (game.stage >= TREE) { g.setColor(new Color(150, 255, 200, 120)); g.fillRect(x + 14, y + 8, 3, 26); }
                return true;
            }
            case "coruja" -> {
                g.setColor(new Color(110, 80, 50)); g.fillRect(x + 6, y + 30, 20, 3);
                g.setColor(new Color(150, 116, 80)); g.fillOval(x + 9, y + 12, 14, 18);
                g.setColor(new Color(240, 230, 200)); g.fillOval(x + 10, y + 14, 6, 6); g.fillOval(x + 16, y + 14, 6, 6);
                g.setColor(new Color(40, 30, 30)); g.fillRect(x + 12, y + 16, 2, 2); g.fillRect(x + 18, y + 16, 2, 2);
                g.setColor(new Color(230, 170, 60)); g.fillRect(x + 15, y + 20, 2, 2);
                return true;
            }
            case "flor_antiga" -> {
                g.setColor(new Color(60, 120, 50)); g.fillRect(x + 15, y + 18, 2, 12);
                g.setColor(new Color(255, 255, 240)); for (int k = 0; k < 6; k++) g.fillOval(x + 12 + (int) (Math.cos(k) * 5), y + 12 + (int) (Math.sin(k) * 5), 6, 6);
                g.setColor(new Color(250, 210, 70)); g.fillOval(x + 13, y + 13, 6, 6);
                return true;
            }
            default -> { return false; }
        }
    }

    /** Copa da Grande Árvore (abaixo dos personagens) e brilho do musgo da floresta. */
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals(FOREST)) return;
        int x = 12 * Game.T - camX, y = 6 * Game.T - camY;
        Color leaves = game.stage >= HOME ? new Color(70, 150, 110) : new Color(52, 110, 70);
        g.setColor(new Color(30, 60, 40)); g.fillOval(x - 46, y - 70, 124, 84);
        g.setColor(leaves); g.fillOval(x - 42, y - 66, 116, 76);
        g.setColor(leaves.brighter()); g.fillOval(x - 24, y - 60, 50, 30);
    }

    @Override public void drawSky(Graphics2D g, int camX, int camY) {
        if (game.map.name.equals(FOREST)) { g.setColor(new Color(10, 40, 30, 60)); g.fillRect(0, 0, Game.W, Game.H); }
        if (!game.map.outdoor || game.map.dark || game.night() < 0.5) return;
        long f = game.access.reduced ? 0 : game.frame;
        if (game.stage >= ENTER && game.stage <= TREE) {                        // o eclipse escurece as noites
            g.setColor(new Color(30, 10, 50, 70)); g.fillRect(0, 0, Game.W, Game.H);
        }
        if (game.map.name.equals(FOREST)) {                                     // musgo luminoso
            for (int i = 0; i < 14; i++) {
                int mx = (i * 137) % Game.W, my = 60 + (i * 89) % (Game.H - 80);
                float a = (float) (0.4 + 0.4 * Math.sin(f / 30.0 + i));
                game.glow(g, mx, my, 10, new Color(150, 255, 200, 140), new Color(150, 255, 200, 0), a);
            }
        }
        if (!auroraSky()) return;
        Color[] colors = {new Color(80, 255, 170), new Color(90, 200, 255), new Color(190, 120, 255)};
        Stroke old = g.getStroke();
        for (int band = 0; band < 3; band++) {
            int base = 18 + band * 16;
            for (int layer = 0; layer < 4; layer++) {
                g.setStroke(new BasicStroke(10 - layer * 2));
                Color c = colors[band];
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) ((12 + layer * 7) * game.night())));
                int[] xs = new int[33], ys = new int[33];
                for (int k = 0; k <= 32; k++) {
                    xs[k] = k * Game.W / 32;
                    ys[k] = base + (int) (Math.sin(k / 4.0 + f / 70.0 + band) * 10 + Math.sin(k / 9.0 - f / 110.0) * 6) + layer * 3;
                }
                g.drawPolyline(xs, ys, 33);
            }
        }
        g.setStroke(old);
    }

    @Override public void save(Properties p) { p.setProperty("eclipse.lanternas", "" + lit); p.setProperty("eclipse.flor", "" + flowerTaken); }
    @Override public void load(Properties p) {
        lit = game.stage >= TREE ? 15 : game.stage == LANTERNS ? game.life.number(p, "eclipse.lanternas", 0, 15) : 0;
        flowerTaken = Boolean.parseBoolean(p.getProperty("eclipse.flor", "false"));
    }
    @Override public String signature() { return "e" + lit + flowerTaken; }
}
