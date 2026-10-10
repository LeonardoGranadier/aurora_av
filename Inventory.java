import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/** Caixa de itens (tecla I). O estado de cada item é derivado do progresso da história e das atividades. */
final class Inventory {
    final Game game;
    int sel, returnState = Game.PLAY;
    Inventory(Game game) { this.game = game; }

    void open(int from) { returnState = from; sel = 0; game.state = Game.INV; }

    /** Grade de 4 colunas. Z, ESC ou I fecham. */
    void key(int k) {
        boolean act = k == KeyEvent.VK_Z || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE;
        int cnt = KEYS.length, antes = sel;
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) sel = (sel + cnt - 1) % cnt;
        else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) sel = (sel + 1) % cnt;
        else if ((k == KeyEvent.VK_UP || k == KeyEvent.VK_W) && sel >= 4) sel -= 4;
        else if ((k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) && sel + 4 < cnt) sel += 4;
        else if (act || k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_I) { game.audio.sfx("back"); game.state = returnState; return; }
        if (sel != antes) game.audio.sfx("select");
    }

    static final String[] KEYS = {"carta", "concha", "bilhete", "mapa", "lanche", "bussola", "estrela", "lanterna", "cristal", "vara", "trofeu", "sinos"};
    static final String[] NAMES = {"Carta da Mamãe", "Concha Brilhante", "Bilhete da Capitã", "Mapa Estrelado",
                                        "Lanche da Mamãe", "Bússola do Seu Marinho", "Estrela do Mar", "Lanterna do Professor", "Cristal de Calor", "Vara do Tio Beto", "Troféu de Pesca", "Medalha dos Sinos"};
    static final Color[] COLORS = {new Color(240, 230, 200), new Color(255, 215, 80), new Color(235, 220, 170),
        new Color(224, 198, 142), new Color(220, 170, 95), new Color(190, 150, 60), new Color(120, 230, 255),
        new Color(255, 215, 120), new Color(255, 125, 55), new Color(80, 190, 150), new Color(245, 200, 65), new Color(140, 210, 245)};

    /** 0 = ainda não encontrado, 1 = em mãos, 2 = já usado/entregue. */
    int state(String k) {
        return switch (k) {
            case "vara" -> game.life.rod ? 1 : 0;
            case "trofeu" -> game.life.completeAlbum() ? 1 : 0;
            case "sinos" -> game.life.bellBest >= VillageLife.BELL_ROUNDS ? 1 : 0;
            case "carta" -> game.stage < 1 ? 0 : game.stage == 1 ? 1 : 2;
            case "concha" -> game.stage < 3 ? 0 : game.stage == 3 ? 1 : 2;
            case "bilhete" -> game.stage >= 5 ? 1 : 0;
            case "mapa" -> game.frags != 0 || game.stage >= 7 ? 1 : 0;
            case "lanche" -> (game.bye & 1) == 0 ? 0 : game.stage >= 11 ? 2 : 1;
            case "bussola" -> (game.bye & 2) != 0 ? 1 : 0;
            case "estrela" -> game.stage < 14 ? 0 : game.stage == 14 ? 1 : 2;
            case "lanterna" -> game.stage >= 16 ? 1 : 0;
            case "cristal" -> game.stage < 18 ? 0 : game.stage == 18 ? 1 : 2;
            default -> 0;
        };
    }

    String desc(String k, int st) {
        return switch (k) {
            case "vara" -> "Uma vara feita pelo Tio Beto, com linha forte e boia vermelha.";
            case "trofeu" -> "As oito espécies de Aurora, registradas no seu álbum. Uma conquista de mestre pescador!";
            case "sinos" -> "As cinco melodias da praça ecoaram sem um erro. Lucas vai querer uma revanche!";
            case "carta" -> st == 1 ? "Uma carta selada, com a letra caprichada da Mamãe. É para o Seu Marinho, na Praia Azul."
                                    : "Entregue ao Seu Marinho. Foi o começo de tudo isso.";
            case "concha" -> st == 1 ? "O coração do farol. Brilha com uma luz dourada e quentinha. Leve ao Seu Marinho."
                                     : "Encaixada no farol. Desde então, ele ilumina a baía toda noite.";
            case "bilhete" -> "Encontrado numa garrafa trazida pela maré: a Capitã Estrela pede ajuda e diz estar na Gruta do Eco.";
            case "mapa" -> game.stage >= 7 ? "O Mapa Estrelado completo. Aponta para a Ilha das Estrelas, onde repousa a lendária Estrela do Mar."
                                      : "Pedaços do mapa do navio da Capitã (" + Integer.bitCount(game.frags) + "/3). Um brilha à noite, outro aparece de dia e o último está na gruta.";
            case "lanche" -> st == 1 ? "Pão com queijo, ainda quentinho. Tem cheiro de casa."
                                     : "Você comeu na viagem. Estava delicioso!";
            case "bussola" -> "Uma bússola velha de latão, presente do Seu Marinho. A agulha sempre aponta para casa.";
            case "estrela" -> st == 1 ? "Uma estrela-do-mar de cristal que pulsa com luz própria. Mostre ao Seu Marinho, à noite."
                                      : "Brilha no topo do farol, ao lado da Concha. A baía nunca esteve tão linda.";
            case "lanterna" -> "Lanterna de latão que o Prof. Carvalho herdou do avô. Ilumina mais longe nas cavernas e nos templos.";
            case "cristal" -> st == 1 ? "O Cristal de Calor do Eremita: quentinho como uma xícara de chocolate. Derrete qualquer gelo."
                                      : "Derreteu o portão de gelo e abriu a trilha até o Pico Estrelado.";
            default -> "";
        };
    }

    Set<String> found() {
        Set<String> r = new HashSet<>();
        for (String k : KEYS) if (state(k) > 0) r.add(k);
        return r;
    }

    void announceNew(Set<String> antes) {
        List<String> novos = new ArrayList<>();
        for (int i = 0; i < KEYS.length; i++)
            if (state(KEYS[i]) > 0 && !antes.contains(KEYS[i])) {
                if (novos.isEmpty()) game.itemGet(KEYS[i]);
                novos.add(NAMES[i]);
            }
        if (!novos.isEmpty()) game.pendingToast = "Novo item: " + String.join(", ", novos) + "  (I)";
    }

    void drawIcon(Graphics2D g, int idx, int x, int y, int size, boolean dim) {
        String k = KEYS[idx];
        BufferedImage im = game.spr.get(k.equals("concha") ? "concha" : "item_" + k);
        Composite old = g.getComposite();
        if (dim) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        if (game.life.drawItem(g, k, x, y, size)) { g.setComposite(old); return; }
        if (im != null) g.drawImage(im, x, y, size, size, null);
        else {                                                         // sem PNG: quadrado colorido com a inicial
            g.setColor(COLORS[idx]); g.fillRoundRect(x + 4, y + 4, size - 8, size - 8, 10, 10);
            g.setColor(new Color(40, 40, 60)); g.setFont(new Font("SansSerif", Font.BOLD, size / 2));
            g.drawString(Lang.t(NAMES[idx].substring(0, 1)), x + size / 2 - size / 8, y + size / 2 + size / 6);
        }
        g.setComposite(old);
    }

    void drawMapPieces(Graphics2D g, int x, int y, int w, int h) {
        boolean full = game.stage >= 7;
        int pw = w / 3;
        for (int i = 0; i < 3; i++) {
            boolean got = full || (game.frags & (1 << i)) != 0;
            int bx = x + i * pw;
            if (got) {
                g.setColor(new Color(224, 198, 142)); g.fillRect(bx, y, pw, h);
                g.setColor(new Color(168, 132, 80)); g.drawRect(bx, y, pw, h);
            } else {
                g.setColor(new Color(40, 46, 100)); g.fillRect(bx, y, pw, h);
                g.setColor(new Color(110, 120, 190));
                g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 4}, 0));
                g.drawRect(bx, y, pw, h);
                g.setStroke(new BasicStroke(1));
                g.setFont(new Font("SansSerif", Font.BOLD, 16)); g.drawString(Lang.t("?"), bx + pw / 2 - 5, y + h / 2 + 6);
            }
        }
        if (full) {
            g.setColor(new Color(110, 165, 100)); g.fillOval(x + w - 58, y + 8, 44, 30);
            g.setColor(new Color(200, 50, 50)); g.setStroke(new BasicStroke(3));
            g.drawLine(x + w - 44, y + 17, x + w - 30, y + 29); g.drawLine(x + w - 30, y + 17, x + w - 44, y + 29);
            g.setStroke(new BasicStroke(1));
            g.setColor(new Color(120, 80, 50));
            for (int k = 0; k < 8; k++) g.fillOval(x + 10 + k * ((w - 80) / 8), y + h - 16 - (int) (Math.sin(k / 7.0 * Math.PI) * 18), 3, 3);
            g.setFont(new Font("SansSerif", Font.PLAIN, 10));
            String ilha = "Ilha das Estrelas";
            g.drawString(Lang.t(ilha), x + w - 6 - g.getFontMetrics().stringWidth(Lang.t(ilha)), y + 50);
        }
    }

    void draw(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, Game.W, Game.H);
        int px = 40, py = 14, pw = Game.W - 80, ph = Game.H - 28;
        g.setColor(new Color(22, 26, 64, 240)); g.fillRoundRect(px, py, pw, ph, 20, 20);
        g.setColor(new Color(255, 215, 120)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(px, py, pw, ph, 20, 20); g.setStroke(new BasicStroke(1));
        g.setFont(new Font("SansSerif", Font.BOLD, 20)); g.setColor(new Color(255, 225, 140)); g.drawString(Lang.t("Itens"), px + 24, py + 34);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(255, 255, 255, 180));
        g.drawString(Lang.t(found().size() + " / " + KEYS.length + " encontrados"), px + pw - 150, py + 32);

        int cell = 64, gap = 10, gx = px + (pw - (4 * cell + 3 * gap)) / 2, gy = py + 50;
        for (int i = 0; i < 12; i++) {                                    // grade 4 x 3
            int x = gx + (i % 4) * (cell + gap), y = gy + (i / 4) * (cell + gap);
            boolean selected = i == sel, real = i < KEYS.length;
            g.setColor(real ? new Color(40, 46, 104) : new Color(30, 34, 76)); g.fillRoundRect(x, y, cell, cell, 12, 12);
            if (real) {
                int st = state(KEYS[i]);
                if (st == 0) { g.setColor(new Color(90, 98, 150)); g.setFont(new Font("SansSerif", Font.BOLD, 28)); g.drawString(Lang.t("?"), x + cell / 2 - 8, y + cell / 2 + 10); }
                else {
                    drawIcon(g, i, x + 10, y + 10, 44, st == 2);
                    if (st == 2) { g.setColor(new Color(110, 230, 140)); g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.drawString(Lang.t("✓"), x + cell - 19, y + cell - 7); }
                }
            }
            g.setColor(selected ? new Color(255, 215, 120) : new Color(80, 90, 150)); g.setStroke(new BasicStroke(selected ? 3 : 1));
            g.drawRoundRect(x, y, cell, cell, 12, 12); g.setStroke(new BasicStroke(1));
        }

        int dy = gy + 3 * cell + 2 * gap + 14, dh = py + ph - 18 - dy;
        g.setColor(new Color(30, 36, 86)); g.fillRoundRect(px + 20, dy, pw - 40, dh, 14, 14);
        if (sel < KEYS.length) {
            String k = KEYS[sel]; int st = state(k);
            if (st == 0) {
                g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.setColor(new Color(160, 170, 220)); g.drawString(Lang.t("???"), px + 44, dy + 36);
                g.setFont(new Font("SansSerif", Font.PLAIN, 14)); g.setColor(new Color(255, 255, 255, 200));
                g.drawString(Lang.t("Você ainda não encontrou este item. Continue explorando!"), px + 44, dy + 66);
            } else {
                drawIcon(g, sel, px + 36, dy + 18, 64, st == 2);
                g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.setColor(new Color(255, 225, 140)); g.drawString(Lang.t(NAMES[sel]), px + 120, dy + 34);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.setColor(st == 2 ? new Color(110, 230, 140) : new Color(150, 220, 255));
                g.drawString(Lang.t(st == 2 ? "Usado / entregue" : k.equals("mapa") && game.stage < 7 ? "Em mãos (incompleto)" : "Em mãos"), px + 120, dy + 52);
                boolean mapa = k.equals("mapa");
                g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(255, 255, 255, 230));
                FontMetrics fm = g.getFontMetrics();
                int ty = dy + 76, width = mapa ? pw - 184 - 120 - 24 : pw - 36 - 120, linhas = 0;
                for (String line : game.wrap(desc(k, st), fm, width)) { if (linhas++ >= 4) break; g.drawString(Lang.t(line), px + 120, ty); ty += 18; }
                if (mapa) drawMapPieces(g, px + pw - 184, dy + 22, 150, 80);
            }
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 11)); g.setColor(new Color(255, 255, 255, 140));
        g.drawString(Lang.t("Setas: escolher   |   Z / ESC / I: fechar"), px + 24, py + ph - 4);
    }
}
