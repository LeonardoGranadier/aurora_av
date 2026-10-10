import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * Estações de 28 dias (primavera, verão, outono, inverno), como em Harvest Moon e Stardew Valley.
 * Mudam a paisagem, os peixes mais comuns, o que cresce no quintal, o nome do festival e as falas.
 * Também guarda os aniversários dos moradores e o calendário (ESC > Calendário ou → na previsão).
 */
final class Seasons implements Feature {
    static final int SPRING = 0, SUMMER = 1, AUTUMN = 2, WINTER = 3, LENGTH = 28;
    static final String[] NAMES = {"Primavera", "Verão", "Outono", "Inverno"};
    static final String[] FESTIVALS = {"Festival das Flores", "Festival do Sol", "Festival da Colheita", "Festival das Luzes"};
    static final Color[] COLORS = {new Color(226, 130, 170), new Color(236, 178, 52), new Color(206, 108, 50), new Color(110, 160, 210)};
    /** Peixes mais comuns (peso dobrado) em cada estação; nenhum peixe deixa de existir. */
    static final int[][] SEASON_FISH = {{3, 4}, {6, 1}, {2, 5}, {7, 0}};
    /** [estação][flor]: o que cresce em cada estação (margarida, tulipa, girassol, flor estelar). */
    static final boolean[][] CROP_SEASONS = {
        {true, true, true, true}, {true, false, true, true}, {true, true, false, true}, {false, false, false, true}};
    static final String[] STARTS = {"Começou a Primavera!", "Começou o Verão!", "Começou o Outono!", "Começou o Inverno!"};
    /** Aniversários: {estação, dia da estação} para prof, beto, rosa, lucas. */
    static final int[][] BIRTHDAYS = {{AUTUMN, 12}, {SUMMER, 5}, {SPRING, 18}, {WINTER, 9}};
    static final String[] BIRTHDAY_LINES = {
        "Você lembrou do meu aniversário?! Meu avô dizia que quem lembra das datas guarda as pessoas no céu.",
        "Aniversário de pescador é dia de peixe na brasa! Obrigado por lembrar, aprendiz.",
        "Que surpresa! Hoje a praça vai ganhar uma flor a mais, só por sua causa.",
        "É hoje! É hoje! Minha irmã sempre me mandava uma melodia no meu aniversário. Você é o primeiro a lembrar!"};

    final Game game;
    int announcedDay = -1, calendarSeason = -1, returnState = Game.PLAY;
    final Set<String> birthdayGifts = new TreeSet<>();   // "quem-ano" que já ganharam presente de aniversário

    Seasons(Game game) { this.game = game; }

    // ---------- Calendário ----------
    /** Estação do dia exibido (1 = primeiro dia). */
    static int season(int shownDay) { return Math.floorMod((shownDay - 1) / LENGTH, 4); }
    static int dayOfSeason(int shownDay) { return Math.floorMod(shownDay - 1, LENGTH) + 1; }
    static int year(int shownDay) { return (shownDay - 1) / (LENGTH * 4) + 1; }
    int shownDay() { return game.day() + 1; }
    int current() { return season(shownDay()); }
    String name() { return NAMES[current()]; }
    String festivalName() { return FESTIVALS[current()]; }

    /** Paisagem com neve: inverno ao ar livre, fora da praia e da ilha tropical. */
    boolean snowCover(GameMap m) {
        return m != null && current() == WINTER && m.outdoor && !m.snowy
            && !m.name.equals("Praia Azul") && !m.name.equals("Ilha das Estrelas");
    }

    int fishFactor(int fish) {
        for (int f : SEASON_FISH[current()]) if (f == fish) return 2;
        return 1;
    }
    static boolean grows(int crop, int shownDay) { return crop >= 0 && crop < 4 && CROP_SEASONS[season(shownDay)][crop]; }

    int birthdayOf(int shownDay) {
        for (int who = 0; who < BIRTHDAYS.length; who++)
            if (BIRTHDAYS[who][0] == season(shownDay) && BIRTHDAYS[who][1] == dayOfSeason(shownDay)) return who;
        return -1;
    }
    boolean birthday(int who) { return birthdayOf(shownDay()) == who; }

    /** Presentes no aniversário valem o dobro (uma vez por ano). */
    int giftPoints(int who, int points) {
        if (!birthday(who)) return points;
        birthdayGifts.add(who + "-" + year(shownDay()));
        return points * 2;
    }

    // ---------- Ciclo ----------
    @Override public void reset() { announcedDay = -1; birthdayGifts.clear(); }
    @Override public void update(double dt) {
        int day = shownDay();
        if (day == announcedDay) return;
        boolean first = announcedDay < 0;
        announcedDay = day;
        if (first && day > 1) return;   // partida carregada: sem aviso repetido
        if (dayOfSeason(day) == 1) { game.banner = day == 1 ? "Primavera em Aurora" : STARTS[current()]; game.bannerT = 4; }
        int who = birthdayOf(day);
        if (who >= 0) game.toast("Hoje é aniversário de " + Bonds.NAMES[who] + "!");
    }

    /** Fala da estação para dias de sol, no lugar da frase genérica do clima. */
    String sunnyLine(int who) {
        if (birthday(who)) return BIRTHDAY_LINES[who];
        return switch (current()) {
            case SPRING -> who == 2 ? "Primavera! As margaridas e tulipas do quintal crescem bem agora." : "A primavera deixa a vila com cheiro de flor.";
            case SUMMER -> who == 1 ? "No verão, a Carpa Dourada e o Peixe-Coral aparecem mais. Aproveite!" : "Que calor! Um bom dia para um mergulho na praia.";
            case AUTUMN -> who == 0 ? "No outono, as noites ficam mais limpas. Bom tempo para observar estrelas." : "As folhas douradas do outono são as minhas favoritas.";
            default -> who == 3 ? "Neve! Vamos fazer um boneco na praça? O Peixe Estelar adora águas frias." : "Brr! Só a Flor Estelar cresce no inverno. Agasalhe-se!";
        };
    }

    @Override public void save(Properties p) { p.setProperty("estacoes.aniversarios", String.join(",", birthdayGifts)); }
    @Override public void load(Properties p) {
        birthdayGifts.clear();
        for (String s : p.getProperty("estacoes.aniversarios", "").split(",")) if (s.matches("[0-3]-\\d{1,6}")) birthdayGifts.add(s);
        announcedDay = -1;
    }
    @Override public String signature() { return "s" + birthdayGifts; }

    // ---------- Paisagem ----------
    /** Sobre os tiles: pétalas na primavera, copas e folhas no outono, neve nas copas no inverno. */
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        GameMap m = game.map;
        if (!m.outdoor || m.snowy || m.name.equals("Praia Azul")) return;
        int season = current(), T = Game.T;
        if (season == SUMMER) {                                                        // luz quente de verão
            g.setColor(new Color(255, 214, 120, 26)); g.fillRect(0, 0, Game.W, Game.H);
            return;
        }
        for (int y = Math.max(0, camY / T); y < Math.min(m.h, (camY + Game.H) / T + 1); y++)
            for (int x = Math.max(0, camX / T); x < Math.min(m.w, (camX + Game.W) / T + 1); x++) {
                char c = m.t[y][x]; int px = x * T - camX, py = y * T - camY, h = (x * 73856093) ^ (y * 19349663);
                if (c == '#' && season == AUTUMN) {
                    g.setColor(new Color(214, 120, 40, 120)); g.fillOval(px + 3, py + 1, 26, 22);
                    g.setColor(new Color(180, 70, 40, 90)); g.fillOval(px + 9, py + 5, 12, 10);
                } else if (c == '#' && season == WINTER) {
                    g.setColor(new Color(245, 250, 255, 200)); g.fillOval(px + 6, py + 1, 20, 8);
                } else if ((c == '.' || c == 'F') && Math.floorMod(h, 7) == 0) {
                    if (season == SPRING) {
                        g.setColor(Math.floorMod(h, 2) == 0 ? new Color(250, 200, 220) : new Color(255, 250, 240));
                        g.fillOval(px + Math.floorMod(h >> 3, 22) + 4, py + Math.floorMod(h >> 7, 22) + 4, 4, 3);
                    } else if (season == AUTUMN) {
                        g.setColor(Math.floorMod(h, 2) == 0 ? new Color(210, 110, 40) : new Color(190, 70, 50));
                        g.fillOval(px + Math.floorMod(h >> 3, 22) + 4, py + Math.floorMod(h >> 7, 22) + 4, 5, 3);
                    }
                }
            }
    }

    // ---------- Calendário ----------
    void openCalendar(int from) { returnState = from; calendarSeason = current(); game.down.clear(); game.state = Game.CALENDAR; }
    @Override public boolean owns(int state) { return state == Game.CALENDAR; }
    @Override public void key(int key) {
        if (key == KeyEvent.VK_LEFT) { calendarSeason = (calendarSeason + 3) % 4; game.audio.sfx("select"); }
        else if (key == KeyEvent.VK_RIGHT) { calendarSeason = (calendarSeason + 1) % 4; game.audio.sfx("select"); }
        else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); }
    }
    @Override public void draw(Graphics2D g) {
        int s = calendarSeason, today = shownDay(), base = ((today - 1) / (LENGTH * 4)) * LENGTH * 4 + s * LENGTH;
        game.life.panel(g, "Calendário · " + NAMES[s] + " · ano " + year(today), "← →: estações · Esc: voltar");
        g.setColor(COLORS[s]); g.fillRect(44, 104, 552, 4);
        String[] week = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};
        for (int i = 0; i < 7; i++) game.life.text(g, week[i], 62 + i * 78, 126, 70, 12);
        for (int d = 1; d <= LENGTH; d++) {
            int shown = base + d, col = Math.floorMod(shown - 1, 7), row = (d - 1 + Math.floorMod(base, 7)) / 7;
            int x = 48 + col * 78, y = 136 + row * 54;
            boolean isToday = shown == today;
            g.setColor(isToday ? new Color(255, 236, 170) : new Color(240, 246, 243)); g.fillRoundRect(x, y, 72, 48, 8, 8);
            g.setColor(isToday ? new Color(200, 140, 40) : new Color(190, 205, 200)); g.drawRoundRect(x, y, 72, 48, 8, 8);
            game.life.text(g, "" + d, x + 6, y + 16, 30, 13);
            String note = col == 6 ? "Festival" : col == 0 ? "Mascate" : "";
            int who = birthdayOf(shown);
            if (who >= 0) note = "♥ " + Bonds.NAMES[who].replace("Prof. ", "").replace("Tio ", "").replace("Dona ", "");
            if (!note.isEmpty()) {
                g.setColor(who >= 0 ? new Color(200, 70, 110) : col == 6 ? new Color(60, 140, 90) : new Color(150, 100, 60));
                g.setFont(new Font("SansSerif", Font.BOLD, 10)); g.drawString(note, x + 6, y + 38);
            }
        }
        String detail = "Festival aos sábados: " + FESTIVALS[s] + ". Peixes comuns: " + VillageLife.FISH[SEASON_FISH[s][0]].name()
            + " e " + VillageLife.FISH[SEASON_FISH[s][1]].name() + ". Flores: " + cropsIn(s) + ".";
        game.life.text(g, detail, 44, 418, 552, 12);
    }
    static String cropsIn(int season) {
        java.util.List<String> names = new ArrayList<>();
        for (int c = 0; c < 4; c++) if (CROP_SEASONS[season][c]) names.add(Gardening.NAMES[c]);
        return String.join(", ", names);
    }
}
