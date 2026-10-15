import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * Museu de Aurora, ao lado do observatório, como a coleção de Animal Crossing e as espécies de horário de Stardew.
 * Três alas: peixes raros (aparecem só em certa estação, hora, clima e lugar, no lugar de um peixe comum; os de
 * águas fundas pedem o barco), insetos pousados pelo mapa (pegos com a rede da porta do museu) e fósseis cavados
 * com a pá onde a terra brilha. Cada doação rende moedas e um comentário do professor; cada ala completa, uma
 * decoração; tudo doado, a conquista de curador. As dicas de onde achar ficam sempre visíveis.
 */
final class Museum implements Feature {
    static final int SPRING = 1, SUMMER = 2, AUTUMN = 4, WINTER = 8, ALL_YEAR = 15, ANY = -1, DRY = -2;
    static final int FISH_WING = 0, BUG_WING = 1, FOSSIL_WING = 2;
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
            "Depois do capítulo 6, à noite, nas águas do Recife de Coral."),
        new Species("Atum-Azul", Boat.SEA, 0, 0, ALL_YEAR, 5, 19, ANY, 0, new Color(50, 90, 180), 1.4,
            "Qualquer estação, de dia, no Alto-mar (de barco)."),
        new Species("Peixe-Espada", Boat.SEA, 0, 0, SUMMER | AUTUMN, 6, 20, ANY, 0, new Color(176, 186, 206), 1.5,
            "Verão e outono, de dia, no Alto-mar (de barco)."),
        new Species("Lula-Gigante", Boat.SEA, 0, 0, ALL_YEAR, 19, 5, ANY, 0, new Color(222, 112, 142), 1.5,
            "Qualquer estação, à noite, no Alto-mar (de barco)."),
        new Species("Peixe-Lua", Boat.SEA, 0, 0, WINTER | SPRING, 8, 17, ANY, 0, new Color(206, 206, 222), 1.3,
            "Inverno e primavera, de dia, no Alto-mar (de barco).")
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
    static final Species[] FOSSILS = {
        fossil("Amonite", "Na areia da Praia Azul e da Ilha da Tartaruga."),
        fossil("Dente de Tubarão Antigo", "Na areia da Praia Azul e da Ilha da Tartaruga."),
        fossil("Trilobita", "Na terra da Trilha da Montanha e do Vale dos Ecos."),
        fossil("Pena de Pedra", "No chão da Floresta Verde e do Vale dos Ecos."),
        fossil("Folha Fóssil", "No chão da vila e da Floresta Verde."),
        fossil("Ovo de Pedra", "Na terra da vila e da Trilha da Montanha."),
        fossil("Casco Antigo", "Só na Ilha da Tartaruga."),
        fossil("Crânio Estelar", "Só na Ilha da Tartaruga.")
    };
    static final String[] FOSSIL_NOTES = {
        "Uma amonite! Esses caracóis do mar viviam aqui quando Aurora inteira era fundo de oceano.",
        "Olhe o tamanho deste dente! O tubarão dono dele devia ser maior que o barco da Capitã.",
        "Trilobitas andavam pelo fundo do mar muito antes dos peixes. Este está perfeito!",
        "Uma pena guardada na pedra... Então já havia aves voando sobre Aurora há tanto tempo!",
        "Repare nas nervuras da folha. Esta árvore é parente distante da Grande Árvore.",
        "Um ovo de pedra! Nunca vamos saber o que ia nascer dele. Talvez seja melhor assim.",
        "O casco de uma tartaruga antiga. A Vovó Tartaruga vai gostar de saber que tem um parente no museu.",
        "Um crânio com marcas de estrela... Os peixes-estrela do mito eram de verdade!"
    };
    static Species fossil(String name, String hint) {
        return new Species(name, "", 0, 0, ALL_YEAR, 0, 24, ANY, 0, new Color(214, 200, 172), 0, hint);
    }
    /** Lugares onde a terra pode brilhar (cerca de um terço por dia) e os fósseis que guardam. */
    record Spot(String map, int x, int y, int[] fossils) {}
    static final Spot[] SPOTS = {
        new Spot("Praia Azul", 3, 11, new int[]{0, 1}), new Spot("Praia Azul", 25, 12, new int[]{1, 0}), new Spot("Praia Azul", 15, 6, new int[]{0, 1}),
        new Spot("Vila Aurora", 6, 19, new int[]{4, 5}), new Spot("Vila Aurora", 21, 19, new int[]{5, 4}),
        new Spot("Floresta Verde", 7, 13, new int[]{3, 4}), new Spot("Floresta Verde", 25, 3, new int[]{4, 3}),
        new Spot("Trilha da Montanha", 19, 10, new int[]{2, 5}), new Spot("Trilha da Montanha", 3, 29, new int[]{5, 2}),
        new Spot(ChapterEight.VALLEY, 20, 15, new int[]{2, 3}), new Spot(ChapterEight.VALLEY, 4, 10, new int[]{3, 2}),
        new Spot(Boat.TURTLE, 5, 12, new int[]{6, 0}), new Spot(Boat.TURTLE, 17, 3, new int[]{7, 1}), new Spot(Boat.TURTLE, 18, 12, new int[]{6, 7})
    };
    static final int[] REWARD_COINS = {20, 20, 30};
    static final double RARE_CHANCE = 0.25, RARE_LUCK = 0.4, RARE_ONLY = 0.7, FOSSIL_CHANCE = 0.6;
    static final int SHOVEL_AFTER = 3;

    final Game game;
    Random random = new Random();
    boolean net, shovel;
    final int[] fish = new int[FISH.length], bugs = new int[BUGS.length], fossils = new int[FOSSILS.length];
    final int[] bugDay = new int[BUGS.length], dugDay = new int[SPOTS.length];
    final boolean[][] given = {new boolean[FISH.length], new boolean[BUGS.length], new boolean[FOSSILS.length]};
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
    /** Peixe raro que morde agora neste mapa (sorteado ao lançar a linha) ou -1. Onde não há peixe comum, morde mais. */
    int roll(boolean commonHere) {
        List<Integer> here = new ArrayList<>();
        for (int i = 0; i < FISH.length; i++) if (FISH[i].map().equals(game.map.name) && present(FISH[i])) here.add(i);
        if (here.isEmpty()) return -1;
        double chance = !commonHere ? RARE_ONLY : game.cooking.active(Cooking.LUCK) ? RARE_LUCK : RARE_CHANCE;
        return random.nextDouble() < chance ? here.get(random.nextInt(here.size())) : -1;
    }
    String caughtFish(int i) {
        boolean first = fish[i] == 0;
        fish[i] = Math.min(999999, fish[i] + 1);
        return (first ? "Peixe raro! " : "Você pescou: ") + FISH[i].name() + "!" + (first ? " Leve ao Museu de Aurora." : "");
    }
    static Species[] list(int wing) { return wing == FISH_WING ? FISH : wing == BUG_WING ? BUGS : FOSSILS; }
    int[] found(int wing) { return wing == FISH_WING ? fish : wing == BUG_WING ? bugs : fossils; }
    boolean donatedFish(int i) { return given[FISH_WING][i]; }
    boolean donatedBug(int i) { return given[BUG_WING][i]; }
    boolean donatedFossil(int i) { return given[FOSSIL_WING][i]; }
    int foundCount(int wing) { return (int) Arrays.stream(found(wing)).filter(c -> c > 0).count(); }
    int fishFound() { return foundCount(FISH_WING); }
    int bugsFound() { return foundCount(BUG_WING); }
    boolean allFish() { return fishFound() == FISH.length; }
    boolean allBugs() { return bugsFound() == BUGS.length; }
    boolean allFossils() { return foundCount(FOSSIL_WING) == FOSSILS.length; }
    boolean wing(int w) { for (boolean b : given[w]) if (!b) return false; return true; }
    boolean fishWing() { return wing(FISH_WING); }
    boolean bugWing() { return wing(BUG_WING); }
    boolean fossilWing() { return wing(FOSSIL_WING); }
    boolean complete() { return fishWing() && bugWing() && fossilWing(); }
    int donatedCount() { int n = 0; for (boolean[] w : given) for (boolean b : w) if (b) n++; return n; }
    int total() { return FISH.length + BUGS.length + FOSSILS.length; }
    boolean open() { return game.stage >= 4; }

    /** Algo que aparece hoje, para as notícias do rádio (ou null). */
    String rumor(int day) {
        List<Species> list = new ArrayList<>(Arrays.asList(FISH)); list.addAll(Arrays.asList(BUGS));
        list.removeIf(s -> game.stage < s.stage() || (s.seasons() & (1 << game.seasons.current())) == 0
            || s.weather() >= 0 && s.weather() != game.weather.current() || s.map().equals(Boat.SEA) && !game.boat.owned);
        return list.isEmpty() ? null : list.get(Math.floorMod(day * 7 + 3, list.size())).hint();
    }

    /** A terra deste lugar brilha hoje? (um terço dos lugares por dia, sem repetir o padrão da véspera) */
    static boolean active(int spot, int day) { return Math.floorMod(day * 7 + spot * 5, 3) == 0; }

    // ---------- Mundo ----------
    @Override public void reset() {
        net = false; shovel = false;
        Arrays.fill(fish, 0); Arrays.fill(bugs, 0); Arrays.fill(fossils, 0); Arrays.fill(bugDay, -1); Arrays.fill(dugDay, -1);
        for (boolean[] w : given) Arrays.fill(w, false);
        tab = 0; selected = 0; status = "";
    }

    @Override public void buildWorld() {
        game.maps.get("Vila Aurora").addNpc(new Npc("museu", "Museu de Aurora", 27, 7, new Color(190, 170, 140)).obj("museu").still());
        for (int i = 0; i < BUGS.length; i++)
            game.maps.get(BUGS[i].map()).addNpc(new Npc("inseto" + i, BUGS[i].name(), BUGS[i].x(), BUGS[i].y(), BUGS[i].color()).obj("inseto").still());
        for (int i = 0; i < SPOTS.length; i++)
            game.maps.get(SPOTS[i].map()).addNpc(new Npc("escavar" + i, "Terra brilhando", SPOTS[i].x(), SPOTS[i].y(), new Color(200, 170, 110)).obj("escavar").still());
    }

    @Override public boolean visible(Npc n) {
        if (n.id.startsWith("inseto")) {
            int i = Integer.parseInt(n.id.substring(6));
            return bugDay[i] != game.day() && present(BUGS[i]);
        }
        if (n.id.startsWith("escavar")) {
            int i = Integer.parseInt(n.id.substring(7));
            return shovel && dugDay[i] != game.day() && active(i, game.day());
        }
        return true;
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
        if (n.id.startsWith("escavar")) { dig(Integer.parseInt(n.id.substring(7))); return true; }
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
        if (!shovel && donatedCount() >= SHOVEL_AFTER) {
            shovel = true;
            game.audio.sfx("fanfare");
            game.say("Prof. Carvalho", "Você está enchendo as vitrines! Hora de abrir a terceira ala: a dos fósseis.",
                "Leve esta pá. Onde a terra brilhar, cave com Z. O chão de Aurora guarda bichos de muito antes da vila.",
                "Na praia, na floresta, na montanha, no vale... e dizem que a ilhota da tartaruga está cheia deles.",
                "Você ganhou a Pá do Professor!");
            game.afterDlg = this::openScreen;
            game.save(false);
            return true;
        }
        openScreen();
        return true;
    }

    /** Cava num lugar brilhante: um fóssil (de preferência um que falta) ou algumas moedas. */
    void dig(int i) {
        dugDay[i] = game.day();
        int[] pool = SPOTS[i].fossils();
        if (random.nextDouble() < FOSSIL_CHANCE) {
            int pick = pool[random.nextInt(pool.length)];
            for (int f : pool) if (fossils[f] == 0) { pick = f; break; }
            boolean first = fossils[pick] == 0;
            fossils[pick] = Math.min(999999, fossils[pick] + 1);
            game.audio.sfx(first ? "fanfare" : "pickup");
            game.say("", (first ? "Fóssil novo! " : "Você desenterrou: ") + FOSSILS[pick].name() + "!" + (first ? " Leve ao Museu de Aurora." : ""));
        } else {
            int coins = 5 + random.nextInt(21);
            game.home.coins = Math.min(HomeDecor.MAX_COINS, game.home.coins + coins);
            game.audio.sfx("pickup");
            game.say("", "Só terra e umas moedas antigas: +" + coins + " moedas.");
        }
        game.save(false);
    }

    void openScreen() { selected = Math.min(selected, count() - 1); status = ""; returnState = Game.PLAY; game.down.clear(); game.state = Game.MUSEUM; }
    void openFromMenu() { openScreen(); returnState = Game.MENU; }

    // ---------- Tela ----------
    int wings() { return shovel ? 3 : 2; }
    int count() { return list(tab).length; }
    int cols() { return tab == BUG_WING ? 5 : 4; }
    @Override public boolean owns(int state) { return state == Game.MUSEUM; }
    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        int old = selected, cols = cols();
        if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_TAB || key == KeyEvent.VK_Q || key == KeyEvent.VK_E) {
            tab = (tab + (key == KeyEvent.VK_Q ? wings() - 1 : 1)) % wings(); selected = 0; status = ""; game.audio.sfx("select"); return;
        }
        if (key == KeyEvent.VK_LEFT) selected = (selected + count() - 1) % count();
        else if (key == KeyEvent.VK_RIGHT) selected = (selected + 1) % count();
        else if (key == KeyEvent.VK_UP) selected = (selected + count() - cols) % count();
        else if (key == KeyEvent.VK_DOWN) selected = (selected + cols) % count();
        else if (act) donate();
        if (selected != old) { game.audio.sfx("select"); status = ""; }
    }

    void donate() {
        if (returnState == Game.MENU) { status = "Para doar, vá até o Museu de Aurora, ao lado do observatório."; return; }
        int i = selected, w = tab;
        if (given[w][i]) { status = "Esta espécie já está na vitrine."; return; }
        if (found(w)[i] == 0) { status = "Você ainda não encontrou esta espécie."; return; }
        boolean wingBefore = wing(w);
        int oldCoins = game.home.coins;
        given[w][i] = true;
        game.home.coins = Math.min(HomeDecor.MAX_COINS, game.home.coins + REWARD_COINS[w]);
        if (!game.save(false)) { given[w][i] = false; game.home.coins = oldCoins; status = "Não foi possível salvar a doação."; return; }
        game.audio.sfx("fanfare");
        String name = list(w)[i].name();
        status = w == FOSSIL_WING ? Lang.done(Lang.t("Prof. Carvalho:") + " " + Lang.t(FOSSIL_NOTES[i]) + " (+" + REWARD_COINS[w] + ")")
            : Lang.done(Lang.t("Doado:") + " " + Lang.t(name) + ". " + Lang.t("O professor deixou " + REWARD_COINS[w] + " moedas de agradecimento."));
        if (!wingBefore && wing(w)) status = switch (w) {
            case FISH_WING -> "Ala dos peixes completa! Um Aquário Raro foi para a sua casa.";
            case BUG_WING -> "Ala dos insetos completa! Um Quadro de Insetos foi para a sua casa.";
            default -> "Ala dos fósseis completa! Um Esqueleto em Miniatura foi para a sua casa.";
        };
    }

    void drawIcon(Graphics2D g, int wing, int i, int cx, int cy, Color c) {
        Graphics2D b = (Graphics2D) g.create();
        if (wing == FISH_WING) { game.life.drawFish(b, cx - 18, cy - 8, 30, c); b.dispose(); return; }
        b.translate(cx - 21, cy - 21); b.scale(1.3, 1.3);
        if (wing == BUG_WING) drawBug(b, i, 0, 0, c, false); else drawFossil(b, i, 0, 0, c);
        b.dispose();
    }

    @Override public void draw(Graphics2D g) {
        game.life.panel(g, "Museu de Aurora", "Tab: trocar de ala · Setas: escolher · Z: doar · Esc: sair");
        game.life.text(g, "Doadas: " + donatedCount() + "/" + total(), 470, 62, 130, 12);
        String[] tabs = {"Peixes raros " + fishFound() + "/" + FISH.length, "Insetos " + bugsFound() + "/" + BUGS.length,
            "Fósseis " + foundCount(FOSSIL_WING) + "/" + FOSSILS.length};
        for (int t = 0; t < wings(); t++) {
            g.setColor(t == tab ? new Color(60, 120, 120) : new Color(205, 220, 214)); g.fillRoundRect(44 + t * 186, 108, 178, 26, 8, 8);
            g.setColor(t == tab ? Color.WHITE : new Color(60, 80, 80)); g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(Lang.t(tabs[t]), 56 + t * 186, 126);
        }
        Species[] list = list(tab);
        int cols = cols(), cell = 552 / cols, rows = (list.length + cols - 1) / cols;
        for (int i = 0; i < list.length; i++) {
            int x = 44 + (i % cols) * cell, y = 142 + (i / cols) * 56;
            boolean have = found(tab)[i] > 0, done = given[tab][i];
            g.setColor(done ? new Color(214, 236, 214) : new Color(232, 238, 236)); g.fillRoundRect(x, y, cell - 8, 50, 8, 8);
            if (i == selected) { g.setColor(new Color(200, 60, 50)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(x, y, cell - 8, 50, 8, 8); g.setStroke(new BasicStroke(1)); }
            drawIcon(g, tab, i, x + (cell - 8) / 2, y + 25, have ? list[i].color() : new Color(170, 184, 180));
            if (done) { g.setColor(new Color(60, 140, 80)); g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.drawString(Lang.t("✓"), x + cell - 26, y + 18); }
        }
        Species s = list[selected];
        boolean have = found(tab)[selected] > 0;
        int y0 = 142 + rows * 56 + 4;
        game.life.text(g, have ? s.name() : "???", 48, y0 + 14, 540, 16);
        game.life.text(g, Lang.done(Lang.t("Onde:") + " " + Lang.t(s.hint())), 48, y0 + 36, 540, 12);
        game.life.text(g, have ? "Encontrados: " + found(tab)[selected] : "Ainda não encontrado.", 48, y0 + 58, 300, 12);
        if (!net && tab == BUG_WING) game.life.text(g, "Pegue a rede na porta do museu.", 330, y0 + 58, 260, 12);
        game.life.text(g, status, 48, y0 + 80, 540, 12);
    }

    // ---------- Desenho ----------
    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.startsWith("escavar")) {
            g.setColor(new Color(120, 90, 60, 160)); g.fillOval(x + 7, y + 20, 18, 7);
            g.setColor(new Color(90, 66, 44)); g.drawLine(x + 11, y + 23, x + 16, y + 21); g.drawLine(x + 16, y + 21, x + 21, y + 24);
            double tw = game.access.reduced ? 1 : 0.5 + 0.5 * Math.sin(game.frame / 10.0 + n.x);
            g.setColor(new Color(255, 240, 160, (int) (120 + 135 * tw)));
            int sx = x + 16, sy = y + 14, r = (int) (3 + 3 * tw);
            g.fillRect(sx - 1, sy - r, 2, r * 2); g.fillRect(sx - r, sy - 1, r * 2, 2);
            return true;
        }
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

    /** Fóssil 32x32: amonite, dente, trilobita, pena, folha, ovo, casco e crânio. */
    void drawFossil(Graphics2D g, int i, int x, int y, Color c) {
        Color dark = c.darker(), deep = dark.darker();
        Stroke old = g.getStroke();
        switch (i) {
            case 0 -> { g.setColor(c); g.fillOval(x + 6, y + 6, 20, 20); g.setColor(deep); g.setStroke(new BasicStroke(2));
                g.drawArc(x + 8, y + 8, 16, 16, 90, 300); g.drawArc(x + 11, y + 11, 10, 10, 30, 300); g.drawArc(x + 14, y + 14, 4, 4, 0, 300); }
            case 1 -> { g.setColor(c); g.fillPolygon(new int[]{x + 9, x + 23, x + 16}, new int[]{y + 24, y + 24, y + 5}, 3);
                g.setColor(dark); g.fillRect(x + 9, y + 23, 14, 3); g.drawLine(x + 16, y + 8, x + 13, y + 22); }
            case 2 -> { g.setColor(c); g.fillOval(x + 8, y + 5, 16, 22); g.setColor(deep);
                for (int k = 0; k < 5; k++) g.drawLine(x + 9, y + 10 + k * 3, x + 23, y + 10 + k * 3);
                g.drawLine(x + 16, y + 6, x + 16, y + 26); g.fillOval(x + 12, y + 5, 8, 5); }
            case 3 -> { g.setColor(c); g.fillOval(x + 9, y + 4, 12, 24); g.setColor(deep); g.setStroke(new BasicStroke(2));
                g.drawLine(x + 15, y + 6, x + 17, y + 29); g.setStroke(old);
                for (int k = 0; k < 6; k++) { g.drawLine(x + 15, y + 9 + k * 3, x + 10, y + 7 + k * 3); g.drawLine(x + 16, y + 9 + k * 3, x + 21, y + 7 + k * 3); } }
            case 4 -> { g.setColor(c); g.fillOval(x + 6, y + 6, 20, 20); g.setColor(deep);
                g.drawLine(x + 8, y + 24, x + 24, y + 8);
                for (int k = 0; k < 4; k++) { int bx = x + 11 + k * 4, by = y + 21 - k * 4; g.drawLine(bx, by, bx - 3, by - 5); g.drawLine(bx, by, bx + 5, by + 3); } }
            case 5 -> { g.setColor(c); g.fillOval(x + 9, y + 5, 14, 20); g.setColor(dark); g.drawLine(x + 12, y + 14, x + 15, y + 17); g.drawLine(x + 15, y + 17, x + 19, y + 13);
                g.setColor(deep); g.fillRect(x + 6, y + 24, 20, 3); }
            case 6 -> { g.setColor(c); g.fillOval(x + 4, y + 8, 24, 17); g.setColor(deep);
                g.drawPolygon(new int[]{x + 12, x + 20, x + 23, x + 16, x + 9}, new int[]{y + 11, y + 11, y + 17, y + 22, y + 17}, 5);
                g.drawLine(x + 12, y + 11, x + 7, y + 13); g.drawLine(x + 20, y + 11, x + 25, y + 13); }
            default -> { g.setColor(c); g.fillOval(x + 6, y + 6, 20, 16); g.fillRect(x + 10, y + 18, 12, 8);
                g.setColor(deep); g.fillOval(x + 10, y + 11, 5, 5); g.fillOval(x + 17, y + 11, 5, 5);
                for (int k = 0; k < 3; k++) g.fillRect(x + 12 + k * 3, y + 22, 2, 4);
                g.setColor(new Color(255, 230, 140)); g.fillRect(x + 15, y + 6, 2, 4); g.fillRect(x + 14, y + 7, 4, 2); }
        }
        g.setStroke(old);
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
        p.setProperty("museu.rede", "" + net); p.setProperty("museu.pa", "" + shovel);
        String[] keys = {"peixe", "inseto", "fossil"};
        for (int w = 0; w < 3; w++) for (int i = 0; i < given[w].length; i++) {
            p.setProperty("museu." + keys[w] + "." + i, "" + found(w)[i]);
            p.setProperty("museu.doado." + keys[w] + "." + i, "" + given[w][i]);
        }
        for (int i = 0; i < BUGS.length; i++) p.setProperty("museu.inseto_dia." + i, "" + (bugDay[i] + 1));      // 0: nunca
        for (int i = 0; i < SPOTS.length; i++) p.setProperty("museu.cavado." + i, "" + (dugDay[i] + 1));
    }
    @Override public void load(Properties p) {
        net = Boolean.parseBoolean(p.getProperty("museu.rede", "false"));
        shovel = Boolean.parseBoolean(p.getProperty("museu.pa", "false"));
        String[] keys = {"peixe", "inseto", "fossil"};
        int legacy = game.life.number(p, "museu.doado", 0, (1 << 18) - 1);                   // versão anterior: bits num número
        for (int w = 0; w < 3; w++) for (int i = 0; i < given[w].length; i++) {
            found(w)[i] = game.life.number(p, "museu." + keys[w] + "." + i, 0, 999999);
            String flag = p.getProperty("museu.doado." + keys[w] + "." + i);
            boolean old = w == FISH_WING && i < 8 && (legacy & (1 << i)) != 0 || w == BUG_WING && (legacy & (1 << (8 + i))) != 0;
            given[w][i] = (flag != null ? Boolean.parseBoolean(flag) : old) && found(w)[i] > 0;      // não doa o que não tem
        }
        for (int i = 0; i < BUGS.length; i++) bugDay[i] = game.life.number(p, "museu.inseto_dia." + i, 0, Integer.MAX_VALUE - 1) - 1;
        for (int i = 0; i < SPOTS.length; i++) dugDay[i] = game.life.number(p, "museu.cavado." + i, 0, Integer.MAX_VALUE - 1) - 1;
        tab = Math.min(tab, wings() - 1); selected = Math.min(selected, count() - 1);
    }
    @Override public String signature() {
        return "m" + net + shovel + Arrays.deepToString(given) + Arrays.toString(fish) + Arrays.toString(bugs) + Arrays.toString(fossils)
            + Arrays.toString(bugDay) + Arrays.toString(dugDay);
    }
}
