import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Chapter 8, optional puzzles, save slots and seasonal music. Run in an empty temporary directory with the assets link. */
public class ChapterEightTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    static Npc npc(Game g, String map, String id) { return ClassicFeaturesTest.npc(g, map, id); }
    static void meet(Game g, String map, String id) { NewChaptersTest.meet(g, map, id); }
    static void finish(Game g) { ClassicFeaturesTest.finishDialogue(g); }
    static void at(Game g, int shownDay, int hour) { g.time = (shownDay - 1) * 1440 + hour * 60; }
    /** Walks/pushes one tile in direction d the way the game does when a key is held. */
    static void step(Game g, int d) {
        g.dir = d;
        int ax = g.tx + Game.DX[d], ay = g.ty + Game.DY[d];
        check(g.free(ax, ay) || g.pushAhead(ax, ay, d), "Step " + d + " from " + g.tx + "," + g.ty);
        g.tx = ax; g.ty = ay;
        for (Feature f : g.features) f.stepped(ax, ay);
    }
    static void walk(Game g, int d, int n) { for (int i = 0; i < n; i++) step(g, d); }
    static final int DOWN = 0, UP = 1, LEFT = 2, RIGHT = 3;
    static void playMelody(Game g, int[] notes) {
        int[] keys = {KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3, KeyEvent.VK_4};
        for (int n : notes) tap(g, keys[n]);
    }

    static void chapterEight(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = ChapterEight.START;
        at(g, 3, 10);
        Npc lucas = g.life.resident("lucas"); lucas.sleeping = false; lucas.walking = false;
        meet(g, "Vila Aurora", "lucas"); check(g.stage == ChapterEight.BELLS, "Lucas starts chapter 8"); finish(g);
        check(!g.vis(npc(g, "Vila Aurora", "lia")), "Lia is not in the village yet");
        for (int day = 3; day <= 5; day++) {
            at(g, day, 10);
            meet(g, "Vila Aurora", "sino");
            check(g.state == Game.SONG, "Bell opens today's melody");
            if (day == 3) {
                tap(g, KeyEvent.VK_4); check(g.melody.progress == 0 || g.chapterEight.todaysMelody()[0] == 3, "Wrong note restarts");
                g.melody.progress = 0; VillageLifeTest.screenshot(g, "melody-day");
            }
            playMelody(g, g.chapterEight.todaysMelody());
            check(g.state == Game.PLAY && g.chapterEight.count() == day - 2, "Melody of day " + day);
            finish(g);
            if (day == 3) {
                meet(g, "Vila Aurora", "sino"); check(g.state == Game.PLAY && g.dlg != null && g.chapterEight.count() == 1, "Same weekday only once"); finish(g);
                check(g.save(false) && g.loadGame() && g.chapterEight.count() == 1, "Melodies persist"); g.state = Game.PLAY;
            }
        }
        check(g.stage == ChapterEight.PASSAGE && !g.vis(npc(g, "Gruta do Eco", "rocha_eco")), "Echo rock opened");
        g.travel("Gruta do Eco", 20, 2); g.dir = UP; step(g, UP); g.checkWarp();
        check(g.map.name.equals(ChapterEight.VALLEY) && g.stage == ChapterEight.BRIDGE, "Passage reaches the valley");
        check(g.worldMap.visited.contains(ChapterEight.VALLEY), "Valley discovered");

        // ponte: as três pedras sobre as placas
        GameMap v = g.map;
        check(v.solid(11, 6) && v.solid(12, 7), "Stream blocks the way before the bridge");
        g.tx = 4; g.ty = 13; walk(g, RIGHT, 2);
        Npc s0 = g.puzzles.stones[0][0];
        check(s0.x == 7 && s0.y == 13, "Boulder follows pushes");
        g.travel(ChapterEight.VALLEY, 11, 16); check(s0.x == 5 && s0.y == 13, "Leaving resets an unsolved puzzle");
        g.tx = 4; g.ty = 13; walk(g, RIGHT, 3); g.tx = 8; g.ty = 14; walk(g, UP, 3);
        check(s0.x == 8 && s0.y == 10, "First plate covered");
        g.tx = 12; g.ty = 15; walk(g, UP, 4);
        g.tx = 20; g.ty = 12; walk(g, LEFT, 3); g.tx = 16; g.ty = 13;
        g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "valley-puzzle");
        walk(g, UP, 2);
        check(g.puzzles.solved[Puzzles.BRIDGE] && g.stage == ChapterEight.LIA && v.at(11, 6) == 'P', "Bridge raised");
        finish(g);
        check(g.save(false) && g.loadGame() && g.maps.get(ChapterEight.VALLEY).at(12, 7) == 'P', "Bridge persists"); g.state = Game.PLAY;
        g.travel(ChapterEight.VALLEY, 12, 5); g.dir = UP; g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "valley-lia");
        meet(g, ChapterEight.VALLEY, "lia_vale"); check(g.stage == ChapterEight.RETURN, "Lia found"); finish(g);
        check(!g.vis(npc(g, ChapterEight.VALLEY, "lia_vale")) && g.vis(npc(g, "Vila Aurora", "lia")), "Lia goes home");
        lucas = g.life.resident("lucas"); lucas.sleeping = false; lucas.walking = false;
        meet(g, "Vila Aurora", "lucas"); finish(g);
        check(g.stage == ChapterEight.FINAL && g.state == Game.PLAY, "Chapter 8 ends without credits: chapter 9 follows");
        check(g.home.unlocked(24) && g.worldMap.destinations().contains("Pico Estrelado"), "Music box unlocked; Cintila calls next");
        meet(g, "Vila Aurora", "lia"); check(g.dlg != null && g.dlg[0].contains("melodia de hoje"), "Lia shares the day's melody"); finish(g);
        g.journal.open(Game.PLAY); g.journal.page = 7; check(g.journal.entries().stream().anyMatch(l -> l.startsWith("Melodias tocadas")), "Journal chapter 8");
        tap(g, KeyEvent.VK_ESCAPE);
        Properties old = g.readSave(); old.remove("creditos.etapa"); old.setProperty("creditos.final", "true");
        g.stage = Story.FINAL; g.credits.load(old); check(g.credits.seenAt == Story.FINAL, "Finished save keeps credits seen");
        old.setProperty("fase", "36"); g.stage = 36; g.credits.load(old); check(g.credits.seenAt == ChapterSeven.FINAL, "Previous version saw credits at chapter 7");
    }

    static void puzzles(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = Story.FINAL;
        GameMap v = g.maps.get(ChapterEight.VALLEY); g.puzzles.solve(Puzzles.BRIDGE); g.chapterEight.applyBridge();
        g.travel(ChapterEight.VALLEY, 5, 4);
        check(g.vis(npc(g, ChapterEight.VALLEY, "portao_ruina")), "Ruins gate closed");
        g.dir = LEFT; check(g.pushAhead(4, 4, LEFT) && g.puzzles.stones[1][0].x == 3, "Push into ruins");
        g.tx = 3; g.ty = 5; g.dir = UP;
        check(g.pushAhead(3, 4, UP), "Push up onto plate");
        g.tx = 7; g.ty = 4; g.dir = LEFT; check(g.pushAhead(6, 4, LEFT), "Second boulder left");
        g.tx = 5; g.ty = 5; g.dir = UP; check(g.pushAhead(5, 4, UP), "Second boulder up");
        check(g.puzzles.solved[Puzzles.RUINS] && !g.vis(npc(g, ChapterEight.VALLEY, "portao_ruina")), "Ruins gate opens");
        g.dlg = null; g.travel(ChapterEight.VALLEY, 2, 3); g.dir = UP; step(g, UP); g.interact();
        check(g.puzzles.opened[0] && g.home.unlocked(21), "Ruins chest opened"); finish(g);

        // reset stone
        g.puzzles.solved[Puzzles.RUINS] = false; g.puzzles.stones[1][0].x = 7; g.puzzles.stones[1][0].y = 2;
        g.talk(npc(g, ChapterEight.VALLEY, "eco_reset_1")); check(g.puzzles.stones[1][0].x == 4 && g.puzzles.stones[1][0].y == 4, "Echo stone resets boulders"); finish(g);
        g.puzzles.solve(Puzzles.RUINS);

        // pegadas da gruta
        g.stage = 15; g.travel("Gruta do Eco", 7, 3);
        for (int[] s : Puzzles.STARS) { g.tx = s[0]; g.ty = s[1]; for (Feature f : g.features) f.stepped(s[0], s[1]); }
        check(!g.puzzles.starsDone, "Stars need the lantern");
        g.stage = Story.FINAL; g.travel("Gruta do Eco", 7, 3);
        g.tx = 10; g.ty = 3; for (Feature f : g.features) f.stepped(10, 3); check(g.puzzles.starStep == 0, "Wrong first star");
        int[] order = {0, 1, 0, 1, 2, 3, 4};
        for (int i : order) { int[] s = Puzzles.STARS[i]; g.tx = s[0]; g.ty = s[1]; for (Feature f : g.features) f.stepped(s[0], s[1]); }
        check(g.puzzles.starsDone && g.vis(npc(g, "Gruta do Eco", "bau_gruta")), "Stars solved, chest appears");
        g.travel("Gruta do Eco", 9, 4); g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "cave-stars");
        meet(g, "Gruta do Eco", "bau_gruta"); check(g.puzzles.opened[1], "Cave chest"); finish(g);

        // baú congelado
        g.stage = 17; meet(g, "Trilha da Montanha", "bau_gelo"); check(!g.puzzles.opened[2], "Ice needs the crystal"); finish(g);
        g.stage = 19; meet(g, "Trilha da Montanha", "bau_gelo"); check(g.puzzles.opened[2], "Crystal melts the ice"); finish(g);
        g.explorer.checkTimer = 0; g.update(); check(g.explorer.unlocked.contains("enigmas"), "Puzzle master achievement");
        check(g.save(false) && g.loadGame() && g.puzzles.openedCount() == 3 && g.puzzles.starsDone && g.puzzles.solved[Puzzles.RUINS], "Puzzles persist");
        Properties legacy = new Properties(); g.puzzles.load(legacy); check(g.puzzles.openedCount() == 0 && !g.puzzles.solved[0], "Old saves start unsolved");
    }

    static void slots(Game g) throws Exception {
        for (String s : Game.SLOTS) { Files.deleteIfExists(Path.of(s)); Files.deleteIfExists(Path.of(s + ".bak")); }
        g.openTitle(); check(!g.opts.contains("Continuar") && !g.opts.contains("Carregar jogo"), "Empty title");
        g.choose("Novo jogo"); check(g.state == Game.NAMING && g.slot == 0, "First new game uses slot 1");
        for (char c : "Ana".toCharArray()) g.keyTyped(new KeyEvent(g, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, c));
        tap(g, KeyEvent.VK_ENTER); tap(g, KeyEvent.VK_Z);
        check(Files.exists(Path.of(Game.SLOTS[0])), "Slot 1 saved");
        Thread.sleep(20);
        g.openTitle(); g.choose("Novo jogo"); check(g.slot == 1, "Second new game uses slot 2, slot 1 kept");
        for (char c : "Bia".toCharArray()) g.keyTyped(new KeyEvent(g, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, c));
        tap(g, KeyEvent.VK_ENTER); tap(g, KeyEvent.VK_Z); g.stage = 9; check(g.save(false), "Slot 2 saved");
        check(Files.exists(Path.of(Game.SLOTS[1])) && Game.slotInfo(0).startsWith("Ana") && Game.slotInfo(1).startsWith("Bia"), "Two named slots");
        g.openTitle(); check(g.opts.contains("Carregar jogo") && g.saveInfo.startsWith("Bia"), "Continue shows the latest slot");
        g.choose("Carregar jogo"); check(g.state == Game.SLOTS_SCREEN, "Slot screen");
        VillageLifeTest.screenshot(g, "slots");
        g.saveSlots.selected = 0; tap(g, KeyEvent.VK_Z);
        check(g.state == Game.PLAY && g.slot == 0 && g.explorer.name.equals("Ana"), "Load slot 1");
        g.stage = 3; check(g.save(false) && Game.readSlot(1).getProperty("fase").equals("9"), "Saving slot 1 leaves slot 2 intact");
        g.openTitle(); g.choose("Novo jogo"); check(g.slot == 2, "Third slot");
        tap(g, KeyEvent.VK_ESCAPE);
        Files.writeString(Path.of(Game.SLOTS[2]), "versao=1\nfase=0\ntempo=0\nmapa=Casa\n");
        g.openTitle(); g.choose("Novo jogo"); check(g.state == Game.SLOTS_SCREEN && g.saveSlots.choosingNew, "All full: choose which to replace");
        g.saveSlots.selected = 2; tap(g, KeyEvent.VK_Z); check(g.state == Game.CONFIRM && g.slot == 2, "Replacing asks confirmation");
        g.choose("Sim, apagar e começar"); check(g.state == Game.NAMING && !Files.exists(Path.of(Game.SLOTS[2])) && Files.exists(Path.of(Game.SLOTS[0])), "Only the chosen slot is erased");
        tap(g, KeyEvent.VK_ESCAPE);
        g.choose("Carregar jogo"); g.saveSlots.selected = 1; tap(g, KeyEvent.VK_DELETE); tap(g, KeyEvent.VK_RIGHT); tap(g, KeyEvent.VK_Z);
        check(!Files.exists(Path.of(Game.SLOTS[1])) && Files.exists(Path.of(Game.SLOTS[0])), "Delete erases one slot");
    }

    static void music(Game g) {
        g.newGame(); g.state = Game.PLAY; g.travel("Vila Aurora", 6, 9);
        String[] expected = {"vila_dia", "vila_verao", "vila_outono", "vila_inverno"};
        for (int s = 0; s < 4; s++) { at(g, s * 28 + 3, 10); check(g.musicFor().equals(expected[s]) && Audio.THEMES.containsKey(expected[s]), "Season music " + s); }
        at(g, 3, 22); check(g.musicFor().equals("vila_noite"), "Night music unchanged");
        g.travel(ChapterEight.VALLEY, 11, 16); check(g.musicFor().equals("vale"), "Valley music");
    }

    static void hints(Game g) throws Exception {
        check(Hints.HINTS.length == Story.OBJECTIVES.length, "One hint set per story stage");
        for (String[] h : Hints.HINTS) check(h.length == 3 && Arrays.stream(h).allMatch(x -> !x.isBlank()), "Three hint layers");
        g.newGame(); g.state = Game.PLAY; g.stage = 6;
        g.journal.open(Game.PLAY); tap(g, KeyEvent.VK_Z);
        check(g.state == Game.PLAY && g.dlg != null && g.dlg.length == 2 && g.dlg[0].equals(Hints.HINTS[6][0]), "Journal gives the vague hint first");
        g.dlg = null; g.hints.ask(); check(g.dlg.length == 3 && g.dlg[1].equals(Hints.HINTS[6][1]), "Second request adds where to go"); g.dlg = null;
        g.hints.ask(); g.hints.ask(); check(g.dlg.length == 3 && g.dlg[2].equals(Hints.HINTS[6][2]), "Third request explains; stays at three");
        VillageLifeTest.screenshot(g, "hint"); g.dlg = null;
        check(g.save(false) && g.loadGame() && g.hints.level == 3, "Hint level persists");
        g.state = Game.PLAY; g.stage = 7; g.hints.ask(); check(g.dlg.length == 2 && g.dlg[0].equals(Hints.HINTS[7][0]), "New stage restarts hints"); g.dlg = null;
        g.openPause(); g.choose("Pedir uma dica"); check(g.state == Game.PLAY && g.dlg != null, "Pause menu hint"); g.dlg = null;
    }

    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        chapterEight(g); puzzles(g); music(g); hints(g); slots(g);
        System.out.println("PASS: " + VillageLifeTest.checks + " chapter 8, puzzle and slot checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
