import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/**
 * Rádio de Aurora, como o rádio do Pokégear: um canal de músicas (cada região visitada libera a sua; a faixa
 * escolhida toca em casa) e o Jornal de Aurora, com notícias do dia que mudam com a história, a estação, o clima,
 * os aniversários, o festival e uma dica do museu. Fica na mesa de casa e no menu de pausa.
 */
final class Radio implements Feature {
    record Track(String theme, String title, String region) {}
    static final Track[] TRACKS = {
        new Track("casa", "Lar Doce Lar", null),
        new Track("vila_dia", "Manhã na Vila", null),
        new Track("vila_verao", "Verão na Praça", null),
        new Track("vila_outono", "Folhas de Outono", null),
        new Track("vila_inverno", "Neve no Telhado", null),
        new Track("vila_noite", "Lanternas da Vila", null),
        new Track("floresta_dia", "Trilha Verde", "Floresta Verde"),
        new Track("floresta_noite", "Vagalumes", "Floresta Verde"),
        new Track("praia_dia", "Brisa da Praia", "Praia Azul"),
        new Track("praia_noite", "Ondas à Noite", "Praia Azul"),
        new Track("final", "A Luz do Farol", "Praia Azul"),
        new Track("gruta", "Ecos da Gruta", "Gruta do Eco"),
        new Track("ilha_dia", "Ilha das Estrelas", "Ilha das Estrelas"),
        new Track("templo", "Templo Estrelado", "Ilha das Estrelas"),
        new Track("montanha_dia", "Montanha Névoa", "Trilha da Montanha"),
        new Track("cabana", "Cabana do Eremita", "Trilha da Montanha"),
        new Track("pico", "Pico Estrelado", "Pico Estrelado"),
        new Track("jardim", "Jardim das Constelações", ChapterFive.GARDEN),
        new Track("vale", "Vale dos Ecos", ChapterEight.VALLEY),
        new Track("titulo", "Aventura em Aurora", null)
    };
    static final String[] STORY_NEWS = {
        "O farol da Praia Azul continua apagado. Seu Marinho diz que só uma luz especial pode acendê-lo.",
        "Pescadores contam ter visto uma garrafa boiando na Praia Azul, numa noite de lua.",
        "A Capitã Estrela prepara o barco. Dizem que ela vai atrás de uma ilha que só aparece nos mapas antigos.",
        "Uma estrela cadente riscou o céu na direção da Montanha Névoa. O Prof. Carvalho está animadíssimo.",
        "A Trilha dos Vagalumes voltou a brilhar. Será que o velho jardim ao norte vai florescer de novo?",
        "Os pescadores juram ouvir o mar cantando à noite. A Capitã acha que é uma baleia.",
        "Há convites misteriosos circulando pela vila. Ninguém sabe quem está organizando a festa no Pico.",
        "O Lucas anda pela praça olhando para o sino. Ele diz que a irmã adorava as melodias da semana.",
        "As estrelas andam piscando mais fraco. A Cintila pediu para todos olharem o céu com atenção.",
        "A aurora voltou a dançar sobre a vila! Moradores dormem tarde para não perder nenhuma noite."
    };

    final Game game;
    String track;                  // faixa tocando em casa (null: desligado)
    int channel, selected, returnState = Game.PLAY, scroll;

    Radio(Game game) { this.game = game; }

    @Override public void reset() { track = null; channel = 0; selected = 0; scroll = 0; }

    @Override public void buildWorld() {
        game.maps.get("Casa").addNpc(new Npc("radio", "Rádio", 8, 3, new Color(150, 100, 70)).obj("radio").still());
    }

    @Override public boolean talk(Npc n) {
        if (!n.id.equals("radio")) return false;
        open(Game.PLAY);
        return true;
    }

    void open(int from) { returnState = from; selected = 0; scroll = 0; game.down.clear(); game.state = Game.RADIO; game.audio.sfx("confirm"); }

    boolean unlocked(Track t) { return t.region() == null || game.worldMap.visited.contains(t.region()); }
    int unlockedCount() { return (int) Arrays.stream(TRACKS).filter(this::unlocked).count(); }

    /** Música que o rádio impõe agora (na tela do rádio ou em casa), ou null. */
    String music() {
        if (track == null) return null;
        return game.state == Game.RADIO || game.map != null && game.map.name.equals("Casa") ? track : null;
    }

    // ---------- Jornal ----------
    /** Notícias do dia; cada linha já pode ser desenhada (as partes vêm traduzidas). */
    List<String> news() {
        List<String> lines = new ArrayList<>();
        int day = game.day(), shown = day + 1;
        lines.add("Hoje é " + ChapterEight.DAY_NAMES[Math.floorMod(shown, 7)] + ", dia " + Seasons.dayOfSeason(shown) + ". Estação: " + game.seasons.name() + ".");
        lines.add("Previsão: hoje, " + Weather.NAMES[game.weather.forDay(shown)] + "; amanhã, " + Weather.NAMES[game.weather.forDay(shown + 1)] + ".");
        lines.add(Math.floorMod(shown, 7) == 0 ? "Hoje tem festa na praça: " + game.seasons.festivalName() + "!"
            : "No sábado tem festa na praça: " + game.seasons.festivalName() + ".");
        int who = game.seasons.birthdayOf(shown);
        if (who >= 0) lines.add("Hoje é aniversário de " + Bonds.NAMES[who] + ". Um presente vale em dobro!");
        else for (int ahead = 1; ahead <= 3; ahead++) {
            int next = game.seasons.birthdayOf(shown + ahead);
            if (next >= 0) { lines.add("Daqui a " + ahead + " dia(s) é aniversário de " + Bonds.NAMES[next] + "."); break; }
        }
        lines.add(STORY_NEWS[Math.min(STORY_NEWS.length - 1, Explorer.chapterOf(game.stage) - (game.stage >= Story.FINAL ? 0 : 1))]);
        if (game.stage >= ChapterEight.BELLS) lines.add("O sino da praça toca a melodia de " + ChapterEight.DAY_NAMES[game.chapterEight.weekday()] + " hoje.");
        String rumor = game.museum.open() ? game.museum.rumor(day) : null;
        if (rumor != null) lines.add(Lang.done(Lang.t("Dica do museu:") + " " + Lang.t(rumor)));
        Cooking.Recipe r = Cooking.RECIPES[Math.floorMod(day * 5 + 1, Cooking.RECIPES.length)];
        lines.add("Receita do dia: " + r.name() + ".");
        if (!game.observatory.complete() && game.stage >= 4) lines.add("O Prof. Carvalho ainda procura doações para restaurar o observatório.");
        return lines;
    }

    // ---------- Tela ----------
    @Override public boolean owns(int state) { return state == Game.RADIO; }
    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_X) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_TAB) {
            channel = 1 - channel; selected = 0; scroll = 0; game.audio.sfx("select"); return;
        }
        if (channel == 1) {
            if (key == KeyEvent.VK_UP) scroll = Math.max(0, scroll - 1);
            else if (key == KeyEvent.VK_DOWN) scroll = Math.min(Math.max(0, news().size() - 5), scroll + 1);
            return;
        }
        int count = TRACKS.length + 1, old = selected;                             // + "Desligar"
        if (key == KeyEvent.VK_UP) selected = (selected + count - 1) % count;
        else if (key == KeyEvent.VK_DOWN) selected = (selected + 1) % count;
        else if (act) {
            if (selected == TRACKS.length) { track = null; game.audio.sfx("back"); }
            else if (unlocked(TRACKS[selected])) { track = TRACKS[selected].theme(); game.audio.sfx("confirm"); }
            else game.audio.sfx("back");
            game.save(false);
        }
        if (selected != old) game.audio.sfx("select");
    }

    @Override public void draw(Graphics2D g) {
        game.life.panel(g, "Rádio de Aurora", "← → trocar de canal · ↑ ↓ escolher · Z: tocar · Esc: sair");
        String[] names = {"Rádio Melodia " + unlockedCount() + "/" + TRACKS.length, "Jornal de Aurora"};
        for (int c = 0; c < 2; c++) {
            g.setColor(c == channel ? new Color(170, 80, 60) : new Color(225, 214, 200)); g.fillRoundRect(44 + c * 220, 108, 210, 26, 8, 8);
            g.setColor(c == channel ? Color.WHITE : new Color(90, 70, 60)); g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(Lang.t(names[c]), 56 + c * 220, 126);
        }
        if (channel == 0) drawTracks(g); else drawNews(g);
    }

    void drawTracks(Graphics2D g) {
        int visible = 10, first = Math.max(0, Math.min(selected - visible / 2, TRACKS.length + 1 - visible));
        for (int row = 0; row < visible && first + row <= TRACKS.length; row++) {
            int i = first + row, y = 146 + row * 27;
            boolean off = i == TRACKS.length, open = off || unlocked(TRACKS[i]);
            boolean playing = off ? track == null : TRACKS[i].theme().equals(track);
            if (i == selected) { g.setColor(new Color(246, 226, 200)); g.fillRoundRect(44, y, 552, 24, 6, 6); }
            String label = off ? "Desligar o rádio" : open ? TRACKS[i].title() : "Bloqueada: visite " + TRACKS[i].region();
            game.life.text(g, (playing ? "♪ " : "   ") + label, 56, y + 17, 520, 14);
        }
        game.life.text(g, track == null ? "O rádio está desligado." : "A faixa escolhida toca em casa e aqui no rádio.", 48, 430, 540, 12);
    }

    void drawNews(Graphics2D g) {
        List<String> lines = news();
        int y = 160, i = scroll;
        for (; i < lines.size() && y < 410; i++) {
            g.setColor(new Color(170, 80, 60)); g.fillOval(48, y - 9, 6, 6);
            game.life.text(g, lines.get(i), 62, y, 530, 13);
            y += 19 * Math.max(1, game.wrap(lines.get(i), g.getFontMetrics(new Font("SansSerif", Font.PLAIN, 13)), 530).size()) + 8;
        }
        if (i < lines.size()) game.life.text(g, "↓", 590, 430, 20, 13);
    }

    // ---------- Desenho ----------
    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (!n.id.equals("radio")) return false;
        g.setColor(new Color(120, 80, 50)); g.fillRect(x + 2, y + 20, 28, 12);                                   // mesinha
        g.setColor(new Color(170, 110, 70)); g.fillRoundRect(x + 5, y + 6, 22, 15, 4, 4);
        g.setColor(new Color(235, 220, 190)); g.fillRect(x + 8, y + 9, 10, 9);
        g.setColor(new Color(90, 60, 40)); for (int i = 0; i < 3; i++) g.drawLine(x + 9, y + 11 + i * 3, x + 16, y + 11 + i * 3);
        g.setColor(new Color(60, 50, 40)); g.fillOval(x + 20, y + 10, 4, 4); g.fillOval(x + 20, y + 15, 4, 4);
        g.drawLine(x + 22, y + 6, x + 26, y);
        if (track != null && !game.access.reduced) {
            g.setColor(new Color(90, 60, 120, 200)); g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(Lang.t("♪"), x + 26 + (int) (Math.sin(game.frame / 15.0) * 2), y + 2 - (int) (game.frame / 6 % 6));
        }
        return true;
    }

    @Override public void save(Properties p) { p.setProperty("radio.faixa", track == null ? "" : track); }
    @Override public void load(Properties p) {
        String t = p.getProperty("radio.faixa", "");
        track = Arrays.stream(TRACKS).anyMatch(k -> k.theme().equals(t)) ? t : null;
    }
    @Override public String signature() { return "r" + track; }
}
