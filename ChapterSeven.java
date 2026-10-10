import java.awt.*;
import java.util.*;

/**
 * Capítulo 7, O Festival do Céu: Cintila convida todos para uma festa no alto do Pico Estrelado.
 * O jogador convida sete amigos, leva um prato da lancheira e celebra sob uma chuva de estrelas.
 * Etapas 32 a 36; a 36 encerra a campanha e rola os créditos.
 */
final class ChapterSeven implements Feature {
    static final int START = 32, INVITE = 33, CLIMB = 34, PARTY = 35, FINAL = 36;
    static final String[] GUESTS = {"mae", "marinho", "capita_fim", "prof", "beto", "rosa", "lucas"};
    static final String[] GUEST_NAMES = {"Mamãe", "Seu Marinho", "Capitã Estrela", "Prof. Carvalho", "Tio Beto", "Dona Rosa", "Lucas"};
    static final String[] GUEST_MAPS = {"Casa", "Praia Azul", "Praia Azul", "Vila Aurora", "Vila Aurora", "Vila Aurora", "Vila Aurora"};
    static final String[] ACCEPT = {
        "Uma festa no céu? Eu vou! E vou levar uma panela de sopa, claro. Ninguém sobe montanha de barriga vazia.",
        "Festa no Pico? Faz quarenta anos que não subo lá... Por você, marujo, eu subo até a lua.",
        "Uma festa das estrelas! Vou levar a luneta do barco. Quero ver a Maré lá de cima.",
        "O Festival do Céu! Meu avô falava dele. Vou levar o caderno: quero anotar cada estrela!",
        "Lá em cima não tem lago... mas tem estrela cadente. Vou levar a vara mesmo assim!",
        "Que convite lindo! Vou fazer coroas de flores para todo mundo.",
        "SÉRIO?! Vou tocar os sinos lá em cima! Será que dá pra levar o sino grande?"};
    static final String[] PARTY_LINES = {
        "Olhe só para isso. Eu sempre soube que você ia longe, mas não sabia que ia tão alto.",
        "Daqui de cima dá para ver o farol, a vila e a baía inteira. Que luz bonita você espalhou.",
        "Lá embaixo, a Maré está cantando junto. Escute!",
        "Anotei quarenta e duas estrelas novas. E a mais brilhante está bem do seu lado.",
        "Nenhum peixe mordeu a isca, mas eu nunca pesquei uma noite tão boa.",
        "Uma coroa de flores para você. Combina com as estrelas.",
        "Escuta! Os sinos daqui de cima ecoam até o mar!"};
    static final int[][] PLACES = {{8, 7}, {12, 7}, {8, 9}, {12, 9}, {9, 10}, {11, 10}, {10, 6}};

    final Game game;
    int invited;   // bits dos convidados

    ChapterSeven(Game game) { this.game = game; }

    int count() { return Integer.bitCount(invited); }
    boolean allInvited() { return invited == (1 << GUESTS.length) - 1; }
    boolean hasDish() { return Arrays.stream(game.cooking.dishes).anyMatch(d -> d > 0); }

    @Override public void reset() { invited = 0; }
    @Override public void buildWorld() {
        GameMap peak = game.maps.get("Pico Estrelado");
        peak.addNpc(new Npc("ceu_cintila", "Cintila", 10, 7, new Color(255, 231, 121)).obj("cintila").stages(CLIMB, 99).when("noite"));
        for (int i = 0; i < GUESTS.length; i++) {
            String sprite = GUESTS[i].equals("capita_fim") ? "capita" : GUESTS[i];
            peak.addNpc(new Npc("festa_" + GUESTS[i], GUEST_NAMES[i], PLACES[i][0], PLACES[i][1], new Color(200, 160, 120))
                .spr(sprite).stages(PARTY, PARTY));
        }
    }

    @Override public boolean talk(Npc n) {
        String id = n.id;
        if (id.equals("porta_farol") && game.stage == START) {
            if (!game.isNight()) { game.say("", "A porta do farol está morna, como se guardasse um segredo para a noite."); return true; }
            game.setStage(INVITE);
            game.say("Cintila", "Tin! Tin! Você ouviu? A Maré voltou a cantar e o céu inteiro quer comemorar!",
                "No alto do Pico Estrelado, as estrelas vão fazer o Festival do Céu. E eu quero que todo mundo venha!",
                "Convide a Mamãe, o Seu Marinho, a Capitã, o Professor, o Tio Beto, a Dona Rosa e o Lucas.",
                "Depois, leve um prato gostoso na lancheira e suba ao Pico, numa noite clara. Eu espero lá!");
            return true;
        }
        if (game.stage == INVITE) {
            int i = Arrays.asList(GUESTS).indexOf(id);
            if (i < 0 || (invited & (1 << i)) != 0) return false;
            invited |= 1 << i;
            java.util.List<String> lines = new ArrayList<>(java.util.List.of(ACCEPT[i]));
            if (id.equals("mae") && !hasDish()) {
                game.cooking.dishes[0]++;
                lines.add("Tome, leve esta Sopa de Lambari na lancheira. Uma festa precisa de comida!");
            }
            if (allInvited()) {
                game.setStage(CLIMB);
                lines.add("Todos os amigos foram convidados! Leve um prato na lancheira e suba ao Pico Estrelado à noite.");
            } else lines.add("Convidados: " + count() + "/" + GUESTS.length + ".");
            game.audio.sfx("pickup");
            game.say(n.name, lines.toArray(String[]::new));
            return true;
        }
        if (id.equals("mae") && game.stage == CLIMB && !hasDish()) {
            game.cooking.dishes[0]++;
            game.say(n.name, "Esqueceu a comida? Leve mais uma Sopa de Lambari. E agasalhe-se, o Pico é frio!");
            return true;
        }
        if (id.equals("ceu_cintila")) {
            if (game.stage == CLIMB) {
                if (!hasDish()) { game.say("Cintila", "Tin! Falta só o banquete. Cozinhe algo no fogão de casa, ou peça uma sopa para a Mamãe!"); return true; }
                int dish = 0; while (game.cooking.dishes[dish] == 0) dish++;
                game.cooking.dishes[dish]--;
                game.setStage(PARTY);
                game.say("Cintila", "Você chegou! E trouxe " + Cooking.RECIPES[dish].name() + "! Agora sim, é festa!",
                    "Um a um, seus amigos sobem a trilha: a Mamãe, o Seu Marinho, a Capitã, o Professor, o Tio Beto, a Dona Rosa e o Lucas.",
                    "As estrelas descem tão perto que dá para tocar. Converse com todo mundo e depois volte a falar comigo.");
            } else if (game.stage == PARTY) {
                game.say("Cintila", "Sabe o que eu aprendi descendo do céu? Que uma estrela sozinha só brilha. Juntas, elas desenham histórias.",
                    "Você acendeu um farol, cruzou o mar, fez um jardim florescer e devolveu a canção à baía. E trouxe todo mundo até aqui.",
                    "Essa é a nossa constelação: " + game.explorer.nameOr("você") + " e Aurora inteira.",
                    "Capítulo 7 concluído: O Festival do Céu.");
                game.afterDlg = () -> { game.setStage(FINAL); game.itemGet("lanterna_festa"); };
            } else game.say("Cintila", "Tin! Toda noite clara, o Pico guarda um pedacinho do festival. Volte quando quiser!");
            return true;
        }
        if (id.startsWith("festa_")) {
            int i = Arrays.asList(GUESTS).indexOf(id.substring(6));
            game.say(GUEST_NAMES[i], PARTY_LINES[i]);
            return true;
        }
        return false;
    }

    /** Chuva de estrelas e lanternas no Pico durante e depois do festival. */
    @Override public void drawWorld(Graphics2D g, int camX, int camY) {
        if (!game.map.name.equals("Pico Estrelado") || game.stage < PARTY || !game.isNight()) return;
        Random r = new Random(17);
        for (int i = 0; i < 40; i++) {
            int x = r.nextInt(20 * Game.T) - camX, y0 = r.nextInt(14 * Game.T);
            int y = game.access.reduced ? y0 - camY : (int) ((y0 + game.frame * (1.5 + i % 3)) % (14 * Game.T)) - camY;
            g.setColor(new Color(255, 245, 190, 200)); g.fillRect(x, y, 2, 2);
            g.setColor(new Color(255, 245, 190, 70)); g.fillRect(x - 3, y - 6, 2, 6);
        }
        Color[] lanterns = {new Color(255, 170, 90), new Color(150, 220, 255), new Color(240, 150, 200), new Color(255, 230, 120)};
        for (int i = 0; i < 8; i++) {
            int x = (7 + i % 7) * Game.T - camX, y = (5 + (i * 3) % 7) * Game.T - camY - 20 - (int) (game.access.reduced ? 0 : Math.sin(game.frame / 25.0 + i) * 4);
            g.setColor(lanterns[i % 4]); g.fillRoundRect(x + 10, y, 12, 14, 4, 4);
            g.setColor(new Color(lanterns[i % 4].getRed(), lanterns[i % 4].getGreen(), lanterns[i % 4].getBlue(), 60)); g.fillOval(x, y - 10, 32, 34);
        }
    }

    @Override public void save(Properties p) { p.setProperty("festa.convidados", "" + invited); }
    @Override public void load(Properties p) {
        invited = game.stage >= CLIMB ? (1 << GUESTS.length) - 1 : game.stage == INVITE ? game.life.number(p, "festa.convidados", 0, (1 << GUESTS.length) - 1) : 0;
    }
    @Override public String signature() { return "f" + invited; }
}
