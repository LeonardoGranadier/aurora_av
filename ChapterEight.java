import java.awt.*;
import java.util.*;

/**
 * Capítulo 8, A Melodia da Semana: Lia, a irmã do Lucas, deixou uma melodia para cada dia da semana e se perdeu
 * no Vale dos Ecos, atrás da Gruta do Eco. Tocar a melodia do dia no sino da praça em três dias diferentes abre
 * a passagem; no vale, pedras empurradas sobre placas erguem a ponte até o coreto onde ela está. Etapas 36 a 42.
 */
final class ChapterEight implements Feature {
    static final String VALLEY = "Vale dos Ecos";
    static final int START = 36, BELLS = 37, PASSAGE = 38, BRIDGE = 39, LIA = 40, RETURN = 41, FINAL = 42;
    /** Melodias por dia da semana, na ordem de Game.WEEKDAYS (sábado primeiro). */
    static final int[][] MELODIES = {{0, 1, 2, 3}, {3, 2, 1, 0}, {0, 2, 1, 3}, {2, 0, 3, 1}, {1, 1, 3, 0}, {3, 0, 2, 2}, {2, 3, 0, 1}};
    static final String[] DAY_NAMES = {"sábado", "domingo", "segunda", "terça", "quarta", "quinta", "sexta"};
    static final int DAYS_NEEDED = 3;

    final Game game;
    int played;   // bits dos dias da semana já tocados

    ChapterEight(Game game) { this.game = game; }

    int weekday() { return Math.floorMod(game.day() + 1, 7); }
    int[] todaysMelody() { return MELODIES[weekday()]; }
    int count() { return Integer.bitCount(played); }

    @Override public void reset() { played = 0; }

    @Override public void buildWorld() {
        GameMap v = new GameMap(VALLEY, 24, 18, '.', true);
        v.border('#');
        v.rect(0, 6, 24, 2, '~');                                         // riacho: só a ponte atravessa
        v.rect(11, 8, 2, 9, '=');
        v.rect(11, 17, 2, 1, '=');
        for (int[] t : new int[][]{{1, 9}, {22, 9}, {1, 14}, {22, 15}, {20, 2}, {16, 1}, {21, 4}}) v.t[t[1]][t[0]] = '#';
        v.warps.add(new Warp(11, 17, "Gruta do Eco", 20, 2));
        v.warps.add(new Warp(12, 17, "Gruta do Eco", 20, 2));
        v.addNpc(new Npc("lia_vale", "Lia", 12, 3, new Color(150, 120, 200)).spr("lia").stages(PASSAGE, LIA));
        v.addNpc(new Npc("coreto", "Coreto", 14, 2, new Color(220, 200, 160)).obj("coreto").still());
        game.maps.put(VALLEY, v);

        GameMap cave = game.maps.get("Gruta do Eco");
        cave.t[1][20] = 'c';
        cave.warps.add(new Warp(20, 1, VALLEY, 11, 16));
        cave.addNpc(new Npc("rocha_eco", "Rocha que vibra", 20, 1, new Color(120, 110, 130)).obj("rocha_eco").still().stages(0, BELLS));

        game.maps.get("Vila Aurora").addNpc(new Npc("lia", "Lia", 22, 8, new Color(150, 120, 200)).stages(RETURN, 99).when("dia"));
    }

    /** Liga a ponte: as pedras chegaram às placas. */
    void bridgeRaised() {
        applyBridge();
        if (game.stage == BRIDGE) game.setStage(LIA);
        game.say("", "As três pedras afundam nas placas. Com um ronco, tábuas sobem do riacho e formam uma ponte!",
            "Do outro lado, no coreto, alguém toca uma flauta. A melodia é a mesma do sino da praça.");
    }
    void applyBridge() { game.maps.get(VALLEY).rect(11, 6, 2, 2, 'P'); }

    @Override public void arrive(GameMap map) {
        if (map.name.equals(VALLEY) && game.stage == PASSAGE) game.setStage(BRIDGE);
    }

    @Override public boolean talk(Npc n) {
        String id = n.id;
        if (id.equals("lucas") && game.stage == START) {
            game.setStage(BELLS);
            game.say(n.name, "Chegou uma carta da minha irmã, a Lia! Mas ela não diz onde está... só tem música.",
                "\"Lucas, toque a melodia de cada dia no sino da praça. Quando o eco de três dias se juntar, a gruta vai mostrar o caminho.\"",
                "Ela deixou uma melodia para cada dia da semana, lembra? Hoje é " + DAY_NAMES[weekday()] + ". Me ajuda? A gente toca juntos!",
                "(Toque a melodia do dia no sino da praça em três dias diferentes. A cama de casa faz o tempo passar.)");
            return true;
        }
        if (id.equals("sino") && game.stage == BELLS) {
            int bit = 1 << weekday();
            if ((played & bit) != 0) { game.say("Lucas", "A melodia de " + DAY_NAMES[weekday()] + " já ecoou. Amanhã é outra! (" + count() + "/" + DAYS_NEEDED + ")"); return true; }
            game.melody.open("Melodia de " + DAY_NAMES[weekday()], "Do caderno da Lia: cada dia da semana tem a sua melodia.", todaysMelody(), () -> {
                played |= bit;
                game.audio.sfx("fanfare");
                if (count() >= DAYS_NEEDED) {
                    game.setStage(PASSAGE);
                    game.say("Lucas", "Ouviu isso?! O eco veio lá da floresta... da Gruta do Eco!",
                        "Três dias de melodia juntos. Aposto que a rocha que vibra, no fundo da gruta, abriu! Vai lá, eu cuido dos sinos.");
                } else game.say("Lucas", "Isso! O sino respondeu com um eco comprido. Melodias tocadas: " + count() + "/" + DAYS_NEEDED + ". Volte em outro dia da semana!");
                game.save(false);
            });
            return true;
        }
        switch (id) {
            case "rocha_eco" -> game.say("", "Uma rocha que vibra a cada som da gruta. Parece esperar por uma música vinda de longe.");
            case "coreto" -> game.say("", "Um coreto antigo, com pautas de música entalhadas nas colunas.");
            case "lia_vale" -> {
                if (game.stage == LIA) {
                    game.setStage(RETURN);
                    game.say("Lia", "Você atravessou a ponte! Eu sou a Lia, irmã do Lucas.",
                        "Vim estudar os ecos deste vale, mas o riacho subiu e a ponte afundou. Fiquei tocando as melodias, esperando alguém ouvir.",
                        "Foi você que tocou os sinos com ele, não foi? Então você é o amigo de aventuras que ele vive falando!",
                        "Vou voltar para a vila pelo caminho das pedras. Encontre a gente na praça!");
                } else game.say("Lia", "Do outro lado do riacho, Lia acena e toca uma melodia. A ponte está afundada.");
            }
            case "lia" -> {
                if (game.stage == RETURN) { game.say("Lia", "Ainda não falei com o Lucas... ele vai chorar, eu sei. Vamos juntos? Ele está na praça!"); return true; }
                game.say("Lia", "Hoje é " + DAY_NAMES[weekday()] + ". A melodia de hoje é: " + MelodyScreen.names(todaysMelody()) + ".",
                    game.isNight() ? "À noite, os sinos ecoam até o Vale dos Ecos." : "O Lucas melhorou muito nos sinos. Acho que alguém andou treinando com ele!");
            }
            case "lucas" -> {
                if (game.stage != RETURN) return false;
                game.say(n.name, "LIA?! É você mesmo?! ...Eu sabia que a melodia ia te trazer de volta!",
                    "Lia: \"Trouxe sim, maninho. E trouxe também o seu melhor amigo de aventuras.\"",
                    "Os sinos da praça tocam sozinhos com o vento, e a vila inteira para para ouvir.",
                    "Capítulo 8 concluído: A Melodia da Semana.");
                game.afterDlg = () -> { game.setStage(FINAL); game.itemGet("caixinha"); };
            }
            default -> { return false; }
        }
        return true;
    }

    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (n.id.equals("rocha_eco")) {
            int shake = game.access.reduced ? 0 : (int) (Math.sin(game.frame / 3.0) * 1);
            g.setColor(new Color(100, 92, 110)); g.fillRoundRect(x + 1 + shake, y + 4, 30, 30, 8, 8);
            g.setColor(new Color(150, 220, 230, 140)); g.drawArc(x - 4, y, 40, 20, 20, 140);
            return true;
        }
        if (n.id.equals("coreto")) {
            g.setColor(new Color(240, 230, 210)); g.fillRect(x + 2, y + 12, 4, 24); g.fillRect(x + 26, y + 12, 4, 24);
            g.setColor(new Color(170, 90, 90)); g.fillPolygon(new int[]{x - 4, x + 16, x + 36}, new int[]{y + 14, y - 4, y + 14}, 3);
            return true;
        }
        return false;
    }

    /** Notas musicais flutuando no vale. */
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals(VALLEY)) return;
        for (int i = 0; i < 10; i++) {
            double t = game.access.reduced ? i : game.frame / 50.0 + i;
            int x = (i * 211) % (24 * Game.T) - camX, y = (int) (40 + (i * 97) % 160 - (t * 12) % 60) - camY;
            g.setColor(new Color(170, 150, 230, 150));
            g.fillOval(x, y + 8, 7, 6); g.fillRect(x + 6, y, 2, 11);
        }
    }

    @Override public void save(Properties p) { p.setProperty("melodia.dias", "" + played); }
    @Override public void load(Properties p) {
        played = game.stage >= PASSAGE ? (1 << DAYS_NEEDED) - 1 : game.stage == BELLS ? game.life.number(p, "melodia.dias", 0, 127) : 0;
    }
    @Override public String signature() { return "w" + played; }
}
