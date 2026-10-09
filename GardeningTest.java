import java.awt.Point;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Execute in a clean temporary directory with an assets link. */
public class GardeningTest {
    static void check(boolean value, String message) { VillageLifeTest.check(value, message); }
    static void run() throws Exception {
        Game g = new Game(); Gardening h = g.gardening; g.state = Game.PLAY;
        check(!h.can && Arrays.stream(h.crops).allMatch(v -> v == -1), "Legacy/new game starts empty");
        g.map = g.maps.get("Vila Aurora"); g.tx = 6; g.ty = 7;
        check(VillageLifeTest.reachable(g, 6, 7).contains(new Point(9, 7)), "Gate approachable from home");
        ChapterFiveTest.interact(g, "yard_gate");
        check(g.map.name.equals(Gardening.MAP) && g.worldMap.visited.contains(Gardening.MAP), "Gate enters and discovers yard");
        check(!h.plant(0, 0), "Tools required");
        ChapterFiveTest.interact(g, "yard_tools"); check(h.can, "Regador obtained");
        Set<Point> area = VillageLifeTest.reachable(g, g.tx, g.ty);
        for (int[] p : Gardening.PLOTS) check(area.contains(new Point(p[0], p[1] + 1)), "Every plot approachable");
        check(area.contains(new Point(6, 13)), "Return reachable");
        g.weather.seed = Weather.LEGACY_SEED; FestivalTest.date(g, 1, 10);
        check(h.plant(0, 0) && !h.plant(0, 1), "Plant only empty plots");
        check(!h.plant(1, 3), "Special seed locked");
        check(h.water(0) && !h.water(0), "Water once daily");
        FestivalTest.date(g, 2, 10); h.sync(); check(h.growth[0] == 1, "One watered day advances once");
        h.sync(); check(h.growth[0] == 1, "Same day cannot double growth");
        check(g.save(false) && g.loadGame() && h.growth[0] == 1, "Partial crop survives load");
        h.water(0); FestivalTest.date(g, 3, 10); h.sync(); check(h.ready(0), "Mature after two watered days");
        check(h.harvest(0) && h.stock[0] == 1 && h.crops[0] == -1 && !h.harvest(0), "Harvest exactly once");
        int dry = WeatherTest.dayOf(g.weather, Weather.SUN);
        FestivalTest.date(g, dry, 10); check(h.plant(1, 1), "Tulip planted");
        FestivalTest.date(g, dry + 1, 10); h.sync(); check(h.growth[1] == 0, "Dry day pauses growth");
        int rain = WeatherTest.dayOf(g.weather, Weather.RAIN);
        FestivalTest.date(g, rain, 10); check(h.plant(2, 2) && h.wet(2), "Rain waters new crops");
        FestivalTest.date(g, rain + 1, 10); h.sync(); check(h.growth[2] == 1, "Rain advances growth while away");
        FestivalTest.date(g, rain + 90, 10); h.sync(); check(h.ready(1) && h.ready(2), "Long travel accounts for rain");
        g.stage = 28; check(h.plant(3, 3), "Chapter five unlocks special seed");
        h.stock[0] = 5; int coins = g.home.coins;
        check(h.sell(0) && h.stock[0] == 4 && g.home.coins == coins + 5, "Rosa pays coins");
        check(h.give(2, 0) && g.bonds.friendship[2] == 15, "Rosa likes flowers");
        g.life.catches[0] = 3; check(!g.bonds.give(2, 0) && !h.give(2, 0), "Flowers and fish share daily limit");
        check(g.bonds.give(1, 0) && !h.give(1, 0), "Fish first also blocks flower"); g.dlg = null;
        FestivalTest.date(g, rain + 91, 22); check(!h.sell(0), "Shop closed at night");
        FestivalTest.date(g, rain + 91, 10); check(h.give(2, 0), "Gift resets next day");
        g.home.coins = HomeDecor.MAX_COINS; check(!h.sell(0), "Coin overflow prevented"); g.home.coins = coins;
        g.state = Game.PLAY; g.map = g.maps.get(Gardening.MAP); g.tx = 7; g.ty = 10; g.bannerT = 0; g.toastT = 0;
        VillageLifeTest.screenshot(g, "garden-yard");
        VillageLifeTest.tap(g, KeyEvent.VK_V); check(g.state == Game.GARDENING, "Basket shortcut");
        VillageLifeTest.screenshot(g, "garden-basket");
        double time = g.time; g.update(); check(g.time == time, "Basket pauses time");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Basket closes");
        g.map = g.maps.get("Vila Aurora"); g.tx = 8; g.ty = 9; g.dir = 1;
        VillageLifeTest.tap(g, KeyEvent.VK_V); check(h.selling, "Facing Rosa shop selects sales");
        VillageLifeTest.screenshot(g, "garden-sales");
        g.state = Game.PLAY;
        check(g.save(false), "Save before failure test");
        Files.delete(Path.of(Game.SAVE)); Files.createDirectory(Path.of(Game.SAVE)); Files.createFile(Path.of(Game.SAVE, "block"));
        int stock = h.stock[0]; coins = g.home.coins;
        check(!h.sell(0) && h.stock[0] == stock && g.home.coins == coins, "Failed sale rolls back");
        check(!h.harvest(1) && h.ready(1), "Failed harvest rolls back");
        check(!h.plant(0, 1) && h.crops[0] == -1, "Failed planting rolls back");
        int friendship = g.bonds.friendship[0], giftDay = g.bonds.gifted[0];
        check(!h.give(0, 0) && h.stock[0] == stock && g.bonds.friendship[0] == friendship && g.bonds.gifted[0] == giftDay, "Failed gift rolls back");
        Files.delete(Path.of(Game.SAVE, "block")); Files.delete(Path.of(Game.SAVE));
        check(g.save(false) && g.loadGame() && h.stock[0] == stock && h.ready(1), "State persists after failed operations");
        h.load(new Properties()); check(!h.can && h.stock[0] == 0 && h.crops[0] == -1, "Old saves have empty garden");
        g.newGame(); check(!h.can && h.stock[0] == 0, "New game resets gardening");
        System.out.println("PASS: " + VillageLifeTest.checks + " gardening checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
