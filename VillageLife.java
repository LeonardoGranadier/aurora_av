import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/** Rotinas da vila e atividades opcionais, independentes das fases da campanha. */
final class VillageLife {
    static final int BELL_ROUNDS = 5;
    static final int WAIT = 0, BITE = 1, REEL = 2, RESULT = 3;
    static final Color INK = new Color(27, 45, 49), PAPER = new Color(239, 247, 244);
    static final Color[] BELL_COLORS = {new Color(226, 99, 93), new Color(75, 153, 215),
        new Color(235, 187, 65), new Color(75, 179, 123)};
    static final String[] BELL_NAMES = {"DÓ", "RÉ", "MI", "SOL"};
    static final String[] BELL_KEYS = {"1 / CIMA", "2 / DIREITA", "3 / BAIXO", "4 / ESQUERDA"};

    record Fish(String name, String map, int period, int stage, Color color, double difficulty) {}
    static final Fish[] FISH = {
        new Fish("Lambari", "Vila Aurora", -1, 0, new Color(160, 186, 198), 0.7),
        new Fish("Carpa Dourada", "Vila Aurora", 0, 4, new Color(236, 174, 58), 1.0),
        new Fish("Bagre da Lua", "Vila Aurora", 1, 4, new Color(126, 112, 175), 1.1),
        new Fish("Truta Verde", "Floresta Verde", -1, 0, new Color(75, 176, 126), 0.9),
        new Fish("Sardinha Azul", "Praia Azul", -1, 0, new Color(85, 163, 221), 0.7),
        new Fish("Peixe-Lanterna", "Praia Azul", 1, 5, new Color(233, 138, 82), 1.2),
        new Fish("Peixe-Coral", "Ilha das Estrelas", 0, 11, new Color(233, 104, 138), 1.2),
        new Fish("Peixe Estelar", "Ilha das Estrelas", 1, 14, new Color(136, 198, 237), 1.4)
    };

    record Resident(String id, int[][] stops, boolean goesHome) {}
    static final Resident[] RESIDENTS = {
        new Resident("prof", new int[][]{{10, 8}, {12, 12}, {18, 8}, {18, 8}}, false),
        new Resident("beto", new int[][]{{19, 13}, {18, 12}, {13, 12}, {18, 6}}, true),
        new Resident("rosa", new int[][]{{9, 12}, {11, 8}, {16, 12}, {18, 6}}, true),
        new Resident("lucas", new int[][]{{17, 10}, {9, 9}, {12, 9}, {18, 6}}, true)
    };

    final Game game;
    final Random random = new Random();
    boolean rod;
    final int[] catches = new int[FISH.length];
    int bellBest, albumSelection, albumReturn;
    int fishPhase, fishIndex;
    double fishTimer, fishElapsed, fishPosition, fishVelocity, fishTarget, catchProgress;
    String result = "";
    final List<Integer> sequence = new ArrayList<>();
    int playback, answer, lit = -1;
    boolean showing, bellFinished;
    boolean festivalBells;
    double bellTimer, flashTimer;

    VillageLife(Game game) { this.game = game; }

    void reset() {
        rod = false; Arrays.fill(catches, 0); bellBest = 0;
        sequence.clear(); result = ""; fishPhase = WAIT;
        lit = -1; showing = false; bellFinished = false;
        festivalBells = false;
    }

    void buildVillage(Game.GameMap village) {
        village.rect(8, 7, 11, 7, '.');
        village.rect(8, 10, 11, 2, '=');
        village.rect(18, 7, 2, 7, '=');
        village.t[6][18] = 'D';
        village.addNpc(new Game.Npc("beto", "Tio Beto", 19, 13, new Color(62, 149, 128)));
        village.addNpc(new Game.Npc("rosa", "Dona Rosa", 9, 12, new Color(211, 106, 148)));
        village.addNpc(new Game.Npc("lucas", "Lucas", 17, 10, new Color(239, 178, 60)));
        village.addNpc(new Game.Npc("sino", "Sinos da Praça", 11, 12, new Color(236, 191, 68)).obj("sino").still());
    }

    Game.Npc resident(String id) {
        return game.maps.get("Vila Aurora").npcs.stream().filter(n -> n.id.equals(id)).findFirst().orElseThrow();
    }

    int routineSlot() {
        double hour = game.hour();
        return hour < 6 || hour >= 21 ? 3 : hour < 12 ? 0 : hour < 18 ? 1 : 2;
    }

    void updateResidents(double dt) {
        Game.GameMap village = game.maps.get("Vila Aurora");
        int slot = routineSlot();
        for (Resident r : RESIDENTS) {
            Game.Npc n = resident(r.id());
            if (n.sleeping) {
                if (slot == 3) continue;
                if (!walkable(village, n, n.x, n.y)) continue;
                n.sleeping = false;
            }
            if (n.walking) {
                n.walkProgress += dt * 2.0;
                if (n.walkProgress < 1) continue;
                n.x = n.toX; n.y = n.toY; n.walking = false; n.walkProgress = 0;
            }
            int[] target = game.weather.destination(r, slot);
            if (n.x == target[0] && n.y == target[1]) {
                if (r.goesHome() && slot == 3) n.sleeping = true;
                continue;
            }
            n.routeWait -= dt;
            if (n.routeWait > 0) continue;
            n.routeWait = 0.25;
            int step = nextStep(village, n, target[0], target[1]);
            if (step < 0) continue;
            n.dir = step; n.toX = n.x + Game.DX[step]; n.toY = n.y + Game.DY[step];
            n.walking = true; n.walkProgress = 0;
        }
    }

    boolean walkable(Game.GameMap map, Game.Npc self, int x, int y) {
        if (map.solid(x, y)) return false;
        for (Game.Warp w : map.warps) if (w.x() == x && w.y() == y) return false;
        if (game.map == map && (game.tx == x && game.ty == y
                || game.moving && game.nx == x && game.ny == y)) return false;
        for (Game.Npc n : map.npcs) {
            if (n == self || !game.vis(n)) continue;
            if (n.x == x && n.y == y || n.walking && n.toX == x && n.toY == y) return false;
        }
        return true;
    }

    // BFS em tiles reserva origem e destino: NPCs não atravessam o jogador nem uns aos outros.
    int nextStep(Game.GameMap map, Game.Npc n, int targetX, int targetY) {
        int[][] first = new int[map.h][map.w];
        for (int[] row : first) Arrays.fill(row, -2);
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(n.x, n.y)); first[n.y][n.x] = -1;
        while (!queue.isEmpty()) {
            Point p = queue.remove();
            if (p.x == targetX && p.y == targetY) return first[p.y][p.x];
            for (int d = 0; d < 4; d++) {
                int x = p.x + Game.DX[d], y = p.y + Game.DY[d];
                if (!walkable(map, n, x, y) || first[y][x] != -2) continue;
                first[y][x] = first[p.y][p.x] == -1 ? d : first[p.y][p.x];
                queue.add(new Point(x, y));
            }
        }
        return -1;
    }

    boolean talk(Game.Npc n) {
        switch (n.id) {
            case "beto" -> {
                if (!rod) {
                    rod = true;
                    game.say(n.name, "O lago está cheio de vida! Esta vara é sua. Cuide bem dela.",
                        "Aponte para a água e aperte Z. Quando a boia afundar, aperte Z de novo!",
                        "Depois, segure e solte Z para manter o peixe na faixa verde. O álbum fica no menu de pausa.");
                } else if (completeAlbum()) game.say(n.name, "As oito espécies! Esse troféu é merecido. Você conhece as águas de Aurora melhor que eu!");
                else game.say(n.name, "Já registrou " + discovered() + " das oito espécies! Cada água guarda um segredo.",
                    game.isNight() ? "O Bagre da Lua gosta do lago à noite, depois que o farol volta a brilhar." : "Dizem que a Carpa Dourada volta ao lago quando o farol se acende.");
            }
            case "rosa" -> game.say(n.name, game.isNight() ? "Já vou recolher as flores. Amanhã tem mais conversa na praça!"
                : "Passei a manhã cuidando das flores. Uma vila bonita é uma vila onde a gente se encontra.",
                "Beto conhece os peixes; Lucas não larga os sinos. E o professor? Sempre olhando o céu!");
            case "lucas" -> game.say(n.name, bellBest >= BELL_ROUNDS ? "Cinco melodias! Você ganhou de mim. A medalha ficou bonita!"
                : "Os sinos da praça cantam quatro notas. Eu já consegui repetir " + Math.max(2, bellBest) + " melodias!",
                "Cada sino tem uma cor e uma seta. Olhe a sequência e toque na mesma ordem.");
            case "sino" -> startBells();
            case "barco" -> {
                if (game.stage < 14) return false;
                game.say("", "A Capitã deixou o barco pronto para outra visita à ilha.", "As águas das estrelas esperam por você!");
                game.afterDlg = () -> { game.time += 90; game.travel("Ilha das Estrelas", 4, 10); };
            }
            default -> { return false; }
        }
        return true;
    }

    List<Integer> availableFish() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < FISH.length; i++) {
            Fish f = FISH[i];
            if (f.map().equals(game.map.name) && game.stage >= f.stage()
                    && (f.period() < 0 || (f.period() == 1) == game.isNight())) ids.add(i);
        }
        return ids;
    }

    void cast() {
        if (!rod) { game.say("", "A água se mexe... O Tio Beto, na vila, deve ter uma vara sobrando."); return; }
        List<Integer> pool = availableFish();
        if (pool.isEmpty()) { game.say("", "As águas estão quietas. Talvez em outro horário, ou depois de uma nova aventura."); return; }
        fishIndex = game.weather.chooseFish(pool, random);
        fishPhase = WAIT; fishTimer = 1.8 + random.nextDouble() * 2.8; fishElapsed = 0;
        fishPosition = 0.5; fishVelocity = 0; fishTarget = 0.5; catchProgress = 0.3;
        result = ""; game.state = Game.FISHING;
        game.audio.sfx("step");
    }

    void updateGame(double dt) {
        if (game.state == Game.BELLS) { updateBells(dt); return; }
        if (game.state != Game.FISHING || fishPhase == RESULT) return;
        fishElapsed += dt;
        if (fishPhase == WAIT || fishPhase == BITE) {
            fishTimer -= dt;
            if (fishTimer <= 0) {
                if (fishPhase == WAIT) { fishPhase = BITE; fishTimer = 1.15; game.audio.sfx("twinkle"); }
                else endFish(false, "O peixe escapou da isca.");
            }
            return;
        }
        double difficulty = FISH[fishIndex].difficulty();
        fishTarget = 0.5 + 0.25 * Math.sin(fishElapsed * (0.8 + difficulty * 0.2))
            + 0.08 * Math.sin(fishElapsed * 2.1);
        double force = game.down.contains(KeyEvent.VK_Z) ? 1.25 : -1.0;
        fishVelocity = Math.max(-0.7, Math.min(0.7, fishVelocity + force * dt));
        fishPosition += fishVelocity * dt;
        if (fishPosition < 0 || fishPosition > 1) {
            fishPosition = Math.max(0, Math.min(1, fishPosition)); fishVelocity = 0;
        }
        double halfZone = halfZone();
        catchProgress += (Math.abs(fishPosition - fishTarget) <= halfZone ? 0.17 : -0.12) * dt;
        if (catchProgress >= 1) endFish(true, "");
        else if (catchProgress <= 0 || fishElapsed > 45) endFish(false, "O peixe voltou para o fundo.");
    }

    void endFish(boolean success, String text) {
        fishPhase = RESULT;
        if (success) {
            boolean first = catches[fishIndex] == 0, hadTrophy = completeAlbum();
            catches[fishIndex] = Math.min(999999, catches[fishIndex] + 1);
            game.festival.caughtFish();
            result = (first ? "Nova espécie: " : "Você pescou: ") + FISH[fishIndex].name() + "!";
            if (!hadTrophy && completeAlbum()) result = "Álbum completo! Troféu de Pesca conquistado!";
            game.audio.sfx(completeAlbum() ? "fanfare" : "pickup");
            game.save(false);
        } else { result = text; game.audio.sfx("back"); }
    }

    int discovered() { int count = 0; for (int n : catches) if (n > 0) count++; return count; }
    double halfZone() { return 0.20 - FISH[fishIndex].difficulty() * 0.025 + (game.bonds.quests[1] == 2 ? 0.035 : 0); }
    boolean completeAlbum() { return discovered() == FISH.length; }
    void openAlbum(int from) { albumReturn = from; albumSelection = 0; game.state = Game.ALBUM; }

    void startBells() {
        startBells(false);
    }

    int bellRounds() { return festivalBells ? Festival.BELL_ROUNDS : BELL_ROUNDS; }

    void startBells(boolean special) {
        festivalBells = special;
        sequence.clear(); bellFinished = false; result = "";
        game.state = Game.BELLS; nextRound();
    }

    void nextRound() {
        sequence.add(random.nextInt(4)); playback = 0; answer = 0;
        showing = true; lit = -1; bellTimer = 0.8; flashTimer = 0;
    }

    void ring(int index) {
        lit = index; flashTimer = festivalBells ? 0.3 : 0.45;
        game.audio.sfx("bell" + index);
    }

    void updateBells(double dt) {
        if (flashTimer > 0) { flashTimer -= dt; if (flashTimer <= 0) lit = -1; }
        if (!showing || bellFinished) return;
        bellTimer -= dt;
        if (bellTimer > 0) return;
        if (playback < sequence.size()) { ring(sequence.get(playback++)); bellTimer = festivalBells ? 0.55 : 0.75; }
        else { showing = false; lit = -1; }
    }

    void answerBell(int index) {
        if (showing || bellFinished) return;
        ring(index);
        if (index != sequence.get(answer)) {
            bellFinished = true; result = "Essa nota era diferente. Vamos tentar outra vez?";
            game.audio.sfx("bad"); return;
        }
        if (++answer < sequence.size()) return;
        if (!festivalBells) { bellBest = Math.max(bellBest, sequence.size()); game.save(false); }
        if (sequence.size() == bellRounds()) {
            bellFinished = true;
            result = festivalBells ? "Sete melodias! Vitória nos sinos do festival!" : "Cinco melodias! Medalha dos Sinos conquistada!";
            if (festivalBells) game.festival.winBells();
            game.audio.sfx("fanfare");
        } else nextRound();
    }

    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE) {
            game.state = game.state == Game.ALBUM ? albumReturn : Game.PLAY;
            game.down.clear(); return;
        }
        if (game.state == Game.ALBUM) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) albumSelection = (albumSelection + 7) % 8;
            else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) albumSelection = (albumSelection + 1) % 8;
            else if (key == KeyEvent.VK_P || key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER) game.state = albumReturn;
        } else if (game.state == Game.FISHING && key == KeyEvent.VK_Z) {
            if (fishPhase == WAIT) endFish(false, "A linha foi recolhida cedo demais.");
            else if (fishPhase == BITE) { fishPhase = REEL; fishElapsed = 0; game.audio.sfx("confirm"); }
            else if (fishPhase == RESULT) { game.state = Game.PLAY; game.down.clear(); }
        } else if (game.state == Game.BELLS) {
            if (bellFinished && (key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER)) { startBells(festivalBells); return; }
            int index = switch (key) {
                case KeyEvent.VK_UP, KeyEvent.VK_W, KeyEvent.VK_1 -> 0;
                case KeyEvent.VK_RIGHT, KeyEvent.VK_D, KeyEvent.VK_2 -> 1;
                case KeyEvent.VK_DOWN, KeyEvent.VK_S, KeyEvent.VK_3 -> 2;
                case KeyEvent.VK_LEFT, KeyEvent.VK_A, KeyEvent.VK_4 -> 3;
                default -> -1;
            };
            if (index >= 0) answerBell(index);
        }
    }

    String signature() { return rod + "|" + Arrays.toString(catches) + "|" + bellBest; }

    void save(Properties p) {
        p.setProperty("vila.vara", Boolean.toString(rod));
        p.setProperty("vila.sinos", Integer.toString(bellBest));
        for (int i = 0; i < catches.length; i++) p.setProperty("vila.peixe." + i, Integer.toString(catches[i]));
        for (Resident r : RESIDENTS) {
            Game.Npc n = resident(r.id()); String prefix = "vila.npc." + r.id();
            p.setProperty(prefix + ".x", Integer.toString(n.x)); p.setProperty(prefix + ".y", Integer.toString(n.y));
            p.setProperty(prefix + ".casa", Boolean.toString(n.sleeping));
        }
    }

    int number(Properties p, String key, int fallback, int max) {
        try { return Math.max(0, Math.min(max, Integer.parseInt(p.getProperty(key, Integer.toString(fallback))))); }
        catch (NumberFormatException e) { return fallback; }
    }

    void load(Properties p) {
        rod = Boolean.parseBoolean(p.getProperty("vila.vara", "false"));
        bellBest = number(p, "vila.sinos", 0, BELL_ROUNDS);
        for (int i = 0; i < catches.length; i++) catches[i] = number(p, "vila.peixe." + i, 0, 999999);
        Game.GameMap village = game.maps.get("Vila Aurora");
        for (Resident r : RESIDENTS) {
            Game.Npc n = resident(r.id()); String prefix = "vila.npc." + r.id();
            int x = number(p, prefix + ".x", n.x, village.w - 1), y = number(p, prefix + ".y", n.y, village.h - 1);
            if (!village.solid(x, y)) { n.x = x; n.y = y; }
            n.sleeping = r.goesHome() && x == 18 && y == 6 && Boolean.parseBoolean(p.getProperty(prefix + ".casa", "false"));
        }
        // Saves anteriores podem colocar o jogador onde agora existe um novo morador.
        for (Resident r : RESIDENTS) {
            Game.Npc n = resident(r.id());
            if (n.sleeping || walkable(village, n, n.x, n.y)) continue;
            boolean found = false;
            for (int radius = 1; radius < village.w + village.h && !found; radius++) {
                for (int y = 1; y < village.h - 1 && !found; y++) {
                    for (int x = 1; x < village.w - 1; x++) {
                        if (Math.abs(n.x - x) + Math.abs(n.y - y) != radius || !walkable(village, n, x, y)) continue;
                        n.x = x; n.y = y; found = true; break;
                    }
                }
            }
        }
    }

    boolean drawItem(Graphics2D g, String key, int x, int y, int size) {
        if (!key.equals("vara") && !key.equals("trofeu") && !key.equals("sinos")) return false;
        Graphics2D icon = (Graphics2D) g.create();
        icon.translate(x, y); icon.scale(size / 48.0, size / 48.0);
        icon.setStroke(new BasicStroke(3));
        if (key.equals("vara")) {
            icon.setColor(new Color(124, 84, 56)); icon.drawLine(8, 40, 31, 6);
            icon.setColor(new Color(172, 206, 217)); icon.drawLine(31, 6, 38, 30);
            icon.setColor(Color.WHITE); icon.fillOval(34, 28, 9, 13);
            icon.setColor(new Color(227, 87, 79)); icon.fillArc(34, 28, 9, 13, 0, 180);
        } else if (key.equals("trofeu")) {
            icon.setColor(new Color(242, 190, 54));
            icon.drawOval(4, 8, 17, 18); icon.drawOval(27, 8, 17, 18);
            icon.fillArc(12, 3, 24, 31, 180, 180); icon.fillRect(12, 5, 24, 14);
            icon.fillRect(21, 28, 6, 11); icon.fillRoundRect(12, 38, 24, 6, 3, 3);
            icon.setColor(new Color(255, 233, 136)); icon.fillRect(16, 8, 4, 12);
        } else {
            icon.setColor(new Color(210, 79, 85));
            icon.fillPolygon(new int[]{10, 19, 26, 17}, new int[]{3, 3, 25, 25}, 4);
            icon.setColor(new Color(70, 150, 210));
            icon.fillPolygon(new int[]{29, 38, 31, 22}, new int[]{3, 3, 25, 25}, 4);
            icon.setColor(new Color(244, 195, 66)); icon.fillOval(12, 20, 25, 25);
            drawBell(icon, 19, 25, 12, new Color(139, 95, 40));
        }
        icon.dispose(); return true;
    }

    boolean drawObject(Graphics2D g, Game.Npc n, int x, int y) {
        if (!n.id.equals("sino")) return false;
        g.setColor(new Color(86, 99, 94)); g.fillRect(x + 3, y + 3, 4, 30); g.fillRect(x + 25, y + 3, 4, 30);
        g.fillRect(x + 3, y + 3, 26, 4);
        drawBell(g, x + 8, y + 9, 17, new Color(235, 191, 72));
        return true;
    }

    void drawBell(Graphics2D g, int x, int y, int size, Color color) {
        g.setColor(color); g.fillArc(x + size / 6, y, size * 2 / 3, size, 0, 180);
        g.fillRect(x + size / 6, y + size / 3, size * 2 / 3, size / 3);
        g.fillRoundRect(x, y + size * 2 / 3, size, size / 6 + 2, 4, 4);
        g.fillOval(x + size * 2 / 5, y + size * 4 / 5, size / 5, size / 5);
    }

    void panel(Graphics2D g, String title, String subtitle) {
        g.setColor(new Color(0, 0, 0, 155)); g.fillRect(0, 0, Game.W, Game.H);
        g.setColor(PAPER); g.fillRoundRect(24, 24, 592, 432, 12, 12);
        g.setColor(INK); g.setFont(new Font("SansSerif", Font.BOLD, 23)); g.drawString(title, 44, 62);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.drawString(subtitle, 44, 85);
        g.setColor(new Color(180, 202, 194)); g.drawLine(44, 100, 596, 100);
    }

    void text(Graphics2D g, String text, int x, int y, int width, int size) {
        g.setColor(INK); g.setFont(new Font("SansSerif", Font.PLAIN, size));
        for (String line : game.wrap(text, g.getFontMetrics(), width)) { g.drawString(line, x, y); y += size + 6; }
    }

    void drawFish(Graphics2D g, int x, int y, int size, Color color) {
        g.setColor(color);
        g.fillOval(x, y, size, size / 2);
        g.fillPolygon(new int[]{x + size - 4, x + size + size / 4, x + size + size / 4},
            new int[]{y + size / 4, y - 2, y + size / 2 + 2}, 3);
        g.setColor(Color.WHITE); g.fillOval(x + size / 6, y + size / 9, size / 7, size / 7);
        g.setColor(INK); g.fillOval(x + size / 6, y + size / 9, Math.max(2, size / 14), Math.max(2, size / 14));
    }

    void draw(Graphics2D g) {
        if (game.state == Game.ALBUM) drawAlbum(g);
        else if (game.state == Game.FISHING) drawFishing(g);
        else drawBells(g);
    }

    void drawAlbum(Graphics2D g) {
        panel(g, "Álbum de pesca", discovered() + " / 8 espécies" + (completeAlbum() ? "  ·  Troféu conquistado" : ""));
        for (int i = 0; i < FISH.length; i++) {
            int y = 114 + i * 36;
            if (i == albumSelection) { g.setColor(new Color(205, 231, 220)); g.fillRoundRect(38, y, 260, 33, 6, 6); }
            drawFish(g, 48, y + 10, 24, catches[i] > 0 ? FISH[i].color() : new Color(170, 184, 180));
            text(g, catches[i] > 0 ? FISH[i].name() : "???", 87, y + 22, 205, 14);
        }
        Fish f = FISH[albumSelection];
        drawFish(g, 370, 126, 110, catches[albumSelection] > 0 ? f.color() : new Color(170, 184, 180));
        text(g, catches[albumSelection] > 0 ? f.name() : "Espécie desconhecida", 326, 220, 266, 18);
        text(g, f.map() + " · " + (f.period() < 0 ? "Dia e noite" : f.period() == 0 ? "Dia" : "Noite"), 326, 254, 258, 14);
        String hint = switch (f.stage()) {
            case 4 -> "Surge depois que o farol é aceso.";
            case 5 -> "Surge após encontrar o bilhete da Capitã.";
            case 11 -> "Nas águas da ilha, durante a expedição.";
            case 14 -> "Surge após encontrar a Estrela do Mar.";
            default -> "Habita estas águas desde o começo da aventura.";
        };
        text(g, hint, 326, 302, 255, 14);
        text(g, "Capturas: " + catches[albumSelection], 326, 374, 240, 14);
        text(g, rod ? "Vara do Tio Beto" : "Vara: ainda não encontrada", 326, 398, 250, 13);
        text(g, "↑ ↓", 46, 437, 100, 15);
        text(g, "ESC", 548, 437, 55, 13);
    }

    void drawFishing(Graphics2D g) {
        panel(g, "Pesca", game.map.name);
        g.setColor(new Color(81, 159, 177)); g.fillRoundRect(44, 118, 552, 126, 8, 8);
        for (int i = 0; i < 12; i++) {
            int x = 56 + i * 46, y = 145 + (i % 3) * 24;
            g.setColor(new Color(167, 219, 220)); g.drawArc(x, y, 28, 10, 180, 180);
        }
        int bobY = fishPhase == BITE ? 215 : 164 + (int) (Math.sin(fishElapsed * 4) * 5);
        g.setColor(PAPER); g.drawLine(120, 122, 319, bobY);
        g.setColor(Color.WHITE); g.fillOval(312, bobY, 16, 20);
        g.setColor(new Color(220, 75, 78)); g.fillArc(312, bobY, 16, 20, 0, 180);
        if (fishPhase == WAIT) text(g, "A boia dança na água...", 60, 291, 500, 20);
        else if (fishPhase == BITE) text(g, "Fisgou!  Z", 60, 291, 500, 24);
        else if (fishPhase == REEL) {
            int bx = 68, by = 280, width = 500;
            g.setColor(new Color(195, 208, 208)); g.fillRoundRect(bx, by, width, 34, 6, 6);
            double half = halfZone();
            g.setColor(new Color(72, 178, 116));
            g.fillRect(bx + (int) ((fishTarget - half) * width), by, (int) (half * 2 * width), 34);
            int fishX = bx + (int) (fishPosition * (width - 22));
            drawFish(g, fishX, by + 9, 20, INK);
            g.setColor(new Color(195, 208, 208)); g.fillRect(bx, 349, width, 12);
            g.setColor(new Color(39, 127, 151)); g.fillRect(bx, 349, (int) (width * catchProgress), 12);
            text(g, "Z  ●", 68, 392, 150, 17);
            text(g, Math.round(catchProgress * 100) + "%", 508, 392, 75, 16);
        } else {
            text(g, result, 60, 285, 520, 19);
            if (catches[fishIndex] > 0) drawFish(g, 470, 350, 54, FISH[fishIndex].color());
            text(g, "Z  ↵", 60, 394, 160, 17);
        }
        text(g, "ESC", 548, 437, 55, 13);
    }

    void drawBells(Graphics2D g) {
        panel(g, festivalBells ? "Sinos em festa" : "Sinos da Praça", "Rodada " + sequence.size() + " / " + bellRounds()
            + (festivalBells ? "   ·   Festival de Aurora" : "   ·   Recorde " + bellBest));
        text(g, bellFinished ? result : showing ? "A praça está cantando..." : "Sua vez · " + answer + " / " + sequence.size(),
            48, 131, 540, 17);
        for (int i = 0; i < 4; i++) {
            int x = 53 + i * 140;
            g.setColor(i == lit ? BELL_COLORS[i].brighter() : new Color(218, 230, 225));
            g.fillRoundRect(x, 176, 114, 152, 8, 8);
            if (i == lit) { g.setColor(INK); g.setStroke(new BasicStroke(3)); g.drawRoundRect(x, 176, 114, 152, 8, 8); g.setStroke(new BasicStroke(1)); }
            drawBell(g, x + 30, 198, 55, BELL_COLORS[i]);
            text(g, BELL_NAMES[i], x + 35, 292, 78, 19);
            text(g, BELL_KEYS[i], x + 4, 319, 109, 11);
        }
        if (bellFinished) text(g, "Z  ↻", 54, 390, 160, 22);
        text(g, "ESC", 548, 437, 55, 13);
    }
}
