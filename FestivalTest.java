import java.awt.Point;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Execute em pasta temporaria vazia, como os demais testes. */
public class FestivalTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void date(Game g, int day, double hour) { g.time = (day - 1) * 1440 + hour * 60; }
    static void finishDialog(Game g) { while (g.dlg != null) VillageLifeTest.tap(g, KeyEvent.VK_Z); }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        Festival f = g.festival;
        for (int day : new int[]{1, 6, 8, 13, 15}) {
            date(g, day, 12); check(!f.active(), "Non-festival day");
            check(f.nextDay() == day + 7 - day % 7, "Next festival date");
        }
        for (int day : new int[]{7, 14, 21}) {
            date(g, day, 7.99); check(!f.active() && f.nextDay() == day, "Before opening");
            date(g, day, 8); check(f.active(), "Opening inclusive");
            date(g, day, 19.99); check(f.active(), "Before closing");
            date(g, day, 20); check(!f.active() && f.nextDay() == day + 7, "Closing exclusive");
        }
        date(g, 7, 10); g.map = g.maps.get("Vila Aurora"); g.tx = 14; g.ty = 16;
        for (int tick = 0; tick < 1600; tick++) {
            g.life.updateResidents(0.05);
            Set<Point> used = new HashSet<>();
            for (VillageLife.Resident r : VillageLife.RESIDENTS) {
                Game.Npc npc = g.life.resident(r.id());
                check(used.add(new Point(npc.x, npc.y)), "Festival NPC origin reservation");
                if (npc.walking) check(used.add(new Point(npc.toX, npc.toY)), "Festival NPC destination reservation");
            }
        }
        for (int i = 0; i < Bonds.IDS.length; i++) {
            Game.Npc npc = g.life.resident(Bonds.IDS[i]);
            check(npc.x == Festival.PLAZA[i][0] && npc.y == Festival.PLAZA[i][1], "Resident reaches festival plaza");
        }
        Set<Point> reachable = VillageLifeTest.reachable(g, 6, 7);
        check(reachable.contains(new Point(12, 9)), "Board accessible");
        check(reachable.contains(new Point(19, 15)), "Lake accessible");
        check(reachable.contains(new Point(29, 10)) && reachable.contains(new Point(14, 21)), "Exits accessible");
        g.bonds.friendship[2] = 0; g.talk(g.life.resident("rosa"));
        String stranger = g.dlg[g.dlg.length - 1];
        g.bonds.friendship[2] = 60; g.talk(g.life.resident("rosa"));
        check(!stranger.equals(g.dlg[g.dlg.length - 1]), "Friendship changes festival dialogue"); g.dlg = null;
        date(g, 8, 10);
        for (int tick = 0; tick < 1600; tick++) g.life.updateResidents(0.05);
        for (VillageLife.Resident r : VillageLife.RESIDENTS) {
            Game.Npc npc = g.life.resident(r.id());
            int[] destination = g.weather.destination(r, 0);
            check(npc.x == destination[0] && npc.y == destination[1], "Daily routine resumes after festival");
        }
        f.open(); f.key(KeyEvent.VK_Z); check(!f.fishing, "No entry outside festival");
        date(g, 7, 10); f.open(); f.key(KeyEvent.VK_Z); check(!f.fishing, "Rod required");
        g.life.rod = true; f.key(KeyEvent.VK_Z);
        check(g.dlg != null && !f.fishing, "Rules precede timer"); finishDialog(g);
        check(f.fishing && f.remaining == 90, "Contest begins after rules");
        for (int state : new int[]{Game.MENU, Game.INV, Game.ALBUM, Game.BONDS, Game.FESTIVAL}) {
            g.state = state; f.update(2); check(f.remaining == 90, "Menus pause timer");
        }
        g.state = Game.PLAY; g.say("", "Pausa"); f.update(2); check(f.remaining == 90, "Dialogue pauses timer"); g.dlg = null;
        g.state = Game.PLAY; f.update(1); check(f.remaining == 89, "Walking uses timer");
        double time = g.time; g.last = System.nanoTime() - 20_000_000; g.update();
        check(g.time == time, "World clock frozen during contest");
        VillageLifeTest.tap(g, KeyEvent.VK_T); check(g.time == time, "Debug time key disabled during contest");
        g.tx = 19; g.ty = 15; g.dir = 3; g.interact();
        check(g.state == Game.FISHING, "Actual lake starts fishing");
        g.life.fishTimer = 0; g.life.updateGame(0.02); VillageLifeTest.tap(g, KeyEvent.VK_Z);
        for (int tick = 0; tick < 2000 && g.life.fishPhase == VillageLife.REEL; tick++) {
            boolean hold = g.life.fishPosition + g.life.fishVelocity * 0.45 < g.life.fishTarget;
            if (hold) g.down.add(KeyEvent.VK_Z); else g.down.remove(KeyEvent.VK_Z);
            f.update(0.02); g.life.updateGame(0.02);
        }
        g.down.clear(); check(f.score == 1 && g.life.catches[0] == 1, "Real capture scores and preserves album");
        double previous = f.remaining; f.update(1); check(f.remaining < previous, "Results count against timer");
        VillageLifeTest.tap(g, KeyEvent.VK_Z);
        f.score = 3; f.remaining = 0.01; g.state = Game.FISHING; f.update(0.02);
        check(!f.fishing && g.state == Game.FESTIVAL && f.fishWinDay == 7, "Timeout records victory");
        check(f.prizes == 0, "Both contests required");
        check(f.startBells() && g.life.festivalBells && g.life.bellRounds() == 7, "Special bell challenge");
        for (int round = 1; round <= 7; round++) {
            while (g.life.showing) g.life.updateBells(0.1);
            for (int note : new ArrayList<>(g.life.sequence)) g.life.answerBell(note);
        }
        check(g.life.bellFinished && f.bellWinDay == 7 && f.prizes == 1, "Both wins award prize");
        check(g.life.bellBest == 0, "Ordinary bell record unchanged");
        Game.Npc souvenir = g.maps.get("Casa").npcs.stream().filter(n -> n.id.equals("festival_souvenir")).findFirst().orElseThrow();
        check(g.vis(souvenir), "Home souvenir visible");
        f.winBells(); f.award(); check(f.prizes == 1, "No duplicate prize");
        g.life.startBells(); check(!g.life.festivalBells && g.life.bellRounds() == 5, "Normal bell mode restored");
        f.open(); check(f.startFishing(), "Fishing retry available");
        f.remaining = 43.25; f.score = 2; check(g.save(false), "Save running contest");
        g.newGame(); check(g.loadGame(), "Load running contest"); g.state = Game.PLAY;
        check(f.fishing && f.score == 2 && f.remaining == 43.25 && f.prizes == 1, "Contest and prize resume");
        g.map = g.maps.get("Casa"); f.update(0.02);
        check(!f.fishing && f.prizes == 1, "Leaving village ends attempt");
        date(g, 14, 10); g.map = g.maps.get("Vila Aurora");
        f.winBells(); check(f.prizes == 1, "Different editions cannot combine wins");
        f.startFishing(); f.score = 2; f.finishFishing(true); check(f.fishWinDay == 7, "Failed attempt does not win");
        f.startFishing(); f.score = 3; f.finishFishing(true);
        check(f.prizes == 2 && f.prizeDay == 14, "Next edition awards again");
        g.state = Game.FESTIVAL; check(g.saveBeforeClose(), "Festival screen saves on close");
        String saved = f.signature(); g.newGame(); check(g.loadGame() && saved.equals(f.signature()), "Festival persistence");
        g.map = g.maps.get("Vila Aurora"); g.tx = 12; g.ty = 11; g.state = Game.PLAY;
        for (int tick = 0; tick < 1600; tick++) g.life.updateResidents(0.05);
        VillageLifeTest.screenshot(g, "festival-plaza");
        f.open(); VillageLifeTest.screenshot(g, "festival-board");
        f.startFishing(); f.remaining = 54; f.score = 2; g.life.cast();
        VillageLifeTest.screenshot(g, "festival-fishing");
        f.finishFishing(true); f.startBells(); g.life.ring(0); VillageLifeTest.screenshot(g, "festival-bells");
        g.map = g.maps.get("Casa"); g.tx = 5; g.ty = 5; g.state = Game.PLAY;
        VillageLifeTest.screenshot(g, "festival-home");
        Properties old = g.readSave(); old.keySet().removeIf(k -> k.toString().startsWith("festival."));
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { old.store(out, "Previous version"); }
        check(g.loadGame() && !f.fishing && f.prizes == 0, "Old saves default to no festival progress");
        Properties malformed = new Properties(); malformed.setProperty("festival.tempo", "NaN");
        malformed.setProperty("festival.pescando", "true"); f.load(malformed);
        check(!f.fishing && f.remaining == 0, "Invalid timer cannot resume");
        g.newGame(); check(f.prizes == 0 && f.fishWinDay == 0 && !f.fishing, "New game resets festival");
        System.out.println("PASS: " + VillageLifeTest.checks + " festival checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
        });
        System.exit(0);
    }
}
