/**
 * Ponto de entrada da versão distribuída (.jar e instaladores).
 * Guarda partida e preferências na pasta de dados do usuário, e não na pasta do jogo,
 * que pode ser somente leitura depois de instalada. "java Game" mantém tudo na pasta atual.
 */
public final class Aurora {
    private Aurora() {}

    public static void main(String[] args) {
        if (System.getProperty(GameFiles.DATA_PROPERTY) == null)
            System.setProperty(GameFiles.DATA_PROPERTY, GameFiles.userDataDir().toString());
        GameFiles.ensureDataDir();
        Game.main(args);
    }
}
