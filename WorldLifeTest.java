import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Ambient life: deterministic visuals, reduced motion, resident chatter. Run in an empty temporary directory with the assets link. */
public class WorldLifeTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void at(Game g, int shownDay, int hour) { g.time = (shownDay - 1) * 1440 + hour * 60; }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        g.travel("Vila Aurora", 15, 15); at(g, 3, 10); g.frame = 20;
        g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "life-spring-day");
        at(g, 30, 22); g.frame = 400; VillageLifeTest.screenshot(g, "life-summer-night");
        at(g, 60, 10); g.frame = 900; VillageLifeTest.screenshot(g, "life-autumn");
        g.travel("Praia Azul", 14, 8); at(g, 3, 10); g.frame = 120; g.toastT = 0; VillageLifeTest.screenshot(g, "life-beach");
        check(g.worldLife.waterTiles().length > 0, "Beach has water for fish");
        g.travel("Vila Aurora", 15, 15); check(g.worldLife.waterTiles().length > 0, "Village lake has water for fish");

        // conversa entre moradores próximos
        at(g, 3, 10); g.dlg = null;
        Npc beto = g.life.resident("beto"), lucas = g.life.resident("lucas");
        for (VillageLife.Resident r : VillageLife.RESIDENTS) { Npc n = g.life.resident(r.id()); n.walking = false; n.sleeping = false; n.x = 1; n.y = 1; }
        beto.x = 14; beto.y = 13; lucas.x = 16; lucas.y = 13; g.tx = 15; g.ty = 15;
        g.worldLife.chatCooldown = 0; g.worldLife.update(0.1);
        check(g.worldLife.chatNpc != null && g.worldLife.chatText != null, "Neighbours start a chat");
        String first = g.worldLife.chatText; Npc speaker = g.worldLife.chatNpc;
        g.toastT = 0; VillageLifeTest.screenshot(g, "life-chat");
        g.worldLife.update(3.1);
        check(g.worldLife.chatNpc != speaker && !g.worldLife.chatText.equals(first), "The other one replies");
        g.worldLife.update(3.1); check(g.worldLife.chatNpc == null && g.worldLife.chatCooldown > 0, "Chat ends with a pause");
        for (WorldLife.Chat c : WorldLife.CHATS) {
            Lang.set("en"); check(!Lang.t(c.first()).equals(c.first()) && !Lang.t(c.reply()).equals(c.reply()), "Chat translated: " + c.first());
        }
        Lang.set("pt");
        beto.x = 1; lucas.x = 3; g.worldLife.chatCooldown = 0; g.worldLife.update(0.1);
        check(g.worldLife.chatNpc == null, "Residents far from the player stay quiet");

        g.access.reduced = true; at(g, 3, 10); g.frame = 0;
        VillageLifeTest.screenshot(g, "life-reduced");
        System.out.println("PASS: " + VillageLifeTest.checks + " world life checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
