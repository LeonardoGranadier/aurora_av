import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Execute em pasta temporaria: cria saves e imagens de teste no diretorio atual. */
public class VillageLifeTest {
    static int checks;
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    static void tap(Game g, int key) {
        KeyEvent e = new KeyEvent(g, KeyEvent.KEY_PRESSED, 0, 0, key, KeyEvent.CHAR_UNDEFINED);
        g.keyPressed(e); g.keyReleased(e);
    }
    static void screenshot(Game g, String name) throws Exception {
        g.setSize(Game.W, Game.H); g.fade = 0;
        BufferedImage image = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); g.paint(graphics); graphics.dispose();
        ImageIO.write(image, "png", Path.of(name + ".png").toFile());
    }
    static void routines(Game g) {
        g.newGame(); g.state = Game.PLAY;
        for (int hour : new int[]{8, 13, 19, 22, 8}) {
            g.time = hour * 60;
            for (int tick = 0; tick < 2500; tick++) {
                g.life.updateResidents(0.05);
                Set<Point> occupied = new HashSet<>();
                for (VillageLife.Resident r : VillageLife.RESIDENTS) {
                    Game.Npc n = g.life.resident(r.id());
                    if (n.sleeping) continue;
                    check(!g.maps.get("Vila Aurora").solid(n.x, n.y), "Resident walkable tile");
                    check(occupied.add(new Point(n.x, n.y)), "No resident overlap");
                    if (n.walking) check(occupied.add(new Point(n.toX, n.toY)), "No destination overlap");
                }
            }
            for (VillageLife.Resident r : VillageLife.RESIDENTS) {
                Game.Npc n = g.life.resident(r.id()); int[] dest = r.stops()[g.life.routineSlot()];
                check(n.x == dest[0] && n.y == dest[1], r.id() + " destination at " + hour);
                check(n.sleeping == (hour == 22 && r.goesHome()), r.id() + " home schedule");
            }
        }
        g.map = g.maps.get("Vila Aurora"); g.tx = 10; g.ty = 9;
        Game.Npc n = g.life.resident("prof");
        check(!g.life.walkable(g.map, n, 10, 9), "NPC avoids player");
        g.moving = true; g.nx = 11; g.ny = 9;
        check(!g.life.walkable(g.map, n, 11, 9), "NPC avoids player destination"); g.moving = false;
        n.walking = true; n.toX = 11; n.toY = 8;
        check(!g.free(11, 8), "Player avoids NPC destination"); n.walking = false;
    }
    static void fishing(Game g) {
        g.newGame(); g.state = Game.PLAY; g.map = g.maps.get("Vila Aurora"); g.tx = 19; g.ty = 15; g.dir = 3;
        g.interact(); check(g.state == Game.PLAY && !g.life.rod, "Rod required"); g.dlg = null;
        g.talk(g.life.resident("beto")); check(g.life.rod, "Beto gives rod"); g.dlg = null;
        for (int i = 0; i < VillageLife.FISH.length; i++) {
            VillageLife.Fish f = VillageLife.FISH[i];
            g.map = g.maps.get(f.map()); g.stage = f.stage(); g.time = f.period() == 1 ? 1320 : 600;
            check(g.life.availableFish().contains(i), "Species available: " + f.name());
            if (f.stage() > 0) {
                g.stage--; check(!g.life.availableFish().contains(i), "Story gate"); g.stage++;
            }
            if (f.period() >= 0) {
                g.time = f.period() == 1 ? 600 : 1320;
                check(!g.life.availableFish().contains(i), "Time gate");
                g.time = f.period() == 1 ? 1320 : 600;
            }
            g.life.cast(); g.life.fishIndex = i; g.life.fishTimer = 0; g.life.updateGame(0.02);
            check(g.life.fishPhase == VillageLife.BITE, "Bite window");
            tap(g, KeyEvent.VK_Z); check(g.life.fishPhase == VillageLife.REEL, "Hook");
            for (int tick = 0; tick < 2250 && g.life.fishPhase == VillageLife.REEL; tick++) {
                boolean hold = g.life.fishPosition + g.life.fishVelocity * 0.45 < g.life.fishTarget;
                if (hold) g.down.add(KeyEvent.VK_Z); else g.down.remove(KeyEvent.VK_Z);
                g.life.updateGame(0.02);
            }
            g.down.clear(); check(g.life.catches[i] == 1, "Catchable: " + f.name());
            tap(g, KeyEvent.VK_Z); check(g.state == Game.PLAY, "Exit result");
        }
        check(g.life.completeAlbum() && g.itemState("trofeu") == 1, "Album trophy");
        g.map = g.maps.get("Vila Aurora"); g.life.cast(); tap(g, KeyEvent.VK_Z);
        check(g.life.fishPhase == VillageLife.RESULT, "Early hook fails");
        g.life.cast(); g.life.fishTimer = 0; g.life.updateGame(0.02); g.life.updateGame(1.2);
        check(g.life.fishPhase == VillageLife.RESULT, "Missed bite fails");
        g.life.cast(); tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Cancel fishing");
        g.life.openAlbum(Game.MENU); tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.MENU, "Album returns to menu");
        g.life.openAlbum(Game.PLAY); tap(g, KeyEvent.VK_UP); check(g.life.albumSelection == 7, "Album wrap");
        g.state = Game.PLAY; g.stage = 21; g.map = g.maps.get("Praia Azul");
        g.talk(g.map.npcs.stream().filter(n -> n.id.equals("barco")).findFirst().orElseThrow());
        check(g.afterDlg != null, "Island revisit available");
        g.afterDlg.run(); g.afterDlg = null; g.dlg = null;
        check(g.map.name.equals("Ilha das Estrelas"), "Boat reaches island");
    }
    static void bells(Game g) {
        g.life.startBells(); g.life.answerBell(0); check(g.life.answer == 0, "Ignore input during playback");
        for (int round = 1; round <= VillageLife.BELL_ROUNDS; round++) {
            for (int tick = 0; tick < 100 && g.life.showing; tick++) g.life.updateBells(0.1);
            check(!g.life.showing, "Playback finishes");
            for (int note : new ArrayList<>(g.life.sequence)) g.life.answerBell(note);
            check(g.life.bellBest == round, "Record advances");
        }
        check(g.life.bellFinished && g.itemState("sinos") == 1, "Bell medal");
        g.life.startBells(); while (g.life.showing) g.life.updateBells(0.1);
        g.life.answerBell((g.life.sequence.get(0) + 1) % 4);
        check(g.life.bellFinished && g.life.bellBest == 5, "Mistake keeps record");
        tap(g, KeyEvent.VK_Z); check(!g.life.bellFinished && g.life.sequence.size() == 1, "Retry");
        tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Exit bells");
        Set<Integer> sounds = new HashSet<>();
        for (int i = 0; i < 4; i++) sounds.add(Arrays.hashCode(Audio.renderSfx("bell" + i)));
        check(sounds.size() == 4, "Distinct bell audio");
    }
    static void persistence(Game g) throws Exception {
        check(g.save(false), "Save"); String signature = g.life.signature();
        g.newGame(); check(!g.life.rod && g.life.discovered() == 0, "New game reset");
        check(g.loadGame() && signature.equals(g.life.signature()), "Round trip");
        Properties old = g.readSave(); old.keySet().removeIf(k -> k.toString().startsWith("vila."));
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { old.store(out, "Old save"); }
        check(g.loadGame(), "Old save loads");
        check(!g.life.rod && g.life.discovered() == 0 && g.life.bellBest == 0, "Old save defaults");
        g.life.rod = true;
        for (int state : new int[]{Game.FISHING, Game.ALBUM, Game.BELLS}) {
            g.state = state; check(g.saveBeforeClose(), "Close new screen");
            check("true".equals(g.readSave().getProperty("vila.vara")), "Close persists progress");
        }
        g.newGame(); g.state = Game.PLAY; g.stage = 20; g.time = 1320;
        g.map = g.maps.get("Praia Azul"); g.tx = 22; g.ty = 9; g.dir = 1;
        g.save(false); g.interact();
        while (g.dlg != null) tap(g, KeyEvent.VK_Z);
        check(g.stage == 21 && "21".equals(g.readSave().getProperty("fase")), "Final dialogue autosave");
        Files.delete(Path.of(Game.SAVE)); Files.createDirectory(Path.of(Game.SAVE));
        Files.createFile(Path.of(Game.SAVE, "block"));
        g.openPause(); g.choose("Salvar e ir ao título"); check(g.state == Game.MENU, "Save failure retains menu");
        for (int state : new int[]{Game.PLAY, Game.MENU, Game.INV, Game.FISHING, Game.ALBUM, Game.BELLS}) {
            g.state = state; check(!g.saveBeforeClose(), "Save failure prevents close");
        }
        Files.delete(Path.of(Game.SAVE, "block")); Files.delete(Path.of(Game.SAVE));
        g.openPause(); g.choose("Salvar e ir ao título"); check(g.state == Game.TITLE, "Successful title transition");
        byte[] saved = Files.readAllBytes(Path.of(Game.SAVE));
        g.newGame(); g.state = Game.TITLE;
        check(g.saveBeforeClose() && Arrays.equals(saved, Files.readAllBytes(Path.of(Game.SAVE))), "Title preserves save");
        g.map = g.maps.get("Vila Aurora"); g.tx = 19; g.ty = 13;
        g.life.load(new Properties());
        check(g.life.resident("beto").x != g.tx || g.life.resident("beto").y != g.ty, "Old save avoids new NPC overlap");
    }
    static Set<Point> reachable(Game g, int x, int y) {
        Set<Point> visited = new HashSet<>(); ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(x, y)); visited.add(new Point(x, y));
        while (!queue.isEmpty()) {
            Point point = queue.remove();
            for (int d = 0; d < 4; d++) {
                Point next = new Point(point.x + Game.DX[d], point.y + Game.DY[d]);
                if (g.free(next.x, next.y) && visited.add(next)) queue.add(next);
            }
        }
        return visited;
    }
    static void access(Game g) {
        g.newGame(); g.state = Game.PLAY;
        String[] maps = {"Vila Aurora", "Floresta Verde", "Praia Azul", "Ilha das Estrelas"};
        int[][] entrances = {{6, 7}, {1, 10}, {14, 1}, {4, 10}};
        for (int i = 0; i < maps.length; i++) {
            g.map = g.maps.get(maps[i]); g.stage = 14;
            Set<Point> reached = reachable(g, entrances[i][0], entrances[i][1]);
            boolean shore = reached.stream().anyMatch(p -> {
                for (int d = 0; d < 4; d++) if (g.map.at(p.x + Game.DX[d], p.y + Game.DY[d]) == '~') return true;
                return false;
            });
            check(shore, "Fishing shore accessible in " + maps[i]);
        }
        g.map = g.maps.get("Vila Aurora"); g.stage = 6; g.time = 1320;
        Set<Point> reached = reachable(g, 6, 7);
        for (String id : new String[]{"prof", "frag1", "beto", "rosa", "lucas", "sino"}) {
            Game.Npc n = g.map.npcs.stream().filter(npc -> npc.id.equals(id)).findFirst().orElseThrow();
            boolean adjacent = false;
            for (int d = 0; d < 4; d++) adjacent |= reached.contains(new Point(n.x + Game.DX[d], n.y + Game.DY[d]));
            check(adjacent, "Village interaction accessible: " + id);
        }
        check(reached.contains(new Point(29, 10)) && reached.contains(new Point(14, 21)), "Village exits accessible");
    }
    static void run() throws Exception {
        Game g = new Game(); access(g); routines(g); fishing(g); bells(g); persistence(g);
        g.newGame(); g.state = Game.PLAY; g.map = g.maps.get("Vila Aurora"); g.tx = 12; g.ty = 12;
        screenshot(g, "village");
        g.life.openAlbum(Game.PLAY); g.life.catches[1] = 3; g.life.albumSelection = 1; screenshot(g, "album");
        g.life.rod = true; g.life.cast(); screenshot(g, "fishing-wait");
        g.life.fishPhase = VillageLife.REEL; screenshot(g, "fishing-reel");
        g.life.startBells(); g.life.ring(2); screenshot(g, "bells");
        g.life.bellFinished = true; g.life.result = "Cinco melodias! Medalha dos Sinos conquistada!"; screenshot(g, "bells-result");
        g.state = Game.INV; g.life.rod = true; Arrays.fill(g.life.catches, 1); g.life.bellBest = 5; g.invSel = 10;
        screenshot(g, "inventory-trophy");
        System.out.println("PASS: " + checks + " checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
        });
        System.exit(0);
    }
}
