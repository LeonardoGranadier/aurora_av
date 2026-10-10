import java.awt.*;
import java.awt.event.KeyEvent;
import java.nio.file.*;

/**
 * Tela dos três espaços de save, aberta pelo título: "Carregar jogo" escolhe qual partida continuar;
 * "Novo jogo" com os três espaços ocupados escolhe qual apagar (com confirmação). Delete apaga um espaço.
 */
final class SaveSlots implements Feature {
    final Game game;
    boolean choosingNew;
    int selected;

    SaveSlots(Game game) { this.game = game; }

    void open(boolean forNewGame) {
        choosingNew = forNewGame;
        selected = Math.max(0, Game.latestSlot());
        game.down.clear(); game.state = Game.SLOTS_SCREEN;
    }

    @Override public boolean owns(int state) { return state == Game.SLOTS_SCREEN; }
    @Override public void key(int key) {
        int n = Game.SLOTS.length, old = selected;
        if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.openTitle(); return; }
        if (key == KeyEvent.VK_UP) selected = (selected + n - 1) % n;
        else if (key == KeyEvent.VK_DOWN) selected = (selected + 1) % n;
        else if (key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            boolean used = Game.readSlot(selected) != null;
            if (choosingNew || !used) {
                game.slot = selected;
                if (used) game.openConfirm();
                else { game.newGame(); game.explorer.startNaming(); }
            } else game.continueSlot(selected);
            return;
        } else if ((key == KeyEvent.VK_DELETE || key == KeyEvent.VK_BACK_SPACE) && Game.readSlot(selected) != null) {
            int s = selected;
            game.access.ask("Apagar a partida do espaço " + (s + 1) + "? Isso não pode ser desfeito.", () -> {
                try { Files.deleteIfExists(Path.of(Game.SLOTS[s])); Files.deleteIfExists(Path.of(Game.SLOTS[s] + ".bak")); game.toast("Espaço " + (s + 1) + " apagado."); }
                catch (Exception e) { game.toast("Não foi possível apagar a partida."); }
            });
        }
        if (selected != old) game.audio.sfx("select");
    }

    @Override public void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 120)); g.fillRect(0, 0, Game.W, Game.H);
        g.setFont(new Font("SansSerif", Font.BOLD, 20)); g.setColor(new Color(255, 240, 200));
        game.center(g, choosingNew ? "Escolha um espaço para o novo jogo" : "Carregar jogo", 190);
        for (int s = 0; s < Game.SLOTS.length; s++) {
            int x = 90, y = 210 + s * 74, w = Game.W - 180;
            boolean sel = s == selected;
            String info = Game.slotInfo(s);
            g.setColor(sel ? new Color(255, 248, 225, 245) : new Color(255, 255, 255, 215)); g.fillRoundRect(x, y, w, 62, 14, 14);
            g.setColor(sel ? new Color(200, 60, 50) : new Color(40, 40, 60)); g.setStroke(new BasicStroke(sel ? 3 : 1)); g.drawRoundRect(x, y, w, 62, 14, 14); g.setStroke(new BasicStroke(1));
            g.setColor(new Color(40, 40, 60)); g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.drawString(Lang.t("Espaço " + (s + 1)), x + 16, y + 26);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(info == null ? new Color(120, 120, 140) : new Color(50, 60, 70));
            g.drawString(Lang.t(info == null ? "Vazio" : info), x + 16, y + 48);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 12)); g.setColor(new Color(255, 255, 255, 200));
        game.center(g, "Setas: escolher · Z: " + (choosingNew ? "começar aqui" : "jogar") + " · Delete: apagar · Esc: voltar", 452);
    }
}
