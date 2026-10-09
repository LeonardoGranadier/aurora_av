import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Conchas secretas escondidas em árvores e pedras (como as Secret Seashells de Link's Awakening e os itens
 * escondidos dos Pokémon). Um brilho fraco denuncia o esconderijo para quem passa perto; olhe para ele e aperte Z.
 * Seu Marinho troca as conchas por recompensas a cada três; a primeira é a Concha que Escuta, que mostra
 * o brilho de longe e conta as conchas restantes no Mapa de Aurora.
 */
final class SecretShells implements Feature {
    record Spot(String map, int x, int y, String where) {}
    static final Spot[] SPOTS = {
        new Spot("Vila Aurora", 12, 2, "uma árvore solitária perto das placas da montanha"),
        new Spot("Vila Aurora", 26, 14, "a árvore à beira do lago"),
        new Spot("Floresta Verde", 16, 9, "uma árvore no caminho da gruta"),
        new Spot("Floresta Verde", 6, 16, "o bosque ao sul da trilha"),
        new Spot("Praia Azul", 19, 5, "uma pedra perto do farol"),
        new Spot("Praia Azul", 3, 9, "uma pedra no canto oeste da praia"),
        new Spot("Gruta do Eco", 8, 4, "uma rocha no salão principal"),
        new Spot("Ilha das Estrelas", 8, 7, "uma pedra no gramado da ilha"),
        new Spot("Ilha das Estrelas", 10, 14, "uma pedra perto da praia sul"),
        new Spot("Trilha da Montanha", 6, 25, "uma árvore no começo da trilha"),
        new Spot("Pico Estrelado", 7, 2, "um pinheiro ao norte da cratera"),
        new Spot("Trilha dos Vagalumes", 8, 10, "uma árvore perto da trilha oeste"),
    };
    static final int PER_REWARD = 3;
    static final String[] REWARDS = {"Concha que Escuta", "40 moedas", "80 moedas", "Coleção de Conchas (decoração)"};

    final Game game;
    final boolean[] found = new boolean[SPOTS.length];
    int rewarded;   // recompensas já entregues pelo Seu Marinho

    SecretShells(Game game) { this.game = game; }

    int found() { int n = 0; for (boolean f : found) if (f) n++; return n; }
    boolean listening() { return rewarded >= 1; }
    int remaining(String map) {
        int n = 0;
        for (int i = 0; i < SPOTS.length; i++) if (!found[i] && SPOTS[i].map().equals(map)) n++;
        return n;
    }
    int total(String map) { return (int) Arrays.stream(SPOTS).filter(s -> s.map().equals(map)).count(); }
    int pendingRewards() { return Math.min(REWARDS.length, found() / PER_REWARD) - rewarded; }

    @Override public void reset() { Arrays.fill(found, false); rewarded = 0; }

    /** Esconderijos são objetos invisíveis sobre a árvore ou pedra, que continua sólida. */
    @Override public void buildWorld() {
        for (int i = 0; i < SPOTS.length; i++) {
            Spot s = SPOTS[i];
            GameMap m = game.maps.get(s.map());
            if (m != null) m.npcs.add(new Npc("concha_secreta" + i, "", s.x(), s.y(), new Color(255, 236, 200)).still());
        }
    }
    int index(Npc n) {
        if (!n.id.startsWith("concha_secreta")) return -1;
        try { return Integer.parseInt(n.id.substring("concha_secreta".length())); } catch (NumberFormatException e) { return -1; }
    }
    @Override public boolean visible(Npc n) { int i = index(n); return i < 0 || !found[i]; }

    @Override public boolean talk(Npc n) {
        int i = index(n);
        if (i < 0) return false;
        found[i] = true;
        game.audio.sfx("pickup");
        game.itemGet("concha_secreta");
        int total = found();
        String tip = total == SPOTS.length ? "Você encontrou todas as conchas de Aurora!"
            : total % PER_REWARD == 0 ? "O Seu Marinho, na Praia Azul, vai adorar ver isso." : "Faltam " + (SPOTS.length - total) + ".";
        game.say("", "Escondida em " + SPOTS[i].where() + ", uma conchinha rosada brilha!",
            "Concha secreta (" + total + "/" + SPOTS.length + ")! " + tip);
        return true;
    }

    /** Depois da conversa com o Seu Marinho, ele entrega as recompensas acumuladas. */
    @Override public void afterTalk(Npc n) {
        if (!n.id.equals("marinho") || pendingRewards() <= 0) return;
        Runnable previous = game.afterDlg;
        game.afterDlg = () -> { if (previous != null) previous.run(); if (game.dlg == null) deliver(); };
    }

    void deliver() {
        List<String> lines = new ArrayList<>();
        if (rewarded == 0) lines.add("Ei, marujo! Essas conchas rosadas... são conchas secretas! Quando eu era menino, colecionava.");
        while (pendingRewards() > 0) {
            int r = rewarded++;
            switch (r) {
                case 0 -> lines.add("Tome a minha Concha que Escuta. Encoste no ouvido: ela canta perto de outras conchas e conta quantas faltam em cada região (veja no mapa, Tab).");
                case 1 -> { game.home.coins = Math.min(HomeDecor.MAX_COINS, game.home.coins + 40); lines.add("Seis conchas! Aqui estão 40 moedas para a sua coleção."); }
                case 2 -> { game.home.coins = Math.min(HomeDecor.MAX_COINS, game.home.coins + 80); lines.add("Nove! Você tem olho de pescador de pérolas. Leve 80 moedas."); }
                default -> lines.add("Todas as doze! Leve esta caixa de vidro para expor a coleção em casa (tecla H). Estou orgulhoso, marujo.");
            }
        }
        game.say("Seu Marinho", lines.toArray(String[]::new));
        game.audio.sfx("fanfare");
        game.save(false);
    }

    /** O brilho aparece de tempos em tempos para quem está perto, ou sempre (fraco) com a Concha que Escuta. */
    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        int i = index(n);
        if (i < 0) return false;
        double distance = Math.hypot(n.x - game.tx, n.y - game.ty);
        long cycle = (game.frame + i * 37L) % 200;
        boolean near = distance <= 4 && cycle < 24, heard = listening() && distance <= 9 && cycle < 60;
        if (!near && !heard) return true;
        int a = (int) (Math.sin(Math.PI * Math.min(1, cycle / (near ? 24.0 : 60.0))) * (near ? 230 : 150));
        int cx = x + 22, cy = y + 10;
        g.setColor(new Color(255, 245, 220, Math.max(0, a)));
        g.fillRect(cx - 1, cy - 5, 2, 10); g.fillRect(cx - 5, cy - 1, 10, 2);
        g.fillRect(cx - 2, cy - 2, 4, 4);
        return true;
    }

    @Override public void save(Properties p) {
        StringBuilder b = new StringBuilder();
        for (boolean f : found) b.append(f ? '1' : '0');
        p.setProperty("conchas.encontradas", b.toString());
        p.setProperty("conchas.recompensas", "" + rewarded);
    }
    @Override public void load(Properties p) {
        String bits = p.getProperty("conchas.encontradas", "");
        for (int i = 0; i < found.length; i++) found[i] = i < bits.length() && bits.charAt(i) == '1';
        rewarded = Math.min(game.life.number(p, "conchas.recompensas", 0, REWARDS.length), found() / PER_REWARD);
    }
    @Override public String signature() { return Arrays.toString(found) + rewarded; }
}
