import java.awt.Point;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Testes de integracao. Executar em um diretorio temporario sem save existente. */
public class BondsTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void talk(Game g, String id) { g.talk(g.life.resident(id)); g.dlg = null; }
    static Npc object(Game g, String id) {
        return g.map.npcs.stream().filter(n -> n.id.equals(id)).findFirst().orElseThrow();
    }
    static void collect(Game g, String map, String id) {
        g.map = g.maps.get(map);
        Npc n = object(g, id);
        check(g.vis(n), "Quest object visible: " + id);
        Set<Point> reachable = VillageLifeTest.reachable(g, map.equals("Floresta Verde") ? 1 : 14,
            map.equals("Floresta Verde") ? 10 : 1);
        boolean interacted = false;
        for (int d = 0; d < 4; d++) {
            int x = n.x - Game.DX[d], y = n.y - Game.DY[d];
            if (!reachable.contains(new Point(x, y))) continue;
            g.tx = x; g.ty = y; g.dir = d; g.interact(); interacted = true; break;
        }
        check(interacted && g.taken.contains(id), "Collectible reachable: " + id);
        check(g.readSave().getProperty("coletados").contains(id), "Collectible autosaved"); g.dlg = null;
    }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        check(!g.bonds.visible("bond_flower0"), "Objects hidden before requests");
        for (String id : Bonds.IDS) talk(g, id);
        for (int i = 0; i < 4; i++) {
            check(g.bonds.quests[i] == 1 && g.bonds.friendship[i] == 5, "Conversation starts request");
            talk(g, Bonds.IDS[i]); check(g.bonds.friendship[i] == 5, "No repeat conversation farming");
        }
        g.time += 1440; talk(g, "rosa"); check(g.bonds.friendship[2] == 10, "Next day conversation");
        g.life.catches[0] = 2; talk(g, "beto"); check(g.bonds.quests[1] == 1, "Incomplete fishing request");
        g.life.catches[0] = 3;
        double before = g.life.halfZone(); talk(g, "beto");
        check(g.bonds.quests[1] == 2 && g.life.halfZone() > before, "Beto reward widens fishing zone");
        int points = g.bonds.friendship[1]; talk(g, "beto");
        check(g.bonds.friendship[1] == points, "No repeated quest reward");
        collect(g, "Floresta Verde", "bond_flower0");
        collect(g, "Floresta Verde", "bond_flower1");
        collect(g, "Floresta Verde", "bond_flower2"); talk(g, "rosa");
        check(g.bonds.quests[2] == 2, "Rosa request complete");
        collect(g, "Floresta Verde", "bond_whistle"); talk(g, "lucas");
        check(g.bonds.quests[3] == 2, "Lucas request complete");
        g.time = 600; collect(g, "Floresta Verde", "bond_tree");
        g.map = g.maps.get("Praia Azul"); check(!g.vis(object(g, "bond_light")), "Observation needs night");
        g.time = 1320; collect(g, "Praia Azul", "bond_light"); talk(g, "prof");
        check(g.bonds.quests[0] == 2, "Professor request complete");
        g.stage = 15; g.talk(g.life.resident("prof"));
        check(g.stage == 16 && g.inventory.state("lanterna") == 1, "Professor campaign preserved"); g.dlg = null;
        g.life.catches[4] = 2; int albumCount = g.life.discovered(); int lucas = g.bonds.friendship[3];
        check(g.bonds.give(3, 4), "Favorite gift accepted");
        check(g.bonds.friendship[3] == lucas + 15, "Favorite bonus");
        check(g.bonds.stock(4) == 1 && g.life.discovered() == albumCount && g.life.catches[4] == 2, "Gift consumes stock, preserves album");
        check(!g.bonds.give(3, 4) && g.bonds.stock(4) == 1, "One gift per day");
        check(!g.bonds.give(0, 7), "Cannot give missing fish");
        g.time += 1440; check(g.bonds.give(3, 4) && g.bonds.stock(4) == 0, "Next day gift");
        g.dlg = null; g.bonds.friendship[0] = 80; g.talk(g.life.resident("prof"));
        check(g.bonds.stories[0] == 3, "Both personal stories unlock");
        check(Arrays.asList(g.dlg).contains(Bonds.STORIES[0][0]), "Unlocked story in dialogue");
        g.talk(g.life.resident("prof"));
        check(!Arrays.asList(g.dlg).contains(Bonds.STORIES[0][0]), "Story not repeated"); g.dlg = null;
        g.save(false); String signature = g.bonds.signature();
        g.newGame(); check(g.bonds.friendship[0] == 0 && g.bonds.quests[0] == 0, "New game resets bonds");
        check(g.loadGame() && g.bonds.signature().equals(signature), "Save round trip");
        check(g.maps.get("Floresta Verde").npcs.stream().noneMatch(n -> n.id.equals("bond_whistle")), "Collected objects remain removed");
        g.state = Game.PLAY; g.dlg = null;
        VillageLifeTest.tap(g, KeyEvent.VK_J); check(g.state == Game.BONDS, "Journal shortcut");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Journal closes to play");
        g.openPause(); g.choose("Laços de Aurora"); VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.MENU, "Journal closes to pause");
        g.map = g.maps.get("Vila Aurora"); Npc beto = g.life.resident("beto");
        g.tx = beto.x - 1; g.ty = beto.y; g.dir = 3; g.state = Game.PLAY;
        VillageLifeTest.tap(g, KeyEvent.VK_G); check(g.state == Game.BONDS && g.bonds.giftTarget == 1, "Gift only beside resident");
        check(g.saveBeforeClose(), "Close journal saves");
        VillageLifeTest.screenshot(g, "gifts");
        g.bonds.openJournal(Game.PLAY);
        for (int i = 0; i < 4; i++) { g.bonds.selection = i; VillageLifeTest.screenshot(g, "bonds-" + i); }
        g.openPause(); VillageLifeTest.screenshot(g, "pause-bonds");
        Properties old = g.readSave(); old.keySet().removeIf(k -> k.toString().startsWith("lacos."));
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { old.store(out, "Previous version"); }
        check(g.loadGame() && g.bonds.friendship[0] == 0 && g.bonds.quests[1] == 0, "Previous save defaults");
        System.out.println("PASS: " + VillageLifeTest.checks + " bond checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
        });
        System.exit(0);
    }
}
