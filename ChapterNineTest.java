import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Chapter 9: the eclipse, the Ancient Forest lanterns, the Great Tree and the new ending. Run in a clean temporary directory. */
public class ChapterNineTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    static Npc npc(Game g, String map, String id) { return ClassicFeaturesTest.npc(g, map, id); }
    static void meet(Game g, String map, String id) { NewChaptersTest.meet(g, map, id); }
    static void finish(Game g) { ClassicFeaturesTest.finishDialogue(g); }
    static void night(Game g) { NewChaptersTest.night(g); }
    static void day(Game g) { NewChaptersTest.day(g); }

    static void chapterNine(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = ChapterNine.START; day(g);
        check(g.worldMap.destinations().contains("Pico Estrelado"), "Map points to the peak");
        check(g.vis(npc(g, ChapterFive.GARDEN, "raizes_antigas")), "Roots close the forest");
        night(g); meet(g, "Pico Estrelado", "ceu_cintila");
        check(g.stage == ChapterNine.ENTER, "Cintila starts chapter 9"); finish(g);
        check(!g.vis(npc(g, ChapterFive.GARDEN, "raizes_antigas")), "Roots open");
        VillageLifeTest.screenshot(g, "eclipse-night");

        g.travel(ChapterFive.GARDEN, 9, 1); g.dir = ChapterEightTest.UP; ChapterEightTest.step(g, ChapterEightTest.UP); g.checkWarp();
        check(g.map.name.equals(ChapterNine.FOREST) && g.stage == ChapterNine.LANTERNS, "North passage reaches the forest");
        check(g.worldMap.visited.contains(ChapterNine.FOREST), "Forest discovered");
        g.travel(ChapterFive.GARDEN, 10, 1); g.dir = ChapterEightTest.UP; ChapterEightTest.step(g, ChapterEightTest.UP); g.checkWarp();
        check(g.map.name.equals(ChapterNine.FOREST), "Both passage tiles warp");
        g.travel(ChapterNine.FOREST, 11, 18); g.dir = ChapterEightTest.DOWN; ChapterEightTest.step(g, ChapterEightTest.DOWN); g.checkWarp();
        check(g.map.name.equals(ChapterFive.GARDEN), "Way back to the garden");

        // lanternas sem presentes
        Arrays.fill(g.gardening.stock, 0); Arrays.fill(g.cooking.dishes, 0); Arrays.fill(g.life.catches, 0);
        meet(g, ChapterNine.FOREST, "lanterna_antiga0"); check(g.chapterNine.count() == 0, "Flower lantern needs a flower"); finish(g);
        meet(g, ChapterNine.FOREST, "lanterna_antiga1"); check(g.chapterNine.count() == 0, "Fish lantern needs a fish"); finish(g);
        meet(g, ChapterNine.FOREST, "lanterna_antiga2"); check(g.chapterNine.count() == 0, "Dish lantern needs a dish"); finish(g);

        // flor silvestre, peixe, sopa da Mamãe
        meet(g, ChapterNine.FOREST, "flor_antiga"); check(g.gardening.stock[0] == 1, "Wild flower picked"); finish(g);
        check(!g.vis(npc(g, ChapterNine.FOREST, "flor_antiga")), "Wild flower gone");
        meet(g, ChapterNine.FOREST, "lanterna_antiga0"); check(g.chapterNine.count() == 1 && g.gardening.stock[0] == 0, "Flower lantern lit"); finish(g);
        g.life.catches[2] = 1;
        meet(g, ChapterNine.FOREST, "lanterna_antiga1"); check(g.chapterNine.count() == 2 && g.bonds.stock(2) == 0 && g.life.catches[2] == 1, "Fish lantern lit, album kept"); finish(g);
        check(g.save(false) && g.loadGame() && g.chapterNine.count() == 2, "Lanterns persist"); g.state = Game.PLAY;
        meet(g, "Casa", "mae"); check(g.cooking.dishes[0] == 1, "Mamãe gives a soup"); finish(g);
        meet(g, ChapterNine.FOREST, "lanterna_antiga2"); check(g.chapterNine.count() == 3 && g.cooking.dishes[0] == 0, "Dish lantern lit"); finish(g);
        meet(g, ChapterNine.FOREST, "lanterna_antiga3"); check(g.state == Game.SONG, "Song lantern opens the melody");
        ChapterEightTest.playMelody(g, ChapterNine.SONG);
        check(g.stage == ChapterNine.TREE && g.chapterNine.count() == 4, "All lanterns lit"); finish(g);
        check(g.worldMap.destinations().contains(ChapterNine.FOREST), "Map points to the tree");

        day(g); meet(g, ChapterNine.FOREST, "grande_arvore"); check(g.stage == ChapterNine.TREE, "Tree sleeps by day"); finish(g);
        g.travel(ChapterNine.FOREST, 12, 8); g.dir = ChapterEightTest.UP; g.toastT = 0; g.bannerT = 0;
        VillageLifeTest.screenshot(g, "ancient-forest");
        night(g); meet(g, ChapterNine.FOREST, "grande_arvore"); check(g.stage == ChapterNine.HOME, "Great Tree lifts the eclipse"); finish(g);
        check(g.chapterNine.auroraSky(), "Aurora in the sky");
        g.travel("Vila Aurora", 12, 10); g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "aurora-sky");
        g.explorer.checkTimer = 0; g.update(); check(g.explorer.unlocked.contains("guardiao"), "Guardian achievement");

        meet(g, "Casa", "mae"); finish(g);
        check(g.stage == Story.FINAL && g.state == Game.CREDITS && g.credits.ending, "Chapter 9 ending rolls credits");
        g.credits.scroll = 380; VillageLifeTest.screenshot(g, "credits-ch9"); tap(g, KeyEvent.VK_Z);
        check(g.home.unlocked(25) && g.worldMap.destinations().isEmpty(), "Seed unlocked; campaign complete");
        check(Mailbox.LETTERS.stream().anyMatch(l -> l.id().equals("coruja_carta") && l.when().test(g)), "Owl letter");
        g.journal.open(Game.PLAY); check(g.journal.page == 8, "Journal chapter 9"); tap(g, KeyEvent.VK_ESCAPE);
        check(Explorer.chapterOf(ChapterNine.LANTERNS) == 9, "Chapter number");
        meet(g, ChapterNine.FOREST, "coruja"); check(g.dlg != null, "Owl after the ending"); finish(g);
        check(g.save(false) && g.loadGame() && g.chapterNine.count() == 4, "Finished save keeps lanterns lit");
    }

    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        chapterNine(g);
        System.out.println("PASS: " + VillageLifeTest.checks + " chapter 9 checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
