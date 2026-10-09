import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Regional overview. Discovery is recorded on arrival, not when rendering. */
final class WorldMap {
    record Region(String name, int x, int y) {}
    static final Region[] REGIONS = {
        new Region("Casa", 75, 240), new Region("Vila Aurora", 225, 240),
        new Region("Floresta Verde", 390, 240), new Region("Praia Azul", 225, 345),
        new Region("Gruta do Eco", 390, 135), new Region("Ilha das Estrelas", 390, 345),
        new Region("Templo Estrelado", 550, 345), new Region("Trilha da Montanha", 225, 135),
        new Region("Cabana do Eremita", 75, 135), new Region("Pico Estrelado", 225, 65),
        new Region(ChapterFive.TRAIL, 550, 240), new Region(ChapterFive.GARDEN, 550, 135),
        new Region(Gardening.MAP, 75, 345)
    };
    final Game game;
    final Set<String> visited = new TreeSet<>();
    int returnState;
    WorldMap(Game game) { this.game = game; }
    void reset() { visited.clear(); }
    void arrive() { if (game.map != null) visited.add(game.map.name); }
    void open(int from) { returnState = from; game.down.clear(); game.state = Game.WORLD_MAP; }
    void key(int key) {
        if (key == KeyEvent.VK_TAB || key == KeyEvent.VK_ESCAPE) {
            game.state = returnState; game.down.clear();
        }
    }
    void save(Properties p) { p.setProperty("mapa.visitados", String.join("|", visited)); }
    void load(Properties p) {
        visited.clear();
        for (String name : p.getProperty("mapa.visitados", "").split("\\|"))
            if (game.maps.containsKey(name)) visited.add(name);
        arrive();
    }
    Set<String> destinations() {
        return switch (game.stage) {
            case 0, 27 -> Set.of("Casa");
            case 1, 3, 4, 8, 10, 14, 20, 21 -> Set.of("Praia Azul");
            case 2 -> Set.of("Floresta Verde");
            case 5, 7 -> Set.of("Gruta do Eco");
            case 6 -> {
                Set<String> remaining = new HashSet<>();
                if ((game.frags & 1) == 0) remaining.add("Vila Aurora");
                if ((game.frags & 2) == 0) remaining.add("Praia Azul");
                if ((game.frags & 4) == 0) remaining.add("Gruta do Eco");
                yield remaining;
            }
            case 9 -> {
                Set<String> remaining = new HashSet<>();
                if ((game.bye & 1) == 0) remaining.add("Casa");
                if ((game.bye & 2) == 0) remaining.add("Praia Azul");
                yield remaining;
            }
            case 11, 12, 13 -> Set.of("Templo Estrelado");
            case 15, 22 -> Set.of("Vila Aurora");
            case 16, 17 -> Set.of("Cabana do Eremita");
            case 18 -> Set.of("Trilha da Montanha");
            case 19 -> Set.of("Pico Estrelado");
            case 23, 24 -> Set.of(ChapterFive.TRAIL);
            case 25, 26 -> Set.of(ChapterFive.GARDEN);
            default -> Set.of();
        };
    }
    boolean connected(String a, String b) {
        return game.maps.get(a).warps.stream().anyMatch(w -> w.to().equals(b))
            || game.maps.get(b).warps.stream().anyMatch(w -> w.to().equals(a))
            || a.equals("Praia Azul") && b.equals("Ilha das Estrelas")
            || b.equals("Praia Azul") && a.equals("Ilha das Estrelas");
    }
    void draw(Graphics2D g) {
        g.setColor(new Color(235, 244, 239)); g.fillRect(0, 0, Game.W, Game.H);
        g.setFont(new Font("Dialog", Font.BOLD, 21)); g.setColor(new Color(36, 66, 65));
        g.drawString("Mapa de Aurora", 20, 30);
        g.setFont(new Font("Dialog", Font.PLAIN, 12));
        g.drawString(visited.size() + "/" + REGIONS.length + " regiões", 525, 28);
        for (int i = 0; i < REGIONS.length; i++) for (int j = i + 1; j < REGIONS.length; j++) {
            Region a = REGIONS[i], b = REGIONS[j];
            if (!connected(a.name, b.name)) continue;
            boolean known = visited.contains(a.name) && visited.contains(b.name);
            g.setColor(known ? new Color(107, 144, 137) : new Color(187, 201, 197));
            g.setStroke(known ? new BasicStroke(3) : new BasicStroke(2, 0, 0, 10, new float[]{4, 5}, 0));
            g.drawLine(a.x, a.y, b.x, b.y);
        }
        g.setStroke(new BasicStroke(2)); Set<String> goals = destinations();
        for (Region r : REGIONS) {
            boolean known = visited.contains(r.name), here = game.map.name.equals(r.name);
            g.setColor(new Color(235, 244, 239)); g.fillRect(r.x - 69, r.y + 15, 138, 35);
            if (goals.contains(r.name)) {
                g.setColor(new Color(192, 117, 38)); g.drawOval(r.x - 15, r.y - 15, 30, 30);
            }
            g.setColor(here ? new Color(41, 121, 140) : known ? new Color(100, 145, 109) : new Color(177, 188, 185));
            g.fillOval(r.x - 9, r.y - 9, 18, 18);
            g.setColor(Color.WHITE); g.setFont(new Font("Dialog", Font.BOLD, 12));
            if (!known) g.drawString("?", r.x - 4, r.y + 4);
            if (here) { g.fillOval(r.x - 3, r.y - 3, 6, 6); }
            g.setColor(new Color(36, 66, 65)); g.setFont(new Font("Dialog", Font.PLAIN, 12));
            String label = known ? r.name : "Desconhecido";
            java.util.List<String> lines = game.wrap(label, g.getFontMetrics(), 133);
            int y = r.y + 29;
            for (String line : lines) { g.drawString(line, r.x - g.getFontMetrics().stringWidth(line) / 2, y); y += 14; }
        }
        g.setColor(new Color(217, 233, 225)); g.fillRect(0, 398, Game.W, 82);
        game.life.text(g, "Você: " + game.map.name, 20, 418, 600, 14);
        game.life.text(g, Game.OBJETIVOS[game.stage], 20, 442, 600, 13);
        g.setColor(new Color(192, 117, 38)); g.drawOval(364, 17, 12, 12);
        g.setColor(new Color(36, 66, 65)); g.setFont(new Font("Dialog", Font.PLAIN, 12));
        g.drawString("Destino da história", 383, 28);
    }
}
