import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.SwingUtilities;

/**
 * Ferramenta: gera GIFs animados de gameplay para divulgação em loja/gifs/ (fica fora do pacote do jogo).
 * O jogo roda de verdade, quadro a quadro, com teclas "seguradas" por código.
 * Rodar na pasta do projeto, numa partida descartável:  java -Daurora.dados=$(mktemp -d) -cp .build Gifs
 */
public class Gifs {
    static final File OUT = new File("loja/gifs");
    static final int FPS = 15;
    static final double DT = 1.0 / FPS;

    static BufferedImage frame(Game g, boolean hud) {
        BufferedImage image = new BufferedImage(Game.W, Game.H, BufferedImage.TYPE_INT_RGB);
        Graphics2D gr = image.createGraphics();
        g.fade = 0; g.toastT = 0; g.photo = !hud;
        try { g.paintScene(gr); } finally { g.photo = false; gr.dispose(); }
        return image;
    }

    /** Avança o jogo e grava n quadros; keys são as teclas lógicas seguradas nesse trecho. */
    static void record(Game g, List<BufferedImage> frames, int n, boolean hud, int... keys) {
        for (int k : keys) g.pressLogical(k);
        for (int i = 0; i < n; i++) { g.step(DT); frames.add(frame(g, hud)); }
        for (int k : keys) { g.down.remove(k); }
    }

    static void save(List<BufferedImage> frames, String name) throws Exception {
        OUT.mkdirs();
        File file = new File(OUT, name);
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(file)) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            for (BufferedImage f : frames) {
                ImageWriteParam param = writer.getDefaultWriteParam();
                ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(f);
                IIOMetadata meta = writer.getDefaultImageMetadata(type, param);
                String format = meta.getNativeMetadataFormatName();
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);
                IIOMetadataNode gce = child(root, "GraphicControlExtension");
                gce.setAttribute("disposalMethod", "none"); gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("transparentColorFlag", "FALSE"); gce.setAttribute("delayTime", "" + (100 / FPS));
                gce.setAttribute("transparentColorIndex", "0");
                IIOMetadataNode apps = child(root, "ApplicationExtensions");
                IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
                app.setAttribute("applicationID", "NETSCAPE"); app.setAttribute("authenticationCode", "2.0");
                app.setUserObject(new byte[]{1, 0, 0});                                   // repetir para sempre
                apps.appendChild(app);
                meta.setFromTree(format, root);
                writer.writeToSequence(new IIOImage(f, null, meta), param);
            }
            writer.endWriteSequence();
        }
        writer.dispose();
        System.out.println(file + " (" + frames.size() + " quadros, " + file.length() / 1024 + " KB)");
    }
    static IIOMetadataNode child(IIOMetadataNode root, String name) {
        for (int i = 0; i < root.getLength(); i++) if (root.item(i).getNodeName().equals(name)) return (IIOMetadataNode) root.item(i);
        IIOMetadataNode node = new IIOMetadataNode(name); root.appendChild(node); return node;
    }

    static void at(Game g, int day, int hour, int minute) {
        g.weather.seed = Weather.LEGACY_SEED;
        while (g.weather.forDay(day + 1) != Weather.SUN) day++;
        g.time = day * 1440 + hour * 60 + minute;
    }
    static void fresh(Game g, int stage) {
        g.newGame(); g.state = Game.PLAY; g.stage = stage; g.explorer.name = "Ana"; g.explorer.form = Explorer.GIRL;
        g.access.textSpeed = 0; g.refreshObjects(); g.bannerT = 0;
    }

    /** Passeio pela vila na primavera, com moradores e borboletas. */
    static List<BufferedImage> village(Game g) {
        List<BufferedImage> f = new ArrayList<>();
        fresh(g, 21); g.travel("Vila Aurora", 6, 11); at(g, 2, 9, 30); g.dir = 3;
        record(g, f, 50, true, KeyEvent.VK_RIGHT);
        record(g, f, 14, true, KeyEvent.VK_DOWN);
        record(g, f, 16, true);
        return f;
    }

    /** Pesca: a boia, a fisgada e a barra de captura. */
    static List<BufferedImage> fishing(Game g) {
        List<BufferedImage> f = new ArrayList<>();
        fresh(g, 21); g.life.rod = true; g.travel("Vila Aurora", 19, 13); at(g, 3, 10, 0); g.dir = 3;
        g.life.cast(); g.life.fishTimer = 1.0;
        record(g, f, 16, true);
        g.life.fishPhase = 2; g.life.catchProgress = 0.35;                                // fisgou: puxando a linha
        for (int i = 0; i < 160; i++) {
            double ahead = g.life.fishTarget - g.life.fishPosition - g.life.fishVelocity * 0.8;
            boolean hold = ahead > 0;                                                      // segue o peixe como um jogador
            if (hold) g.down.add(KeyEvent.VK_Z); else g.down.remove(KeyEvent.VK_Z);
            g.step(DT); f.add(frame(g, true));
            if (g.life.fishPhase == VillageLife.RESULT) break;
        }
        g.down.remove(KeyEvent.VK_Z);
        record(g, f, 20, true);
        g.state = Game.PLAY;
        return f;
    }

    /** A Gaivota no Alto-mar e a aurora sobre a vila à noite. */
    static List<BufferedImage> sea(Game g) {
        List<BufferedImage> f = new ArrayList<>();
        fresh(g, Story.FINAL); g.boat.owned = true; g.travel(Boat.SEA, 8, 7); at(g, 5, 17, 30); g.dir = 3;
        record(g, f, 30, false, KeyEvent.VK_RIGHT);
        record(g, f, 20, false);
        return f;
    }
    static List<BufferedImage> aurora(Game g) {
        List<BufferedImage> f = new ArrayList<>();
        fresh(g, Story.FINAL); g.travel("Vila Aurora", 15, 13); at(g, 8, 22, 0); g.dir = 1;
        record(g, f, 24, false, KeyEvent.VK_UP);
        record(g, f, 36, false);
        return f;
    }

    public static void main(String[] args) throws Exception {
        if (System.getProperty(GameFiles.DATA_PROPERTY) == null) { System.err.println("Use -Daurora.dados=<pasta temporária> para não tocar nos seus saves."); System.exit(1); }
        Files.createDirectories(Path.of(System.getProperty(GameFiles.DATA_PROPERTY)));
        SwingUtilities.invokeAndWait(() -> {
            try {
                Game g = new Game();
                for (String lang : new String[]{"pt", "en"}) {
                    Lang.set(lang); g.registerNames();
                    List<BufferedImage> v = village(g), fi = fishing(g), s = sea(g), a = aurora(g);
                    save(v, "vila-" + lang + ".gif"); save(fi, "pesca-" + lang + ".gif");
                    if (lang.equals("pt")) { save(s, "alto-mar.gif"); save(a, "aurora.gif"); }             // sem textos na tela
                    List<BufferedImage> all = new ArrayList<>();                                // trailer curto: um pouco de cada
                    all.addAll(v.subList(0, Math.min(45, v.size()))); all.addAll(fi); all.addAll(s.subList(0, Math.min(30, s.size()))); all.addAll(a);
                    save(all, "aventura-em-aurora-" + lang + ".gif");
                }
                System.exit(0);
            } catch (Exception e) { e.printStackTrace(); System.exit(1); }
        });
    }
}
