import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Regional overview. Discovery is recorded on arrival, not when rendering. */
final class WorldMap {
    /** labelRight puts the name beside the node, keeping its vertical edges and diagonals visible. */
    record Region(String name, int x, int y, boolean labelRight) {
        Region(String name, int x, int y) { this(name, x, y, false); }
    }
    static final Region[] REGIONS = {
        new Region("Casa", 75, 205), new Region("Vila Aurora", 225, 205, true),
        new Region("Floresta Verde", 390, 205), new Region("Praia Azul", 225, 290),
        new Region("Gruta do Eco", 390, 120), new Region("Ilha das Estrelas", 390, 290),
        new Region("Templo Estrelado", 550, 290), new Region("Trilha da Montanha", 225, 120),
        new Region("Cabana do Eremita", 75, 120), new Region("Pico Estrelado", 225, 55),
        new Region(ChapterFive.TRAIL, 550, 205), new Region(ChapterFive.GARDEN, 550, 120),
        new Region(Gardening.MAP, 75, 290), new Region(ChapterSix.REEF, 390, 365),
        new Region(ChapterEight.VALLEY, 390, 55)
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
            case 28, 31, 32 -> Set.of("Praia Azul");
            case 29 -> {
                Set<String> remaining = new HashSet<>();
                for (int i = 0; i < 3; i++) if ((game.chapterSix.heard & (1 << i)) == 0) remaining.add(ChapterSix.SHELL_MAPS[i]);
                yield remaining;
            }
            case 30 -> Set.of(ChapterSix.REEF);
            case 33 -> {
                Set<String> remaining = new HashSet<>();
                for (int i = 0; i < ChapterSeven.GUESTS.length; i++)
                    if ((game.chapterSeven.invited & (1 << i)) == 0) remaining.add(ChapterSeven.GUEST_MAPS[i]);
                yield remaining;
            }
            case 34, 35 -> Set.of("Pico Estrelado");
            case 36, 37, 41 -> Set.of("Vila Aurora");
            case 38 -> Set.of("Gruta do Eco");
            case 39, 40 -> Set.of(ChapterEight.VALLEY);
            default -> Set.of();
        };
    }
    boolean connected(String a, String b) {
        return game.maps.get(a).warps.stream().anyMatch(w -> w.to().equals(b))
            || game.maps.get(b).warps.stream().anyMatch(w -> w.to().equals(a))
            || a.equals("Praia Azul") && b.equals("Ilha das Estrelas")
            || b.equals("Praia Azul") && a.equals("Ilha das Estrelas")
            || a.equals("Ilha das Estrelas") && b.equals(ChapterSix.REEF)                      // bote da ilha
            || b.equals("Ilha das Estrelas") && a.equals(ChapterSix.REEF);
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
            if (goals.contains(r.name)) {
                g.setColor(new Color(192, 117, 38)); g.drawOval(r.x - 15, r.y - 15, 30, 30);
            }
            g.setColor(here ? new Color(41, 121, 140) : known ? new Color(100, 145, 109) : new Color(177, 188, 185));
            g.fillOval(r.x - 9, r.y - 9, 18, 18);
            g.setColor(Color.WHITE); g.setFont(new Font("Dialog", Font.BOLD, 12));
            if (!known) g.drawString("?", r.x - 4, r.y + 4);
            if (here) { g.fillOval(r.x - 3, r.y - 3, 6, 6); }
            g.setFont(new Font("Dialog", Font.PLAIN, 12)); FontMetrics fm = g.getFontMetrics();
            String label = known ? r.name : "Desconhecido";
            java.util.List<String> lines = game.wrap(label, fm, r.labelRight ? 90 : 133);
            int width = 0; for (String line : lines) width = Math.max(width, fm.stringWidth(line));
            int left = r.labelRight ? r.x + 18 : r.x - width / 2, top = r.labelRight ? r.y + 8 : r.y + 15;
            g.setColor(new Color(235, 244, 239)); g.fillRect(left - 3, top, width + 6, lines.size() * 14 + 7);
            g.setColor(new Color(36, 66, 65));
            int y = top + 14;
            for (String line : lines) {
                g.drawString(line, r.labelRight ? left : r.x - fm.stringWidth(line) / 2, y); y += 14;
            }
            int shells = game.shells.remaining(r.name);
            if (known && game.shells.listening() && shells > 0) {                // Concha que Escuta: conchas restantes
                g.setColor(new Color(240, 150, 175)); g.fillOval(r.x + 8, r.y - 20, 14, 14);
                g.setColor(Color.WHITE); g.setFont(new Font("Dialog", Font.BOLD, 10));
                g.drawString("" + shells, r.x + 12, r.y - 9);
            }
        }
        g.setColor(new Color(217, 233, 225)); g.fillRect(0, 418, Game.W, 62);
        String shellNote = game.shells.listening() ? "   ·   Conchas secretas aqui: " + game.shells.remaining(game.map.name) + " de " + game.shells.total(game.map.name) : "";
        game.life.text(g, "Você: " + game.map.name + shellNote, 20, 438, 600, 14);
        game.life.text(g, Story.OBJECTIVES[game.stage], 20, 460, 600, 13);
        g.setColor(new Color(192, 117, 38)); g.drawOval(364, 17, 12, 12);
        g.setColor(new Color(36, 66, 65)); g.setFont(new Font("Dialog", Font.PLAIN, 12));
        g.drawString("Destino da história", 383, 28);
    }
}
