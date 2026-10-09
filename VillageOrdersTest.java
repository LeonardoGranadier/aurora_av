import java.awt.Point;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class VillageOrdersTest {
    static void check(boolean value, String message) { VillageLifeTest.check(value, message); }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY; VillageOrders o = g.orders;
        g.map = g.maps.get("Vila Aurora"); g.tx = 6; g.ty = 7;
        check(VillageLifeTest.reachable(g, 6, 7).contains(new Point(11, 15)), "Board accessible");
        for (int[] stop : Festival.PLAZA) check(stop[0] != 11 || stop[1] != 14, "Board leaves festival stops free");
        check(o.refresh() && o.orders.isEmpty(), "No inaccessible requests without tools");
        g.life.rod = true; check(o.refresh() && o.orders.isEmpty(), "Today's selection stays fixed");
        FestivalTest.date(g, 2, 10); check(o.refresh() && o.orders.size() == 3, "Three early-game fish requests");
        for (VillageOrders.Order order : o.orders) check(order.item() < 8 && VillageLife.FISH[order.item()].stage() == 0, "No locked fish");
        String selection = o.orders.toString();
        check(g.loadGame() && selection.equals(o.orders.toString()), "Reload preserves selection"); g.state = Game.PLAY;
        check(!o.deliver(0), "Insufficient stock refused");
        VillageOrders.Order first = o.orders.get(0); g.life.catches[first.item()] = first.count() + 2;
        int coins = g.home.coins, dayGift = g.bonds.gifted[first.who()];
        check(o.deliver(0) && o.total == 1 && g.home.coins == coins + first.reward(), "Delivery rewards once");
        check(o.stock(first.item()) == 2 && g.life.catches[first.item()] == first.count() + 2, "Consumes shared stock but preserves album");
        check(g.bonds.friendship[first.who()] == 5 && g.bonds.gifted[first.who()] == dayGift, "Reward independent of gifts");
        check(!o.deliver(0), "Duplicate refused");
        check(g.loadGame() && o.done == 1 && !o.deliver(0), "Completed status survives reload");
        FestivalTest.date(g, 3, 10); check(!o.deliver(1), "Expired request cannot deliver");
        int friendship = Arrays.stream(g.bonds.friendship).sum();
        g.life.rod = false; g.gardening.stock[1] = 2;
        check(o.refresh() && o.orders.size() == 1 && o.orders.get(0).item() == 9, "Only harvested flower requested");
        check(Arrays.stream(g.bonds.friendship).sum() == friendship && o.total == 1, "Expiration has no penalty");
        check(o.deliver(0) && g.gardening.stock[1] == 1, "Flower delivery consumes basket");
        FestivalTest.date(g, 4, 10); check(o.refresh(), "Next day's requests");
        g.home.coins = HomeDecor.MAX_COINS; check(!o.deliver(0), "No coin overflow"); g.home.coins = 0;
        check(g.save(false), "Save before failure");
        Files.delete(Path.of(Game.SAVE)); Files.createDirectory(Path.of(Game.SAVE)); Files.createFile(Path.of(Game.SAVE, "block"));
        int total = o.total, stock = g.gardening.stock[1]; friendship = Arrays.stream(g.bonds.friendship).sum();
        check(!o.deliver(0) && o.total == total && o.done == 0 && g.gardening.stock[1] == stock && g.home.coins == 0
            && Arrays.stream(g.bonds.friendship).sum() == friendship, "Failed delivery rolls back every reward and item");
        FestivalTest.date(g, 5, 10); int oldDay = o.day;
        check(!o.refresh() && o.day == oldDay, "Failed daily save restores old selection");
        Files.delete(Path.of(Game.SAVE, "block")); Files.delete(Path.of(Game.SAVE));
        g.life.rod = true; g.stage = 28;
        for (int d = 6; o.total < 10; d++) {
            FestivalTest.date(g, d, 10); check(o.refresh(), "Generate reward progression");
            for (int i = 0; i < o.orders.size() && o.total < 10; i++) {
                VillageOrders.Order order = o.orders.get(i);
                if (order.item() < 8) g.life.catches[order.item()] += order.count();
                else g.gardening.stock[order.item() - 8] += order.count();
                check(o.deliver(i), "Reward progression delivery");
            }
        }
        check(o.total == 10 && g.home.unlocked(16), "Ten deliveries unlock decoration");
        g.map = g.maps.get("Casa"); g.tx = 5; g.ty = 5;
        check(g.home.place(16, 7, 4, 0), "Decoration placeable");
        check(g.loadGame() && g.home.placed(16) && o.total == 10, "Decoration survives reload ordering");
        g.state = Game.PLAY; g.map = g.maps.get("Vila Aurora"); g.tx = 11; g.ty = 15; g.dir = 1;
        g.interact(); check(g.state == Game.ORDERS, "Board opens through Z interaction");
        double time = g.time; g.update(); check(time == g.time, "Board pauses time");
        g.toastT = 0; VillageLifeTest.screenshot(g, "orders-board");
        VillageLifeTest.tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Escape closes board");
        g.map = g.maps.get("Casa"); g.tx = 5; g.ty = 5; g.home.openDecor(Game.PLAY); g.home.selected = 16;
        VillageLifeTest.screenshot(g, "orders-decoration");
        o.load(new Properties()); check(o.total == 0 && o.orders.isEmpty() && !g.home.unlocked(16), "Old saves start clean");
        g.newGame(); check(o.total == 0 && o.day == -1, "New game resets requests");
        System.out.println("PASS: " + VillageLifeTest.checks + " village order checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
