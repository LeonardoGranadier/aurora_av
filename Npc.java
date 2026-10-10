import java.awt.Color;

/** Personagem ou objeto interativo de um mapa. */
class Npc {
    String id, name, sprite, when;           // when: "dia", "noite" ou null (sempre)
    int x, y, dir = 0, minStage = 0, maxStage = 99;
    Color color; boolean object;             // object = item brilhante (sem corpo)
    Npc(String id, String name, int x, int y, Color color) {
        this.id = id; this.name = name; this.x = x; this.y = y; this.color = color; this.sprite = id;
    }
    boolean still;                           // objeto parado (sem flutuar), ex.: cama, portão
    boolean walking, sleeping;
    int toX, toY;
    double walkProgress, routeWait;
    Npc obj(String spriteKey) { object = true; sprite = spriteKey; return this; }
    Npc still() { still = true; return this; }
    Npc spr(String spriteKey) { sprite = spriteKey; return this; }
    Npc when(String w) { when = w; return this; }
    Npc stages(int min, int max) { minStage = min; maxStage = max; return this; }
}
