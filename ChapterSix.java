import java.awt.*;
import java.util.*;

/**
 * Capítulo 6, A Canção da Maré: a Capitã ouve um canto no mar. Três conchas cantoras (Gruta do Eco,
 * Ilha das Estrelas e o novo Recife de Coral) ensinam a canção da baleia Maré, guardiã da baía, que se
 * perdeu quando o farol passou a brilhar com luz de estrela. Tocar a canção na ponta do recife, à noite,
 * a traz de volta. Etapas 28 a 32.
 */
final class ChapterSix implements Feature {
    static final String REEF = "Recife de Coral";
    static final int START = 28, LISTEN = 29, PLAY_SONG = 30, TELL = 31, DONE = 32;
    /** A canção: MI, DÓ, SOL, RÉ, MI (índices das notas dos sinos). */
    static final int[] SONG = {2, 0, 3, 1, 2};
    /** Cada concha ensina um trecho da canção: {primeira nota, quantidade}. */
    static final int[][] PARTS = {{0, 2}, {2, 2}, {4, 1}};
    static final String[] SHELL_MAPS = {"Gruta do Eco", "Ilha das Estrelas", REEF};
    static final int[][] SHELL_SPOTS = {{21, 6}, {24, 13}, {11, 4}};

    final Game game;
    int heard;          // bits das conchas ouvidas

    ChapterSix(Game game) { this.game = game; }

    boolean knows(int note) {
        for (int i = 0; i < PARTS.length; i++)
            if ((heard & (1 << i)) != 0 && note >= PARTS[i][0] && note < PARTS[i][0] + PARTS[i][1]) return true;
        return game.stage >= PLAY_SONG;
    }
    boolean whaleHome() { return game.stage >= TELL; }

    @Override public void reset() { heard = 0; }

    @Override public void buildWorld() {
        GameMap reef = new GameMap(REEF, 24, 16, '~', true);
        reef.rect(0, 7, 3, 1, 'P');                                     // píer do bote
        reef.rect(3, 6, 4, 3, 's'); reef.rect(7, 7, 4, 1, 's');
        reef.rect(9, 3, 4, 5, 's'); reef.rect(13, 7, 5, 1, 's');
        reef.rect(16, 8, 2, 3, 's'); reef.rect(17, 10, 4, 1, 's');
        reef.rect(19, 5, 3, 6, 's');
        for (int[] c : new int[][]{{4, 5}, {6, 9}, {9, 2}, {13, 3}, {12, 8}, {15, 6}, {18, 12}, {21, 4}, {22, 9}, {3, 9}, {14, 10}})
            reef.t[c[1]][c[0]] = 'R';
        reef.addNpc(new Npc("bote_recife", "Bote", 1, 6, new Color(140, 90, 50)).obj("barco").still());
        reef.addNpc(new Npc("fogueira", "", 4, 6, new Color(244, 152, 64)).obj("fogueira").still());
        reef.addNpc(new Npc("recife_ponta", "Ponta do recife", 21, 7, new Color(120, 200, 210)).obj("recife_ponta").still());
        game.maps.put(REEF, reef);
        game.maps.get("Ilha das Estrelas").addNpc(new Npc("bote_ilha", "Bote", 25, 10, new Color(140, 90, 50)).obj("barco").still());
        for (int i = 0; i < 3; i++)
            game.maps.get(SHELL_MAPS[i]).addNpc(new Npc("concha_cantora" + i, "Concha cantora", SHELL_SPOTS[i][0], SHELL_SPOTS[i][1],
                new Color(150, 220, 230)).obj("concha_cantora").still().stages(LISTEN, 99));
    }

    @Override public boolean talk(Npc n) {
        String id = n.id;
        if (id.equals("capita_fim") && game.stage == START) {
            if (!game.isNight()) { game.say(n.name, "De dia o mar só sussurra. Volte à noite, marujo: quero que você escute uma coisa."); return true; }
            game.setStage(LISTEN);
            game.say(n.name, "Escute... está ouvindo? Esse canto vem do fundo da baía há três noites.",
                "É a Maré, a velha baleia guardiã. Quando o farol ganhou luz de estrela, ela se confundiu e se perdeu além do recife.",
                "Dizem que conchas cantoras guardam a canção dela: uma na Gruta do Eco, outra na Ilha das Estrelas e a última no Recife de Coral.",
                "Há um bote na praia leste da ilha que leva ao recife. Aprenda a canção e toque-a na ponta do recife, à noite.");
            return true;
        }
        if (id.equals("capita_fim") && game.stage == TELL) {
            game.setStage(DONE);
            game.say(n.name, "Eu ouvi daqui! A canção dela voltou a ecoar pela baía inteira.",
                "Sabe, marujo? Faróis mostram o caminho de volta, mas canções também. Obrigada por ouvir o mar.",
                "Capítulo 6 concluído: A Canção da Maré.",
                "Numa noite clara, passe na porta do farol. A Cintila anda cochichando sobre uma festa...");
            game.afterDlg = () -> game.itemGet("escama");
            return true;
        }
        if (id.startsWith("concha_cantora")) {
            int i = id.charAt(14) - '0';
            if (game.stage != LISTEN) { game.say("", "A concha cantora murmura a canção da Maré, baixinho."); return true; }
            boolean first = (heard & (1 << i)) == 0;
            heard |= 1 << i;
            StringBuilder notes = new StringBuilder();
            for (int k = PARTS[i][0]; k < PARTS[i][0] + PARTS[i][1]; k++) {
                game.audio.sfx("bell" + SONG[k]);
                notes.append(notes.length() == 0 ? "" : ", ").append(VillageLife.BELL_NAMES[SONG[k]]);
            }
            int count = Integer.bitCount(heard);
            if (count == 3) game.setStage(PLAY_SONG);
            game.say("", "Você encosta a concha no ouvido. Ela canta: " + notes + ".",
                count == 3 ? "Agora você conhece a canção inteira: " + songText() + ". Toque-a na ponta do Recife de Coral, à noite."
                    : (first ? "Trecho aprendido! " : "") + "Conchas ouvidas: " + count + "/3. A canção fica anotada no diário.");
            return true;
        }
        switch (id) {
            case "bote_ilha" -> {
                if (game.stage < LISTEN) { game.say("", "Um bote pequeno, amarrado na praia leste. Ainda não há motivo para ir mar adentro."); return true; }
                game.say("", "Você rema até o Recife de Coral. A água fica verde-clara e cheia de peixinhos.");
                game.afterDlg = () -> { game.time += 30; game.travel(REEF, 2, 7); };
            }
            case "bote_recife" -> {
                game.say("", "Você rema de volta para a Ilha das Estrelas.");
                game.afterDlg = () -> { game.time += 30; game.travel("Ilha das Estrelas", 24, 10); };
            }
            case "recife_ponta" -> {
                if (whaleHome()) game.say("", game.isNight() ? "A Maré nada em círculos lentos e canta para as estrelas. A baía está em paz."
                    : "Ao longe, um jato de água sobe do mar. A Maré vigia a baía.");
                else if (game.stage < PLAY_SONG) game.say("", "A ponta do recife olha para o mar aberto. O vento parece cantarolar algo incompleto.");
                else if (!game.isNight()) game.say("", "O mar está calmo demais. O canto da Maré só responde à noite. A fogueira do recife permite esperar.");
                else game.melody.open("Canção da Maré", "As notas também aparecem em cores: dá para tocar sem som.", SONG, this::whaleReturns);
            }
            default -> { return false; }
        }
        return true;
    }

    String songText() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < SONG.length; i++) b.append(i == 0 ? "" : ", ").append(knows(i) ? VillageLife.BELL_NAMES[SONG[i]] : "?");
        return b.toString();
    }

    /** A canção certa traz a Maré de volta. */
    void whaleReturns() {
        game.setStage(TELL);
        game.audio.sfx("fanfare");
        game.say("", "A última nota se espalha pela água... e o mar responde.",
            "Uma baleia enorme, salpicada de luzes como um céu noturno, ergue-se ao lado do recife: é a Maré!",
            "Ela canta de volta, e o farol, lá longe, pisca em resposta. A guardiã encontrou o caminho de casa.",
            "Conte à Capitã Estrela o que aconteceu.");
    }

    // ---------- Desenho ----------
    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.startsWith("concha_cantora")) {
            int glow = game.access.reduced ? 0 : (int) (Math.sin(game.frame / 12.0) * 40);
            g.setColor(new Color(150, 220, 230, 90 + glow)); g.fillOval(x + 2, y + 8, 28, 24);
            g.setColor(new Color(225, 245, 250)); g.fillArc(x + 7, y + 12, 18, 18, 0, 180);
            g.setColor(new Color(120, 180, 200)); for (int i = -6; i <= 6; i += 3) g.drawLine(x + 16, y + 21, x + 16 + i, y + 13);
            return true;
        }
        if (n.id.equals("recife_ponta")) {
            g.setColor(new Color(110, 120, 125)); g.fillOval(x + 3, y + 14, 26, 18);
            g.setColor(new Color(240, 140, 150)); g.fillOval(x + 8, y + 10, 8, 10); g.fillOval(x + 16, y + 8, 7, 12);
            return true;
        }
        return false;
    }
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals(REEF)) return;
        boolean night = game.isNight();
        if (whaleHome() && night) {                                                // a Maré nadando ao lado do recife
            double t = game.access.reduced ? 0 : game.frame / 60.0;
            int wx = 13 * Game.T - camX + (int) (Math.cos(t) * 30), wy = 11 * Game.T - camY + 8 + (int) (Math.sin(t) * 12);
            g.setColor(new Color(70, 110, 170, 230)); g.fillOval(wx, wy, 120, 46);
            g.fillPolygon(new int[]{wx + 112, wx + 150, wx + 146}, new int[]{wy + 22, wy + 4, wy + 40}, 3);
            g.setColor(new Color(200, 225, 255)); g.fillOval(wx + 18, wy + 14, 6, 6);
            Random r = new Random(8);
            for (int i = 0; i < 12; i++) { g.setColor(new Color(255, 250, 200, 200)); g.fillRect(wx + 30 + r.nextInt(80), wy + 6 + r.nextInt(30), 2, 2); }
        }
        if (night && game.stage >= LISTEN) {
            g.setColor(new Color(150, 230, 240, 70));
            for (int i = 0; i < 18; i++) {
                int x = (i * 151 % (24 * Game.T)) - camX, y = (i * 89 % (16 * Game.T)) - camY;
                g.fillOval(x, y + (int) (Math.sin(game.frame / 30.0 + i) * 4), 4, 4);
            }
        }
    }

    @Override public void save(Properties p) { p.setProperty("mare.conchas", "" + heard); }
    @Override public void load(Properties p) {
        heard = game.stage >= PLAY_SONG ? 7 : game.stage == LISTEN ? game.life.number(p, "mare.conchas", 0, 7) : 0;
    }
    @Override public String signature() { return "m" + heard; }
}
