import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Gera os sprites de pixel art na pasta assets/ (personagens em Characters.java, tiles principais em Tiles.java).
 * Rodar:  javac MakeAssets.java && java MakeAssets
 * Depois é só substituir qualquer PNG pelo seu próprio (mesmo nome de arquivo).
 */
public class MakeAssets {
    static final int S = 16, SCALE = 2;          // desenha em 16x16 e amplia 2x (32x32)
    static final Random rnd = new Random(3);

    static Color c(int r, int g, int b) { return new Color(r, g, b); }
    static BufferedImage canvas(int w, int h) { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }
    static void px(BufferedImage i, int x, int y, Color c) {
        if (x >= 0 && y >= 0 && x < i.getWidth() && y < i.getHeight()) i.setRGB(x, y, c.getRGB());
    }
    static void rect(BufferedImage i, int x, int y, int w, int h, Color c) {
        for (int j = y; j < y + h; j++) for (int k = x; k < x + w; k++) px(i, k, j, c);
    }
    static BufferedImage tile(Color base) { BufferedImage i = canvas(S, S); rect(i, 0, 0, S, S, base); return i; }
    static void noise(BufferedImage i, Color c, int n) { for (int k = 0; k < n; k++) px(i, rnd.nextInt(S), rnd.nextInt(S), c); }
    static void disc(BufferedImage i, double cx, double cy, double r, Color c) {
        for (int y = 0; y < i.getHeight(); y++)
            for (int x = 0; x < i.getWidth(); x++)
                if (Math.hypot(x + 0.5 - cx, y + 0.5 - cy) <= r) px(i, x, y, c);
    }
    interface Saver { void save(BufferedImage image, String name) throws Exception; }

    static void save(BufferedImage i, String name) throws Exception {
        BufferedImage o = canvas(i.getWidth() * SCALE, i.getHeight() * SCALE);
        for (int y = 0; y < o.getHeight(); y++)
            for (int x = 0; x < o.getWidth(); x++) o.setRGB(x, y, i.getRGB(x / SCALE, y / SCALE));
        ImageIO.write(o, "png", new File("assets/" + name + ".png"));
    }

    // ---------- Tiles ----------
    static void tiles() throws Exception {
        BufferedImage i = tile(c(92, 168, 72)); noise(i, c(74, 146, 58), 14); noise(i, c(112, 188, 92), 8); save(i, "grass");

        i = canvas(S, S);
        for (int[] f : new int[][]{{4, 5}, {11, 11}}) {
            Color petal = f[0] == 4 ? c(255, 150, 190) : c(255, 255, 255);
            px(i, f[0], f[1] - 1, petal); px(i, f[0] - 1, f[1], petal); px(i, f[0] + 1, f[1], petal); px(i, f[0], f[1] + 1, petal);
            px(i, f[0], f[1], c(255, 220, 70));
        }
        save(i, "flower");

        i = canvas(S, S);
        rect(i, 7, 11, 3, 5, c(110, 75, 40));
        disc(i, 8, 7, 7.2, c(30, 100, 44));
        disc(i, 8, 6.5, 6.2, c(44, 130, 56));
        disc(i, 6, 5, 2.6, c(70, 160, 76));
        px(i, 10, 8, c(34, 108, 48)); px(i, 11, 7, c(34, 108, 48));
        save(i, "tree");

        for (int w = 1; w <= 2; w++) {
            i = tile(c(52, 110, 200));
            Color l = c(130, 180, 240);
            int o = (w - 1) * 3;
            for (int k = 0; k < 4; k++) { px(i, 2 + o + k, 4, l); px(i, 8 - o + k, 11, l); }
            save(i, "water" + w);
        }

        i = tile(c(232, 212, 150)); noise(i, c(210, 188, 125), 12); noise(i, c(244, 228, 175), 6); save(i, "sand");
        i = tile(c(205, 175, 115)); noise(i, c(180, 150, 95), 14); noise(i, c(160, 135, 85), 4); save(i, "path");

        i = tile(c(205, 155, 100));
        rect(i, 0, 7, S, 1, c(170, 120, 75)); rect(i, 0, 15, S, 1, c(170, 120, 75));
        rect(i, 5, 0, 1, 7, c(170, 120, 75)); rect(i, 11, 8, 1, 7, c(170, 120, 75));
        save(i, "floor");

        i = tile(c(110, 75, 55));
        for (int r = 3; r < S; r += 4) rect(i, 0, r, S, 1, c(80, 52, 38));
        for (int r = 0; r < S; r += 4) rect(i, (r / 4) % 2 == 0 ? 4 : 10, r, 1, 3, c(80, 52, 38));
        save(i, "wall");

        i = tile(c(190, 70, 60));
        for (int r = 3; r < S; r += 4) rect(i, 0, r, S, 1, c(150, 50, 45));
        for (int r = 0; r < S; r += 4) for (int x = (r / 4) % 2 * 4; x < S; x += 8) px(i, x, r, c(215, 95, 80));
        save(i, "roof");

        i = tile(c(240, 230, 205)); noise(i, c(225, 214, 188), 8); rect(i, 0, 15, S, 1, c(200, 190, 165)); save(i, "house_wall");

        for (int n = 0; n < 2; n++) {
            i = canvas(S, S);
            rect(i, 3, 3, 10, 9, c(90, 70, 50));
            rect(i, 4, 4, 8, 7, n == 0 ? c(130, 190, 230) : c(255, 220, 120));
            rect(i, 8, 4, 1, 7, c(90, 70, 50)); rect(i, 4, 7, 8, 1, c(90, 70, 50));
            if (n == 0) { px(i, 5, 5, c(220, 240, 255)); px(i, 9, 8, c(220, 240, 255)); }
            save(i, n == 0 ? "window" : "window_night");
        }

        i = canvas(S, S);
        rect(i, 3, 1, 10, 15, c(90, 55, 30)); rect(i, 4, 2, 8, 14, c(120, 75, 40));
        rect(i, 8, 2, 1, 14, c(95, 60, 32)); px(i, 10, 9, c(240, 200, 80));
        save(i, "door");

        // Gruta
        i = tile(c(84, 78, 90)); noise(i, c(110, 104, 118), 12); noise(i, c(60, 56, 68), 14); rect(i, 0, 0, S, 1, c(120, 114, 128)); save(i, "rock");
        i = tile(c(62, 56, 70)); noise(i, c(78, 72, 88), 10); noise(i, c(48, 44, 56), 10); save(i, "cave_floor");
        i = canvas(S, S);
        disc(i, 8, 9, 6.5, c(15, 12, 20)); rect(i, 2, 9, 12, 7, c(15, 12, 20));
        for (int x = 2; x < 14; x += 3) px(i, x, 2 + (x % 2), c(60, 56, 68));
        save(i, "cave_mouth");

        // Itens brilhantes
        i = canvas(S, S);
        disc(i, 8, 9, 7, new Color(180, 230, 255, 60));
        rect(i, 7, 3, 2, 3, new Color(150, 210, 220)); rect(i, 6, 6, 4, 8, new Color(150, 210, 220));
        rect(i, 7, 2, 2, 1, c(150, 100, 60)); rect(i, 7, 8, 2, 4, c(240, 230, 200)); px(i, 6, 7, c(255, 255, 255));
        save(i, "garrafa");
        i = canvas(S, S);
        disc(i, 8, 8, 7, new Color(255, 240, 150, 80));
        rect(i, 4, 5, 8, 7, c(240, 225, 185)); px(i, 4, 5, new Color(0, 0, 0, 0)); px(i, 11, 11, new Color(0, 0, 0, 0));
        rect(i, 5, 7, 6, 1, c(150, 110, 70)); rect(i, 5, 9, 4, 1, c(150, 110, 70)); px(i, 10, 6, c(220, 60, 50));
        save(i, "frag");

        // Cais e piso do templo
        i = tile(c(150, 105, 60));
        for (int r = 3; r < S; r += 4) rect(i, 0, r, S, 1, c(105, 72, 40));
        for (int r = 0; r < S; r += 4) { px(i, 2, r + 1, c(70, 50, 30)); px(i, 13, r + 1, c(70, 50, 30)); }
        save(i, "pier");
        i = tile(c(120, 128, 150));
        rect(i, 0, 0, S, 1, c(90, 98, 120)); rect(i, 0, 0, 1, S, c(90, 98, 120));
        rect(i, 7, 5, 2, 6, c(190, 200, 235)); rect(i, 5, 7, 6, 2, c(190, 200, 235));
        save(i, "temple_floor");

        // Mobília e objetos do capítulo 3
        i = canvas(S, S);
        rect(i, 1, 2, 14, 13, c(110, 75, 40)); rect(i, 2, 3, 12, 11, c(235, 235, 240));
        rect(i, 2, 3, 5, 4, c(255, 255, 255)); rect(i, 2, 7, 12, 7, c(200, 60, 60)); rect(i, 2, 9, 12, 1, c(160, 40, 45));
        save(i, "cama");
        i = canvas(S, S);
        rect(i, 8, 2, 1, 9, c(90, 60, 35)); rect(i, 9, 3, 5, 6, c(245, 245, 245)); rect(i, 9, 3, 5, 1, c(210, 210, 220));
        px(i, 8, 1, c(220, 60, 50)); px(i, 9, 1, c(220, 60, 50));
        rect(i, 2, 10, 12, 3, c(140, 90, 50)); rect(i, 3, 13, 10, 1, c(100, 64, 36)); rect(i, 2, 10, 12, 1, c(170, 115, 65));
        save(i, "barco");
        i = canvas(S, S);
        rect(i, 1, 1, 14, 15, c(110, 110, 125)); rect(i, 4, 4, 8, 12, c(70, 70, 85));
        for (int x = 5; x < 12; x += 2) rect(i, x, 5, 1, 11, c(110, 110, 125));
        rect(i, 7, 2, 2, 1, c(255, 225, 100)); px(i, 8, 1, c(255, 225, 100)); px(i, 8, 3, c(255, 225, 100));
        save(i, "portao");
        i = canvas(S, S);
        rect(i, 4, 12, 8, 3, c(130, 130, 140)); rect(i, 6, 6, 4, 6, c(160, 160, 170)); disc(i, 8, 4, 2.4, c(172, 172, 182));
        rect(i, 4, 7, 2, 4, c(150, 150, 160)); rect(i, 10, 7, 2, 4, c(150, 150, 160)); px(i, 8, 3, c(255, 225, 100));
        save(i, "estatua");
        i = canvas(S, S);
        rect(i, 2, 2, 12, 13, c(80, 76, 86)); rect(i, 3, 3, 10, 11, c(120, 115, 125));
        disc(i, 5.5, 6, 1.6, c(240, 200, 60)); disc(i, 8.5, 6, 1.8, c(225, 225, 245)); disc(i, 9.3, 5.6, 1.5, c(120, 115, 125));
        px(i, 11, 6, c(255, 240, 150)); px(i, 10, 6, c(255, 240, 150)); px(i, 12, 6, c(255, 240, 150)); px(i, 11, 5, c(255, 240, 150)); px(i, 11, 7, c(255, 240, 150));
        for (int y = 9; y < 13; y += 2) rect(i, 4, y, 8, 1, c(90, 85, 95));
        save(i, "mural");
        for (int on = 0; on < 2; on++) {
            i = canvas(S, S);
            if (on == 1) disc(i, 8, 8, 7, new Color(255, 160, 60, 70));
            rect(i, 4, 9, 8, 4, c(90, 90, 100)); rect(i, 5, 13, 6, 2, c(70, 70, 80)); rect(i, 5, 8, 6, 1, c(40, 40, 50));
            if (on == 1) { rect(i, 6, 5, 4, 4, c(255, 150, 40)); rect(i, 7, 3, 2, 3, c(255, 200, 60)); px(i, 8, 2, c(255, 240, 150)); }
            save(i, on == 1 ? "brasa_on" : "brasa_off");
        }
        for (int st = 0; st < 2; st++) {
            i = canvas(S, S);
            rect(i, 3, 9, 10, 6, c(150, 150, 165)); rect(i, 2, 8, 12, 2, c(180, 180, 195));
            if (st == 0) {
                disc(i, 8, 5, 6, new Color(150, 235, 255, 70));
                disc(i, 8, 5, 2, c(120, 230, 255));
                for (int[] a : new int[][]{{8, 2}, {5, 4}, {11, 4}, {6, 7}, {10, 7}}) px(i, a[0], a[1], c(160, 240, 255));
                px(i, 8, 4, c(255, 255, 255));
            }
            save(i, st == 0 ? "altar" : "pedestal");
        }
        i = canvas(S, S);
        disc(i, 8, 9, 6, new Color(255, 170, 60, 60));
        rect(i, 4, 12, 8, 2, c(100, 65, 35)); rect(i, 5, 11, 6, 1, c(120, 80, 45));
        rect(i, 7, 6, 3, 6, c(255, 140, 40)); rect(i, 8, 4, 1, 4, c(255, 210, 70));
        save(i, "fogueira");

        // Ícones da caixa de itens
        i = canvas(S, S);                                                      // carta
        rect(i, 2, 4, 12, 9, c(245, 235, 205)); rect(i, 2, 12, 12, 1, c(215, 200, 165));
        for (int k = 0; k < 6; k++) { px(i, 2 + k, 4 + k, c(190, 172, 135)); px(i, 13 - k, 4 + k, c(190, 172, 135)); }
        disc(i, 8, 9.5, 1.7, c(200, 50, 50));
        save(i, "item_carta");
        i = canvas(S, S);                                                      // bilhete
        rect(i, 4, 2, 8, 12, c(238, 222, 170)); px(i, 11, 2, new Color(0, 0, 0, 0)); px(i, 11, 3, c(210, 190, 135)); px(i, 10, 2, c(210, 190, 135));
        for (int y = 5; y < 12; y += 2) rect(i, 5, y, 6, 1, c(150, 110, 70));
        px(i, 10, 11, c(200, 60, 50));
        save(i, "item_bilhete");
        i = canvas(S, S);                                                      // mapa
        rect(i, 2, 3, 12, 10, c(224, 198, 142));
        rect(i, 2, 3, 12, 1, c(168, 132, 80)); rect(i, 2, 12, 12, 1, c(168, 132, 80)); rect(i, 2, 3, 1, 10, c(168, 132, 80)); rect(i, 13, 3, 1, 10, c(168, 132, 80));
        disc(i, 11, 6, 1.9, c(110, 165, 100));
        for (int[] q : new int[][]{{4, 6}, {6, 5}, {8, 5}, {9, 6}}) px(i, q[0], q[1], c(120, 80, 50));
        for (int k = 0; k < 3; k++) { px(i, 4 + k, 9 + k, c(200, 50, 50)); px(i, 6 - k, 9 + k, c(200, 50, 50)); }
        save(i, "item_mapa");
        i = canvas(S, S);                                                      // lanche
        disc(i, 8, 9, 5.2, c(220, 170, 95)); rect(i, 2, 10, 12, 4, new Color(0, 0, 0, 0));
        rect(i, 3, 10, 10, 1, c(250, 215, 80)); rect(i, 3, 11, 10, 2, c(200, 150, 80));
        for (int[] q : new int[][]{{6, 7}, {9, 6}, {10, 8}, {5, 8}}) px(i, q[0], q[1], c(250, 235, 190));
        save(i, "item_lanche");
        i = canvas(S, S);                                                      // bússola
        disc(i, 8, 8, 6.6, c(190, 150, 60)); disc(i, 8, 8, 5.2, c(238, 238, 228));
        for (int k = 3; k <= 7; k++) { px(i, 8, k, c(200, 50, 50)); }
        for (int k = 9; k <= 12; k++) { px(i, 8, k, c(90, 90, 100)); }
        px(i, 8, 8, c(40, 40, 50)); px(i, 3, 8, c(150, 150, 150)); px(i, 13, 8, c(150, 150, 150));
        save(i, "item_bussola");
        i = canvas(S, S);                                                      // estrela do mar
        disc(i, 8, 8, 7.2, new Color(150, 235, 255, 70));
        for (int k = 0; k < 5; k++) {
            double ang = Math.toRadians(-90 + 72 * k);
            for (int rr = 1; rr <= 6; rr++) { int ax = (int) Math.round(8 + Math.cos(ang) * rr), ay = (int) Math.round(8 + Math.sin(ang) * rr);
                px(i, ax, ay, c(120, 230, 255)); if (rr <= 3) { px(i, ax + 1, ay, c(120, 230, 255)); px(i, ax, ay + 1, c(120, 230, 255)); } }
        }
        disc(i, 8, 8, 2.2, c(170, 245, 255)); px(i, 8, 7, c(255, 255, 255));
        save(i, "item_estrela");

        // ---- Capítulo 4: neve, gelo e objetos da montanha ----
        i = tile(c(226, 236, 246)); noise(i, c(205, 220, 236), 10); noise(i, c(246, 250, 255), 8); save(i, "snow");
        i = tile(c(170, 225, 245));
        for (int k = 0; k < 4; k++) { int bx = 2 + k * 4, by = 12 - (k % 2) * 6; for (int t = 0; t < 4; t++) px(i, bx + t, by - t, c(218, 243, 253)); }
        noise(i, c(135, 200, 228), 8); noise(i, c(235, 250, 255), 5);
        save(i, "ice");
        i = canvas(S, S);                                                  // pinheiro nevado
        rect(i, 7, 12, 2, 4, c(90, 60, 35));
        int[][] tiers = {{1, 5, 3}, {4, 5, 4}, {7, 6, 5}};
        for (int[] tr : tiers) for (int r = 0; r < tr[1]; r++) {
            int hw = 1 + r * (tr[2] - 1) / (tr[1] - 1);
            rect(i, 8 - hw, tr[0] + r, 2 * hw, 1, c(28, 92, 62));
            if (r < 2) rect(i, 8 - hw, tr[0] + r, 2 * hw, 1, c(232, 242, 250));
            px(i, 8 - hw, tr[0] + r, c(20, 70, 48)); px(i, 7 + hw, tr[0] + r, c(20, 70, 48));
        }
        save(i, "pine");

        i = canvas(S, S);                                                  // bloco de gelo
        rect(i, 2, 3, 12, 11, c(120, 185, 225)); rect(i, 3, 4, 10, 9, c(170, 228, 250));
        rect(i, 4, 5, 3, 2, c(235, 250, 255)); rect(i, 2, 13, 12, 1, c(90, 150, 195));
        px(i, 9, 7, c(120, 185, 225)); px(i, 10, 8, c(120, 185, 225)); px(i, 10, 9, c(120, 185, 225)); px(i, 11, 10, c(120, 185, 225));
        save(i, "gelo");
        for (int on = 0; on < 2; on++) {                                   // fogão
            i = canvas(S, S);
            if (on == 1) disc(i, 8, 10, 7, new Color(255, 150, 50, 60));
            rect(i, 10, 0, 3, 4, c(60, 60, 70)); rect(i, 3, 4, 10, 11, c(75, 75, 85)); rect(i, 2, 3, 12, 2, c(98, 98, 108));
            rect(i, 5, 8, 6, 5, c(25, 25, 35));
            if (on == 1) { rect(i, 6, 10, 4, 3, c(255, 150, 40)); rect(i, 7, 9, 2, 3, c(255, 210, 70)); }
            save(i, on == 1 ? "fogao_on" : "fogao_off");
        }
        i = canvas(S, S);                                                  // trenó
        rect(i, 2, 12, 12, 1, c(120, 80, 45)); px(i, 1, 11, c(120, 80, 45)); px(i, 14, 11, c(120, 80, 45)); px(i, 1, 10, c(120, 80, 45));
        rect(i, 3, 8, 10, 4, c(165, 112, 62)); for (int x = 5; x < 13; x += 3) rect(i, x, 8, 1, 4, c(120, 80, 45));
        rect(i, 3, 8, 10, 1, c(190, 140, 85)); px(i, 2, 9, c(200, 60, 60)); px(i, 1, 10, c(200, 60, 60));
        save(i, "treno");
        i = canvas(S, S);                                                  // Cintila (a estrela cadente)
        disc(i, 8, 8, 7.5, new Color(255, 240, 150, 80));
        for (int k = 0; k < 5; k++) {
            double ang = Math.toRadians(-90 + 72 * k);
            for (int rr = 1; rr <= 6; rr++) { int ax = (int) Math.round(8 + Math.cos(ang) * rr), ay = (int) Math.round(8 + Math.sin(ang) * rr);
                px(i, ax, ay, c(255, 225, 100)); if (rr <= 4) { px(i, ax + 1, ay, c(255, 225, 100)); px(i, ax, ay + 1, c(255, 225, 100)); } }
        }
        disc(i, 8, 8, 3.4, c(255, 236, 130));
        px(i, 7, 7, c(60, 40, 30)); px(i, 9, 7, c(60, 40, 30)); px(i, 6, 9, c(255, 160, 150)); px(i, 10, 9, c(255, 160, 150)); px(i, 8, 9, c(60, 40, 30));
        px(i, 7, 6, c(255, 255, 255));
        save(i, "cintila");
        i = canvas(S, S);                                                  // placa
        rect(i, 7, 7, 2, 8, c(110, 75, 40)); rect(i, 2, 2, 12, 6, c(190, 150, 95)); rect(i, 2, 2, 12, 1, c(140, 100, 60)); rect(i, 2, 7, 12, 1, c(140, 100, 60));
        rect(i, 4, 4, 8, 1, c(100, 70, 40)); rect(i, 4, 6, 5, 1, c(100, 70, 40));
        save(i, "placa");
        i = canvas(S, S);                                                  // porta do farol
        rect(i, 4, 2, 8, 13, c(40, 50, 90)); rect(i, 5, 3, 6, 11, c(70, 85, 140));
        px(i, 10, 9, c(240, 200, 80)); px(i, 8, 5, c(255, 225, 100)); px(i, 7, 6, c(255, 225, 100)); px(i, 9, 6, c(255, 225, 100)); px(i, 8, 7, c(255, 225, 100));
        for (int y = 4; y < 14; y += 3) { px(i, 5, y, c(40, 50, 90)); px(i, 10, y, c(40, 50, 90)); }
        save(i, "porta_farol");

        // ícones: lanterna e cristal
        i = canvas(S, S);
        rect(i, 6, 1, 4, 1, c(150, 115, 40)); px(i, 5, 2, c(150, 115, 40)); px(i, 10, 2, c(150, 115, 40)); rect(i, 5, 3, 6, 1, c(190, 150, 60));
        rect(i, 5, 4, 6, 9, c(190, 150, 60)); rect(i, 6, 5, 4, 7, c(255, 235, 140)); rect(i, 7, 6, 2, 4, c(255, 190, 70)); px(i, 8, 6, c(255, 250, 200));
        rect(i, 5, 13, 6, 1, c(150, 115, 40)); rect(i, 4, 14, 8, 1, c(120, 90, 30));
        save(i, "item_lanterna");
        i = canvas(S, S);
        disc(i, 8, 8, 7, new Color(255, 140, 60, 70));
        int[] wd = {1, 2, 3, 4, 4, 4, 4, 3, 3, 2, 2, 1};
        for (int k = 0; k < wd.length; k++) rect(i, 8 - wd[k], 2 + k, wd[k] * 2, 1, k < 6 ? c(255, 125, 55) : c(220, 85, 45));
        rect(i, 6, 4, 2, 3, c(255, 205, 130)); px(i, 6, 3, c(255, 235, 190));
        save(i, "item_cristal");

        // Farol: variantes esquerda (_l), meio e direita (_r) para dar volume
        Color[] stripe = {c(205, 60, 55), c(245, 245, 245)};
        String[] nm = {"lh_red", "lh_white"};
        for (int k = 0; k < 2; k++) {
            for (String side : new String[]{"_l", "", "_r"}) {
                i = tile(stripe[k]);
                Color hi = k == 0 ? c(228, 92, 86) : c(255, 255, 255), sh = k == 0 ? c(165, 40, 40) : c(205, 205, 218);
                Color edge = c(60, 50, 60);
                if (side.equals("_l")) { rect(i, 0, 0, 1, S, edge); rect(i, 3, 0, 2, S, hi); }
                if (side.equals("")) { rect(i, 11, 0, 5, S, sh); rect(i, 3, 0, 2, S, hi); }
                if (side.equals("_r")) { rect(i, 15, 0, 1, S, edge); rect(i, 8, 0, 8, S, sh); }
                save(i, nm[k] + side);
            }
        }
        for (String side : new String[]{"_l", "", "_r"}) {
            boolean mid = side.equals("");
            i = tile(c(60, 60, 75)); rect(i, 0, 0, S, 2, c(90, 90, 110));
            if (side.equals("_l")) rect(i, 0, 0, 1, S, c(40, 40, 50));
            if (side.equals("_r")) rect(i, 15, 0, 1, S, c(40, 40, 50));
            if (mid) { disc(i, 8, 8, 5.5, c(255, 235, 130)); disc(i, 8, 8, 3, c(255, 250, 200)); }
            save(i, mid ? "lh_lamp" : "lh_top" + side);
        }
    }

    // ---------- Personagens: Characters.java (moldes 16x16 com contorno, rosto e caminhada) ----------
    static void characters() throws Exception {
        for (var e : Characters.all().entrySet()) save(Characters.sheet(e.getValue()), e.getKey());   // veja Characters.java

        BufferedImage i = canvas(S, S);
        disc(i, 8, 8, 7, new Color(255, 230, 120, 90));
        disc(i, 8, 9, 4.6, c(255, 215, 80));
        for (int x : new int[]{6, 8, 10}) rect(i, x, 7, 1, 5, c(220, 170, 40));
        px(i, 6, 7, c(255, 255, 255)); px(i, 7, 6, c(255, 245, 180));
        save(i, "concha");
    }

    public static void main(String[] args) throws Exception {
        new File("assets").mkdirs();
        tiles();
        Tiles.generate(MakeAssets::save);        // tiles principais redesenhados (substituem os de tiles())
        characters();
        System.out.println("Sprites gerados em assets/");
    }
}
