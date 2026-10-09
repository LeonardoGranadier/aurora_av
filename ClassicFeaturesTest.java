import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Run in an empty temporary directory with the assets link (sh testar.sh ClassicFeaturesTest). */
public class ClassicFeaturesTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    static Npc npc(Game g, String map, String id) {
        return g.maps.get(map).npcs.stream().filter(n -> n.id.equals(id)).findFirst().orElseThrow();
    }
    static void finishDialogue(Game g) { for (int i = 0; i < 40 && g.dlg != null; i++) tap(g, KeyEvent.VK_Z); }
    /** Shown day d (1 = first day) at the given hour. */
    static void at(Game g, int shownDay, int hour) { g.time = (shownDay - 1) * 1440 + hour * 60; }

    static boolean reachable(GameMap m, int tx, int ty) {
        boolean[][] seen = new boolean[m.h][m.w]; Deque<int[]> q = new ArrayDeque<>();
        for (Warp w : m.warps) q.add(new int[]{w.x(), w.y()});
        while (!q.isEmpty()) {
            int[] c = q.poll(); int x = c[0], y = c[1];
            if (x < 0 || y < 0 || x >= m.w || y >= m.h || seen[y][x] || m.solid(x, y)) continue;
            seen[y][x] = true;
            if (x == tx && y == ty) return true;
            q.add(new int[]{x + 1, y}); q.add(new int[]{x - 1, y}); q.add(new int[]{x, y + 1}); q.add(new int[]{x, y - 1});
        }
        return false;
    }

    static void shells(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY;
        Set<String> used = new HashSet<>();
        for (SecretShells.Spot s : SecretShells.SPOTS) {
            GameMap m = g.maps.get(s.map());
            check(m != null && m.solid(s.x(), s.y()), "Shell hidden in solid tile: " + s);
            check(used.add(s.map() + s.x() + "," + s.y()), "Shell spots distinct");
            boolean access = false;
            for (int d = 0; d < 4; d++) {
                int x = s.x() + Game.DX[d], y = s.y() + Game.DY[d];
                if (m.at(x, y) != 'i' && reachable(m, x, y)) access = true;
            }
            check(access, "Shell reachable from a floor tile: " + s);
            for (Npc n : m.npcs) check(n.id.startsWith("concha_secreta") || n.x != s.x() || n.y != s.y(), "Shell does not share a tile");
        }
        SecretShells.Spot first = SecretShells.SPOTS[0];
        g.travel(first.map(), first.x(), first.y() + 1); g.dir = 1;
        g.toastT = 0; VillageLifeTest.screenshot(g, "shell-before");
        g.interact();
        check(g.shells.found[0] && g.dlg != null && g.itemGetKey.equals("concha_secreta"), "Shell found with item pose");
        VillageLifeTest.screenshot(g, "shell-found");
        finishDialogue(g);
        check(!g.vis(npc(g, first.map(), "concha_secreta0")), "Found shell disappears");
        g.interact(); check(g.dlg == null && g.shells.found() == 1, "Shell cannot be found twice");
        check(g.loadGame() && g.shells.found[0], "Shell persists");
        Arrays.fill(g.shells.found, 0, 3, true);
        g.travel("Praia Azul", 17, 10); g.dir = 1; g.interact();
        finishDialogue(g);
        check(g.shells.rewarded == 1 && g.shells.listening(), "Seu Marinho gives the listening shell");
        Arrays.fill(g.shells.found, 0, 9, true); int coins = g.home.coins;
        g.interact(); finishDialogue(g);
        check(g.shells.rewarded == 3 && g.home.coins == coins + 120, "Accumulated rewards delivered together");
        Arrays.fill(g.shells.found, true); g.interact(); finishDialogue(g);
        check(g.home.unlocked(17), "All shells unlock the collection");
        g.worldMap.open(Game.PLAY); VillageLifeTest.screenshot(g, "shell-map"); tap(g, KeyEvent.VK_ESCAPE);
        Properties legacy = new Properties(); g.shells.load(legacy); check(g.shells.found() == 0 && g.shells.rewarded == 0, "Old saves start without shells");
    }

    static void trades(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY;
        Npc merchant = npc(g, "Praia Azul", "mascate");
        at(g, 1, 9); check(Game.weekday(1).equals("Dom") && g.vis(merchant), "Merchant visits on Sunday");
        at(g, 2, 9); check(!g.vis(merchant), "Merchant absent on Monday");
        at(g, 8, 19); check(!g.vis(merchant), "Merchant leaves at 18h");
        check(!g.trades.talk(npc(g, "Floresta Verde", "lenhador")), "Woodcutter ignores trade before it starts");
        at(g, 8, 9); g.talk(merchant); check(g.trades.step == 1 && g.trades.item().equals("Bolinha de gude"), "Merchant starts chain");
        VillageLifeTest.screenshot(g, "trade-merchant"); finishDialogue(g);
        g.talk(npc(g, "Floresta Verde", "lenhador")); check(g.trades.step == 2, "Marble traded"); finishDialogue(g);
        g.talk(npc(g, "Trilha da Montanha", "montanhista")); check(g.trades.step == 3, "Pine cone traded"); finishDialogue(g);
        g.stage = 16; g.talk(npc(g, "Cabana do Eremita", "eremita"));
        check(g.trades.step == 3 && g.stage == 17, "Hermit story has priority"); finishDialogue(g);
        g.stage = 19; g.talk(npc(g, "Cabana do Eremita", "eremita")); check(g.trades.step == 4, "Scarf traded"); finishDialogue(g);
        g.talk(npc(g, "Praia Azul", "capita_fim")); check(g.trades.step == 5, "Tea traded"); finishDialogue(g);
        g.stage = 15; g.talk(npc(g, "Vila Aurora", "prof")); check(g.trades.step == 5 && g.stage == 16, "Professor story has priority"); finishDialogue(g);
        g.stage = 20; g.talk(npc(g, "Vila Aurora", "prof")); check(g.trades.complete(), "Chain completes with the key"); finishDialogue(g);
        check(g.loadGame() && g.trades.complete(), "Trades persist");
    }

    static void observatory(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY;
        Observatory o = g.observatory;
        check(!o.ready(0) && !o.donate(0), "Empty bundle refused");
        g.life.catches[0] = 1; g.life.catches[1] = 2; g.life.catches[2] = 1;
        check(o.ready(0), "Fish bundle ready");
        Files.deleteIfExists(Path.of(Game.SAVE));
        Files.createDirectory(Path.of(Game.SAVE)); Files.createFile(Path.of(Game.SAVE, "block"));
        check(!o.donate(0) && !o.has(0) && g.bonds.stock(0) == 1, "Failed save rolls back donation");
        Files.delete(Path.of(Game.SAVE, "block")); Files.delete(Path.of(Game.SAVE));
        check(o.donate(0) && o.has(0) && g.bonds.stock(1) == 1 && g.life.catches[1] == 2, "Fish donated from stock; album kept");
        g.gardening.stock[0] = 1; g.gardening.stock[1] = 1; g.gardening.stock[2] = 1;
        check(o.donate(1) && Arrays.stream(g.gardening.stock).sum() == 0, "Flowers donated");
        g.home.coins = 200; Arrays.fill(g.shells.found, 0, 6, true);
        check(o.donate(2) && g.home.coins == 170, "Sea bundle donated");
        check(!o.ready(3), "Last bundle needs the key");
        g.trades.step = TradeQuest.STEPS.length;
        g.talk(npc(g, "Vila Aurora", "observatorio")); check(g.state == Game.OBSERVATORY && !o.viewing, "Donation panel opens");
        o.selected = 3; tap(g, KeyEvent.VK_Z); check(g.state == Game.HANDOFF, "Donation asks confirmation");
        tap(g, KeyEvent.VK_RIGHT); tap(g, KeyEvent.VK_Z);
        check(o.complete() && g.home.coins == 70 && g.home.unlocked(18), "Observatory restored");
        VillageLifeTest.screenshot(g, "observatory-panel"); tap(g, KeyEvent.VK_ESCAPE);
        g.time = 22 * 60; g.travel("Vila Aurora", 25, 5); g.dir = 1; g.state = Game.PLAY;
        VillageLifeTest.screenshot(g, "observatory-village-night");
        g.interact(); check(g.state == Game.OBSERVATORY && o.viewing, "Telescope shows the sky at night");
        tap(g, KeyEvent.VK_RIGHT); VillageLifeTest.screenshot(g, "observatory-sky"); tap(g, KeyEvent.VK_Z);
        check(g.state == Game.PLAY, "Sky closes");
        check(g.vis(npc(g, "Praia Azul", "luneta_praia")), "Beach telescope appears");
        check(g.loadGame() && g.observatory.complete(), "Observatory persists");
    }

    static void mail(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; at(g, 1, 8);
        g.update(); check(g.mail.received.isEmpty(), "No mail on the first morning");
        Npc box = npc(g, "Vila Aurora", "caixa_correio");
        g.talk(box); check(g.dlg != null && g.state == Game.PLAY, "Empty mailbox message"); g.dlg = null;
        at(g, 2, 7); g.update(); check(g.mail.received.contains("mae_bemvindo") && g.mail.unread() > 0, "Letter arrives next day");
        g.stage = 15; g.life.rod = true; g.life.bellBest = VillageLife.BELL_ROUNDS;
        int before = g.mail.received.size();
        at(g, 3, 7); g.update(); check(g.mail.received.size() - before == Mailbox.PER_DAY, "At most two letters per day");
        at(g, 6, 7); g.update(); at(g, 7, 7); g.update();
        check(g.mail.received.contains("capita_obrigada"), "Event letters arrive");
        g.talk(box); check(g.state == Game.MAIL, "Mailbox opens");
        VillageLifeTest.screenshot(g, "mail-list");
        int coins = g.home.coins;
        g.mail.selected = g.mail.received.size() - 1 - g.mail.received.indexOf("capita_obrigada");
        tap(g, KeyEvent.VK_Z); check(g.mail.reading && g.home.coins == coins + 30, "Letter gift collected");
        VillageLifeTest.screenshot(g, "mail-letter");
        tap(g, KeyEvent.VK_Z); tap(g, KeyEvent.VK_Z); tap(g, KeyEvent.VK_Z);
        check(g.home.coins == coins + 30, "Gift collected only once");
        tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Mailbox closes");
        check(g.loadGame() && g.mail.read.contains("capita_obrigada"), "Mail persists");
    }

    static void explorer(Game g) throws Exception {
        Files.deleteIfExists(Path.of(Game.SAVE)); Files.deleteIfExists(Path.of(Game.BACKUP));
        g.openTitle(); check(g.opts.contains("Créditos"), "Title lists credits");
        g.choose("Novo jogo"); check(g.state == Game.NAMING, "New game asks the name");
        for (char c : "Ana Lua".toCharArray()) g.keyTyped(new java.awt.event.KeyEvent(g, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, c));
        VillageLifeTest.screenshot(g, "naming");
        tap(g, KeyEvent.VK_BACK_SPACE); tap(g, KeyEvent.VK_ENTER);
        check(g.explorer.name.equals("Ana Lu") && g.explorer.step == 1, "Name typed and edited");
        tap(g, KeyEvent.VK_DOWN); VillageLifeTest.screenshot(g, "naming-form"); tap(g, KeyEvent.VK_Z);
        check(g.state == Game.PLAY && g.explorer.form == Explorer.GIRL, "Form chosen");
        check(g.explorer.w("o", "a", "e").equals("a"), "Text follows the chosen form");
        g.travel("Casa", 3, 4); g.dir = 1; g.interact();
        check(g.dlg != null && g.dlg[0].contains("Ana Lu"), "Mother greets by name"); finishDialogue(g);
        check(g.loadGame() && g.explorer.name.equals("Ana Lu") && g.explorer.form == Explorer.GIRL, "Name persists");
        Properties legacy = g.readSave(); legacy.remove("explorador.nome"); legacy.remove("explorador.forma"); legacy.setProperty("fase", "28");
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { legacy.store(out, "Old save"); }
        check(g.loadGame() && g.explorer.form == Explorer.UNSET && g.explorer.w("o", "a", "e").equals("e"), "Old saves use neutral text");
        check(g.explorer.unlocked.contains("jardim") && g.explorer.announce.isEmpty(), "Old achievements unlocked silently");
        g.newGame(); g.state = Game.PLAY; g.stage = 4; g.explorer.checkTimer = 0; g.toastT = 0; g.update();
        check(g.explorer.unlocked.contains("farol") && g.toastTxt.startsWith("Conquista"), "Achievement announced");
        tap(g, KeyEvent.VK_C); check(g.state == Game.CARD, "C opens explorer card");
        VillageLifeTest.screenshot(g, "explorer-card"); tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.PLAY, "Card closes");
        g.openPause(); g.choose("Cartão do Explorador"); check(g.state == Game.CARD, "Pause opens card"); tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.MENU, "Card returns to pause");
    }

    static void presentation(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY;
        g.setStage(28); check(g.state == Game.CREDITS && g.credits.ending, "Ending rolls credits");
        g.credits.scroll = 260; VillageLifeTest.screenshot(g, "credits-ending");
        tap(g, KeyEvent.VK_Z); check(g.state == Game.PLAY, "Credits return to play");
        g.setStage(28); check(g.state == Game.PLAY, "Credits roll once");
        g.openTitle(); g.choose("Créditos"); check(g.state == Game.CREDITS && !g.credits.ending, "Credits from title");
        tap(g, KeyEvent.VK_ESCAPE); check(g.state == Game.TITLE, "Credits back to title");

        g.newGame(); g.state = Game.PLAY;
        g.access.textSpeed = 2; g.say("Mamãe", "Um texto que aparece letra por letra.", "Segunda fala.");
        check(g.dlgChars == 0, "Typewriter starts empty");
        tap(g, KeyEvent.VK_Z); check(g.dlgIdx == 0 && g.dlgChars >= g.pageChars(), "First press reveals the page");
        tap(g, KeyEvent.VK_Z); check(g.dlgIdx == 1, "Second press advances");
        g.access.textSpeed = 0; g.dlg = null;

        double time = g.time; tap(g, KeyEvent.VK_T); check(g.time == time && !Game.DEV, "Time shortcut only in developer mode");
        g.down.add(KeyEvent.VK_SHIFT); check(g.running(), "Shift runs"); g.down.clear();
        check(Game.weekday(7).equals("Sáb") && Game.weekday(8).equals("Dom"), "Festival day is Saturday");

        g.travel("Vila Aurora", 7, 8); g.dir = 1; g.toastT = 0;
        VillageLifeTest.screenshot(g, "interaction-hint");
        g.itemGet("vara"); VillageLifeTest.screenshot(g, "item-get");

        g.takePhoto();
        try (var photos = Files.list(GameFiles.data("fotos"))) { check(photos.anyMatch(p -> p.toString().endsWith(".png")), "Photo saved"); }

        check(g.save(false) && g.save(false) && Files.isRegularFile(Path.of(Game.BACKUP)), "Previous save kept as backup");
        Files.writeString(Path.of(Game.SAVE), "corrompido");
        check(g.readSave() != null && g.loadGame(), "Corrupted save recovered from backup");
        Throwable error = new IllegalStateException("teste de registro");
        check(GameFiles.logError(error) && Files.readString(GameFiles.data("erros.log")).contains("teste de registro"), "Errors logged");
        check(!GameFiles.logError(error), "Repeated error logged once");
    }

    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        shells(g); trades(g); observatory(g); mail(g); explorer(g); presentation(g);
        System.out.println("PASS: " + VillageLifeTest.checks + " classic feature checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
