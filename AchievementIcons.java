import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Ícones das conquistas: um brasão 64x64 por conquista, montado com os próprios desenhos do jogo (itens, decorações,
 * peixes, insetos, sprites), com moldura colorida por tipo. O Cartão do Explorador mostra a versão pequena;
 * a ferramenta Capturas exporta em 256x256 (colorida e cinza) para a Steam. Bloqueada, a versão é cinza.
 */
final class AchievementIcons {
    static final int BASE = 64;
    /** Cores da moldura: história, vila, coleção, segredos. */
    static final Color[] FRAMES = {new Color(232, 186, 70), new Color(110, 190, 150), new Color(100, 160, 225), new Color(190, 130, 220)};

    final Game game;
    final Map<String, BufferedImage> cache = new HashMap<>();

    AchievementIcons(Game game) { this.game = game; }

    static int kind(String id) {
        return switch (id) {
            case "farol", "mapa", "marujo", "estrela_mar", "cintila", "jardim", "mare", "festival_ceu", "melodia", "guardiao" -> 0;
            case "amizade", "pedidos", "marinheiro", "encomendas", "festival", "aniversario", "gato", "chef", "ano" -> 1;
            case "album", "lendario", "insetos", "curador", "observatorio", "sinos", "paleontologo" -> 2;
            default -> 3;
        };
    }

    /** Ícone em BASE x BASE; got = falso devolve a versão cinza e apagada. */
    BufferedImage icon(String id, boolean got) {
        return cache.computeIfAbsent(id + got, k -> {
            BufferedImage img = render(id);
            return got ? img : gray(img);
        });
    }

    void draw(Graphics2D g, String id, int x, int y, int size, boolean got) {
        Object hint = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(icon(id, got), x, y, size, size, null);
        if (hint != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
    }

    static BufferedImage gray(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
            int argb = src.getRGB(x, y), a = argb >>> 24;
            int l = (int) (((argb >> 16 & 255) * 0.3 + (argb >> 8 & 255) * 0.59 + (argb & 255) * 0.11) * 0.55 + 70);
            out.setRGB(x, y, a << 24 | l << 16 | l << 8 | l);
        }
        return out;
    }

    BufferedImage render(String id) {
        BufferedImage img = new BufferedImage(BASE, BASE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color frame = FRAMES[kind(id)];
        g.setColor(frame.darker()); g.fillOval(1, 1, 62, 62);
        g.setColor(frame); g.fillOval(4, 4, 56, 56);
        g.setPaint(new GradientPaint(0, 8, new Color(36, 44, 86), 0, 56, new Color(70, 64, 130)));
        g.fillOval(8, 8, 48, 48);
        g.setColor(new Color(255, 255, 255, 40)); g.fillOval(14, 11, 26, 12);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        Graphics2D e = (Graphics2D) g.create();
        e.clip(new java.awt.geom.Ellipse2D.Double(8, 8, 48, 48));
        try { emblem(e, id); } catch (RuntimeException ex) { star(e); }
        e.dispose(); g.dispose();
        return img;
    }

    /** Desenha o símbolo dentro do círculo central (8..56). */
    void emblem(Graphics2D g, String id) {
        switch (id) {
            case "farol" -> item(g, "concha");
            case "mapa" -> item(g, "mapa");
            case "marujo" -> sprite(g, "barco");
            case "estrela_mar" -> item(g, "estrela");
            case "cintila" -> sprite(g, "cintila");
            case "jardim" -> decor(g, "flor_constelacao");
            case "album" -> item(g, "trofeu");
            case "sinos" -> item(g, "sinos");
            case "festival" -> decor(g, "estrela");
            case "amizade" -> heart(g);
            case "pedidos" -> decor(g, "apito");
            case "encomendas" -> sprite(g, "placa");
            case "regioes" -> item(g, "bussola");
            case "conchas" -> decor(g, "colecao_conchas");
            case "trocas" -> gift(g);
            case "observatorio" -> decor(g, "luneta");
            case "mare" -> decor(g, "escama");
            case "festival_ceu" -> decor(g, "lanterna_festa");
            case "melodia" -> decor(g, "caixinha");
            case "guardiao" -> decor(g, "semente_arvore");
            case "lendario" -> game.life.drawFish(g, 14, 24, 28, Museum.FISH[7].color());
            case "insetos" -> { Graphics2D s = scaled(g, 1.2); game.museum.drawBug(s, 9, 0, 0, Museum.BUGS[9].color(), false); s.dispose(); }
            case "paleontologo" -> { Graphics2D s = scaled(g, 1.25); game.museum.drawFossil(s, 0, 0, 0, Museum.FOSSILS[0].color()); s.dispose(); }
            case "marinheiro" -> { Graphics2D s = scaled(g, 1.3); game.boat.drawObject(s, new Npc("gaivota_ilha", "", 0, 0, Color.WHITE), 0, -4); s.dispose(); }
            case "curador" -> { Graphics2D s = scaled(g, 1.0); game.museum.drawObject(s, new Npc("museu", "", 0, 0, Color.WHITE), 0, 0); s.dispose(); }
            case "enigmas" -> chest(g);
            case "ano" -> seasons(g);
            case "chef" -> sprite(g, "fogao_on");
            case "aniversario" -> cake(g);
            case "gato" -> { Graphics2D s = scaled(g, 1.25); game.cat.drawObject(s, game.cat.cat, 0, -4); s.dispose(); }
            default -> star(g);
        }
    }

    /** Área de 32x32 centralizada e ampliada para caber no círculo. */
    static Graphics2D scaled(Graphics2D g, double scale) {
        Graphics2D s = (Graphics2D) g.create();
        s.translate(32 - 16 * scale, 32 - 16 * scale); s.scale(scale, scale);
        return s;
    }
    void item(Graphics2D g, String key) {
        int idx = Arrays.asList(Inventory.KEYS).indexOf(key);
        if (idx < 0) { star(g); return; }
        game.inventory.drawIcon(g, idx, 14, 14, 36, false);
    }
    void sprite(Graphics2D g, String key) {
        BufferedImage s = game.tileSprite(key);
        if (s == null) { star(g); return; }
        g.drawImage(s, 14, 14, 36, 36, null);
    }
    void decor(Graphics2D g, String id) {
        for (int i = 0; i < HomeDecor.ITEMS.length; i++)
            if (HomeDecor.ITEMS[i].id().equals(id)) { game.home.drawItem(g, i, 14, 13, 36, 0); return; }
        star(g);
    }
    static void star(Graphics2D g) {
        int[] xs = new int[10], ys = new int[10];
        for (int i = 0; i < 10; i++) { double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? 17 : 7; xs[i] = 32 + (int) (Math.cos(a) * r); ys[i] = 33 + (int) (Math.sin(a) * r); }
        g.setColor(new Color(255, 222, 100)); g.fillPolygon(xs, ys, 10);
    }
    static void heart(Graphics2D g) {
        g.setColor(new Color(236, 90, 120));
        g.fillOval(18, 20, 15, 15); g.fillOval(31, 20, 15, 15);
        g.fillPolygon(new int[]{19, 45, 32}, new int[]{30, 30, 46}, 3);
        g.setColor(new Color(255, 190, 205)); g.fillRect(23, 24, 4, 3);
    }
    static void gift(Graphics2D g) {
        g.setColor(new Color(90, 160, 210)); g.fillRect(19, 26, 26, 20);
        g.setColor(new Color(70, 130, 180)); g.fillRect(17, 21, 30, 6);
        g.setColor(new Color(240, 200, 80)); g.fillRect(30, 21, 5, 25); g.fillOval(24, 15, 8, 7); g.fillOval(33, 15, 8, 7);
    }
    static void chest(Graphics2D g) {
        g.setColor(new Color(140, 90, 50)); g.fillRect(17, 27, 30, 18);
        g.setColor(new Color(170, 115, 65)); g.fillRoundRect(17, 19, 30, 11, 8, 8);
        g.setColor(new Color(240, 200, 80)); g.fillRect(17, 28, 30, 3); g.fillRect(30, 26, 5, 8);
        g.setColor(new Color(255, 240, 160)); g.fillRect(24, 14, 2, 4); g.fillRect(38, 15, 2, 3);
    }
    static void seasons(Graphics2D g) {
        Color[] c = {new Color(240, 150, 190), new Color(250, 210, 70), new Color(230, 130, 60), new Color(200, 230, 250)};
        for (int i = 0; i < 4; i++) { g.setColor(c[i]); g.fillArc(16, 16, 32, 32, 90 - i * 90, -90); }
        g.setColor(new Color(36, 44, 86)); g.fillOval(27, 27, 10, 10);
    }
    static void cake(Graphics2D g) {
        g.setColor(new Color(250, 236, 220)); g.fillRect(18, 30, 28, 14);
        g.setColor(new Color(236, 130, 160)); g.fillRect(18, 28, 28, 5); for (int i = 0; i < 4; i++) g.fillOval(18 + i * 7, 31, 7, 5);
        g.setColor(new Color(130, 200, 240)); g.fillRect(31, 18, 3, 10);
        g.setColor(new Color(255, 200, 80)); g.fillOval(30, 13, 5, 6);
    }
}
