import java.util.*;

/** Grade de tiles de uma região, com passagens e personagens. */
class GameMap {
    String name; int w, h; char[][] t; boolean outdoor, dark, snowy; char clear = '.';
    List<Warp> warps = new ArrayList<>();
    List<Npc> npcs = new ArrayList<>();

    GameMap(String name, int w, int h, char fill, boolean outdoor) {
        this.name = name; this.w = w; this.h = h; this.outdoor = outdoor;
        t = new char[h][w];
        for (char[] row : t) Arrays.fill(row, fill);
    }
    void rect(int x, int y, int rw, int rh, char c) {
        for (int j = y; j < y + rh; j++)
            for (int i = x; i < x + rw; i++)
                if (i >= 0 && j >= 0 && i < w && j < h) t[j][i] = c;
    }
    void border(char c) { rect(0, 0, w, 1, c); rect(0, h - 1, w, 1, c); rect(0, 0, 1, h, c); rect(w - 1, 0, 1, h, c); }
    void house(int x, int y, boolean door) { rect(x, y, 5, 3, 'H'); if (door) t[y + 2][x + 2] = 'D'; }
    char at(int x, int y) { return (x < 0 || y < 0 || x >= w || y >= h) ? '#' : t[y][x]; }
    boolean solid(int x, int y) { return "#~HLXR".indexOf(at(x, y)) >= 0; }
    void addNpc(Npc n) { npcs.add(n); if (t[n.y][n.x] == '#') t[n.y][n.x] = clear; }
}
