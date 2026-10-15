import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/**
 * Ferramenta: gera as imagens da página da loja em loja/ (fica fora do pacote do jogo).
 *   capa.png (630x500), icone.png (512x512) e capturas/NN-nome-pt.png / -en.png em 1280x960 (cenas em 2x).
 *   steam/: cápsulas nos tamanhos da Steam (cabeçalho, pequena, principal, vertical, biblioteca, herói e logo).
 * Rodar na pasta do projeto, numa partida descartável:  java -Daurora.dados=$(mktemp -d) -cp .build Capturas
 */
public class Capturas {
    static final File OUT = new File("loja");

    /** Cena em 2x, sem HUD, ou a tela atual inteira (menus) quando withHud. */
    static BufferedImage frame(Game g, boolean withHud) {
        BufferedImage image = new BufferedImage(Game.W * 2, Game.H * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = image.createGraphics();
        gr.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        gr.scale(2, 2); g.fade = 0; g.toastT = 0; g.bannerT = 0; g.photo = !withHud;
        try { g.paintScene(gr); } finally { g.photo = false; gr.dispose(); }
        return image;
    }
    static void save(BufferedImage image, String name) throws Exception {
        File file = new File(OUT, name); file.getParentFile().mkdirs();
        ImageIO.write(image, "png", file); System.out.println(file);
    }
    /** Primeiro dia de sol a partir de shownDay (o clima das capturas é sempre bonito). */
    static void at(Game g, int shownDay, int hour) {
        g.weather.seed = Weather.LEGACY_SEED;
        while (g.weather.forDay(shownDay) != Weather.SUN) shownDay++;
        g.time = (shownDay - 1) * 1440 + hour * 60;
    }

    static void scenes(Game g, String lang) throws Exception {
        Lang.set(lang); g.registerNames();
        String sfx = "-" + lang + ".png";
        g.newGame(); g.state = Game.PLAY; g.explorer.name = "Ana"; g.explorer.form = Explorer.GIRL; Lang.addNames("Ana");
        g.access.textSpeed = 0; g.stage = 21; g.refreshObjects();

        // 1. vila na primavera, com moradores conversando
        g.travel("Vila Aurora", 15, 14); at(g, 3, 10); g.frame = 60; g.dlg = null;
        Npc beto = g.life.resident("beto"), lucas = g.life.resident("lucas");
        beto.x = 14; beto.y = 12; lucas.x = 16; lucas.y = 12; beto.walking = lucas.walking = false;
        g.worldLife.chatNpc = beto; g.worldLife.chatPartner = lucas; g.worldLife.chatText = "Lucas, quer aprender a pescar?"; g.worldLife.chatTimer = 3;
        save(frame(g, false), "capturas/01-vila" + sfx); g.worldLife.chatNpc = null;
        // 2. praia à noite, farol e estrelas
        g.stage = 21; g.travel("Praia Azul", 19, 9); at(g, 4, 23); g.frame = 300;
        save(frame(g, false), "capturas/02-farol" + sfx);
        // 3. diálogo com o professor
        g.travel("Vila Aurora", 10, 9); at(g, 5, 9); g.dir = 1;
        g.say("Prof. Carvalho", "Você viu o céu ontem à noite? Uma estrela cadente caiu na Montanha Névoa, ao norte!");
        save(frame(g, true), "capturas/03-dialogo" + sfx); g.dlg = null;
        // 4. festival no Pico Estrelado
        g.stage = ChapterSeven.PARTY; g.cooking.dishes[0] = 0; g.travel("Pico Estrelado", 10, 9); at(g, 6, 22); g.dir = 1; g.frame = 500;
        save(frame(g, false), "capturas/04-festival" + sfx);
        // 5. inverno na vila
        g.stage = 21; g.travel("Vila Aurora", 15, 11); at(g, 88, 10); g.frame = 800;
        save(frame(g, false), "capturas/05-inverno" + sfx);
        // 6. constelações do observatório
        g.observatory.done = 15; g.observatory.viewing = true; g.observatory.selected = 2; g.state = Game.OBSERVATORY; g.frame = 40;
        save(frame(g, true), "capturas/06-constelacoes" + sfx); g.state = Game.PLAY;
        // 7. pesca
        g.travel("Vila Aurora", 19, 13); at(g, 3, 10); g.life.rod = true; g.life.cast();
        g.life.fishPhase = 2; g.life.catchProgress = 0.62; g.life.fishPosition = 0.45; g.life.fishTarget = 0.5;
        save(frame(g, true), "capturas/07-pesca" + sfx); g.state = Game.PLAY;
        // 8. cartão do explorador
        for (String id : new String[]{"farol", "mapa", "marujo", "estrela_mar", "cintila", "album", "sinos"}) g.explorer.unlocked.add(id);
        g.explorer.playSeconds = 3 * 3600 + 1240; g.home.coins = 340; g.explorer.openCard(Game.PLAY);
        save(frame(g, true), "capturas/08-cartao" + sfx); g.state = Game.PLAY;
        // 9. Floresta Antiga com as lanternas acesas
        g.explorer.unlocked.clear();
        g.stage = ChapterNine.TREE; g.chapterNine.lit = 15; g.travel(ChapterNine.FOREST, 12, 8); at(g, 7, 10); g.dir = 1; g.frame = 200;
        save(frame(g, false), "capturas/09-floresta" + sfx);
        // 10. aurora sobre a vila
        g.stage = Story.FINAL; g.travel("Vila Aurora", 15, 11); at(g, 8, 22); g.frame = 900;
        save(frame(g, false), "capturas/10-aurora" + sfx);
        // 11. museu com peixes raros
        g.museum.net = true; for (int i : new int[]{0, 2, 3, 5, 7}) g.museum.fish[i] = 1;
        for (int i : new int[]{0, 2, 4}) g.museum.given[Museum.FISH_WING][i] = true; g.museum.tab = 0; g.museum.selected = 7; g.museum.status = "";
        g.travel("Vila Aurora", 27, 8); at(g, 9, 10); g.museum.openScreen();
        save(frame(g, true), "capturas/11-museu" + sfx); g.state = Game.PLAY;
        // 12. a Gaivota no Alto-mar
        g.boat.owned = true; g.travel(Boat.SEA, 9, 7); at(g, 10, 17); g.dir = 0; g.frame = 300;
        save(frame(g, false), "capturas/12-alto-mar" + sfx);
    }

    /** A cena das cápsulas: o farol sob a aurora, em 2x (1280x960), sem HUD. */
    static BufferedImage capsuleScene(Game g) {
        Lang.set("pt"); g.registerNames();
        g.newGame(); g.state = Game.PLAY; g.stage = Story.FINAL; g.refreshObjects();
        g.travel("Praia Azul", 19, 9); at(g, 4, 23); g.frame = 420;
        return frame(g, false);
    }

    /** Recorta a cena na proporção pedida (centrada em focusX/focusY) e amplia sem borrar os pixels. */
    static BufferedImage crop(BufferedImage scene, int w, int h, double focusX, double focusY, boolean alpha) {
        double ratio = (double) w / h;
        int cw = scene.getWidth(), ch = (int) Math.round(cw / ratio);
        if (ch > scene.getHeight()) { ch = scene.getHeight(); cw = (int) Math.round(ch * ratio); }
        int x = (int) Math.max(0, Math.min(scene.getWidth() - cw, focusX * scene.getWidth() - cw / 2.0));
        int y = (int) Math.max(0, Math.min(scene.getHeight() - ch, focusY * scene.getHeight() - ch / 2.0));
        BufferedImage out = new BufferedImage(w, h, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D c = out.createGraphics();
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        c.drawImage(scene, 0, 0, w, h, x, y, x + cw, y + ch, null);
        c.dispose();
        return out;
    }

    /** Título com sombra e, se couber, o subtítulo, na faixa inferior (ou centrado quando bottom é falso). */
    static void title(BufferedImage img, double scale, boolean bottom, boolean subtitle, boolean shade) {
        int w = img.getWidth(), h = img.getHeight();
        Graphics2D c = img.createGraphics();
        c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (shade) {
            c.setPaint(new GradientPaint(0, h * 0.45f, new Color(8, 10, 40, 0), 0, h, new Color(8, 10, 40, 225)));
            c.fillRect(0, (int) (h * 0.45), w, h);
        }
        String text = "Aventura em Aurora";
        int size = (int) Math.round(Math.min(w / 10.0, h / 4.5) * scale);
        c.setFont(new Font("Serif", Font.BOLD, size));
        FontMetrics fm = c.getFontMetrics();
        while (fm.stringWidth(text) > w * 0.9 && size > 8) { size--; c.setFont(new Font("Serif", Font.BOLD, size)); fm = c.getFontMetrics(); }
        int y = bottom ? (int) (h - (subtitle ? size * 0.95 : size * 0.45)) : h / 2 + fm.getAscent() / 3;
        int x = (w - fm.stringWidth(text)) / 2, off = Math.max(2, size / 18);
        c.setColor(new Color(0, 0, 0, 160)); c.drawString(text, x + off, y + off);
        c.setColor(new Color(255, 240, 200)); c.drawString(text, x, y);
        if (subtitle) {
            String sub = "uma pequena história de exploração";
            c.setFont(new Font("Serif", Font.ITALIC, Math.max(12, size * 2 / 5))); fm = c.getFontMetrics();
            c.setColor(new Color(255, 255, 255, 225)); c.drawString(sub, (w - fm.stringWidth(sub)) / 2, y + (int) (size * 0.62));
        }
        c.dispose();
    }

    /** Imagens da Steam (tamanhos de 2024): cápsulas com título, herói sem texto e logo transparente. */
    static void steam(Game g) throws Exception {
        BufferedImage scene = capsuleScene(g);
        double fx = 0.62, fy = 0.3;                           // farol e aurora
        BufferedImage img;
        img = crop(scene, 920, 430, fx, fy, false); title(img, 1, true, true, true); save(img, "steam/cabecalho-920x430.png");
        img = crop(scene, 462, 174, fx, fy, false); title(img, 1.25, true, false, true); save(img, "steam/capsula-pequena-462x174.png");
        img = crop(scene, 1232, 706, fx, fy, false); title(img, 1, true, true, true); save(img, "steam/capsula-principal-1232x706.png");
        img = crop(scene, 748, 896, fx, fy, false); title(img, 1.5, true, true, true); save(img, "steam/capsula-vertical-748x896.png");
        img = crop(scene, 600, 900, fx, fy, false); title(img, 1.45, true, true, true); save(img, "steam/biblioteca-600x900.png");
        img = crop(scene, 3840, 1240, fx, 0.2, false); save(img, "steam/heroi-3840x1240.png");
        img = crop(scene, 1438, 810, fx, fy, false); save(img, "steam/fundo-1438x810.png");
        BufferedImage logo = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_ARGB);
        title(logo, 1.4, false, false, false); save(logo, "steam/logo-1280x720.png");
        BufferedImage community = new BufferedImage(184, 184, BufferedImage.TYPE_INT_RGB);
        Graphics2D c = community.createGraphics();
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        c.drawImage(ImageIO.read(new File(OUT, "icone.png")), 0, 0, 184, 184, null); c.dispose();
        save(community, "steam/icone-comunidade-184x184.png");
        // conquistas: 256x256, colorida e cinza (a Steam pede as duas), com o nome de API do Steam.java
        for (Explorer.Achievement a : Explorer.ACHIEVEMENTS) {
            String name = Steam.apiName(a.id());
            save(upscale(g.icons.icon(a.id(), true), 256), "steam/conquistas/" + name + ".png");
            save(upscale(g.icons.icon(a.id(), false), 256), "steam/conquistas/" + name + "_cinza.png");
        }
    }

    static BufferedImage upscale(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D c = out.createGraphics();
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        c.drawImage(src, 0, 0, size, size, null); c.dispose();
        return out;
    }

    /** Capa 630x500: a praia à noite com o farol e o título. */
    static void cover(Game g) throws Exception {
        Lang.set("pt"); g.registerNames();
        BufferedImage scene = capsuleScene(g);
        BufferedImage cover = new BufferedImage(630, 500, BufferedImage.TYPE_INT_RGB);
        Graphics2D c = cover.createGraphics();
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        c.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.drawImage(scene, 0, 0, 630, 500, 272, 40, 272 + 1008, 40 + 800, null);
        c.setPaint(new GradientPaint(0, 330, new Color(8, 10, 40, 0), 0, 470, new Color(8, 10, 40, 220)));
        c.fillRect(0, 330, 630, 170);
        c.setFont(new Font("Serif", Font.BOLD, 54));
        FontMetrics fm = c.getFontMetrics();
        String title = "Aventura em Aurora";
        int x = (630 - fm.stringWidth(title)) / 2;
        c.setColor(new Color(0, 0, 0, 150)); c.drawString(title, x + 3, 443);
        c.setColor(new Color(255, 240, 200)); c.drawString(title, x, 440);
        c.setFont(new Font("Serif", Font.ITALIC, 20)); fm = c.getFontMetrics();
        String sub = "uma pequena história de exploração";
        c.setColor(new Color(255, 255, 255, 220)); c.drawString(sub, (630 - fm.stringWidth(sub)) / 2, 474);
        c.dispose();
        save(cover, "capa.png");
    }

    /** Ícone 512x512: a Cintila sobre o céu noturno. */
    static void icon(Game g) throws Exception {
        BufferedImage icon = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D c = icon.createGraphics();
        c.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        c.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        c.setPaint(new GradientPaint(0, 0, new Color(18, 22, 70), 0, 512, new Color(70, 60, 140)));
        c.fillRoundRect(0, 0, 512, 512, 96, 96);
        c.setColor(new Color(255, 240, 160, 70)); c.fillOval(96, 96, 320, 320);
        BufferedImage star = g.tileSprite("cintila");
        if (star != null) c.drawImage(star, 128, 120, 256, 256, null);
        else { c.setColor(new Color(255, 230, 110)); c.fill(new Polygon(new int[]{256, 300, 400, 320, 350, 256, 162, 192, 112, 212}, new int[]{100, 210, 220, 290, 400, 340, 400, 290, 220, 210}, 10)); }
        c.dispose();
        save(icon, "icone.png");
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty(GameFiles.DATA_PROPERTY) == null) { System.err.println("Use -Daurora.dados=<pasta temporária> para não tocar nos seus saves."); System.exit(1); }
        Files.createDirectories(Path.of(System.getProperty(GameFiles.DATA_PROPERTY)));
        SwingUtilities.invokeAndWait(() -> {
            try {
                Game g = new Game();
                scenes(g, "pt"); scenes(g, "en"); cover(g); icon(g); steam(g);
                System.exit(0);
            } catch (Exception e) { e.printStackTrace(); System.exit(1); }
        });
    }
}
