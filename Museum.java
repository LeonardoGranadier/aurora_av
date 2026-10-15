import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * Museu de Aurora, ao lado do observatório, como a coleção de Animal Crossing e as espécies de horário de Stardew:
 * 8 peixes raros (aparecem só em certa estação, hora, clima e lugar, no lugar de um peixe comum) e 10 insetos
 * pousados pelo mapa (pegos com a rede que fica na porta do museu). Cada espécie doada rende moedas; cada ala
 * completa, uma decoração; tudo doado, a conquista de curador. As dicas de onde achar ficam sempre visíveis.
 */
final class Museum implements Feature {
    static final int SPRING = 1, SUMMER = 2, AUTUMN = 4, WINTER = 8, ALL_YEAR = 15, ANY = -1, DRY = -2;
    /** from/to: horas em que aparece (to < from atravessa a meia-noite); weather: ANY, DRY (sem chuva) ou um clima. */
    record Species(String name, String map, int x, int y, int seasons, int from, int to, int weather, int stage,
                   Color color, double difficulty, String hint) {}
    static final Species[] FISH = {
        new Species("Salmão-Real", "Floresta Verde", 0, 0, AUTUMN, 6, 18, ANY, 0, new Color(232, 112, 92), 1.3,
            "Outono, de dia, no lago da Floresta Verde."),
        new Species("Enguia Elétrica", "Vila Aurora", 0, 0, SUMMER, 19, 5, Weather.RAIN, 0, new Color(240, 222, 80), 1.4,
            "Verão, à noite, no lago da vila, quando chove."),
        new Species("Peixe-Neblina", "Praia Azul", 0, 0, ALL_YEAR, 0, 24, Weather.FOG, 0, new Color(205, 214, 228), 1.2,
            "Qualquer estação, no mar da Praia Azul, em dia de neblina."),
        new Species("Carpa Arco-Íris", ChapterFive.TRAIL, 0, 0, SPRING, 6, 18, ANY, 23, new Color(240, 140, 200), 1.2,
            "Primavera, de dia, no lago da Trilha dos Vagalumes."),
        new Species("Truta de Gelo", "Floresta Verde", 0, 0, WINTER, 5, 11, ANY, 0, new Color(170, 220, 245), 1.3,
            "Inverno, de manhã cedo, no lago da Floresta Verde."),
        new Species("Baiacu Dourado", "Ilha das Estrelas", 0, 0, SUMMER, 9, 17, Weather.SUN, 11, new Color(246, 196, 60), 1.3,
            "Verão, com sol, no mar da Ilha das Estrelas."),
        new Species("Peixe-Vagalume", ChapterFive.TRAIL, 0, 0, SUMMER | AUTUMN, 20, 3, ANY, 23, new Color(200, 255, 140), 1.4,
            "Verão e outono, à noite, no lago da Trilha dos Vagalumes."),
        new Species("Rei do Recife", ChapterSix.REEF, 0, 0, ALL_YEAR, 21, 4, ANY, ChapterSix.DONE, new Color(120, 90, 210), 1.5,
            "Depois do capítulo 6, à noite, nas águas do Recife de Coral.")
    };
    static final Species[] BUGS = {
        new Species("Borboleta-Limão", "Vila Aurora", 2, 19, SPRING | SUMMER, 7, 17, DRY, 0, new Color(250, 230, 90), 0,
            "Primavera e verão, de dia, no canto sudoeste da vila."),
        new Species("Joaninha", "Floresta Verde", 18, 4, SPRING, 6, 18, DRY, 0, new Color(220, 50, 50), 0,
            "Primavera, de dia, perto do lago da Floresta Verde."),
        new Species("Cigarra", "Floresta Verde", 21, 13, SUMMER, 9, 18, DRY, 0, new Color(120, 150, 90), 0,
            "Verão, de dia, nas árvores do sul da Floresta Verde."),
        new Species("Vagalume-Rei", ChapterFive.TRAIL, 9, 17, SUMMER, 19, 4, DRY, 23, new Color(230, 255, 120), 0,
            "Verão, à noite, no sul da Trilha dos Vagalumes."),
        new Species("Besouro-Rinoceronte", "Floresta Verde", 5, 16, SUMMER, 20, 4, ANY, 0, new Color(110, 70, 40), 0,
            "Verão, à noite, no sudoeste da Floresta Verde."),
        new Species("Libélula Azul", "Vila Aurora", 27, 16, SUMMER | AUTUMN, 8, 17, DRY, 0, new Color(70, 140, 230), 0,
            "Verão e outono, de dia, na beira do lago da vila."),
        new Species("Grilo-Cantor", "Vila Aurora", 24, 19, AUTUMN, 18, 2, ANY, 0, new Color(90, 130, 60), 0,
            "Outono, à noite, no sudeste da vila."),
        new Species("Mariposa-Lua", ChapterFive.TRAIL, 3, 12, AUTUMN | WINTER, 19, 4, DRY, 23, new Color(220, 225, 250), 0,
            "Outono e inverno, à noite, no oeste da Trilha dos Vagalumes."),
        new Species("Besouro-de-Gelo", "Trilha da Montanha", 3, 10, ALL_YEAR, 9, 16, DRY, 16, new Color(150, 210, 240), 0,
            "Qualquer estação, de dia, na neve perto da cabana do Eremita."),
        new Species("Borboleta-Aurora", ChapterNine.FOREST, 18, 10, ALL_YEAR, 20, 4, DRY, ChapterNine.FINAL, new Color(120, 255, 190), 0,
            "Depois do capítulo 9, à noite, no leste da Floresta Antiga.")
    };
    static final int[] REWARD_COINS = {20, 20};
    static final double RARE_CHANCE = 0.25, RARE_LUCK = 0.4;

    final Game game;
    Random random = new Random();
    boolean net;
    final int[] fish = new int[FISH.length], bugs = new int[BUGS.length], bugDay = new int[BUGS.length];
    int donated;                  // bits: peixes 0..7, insetos 8..17
    int tab, selected, returnState = Game.PLAY;
    String status = "";

    Museum(Game game) { this.game = game; reset(); }

    // ---------- Regras ----------
    static boolean inHours(double hour, int from, int to) { return from <= to ? hour >= from && hour < to : hour >= from || hour < to; }
    boolean present(Species s) {
        int weather = game.weather.current();
        boolean sky = s.weather() == ANY || (s.weather() == DRY ? weather != Weather.RAIN : weather == s.weather());
        return sky && game.stage >= s.stage() && (s.seasons() & (1 << game.seasons.current())) != 0 && inHours(game.hour(), s.from(), s.to());
    }
    /** Peixe raro que morde agora neste mapa (sorteado ao lançar a linha) ou -1. */
    int roll() {
        List<Integer> here = new ArrayList<>();
        for (int i = 0; i < FISH.length; i++) if (FISH[i].map().equals(game.map.name) && present(FISH[i])) here.add(i);
        if (here.isEmpty()) return -1;
        double chance = game.cooking.active(Cooking.LUCK) ? RARE_LUCK : RARE_CHANCE;
        return random.nextDouble() < chance ? here.get(random.nextInt(here.size())) : -1;
    }
    String caughtFish(int i) {
        boolean first = fish[i] == 0;
        fish[i] = Math.min(999999, fish[i] + 1);
        return (first ? "Peixe raro! " : "Você pescou: ") + FISH[i].name() + "!" + (first ? " Leve ao Museu de Aurora." : "");
    }
    boolean donatedFish(int i) { return (donated & (1 << i)) != 0; }
    boolean donatedBug(int i) { return (donated & (1 << (8 + i))) != 0; }
    int fishFound() { return (int) Arrays.stream(fish).filter(c -> c > 0).count(); }
    int bugsFound() { return (int) Arrays.stream(bugs).filter(c -> c > 0).count(); }
    boolean allFish() { return fishFound() == FISH.length; }
    boolean allBugs() { return bugsFound() == BUGS.length; }
    boolean fishWing() { for (int i = 0; i < FISH.length; i++) if (!donatedFish(i)) return false; return true; }
    boolean bugWing() { for (int i = 0; i < BUGS.length; i++) if (!donatedBug(i)) return false; return true; }
    boolean complete() { return fishWing() && bugWing(); }
    int donatedCount() { return Integer.bitCount(donated); }
    boolean open() { return game.stage >= 4; }

    /** Algo que aparece hoje, para as notícias do rádio (ou null). */
    String rumor(int day) {
        List<Species> list = new ArrayList<>(Arrays.asList(FISH)); list.addAll(Arrays.asList(BUGS));
        list.removeIf(s -> game.stage < s.stage() || (s.seasons() & (1 << game.seasons.current())) == 0
            || s.weather() >= 0 && s.weather() != game.weather.current());
        return list.isEmpty() ? null : list.get(Math.floorMod(day * 7 + 3, list.size())).hint();
    }

    // ---------- Mundo ----------
    @Override public void reset() {
        net = false; Arrays.fill(fish, 0); Arrays.fill(bugs, 0); Arrays.fill(bugDay, -1);
        donated = 0; tab = 0; selected = 0; status = "";
    }

    @Override public void buildWorld() {
        game.maps.get("Vila Aurora").addNpc(new Npc("museu", "Museu de Aurora", 27, 7, new Color(190, 170, 140)).obj("museu").still());
        for (int i = 0; i < BUGS.length; i++)
            game.maps.get(BUGS[i].map()).addNpc(new Npc("inseto" + i, BUGS[i].name(), BUGS[i].x(), BUGS[i].y(), BUGS[i].color()).obj("inseto").still());
    }

    @Override public boolean visible(Npc n) {
        if (!n.id.startsWith("inseto")) return true;
        int i = Integer.parseInt(n.id.substring(6));
        return bugDay[i] != game.day() && present(BUGS[i]);
    }

    @Override public boolean talk(Npc n) {
        if (n.id.startsWith("inseto")) {
            int i = Integer.parseInt(n.id.substring(6));
            if (!net) { game.say("", "Um(a) " + BUGS[i].name() + "! Sem uma rede, ele(a) escapa entre os dedos. O museu ao lado do observatório empresta uma."); return true; }
            boolean first = bugs[i] == 0;
            bugs[i] = Math.min(999999, bugs[i] + 1); bugDay[i] = game.day();
            game.audio.sfx(first ? "fanfare" : "pickup");
            game.say("", (first ? "Inseto novo! " : "Você pegou: ") + BUGS[i].name() + "!" + (first ? " Leve ao Museu de Aurora." : ""));
            game.save(false);
            return true;
        }
        if (!n.id.equals("museu")) return false;
        if (!open()) { game.say("Museu de Aurora", "Uma placa na porta: \"Museu em arrumação. Abre depois que o farol voltar a brilhar.\""); return true; }
        if (!net) {
            net = true;
            game.audio.sfx("fanfare");
            game.say("Museu de Aurora", "Um bilhete do Prof. Carvalho está preso na porta, junto de uma rede:",
                "\"O Museu de Aurora precisa de ajuda! Traga peixes raros e insetos. Cada espécie tem sua estação, sua hora e seu clima.\"",
                "\"A rede é sua. Os insetos aparecem pousados pelo mapa; fale com eles para pegar. Prof. C.\"",
                "Você ganhou a Rede de Insetos! O rádio de casa também dá dicas do que aparece em cada dia.");
            game.afterDlg = this::openScreen;
            game.save(false);
            return true;
        }
        openScreen();
        return true;
    }

    void openScreen() { selected = 0; status = ""; returnState = Game.PLAY; game.down.clear(); game.state = Game.MUSEUM; }
    void openFromMenu() { openScreen(); returnState = Game.MENU; }

    // ---------- Tela ----------
    int count() { return tab == 0 ? FISH.length : BUGS.length; }
    @Override public boolean owns(int state) { return state == Game.MUSEUM; }
    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        int old = selected, cols = tab == 0 ? 4 : 5;
        if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_TAB || key == KeyEvent.VK_Q || key == KeyEvent.VK_E) { tab = 1 - tab; selected = 0; status = ""; game.audio.sfx("select"); return; }
        if (key == KeyEvent.VK_LEFT) selected = (selected + count() - 1) % count();
        else if (key == KeyEvent.VK_RIGHT) selected = (selected + 1) % count();
        else if (key == KeyEvent.VK_UP) selected = (selected + count() - cols) % count();
        else if (key == KeyEvent.VK_DOWN) selected = (selected + cols) % count();
        else if (act) donate();
        if (selected != old) { game.audio.sfx("select"); status = ""; }
    }

    void donate() {
        if (returnState == Game.MENU) { status = "Para doar, vá até o Museu de Aurora, ao lado do observatório."; return; }
        int i = selected, bit = tab == 0 ? i : 8 + i;
        boolean have = tab == 0 ? fish[i] > 0 : bugs[i] > 0;
        if ((donated & (1 << bit)) != 0) { status = "Esta espécie já está na vitrine."; return; }
        if (!have) { status = "Você ainda não encontrou esta espécie."; return; }
        boolean wingBefore = tab == 0 ? fishWing() : bugWing();
        int oldDonated = donated, oldCoins = game.home.coins;
        donated |= 1 << bit;
        game.home.coins = Math.min(HomeDecor.MAX_COINS, game.home.coins + REWARD_COINS[tab]);
        if (!game.save(false)) { donated = oldDonated; game.home.coins = oldCoins; status = "Não foi possível salvar a doação."; return; }
        game.audio.sfx("fanfare");
        String name = (tab == 0 ? FISH : BUGS)[i].name();
        status = Lang.done(Lang.t("Doado:") + " " + Lang.t(name) + ". " + Lang.t("O professor deixou " + REWARD_COINS[tab] + " moedas de agradecimento."));
        if (!wingBefore && (tab == 0 ? fishWing() : bugWing()))
            status = tab == 0 ? "Ala dos peixes completa! Um Aquário Raro foi para a sua casa." : "Ala dos insetos completa! Um Quadro de Insetos foi para a sua casa.";
    }

    @Override public void draw(Graphics2D g) {
        game.life.panel(g, "Museu de Aurora", "Tab: trocar de ala · Setas: escolher · Z: doar · Esc: sair");
        String[] tabs = {"Peixes raros " + fishFound() + "/" + FISH.length, "Insetos " + bugsFound() + "/" + BUGS.length};
        for (int t = 0; t < 2; t++) {
            g.setColor(t == tab ? new Color(60, 120, 120) : new Color(205, 220, 214)); g.fillRoundRect(44 + t * 200, 108, 190, 26, 8, 8);
            g.setColor(t == tab ? Color.WHITE : new Color(60, 80, 80)); g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(Lang.t(tabs[t]), 56 + t * 200, 126);
        }
        game.life.text(g, "Doadas: " + donatedCount() + "/" + (FISH.length + BUGS.length), 470, 126, 130, 12);
        Species[] list = tab == 0 ? FISH : BUGS;
        int cols = tab == 0 ? 4 : 5, cell = tab == 0 ? 132 : 106;
        for (int i = 0; i < list.length; i++) {
            int x = 44 + (i % cols) * cell, y = 146 + (i / cols) * 70;
            boolean have = tab == 0 ? fish[i] > 0 : bugs[i] > 0, given = tab == 0 ? donatedFish(i) : donatedBug(i);
            g.setColor(given ? new Color(214, 236, 214) : new Color(232, 238, 236)); g.fillRoundRect(x, y, cell - 8, 62, 8, 8);
            if (i == selected) { g.setColor(new Color(200, 60, 50)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(x, y, cell - 8, 62, 8, 8); g.setStroke(new BasicStroke(1)); }
            Color c = have ? list[i].color() : new Color(170, 184, 180);
            if (tab == 0) game.life.drawFish(g, x + (cell - 8) / 2 - 22, y + 14, 36, c);
            else {
                Graphics2D b = (Graphics2D) g.create(); b.translate(x + (cell - 8) / 2 - 26, y + 4); b.scale(1.65, 1.65);
                drawBug(b, i, 0, -2, c, false); b.dispose();
            }
            if (given) { g.setColor(new Color(60, 140, 80)); g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.drawString(Lang.t("✓"), x + cell - 26, y + 18); }
        }
        Species s = list[selected];
        boolean have = tab == 0 ? fish[selected] > 0 : bugs[selected] > 0;
        int y0 = 296;
        game.life.text(g, have ? s.name() : "???", 48, y0 + 16, 540, 17);
        game.life.text(g, Lang.done(Lang.t("Onde:") + " " + Lang.t(s.hint())), 48, y0 + 42, 540, 13);
        game.life.text(g, have ? "Encontrados: " + (tab == 0 ? fish[selected] : bugs[selected]) : "Ainda não encontrado.", 48, y0 + 82, 300, 13);
        if (!net && tab == 1) game.life.text(g, "Pegue a rede na porta do museu.", 330, y0 + 82, 260, 13);
        game.life.text(g, status, 48, y0 + 112, 540, 13);
    }

    // ---------- Desenho ----------
    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.startsWith("inseto")) {
            int i = Integer.parseInt(n.id.substring(6));
            int bob = game.access.reduced ? 0 : (int) (Math.sin(game.frame / 9.0 + i) * 2);
            if (game.night() > 0.4 && (i == 3 || i == 9))
                game.glow(g, x + 16, y + 16 + bob, 20, new Color(BUGS[i].color().getRed(), BUGS[i].color().getGreen(), BUGS[i].color().getBlue(), 150),
                    new Color(BUGS[i].color().getRed(), BUGS[i].color().getGreen(), BUGS[i].color().getBlue(), 0), 1f);
            drawBug(g, i, x, y + bob, BUGS[i].color(), !game.access.reduced);
            return true;
        }
        if (!n.id.equals("museu")) return false;
        g.setColor(new Color(0, 0, 0, 45)); g.fillOval(x - 2, y + 28, 36, 8);
        g.setColor(new Color(226, 214, 190)); g.fillRect(x, y + 10, 32, 22);
        g.setColor(new Color(150, 110, 80)); g.fillPolygon(new int[]{x - 3, x + 16, x + 35}, new int[]{y + 11, y - 2, y + 11}, 3);
        g.setColor(new Color(200, 190, 170)); for (int c = 0; c < 4; c++) g.fillRect(x + 2 + c * 9, y + 12, 3, 20);
        g.setColor(new Color(120, 90, 60)); g.fillRect(x + 12, y + 20, 8, 12);
        g.setColor(complete() ? new Color(246, 204, 76) : new Color(90, 150, 200)); g.fillOval(x + 13, y + 3, 6, 6);
        return true;
    }

    /** Inseto 32x32 desenhado por código: borboletas, besouros, libélula, cigarra, grilo e vagalumes. */
    void drawBug(Graphics2D g, int i, int x, int y, Color c, boolean animate) {
        boolean up = animate && (game.frame / 8 + i) % 2 == 0;
        Color dark = c.darker().darker();
        switch (i) {
            case 0, 7, 9 -> {                                                     // borboletas e mariposa
                int w = up ? 6 : 10;
                g.setColor(c); g.fillOval(x + 16 - w - 1, y + 9, w, 9); g.fillOval(x + 17, y + 9, w, 9);
                g.fillOval(x + 16 - w + 1, y + 17, w - 2, 7); g.fillOval(x + 17, y + 17, w - 2, 7);
                g.setColor(dark); g.fillRect(x + 15, y + 9, 2, 15); g.drawLine(x + 15, y + 9, x + 12, y + 5); g.drawLine(x + 16, y + 9, x + 19, y + 5);
            }
            case 5 -> {                                                           // libélula
                g.setColor(new Color(220, 240, 255, 200)); g.fillOval(x + 5, y + (up ? 9 : 11), 10, 4); g.fillOval(x + 17, y + (up ? 9 : 11), 10, 4);
                g.fillOval(x + 6, y + 15, 9, 3); g.fillOval(x + 17, y + 15, 9, 3);
                g.setColor(c); g.fillRect(x + 15, y + 8, 3, 18); g.setColor(dark); g.fillOval(x + 14, y + 6, 5, 5);
            }
            case 6, 2 -> {                                                        // grilo e cigarra
                g.setColor(c); g.fillOval(x + 8, y + 13, 17, 8);
                g.setColor(dark); g.drawLine(x + 12, y + 20, x + 8, y + 25); g.drawLine(x + 20, y + 20, x + 25, y + 25);
                g.drawLine(x + 22, y + 14, x + 28, y + 8); g.fillOval(x + 21, y + 12, 6, 6);
                if (i == 2) { g.setColor(new Color(220, 240, 230, 180)); g.fillOval(x + 9, y + (up ? 8 : 10), 13, 6); }
            }
            case 3 -> {                                                           // vagalume
                g.setColor(dark); g.fillOval(x + 12, y + 10, 8, 9);
                g.setColor(c); g.fillOval(x + 12, y + 17, 8, 7);
                g.setColor(new Color(230, 240, 255, 180)); g.fillOval(x + 7, y + (up ? 8 : 10), 7, 5); g.fillOval(x + 18, y + (up ? 8 : 10), 7, 5);
            }
            default -> {                                                          // besouros e joaninha
                g.setColor(dark); g.fillOval(x + 12, y + 8, 8, 6);
                g.setColor(c); g.fillOval(x + 9, y + 12, 14, 14);
                g.setColor(dark); g.drawLine(x + 16, y + 12, x + 16, y + 25);
                if (i == 1) { g.fillOval(x + 11, y + 16, 3, 3); g.fillOval(x + 18, y + 15, 3, 3); g.fillOval(x + 13, y + 21, 3, 3); g.fillOval(x + 18, y + 21, 3, 3); }
                if (i == 4) { g.setColor(dark); g.fillRect(x + 15, y + 2, 2, 7); }
                g.setColor(dark); g.drawLine(x + 9, y + 18, x + 5, y + 20); g.drawLine(x + 23, y + 18, x + 27, y + 20);
            }
        }
    }

    // ---------- Save ----------
    @Override public void save(Properties p) {
        p.setProperty("museu.rede", "" + net); p.setProperty("museu.doado", "" + donated);
        for (int i = 0; i < FISH.length; i++) p.setProperty("museu.peixe." + i, "" + fish[i]);
        for (int i = 0; i < BUGS.length; i++) { p.setProperty("museu.inseto." + i, "" + bugs[i]); p.setProperty("museu.inseto_dia." + i, "" + (bugDay[i] + 1)); }   // 0: nunca
    }
    @Override public void load(Properties p) {
        net = Boolean.parseBoolean(p.getProperty("museu.rede", "false"));
        donated = game.life.number(p, "museu.doado", 0, (1 << 18) - 1);
        for (int i = 0; i < FISH.length; i++) fish[i] = game.life.number(p, "museu.peixe." + i, 0, 999999);
        for (int i = 0; i < BUGS.length; i++) { bugs[i] = game.life.number(p, "museu.inseto." + i, 0, 999999); bugDay[i] = game.life.number(p, "museu.inseto_dia." + i, 0, Integer.MAX_VALUE - 1) - 1; }
        for (int i = 0; i < FISH.length; i++) if (fish[i] == 0) donated &= ~(1 << i);          // não doa o que não tem
        for (int i = 0; i < BUGS.length; i++) if (bugs[i] == 0) donated &= ~(1 << (8 + i));
    }
    @Override public String signature() { return "m" + net + donated + Arrays.toString(fish) + Arrays.toString(bugs) + Arrays.toString(bugDay); }
}
