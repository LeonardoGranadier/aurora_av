import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Run in an empty temporary directory with the assets link (sh testar.sh SeasonsCookingTest). */
public class SeasonsCookingTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    static void at(Game g, int shownDay, int hour) { g.time = (shownDay - 1) * 1440 + hour * 60; }
    static Npc npc(Game g, String map, String id) { return ClassicFeaturesTest.npc(g, map, id); }

    static void seasons(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY;
        check(Seasons.season(1) == Seasons.SPRING && Seasons.season(28) == Seasons.SPRING, "Spring lasts 28 days");
        check(Seasons.season(29) == Seasons.SUMMER && Seasons.season(57) == Seasons.AUTUMN && Seasons.season(85) == Seasons.WINTER, "Season order");
        check(Seasons.season(113) == Seasons.SPRING && Seasons.year(113) == 2 && Seasons.dayOfSeason(113) == 1, "New year");
        for (int s = 0; s < 4; s++) {
            at(g, s * 28 + 3, 10);
            for (int f = 0; f < 8; f++) check(g.weather.fishWeight(f) > 0, "No species disappears in any season");
            for (int f : Seasons.SEASON_FISH[s]) check(g.seasons.fishFactor(f) == 2, "Season fish favoured");
        }
        at(g, 3, 10); check(g.seasons.fishFactor(0) == 1 && g.seasons.fishFactor(1) == 1, "Spring keeps village rain odds");

        GameMap village = g.maps.get("Vila Aurora"), beach = g.maps.get("Praia Azul");
        at(g, 90, 10); check(g.seasons.snowCover(village) && !g.seasons.snowCover(beach) && !g.seasons.snowCover(g.maps.get("Casa")), "Winter covers inland");
        int winterRain = -1;
        for (int d = 85; d <= 112; d++) if (g.weather.forDay(d) == Weather.RAIN) { winterRain = d; break; }
        if (winterRain > 0) {
            at(g, winterRain, 10); g.map = village;
            check(g.weather.label().equals("Neve") && g.weather.snowing(), "Winter rain falls as snow");
            g.map = beach; check(g.weather.label().equals("Chuva"), "Beach keeps rain");
        }
        String[] shots = {"season-spring", "season-summer", "season-autumn", "season-winter"};
        for (int s = 0; s < 4; s++) {
            int day = s * 28 + 2;
            while (g.weather.forDay(day) != Weather.SUN) day++;
            at(g, day, 10); g.travel("Vila Aurora", 6, 9); g.toastT = 0; g.bannerT = 0;
            VillageLifeTest.screenshot(g, shots[s]);
        }
        at(g, 1, 10); g.update(); check(g.banner.equals("Primavera em Aurora"), "First day announces spring");
        at(g, 29, 8); g.update(); check(g.banner.equals("Começou o Verão!"), "Season change announced");

        // Quintal
        Gardening h = g.gardening; h.can = true;
        at(g, 90, 10); check(!h.plant(0, 2) && !h.plant(0, 0), "Nothing ordinary grows in winter");
        h.status = ""; g.gardening.open(); h.plot = 0; h.selected = 2; tap(g, KeyEvent.VK_Z);
        check(h.status.contains("não cresce"), "Out-of-season message"); tap(g, KeyEvent.VK_ESCAPE);
        g.stage = 28; check(h.plant(0, 3), "Star flower grows in winter");
        at(g, 26, 10); check(h.plant(1, 1), "Tulip planted in spring");
        h.watered[1] = 26; at(g, 27, 10); h.sync(); check(h.growth[1] == 1, "Spring growth");
        h.lastDay[1] = 29; h.watered[1] = 29; at(g, 30, 10); h.sync(); check(h.growth[1] == 1, "Tulip waits through summer");

        // Aniversários
        at(g, 18, 10); check(g.seasons.birthday(2) && g.seasons.birthdayOf(18) == 2, "Rosa's birthday in spring");
        g.seasons.announcedDay = 17; g.toastT = 0; g.update(); check(g.toastTxt.contains("Dona Rosa"), "Birthday announced");
        h.stock[0] = 1; int before = g.bonds.friendship[2];
        check(h.give(2, 0) && g.bonds.friendship[2] - before == 30, "Birthday gift doubles friendship");
        check(g.seasons.birthdayGifts.contains("2-1"), "Birthday gift recorded");
        check(g.seasons.sunnyLine(2).equals(Seasons.BIRTHDAY_LINES[2]), "Birthday dialogue");
        check(g.loadGame() && g.seasons.birthdayGifts.contains("2-1"), "Birthday persists");

        // Calendário
        g.state = Game.PLAY; g.openPause(); g.choose("Calendário"); check(g.state == Game.CALENDAR, "Calendar from pause");
        VillageLifeTest.screenshot(g, "calendar"); tap(g, KeyEvent.VK_RIGHT); check(g.seasons.calendarSeason == Seasons.SUMMER, "Calendar browses seasons");
        VillageLifeTest.screenshot(g, "calendar-summer"); tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.MENU, "Calendar returns");
        g.state = Game.PLAY; tap(g, KeyEvent.VK_F); tap(g, KeyEvent.VK_RIGHT); check(g.state == Game.CALENDAR, "Forecast opens calendar");
        tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Calendar returns to play");
    }

    static void cooking(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; at(g, 3, 10);
        Cooking c = g.cooking;
        GameMap house = g.maps.get("Casa");
        check(house.solid(7, 2) && npc(g, "Casa", "fogao_casa") != null && g.home.layoutAccessible(), "Stove sits in the wall, house stays accessible");
        g.travel("Casa", 7, 3); g.dir = 1; g.interact();
        check(g.state == Game.COOKING && c.kitchen, "Stove opens kitchen");
        c.selected = 3; tap(g, KeyEvent.VK_Z); check(c.status.contains("Tio Beto"), "Unknown recipe shows teacher");
        c.selected = 0; tap(g, KeyEvent.VK_Z); check(c.status.startsWith("Faltam"), "Missing ingredients");
        g.life.catches[0] = 2; g.gardening.stock[0] = 1;
        tap(g, KeyEvent.VK_Z); check(g.state == Game.HANDOFF, "Cooking asks confirmation");
        tap(g, KeyEvent.VK_RIGHT); tap(g, KeyEvent.VK_Z);
        check(c.dishes[0] == 1 && g.bonds.stock(0) == 1 && g.gardening.stock[0] == 0 && g.life.catches[0] == 2, "Soup cooked from stock, album kept");
        VillageLifeTest.screenshot(g, "kitchen");
        g.gardening.stock[0] = 1;
        Files.deleteIfExists(Path.of(Game.SAVE)); Files.createDirectory(Path.of(Game.SAVE)); Files.createFile(Path.of(Game.SAVE, "block"));
        check(!c.cook(0) && c.dishes[0] == 1 && g.bonds.stock(0) == 1 && g.gardening.stock[0] == 1, "Failed save restores ingredients");
        Files.delete(Path.of(Game.SAVE, "block")); Files.delete(Path.of(Game.SAVE));
        tap(g, KeyEvent.VK_RIGHT); check(!c.kitchen, "Kitchen switches to lunchbox at home");
        c.selected = 0; tap(g, KeyEvent.VK_Z); check(c.active(Cooking.HEART) && c.dishes[0] == 0, "Eating grants the day's effect");
        tap(g, KeyEvent.VK_ESCAPE);
        g.life.catches[4] = 1; check(g.bonds.give(3, 4) && g.bonds.friendship[3] == 22, "Warm heart boosts gifts by half"); g.dlg = null;
        at(g, 4, 10); check(!c.active(Cooking.HEART), "Effect ends with the day");

        c.dishes[1] = 1; check(c.eat(1) && c.speedFactor() > 1, "Sardine quickens steps");
        c.dishes[2] = 1; double zone = g.life.halfZoneFor(0); check(c.eat(2) && g.life.halfZoneFor(0) > zone, "Tea widens the fishing zone");
        g.stage = 15; c.dishes[6] = 1; check(c.known(6) && c.eat(6) && c.nightVision(), "Sea stew gives night vision");
        check(!c.known(3), "Beto's recipe needs friendship"); g.bonds.friendship[1] = 40; check(c.known(3), "Friendship teaches recipes");

        // Lancheira diante de um morador
        g.travel("Vila Aurora", 19, 14); g.dir = 1;
        Npc beto = g.life.resident("beto"); beto.x = 19; beto.y = 13; beto.walking = false; beto.sleeping = false;
        at(g, 5, 10); c.dishes[3] = 1; int friend = g.bonds.friendship[1];
        tap(g, KeyEvent.VK_K); check(g.state == Game.COOKING && c.target == 1, "Lunchbox targets the resident ahead");
        c.selected = 3; VillageLifeTest.screenshot(g, "lunchbox-gift");
        tap(g, KeyEvent.VK_Z); tap(g, KeyEvent.VK_RIGHT); tap(g, KeyEvent.VK_Z);
        check(c.dishes[3] == 0 && g.bonds.friendship[1] == Math.min(100, friend + 20) && g.dlg != null, "Favorite dish gift");
        ClassicFeaturesTest.finishDialogue(g);
        c.dishes[3] = 1; check(!c.give(1, 3), "Dish shares the daily gift limit");
        check(g.loadGame() && c.cooked.contains("sopa") && c.dishes[3] == 0 && c.dishes[6] == 0, "Cooking persists");
        Properties legacy = new Properties(); c.load(legacy); check(Arrays.stream(c.dishes).sum() == 0 && c.effects == 0, "Old saves start without dishes");
    }

    static void cat(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; at(g, 2, 10);
        Npc cat = npc(g, "Vila Aurora", "gato");
        check(g.vis(cat) && VillageCat.AREA.contains(cat.x, cat.y), "Cat out by day");
        at(g, 2, 22); check(!g.vis(cat), "Cat sleeps at night"); at(g, 2, 10);
        g.map = g.maps.get("Vila Aurora"); g.tx = 20; g.ty = 4;
        for (int i = 0; i < 400; i++) { g.cat.wait = 0; g.cat.update(0.5); check(VillageCat.AREA.contains(cat.x, cat.y), "Cat stays in its meadow"); }
        g.travel("Vila Aurora", cat.x, cat.y - 1);
        if (g.map.solid(cat.x, cat.y - 1)) g.travel("Vila Aurora", cat.x + 1, cat.y);
        for (int day = 0; day < 7; day++) {
            at(g, 3 + day, 10); check(g.cat.talk(cat), "Cat petted"); g.dlg = null;
            check(!g.cat.talk(cat) || g.cat.petDays == day + 1, "Once per day"); g.dlg = null;
        }
        g.explorer.checkTimer = 0; g.update();
        check(g.cat.petDays == 7 && g.explorer.unlocked.contains("gato"), "Cat friendship achievement");
        check(Explorer.ACHIEVEMENTS.size() == 30, "Thirty achievements");
        g.explorer.openCard(Game.PLAY); VillageLifeTest.screenshot(g, "card-20"); tap(g, KeyEvent.VK_ESCAPE);
        g.travel("Vila Aurora", 4, 14); g.toastT = 0; VillageLifeTest.screenshot(g, "cat");
    }

    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        seasons(g); cooking(g); cat(g);
        System.out.println("PASS: " + VillageLifeTest.checks + " season and cooking checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
