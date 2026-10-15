import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * O barco próprio, a Gaivota: depois do capítulo 3, o Seu Marinho vende um barquinho velho no cais da Praia Azul.
 * Consertado, ele abre a carta náutica para navegar entre a praia, a Ilha das Estrelas, o Alto-mar (pesca de
 * peixes de águas fundas) e a Ilha da Tartaruga, onde a Vovó Tartaruga guarda histórias e fósseis na areia.
 */
final class Boat implements Feature {
    static final String SEA = "Alto-mar", TURTLE = "Ilha da Tartaruga";
    static final int PRICE = 150, STAGE = 15;
    record Port(String map, String name, int x, int y, int chartX, int chartY, String about) {}
    static final Port[] PORTS = {
        new Port("Praia Azul", "Praia Azul", 6, 16, 170, 170, "O cais de casa, ao lado do farol."),
        new Port("Ilha das Estrelas", "Ilha das Estrelas", 1, 10, 380, 150, "A ilha do Templo Estrelado."),
        new Port(SEA, SEA, 9, 7, 470, 300, "Mar aberto: peixes de águas fundas mordem a qualquer hora."),
        new Port(TURTLE, TURTLE, 3, 8, 230, 330, "Uma ilhota de areia branca. Dizem que uma tartaruga muito velha mora lá.")
    };
    static final String[] TURTLE_TALES = {
        "Hmmm... no meu tempo, o farol nem existia. Os barcos se guiavam pelas estrelas.",
        "A areia daqui guarda coisas mais velhas que eu. E olha que eu sou bem velha.",
        "Já vi a baleia Maré pequenininha, do tamanho de um barco. Cresceu bem, aquela.",
        "Quando o mar fica parado e cinzento, os peixes do fundo sobem para espiar.",
        "Dizem que, no Alto-mar, à noite, uma lula gigante acende as águas como lanterna.",
        "Cave onde a areia brilha. O mar devolve o que guardou, um pouquinho por dia."
    };

    final Game game;
    boolean owned;
    int selected, returnState = Game.PLAY;
    String status = "";

    Boat(Game game) { this.game = game; }

    @Override public void reset() { owned = false; selected = 0; status = ""; }

    @Override public void buildWorld() {
        game.maps.get("Praia Azul").addNpc(new Npc("gaivota", "Gaivota", 7, 16, new Color(240, 240, 235)).obj("gaivota").still().stages(STAGE, 99));

        GameMap sea = new GameMap(SEA, 20, 15, '~', true);
        sea.rect(7, 6, 6, 3, 'P');
        sea.addNpc(new Npc("gaivota_mar", "Gaivota", 12, 7, new Color(240, 240, 235)).obj("leme").still());
        game.maps.put(SEA, sea);

        GameMap island = new GameMap(TURTLE, 22, 16, '~', true);
        island.rect(3, 2, 17, 12, 's');
        island.rect(6, 4, 11, 8, '.');
        for (int[] t : new int[][]{{7, 4}, {15, 4}, {16, 10}, {6, 11}, {11, 4}}) island.t[t[1]][t[0]] = '#';
        island.rect(9, 8, 3, 1, 'F');
        island.rect(0, 8, 3, 1, 'P');
        island.addNpc(new Npc("gaivota_ilha", "Gaivota", 0, 9, new Color(240, 240, 235)).obj("gaivota").still());
        island.addNpc(new Npc("tartaruga", "Vovó Tartaruga", 12, 6, new Color(110, 150, 90)).obj("tartaruga").still());
        game.maps.put(TURTLE, island);
    }

    @Override public boolean talk(Npc n) {
        switch (n.id) {
            case "gaivota" -> {
                if (!owned) offer(); else openChart(Game.PLAY);
            }
            case "gaivota_mar", "gaivota_ilha" -> openChart(Game.PLAY);
            case "tartaruga" -> {
                String tale = TURTLE_TALES[Math.floorMod(game.day(), TURTLE_TALES.length)];
                if (game.museum.shovel) game.say("Vovó Tartaruga", tale);
                else game.say("Vovó Tartaruga", "Hmmm... uma visita! Faz tempo. Se gosta de coisas antigas, peça uma pá ao professor do museu.", tale);
            }
            default -> { return false; }
        }
        return true;
    }

    void offer() {
        game.say("Seu Marinho", "Essa é a Gaivota, meu primeiro barco. Está velhinha, mas o casco é bom.",
            "Por " + PRICE + " moedas eu conserto a vela e ela é sua. Com ela dá para ir ao Alto-mar e às ilhas pequenas.");
        game.afterDlg = () -> {
            if (game.home.coins < PRICE) { game.say("Seu Marinho", "Junte " + PRICE + " moedas e volte. A Gaivota espera por você."); return; }
            game.access.ask("Pagar " + PRICE + " moedas pelo conserto da Gaivota?", () -> {
                game.home.coins -= PRICE; owned = true;
                if (!game.save(false)) { game.home.coins += PRICE; owned = false; game.toast("Não foi possível salvar."); return; }
                game.audio.sfx("fanfare");
                game.say("Seu Marinho", "Vela nova, leme firme! A Gaivota é sua. Fale com ela no cais para abrir a carta náutica.");
            });
        };
    }

    void openChart(int from) {
        returnState = from; status = "";
        selected = 0;
        for (int i = 0; i < PORTS.length; i++) if (PORTS[i].map().equals(game.map.name)) selected = (i + 1) % PORTS.length;
        game.down.clear(); game.state = Game.BOAT;
    }

    boolean reachable(Port p) { return !p.map().equals("Ilha das Estrelas") || game.stage >= 11; }

    @Override public boolean owns(int state) { return state == Game.BOAT; }
    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        int old = selected;
        if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_LEFT) selected = (selected + PORTS.length - 1) % PORTS.length;
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_RIGHT) selected = (selected + 1) % PORTS.length;
        else if (act) {
            Port p = PORTS[selected];
            if (p.map().equals(game.map.name)) status = "Você já está aqui.";
            else if (!reachable(p)) status = "A Gaivota ainda não conhece esse caminho.";
            else { game.state = Game.PLAY; game.travel(p.map(), p.x(), p.y()); game.dir = 0; return; }
        }
        if (selected != old) { game.audio.sfx("select"); status = ""; }
    }

    @Override public void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120)); g.fillRect(0, 0, Game.W, Game.H);
        Ui.window(g, 20, 20, Game.W - 40, Game.H - 40);
        g.setFont(new Font("SansSerif", Font.BOLD, 16)); Ui.text(g, Lang.t("Carta náutica da Gaivota"), 38, 50, Ui.GOLD);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11)); Ui.text(g, Lang.t("Setas: escolher · Z: navegar · Esc: ficar"), 38, 68, Ui.SOFT);
        g.setColor(new Color(58, 110, 170)); g.fillRect(36, 80, Game.W - 72, 270);                          // mar
        g.setColor(new Color(90, 145, 200));
        for (int i = 0; i < 40; i++) { int wx = 46 + (i * 97) % (Game.W - 100), wy = 90 + (i * 53) % 250; g.drawArc(wx, wy, 14, 6, 0, 180); }
        g.setColor(new Color(240, 222, 164)); g.fillRect(36, 80, 120, 270);                                 // costa de Aurora
        g.setColor(new Color(104, 176, 80)); g.fillRect(36, 80, 90, 270);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1, new float[]{5, 5}, 0));
        g.setColor(new Color(255, 255, 255, 140));
        Port home = PORTS[0];
        for (int i = 1; i < PORTS.length; i++) g.drawLine(home.chartX(), home.chartY(), PORTS[i].chartX(), PORTS[i].chartY());
        g.setStroke(new BasicStroke(1));
        for (int i = 0; i < PORTS.length; i++) {
            Port p = PORTS[i];
            boolean here = p.map().equals(game.map.name), open = reachable(p);
            int x = p.chartX(), y = p.chartY();
            if (!p.map().equals(SEA)) { g.setColor(new Color(240, 222, 164)); g.fillOval(x - 22, y - 12, 44, 24); g.setColor(new Color(104, 176, 80)); g.fillOval(x - 14, y - 8, 28, 14); }
            else { g.setColor(new Color(36, 80, 140)); g.fillOval(x - 26, y - 14, 52, 28); }
            g.setColor(i == selected ? Ui.GOLD : open ? Color.WHITE : new Color(160, 170, 180));
            g.fillOval(x - 5, y - 5, 10, 10);
            if (here) { g.setColor(new Color(230, 70, 60)); g.fillPolygon(new int[]{x, x + 8, x}, new int[]{y - 24, y - 20, y - 16}, 3); g.setColor(Ui.INK); g.drawLine(x, y - 24, x, y - 6); }
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            String label = Lang.t(open ? p.name() : "???");
            Ui.text(g, label, x - g.getFontMetrics().stringWidth(label) / 2, y + 26, i == selected ? Ui.GOLD : Ui.TEXT);
            if (i == selected) Ui.cursor(g, x - 22, y);
        }
        Port p = PORTS[selected];
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        int y = 372;
        for (String line : game.wrap(reachable(p) ? p.about() : "Um lugar que a Gaivota ainda não conhece.", g.getFontMetrics(), Game.W - 90)) { Ui.text(g, line, 40, y, Ui.TEXT); y += 17; }
        if (!status.isEmpty()) { g.setFont(new Font("SansSerif", Font.BOLD, 12)); Ui.text(g, Lang.t(status), 40, 420, Ui.GOLD); }
    }

    // ---------- Desenho ----------
    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        switch (n.id) {
            case "gaivota", "gaivota_ilha" -> {
                int bob = game.access.reduced ? 0 : (int) (Math.sin(game.frame / 18.0) * 1.5);
                boolean fixed = owned || !n.id.equals("gaivota");
                g.setColor(new Color(110, 72, 44)); g.fillRoundRect(x + 1, y + 18 + bob, 30, 10, 8, 8);           // casco
                g.setColor(new Color(150, 100, 60)); g.fillRect(x + 3, y + 18 + bob, 26, 3);
                g.setColor(new Color(90, 60, 40)); g.fillRect(x + 15, y + 2 + bob, 2, 17);                         // mastro
                g.setColor(fixed ? new Color(250, 250, 244) : new Color(196, 186, 160));
                g.fillPolygon(new int[]{x + 17, x + 17, x + 28}, new int[]{y + 3 + bob, y + 17 + bob, y + 17 + bob}, 3);
                if (!fixed) { g.setColor(new Color(120, 100, 80)); g.drawLine(x + 19, y + 9 + bob, x + 23, y + 14 + bob); }
                g.setColor(new Color(230, 70, 60)); g.fillRect(x + 17, y + 1 + bob, 5, 3);
                return true;
            }
            case "gaivota_mar" -> {                                                                              // leme no convés
                g.setColor(new Color(110, 72, 44)); g.drawOval(x + 8, y + 8, 16, 16); g.drawOval(x + 9, y + 9, 14, 14);
                for (int k = 0; k < 4; k++) { double a = k * Math.PI / 4; g.drawLine(x + 16 - (int) (Math.cos(a) * 11), y + 16 - (int) (Math.sin(a) * 11), x + 16 + (int) (Math.cos(a) * 11), y + 16 + (int) (Math.sin(a) * 11)); }
                g.setColor(new Color(150, 100, 60)); g.fillOval(x + 13, y + 13, 6, 6);
                g.setColor(new Color(90, 60, 40)); g.fillRect(x + 15, y + 22, 2, 8);
                return true;
            }
            case "tartaruga" -> {
                g.setColor(new Color(0, 0, 0, 60)); g.fillOval(x - 2, y + 24, 38, 9);
                g.setColor(new Color(170, 190, 120)); g.fillOval(x + 25, y + 10, 12, 11);                           // cabeça
                g.setColor(Ui.INK); g.fillRect(x + 32, y + 13, 2, 2);
                g.setColor(new Color(240, 240, 230)); g.fillRect(x + 30, y + 19, 4, 4);                              // barbicha
                g.setColor(new Color(150, 170, 100)); g.fillOval(x, y + 22, 9, 7); g.fillOval(x + 20, y + 23, 9, 7);
                g.setColor(Ui.INK); g.fillOval(x - 2, y + 4, 32, 24);                                               // casco
                g.setColor(new Color(160, 112, 62)); g.fillOval(x, y + 6, 28, 20);
                g.setColor(new Color(200, 150, 86));
                g.fillPolygon(new int[]{x + 10, x + 18, x + 21, x + 14, x + 7}, new int[]{y + 9, y + 9, y + 15, y + 20, y + 15}, 5);
                g.setColor(new Color(110, 74, 40)); g.drawLine(x + 7, y + 15, x + 1, y + 14); g.drawLine(x + 21, y + 15, x + 27, y + 14);
                return true;
            }
            default -> { return false; }
        }
    }

    /** No Alto-mar: ondas passando e a Gaivota inteira (casco em volta do convés), abaixo dos personagens. */
    @Override public void drawGround(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals(SEA)) return;
        int T = Game.T;
        if (!game.access.reduced) {
            g.setColor(new Color(255, 255, 255, 90));
            for (int i = 0; i < 16; i++) {
                int x = (int) ((i * 113 + game.frame * 0.6) % (game.map.w * T)) - camX, y = (i * 71) % (game.map.h * T) - camY;
                g.drawArc(x, y, 18, 6, 0, 180);
            }
        }
        int x0 = 7 * T - camX, y0 = 6 * T - camY, w = 6 * T, h = 3 * T;
        g.setColor(new Color(0, 0, 0, 50)); g.fillOval(x0 - 14, y0 + h - 6, w + 50, 18);                      // sombra na água
        int[] hx = {x0 - 10, x0 + w + 6, x0 + w + 40, x0 + w + 6, x0 - 10}, hy = {y0 - 8, y0 - 8, y0 + h / 2, y0 + h + 8, y0 + h + 8};
        g.setColor(new Color(92, 58, 34)); g.fillPolygon(hx, hy, 5);                                          // casco
        g.setColor(new Color(130, 84, 48)); g.setStroke(new BasicStroke(4)); g.drawPolygon(hx, hy, 5); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(240, 236, 224)); g.fillRect(x0 - 10, y0 + h + 4, w + 16, 4);                    // faixa branca
        g.setColor(new Color(176, 128, 80)); g.fillRect(x0, y0, w, h);                                         // convés
        g.setColor(new Color(140, 98, 60));
        for (int k = 1; k < 6; k++) g.fillRect(x0, y0 + k * h / 6, w, 1);
        for (int k = 0; k < 6; k++) g.fillRect(x0 + ((k * 37) % w), y0 + (k % 6) * h / 6, 1, h / 6);
    }

    /** Mastro e vela por cima de tudo (o jogador passa por baixo). */
    @Override public void drawSky(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals(SEA)) return;
        int T = Game.T, mx = 8 * T - camX + 4, my = 6 * T - camY;
        g.setColor(new Color(80, 52, 32)); g.fillRect(mx, my - 70, 5, 82);
        int sway = game.access.reduced ? 0 : (int) (Math.sin(game.frame / 40.0) * 3);
        g.setColor(new Color(250, 248, 238, 235));
        g.fillPolygon(new int[]{mx + 6, mx + 6, mx + 58 + sway}, new int[]{my - 66, my + 2, my + 2}, 3);
        g.setColor(new Color(230, 70, 60)); g.fillRect(mx + 5, my - 76, 14, 7);
    }

    @Override public void save(Properties p) { p.setProperty("barco.gaivota", "" + owned); }
    @Override public void load(Properties p) { owned = Boolean.parseBoolean(p.getProperty("barco.gaivota", "false")); }
    @Override public String signature() { return "b" + owned; }
}
