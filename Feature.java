import java.awt.Graphics2D;
import java.util.Properties;

/**
 * Sistema que se conecta ao Game pelos ganchos abaixo, todos opcionais.
 * Game chama cada gancho em todos os sistemas de Game.features, na ordem de registro,
 * depois dos sistemas mais antigos (que continuam ligados diretamente em Game).
 */
interface Feature {
    /** Novo jogo: volta ao estado inicial. */
    default void reset() {}
    /** Novo jogo: acrescenta objetos e personagens aos mapas (já construídos). */
    default void buildWorld() {}
    default void save(Properties p) {}
    /** Ausência de chaves (saves antigos) deve resultar no estado inicial. */
    default void load(Properties p) {}
    /** Muda quando há algo a salvar automaticamente. */
    default String signature() { return ""; }

    /** Conversa ou interação com um objeto; true quando o sistema tratou a interação. */
    default boolean talk(Npc n) { return false; }
    /** Depois de qualquer conversa tratada pelo roteiro. */
    default void afterTalk(Npc n) {}
    /** false esconde o personagem ou objeto. */
    default boolean visible(Npc n) { return true; }
    /** Desenho próprio de um objeto do mapa; true substitui o desenho padrão. */
    default boolean drawObject(Graphics2D g, Npc n, int x, int y) { return false; }
    /** Elementos sobre o mapa, abaixo do HUD. */
    default void drawWorld(Graphics2D g, int camX, int camY) {}
    /** Elementos sobre a iluminação (noite, cavernas): brilhos, pássaros, partículas. */
    default void drawSky(Graphics2D g, int camX, int camY) {}

    /** O jogador tentou andar para (x, y), ocupado: true se o sistema empurrou o que estava lá e liberou a passagem. */
    default boolean push(int x, int y, int dir) { return false; }
    /** O jogador terminou um passo em (x, y). */
    default void stepped(int x, int y) {}
    /** O jogador chegou a um mapa (porta, viagem ou carregamento). */
    default void arrive(GameMap map) {}

    /** Avanço do relógio durante o jogo (estado PLAY). */
    default void update(double dt) {}
    /** Tela própria: o estado de tela que este sistema controla. */
    default boolean owns(int state) { return false; }
    default void key(int key) {}
    default void typed(char c) {}
    default void draw(Graphics2D g) {}
}
