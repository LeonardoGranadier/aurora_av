import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/** Campaign history derived from progress; optional clues require an actual conversation. */
final class AdventureJournal {
    static final String[] TITLES = {"A Luz do Farol", "O Chamado da Capitã", "A Ilha das Estrelas", "A Estrela Cadente", "O Jardim das Constelações"};
    static final int[] START = {0, 4, 8, 15, 22}, END = {4, 8, 15, 21, 28};
    static final String[] SUMMARIES = {
        "A carta levou você até Seu Marinho. A Concha Brilhante devolveu a luz ao farol.",
        "O bilhete da Capitã Estrela revelou um mapa partido. Os três fragmentos foram reunidos.",
        "A viagem à ilha revelou a Estrela do Mar. Sua luz passou a iluminar a baía junto do farol.",
        "Na montanha, você encontrou Cintila e a ajudou a voltar ao céu pelo farol.",
        "O jardim voltou a florescer. Cintila ofereceu uma Flor-Constelação, agora compartilhada com Mamãe."
    };
    static final String[] CLUES = {"garrafa", "capita", "mural", "prof", "garden_sign", "garden_mural"};
    static final String[] CLUE_NAMES = {"Bilhete da Capitã", "Os fragmentos", "Inscrição do templo", "O caminho do jardim", "Marco da trilha", "Inscrição do jardim"};
    static final int[] CLUE_CHAPTER = {1, 1, 2, 4, 4, 4};
    final Game game;
    final Map<String, String> clues = new LinkedHashMap<>();
    int page, scroll, returnState;
    AdventureJournal(Game game) { this.game = game; }
    void reset() { clues.clear(); page = 0; scroll = 0; }
    int latest() {
        for (int i = START.length - 1; i >= 0; i--) if (game.stage >= START[i]) return i;
        return 0;
    }
    void record(Npc npc, int previousStage) {
        if (game.dlg == null || !Arrays.asList(CLUES).contains(npc.id)) return;
        if (npc.id.equals("capita") && previousStage != 5 || npc.id.equals("prof") && previousStage != 22) return;
        clues.putIfAbsent(npc.id, String.join(" ", game.dlg));
    }
    void open(int from) { page = latest(); scroll = 0; returnState = from; game.down.clear(); game.state = Game.JOURNAL; }
    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_L) { game.state = returnState; game.down.clear(); }
        else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) { page = Math.max(0, page - 1); scroll = 0; }
        else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) { page = Math.min(latest(), page + 1); scroll = 0; }
        else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) scroll = Math.max(0, scroll - 1);
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) scroll = Math.min(maxScroll(), scroll + 1);
    }
    List<String> entries() {
        List<String> lines = new ArrayList<>();
        boolean complete = game.stage >= END[page];
        lines.add(complete ? "Concluído" : "Em andamento");
        if (complete) { lines.add(SUMMARIES[page]); lines.add(""); }
        for (int i = START[page]; i < END[page] && i <= game.stage; i++)
            lines.add((i < game.stage ? "[x] " : "> ") + Story.OBJECTIVES[i]);
        if (page == 3 && game.stage == 21) lines.add("> " + Story.OBJECTIVES[21]);
        if (page == 1 && game.stage >= 6) lines.add("Fragmentos do mapa: " + Integer.bitCount(game.frags) + "/3");
        if (page == 0 && game.stage >= 1) lines.add(game.stage >= 2 ? "Carta: entregue ao Seu Marinho." : "Carta: recebida de Mamãe.");
        if (page == 0 && game.stage >= 3) lines.add(game.stage >= 4 ? "Concha Brilhante: entregue ao Seu Marinho." : "Concha Brilhante: encontrada.");
        if (page == 2 && game.stage >= 14) lines.add("Estrela do Mar: encontrada.");
        if (page == 3 && game.stage >= 16) lines.add("Lanterna do Professor: recebida.");
        if (page == 3 && game.stage >= 18) lines.add("Cristal de Calor: recebido.");
        if (page == 2 && game.stage >= 12) lines.add("Braseiros acesos: " + game.brasas + "/3");
        if (page == 4 && game.stage >= 23) lines.add("Sementes de luz: " + Integer.bitCount(game.chapter.seeds) + "/3");
        if (page == 4 && game.stage >= 27) lines.add(game.stage >= 28 ? "Flor-Constelação: entregue à Mamãe." : "Flor-Constelação: recebida de Cintila.");
        lines.add(""); lines.add("Pistas registradas");
        boolean any = false;
        for (int i = 0; i < CLUES.length; i++) if (CLUE_CHAPTER[i] == page && clues.containsKey(CLUES[i])) {
            any = true; lines.add(CLUE_NAMES[i]); lines.add(clues.get(CLUES[i])); lines.add("");
        }
        if (!any) lines.add("Nenhuma anotação neste capítulo.");
        return lines;
    }
    List<String> wrapped() {
        List<String> result = new ArrayList<>();
        FontMetrics fm = game.getFontMetrics(new Font("Dialog", Font.PLAIN, game.access.textSize()));
        for (String line : entries()) {
            if (line.isEmpty()) result.add(""); else result.addAll(game.wrap(line, fm, 554));
        }
        return result;
    }
    int maxScroll() { return Math.max(0, wrapped().size() - game.access.journalRows()); }
    void save(Properties p) { clues.forEach((id, value) -> p.setProperty("diario." + id, value)); }
    void load(Properties p) {
        clues.clear();
        for (int i = 0; i < CLUES.length; i++) {
            String text = p.getProperty("diario." + CLUES[i]);
            if (text != null && !text.isBlank() && text.length() <= 6000 && CLUE_CHAPTER[i] <= latest()) clues.put(CLUES[i], text);
        }
    }
    void draw(Graphics2D g) {
        g.setColor(new Color(238, 245, 241)); g.fillRect(0, 0, Game.W, Game.H);
        g.setColor(new Color(34, 62, 64)); g.setFont(new Font("Dialog", Font.BOLD, 22));
        g.drawString("Diário de Aventuras", 24, 34);
        g.setFont(new Font("Dialog", Font.BOLD, 16)); g.drawString((page + 1) + ". " + TITLES[page], 42, 76);
        g.setFont(new Font("Dialog", Font.PLAIN, game.access.textSize()));
        List<String> lines = wrapped(); scroll = Math.min(scroll, maxScroll());
        for (int i = 0; i < game.access.journalRows() && scroll + i < lines.size(); i++) {
            String line = lines.get(scroll + i);
            g.setColor(line.startsWith(">") ? new Color(154, 82, 32) : new Color(34, 62, 64));
            g.drawString(line, 42, 108 + i * (game.access.large ? 29 : 22));
        }
        g.setColor(new Color(90, 139, 130));
        if (scroll > 0) g.fillPolygon(new int[]{606, 612, 618}, new int[]{109, 101, 109}, 3);
        if (scroll < maxScroll()) g.fillPolygon(new int[]{606, 612, 618}, new int[]{411, 419, 411}, 3);
        g.drawString((page + 1) + " / " + (latest() + 1), 290, 461);
        if (page > 0) g.drawString("←", 42, 461);
        if (page < latest()) g.drawString("→", 582, 461);
    }
}
