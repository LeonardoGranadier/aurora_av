import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

/**
 * Quem joga: nome e tratamento escolhidos no novo jogo, tempo de jogo, conquistas e o Cartão do Explorador (tecla C).
 * Inspirado na escolha de personagem e no cartão de treinador dos Pokémon de Game Boy.
 */
final class Explorer implements Feature {
    static final int BOY = 0, GIRL = 1, NEUTRAL = 2, UNSET = -1;
    static final String[] FORMS = {"Menino", "Menina", "Prefiro não dizer"};
    static final int MAX_NAME = 12;

    record Achievement(String id, String name, String description, Predicate<Game> done) {}
    static final List<Achievement> ACHIEVEMENTS = List.of(
        new Achievement("farol", "Luz do Farol", "Devolveu a Concha Brilhante ao farol.", g -> g.stage >= 4),
        new Achievement("mapa", "Cartógrafo da Capitã", "Reuniu os três pedaços do Mapa Estrelado.", g -> g.stage >= 7),
        new Achievement("marujo", "Marujo", "Zarpou para a Ilha das Estrelas.", g -> g.stage >= 11),
        new Achievement("estrela_mar", "Guardião do Mar", "Levou a Estrela do Mar ao topo do farol.", g -> g.stage >= 15),
        new Achievement("cintila", "Amizade Estelar", "Ajudou a Cintila a voltar para o céu.", g -> g.stage >= 21),
        new Achievement("jardim", "Laços entre Céu e Terra", "Fez o Jardim das Constelações florescer.", g -> g.stage >= 28),
        new Achievement("album", "Mestre Pescador", "Registrou as oito espécies de Aurora.", g -> g.life.completeAlbum()),
        new Achievement("sinos", "Ouvido de Ouro", "Repetiu as cinco melodias dos sinos.", g -> g.life.bellBest >= VillageLife.BELL_ROUNDS),
        new Achievement("festival", "Estrela do Festival", "Venceu as duas provas numa mesma festa.", g -> g.festival.prizes > 0),
        new Achievement("amizade", "Coração da Vila", "Chegou a cinco corações com um morador.", g -> Arrays.stream(g.bonds.friendship).anyMatch(f -> f >= 100)),
        new Achievement("pedidos", "Mão Amiga", "Concluiu os quatro pedidos dos moradores.", g -> Arrays.stream(g.bonds.quests).allMatch(q -> q == 2)),
        new Achievement("encomendas", "Vizinhança Prestativa", "Entregou dez encomendas do mural.", g -> g.orders.total >= 10),
        new Achievement("regioes", "Pés na Estrada", "Visitou todas as regiões de Aurora.", g -> g.worldMap.visited.size() == WorldMap.REGIONS.length),
        new Achievement("conchas", "Ouvido de Concha", "Encontrou todas as conchas secretas.", g -> g.shells.found() == SecretShells.SPOTS.length),
        new Achievement("trocas", "Rei das Trocas", "Completou a corrente de trocas da vila.", g -> g.trades.complete()),
        new Achievement("observatorio", "Céu Restaurado", "Restaurou o observatório do professor.", g -> g.observatory.complete()),
        new Achievement("mare", "Canção da Maré", "Trouxe a baleia guardiã de volta para a baía.", g -> g.stage >= ChapterSix.DONE),
        new Achievement("festival_ceu", "Festival do Céu", "Reuniu Aurora inteira no alto do Pico Estrelado.", g -> g.stage >= ChapterSeven.FINAL),
        new Achievement("melodia", "Melodia da Semana", "Trouxe a Lia de volta para o Lucas.", g -> g.stage >= ChapterEight.FINAL),
        new Achievement("guardiao", "Guardião da Aurora", "Desfez o eclipse com a Grande Árvore.", g -> g.stage >= ChapterNine.HOME),
        new Achievement("enigmas", "Mestre dos Enigmas", "Abriu os três baús escondidos de Aurora.", g -> g.puzzles.openedCount() == Puzzles.CHESTS.length),
        new Achievement("ano", "Um Ano em Aurora", "Viveu as quatro estações da vila.", g -> g.day() + 1 > Seasons.LENGTH * 3),
        new Achievement("chef", "Chef de Aurora", "Cozinhou todas as receitas.", g -> g.cooking.cooked.size() == Cooking.RECIPES.length),
        new Achievement("aniversario", "Quem Ama Lembra", "Deu um presente de aniversário a um morador.", g -> !g.seasons.birthdayGifts.isEmpty()),
        new Achievement("gato", "Amigo do Bolinha", "Fez carinho no Bolinha em sete dias diferentes.", g -> g.cat.petDays >= 7)
    );

    final Game game;
    String name = "";
    int form = UNSET;
    double playSeconds, checkTimer;
    final Set<String> unlocked = new TreeSet<>();
    final Deque<Achievement> announce = new ArrayDeque<>();
    // tela de nome
    int step, formSel, cardSel, returnState = Game.PLAY;
    boolean legacy, asked;   // legacy: partida antiga escolhendo nome agora; asked: a Mamãe já perguntou

    Explorer(Game game) { this.game = game; }

    // ---------- Textos ----------
    /** Escolhe a forma do texto conforme o tratamento; partidas antigas usam a forma neutra. */
    String w(String boy, String girl, String neutral) { return form == BOY ? boy : form == GIRL ? girl : neutral; }
    /** Nome do jogador, ou a alternativa para partidas antigas sem nome. */
    String nameOr(String fallback) { return name.isBlank() ? fallback : name; }

    // ---------- Ciclo do jogo ----------
    @Override public void reset() { asked = false; legacy = false; name = ""; form = UNSET; playSeconds = 0; unlocked.clear(); announce.clear(); cardSel = 0; }
    @Override public void save(Properties p) {
        p.setProperty("explorador.nome", name);
        p.setProperty("explorador.forma", "" + form);
        p.setProperty("explorador.tempo", "" + (long) playSeconds);
        p.setProperty("explorador.conquistas", String.join(",", unlocked));
        p.setProperty("explorador.perguntado", "" + asked);
    }
    @Override public void load(Properties p) {
        name = clean(p.getProperty("explorador.nome", ""));
        asked = Boolean.parseBoolean(p.getProperty("explorador.perguntado", "false"));
        if (!name.isBlank()) Lang.addNames(name);
        form = parseForm(p);
        try { playSeconds = Math.max(0, Long.parseLong(p.getProperty("explorador.tempo", "0"))); } catch (NumberFormatException e) { playSeconds = 0; }
        unlocked.clear();
        for (String id : p.getProperty("explorador.conquistas", "").split(","))
            if (ACHIEVEMENTS.stream().anyMatch(a -> a.id().equals(id))) unlocked.add(id);
        for (Achievement a : ACHIEVEMENTS) if (a.done().test(game)) unlocked.add(a.id());   // conquistas antigas, sem aviso
        announce.clear();
    }
    int parseForm(Properties p) {
        try { int f = Integer.parseInt(p.getProperty("explorador.forma", "-1")); return f >= BOY && f <= NEUTRAL ? f : UNSET; }
        catch (NumberFormatException e) { return UNSET; }
    }
    static String clean(String raw) {
        StringBuilder b = new StringBuilder();
        for (char c : raw.toCharArray()) if (b.length() < MAX_NAME && (Character.isLetter(c) || c == ' ' || c == '-')) b.append(c);
        return b.toString().trim();
    }
    @Override public String signature() { return name + form + unlocked; }

    @Override public void update(double dt) {
        playSeconds += dt;
        checkTimer -= dt;
        if (checkTimer <= 0) {
            checkTimer = 0.5;
            for (Achievement a : ACHIEVEMENTS)
                if (!unlocked.contains(a.id()) && a.done().test(game)) { unlocked.add(a.id()); announce.add(a); }
        }
        if (!announce.isEmpty() && game.toastT <= 0 && game.dlg == null) {
            game.toast("Conquista: " + announce.poll().name() + "  (C)");
            game.audio.sfx("pickup");
        }
    }

    // ---------- Novo jogo: nome e tratamento ----------
    void startNaming() { legacy = false; step = 0; formSel = 0; name = ""; form = UNSET; game.down.clear(); game.state = Game.NAMING; }
    /** Partida antiga sem nome: a Mamãe pergunta uma vez. Esc mantém o tratamento neutro. */
    void askLegacy() { startNaming(); legacy = true; asked = true; }

    // ---------- Cartão ----------
    void openCard(int from) { returnState = from; game.down.clear(); game.state = Game.CARD; }

    @Override public boolean owns(int state) { return state == Game.NAMING || state == Game.CARD; }

    @Override public void typed(char c) {
        if (game.state != Game.NAMING || step != 0) return;
        if ((Character.isLetter(c) || c == ' ' || c == '-') && name.length() < MAX_NAME && !(c == ' ' && name.isEmpty())) {
            name += c; game.audio.sfx("select");
        }
    }

    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        if (game.state == Game.CARD) {
            int n = ACHIEVEMENTS.size(), old = cardSel;
            if (key == KeyEvent.VK_LEFT) cardSel = (cardSel + n - 1) % n;
            else if (key == KeyEvent.VK_RIGHT) cardSel = (cardSel + 1) % n;
            else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) cardSel = (cardSel + (n + 1) / 2) % n;
            else if (act || key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_C) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
            if (cardSel != old) game.audio.sfx("select");
            return;
        }
        if (step == 0) {
            if (key == KeyEvent.VK_BACK_SPACE && !name.isEmpty()) { name = name.substring(0, name.length() - 1); game.audio.sfx("back"); }
            else if (key == KeyEvent.VK_ESCAPE && legacy) { name = ""; game.audio.sfx("back"); game.state = Game.PLAY; game.save(false); }
            else if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.openTitle(); }
            else if (key == KeyEvent.VK_ENTER && !name.isBlank()) { name = clean(name); Lang.addNames(name); step = 1; game.audio.sfx("confirm"); }
            return;
        }
        if (key == KeyEvent.VK_UP) { formSel = (formSel + FORMS.length - 1) % FORMS.length; game.audio.sfx("select"); }
        else if (key == KeyEvent.VK_DOWN) { formSel = (formSel + 1) % FORMS.length; game.audio.sfx("select"); }
        else if (key == KeyEvent.VK_ESCAPE) { step = 0; game.audio.sfx("back"); }
        else if (act) {
            form = formSel; game.audio.sfx("confirm");
            game.state = Game.PLAY; game.fade = 1; game.down.clear();
            game.save(false);
        }
    }

    // ---------- Desenho ----------
    @Override public void draw(Graphics2D g) {
        if (game.state == Game.NAMING) drawNaming(g); else drawCard(g);
    }

    void drawNaming(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(12, 16, 52), 0, Game.H, new Color(60, 70, 130)));
        g.fillRect(0, 0, Game.W, Game.H);
        Random r = new Random(9);
        for (int i = 0; i < 60; i++) { g.setColor(new Color(255, 255, 255, 60 + r.nextInt(140))); g.fillRect(r.nextInt(Game.W), r.nextInt(Game.H), 2, 2); }
        java.awt.geom.AffineTransform before = g.getTransform();
        g.translate(Game.W / 2 - 32, 62); g.scale(2, 2);
        game.person(g, "mae", 0, 0, new Color(200, 90, 150), 0, false, 0, true);
        g.setTransform(before);
        g.setColor(new Color(255, 255, 255, 240)); g.fillRoundRect(60, 150, Game.W - 120, 250, 18, 18);
        g.setColor(new Color(40, 40, 60)); g.setStroke(new BasicStroke(4)); g.drawRoundRect(60, 150, Game.W - 120, 250, 18, 18); g.setStroke(new BasicStroke(1));
        g.setFont(new Font("SansSerif", Font.PLAIN, 17)); g.setColor(new Color(30, 30, 40));
        if (step == 0) {
            game.center(g, legacy ? "Mamãe: \"Que cabeça a minha... me diga de novo o seu nome?\"" : "Mamãe: \"Bom dia, meu amor! Como é mesmo o seu nome?\"", 192);
            g.setColor(new Color(235, 240, 250)); g.fillRoundRect(170, 222, 300, 44, 10, 10);
            g.setColor(new Color(40, 40, 60)); g.setFont(new Font("SansSerif", Font.BOLD, 24));
            String cursor = (game.frame / 30) % 2 == 0 ? "_" : " ";
            game.center(g, name + (name.length() < MAX_NAME ? cursor : ""), 254);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(90, 90, 110));
            game.center(g, "Digite o nome (até " + MAX_NAME + " letras) · Backspace apaga · Enter confirma · Esc " + (legacy ? "pula" : "volta"), 300);
            game.center(g, "Seu nome aparece nos diálogos e no Cartão do Explorador.", 322);
        } else {
            game.center(g, "Mamãe: \"" + name + "... que nome lindo!", 184);
            game.center(g, "E como você prefere que as pessoas te chamem?\"", 206);
            g.setFont(new Font("SansSerif", Font.BOLD, 19));
            for (int i = 0; i < FORMS.length; i++) {
                g.setColor(i == formSel ? new Color(200, 60, 50) : new Color(40, 40, 60));
                game.center(g, (i == formSel ? "▶ " : "") + FORMS[i], 248 + i * 34);
            }
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(90, 90, 110));
            game.center(g, "Muda apenas o jeito como os personagens se referem a você.", 360);
            game.center(g, "Setas escolhem · Z/Enter confirma · Esc volta ao nome", 380);
        }
    }

    static String clock(double seconds) {
        long minutes = (long) seconds / 60;
        return String.format("%d:%02d", minutes / 60, minutes % 60);
    }

    void drawCard(Graphics2D g) {
        game.life.panel(g, "Cartão do Explorador", "Setas: ver conquistas · Z / Esc / C: fechar");
        // moldura do cartão
        g.setColor(new Color(214, 232, 245)); g.fillRoundRect(44, 112, 552, 150, 14, 14);
        g.setColor(new Color(92, 132, 168)); g.setStroke(new BasicStroke(2)); g.drawRoundRect(44, 112, 552, 150, 14, 14); g.setStroke(new BasicStroke(1));
        g.setColor(new Color(250, 252, 255)); g.fillRoundRect(60, 128, 88, 104, 10, 10);
        java.awt.geom.AffineTransform before = g.getTransform();
        g.translate(72, 140); g.scale(2, 2);
        game.person(g, "player", 0, 0, new Color(220, 50, 50), 0, false, 0, true);
        g.setTransform(before);
        int day = (int) (game.time / 1440) + 1;
        String[] rows = {
            "Nome: " + nameOr("Explorador(a)"),
            "Tempo de jogo: " + clock(playSeconds) + "      " + game.seasons.name() + " " + Seasons.dayOfSeason(day) + " (" + Game.weekday(day) + "), ano " + Seasons.year(day),
            "Capítulo " + chapter() + "      Moedas: " + game.home.coins,
            "Álbum: " + Arrays.stream(game.life.catches).filter(c -> c > 0).count() + "/8 espécies      Regiões: "
                + game.worldMap.visited.size() + "/" + WorldMap.REGIONS.length,
            "Conchas secretas: " + game.shells.found() + "/" + SecretShells.SPOTS.length + "      Conquistas: " + unlocked.size() + "/" + ACHIEVEMENTS.size(),
            game.trades.complete() ? Lang.done(Lang.t("Corrente de trocas completa!")) : Lang.done(Lang.t("Troca:") + " "
                + (game.trades.item() == null ? "" : Lang.t(game.trades.item()) + ". ") + Lang.t(game.trades.hint()))
        };
        for (int i = 0; i < rows.length - 1; i++) game.life.text(g, rows[i], 166, 142 + i * 22, 420, i == 0 ? 16 : 14);
        game.life.text(g, rows[rows.length - 1], 166, 142 + (rows.length - 1) * 22, 420, 11);
        // medalhas
        int cols = (ACHIEVEMENTS.size() + 1) / 2, step = Math.min(66, 548 / cols);
        for (int i = 0; i < ACHIEVEMENTS.size(); i++) {
            int x = 50 + (i % cols) * step, y = 282 + (i / cols) * 58;
            boolean got = unlocked.contains(ACHIEVEMENTS.get(i).id());
            medal(g, x + 8, y, got, i);
            if (i == cardSel) { g.setColor(new Color(200, 60, 50)); g.setStroke(new BasicStroke(3)); g.drawOval(x + 3, y - 5, 50, 50); g.setStroke(new BasicStroke(1)); }
        }
        Achievement a = ACHIEVEMENTS.get(cardSel);
        boolean got = unlocked.contains(a.id());
        game.life.text(g, (got ? "★ " : "? ") + a.name(), 48, 412, 540, 15);
        game.life.text(g, got ? a.description() : Lang.done(Lang.t("Ainda não conquistada.") + " " + Lang.t(a.description())), 48, 434, 540, 13);
    }

    int chapter() { return chapterOf(game.stage); }
    static int chapterOf(int s) { return s < 4 ? 1 : s < 8 ? 2 : s <= 15 ? 3 : s <= 21 ? 4 : s < 28 ? 5 : s < 32 ? 6 : s < 36 ? 7 : s < 42 ? 8 : 9; }

    void medal(Graphics2D g, int x, int y, boolean got, int index) {
        Color[] colors = {new Color(241, 196, 65), new Color(120, 200, 230), new Color(230, 130, 110), new Color(150, 210, 140)};
        Color c = colors[index % colors.length];
        g.setColor(got ? c.darker() : new Color(190, 198, 205)); g.fillOval(x, y, 40, 40);
        g.setColor(got ? c : new Color(215, 222, 228)); g.fillOval(x + 4, y + 4, 32, 32);
        g.setColor(got ? new Color(255, 255, 255, 230) : new Color(160, 168, 176));
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        String mark = got ? "★" : "?";
        g.drawString(Lang.t(mark), x + 20 - g.getFontMetrics().stringWidth(Lang.t(mark)) / 2, y + 27);
    }
}
