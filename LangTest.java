import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/**
 * Percorre o jogo em inglês: conversa com todos os personagens e objetos de todos os mapas, em cada etapa da
 * história, de dia e de noite, e abre todas as telas. Falha se algum texto desenhado não tiver tradução.
 * Rode numa pasta temporária vazia com o atalho assets (sh testar.sh LangTest).
 */
public class LangTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static final BufferedImage CANVAS = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_RGB);
    static void paint(Game g) { g.setSize(Game.W, Game.H); Graphics2D gr = CANVAS.createGraphics(); g.paint(gr); gr.dispose(); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    /** Lê o diálogo inteiro (cada página passa pelo tradutor) e fecha telas abertas pela conversa. */
    static void drain(Game g) {
        for (int i = 0; i < 60 && (g.dlg != null || g.state != Game.PLAY); i++) {
            paint(g);
            if (g.dlg != null && g.state == Game.PLAY) tap(g, KeyEvent.VK_Z);
            else if (g.state == Game.HANDOFF) tap(g, KeyEvent.VK_ESCAPE);
            else if (g.state == Game.SONG) { g.state = Game.PLAY; }
            else tap(g, KeyEvent.VK_ESCAPE);
        }
        g.state = Game.PLAY; g.dlg = null;
    }
    static void screens(Game g) {
        int[] keys = {KeyEvent.VK_I, KeyEvent.VK_C, KeyEvent.VK_L, KeyEvent.VK_TAB, KeyEvent.VK_F, KeyEvent.VK_P, KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_V};
        for (int k : keys) {
            g.state = Game.PLAY; g.dlg = null; tap(g, k); paint(g);
            if (g.state == Game.JOURNAL) for (int p = 0; p <= g.journal.latest(); p++) { g.journal.page = p; paint(g); }
            if (g.state == Game.CARD) for (int a = 0; a < Explorer.ACHIEVEMENTS.size(); a++) { g.explorer.cardSel = a; paint(g); }
            if (g.state == Game.INV) for (int a = 0; a < Inventory.KEYS.length; a++) { g.inventory.sel = a; paint(g); }
            if (g.state == Game.COOKING) for (int a = 0; a < Cooking.RECIPES.length; a++) { g.cooking.selected = a; paint(g); g.cooking.kitchen = !g.cooking.kitchen; paint(g); }
            if (g.state == Game.BONDS) for (int a = 0; a < 4; a++) { g.bonds.selection = a; paint(g); }
            if (g.state == Game.ALBUM) for (int a = 0; a < 8; a++) { g.life.albumSelection = a; paint(g); }
            g.state = Game.PLAY;
        }
        g.openPause(); paint(g);
        for (String option : new ArrayList<>(g.opts)) {
            if (option.startsWith("Salvar e ir") || option.startsWith("Idioma")) continue;
            g.openPause(); g.choose(option); paint(g);
            if (g.state == Game.CALENDAR) for (int s = 0; s < 4; s++) { g.seasons.calendarSeason = s; paint(g); }
            if (g.state == Game.ACCESSIBILITY) for (int r = 0; r < Accessibility.LOGICAL.length + Accessibility.OPTIONS + 1; r++) { g.access.selected = r; paint(g); }
            drain(g);
        }
    }

    static void run() throws Exception {
        Game g = new Game();
        Lang.set("en"); g.registerNames();
        g.openTitle(); paint(g);
        for (int stage = 0; stage <= Story.FINAL; stage++) {
            for (int hour : new int[]{10, 22}) {
                g.newGame(); g.state = Game.PLAY; g.explorer.name = "Ana"; g.explorer.form = Explorer.GIRL; Lang.addNames("Ana");
                g.stage = stage; g.time = (stage % 7) * 1440 + hour * 60; g.refreshObjects();
                g.life.rod = stage >= 3; g.life.catches[0] = 3; g.life.catches[1] = 1; g.life.catches[4] = 2; g.gardening.stock[0] = 1; g.gardening.can = true;
                g.cooking.dishes[0] = 1; g.home.coins = 500; g.trades.step = Math.min(TradeQuest.STEPS.length, stage / 6);
                for (String mapName : new ArrayList<>(g.maps.keySet())) {
                    GameMap m = g.maps.get(mapName);
                    for (Npc n : new ArrayList<>(m.npcs)) {
                        if (!g.vis(n)) continue;
                        g.map = m; g.tx = n.x; g.ty = n.y + 1; g.state = Game.PLAY; g.dlg = null;
                        try { g.talk(n); } catch (RuntimeException e) { throw new AssertionError("Talk " + n.id + " at stage " + stage, e); }
                        drain(g);
                        g.stage = stage; g.map = m;
                    }
                    paint(g);
                }
                if (hour == 10 && stage % 6 == 0) { g.map = g.maps.get("Casa"); g.tx = 5; g.ty = 5; screens(g); g.stage = stage; }
            }
        }
        // telas e momentos que a varredura não alcança sozinha
        g.newGame(); g.state = Game.PLAY; g.stage = Story.FINAL;
        g.credits.open(Game.PLAY, true); for (int s = 0; s < 1600; s += 40) { g.credits.scroll = s; paint(g); }
        g.state = Game.PLAY; g.explorer.startNaming(); paint(g); g.explorer.step = 1; paint(g); g.explorer.askLegacy(); paint(g);
        g.state = Game.PLAY; g.saveSlots.open(false); paint(g); g.saveSlots.open(true); paint(g);
        g.state = Game.PLAY; g.home.openDecor(Game.PLAY); paint(g);
        for (int b = 0; b < 4; b++) { g.observatory.viewing = false; g.observatory.selected = b; g.state = Game.OBSERVATORY; paint(g); }
        g.observatory.viewing = true; for (int c = 0; c < Observatory.SKY.length; c++) { g.observatory.selected = c; paint(g); }
        g.state = Game.PLAY; g.mail.received.clear(); g.mail.received.addAll(Mailbox.LETTERS.stream().map(Mailbox.Letter::id).toList());
        for (int l = 0; l < Mailbox.LETTERS.size(); l++) { g.mail.selected = l; g.mail.reading = false; g.state = Game.MAIL; paint(g); g.mail.reading = true; paint(g); }
        g.state = Game.PLAY;
        for (int h = 0; h < Hints.HINTS.length; h++) { g.stage = h; for (int i = 0; i < 3; i++) { g.hints.ask(); drain(g); } }
        for (int s = 0; s < Seasons.NAMES.length; s++) { g.time = (s * 28 + 4) * 1440 + 600; g.map = g.maps.get("Vila Aurora"); g.state = Game.WEATHER; paint(g); g.state = Game.PLAY; paint(g); }

        g.say("", "Frase de controle sem tradução."); drain(g);          // o teste precisa enxergar faltas
        check(Lang.missing.contains("Frase de controle sem tradução."), "Crawler detects untranslated text");
        Lang.missing.remove("Frase de controle sem tradução.");
        System.out.println("Textos traduzidos na varredura: " + Lang.translatedCount());
        Set<String> missing = new TreeSet<>(Lang.missing);
        missing.removeIf(s -> s.equals("Ana") || s.startsWith("Ana "));
        if (!missing.isEmpty()) {
            Files.write(Path.of("faltando.txt"), missing);
            System.out.println("Sem tradução (" + missing.size() + "), veja faltando.txt:");
            missing.stream().limit(40).forEach(s -> System.out.println("  " + s));
        }
        check(missing.isEmpty(), "Every drawn text has an English translation");
        Lang.set("pt");
        System.out.println("PASS: " + VillageLifeTest.checks + " language checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
