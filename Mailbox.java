import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Caixa de correio ao lado de casa, na vila. Como as ligações do Pokégear e as cartas de Animal Crossing e
 * Stardew Valley: quando algo acontece, alguém escreve na manhã seguinte, com dicas, histórias e às vezes moedas.
 * Chegam no máximo duas cartas por dia. A bandeira vermelha levantada avisa que há carta nova.
 */
final class Mailbox implements Feature {
    record Letter(String id, String from, String title, int coins, Predicate<Game> when, Function<Game, String> body) {}
    static final int PER_DAY = 2;
    static final List<Letter> LETTERS = List.of(
        new Letter("mae_bemvindo", "Mamãe", "Para quem saiu cedo", 0, g -> g.day() >= 1,
            g -> "Bom dia, " + g.explorer.nameOr("meu bem") + "! Deixei esta carta na caixa porque você sai antes do café. "
                + "Lembre: a cama de casa faz o tempo passar, e o Mapa de Aurora (Tab) mostra para onde ir. "
                + "Volte sempre que quiser contar uma aventura. Beijos, Mamãe."),
        new Letter("bento", "Seu Bento, o mascate", "Trocas aos domingos", 0, g -> g.day() >= 2 && g.trades.step == 0,
            g -> "Saudações! Sou Bento, mascate de estrada. Todo domingo, das 8h às 18h, monto minha banca na Praia Azul. "
                + "Não vendo nada: eu troco. Passe por lá e começamos uma corrente de trocas que vai longe!"),
        new Letter("rosa_festival", "Dona Rosa", "Festival de Aurora", 0, g -> g.day() >= 4,
            g -> g.explorer.w("Querido vizinho", "Querida vizinha", "Olá, pessoa querida da vizinhança") + ", todo sábado a praça vira festa! "
                + "Inscreva-se no mural verde para o concurso de pesca e os sinos. Eu estarei na banca com flores novas."),
        new Letter("beto_chuva", "Tio Beto", "Segredo de pescador", 0, g -> g.life.rod,
            g -> "Aprendiz! Um segredo: quando chove, a Carpa Dourada, o Bagre da Lua e a Truta Verde ficam mais famintos. "
                + "Pegue o guarda-chuva e vá para a beira d'água. Ass.: Beto."),
        new Letter("marinho_conchas", "Seu Marinho", "Conchas cor-de-rosa", 0, g -> g.shells.found() >= 1,
            g -> "Marujo, ouvi dizer que você achou uma concha secreta! Há doze escondidas em Aurora, dentro de árvores e pedras. "
                + "Repare num brilho fraquinho quando passar perto. A cada três, venha me ver: tenho presentes guardados."),
        new Letter("prof_noite", "Prof. Carvalho", "Anotações de campo", 0, g -> g.stage >= 4,
            g -> "Desde que o farol voltou a brilhar, anoto coisas curiosas: garrafas na maré, luzes na gruta. "
                + "A noite de Aurora guarda segredos que o dia esconde. Observe, anote e conte tudo! Prof. Carvalho."),
        new Letter("lucas_sinos", "Lucas", "Revanche!", 0, g -> g.life.bellBest >= VillageLife.BELL_ROUNDS,
            g -> "Você acertou as cinco melodias?! Ninguém nunca tinha conseguido. No festival de sábado tem sete rodadas, "
                + "mais rápidas. Duvido que você consiga! Seu amigo, Lucas."),
        new Letter("capita_obrigada", "Capitã Estrela", "Do cais da Praia Azul", 30, g -> g.stage >= 15,
            g -> "Marujo, ainda vejo a Estrela do Mar brilhando no farol toda noite. Junto desta carta vão 30 moedas "
                + "que achei no fundo do meu baú. Use bem! Com gratidão, Capitã Estrela."),
        new Letter("eremita_frio", "Eremita", "Brrr", 0, g -> g.stage >= 19 && g.trades.step < 4,
            g -> "Rapaz, moça, quem quer que seja: esta montanha é fria demais. Se alguém aparecer com um cachecol, "
                + "eu pago com o meu melhor chá. Hmpf. O Eremita."),
        new Letter("cintila", "Cintila", "Tin, tin!", 0, g -> g.stage >= 21,
            g -> "Tin! Daqui de cima vejo a vila inteira. Obrigada por me trazer de volta, " + g.explorer.nameOr("amigo") + "! "
                + "Ouvi o farol sussurrar uma mensagem para você. Vá até a porta dele numa noite clara..."),
        new Letter("rosa_flores", "Dona Rosa", "Flores do quintal", 0, g -> Arrays.stream(g.gardening.stock).sum() > 0,
            g -> "Vi flores lindas saindo do seu quintal! Compro na minha banca, das 6h às 21h. E o Prof. Carvalho anda "
                + "dizendo que o observatório dele precisa de flores. Que mistério!"),
        new Letter("prof_observatorio", "Prof. Carvalho", "O observatório do meu avô", 0, g -> g.trades.step >= TradeQuest.STEPS.length,
            g -> "Com a chave em mãos, falta pouco! O observatório, no nordeste da vila, precisa de doações: peixes, flores, "
                + "conchas e moedas. Cada conjunto devolve um pedaço da vila. Conto com você!"),
        new Letter("mae_orgulho", "Mamãe", "Um recado na geladeira", 20, g -> g.explorer.unlocked.size() >= 8,
            g -> "Vi seu Cartão do Explorador cheio de medalhas! Estou tão orgulhosa. Deixei 20 moedas para um docinho "
                + "na banca da Rosa. Com amor, Mamãe."),
        new Letter("mae_cozinha", "Mamãe", "A cozinha é sua", 0, g -> g.day() >= 2,
            g -> "Liberei o fogão da cozinha para você! Com peixes e flores dá para fazer pratos gostosos. Comece pela minha "
                + "Sopa de Lambari. Os pratos ficam na lancheira (tecla K): comer dá um ânimo para o dia, e os moradores adoram ganhar um."),
        new Letter("verao", "Prof. Carvalho", "Chegou o verão", 0, g -> Seasons.season(g.day() + 1) == Seasons.SUMMER,
            g -> "Verão! Os girassóis estão no auge e a Carpa Dourada e o Peixe-Coral aparecem mais. As tulipas descansam até o outono. "
                + "E não esqueça: o aniversário do Tio Beto é no dia 5!"),
        new Letter("outono", "Prof. Carvalho", "Folhas douradas", 0, g -> Seasons.season(g.day() + 1) == Seasons.AUTUMN,
            g -> "Outono em Aurora: o Bagre da Lua e o Peixe-Lanterna ficam mais comuns e o céu, mais limpo. "
                + "Ah, e o dia 12 é meu aniversário. Não que eu esteja contando!"),
        new Letter("inverno", "Lucas", "NEVE!!!", 0, g -> Seasons.season(g.day() + 1) == Seasons.WINTER,
            g -> "Nevou na vila inteira! A chuva vira neve e só a Flor Estelar cresce. O Peixe Estelar adora água fria. "
                + "Meu aniversário é dia 9, viu? Gosto de Truta com Ervas e de Banquete Estelar!"),
        new Letter("receita_torta", "Tio Beto", "Receita: Torta de Carpa", 0, g -> g.bonds.friendship[1] >= 40,
            g -> "Aprendiz, segredo de família: Carpa Dourada com Girassol, no fogão de casa. Dá uma sorte danada na pesca! "
                + "E se quiser me agradar, sardinha na brasa nunca falha."),
        new Letter("receita_doce", "Dona Rosa", "Receita: Doce de Girassol", 0, g -> g.bonds.friendship[2] >= 40,
            g -> "Girassol e tulipa, um pouco de paciência e pronto: Doce de Girassol! Deixa qualquer conversa mais gostosa. "
                + "Meu favorito, depois desse, é a Sopa de Lambari da sua mãe."),
        new Letter("receita_truta", "Lucas", "Receita da minha irmã", 0, g -> g.bonds.friendship[3] >= 40,
            g -> "Minha irmã fazia Truta com Ervas: Truta Verde e Margarida. Depois de comer, eu corria a vila inteira sem cansar!"),
        new Letter("receita_ensopado", "Capitã Estrela", "Receita: Ensopado do Mar", 0, g -> g.stage >= 15,
            g -> "Ensopado do Mar dos tempos de navio: Peixe-Lanterna, Sardinha Azul e uma Margarida. "
                + "Depois dele, enxergava o mar até nas noites sem lua. O professor adora."),
        new Letter("receita_banquete", "Cintila", "Tin! Uma receita do céu", 0, g -> g.stage >= 28,
            g -> "Tin! No céu contam de um Banquete Estelar: Peixe Estelar e Flor Estelar. Dá todos os ânimos de uma vez!"),
        new Letter("vila_obrigado", "Moradores de Aurora", "Obrigado!", 100, g -> g.observatory.complete(),
            g -> "O observatório está aberto e a vila nunca esteve tão bonita: lanternas, flores, uma luneta na praia... "
                + "Juntamos 100 moedas como agradecimento. Volte à noite e olhe as estrelas com a gente! "
                + "Prof. Carvalho, Tio Beto, Dona Rosa e Lucas.")
    );

    final Game game;
    final List<String> received = new ArrayList<>();
    final Set<String> read = new HashSet<>();
    int lastDay = -1, selected, returnState = Game.PLAY;
    boolean reading;

    Mailbox(Game game) { this.game = game; }

    int unread() { return (int) received.stream().filter(id -> !read.contains(id)).count(); }
    Letter letter(String id) { return LETTERS.stream().filter(l -> l.id().equals(id)).findFirst().orElse(null); }

    /** Novas cartas chegam na virada do dia (ou na primeira atualização de uma partida carregada). */
    void deliver() {
        int today = game.day();
        if (today == lastDay) return;
        lastDay = today;
        int count = 0;
        for (Letter l : LETTERS) {
            if (count >= PER_DAY) break;
            if (!received.contains(l.id()) && l.when().test(game)) { received.add(l.id()); count++; }
        }
        if (count > 0 && game.map != null && !game.map.name.equals("Casa")) game.toast(count == 1 ? "Chegou uma carta na caixa de correio." : "Chegaram cartas na caixa de correio.");
    }

    @Override public void reset() { received.clear(); read.clear(); lastDay = -1; selected = 0; reading = false; }
    @Override public void buildWorld() {
        game.maps.get("Vila Aurora").addNpc(new Npc("caixa_correio", "Caixa de correio", 7, 7, new Color(70, 110, 170)).obj("caixa_correio").still());
    }
    @Override public void update(double dt) { deliver(); }

    @Override public boolean talk(Npc n) {
        if (!n.id.equals("caixa_correio")) return false;
        if (received.isEmpty()) { game.say("", "A caixa de correio está vazia. Volte amanhã!"); return true; }
        selected = 0; reading = false; returnState = Game.PLAY; game.down.clear(); game.state = Game.MAIL;
        return true;
    }

    /** Abre a carta; moedas anexas são guardadas na primeira leitura. */
    void open(int index) {
        Letter l = letter(received.get(received.size() - 1 - index));
        reading = true;
        if (read.add(l.id()) && l.coins() > 0) {
            game.home.coins = Math.min(HomeDecor.MAX_COINS, game.home.coins + l.coins());
            game.audio.sfx("pickup");
            game.toast("+" + l.coins() + " moedas");
        }
        game.save(false);
    }

    @Override public boolean owns(int state) { return state == Game.MAIL; }
    @Override public void key(int key) {
        boolean act = key == KeyEvent.VK_Z || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE;
        if (reading) { if (act || key == KeyEvent.VK_ESCAPE) { reading = false; game.audio.sfx("back"); } return; }
        int n = received.size(), old = selected;
        if (key == KeyEvent.VK_ESCAPE) { game.audio.sfx("back"); game.state = returnState; game.down.clear(); return; }
        if (key == KeyEvent.VK_UP) selected = (selected + n - 1) % n;
        else if (key == KeyEvent.VK_DOWN) selected = (selected + 1) % n;
        else if (act) { game.audio.sfx("confirm"); open(selected); return; }
        if (selected != old) game.audio.sfx("select");
    }

    @Override public boolean drawObject(Graphics2D g, Npc n, int x, int y) {
        if (!n.id.equals("caixa_correio")) return false;
        g.setColor(new Color(110, 80, 55)); g.fillRect(x + 14, y + 20, 4, 16);
        g.setColor(new Color(70, 110, 170)); g.fillRoundRect(x + 6, y + 8, 20, 14, 8, 8);
        g.setColor(new Color(40, 60, 100)); g.fillRect(x + 9, y + 15, 14, 2);
        if (unread() > 0) {                                                      // bandeira levantada
            g.setColor(new Color(90, 90, 90)); g.fillRect(x + 25, y + 2, 2, 12);
            g.setColor(new Color(220, 60, 50)); g.fillRect(x + 26, y + 2, 7, 5);
        }
        return true;
    }

    @Override public void draw(Graphics2D g) {
        if (reading) {
            Letter l = letter(received.get(received.size() - 1 - selected));
            g.setColor(new Color(0, 0, 0, 155)); g.fillRect(0, 0, Game.W, Game.H);
            g.setColor(new Color(252, 246, 230)); g.fillRoundRect(70, 40, 500, 400, 10, 10);
            g.setColor(new Color(200, 180, 140)); for (int y = 130; y < 410; y += 24) g.drawLine(90, y, 550, y);
            g.setColor(new Color(80, 60, 40)); g.setFont(new Font("Serif", Font.BOLD, 22)); g.drawString(l.title(), 92, 84);
            g.setFont(new Font("Serif", Font.ITALIC, 14)); g.drawString("De: " + l.from(), 92, 108);
            g.setFont(new Font("Serif", Font.PLAIN, game.access.large ? 19 : 16));
            int y = 146;
            for (String line : game.wrap(l.body().apply(game), g.getFontMetrics(), 450)) { g.drawString(line, 92, y); y += game.access.large ? 26 : 24; }
            if (l.coins() > 0) { g.setColor(new Color(170, 120, 30)); g.drawString("Anexo: " + l.coins() + " moedas (guardadas)", 92, 420); }
            return;
        }
        game.life.panel(g, "Cartas", "Setas: escolher · Z: ler · Esc: sair");
        int start = Math.max(0, selected - 7);
        for (int i = start; i < Math.min(received.size(), start + 8); i++) {
            Letter l = letter(received.get(received.size() - 1 - i));
            int y = 128 + (i - start) * 38;
            if (i == selected) { g.setColor(new Color(207, 231, 221)); g.fillRect(38, y - 22, 564, 34); }
            boolean isNew = !read.contains(l.id());
            game.life.text(g, (isNew ? "● " : "   ") + l.title(), 48, y, 340, 15);
            game.life.text(g, l.from(), 400, y, 190, 13);
        }
        game.life.text(g, unread() + " não lida(s) · " + received.size() + " carta(s)", 44, 440, 540, 13);
    }

    @Override public void save(Properties p) {
        p.setProperty("cartas.recebidas", String.join(",", received));
        p.setProperty("cartas.lidas", String.join(",", new TreeSet<>(read)));
        p.setProperty("cartas.dia", "" + lastDay);
    }
    @Override public void load(Properties p) {
        received.clear(); read.clear();
        for (String id : p.getProperty("cartas.recebidas", "").split(",")) if (letter(id) != null && !received.contains(id)) received.add(id);
        for (String id : p.getProperty("cartas.lidas", "").split(",")) if (received.contains(id)) read.add(id);
        try { lastDay = Integer.parseInt(p.getProperty("cartas.dia", "-1")); } catch (NumberFormatException e) { lastDay = -1; }
    }
    @Override public String signature() { return received + "" + read; }
}
