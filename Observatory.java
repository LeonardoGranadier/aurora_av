import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.*;

/**
 * Restauração do observatório do avô do Prof. Carvalho, no nordeste da vila, como os pacotes do Centro Comunitário
 * de Stardew Valley: cada conjunto doado muda a vila para sempre. O último exige a chave da corrente de trocas e
 * abre a luneta, que mostra as constelações de Aurora à noite.
 */
final class Observatory implements Feature {
    record Bundle(String name, String needs, String reward) {}
    static final Bundle[] BUNDLES = {
        new Bundle("Peixes da vila", "1 Lambari, 1 Carpa Dourada e 1 Bagre da Lua", "Lanternas acendem nos caminhos da vila à noite."),
        new Bundle("Flores do quintal", "1 Margarida, 1 Tulipa e 1 Girassol", "Canteiros floridos enfeitam a praça."),
        new Bundle("Tesouros do mar", "6 conchas secretas encontradas e 30 moedas", "Uma luneta pública aparece na Praia Azul."),
        new Bundle("Luz das estrelas", "A Chave do Observatório e 100 moedas", "O observatório abre: veja as constelações à noite.")
    };
    static final int[] VILLAGE_FISH = {0, 1, 2};
    static final int[][] LANTERNS = {{5, 9}, {11, 9}, {17, 9}, {23, 9}, {13, 13}, {16, 15}, {13, 19}, {21, 7}};
    static final int[][] BEDS = {{9, 13}, {10, 13}, {12, 13}, {13, 14}, {16, 13}, {17, 13}, {16, 14}};

    record Constellation(String name, String story, int[][] stars, int[][] lines) {}
    static final Constellation[] SKY = {
        new Constellation("A Concha", "Brilha sobre o farol desde a noite em que a Concha Brilhante voltou para casa.",
            new int[][]{{90, 150}, {120, 120}, {150, 112}, {178, 128}, {190, 160}, {140, 175}}, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 0}, {5, 2}}),
        new Constellation("O Barco da Capitã", "Dizem que aponta o caminho da Ilha das Estrelas para quem se perdeu no mar.",
            new int[][]{{250, 200}, {290, 210}, {330, 205}, {300, 170}, {300, 140}}, new int[][]{{0, 1}, {1, 2}, {1, 3}, {3, 4}}),
        new Constellation("Cintila", "Uma estrela pequena, mas a mais brilhante da noite. Pisca quando alguém olha para o farol.",
            new int[][]{{420, 110}, {435, 140}, {465, 145}, {440, 160}, {450, 192}, {420, 172}, {392, 190}, {400, 160}, {376, 145}, {406, 140}},
            new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 7}, {7, 8}, {8, 9}, {9, 0}}),
        new Constellation("O Peixe Dourado", "Os pescadores de Aurora pedem a ele boa sorte antes de lançar a linha.",
            new int[][]{{110, 260}, {150, 245}, {190, 255}, {215, 240}, {215, 275}, {190, 262}}, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 2}}),
        new Constellation("A Flor-Constelação", "Nasceu do jardim que você fez florescer. Cada pétala é um laço entre o céu e a vila.",
            new int[][]{{470, 260}, {470, 225}, {500, 245}, {500, 280}, {470, 295}, {440, 280}, {440, 245}},
            new int[][]{{0, 1}, {0, 2}, {0, 3}, {0, 4}, {0, 5}, {0, 6}, {1, 2}, {2, 3}, {3, 4}, {4, 5}, {5, 6}, {6, 1}}),
        new Constellation("Os Sinos", "Quatro estrelas que tocam uma melodia que só Lucas diz conseguir ouvir.",
            new int[][]{{300, 300}, {330, 290}, {360, 300}, {330, 320}}, new int[][]{{0, 1}, {1, 2}, {2, 3}, {3, 0}})
    };

    final Game game;
    int done;           // bits dos conjuntos concluídos
    int selected, returnState = Game.PLAY;
    boolean viewing;    // true: céu estrelado; false: painel de doações
    String status = "";

    Observatory(Game game) { this.game = game; }

    boolean has(int bundle) { return (done & (1 << bundle)) != 0; }
    boolean complete() { return done == (1 << BUNDLES.length) - 1; }

    boolean ready(int bundle) {
        return switch (bundle) {
            case 0 -> Arrays.stream(VILLAGE_FISH).allMatch(f -> game.bonds.stock(f) > 0);
            case 1 -> game.gardening.stock[0] > 0 && game.gardening.stock[1] > 0 && game.gardening.stock[2] > 0;
            case 2 -> game.shells.found() >= 6 && game.home.coins >= 30;
            default -> game.trades.complete() && game.home.coins >= 100;
        };
    }

    /** Doa o conjunto; se o jogo não puder ser salvo, tudo volta ao que era. */
    boolean donate(int bundle) {
        if (has(bundle) || !ready(bundle)) return false;
        int[] spent = game.bonds.spent.clone(), flowers = game.gardening.stock.clone();
        int coins = game.home.coins, before = done;
        switch (bundle) {
            case 0 -> { for (int f : VILLAGE_FISH) game.bonds.spent[f]++; }
            case 1 -> { for (int i = 0; i < 3; i++) game.gardening.stock[i]--; }
            case 2 -> game.home.coins -= 30;
            default -> game.home.coins -= 100;
        }
        done |= 1 << bundle;
        if (game.save(false)) return true;
        System.arraycopy(spent, 0, game.bonds.spent, 0, spent.length);
        System.arraycopy(flowers, 0, game.gardening.stock, 0, flowers.length);
        game.home.coins = coins; done = before;
        return false;
    }

    @Override public void reset() { done = 0; selected = 0; viewing = false; status = ""; }
    @Override public void buildWorld() {
        game.maps.get("Vila Aurora").addNpc(new Npc("observatorio", "Observatório", 25, 4, new Color(150, 160, 190)).obj("observatorio").still());
        game.maps.get("Praia Azul").addNpc(new Npc("luneta_praia", "", 12, 4, new Color(120, 110, 90)).obj("luneta").still());
    }
    @Override public boolean visible(Npc n) { return !n.id.equals("luneta_praia") || has(2); }

    @Override public boolean talk(Npc n) {
        if (n.id.equals("luneta_praia")) {
            game.say("", game.isNight() ? "Pela luneta, o farol parece uma estrela pousada na torre." : "Pela luneta, você vê a Ilha das Estrelas no horizonte. Um barquinho de pesca acena!");
            return true;
        }
        if (!n.id.equals("observatorio")) return false;
        selected = 0; status = ""; returnState = Game.PLAY;
        viewing = complete() && game.isNight();
        if (complete() && !game.isNight()) { game.say("Observatório", "A cúpula está aberta, mas o céu está claro demais. Volte à noite para ver as constelações."); return true; }
        game.down.clear(); game.state = Game.OBSERVATORY;
        return true;
    }

    @Override public boolean owns(int state) { return state == Game.OBSERVATORY; }
    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        int count = viewing ? SKY.length : BUNDLES.length, old = selected;
        if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT) selected = (selected + count - 1) % count;
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT) selected = (selected + 1) % count;
        else if (act && viewing) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        else if (act) {
            int b = selected;
            if (has(b)) status = "Este conjunto já foi doado.";
            else if (!ready(b)) status = "Ainda falta: " + BUNDLES[b].needs() + ".";
            else game.access.ask("Doar " + BUNDLES[b].needs() + " ao observatório?", () -> {
                if (donate(b)) {
                    game.audio.sfx("fanfare");
                    status = "Conjunto doado! " + BUNDLES[b].reward();
                    if (complete()) status = "Observatório restaurado! Volte à noite para ver as constelações.";
                } else status = "Não foi possível salvar a doação. Nada foi gasto.";
            });
        }
        if (selected != old) game.audio.sfx("select");
    }

    // ---------- Mudanças na vila ----------
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals("Vila Aurora")) return;
        int T = Game.T;
        if (has(1)) {
            Color[] petals = {new Color(246, 242, 225), new Color(229, 112, 153), new Color(245, 192, 66)};
            for (int[] b : BEDS) {
                int x = b[0] * T - camX, y = b[1] * T - camY;
                g.setColor(new Color(110, 80, 50, 140)); g.fillRoundRect(x + 3, y + 18, 26, 10, 6, 6);
                for (int i = 0; i < 4; i++) { g.setColor(petals[(b[0] + i) % 3]); g.fillOval(x + 5 + i * 6, y + 14 + (i % 2) * 4, 6, 6); }
            }
        }
        if (has(0)) {
            double night = game.night();
            for (int[] l : LANTERNS) {
                int x = l[0] * T - camX, y = l[1] * T - camY;
                if (night > 0) {
                    g.setPaint(new RadialGradientPaint(new Point2D.Float(x + 16, y + 20), 46, new float[]{0f, 1f},
                        new Color[]{new Color(255, 214, 120, (int) (120 * night)), new Color(255, 214, 120, 0)}));
                    g.fillRect(x - 32, y - 28, 96, 96);
                }
                g.setColor(new Color(70, 60, 55)); g.fillRect(x + 14, y + 16, 4, 12);
                g.setColor(night > 0.3 ? new Color(255, 225, 140) : new Color(210, 205, 190)); g.fillRoundRect(x + 11, y + 10, 10, 9, 3, 3);
            }
        }
    }

    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.equals("luneta_praia")) {
            g.setColor(new Color(90, 80, 70)); g.drawLine(x + 16, y + 22, x + 9, y + 34); g.drawLine(x + 16, y + 22, x + 23, y + 34);
            g.setColor(new Color(180, 150, 80)); g.fillRoundRect(x + 6, y + 10, 22, 8, 4, 4);
            g.setColor(new Color(130, 100, 50)); g.fillRect(x + 24, y + 9, 5, 10);
            return true;
        }
        if (!n.id.equals("observatorio")) return false;
        boolean open = has(3);
        g.setColor(new Color(205, 200, 190)); g.fillRect(x - 4, y + 14, 40, 22);
        g.setColor(new Color(150, 140, 130)); g.drawRect(x - 4, y + 14, 40, 22);
        g.setColor(open ? new Color(120, 150, 205) : new Color(140, 140, 150)); g.fillArc(x - 4, y - 6, 40, 40, 0, 180);
        g.setColor(open ? new Color(40, 50, 90) : new Color(110, 110, 120)); g.fillRect(x + 12, y - 4, 8, 18);
        if (open) { g.setColor(new Color(180, 150, 80)); g.fillRect(x + 14, y - 10, 5, 12); }
        g.setColor(new Color(100, 80, 60)); g.fillRect(x + 12, y + 24, 8, 12);
        return true;
    }

    // ---------- Telas ----------
    @Override public void draw(Graphics2D g) { if (viewing) drawSky(g); else drawBundles(g); }

    void drawBundles(Graphics2D g) {
        game.life.panel(g, "Observatório de Aurora", "Doe conjuntos para restaurar o observatório · Z: doar · Esc: sair");
        for (int i = 0; i < BUNDLES.length; i++) {
            int y = 118 + i * 64;
            if (i == selected) { g.setColor(new Color(207, 231, 221)); g.fillRect(38, y - 6, 564, 60); }
            boolean got = has(i), ready = ready(i);
            g.setColor(got ? new Color(92, 160, 110) : ready ? new Color(230, 180, 70) : new Color(180, 190, 196));
            g.fillOval(46, y + 6, 26, 26);
            g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 15));
            g.drawString(got ? "✓" : ready ? "!" : "" + (i + 1), got ? 52 : 55, y + 25);
            game.life.text(g, BUNDLES[i].name() + (got ? "  (doado)" : ready ? "  (pronto para doar)" : ""), 84, y + 12, 500, 15);
            game.life.text(g, got ? BUNDLES[i].reward() : "Precisa de: " + BUNDLES[i].needs(), 84, y + 34, 500, 13);
        }
        game.life.text(g, status, 44, 392, 552, 14);
        if (!game.trades.complete()) game.life.text(g, "O último conjunto pede a chave do observatório. " + game.trades.hint(), 44, 430, 552, 12);
    }

    void drawSky(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(6, 8, 30), 0, Game.H, new Color(28, 30, 80)));
        g.fillRect(0, 0, Game.W, Game.H);
        Random r = new Random(11);
        for (int i = 0; i < 160; i++) {
            int a = 60 + r.nextInt(150);
            if (!game.access.reduced) a = (int) (a * (0.75 + 0.25 * Math.sin(game.frame / 18.0 + i)));
            g.setColor(new Color(255, 255, 255, Math.max(0, a))); g.fillRect(r.nextInt(Game.W), r.nextInt(380), 2, 2);
        }
        for (int c = 0; c < SKY.length; c++) {
            Constellation k = SKY[c]; boolean sel = c == selected;
            g.setColor(sel ? new Color(255, 225, 140, 220) : new Color(160, 180, 255, 90));
            g.setStroke(new BasicStroke(sel ? 2 : 1));
            for (int[] l : k.lines()) g.drawLine(k.stars()[l[0]][0], k.stars()[l[0]][1], k.stars()[l[1]][0], k.stars()[l[1]][1]);
            for (int[] s : k.stars()) { g.setColor(sel ? new Color(255, 245, 200) : new Color(225, 230, 255)); g.fillOval(s[0] - 3, s[1] - 3, 6, 6); }
        }
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(0, 0, 0, 150)); g.fillRoundRect(20, 386, Game.W - 40, 80, 14, 14);
        Constellation k = SKY[selected];
        g.setColor(new Color(255, 230, 150)); g.setFont(new Font("Serif", Font.BOLD, 20)); g.drawString(k.name(), 36, 414);
        g.setColor(new Color(235, 238, 255)); g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        int y = 436; for (String line : game.wrap(k.story(), g.getFontMetrics(), Game.W - 80)) { g.drawString(line, 36, y); y += 17; }
        g.setColor(new Color(255, 255, 255, 150)); g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString("Setas: outra constelação · Z / Esc: sair", Game.W - 230, 30);
    }

    @Override public void save(Properties p) { p.setProperty("observatorio.conjuntos", "" + done); }
    @Override public void load(Properties p) { done = game.life.number(p, "observatorio.conjuntos", 0, (1 << BUNDLES.length) - 1); }
    @Override public String signature() { return "o" + done; }
}
