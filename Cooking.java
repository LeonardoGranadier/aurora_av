import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

/**
 * Culinária: o fogão da cozinha de casa transforma peixes e flores em pratos (como em Harvest Moon e Stardew).
 * Receitas são ensinadas pelos moradores conforme a amizade e a história. Pratos ficam na lancheira (tecla K):
 * comer dá um efeito até o fim do dia; diante de um morador, vira presente (conta como o presente do dia).
 */
final class Cooking implements Feature {
    static final int HEART = 1, LIGHT_STEP = 2, CALM = 4, LUCK = 8, NIGHT = 16, CHAT = 32, ALL = 63;
    static final String[] EFFECT_NAMES = {"Coração quente", "Passo leve", "Calma de pescador", "Sorte grande", "Visão noturna", "Boa prosa"};
    static final String[] EFFECT_TEXT = {"presentes rendem 50% mais amizade", "você anda e corre mais rápido",
        "a faixa verde da pesca fica maior", "faixa maior e peixes raros mais comuns", "lugares escuros e noites ficam mais claros",
        "conversas rendem +3 de amizade"};

    record Recipe(String id, String name, int[] fish, int[] flowers, int effect, String teacher, Predicate<Game> known) {}
    static final Recipe[] RECIPES = {
        new Recipe("sopa", "Sopa de Lambari", new int[]{0}, new int[]{0}, HEART, "Receita da Mamãe", g -> true),
        new Recipe("sardinha", "Sardinha na Brasa", new int[]{4}, new int[]{}, LIGHT_STEP, "Receita da Mamãe", g -> true),
        new Recipe("cha", "Chá de Tulipa", new int[]{}, new int[]{1}, CALM, "Receita da Mamãe", g -> true),
        new Recipe("torta", "Torta de Carpa", new int[]{1}, new int[]{2}, LUCK, "Tio Beto ensina com 2 corações", g -> g.bonds.friendship[1] >= 40),
        new Recipe("doce", "Doce de Girassol", new int[]{}, new int[]{2, 1}, CHAT, "Dona Rosa ensina com 2 corações", g -> g.bonds.friendship[2] >= 40),
        new Recipe("truta", "Truta com Ervas", new int[]{3}, new int[]{0}, LIGHT_STEP | CALM, "Lucas ensina com 2 corações", g -> g.bonds.friendship[3] >= 40),
        new Recipe("ensopado", "Ensopado do Mar", new int[]{5, 4}, new int[]{0}, NIGHT, "A Capitã ensina depois do capítulo 3", g -> g.stage >= 15),
        new Recipe("banquete", "Banquete Estelar", new int[]{7}, new int[]{3}, ALL, "Cintila ensina depois do capítulo 5", g -> g.stage >= 28)
    };
    /** Pratos favoritos de prof, beto, rosa e lucas. */
    static final int[][] FAVORITES = {{2, 6}, {3, 1}, {4, 0}, {5, 7}};

    final Game game;
    final int[] dishes = new int[RECIPES.length];
    final Set<String> cooked = new TreeSet<>();
    int effects, effectDay = -1;
    int selected, target = -1, returnState = Game.PLAY;
    boolean kitchen;    // true: fogão; false: lancheira
    String status = "";

    Cooking(Game game) { this.game = game; }

    boolean known(int r) { return RECIPES[r].known().test(game); }
    boolean active(int effect) { return effectDay == game.day() && (effects & effect) != 0; }
    double speedFactor() { return active(LIGHT_STEP) ? 1.25 : 1; }
    double fishingBonus() { return (active(CALM) ? 0.03 : 0) + (active(LUCK) ? 0.03 : 0); }
    int rareFactor(int fish) { return active(LUCK) && VillageLife.FISH[fish].difficulty() >= 1.2 ? 2 : 1; }
    int giftPoints(int points) { return active(HEART) ? points + points / 2 : points; }
    int talkBonus() { return active(CHAT) ? 3 : 0; }
    boolean nightVision() { return active(NIGHT); }

    boolean has(int r) {
        Recipe recipe = RECIPES[r];
        int[] fishNeed = new int[8], flowerNeed = new int[4];
        for (int f : recipe.fish()) fishNeed[f]++;
        for (int f : recipe.flowers()) flowerNeed[f]++;
        for (int f = 0; f < 8; f++) if (game.bonds.stock(f) < fishNeed[f]) return false;
        for (int f = 0; f < 4; f++) if (game.gardening.stock[f] < flowerNeed[f]) return false;
        return true;
    }

    /** Prepara um prato; ingredientes saem do estoque (o álbum de pesca não muda). Falha ao salvar desfaz tudo. */
    boolean cook(int r) {
        if (!known(r) || !has(r) || dishes[r] >= 99) return false;
        int[] spent = game.bonds.spent.clone(), flowers = game.gardening.stock.clone();
        Set<String> before = new TreeSet<>(cooked);
        for (int f : RECIPES[r].fish()) game.bonds.spent[f]++;
        for (int f : RECIPES[r].flowers()) game.gardening.stock[f]--;
        dishes[r]++; cooked.add(RECIPES[r].id());
        if (game.save(false)) return true;
        System.arraycopy(spent, 0, game.bonds.spent, 0, spent.length);
        System.arraycopy(flowers, 0, game.gardening.stock, 0, flowers.length);
        dishes[r]--; cooked.clear(); cooked.addAll(before);
        return false;
    }

    boolean eat(int r) {
        if (dishes[r] == 0) return false;
        int oldEffects = effects, oldDay = effectDay;
        if (effectDay != game.day()) effects = 0;
        effects |= RECIPES[r].effect(); effectDay = game.day(); dishes[r]--;
        if (game.save(false)) return true;
        effects = oldEffects; effectDay = oldDay; dishes[r]++;
        return false;
    }

    boolean favorite(int who, int r) { return FAVORITES[who][0] == r || FAVORITES[who][1] == r; }

    boolean give(int who, int r) {
        if (dishes[r] == 0 || game.bonds.gifted[who] == game.bonds.day()) return false;
        int oldFriendship = game.bonds.friendship[who], oldGifted = game.bonds.gifted[who];
        Set<String> birthdays = new TreeSet<>(game.seasons.birthdayGifts);
        dishes[r]--; game.bonds.gifted[who] = game.bonds.day();
        game.bonds.addFriendship(who, game.giftPoints(who, favorite(who, r) ? 20 : 12));
        if (game.save(false)) return true;
        dishes[r]++; game.bonds.friendship[who] = oldFriendship; game.bonds.gifted[who] = oldGifted;
        game.seasons.birthdayGifts.clear(); game.seasons.birthdayGifts.addAll(birthdays);
        return false;
    }

    // ---------- Mundo ----------
    @Override public void reset() { Arrays.fill(dishes, 0); cooked.clear(); effects = 0; effectDay = -1; status = ""; }
    /** O fogão fica na parede da cozinha (tile sólido), então não ocupa piso da casa. */
    @Override public void buildWorld() {
        game.maps.get("Casa").npcs.add(new Npc("fogao_casa", "Fogão", 7, 2, new Color(90, 90, 100)).obj("fogao_casa").still());
    }
    @Override public boolean talk(Npc n) {
        if (!n.id.equals("fogao_casa")) return false;
        openKitchen();
        return true;
    }
    void openKitchen() { kitchen = true; selected = 0; target = -1; status = ""; returnState = Game.PLAY; game.down.clear(); game.state = Game.COOKING; }
    /** Lancheira: diante de um morador, oferece pratos como presente. */
    void openLunchbox() {
        kitchen = false; selected = 0; status = ""; target = -1; returnState = Game.PLAY;
        int x = game.tx + Game.DX[game.dir], y = game.ty + Game.DY[game.dir];
        for (Npc n : game.map.npcs) if (game.vis(n) && n.x == x && n.y == y) target = game.bonds.residentIndex(n.id);
        game.down.clear(); game.state = Game.COOKING;
    }

    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (!n.id.equals("fogao_casa")) return false;
        g.setColor(new Color(70, 72, 82)); g.fillRoundRect(x + 2, y + 8, 28, 28, 4, 4);
        g.setColor(new Color(40, 42, 50)); g.fillOval(x + 6, y + 11, 9, 6); g.fillOval(x + 17, y + 11, 9, 6);
        g.setColor(new Color(150, 150, 160)); g.fillRect(x + 6, y + 22, 20, 10);
        g.setColor(new Color(255, 140, 50, 150 + (int) (Math.sin(game.frame / 8.0) * 60))); g.fillRect(x + 9, y + 25, 14, 4);
        return true;
    }

    // ---------- Tela ----------
    @Override public boolean owns(int state) { return state == Game.COOKING; }
    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        int n = RECIPES.length, old = selected;
        if (key == KeyEvent.VK_ESCAPE || (!kitchen && key == KeyEvent.VK_K)) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP) selected = (selected + n - 1) % n;
        else if (key == KeyEvent.VK_DOWN) selected = (selected + 1) % n;
        else if ((key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) && target < 0 && game.map.name.equals("Casa")) {
            kitchen = !kitchen; status = "";                                       // em casa, alterna fogão e lancheira
        }
        else if (act) {
            int r = selected;
            if (kitchen) {
                if (!known(r)) status = "Receita desconhecida. " + RECIPES[r].teacher() + ".";
                else if (!has(r)) status = "Faltam ingredientes: " + ingredients(r) + ".";
                else game.access.ask("Cozinhar " + RECIPES[r].name() + " com " + ingredients(r) + "?", () -> {
                    if (cook(r)) { game.audio.sfx("fire"); status = RECIPES[r].name() + " pronto! Está na lancheira (K)."; }
                    else status = "Não foi possível salvar. Nada foi gasto.";
                });
            } else if (dishes[r] == 0) status = "Nenhum prato deste tipo na lancheira.";
            else if (target >= 0) {
                int who = target;
                if (game.bonds.gifted[who] == game.bonds.day()) status = Bonds.NAMES[who] + " já ganhou um presente hoje.";
                else game.access.ask("Dar " + RECIPES[r].name() + " para " + Bonds.NAMES[who] + "?", () -> {
                    if (give(who, r)) {
                        game.state = Game.PLAY; game.audio.sfx("pickup");
                        game.say(Bonds.NAMES[who], favorite(who, r) ? "Meu prato favorito! Como você sabia? Muito obrigado!" : "Que cheiro bom! Vou comer agora mesmo. Obrigado!");
                    } else status = "Não foi possível entregar.";
                });
            } else if (eat(r)) { game.audio.sfx("pickup"); status = "Delicioso! Hoje: " + effectList(RECIPES[r].effect()) + "."; }
            else status = "Não foi possível salvar.";
        }
        if (selected != old) { game.audio.sfx("select"); status = ""; }
    }

    String ingredients(int r) {
        List<String> parts = new ArrayList<>();
        for (int f : RECIPES[r].fish()) parts.add(VillageLife.FISH[f].name());
        for (int f : RECIPES[r].flowers()) parts.add(Gardening.NAMES[f]);
        return String.join(" + ", parts);
    }
    static String effectList(int bits) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < EFFECT_NAMES.length; i++) if ((bits & (1 << i)) != 0) parts.add(EFFECT_NAMES[i] + " (" + EFFECT_TEXT[i] + ")");
        return String.join("; ", parts);
    }

    @Override public void draw(Graphics2D g) {
        String title = kitchen ? "Cozinha" : target >= 0 ? "Presente para " + Bonds.NAMES[target] : "Lancheira";
        String swap = target < 0 && game.map.name.equals("Casa") ? " · ← →: " + (kitchen ? "lancheira" : "fogão") : "";
        game.life.panel(g, title, (kitchen ? "Z: cozinhar · Esc: sair" : target >= 0 ? "Z: oferecer · Esc: sair" : "Z: comer · Esc / K: fechar") + swap);
        for (int i = 0; i < RECIPES.length; i++) {
            int y = 124 + i * 34;
            if (i == selected) { g.setColor(new Color(207, 231, 221)); g.fillRect(38, y - 21, 564, 32); }
            boolean known = known(i);
            g.setColor(known ? new Color(230, 160, 80) : new Color(190, 198, 205)); g.fillOval(46, y - 14, 18, 18);
            game.life.text(g, known ? RECIPES[i].name() : "???", 74, y, 210, 15);
            String right = kitchen ? (known ? ingredients(i) + (has(i) ? "  ✓" : "") : RECIPES[i].teacher()) : "× " + dishes[i];
            game.life.text(g, right, 290, y, 300, 12);
        }
        Recipe r = RECIPES[selected];
        String info = !known(selected) ? r.teacher() + "." : "Efeito: " + effectList(r.effect()) + ".";
        if (target >= 0 && favorite(target, selected) && known(selected)) info += " Prato favorito!";
        game.life.text(g, status.isEmpty() ? info : status, 44, 400, 552, 13);
        if (effectDay == game.day() && effects != 0) game.life.text(g, "Ativo hoje: " + effectList(effects).replaceAll(" \\([^)]*\\)", ""), 44, 440, 552, 12);
    }

    // ---------- Save ----------
    @Override public void save(Properties p) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < dishes.length; i++) b.append(i == 0 ? "" : ",").append(dishes[i]);
        p.setProperty("cozinha.pratos", b.toString());
        p.setProperty("cozinha.receitas", String.join(",", cooked));
        p.setProperty("cozinha.efeitos", effects + "@" + effectDay);
    }
    @Override public void load(Properties p) {
        reset();
        String[] parts = p.getProperty("cozinha.pratos", "").split(",");
        for (int i = 0; i < dishes.length && i < parts.length; i++)
            try { dishes[i] = Math.max(0, Math.min(99, Integer.parseInt(parts[i].trim()))); } catch (NumberFormatException ignored) {}
        for (String id : p.getProperty("cozinha.receitas", "").split(","))
            if (Arrays.stream(RECIPES).anyMatch(r -> r.id().equals(id))) cooked.add(id);
        String[] fx = p.getProperty("cozinha.efeitos", "0@-1").split("@");
        try { effects = Integer.parseInt(fx[0]) & ALL; effectDay = Integer.parseInt(fx[1]); } catch (Exception e) { effects = 0; effectDay = -1; }
    }
    @Override public String signature() { return Arrays.toString(dishes) + cooked + effects + "@" + effectDay; }
}
