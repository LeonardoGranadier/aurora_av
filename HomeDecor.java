import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;

/** Loja, estoque de vendas e decoracao em tiles da casa original. */
final class HomeDecor {
    static final int SHOP = 0, SELL = 1, DECOR = 2, STAR = 14, MAX_COINS = 1_000_000_000;
    static final int[] FISH_PRICES = {5, 12, 14, 8, 7, 18, 22, 30};
    record Item(String id, String name, int price, int w, int h, boolean solid, Color color) {}
    static final Item[] ITEMS = {
        new Item("vaso", "Vaso de flores", 10, 1, 1, true, new Color(84, 165, 118)),
        new Item("cadeira", "Cadeira", 12, 1, 1, true, new Color(196, 151, 98)),
        new Item("mesa", "Mesa de jantar", 25, 2, 1, true, new Color(181, 127, 76)),
        new Item("sofa", "Sofá verde", 35, 2, 1, true, new Color(77, 151, 130)),
        new Item("estante", "Estante de livros", 30, 2, 1, true, new Color(157, 109, 75)),
        new Item("tapete", "Tapete azul", 18, 2, 2, false, new Color(80, 145, 184)),
        new Item("luminaria", "Luminária", 20, 1, 1, true, new Color(241, 206, 105)),
        new Item("banco", "Banco da varanda", 16, 2, 1, true, new Color(183, 151, 108)),
        new Item("trofeu", "Troféu de pesca", 0, 1, 1, true, new Color(241, 196, 65)),
        new Item("medalha", "Medalha dos sinos", 0, 1, 1, true, new Color(113, 175, 221)),
        new Item("caderno", "Caderno do professor", 0, 1, 1, true, new Color(91, 150, 162)),
        new Item("boia", "Boia de cortiça", 0, 1, 1, true, new Color(220, 127, 92)),
        new Item("flor", "Flor prensada", 0, 1, 1, true, new Color(220, 118, 164)),
        new Item("apito", "Apito da amizade", 0, 1, 1, true, new Color(191, 156, 80)),
        new Item("estrela", "Estrela de Aurora", 0, 1, 1, true, new Color(246, 204, 76)),
        new Item("flor_constelacao", "Flor-Constelação", 0, 1, 1, true, new Color(234, 170, 211)),
        new Item("lembranca_vila", "Lembrança da Vila", 0, 1, 1, true, new Color(93, 167, 156))
    };
    final Game game;
    int coins;
    final int[] sold = new int[8];
    final boolean[] owned = new boolean[8];
    final int[] xs = new int[ITEMS.length], ys = new int[ITEMS.length], rotations = new int[ITEMS.length];
    int mode, selected, amount = 1, returnState, cursorX, cursorY, rotation;
    boolean positioning;
    String status = "";

    HomeDecor(Game game) { this.game = game; reset(); }
    void reset() {
        coins = 0; Arrays.fill(sold, 0); Arrays.fill(owned, false);
        Arrays.fill(xs, -1); Arrays.fill(ys, -1); Arrays.fill(rotations, 0);
        positioning = false; selected = 0; amount = 1; status = "";
    }
    void buildWorld() {
        GameMap village = game.maps.get("Vila Aurora");
        village.addNpc(new Npc("rosa_shop", "Loja da Dona Rosa", 8, 8, new Color(211, 106, 148)).obj("rosa_shop").still());
        village.addNpc(new Npc("beto_market", "Banca do Tio Beto", 19, 12, new Color(62, 149, 128)).obj("beto_market").still());
    }
    boolean talk(Npc npc) {
        if (!npc.id.equals("rosa_shop") && !npc.id.equals("beto_market")) return false;
        if (game.hour() < 6 || game.hour() >= 21) {
            game.say(npc.name, "Voltamos às 6h. Nosso horário é das 6h às 21h."); return true;
        }
        mode = npc.id.equals("rosa_shop") ? SHOP : SELL;
        selected = 0; amount = 1; status = ""; returnState = Game.PLAY; game.state = Game.HOME;
        return true;
    }
    void openDecor(int from) {
        if (!game.map.name.equals("Casa")) { game.toast("A decoração fica em casa."); return; }
        mode = DECOR; selected = 0; returnState = from; positioning = false; status = "";
        cursorX = game.tx; cursorY = game.ty; game.state = Game.HOME;
    }
    boolean unlocked(int id) {
        if (id < 8) return owned[id];
        if (id == 8) return game.life.completeAlbum();
        if (id == 9) return game.life.bellBest >= VillageLife.BELL_ROUNDS;
        if (id < STAR) return game.bonds.quests[id - 10] == 2;
        return id == STAR ? game.festival.prizes > 0 : id == 15 ? game.stage >= 28 : game.orders.total >= 10;
    }
    boolean placed(int id) { return unlocked(id) && xs[id] >= 0 && ys[id] >= 0; }
    int width(int id, int r) { return r % 2 == 0 ? ITEMS[id].w() : ITEMS[id].h(); }
    int height(int id, int r) { return r % 2 == 0 ? ITEMS[id].h() : ITEMS[id].w(); }
    boolean covers(int id, int x, int y) {
        return placed(id) && x >= xs[id] && y >= ys[id]
            && x < xs[id] + width(id, rotations[id]) && y < ys[id] + height(id, rotations[id]);
    }
    boolean blocks(int x, int y, int except) {
        for (int id = 0; id < ITEMS.length; id++) if (id != except && ITEMS[id].solid() && covers(id, x, y)) return true;
        return false;
    }
    boolean commit(Runnable undo) {
        if (game.save(false)) { game.audio.sfx("confirm"); return true; }
        undo.run(); syncStar(); return false;
    }
    boolean buy(int id) {
        if (id < 0 || id >= 8 || owned[id] || coins < ITEMS[id].price()) return false;
        int oldCoins = coins; coins -= ITEMS[id].price(); owned[id] = true;
        return commit(() -> { coins = oldCoins; owned[id] = false; });
    }
    boolean sell(int fish, int count) {
        if (fish < 0 || fish >= 8 || count <= 0 || count > game.bonds.stock(fish)) return false;
        long value = (long) count * FISH_PRICES[fish];
        if (value > MAX_COINS - coins) return false;
        int oldCoins = coins, oldSold = sold[fish];
        coins += (int) value; sold[fish] += count;
        return commit(() -> { coins = oldCoins; sold[fish] = oldSold; });
    }

    boolean fixedNpc(int x, int y) {
        for (Npc n : game.maps.get("Casa").npcs)
            if (!n.id.equals("festival_souvenir") && n.x == x && n.y == y) return true;
        return false;
    }
    boolean emptyFloor(int x, int y) {
        return !game.maps.get("Casa").solid(x, y) && !fixedNpc(x, y) && !blocks(x, y, -1);
    }
    boolean layoutAccessible() {
        GameMap house = game.maps.get("Casa");
        if (!emptyFloor(5, 6) || !emptyFloor(5, 5) || !emptyFloor(5, 7)) return false;
        Set<Point> visited = new HashSet<>(); ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(new Point(5, 6)); visited.add(new Point(5, 6));
        while (!queue.isEmpty()) {
            Point p = queue.remove();
            for (int d = 0; d < 4; d++) {
                Point next = new Point(p.x + Game.DX[d], p.y + Game.DY[d]);
                if (emptyFloor(next.x, next.y) && visited.add(next)) queue.add(next);
            }
        }
        // Nenhum trecho de piso pode virar uma ilha inacessivel.
        for (int y = 0; y < house.h; y++) for (int x = 0; x < house.w; x++)
            if (emptyFloor(x, y) && !visited.contains(new Point(x, y))) return false;
        for (Npc n : house.npcs) {
            if (n.id.equals("festival_souvenir")) continue;
            boolean reachable = false;
            for (int d = 0; d < 4; d++) reachable |= visited.contains(new Point(n.x + Game.DX[d], n.y + Game.DY[d]));
            if (!reachable) return false;
        }
        return true;
    }
    boolean canPlace(int id, int x, int y, int r) {
        if (id < 0 || id >= ITEMS.length || !unlocked(id) || r < 0 || r > 3) return false;
        GameMap house = game.maps.get("Casa");
        int w = width(id, r), h = height(id, r);
        if (x < 0 || y < 0 || x > house.w - w || y > house.h - h) return false;
        for (int yy = y; yy < y + h; yy++) for (int xx = x; xx < x + w; xx++) {
            if (house.at(xx, yy) != 'f' || fixedNpc(xx, yy)) return false;
            if (ITEMS[id].solid() && (xx == 5 && (yy == 5 || yy == 6)
                    || game.map == house && xx == game.tx && yy == game.ty)) return false;
            for (int other = 0; other < ITEMS.length; other++)
                if (id != other && ITEMS[id].solid() == ITEMS[other].solid() && covers(other, xx, yy)) return false;
        }
        int oldX = xs[id], oldY = ys[id], oldR = rotations[id];
        xs[id] = x; ys[id] = y; rotations[id] = r;
        try { return layoutAccessible(); }
        finally { xs[id] = oldX; ys[id] = oldY; rotations[id] = oldR; }
    }
    boolean place(int id, int x, int y, int r) {
        if (!canPlace(id, x, y, r)) return false;
        int oldX = xs[id], oldY = ys[id], oldR = rotations[id];
        xs[id] = x; ys[id] = y; rotations[id] = r; syncStar();
        return commit(() -> { xs[id] = oldX; ys[id] = oldY; rotations[id] = oldR; });
    }
    boolean remove(int id) {
        if (!placed(id)) return false;
        int oldX = xs[id], oldY = ys[id]; xs[id] = -1; ys[id] = -1;
        if (!layoutAccessible()) { xs[id] = oldX; ys[id] = oldY; return false; }
        syncStar();
        return commit(() -> { xs[id] = oldX; ys[id] = oldY; });
    }
    void syncStar() {
        for (Npc n : game.maps.get("Casa").npcs)
            if (n.id.equals("festival_souvenir") && placed(STAR)) { n.x = xs[STAR]; n.y = ys[STAR]; }
    }
    void placeFestivalStar() {
        if (placed(STAR)) return;
        if (canPlace(STAR, 7, 4, 0)) { xs[STAR] = 7; ys[STAR] = 4; }
        else {
            for (int y = 1; y < 7 && xs[STAR] < 0; y++) for (int x = 1; x < 9; x++) {
                if (canPlace(STAR, x, y, 0)) { xs[STAR] = x; ys[STAR] = y; break; }
            }
        }
        syncStar();
    }

    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE) {
            if (mode == DECOR && positioning) { positioning = false; status = ""; return; }
            game.state = returnState; game.down.clear(); return;
        }
        if (mode == DECOR && positioning) {
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) cursorY = Math.max(0, cursorY - 1);
            if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) cursorY = Math.min(7, cursorY + 1);
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) cursorX = Math.max(0, cursorX - 1);
            if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) cursorX = Math.min(9, cursorX + 1);
            if (key == KeyEvent.VK_R) rotation = (rotation + 1) % 4;
            if (key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER) {
                if (place(selected, cursorX, cursorY, rotation)) { positioning = false; status = "Lugar atualizado."; }
                else status = "Posição indisponível ou caminho bloqueado.";
            }
            return;
        }
        int count = mode == DECOR ? ITEMS.length : 8;
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) { selected = (selected + count - 1) % count; amount = 1; status = ""; }
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) { selected = (selected + 1) % count; amount = 1; status = ""; }
        if (mode == SELL) {
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) amount = Math.max(1, amount - 1);
            if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) amount = Math.max(1, Math.min(game.bonds.stock(selected), amount + 1));
        }
        if (mode == DECOR && (key == KeyEvent.VK_DELETE || key == KeyEvent.VK_BACK_SPACE)) {
            status = remove(selected) ? "Guardado." : "Não foi possível guardar. Confira a posição e os caminhos."; return;
        }
        if (key != KeyEvent.VK_Z && key != KeyEvent.VK_ENTER) return;
        if (mode == SHOP) status = buy(selected) ? "Comprado: " + ITEMS[selected].name() : owned[selected] ? "Você já tem este móvel." : "Compra não realizada. Confira o saldo.";
        else if (mode == SELL) {
            status = sell(selected, amount) ? "Venda concluída." : "Venda não realizada. Confira o estoque.";
            amount = 1;
        } else if (unlocked(selected)) {
            positioning = true; cursorX = placed(selected) ? xs[selected] : game.tx;
            cursorY = placed(selected) ? ys[selected] : game.ty; rotation = rotations[selected]; status = "";
        } else status = selected < 8 ? "Disponível na loja da Dona Rosa." : "Uma lembrança de uma conquista futura.";
    }

    String signature() { return coins + Arrays.toString(sold) + Arrays.toString(owned) + Arrays.toString(xs) + Arrays.toString(ys) + Arrays.toString(rotations); }
    void save(Properties p) {
        p.setProperty("casa.versao", "1"); p.setProperty("casa.moedas", Integer.toString(coins));
        for (int i = 0; i < 8; i++) {
            p.setProperty("casa.vendido." + i, Integer.toString(sold[i]));
            p.setProperty("casa.comprado." + ITEMS[i].id(), Boolean.toString(owned[i]));
        }
        for (int i = 0; i < ITEMS.length; i++) {
            String key = "casa.movel." + ITEMS[i].id();
            p.setProperty(key + ".x", Integer.toString(xs[i])); p.setProperty(key + ".y", Integer.toString(ys[i]));
            p.setProperty(key + ".r", Integer.toString(rotations[i]));
        }
    }
    int coordinate(Properties p, String key) {
        try { return Integer.parseInt(p.getProperty(key, "-1")); }
        catch (NumberFormatException e) { return -1; }
    }
    void load(Properties p) {
        reset(); coins = game.life.number(p, "casa.moedas", 0, MAX_COINS);
        for (int i = 0; i < 8; i++) {
            sold[i] = Math.min(game.life.number(p, "casa.vendido." + i, 0, 999999), Math.max(0, game.life.catches[i] - game.bonds.spent[i]));
            owned[i] = Boolean.parseBoolean(p.getProperty("casa.comprado." + ITEMS[i].id(), "false"));
        }
        for (int i = 0; i < ITEMS.length; i++) {
            String key = "casa.movel." + ITEMS[i].id();
            int x = coordinate(p, key + ".x"), y = coordinate(p, key + ".y"), r = coordinate(p, key + ".r");
            if (unlocked(i) && x >= 0 && x < 10 && y >= 0 && y < 8 && r >= 0 && r <= 3) { xs[i] = x; ys[i] = y; rotations[i] = r; }
        }
        // Valida o conjunto completo: a ordem do catalogo nao e a ordem de colocacao.
        for (int i = 0; i < ITEMS.length; i++)
            if (placed(i) && !canPlace(i, xs[i], ys[i], rotations[i])) { xs[i] = -1; ys[i] = -1; }
        if (!p.containsKey("casa.versao") && game.festival.prizes > 0) placeFestivalStar();
        syncStar();
    }

    boolean drawStall(Graphics2D g, Npc n, int x, int y) {
        if (!n.id.equals("rosa_shop") && !n.id.equals("beto_market")) return false;
        boolean rosa = n.id.equals("rosa_shop");
        g.setColor(new Color(139, 108, 77)); g.fillRect(x + 2, y + 15, 28, 16);
        for (int i = 0; i < 4; i++) {
            g.setColor(i % 2 == 0 ? (rosa ? new Color(224, 124, 158) : new Color(81, 167, 167)) : new Color(242, 237, 207));
            g.fillRect(x + i * 8, y + 1, 8, 12);
        }
        if (rosa) drawItem(g, 0, x + 8, y + 15, 18, 0);
        else game.life.drawFish(g, x + 7, y + 20, 15, new Color(140, 197, 224));
        return true;
    }
    void drawFurniture(Graphics2D g, int camX, int camY, int layer) {
        if (!game.map.name.equals("Casa")) return;
        for (int i = 0; i < ITEMS.length; i++) {
            if (!placed(i) || i == STAR) continue;
            int target = !ITEMS[i].solid() ? 0 : ys[i] + height(i, rotations[i]) - 1 <= game.ty ? 1 : 2;
            if (target == layer) drawItem(g, i, xs[i] * Game.T - camX, ys[i] * Game.T - camY, Game.T, rotations[i]);
        }
    }
    void drawItem(Graphics2D g, int id, int x, int y, int cell, int r) {
        Graphics2D a = (Graphics2D) g.create(); a.translate(x, y); a.scale(cell / 32.0, cell / 32.0);
        int w = ITEMS[id].w() * 32, h = ITEMS[id].h() * 32;
        if (r == 1) { a.translate(h, 0); a.rotate(Math.PI / 2); }
        else if (r == 2) { a.translate(w, h); a.rotate(Math.PI); }
        else if (r == 3) { a.translate(0, w); a.rotate(-Math.PI / 2); }
        Color color = ITEMS[id].color(); a.setColor(color);
        switch (id) {
            case 0 -> {
                a.setColor(new Color(180, 108, 83)); a.fillPolygon(new int[]{7, 25, 22, 10}, new int[]{17, 17, 30, 30}, 4);
                a.setColor(color); a.fillOval(3, 7, 16, 12); a.fillOval(14, 2, 15, 17);
                a.setColor(new Color(242, 165, 190)); a.fillOval(10, 4, 9, 9);
            }
            case 1 -> { a.fillRect(5, 4, 22, 8); a.fillRoundRect(5, 12, 22, 13, 4, 4); a.setColor(color.darker()); a.fillRect(6, 25, 4, 6); a.fillRect(22, 25, 4, 6); }
            case 2, 7 -> { a.fillRoundRect(2, 5, w - 4, id == 2 ? 21 : 16, 4, 4); a.setColor(color.darker()); a.fillRect(6, 26, 5, 5); a.fillRect(w - 11, 26, 5, 5); a.drawLine(5, 10, w - 5, 10); }
            case 3 -> {
                a.fillRoundRect(2, 4, w - 4, 26, 6, 6); a.setColor(color.brighter());
                a.fillRoundRect(8, 13, 23, 13, 4, 4); a.fillRoundRect(33, 13, 23, 13, 4, 4);
                a.setColor(color.darker()); a.fillRect(2, 13, 5, 15); a.fillRect(w - 7, 13, 5, 15);
            }
            case 4 -> {
                a.fillRect(2, 2, w - 4, 29);
                for (int i = 0; i < 9; i++) { a.setColor(VillageLife.BELL_COLORS[i % 4]); a.fillRect(7 + i * 5, 7, 4, 16); }
                a.setColor(color.darker()); a.fillRect(4, 24, w - 8, 3);
            }
            case 5 -> {
                a.fillRect(2, 2, w - 4, h - 4); a.setColor(new Color(179, 223, 225));
                a.drawRect(6, 6, w - 13, h - 13); a.drawRect(10, 10, w - 21, h - 21);
                a.fillPolygon(new int[]{32, 43, 32, 21}, new int[]{19, 32, 45, 32}, 4);
            }
            case 6 -> {
                a.setColor(new Color(100, 111, 115)); a.fillRect(14, 13, 4, 16); a.fillOval(8, 26, 17, 5);
                a.setColor(color); a.fillPolygon(new int[]{9, 23, 28, 4}, new int[]{2, 2, 16, 16}, 4);
            }
            case 8, 9 -> { a.setColor(new Color(141, 103, 76)); a.fillRect(3, 26, 26, 5); game.life.drawItem(a, id == 8 ? "trofeu" : "sinos", 3, 1, 26); }
            case 10 -> { a.fillRect(5, 7, 23, 20); a.setColor(new Color(240, 238, 209)); a.fillRect(9, 9, 16, 16); a.setColor(color); a.drawLine(12, 14, 22, 14); a.drawLine(12, 19, 22, 19); }
            case 11 -> { a.setColor(new Color(145, 109, 78)); a.fillRect(3, 25, 26, 5); a.setColor(Color.WHITE); a.fillOval(10, 6, 12, 19); a.setColor(color); a.fillArc(10, 6, 12, 19, 0, 180); }
            case 12 -> { a.setColor(new Color(180, 144, 95)); a.fillRect(4, 3, 24, 27); a.setColor(new Color(245, 239, 212)); a.fillRect(7, 6, 18, 21); a.setColor(new Color(74, 145, 101)); a.fillRect(15, 15, 2, 10); a.setColor(color); a.fillOval(10, 8, 12, 12); }
            case 13 -> { a.setColor(new Color(141, 103, 76)); a.fillRect(3, 25, 26, 5); a.setColor(color); a.fillRoundRect(4, 14, 24, 9, 4, 4); a.setColor(VillageLife.INK); a.fillOval(9, 16, 4, 4); }
            case STAR -> {
                int[] xx = new int[10], yy = new int[10];
                for (int i = 0; i < 10; i++) { double angle = -Math.PI / 2 + i * Math.PI / 5; int radius = i % 2 == 0 ? 14 : 6; xx[i] = 16 + (int) (Math.cos(angle) * radius); yy[i] = 16 + (int) (Math.sin(angle) * radius); }
                a.fillPolygon(xx, yy, 10);
            }
            case 15 -> {
                a.setColor(new Color(89, 149, 153)); a.fillRoundRect(7, 20, 18, 11, 4, 4);
                a.setColor(new Color(66, 134, 92)); a.fillRect(15, 10, 3, 13);
                game.chapter.star(a, 16, 11, 10, color);
                game.chapter.star(a, 16, 11, 4, new Color(253, 226, 120));
            }
            case 16 -> {
                a.setColor(new Color(137, 105, 74)); a.fillRect(3, 26, 26, 5);
                a.setColor(color); a.fillRect(7, 12, 19, 14);
                a.setColor(new Color(215, 135, 113)); a.fillPolygon(new int[]{4, 16, 29}, new int[]{13, 2, 13}, 3);
                a.setColor(new Color(245, 225, 147)); a.fillRect(14, 18, 6, 8);
            }
            default -> { }
        }
        a.dispose();
    }
    void draw(Graphics2D g) {
        if (mode == DECOR) { drawEditor(g); return; }
        game.life.panel(g, mode == SHOP ? "Loja da Dona Rosa" : "Banca do Tio Beto", "Moedas: " + coins);
        for (int i = 0; i < 8; i++) {
            int y = 109 + i * 34;
            if (selected == i) { g.setColor(new Color(205, 231, 220)); g.fillRoundRect(38, y, 558, 33, 6, 6); }
            if (mode == SHOP) {
                drawItem(g, i, 49, y + 3, 25 / Math.max(ITEMS[i].w(), ITEMS[i].h()), 0);
                game.life.text(g, ITEMS[i].name(), 89, y + 23, 270, 15);
                game.life.text(g, owned[i] ? "Comprado" : ITEMS[i].price() + " moedas", 455, y + 23, 132, 13);
            } else {
                game.life.drawFish(g, 49, y + 9, 24, VillageLife.FISH[i].color());
                game.life.text(g, VillageLife.FISH[i].name(), 89, y + 23, 255, 15);
                game.life.text(g, "× " + game.bonds.stock(i), 365, y + 23, 85, 13);
                game.life.text(g, FISH_PRICES[i] + " moedas", 483, y + 23, 112, 13);
            }
        }
        game.life.text(g, mode == SELL ? "‹  " + amount + "  ›     Total: " + ((long) amount * FISH_PRICES[selected]) + " moedas" : status, 46, 403, 542, 14);
        if (mode == SELL && !status.isEmpty()) game.life.text(g, status, 46, 424, 500, 12);
        game.life.text(g, "↑ ↓    Z  ↵", 46, 445, 230, 13); game.life.text(g, "ESC", 548, 445, 50, 13);
    }
    void drawEditor(Graphics2D g) {
        g.setColor(VillageLife.PAPER); g.fillRect(0, 0, Game.W, Game.H);
        game.life.text(g, "Meu cantinho em Aurora", 20, 35, 530, 23);
        game.life.text(g, "Moedas: " + coins, 20, 62, 280, 14);
        GameMap house = game.maps.get("Casa"); int bx = 16, by = 118;
        for (int y = 0; y < house.h; y++) for (int x = 0; x < house.w; x++) game.tile(g, house, x, y, bx + x * 32, by + y * 32);
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < ITEMS.length; i++)
            if (placed(i) && ITEMS[i].solid() == (pass == 1)) drawItem(g, i, bx + xs[i] * 32, by + ys[i] * 32, 32, rotations[i]);
        for (Npc n : house.npcs) if (!n.id.equals("festival_souvenir")) game.drawNpc(g, n, -bx, -by);
        game.person(g, "player", bx + game.tx * 32, by + game.ty * 32 - 4, new Color(220, 50, 50), game.dir, false, 0, true);
        g.setColor(new Color(255, 255, 255, 50));
        for (int x = 0; x <= house.w; x++) g.drawLine(bx + x * 32, by, bx + x * 32, by + 256);
        for (int y = 0; y <= house.h; y++) g.drawLine(bx, by + y * 32, bx + 320, by + y * 32);
        if (positioning) {
            boolean valid = canPlace(selected, cursorX, cursorY, rotation);
            Shape clip = g.getClip(); g.clipRect(bx, by, 320, 256);
            Composite old = g.getComposite(); g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.65f));
            drawItem(g, selected, bx + cursorX * 32, by + cursorY * 32, 32, rotation); g.setComposite(old);
            g.setColor(valid ? new Color(51, 170, 109) : new Color(215, 76, 80)); g.setStroke(new BasicStroke(3));
            g.drawRect(bx + cursorX * 32 + 1, by + cursorY * 32 + 1, width(selected, rotation) * 32 - 3, height(selected, rotation) * 32 - 3);
            g.setStroke(new BasicStroke(1)); g.setClip(clip);
        }
        int start = Math.max(0, Math.min(ITEMS.length - 8, selected - 3));
        for (int row = 0; row < 8; row++) {
            int i = start + row, y = 111 + row * 35;
            if (selected == i) { g.setColor(new Color(205, 231, 220)); g.fillRoundRect(347, y, 280, 33, 6, 6); }
            game.life.text(g, ITEMS[i].name(), 357, y + 16, 265, 13);
            game.life.text(g, !unlocked(i) ? "A descobrir" : placed(i) ? "Na casa" : "Guardado", 357, y + 30, 263, 10);
        }
        game.life.text(g, (selected + 1) + " / " + ITEMS.length, 555, 96, 70, 12);
        game.life.text(g, status, 20, 412, 600, 13);
        game.life.text(g, positioning ? "↑ ↓ ← →     R ↻     Z ✓" : "↑ ↓     Z ↵     DEL −", 20, 450, 450, 15);
        game.life.text(g, "ESC", 578, 450, 50, 13);
    }
}
