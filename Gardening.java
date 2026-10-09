import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Watered days advance crops once; harvesting, sales and gifts are saved transactions. */
final class Gardening {
    static final String MAP = "Quintal de Casa";
    static final String[] NAMES = {"Margarida", "Tulipa", "Girassol", "Flor Estelar"};
    static final int[] DAYS = {2, 3, 4, 5}, PRICE = {5, 9, 14, 22};
    static final Color[] COLORS = {new Color(246, 242, 225), new Color(229, 112, 153), new Color(245, 192, 66), new Color(180, 157, 227)};
    static final int[][] PLOTS = {{4, 4}, {7, 4}, {10, 4}, {4, 7}, {7, 7}, {10, 7}};
    final Game game;
    final int[] crops = new int[6], growth = new int[6], watered = new int[6], lastDay = new int[6], stock = new int[4];
    boolean can;
    int selected, plot = -1, target = -1;
    boolean selling;
    String status = "";
    Gardening(Game game) { this.game = game; reset(); }
    void reset() { Arrays.fill(crops, -1); Arrays.fill(growth, 0); Arrays.fill(watered, -1); Arrays.fill(lastDay, 1); Arrays.fill(stock, 0); can = false; }
    void buildWorld() {
        GameMap village = game.maps.get("Vila Aurora");
        village.rect(9, 6, 1, 5, '=');
        village.addNpc(new Npc("yard_gate", "Quintal de Casa", 9, 6, COLORS[2]).obj("portao").still());
        GameMap yard = new GameMap(MAP, 16, 14, '.', true);
        yard.border('#'); yard.rect(6, 9, 2, 5, '=');
        yard.warps.add(new Warp(6, 13, "Vila Aurora", 9, 7));
        yard.warps.add(new Warp(7, 13, "Vila Aurora", 9, 7));
        yard.addNpc(new Npc("yard_tools", "Bancada de jardinagem", 3, 10, COLORS[1]).obj("yard_tools").still());
        for (int i = 0; i < PLOTS.length; i++) yard.addNpc(new Npc("yard_plot" + i, "Canteiro", PLOTS[i][0], PLOTS[i][1], COLORS[0]).obj("yard_plot").still());
        yard.rect(12, 9, 2, 2, '~');
        game.maps.put(MAP, yard);
    }
    boolean unlocked(int type) { return type >= 0 && type < 4 && (type < 3 || game.stage >= 28); }
    boolean wet(int i) { return watered[i] == game.weather.day() || game.weather.current() == Weather.RAIN; }
    void sync() {
        int today = game.weather.day();
        for (int i = 0; i < 6; i++) {
            if (crops[i] < 0) { lastDay[i] = today; continue; }
            for (int d = lastDay[i]; d < today && growth[i] < DAYS[crops[i]]; d++)
                if (Seasons.grows(crops[i], d) && (watered[i] == d || game.weather.forDay(d) == Weather.RAIN)) growth[i]++;
            lastDay[i] = today;
        }
    }
    boolean ready(int i) { return crops[i] >= 0 && growth[i] >= DAYS[crops[i]]; }
    boolean talk(Npc npc) {
        if (npc.id.equals("yard_gate")) { game.travel(MAP, 6, 12); return true; }
        if (npc.id.equals("yard_tools")) {
            can = true;
            game.say("", "Você pega o regador e encontra sementes de margarida, tulipa e girassol. A bancada mantém uma reserva para seus canteiros.",
                "Plante com Z no canteiro vazio. Depois, Z rega ou colhe. Cada dia regado conta para o crescimento; a chuva também cuida das flores.",
                "V abre a cesta. Diante de um morador, permite presentear; diante da loja de Rosa, permite vender."); return true;
        }
        if (!npc.id.startsWith("yard_plot")) return false;
        sync(); int i = npc.id.charAt(9) - '0';
        if (!can) { game.say("", "O regador e as sementes estão na bancada do quintal."); return true; }
        if (crops[i] < 0) { open(); plot = i; }
        else if (ready(i)) { if (harvest(i)) game.toast("Flor colhida!"); else game.toast("Não foi possível colher."); }
        else {
            boolean changed = water(i);
            game.toast((changed ? "Regado" : wet(i) ? "Terra úmida" : "Falha ao regar")
                + " · " + NAMES[crops[i]] + " · " + growth[i] + "/" + DAYS[crops[i]] + " dias");
        }
        return true;
    }
    void open() { sync(); plot = -1; target = -1; selling = false; selected = 0; status = ""; game.down.clear(); game.state = Game.GARDENING; }
    void openBasket() {
        open(); int x = game.tx + Game.DX[game.dir], y = game.ty + Game.DY[game.dir];
        for (Npc n : game.map.npcs) if (game.vis(n) && n.x == x && n.y == y) {
            target = game.bonds.residentIndex(n.id);
            selling = n.id.equals("rosa_shop");
            if (selling && (game.hour() < 6 || game.hour() >= 21)) { selling = false; status = "A loja abre das 6h às 21h."; }
            break;
        }
    }
    boolean transaction(Runnable change) {
        sync(); Properties before = new Properties(); save(before);
        int coins = game.home.coins; int[] friends = game.bonds.friendship.clone(), gifted = game.bonds.gifted.clone();
        change.run();
        if (game.save(false)) return true;
        load(before); game.home.coins = coins;
        System.arraycopy(friends, 0, game.bonds.friendship, 0, 4); System.arraycopy(gifted, 0, game.bonds.gifted, 0, 4);
        return false;
    }
    boolean plant(int i, int type) {
        if (!can || i < 0 || i >= 6 || crops[i] != -1 || !unlocked(type) || !Seasons.grows(type, game.weather.day())) return false;
        return transaction(() -> { crops[i] = type; growth[i] = 0; watered[i] = -1; lastDay[i] = game.weather.day(); });
    }
    boolean water(int i) {
        if (!can || crops[i] < 0 || ready(i) || wet(i)) return false;
        return transaction(() -> watered[i] = game.weather.day());
    }
    boolean harvest(int i) {
        sync(); if (!ready(i) || stock[crops[i]] >= 999999) return false;
        return transaction(() -> { stock[crops[i]]++; crops[i] = -1; growth[i] = 0; watered[i] = -1; });
    }
    boolean sell(int type) {
        if (stock[type] == 0 || game.home.coins > HomeDecor.MAX_COINS - PRICE[type] || game.hour() < 6 || game.hour() >= 21) return false;
        return transaction(() -> { stock[type]--; game.home.coins += PRICE[type]; });
    }
    boolean give(int who, int type) {
        if (stock[type] == 0 || game.bonds.gifted[who] == game.bonds.day()) return false;
        return transaction(() -> { stock[type]--; game.bonds.gifted[who] = game.bonds.day(); game.bonds.addFriendship(who, game.giftPoints(who, who == 2 ? 15 : 8)); });
    }
    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_V) { game.state = Game.PLAY; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) selected = (selected + 3) % 4;
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) selected = (selected + 1) % 4;
        if (key != KeyEvent.VK_Z && key != KeyEvent.VK_ENTER) return;
        if (plot >= 0) {
            if (plant(plot, selected)) { game.state = Game.PLAY; game.toast("Sementes plantadas."); }
            else status = unlocked(selected) && !Seasons.grows(selected, game.weather.day())
                ? NAMES[selected] + " não cresce no " + game.seasons.name().toLowerCase() + ". Plantas fora de estação ficam esperando."
                : "Sementes indisponíveis ou falha ao salvar.";
        } else if (selling) status = sell(selected) ? "Uma flor vendida." : "Venda indisponível ou falha ao salvar.";
        else if (target >= 0) {
            int who = target, type = selected;
            game.access.ask("Entregar 1 " + NAMES[type] + " para " + Bonds.NAMES[who] + "?", () ->
                status = give(who, type) ? "Presente entregue!" : "Sem flores, presente diário já entregue ou falha ao salvar.");
        }
    }
    static String seasonsOf(int crop) {
        java.util.List<String> names = new ArrayList<>();
        for (int s = 0; s < 4; s++) if (Seasons.CROP_SEASONS[s][crop]) names.add(Seasons.NAMES[s]);
        return names.size() == 4 ? "todas as estações" : String.join(", ", names);
    }
    void save(Properties p) {
        p.setProperty("horta.regador", Boolean.toString(can));
        int[][] arrays = {crops, growth, watered, lastDay, stock};
        for (int a = 0; a < arrays.length; a++) for (int i = 0; i < arrays[a].length; i++) p.setProperty("horta." + a + "." + i, "" + arrays[a][i]);
    }
    int read(Properties p, int a, int i, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(p.getProperty("horta." + a + "." + i, "" + fallback)))); }
        catch (NumberFormatException e) { return fallback; }
    }
    void load(Properties p) {
        reset(); can = Boolean.parseBoolean(p.getProperty("horta.regador", "false")); int today = game.weather.day();
        for (int i = 0; i < 6; i++) {
            crops[i] = read(p, 0, i, -1, -1, 3);
            growth[i] = crops[i] < 0 ? 0 : read(p, 1, i, 0, 0, DAYS[crops[i]]);
            watered[i] = read(p, 2, i, -1, -1, today);
            lastDay[i] = read(p, 3, i, today, 1, today);
        }
        for (int i = 0; i < 4; i++) stock[i] = read(p, 4, i, 0, 0, 999999);
    }
    String signature() { return can + Arrays.toString(crops) + Arrays.toString(growth) + Arrays.toString(watered) + Arrays.toString(stock); }
    boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.equals("yard_tools")) {
            g.setColor(new Color(144, 103, 73)); g.fillRect(x, y + 14, 32, 17);
            g.setColor(new Color(69, 155, 172)); g.fillRect(x + 5, y + 5, 15, 12); g.drawOval(x + 16, y + 2, 11, 11); return true;
        }
        if (!n.id.startsWith("yard_plot")) return false;
        int i = n.id.charAt(9) - '0';
        g.setColor(wet(i) ? new Color(91, 79, 66) : new Color(150, 111, 73)); g.fillRect(x, y + 6, 32, 26);
        g.setColor(new Color(188, 151, 111)); g.drawRect(x, y + 6, 31, 25);
        if (crops[i] >= 0) {
            g.setColor(new Color(64, 143, 83)); g.fillRect(x + 15, y + 12, 3, 16);
            g.fillOval(x + 7, y + 16, 11, 5);
            if (ready(i)) {
                for (int petal = 0; petal < 5; petal++) { g.setColor(COLORS[crops[i]]); g.fillOval(x + 11 + (int)(Math.cos(petal * 1.257) * 7), y + 5 + (int)(Math.sin(petal * 1.257) * 7), 8, 8); }
                g.setColor(new Color(245, 209, 84)); g.fillOval(x + 13, y + 7, 5, 5);
            } else { g.setColor(COLORS[crops[i]]); g.fillOval(x + 13, y + 9, 6, 6); }
            for (int d = 0; d < DAYS[crops[i]]; d++) {
                g.setColor(d < growth[i] ? new Color(228, 228, 126) : new Color(66, 67, 56));
                g.fillRect(x + 3 + d * 5, y + 28, 3, 2);
            }
        }
        return true;
    }
    void draw(Graphics2D g) {
        String title = plot >= 0 ? "Canteiro " + (plot + 1) : selling ? "Flores para Dona Rosa" : target >= 0 ? "Presente para " + Bonds.NAMES[target] : "Cesta de flores";
        game.life.panel(g, title, "Regador: " + (can ? "disponível" : "na bancada") + "   Moedas: " + game.home.coins);
        for (int i = 0; i < 4; i++) {
            int y = 122 + i * 62;
            if (selected == i) { g.setColor(new Color(206, 232, 217)); g.fillRect(40, y - 22, 556, 54); }
            g.setColor(COLORS[i]); g.fillOval(51, y - 10, 19, 19);
            boolean known = unlocked(i) || stock[i] > 0;
            game.life.text(g, known ? NAMES[i] : "Semente desconhecida", 86, y, 310, 16);
            game.life.text(g, known ? DAYS[i] + " dias regados · " + PRICE[i] + " moedas · " + seasonsOf(i) : "Após concluir o capítulo 5", 86, y + 21, 400, 13);
            game.life.text(g, "× " + stock[i], 495, y, 90, 16);
        }
        game.life.text(g, status, 44, 390, 548, 14);
        if (plot >= 0) game.life.text(g, "Canteiro vazio", 44, 438, 400, 14);
    }
}
