import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;
import javax.swing.SwingUtilities;

/** Plays chapters 6 and 7 and the legacy name question. Run in an empty temporary directory with the assets link. */
public class NewChaptersTest {
    static void check(boolean ok, String message) { VillageLifeTest.check(ok, message); }
    static void tap(Game g, int key) { VillageLifeTest.tap(g, key); }
    static Npc npc(Game g, String map, String id) { return ClassicFeaturesTest.npc(g, map, id); }
    static void finish(Game g) { ClassicFeaturesTest.finishDialogue(g); }
    static void night(Game g) { g.time = Math.floor(g.time / 1440) * 1440 + 22 * 60; }
    static void day(Game g) { g.time = Math.floor(g.time / 1440) * 1440 + 10 * 60; }
    /** Stands next to the npc (first free neighbour), faces it and interacts, like a player would. */
    static void meet(Game g, String map, String id) {
        Npc n = npc(g, map, id); GameMap m = g.maps.get(map);
        for (int d = 0; d < 4; d++) {
            int x = n.x - Game.DX[d], y = n.y - Game.DY[d];
            boolean taken = m.npcs.stream().anyMatch(o -> o != n && g.vis(o) && o.x == x && o.y == y);
            if (!m.solid(x, y) && !taken) { g.travel(map, x, y); g.dir = d; g.interact(); return; }
        }
        throw new AssertionError("No free tile next to " + id);
    }

    static void chapterSix(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = 28; g.life.rod = true;
        check(g.worldMap.destinations().equals(Set.of("Praia Azul")), "Chapter 6 points to the beach");
        day(g); meet(g, "Praia Azul", "capita_fim"); check(g.stage == 28, "Captain waits for the night"); finish(g);
        night(g); meet(g, "Praia Azul", "capita_fim"); check(g.stage == ChapterSix.LISTEN, "Chapter 6 starts at night"); finish(g);
        check(g.worldMap.destinations().size() == 3, "Three singing shells to find");
        meet(g, "Ilha das Estrelas", "bote_ilha"); finish(g);
        check(g.map.name.equals(ChapterSix.REEF), "Island boat reaches the reef");
        check(g.worldMap.visited.contains(ChapterSix.REEF), "Reef discovered");
        g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "reef-night");
        for (int i = 0; i < 3; i++) {
            meet(g, ChapterSix.SHELL_MAPS[i], "concha_cantora" + i);
            check(Integer.bitCount(g.chapterSix.heard) == i + 1, "Shell heard " + i);
            finish(g);
        }
        check(g.stage == ChapterSix.PLAY_SONG && g.chapterSix.songText().equals("MI, DÓ, SOL, RÉ, MI"), "Song learned");
        g.journal.open(Game.PLAY); check(g.journal.entries().stream().anyMatch(l -> l.startsWith("Canção da Maré")), "Song in journal");
        VillageLifeTest.screenshot(g, "journal-ch6"); tap(g, KeyEvent.VK_ESCAPE);
        day(g); meet(g, ChapterSix.REEF, "recife_ponta"); check(g.state == Game.PLAY && g.dlg != null, "Song waits for night"); finish(g);
        night(g); meet(g, ChapterSix.REEF, "recife_ponta"); check(g.state == Game.SONG, "Song screen opens");
        tap(g, KeyEvent.VK_DOWN); tap(g, KeyEvent.VK_RIGHT);
        check(g.chapterSix.progress == 0 && g.chapterSix.status.startsWith("A nota"), "Wrong note restarts");
        tap(g, KeyEvent.VK_DOWN); tap(g, KeyEvent.VK_UP); VillageLifeTest.screenshot(g, "song");
        tap(g, KeyEvent.VK_LEFT); tap(g, KeyEvent.VK_2); tap(g, KeyEvent.VK_3);
        check(g.stage == ChapterSix.TELL && g.state == Game.PLAY && g.dlg != null, "Song calls the whale");
        finish(g); g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "whale");
        check(g.save(false) && g.loadGame() && g.chapterSix.heard == 7 && g.stage == ChapterSix.TELL, "Chapter 6 persists");
        meet(g, ChapterSix.REEF, "bote_recife"); finish(g); check(g.map.name.equals("Ilha das Estrelas"), "Reef boat returns");
        meet(g, "Praia Azul", "capita_fim"); finish(g);
        check(g.stage == ChapterSix.DONE && g.home.unlocked(19), "Chapter 6 complete, whale scale unlocked");
    }

    static void chapterSeven(Game g) throws Exception {
        day(g); meet(g, "Praia Azul", "porta_farol"); check(g.stage == ChapterSeven.START, "Invitation waits for night"); finish(g);
        night(g); meet(g, "Praia Azul", "porta_farol"); check(g.stage == ChapterSeven.INVITE, "Chapter 7 starts"); finish(g);
        Arrays.fill(g.cooking.dishes, 0);
        day(g);
        for (int i = 0; i < ChapterSeven.GUESTS.length; i++) {
            String id = ChapterSeven.GUESTS[i];
            Npc guest = npc(g, ChapterSeven.GUEST_MAPS[i], id);
            guest.sleeping = false; guest.walking = false;
            meet(g, ChapterSeven.GUEST_MAPS[i], id);
            check((g.chapterSeven.invited & (1 << i)) != 0, "Invited " + id);
            finish(g);
            if (i == 0) check(g.cooking.dishes[0] == 1, "Mother packs a soup");
            if (i == 2) { check(g.save(false) && g.loadGame() && g.chapterSeven.count() == 3, "Invites persist"); g.state = Game.PLAY; day(g); }
        }
        check(g.stage == ChapterSeven.CLIMB && g.worldMap.destinations().equals(Set.of("Pico Estrelado")), "Everyone invited");
        day(g); g.travel("Pico Estrelado", 10, 12);
        check(!g.vis(npc(g, "Pico Estrelado", "ceu_cintila")), "Cintila appears only at night");
        night(g); meet(g, "Pico Estrelado", "ceu_cintila");
        check(g.stage == ChapterSeven.PARTY && g.cooking.dishes[0] == 0, "Dish offered, party begins"); finish(g);
        for (String id : ChapterSeven.GUESTS) check(g.vis(npc(g, "Pico Estrelado", "festa_" + id)), "Guest at the party: " + id);
        g.travel("Pico Estrelado", 10, 9); g.dir = 1; g.toastT = 0; g.bannerT = 0; VillageLifeTest.screenshot(g, "festival-sky");
        meet(g, "Pico Estrelado", "festa_lucas"); check(g.dlg != null && g.dlg[0].contains("sinos"), "Guests talk"); finish(g);
        meet(g, "Pico Estrelado", "ceu_cintila"); finish(g);
        check(g.stage == Story.FINAL && g.state == Game.CREDITS, "Finale rolls the credits");
        g.credits.scroll = 300; VillageLifeTest.screenshot(g, "credits-final"); tap(g, KeyEvent.VK_Z);
        check(g.home.unlocked(20), "Festival lantern unlocked");
        g.explorer.checkTimer = 0; g.update();
        check(g.explorer.unlocked.contains("festival_ceu") && g.explorer.unlocked.contains("mare"), "Chapter achievements");
        check(!g.vis(npc(g, "Pico Estrelado", "festa_mae")), "Guests go home after the party");
        check(g.worldMap.destinations().isEmpty() && Explorer.chapterOf(g.stage) == 7, "Campaign complete");
        g.worldMap.open(Game.PLAY); VillageLifeTest.screenshot(g, "map-final"); tap(g, KeyEvent.VK_ESCAPE);
    }

    static void legacyName(Game g) throws Exception {
        g.newGame(); g.state = Game.PLAY; g.stage = 21;
        check(g.save(false), "Saved");
        Properties p = g.readSave(); p.remove("explorador.nome"); p.remove("explorador.forma"); p.remove("explorador.perguntado");
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { p.store(out, "Old save"); }
        g.openTitle(); g.choose("Continuar");
        check(g.state == Game.NAMING && g.explorer.legacy && g.stage == 21, "Mother asks the name on an old save");
        VillageLifeTest.screenshot(g, "legacy-name");
        for (char c : "Leo".toCharArray()) g.keyTyped(new KeyEvent(g, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, c));
        tap(g, KeyEvent.VK_ENTER); tap(g, KeyEvent.VK_Z);
        check(g.state == Game.PLAY && g.explorer.name.equals("Leo") && g.explorer.form == Explorer.BOY && g.stage == 21, "Old save keeps progress with a name");
        g.openTitle(); g.choose("Continuar"); check(g.state == Game.PLAY && g.explorer.name.equals("Leo"), "Named save is not asked again");
        p.remove("explorador.perguntado");
        try (var out = Files.newOutputStream(Path.of(Game.SAVE))) { p.store(out, "Old save"); }
        g.openTitle(); g.choose("Continuar"); tap(g, KeyEvent.VK_ESCAPE);
        check(g.state == Game.PLAY && g.explorer.name.isEmpty() && g.explorer.asked, "Esc skips and keeps neutral text");
        g.openTitle(); g.choose("Continuar"); check(g.state == Game.PLAY, "Skipped question is not repeated");
    }

    static void run() throws Exception {
        Game g = new Game(); g.state = Game.PLAY;
        chapterSix(g); chapterSeven(g); legacyName(g);
        System.out.println("PASS: " + VillageLifeTest.checks + " new chapter checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
