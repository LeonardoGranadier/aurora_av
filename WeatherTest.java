import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Calendario, probabilidades, abrigos e persistencia. Executar em pasta temporaria vazia. */
public class WeatherTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static int dayOf(Weather weather, int kind) {
        for (int day = 2; day < 366; day++) if (day % 7 != 0 && weather.forDay(day) == kind) return day;
        throw new AssertionError("Missing weather kind");
    }
    static void settle(Game g) {
        for (int tick = 0; tick < 2400; tick++) {
            g.life.updateResidents(0.05);
            Set<Point> occupied = new HashSet<>();
            for (VillageLife.Resident r : VillageLife.RESIDENTS) {
                Npc n = g.life.resident(r.id()); if (n.sleeping) continue;
                check(occupied.add(new Point(n.x, n.y)), "No resident overlap");
                if (n.walking) check(occupied.add(new Point(n.toX, n.toY)), "No destination overlap");
            }
        }
    }
    static int pixels(Game g) {
        BufferedImage image = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics(); g.weather.drawWorld(graphics); graphics.dispose();
        return Arrays.hashCode(image.getRGB(0, 0, Game.W, Game.H, null, 0, Game.W));
    }
    static void run() throws Exception {
        Game g = new Game(); Weather w = g.weather; w.seed = Weather.LEGACY_SEED;
        g.state = Game.PLAY; g.map = g.maps.get("Vila Aurora"); g.tx = 14; g.ty = 15;
        check(w.forDay(1) == Weather.SUN, "First day sunny");
        int rain = dayOf(w, Weather.RAIN), fog = dayOf(w, Weather.FOG), sun = dayOf(w, Weather.SUN);
        for (int i = 1; i <= 365; i++) check(w.forDay(i) == w.forDay(i), "Stable forecast");
        FestivalTest.date(g, rain, 22); settle(g);
        check(g.life.resident("beto").sleeping && g.life.resident("rosa").sleeping && g.life.resident("lucas").sleeping, "Rain preserves night rest");
        FestivalTest.date(g, rain, 8); settle(g);
        for (int i = 0; i < Bonds.IDS.length; i++) {
            Npc n = g.life.resident(Bonds.IDS[i]);
            check(!n.sleeping && n.x == Weather.SHELTER[i][0] && n.y == Weather.SHELTER[i][1], "Everyone can leave home and reach shelter");
        }
        Set<Point> reachable = VillageLifeTest.reachable(g, 6, 7);
        check(reachable.contains(new Point(18, 7)) && reachable.contains(new Point(19, 15)), "Door and lake stay accessible");
        for (String id : Bonds.IDS) {
            Npc n = g.life.resident(id); boolean accessible = false;
            for (int d = 0; d < 4; d++) accessible |= reachable.contains(new Point(n.x + Game.DX[d], n.y + Game.DY[d]));
            check(accessible, "Sheltered resident can be approached");
        }
        FestivalTest.date(g, sun, 10); settle(g);
        for (VillageLife.Resident r : VillageLife.RESIDENTS) {
            Npc n = g.life.resident(r.id()); check(n.x == r.stops()[0][0] && n.y == r.stops()[0][1], "Sunny routines resume");
        }
        FestivalTest.date(g, 7, 10);
        for (int i = 0; i < Bonds.IDS.length; i++) check(Arrays.equals(w.destination(VillageLife.RESIDENTS[i], 0), Festival.PLAZA[i]), "Festival has priority");
        FestivalTest.date(g, rain, 10); g.stage = 4;
        check(g.life.availableFish().equals(java.util.List.of(0, 1)), "Rain keeps story/time gates");
        check(w.fishWeight(0) > 0 && w.fishWeight(1) > w.fishWeight(0), "Rain increases carpa odds without removing lambari");
        int[] counts = new int[8]; Random random = new Random(23);
        for (int i = 0; i < 12000; i++) counts[w.chooseFish(g.life.availableFish(), random)]++;
        check(counts[0] > 0 && counts[1] > counts[0] * 2 && counts[1] < counts[0] * 4, "Weighted sample matches rain bonus");
        for (VillageLife.Fish fish : VillageLife.FISH) {
            g.map = g.maps.get(fish.map()); g.stage = fish.stage();
            int index = Arrays.asList(VillageLife.FISH).indexOf(fish);
            for (int day : new int[]{sun, rain, fog}) {
                FestivalTest.date(g, day, fish.period() == 1 ? 22 : 10);
                check(g.life.availableFish().contains(index) && w.fishWeight(index) > 0, "Every species remains possible in all climates");
            }
        }
        g.map = g.maps.get("Vila Aurora"); FestivalTest.date(g, rain, 10);
        int today = w.current(), tomorrow = w.forDay(w.day() + 1); long seed = w.seed;
        check(g.save(false), "Save weather"); g.newGame(); check(g.loadGame(), "Load weather");
        check(w.seed == seed && w.current() == today && w.forDay(w.day() + 1) == tomorrow, "Reload cannot reroll weather or forecast");
        g.state = Game.PLAY; g.time = Math.floor(g.time / 1440) * 1440 + 1439.99;
        g.last = System.nanoTime() - 50_000_000; g.update(); check(w.current() == tomorrow, "Midnight follows forecast");
        VillageLifeTest.tap(g, KeyEvent.VK_F); check(g.state == Game.WEATHER, "Forecast shortcut");
        check(g.saveBeforeClose(), "Forecast saves on close"); VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.PLAY, "Forecast returns to play");
        g.openPause(); g.choose("Previsão do tempo"); VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.MENU, "Forecast returns to menu");
        g.state = Game.PLAY; FestivalTest.date(g, sun, 10); int clear = pixels(g);
        FestivalTest.date(g, rain, 10); int wet = pixels(g); g.frame += 3;
        check(wet != clear && wet != pixels(g), "Rain renders and moves");
        g.map = g.maps.get("Casa"); check(pixels(g) == clear, "Rain absent indoors");
        g.map = g.maps.get("Trilha da Montanha"); check(pixels(g) == clear && w.label().equals("Neve"), "Existing mountain snow preserved");
        g.map = g.maps.get("Vila Aurora"); FestivalTest.date(g, fog, 10);
        int mist = pixels(g); g.frame += 25; check(mist != clear && mist != pixels(g), "Fog renders and moves");
        g.tx = 17; g.ty = 10; g.toastT = 0; g.bannerT = 0;
        FestivalTest.date(g, rain, 10); settle(g); VillageLifeTest.screenshot(g, "weather-rain");
        FestivalTest.date(g, fog, 10); VillageLifeTest.screenshot(g, "weather-fog");
        w.open(Game.PLAY); VillageLifeTest.screenshot(g, "weather-forecast");
        g.map = g.maps.get("Casa"); g.openPause(); VillageLifeTest.screenshot(g, "weather-pause");
        Properties old = g.readSave(); old.remove("clima.semente");
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { old.store(out, "Old save"); }
        check(g.loadGame() && w.seed == Weather.LEGACY_SEED, "Old save has stable default weather");
        Properties invalid = new Properties(); invalid.setProperty("clima.semente", "invalid"); w.load(invalid);
        check(w.seed == Weather.LEGACY_SEED, "Invalid seed uses stable fallback");
        System.out.println("PASS: " + VillageLifeTest.checks + " weather checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
        });
        System.exit(0);
    }
}
