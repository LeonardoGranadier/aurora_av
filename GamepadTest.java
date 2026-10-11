import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Gamepad mapping with a simulated controller (no hardware needed). Run in an empty temporary directory with the assets link. */
public class GamepadTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static Gamepad.Pad pad(String... pressed) {
        Set<String> p = Set.of(pressed);
        return new Gamepad.Pad(true, p.contains("up"), p.contains("down"), p.contains("left"), p.contains("right"), p.contains("a"), p.contains("b"),
            p.contains("x"), p.contains("y"), p.contains("lb"), p.contains("rb"), p.contains("start"), p.contains("back"), p.contains("lt"), p.contains("rt"));
    }
    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        Gamepad gp = g.gamepad;
        check(!gp.available(), "No controller backend in tests");
        gp.poll();                                                     // sem fonte: nada acontece
        gp.apply(pad()); check(gp.connected && g.toastTxt.equals("Controle conectado."), "Connection announced");
        gp.apply(pad("up")); check(g.down.contains(KeyEvent.VK_UP), "D-pad up holds UP");
        gp.apply(pad("up")); check(g.down.contains(KeyEvent.VK_UP), "Held stays held");
        gp.apply(pad()); check(!g.down.contains(KeyEvent.VK_UP), "Release lets go");
        g.say("Mamãe", "Primeira fala.", "Segunda fala.");
        gp.apply(pad("a")); check(g.dlgIdx == 1, "A advances dialogue"); gp.apply(pad("a")); check(g.dlgIdx == 1, "Holding A does not repeat");
        gp.apply(pad()); gp.apply(pad("a")); check(g.dlg == null, "Second press closes dialogue"); gp.apply(pad());
        gp.apply(pad("start")); check(g.state == Game.MENU, "Start opens pause"); gp.apply(pad());
        gp.apply(pad("down")); gp.apply(pad()); check(g.menuSel == 1, "D-pad moves in menus");
        gp.apply(pad("b")); check(g.state == Game.PLAY, "B goes back"); gp.apply(pad());
        gp.apply(pad("x")); check(g.state == Game.INV, "X opens items"); gp.apply(pad()); gp.apply(pad("b")); gp.apply(pad());
        gp.apply(pad("y")); check(g.state == Game.CARD, "Y opens card"); gp.apply(pad()); gp.apply(pad("b")); gp.apply(pad());
        gp.apply(pad("back")); check(g.state == Game.WORLD_MAP, "Select opens map"); gp.apply(pad()); gp.apply(pad("b")); gp.apply(pad());
        gp.apply(pad("rb")); check(g.state == Game.JOURNAL, "RB opens journal"); gp.apply(pad()); gp.apply(pad("b")); gp.apply(pad());
        gp.apply(pad("lb")); check(g.state == Game.COOKING, "LB opens lunchbox"); gp.apply(pad()); gp.apply(pad("b")); gp.apply(pad());
        gp.apply(pad("rt")); check(g.running(), "RT runs"); gp.apply(pad()); check(!g.running(), "Release RT walks");

        // teclado e controle juntos: soltar o controle não solta a tecla ainda pressionada
        g.keyPressed(new KeyEvent(g, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        gp.apply(pad("right")); gp.apply(pad()); check(g.down.contains(KeyEvent.VK_RIGHT), "Keyboard hold survives controller release");
        g.keyReleased(new KeyEvent(g, KeyEvent.KEY_RELEASED, 0, 0, KeyEvent.VK_RIGHT, KeyEvent.CHAR_UNDEFINED));
        check(!g.down.contains(KeyEvent.VK_RIGHT), "Keyboard release");

        // remapeamento: o controle usa ações lógicas, então continua funcionando com teclas trocadas
        g.access.bind(4, KeyEvent.VK_Q); g.say("", "Teste."); gp.apply(pad("a")); check(g.dlg == null, "A works with remapped keyboard"); gp.apply(pad());

        // pesca: segurar A mantém a ação pressionada
        gp.apply(pad("a")); check(g.down.contains(KeyEvent.VK_Z), "Holding A holds the action for fishing"); g.dlg = null;
        Gamepad.Pad lost = new Gamepad.Pad(false, true, false, false, false, true, false, false, false, false, false, false, false, false, false);
        gp.apply(lost); check(!gp.connected && !g.down.contains(KeyEvent.VK_Z) && g.toastTxt.equals("Controle desconectado."), "Disconnect releases everything");

        Lang.set("en"); check(Lang.t("Controle conectado.").equals("Controller connected.") && Lang.t("Controle desconectado.").equals("Controller disconnected."), "Messages translated");
        Lang.set("pt");
        System.out.println("PASS: " + VillageLifeTest.checks + " gamepad checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
