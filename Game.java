import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.List;

/**
 * Jogo de exploração estilo Pokémon FireRed (sem captura).
 * Requer Java 17+.  Compilar:  javac Game.java   Rodar:  java Game
 *
 * Controles: setas/WASD = mover | Z, Enter ou Espaço = falar/avançar | ESC = menu/salvar | T = avançar 2h
 */
public class Game extends JPanel implements KeyListener {

    // ---------- Constantes ----------
    static final int T = 32, VW = 20, VH = 15, W = VW * T, H = VH * T;
    static final int[] DX = {0, 0, -1, 1}, DY = {1, -1, 0, 0}; // baixo, cima, esquerda, direita
    static final int[] OPP = {1, 0, 3, 2};
    static final int TITLE = 0, PLAY = 1, MENU = 2, CONFIRM = 3, INV = 4;   // estados da tela
    static final int FISHING = 5, ALBUM = 6, BELLS = 7;
    static final int BONDS = 8;
    static final int FESTIVAL = 9;
    static final int HOME = 10;
    static final int WEATHER = 11;
    static final int WORLD_MAP = 12;
    static final int JOURNAL = 13;
    static final int SOUND = 14;
    static final int GARDENING = 15;
    static final int ORDERS = 16;
    static final String SAVE = "savegame.properties";
    static final String[] OBJETIVOS = {
        "Fale com a mamãe aqui em casa.",
        "Leve a carta ao Seu Marinho (Praia Azul, ao sul da vila).",
        "Ache a Concha Brilhante (Floresta Verde, a leste da vila).",
        "Leve a concha de volta ao Seu Marinho.",
        "Volte à Praia Azul depois que anoitecer (durma na cama de casa).",
        "Encontre a Capitã Estrela na Gruta do Eco (norte da floresta).",
        "Ache os 3 pedaços do mapa: vila à noite, praia de dia, gruta.",
        "Leve o Mapa Estrelado completo à Capitã Estrela.",
        "Encontre a Capitã no cais da Praia Azul (de dia).",
        "Despeça-se da mamãe e do Seu Marinho antes de zarpar.",
        "Volte ao cais e zarpe com a Capitã.",
        "Ache o Templo Estrelado: o portão só abre à noite (fogueira).",
        "Acenda os 3 braseiros na ordem do mural.",
        "O altar se revelou! Pegue a Estrela do Mar.",
        "Volte de barco e mostre a Estrela ao Seu Marinho, à noite.",
        "Fale com o Prof. Carvalho na vila: ele viu algo no céu.",
        "Suba a Montanha Névoa (saída norte da vila) e ache o Eremita.",
        "Acenda o fogão do Eremita com a Lanterna.",
        "Derreta o portão de gelo da trilha com o Cristal de Calor.",
        "Suba ao Pico Estrelado: algo brilha na cratera.",
        "Leve a Cintila ao farol, à noite (o trenó desce a montanha).",
        "Volte à porta do farol à noite: Cintila tem uma mensagem.",
        "Mostre a mensagem de Cintila ao Prof. Carvalho.",
        "Busque 3 sementes na Trilha dos Vagalumes, a leste da floresta.",
        "Abra o portão ao norte da Trilha dos Vagalumes.",
        "Alinhe as pedras do jardim e visite a fonte à noite.",
        "Fale com Cintila junto à fonte do jardim.",
        "Leve a Flor-Constelação à Mamãe, em casa.",
        "Capítulo 5 concluído! O jardim voltou a florescer."
    };

    // ---------- Dados do mundo ----------
    record Warp(int x, int y, String to, int tx, int ty) {}

    static class Npc {
        String id, name, sprite, when;           // when: "dia", "noite" ou null (sempre)
        int x, y, dir = 0, minStage = 0, maxStage = 99;
        Color color; boolean object;             // object = item brilhante (sem corpo)
        Npc(String id, String name, int x, int y, Color color) {
            this.id = id; this.name = name; this.x = x; this.y = y; this.color = color; this.sprite = id;
        }
        boolean still;                           // objeto parado (sem flutuar), ex.: cama, portão
        boolean walking, sleeping;
        int toX, toY;
        double walkProgress, routeWait;
        Npc obj(String spriteKey) { object = true; sprite = spriteKey; return this; }
        Npc still() { still = true; return this; }
        Npc spr(String spriteKey) { sprite = spriteKey; return this; }
        Npc when(String w) { when = w; return this; }
        Npc stages(int min, int max) { minStage = min; maxStage = max; return this; }
    }

    static class GameMap {
        String name; int w, h; char[][] t; boolean outdoor, dark, snowy; char clear = '.';
        List<Warp> warps = new ArrayList<>();
        List<Npc> npcs = new ArrayList<>();

        GameMap(String name, int w, int h, char fill, boolean outdoor) {
            this.name = name; this.w = w; this.h = h; this.outdoor = outdoor;
            t = new char[h][w];
            for (char[] row : t) Arrays.fill(row, fill);
        }
        void rect(int x, int y, int rw, int rh, char c) {
            for (int j = y; j < y + rh; j++)
                for (int i = x; i < x + rw; i++)
                    if (i >= 0 && j >= 0 && i < w && j < h) t[j][i] = c;
        }
        void border(char c) { rect(0, 0, w, 1, c); rect(0, h - 1, w, 1, c); rect(0, 0, 1, h, c); rect(w - 1, 0, 1, h, c); }
        void house(int x, int y, boolean door) { rect(x, y, 5, 3, 'H'); if (door) t[y + 2][x + 2] = 'D'; }
        char at(int x, int y) { return (x < 0 || y < 0 || x >= w || y >= h) ? '#' : t[y][x]; }
        boolean solid(int x, int y) { return "#~HLXR".indexOf(at(x, y)) >= 0; }
        void addNpc(Npc n) { npcs.add(n); if (t[n.y][n.x] == '#') t[n.y][n.x] = clear; }
    }

    // ---------- Estado do jogo ----------
    final Map<String, GameMap> maps = new HashMap<>();
    GameMap map;
    int tx, ty, nx, ny, dir = 0, stage = 0, frags = 0, bye = 0, brasas = 0;   // bye: despedidas | brasas: braseiros acesos
    Runnable afterDlg;     // ação executada quando o diálogo termina (ex.: zarpar)
    String banner = ""; double bannerT;
    int state = TITLE, menuSel;
    final List<String> opts = new ArrayList<>();
    final Set<String> taken = new HashSet<>();     // itens já coletados (somem do mapa)
    String saveInfo, lastSig = "", toastTxt = ""; double toastT;
    final Audio audio = new Audio();
    String curMusic = "", pendingToast;      // pendingToast: aviso de "Novo item" mostrado quando o diálogo termina
    int invSel, invReturn = PLAY;
    double prog, time = 8 * 60, fade = 1;     // time = minutos de jogo
    boolean moving, sliding;     // sliding: deslizando no gelo
    int fx, fy;                  // casa onde está a Cintila (seguidora), uma atrás do jogador
    long frame, last = System.nanoTime();
    String[] dlg; int dlgIdx; String speaker;
    final Set<Integer> down = new HashSet<>();
    final Map<String, BufferedImage> spr = new HashMap<>();   // sprites carregados de assets/
    final Bonds bonds = new Bonds(this);
    final VillageLife life = new VillageLife(this);
    final Festival festival = new Festival(this);
    final HomeDecor home = new HomeDecor(this);
    final Weather weather = new Weather(this);
    final ChapterFive chapter = new ChapterFive(this);
    final WorldMap worldMap = new WorldMap(this);
    final AdventureJournal journal = new AdventureJournal(this);
    final Soundscape soundscape = new Soundscape(this);
    final Gardening gardening = new Gardening(this);
    final VillageOrders orders = new VillageOrders(this);

    public Game() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { down.clear(); }
        });
        loadSprites();
        newGame();
        openTitle();
        new javax.swing.Timer(16, e -> { update(); repaint(); }).start();
    }

    // ---------- Construção dos mapas ----------
    void buildMaps() {
        Random r = new Random(7);

        // Vila Aurora
        GameMap v = new GameMap("Vila Aurora", 30, 22, '.', true);
        for (int i = 0; i < 45; i++) v.t[r.nextInt(22)][r.nextInt(30)] = r.nextInt(3) == 0 ? '#' : 'F';
        v.border('#');
        v.house(4, 4, true);
        v.house(16, 4, false);
        v.rect(6, 7, 1, 5, '=');
        v.rect(6, 10, 24, 2, '=');
        v.rect(14, 10, 2, 12, '=');
        v.rect(14, 0, 2, 10, '=');                                  // trilha da montanha
        v.rect(20, 14, 6, 4, '~');
        v.warps.add(new Warp(6, 6, "Casa", 5, 6));
        v.warps.add(new Warp(29, 10, "Floresta Verde", 1, 10));
        v.warps.add(new Warp(29, 11, "Floresta Verde", 1, 11));
        v.warps.add(new Warp(14, 21, "Praia Azul", 14, 1));
        v.warps.add(new Warp(15, 21, "Praia Azul", 15, 1));
        v.addNpc(new Npc("prof", "Prof. Carvalho", 10, 8, new Color(240, 240, 240)));
        v.addNpc(new Npc("placa", "", 14, 1, new Color(190, 150, 95)).obj("placa").still().stages(0, 15));    // trilha fechada até o cap. 4
        v.addNpc(new Npc("placa", "", 15, 1, new Color(190, 150, 95)).obj("placa").still().stages(0, 15));
        v.warps.add(new Warp(14, 0, "Trilha da Montanha", 10, 30));
        v.warps.add(new Warp(15, 0, "Trilha da Montanha", 11, 30));
        v.addNpc(new Npc("frag1", "", 19, 15, new Color(255, 240, 150)).obj("frag").when("noite").stages(6, 6));
        maps.put(v.name, v);
        life.buildVillage(v);

        // Floresta Verde
        GameMap f = new GameMap("Floresta Verde", 30, 22, '.', true);
        for (int y = 0; y < 22; y++) for (int x = 0; x < 30; x++) if (r.nextInt(100) < 38) f.t[y][x] = '#';
        f.rect(5, 3, 5, 3, '~');
        f.rect(21, 2, 7, 4, '.');
        f.border('#');
        f.rect(0, 10, 26, 2, '=');
        f.rect(24, 4, 2, 8, '=');
        f.rect(12, 0, 6, 3, 'R'); f.t[2][14] = 'O'; f.t[2][15] = 'O';   // entrada da gruta
        f.rect(14, 3, 2, 7, '=');
        f.warps.add(new Warp(0, 10, "Vila Aurora", 28, 10));
        f.warps.add(new Warp(0, 11, "Vila Aurora", 28, 11));
        f.warps.add(new Warp(14, 2, "Gruta do Eco", 11, 14));
        f.warps.add(new Warp(15, 2, "Gruta do Eco", 12, 14));
        f.addNpc(new Npc("lenhador", "Lenhador", 10, 9, new Color(170, 60, 40)));
        f.addNpc(new Npc("concha", "", 24, 4, new Color(255, 215, 80)).obj("concha"));
        maps.put(f.name, f);

        // Praia Azul
        GameMap b = new GameMap("Praia Azul", 30, 22, 's', true);
        for (int i = 0; i < 14; i++) b.t[2 + r.nextInt(11)][1 + r.nextInt(28)] = '#';
        b.border('#');
        b.rect(0, 14, 30, 8, '~');
        b.rect(14, 0, 2, 1, '=');
        b.rect(21, 3, 3, 5, 'L');
        b.rect(5, 13, 2, 5, 'P');                                   // cais
        b.warps.add(new Warp(14, 0, "Vila Aurora", 14, 20));
        b.warps.add(new Warp(15, 0, "Vila Aurora", 15, 20));
        b.addNpc(new Npc("marinho", "Seu Marinho", 17, 9, new Color(40, 90, 160)));
        b.addNpc(new Npc("frag2", "", 8, 10, new Color(255, 240, 150)).obj("frag").when("dia").stages(6, 6));
        b.addNpc(new Npc("garrafa", "", 12, 12, new Color(170, 225, 240)).obj("garrafa").when("noite").stages(4, 4));
        b.addNpc(new Npc("barco", "", 5, 18, new Color(140, 90, 50)).obj("barco").still().stages(8, 99));
        b.addNpc(new Npc("capita_cais", "Capitã Estrela", 6, 15, new Color(40, 140, 150)).spr("capita").stages(8, 10).when("dia"));
        b.addNpc(new Npc("porta_farol", "", 22, 8, new Color(70, 85, 140)).obj("porta_farol").still());
        b.addNpc(new Npc("capita_fim", "Capitã Estrela", 6, 15, new Color(40, 140, 150)).spr("capita").stages(14, 99));
        maps.put(b.name, b);

        // Casa do jogador
        GameMap c = new GameMap("Casa", 10, 8, 'f', false);
        c.border('X');
        c.rect(7, 1, 2, 2, 'X');
        c.t[7][5] = 'D';
        c.warps.add(new Warp(5, 7, "Vila Aurora", 6, 7));
        c.addNpc(new Npc("mae", "Mamãe", 3, 3, new Color(200, 90, 150)));
        c.addNpc(new Npc("cama", "", 1, 1, new Color(200, 60, 60)).obj("cama").still());
        maps.put(c.name, c);

        // Gruta do Eco (escura)
        GameMap g = new GameMap("Gruta do Eco", 24, 16, 'R', false);
        g.dark = true;
        g.rect(9, 11, 6, 4, 'c');   // entrada
        g.rect(11, 7, 2, 4, 'c');   // corredor
        g.rect(6, 2, 12, 5, 'c');   // salão principal
        g.rect(18, 4, 1, 2, 'c');   // passagem
        g.rect(19, 2, 3, 5, 'c');   // câmara do fundo
        g.t[12][10] = 'R'; g.t[4][8] = 'R'; g.t[5][15] = 'R'; g.t[3][20] = 'R';
        g.t[15][11] = 'O'; g.t[15][12] = 'O';
        g.warps.add(new Warp(11, 15, "Floresta Verde", 14, 3));
        g.warps.add(new Warp(12, 15, "Floresta Verde", 15, 3));
        g.addNpc(new Npc("capita", "Capitã Estrela", 11, 4, new Color(40, 140, 150)).stages(0, 7));
        g.addNpc(new Npc("frag3", "", 21, 3, new Color(255, 240, 150)).obj("frag").stages(6, 6));
        maps.put(g.name, g);

        // Ilha das Estrelas
        GameMap il = new GameMap("Ilha das Estrelas", 28, 20, '~', true);
        il.rect(2, 3, 24, 14, 's');
        il.rect(5, 5, 18, 10, '.');
        for (int k = 0; k < 40; k++) {
            int x = 5 + r.nextInt(18), y = 5 + r.nextInt(10);
            if (il.t[y][x] == '.') il.t[y][x] = r.nextInt(3) == 0 ? '#' : 'F';
        }
        il.rect(17, 2, 8, 4, 'R'); il.t[5][21] = 'O'; il.t[5][22] = 'O';      // fachada do templo
        il.rect(4, 10, 19, 1, '=');
        il.rect(21, 6, 2, 5, '=');
        il.rect(1, 10, 3, 1, 'P');                                             // cais da ilha
        il.warps.add(new Warp(21, 5, "Templo Estrelado", 8, 14));
        il.warps.add(new Warp(22, 5, "Templo Estrelado", 9, 14));
        il.addNpc(new Npc("capita_ilha", "Capitã Estrela", 6, 9, new Color(40, 140, 150)).spr("capita").stages(11, 14));
        il.addNpc(new Npc("barco_ilha", "", 0, 10, new Color(140, 90, 50)).obj("barco").still());
        il.addNpc(new Npc("fogueira", "", 7, 12, new Color(255, 140, 40)).obj("fogueira").still());
        il.addNpc(new Npc("estatua", "", 19, 7, new Color(160, 160, 170)).obj("estatua").still());
        il.addNpc(new Npc("portao", "", 21, 6, new Color(110, 110, 125)).obj("portao").still().when("dia"));   // some à noite
        il.addNpc(new Npc("portao", "", 22, 6, new Color(110, 110, 125)).obj("portao").still().when("dia"));
        maps.put(il.name, il);

        // Templo Estrelado (escuro)
        GameMap tp = new GameMap("Templo Estrelado", 18, 16, 'R', false);
        tp.dark = true;
        tp.rect(7, 12, 4, 3, 'm');   // entrada
        tp.rect(8, 10, 2, 2, 'm');   // corredor
        tp.rect(3, 5, 12, 5, 'm');   // salão dos braseiros
        tp.rect(8, 3, 2, 2, 'm');    // passagem selada
        tp.rect(6, 1, 6, 2, 'm');    // santuário
        tp.t[15][8] = 'O'; tp.t[15][9] = 'O';
        tp.warps.add(new Warp(8, 15, "Ilha das Estrelas", 21, 7));
        tp.warps.add(new Warp(9, 15, "Ilha das Estrelas", 22, 7));
        tp.addNpc(new Npc("br1", "", 13, 7, new Color(255, 160, 60)).obj("brasa_off").still());   // Sol (leste)
        tp.addNpc(new Npc("br2", "", 4, 7, new Color(255, 160, 60)).obj("brasa_off").still());    // Lua (oeste)
        tp.addNpc(new Npc("br3", "", 11, 5, new Color(255, 160, 60)).obj("brasa_off").still());   // Estrela (norte)
        tp.addNpc(new Npc("mural", "", 5, 9, new Color(120, 115, 125)).obj("mural").still());
        tp.addNpc(new Npc("portao2", "", 8, 4, new Color(110, 110, 125)).obj("portao").still());
        tp.addNpc(new Npc("portao2", "", 9, 4, new Color(110, 110, 125)).obj("portao").still());
        tp.addNpc(new Npc("altar", "", 8, 1, new Color(120, 230, 255)).obj("altar").still());
        maps.put(tp.name, tp);

        // ===== Capítulo 4 =====
        // Trilha da Montanha: neve + campo de gelo (layout validado por busca: 11 movimentos, com volta e sem becos)
        GameMap tr = new GameMap("Trilha da Montanha", 22, 32, 'n', true);
        tr.snowy = true; tr.clear = 'n';
        for (int k = 0; k < 70; k++) tr.t[r.nextInt(32)][r.nextInt(22)] = '#';
        tr.border('#');
        tr.rect(1, 12, 20, 13, '#');                      // faixa de floresta de lado a lado: o único caminho é pelo gelo
        tr.rect(1, 5, 20, 1, '#');                        // barreira: o portão de gelo (16,5) é a única passagem
        tr.rect(4, 13, 14, 11, 'i');
        int[][] pedras = {{4, 16}, {4, 18}, {4, 19}, {5, 13}, {8, 22}, {9, 13}, {10, 15}, {12, 14}, {12, 16}, {12, 23},
                          {13, 17}, {13, 20}, {13, 21}, {14, 14}, {15, 13}, {15, 17}};
        for (int[] pd : pedras) tr.t[pd[1]][pd[0]] = 'R';
        tr.rect(10, 24, 2, 8, '=');                       // entrada pelo sul
        tr.rect(16, 0, 1, 13, '=');                       // saída do gelo até o pico
        tr.rect(5, 9, 12, 1, '=');                        // ramal da cabana
        tr.house(3, 5, true);
        tr.rect(5, 8, 1, 1, '=');
        tr.warps.add(new Warp(10, 31, "Vila Aurora", 14, 1));
        tr.warps.add(new Warp(11, 31, "Vila Aurora", 15, 1));
        tr.warps.add(new Warp(5, 7, "Cabana do Eremita", 5, 6));
        tr.warps.add(new Warp(16, 0, "Pico Estrelado", 10, 18));
        tr.addNpc(new Npc("montanhista", "Montanhista", 9, 28, new Color(200, 60, 60)));
        tr.addNpc(new Npc("gelo", "", 16, 5, new Color(170, 228, 250)).obj("gelo").still().stages(0, 18));
        maps.put(tr.name, tr);

        GameMap cb = new GameMap("Cabana do Eremita", 10, 8, 'f', false);
        cb.border('X'); cb.t[7][5] = 'D';
        cb.warps.add(new Warp(5, 7, "Trilha da Montanha", 5, 8));
        cb.addNpc(new Npc("eremita", "Eremita", 3, 3, new Color(110, 80, 60)));
        cb.addNpc(new Npc("fogao", "", 7, 2, new Color(75, 75, 85)).obj("fogao_off").still());
        maps.put(cb.name, cb);

        GameMap pc = new GameMap("Pico Estrelado", 20, 20, 'n', true);
        pc.snowy = true; pc.clear = 'n';
        for (int k = 0; k < 45; k++) pc.t[r.nextInt(20)][r.nextInt(20)] = '#';
        pc.border('#');
        pc.rect(8, 6, 5, 5, 'c');                         // cratera
        pc.t[5][7] = 'R'; pc.t[5][13] = 'R'; pc.t[11][7] = 'R'; pc.t[11][13] = 'R';
        pc.rect(9, 11, 2, 9, '=');
        pc.warps.add(new Warp(9, 19, "Trilha da Montanha", 16, 1));
        pc.warps.add(new Warp(10, 19, "Trilha da Montanha", 16, 1));
        pc.addNpc(new Npc("cintila", "", 10, 8, new Color(255, 230, 110)).obj("cintila").stages(19, 19));
        pc.addNpc(new Npc("treno", "", 15, 12, new Color(160, 110, 60)).obj("treno").still());
        maps.put(pc.name, pc);
    }

    // ---------- Sprites ----------
    void loadSprites() {
        File[] files = new File("assets").listFiles((d, n) -> n.endsWith(".png"));
        if (files == null) { System.out.println("Pasta assets/ não encontrada: usando desenhos por código."); return; }
        for (File f : files) {
            try { spr.put(f.getName().replace(".png", ""), ImageIO.read(f)); }
            catch (Exception e) { System.err.println("Erro ao carregar " + f + ": " + e); }
        }
    }

    boolean img(Graphics2D g, String key, int x, int y) {
        BufferedImage i = spr.get(key);
        if (i == null) return false;
        g.drawImage(i, x, y, T, T, null);
        return true;
    }

    /** Desenha o tile com PNGs. Retorna false se faltar sprite (aí o desenho por código assume). */
    boolean spriteTile(Graphics2D g, GameMap m, char c, int tx, int ty, int x, int y) {
        boolean praia = m.name.equals("Praia Azul");
        switch (c) {
            case '.', 'F', '#' -> {
                if (!img(g, praia ? "sand" : m.snowy ? "snow" : "grass", x, y)) return false;
                if (c == 'F') img(g, "flower", x, y);
                if (c == '#') img(g, m.snowy ? "pine" : "tree", x, y);
                return true;
            }
            case '~' -> { return img(g, (frame / 30 + tx + ty) % 2 == 0 ? "water1" : "water2", x, y); }
            case 's' -> { return img(g, "sand", x, y); }
            case '=' -> { return img(g, "path", x, y); }
            case 'f' -> { return img(g, "floor", x, y); }
            case 'X' -> { return img(g, "wall", x, y); }
            case 'H' -> {
                if (m.at(tx, ty - 1) != 'H') return img(g, "roof", x, y);
                if (!img(g, "house_wall", x, y)) return false;
                int k = 0; while (m.at(tx - k - 1, ty) == 'H') k++;
                if (m.at(tx, ty - 2) != 'H' && (k == 1 || k == 3)) img(g, isNight() ? "window_night" : "window", x, y);
                return true;
            }
            case 'D' -> {
                if (!img(g, m.outdoor ? "house_wall" : "floor", x, y)) return false;
                return img(g, "door", x, y);
            }
            case 'L' -> {
                boolean top = m.at(tx, ty - 1) != 'L';
                boolean left = m.at(tx - 1, ty) != 'L', right = m.at(tx + 1, ty) != 'L';
                String side = left ? "_l" : right ? "_r" : "";
                if (top) return img(g, side.isEmpty() ? "lh_lamp" : "lh_top" + side, x, y);
                return img(g, (ty % 2 == 0 ? "lh_red" : "lh_white") + side, x, y);
            }
            case 'n' -> { return img(g, "snow", x, y); }
            case 'i' -> { return img(g, "ice", x, y); }
            case 'P' -> { return img(g, "pier", x, y); }
            case 'm' -> { return img(g, "temple_floor", x, y); }
            case 'R' -> { return img(g, "rock", x, y); }
            case 'c' -> { return img(g, "cave_floor", x, y); }
            case 'O' -> { if (!img(g, "rock", x, y)) return false; return img(g, "cave_mouth", x, y); }
            default -> { return false; }
        }
    }

    // ---------- Tempo ----------
    double hour() { return (time / 60.0) % 24; }
    double night() {
        double h = hour();
        if (h >= 7 && h < 17) return 0;
        if (h >= 17 && h < 20) return (h - 17) / 3;
        if (h >= 20 || h < 5) return 1;
        return 1 - (h - 5) / 2;
    }
    boolean isNight() { return night() > 0.6; }

    // ---------- Lógica ----------
    void update() {
        long now = System.nanoTime();
        double dt = Math.min(0.05, (now - last) / 1e9);
        last = now;
        frame++;
        if (toastT > 0) toastT -= dt;
        String want = musicFor();
        soundscape.update();
        if (!want.equals(curMusic)) { curMusic = want; audio.music(want); }
        festival.update(dt);
        if (state == FISHING || state == BELLS) { life.updateGame(dt); return; }
        if (state != PLAY) return;     // título e menus pausam o jogo
        if (!festival.fishing) time += dt * 6; // O concurso usa seu próprio cronômetro.
        gardening.sync();
        if (fade > 0) fade -= dt * 3;
        if (bannerT > 0) bannerT -= dt;
        if (dlg == null) life.updateResidents(dt);

        if (moving) {
            prog += dt * (sliding ? 7.5 : 4.5);
            if (prog >= 1) {
                fx = tx; fy = ty;                                            // a Cintila ocupa a casa que o jogador deixou
                tx = nx; ty = ny; moving = false; prog = 0;
                int ax = tx + DX[dir], ay = ty + DY[dir];
                if (map.at(tx, ty) == 'i' && free(ax, ay)) { nx = ax; ny = ay; moving = true; sliding = true; }   // gelo: continua deslizando
                else { sliding = false; audio.sfx("step"); checkWarp(); }
            }
        } else if (dlg == null) {
            int d = -1;
            if (held(KeyEvent.VK_DOWN, KeyEvent.VK_S)) d = 0;
            else if (held(KeyEvent.VK_UP, KeyEvent.VK_W)) d = 1;
            else if (held(KeyEvent.VK_LEFT, KeyEvent.VK_A)) d = 2;
            else if (held(KeyEvent.VK_RIGHT, KeyEvent.VK_D)) d = 3;
            if (d >= 0) {
                dir = d;
                int ax = tx + DX[d], ay = ty + DY[d];
                if (free(ax, ay)) {
                    nx = ax; ny = ay; moving = true; prog = 0;
                    if (map.at(ax, ay) == 'i') audio.sfx("slide");
                }
            }
        }
    }

    boolean held(int a, int b) { return down.contains(a) || down.contains(b); }

    boolean free(int x, int y) {
        if (map.solid(x, y)) return false;
        if (map.name.equals("Casa") && home.blocks(x, y, -1)) return false;
        for (Npc n : map.npcs) if (vis(n) && (n.x == x && n.y == y || n.walking && n.toX == x && n.toY == y)) return false;
        return true;
    }

    void resetFollower() {
        int bx = tx - DX[dir], by = ty - DY[dir];
        if (map.solid(bx, by)) { bx = tx; by = ty; }
        fx = bx; fy = by;
    }

    void checkWarp() {
        for (Warp w : map.warps) {
            if (w.x() == tx && w.y() == ty) {
                if (!chapter.allowWarp(w.to())) { toast("A passagem ainda está selada."); return; }
                map = maps.get(w.to()); tx = w.tx(); ty = w.ty(); fade = 1; resetFollower();
                worldMap.arrive();
                audio.sfx("door");
                if (map.name.equals("Templo Estrelado") && stage == 11) setStage(12);
                save(false);
                return;
            }
        }
    }

    void interact() {
        int ax = tx + DX[dir], ay = ty + DY[dir];
        for (Npc n : map.npcs) if (vis(n) && n.x == ax && n.y == ay) { n.dir = OPP[dir]; Set<String> antes = itemKeys(); talk(n); audio.sfx("blip"); notifyItems(antes); if (!sig().equals(lastSig)) save(true); return; }
        if (map.at(ax, ay) == '~') life.cast();
    }

    /** NPC/objeto só aparece na fase e no horário certos. */
    boolean vis(Npc n) {
        if (!chapter.visible(n.id)) return false;
        if (n.id.equals("festival_souvenir") && (festival.prizes == 0 || !home.placed(HomeDecor.STAR))) return false;
        if (!bonds.visible(n.id)) return false;
        if (n.sleeping) return false;
        if (n.id.equals("portao2") && brasas >= 3) return false;     // portão interno abre com os 3 braseiros
        if (stage < n.minStage || stage > n.maxStage) return false;
        if ("noite".equals(n.when) && !isNight()) return false;
        if ("dia".equals(n.when) && isNight()) return false;
        return true;
    }

    void setStage(int st) {
        stage = st;
        banner = switch (st) {
            case 4 -> "Capítulo 1 concluído: A Luz do Farol";
            case 5 -> "Capítulo 2: O Chamado da Capitã";
            case 7 -> "Mapa Estrelado completo!";
            case 8 -> "Capítulo 3: A Ilha das Estrelas";
            case 10 -> "Despedidas feitas! Volte ao cais.";
            case 11 -> "Ilha das Estrelas";
            case 12 -> "O Templo Estrelado";
            case 13 -> "O altar se revelou!";
            case 14 -> "A Estrela do Mar é sua!";
            case 15 -> "Fim do Capítulo 3";
            case 16 -> "Capítulo 4: A Estrela Cadente";
            case 18 -> "Cristal de Calor obtido!";
            case 19 -> "O portão de gelo derreteu!";
            case 20 -> "A Cintila vai com você!";
            case 21 -> "Fim do Capítulo 4 — continua...";
            case 22 -> "Capítulo 5: O Jardim das Constelações";
            case 23 -> "A Trilha dos Vagalumes está aberta";
            case 24 -> "Três sementes de luz reunidas";
            case 25 -> "O Jardim das Constelações";
            case 26 -> "O jardim despertou!";
            case 27 -> "Uma flor para levar para casa";
            case 28 -> "Capítulo 5 concluído: Laços entre céu e terra";
            default -> "Objetivo atualizado";
        };
        bannerT = 4;
        if (st == 4 || st == 5 || st == 7 || st == 8 || st == 13 || st == 14 || st == 15 || st == 16 || st == 20 || st == 21 || st >= 22) audio.sfx("fanfare");
        refreshObjects();
    }

    void say(String who, String... lines) { afterDlg = null; speaker = who; dlg = lines; dlgIdx = 0; }

    // ---------- Ações do mundo ----------
    void checkBye() { if (bye == 3) setStage(10); }

    /** Descansar: de dia pula para a noite (20:00); de noite pula para a manhã (07:30). */
    void rest(boolean paraNoite) {
        double ini = Math.floor(time / 1440) * 1440;
        if (paraNoite) time = ini + 20 * 60;
        else time = ini + (hour() >= 12 ? 1440 : 0) + 7.5 * 60;
        fade = 1;
        audio.sfx("sleep");
        toast(paraNoite ? "Anoiteceu." : "Amanheceu!");
        save(false);
    }

    void travel(String mapName, int x, int y) {
        map = maps.get(mapName); tx = x; ty = y; moving = false; sliding = false; prog = 0; fade = 1; resetFollower();
        worldMap.arrive();
        audio.sfx("warp");
        save(false);
    }

    /** Braseiros acesos e altar mudam de sprite conforme o progresso. */
    void refreshObjects() {
        for (GameMap m : maps.values())
            for (Npc n : m.npcs) {
                if (n.id.length() == 3 && n.id.startsWith("br")) n.sprite = (n.id.charAt(2) - '0') <= brasas ? "brasa_on" : "brasa_off";
                if (n.id.equals("altar")) n.sprite = stage >= 14 ? "pedestal" : "altar";
                if (n.id.equals("fogao")) n.sprite = stage >= 18 ? "fogao_on" : "fogao_off";
            }
    }

    void talk(Npc n) {
        if (orders.talk(n)) return;
        if (gardening.talk(n)) return;
        if (home.talk(n)) return;
        if (festival.talk(n)) return;
        if (bonds.collect(n)) return;
        int previousStage = stage;
        storyTalk(n);
        journal.record(n, previousStage);
        bonds.afterTalk(n);
        weather.afterTalk(n);
        festival.afterTalk(n);
    }

    void storyTalk(Npc n) {
        if (chapter.talk(n)) return;
        if (life.talk(n)) return;
        boolean noite = isNight();
        switch (n.id) {
            case "mae" -> {
                if (stage == 0) {
                    setStage(1);
                    say(n.name, "Bom dia! Hoje é um dia especial na Vila Aurora.",
                        "Preciso que leve esta carta ao Seu Marinho, na Praia Azul.",
                        "Siga a trilha ao sul da vila. Boa viagem!");
                } else if (stage == 1) say(n.name, noite ? "Já está escuro... leve cuidado!" : "Seu Marinho mora na praia, ao sul da vila. Vá com calma!");
                else if (stage < 4) say(n.name, "Uma concha mágica? Cuidado na floresta, e volte antes de escurecer!");
                else if (stage == 4) say(n.name, "Vi o farol acendendo daqui de casa! Que orgulho!",
                    "Dizem que essa luz atrai visitantes misteriosos à noite. Vá ver, mas agasalhe-se!");
                else if (stage <= 7) say(n.name, "Uma capitã numa gruta? Leve cuidado com o escuro e volte para o jantar!");
                else if (stage == 8) say(n.name, "Uma viagem de barco?! Que emoção! Mas fale primeiro com a Capitã, no cais da praia, de dia.");
                else if (stage == 9 && (bye & 1) == 0) {
                    bye |= 1;
                    say(n.name, "Então vai mesmo velejar? Meu coração aperta... mas sei que você é corajoso(a).",
                        "Leve este lanche de pão com queijo e um agasalho. E volte inteiro(a)!");
                    checkBye();
                } else if (stage <= 13) say(n.name, "Boa viagem! Eu espero aqui, com o jantar quentinho.");
                else if (stage == 14) say(n.name, "Voltou! Depois você me conta tudo. Mostre a Estrela ao Seu Marinho, à noite!");
                else if (stage == 15) say(n.name, "Meu(minha) explorador(a) das estrelas! O farol nunca brilhou tanto!");
                else if (stage <= 19) say(n.name, "Vai subir a montanha? Leve um cachecol e cuidado com o gelo!");
                else if (stage == 20) say(n.name, "Uma estrelinha que fala?! Que fofa! Leve-a ao farol, antes que ela fique com frio.");
                else say(n.name, "O céu inteiro brilhou ontem à noite. Foi você, não foi?");
            }
            case "marinho" -> {
                switch (stage) {
                    case 0 -> say(n.name, "Olá, jovem! Estou só olhando o mar.");
                    case 1 -> {
                        setStage(2);
                        say(n.name, "Uma carta da sua mãe? Obrigado!",
                            "O farol apagou: a Concha Brilhante, o coração dele, sumiu na Floresta Verde.",
                            "Pode buscá-la? Fica numa clareira, no fim da trilha da floresta.");
                    }
                    case 2 -> say(n.name, "A floresta fica a leste da vila. Procure uma luz dourada.");
                    case 3 -> {
                        setStage(4);
                        say(n.name, "A Concha Brilhante! Você conseguiu!", "*clique* ...O farol voltou a brilhar!",
                            "Sabe de uma coisa? Luz assim atrai visitantes. Volte à noite e fique de olho no mar.");
                    }
                    case 4 -> say(n.name, noite ? "Olhe o mar... a maré trouxe algo. Procure pela praia!" : "O farol faz sua mágica à noite. Volte depois que escurecer!");
                    case 5 -> say(n.name, "Capitã Estrela?! Há anos não ouço esse nome... Fomos tripulantes juntos. Cuide dela.");
                    case 6 -> say(n.name, "Pedaços de mapa? A maré baixa, de dia, deixa a praia cheia de segredos. Procure com atenção.");
                    case 7 -> say(n.name, "Então o mapa está completo! Leve à Capitã. O mar chama...");
                    case 8 -> say(n.name, "A Capitã está no cais? Ela sempre foi teimosa... mas é a melhor navegadora que conheço.");
                    case 9 -> {
                        if ((bye & 2) == 0) {
                            bye |= 2;
                            say(n.name, "Vai zarpar com a Capitã? Então leve esta bússola velha: ela sempre aponta para casa.",
                                "Cuide dela... e de você, marujo.");
                            checkBye();
                        } else say(n.name, "Boa viagem, marujo. O mar é generoso com quem o respeita.");
                    }
                    case 10, 11, 12, 13 -> say(n.name, "Boa viagem, marujo. O mar é generoso com quem o respeita.");
                    case 14 -> {
                        if (noite) {
                            setStage(15);
                            say(n.name, "Isso é... a Estrela do Mar?! Ela brilha como o próprio mar!",
                                "Venha, vamos colocá-la no topo do farol, junto da Concha Brilhante.",
                                "*clique* ...Olhe! A baía inteira se iluminou!",
                                "Obrigado, marujo. Você devolveu a luz a Aurora.",
                                "FIM DO CAPÍTULO 3. Obrigado por jogar! (Continua...)");
                        } else say(n.name, "Uma estrela do mar de cristal?! Ela brilha melhor no escuro. Volte depois que anoitecer!");
                    }
                    case 15 -> say(n.name, "O farol e a Estrela juntos... a baía inteira brilha! Obrigado, marujo.");
                    case 16, 17, 18, 19 -> say(n.name, "Uma estrela caiu na Montanha Névoa? O farol vai ficar curioso. Boa subida, marujo!");
                    case 20 -> say(n.name, noite ? "Essa luzinha no seu ombro... Vá até a porta do farol, ao pé da torre!" : "Uma estrelinha? Traga-a à noite: é quando o farol chama quem vem do céu.");
                    default -> say(n.name, "O farol nunca brilhou tanto. Uma estrela pousou nele... jamais vou esquecer.");
                }
            }
            case "concha" -> {
                if (stage == 2) {
                    setStage(3);
                    take(n);
                    say("", "Uma concha dourada brilha entre as folhas...", "Você pegou a Concha Brilhante!");
                } else say("", "Uma concha dourada. Parece pertencer a alguém... melhor deixar aí por enquanto.");
            }
            case "garrafa" -> {
                take(n);
                setStage(5);
                say("", "Uma garrafa trazida pela maré... há um bilhete dentro.",
                    "\"Vi o farol acender depois de tantos anos. Preciso de ajuda. Estou na Gruta do Eco, ao norte da Floresta Verde. — Capitã Estrela\"");
            }
            case "frag1", "frag2", "frag3" -> {
                frags |= 1 << (n.id.charAt(4) - '1');
                take(n);
                int cnt = Integer.bitCount(frags);
                String txt = n.id.equals("frag1") ? "Um pedaço de pergaminho brilha ao luar..."
                    : n.id.equals("frag2") ? "Com a maré baixa, algo brilha na areia seca..."
                    : "Sobre uma pedra, no fundo da gruta, repousa um pedaço de mapa.";
                if (cnt == 3) {
                    setStage(7);
                    say("", txt, "Fragmento do Mapa Estrelado (3/3)!", "Os três pedaços se encaixam sozinhos. Leve o mapa à Capitã Estrela!");
                } else say("", txt, "Fragmento do Mapa Estrelado (" + cnt + "/3)!");
            }
            case "capita" -> {
                switch (stage) {
                    case 5 -> {
                        setStage(6);
                        say(n.name, "Você veio! Foi você quem acendeu o farol, não foi? Eu sou a Capitã Estrela.",
                            "Meu navio naufragou há anos e o Mapa Estrelado se partiu em três pedaços.",
                            "Um brilha à noite, na vila, perto do lago. Outro foi enterrado na praia: só aparece na maré baixa, de dia.",
                            "O terceiro está no fundo desta gruta, ao leste. Me traga todos!");
                    }
                    case 6 -> say(n.name, "Faltam " + (3 - Integer.bitCount(frags)) + " pedaço(s). Vila à noite, praia de dia, e o fundo desta gruta.");
                    case 7 -> {
                        setStage(8);
                        say(n.name, "O Mapa Estrelado inteiro! Há anos eu sonhava com isso.",
                            "Ele aponta para a Ilha das Estrelas, onde repousa a lendária Estrela do Mar.",
                            "Meu barco, o Estrela do Norte, ainda está inteiro no cais da Praia Azul.",
                            "Sem você eu ainda estaria perdida no escuro. Vamos velejar! Me encontre no cais, de dia.");
                    }
                    default -> say(n.name, "Shh! Esta gruta ecoa tudo... Volte quando a luz do farol me chamar.");
                }
            }
            case "capita_cais" -> {
                switch (stage) {
                    case 8 -> {
                        setStage(9);
                        say(n.name, "Chegou! Este é o Estrela do Norte, meu barco. Casco firme e vela nova!",
                            "A Ilha das Estrelas fica mar adentro. Antes de zarpar, despeça-se da sua mãe e do Seu Marinho.",
                            "Quem parte sem se despedir não volta com o mesmo sorriso!");
                    }
                    case 9 -> say(n.name, "Já se despediu? Fale com sua mãe e com o Seu Marinho (" + Integer.bitCount(bye) + "/2).");
                    default -> {
                        say(n.name, "Tudo pronto? Então, ao mar!",
                            "...O Estrela do Norte corta as ondas, e o farol vai ficando pequeno no horizonte.",
                            "Lá está! A Ilha das Estrelas surge entre as nuvens!");
                        afterDlg = () -> { setStage(11); time += 90; travel("Ilha das Estrelas", 4, 10); };
                    }
                }
            }
            case "capita_ilha" -> {
                if (stage == 11) {
                    if (noite) say(n.name, "Olhe! O portão do templo sumiu! Vamos logo, ele fica a leste.");
                    else say(n.name, "Chegamos! O templo fica a leste, mas dizem que o portão só some sob as estrelas.",
                        "Descanse na fogueira até anoitecer. Eu espero aqui.");
                } else if (stage == 12) say(n.name, "O mural do salão deve dizer a ordem das chamas. Leia com atenção!");
                else if (stage == 13) say(n.name, "O portão interno abriu! O altar está logo ali!");
                else say(n.name, "Vamos para casa! O barco está no cais, a oeste.");
            }
            case "placa" -> say("Placa", "\"Trilha da Montanha Névoa: FECHADA. Neve forte e gelo.\"");
            case "montanhista" -> say(n.name, stage >= 19 ? "Soube que o gelo do portão derreteu? Quem diria!"
                : "A trilha congelada é traiçoeira: você desliza até bater em alguma coisa. Procure as pedras: elas fazem você parar no lugar certo!");
            case "eremita" -> {
                switch (stage) {
                    case 16 -> {
                        setStage(17);
                        say(n.name, "Visitas! Você veio por causa da estrela cadente, não foi? Eu vi: ela caiu lá no Pico!",
                            "Mas o portão da trilha está preso num bloco de gelo. Só o Cristal de Calor, que guardo aqui, o derrete.",
                            "O problema: meu fogão apagou e o cristal só desperta com o calor do fogo. Acenda-o com a sua lanterna!");
                    }
                    case 17 -> say(n.name, "Meu fogão está apagado. Acenda-o com a lanterna e o Cristal de Calor desperta!");
                    case 18 -> say(n.name, "Use o cristal no portão de gelo, mais ao norte na trilha. E cuide da estrela: ela deve estar frágil.");
                    case 19 -> say(n.name, "Boa sorte lá em cima. O Pico é lindo à noite.");
                    case 20 -> say(n.name, "A estrelinha está com você? Que bom que alguém a encontrou. Desça de trenó, é mais rápido!");
                    default -> say(n.name, stage >= 21 ? "O céu dessa noite... nunca vi nada igual. Obrigado, jovem." : "Hm? Não esperava visita...");
                }
            }
            case "fogao" -> {
                if (stage == 17) {
                    setStage(18);
                    audio.sfx("fire");
                    say("", "Você acende o fogão com a Lanterna. A lenha estala e esquenta a cabana toda.",
                        "Na prateleira, o Cristal de Calor começa a brilhar em tons de laranja.",
                        "O Eremita sorri e entrega o Cristal de Calor a você!");
                } else if (stage < 17) say("", "O fogão está apagado e frio. Melhor falar com o dono da cabana.");
                else say("", "O fogão crepita, quentinho.");
            }
            case "gelo" -> {
                if (stage == 18) {
                    setStage(19);
                    audio.sfx("fire");
                    say("", "Você aproxima o Cristal de Calor do bloco de gelo...", "O gelo brilha em vermelho, racha e derrete em segundos!", "A trilha até o Pico Estrelado está livre.");
                } else say("", "Um bloco de gelo maciço bloqueia a trilha. Dizem que o Eremita sabe como derretê-lo.");
            }
            case "treno" -> {
                if (stage >= 20) {
                    say("", "Você se senta no trenó com a Cintila no colo.", "Ele desce a montanha em disparada, levantando neve!");
                    afterDlg = () -> { time += 20; travel("Vila Aurora", 15, 2); };
                } else say("", "Um trenó de madeira, deixado pelo Eremita. Primeiro veja o que brilha na cratera.");
            }
            case "cintila" -> {
                take(n);
                setStage(20);
                audio.sfx("twinkle");
                say("Cintila", "Tin... tin... (uma estrelinha de olhos grandes pisca fraquinho na neve)",
                    "\"Você... acendeu aquele farol? Vi a luz lá do céu e quis chegar mais perto... e caí.\"",
                    "\"Estou fraca demais para voltar. Só a luz do farol, à noite, me dá força. Pode me levar até lá?\"",
                    "A Cintila pula para perto de você e passa a seguir seus passos!");
            }
            case "porta_farol" -> {
                if (stage == 20 && noite) {
                    say("", "Você encosta na porta do farol e a Cintila começa a brilhar, cada vez mais forte...",
                        "Cintila: \"A luz do farol... é quentinha. Obrigada, amigo(a)!\"",
                        "Ela sobe voando até a lente do farol, e o feixe de luz se enche de estrelas!",
                        "Cintila: \"Agora posso voltar para casa, no céu. Procure por mim nas noites claras!\"",
                        "FIM DO CAPÍTULO 4. Obrigado por jogar! (Continua...)");
                    afterDlg = () -> setStage(21);
                } else if (stage == 20) say("Cintila", "Tin! Só consigo subir à noite, quando a luz do farol me chama. Vamos esperar escurecer?");
                else if (stage >= 21) say("", "A porta do farol. Lá em cima, uma estrelinha cintila a noite toda.");
                else say("", "A porta do farol está trancada. O Seu Marinho cuida dele com carinho.");
            }
            case "capita_fim" -> {
                if (stage == 14) say(n.name, "Vá mostrar a Estrela ao Seu Marinho. À noite, o farol vai brilhar como nunca!");
                else if (stage >= 21) say(n.name, "Uma estrela voltou para o céu... Já vi muito mar, mas nada tão lindo.");
                else say(n.name, "Minha vida inteira procurando... e foi você quem a encontrou. Obrigada.", "O mar tem um novo guardião!");
            }
            case "barco" -> say("", stage <= 10 ? "O Estrela do Norte, o barco da Capitã. Casco firme e vela nova." : "O Estrela do Norte balança suavemente no cais.");
            case "barco_ilha" -> {
                if (stage >= 14) {
                    say("", "Hora de voltar para casa.", "O Estrela do Norte singra as ondas, com a Estrela do Mar brilhando no convés...");
                    afterDlg = () -> { time += 90; travel("Praia Azul", 5, 15); };
                } else say("", "O barco da Capitã. Ainda temos trabalho nesta ilha.");
            }
            case "cama", "fogueira" -> {
                boolean paraNoite = !noite;
                say("", (n.id.equals("cama") ? "Você se deita na cama" : "Você se senta junto à fogueira")
                    + (paraNoite ? " e cochila até o entardecer..." : " e descansa até o amanhecer..."));
                afterDlg = () -> rest(paraNoite);
            }
            case "estatua" -> say("Estátua", noite ? "As estrelas despertaram. Entre, viajante." : "Somente quem segue as estrelas cruza o portão.");
            case "portao" -> say("", "Um portão de pedra com uma estrela gravada. Está selado... talvez as estrelas saibam abri-lo.");
            case "portao2" -> say("", "Um segundo portão selado. Ele parece reagir às chamas do salão.");
            case "mural" -> say("", "Um mural antigo com três símbolos: o Sol, a Lua e uma Estrela.",
                "\"O Sol nasce primeiro, a Lua o segue, e a Estrela fecha a noite.\"",
                "Os braseiros do salão devem ser acesos nessa ordem.");
            case "br1", "br2", "br3" -> {
                int idx = n.id.charAt(2) - '0';
                String de = new String[]{"do Sol", "da Lua", "da Estrela"}[idx - 1];
                if (idx <= brasas) say("", "O braseiro " + de + " já está aceso.");
                else if (idx == brasas + 1) {
                    brasas++;
                    audio.sfx("fire");
                    refreshObjects();
                    if (brasas == 3) {
                        setStage(13);
                        say("", "A chama " + de + " se acende!", "As três chamas brilham juntas... com um ronco, o portão interno se abre!");
                    } else say("", "A chama " + de + " se acende!");
                } else {
                    brasas = 0;
                    audio.sfx("bad");
                    refreshObjects();
                    say("", "A chama " + de + " se acende... mas logo se apaga, e as outras também!", "A ordem estava errada. Releia o mural.");
                }
            }
            case "altar" -> {
                if (stage == 13) {
                    setStage(14);
                    say("", "No altar repousa a Estrela do Mar: uma estrela-do-mar de cristal que pulsa com luz própria.",
                        "Você a pegou! Ela está morna e parece sussurrar o som do mar.",
                        "Vamos voltar para casa! Leve a Estrela ao Seu Marinho, à noite.");
                } else if (stage > 13) say("", "O altar está vazio, mas ainda ecoa o som do mar.");
                else say("", "Um altar de pedra, coberto por uma luz fraca. Ainda está selado.");
            }
            case "prof" -> {
                if (stage <= 3) {
                    if (noite) say(n.name, "Já escureceu... a vila fica bonita à noite, mas cuidado na floresta.");
                    else say(n.name, "Sou o Prof. Carvalho. Estudo as regiões daqui: Vila, Floresta e Praia.",
                        stage < 2 ? "Dizem que o Seu Marinho anda precisando de ajuda." : "Boa sorte na sua missão!");
                } else if (stage == 4) say(n.name, "O farol voltou a brilhar! Dizem que à noite a maré traz surpresas.");
                else if (stage == 5) say(n.name, "Uma capitã na Gruta do Eco? Eu achava que era lenda!");
                else if (stage == 6) say(n.name, "Um mapa em pedaços? Os fragmentos brilham à noite, perto da água. Procure ao redor do lago da vila!");
                else if (stage == 7) say(n.name, "Mapa completo? Fascinante! Mostre à Capitã.");
                else if (stage <= 10) say(n.name, "Uma viagem à Ilha das Estrelas?! Anote tudo! Eu daria meu microscópio para ir junto!");
                else if (stage <= 13) say(n.name, "Quando voltar, conte cada detalhe. Vou deixar o caderno pronto!");
                else if (stage == 14) say(n.name, "Uma estrela-do-mar de cristal? Isso muda os livros de ciência!");
                else if (stage == 15) {
                    setStage(16);
                    say(n.name, "Você viu o céu ontem à noite? Uma estrela cadente caiu na Montanha Névoa, ao norte!",
                        "Meu avô tinha um observatório lá em cima e uma trilha que só os corajosos cruzam.",
                        "Leve esta lanterna dele: ela ilumina mais longe no escuro. A trilha começa na saída norte da vila.",
                        "Procure o Eremita da cabana. Ele conhece a montanha melhor que ninguém!");
                } else if (stage <= 19) say(n.name, "A Montanha Névoa fica ao norte da vila. Cuidado com o gelo: ele escorrega!");
                else if (stage == 20) say(n.name, "Uma estrelinha falante?! Leve-a ao farol, à noite. Ela vai se sentir em casa.");
                else say(n.name, noite ? "Olhe para o céu! Uma estrela nova brilha sobre o farol!" : "Dizem que o céu da vila brilhou diferente ontem à noite. Vou anotar tudo!");
            }
            case "lenhador" -> {
                if (stage >= 16) say(n.name, "Dizem que a estrela caiu na montanha. Eu ouvi o estrondo daqui da floresta!");
                else if (stage >= 8) say(n.name, "Ouvi dizer que vão velejar. Leve um chapéu, o sol do mar queima!");
                else if (stage >= 5) say(n.name, "A gruta fica no fim da trilha ao norte. Dizem que ela ecoa até de dia.");
                else say(n.name, noite ? "À noite essas árvores parecem sussurrar..." : "Esta floresta é enorme. Dizem que há uma clareira mágica ao leste.");
            }
            default -> say(n.name, "...");
        }
    }

    void drawNpc(Graphics2D g, Npc n, int camX, int camY) {
        int x = (int) ((n.x + (n.walking ? (n.toX - n.x) * n.walkProgress : 0)) * T) - camX;
        int y = (int) ((n.y + (n.walking ? (n.toY - n.y) * n.walkProgress : 0)) * T) - camY - 4;
        if (orders.drawObject(g, n, x, y) || gardening.drawObject(g, n, x, y) || chapter.drawObject(g, n, x, y) || home.drawStall(g, n, x, y) || festival.drawObject(g, n, x, y) || bonds.drawObject(g, n, x, y) || life.drawObject(g, n, x, y)) return;
        if (!n.object) { person(g, n.sprite, x, y, n.color, n.dir, n.walking, n.walkProgress, true); return; }
        BufferedImage im = spr.get(n.sprite);
        int bob = n.still ? 0 : (int) (Math.sin(frame / 15.0) * 2);
        if (im != null) { g.drawImage(im, x, y + 4 + bob, T, T, null); return; }
        g.setColor(new Color(n.color.getRed(), n.color.getGreen(), n.color.getBlue(), 90));    // sem PNG: bolinha colorida
        g.fillOval(x + 2, y + 6 + bob, 28, 28);
        g.setColor(n.color);
        g.fillOval(x + 9, y + 14 + bob, 14, 14);
    }

    // ---------- Música ----------
    String musicFor() {
        if (state == TITLE || state == CONFIRM) return "titulo";
        boolean n = isNight();
        return switch (map.name) {
            case "Casa" -> "casa";
            case "Vila Aurora", "Quintal de Casa" -> n ? "vila_noite" : "vila_dia";
            case "Floresta Verde" -> n ? "floresta_noite" : "floresta_dia";
            case "Praia Azul" -> stage >= 15 && n ? "final" : n ? "praia_noite" : "praia_dia";
            case "Ilha das Estrelas" -> n ? "ilha_noite" : "ilha_dia";
            case "Gruta do Eco" -> "gruta";
            case "Trilha da Montanha" -> n ? "montanha_noite" : "montanha_dia";
            case "Cabana do Eremita" -> "cabana";
            case "Pico Estrelado" -> "pico";
            case "Templo Estrelado" -> "templo";
            case "Trilha dos Vagalumes" -> n ? "floresta_noite" : "floresta_dia";
            case "Jardim das Constelações" -> "jardim";
            default -> "vila_dia";
        };
    }

    // ---------- Caixa de itens ----------
    static final String[] ITEM_KEYS = {"carta", "concha", "bilhete", "mapa", "lanche", "bussola", "estrela", "lanterna", "cristal", "vara", "trofeu", "sinos"};
    static final String[] ITEM_NAMES = {"Carta da Mamãe", "Concha Brilhante", "Bilhete da Capitã", "Mapa Estrelado",
                                        "Lanche da Mamãe", "Bússola do Seu Marinho", "Estrela do Mar", "Lanterna do Professor", "Cristal de Calor", "Vara do Tio Beto", "Troféu de Pesca", "Medalha dos Sinos"};
    static final Color[] ITEM_COLORS = {new Color(240, 230, 200), new Color(255, 215, 80), new Color(235, 220, 170),
        new Color(224, 198, 142), new Color(220, 170, 95), new Color(190, 150, 60), new Color(120, 230, 255),
        new Color(255, 215, 120), new Color(255, 125, 55), new Color(80, 190, 150), new Color(245, 200, 65), new Color(140, 210, 245)};

    /** 0 = ainda não encontrado, 1 = em mãos, 2 = já usado/entregue. */
    int itemState(String k) {
        return switch (k) {
            case "vara" -> life.rod ? 1 : 0;
            case "trofeu" -> life.completeAlbum() ? 1 : 0;
            case "sinos" -> life.bellBest >= VillageLife.BELL_ROUNDS ? 1 : 0;
            case "carta" -> stage < 1 ? 0 : stage == 1 ? 1 : 2;
            case "concha" -> stage < 3 ? 0 : stage == 3 ? 1 : 2;
            case "bilhete" -> stage >= 5 ? 1 : 0;
            case "mapa" -> frags != 0 || stage >= 7 ? 1 : 0;
            case "lanche" -> (bye & 1) == 0 ? 0 : stage >= 11 ? 2 : 1;
            case "bussola" -> (bye & 2) != 0 ? 1 : 0;
            case "estrela" -> stage < 14 ? 0 : stage == 14 ? 1 : 2;
            case "lanterna" -> stage >= 16 ? 1 : 0;
            case "cristal" -> stage < 18 ? 0 : stage == 18 ? 1 : 2;
            default -> 0;
        };
    }

    String itemDesc(String k, int st) {
        return switch (k) {
            case "vara" -> "Uma vara feita pelo Tio Beto, com linha forte e boia vermelha.";
            case "trofeu" -> "As oito espécies de Aurora, registradas no seu álbum. Uma conquista de mestre pescador!";
            case "sinos" -> "As cinco melodias da praça ecoaram sem um erro. Lucas vai querer uma revanche!";
            case "carta" -> st == 1 ? "Uma carta selada, com a letra caprichada da Mamãe. É para o Seu Marinho, na Praia Azul."
                                    : "Entregue ao Seu Marinho. Foi o começo de tudo isso.";
            case "concha" -> st == 1 ? "O coração do farol. Brilha com uma luz dourada e quentinha. Leve ao Seu Marinho."
                                     : "Encaixada no farol. Desde então, ele ilumina a baía toda noite.";
            case "bilhete" -> "Encontrado numa garrafa trazida pela maré: a Capitã Estrela pede ajuda e diz estar na Gruta do Eco.";
            case "mapa" -> stage >= 7 ? "O Mapa Estrelado completo. Aponta para a Ilha das Estrelas, onde repousa a lendária Estrela do Mar."
                                      : "Pedaços do mapa do navio da Capitã (" + Integer.bitCount(frags) + "/3). Um brilha à noite, outro aparece de dia e o último está na gruta.";
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

    Set<String> itemKeys() {
        Set<String> r = new HashSet<>();
        for (String k : ITEM_KEYS) if (itemState(k) > 0) r.add(k);
        return r;
    }

    void notifyItems(Set<String> antes) {
        List<String> novos = new ArrayList<>();
        for (int i = 0; i < ITEM_KEYS.length; i++)
            if (itemState(ITEM_KEYS[i]) > 0 && !antes.contains(ITEM_KEYS[i])) novos.add(ITEM_NAMES[i]);
        if (!novos.isEmpty()) pendingToast = "Novo item: " + String.join(", ", novos) + "  (I)";
    }

    void drawItemIcon(Graphics2D g, int idx, int x, int y, int size, boolean dim) {
        String k = ITEM_KEYS[idx];
        BufferedImage im = spr.get(k.equals("concha") ? "concha" : "item_" + k);
        Composite old = g.getComposite();
        if (dim) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
        if (life.drawItem(g, k, x, y, size)) { g.setComposite(old); return; }
        if (im != null) g.drawImage(im, x, y, size, size, null);
        else {                                                         // sem PNG: quadrado colorido com a inicial
            g.setColor(ITEM_COLORS[idx]); g.fillRoundRect(x + 4, y + 4, size - 8, size - 8, 10, 10);
            g.setColor(new Color(40, 40, 60)); g.setFont(new Font("SansSerif", Font.BOLD, size / 2));
            g.drawString(ITEM_NAMES[idx].substring(0, 1), x + size / 2 - size / 8, y + size / 2 + size / 6);
        }
        g.setComposite(old);
    }

    void drawMapPieces(Graphics2D g, int x, int y, int w, int h) {
        boolean full = stage >= 7;
        int pw = w / 3;
        for (int i = 0; i < 3; i++) {
            boolean got = full || (frags & (1 << i)) != 0;
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
                g.setFont(new Font("SansSerif", Font.BOLD, 16)); g.drawString("?", bx + pw / 2 - 5, y + h / 2 + 6);
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
            g.drawString(ilha, x + w - 6 - g.getFontMetrics().stringWidth(ilha), y + 50);
        }
    }

    void drawInventory(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150)); g.fillRect(0, 0, W, H);
        int px = 40, py = 14, pw = W - 80, ph = H - 28;
        g.setColor(new Color(22, 26, 64, 240)); g.fillRoundRect(px, py, pw, ph, 20, 20);
        g.setColor(new Color(255, 215, 120)); g.setStroke(new BasicStroke(3)); g.drawRoundRect(px, py, pw, ph, 20, 20); g.setStroke(new BasicStroke(1));
        g.setFont(new Font("SansSerif", Font.BOLD, 20)); g.setColor(new Color(255, 225, 140)); g.drawString("Itens", px + 24, py + 34);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(255, 255, 255, 180));
        g.drawString(itemKeys().size() + " / " + ITEM_KEYS.length + " encontrados", px + pw - 150, py + 32);

        int cell = 64, gap = 10, gx = px + (pw - (4 * cell + 3 * gap)) / 2, gy = py + 50;
        for (int i = 0; i < 12; i++) {                                    // grade 4 x 3
            int x = gx + (i % 4) * (cell + gap), y = gy + (i / 4) * (cell + gap);
            boolean sel = i == invSel, real = i < ITEM_KEYS.length;
            g.setColor(real ? new Color(40, 46, 104) : new Color(30, 34, 76)); g.fillRoundRect(x, y, cell, cell, 12, 12);
            if (real) {
                int st = itemState(ITEM_KEYS[i]);
                if (st == 0) { g.setColor(new Color(90, 98, 150)); g.setFont(new Font("SansSerif", Font.BOLD, 28)); g.drawString("?", x + cell / 2 - 8, y + cell / 2 + 10); }
                else {
                    drawItemIcon(g, i, x + 10, y + 10, 44, st == 2);
                    if (st == 2) { g.setColor(new Color(110, 230, 140)); g.setFont(new Font("SansSerif", Font.BOLD, 14)); g.drawString("✓", x + cell - 19, y + cell - 7); }
                }
            }
            g.setColor(sel ? new Color(255, 215, 120) : new Color(80, 90, 150)); g.setStroke(new BasicStroke(sel ? 3 : 1));
            g.drawRoundRect(x, y, cell, cell, 12, 12); g.setStroke(new BasicStroke(1));
        }

        int dy = gy + 3 * cell + 2 * gap + 14, dh = py + ph - 18 - dy;
        g.setColor(new Color(30, 36, 86)); g.fillRoundRect(px + 20, dy, pw - 40, dh, 14, 14);
        if (invSel < ITEM_KEYS.length) {
            String k = ITEM_KEYS[invSel]; int st = itemState(k);
            if (st == 0) {
                g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.setColor(new Color(160, 170, 220)); g.drawString("???", px + 44, dy + 36);
                g.setFont(new Font("SansSerif", Font.PLAIN, 14)); g.setColor(new Color(255, 255, 255, 200));
                g.drawString("Você ainda não encontrou este item. Continue explorando!", px + 44, dy + 66);
            } else {
                drawItemIcon(g, invSel, px + 36, dy + 18, 64, st == 2);
                g.setFont(new Font("SansSerif", Font.BOLD, 18)); g.setColor(new Color(255, 225, 140)); g.drawString(ITEM_NAMES[invSel], px + 120, dy + 34);
                g.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g.setColor(st == 2 ? new Color(110, 230, 140) : new Color(150, 220, 255));
                g.drawString(st == 2 ? "Usado / entregue" : k.equals("mapa") && stage < 7 ? "Em mãos (incompleto)" : "Em mãos", px + 120, dy + 52);
                boolean mapa = k.equals("mapa");
                g.setFont(new Font("SansSerif", Font.PLAIN, 13)); g.setColor(new Color(255, 255, 255, 230));
                FontMetrics fm = g.getFontMetrics();
                int ty = dy + 76, width = mapa ? pw - 184 - 120 - 24 : pw - 36 - 120, linhas = 0;
                for (String line : wrap(itemDesc(k, st), fm, width)) { if (linhas++ >= 4) break; g.drawString(line, px + 120, ty); ty += 18; }
                if (mapa) drawMapPieces(g, px + pw - 184, dy + 22, 150, 80);
            }
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 11)); g.setColor(new Color(255, 255, 255, 140));
        g.drawString("Setas: escolher   |   Z / ESC / I: fechar", px + 24, py + ph - 4);
    }

    // ---------- Save / Load ----------
    void take(Npc n) { map.npcs.remove(n); taken.add(n.id); }

    String sig() { return stage + "|" + frags + "|" + bye + "|" + brasas + "|" + new TreeSet<>(taken) + "|" + life.signature() + "|" + bonds.signature() + "|" + festival.signature() + "|" + home.signature() + "|" + weather.seed + "|" + chapter.signature() + "|" + journal.clues + "|" + gardening.signature(); }

    void newGame() {
        orders.reset();
        gardening.reset();
        journal.reset();
        worldMap.reset();
        chapter.reset();
        weather.reset();
        home.reset();
        festival.reset();
        bonds.reset();
        life.reset();
        maps.clear(); buildMaps(); taken.clear();
        bonds.buildWorld();
        festival.buildWorld();
        home.buildWorld();
        weather.buildWorld();
        chapter.buildWorld();
        gardening.buildWorld();
        orders.buildWorld();
        map = maps.get("Casa"); tx = 5; ty = 5; dir = 0; fx = 5; fy = 5; sliding = false;
        worldMap.arrive();
        moving = false; prog = 0; stage = 0; frags = 0; time = 8 * 60;
        dlg = null; afterDlg = null; bye = 0; brasas = 0;
        pendingToast = null; toastTxt = ""; toastT = 0; down.clear();
        banner = ""; bannerT = 0; fade = 1; refreshObjects(); lastSig = sig();
    }

    boolean save(boolean showToast) {
        try {
            gardening.sync();
            Properties p = new Properties();
            p.setProperty("versao", "1");
            p.setProperty("mapa", map.name);
            p.setProperty("x", String.valueOf(tx)); p.setProperty("y", String.valueOf(ty));
            p.setProperty("direcao", String.valueOf(dir));
            p.setProperty("tempo", String.valueOf(time));
            p.setProperty("fase", String.valueOf(stage));
            p.setProperty("fragmentos", String.valueOf(frags));
            p.setProperty("despedidas", String.valueOf(bye));
            p.setProperty("braseiros", String.valueOf(brasas));
            p.setProperty("coletados", String.join(",", new TreeSet<>(taken)));
            life.save(p);
            bonds.save(p);
            festival.save(p);
            home.save(p);
            weather.save(p);
            chapter.save(p);
            worldMap.save(p);
            journal.save(p);
            gardening.save(p);
            orders.save(p);
            Path tmp = Paths.get(SAVE + ".tmp");                       // grava num temporário e troca (evita save corrompido)
            try (OutputStream o = Files.newOutputStream(tmp)) { p.store(o, "Aventura em Aurora - save"); }
            Files.move(tmp, Paths.get(SAVE), StandardCopyOption.REPLACE_EXISTING);
            lastSig = sig();
            if (showToast) { toast("Jogo salvo!"); audio.sfx("save"); }
            return true;
        } catch (Exception e) {
            toast("Erro ao salvar: " + e.getMessage());
            return false;
        }
    }

    boolean saveBeforeClose() {
        if (state != TITLE && state != CONFIRM) return save(false);
        return true;
    }

    Properties readSave() {
        File f = new File(SAVE);
        if (!f.exists()) return null;
        try (InputStream in = new FileInputStream(f)) {
            Properties p = new Properties(); p.load(in);
            return "1".equals(p.getProperty("versao")) ? p : null;
        } catch (Exception e) { return null; }
    }

    /** Texto do slot de save na tela de título (ou null se não houver save válido). */
    String readInfo() {
        Properties p = readSave();
        if (p == null) return null;
        try {
            int st = Integer.parseInt(p.getProperty("fase"));
            int dia = (int) (Double.parseDouble(p.getProperty("tempo")) / 1440) + 1;
            return (st < 4 ? "Capítulo 1" : st < 8 ? "Capítulo 2" : st <= 15 ? "Capítulo 3" : st <= 21 ? "Capítulo 4" : "Capítulo 5") + "  •  Dia " + dia + "  •  " + p.getProperty("mapa");
        } catch (Exception e) { return null; }
    }

    boolean loadGame() {
        Properties p = readSave();
        if (p == null) return false;
        try {
            newGame();
            GameMap m = maps.get(p.getProperty("mapa"));
            if (m == null) return false;
            int x = Integer.parseInt(p.getProperty("x")), y = Integer.parseInt(p.getProperty("y"));
            stage = Math.max(0, Math.min(OBJETIVOS.length - 1, Integer.parseInt(p.getProperty("fase"))));
            frags = Integer.parseInt(p.getProperty("fragmentos")) & 7;
            bye = Integer.parseInt(p.getProperty("despedidas", "0")) & 3;           // saves antigos não têm estes campos
            brasas = Math.max(0, Math.min(3, Integer.parseInt(p.getProperty("braseiros", "0"))));
            time = Double.parseDouble(p.getProperty("tempo"));
            dir = Math.max(0, Math.min(3, Integer.parseInt(p.getProperty("direcao", "0"))));
            for (String id : p.getProperty("coletados", "").split(",")) {
                if (id.isBlank()) continue;
                taken.add(id);
                for (GameMap mm : maps.values()) mm.npcs.removeIf(n -> n.id.equals(id));
            }
            if (m.solid(x, y)) { m = maps.get("Casa"); x = 5; y = 5; }   // posição inválida: volta para casa
            map = m; tx = x; ty = y; resetFollower();
            life.load(p);
            bonds.load(p);
            festival.load(p);
            orders.load(p);
            home.load(p);
            weather.load(p);
            chapter.load(p);
            worldMap.load(p);
            journal.load(p);
            gardening.load(p);
            gardening.sync();
            refreshObjects();
            lastSig = sig();
            return true;
        } catch (Exception e) { return false; }
    }

    void toast(String t) { toastTxt = t; toastT = 2.2; }

    // ---------- Menus ----------
    void openTitle() {
        saveInfo = readInfo();
        opts.clear();
        if (saveInfo != null) opts.add("Continuar");
        opts.add("Novo jogo"); opts.add("Sair");
        menuSel = 0; state = TITLE;
    }
    void openPause() {
        opts.clear();
        opts.addAll(List.of("Voltar ao jogo", "Itens", "Álbum de pesca", "Laços de Aurora", "Salvar jogo",
            "Música: " + (audio.musicOn ? "ligada" : "desligada"),
            "Efeitos: " + (audio.sfxOn ? "ligados" : "desligados"),
            "Salvar e ir ao título"));
        if (map.name.equals("Casa")) opts.add(4, "Decorar casa");
        opts.add(4, "Previsão do tempo");
        opts.add(4, "Mapa de Aurora");
        opts.add(4, "Diário de Aventuras");
        opts.add(4, "Volumes de áudio");
        state = MENU;
    }
    void openConfirm() { opts.clear(); opts.addAll(List.of("Não, voltar", "Sim, apagar e começar")); menuSel = 0; state = CONFIRM; }
    void openItems(int from) { invReturn = from; invSel = 0; state = INV; }

    void menuKey(int k) {
        boolean act = k == KeyEvent.VK_Z || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE;
        if (state == INV) {                                          // caixa de itens: grade 4 colunas
            int cnt = ITEM_KEYS.length, antes = invSel;
            if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) invSel = (invSel + cnt - 1) % cnt;
            else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) invSel = (invSel + 1) % cnt;
            else if ((k == KeyEvent.VK_UP || k == KeyEvent.VK_W) && invSel >= 4) invSel -= 4;
            else if ((k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) && invSel + 4 < cnt) invSel += 4;
            else if (act || k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_I) { audio.sfx("back"); state = invReturn; return; }
            if (invSel != antes) audio.sfx("select");
            return;
        }
        int n = opts.size(), old = menuSel;
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) menuSel = (menuSel + n - 1) % n;
        else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) menuSel = (menuSel + 1) % n;
        else if (act) { choose(opts.get(menuSel)); return; }
        else if (k == KeyEvent.VK_ESCAPE) {
            if (state == MENU) { audio.sfx("back"); state = PLAY; }
            else if (state == CONFIRM) { audio.sfx("back"); openTitle(); }
        }
        if (menuSel != old) audio.sfx("select");
    }

    void choose(String o) {
        if (!o.equals("Sair")) audio.sfx("confirm");
        if (o.startsWith("Música")) { audio.setMusic(!audio.musicOn); openPause(); return; }
        if (o.startsWith("Efeitos")) { audio.setSfx(!audio.sfxOn); openPause(); return; }
        switch (o) {
            case "Continuar" -> { if (loadGame()) state = PLAY; else toast("Não foi possível carregar o save."); }
            case "Novo jogo" -> { if (saveInfo != null) openConfirm(); else { newGame(); state = PLAY; } }
            case "Sim, apagar e começar" -> { try { Files.deleteIfExists(Paths.get(SAVE)); } catch (Exception e) { } newGame(); state = PLAY; }
            case "Não, voltar" -> openTitle();
            case "Sair" -> System.exit(0);
            case "Voltar ao jogo" -> state = PLAY;
            case "Itens" -> openItems(MENU);
            case "Álbum de pesca" -> life.openAlbum(MENU);
            case "Laços de Aurora" -> bonds.openJournal(MENU);
            case "Decorar casa" -> home.openDecor(MENU);
            case "Previsão do tempo" -> weather.open(MENU);
            case "Mapa de Aurora" -> worldMap.open(MENU);
            case "Diário de Aventuras" -> journal.open(MENU);
            case "Volumes de áudio" -> soundscape.open();
            case "Salvar jogo" -> save(true);
            case "Salvar e ir ao título" -> { if (save(false)) openTitle(); }
            default -> {}
        }
    }

    // ---------- Input ----------
    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (!down.add(k)) return; // ignora auto-repeat
        if (state == WORLD_MAP) { worldMap.key(k); return; }
        if (state == JOURNAL) { journal.key(k); return; }
        if (state == SOUND) { soundscape.key(k); return; }
        if (state == GARDENING) { gardening.key(k); return; }
        if (state == ORDERS) { orders.key(k); return; }
        if (state == WEATHER) { weather.key(k); return; }
        if (state == HOME) { home.key(k); return; }
        if (state == FESTIVAL) { festival.key(k); return; }
        if (state == BONDS) { bonds.key(k); return; }
        if (state == FISHING || state == ALBUM || state == BELLS) { life.key(k); return; }
        if (state != PLAY) { menuKey(k); return; }
        if (k == KeyEvent.VK_ESCAPE && dlg == null && !moving) { menuSel = 0; audio.sfx("select"); openPause(); return; }
        if (k == KeyEvent.VK_I && dlg == null && !moving) { audio.sfx("select"); openItems(PLAY); return; }
        if (k == KeyEvent.VK_P && dlg == null && !moving) { life.openAlbum(PLAY); return; }
        if (k == KeyEvent.VK_J && dlg == null && !moving) { bonds.openJournal(PLAY); return; }
        if (k == KeyEvent.VK_G && dlg == null && !moving) { bonds.openGift(); return; }
        if (k == KeyEvent.VK_H && dlg == null && !moving) { home.openDecor(PLAY); return; }
        if (k == KeyEvent.VK_F && dlg == null && !moving) { weather.open(PLAY); return; }
        if (k == KeyEvent.VK_TAB && dlg == null && !moving) { worldMap.open(PLAY); return; }
        if (k == KeyEvent.VK_L && dlg == null && !moving) { journal.open(PLAY); return; }
        if (k == KeyEvent.VK_V && dlg == null && !moving) { gardening.openBasket(); return; }
        if (k == KeyEvent.VK_M) { audio.setMusic(!audio.musicOn); toast(audio.musicOn ? "Música ligada" : "Música desligada"); return; }
        boolean action = k == KeyEvent.VK_Z || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE;
        if (action) {
            if (dlg != null) {
                audio.sfx("blip");
                if (++dlgIdx >= dlg.length) {
                    dlg = null;
                    if (pendingToast != null) { toast(pendingToast); audio.sfx("pickup"); pendingToast = null; }
                    if (afterDlg != null) { Runnable r = afterDlg; afterDlg = null; r.run(); }
                    if (!sig().equals(lastSig)) save(false);
                }
            }
            else if (!moving) interact();
        }
        if (k == KeyEvent.VK_T && !festival.fishing) time += 120;
    }
    @Override public void keyReleased(KeyEvent e) { down.remove(e.getKeyCode()); }
    @Override public void keyTyped(KeyEvent e) {}

    // ---------- Desenho ----------
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, W, H);
        if (state == TITLE || state == CONFIRM) { drawTitle(g); drawToast(g); return; }

        double mv = moving ? prog : 0;
        double ppx = (tx + (nx - tx) * mv) * T, ppy = (ty + (ny - ty) * mv) * T;
        int camX = map.w * T <= W ? -(W - map.w * T) / 2 : (int) Math.max(0, Math.min(map.w * T - W, ppx + T / 2.0 - W / 2.0));
        int camY = map.h * T <= H ? -(H - map.h * T) / 2 : (int) Math.max(0, Math.min(map.h * T - H, ppy + T / 2.0 - H / 2.0));

        // tiles
        for (int y = Math.max(0, camY / T); y < Math.min(map.h, (camY + H) / T + 1); y++)
            for (int x = Math.max(0, camX / T); x < Math.min(map.w, (camX + W) / T + 1); x++)
                tile(g, map, x, y, x * T - camX, y * T - camY);

        festival.drawDecorations(g, camX, camY);
        weather.drawShelter(g, camX, camY);
        chapter.drawWorld(g, camX, camY);
        home.drawFurniture(g, camX, camY, 0);
        // entidades (ordenadas por y)
        home.drawFurniture(g, camX, camY, 1);
        for (Npc n : map.npcs) if (vis(n) && n.y <= ty) drawNpc(g, n, camX, camY);
        person(g, "player", (int) ppx - camX, (int) ppy - camY - 4, new Color(220, 50, 50), dir, moving, prog, true);
        if (stage == 20) drawFollower(g, camX, camY, mv);
        for (Npc n : map.npcs) if (vis(n) && n.y > ty) drawNpc(g, n, camX, camY);
        home.drawFurniture(g, camX, camY, 2);

        // dia/noite
        if (map.snowy) drawSnow(g);
        if (map.dark) {
            int sx = (int) ppx - camX + T / 2, sy = (int) ppy - camY + T / 2;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(sx, sy), stage >= 16 ? 210f : 150f, new float[]{0f, 1f},
                new Color[]{new Color(5, 5, 20, 10), new Color(5, 5, 20, 238)}));
            g.fillRect(0, 0, W, H);
            for (Npc n : map.npcs) {                                       // chamas e altar iluminam o ambiente
                boolean fogo = "brasa_on".equals(n.sprite);
                if (!vis(n) || !(fogo || "altar".equals(n.sprite))) continue;
                Color luz = fogo ? new Color(255, 170, 70) : new Color(130, 235, 255);
                float bx = n.x * T + T / 2f - camX, by = n.y * T + T / 2f - camY;
                g.setPaint(new RadialGradientPaint(new Point2D.Float(bx, by), 120, new float[]{0f, 1f},
                    new Color[]{new Color(luz.getRed(), luz.getGreen(), luz.getBlue(), 150), new Color(luz.getRed(), luz.getGreen(), luz.getBlue(), 0)}));
                g.fillRect(0, 0, W, H);
            }
        } else if (map.outdoor) {
            double n = night();
            int sx = (int) ppx - camX + T / 2, sy = (int) ppy - camY + T / 2;
            if (n > 0) {
                g.setPaint(new RadialGradientPaint(new Point2D.Float(sx, sy), 190, new float[]{0f, 1f},
                    new Color[]{new Color(10, 20, 70, (int) (n * 50)), new Color(10, 20, 70, (int) (n * 175))}));
                g.fillRect(0, 0, W, H);
            }
            if (n > 0 && n < 1) { // entardecer / amanhecer
                g.setColor(new Color(255, 120, 40, (int) (Math.sin(Math.PI * n) * 60)));
                g.fillRect(0, 0, W, H);
            }
            if (n > 0 && stage >= 4 && map.name.equals("Praia Azul")) { // farol aceso
                float lx = 22 * T + T / 2f - camX, ly = 3 * T + T / 2f - camY;
                g.setPaint(new RadialGradientPaint(new Point2D.Float(lx, ly), stage >= 15 ? 260f : 150f, new float[]{0f, 1f},
                    new Color[]{new Color(255, 230, 120, (int) (n * 190)), new Color(255, 230, 120, 0)}));
                g.fillRect(0, 0, W, H);
            }
        }

        if (stage == 20) followerGlow(g, camX, camY, mv);
        if (stage >= 21 && map.name.equals("Praia Azul") && night() > 0) {      // a Cintila brilha no céu, sobre o farol
            double nn = night();
            int sx = (int) (22.5 * T) - camX + 100, sy = Math.max(78, (int) (1.1 * T) - camY);   // sempre à vista, ao lado da torre
            double pulse = 0.75 + 0.25 * Math.sin(frame / 14.0);
            g.setPaint(new RadialGradientPaint(new Point2D.Float(sx, sy), 70, new float[]{0f, 1f},
                new Color[]{new Color(255, 240, 150, (int) (200 * nn * pulse)), new Color(255, 240, 150, 0)}));
            g.fillRect(sx - 70, sy - 70, 140, 140);
            g.setColor(new Color(255, 250, 210, (int) (255 * nn)));
            g.fillRect(sx - 1, sy - 9, 3, 19); g.fillRect(sx - 9, sy - 1, 19, 3); g.fillRect(sx - 3, sy - 3, 7, 7);
        }
        if (stage >= 15 && map.name.equals("Praia Azul") && night() > 0) {   // final: a baía brilha de estrelas
            double nn = night();
            Random rr = new Random(42);
            for (int i = 0; i < 70; i++) {
                int sx = rr.nextInt(map.w * T) - camX, sy = 14 * T + rr.nextInt(8 * T) - camY;
                int a = (int) ((Math.sin(frame / 20.0 + i * 1.7) * 0.5 + 0.5) * nn * 230);
                g.setColor(new Color(255, 245, 170, a));
                g.fillRect(sx, sy, 3, 3); g.fillRect(sx - 2, sy + 1, 7, 1); g.fillRect(sx + 1, sy - 2, 1, 7);
            }
        }
        weather.drawWorld(g);
        hud(g);
        if (dlg != null) dialogBox(g);
        if (fade > 0) { g.setColor(new Color(0, 0, 0, (int) (Math.min(1, fade) * 255))); g.fillRect(0, 0, W, H); }
        if (state == MENU) drawMenu(g);
        if (state == INV) drawInventory(g);
        if (state == FISHING || state == ALBUM || state == BELLS) life.draw(g);
        if (state == BONDS) bonds.draw(g);
        if (state == FESTIVAL) festival.draw(g);
        if (state == HOME) home.draw(g);
        if (state == WEATHER) weather.drawForecast(g);
        if (state == WORLD_MAP) worldMap.draw(g);
        if (state == JOURNAL) journal.draw(g);
        if (state == SOUND) soundscape.draw(g);
        if (state == GARDENING) gardening.draw(g);
        if (state == ORDERS) orders.draw(g);
        festival.drawTimer(g);
        drawToast(g);
    }

    void center(Graphics2D g, String s, int y) { g.drawString(s, (W - g.getFontMetrics().stringWidth(s)) / 2, y); }

    void drawTitle(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(12, 16, 52), 0, H, new Color(255, 150, 90)));
        g.fillRect(0, 0, W, H);
        Random r = new Random(5);
        for (int i = 0; i < 70; i++) {                                  // estrelas piscando
            int x = r.nextInt(W), y = r.nextInt(H * 2 / 3), sz = (frame / 25 + i) % 7 == 0 ? 3 : 2;
            g.setColor(new Color(255, 255, 255, 90 + r.nextInt(150)));
            g.fillRect(x, y, sz - 1, sz - 1);
        }
        g.setColor(new Color(250, 245, 210)); g.fillOval(520, 46, 44, 44);
        g.setColor(new Color(30, 58, 74)); g.fillOval(-140, 360, 560, 320);
        g.setColor(new Color(22, 44, 58)); g.fillOval(260, 390, 620, 330);
        g.setColor(new Color(235, 235, 240)); g.fillRect(556, 318, 22, 76);   // farol
        g.setColor(new Color(205, 60, 55)); g.fillRect(556, 338, 22, 10); g.fillRect(556, 360, 22, 10);
        g.setColor(new Color(60, 60, 75)); g.fillRect(552, 310, 30, 8);
        g.setColor(new Color(255, 235, 130)); g.fillRect(560, 300, 14, 10);
        g.setPaint(new RadialGradientPaint(new Point2D.Float(567, 305), 90, new float[]{0f, 1f},
            new Color[]{new Color(255, 235, 130, 150), new Color(255, 235, 130, 0)}));
        g.fillRect(450, 215, 230, 180);

        g.setFont(new Font("Serif", Font.BOLD, 46));
        g.translate(3, 3); g.setColor(new Color(0, 0, 0, 130)); center(g, "Aventura em Aurora", 120); g.translate(-3, -3);
        g.setColor(new Color(255, 240, 200)); center(g, "Aventura em Aurora", 120);
        g.setFont(new Font("Serif", Font.ITALIC, 17));
        g.setColor(new Color(255, 255, 255, 200)); center(g, "uma pequena história de exploração", 150);

        int y0 = 235;
        if (state == CONFIRM) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 15)); g.setColor(new Color(255, 255, 255, 230));
            center(g, "Já existe um jogo salvo. Apagar e começar de novo?", 208);
        }
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        for (int i = 0; i < opts.size(); i++) {
            boolean sel = i == menuSel;
            g.setColor(sel ? new Color(255, 225, 120) : new Color(255, 255, 255, 215));
            center(g, (sel ? "▶  " : "") + opts.get(i), y0 + i * 38);
        }
        if (state == TITLE && saveInfo != null && opts.get(menuSel).equals("Continuar")) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 14)); g.setColor(new Color(255, 240, 170));
            center(g, saveInfo, y0 + opts.size() * 38 + 6);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 12)); g.setColor(new Color(255, 255, 255, 150));
        center(g, "Setas: mover   |   Z / Enter: escolher   |   ESC: voltar", H - 14);
    }

    void drawMenu(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 130)); g.fillRect(0, 0, W, H);
        int step = Math.min(38, (H - 100) / Math.max(1, opts.size()));
        int bw = 300, bh = 70 + opts.size() * step, bx = (W - bw) / 2, by = (H - bh) / 2;
        g.setColor(new Color(255, 255, 255, 242)); g.fillRoundRect(bx, by, bw, bh, 18, 18);
        g.setColor(new Color(40, 40, 60)); g.setStroke(new BasicStroke(4)); g.drawRoundRect(bx, by, bw, bh, 18, 18); g.setStroke(new BasicStroke(1));
        g.setFont(new Font("SansSerif", Font.BOLD, 18)); center(g, "Pausa", by + 34);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        for (int i = 0; i < opts.size(); i++) {
            boolean sel = i == menuSel;
            g.setColor(sel ? new Color(200, 60, 50) : new Color(40, 40, 60));
            g.drawString((sel ? "▶ " : "    ") + opts.get(i), bx + 36, by + 72 + i * step);
        }
    }

    void drawToast(Graphics2D g) {
        if (toastT <= 0) return;
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        int tw = g.getFontMetrics().stringWidth(toastTxt) + 28, a = (int) (Math.min(1, toastT) * 230);
        g.setColor(new Color(20, 20, 40, a)); g.fillRoundRect(W - tw - 12, H - 44, tw, 28, 14, 14);
        g.setColor(new Color(255, 255, 255, a)); g.drawString(toastTxt, W - tw + 2, H - 25);
    }

    void drawSnow(Graphics2D g) {                                          // neve caindo (determinística, sem estado)
        for (int i = 0; i < 110; i++) {
            double fall = 0.6 + (i % 5) * 0.25, x = ((i * 53) % W + frame * (0.3 + (i % 4) * 0.1) + Math.sin(frame / 40.0 + i) * 12 + W) % W;
            double y = (((i * 7919) % 1000) / 1000.0 * H + frame * fall) % H;
            int sz = 1 + (i % 3 == 0 ? 1 : 0);
            g.setColor(new Color(255, 255, 255, 150 + (i % 4) * 25));
            g.fillRect((int) x, (int) y, sz, sz);
        }
    }

    void drawFollower(Graphics2D g, int camX, int camY, double mv) {
        double px = (fx + (tx - fx) * mv) * T, py = (fy + (ty - fy) * mv) * T;
        int bob = (int) (Math.sin(frame / 12.0) * 3), x = (int) px - camX, y = (int) py - camY - 10 + bob;
        BufferedImage im = spr.get("cintila");
        if (im != null) g.drawImage(im, x, y, T, T, null);
        else { g.setColor(new Color(255, 230, 110)); g.fillOval(x + 6, y + 6, 20, 20); }
    }

    void followerGlow(Graphics2D g, int camX, int camY, double mv) {
        double px = (fx + (tx - fx) * mv) * T, py = (fy + (ty - fy) * mv) * T;
        float cx = (float) px + T / 2f - camX, cy = (float) py + T / 2f - camY - 10;
        g.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), 80, new float[]{0f, 1f},
            new Color[]{new Color(255, 240, 150, 120), new Color(255, 240, 150, 0)}));
        g.fillRect((int) cx - 80, (int) cy - 80, 160, 160);
    }

    void hud(Graphics2D g) {
        int hh = (int) hour(), mm = (int) ((hour() - hh) * 60);
        int dia = (int) (time / 1440) + 1;
        String obj = "Objetivo: " + OBJETIVOS[stage] + (stage == 6 ? " (" + Integer.bitCount(frags) + "/3)" : stage == 9 ? " (" + Integer.bitCount(bye) + "/2)" : stage == 12 ? " (" + brasas + "/3)" : stage == 23 ? " (" + Integer.bitCount(chapter.seeds) + "/3)" : "");
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String heading = String.format("%s  •  Dia %d  %02d:%02d  •  %s", map.name, dia, hh, mm, weather.label());
        int bw = Math.max(280, g.getFontMetrics().stringWidth(obj) + 32);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        bw = Math.max(bw, g.getFontMetrics().stringWidth(heading) + 56);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(8, 8, bw, 46, 12, 12);
        g.setColor(isNight() ? new Color(200, 210, 255) : new Color(255, 220, 80)); // sol / lua
        g.fillOval(16, 16, 14, 14);
        if (isNight()) { g.setColor(new Color(0, 0, 0, 200)); g.fillOval(21, 14, 14, 14); }
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString(heading, 40, 27);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(255, 240, 170));
        g.drawString(obj, 16, 46);
        if (dlg == null && state == PLAY) {
            g.setColor(new Color(255, 255, 255, 170));
            g.drawString("Setas: mover | Z: falar | I: itens | ESC: menu | M: música", 12, H - 10);
        }
        if (bannerT > 0) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            int tw = g.getFontMetrics().stringWidth(banner) + 40, a = (int) (Math.min(1, bannerT) * 220);
            g.setColor(new Color(20, 20, 40, a));
            g.fillRoundRect((W - tw) / 2, 66, tw, 34, 16, 16);
            g.setColor(new Color(255, 230, 140, Math.min(255, a + 35)));
            g.drawString(banner, (W - tw) / 2 + 20, 88);
        }
    }

    void dialogBox(Graphics2D g) {
        int bx = 20, by = H - 130, bw = W - 40, bh = 110;
        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(bx, by, bw, bh, 18, 18);
        g.setColor(new Color(40, 40, 60));
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(bx, by, bw, bh, 18, 18);
        g.setStroke(new BasicStroke(1));
        if (!speaker.isEmpty()) {
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(40, 40, 60));
            g.fillRoundRect(bx + 14, by - 14, g.getFontMetrics().stringWidth(speaker) + 24, 24, 10, 10);
            g.setColor(Color.WHITE);
            g.drawString(speaker, bx + 26, by + 3);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.setColor(new Color(30, 30, 40));
        FontMetrics fm = g.getFontMetrics();
        int ly = by + 38;
        for (String line : wrap(dlg[dlgIdx], fm, bw - 50)) { g.drawString(line, bx + 24, ly); ly += 24; }
        if ((frame / 25) % 2 == 0) g.drawString("▼", bx + bw - 36, by + bh - 14);
    }

    List<String> wrap(String s, FontMetrics fm, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : s.split(" ")) {
            if (cur.length() > 0 && fm.stringWidth(cur + " " + w) > maxW) { out.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(w);
        }
        out.add(cur.toString());
        return out;
    }

    void person(Graphics2D g, String id, int x, int y, Color body, int d, boolean walking, double p, boolean human) {
        BufferedImage sheet = spr.get(id);
        if (sheet != null) {
            if (!human) { g.drawImage(sheet, x, y + 4 + (int) (Math.sin(frame / 15.0) * 2), T, T, null); return; }   // objeto (concha): imagem única
            g.setColor(new Color(0, 0, 0, 60));
            g.fillOval(x + 6, y + 28, 20, 7);
            int fw = sheet.getWidth() / 3, fh = sheet.getHeight() / 4;           // 3 colunas x 4 linhas
            int col = walking ? (p < 0.5 ? 1 : 2) : 0;
            g.drawImage(sheet.getSubimage(col * fw, d * fh, fw, fh), x, y + 2, T, T, null);
            return;
        }
        if (!human) { // objeto brilhante (concha)
            g.setColor(new Color(255, 230, 120, 90));
            g.fillOval(x + 2, y + 6, 28, 28);
            g.setColor(body);
            g.fillOval(x + 9, y + 14, 14, 14);
            g.setColor(Color.WHITE);
            g.fillOval(x + 12, y + 16, 4, 4);
            return;
        }
        int bob = walking ? (int) (Math.sin(p * Math.PI * 2) * 2) : 0;
        g.setColor(new Color(0, 0, 0, 60));
        g.fillOval(x + 6, y + 28, 20, 7);
        g.setColor(new Color(50, 50, 80)); // pernas
        int step = walking ? (int) (Math.sin(p * Math.PI * 2) * 3) : 0;
        g.fillRect(x + 10, y + 24 + bob, 5, 6 + step / 2);
        g.fillRect(x + 17, y + 24 + bob, 5, 6 - step / 2);
        g.setColor(body);
        g.fillRoundRect(x + 8, y + 14 + bob, 16, 13, 6, 6);
        g.setColor(new Color(245, 205, 165));
        g.fillOval(x + 9, y + 3 + bob, 14, 13);
        g.setColor(body.darker());
        g.fillArc(x + 8, y + 1 + bob, 16, 12, 0, 180);
        g.setColor(Color.BLACK);
        switch (d) {
            case 0 -> { g.fillOval(x + 12, y + 9 + bob, 3, 3); g.fillOval(x + 18, y + 9 + bob, 3, 3); }
            case 2 -> g.fillOval(x + 10, y + 9 + bob, 3, 3);
            case 3 -> g.fillOval(x + 19, y + 9 + bob, 3, 3);
            default -> {}
        }
    }

    Color ground(GameMap m) { return m.name.equals("Praia Azul") ? new Color(232, 212, 150) : m.snowy ? new Color(226, 236, 246) : new Color(92, 168, 72); }

    void tile(Graphics2D g, GameMap m, int tx, int ty, int x, int y) {
        char c = m.t[ty][tx];
        int h = tx * 7 + ty * 13;
        if (!spr.isEmpty() && spriteTile(g, m, c, tx, ty, x, y)) return;
        switch (c) {
            case '.', 'F', '#' -> {
                g.setColor(ground(m)); g.fillRect(x, y, T, T);
                if (h % 5 == 0) { g.setColor(new Color(70, 140, 56)); g.drawLine(x + 8, y + 20, x + 10, y + 14); g.drawLine(x + 11, y + 20, x + 13, y + 14); }
                if (c == 'F') {
                    g.setColor(h % 2 == 0 ? new Color(255, 150, 190) : new Color(255, 230, 90)); g.fillOval(x + 9, y + 11, 5, 5);
                    g.setColor(Color.WHITE); g.fillOval(x + 20, y + 20, 5, 5);
                }
                if (c == '#') {
                    g.setColor(new Color(0, 0, 0, 40)); g.fillOval(x + 4, y + 22, 26, 8);
                    g.setColor(new Color(110, 75, 40)); g.fillRect(x + 13, y + 19, 6, 11);
                    g.setColor(new Color(40, 120, 50)); g.fillOval(x + 1, y + 1, 30, 26);
                    g.setColor(new Color(62, 152, 66)); g.fillOval(x + 7, y + 5, 11, 8);
                }
            }
            case '~' -> {
                g.setColor(new Color(52, 110, 200)); g.fillRect(x, y, T, T);
                g.setColor(new Color(130, 180, 240));
                int o = (int) ((frame / 15 + tx + ty) % 3) * 4;
                g.drawArc(x + 4 + o, y + 8, 10, 6, 0, 180);
                g.drawArc(x + 14 - o, y + 22, 10, 6, 0, 180);
            }
            case 's' -> {
                g.setColor(new Color(232, 212, 150)); g.fillRect(x, y, T, T);
                if (h % 3 == 0) { g.setColor(new Color(210, 188, 125)); g.fillOval(x + 9, y + 14, 3, 3); g.fillOval(x + 21, y + 6, 3, 3); }
            }
            case '=' -> {
                g.setColor(new Color(205, 175, 115)); g.fillRect(x, y, T, T);
                if (h % 3 == 0) { g.setColor(new Color(180, 150, 95)); g.fillOval(x + 8, y + 10, 5, 4); }
            }
            case 'f' -> {
                g.setColor(new Color(205, 155, 100)); g.fillRect(x, y, T, T);
                g.setColor(new Color(170, 120, 75)); g.drawLine(x, y + 15, x + T, y + 15); g.drawLine(x + 15 + (ty % 2) * 6, y, x + 15 + (ty % 2) * 6, y + 15);
            }
            case 'X' -> {
                g.setColor(new Color(110, 75, 55)); g.fillRect(x, y, T, T);
                g.setColor(new Color(80, 52, 38)); g.drawRect(x, y, T - 1, T - 1); g.drawLine(x, y + 16, x + T, y + 16);
            }
            case 'H' -> {
                boolean roof = m.at(tx, ty - 1) != 'H';
                if (roof) {
                    g.setColor(new Color(190, 70, 60)); g.fillRect(x, y, T, T);
                    g.setColor(new Color(150, 50, 45));
                    for (int i = 6; i < T; i += 8) g.drawLine(x, y + i, x + T, y + i);
                } else {
                    g.setColor(new Color(240, 230, 205)); g.fillRect(x, y, T, T);
                    int k = 0; while (m.at(tx - k - 1, ty) == 'H') k++;
                    if (m.at(tx, ty - 2) != 'H' && (k == 1 || k == 3)) {
                        g.setColor(isNight() ? new Color(255, 220, 120) : new Color(130, 190, 230));
                        g.fillRect(x + 6, y + 8, 20, 16);
                        g.setColor(new Color(90, 70, 50)); g.drawRect(x + 6, y + 8, 20, 16); g.drawLine(x + 16, y + 8, x + 16, y + 24);
                    }
                }
            }
            case 'D' -> {
                g.setColor(m.outdoor ? new Color(240, 230, 205) : new Color(205, 155, 100)); g.fillRect(x, y, T, T);
                g.setColor(new Color(120, 75, 40)); g.fillRect(x + 5, y + 2, 22, T - 2);
                g.setColor(new Color(240, 200, 80)); g.fillOval(x + 21, y + 17, 4, 4);
            }
            case 'L' -> {
                boolean top = m.at(tx, ty - 1) != 'L';
                boolean mid = m.at(tx - 1, ty) == 'L' && m.at(tx + 1, ty) == 'L';
                g.setColor(ty % 2 == 0 ? new Color(205, 60, 55) : new Color(245, 245, 245)); g.fillRect(x, y, T, T);
                if (top) { g.setColor(new Color(60, 60, 75)); g.fillRect(x, y, T, T); }
                if (top && mid) { g.setColor(new Color(255, 235, 130)); g.fillOval(x + 6, y + 6, 20, 20); }
            }
            case 'n' -> { g.setColor(new Color(226, 236, 246)); g.fillRect(x, y, T, T); if (h % 4 == 0) { g.setColor(new Color(205, 220, 236)); g.fillRect(x + 8, y + 12, 5, 3); } }
            case 'i' -> {
                g.setColor(new Color(170, 225, 245)); g.fillRect(x, y, T, T);
                g.setColor(new Color(218, 243, 253)); g.drawLine(x + 5, y + 24, x + 14, y + 15); g.drawLine(x + 17, y + 14, x + 25, y + 7);
            }
            case 'P' -> {
                g.setColor(new Color(150, 105, 60)); g.fillRect(x, y, T, T);
                g.setColor(new Color(105, 72, 40)); for (int i = 6; i < T; i += 8) g.drawLine(x, y + i, x + T, y + i);
            }
            case 'm' -> {
                g.setColor(new Color(120, 128, 150)); g.fillRect(x, y, T, T);
                g.setColor(new Color(90, 98, 120)); g.drawRect(x, y, T - 1, T - 1);
                g.setColor(new Color(190, 200, 235)); g.fillRect(x + 14, y + 10, 4, 12); g.fillRect(x + 10, y + 14, 12, 4);
            }
            case 'R', 'c', 'O' -> {
                g.setColor(c == 'c' ? new Color(62, 56, 70) : new Color(84, 78, 90)); g.fillRect(x, y, T, T);
                g.setColor(c == 'c' ? new Color(48, 44, 56) : new Color(110, 104, 118));
                g.fillRect(x + (h % 4) * 6, y + (h % 3) * 8 + 4, 5, 3);
                if (c == 'O') { g.setColor(new Color(15, 12, 20)); g.fillArc(x + 2, y + 4, 28, 56, 0, 180); }
            }
            default -> {}
        }
    }

    // ---------- main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Aventura em Aurora (demo)");
            Game game = new Game();
            f.add(game);
            f.pack();
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    if (game.saveBeforeClose()) System.exit(0);
                }
            });
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
