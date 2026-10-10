import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/** Calendario deterministico a partir de uma semente salva, sem sorteios na renderizacao. */
final class Weather {
    static final int SUN = 0, RAIN = 1, FOG = 2;
    static final long LEGACY_SEED = 20261008L;
    static final String[] NAMES = {"Sol", "Chuva", "Neblina"};
    static final int[][] SHELTER = {{16, 7}, {17, 7}, {19, 7}, {20, 7}};
    final Game game;
    long seed;
    int returnState;

    Weather(Game game) { this.game = game; }
    void reset() { seed = new Random().nextLong(); }
    void buildWorld() {
        GameMap village = game.maps.get("Vila Aurora");
        for (int y = 7; y <= 8; y++) for (int x = 16; x <= 20; x++)
            if (village.t[y][x] == '#') village.t[y][x] = '.';
    }
    int day() { return (int) Math.floor(game.time / 1440) + 1; }
    int forDay(int day) {
        if (day <= 1) return SUN;
        int roll = new Random(seed ^ (0x9E3779B97F4A7C15L * day)).nextInt(100);
        return roll < 50 ? SUN : roll < 80 ? RAIN : FOG;
    }
    int current() { return forDay(day()); }
    /** No inverno, a chuva cai como neve na paisagem coberta. */
    boolean snowing() { return current() == RAIN && game.seasons.snowCover(game.map); }
    String label() { return game.map.snowy || snowing() ? "Neve" : NAMES[current()]; }
    int[] destination(VillageLife.Resident resident, int slot) {
        if (game.festival.active()) return game.festival.destination(resident, slot);
        int who = game.bonds.residentIndex(resident.id());
        if (current() == RAIN && slot != 3 && who >= 0) return SHELTER[who];
        return resident.stops()[slot];
    }
    /** Chuva, estação e o prato "Sorte grande" multiplicam o peso; nenhuma espécie chega a zero. */
    int fishWeight(int fish) {
        return rainWeight(fish) * game.seasons.fishFactor(fish) * game.cooking.rareFactor(fish);
    }
    int rainWeight(int fish) {
        if (current() != RAIN || !game.map.outdoor || game.map.snowy) return 1;
        return fish == 1 || fish == 2 || fish == 3 || fish == 5 || fish == 6 ? 3 : 1;
    }
    int chooseFish(List<Integer> pool, Random random) {
        if (pool.isEmpty()) throw new IllegalArgumentException("Sem especies disponiveis");
        int total = 0;
        for (int fish : pool) total += fishWeight(fish);
        int roll = random.nextInt(total);
        for (int fish : pool) { roll -= fishWeight(fish); if (roll < 0) return fish; }
        throw new IllegalStateException("Probabilidades de pesca invalidas");
    }
    void afterTalk(Npc npc) {
        int who = game.bonds.residentIndex(npc.id);
        if (who < 0 || game.dlg == null || !game.map.outdoor) return;
        String line = switch (current()) {
            case RAIN -> switch (who) {
                case 0 -> "A chuva também tem seus segredos. Repare em como muda o cheiro da terra!";
                case 1 -> "Com chuva, alguns peixes ficam mais ativos. Carpas e bagres gostam dessa água renovada.";
                case 2 -> "Minhas flores agradecem essa chuva. Depois vou conferir se os vasos ficaram bem.";
                default -> "Gosto do som das gotas na cobertura. Parece outra música para os sinos!";
            };
            case FOG -> "A neblina deixou Aurora quietinha hoje. Ainda dá para explorar, mas vou caminhar com calma.";
            default -> game.seasons.sunnyLine(who);
        };
        String[] lines = Arrays.copyOf(game.dlg, game.dlg.length + 1);
        lines[lines.length - 1] = line; game.dlg = lines;
    }
    void open(int from) { returnState = from; game.state = Game.WEATHER; }
    void key(int key) {
        if (key == KeyEvent.VK_RIGHT) { game.seasons.openCalendar(returnState); return; }
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_F || key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER) {
            game.state = returnState; game.down.clear();
        }
    }
    void save(Properties p) { p.setProperty("clima.semente", Long.toString(seed)); }
    void load(Properties p) {
        try { seed = Long.parseLong(p.getProperty("clima.semente", Long.toString(LEGACY_SEED))); }
        catch (NumberFormatException e) { seed = LEGACY_SEED; }
    }
    void drawShelter(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals("Vila Aurora")) return;
        int x = 16 * Game.T - camX, y = 7 * Game.T - camY - 9;
        g.setColor(new Color(116, 98, 74)); g.fillRect(x, y + 2, 3, 34); g.fillRect(x + 157, y + 2, 3, 34);
        for (int i = 0; i < 10; i++) {
            g.setColor(i % 2 == 0 ? new Color(87, 146, 132) : new Color(223, 235, 220));
            g.fillRect(x + i * 16, y, 16, 12);
        }
    }
    void drawWorld(Graphics2D g) {
        if (!game.map.outdoor || game.map.snowy) return;
        if (snowing()) {
            g.setColor(new Color(220, 230, 245, 30)); g.fillRect(0, 0, Game.W, Game.H);
            game.drawSnow(g);
        } else if (current() == RAIN) {
            g.setColor(new Color(28, 61, 74, 24)); g.fillRect(0, 0, Game.W, Game.H);
            g.setColor(new Color(199, 227, 239, 115));
            for (int i = 0; i < 80; i++) {
                int x = (int) ((i * 97L + game.frame * 2) % (Game.W + 24)) - 12;
                int y = (int) ((i * 71L + game.frame * (5 + i % 3)) % (Game.H + 24)) - 12;
                g.drawLine(x, y, x - 3, y + 10);
            }
        } else if (current() == FOG) {
            // Faixas suaves mantem personagens, objetos e caminhos legiveis.
            for (int y = 0; y < Game.H; y += 3) {
                int alpha = 30 + (int) (14 * (1 + Math.sin(y / 38.0 + game.frame / 130.0)));
                g.setColor(new Color(212, 229, 225, alpha)); g.fillRect(0, y, Game.W, 3);
            }
        }
    }
    void icon(Graphics2D g, int kind, int x, int y) {
        if (kind == SUN) {
            g.setColor(new Color(234, 185, 58)); g.fillOval(x + 13, y + 9, 27, 27);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                g.drawLine(x + 26 + (int) (Math.cos(a) * 18), y + 22 + (int) (Math.sin(a) * 18),
                    x + 26 + (int) (Math.cos(a) * 24), y + 22 + (int) (Math.sin(a) * 24));
            }
        } else {
            g.setColor(new Color(128, 164, 177));
            g.fillOval(x + 3, y + 10, 28, 21); g.fillOval(x + 19, y + 2, 28, 29); g.fillOval(x + 33, y + 13, 23, 18);
            g.setColor(new Color(75, 139, 171));
            for (int i = 0; i < 3; i++) {
                if (kind == RAIN) g.drawLine(x + 15 + i * 14, y + 34, x + 11 + i * 14, y + 44);
                else g.drawLine(x + 2, y + 34 + i * 5, x + 54, y + 34 + i * 5);
            }
        }
    }
    void drawForecast(Graphics2D g) {
        game.life.panel(g, "O tempo em Aurora", game.seasons.name() + ", dia " + Seasons.dayOfSeason(day()) + " de " + Seasons.LENGTH + " · →: calendário");
        for (int i = 0; i < 3; i++) {
            int x = 49 + i * 185, kind = forDay(day() + i);
            icon(g, kind, x + 42, 139);
            game.life.text(g, i == 0 ? "Hoje" : i == 1 ? "Amanhã" : "Depois de amanhã", x, 223, 175, 15);
            game.life.text(g, "Dia " + (day() + i) + " · " + NAMES[kind], x, 248, 174, 14);
        }
        String detail = switch (current()) {
            case RAIN -> "Carpa Dourada, Bagre da Lua, Truta Verde, Peixe-Lanterna e Peixe-Coral ficam mais ativos na chuva.";
            case FOG -> "Névoa baixa sobre as trilhas. As águas mantêm o ritmo habitual.";
            default -> "Céu aberto nas terras baixas. Um bom dia para visitar os moradores.";
        };
        game.life.text(g, detail, 48, 304, 535, 16);
        int s = game.seasons.current();
        game.life.text(g, "Na estação: " + VillageLife.FISH[Seasons.SEASON_FISH[s][0]].name() + " e " + VillageLife.FISH[Seasons.SEASON_FISH[s][1]].name()
            + " aparecem mais. Flores que crescem: " + Seasons.cropsIn(s) + ". " + (s == Seasons.WINTER ? "No inverno, a chuva cai como neve." : ""), 48, 352, 535, 13);
        game.life.text(g, "Na montanha, a neve permanece. A programação do festival continua em qualquer clima.", 48, 398, 535, 13);
        game.life.text(g, "ESC", 548, 437, 55, 13);
    }
}
