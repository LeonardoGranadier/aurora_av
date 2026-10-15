import java.awt.*;

/**
 * Estilo das janelas do jogo, à moda dos RPGs clássicos: moldura clara com contorno escuro e cantos recortados,
 * fundo azul-marinho em degradê, sombra, texto com sombra e teclas desenhadas como botões.
 */
final class Ui {
    private Ui() {}

    static final Color INK = new Color(16, 18, 38), EDGE = new Color(240, 234, 210), EDGE_SHADE = new Color(178, 168, 140),
        TOP = new Color(46, 64, 132), BOTTOM = new Color(26, 34, 84), GOLD = new Color(255, 214, 110), TEXT = new Color(250, 248, 240),
        SOFT = new Color(196, 206, 236);

    /** Janela clássica; o conteúdo começa em x + 10, y + 8. alpha < 255 deixa o fundo um pouco transparente. */
    static void window(Graphics2D g, int x, int y, int w, int h, int alpha) {
        g.setColor(new Color(0, 0, 0, alpha * 90 / 255)); g.fillRect(x + 3, y + 3, w, h);                 // sombra
        g.setColor(INK);                                                                                    // contorno, cantos recortados
        g.fillRect(x + 2, y, w - 4, h); g.fillRect(x, y + 2, w, h - 4); g.fillRect(x + 1, y + 1, w - 2, h - 2);
        g.setColor(EDGE); g.fillRect(x + 2, y + 2, w - 4, h - 4);                                           // moldura clara
        g.setColor(EDGE_SHADE); g.fillRect(x + 2, y + h - 4, w - 4, 2); g.fillRect(x + w - 4, y + 2, 2, h - 4);
        g.setColor(INK); g.fillRect(x + 4, y + 4, w - 8, h - 8);                                            // filete interno
        Paint old = g.getPaint();
        g.setPaint(new GradientPaint(0, y + 5, alpha(TOP, alpha), 0, y + h - 5, alpha(BOTTOM, alpha)));
        g.fillRect(x + 5, y + 5, w - 10, h - 10);
        g.setPaint(old);
        g.setColor(new Color(255, 255, 255, 34)); g.fillRect(x + 5, y + 5, w - 10, 2);                     // brilho no topo
    }
    static void window(Graphics2D g, int x, int y, int w, int h) { window(g, x, y, w, h, 238); }

    static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    /** Texto (já traduzido) com sombra de 1 pixel. */
    static void text(Graphics2D g, String s, int x, int y, Color color) {
        g.setColor(new Color(0, 0, 0, Math.min(200, color.getAlpha()))); g.drawString(s, x + 1, y + 1);
        g.setColor(color); g.drawString(s, x, y);
    }

    /** Tecla como um botãozinho; devolve a largura. O rótulo é desenhado como está. */
    static int key(Graphics2D g, String label, int x, int y) {
        Font old = g.getFont();
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        FontMetrics fm = g.getFontMetrics();
        int w = Math.max(15, fm.stringWidth(label) + 8), h = 15;
        g.setColor(INK); g.fillRect(x + 1, y, w - 2, h); g.fillRect(x, y + 1, w, h - 2);
        g.setColor(new Color(150, 144, 128)); g.fillRect(x + 1, y + 1, w - 2, h - 2);                      // lateral
        g.setColor(new Color(236, 232, 216)); g.fillRect(x + 1, y + 1, w - 2, h - 4);                      // tampa
        g.setColor(INK); g.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + 11);
        g.setFont(old);
        return w;
    }

    /** Sol (dia) ou lua (noite), em pixel art de 12x12. */
    static void sky(Graphics2D g, int x, int y, boolean night) {
        if (night) {
            g.setColor(new Color(226, 230, 255)); g.fillOval(x, y, 12, 12);
            g.setColor(BOTTOM); g.fillOval(x + 4, y - 2, 11, 11);
        } else {
            g.setColor(new Color(255, 196, 60));
            g.fillRect(x + 5, y - 2, 2, 16); g.fillRect(x - 2, y + 5, 16, 2);
            g.setColor(new Color(255, 222, 100)); g.fillOval(x + 1, y + 1, 10, 10);
        }
    }

    /** Ícone pequeno do clima: sol, nuvem com chuva, neblina ou neve. */
    static void weather(Graphics2D g, int x, int y, String label, int kind) {
        if (label.equals("Neve")) {
            g.setColor(Color.WHITE);
            for (int i = 0; i < 3; i++) { g.fillRect(x + 1 + i * 4, y + 2 + (i % 2) * 5, 2, 2); }
            g.fillRect(x + 6, y + 8, 2, 2);
            return;
        }
        if (kind == Weather.SUN) { g.setColor(new Color(255, 214, 90)); g.fillOval(x + 1, y + 1, 10, 10); return; }
        g.setColor(new Color(214, 222, 236)); g.fillOval(x, y + 3, 8, 6); g.fillOval(x + 4, y + 1, 9, 8);
        g.setColor(new Color(130, 190, 240));
        if (kind == Weather.RAIN) for (int i = 0; i < 3; i++) g.fillRect(x + 2 + i * 4, y + 10, 1, 3);
        else { g.setColor(new Color(214, 222, 236, 180)); g.fillRect(x - 1, y + 11, 15, 1); g.fillRect(x + 1, y + 13, 12, 1); }
    }

    /** Estrelinha de 12x12 (céu limpo à noite). */
    static void star(Graphics2D g, int x, int y) {
        g.setColor(new Color(255, 236, 150));
        g.fillRect(x + 5, y, 2, 12); g.fillRect(x, y + 5, 12, 2); g.fillRect(x + 4, y + 4, 4, 4);
    }

    /** Setinha de cursor (▶) em pixel art. */
    static void cursor(Graphics2D g, int x, int y) {
        g.setColor(INK); g.fillPolygon(new int[]{x + 1, x + 1, x + 8}, new int[]{y - 5, y + 5, y}, 3);
        g.setColor(GOLD); g.fillPolygon(new int[]{x, x, x + 7}, new int[]{y - 6, y + 4, y - 1}, 3);
    }
}
