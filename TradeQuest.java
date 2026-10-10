import java.awt.*;
import java.util.*;

/**
 * Corrente de trocas, como a de Link's Awakening: cada pessoa troca o que você leva por algo que outra pessoa quer.
 * Começa com Seu Bento, o mascate, que visita a Praia Azul aos domingos (8h às 18h), e termina com a Chave do
 * Observatório, entregue pelo Prof. Carvalho. As trocas só interrompem conversas sem passos da história.
 */
final class TradeQuest implements Feature {
    record Step(String item, String hint, String giver, String giverName, String[] lines) {}
    /** STEPS[i].item é o que o jogador tem depois da troca i; giver é quem o entrega. */
    static final Step[] STEPS = {
        new Step("Bolinha de gude", "O Lenhador, na Floresta Verde, comentou que o filho vive perdendo bolinhas.", "mascate", "Seu Bento",
            new String[]{"Olá, olá! Sou Bento, mascate de estrada. Venho à praia todo domingo.",
                "Eu troco coisas por histórias. Leve esta bolinha de gude: no caminho certo, ela vira um tesouro!"}),
        new Step("Pinha dourada", "O Montanhista da Trilha da Montanha coleciona lembranças da natureza.", "lenhador", "Lenhador",
            new String[]{"Uma bolinha de gude azul! Meu filho perdeu a dele no rio e não para de chorar.",
                "Tome, achei esta pinha dourada no alto de um pinheiro. Brilha como ouro, mas é seu agora."}),
        new Step("Cachecol de lã", "O Eremita da cabana vive reclamando do frio.", "montanhista", "Montanhista",
            new String[]{"Uma pinha dourada?! Dizem que traz sorte para quem sobe montanhas!",
                "Fique com meu cachecol de lã. Eu já estou quente só de olhar para essa pinha."}),
        new Step("Chá de estrelas", "A Capitã Estrela, no cais da praia, adora um chá depois de velejar.", "eremita", "Eremita",
            new String[]{"Um cachecol! Finalmente meu pescoço vai parar de bater os dentes. Ha!",
                "Leve este chá de estrelas. As folhas secam sob o céu do Pico e têm gosto de noite clara."}),
        new Step("Luneta antiga", "O Prof. Carvalho sempre quis uma lente boa para o observatório do avô dele.", "capita_fim", "Capitã Estrela",
            new String[]{"Chá de estrelas... o cheiro me leva de volta ao convés, em noites sem vento.",
                "Tome a luneta do Estrela do Norte. Já mostrou o caminho para muitos marujos; agora é sua vez."}),
        new Step("Chave do Observatório", "Leve a chave ao observatório, no nordeste da vila, e complete a restauração.", "prof", "Prof. Carvalho",
            new String[]{"Esta luneta! É exatamente a lente que faltava no observatório do meu avô!",
                "Fique com a chave do observatório, no nordeste da vila. Com a luneta e um pouco de ajuda, o céu volta a ter nome!"})
    };

    final Game game;
    int step;   // quantas trocas já foram feitas; 0 = ainda não conheceu o mascate

    TradeQuest(Game game) { this.game = game; }

    boolean complete() { return step >= STEPS.length; }
    String item() { return step == 0 ? null : STEPS[step - 1].item(); }
    String hint() { return step == 0 ? "Aos domingos, um mascate visita a Praia Azul." : STEPS[step - 1].hint(); }

    static boolean merchantDay(Game game) {
        int day = (int) (game.time / 1440) + 1;
        return Game.weekday(day).equals("Dom") && game.hour() >= 8 && game.hour() < 18;
    }

    @Override public void reset() { step = 0; }
    @Override public void buildWorld() {
        game.maps.get("Praia Azul").addNpc(new Npc("mascate", "Seu Bento", 10, 6, new Color(150, 95, 60)).spr("mascate"));
    }
    @Override public boolean visible(Npc n) { return !n.id.equals("mascate") || merchantDay(game); }

    /** A pessoa só aceita a troca quando a história não precisa dela naquela conversa. */
    boolean available(String id) {
        int s = game.stage;
        return switch (id) {
            case "eremita" -> s >= 19;
            case "prof" -> s != 15 && s != 22;
            default -> true;
        };
    }

    @Override public boolean talk(Npc n) {
        if (n.id.equals("mascate")) {
            if (step == 0) { trade(); return true; }
            game.say(n.name, complete() ? "A corrente se fechou! Uma bolinha virou a chave do céu. Que história boa, hein?"
                : "Já trocou a " + STEPS[0].item().toLowerCase() + "? Siga a corrente: " + hint());
            return true;
        }
        if (step == 0 || complete() || !STEPS[step].giver().equals(n.id) || !available(n.id)) return false;
        trade();
        return true;
    }

    void trade() {
        Step s = STEPS[step];
        String received = s.item();
        step++;
        game.audio.sfx("pickup");
        game.itemGet("troca");
        String[] lines = Arrays.copyOf(s.lines(), s.lines().length + 1);
        lines[lines.length - 1] = "Você recebeu: " + received + "! " + (complete() ? "" : "Dica: " + hint());
        game.say(s.giverName(), lines);
    }

    @Override public void save(Properties p) { p.setProperty("trocas.etapa", "" + step); }
    @Override public void load(Properties p) { step = game.life.number(p, "trocas.etapa", 0, STEPS.length); }
    @Override public String signature() { return "t" + step; }
}
