import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;

/**
 * Conquistas da Steam pela biblioteca steamworks4j, carregada por reflexão como o controle: sem
 * lib/steamworks4j*.jar (versão da itch.io) ou sem o cliente Steam aberto, nada acontece.
 * Cada conquista do Cartão do Explorador vira "ACH_" + id em maiúsculas (ex.: ACH_FAROL); cadastre os
 * mesmos nomes no Steamworks (veja STEAM.md). Conquistas de partidas antigas são enviadas ao abrir o jogo.
 * Para testar fora da Steam, crie steam_appid.txt com o App ID na pasta de onde o jogo é iniciado.
 */
final class Steam {
    static final String API = "com.codedisaster.steamworks.SteamAPI";

    /** O lado da Steam, separado para o teste poder trocar por um falso. */
    interface Backend { boolean unlock(String apiName); void store(); void runCallbacks(); void shutdown(); }

    final Game game;
    Backend backend;
    final Set<String> sent = new HashSet<>();
    double callbackT, syncT;

    Steam(Game game) { this.game = game; }

    static String apiName(String id) { return "ACH_" + id.toUpperCase(Locale.ROOT); }
    boolean active() { return backend != null; }

    void start() {
        if (backend != null) return;
        backend = steamworks();
        if (backend != null) Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    synchronized void stop() {
        if (backend == null) return;
        try { backend.shutdown(); } catch (RuntimeException e) { GameFiles.logError(e); }
        backend = null;
    }

    /** Chamado a cada passo do jogo: callbacks da Steam e envio das conquistas novas. */
    synchronized void update(double dt) {
        if (backend == null) return;
        try {
            callbackT -= dt; syncT -= dt;
            if (callbackT <= 0) { callbackT = 0.1; backend.runCallbacks(); }
            if (syncT <= 0) { syncT = 1; sync(); }
        } catch (RuntimeException e) { GameFiles.logError(e); stop(); }
    }

    void sync() {
        boolean changed = false;
        for (String id : game.explorer.unlocked)
            if (!sent.contains(id) && backend.unlock(apiName(id))) { sent.add(id); changed = true; }
        if (changed) backend.store();
    }

    // ---------- steamworks4j por reflexão ----------
    static Backend steamworks() {
        try {
            Class<?> api = Class.forName(API);
            ClassLoader loader = api.getClassLoader();
            Class<?> libLoader = Class.forName("com.codedisaster.steamworks.SteamLibraryLoader");
            Path dir = Files.createTempDirectory("aurora-steam");
            dir.toFile().deleteOnExit();
            Object natives = Proxy.newProxyInstance(loader, new Class<?>[]{libLoader}, (proxy, method, args) -> {
                if (method.getName().equals("loadLibrary")) return extract(loader, dir, (String) args[0]);
                if (method.getName().equals("setLibraryPath")) return null;
                if (method.isDefault()) return null;
                return method.getName().equals("hashCode") ? System.identityHashCode(proxy)
                    : method.getName().equals("equals") ? proxy == args[0] : "SteamLibraryLoader";
            });
            if (!(boolean) api.getMethod("loadLibraries", libLoader).invoke(null, natives)) return null;
            if (!(boolean) api.getMethod("init").invoke(null)) return null;            // Steam fechada ou sem App ID
            Class<?> callback = Class.forName("com.codedisaster.steamworks.SteamUserStatsCallback");
            Object listener = Proxy.newProxyInstance(loader, new Class<?>[]{callback}, (proxy, method, args) ->
                method.getName().equals("hashCode") ? System.identityHashCode(proxy)
                    : method.getName().equals("equals") ? proxy == args[0]
                    : method.getName().equals("toString") ? "AuroraStats" : null);
            Class<?> statsClass = Class.forName("com.codedisaster.steamworks.SteamUserStats");
            Object stats = statsClass.getConstructor(callback).newInstance(listener);
            Method set = statsClass.getMethod("setAchievement", String.class), store = statsClass.getMethod("storeStats");
            Method dispose = statsClass.getMethod("dispose"), run = api.getMethod("runCallbacks"), shutdown = api.getMethod("shutdown");
            return new Backend() {
                public boolean unlock(String name) { return (boolean) call(set, stats, name); }
                public void store() { call(store, stats); }
                public void runCallbacks() { call(run, null); }
                public void shutdown() { call(dispose, stats); call(shutdown, null); }
            };
        } catch (ClassNotFoundException e) {
            return null;                                                             // versão sem Steam
        } catch (ReflectiveOperationException | LinkageError | java.io.IOException | RuntimeException e) {
            GameFiles.logError(e);
            return null;
        }
    }

    static Object call(Method m, Object target, Object... args) {
        try { return m.invoke(target, args); } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** Copia a biblioteca nativa de dentro do jar da steamworks4j para uma pasta temporária e a carrega. */
    static boolean extract(ClassLoader loader, Path dir, String name) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        boolean x64 = System.getProperty("os.arch").contains("64");
        String file = os.contains("win") ? name + (x64 ? "64" : "") + ".dll"
            : os.contains("mac") ? "lib" + name + ".dylib" : "lib" + name + ".so";
        try (InputStream in = loader.getResourceAsStream(file)) {
            if (in == null) return false;
            Path out = dir.resolve(file);
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
            out.toFile().deleteOnExit();
            System.load(out.toAbsolutePath().toString());
            return true;
        } catch (java.io.IOException | LinkageError e) {
            GameFiles.logError(e);
            return false;
        }
    }
}
