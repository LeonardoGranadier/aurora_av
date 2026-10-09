import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;
import java.util.*;

/** Persistent input preferences, readable story text and explicit item handoffs. */
final class Accessibility {
    static final Path FILE = GameFiles.data("accessibility.properties");
    static final int[] LOGICAL = {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
        KeyEvent.VK_Z, KeyEvent.VK_I, KeyEvent.VK_P, KeyEvent.VK_J, KeyEvent.VK_G, KeyEvent.VK_H,
        KeyEvent.VK_F, KeyEvent.VK_TAB, KeyEvent.VK_L, KeyEvent.VK_V, KeyEvent.VK_M, KeyEvent.VK_R, KeyEvent.VK_DELETE, KeyEvent.VK_T};
    static final int[] DEFAULTS = {KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D,
        KeyEvent.VK_Z, KeyEvent.VK_I, KeyEvent.VK_P, KeyEvent.VK_J, KeyEvent.VK_G, KeyEvent.VK_H,
        KeyEvent.VK_F, KeyEvent.VK_TAB, KeyEvent.VK_L, KeyEvent.VK_V, KeyEvent.VK_M, KeyEvent.VK_R, KeyEvent.VK_DELETE, KeyEvent.VK_T};
    static final String[] LABELS = {"Cima / nota 1", "Baixo / nota 3", "Esquerda / nota 4", "Direita / nota 2",
        "Interagir / fisgar / segurar", "Itens", "Álbum de pesca", "Laços de Aurora", "Presentear peixe", "Decorar casa",
        "Previsão", "Mapa", "Diário", "Cesta de flores", "Música ligada/desligada", "Girar móvel", "Guardar móvel", "Avançar 2h (teste)"};
    final Game game;
    final int[] keys = DEFAULTS.clone();
    boolean large, reduced, capturing, yes;
    int selected, returnState, dialogPage;
    Runnable pending;
    String question = "", status = "";
    Accessibility(Game game) { this.game = game; load(); }
    boolean reserved(int key) {
        return key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE
            || key == KeyEvent.VK_BACK_SPACE || key >= KeyEvent.VK_LEFT && key <= KeyEvent.VK_DOWN
            || key >= KeyEvent.VK_1 && key <= KeyEvent.VK_4;
    }
    int translate(int key) {
        if (reserved(key)) return key;
        for (int i = 0; i < keys.length; i++) if (keys[i] == key) return LOGICAL[i];
        return KeyEvent.VK_UNDEFINED;
    }
    boolean bind(int index, int key) {
        if (key == KeyEvent.VK_UNDEFINED || reserved(key) || key == KeyEvent.VK_SHIFT || key == KeyEvent.VK_CONTROL || key == KeyEvent.VK_ALT) return false;
        for (int i = 0; i < keys.length; i++) if (i != index && keys[i] == key) return false;
        int old = keys[index]; keys[index] = key;
        if (save()) return true;
        keys[index] = old; return false;
    }
    void load() {
        Properties p = new Properties();
        try (var in = Files.newInputStream(FILE)) { p.load(in); } catch (Exception e) { return; }
        large = Boolean.parseBoolean(p.getProperty("large", "false")); reduced = Boolean.parseBoolean(p.getProperty("reduced", "false"));
        int[] loaded = DEFAULTS.clone(); Set<Integer> seen = new HashSet<>();
        try {
            for (int i = 0; i < keys.length; i++) {
                loaded[i] = Integer.parseInt(p.getProperty("key." + i, "" + DEFAULTS[i]));
                if (loaded[i] <= 0 || reserved(loaded[i]) || !seen.add(loaded[i])) return;
            }
            System.arraycopy(loaded, 0, keys, 0, keys.length);
        } catch (NumberFormatException e) { /* malformed bindings retain defaults */ }
    }
    boolean save() {
        Properties p = new Properties(); p.setProperty("large", "" + large); p.setProperty("reduced", "" + reduced);
        for (int i = 0; i < keys.length; i++) p.setProperty("key." + i, "" + keys[i]);
        try {
            Path tmp = Path.of(FILE + ".tmp");
            try (var out = Files.newOutputStream(tmp)) { p.store(out, "Aurora - accessibility"); }
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING); return true;
        } catch (Exception e) { status = "Falha ao salvar as preferências."; return false; }
    }
    void open() { selected = 0; capturing = false; status = ""; game.down.clear(); game.state = Game.ACCESSIBILITY; }
    void capture(int raw) {
        if (raw == KeyEvent.VK_ESCAPE) { capturing = false; return; }
        if (bind(selected - 2, raw)) { capturing = false; status = "Tecla atualizada."; }
        else status = "Tecla reservada, já usada ou falha ao salvar.";
    }
    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE) { game.state = Game.MENU; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP) selected = Math.max(0, selected - 1);
        if (key == KeyEvent.VK_DOWN) selected = Math.min(keys.length + 2, selected + 1);
        if (key != KeyEvent.VK_Z && key != KeyEvent.VK_ENTER && key != KeyEvent.VK_SPACE) return;
        if (selected < 2) {
            if (selected == 0) large = !large; else reduced = !reduced;
            if (!save()) { if (selected == 0) large = !large; else reduced = !reduced; }
        } else if (selected < keys.length + 2) { capturing = true; status = "Pressione a nova tecla. Esc cancela."; }
        else {
            int[] old = keys.clone(); System.arraycopy(DEFAULTS, 0, keys, 0, keys.length);
            if (!save()) System.arraycopy(old, 0, keys, 0, keys.length);
            else status = "Teclas padrão restauradas.";
        }
    }
    void ask(String text, Runnable action) {
        returnState = game.state; pending = action; question = text; yes = false;
        game.down.clear(); game.state = Game.HANDOFF;
    }
    void confirmKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { pending = null; game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) yes = !yes;
        if (key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            Runnable action = pending; pending = null; game.state = returnState; game.down.clear();
            if (yes && action != null) action.run();
        }
    }
    int textSize() { return large ? 19 : 14; }
    int journalRows() { return large ? 11 : 15; }
    java.util.List<String> dialogLines() {
        return game.wrap(game.dlg[game.dlgIdx], game.getFontMetrics(new Font("SansSerif", Font.PLAIN, large ? 22 : 17)), Game.W - 90);
    }
    boolean moreDialog() {
        int rows = large ? 5 : 3;
        if ((dialogPage + 1) * rows < dialogLines().size()) { dialogPage++; return true; }
        dialogPage = 0; return false;
    }
    void draw(Graphics2D g) {
        game.life.panel(g, "Acessibilidade e controles", "Setas: navegar · Enter: escolher · Esc: voltar");
        int start = Math.max(0, selected - 6);
        for (int row = start; row <= Math.min(keys.length + 2, start + 7); row++) {
            int y = 126 + (row - start) * 32;
            if (row == selected) { g.setColor(new Color(207, 231, 221)); g.fillRect(38, y - 21, 560, 30); }
            String label = row == 0 ? "Texto de diálogos e diário" : row == 1 ? "Reduzir movimento e transições" : row == keys.length + 2 ? "Restaurar teclas" : LABELS[row - 2];
            String value = row == 0 ? large ? "Grande" : "Normal" : row == 1 ? reduced ? "Sim" : "Não" : row == keys.length + 2 ? "" : KeyEvent.getKeyText(keys[row - 2]);
            game.life.text(g, label, 48, y, 370, 15); game.life.text(g, value, 438, y, 150, 14);
        }
        game.life.text(g, status, 44, 386, 540, 14);
        game.life.text(g, "Fixos: Esc · Enter/Espaço · setas · notas 1–4 · Backspace", 44, 440, 545, 13);
    }
    void drawConfirmation(Graphics2D g) {
        game.life.panel(g, "Confirmar entrega", "");
        game.life.text(g, question, 48, 156, 520, 20);
        game.life.text(g, yes ? "  Cancelar                 > Confirmar" : "> Cancelar                   Confirmar", 70, 330, 490, 19);
    }
}
