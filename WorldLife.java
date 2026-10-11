import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Vida no mundo, só visual: gaivotas na praia, vagalumes nas noites quentes, borboletas de dia, pétalas na
 * primavera, folhas no outono, peixes pulando nas águas e conversas curtas entre moradores próximos.
 * Tudo é calculado a partir do relógio de quadros (sem estado para salvar). Com "reduzir movimento",
 * os efeitos que se movem não aparecem.
 */
final class WorldLife implements Feature {
    record Chat(String a, String b, String first, String reply) {}
    /** Conversas: ids dos dois moradores (null = qualquer um), fala e resposta. */
    static final List<Chat> CHATS = List.of(
        new Chat("beto", "lucas", "Lucas, quer aprender a pescar?", "Só se o peixe gostar de música!"),
        new Chat("rosa", "prof", "Professor, que flor é essa?", "Uma margarida! Vou anotar no caderno."),
        new Chat("prof", "lucas", "Quantas estrelas você contou ontem?", "Perdi a conta no cem!"),
        new Chat("rosa", "beto", "Beto, guardei uma flor para a sua banca.", "Vai ficar bonita ao lado dos peixes!"),
        new Chat("lucas", "rosa", "Dona Rosa, os sinos combinam com flores?", "Tudo combina com flores, Lucas."),
        new Chat("prof", "beto", "Os peixes mudam com as estações, sabia?", "Eu sabia! Só não sabia explicar."),
        new Chat(null, null, "Viu o farol ontem à noite?", "Brilhava como nunca!"),
        new Chat(null, null, "Você pescou alguma coisa hoje?", "Só um sapato... de novo!"),
        new Chat(null, null, "Que dia bonito em Aurora!", "Bom para um passeio na praça."),
        new Chat(null, null, "Já foi ao festival de sábado?", "Não perco um!")
    );
    static final Set<String> GARDENS = Set.of("Vila Aurora", "Floresta Verde", "Quintal de Casa", "Jardim das Constelações", "Vale dos Ecos");
    static final Set<String> SEA = Set.of("Praia Azul", "Ilha das Estrelas", "Recife de Coral");

    final Game game;
    final Random random = new Random();
    final Map<String, int[][]> water = new HashMap<>();
    Npc chatNpc, chatPartner;
    String chatText, chatReply;
    double chatTimer, chatCooldown = 5;

    WorldLife(Game game) { this.game = game; }

    boolean moving() { return !game.access.reduced; }
    boolean outside() { return game.map.outdoor && !game.map.dark; }
    int season() { return game.seasons.current(); }
    static int hash(int a, int b) { int h = a * 73856093 ^ b * 19349663; h ^= h >>> 13; return h * 0x5bd1e995 >>> 1; }

    // ---------- Conversas entre moradores ----------
    @Override public void reset() { chatNpc = null; chatTimer = 0; chatCooldown = 5; }
    @Override public void update(double dt) {
        if (chatTimer > 0) {
            chatTimer -= dt;
            if (chatTimer <= 0 && chatReply != null && chatPartner != null) {   // o outro responde
                Npc speaker = chatPartner; chatPartner = chatNpc; chatNpc = speaker;
                chatText = chatReply; chatReply = null; chatTimer = 3;
            } else if (chatTimer <= 0) { chatNpc = null; chatCooldown = 8 + random.nextDouble() * 10; }
            return;
        }
        chatCooldown -= dt;
        if (chatCooldown > 0 || game.dlg != null) return;
        chatCooldown = 3;
        List<Npc> people = new ArrayList<>();
        for (VillageLife.Resident r : VillageLife.RESIDENTS) {
            Npc n = game.life.resident(r.id());
            if (game.map.npcs.contains(n) && game.vis(n) && !n.walking && Math.hypot(n.x - game.tx, n.y - game.ty) < 9) people.add(n);
        }
        Collections.shuffle(people, random);
        for (Npc a : people) for (Npc b : people) {
            if (a == b || Math.abs(a.x - b.x) + Math.abs(a.y - b.y) > 4) continue;
            List<Chat> options = new ArrayList<>();
            for (Chat c : CHATS) if (c.a() == null || c.a().equals(a.id) && c.b().equals(b.id)) options.add(c);
            Chat c = options.get(random.nextInt(options.size()));
            chatNpc = a; chatPartner = b; chatText = c.first(); chatReply = c.reply(); chatTimer = 3;
            return;
        }
    }

    // ---------- Abaixo da iluminação: peixes, borboletas, pétalas e folhas ----------
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        if (!outside() || game.map.snowy || !moving()) return;
        long f = game.frame;
        drawFish(g, camX, camY, f);
        int s = season();
        boolean day = !game.isNight();
        if (day && (s == Seasons.SPRING || s == Seasons.SUMMER) && GARDENS.contains(game.map.name) && !game.seasons.snowCover(game.map)) drawButterflies(g, camX, camY, f);
        if (s == Seasons.SPRING || s == Seasons.AUTUMN) drawFalling(g, f, s == Seasons.SPRING);
    }

    int[][] waterTiles() {
        return water.computeIfAbsent(game.map.name, k -> {
            List<int[]> list = new ArrayList<>();
            GameMap m = game.map;
            for (int y = 1; y < m.h - 1; y++) for (int x = 1; x < m.w - 1; x++)
                if (m.t[y][x] == '~' && m.at(x - 1, y) == '~' && m.at(x + 1, y) == '~' && m.at(x, y - 1) == '~') list.add(new int[]{x, y});
            return list.toArray(new int[0][]);
        });
    }

    void drawFish(Graphics2D g, int camX, int camY, long f) {
        int[][] tiles = waterTiles();
        if (tiles.length == 0) return;
        int cycle = (int) (f / 150), phase = (int) (f % 150);
        int[] t = tiles[hash(cycle, game.map.name.hashCode()) % tiles.length];
        int x = t[0] * Game.T - camX + 16, y = t[1] * Game.T - camY + 18;
        if (x < -20 || y < -20 || x > Game.W + 20 || y > Game.H + 20) return;
        if (phase < 36) {                                                      // salto em arco
            double p = phase / 36.0;
            int fx = x - 10 + (int) (p * 20), fy = y - (int) (Math.sin(Math.PI * p) * 18);
            g.setColor(new Color(220, 235, 245)); g.fillOval(fx - 5, fy - 3, 11, 6);
            g.fillPolygon(new int[]{fx - 5, fx - 10, fx - 10}, new int[]{fy, fy - 4, fy + 4}, 3);
        }
        int ring = phase < 36 ? phase : phase - 36;
        if (ring < 50) {                                                       // ondinhas
            int a = Math.max(0, 160 - ring * 3), r = 3 + ring / 3;
            g.setColor(new Color(235, 245, 255, a)); g.drawOval(x - r, y - r / 2, r * 2, r);
        }
    }

    void drawButterflies(Graphics2D g, int camX, int camY, long f) {
        Color[] colors = {new Color(250, 200, 80), new Color(235, 130, 180), new Color(150, 200, 250), new Color(255, 255, 255)};
        GameMap m = game.map;
        for (int i = 0; i < 6; i++) {
            int bx = 2 + hash(i, m.name.hashCode()) % Math.max(1, m.w - 4), by = 2 + hash(m.name.hashCode(), i * 7) % Math.max(1, m.h - 4);
            double t = f / 60.0 + i * 1.7;
            int x = bx * Game.T - camX + (int) (Math.sin(t * 0.9) * 40), y = by * Game.T - camY + (int) (Math.sin(t * 1.3) * 22);
            if (x < -10 || y < -10 || x > Game.W + 10 || y > Game.H + 10) continue;
            int wing = 2 + (int) (Math.abs(Math.sin(f / 4.0 + i)) * 5);
            g.setColor(colors[i % colors.length]);
            g.fillOval(x - wing - 1, y - 4, wing + 1, 7); g.fillOval(x + 1, y - 4, wing + 1, 7);
            g.setColor(new Color(60, 50, 40)); g.fillRect(x, y - 4, 1, 7);
        }
    }

    void drawFalling(Graphics2D g, long f, boolean petals) {
        for (int i = 0; i < 14; i++) {
            double speed = 0.5 + (i % 4) * 0.15;
            int x = (int) Math.floorMod((long) (i * 97 + f * speed * 0.7), Game.W + 40) - 20;
            int y = (int) Math.floorMod((long) (i * 53 + f * speed), Game.H + 40) - 20;
            x += (int) (Math.sin(f / 30.0 + i) * 8);
            if (petals) { g.setColor(i % 2 == 0 ? new Color(250, 205, 225, 220) : new Color(255, 245, 250, 220)); g.fillOval(x, y, 4, 3); }
            else { g.setColor(i % 3 == 0 ? new Color(205, 110, 45, 230) : i % 3 == 1 ? new Color(220, 160, 50, 230) : new Color(175, 70, 45, 230)); g.fillOval(x, y, 6, 3); }
        }
    }

    // ---------- Sobre a iluminação: vagalumes, gaivotas e balões de conversa ----------
    @Override public void drawSky(Graphics2D g, int camX, int camY) {
        long f = game.frame;
        if (outside() && moving() && !game.map.snowy) {
            int s = season();
            if (game.night() > 0.5 && (s == Seasons.SPRING || s == Seasons.SUMMER) && GARDENS.contains(game.map.name)) drawFireflies(g, camX, camY, f);
            if (!game.isNight() && SEA.contains(game.map.name)) drawGulls(g, f);
        }
        drawChat(g, camX, camY);
    }

    void drawFireflies(Graphics2D g, int camX, int camY, long f) {
        GameMap m = game.map;
        for (int i = 0; i < 18; i++) {
            int bx = 1 + hash(i * 3, m.name.hashCode() + 11) % Math.max(1, m.w - 2), by = 1 + hash(m.name.hashCode() + 5, i * 13) % Math.max(1, m.h - 2);
            double t = f / 90.0 + i;
            int x = bx * Game.T - camX + (int) (Math.sin(t) * 20), y = by * Game.T - camY + (int) (Math.cos(t * 0.7) * 14);
            if (x < -12 || y < -12 || x > Game.W + 12 || y > Game.H + 12) continue;
            float pulse = (float) (0.4 + 0.6 * Math.max(0, Math.sin(f / 20.0 + i * 2.1)));
            game.glow(g, x, y, 12, new Color(240, 255, 140, 160), new Color(240, 255, 140, 0), pulse);
            g.setColor(new Color(250, 255, 190, (int) (255 * pulse))); g.fillRect(x - 1, y - 1, 2, 2);
        }
    }

    void drawGulls(Graphics2D g, long f) {
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < 3; i++) {
            int x = (int) Math.floorMod((long) (f * (1.1 + i * 0.3) + i * 260), Game.W + 160) - 80;
            int y = 70 + i * 46 + (int) (Math.sin(f / 50.0 + i) * 10);
            int flap = (int) (Math.sin(f / 7.0 + i) * 4);
            g.setColor(new Color(0, 0, 0, 35)); g.drawLine(x - 8, y + 60, x, y + 64 - flap / 2); g.drawLine(x, y + 64 - flap / 2, x + 8, y + 60);
            g.setColor(new Color(250, 250, 250)); g.drawLine(x - 9, y - flap, x, y + 3); g.drawLine(x, y + 3, x + 9, y - flap);
        }
        g.setStroke(new BasicStroke(1));
    }

    void drawChat(Graphics2D g, int camX, int camY) {
        if (chatNpc == null || chatTimer <= 0 || game.state != Game.PLAY || game.dlg != null || !game.map.npcs.contains(chatNpc) || !game.vis(chatNpc)) return;
        Font font = new Font("SansSerif", Font.PLAIN, 11);
        FontMetrics fm = g.getFontMetrics(font);
        List<String> lines = game.wrap(chatText, fm, 150);
        int w = 0; for (String l : lines) w = Math.max(w, fm.stringWidth(l));
        int h = lines.size() * 13 + 8;
        int cx = chatNpc.x * Game.T - camX + 16, top = chatNpc.y * Game.T - camY - 14 - h;
        int left = Math.max(4, Math.min(Game.W - w - 16, cx - w / 2 - 6));
        g.setColor(new Color(255, 255, 255, 235)); g.fillRoundRect(left, top, w + 12, h, 10, 10);
        g.fillPolygon(new int[]{cx - 5, cx + 5, cx}, new int[]{top + h - 1, top + h - 1, top + h + 6}, 3);
        g.setColor(new Color(60, 60, 80)); g.drawRoundRect(left, top, w + 12, h, 10, 10);
        g.setFont(font);
        for (int i = 0; i < lines.size(); i++) g.drawString(lines.get(i), left + 6, top + 15 + i * 13);
    }
}
