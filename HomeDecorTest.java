import java.awt.Point;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Testes de economia, colocacao e migracao. Execute em pasta temporaria vazia. */
public class HomeDecorTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static Npc npc(Game g, String map, String id) {
        return g.maps.get(map).npcs.stream().filter(n -> n.id.equals(id)).findFirst().orElseThrow();
    }
    static void run() throws Exception {
        Game g = new Game(); HomeDecor home = g.home; g.state = Game.PLAY;
        check(home.coins == 0 && !home.buy(0), "No starting money");
        g.life.catches[0] = 12;
        check(g.bonds.give(1, 0), "Gift consumes a fish"); g.dlg = null;
        check(home.sell(0, 3) && home.coins == 15, "Sell exact quantity for coins");
        check(g.bonds.stock(0) == 8 && g.life.catches[0] == 12, "Shared stock preserves album");
        check(!home.sell(0, 9) && !home.sell(0, -1) && !home.sell(0, 0), "Reject invalid quantities");
        check(home.buy(0) && home.coins == 5 && home.owned[0], "Purchase pays correct price");
        check(!home.buy(0) && home.coins == 5, "No duplicate purchase");
        check(!home.buy(7), "Insufficient funds");
        home.coins = HomeDecor.MAX_COINS; check(!home.sell(0, 1), "Coin overflow rejected"); home.coins = 1000;
        for (int i = 1; i < 8; i++) check(home.buy(i), "All eight furniture types purchasable");
        g.map = g.maps.get("Casa"); g.tx = 5; g.ty = 5;
        check(!home.canPlace(1, 5, 7, 0), "Door protected");
        check(!home.canPlace(1, 5, 6, 0), "Arrival protected");
        check(!home.canPlace(1, 5, 5, 0), "Spawn protected");
        check(!home.canPlace(1, 3, 3, 0) && !home.canPlace(1, 1, 1, 0), "Mother and bed protected");
        check(!home.canPlace(2, 8, 5, 0) && !home.canPlace(0, Integer.MAX_VALUE, 2, 0), "Bounds protected");
        check(home.place(5, 5, 4, 0) && g.free(5, 4), "Rug is walkable");
        check(home.place(2, 2, 5, 1), "Rotate a two-tile table vertically");
        check(home.width(2, 1) == 1 && home.height(2, 1) == 2 && !g.free(2, 6), "Rotated footprint collides");
        check(!home.canPlace(3, 2, 6, 0), "Furniture overlap rejected");
        check(home.place(1, 2, 1, 0), "One bedside tile can be used");
        check(!home.canPlace(0, 1, 2, 0), "Cannot seal bed access");
        check(home.place(1, 4, 2, 0) && g.free(2, 1), "Moving frees old tile");
        g.tx = 4; g.ty = 4;
        check(!home.canPlace(6, 4, 4, 0), "Cannot cover player");
        g.tx = 5; g.ty = 5;
        check(home.place(0, 6, 4, 0), "Furniture may sit on rug");
        check(home.remove(0) && home.owned[0] && g.free(6, 4), "Storage preserves ownership");
        for (int i = 8; i < HomeDecor.ITEMS.length; i++) check(!home.unlocked(i), "Rewards locked before achievements");
        Arrays.fill(g.life.catches, 3); g.life.catches[0] = 12; g.life.bellBest = 5;
        Arrays.fill(g.bonds.quests, 2); g.festival.prizes = 1;
        for (int i = 8; i <= HomeDecor.STAR; i++) check(home.unlocked(i), "Earned decorations unlocked");
        check(!home.unlocked(15), "Chapter five flower remains locked");
        home.placeFestivalStar(); check(home.placed(HomeDecor.STAR), "Festival star initially displayed");
        check(home.place(HomeDecor.STAR, 8, 5, 0), "Festival star movable");
        check(npc(g, "Casa", "festival_souvenir").x == 8, "Festival NPC follows placement");
        check(home.remove(HomeDecor.STAR) && !g.vis(npc(g, "Casa", "festival_souvenir")), "Stored star hidden");
        check(home.place(8, 7, 4, 0), "Fishing trophy displayed");
        check(home.place(9, 8, 4, 0), "Bell medal displayed");
        check(home.place(10, 2, 2, 0), "Friendship keepsake displayed");
        check(home.layoutAccessible(), "Furnished house remains connected");
        check(g.save(false), "Save furnished house"); String signature = home.signature();
        g.newGame(); check(g.loadGame() && home.signature().equals(signature), "Economy and layout round trip");
        check(!g.vis(npc(g, "Casa", "festival_souvenir")), "Stored star remains stored after loading");
        check(g.bonds.stock(0) == 8, "Sold and gifted stock remains spent after loading");
        g.state = Game.PLAY; g.dlg = null;
        VillageLifeTest.tap(g, KeyEvent.VK_H); check(g.state == Game.HOME && home.mode == HomeDecor.DECOR, "Decoration shortcut");
        home.selected = 0; VillageLifeTest.tap(g, KeyEvent.VK_Z); check(home.positioning, "Begin placement");
        VillageLifeTest.tap(g, KeyEvent.VK_R); check(home.rotation == 1, "Rotate preview");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(!home.positioning && g.state == Game.HOME, "Cancel preview only");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Exit editor");
        g.openPause(); g.choose("Decorar casa"); VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.MENU, "Editor returns to pause");
        g.map = g.maps.get("Vila Aurora"); g.state = Game.PLAY;
        home.openDecor(Game.PLAY); check(g.state == Game.PLAY, "No editing outside house");
        Set<Point> reachable = VillageLifeTest.reachable(g, 6, 7);
        for (String id : new String[]{"rosa_shop", "beto_market"}) {
            Npc n = npc(g, "Vila Aurora", id); boolean access = false;
            for (int d = 0; d < 4; d++) access |= reachable.contains(new Point(n.x + Game.DX[d], n.y + Game.DY[d]));
            check(access, "Shop accessible: " + id);
        }
        g.time = 22 * 60; home.talk(npc(g, "Vila Aurora", "rosa_shop")); check(g.state == Game.PLAY && g.dlg != null, "Night closure");
        g.dlg = null; g.time = 8 * 60; home.talk(npc(g, "Vila Aurora", "rosa_shop"));
        check(g.state == Game.HOME && home.mode == HomeDecor.SHOP, "Day shop opens");
        g.toastT = 0;
        VillageLifeTest.screenshot(g, "home-shop");
        home.talk(npc(g, "Vila Aurora", "beto_market")); home.amount = 2;
        VillageLifeTest.screenshot(g, "home-sell");
        g.map = g.maps.get("Casa"); g.tx = 5; g.ty = 5; g.state = Game.PLAY;
        home.place(3, 6, 5, 0); home.place(6, 8, 3, 0);
        VillageLifeTest.screenshot(g, "home-furnished");
        home.openDecor(Game.PLAY); home.selected = 8; VillageLifeTest.screenshot(g, "home-editor");
        home.selected = 2; home.positioning = true; home.cursorX = 5; home.cursorY = 6; home.rotation = 0;
        VillageLifeTest.screenshot(g, "home-invalid-placement");
        home.positioning = false;
        Files.delete(Path.of(Game.SAVE)); Files.createDirectory(Path.of(Game.SAVE)); Files.createFile(Path.of(Game.SAVE, "block"));
        int coins = home.coins, sold = home.sold[0];
        check(!home.sell(0, 1) && home.coins == coins && home.sold[0] == sold, "Failed save rolls back sale");
        home.owned[7] = false;
        check(!home.buy(7) && !home.owned[7] && home.coins == coins, "Failed save rolls back purchase");
        int oldX = home.xs[8], oldY = home.ys[8];
        check(!home.remove(8) && home.xs[8] == oldX && home.ys[8] == oldY, "Failed save rolls back removal");
        Files.delete(Path.of(Game.SAVE, "block")); Files.delete(Path.of(Game.SAVE));
        g.save(false); Properties old = g.readSave(); old.keySet().removeIf(k -> k.toString().startsWith("casa."));
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { old.store(out, "Previous version"); }
        check(g.loadGame() && home.coins == 0 && !home.owned[0], "Old save has default economy");
        check(home.placed(HomeDecor.STAR), "Old festival keepsake migrates");
        Properties corrupt = new Properties(); corrupt.setProperty("casa.comprado.mesa", "true");
        corrupt.setProperty("casa.movel.mesa.x", "5"); corrupt.setProperty("casa.movel.mesa.y", "6"); corrupt.setProperty("casa.movel.mesa.r", "0");
        corrupt.setProperty("casa.moedas", "NaN"); home.load(corrupt);
        check(home.coins == 0 && !home.placed(2) && home.owned[2], "Invalid layout returns furniture to storage");
        g.newGame(); check(home.coins == 0 && home.sold[0] == 0 && !home.owned[0], "New game clears home progress");
        g.state = Game.PLAY; Arrays.fill(home.owned, true);
        check(home.place(6, 8, 6, 0) && home.place(1, 7, 6, 0) && home.place(2, 8, 4, 1), "Furniture fills a corner without leaving trapped floor");
        String corner = home.signature();
        check(!home.remove(6), "Removing enclosed corner item preserves accessible layout");
        check(g.save(false) && g.loadGame() && corner.equals(home.signature()), "Reload validates complete layout independent of catalog order");
        System.out.println("PASS: " + VillageLifeTest.checks + " home checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> {
            try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
        });
        System.exit(0);
    }
}
