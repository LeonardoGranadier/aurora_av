import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Personagens em pixel art 16x16 (ampliados 2x pelo MakeAssets), no estilo dos RPGs de Game Boy Advance:
 * contorno escuro, cabeça grande, olhos, sombreado e três quadros de caminhada.
 * Cada personagem combina uma cabeça (estilo de cabelo ou chapéu), um corpo (calça ou vestido) e uma paleta.
 * Folha: 3 colunas (parado, passo 1, passo 2) x 4 linhas (baixo, cima, esquerda, direita = esquerda espelhada).
 *
 * Letras dos moldes: . transparente · O contorno · S pele · s pele na sombra · E olho · M bochecha
 * H cabelo · h cabelo na sombra · A chapéu · a aba/sombra do chapéu · C roupa · c roupa na sombra
 * P calça · p calça na sombra · B sapato · G óculos · D barba
 */
final class Characters {
    private Characters() {}

    // ---------- Cabeças (linhas 0 a 8), por estilo: baixo, cima, esquerda ----------
    static final Map<String, String[][]> HEADS = new HashMap<>();
    static {
        HEADS.put("curto", new String[][]{{
            "................",
            ".....OOOOOO.....",
            "....OHHHHHHO....",
            "...OHHHHHHHhO...",
            "...OHHHHHHHHO...",
            "...OHSSSSSShO...",
            "...OSESSSSESO...",
            "...OSMSSSSMSO...",
            "....OOSSSSOO....",
        }, {
            "................",
            ".....OOOOOO.....",
            "....OHHHHHHO....",
            "...OHHHHHHHhO...",
            "...OHHHHHHHHO...",
            "...OHHHHHHHhO...",
            "...OhHHHHHHhO...",
            "...OOhHHHHhOO...",
            "....OOSSSSOO....",
        }, {
            "................",
            ".....OOOOOO.....",
            "....OHHHHHHO....",
            "...OHHHHHHHhO...",
            "...OHHHHHHHHO...",
            "...OSSSHHHHhO...",
            "..OSESSSHHHHO...",
            "...OMSSSShhO....",
            "....OOSSSOO.....",
        }});
        HEADS.put("longo", new String[][]{{
            "................",
            ".....OOOOOO.....",
            "....OHHHHHHO....",
            "...OHHHHHHHhO...",
            "..OHHHHHHHHHhO..",
            "..OHSSSSSSSShO..",
            "..OHSESSSSESHO..",
            "..OHSMSSSSMShO..",
            "..OHhOSSSSOHhO..",
        }, {
            "................",
            ".....OOOOOO.....",
            "....OHHHHHHO....",
            "...OHHHHHHHhO...",
            "..OHHHHHHHHHhO..",
            "..OHHHHHHHHHhO..",
            "..OHHHHHHHHHhO..",
            "..OhHHHHHHHHhO..",
            "..OOhHHHHHHhOO..",
        }, {
            "................",
            ".....OOOOOO.....",
            "....OHHHHHHO....",
            "...OHHHHHHHhO...",
            "...OHHHHHHHHhO..",
            "...OSSSHHHHHhO..",
            "..OSESSSHHHHhO..",
            "...OMSSSShHHhO..",
            "....OOSSSOHhO...",
        }});
        HEADS.put("bone", new String[][]{{
            ".....OOOOOO.....",
            "....OAAAAAAO....",
            "...OAAAAAAAaO...",
            "...OAAAAAAAAO...",
            "..OaaaaaaaaaaO..",
            "...OHSSSSSShO...",
            "...OSESSSSESO...",
            "...OSMSSSSMSO...",
            "....OOSSSSOO....",
        }, {
            ".....OOOOOO.....",
            "....OAAAAAAO....",
            "...OAAAAAAAaO...",
            "...OAAAAAAAAO...",
            "...OaAAAAAAaO...",
            "...OHHHHHHHhO...",
            "...OhHHHHHHhO...",
            "...OOhHHHHhOO...",
            "....OOSSSSOO....",
        }, {
            ".....OOOOOO.....",
            "....OAAAAAAO....",
            "...OAAAAAAAaO...",
            "...OAAAAAAAAO...",
            "OaaaaaAAAAAaO...",
            "...OSSSHHHHhO...",
            "..OSESSSHHHHO...",
            "...OMSSSShhO....",
            "....OOSSSOO.....",
        }});
        HEADS.put("chapeu", new String[][]{{
            "......OOOO......",
            ".....OAAAAO.....",
            "....OAAAAAaO....",
            "....OaaaaaaO....",
            ".OOaaaaaaaaaaOO.",
            "...OHSSSSSShO...",
            "...OSESSSSESO...",
            "...OSMSSSSMSO...",
            "....OOSSSSOO....",
        }, {
            "......OOOO......",
            ".....OAAAAO.....",
            "....OAAAAAaO....",
            "....OaaaaaaO....",
            ".OOaaaaaaaaaaOO.",
            "...OHHHHHHHhO...",
            "...OhHHHHHHhO...",
            "...OOhHHHHhOO...",
            "....OOSSSSOO....",
        }, {
            "......OOOO......",
            ".....OAAAAO.....",
            "....OAAAAAaO....",
            "....OaaaaaaO....",
            ".OOaaaaaaaaaaOO.",
            "...OSSSHHHHhO...",
            "..OSESSSHHHHO...",
            "...OMSSSShhO....",
            "....OOSSSOO.....",
        }});
        HEADS.put("calvo", new String[][]{{
            "................",
            ".....OOOOOO.....",
            "....OSSSSSSO....",
            "...OSSSSSSSsO...",
            "...OHSSSSSShO...",
            "...OHSSSSSShO...",
            "...OGGSSSGGSO...",
            "...OSMSSSSMSO...",
            "....OOSSSSOO....",
        }, {
            "................",
            ".....OOOOOO.....",
            "....OSSSSSSO....",
            "...OSSSSSSSsO...",
            "...OHSSSSSShO...",
            "...OHHHHHHHhO...",
            "...OhHHHHHHhO...",
            "...OOhHHHHhOO...",
            "....OOSSSSOO....",
        }, {
            "................",
            ".....OOOOOO.....",
            "....OSSSSSSO....",
            "...OSSSSSSSsO...",
            "...OSSSSSHHhO...",
            "...OSSSSHHHhO...",
            "..OGGSSSHHHHO...",
            "...OMSSSShhO....",
            "....OOSSSOO.....",
        }});
        HEADS.put("coque", new String[][]{{
            "......OOOO......",
            ".....OHHHhO.....",
            "....OOHHHhOO....",
            "...OHHHHHHHhO...",
            "...OHHHHHHHHO...",
            "...OHSSSSSShO...",
            "...OSESSSSESO...",
            "...OSMSSSSMSO...",
            "....OOSSSSOO....",
        }, {
            "......OOOO......",
            ".....OHHHhO.....",
            "....OOHHHhOO....",
            "...OHHHHHHHhO...",
            "...OHHHHHHHHO...",
            "...OHHHHHHHhO...",
            "...OhHHHHHHhO...",
            "...OOhHHHHhOO...",
            "....OOSSSSOO....",
        }, {
            "........OOOO....",
            ".......OHHHhO...",
            "....OOOOHHhO....",
            "...OHHHHHHHhO...",
            "...OHHHHHHHHO...",
            "...OSSSHHHHhO...",
            "..OSESSSHHHHO...",
            "...OMSSSShhO....",
            "....OOSSSOO.....",
        }});
        HEADS.put("espetado", new String[][]{{
            "....O..O..O.....",
            "...OHOOHOOHO....",
            "...OHHHHHHHhO...",
            "..OHHHHHHHHHhO..",
            "...OHHHHHHHHO...",
            "...OHSSSSSShO...",
            "...OSESSSSESO...",
            "...OSMSSSSMSO...",
            "....OOSSSSOO....",
        }, {
            "....O..O..O.....",
            "...OHOOHOOHO....",
            "...OHHHHHHHhO...",
            "..OHHHHHHHHHhO..",
            "...OHHHHHHHHO...",
            "...OHHHHHHHhO...",
            "...OhHHHHHHhO...",
            "...OOhHHHHhOO...",
            "....OOSSSSOO....",
        }, {
            ".....O..O..O....",
            "....OHOOHOOHO...",
            "...OHHHHHHHhO...",
            "..OHHHHHHHHHhO..",
            "...OHHHHHHHHO...",
            "...OSSSHHHHhO...",
            "..OSESSSHHHHO...",
            "...OMSSSShhO....",
            "....OOSSSOO.....",
        }});
    }

    // ---------- Corpos (linhas 9 a 15): calça ou vestido; baixo/cima e esquerda; 3 quadros ----------
    static final String[][] PANTS_FRONT = {{
        ".....OCCCCO.....",
        "....OCCCCCcO....",
        "...OCOCCCCOcO...",
        "...OSOCCCcOSO...",
        "....OPPPPPpO....",
        "....OPpOOPpO....",
        "....OBBOOBBO....",
    }, {
        ".....OCCCCO.....",
        "....OCCCCCcO....",
        "...OCOCCCCOcO...",
        "...OSOCCCcOSO...",
        "....OPPPPPpO....",
        "....OPpOOPpO....",
        "....OBBO.OOO....",
    }, {
        ".....OCCCCO.....",
        "....OCCCCCcO....",
        "...OCOCCCCOcO...",
        "...OSOCCCcOSO...",
        "....OPPPPPpO....",
        "....OPpOOPpO....",
        "....OOO.OBBO....",
    }};
    static final String[][] PANTS_SIDE = {{
        "......OCCCO.....",
        ".....OCCCCcO....",
        ".....OCCCCcO....",
        ".....OCSSCcO....",
        "......OPPpO.....",
        "......OPPpO.....",
        ".....OBBBBO.....",
    }, {
        "......OCCCO.....",
        ".....OCCCCcO....",
        ".....OCCCCcO....",
        ".....OSSCCcO....",
        ".....OPPPPpO....",
        "....OPpO.OPpO...",
        "...OBBO...OBBO..",
    }, {
        "......OCCCO.....",
        ".....OCCCCcO....",
        ".....OCCCCcO....",
        ".....OCCCSSO....",
        ".....OPPPPpO....",
        ".....OPpOPpO....",
        "....OBBOOBBO....",
    }};
    static final String[][] DRESS_FRONT = {{
        ".....OCCCCO.....",
        "....OCCCCCcO....",
        "...OCOCCCCOcO...",
        "...OSOCCCcOSO...",
        "....OCCCCCcO....",
        "...OCCCCCCCcO...",
        "....OBBOOBBO....",
    }, {
        ".....OCCCCO.....",
        "....OCCCCCcO....",
        "...OCOCCCCOcO...",
        "...OSOCCCcOSO...",
        "....OCCCCCcO....",
        "...OCCCCCCCcO...",
        "....OBBO.OO.....",
    }, {
        ".....OCCCCO.....",
        "....OCCCCCcO....",
        "...OCOCCCCOcO...",
        "...OSOCCCcOSO...",
        "....OCCCCCcO....",
        "...OCCCCCCCcO...",
        ".....OO.OBBO....",
    }};
    static final String[][] DRESS_SIDE = {{
        "......OCCCO.....",
        ".....OCCCCcO....",
        ".....OCCCCcO....",
        ".....OCSSCcO....",
        ".....OCCCCcO....",
        "....OCCCCCCcO...",
        ".....OBBOBBO....",
    }, {
        "......OCCCO.....",
        ".....OCCCCcO....",
        ".....OCCCCcO....",
        ".....OSSCCcO....",
        ".....OCCCCcO....",
        "....OCCCCCCcO...",
        "....OBBO..OBO...",
    }, {
        "......OCCCO.....",
        ".....OCCCCcO....",
        ".....OCCCCcO....",
        ".....OCCCSSO....",
        ".....OCCCCcO....",
        "....OCCCCCCcO...",
        ".....OBOOBBO....",
    }};

    /** Paleta de um personagem e as escolhas de cabeça, corpo e barba. */
    record Look(String head, boolean dress, boolean beard, Color skin, Color hair, Color hat, Color clothes, Color pants, Color shoes) {}

    static Color shade(Color c, double f) { return new Color((int) (c.getRed() * f), (int) (c.getGreen() * f), (int) (c.getBlue() * f), c.getAlpha()); }

    static Color colorFor(char ch, Look l) {
        return switch (ch) {
            case 'O' -> new Color(38, 30, 46);
            case 'S' -> l.skin(); case 's' -> shade(l.skin(), 0.85);
            case 'E' -> new Color(38, 30, 46);
            case 'M' -> new Color(240, 150, 150);
            case 'H' -> l.hair(); case 'h' -> shade(l.hair(), 0.75);
            case 'A' -> l.hat(); case 'a' -> shade(l.hat(), 0.72);
            case 'C' -> l.clothes(); case 'c' -> shade(l.clothes(), 0.75);
            case 'P' -> l.pants(); case 'p' -> shade(l.pants(), 0.75);
            case 'B' -> l.shoes();
            case 'G' -> new Color(70, 80, 100);
            case 'D' -> l.hair();
            default -> null;
        };
    }

    /** Quadro 16x16 (direção 0 baixo, 1 cima, 2 esquerda, 3 direita; quadro 0 parado, 1 e 2 passos). */
    static BufferedImage frame(Look l, int dir, int step) {
        int view = dir == 3 ? 2 : dir;
        String[] head = HEADS.get(l.head())[view];
        String[][] bodies = l.dress() ? (view == 2 ? DRESS_SIDE : DRESS_FRONT) : (view == 2 ? PANTS_SIDE : PANTS_FRONT);
        String[] body = bodies[step];
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 7; y++) paint(img, body[y], 9 + y, l);
        for (int y = 0; y < 9; y++) paint(img, head[y], y, l);
        if (l.beard() && view != 1) beard(img, view, l);
        if (dir == 3) img = mirror(img);
        return img;
    }

    static void paint(BufferedImage img, String row, int y, Look l) {
        for (int x = 0; x < 16 && x < row.length(); x++) {
            Color c = colorFor(row.charAt(x), l);
            if (c != null) img.setRGB(x, y, c.getRGB());
        }
    }

    /** Barba cobrindo queixo e bochechas. */
    static void beard(BufferedImage img, int view, Look l) {
        int color = l.hair().getRGB(), dark = shade(l.hair(), 0.75).getRGB(), outline = new Color(38, 30, 46).getRGB();
        if (view == 0) {                                                      // costeletas, queixo e ponta abaixo do rosto
            img.setRGB(4, 7, dark); img.setRGB(11, 7, dark);
            for (int x = 5; x <= 10; x++) img.setRGB(x, 8, x == 10 ? dark : color);
            for (int x = 6; x <= 9; x++) img.setRGB(x, 9, x == 9 ? dark : color);
            img.setRGB(5, 9, outline); img.setRGB(10, 9, outline);
            img.setRGB(6, 10, outline); img.setRGB(7, 10, color); img.setRGB(8, 10, color); img.setRGB(9, 10, outline);
        } else {
            for (int x = 4; x <= 7; x++) img.setRGB(x, 8, color);
            for (int x = 5; x <= 7; x++) img.setRGB(x, 9, color);
            img.setRGB(4, 9, outline); img.setRGB(5, 10, outline); img.setRGB(6, 10, color); img.setRGB(7, 10, outline);
        }
    }

    static BufferedImage mirror(BufferedImage img) {
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) out.setRGB(15 - x, y, img.getRGB(x, y));
        return out;
    }

    /** Folha 48x64: 3 quadros x 4 direções. */
    static BufferedImage sheet(Look l) {
        BufferedImage sheet = new BufferedImage(48, 64, BufferedImage.TYPE_INT_ARGB);
        for (int d = 0; d < 4; d++) for (int f = 0; f < 3; f++) {
            BufferedImage fr = frame(l, d, f);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) sheet.setRGB(f * 16 + x, d * 16 + y, fr.getRGB(x, y));
        }
        return sheet;
    }

    static Color c(int r, int g, int b) { return new Color(r, g, b); }
    static final Color SKIN = c(250, 214, 178), SKIN_TAN = c(222, 172, 128), SKIN_DARK = c(170, 116, 82);

    /** Todos os personagens do jogo (nome do arquivo → aparência). */
    static Map<String, Look> all() {
        Map<String, Look> m = new LinkedHashMap<>();
        m.put("player",      new Look("bone", false, false, SKIN, c(110, 70, 40), c(220, 60, 60), c(56, 96, 196), c(48, 56, 90), c(200, 60, 60)));
        m.put("mae",         new Look("longo", true, false, SKIN, c(130, 78, 44), null, c(214, 100, 156), null, c(120, 70, 50)));
        m.put("prof",        new Look("calvo", false, false, SKIN, c(230, 230, 236), null, c(244, 244, 248), c(84, 90, 110), c(70, 50, 40)));
        m.put("marinho",     new Look("chapeu", false, true, SKIN_TAN, c(236, 236, 240), c(44, 62, 120), c(48, 92, 168), c(52, 62, 96), c(40, 40, 50)));
        m.put("capita",      new Look("chapeu", false, false, SKIN_TAN, c(236, 236, 236), c(30, 110, 120), c(40, 146, 156), c(250, 246, 236), c(90, 60, 40)));
        m.put("eremita",     new Look("calvo", true, true, SKIN, c(210, 210, 214), null, c(120, 88, 64), null, c(70, 52, 40)));
        m.put("montanhista", new Look("bone", false, false, SKIN, c(80, 50, 30), c(56, 70, 150), c(214, 66, 60), c(60, 70, 96), c(60, 44, 34)));
        m.put("lenhador",    new Look("curto", false, true, SKIN_TAN, c(100, 58, 30), null, c(180, 56, 44), c(84, 70, 56), c(60, 40, 30)));
        m.put("beto",        new Look("bone", false, false, SKIN_DARK, c(40, 32, 30), c(232, 200, 120), c(62, 150, 128), c(60, 66, 80), c(70, 50, 40)));
        m.put("rosa",        new Look("coque", true, false, SKIN, c(170, 70, 50), null, c(218, 108, 150), null, c(150, 70, 90)));
        m.put("lucas",       new Look("espetado", false, false, SKIN_TAN, c(92, 56, 32), null, c(242, 182, 62), c(70, 96, 150), c(220, 220, 230)));
        m.put("lia",         new Look("longo", true, false, SKIN_TAN, c(60, 40, 70), null, c(152, 122, 204), null, c(80, 60, 110)));
        m.put("mascate",     new Look("chapeu", false, true, SKIN_TAN, c(80, 60, 40), c(150, 100, 60), c(160, 110, 64), c(80, 70, 60), c(70, 50, 40)));
        return m;
    }
}
