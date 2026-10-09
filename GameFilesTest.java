import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

/** Run in an empty temporary directory with the assets link, like the other tests. */
public class GameFilesTest {
    static void check(boolean value, String message) { VillageLifeTest.check(value, message); }
    static BufferedImage render(Game g, int w, int h) {
        g.setSize(w, h); g.fade = 0;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics(); g.paint(graphics); graphics.dispose();
        return image;
    }
    /** Fraction of non-black pixels. */
    static double lit(BufferedImage image) {
        int lit = 0, total = 0;
        for (int y = 0; y < image.getHeight(); y += 2) for (int x = 0; x < image.getWidth(); x += 2, total++)
            if ((image.getRGB(x, y) & 0xffffff) != 0) lit++;
        return lit / (double) total;
    }
    static void run() throws Exception {
        check(Game.SAVE.equals("savegame.properties"), "Development keeps the save in the current folder");
        check(Audio.CONFIG.getPath().equals("config.properties"), "Development keeps options in the current folder");
        check(GameFiles.assetsDir().isDirectory(), "Assets found");
        String previous = System.getProperty(GameFiles.DATA_PROPERTY);
        try {
            System.setProperty(GameFiles.DATA_PROPERTY, "pasta-dados");
            check(GameFiles.data("x.properties").equals(Path.of("pasta-dados", "x.properties")), "Configured data folder used");
            GameFiles.ensureDataDir(); check(Files.isDirectory(Path.of("pasta-dados")), "Data folder created");
        } finally {
            if (previous == null) System.clearProperty(GameFiles.DATA_PROPERTY); else System.setProperty(GameFiles.DATA_PROPERTY, previous);
        }
        check(GameFiles.userDataDir().isAbsolute(), "User data folder is absolute");

        Game g = new Game(); g.state = Game.PLAY;
        BufferedImage base = render(g, Game.W, Game.H), doubled = render(g, Game.W * 2, Game.H * 2);
        int same = 0, total = 0;
        for (int y = 40; y < Game.H - 40; y += 7) for (int x = 0; x < Game.W; x += 7, total++)
            if (base.getRGB(x, y) == doubled.getRGB(x * 2, y * 2)) same++;
        check(same > total * 0.95, "Double scale reproduces the scene");
        BufferedImage wide = render(g, Game.W * 2 + 200, Game.H * 2);
        check((wide.getRGB(50, Game.H) & 0xffffff) == 0 && (wide.getRGB(Game.W * 2 + 150, Game.H) & 0xffffff) == 0, "Wide window letterboxed");
        check(wide.getRGB(100 + 2 * 100, 2 * 200) == base.getRGB(100, 200), "Wide window centered");
        BufferedImage small = render(g, Game.W / 2, Game.H / 2);
        double baseLit = lit(base), smallLit = lit(small);
        check(baseLit > 0.1 && Math.abs(baseLit - smallLit) < 0.05, "Small window shows the whole scene scaled down");
        ImageIO.write(doubled, "png", Path.of("scale-2x.png").toFile());
        System.out.println("PASS: " + VillageLifeTest.checks + " file and display checks");
    }
    public static void main(String[] args) throws Exception {
        if (Files.exists(Path.of(Game.SAVE))) throw new IllegalStateException("Use a clean temporary directory.");
        SwingUtilities.invokeAndWait(() -> { try { run(); } catch (Throwable e) { e.printStackTrace(); System.exit(1); } });
        System.exit(0);
    }
}
