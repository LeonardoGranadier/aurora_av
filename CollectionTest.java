import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Museum (rare fish, insects, donations), village radio and achievement icons. Run in a clean temporary directory. */
public class CollectionTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    static Npc npc(Game g, String map, String id) { return ClassicFeaturesTest.npc(g, map, id); }
    static void meet(Game g, String map, String id) { NewChaptersTest.meet(g, map, id); }
    static void finish(Game g) { ClassicFeaturesTest.finishDialogue(g); }
    /** First day (0-based) of the given season with the given weather. */
    static int dayWith(Game g, int season, int weather) {
        for (int d = 0; d < 400; d++) if (Seasons.season(d + 1) == season && g.weather.forDay(d + 1) == weather) return d;
        throw new AssertionError("No day with season " + season + " and weather " + weather);
    }
    static void at(Game g, int day, int hour) { g.time = day * 1440 + hour * 60; }
    /** Flood fill from the map's warps (or the boat dock of the turtle island), walking on non-solid tiles. */
    static boolean reachable(GameMap m, int tx, int ty) {
        boolean[][] seen = new boolean[m.h][m.w]; Deque<int[]> q = new ArrayDeque<>();
        for (Warp w : m.warps) q.add(new int[]{w.x(), w.y()});
        if (m.name.equals(Boat.TURTLE)) q.add(new int[]{Boat.PORTS[3].x(), Boat.PORTS[3].y()});
        while (!q.isEmpty()) {
            int[] c = q.poll(); int x = c[0], y = c[1];
            if (x < 0 || y < 0 || x >= m.w || y >= m.h || seen[y][x] || m.solid(x, y)) continue;
            seen[y][x] = true;
            if (x == tx && y == ty) return true;
            q.add(new int[]{x + 1, y}); q.add(new int[]{x - 1, y}); q.add(new int[]{x, y + 1}); q.add(new int[]{x, y - 1});
        }
        return false;
    }
    static Random always(double value) { return new Random() { @Override public double nextDouble() { return value; } }; }

    static void museum(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = 0;
        meet(g, "Vila Aurora", "museu"); check(!g.museum.net && g.state == Game.PLAY, "Museum closed before chapter 1 ends"); finish(g);
        int spring = dayWith(g, 0, Weather.SUN);
        at(g, spring, 10);
        meet(g, "Vila Aurora", "inseto0"); check(g.museum.bugs[0] == 0, "Bugs escape without a net"); finish(g);
        g.stage = 4; meet(g, "Vila Aurora", "museu"); finish(g);
        check(g.museum.net && g.state == Game.MUSEUM, "First visit gives the net and opens the museum");
        VillageLifeTest.screenshot(g, "museum-empty"); tap(g, KeyEvent.VK_ESCAPE);

        // insetos: estação, hora e clima
        check(g.vis(npc(g, "Vila Aurora", "inseto0")), "Lemon butterfly on a sunny spring day");
        at(g, spring, 22); check(!g.vis(npc(g, "Vila Aurora", "inseto0")), "Butterfly hides at night");
        at(g, dayWith(g, 0, Weather.RAIN), 10); check(!g.vis(npc(g, "Vila Aurora", "inseto0")), "Butterfly hides in the rain");
        at(g, dayWith(g, 2, Weather.SUN), 10); check(!g.vis(npc(g, "Vila Aurora", "inseto0")), "Butterfly is gone in autumn");
        at(g, spring, 10); g.travel("Vila Aurora", 2, 18); g.dir = 0; g.toastT = 0; g.bannerT = 0;
        VillageLifeTest.screenshot(g, "bug-in-world");
        meet(g, "Vila Aurora", "inseto0"); check(g.museum.bugs[0] == 1, "Butterfly caught"); finish(g);
        check(!g.vis(npc(g, "Vila Aurora", "inseto0")), "Caught today: gone until tomorrow");
        int nextSunny = spring + 1; while (g.weather.forDay(nextSunny + 1) == Weather.RAIN) nextSunny++;
        at(g, nextSunny, 10); check(g.vis(npc(g, "Vila Aurora", "inseto0")), "Back the next day");
        check(!g.vis(npc(g, ChapterNine.FOREST, "inseto9")), "Aurora butterfly needs chapter 9");

        // peixe raro no lugar de um comum
        g.life.rod = true; g.museum.random = always(0);
        at(g, dayWith(g, 0, Weather.FOG), 10); g.travel("Praia Azul", 10, 13); g.dir = 0;
        g.life.cast(); check(g.state == Game.FISHING && g.life.rare == 2, "Fog brings the mist fish");
        int sardines = g.life.catches[4];
        g.life.endFish(true, ""); check(g.museum.fish[2] == 1 && g.life.catches[4] == sardines && g.life.result.contains("Peixe-Neblina"), "Rare fish goes to the collection");
        VillageLifeTest.screenshot(g, "rare-fish"); g.state = Game.PLAY;
        at(g, spring, 10); g.life.cast(); check(g.life.rare < 0, "No rare fish without fog"); g.state = Game.PLAY;
        g.stage = 23; g.travel(ChapterFive.TRAIL, 16, 15); g.museum.random = always(0.99); g.dlg = null;
        g.life.cast(); check(g.state == Game.PLAY && g.dlg != null, "Trail pond is quiet when the rare fish does not bite"); finish(g);
        g.museum.random = always(0); g.life.cast(); check(g.state == Game.FISHING && g.life.rare == 3, "Rainbow carp in the trail pond");
        g.life.endFish(true, ""); g.state = Game.PLAY;

        // doações
        int coins = g.home.coins;
        meet(g, "Vila Aurora", "museu"); check(g.state == Game.MUSEUM, "Museum opens");
        g.museum.selected = 2; tap(g, KeyEvent.VK_Z); check(g.museum.donatedFish(2) && g.home.coins == coins + 20, "Donation pays 20 coins");
        tap(g, KeyEvent.VK_Z); check(g.home.coins == coins + 20 && g.museum.status.contains("vitrine"), "No double donation");
        g.museum.selected = 0; tap(g, KeyEvent.VK_Z); check(!g.museum.donatedFish(0), "Cannot donate what was not found");
        tap(g, KeyEvent.VK_TAB); check(g.museum.tab == 1, "Insect wing"); g.museum.selected = 0; tap(g, KeyEvent.VK_Z);
        check(g.museum.donatedBug(0), "Bug donated");
        VillageLifeTest.screenshot(g, "museum-bugs");
        Arrays.fill(g.museum.fish, 1); Arrays.fill(g.museum.bugs, 1);
        for (int t = 0; t < 2; t++) { g.museum.tab = t; for (int i = 0; i < g.museum.count(); i++) { g.museum.selected = i; tap(g, KeyEvent.VK_Z); } }
        check(g.museum.fishWing() && g.museum.bugWing() && !g.museum.complete() && g.home.unlocked(26) && g.home.unlocked(27), "Fish and bug wings: aquarium and insect frame");
        tap(g, KeyEvent.VK_ESCAPE);
        g.explorer.checkTimer = 0; g.update();
        check(g.explorer.unlocked.containsAll(List.of("lendario", "insetos")) && !g.explorer.unlocked.contains("curador"), "Collection achievements");
        check(g.save(false) && g.loadGame() && g.museum.fishWing() && g.museum.net && g.museum.bugs[0] == 1, "Museum persists");
        Properties p = new Properties(); g.museum.save(p); p.setProperty("museu.peixe.3", "0"); g.museum.load(p);
        check(!g.museum.donatedFish(3), "Load never keeps a donation without the catch");
        Properties legacy = new Properties(); legacy.setProperty("museu.rede", "true"); legacy.setProperty("museu.doado", "" + (1 | 1 << 2 | 1 << 8));
        legacy.setProperty("museu.peixe.0", "1"); legacy.setProperty("museu.peixe.2", "1"); legacy.setProperty("museu.inseto.0", "2");
        g.museum.load(legacy);
        check(g.museum.donatedFish(0) && g.museum.donatedFish(2) && g.museum.donatedBug(0) && !g.museum.donatedFish(1) && !g.museum.shovel, "Old saves keep their donations");
        g.state = Game.PLAY; g.openPause(); check(g.opts.contains("Coleção do Museu"), "Pause menu shows the collection");
        g.choose("Coleção do Museu"); check(g.state == Game.MUSEUM, "Collection from the menu"); tap(g, KeyEvent.VK_Z);
        check(g.museum.status.contains("Museu"), "Donating from the menu asks to visit"); tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.MENU, "Back to the menu"); g.state = Game.PLAY;
    }

    static void boatAndFossils(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = 14;
        check(!g.vis(npc(g, "Praia Azul", "gaivota")), "No boat for sale before chapter 3 ends");
        g.stage = 15; g.home.coins = 100;
        meet(g, "Praia Azul", "gaivota"); finish(g); finish(g);
        check(!g.boat.owned && g.home.coins == 100, "Not enough coins");
        g.home.coins = 200; meet(g, "Praia Azul", "gaivota"); finish(g);
        check(g.state == Game.HANDOFF, "Asks before paying"); tap(g, KeyEvent.VK_LEFT); tap(g, KeyEvent.VK_Z); finish(g);
        check(g.boat.owned && g.home.coins == 50, "Boat repaired for 150 coins");
        meet(g, "Praia Azul", "gaivota"); check(g.state == Game.BOAT && Boat.PORTS[g.boat.selected].map().equals("Ilha das Estrelas"), "Nautical chart opens");
        VillageLifeTest.screenshot(g, "boat-chart");
        g.boat.selected = 2; tap(g, KeyEvent.VK_Z); check(g.map.name.equals(Boat.SEA) && g.state == Game.PLAY, "Sailed to the open sea");
        check(g.worldMap.visited.contains(Boat.SEA), "Open sea on the world map");
        g.toastT = 0; g.bannerT = 0; g.fade = 0; VillageLifeTest.screenshot(g, "open-sea");
        // pesca de águas fundas: sem peixe comum, o raro morde mais
        g.life.rod = true; at(g, 3, 10); g.tx = 9; g.ty = 8; g.dir = 0;
        g.museum.random = always(0.6); g.life.cast(); check(g.state == Game.FISHING && g.life.rare >= 8, "Deep-sea fish bites often");
        g.life.endFish(true, ""); check(g.museum.fish[g.life.rare] == 1, "Deep-sea fish collected"); g.state = Game.PLAY;
        g.explorer.checkTimer = 0; g.update(); check(g.explorer.unlocked.contains("marinheiro"), "Sailor achievement");
        meet(g, Boat.SEA, "gaivota_mar"); check(g.state == Game.BOAT, "Helm opens the chart");
        g.boat.selected = 3; tap(g, KeyEvent.VK_Z); check(g.map.name.equals(Boat.TURTLE), "Turtle island");
        meet(g, Boat.TURTLE, "tartaruga"); check(g.dlg != null && g.dlg[0].contains("pá"), "Turtle points to the shovel"); finish(g);
        g.toastT = 0; g.bannerT = 0; g.fade = 0; g.tx = 9; g.ty = 9; VillageLifeTest.screenshot(g, "turtle-island");
        meet(g, Boat.TURTLE, "gaivota_ilha"); g.boat.selected = 0; tap(g, KeyEvent.VK_Z); check(g.map.name.equals("Praia Azul"), "Sailed home");
        check(g.save(false) && g.loadGame() && g.boat.owned, "Boat persists"); g.state = Game.PLAY;

        // fósseis: a pá vem depois de 3 doações
        g.stage = 15; g.museum.net = true;
        int day = 0; while (!Museum.active(0, day)) day++;
        at(g, day, 10);
        check(!g.vis(npc(g, "Praia Azul", "escavar0")), "No glowing ground without the shovel");
        g.museum.fish[0] = 1; g.museum.bugs[0] = 1; g.museum.bugs[1] = 1;
        meet(g, "Vila Aurora", "museu"); finish(g); check(!g.museum.shovel, "No shovel before three donations"); g.state = Game.PLAY;
        for (int[] d : new int[][]{{0, 0}, {1, 0}, {1, 1}}) { g.museum.given[d[0]][d[1]] = true; }
        meet(g, "Vila Aurora", "museu"); check(g.museum.shovel && g.dlg != null, "Professor gives the shovel"); finish(g);
        check(g.state == Game.MUSEUM && g.museum.wings() == 3, "Fossil wing opens"); tap(g, KeyEvent.VK_ESCAPE);
        check(g.vis(npc(g, "Praia Azul", "escavar0")), "Glowing ground today");
        int active = 0; for (int i = 0; i < Museum.SPOTS.length; i++) if (Museum.active(i, day)) active++;
        check(active >= 3 && active <= 6, "About a third of the spots glow each day");
        g.museum.random = always(0); meet(g, "Praia Azul", "escavar0");
        check(g.museum.fossils[0] == 1 && g.dlg[0].contains("Amonite"), "Dug an ammonite"); finish(g);
        check(!g.vis(npc(g, "Praia Azul", "escavar0")), "Dug spot rests until tomorrow");
        int coins = g.home.coins; g.museum.random = always(0.99);
        int other = 1; while (!Museum.active(other, day)) other++;
        meet(g, Museum.SPOTS[other].map(), "escavar" + other); check(g.home.coins > coins, "Sometimes only coins"); finish(g);
        for (Museum.Spot sp : Museum.SPOTS) {
            GameMap m = g.maps.get(sp.map());
            check("#~HLXR".indexOf(m.at(sp.x(), sp.y())) < 0 && reachable(m, sp.x(), sp.y()), "Dig spot reachable " + sp);
        }
        // doar e completar a ala
        meet(g, "Vila Aurora", "museu"); g.museum.tab = Museum.FOSSIL_WING; g.museum.selected = 0; coins = g.home.coins; tap(g, KeyEvent.VK_Z);
        check(g.museum.donatedFossil(0) && g.home.coins == coins + 30 && g.museum.status.contains("amonite"), "Professor comments the fossil");
        VillageLifeTest.screenshot(g, "museum-fossils");
        Arrays.fill(g.museum.fossils, 1); Arrays.fill(g.museum.fish, 1); Arrays.fill(g.museum.bugs, 1);
        for (int t = 0; t < 3; t++) { g.museum.tab = t; for (int i = 0; i < g.museum.count(); i++) { g.museum.selected = i; tap(g, KeyEvent.VK_Z); } }
        check(g.museum.complete() && g.home.unlocked(28), "Three wings: miniature skeleton");
        tap(g, KeyEvent.VK_ESCAPE); g.explorer.checkTimer = 0; g.update();
        check(g.explorer.unlocked.containsAll(List.of("paleontologo", "curador")), "Fossil and curator achievements");
        check(g.save(false) && g.loadGame() && g.museum.shovel && g.museum.fossilWing(), "Fossils persist"); g.state = Game.PLAY;
    }

    static void radio(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = 10;
        meet(g, "Casa", "radio"); check(g.state == Game.RADIO && g.radio.channel == 0, "Radio opens on the music channel");
        int beach = -1, valley = -1;
        for (int i = 0; i < Radio.TRACKS.length; i++) { if (Radio.TRACKS[i].theme().equals("praia_dia")) beach = i; if (Radio.TRACKS[i].theme().equals("vale")) valley = i; }
        g.radio.selected = valley; tap(g, KeyEvent.VK_Z); check(g.radio.track == null, "Locked track does not play");
        g.worldMap.visited.add("Praia Azul");
        g.radio.selected = beach; tap(g, KeyEvent.VK_Z); check("praia_dia".equals(g.radio.track) && g.musicFor().equals("praia_dia"), "Track plays");
        VillageLifeTest.screenshot(g, "radio-music");
        tap(g, KeyEvent.VK_RIGHT); check(g.radio.channel == 1 && !g.radio.news().isEmpty(), "News channel");
        VillageLifeTest.screenshot(g, "radio-news");
        tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Radio closes");
        check(g.map.name.equals("Casa") && g.musicFor().equals("praia_dia"), "Radio music plays at home");
        g.travel("Vila Aurora", 15, 11); check(!g.musicFor().equals("praia_dia"), "Village keeps its own music");
        check(g.save(false) && g.loadGame() && "praia_dia".equals(g.radio.track), "Radio track persists"); g.state = Game.PLAY;
        g.openPause(); g.choose("Rádio de Aurora"); check(g.state == Game.RADIO && g.radio.returnState == Game.MENU, "Radio from the pause menu");
        g.radio.channel = 0; g.radio.selected = Radio.TRACKS.length; tap(g, KeyEvent.VK_Z); check(g.radio.track == null, "Radio off");
        tap(g, KeyEvent.VK_ESCAPE); g.state = Game.PLAY;
        // notícias: festival, aniversário e capítulo
        for (int d = 0; d < 112; d++) {
            at(g, d, 9);
            List<String> news = g.radio.news();
            check(news.size() >= 5, "Enough news on day " + d);
            if (Math.floorMod(d + 1, 7) == 0) check(news.get(2).startsWith("Hoje tem festa"), "Festival news on saturday");
        }
        g.stage = Story.FINAL; check(g.radio.news().contains(Radio.STORY_NEWS[Radio.STORY_NEWS.length - 1]), "Final news");
    }

    static void icons(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY;
        Set<Integer> looks = new HashSet<>();
        for (Explorer.Achievement a : Explorer.ACHIEVEMENTS) {
            BufferedImage on = g.icons.icon(a.id(), true), off = g.icons.icon(a.id(), false);
            check(on.getWidth() == AchievementIcons.BASE && (on.getRGB(32, 32) >>> 24) == 255, "Icon " + a.id());
            check(on.getRGB(32, 32) != off.getRGB(32, 32) || on.getRGB(30, 26) != off.getRGB(30, 26), "Locked icon is different " + a.id());
            int hash = 0; for (int y = 10; y < 54; y += 3) for (int x = 10; x < 54; x += 3) hash = hash * 31 + on.getRGB(x, y);
            looks.add(hash);
        }
        check(looks.size() == Explorer.ACHIEVEMENTS.size(), "Every achievement has its own icon");
        for (int i = 0; i < 14; i++) g.explorer.unlocked.add(Explorer.ACHIEVEMENTS.get(i * 2).id());
        g.explorer.openCard(Game.PLAY); g.explorer.cardSel = 4;
        VillageLifeTest.screenshot(g, "card-icons");
        tap(g, KeyEvent.VK_DOWN); check(g.explorer.cardSel == 4 + (Explorer.ACHIEVEMENTS.size() + 2) / 3, "Down moves one row");
        tap(g, KeyEvent.VK_ESCAPE);
    }

    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        museum(g); boatAndFossils(g); radio(g); icons(g);
        System.out.println("PASS: " + VillageLifeTest.checks + " museum, boat, radio and icon checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
