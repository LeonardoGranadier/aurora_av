import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Steam bridge: without steamworks4j it stays off; with a fake backend, achievements are sent once each. */
public class SteamTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }

    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        g.steam.start(); check(!g.steam.active(), "No steamworks4j on the class path: Steam stays off");
        g.steam.update(5); check(true, "Update without Steam is a no-op");
        check(Steam.apiName("festival_ceu").equals("ACH_FESTIVAL_CEU"), "API names");
        Set<String> names = new HashSet<>();
        for (Explorer.Achievement a : Explorer.ACHIEVEMENTS) check(names.add(Steam.apiName(a.id())) && Steam.apiName(a.id()).matches("ACH_[A-Z0-9_]+"), "Unique API name " + a.id());

        List<String> unlocked = new ArrayList<>(); int[] stores = {0}, callbacks = {0}, shutdowns = {0};
        g.steam.backend = new Steam.Backend() {
            public boolean unlock(String n) { unlocked.add(n); return true; }
            public void store() { stores[0]++; }
            public void runCallbacks() { callbacks[0]++; }
            public void shutdown() { shutdowns[0]++; }
        };
        g.newGame(); g.state = Game.PLAY; g.stage = 4; g.explorer.checkTimer = 0; g.update();
        check(g.explorer.unlocked.contains("farol"), "Achievement unlocked in game");
        g.steam.update(1.5);
        check(unlocked.equals(List.of("ACH_FAROL")) && stores[0] == 1 && callbacks[0] == 1, "Sent to Steam and stored");
        g.steam.update(1.5); check(unlocked.size() == 1 && stores[0] == 1, "Each achievement sent once");
        g.stage = 7; g.explorer.checkTimer = 0; g.update(); g.steam.update(1.5);
        check(unlocked.contains("ACH_MAPA") && stores[0] == 2, "New achievement sent");
        g.steam.backend = new Steam.Backend() {
            public boolean unlock(String n) { throw new IllegalStateException("steam crashed"); }
            public void store() {} public void runCallbacks() {} public void shutdown() { shutdowns[0]++; }
        };
        g.steam.sent.clear(); g.steam.update(1.5);
        check(!g.steam.active() && shutdowns[0] == 1, "Steam errors switch the bridge off instead of crashing");
        System.out.println("PASS: " + VillageLifeTest.checks + " steam checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
