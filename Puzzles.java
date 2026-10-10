import java.awt.*;
import java.util.*;

/**
 * Quebra-cabeças de cenário, à moda de Zelda, Pokémon e Golden Sun:
 * - pedras empurráveis sobre placas (a ponte do Vale dos Ecos, cap. 8, e as Ruínas do Vale, opcionais);
 *   uma pedra do eco recoloca as pedras, e sair do mapa também, enquanto não estiver resolvido;
 * - pegadas de estrela na Gruta do Eco, que só aparecem com a Lanterna (cap. 4): pise da menor à maior luz;
 * - um baú preso no gelo da Trilha da Montanha, que só o Cristal de Calor derrete.
 * Os três baús opcionais guardam moedas e decorações.
 */
final class Puzzles implements Feature {
    record Push(String id, String map, int[][] boulders, int[][] plates, int[] reset) {}
    static final Push[] PUSH = {
        new Push("ponte", ChapterEight.VALLEY, new int[][]{{5, 13}, {12, 14}, {19, 12}}, new int[][]{{8, 10}, {12, 10}, {16, 10}}, new int[]{2, 16}),
        new Push("ruina", ChapterEight.VALLEY, new int[][]{{4, 4}, {6, 4}}, new int[][]{{3, 3}, {5, 3}}, new int[]{8, 1})
    };
    static final int BRIDGE = 0, RUINS = 1;
    /** Pegadas na Gruta do Eco, em ordem crescente de brilho. */
    static final int[][] STARS = {{7, 2}, {10, 3}, {14, 2}, {16, 5}, {15, 6}};
    static final String[] CHESTS = {"bau_ruina", "bau_gruta", "bau_gelo"};
    static final String[] CHEST_ITEMS = {"Partitura Perdida", "Estrela da Gruta", "Floco Eterno"};
    static final int[] CHEST_COINS = {60, 80, 50};

    final Game game;
    final boolean[] solved = new boolean[PUSH.length];
    final boolean[] opened = new boolean[CHESTS.length];
    final Npc[][] stones = new Npc[PUSH.length][];
    final double[] slide = new double[8];
    int starStep;
    boolean starsDone;

    Puzzles(Game game) { this.game = game; }

    int openedCount() { int n = 0; for (boolean o : opened) if (o) n++; return n; }

    @Override public void reset() { Arrays.fill(solved, false); Arrays.fill(opened, false); starStep = 0; starsDone = false; }

    @Override public void buildWorld() {
        for (int p = 0; p < PUSH.length; p++) {
            GameMap m = game.maps.get(PUSH[p].map());
            stones[p] = new Npc[PUSH[p].boulders().length];
            for (int i = 0; i < stones[p].length; i++) {
                stones[p][i] = new Npc("pedra_" + p + "_" + i, "Pedra", PUSH[p].boulders()[i][0], PUSH[p].boulders()[i][1], new Color(130, 125, 120)).obj("pedra").still();
                m.addNpc(stones[p][i]);
            }
            m.addNpc(new Npc("eco_reset_" + p, "Pedra do eco", PUSH[p].reset()[0], PUSH[p].reset()[1], new Color(150, 190, 200)).obj("eco_reset").still());
        }
        GameMap valley = game.maps.get(ChapterEight.VALLEY);
        valley.t[1][1] = 'R'; valley.t[1][3] = 'R';
        valley.addNpc(new Npc("portao_ruina", "Portão das ruínas", 2, 2, new Color(120, 120, 130)).obj("portao").still());
        valley.addNpc(new Npc("bau_ruina", "Baú", 2, 1, new Color(170, 120, 60)).obj("bau").still());
        GameMap cave = game.maps.get("Gruta do Eco");
        cave.addNpc(new Npc("mural_estrelas", "Inscrição", 6, 4, new Color(150, 150, 170)).obj("mural").still().stages(16, 99));
        cave.addNpc(new Npc("bau_gruta", "Baú", 6, 6, new Color(170, 120, 60)).obj("bau").still());
        game.maps.get("Trilha da Montanha").addNpc(new Npc("bau_gelo", "Baú congelado", 19, 2, new Color(170, 228, 250)).obj("bau").still());
    }

    /** Volta as pedras ao início (puzzles ainda não resolvidos). */
    void resetStones(int p) {
        if (solved[p]) return;
        for (int i = 0; i < stones[p].length; i++) { stones[p][i].x = PUSH[p].boulders()[i][0]; stones[p][i].y = PUSH[p].boulders()[i][1]; }
    }
    void solve(int p) {
        solved[p] = true;
        for (int i = 0; i < stones[p].length; i++) { stones[p][i].x = PUSH[p].plates()[i][0]; stones[p][i].y = PUSH[p].plates()[i][1]; }
    }
    boolean covered(int p) {
        for (int[] plate : PUSH[p].plates()) {
            boolean on = false;
            for (Npc s : stones[p]) on |= s.x == plate[0] && s.y == plate[1];
            if (!on) return false;
        }
        return true;
    }

    @Override public void arrive(GameMap map) {
        for (int p = 0; p < PUSH.length; p++) if (PUSH[p].map().equals(map.name)) resetStones(p);
        if (map.name.equals("Gruta do Eco") && !starsDone) starStep = 0;
    }

    @Override public boolean push(int x, int y, int dir) {
        for (int p = 0; p < PUSH.length; p++) {
            if (solved[p] || !PUSH[p].map().equals(game.map.name)) continue;
            for (int i = 0; i < stones[p].length; i++) {
                Npc s = stones[p][i];
                if (s.x != x || s.y != y) continue;
                int nx = x + Game.DX[dir], ny = y + Game.DY[dir];
                if (!open(nx, ny)) { game.audio.sfx("bad"); return false; }
                s.x = nx; s.y = ny; s.dir = dir; slide[p * 4 + i] = 1;
                game.audio.sfx("slide");
                if (covered(p)) {
                    solved[p] = true;
                    game.audio.sfx("fanfare");
                    if (p == BRIDGE) game.chapterEight.bridgeRaised();
                    else game.toast("Click! O portão das ruínas se abriu.");
                    game.save(false);
                }
                return true;
            }
        }
        return false;
    }
    /** Pedras param em tiles livres: nada sólido, sem água, sem personagem, sem passagem. */
    boolean open(int x, int y) {
        GameMap m = game.map;
        if (m.solid(x, y) || m.at(x, y) == 'P') return false;
        for (Warp w : m.warps) if (w.x() == x && w.y() == y) return false;
        for (Npc n : m.npcs) if (game.vis(n) && n.x == x && n.y == y) return false;
        return !(game.tx == x && game.ty == y);
    }

    @Override public void update(double dt) {
        for (int i = 0; i < slide.length; i++) if (slide[i] > 0) slide[i] = Math.max(0, slide[i] - dt * 6);
    }

    @Override public void stepped(int x, int y) {
        if (!game.map.name.equals("Gruta do Eco") || game.stage < 16 || starsDone) return;
        for (int i = 0; i < STARS.length; i++) {
            if (STARS[i][0] != x || STARS[i][1] != y) continue;
            if (i == starStep) {
                starStep++;
                game.audio.sfx("bell" + (i % 4));
                if (starStep == STARS.length) { starsDone = true; game.audio.sfx("fanfare"); game.toast("As pegadas brilham juntas! Um baú surgiu no canto do salão."); game.save(false); }
            } else if (i > 0 || starStep > 0) { starStep = i == 0 ? 1 : 0; game.audio.sfx("bad"); }
            return;
        }
    }

    @Override public boolean visible(Npc n) {
        return switch (n.id) {
            case "portao_ruina" -> !solved[RUINS];
            case "bau_gruta" -> starsDone;
            default -> true;
        };
    }

    @Override public boolean talk(Npc n) {
        String id = n.id;
        if (id.startsWith("eco_reset_")) {
            int p = id.charAt(10) - '0';
            if (solved[p]) game.say("", "A pedra do eco canta baixinho. Tudo aqui já está no lugar.");
            else { resetStones(p); game.audio.sfx("twinkle"); game.say("", "Você toca a pedra do eco. Um som grave ecoa, e as pedras voltam aos seus lugares."); }
            return true;
        }
        if (id.startsWith("pedra_")) { game.say("", "Uma pedra redonda e pesada. Talvez dê para empurrá-la andando contra ela."); return true; }
        switch (id) {
            case "portao_ruina" -> game.say("", "Um portão de pedra antigo. Duas placas no chão parecem esperar por algo pesado.");
            case "mural_estrelas" -> game.say("", "\"Pegadas de estrela dormem neste salão. Só a luz da lanterna as revela.\"",
                "\"Caminhe sobre elas da menor à maior luz, e o salão guardará um presente.\"");
            case "bau_gelo" -> {
                if (opened[2]) { game.say("", "O baú está vazio. O gelo derretido virou uma pocinha brilhante."); return true; }
                if (game.stage < 18) { game.say("", "Um baú preso num bloco de gelo grosso. Só um calor muito forte derreteria isso."); return true; }
                game.audio.sfx("fire");
                openChest(2, "Você aproxima o Cristal de Calor: ainda resta um pouco de calor nele. O gelo derrete e o baú se abre!");
            }
            case "bau_ruina", "bau_gruta" -> {
                int c = id.equals("bau_ruina") ? 0 : 1;
                if (opened[c]) game.say("", "O baú está vazio.");
                else openChest(c, c == 0 ? "Dentro do baú das ruínas, uma partitura antiga, ainda legível." : "O baú que surgiu com as estrelas guarda uma estrela de cristal.");
            }
            default -> { return false; }
        }
        return true;
    }

    void openChest(int c, String text) {
        opened[c] = true;
        game.home.coins = Math.min(HomeDecor.MAX_COINS, game.home.coins + CHEST_COINS[c]);
        game.itemGet("bau");
        game.audio.sfx("pickup");
        game.say("", text, "Você ganhou " + CHEST_COINS[c] + " moedas e a decoração " + CHEST_ITEMS[c] + " (tecla H em casa).",
            openedCount() == CHESTS.length ? "Os três enigmas escondidos de Aurora foram resolvidos!" : "Enigmas resolvidos: " + openedCount() + "/" + CHESTS.length + ".");
    }

    // ---------- Desenho ----------
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        int T = Game.T;
        for (int p = 0; p < PUSH.length; p++) {
            if (!PUSH[p].map().equals(game.map.name)) continue;
            for (int[] plate : PUSH[p].plates()) {
                int x = plate[0] * T - camX, y = plate[1] * T - camY;
                boolean on = Arrays.stream(stones[p]).anyMatch(s -> s.x == plate[0] && s.y == plate[1]);
                g.setColor(new Color(120, 125, 140)); g.fillRect(x + 3, y + 3, 26, 26);
                g.setColor(on ? new Color(140, 220, 200) : new Color(170, 175, 190)); g.fillRect(x + 6, y + 6, 20, 20);
            }
        }
        if (game.map.name.equals("Gruta do Eco") && game.stage >= 16 && !starsDone) {
            for (int i = 0; i < STARS.length; i++) {
                int x = STARS[i][0] * T - camX + 16, y = STARS[i][1] * T - camY + 16, r = 3 + i * 2;
                boolean lit = i < starStep;
                game.glow(g, x, y, 10 + i * 5, new Color(255, 245, 190, lit ? 200 : 120), new Color(255, 245, 190, 0), 1);
                game.chapter.star(g, x, y, r, lit ? new Color(255, 236, 140) : new Color(220, 225, 255));
            }
        }
    }

    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.startsWith("pedra_")) {
            String[] parts = n.id.split("_");
            int p = Integer.parseInt(parts[1]), i = Integer.parseInt(parts[2]);
            double s = slide[p * 4 + i];
            int ox = (int) (-Game.DX[n.dir] * s * Game.T), oy = (int) (-Game.DY[n.dir] * s * Game.T);
            g.setColor(new Color(0, 0, 0, 50)); g.fillOval(x + 4 + ox, y + 28 + oy, 24, 7);
            g.setColor(new Color(128, 122, 116)); g.fillOval(x + 3 + ox, y + 8 + oy, 26, 24);
            g.setColor(new Color(160, 155, 148)); g.fillOval(x + 8 + ox, y + 11 + oy, 10, 7);
            return true;
        }
        if (n.id.startsWith("eco_reset_")) {
            g.setColor(new Color(110, 140, 150)); g.fillRoundRect(x + 8, y + 10, 16, 22, 6, 6);
            g.setColor(new Color(170, 230, 240)); g.drawArc(x + 2, y + 6, 28, 14, 20, 140); g.drawArc(x - 2, y + 2, 36, 18, 30, 120);
            return true;
        }
        if (n.id.startsWith("bau_")) {
            int c = Arrays.asList(CHESTS).indexOf(n.id);
            if (n.id.equals("bau_gelo") && !opened[2]) {
                g.setColor(new Color(170, 228, 250, 220)); g.fillRoundRect(x + 1, y + 6, 30, 28, 6, 6);
                g.setColor(new Color(255, 255, 255, 160)); g.drawLine(x + 6, y + 10, x + 12, y + 16);
            }
            g.setColor(new Color(140, 90, 45)); g.fillRect(x + 6, y + 14, 20, 16);
            g.setColor(c >= 0 && opened[c] ? new Color(90, 60, 30) : new Color(180, 120, 60)); g.fillRect(x + 6, y + 12, 20, 6);
            g.setColor(new Color(230, 190, 80)); g.fillRect(x + 14, y + 17, 4, 5);
            return true;
        }
        return false;
    }

    @Override public void save(Properties p) {
        StringBuilder b = new StringBuilder();
        for (boolean s : solved) b.append(s ? '1' : '0');
        p.setProperty("enigmas.empurrar", b.toString());
        StringBuilder c = new StringBuilder();
        for (boolean o : opened) c.append(o ? '1' : '0');
        p.setProperty("enigmas.baus", c.toString());
        p.setProperty("enigmas.estrelas", "" + starsDone);
    }
    @Override public void load(Properties p) {
        String s = p.getProperty("enigmas.empurrar", ""), c = p.getProperty("enigmas.baus", "");
        for (int i = 0; i < PUSH.length; i++) { solved[i] = false; resetStones(i); if (i < s.length() && s.charAt(i) == '1') solve(i); }
        for (int i = 0; i < opened.length; i++) opened[i] = i < c.length() && c.charAt(i) == '1';
        starsDone = Boolean.parseBoolean(p.getProperty("enigmas.estrelas", "false")); starStep = 0;
        if (solved[BRIDGE]) game.chapterEight.applyBridge();
    }
    @Override public String signature() { return Arrays.toString(solved) + Arrays.toString(opened) + starsDone; }
}
