import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/** Créditos: rolam no fim do capítulo 5 (com o resumo da jornada) e podem ser vistos pelo título. */
final class Credits implements Feature {
    /** Edite para assinar o jogo. */
    static final String AUTHOR = "Leonardo Granadier";
    static final String[] CAST = {"Mamãe", "Seu Marinho", "Capitã Estrela", "Prof. Carvalho", "Tio Beto", "Dona Rosa",
        "Lucas", "Lenhador", "Eremita", "Montanhista", "Seu Bento, o mascate", "Bolinha, o gato", "Maré, a baleia", "Cintila"};

    final Game game;
    double scroll;
    int returnState = Game.TITLE;
    boolean ending, seen;

    Credits(Game game) { this.game = game; }

    void open(int from, boolean ending) {
        returnState = from; this.ending = ending; scroll = 0;
        game.down.clear(); game.state = Game.CREDITS;
    }

    List<String> lines() {
        List<String> l = new ArrayList<>();
        if (ending) {
            l.addAll(List.of("#Obrigado por jogar!", "", game.explorer.nameOr("Você") + " devolveu a luz ao farol,",
                "cruzou o mar, ajudou uma estrela a voltar para casa,", "fez um jardim florescer, devolveu a canção da baleia Maré",
                "e reuniu Aurora inteira no Festival do Céu.", "",
                "Tempo de jogo: " + Explorer.clock(game.explorer.playSeconds) + "   ·   Dia " + (game.day() + 1),
                "Conquistas: " + game.explorer.unlocked.size() + "/" + Explorer.ACHIEVEMENTS.size()
                    + "   ·   Conchas: " + game.shells.found() + "/" + SecretShells.SPOTS.length, ""));
        }
        l.addAll(List.of("#Aventura em Aurora", "versão " + Game.VERSION, "", "@Criação", AUTHOR, "", "@Elenco"));
        l.addAll(Arrays.asList(CAST));
        l.addAll(List.of("", "@Feito com", "Java e Swing, sem bibliotecas externas", "Música e efeitos sintetizados por código", "",
            "@Inspirado em", "Pokémon Red, Blue, Gold e Silver", "The Legend of Zelda: Link's Awakening e The Minish Cap",
            "Harvest Moon, Stardew Valley e Animal Crossing", "EarthBound e Golden Sun", ""));
        if (ending) l.addAll(List.of("A vila continua aqui.", "Ainda há conchas, trocas, estrelas e amigos esperando.", ""));
        l.add("#Até a próxima noite clara!");
        return l;
    }

    @Override public boolean owns(int state) { return state == Game.CREDITS; }
    /** Os créditos rolam também fora do estado PLAY, por isso avançam no desenho. */
    @Override public void draw(Graphics2D g) {
        if (!game.access.reduced) scroll += 0.6;
        g.setPaint(new GradientPaint(0, 0, new Color(8, 10, 36), 0, Game.H, new Color(40, 40, 95)));
        g.fillRect(0, 0, Game.W, Game.H);
        Random r = new Random(3);
        for (int i = 0; i < 90; i++) { g.setColor(new Color(255, 255, 255, 50 + r.nextInt(150))); g.fillRect(r.nextInt(Game.W), r.nextInt(Game.H), 2, 2); }
        List<String> lines = lines();
        int y = game.access.reduced ? (int) (60 - scroll) : (int) (Game.H - scroll);
        int total = 0;
        for (String line : lines) {
            Font f = line.startsWith("#") ? new Font("Serif", Font.BOLD, 30) : line.startsWith("@") ? new Font("SansSerif", Font.BOLD, 15) : new Font("SansSerif", Font.PLAIN, 16);
            g.setFont(f);
            g.setColor(line.startsWith("@") ? new Color(255, 215, 120) : line.startsWith("#") ? new Color(255, 240, 200) : new Color(230, 235, 255));
            String text = line.replaceFirst("^[#@]", "");
            if (y > -40 && y < Game.H + 40) game.center(g, text, y);
            int step = line.startsWith("#") ? 46 : 26;
            y += step; total += step;
        }
        if (!game.access.reduced && scroll > total + Game.H) scroll = 0;
        g.setColor(new Color(255, 255, 255, 150)); g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(game.access.reduced ? "Setas: rolar · Z / Esc: sair" : "Z / Esc: sair", Game.W - 170, Game.H - 12);
    }
    @Override public void key(int key) {
        if (game.access.reduced && (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_UP)) {   // sem rolagem automática
            scroll = Math.max(0, scroll + (key == KeyEvent.VK_DOWN ? 52 : -52)); return;
        }
        if (key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_ESCAPE) {
            game.audio.sfx("back");
            if (returnState == Game.TITLE) game.openTitle(); else { game.state = returnState; game.down.clear(); }
        }
    }

    @Override public void reset() { seen = false; }
    @Override public void save(Properties p) { p.setProperty("creditos.final", "" + seen); }
    @Override public void load(Properties p) { seen = Boolean.parseBoolean(p.getProperty("creditos.final", "false")) || game.stage >= Story.FINAL; }
}
