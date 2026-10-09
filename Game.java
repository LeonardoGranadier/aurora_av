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
    static final int ACCESSIBILITY = 17, HANDOFF = 18;
    static final int NAMING = 19, CARD = 20, MAIL = 21, CREDITS = 22, TRADE = 23, OBSERVATORY = 24, CALENDAR = 25, COOKING = 26, SONG = 27;
    static final String VERSION = "1.0.0";
    /** Atalhos de desenvolvimento (T avança 2h): java -Daurora.dev=true Game. jogar.sh já liga. */
    static final boolean DEV = Boolean.getBoolean("aurora.dev");
    static final String SAVE = GameFiles.data("savegame.properties").toString(), BACKUP = SAVE + ".bak";

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
    final Accessibility access = new Accessibility(this);
    final Inventory inventory = new Inventory(this);
    final Story story = new Story(this);
    final SecretShells shells = new SecretShells(this);
    final TradeQuest trades = new TradeQuest(this);
    final Observatory observatory = new Observatory(this);
    final Mailbox mail = new Mailbox(this);
    final Explorer explorer = new Explorer(this);
    final Credits credits = new Credits(this);
    final ChapterSix chapterSix = new ChapterSix(this);
    final ChapterSeven chapterSeven = new ChapterSeven(this);
    final Seasons seasons = new Seasons(this);
    final Cooking cooking = new Cooking(this);
    final VillageCat cat = new VillageCat(this);
    /** Sistemas ligados pela interface Feature, na ordem em que recebem cada gancho. */
    final List<Feature> features = List.of(chapterSix, chapterSeven, shells, trades, observatory, mail, explorer, credits, seasons, cooking, cat);
    double dlgChars;                // letras já mostradas na página atual do diálogo (efeito de máquina de escrever)
    String itemGetKey; double itemGetT;   // pose de "item obtido" sobre a cabeça do jogador
    boolean photo;                  // desenhando uma foto: sem HUD, diálogos nem avisos
    final Map<Integer, Integer> pressedKeys = new HashMap<>();

    public Game() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        addKeyListener(this);
        addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { down.clear(); pressedKeys.clear(); }
        });
        loadSprites();
        newGame();
        openTitle();
        new javax.swing.Timer(16, e -> { update(); repaint(); }).start();
    }

    // ---------- Sprites ----------
    void loadSprites() {
        File[] files = GameFiles.assetsDir().listFiles((d, n) -> n.endsWith(".png"));
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
        boolean praia = m.name.equals("Praia Azul"), snowy = m.snowy || seasons.snowCover(m);   // inverno cobre a paisagem
        switch (c) {
            case '.', 'F', '#' -> {
                if (!img(g, praia ? "sand" : snowy ? "snow" : "grass", x, y)) return false;
                if (c == 'F' && !snowy) img(g, "flower", x, y);
                if (c == '#') img(g, snowy ? "pine" : "tree", x, y);
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
    /** Dia da partida contado a partir de 0; o HUD mostra day() + 1. */
    int day() { return (int) Math.floor(time / 1440); }
    /** Dia da semana do dia exibido (1 = domingo); o festival cai sempre no sábado. */
    static final String[] WEEKDAYS = {"Sáb", "Dom", "Seg", "Ter", "Qua", "Qui", "Sex"};
    static String weekday(int shownDay) { return WEEKDAYS[Math.floorMod(shownDay, 7)]; }

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
        if (dlg != null) dlgChars += dt * access.charsPerSecond();
        if (itemGetT > 0) itemGetT -= dt;
        if (!festival.fishing) time += dt * 6; // O concurso usa seu próprio cronômetro.
        gardening.sync();
        for (Feature f : features) f.update(dt);
        if (fade > 0) fade -= dt * 3;
        if (bannerT > 0) bannerT -= dt;
        if (dlg == null) life.updateResidents(dt);

        if (moving) {
            prog += dt * (sliding ? 7.5 : (running() ? 7.5 : 4.5) * cooking.speedFactor());
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
    /** Segurar Shift corre (como os tênis de corrida dos Pokémon). */
    boolean running() { return down.contains(KeyEvent.VK_SHIFT) && !sliding; }
    /** Pontos de amizade de um presente: dobram no aniversário e crescem com o prato "Coração quente". */
    int giftPoints(int who, int base) { return cooking.giftPoints(seasons.giftPoints(who, base)); }

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
        for (Npc n : map.npcs) if (vis(n) && n.x == ax && n.y == ay) { n.dir = OPP[dir]; Set<String> antes = inventory.found(); talk(n); audio.sfx("blip"); inventory.announceNew(antes); if (!sig().equals(lastSig)) save(true); return; }
        if (map.at(ax, ay) == '~') life.cast();
    }

    /** NPC/objeto só aparece na fase e no horário certos. */
    boolean vis(Npc n) {
        if (!chapter.visible(n.id)) return false;
        if (n.id.equals("festival_souvenir") && (festival.prizes == 0 || !home.placed(HomeDecor.STAR))) return false;
        if (!bonds.visible(n.id)) return false;
        for (Feature f : features) if (!f.visible(n)) return false;
        if (n.sleeping) return false;
        if (n.id.equals("portao2") && brasas >= 3) return false;     // portão interno abre com os 3 braseiros
        if (stage < n.minStage || stage > n.maxStage) return false;
        if ("noite".equals(n.when) && !isNight()) return false;
        if ("dia".equals(n.when) && isNight()) return false;
        return true;
    }

    void setStage(int st) {
        stage = st;
        banner = Story.banner(st);
        bannerT = 4;
        if (Story.fanfare(st)) audio.sfx("fanfare");
        if (st == Story.FINAL && !credits.seen) { credits.seen = true; credits.open(PLAY, true); }
        refreshObjects();
    }

    void say(String who, String... lines) { access.dialogPage = 0; afterDlg = null; speaker = who; dlg = lines; dlgIdx = 0; restartReveal(); }
    /** Nova página: com texto instantâneo, já aparece inteira. */
    void restartReveal() { dlgChars = access.textSpeed == 0 ? Double.MAX_VALUE : 0; }

    /** Letras da página atual do diálogo. */
    int pageChars() {
        List<String> lines = access.dialogLines(); int rows = access.large ? 5 : 3, n = 0;
        for (int i = access.dialogPage * rows; i < Math.min(lines.size(), (access.dialogPage + 1) * rows); i++) n += lines.get(i).length();
        return n;
    }

    /** Pose de "item obtido" (como nos Zelda): o jogador vira para a tela e ergue o item por alguns segundos. */
    void itemGet(String key) { itemGetKey = key; itemGetT = 2.2; dir = 0; }

    // ---------- Ações do mundo ----------

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
        for (Feature f : features) if (f.talk(n)) return;
        int previousStage = stage;
        story.talk(n);
        journal.record(n, previousStage);
        bonds.afterTalk(n);
        weather.afterTalk(n);
        festival.afterTalk(n);
        for (Feature f : features) f.afterTalk(n);
    }

    void drawNpc(Graphics2D g, Npc n, int camX, int camY) {
        int x = (int) ((n.x + (n.walking ? (n.toX - n.x) * n.walkProgress : 0)) * T) - camX;
        int y = (int) ((n.y + (n.walking ? (n.toY - n.y) * n.walkProgress : 0)) * T) - camY - 4;
        for (Feature f : features) if (f.drawObject(g, n, x, y)) return;
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
            case ChapterSix.REEF -> n ? "ilha_noite" : "ilha_dia";
            case "Jardim das Constelações" -> "jardim";
            default -> "vila_dia";
        };
    }

    // ---------- Save / Load ----------
    void take(Npc n) { map.npcs.remove(n); taken.add(n.id); }

    String sig() { return stage + "|" + frags + "|" + bye + "|" + brasas + "|" + new TreeSet<>(taken) + "|" + life.signature() + "|" + bonds.signature() + "|" + festival.signature() + "|" + home.signature() + "|" + weather.seed + "|" + chapter.signature() + "|" + journal.clues + "|" + gardening.signature() + featureSignature(); }
    String featureSignature() { StringBuilder b = new StringBuilder(); for (Feature f : features) b.append('|').append(f.signature()); return b.toString(); }

    void newGame() {
        orders.reset();
        for (Feature f : features) f.reset();
        gardening.reset();
        journal.reset();
        worldMap.reset();
        chapter.reset();
        weather.reset();
        home.reset();
        festival.reset();
        bonds.reset();
        life.reset();
        maps.clear(); World.build(this); taken.clear();
        bonds.buildWorld();
        festival.buildWorld();
        home.buildWorld();
        weather.buildWorld();
        chapter.buildWorld();
        gardening.buildWorld();
        orders.buildWorld();
        for (Feature f : features) f.buildWorld();
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
            for (Feature f : features) f.save(p);
            Path tmp = Paths.get(SAVE + ".tmp");                       // grava num temporário e troca (evita save corrompido)
            try (OutputStream o = Files.newOutputStream(tmp)) { p.store(o, "Aventura em Aurora - save"); }
            if (Files.isRegularFile(Paths.get(SAVE)))                          // a versão anterior vira o backup
                Files.copy(Paths.get(SAVE), Paths.get(BACKUP), StandardCopyOption.REPLACE_EXISTING);
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

    /** Partida salva; se o arquivo principal estiver ilegível, tenta o backup da gravação anterior. */
    Properties readSave() {
        if (!new File(SAVE).exists()) return null;
        Properties p = readSaveFile(SAVE);
        return p != null ? p : readSaveFile(BACKUP);
    }
    static Properties readSaveFile(String path) {
        File f = new File(path);
        if (!f.isFile()) return null;
        try (InputStream in = new FileInputStream(f)) {
            Properties p = new Properties(); p.load(in);
            if (!"1".equals(p.getProperty("versao"))) return null;
            Integer.parseInt(p.getProperty("fase")); Double.parseDouble(p.getProperty("tempo"));   // campos essenciais
            return p.getProperty("mapa") != null ? p : null;
        } catch (Exception e) { return null; }
    }

    /** Texto do slot de save na tela de título (ou null se não houver save válido). */
    String readInfo() {
        Properties p = readSave();
        if (p == null) return null;
        try {
            int st = Integer.parseInt(p.getProperty("fase"));
            int dia = (int) (Double.parseDouble(p.getProperty("tempo")) / 1440) + 1;
            return "Capítulo " + Explorer.chapterOf(st) + "  •  Dia " + dia + "  •  " + p.getProperty("mapa");
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
            stage = Math.max(0, Math.min(Story.OBJECTIVES.length - 1, Integer.parseInt(p.getProperty("fase"))));
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
            for (Feature f : features) f.load(p);
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
        opts.add("Novo jogo"); opts.add("Créditos"); opts.add("Sair");
        menuSel = 0; state = TITLE;
    }
    void openPause() {
        opts.clear();
        opts.addAll(List.of("Voltar ao jogo", "Itens", "Cartão do Explorador", "Álbum de pesca", "Laços de Aurora", "Salvar jogo",
            "Música: " + (audio.musicOn ? "ligada" : "desligada"),
            "Efeitos: " + (audio.sfxOn ? "ligados" : "desligados"),
            "Salvar e ir ao título"));
        if (map.name.equals("Casa")) opts.add(5, "Decorar casa");
        opts.add(5, "Previsão do tempo");
        opts.add(5, "Calendário");
        opts.add(3, "Lancheira");
        opts.add(5, "Mapa de Aurora");
        opts.add(5, "Diário de Aventuras");
        opts.add(5, "Volumes de áudio");
        opts.add(5, "Acessibilidade e controles");
        state = MENU;
    }
    void openConfirm() { opts.clear(); opts.addAll(List.of("Não, voltar", "Sim, apagar e começar")); menuSel = 0; state = CONFIRM; }

    void menuKey(int k) {
        boolean act = k == KeyEvent.VK_Z || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE;
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
            case "Continuar" -> {
                if (!loadGame()) { toast("Não foi possível carregar o save."); return; }
                state = PLAY;
                if (explorer.name.isBlank() && !explorer.asked) explorer.askLegacy();   // partidas de antes da tela de nome
            }
            case "Novo jogo" -> { if (saveInfo != null) openConfirm(); else { newGame(); explorer.startNaming(); } }
            case "Créditos" -> credits.open(TITLE, false);
            case "Cartão do Explorador" -> explorer.openCard(MENU);
            case "Calendário" -> seasons.openCalendar(MENU);
            case "Lancheira" -> { cooking.openLunchbox(); cooking.returnState = MENU; }
            case "Sim, apagar e começar" -> { try { Files.deleteIfExists(Paths.get(SAVE)); Files.deleteIfExists(Paths.get(BACKUP)); } catch (Exception e) { toast("Não foi possível apagar a partida."); return; } newGame(); explorer.startNaming(); }
            case "Não, voltar" -> openTitle();
            case "Sair" -> System.exit(0);
            case "Voltar ao jogo" -> state = PLAY;
            case "Itens" -> inventory.open(MENU);
            case "Álbum de pesca" -> life.openAlbum(MENU);
            case "Laços de Aurora" -> bonds.openJournal(MENU);
            case "Decorar casa" -> home.openDecor(MENU);
            case "Previsão do tempo" -> weather.open(MENU);
            case "Mapa de Aurora" -> worldMap.open(MENU);
            case "Diário de Aventuras" -> journal.open(MENU);
            case "Volumes de áudio" -> soundscape.open();
            case "Acessibilidade e controles" -> access.open();
            case "Salvar jogo" -> save(true);
            case "Salvar e ir ao título" -> { if (save(false)) openTitle(); }
            default -> {}
        }
    }

    // ---------- Input ----------
    @Override public void keyPressed(KeyEvent e) {
        int raw = e.getKeyCode();
        if (raw == KeyEvent.VK_F11) { toggleFullscreen(); return; }
        if (raw == KeyEvent.VK_F12) { takePhoto(); return; }
        if (pressedKeys.containsKey(raw)) return;
        int k = access.translate(raw);
        pressedKeys.put(raw, k);
        if (state == ACCESSIBILITY && access.capturing) { access.capture(raw); return; }
        if (k == KeyEvent.VK_UNDEFINED) return;
        if (!down.add(k)) return; // ignora auto-repeat
        if (state == ACCESSIBILITY) { access.key(k); return; }
        if (state == HANDOFF) { access.confirmKey(k); return; }
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
        if (state == INV) { inventory.key(k); return; }
        for (Feature f : features) if (f.owns(state)) { f.key(k); return; }
        if (state != PLAY) { menuKey(k); return; }
        if (k == KeyEvent.VK_ESCAPE && dlg == null && !moving) { menuSel = 0; audio.sfx("select"); openPause(); return; }
        if (k == KeyEvent.VK_I && dlg == null && !moving) { audio.sfx("select"); inventory.open(PLAY); return; }
        if (k == KeyEvent.VK_P && dlg == null && !moving) { life.openAlbum(PLAY); return; }
        if (k == KeyEvent.VK_J && dlg == null && !moving) { bonds.openJournal(PLAY); return; }
        if (k == KeyEvent.VK_G && dlg == null && !moving) { bonds.openGift(); return; }
        if (k == KeyEvent.VK_H && dlg == null && !moving) { home.openDecor(PLAY); return; }
        if (k == KeyEvent.VK_F && dlg == null && !moving) { weather.open(PLAY); return; }
        if (k == KeyEvent.VK_TAB && dlg == null && !moving) { worldMap.open(PLAY); return; }
        if (k == KeyEvent.VK_L && dlg == null && !moving) { journal.open(PLAY); return; }
        if (k == KeyEvent.VK_V && dlg == null && !moving) { gardening.openBasket(); return; }
        if (k == KeyEvent.VK_C && dlg == null && !moving) { audio.sfx("select"); explorer.openCard(PLAY); return; }
        if (k == KeyEvent.VK_K && dlg == null && !moving) { audio.sfx("select"); cooking.openLunchbox(); return; }
        if (k == KeyEvent.VK_M) { audio.setMusic(!audio.musicOn); toast(audio.musicOn ? "Música ligada" : "Música desligada"); return; }
        boolean action = k == KeyEvent.VK_Z || k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE;
        if (action) {
            if (dlg != null) {
                if (dlgChars < pageChars()) { dlgChars = Double.MAX_VALUE; return; }   // primeiro toque mostra a página inteira
                restartReveal();
                if (access.moreDialog()) return;
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
        if (k == KeyEvent.VK_T && DEV && !festival.fishing) time += 120;
    }
    @Override public void keyReleased(KeyEvent e) {
        Integer logical = pressedKeys.remove(e.getKeyCode());
        if (logical != null && !pressedKeys.containsValue(logical)) down.remove(logical);
    }
    @Override public void keyTyped(KeyEvent e) {
        for (Feature f : features) if (f.owns(state)) { f.typed(e.getKeyChar()); return; }
    }

    // ---------- Desenho ----------
    /** Desenha a cena de 640x480 ampliada para caber no painel, centralizada e com faixas pretas. */
    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        long actualFrame = frame;
        try {
            g.setColor(Color.BLACK); g.fillRect(0, 0, getWidth(), getHeight());
            double s = Math.min(getWidth() / (double) W, getHeight() / (double) H);
            if (s >= 1) s = Math.floor(s);                               // escala inteira mantém a pixel art nítida
            if (s <= 0) return;
            g.translate((getWidth() - W * s) / 2, (getHeight() - H * s) / 2);
            g.scale(s, s);
            g.clipRect(0, 0, W, H);
            if (access.reduced) frame = 0;
            paintScene(g);
        } finally { frame = actualFrame; g.dispose(); }
    }

    /** Maior escala inteira que cabe com folga na tela (mínimo 1). */
    static int windowScale() {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        return Math.max(1, Math.min((screen.width - 40) / W, (screen.height - 80) / H));
    }

    /** F11 alterna tela cheia; a cena continua proporcional. */
    void toggleFullscreen() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) return;
        GraphicsDevice device = window.getGraphicsConfiguration().getDevice();
        boolean full = device.getFullScreenWindow() != window;
        device.setFullScreenWindow(full ? window : null);
        requestFocusInWindow();
        Properties video = new Properties(); video.setProperty("telaCheia", "" + full);
        try (OutputStream o = Files.newOutputStream(GameFiles.data("video.properties"))) { video.store(o, "Aventura em Aurora - video"); }
        catch (Exception ignored) {}
    }
    void paintScene(Graphics g0) {
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
        for (Feature f : features) f.drawWorld(g, camX, camY);

        // dia/noite
        if (map.snowy) drawSnow(g);
        if (map.dark) {
            int sx = (int) ppx - camX + T / 2, sy = (int) ppy - camY + T / 2;
            g.setPaint(new RadialGradientPaint(new Point2D.Float(sx, sy), (stage >= 16 ? 210f : 150f) + (cooking.nightVision() ? 70f : 0f), new float[]{0f, 1f},
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
                    new Color[]{new Color(10, 20, 70, (int) (n * 50)), new Color(10, 20, 70, (int) (n * (cooking.nightVision() ? 100 : 175)))}));
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
        drawItemGet(g, (int) ppx - camX, (int) ppy - camY - 4);
        if (!photo && state == PLAY && dlg == null && !moving) drawInteractionHint(g, camX, camY);
        if (photo) return;
        hud(g);
        if (dlg != null) dialogBox(g);
        if (fade > 0 && !access.reduced) { g.setColor(new Color(0, 0, 0, (int) (Math.min(1, fade) * 255))); g.fillRect(0, 0, W, H); }
        if (state == MENU) drawMenu(g);
        if (state == INV) inventory.draw(g);
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
        if (state == ACCESSIBILITY) access.draw(g);
        if (state == HANDOFF) access.drawConfirmation(g);
        for (Feature f : features) if (f.owns(state)) f.draw(g);
        festival.drawTimer(g);
        drawToast(g);
    }

    /** Item erguido sobre a cabeça, com raios de brilho. */
    void drawItemGet(Graphics2D g, int px, int py) {
        if (itemGetT <= 0 || itemGetKey == null) return;
        int cx = px + T / 2, cy = py - 18;
        double a = Math.min(1, itemGetT / 0.4);
        if (!access.reduced) {
            g.setColor(new Color(255, 240, 160, (int) (110 * a)));
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4 + frame / 40.0;
                g.setStroke(new BasicStroke(3));
                g.drawLine(cx + (int) (Math.cos(ang) * 16), cy + (int) (Math.sin(ang) * 16), cx + (int) (Math.cos(ang) * 26), cy + (int) (Math.sin(ang) * 26));
            }
            g.setStroke(new BasicStroke(1));
        }
        int idx = Arrays.asList(Inventory.KEYS).indexOf(itemGetKey);
        if (idx >= 0) inventory.drawIcon(g, idx, cx - 12, cy - 12, 24, false);
        else if (itemGetKey.equals("concha_secreta")) {
            g.setColor(new Color(240, 150, 175)); g.fillArc(cx - 11, cy - 9, 22, 22, 0, 180);
            g.setColor(new Color(200, 100, 130)); for (int i = -8; i <= 8; i += 4) g.drawLine(cx, cy + 2, cx + i, cy - 7);
            g.setColor(new Color(240, 150, 175)); g.fillRect(cx - 4, cy + 1, 8, 5);
        } else {                                                                    // presente da corrente de trocas
            g.setColor(new Color(90, 160, 210)); g.fillRect(cx - 10, cy - 8, 20, 16);
            g.setColor(new Color(240, 200, 80)); g.fillRect(cx - 2, cy - 8, 4, 16); g.fillRect(cx - 10, cy - 2, 20, 4);
        }
    }

    /** Balãozinho sobre o que está à frente e pode ser usado (sem entregar as conchas secretas). */
    void drawInteractionHint(Graphics2D g, int camX, int camY) {
        int ax = tx + DX[dir], ay = ty + DY[dir];
        boolean target = false;
        for (Npc n : map.npcs) if (vis(n) && n.x == ax && n.y == ay && !n.id.startsWith("concha_secreta")) target = true;
        boolean water = map.at(ax, ay) == '~' && life.rod;
        if (!target && !water) return;
        int x = ax * T - camX + T / 2, y = ay * T - camY - 10 + (access.reduced ? 0 : (int) (Math.sin(frame / 12.0) * 2));
        g.setColor(new Color(255, 255, 255, 230)); g.fillRoundRect(x - 9, y - 9, 18, 16, 8, 8);
        g.fillPolygon(new int[]{x - 3, x + 3, x}, new int[]{y + 6, y + 6, y + 10}, 3);
        g.setColor(new Color(40, 40, 60)); g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString(water ? "~" : "Z", x - 3, y + 4);
    }

    /** F12: salva a cena atual, sem interface, em fotos/ na pasta de dados (em 2x). */
    void takePhoto() {
        if (state == TITLE || state == CONFIRM || map == null) return;
        BufferedImage image = new BufferedImage(W * 2, H * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.scale(2, 2); photo = true;
        try { paintScene(g); } finally { photo = false; g.dispose(); }
        try {
            Path dir = GameFiles.data("fotos"); Files.createDirectories(dir);
            String name = "aurora-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".png";
            ImageIO.write(image, "png", dir.resolve(name).toFile());
            audio.sfx("save"); toast("Foto salva: fotos/" + name);
        } catch (Exception e) { toast("Não foi possível salvar a foto."); }
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
        g.setFont(new Font("SansSerif", Font.PLAIN, 11)); g.setColor(new Color(255, 255, 255, 120));
        g.drawString("v" + VERSION + "  ·  F11: tela cheia", 10, H - 10);
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
        String obj = "Objetivo: " + Story.OBJECTIVES[stage] + (stage == 6 ? " (" + Integer.bitCount(frags) + "/3)" : stage == 9 ? " (" + Integer.bitCount(bye) + "/2)" : stage == 12 ? " (" + brasas + "/3)" : stage == 23 ? " (" + Integer.bitCount(chapter.seeds) + "/3)"
            : stage == ChapterSix.LISTEN ? " (" + Integer.bitCount(chapterSix.heard) + "/3)" : stage == ChapterSeven.INVITE ? " (" + chapterSeven.count() + "/7)" : "");
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        String heading = String.format("%s  •  %s %d (%s)  %02d:%02d  •  %s", map.name, seasons.name(), Seasons.dayOfSeason(dia), weekday(dia), hh, mm, weather.label());
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
            g.drawString("Setas: mover | Shift: correr | Z: falar | I: itens | K: lancheira | C: cartão | ESC: menu", 12, H - 10);
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
        int bx = 20, bh = access.large ? 200 : 110, by = H - bh - 20, bw = W - 40;
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
        g.setFont(new Font("SansSerif", Font.PLAIN, access.large ? 22 : 17));
        g.setColor(new Color(30, 30, 40));
        FontMetrics fm = g.getFontMetrics();
        int ly = by + 38;
        List<String> lines = access.dialogLines(); int rows = access.large ? 5 : 3;
        long shown = (long) Math.min(Integer.MAX_VALUE, dlgChars);
        for (int i = access.dialogPage * rows; i < Math.min(lines.size(), (access.dialogPage + 1) * rows); i++) {
            String line = lines.get(i);
            g.drawString(line.substring(0, (int) Math.min(line.length(), Math.max(0, shown))), bx + 24, ly); ly += access.large ? 30 : 24;
            shown -= line.length();
        }
        if (dlgChars >= pageChars() && (frame / 25) % 2 == 0) g.drawString("▼", bx + bw - 36, by + bh - 14);
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
            JFrame f = new JFrame("Aventura em Aurora");
            Game game = new Game();
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                if (GameFiles.logError(error)) game.toast("Algo deu errado. Detalhes em erros.log.");
            });
            game.setPreferredSize(new Dimension(W * windowScale(), H * windowScale()));
            f.add(game);
            f.pack();
            f.setMinimumSize(new Dimension(W / 2, H / 2));
            BufferedImage icon = game.spr.get("cintila");
            if (icon != null) f.setIconImage(icon);
            f.setLocationRelativeTo(null);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    if (game.saveBeforeClose()) System.exit(0);
                }
            });
            f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            f.setVisible(true);
            game.requestFocusInWindow();
            Properties video = new Properties();
            try (InputStream in = Files.newInputStream(GameFiles.data("video.properties"))) { video.load(in); } catch (Exception ignored) {}
            if (Boolean.parseBoolean(video.getProperty("telaCheia"))) game.toggleFullscreen();
        });
    }
}
