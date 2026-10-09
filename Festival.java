import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Evento recorrente do calendario do jogo; concursos nao alteram a campanha. */
final class Festival {
    static final int BELL_ROUNDS = 7, FISH_GOAL = 3;
    static final double FISH_SECONDS = 90;
    static final int[][] PLAZA = {{10, 9}, {16, 12}, {9, 11}, {12, 13}};
    final Game game;
    boolean fishing;
    int fishingDay, score, fishBest, fishWinDay, bellWinDay, prizeDay, prizes, announcedDay;
    double remaining;
    int selection;
    String message = "";

    Festival(Game game) { this.game = game; }
    void reset() {
        fishing = false; fishingDay = 0; score = 0; fishBest = 0;
        fishWinDay = 0; bellWinDay = 0; prizeDay = 0; prizes = 0; announcedDay = 0;
        remaining = 0; selection = 0; message = "";
    }
    int day() { return (int) Math.floor(game.time / 1440) + 1; }
    boolean active() { return day() > 0 && day() % 7 == 0 && game.hour() >= 8 && game.hour() < 20; }
    int nextDay() {
        int today = day();
        if (today % 7 == 0 && game.hour() < 20) return today;
        return today + 7 - Math.floorMod(today, 7);
    }
    void buildWorld() {
        game.maps.get("Vila Aurora").addNpc(new Npc("festival_board", "Mural da Praça", 12, 8,
            new Color(90, 151, 133)).obj("festival_board").still());
        game.maps.get("Casa").addNpc(new Npc("festival_souvenir", "Estrela de Aurora", 7, 4,
            new Color(247, 201, 85)).obj("festival_souvenir").still());
    }
    int[] destination(VillageLife.Resident resident, int slot) {
        int who = game.bonds.residentIndex(resident.id());
        return active() && who >= 0 ? PLAZA[who] : resident.stops()[slot];
    }
    boolean talk(Npc npc) {
        if (npc.id.equals("festival_board")) { open(); return true; }
        if (npc.id.equals("festival_souvenir")) {
            game.say("", "A Estrela de Aurora ilumina a prateleira. Uma lembrança dos dias de festa com a vila.",
                "Edições vencidas: " + prizes + ". Última conquista: dia " + prizeDay + ".");
            return true;
        }
        return false;
    }
    void afterTalk(Npc npc) {
        int who = game.bonds.residentIndex(npc.id);
        if (!active() || who < 0 || game.dlg == null) return;
        boolean friend = game.bonds.friendship[who] >= 40;
        String line = switch (who) {
            case 0 -> friend ? "Hoje guardei um lugar na praça para você. Descobertas são melhores entre amigos!"
                : "Um festival é uma bela oportunidade de conhecer quem vive ao nosso lado.";
            case 1 -> friend ? "Meu parceiro de pescaria chegou! Quero ver essa boia brilhar no concurso."
                : "O concurso de pesca está no mural. Três peixes em noventa segundos: aceita o desafio?";
            case 2 -> friend ? "Separei minhas flores mais bonitas para receber você. Que bom celebrar juntos!"
                : "Hoje a praça está de roupa nova! Que esta festa traga boas amizades.";
            default -> friend ? "Vamos fazer a praça cantar, amigo! Hoje os sinos têm sete melodias."
                : "Os sinos estão mais rápidos hoje. O desafio especial está no mural!";
        };
        String[] lines = Arrays.copyOf(game.dlg, game.dlg.length + 1); lines[lines.length - 1] = line; game.dlg = lines;
    }
    void open() { selection = 0; message = ""; game.state = Game.FESTIVAL; }
    boolean canEnter() { return active() && game.map.name.equals("Vila Aurora"); }

    boolean startFishing() {
        if (!canEnter() || fishing || !game.life.rod) return false;
        fishing = true; fishingDay = day(); score = 0; remaining = FISH_SECONDS;
        game.state = Game.PLAY; game.save(false); return true;
    }
    boolean startBells() {
        if (!canEnter() || fishing) return false;
        game.life.startBells(true); return true;
    }
    void caughtFish() {
        if (fishing && remaining > 0 && game.map.name.equals("Vila Aurora") && day() == fishingDay)
            score++;
    }
    void finishFishing(boolean eligible) {
        if (!fishing) return;
        fishing = false; remaining = 0;
        game.moving = false; game.prog = 0; game.down.clear();
        fishBest = Math.max(fishBest, score);
        boolean won = eligible && active() && day() == fishingDay && score >= FISH_GOAL;
        if (won) fishWinDay = fishingDay;
        message = won ? "Vitória na pesca! " + score + " peixes nesta tentativa."
            : eligible ? "Fim do concurso: " + score + " / " + FISH_GOAL + " peixes. Vale tentar de novo!"
            : "Concurso encerrado ao sair da vila ou da data da festa.";
        game.state = Game.FESTIVAL; selection = 0;
        award(); game.save(false); game.audio.sfx(won ? "fanfare" : "back");
    }
    void winBells() {
        if (!canEnter()) return;
        bellWinDay = day(); award(); game.save(false);
    }
    void award() {
        if (!active() || fishWinDay != day() || bellWinDay != day() || prizeDay == day()) return;
        prizeDay = day(); prizes++;
        if (prizes == 1) game.home.placeFestivalStar();
        game.toast("Estrela de Aurora conquistada! Está em casa.");
    }
    void update(double dt) {
        if (fishing) {
            if (game.state == Game.TITLE || game.state == Game.CONFIRM) return;
            if (!active() || day() != fishingDay || !game.map.name.equals("Vila Aurora")) {
                finishFishing(false); return;
            }
            // Menus e dialogos pausam o cronometro; resultados de pesca continuam contando.
            if (game.state == Game.FISHING || game.state == Game.PLAY && game.dlg == null) {
                remaining = Math.max(0, remaining - dt);
                if (remaining == 0) finishFishing(true);
            }
        }
        if (game.state == Game.PLAY && game.dlg == null && canEnter() && announcedDay != day()) {
            announcedDay = day(); game.toast("Festival de Aurora! Visite o mural da praça.");
        }
    }
    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE) { game.state = Game.PLAY; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) selection = (selection + 2) % 3;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) selection = (selection + 1) % 3;
        if (key != KeyEvent.VK_Z && key != KeyEvent.VK_ENTER && key != KeyEvent.VK_SPACE) return;
        if (selection == 2) { game.state = Game.PLAY; return; }
        if (selection == 0 && fishing) { finishFishing(true); return; }
        if (!canEnter()) { message = "A próxima edição será no dia " + nextDay() + ", das 8h às 20h."; return; }
        if (selection == 1) {
            if (!startBells()) message = "Encerre a tentativa de pesca antes de tocar os sinos.";
            return;
        }
        if (!game.life.rod) { message = "Fale com o Tio Beto para ganhar sua vara primeiro."; return; }
        game.state = Game.PLAY;
        game.say("Tio Beto", "Pesque no lago da vila: três peixes em noventa segundos! O cronômetro começa ao terminar esta conversa.",
            "Os peixes continuam no seu álbum. Menus pausam o concurso; sair da vila encerra a tentativa.",
            "Você pode encerrar a tentativa pelo mural. Boa pescaria!");
        game.afterDlg = () -> {
            if (!startFishing()) { open(); message = "As inscrições encerraram. Próxima edição: dia " + nextDay() + "."; }
        };
    }

    String signature() { return fishing + "|" + fishingDay + "|" + score + "|" + fishBest + "|" + fishWinDay + "|" + bellWinDay + "|" + prizeDay + "|" + prizes; }
    void save(Properties p) {
        p.setProperty("festival.pescando", Boolean.toString(fishing));
        p.setProperty("festival.tempo", Double.toString(remaining));
        String[] names = {"diaPesca", "pontos", "recorde", "vitoriaPesca", "vitoriaSinos", "diaPremio", "premios", "aviso"};
        int[] values = {fishingDay, score, fishBest, fishWinDay, bellWinDay, prizeDay, prizes, announcedDay};
        for (int i = 0; i < names.length; i++) p.setProperty("festival." + names[i], Integer.toString(values[i]));
    }
    void load(Properties p) {
        fishingDay = read(p, "diaPesca"); score = read(p, "pontos"); fishBest = read(p, "recorde");
        fishWinDay = read(p, "vitoriaPesca"); bellWinDay = read(p, "vitoriaSinos");
        prizeDay = read(p, "diaPremio"); prizes = read(p, "premios"); announcedDay = read(p, "aviso");
        try { remaining = Double.parseDouble(p.getProperty("festival.tempo", "0")); }
        catch (NumberFormatException e) { remaining = 0; }
        if (!Double.isFinite(remaining)) remaining = 0;
        remaining = Math.max(0, Math.min(FISH_SECONDS, remaining));
        fishing = Boolean.parseBoolean(p.getProperty("festival.pescando", "false"))
            && canEnter() && fishingDay == day() && remaining > 0;
        if (!fishing) remaining = 0;
    }
    int read(Properties p, String key) { return game.life.number(p, "festival." + key, 0, Integer.MAX_VALUE); }

    void draw(Graphics2D g) {
        game.life.panel(g, "Festival de Aurora", active() ? "Dia " + day() + " · A praça está em festa até as 20h"
            : "Próxima edição: dia " + nextDay() + " · 8h às 20h");
        game.life.text(g, "Pesca: " + (fishWinDay == day() ? "vitória nesta edição" : "3 peixes em 90 segundos")
            + "   ·   Recorde: " + fishBest, 46, 131, 548, 15);
        game.life.text(g, "Sinos: " + (bellWinDay == day() ? "vitória nesta edição" : "7 melodias, ritmo especial"), 46, 159, 545, 15);
        game.life.text(g, "Prêmio: Estrela de Aurora para sua casa", 46, 194, 545, 16);
        game.life.text(g, prizeDay == day() ? "Lembrança conquistada nesta edição!" : "Vença os dois concursos na mesma edição.", 46, 218, 545, 13);
        String[] options = {fishing ? "Encerrar concurso · " + score + " peixes" : "Concurso de pesca", "Sinos em festa", "Voltar à vila"};
        for (int i = 0; i < 3; i++) {
            int y = 246 + i * 43;
            if (selection == i) { g.setColor(new Color(205, 231, 220)); g.fillRoundRect(40, y, 556, 36, 6, 6); }
            game.life.text(g, (selection == i ? "▶ " : "   ") + options[i], 52, y + 25, 530, 17);
        }
        game.life.text(g, message.isEmpty() ? "Edições vencidas: " + prizes : message, 46, 397, 548, 13);
        game.life.text(g, "↑ ↓    Z  ↵", 46, 439, 240, 14); game.life.text(g, "ESC", 548, 439, 55, 13);
    }
    void drawTimer(Graphics2D g) {
        if (!fishing || game.state != Game.PLAY && game.state != Game.FISHING) return;
        int x = 400, y = game.state == Game.FISHING ? 44 : 110;
        g.setColor(new Color(27, 45, 49, 235)); g.fillRoundRect(x, y, 198, 33, 6, 6);
        g.setColor(remaining <= 15 ? new Color(255, 158, 125) : Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString(String.format("%02ds  ·  %d/%d peixes", (int) Math.ceil(remaining), score, FISH_GOAL), x + 13, y + 22);
    }
    void drawDecorations(Graphics2D g, int camX, int camY) {
        if (!active() || !game.map.name.equals("Vila Aurora")) return;
        for (int row = 0; row < 2; row++) {
            int x = 8 * Game.T - camX, y = (7 + row * 7) * Game.T - camY + 4;
            g.setColor(new Color(86, 86, 72)); g.drawLine(x, y, x + 11 * Game.T, y);
            for (int i = 0; i < 16; i++) {
                int fx = x + i * 22;
                g.setColor(VillageLife.BELL_COLORS[(i + row) % 4]);
                g.fillPolygon(new int[]{fx, fx + 18, fx + 9}, new int[]{y + 1, y + 1, y + 19}, 3);
            }
        }
    }
    boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.equals("festival_board")) {
            g.setColor(new Color(139, 105, 69)); g.fillRect(x + 3, y + 3, 27, 25); g.fillRect(x + 7, y + 25, 4, 9); g.fillRect(x + 23, y + 25, 4, 9);
            g.setColor(new Color(43, 101, 85)); g.fillRect(x + 6, y + 6, 21, 18);
            g.setColor(active() ? new Color(255, 219, 94) : Color.WHITE);
            g.drawLine(x + 9, y + 11, x + 23, y + 11); g.drawLine(x + 9, y + 16, x + 23, y + 16);
            return true;
        }
        if (!n.id.equals("festival_souvenir")) return false;
        g.setColor(new Color(126, 89, 60)); g.fillRect(x + 2, y + 28, 29, 5);
        int[] xs = new int[10], ys = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5; int radius = i % 2 == 0 ? 14 : 6;
            xs[i] = x + 16 + (int) (Math.cos(a) * radius); ys[i] = y + 15 + (int) (Math.sin(a) * radius);
        }
        g.setColor(new Color(248, 201, 78)); g.fillPolygon(xs, ys, 10);
        g.setColor(new Color(255, 241, 166)); g.fillOval(x + 13, y + 11, 5, 5);
        return true;
    }
}
