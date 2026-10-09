import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;

/** Independent ambient layer and keyboard-operated volume sliders. */
final class Soundscape {
    final Game game;
    int selected;
    Soundscape(Game game) { this.game = game; }
    String desired() {
        if (game.state == Game.TITLE || game.state == Game.CONFIRM || !game.map.outdoor) return "";
        String base = game.map.snowy ? "vento" : game.map.name.equals("Praia Azul") || game.map.name.equals("Ilha das Estrelas")
            ? "mar" : game.map.name.equals(ChapterFive.GARDEN) ? "fonte" : "campo";
        return base + (game.isNight() ? "_noite" : "_dia")
            + (!game.map.snowy && game.weather.current() == Weather.RAIN ? "_chuva" : "");
    }
    void update() { game.audio.ambient(desired()); }
    void open() { selected = 0; game.down.clear(); game.state = Game.SOUND; }
    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE) { game.state = Game.MENU; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) selected = Math.max(0, selected - 1);
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) selected = Math.min(2, selected + 1);
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) game.audio.setVolume(selected, level() - 5);
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) game.audio.setVolume(selected, level() + 5);
    }
    int level() { return levels()[selected]; }
    int[] levels() { return new int[]{game.audio.musicVolume, game.audio.sfxVolume, game.audio.ambientVolume}; }
    void draw(Graphics2D g) {
        game.life.panel(g, "Sons de Aurora", "");
        String[] names = {"Música", "Efeitos", "Ambiente"}; int[] levels = levels();
        for (int i = 0; i < 3; i++) {
            int y = 150 + i * 90;
            g.setColor(new Color(34, 62, 64)); g.setFont(new Font("Dialog", Font.BOLD, 17));
            g.drawString(names[i], 54, y);
            String value = levels[i] + "%";
            if (i == 0 && !game.audio.musicOn || i == 1 && !game.audio.sfxOn) value += " (desligado)";
            g.setFont(new Font("Dialog", Font.PLAIN, 14)); g.drawString(value, 410, y);
            g.setColor(new Color(192, 212, 204)); g.fillRect(54, y + 20, 520, 6);
            g.setColor(i == selected ? new Color(180, 103, 43) : new Color(69, 135, 130));
            g.fillRect(54, y + 20, levels[i] * 520 / 100, 6);
            g.fillOval(48 + levels[i] * 520 / 100, y + 16, 14, 14);
        }
    }
    static byte[] render(String name) {
        int count = (int) Audio.RATE * 8;
        float[] samples = new float[count]; Random random = new Random(name.hashCode());
        double low = 0; boolean night = name.contains("noite"), rain = name.contains("chuva");
        for (int i = 0; i < count; i++) {
            double t = i / (double) Audio.RATE, noise = random.nextDouble() * 2 - 1;
            low += 0.025 * (noise - low);
            double wave = 0.5 - 0.5 * Math.cos(2 * Math.PI * t / 4);
            double value = name.startsWith("mar") ? low * (0.6 + wave * 2.5)
                : name.startsWith("vento") ? low * (0.8 + wave)
                : name.startsWith("fonte") ? low + noise * 0.09 : low * 0.3;
            if (night && !name.startsWith("vento")) value += 0.025 * Math.sin(2 * Math.PI * 2800 * t) * Math.pow(Math.sin(Math.PI * 3 * t), 12);
            if (!night && name.startsWith("campo")) value += 0.025 * Math.sin(2 * Math.PI * 900 * t + 7 * Math.sin(2 * Math.PI * 4 * t)) * Math.pow(Math.sin(Math.PI * t / 2), 24);
            if (rain) value += noise * 0.14;
            // A short envelope avoids discontinuities at the loop boundary.
            double envelope = Math.min(1, Math.min(i, count - 1 - i) / (Audio.RATE * 0.12));
            samples[i] = (float) (value * envelope);
        }
        return Audio.toBytes(samples);
    }
}
