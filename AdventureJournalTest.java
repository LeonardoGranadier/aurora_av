import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class AdventureJournalTest {
    static void check(boolean value, String message) { VillageLifeTest.check(value, message); }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        AdventureJournal j = g.journal;
        VillageLifeTest.tap(g, KeyEvent.VK_L); check(g.state == Game.JOURNAL, "Shortcut opens journal");
        check(j.latest() == 0 && !String.join(" ", j.entries()).contains("Cintila"), "No future spoilers");
        VillageLifeTest.tap(g, KeyEvent.VK_RIGHT); check(j.page == 0, "Future chapters locked");
        double time = g.time; g.update(); check(time == g.time, "Journal pauses time");
        VillageLifeTest.screenshot(g, "journal-start");
        VillageLifeTest.tap(g, KeyEvent.VK_L); check(g.state == Game.PLAY && g.down.isEmpty(), "Shortcut closes cleanly");
        g.say("", "Test"); VillageLifeTest.tap(g, KeyEvent.VK_L); check(g.state == Game.PLAY, "Dialogue blocks shortcut"); g.dlg = null;
        g.moving = true; VillageLifeTest.tap(g, KeyEvent.VK_L); check(g.state == Game.PLAY, "Movement blocks shortcut"); g.moving = false;
        g.stage = 25; g.map = g.maps.get(ChapterFive.GARDEN); g.tx = 6; g.ty = 13; g.dir = 1;
        check(j.clues.isEmpty(), "Stage alone never reveals inscriptions");
        g.interact(); check(j.clues.containsKey("garden_mural"), "Reading inscription records clue");
        check(g.readSave().containsKey("diario.garden_mural"), "Clue autosaved without stage change");
        g.dlg = null; check(g.loadGame() && j.clues.size() == 1, "Clue survives loading");
        g.state = Game.PLAY; j.open(Game.PLAY);
        check(j.page == 4 && String.join(" ", j.entries()).contains("No poente"), "Recorded clue visible");
        for (int i = 0; i < 100; i++) VillageLifeTest.tap(g, KeyEvent.VK_DOWN);
        check(j.scroll == j.maxScroll(), "Scroll clamps to content");
        VillageLifeTest.screenshot(g, "journal-clue");
        VillageLifeTest.tap(g, KeyEvent.VK_LEFT); check(j.page == 3 && j.scroll == 0, "Chapter navigation resets scroll");
        check(j.entries().contains("Concluído"), "Past chapter completed");
        VillageLifeTest.screenshot(g, "journal-history");
        check(g.saveBeforeClose(), "Closing in journal saves");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Escape returns to play");
        g.openPause(); g.choose("Diário de Aventuras"); VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.MENU, "Escape returns to menu");
        g.map = g.maps.get("Casa"); g.openPause(); g.toastT = 0; VillageLifeTest.screenshot(g, "journal-pause");
        g.stage = 28; j.page = 4; check(j.entries().contains("Flor-Constelação: entregue à Mamãe."), "Final delivery registered");
        j.load(new Properties()); check(j.clues.isEmpty() && j.entries().contains("Concluído"), "Legacy save restores progress without invented clues");
        g.stage = 23; g.talk(g.life.resident("prof"));
        check(!j.clues.containsKey("prof"), "Generic repeat dialogue is not a campaign clue"); g.dlg = null;
        g.stage = 22; g.talk(g.life.resident("prof"));
        check(j.clues.containsKey("prof"), "Story conversation recorded"); g.dlg = null;
        g.stage = 28; j.open(Game.PLAY);
        j.clues.put("garden_mural", "Anotação longa. ".repeat(100));
        for (int i = 0; i < 200; i++) VillageLifeTest.tap(g, KeyEvent.VK_DOWN);
        check(j.scroll > 0 && j.scroll == j.maxScroll(), "Long notes really scroll to final line");
        g.newGame(); check(j.clues.isEmpty() && j.latest() == 0, "New game resets journal");
        System.out.println("PASS: " + VillageLifeTest.checks + " adventure journal checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
