import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Run in an empty temporary directory to protect existing saves. */
public class WorldMapTest {
    static void check(boolean value, String message) { VillageLifeTest.check(value, message); }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        check(!g.getFocusTraversalKeysEnabled(), "Tab reaches game input");
        check(g.worldMap.visited.equals(Set.of("Casa")), "New game only discovers home");
        VillageLifeTest.tap(g, KeyEvent.VK_TAB); check(g.state == Game.WORLD_MAP, "Tab opens map");
        double time = g.time; g.update(); check(time == g.time, "Map pauses clock");
        VillageLifeTest.screenshot(g, "map-new");
        VillageLifeTest.tap(g, KeyEvent.VK_TAB); check(g.state == Game.PLAY && g.down.isEmpty(), "Tab closes map without stuck keys");
        g.say("", "Test"); VillageLifeTest.tap(g, KeyEvent.VK_TAB); check(g.state == Game.PLAY, "Dialogue blocks map"); g.dlg = null;
        g.moving = true; VillageLifeTest.tap(g, KeyEvent.VK_TAB); check(g.state == Game.PLAY, "Movement blocks map"); g.moving = false;
        g.tx = 5; g.ty = 7; g.checkWarp();
        check(g.worldMap.visited.contains("Vila Aurora"), "Warp discovers region");
        check(g.readSave().getProperty("mapa.visitados").contains("Vila Aurora"), "Warp discovery autosaves");
        g.travel("Ilha das Estrelas", 4, 10);
        check(g.worldMap.visited.contains("Ilha das Estrelas"), "Boat travel discovers region");
        check(g.loadGame() && g.worldMap.visited.size() == 3, "Discoveries persist");
        Properties legacy = new Properties(); legacy.setProperty("mapa.visitados", "Fake|Casa");
        g.worldMap.load(legacy); check(g.worldMap.visited.equals(Set.of("Casa", "Ilha das Estrelas")), "Unknown save names ignored");
        g.worldMap.load(new Properties()); check(g.worldMap.visited.equals(Set.of("Ilha das Estrelas")), "Legacy save discovers current region only");
        for (int stage = 0; stage < Game.OBJETIVOS.length; stage++) {
            g.stage = stage;
            for (String name : g.worldMap.destinations()) check(g.maps.containsKey(name), "Every story destination exists");
            check(stage == 28 || !g.worldMap.destinations().isEmpty(), "Active story has destination");
        }
        g.stage = 6; g.frags = 1;
        check(g.worldMap.destinations().equals(Set.of("Praia Azul", "Gruta do Eco")), "Collected fragments removed from destinations");
        g.stage = 9; g.bye = 1; check(g.worldMap.destinations().equals(Set.of("Praia Azul")), "Farewell updates destination");
        check(g.worldMap.connected("Praia Azul", "Ilha das Estrelas"), "Boat route shown");
        for (WorldMap.Region r : WorldMap.REGIONS) {
            check(g.maps.containsKey(r.name()), "Every region exists");
            g.worldMap.visited.add(r.name());
        }
        g.stage = 25; g.travel(ChapterFive.GARDEN, 9, 16); g.worldMap.open(Game.PLAY);
        g.toastT = 0; VillageLifeTest.screenshot(g, "map-complete");
        check(g.saveBeforeClose(), "Closing with map open saves");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Escape returns to play");
        g.travel("Casa", 5, 5); g.openPause(); g.choose("Mapa de Aurora");
        check(g.state == Game.WORLD_MAP, "Pause menu opens map");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.MENU, "Escape returns to pause");
        g.toastT = 0; VillageLifeTest.screenshot(g, "map-pause");
        g.newGame(); check(g.worldMap.visited.equals(Set.of("Casa")), "Restart clears discoveries");
        System.out.println("PASS: " + VillageLifeTest.checks + " world map checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
