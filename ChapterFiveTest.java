import java.awt.Point;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Run in a clean temporary directory, never beside the player's save. */
public class ChapterFiveTest {
    static void check(boolean value, String message) { VillageLifeTest.check(value, message); }
    static void close(Game g) {
        for (int i = 0; g.dlg != null && i < 100; i++) VillageLifeTest.tap(g, KeyEvent.VK_Z);
        check(g.dlg == null, "Dialogue closes");
    }
    static void enter(Game g, String map, int x, int y) {
        g.map = g.maps.get(map); g.tx = x; g.ty = y; g.state = Game.PLAY;
    }
    static void interact(Game g, String id) {
        Game.Npc n = g.map.npcs.stream().filter(v -> v.id.equals(id) && g.vis(v)).findFirst().orElseThrow();
        Set<Point> area = VillageLifeTest.reachable(g, g.tx, g.ty);
        for (int d = 0; d < 4; d++) {
            int x = n.x - Game.DX[d], y = n.y - Game.DY[d];
            if (!area.contains(new Point(x, y))) continue;
            g.tx = x; g.ty = y; g.dir = d; g.interact(); close(g); return;
        }
        throw new AssertionError("Unreachable interaction: " + id);
    }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY; g.stage = 21;
        check(!g.chapter.allowWarp(ChapterFive.TRAIL), "Trail locked before message");
        enter(g, "Praia Azul", 10, 10);
        Game.Npc door = g.maps.values().stream().flatMap(m -> m.npcs.stream()).filter(n -> n.id.equals("porta_farol")).findFirst().orElseThrow();
        FestivalTest.date(g, 2, 10); g.talk(door); close(g); check(g.stage == 21, "Message waits for night");
        FestivalTest.date(g, 2, 22); g.talk(door); close(g); check(g.stage == 22, "Night message starts chapter");
        g.talk(g.life.resident("prof")); close(g); check(g.stage == 23, "Professor unlocks trail");
        enter(g, "Floresta Verde", 28, 6);
        check(g.free(29, 6) && g.chapter.allowWarp(ChapterFive.TRAIL), "Forest entrance opens");
        check(VillageLifeTest.reachable(g, 1, 10).contains(new Point(29, 6)), "Trail reachable from village entrance");
        g.tx = 29; g.ty = 6; g.checkWarp();
        check(g.map.name.equals(ChapterFive.TRAIL) && g.free(g.tx, g.ty), "Forest warp enters trail safely");
        check(!VillageLifeTest.reachable(g, 1, 14).contains(new Point(12, 0)), "Gate cannot be bypassed");
        interact(g, "garden_seed0"); check(g.chapter.seeds == 1, "First seed collected");
        check(g.loadGame() && g.chapter.seeds == 1 && g.stage == 23, "Partial seeds autosaved"); g.state = Game.PLAY;
        interact(g, "garden_seed1"); interact(g, "garden_seed2");
        check(g.stage == 24 && g.chapter.seeds == 7, "All seeds collected");
        g.bannerT = 0; g.toastT = 0; VillageLifeTest.screenshot(g, "chapter-trail");
        interact(g, "garden_gate"); check(g.stage == 25 && g.chapter.allowWarp(ChapterFive.GARDEN), "Garden opens");
        check(VillageLifeTest.reachable(g, g.tx, g.ty).contains(new Point(12, 0)), "Garden exit reachable");
        g.tx = 12; g.ty = 0; g.checkWarp();
        check(g.map.name.equals(ChapterFive.GARDEN) && g.free(g.tx, g.ty), "Garden warp enters safely");
        interact(g, "garden_mural"); interact(g, "garden_fountain"); check(g.stage == 25, "Wrong symbols refused");
        interact(g, "garden_stone0");
        check(g.loadGame() && g.chapter.stones[0] == 0, "Partial puzzle autosaved"); g.state = Game.PLAY;
        for (int i = 0; i < 3; i++) while (g.chapter.stones[i] != ChapterFive.TARGET[i]) interact(g, "garden_stone" + i);
        FestivalTest.date(g, 3, 10); interact(g, "garden_fountain"); check(g.stage == 25, "Fountain needs night");
        interact(g, "fogueira"); check(g.isNight(), "Camp advances to night");
        interact(g, "garden_fountain"); check(g.stage == 26, "Garden blooms");
        interact(g, "garden_stone0"); check(g.chapter.solved(), "Completed puzzle stays solved");
        g.tx = 10; g.ty = 10; g.bannerT = 0; g.toastT = 0; VillageLifeTest.screenshot(g, "chapter-garden");
        interact(g, "garden_cintila"); check(g.stage == 27 && !g.home.unlocked(15), "Flower awaits delivery");
        check(VillageLifeTest.reachable(g, g.tx, g.ty).contains(new Point(9, 17)), "Garden return reachable");
        enter(g, ChapterFive.TRAIL, 12, 1);
        check(VillageLifeTest.reachable(g, 12, 1).contains(new Point(0, 14)), "Trail return reachable");
        enter(g, "Casa", 5, 5); interact(g, "mae");
        check(g.stage == 28 && g.home.unlocked(15), "Ending unlocks decoration");
        check(g.readSave().getProperty("fase").equals("28"), "Ending callback saved");
        check(g.loadGame() && g.stage == 28 && g.chapter.solved() && g.home.unlocked(15), "Ending survives reload");
        g.state = Game.PLAY;
        check(g.home.place(15, 7, 4, 0), "Flower can be placed");
        check(g.loadGame() && g.home.placed(15), "Flower placement persists");
        g.state = Game.PLAY; g.home.openDecor(Game.PLAY); g.home.selected = 15;
        VillageLifeTest.screenshot(g, "chapter-flower");
        g.newGame(); check(g.chapter.seeds == 0 && !g.chapter.solved() && !g.home.unlocked(15), "New game resets chapter");
        g.stage = 21; g.chapter.load(new Properties()); check(g.chapter.seeds == 0 && !g.chapter.solved(), "Legacy chapter four save supported");
        System.out.println("PASS: " + VillageLifeTest.checks + " chapter checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
