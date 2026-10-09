import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

public class AccessibilityTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY; Accessibility a = g.access;
        check(a.translate(KeyEvent.VK_W) == KeyEvent.VK_UP, "Movement aliases normalized");
        check(!a.bind(4, KeyEvent.VK_ESCAPE) && !a.bind(4, KeyEvent.VK_I), "Reserved and duplicate keys rejected");
        check(a.bind(4, KeyEvent.VK_Q), "Remap action");
        check(a.translate(KeyEvent.VK_Z) == KeyEvent.VK_UNDEFINED && a.translate(KeyEvent.VK_Q) == KeyEvent.VK_Z, "Old action released");
        g.say("", "Uma conversa."); tap(g, KeyEvent.VK_Q); check(g.dlg == null, "Remapped action closes dialogue");
        g.keyPressed(new KeyEvent(g, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_Q, 'q'));
        check(g.down.contains(KeyEvent.VK_Z), "Remapped key supports holding for fishing");
        g.keyReleased(new KeyEvent(g, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_Q, 'q'));
        check(!g.down.contains(KeyEvent.VK_Z), "Release clears logical held key"); g.dlg = null;
        a.large = true; a.reduced = true; check(a.save(), "Preferences save");
        Accessibility loaded = new Accessibility(g); check(loaded.large && loaded.reduced && loaded.keys[4] == KeyEvent.VK_Q, "Preferences persist");
        g.say("Mamãe", "Uma história muito longa para conferir a leitura ampliada. ".repeat(8));
        int lines = a.dialogLines().size(); check(lines > 5, "Large text wraps");
        tap(g, KeyEvent.VK_ENTER); check(g.dlg != null && a.dialogPage == 1, "Long dialogue pages before advancing");
        g.toastT = 0; VillageLifeTest.screenshot(g, "access-dialogue");
        for (int i = 0; i < 30 && g.dlg != null; i++) tap(g, KeyEvent.VK_ENTER);
        check(g.dlg == null && a.dialogPage == 0, "Whole dialogue readable");
        long frame = g.frame = 123; VillageLifeTest.screenshot(g, "access-still"); check(g.frame == frame, "Reduced motion does not change simulation clock");
        g.stage = 28; g.journal.open(Game.PLAY); check(g.journal.maxScroll() > 0, "Large journal scrolls");
        VillageLifeTest.screenshot(g, "access-journal");
        g.openPause(); g.choose("Acessibilidade e controles"); check(g.state == Game.ACCESSIBILITY, "Panel opens");
        VillageLifeTest.screenshot(g, "access-settings");
        a.selected = 6; tap(g, KeyEvent.VK_ENTER); check(a.capturing, "Capture started");
        tap(g, KeyEvent.VK_ESCAPE); check(!a.capturing && g.state == Game.ACCESSIBILITY, "Escape cancels binding");
        tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.MENU, "Panel returns to pause");
        g.life.catches[0] = 2; g.bonds.openJournal(Game.PLAY); g.bonds.giftTarget = 0; g.bonds.selection = 0;
        tap(g, KeyEvent.VK_Q); check(g.state == Game.HANDOFF && g.bonds.stock(0) == 2, "Fish waits for confirmation");
        tap(g, KeyEvent.VK_ENTER); check(g.state == Game.BONDS && g.bonds.stock(0) == 2, "Default choice cancels");
        tap(g, KeyEvent.VK_Q); tap(g, KeyEvent.VK_RIGHT); tap(g, KeyEvent.VK_ENTER);
        check(g.bonds.stock(0) == 1, "Confirmed gift consumes exactly one"); g.dlg = null;
        g.gardening.stock[0] = 1; g.gardening.open(); g.gardening.target = 1;
        tap(g, KeyEvent.VK_Q); check(g.state == Game.HANDOFF, "Flower confirmation");
        VillageLifeTest.screenshot(g, "access-confirmation"); tap(g, KeyEvent.VK_ESCAPE);
        check(g.gardening.stock[0] == 1 && g.state == Game.GARDENING, "Cancelled flower retained");
        g.life.rod = true; g.orders.refresh(); g.state = Game.ORDERS;
        tap(g, KeyEvent.VK_Q); check(g.state == Game.HANDOFF, "Order confirmation"); tap(g, KeyEvent.VK_ESCAPE);
        check(g.orders.total == 0, "Cancelled order unchanged");
        check(g.save(false), "Existing save prepared"); g.openTitle(); g.choose("Novo jogo");
        check(g.state == Game.CONFIRM && g.menuSel == 0, "New game protects existing save"); tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.TITLE && Files.exists(Path.of(Game.SAVE)), "Cancelled new game retains save");
        g.newGame(); check(a.large && a.keys[4] == KeyEvent.VK_Q, "Preferences survive new game");
        System.out.println("PASS: " + VillageLifeTest.checks + " accessibility checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE)) || Files.exists(Accessibility.FILE)) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
