import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Onde o jogo encontra seus arquivos.
 *
 * Dados do jogador (partida e preferências) ficam na pasta indicada pela propriedade "aurora.dados";
 * sem ela, na pasta atual, como no desenvolvimento e nos testes. O lançador da versão distribuída
 * (Aurora.main) aponta essa propriedade para a pasta de dados do usuário no sistema.
 *
 * Os assets são procurados em "aurora.assets", na pasta atual e ao lado do código compilado (.jar ou .build/),
 * para que o jogo abra corretamente de qualquer diretório.
 */
final class GameFiles {
    private GameFiles() {}

    static final String DATA_PROPERTY = "aurora.dados", ASSETS_PROPERTY = "aurora.assets";

    static Path dataDir() { return Path.of(System.getProperty(DATA_PROPERTY, "")); }

    /** Arquivo de dados do jogador; relativo à pasta atual quando nenhuma pasta foi configurada. */
    static Path data(String name) { return dataDir().resolve(name); }

    /** Cria a pasta de dados, se necessário. Falhas aparecem depois, ao gravar. */
    static void ensureDataDir() {
        Path dir = dataDir();
        if (dir.toString().isEmpty()) return;
        try { Files.createDirectories(dir); } catch (Exception ignored) {}
    }

    /** Pasta assets/ encontrada, ou "assets" relativo (o jogo então desenha tudo por código). */
    static File assetsDir() {
        String configured = System.getProperty(ASSETS_PROPERTY);
        if (configured != null) return new File(configured);
        File local = new File("assets");
        if (local.isDirectory()) return local;
        try {
            File code = new File(GameFiles.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = code.isDirectory() ? code : code.getParentFile();          // .build/ ou a pasta do .jar
            for (File base : new File[]{dir, dir == null ? null : dir.getParentFile()}) {
                if (base != null && new File(base, "assets").isDirectory()) return new File(base, "assets");
            }
        } catch (Exception ignored) {}
        return local;
    }

    static File asset(String name) { return new File(assetsDir(), name); }

    /** Pasta de dados do usuário conforme o sistema operacional. */
    static Path userDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, "Aventura em Aurora");
        }
        if (os.contains("mac")) return Path.of(home, "Library", "Application Support", "Aventura em Aurora");
        String xdg = System.getenv("XDG_DATA_HOME");
        return (xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(home, ".local", "share")).resolve("aventura-aurora");
    }
}
