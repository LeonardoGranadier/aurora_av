import java.awt.*;
import java.util.*;

/**
 * Bolinha, o gato da vila: passeia de dia pelo campo a sudoeste da praça, longe das rotas dos moradores,
 * e dorme à noite. Um carinho por dia; sete dias de carinho rendem uma conquista.
 */
final class VillageCat implements Feature {
    static final Rectangle AREA = new Rectangle(2, 15, 11, 5);   // x 2..12, y 15..19
    static final int START_X = 3, START_Y = 16;

    final Game game;
    final Random random = new Random();
    Npc cat;
    int petDays, pettedDay = -1;
    double wait = 2;

    VillageCat(Game game) { this.game = game; }

    boolean awake() { return game.hour() >= 6 && game.hour() < 21; }

    @Override public void reset() { petDays = 0; pettedDay = -1; }
    @Override public void buildWorld() {
        cat = new Npc("gato", "Bolinha", START_X, START_Y, new Color(235, 160, 80)).obj("gato");
        game.maps.get("Vila Aurora").addNpc(cat);
    }
    @Override public boolean visible(Npc n) { return n != cat || awake(); }

    boolean open(int x, int y) {
        GameMap m = game.maps.get("Vila Aurora");
        if (!AREA.contains(x, y) || "#~HDXLR".indexOf(m.at(x, y)) >= 0) return false;
        if (game.map == m && (game.tx == x && game.ty == y || game.moving && game.nx == x && game.ny == y)) return false;
        for (Npc n : m.npcs) if (n != cat && game.vis(n) && (n.x == x && n.y == y || n.walking && n.toX == x && n.toY == y)) return false;
        return true;
    }

    @Override public void update(double dt) {
        if (cat == null || !awake() || game.dlg != null) return;
        if (cat.walking) {
            cat.walkProgress += dt * 2.5;
            if (cat.walkProgress >= 1) { cat.x = cat.toX; cat.y = cat.toY; cat.walking = false; cat.walkProgress = 0; }
            return;
        }
        wait -= dt;
        if (wait > 0) return;
        wait = 1.5 + random.nextDouble() * 3;
        int d = random.nextInt(4), x = cat.x + Game.DX[d], y = cat.y + Game.DY[d];
        if (open(x, y)) { cat.dir = d; cat.toX = x; cat.toY = y; cat.walking = true; cat.walkProgress = 0; }
    }

    @Override public boolean talk(Npc n) {
        if (n != cat) return false;
        if (pettedDay == game.day()) { game.say("Bolinha", "Bolinha se espreguiça e pisca devagar para você. (Já ganhou carinho hoje.)"); return true; }
        pettedDay = game.day(); petDays++;
        game.audio.sfx("twinkle");
        game.say("Bolinha", "Rrrrr... Bolinha ronrona e se enrosca nos seus pés.",
            petDays == 7 ? "Bolinha agora te segue com os olhos por toda a praça. Vocês são amigos de verdade!" : "Carinhos em Bolinha: " + petDays + ".");
        return true;
    }

    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n != cat) return false;
        int bob = game.access.reduced ? 0 : (int) (Math.sin(game.frame / 10.0) * (n.walking ? 1 : 0));
        int cx = x + 16, cy = y + 24 + bob;
        boolean left = n.dir == 2;
        g.setColor(new Color(0, 0, 0, 50)); g.fillOval(cx - 10, cy + 6, 20, 5);
        g.setColor(new Color(235, 160, 80)); g.fillOval(cx - 9, cy - 4, 18, 11);                // corpo
        int hx = left ? cx - 11 : cx + 3;
        g.fillOval(hx, cy - 10, 10, 10);                                                       // cabeça
        g.fillPolygon(new int[]{hx + 1, hx + 3, hx + 5}, new int[]{cy - 8, cy - 14, cy - 8}, 3);
        g.fillPolygon(new int[]{hx + 5, hx + 7, hx + 9}, new int[]{cy - 8, cy - 14, cy - 8}, 3);
        int tail = left ? cx + 8 : cx - 12;
        g.setStroke(new BasicStroke(3)); g.drawArc(tail, cy - 10, 6, 12, left ? -90 : 90, 180); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(200, 120, 60)); g.fillRect(cx - 4, cy - 3, 2, 8); g.fillRect(cx + 1, cy - 3, 2, 8);
        g.setColor(new Color(40, 40, 40)); g.fillRect(hx + 2, cy - 7, 2, 2); g.fillRect(hx + 6, cy - 7, 2, 2);
        return true;
    }

    @Override public void save(Properties p) { p.setProperty("gato.carinhos", "" + petDays); p.setProperty("gato.dia", "" + pettedDay); }
    @Override public void load(Properties p) {
        petDays = game.life.number(p, "gato.carinhos", 0, 999999);
        try { pettedDay = Integer.parseInt(p.getProperty("gato.dia", "-1")); } catch (NumberFormatException e) { pettedDay = -1; }
    }
    @Override public String signature() { return "g" + petDays + "@" + pettedDay; }
}
