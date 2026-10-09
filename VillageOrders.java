import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;

/** A saved daily selection. Item numbers 0..7 are fish, 8..11 are flowers. */
final class VillageOrders {
    record Order(int item, int count, int who) {
        String name() { return item < 8 ? VillageLife.FISH[item].name() : Gardening.NAMES[item - 8]; }
        int reward() { return count * (item < 8 ? HomeDecor.FISH_PRICES[item] : Gardening.PRICE[item - 8]) + 8; }
    }
    final Game game;
    final List<Order> orders = new ArrayList<>();
    int day = -1, done, total, selected;
    String status = "";
    VillageOrders(Game game) { this.game = game; }
    void reset() { orders.clear(); day = -1; done = total = selected = 0; status = ""; }
    void buildWorld() {
        Game.GameMap village = game.maps.get("Vila Aurora"); village.t[13][12] = '.';
        village.addNpc(new Game.Npc("orders_board", "Encomendas da Vila", 12, 13,
            new Color(102, 147, 163)).obj("orders_board").still());
    }
    int stock(int item) { return item < 8 ? game.bonds.stock(item) : game.gardening.stock[item - 8]; }
    boolean refresh() {
        if (day == game.weather.day()) return true;
        Properties before = new Properties(); save(before);
        day = game.weather.day(); done = selected = 0; orders.clear();
        List<Integer> pool = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            VillageLife.Fish fish = VillageLife.FISH[i];
            boolean time = fish.period() < 0 || fish.period() == (game.isNight() ? 1 : 0);
            if (game.life.rod && game.stage >= fish.stage() && (stock(i) > 0 || time)) pool.add(i);
        }
        for (int i = 0; i < 4; i++) if (game.gardening.unlocked(i) && stock(i + 8) > 0) pool.add(i + 8);
        Random random = new Random(game.weather.seed ^ (day * 0x632BE59BD9B4E019L));
        Collections.shuffle(pool, random);
        for (int i = 0; i < Math.min(3, pool.size()); i++) {
            int item = pool.get(i);
            int count = item >= 8 ? 1 : 1 + random.nextInt(2);
            orders.add(new Order(item, count, random.nextInt(4)));
        }
        if (game.save(false)) return true;
        load(before); status = "Não foi possível salvar os pedidos. Tente novamente."; return false;
    }
    boolean talk(Game.Npc npc) {
        if (!npc.id.equals("orders_board")) return false;
        status = ""; refresh(); selected = 0; game.down.clear(); game.state = Game.ORDERS; return true;
    }
    boolean deliver(int index) {
        if (day != game.weather.day()) { status = "Os pedidos são de outro dia. Consulte o mural novamente."; return false; }
        if (index < 0 || index >= orders.size() || (done & (1 << index)) != 0) return false;
        Order order = orders.get(index);
        if (stock(order.item) < order.count) { status = "Quantidade insuficiente no estoque."; return false; }
        if (game.home.coins > HomeDecor.MAX_COINS - order.reward()) { status = "Sua carteira está cheia."; return false; }
        int oldDone = done, oldTotal = total, coins = game.home.coins, friendship = game.bonds.friendship[order.who];
        int[] inventory = order.item < 8 ? game.bonds.spent : game.gardening.stock;
        int slot = order.item < 8 ? order.item : order.item - 8, oldStock = inventory[slot];
        inventory[slot] += order.item < 8 ? order.count : -order.count;
        done |= 1 << index; total = Math.min(999999, total + 1);
        game.home.coins += order.reward(); game.bonds.addFriendship(order.who, 5);
        if (!game.save(false)) {
            inventory[slot] = oldStock; done = oldDone; total = oldTotal;
            game.home.coins = coins; game.bonds.friendship[order.who] = friendship;
            status = "Falha ao salvar. A entrega foi desfeita."; return false;
        }
        status = oldTotal < 10 && total >= 10 ? "Lembrança da Vila liberada na decoração da casa!" : "Entrega concluída: +" + order.reward() + " moedas e +5 de amizade.";
        game.audio.sfx("pickup"); return true;
    }
    void key(int key) {
        if (key == KeyEvent.VK_ESCAPE) { game.state = Game.PLAY; game.down.clear(); return; }
        if (orders.isEmpty()) return;
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) selected = (selected + orders.size() - 1) % orders.size();
        if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) selected = (selected + 1) % orders.size();
        if (key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER) deliver(selected);
    }
    void save(Properties p) {
        p.setProperty("pedidos.dia", "" + day); p.setProperty("pedidos.feitos", "" + done); p.setProperty("pedidos.total", "" + total);
        p.setProperty("pedidos.quantidade", "" + orders.size());
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i); p.setProperty("pedidos." + i, o.item + "," + o.count + "," + o.who);
        }
    }
    void load(Properties p) {
        reset();
        day = number(p, "pedidos.dia", -1, -1, Integer.MAX_VALUE);
        total = number(p, "pedidos.total", 0, 0, 999999);
        int count = number(p, "pedidos.quantidade", 0, 0, 3);
        try {
            for (int i = 0; i < count; i++) {
                String[] parts = p.getProperty("pedidos." + i, "").split(",");
                if (parts.length != 3) throw new IllegalArgumentException();
                int item = Integer.parseInt(parts[0]), amount = Integer.parseInt(parts[1]), who = Integer.parseInt(parts[2]);
                if (item < 0 || item > 11 || amount < 1 || amount > 2 || who < 0 || who > 3) throw new IllegalArgumentException();
                orders.add(new Order(item, amount, who));
            }
        } catch (IllegalArgumentException e) { orders.clear(); }
        done = number(p, "pedidos.feitos", 0, 0, 7) & ((1 << orders.size()) - 1);
    }
    int number(Properties p, String key, int fallback, int min, int max) {
        try { return Math.max(min, Math.min(max, Integer.parseInt(p.getProperty(key, "" + fallback)))); }
        catch (NumberFormatException e) { return fallback; }
    }
    boolean drawObject(Graphics2D g, Game.Npc n, int x, int y) {
        if (!n.id.equals("orders_board")) return false;
        g.setColor(new Color(115, 97, 76)); g.fillRect(x + 4, y + 5, 3, 27); g.fillRect(x + 25, y + 5, 3, 27);
        g.setColor(new Color(79, 137, 151)); g.fillRect(x, y, 32, 24);
        g.setColor(new Color(246, 241, 215));
        for (int i = 0; i < 3; i++) g.fillRect(x + 3 + i * 10, y + 4, 7, 15);
        return true;
    }
    void draw(Graphics2D g) {
        game.life.panel(g, "Encomendas da Vila", "Dia " + day + " · " + total + " entregas · Moedas: " + game.home.coins);
        for (int i = 0; i < orders.size(); i++) {
            Order order = orders.get(i); int y = 113 + i * 84; boolean complete = (done & (1 << i)) != 0;
            if (selected == i) { g.setColor(new Color(207, 231, 221)); g.fillRect(38, y - 15, 560, 78); }
            game.life.text(g, Bonds.NAMES[order.who] + " · " + (complete ? "Entregue" : "Pendente"), 50, y + 5, 520, 15);
            game.life.text(g, order.count + " × " + order.name() + " · Estoque: " + stock(order.item), 50, y + 28, 520, 16);
            game.life.text(g, order.reward() + " moedas · +5 amizade", 50, y + 50, 520, 13);
        }
        if (orders.isEmpty()) game.life.text(g, "Nenhum pedido hoje. Novas encomendas amanhã.", 50, 145, 510, 17);
        game.life.text(g, status, 44, 385, 540, 14);
        game.life.text(g, total >= 10 ? "Lembrança da Vila conquistada" : "Lembrança da Vila: " + total + "/10 entregas", 44, 445, 535, 14);
    }
}
