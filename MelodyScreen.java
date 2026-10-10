import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Tela para tocar uma melodia conhecida nas quatro notas dos sinos (setas ou 1-4). As notas aparecem em cores e
 * com o nome, então dá para tocar sem som. Um erro recomeça a melodia. Usada pela canção da Maré (cap. 6) e
 * pelas melodias da semana (cap. 8).
 */
final class MelodyScreen implements Feature {
    final Game game;
    String title = "", hint = "", status = "";
    int[] notes = {};
    int progress, returnState = Game.PLAY;
    Runnable success;

    MelodyScreen(Game game) { this.game = game; }

    void open(String title, String hint, int[] notes, Runnable success) {
        this.title = title; this.hint = hint; this.notes = notes; this.success = success;
        progress = 0; status = ""; returnState = game.state;
        game.down.clear(); game.state = Game.SONG;
    }

    static String names(int[] notes) {
        StringBuilder b = new StringBuilder();
        for (int n : notes) b.append(b.length() == 0 ? "" : ", ").append(VillageLife.BELL_NAMES[n]);
        return b.toString();
    }

    @Override public boolean owns(int state) { return state == Game.SONG; }
    @Override public void key(int key) {
        if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        int note = switch (key) {
            case KeyEvent.VK_UP, KeyEvent.VK_1 -> 0;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_2 -> 1;
            case KeyEvent.VK_DOWN, KeyEvent.VK_3 -> 2;
            case KeyEvent.VK_LEFT, KeyEvent.VK_4 -> 3;
            default -> -1;
        };
        if (note < 0) return;
        game.audio.sfx("bell" + note);
        if (note != notes[progress]) { progress = 0; status = "A nota se perde no vento. Comece de novo: " + names(notes) + "."; game.audio.sfx("bad"); return; }
        if (++progress < notes.length) { status = "Continue..."; return; }
        game.state = returnState; game.down.clear();
        if (success != null) success.run();
    }
    @Override public void draw(Graphics2D g) {
        game.life.panel(g, title, "Toque as notas na ordem · setas ou 1-4 · Esc: sair");
        int step = Math.min(104, 520 / Math.max(1, notes.length)), start = 320 - step * notes.length / 2 + (step - 70) / 2;
        for (int i = 0; i < notes.length; i++) {
            int x = start + i * step, y = 150;
            Color c = VillageLife.BELL_COLORS[notes[i]];
            g.setColor(i < progress ? c : new Color(c.getRed(), c.getGreen(), c.getBlue(), 90)); g.fillOval(x, y, 70, 70);
            g.setColor(new Color(40, 50, 60)); g.setStroke(new BasicStroke(i == progress ? 4 : 1)); g.drawOval(x, y, 70, 70); g.setStroke(new BasicStroke(1));
            game.life.text(g, VillageLife.BELL_NAMES[notes[i]], x + 18, y + 96, 80, 16);
            game.life.text(g, VillageLife.BELL_KEYS[notes[i]], x + 2, y + 118, 100, 11);
        }
        game.life.text(g, status, 48, 330, 540, 15);
        game.life.text(g, hint, 48, 400, 540, 12);
    }
}
