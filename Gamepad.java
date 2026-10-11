import java.awt.event.KeyEvent;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Controle (Xbox, PlayStation, Switch Pro, Steam Deck...) pela biblioteca Jamepad (SDL2), carregada por reflexão:
 * sem lib/jamepad*.jar o jogo funciona só com o teclado. Os botões viram as mesmas ações lógicas do teclado:
 *   direcional ou analógico esquerdo: mover · A: falar/escolher (segurar na pesca) · B ou Start: voltar/pausa
 *   X: itens · Y: cartão · LB: lancheira · RB: diário · Select: mapa · RT: correr · LT: cesta de flores
 */
final class Gamepad {
    /** Estado de um controle, já reduzido aos botões que o jogo usa. */
    record Pad(boolean connected, boolean up, boolean down, boolean left, boolean right, boolean a, boolean b, boolean x, boolean y,
               boolean lb, boolean rb, boolean start, boolean back, boolean lt, boolean rt) {
        static final Pad NONE = new Pad(false, false, false, false, false, false, false, false, false, false, false, false, false, false, false);
    }
    interface Source { Pad read(); void close(); }
    static final float STICK = 0.5f;

    final Game game;
    Source source;
    boolean connected;
    final Set<Integer> held = new HashSet<>();

    Gamepad(Game game) { this.game = game; }

    boolean available() { return source != null; }
    void start() { if (source == null) source = jamepad(); }
    void stop() { if (source != null) source.close(); source = null; }

    void poll() {
        if (source == null) return;
        try { apply(source.read()); } catch (RuntimeException e) { GameFiles.logError(e); stop(); }
    }

    /** Compara com o quadro anterior: botões novos apertam a ação, botões soltos a soltam. */
    void apply(Pad p) {
        if (p.connected() != connected) {
            connected = p.connected();
            game.toast(connected ? "Controle conectado." : "Controle desconectado.");
            if (!connected) { for (int k : new ArrayList<>(held)) game.releaseLogical(k); held.clear(); }
        }
        if (!connected) return;
        Map<Integer, Boolean> want = new LinkedHashMap<>();
        want.put(KeyEvent.VK_UP, p.up()); want.put(KeyEvent.VK_DOWN, p.down());
        want.put(KeyEvent.VK_LEFT, p.left()); want.put(KeyEvent.VK_RIGHT, p.right());
        want.put(KeyEvent.VK_Z, p.a()); want.put(KeyEvent.VK_ESCAPE, p.b() || p.start());
        want.put(KeyEvent.VK_I, p.x()); want.put(KeyEvent.VK_C, p.y());
        want.put(KeyEvent.VK_K, p.lb()); want.put(KeyEvent.VK_L, p.rb());
        want.put(KeyEvent.VK_TAB, p.back()); want.put(KeyEvent.VK_V, p.lt()); want.put(KeyEvent.VK_SHIFT, p.rt());
        for (Map.Entry<Integer, Boolean> e : want.entrySet()) {
            int k = e.getKey();
            if (e.getValue() && held.add(k)) game.pressLogical(k);
            else if (!e.getValue() && held.remove(k)) game.releaseLogical(k);
        }
    }

    /** Jamepad por reflexão; null se a biblioteca ou o SDL nativo não estiverem disponíveis. */
    static Source jamepad() {
        try {
            Class<?> managerClass = Class.forName("com.studiohartman.jamepad.ControllerManager");
            Object manager = managerClass.getConstructor().newInstance();
            PrintStream err = System.err;                                   // o Jamepad avisa sobre um arquivo opcional de mapeamentos
            System.setErr(new PrintStream(OutputStream.nullOutputStream()));
            try { managerClass.getMethod("initSDLGamepad").invoke(manager); } finally { System.setErr(err); }
            Method update = managerClass.getMethod("update"), getState = managerClass.getMethod("getState", int.class);
            Method quit = managerClass.getMethod("quitSDLGamepad");
            Class<?> stateClass = Class.forName("com.studiohartman.jamepad.ControllerState");
            Map<String, Field> f = new HashMap<>();
            for (String name : List.of("isConnected", "dpadUp", "dpadDown", "dpadLeft", "dpadRight", "a", "b", "x", "y", "lb", "rb",
                    "start", "back", "leftStickX", "leftStickY", "leftTrigger", "rightTrigger")) f.put(name, stateClass.getField(name));
            return new Source() {
                public Pad read() {
                    try {
                        update.invoke(manager);
                        Object s = getState.invoke(manager, 0);
                        float sx = f.get("leftStickX").getFloat(s), sy = f.get("leftStickY").getFloat(s);
                        return new Pad(f.get("isConnected").getBoolean(s),
                            f.get("dpadUp").getBoolean(s) || sy > STICK, f.get("dpadDown").getBoolean(s) || sy < -STICK,
                            f.get("dpadLeft").getBoolean(s) || sx < -STICK, f.get("dpadRight").getBoolean(s) || sx > STICK,
                            f.get("a").getBoolean(s), f.get("b").getBoolean(s), f.get("x").getBoolean(s), f.get("y").getBoolean(s),
                            f.get("lb").getBoolean(s), f.get("rb").getBoolean(s), f.get("start").getBoolean(s), f.get("back").getBoolean(s),
                            f.get("leftTrigger").getFloat(s) > STICK, f.get("rightTrigger").getFloat(s) > STICK);
                    } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                }
                public void close() { try { quit.invoke(manager); } catch (ReflectiveOperationException ignored) {} }
            };
        } catch (ClassNotFoundException e) {
            return null;                                                    // sem a biblioteca: só teclado
        } catch (Throwable t) {
            System.err.println("Controle indisponível: " + t);
            return null;
        }
    }
}
