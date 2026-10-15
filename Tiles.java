import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Tiles principais em pixel art 16x16 (o MakeAssets amplia 2x): grama, caminho, areia, água, árvores,
 * pedras, telhado, parede, piso, janela, porta, neve e gelo. Desenhados com moldes de texto para ficarem
 * consistentes entre si. Letras: '.' transparente e as definidas na paleta de cada molde.
 */
final class Tiles {
    private Tiles() {}

    static Color c(int r, int g, int b) { return new Color(r, g, b); }

    static BufferedImage draw(String[] rows, Map<Character, Color> palette) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            Color col = palette.get(rows[y].charAt(x));
            if (col != null) img.setRGB(x, y, col.getRGB());
        }
        return img;
    }

    static final String[] GRASS = {
        "gggggggggggggggg",
        "gggdggggggggLggg",
        "ggdgdgggggggggggg".substring(0, 16),
        "gggggggggggggggg",
        "gggggggLgggggggg",
        "ggggggggggggdggg",
        "gLgggggggggdgdgg",
        "gggggggggggggggg",
        "ggggdggggggggggg",
        "gggdgdggggLggggg",
        "gggggggggggggggg",
        "gggggggggggggggg",
        "ggggggggdgggggLg",
        "gLgggggggdggdggg",
        "gggggggggggdgggg",
        "gggggggggggggggg",
    };
    static final String[] PATH = {
        "pppppppppppppppp",
        "ppppqpppppppppPp",
        "ppppppppkkpppppp",
        "pPpppppkllkppppp",
        "pppppppkkkpppqpp",
        "pppqpppppppppppp",
        "pppppppppppPpppp",
        "pppppPppppppppqp",
        "pkkppppppqpppppp",
        "klkkpppppppppppp",
        "kkkpppppppppkkpp",
        "ppppppqpppppklkp",
        "pppPppppppppkkkp",
        "ppppppppPppppppp",
        "ppqpppppppppqppp",
        "pppppppppppppppp",
    };
    static final String[] SAND = {
        "ssssssssssssssss",
        "sssssssssssssLss",
        "ssddsssssssssssss".substring(0, 16),
        "sssssddsssssssss",
        "ssssssssddssssss",
        "sLssssssssssssss",
        "ssssssssssssddss",
        "sssssssssssssssd",
        "ssssLsssssssssss",
        "ddsssssssssLssss",
        "ssddssssssssssss",
        "ssssddssssssssss",
        "ssssssssssddssss",
        "sssssssssssssdds",
        "sssLssssssssssss",
        "ssssssssssssssss",
    };
    static final String[][] WATER = {{
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwLLLwwwwwwwwww",
        "wwLwwwLwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwdwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwLLLww",
        "wwwwwwwwwwLwwwLw",
        "wwwdwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwLLLwwwwwwww",
        "wwwwLwwwLwwwwdww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
    }, {
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwLLLwwwwwwww",
        "wwwwLwwwLwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwdwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwLLLwwww",
        "wwwwwwwwLwwwLwww",
        "wdwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
        "wwwLLLwwwwwwwwww",
        "wwLwwwLwwwwwwwdw",
        "wwwwwwwwwwwwwwww",
        "wwwwwwwwwwwwwwww",
    }};
    static final String[] TREE = {
        "....OOOOOOO.....",
        "..OOlLLLLLlOO...",
        ".OlLLLLLLLLLlO..",
        ".OLLlLLLLLLLLO..",
        "OlLLLLLLLLlLLlO.",
        "OLLLLLLLLLLLLLO.",
        "OLLLLLLLLLLLLdO.",
        "OlLLLLLLLLLLddO.",
        ".OLLLLLLLLLddO..",
        ".OdLLLLLLLdddO..",
        "..OddLLLddddO...",
        "...OOdddddOO....",
        "......OtTO......",
        "......OtTO......",
        ".....OttTTO.....",
        "....hhhhhhhh....",
    };
    static final String[] PINE = {
        ".......OO.......",
        "......OLLO......",
        ".....OLLLdO.....",
        "....OLLLLLdO....",
        "......OLLdO.....",
        "....OOLLLLdOO...",
        "...OLLLLLLLLdO..",
        "....OOLLLLLdOO..",
        "...OOLLLLLLLdOO.",
        "..OLLLLLLLLLLLdO",
        "..OdLLLLLLLLLddO",
        "...OOdddddddddOO",
        "......OtTO......",
        "......OtTO......",
        ".....OttTTO.....",
        "....hhhhhhhh....",
    };
    static final String[] ROCK = {
        "kkkkkkkkkkkkkkkk",
        "kLLLLkkkkLLLkkkk",
        "kLrrrrdkkLrrrdkk",
        "kLrrrrrdkLrrrrdk",
        "kLrrrrrdkdrrrrdk",
        "kdrrrrrdkkdrrrdk",
        "kkdddddkkkkdddkk",
        "kkkkkkkkkkkkkkkk",
        "kkkLLLkkkLLLLLkk",
        "kkLrrrdkLrrrrrdk",
        "kLrrrrrdLrrrrrrd",
        "kLrrrrrddrrrrrrd",
        "kdrrrrdkkdrrrrdk",
        "kkddddkkkkddddkk",
        "kkkkkkkkkkkkkkkk",
        "kkkkkkkkkkkkkkkk",
    };
    static final String[] ROOF = {
        "rrrrrrrrrrrrrrrr",
        "rLLrrrLLrrrLLrrr",
        "rrrrrrrrrrrrrrrr",
        "dddddddddddddddd",
        "rrrrLLrrrLLrrrLL",
        "rrrrrrrrrrrrrrrr",
        "rrrrrrrrrrrrrrrr",
        "dddddddddddddddd",
        "rLLrrrLLrrrLLrrr",
        "rrrrrrrrrrrrrrrr",
        "rrrrrrrrrrrrrrrr",
        "dddddddddddddddd",
        "rrrrLLrrrLLrrrLL",
        "rrrrrrrrrrrrrrrr",
        "rrrrrrrrrrrrrrrr",
        "dddddddddddddddd",
    };
    static final String[] FLOOR = {
        "wwwwwwwwwwwwwwww",
        "wwlwwwwwwwwwlwww",
        "wwwwwwwwwwwwwwww",
        "dddddddddddddddd",
        "wwwwwwwdwwwwwwww",
        "wwwwlwwdwwwwwlww",
        "wwwwwwwdwwwwwwww",
        "dddddddddddddddd",
        "wwdwwwwwwwwwwwww",
        "wwdwwwwwwlwwwwww",
        "wwdwwwwwwwwwwwww",
        "dddddddddddddddd",
        "wwwwwwwwwwdwwwww",
        "wwlwwwwwwwdwwwlw",
        "wwwwwwwwwwdwwwww",
        "dddddddddddddddd",
    };

    /** Flores: três canteirinhos de cores diferentes. */
    static BufferedImage flower() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[][] spots = {{4, 4}, {11, 6}, {6, 11}};
        Color[] petals = {c(255, 150, 190), c(255, 255, 255), c(250, 210, 80)};
        for (int i = 0; i < spots.length; i++) {
            int x = spots[i][0], y = spots[i][1];
            int p = petals[i].getRGB(), core = (i == 2 ? c(220, 120, 50) : c(250, 210, 70)).getRGB(), stem = c(56, 120, 50).getRGB();
            img.setRGB(x, y - 1, p); img.setRGB(x - 1, y, p); img.setRGB(x + 1, y, p); img.setRGB(x, y + 1, p);
            img.setRGB(x, y, core); img.setRGB(x, y + 2, stem);
        }
        return img;
    }

    static void generate(MakeAssets.Saver save) throws Exception {
        Color gBase = c(104, 176, 80), gDark = c(78, 148, 60), gLight = c(140, 200, 104);
        save.save(draw(GRASS, Map.of('g', gBase, 'd', gDark, 'L', gLight)), "grass");
        save.save(draw(PATH, Map.of('p', c(214, 182, 128), 'q', c(196, 162, 108), 'P', c(228, 202, 150),
            'k', c(150, 136, 120), 'l', c(196, 186, 172))), "path");
        save.save(draw(SAND, Map.of('s', c(240, 222, 164), 'd', c(222, 200, 140), 'L', c(250, 238, 196))), "sand");
        for (int f = 0; f < 2; f++)
            save.save(draw(WATER[f], Map.of('w', c(64, 132, 214), 'd', c(48, 110, 192), 'L', c(160, 210, 248))), "water" + (f + 1));
        Color outline = c(28, 52, 40), shadow = new Color(20, 40, 20, 70);
        save.save(draw(TREE, Map.of('O', outline, 'L', c(64, 150, 70), 'l', c(110, 190, 96), 'd', c(40, 112, 56),
            't', c(110, 72, 40), 'T', c(140, 96, 56), 'h', shadow)), "tree");
        save.save(draw(PINE, Map.of('O', outline, 'L', c(46, 120, 76), 'd', c(30, 90, 60),
            't', c(100, 66, 40), 'T', c(130, 90, 54), 'h', shadow)), "pine");
        save.save(draw(ROCK, Map.of('k', c(70, 64, 78), 'r', c(118, 112, 126), 'L', c(150, 146, 160), 'd', c(90, 84, 98))), "rock");
        save.save(draw(ROOF, Map.of('r', c(196, 74, 62), 'L', c(222, 108, 88), 'd', c(150, 50, 46))), "roof");
        save.save(draw(FLOOR, Map.of('w', c(206, 156, 100), 'l', c(222, 176, 120), 'd', c(170, 120, 74))), "floor");
        save.save(flower(), "flower");
    }
}
