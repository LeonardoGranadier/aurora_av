import java.awt.Color;
import java.util.Random;

/** Mapas da campanha principal. Sistemas adicionais acrescentam os seus em buildWorld(). */
final class World {
    private World() {}

    static void build(Game game) {
        Random r = new Random(7);

        // Vila Aurora
        GameMap v = new GameMap("Vila Aurora", 30, 22, '.', true);
        for (int i = 0; i < 45; i++) v.t[r.nextInt(22)][r.nextInt(30)] = r.nextInt(3) == 0 ? '#' : 'F';
        v.border('#');
        v.house(4, 4, true);
        v.house(16, 4, false);
        v.rect(6, 7, 1, 5, '=');
        v.rect(6, 10, 24, 2, '=');
        v.rect(14, 10, 2, 12, '=');
        v.rect(14, 0, 2, 10, '=');                                  // trilha da montanha
        v.rect(20, 14, 6, 4, '~');
        v.warps.add(new Warp(6, 6, "Casa", 5, 6));
        v.warps.add(new Warp(29, 10, "Floresta Verde", 1, 10));
        v.warps.add(new Warp(29, 11, "Floresta Verde", 1, 11));
        v.warps.add(new Warp(14, 21, "Praia Azul", 14, 1));
        v.warps.add(new Warp(15, 21, "Praia Azul", 15, 1));
        v.addNpc(new Npc("prof", "Prof. Carvalho", 10, 8, new Color(240, 240, 240)));
        v.addNpc(new Npc("placa", "", 14, 1, new Color(190, 150, 95)).obj("placa").still().stages(0, 15));    // trilha fechada até o cap. 4
        v.addNpc(new Npc("placa", "", 15, 1, new Color(190, 150, 95)).obj("placa").still().stages(0, 15));
        v.warps.add(new Warp(14, 0, "Trilha da Montanha", 10, 30));
        v.warps.add(new Warp(15, 0, "Trilha da Montanha", 11, 30));
        v.addNpc(new Npc("frag1", "", 19, 15, new Color(255, 240, 150)).obj("frag").when("noite").stages(6, 6));
        game.maps.put(v.name, v);
        game.life.buildVillage(v);

        // Floresta Verde
        GameMap f = new GameMap("Floresta Verde", 30, 22, '.', true);
        for (int y = 0; y < 22; y++) for (int x = 0; x < 30; x++) if (r.nextInt(100) < 38) f.t[y][x] = '#';
        f.rect(5, 3, 5, 3, '~');
        f.rect(21, 2, 7, 4, '.');
        f.border('#');
        f.rect(0, 10, 26, 2, '=');
        f.rect(24, 4, 2, 8, '=');
        f.rect(12, 0, 6, 3, 'R'); f.t[2][14] = 'O'; f.t[2][15] = 'O';   // entrada da gruta
        f.rect(14, 3, 2, 7, '=');
        f.warps.add(new Warp(0, 10, "Vila Aurora", 28, 10));
        f.warps.add(new Warp(0, 11, "Vila Aurora", 28, 11));
        f.warps.add(new Warp(14, 2, "Gruta do Eco", 11, 14));
        f.warps.add(new Warp(15, 2, "Gruta do Eco", 12, 14));
        f.addNpc(new Npc("lenhador", "Lenhador", 10, 9, new Color(170, 60, 40)));
        f.addNpc(new Npc("concha", "", 24, 4, new Color(255, 215, 80)).obj("concha"));
        game.maps.put(f.name, f);

        // Praia Azul
        GameMap b = new GameMap("Praia Azul", 30, 22, 's', true);
        for (int i = 0; i < 14; i++) b.t[2 + r.nextInt(11)][1 + r.nextInt(28)] = '#';
        b.border('#');
        b.rect(0, 14, 30, 8, '~');
        b.rect(14, 0, 2, 1, '=');
        b.rect(21, 3, 3, 5, 'L');
        b.rect(5, 13, 2, 5, 'P');                                   // cais
        b.warps.add(new Warp(14, 0, "Vila Aurora", 14, 20));
        b.warps.add(new Warp(15, 0, "Vila Aurora", 15, 20));
        b.addNpc(new Npc("marinho", "Seu Marinho", 17, 9, new Color(40, 90, 160)));
        b.addNpc(new Npc("frag2", "", 8, 10, new Color(255, 240, 150)).obj("frag").when("dia").stages(6, 6));
        b.addNpc(new Npc("garrafa", "", 12, 12, new Color(170, 225, 240)).obj("garrafa").when("noite").stages(4, 4));
        b.addNpc(new Npc("barco", "", 5, 18, new Color(140, 90, 50)).obj("barco").still().stages(8, 99));
        b.addNpc(new Npc("capita_cais", "Capitã Estrela", 6, 15, new Color(40, 140, 150)).spr("capita").stages(8, 10).when("dia"));
        b.addNpc(new Npc("porta_farol", "", 22, 8, new Color(70, 85, 140)).obj("porta_farol").still());
        b.addNpc(new Npc("capita_fim", "Capitã Estrela", 6, 15, new Color(40, 140, 150)).spr("capita").stages(14, 99));
        game.maps.put(b.name, b);

        // Casa do jogador
        GameMap c = new GameMap("Casa", 10, 8, 'f', false);
        c.border('X');
        c.rect(7, 1, 2, 2, 'X');
        c.t[7][5] = 'D';
        c.warps.add(new Warp(5, 7, "Vila Aurora", 6, 7));
        c.addNpc(new Npc("mae", "Mamãe", 3, 3, new Color(200, 90, 150)));
        c.addNpc(new Npc("cama", "", 1, 1, new Color(200, 60, 60)).obj("cama").still());
        game.maps.put(c.name, c);

        // Gruta do Eco (escura)
        GameMap g = new GameMap("Gruta do Eco", 24, 16, 'R', false);
        g.dark = true;
        g.rect(9, 11, 6, 4, 'c');   // entrada
        g.rect(11, 7, 2, 4, 'c');   // corredor
        g.rect(6, 2, 12, 5, 'c');   // salão principal
        g.rect(18, 4, 1, 2, 'c');   // passagem
        g.rect(19, 2, 3, 5, 'c');   // câmara do fundo
        g.t[12][10] = 'R'; g.t[4][8] = 'R'; g.t[5][15] = 'R'; g.t[3][20] = 'R';
        g.t[15][11] = 'O'; g.t[15][12] = 'O';
        g.warps.add(new Warp(11, 15, "Floresta Verde", 14, 3));
        g.warps.add(new Warp(12, 15, "Floresta Verde", 15, 3));
        g.addNpc(new Npc("capita", "Capitã Estrela", 11, 4, new Color(40, 140, 150)).stages(0, 7));
        g.addNpc(new Npc("frag3", "", 21, 3, new Color(255, 240, 150)).obj("frag").stages(6, 6));
        game.maps.put(g.name, g);

        // Ilha das Estrelas
        GameMap il = new GameMap("Ilha das Estrelas", 28, 20, '~', true);
        il.rect(2, 3, 24, 14, 's');
        il.rect(5, 5, 18, 10, '.');
        for (int k = 0; k < 40; k++) {
            int x = 5 + r.nextInt(18), y = 5 + r.nextInt(10);
            if (il.t[y][x] == '.') il.t[y][x] = r.nextInt(3) == 0 ? '#' : 'F';
        }
        il.rect(17, 2, 8, 4, 'R'); il.t[5][21] = 'O'; il.t[5][22] = 'O';      // fachada do templo
        il.rect(4, 10, 19, 1, '=');
        il.rect(21, 6, 2, 5, '=');
        il.rect(1, 10, 3, 1, 'P');                                             // cais da ilha
        il.warps.add(new Warp(21, 5, "Templo Estrelado", 8, 14));
        il.warps.add(new Warp(22, 5, "Templo Estrelado", 9, 14));
        il.addNpc(new Npc("capita_ilha", "Capitã Estrela", 6, 9, new Color(40, 140, 150)).spr("capita").stages(11, 14));
        il.addNpc(new Npc("barco_ilha", "", 0, 10, new Color(140, 90, 50)).obj("barco").still());
        il.addNpc(new Npc("fogueira", "", 7, 12, new Color(255, 140, 40)).obj("fogueira").still());
        il.addNpc(new Npc("estatua", "", 19, 7, new Color(160, 160, 170)).obj("estatua").still());
        il.addNpc(new Npc("portao", "", 21, 6, new Color(110, 110, 125)).obj("portao").still().when("dia"));   // some à noite
        il.addNpc(new Npc("portao", "", 22, 6, new Color(110, 110, 125)).obj("portao").still().when("dia"));
        game.maps.put(il.name, il);

        // Templo Estrelado (escuro)
        GameMap tp = new GameMap("Templo Estrelado", 18, 16, 'R', false);
        tp.dark = true;
        tp.rect(7, 12, 4, 3, 'm');   // entrada
        tp.rect(8, 10, 2, 2, 'm');   // corredor
        tp.rect(3, 5, 12, 5, 'm');   // salão dos braseiros
        tp.rect(8, 3, 2, 2, 'm');    // passagem selada
        tp.rect(6, 1, 6, 2, 'm');    // santuário
        tp.t[15][8] = 'O'; tp.t[15][9] = 'O';
        tp.warps.add(new Warp(8, 15, "Ilha das Estrelas", 21, 7));
        tp.warps.add(new Warp(9, 15, "Ilha das Estrelas", 22, 7));
        tp.addNpc(new Npc("br1", "", 13, 7, new Color(255, 160, 60)).obj("brasa_off").still());   // Sol (leste)
        tp.addNpc(new Npc("br2", "", 4, 7, new Color(255, 160, 60)).obj("brasa_off").still());    // Lua (oeste)
        tp.addNpc(new Npc("br3", "", 11, 5, new Color(255, 160, 60)).obj("brasa_off").still());   // Estrela (norte)
        tp.addNpc(new Npc("mural", "", 5, 9, new Color(120, 115, 125)).obj("mural").still());
        tp.addNpc(new Npc("portao2", "", 8, 4, new Color(110, 110, 125)).obj("portao").still());
        tp.addNpc(new Npc("portao2", "", 9, 4, new Color(110, 110, 125)).obj("portao").still());
        tp.addNpc(new Npc("altar", "", 8, 1, new Color(120, 230, 255)).obj("altar").still());
        game.maps.put(tp.name, tp);

        // ===== Capítulo 4 =====
        // Trilha da Montanha: neve + campo de gelo (layout validado por busca: 11 movimentos, com volta e sem becos)
        GameMap tr = new GameMap("Trilha da Montanha", 22, 32, 'n', true);
        tr.snowy = true; tr.clear = 'n';
        for (int k = 0; k < 70; k++) tr.t[r.nextInt(32)][r.nextInt(22)] = '#';
        tr.border('#');
        tr.rect(1, 12, 20, 13, '#');                      // faixa de floresta de lado a lado: o único caminho é pelo gelo
        tr.rect(1, 5, 20, 1, '#');                        // barreira: o portão de gelo (16,5) é a única passagem
        tr.rect(4, 13, 14, 11, 'i');
        int[][] pedras = {{4, 16}, {4, 18}, {4, 19}, {5, 13}, {8, 22}, {9, 13}, {10, 15}, {12, 14}, {12, 16}, {12, 23},
                          {13, 17}, {13, 20}, {13, 21}, {14, 14}, {15, 13}, {15, 17}};
        for (int[] pd : pedras) tr.t[pd[1]][pd[0]] = 'R';
        tr.rect(10, 24, 2, 8, '=');                       // entrada pelo sul
        tr.rect(16, 0, 1, 13, '=');                       // saída do gelo até o pico
        tr.rect(5, 9, 12, 1, '=');                        // ramal da cabana
        tr.house(3, 5, true);
        tr.rect(5, 8, 1, 1, '=');
        tr.warps.add(new Warp(10, 31, "Vila Aurora", 14, 1));
        tr.warps.add(new Warp(11, 31, "Vila Aurora", 15, 1));
        tr.warps.add(new Warp(5, 7, "Cabana do Eremita", 5, 6));
        tr.warps.add(new Warp(16, 0, "Pico Estrelado", 10, 18));
        tr.addNpc(new Npc("montanhista", "Montanhista", 9, 28, new Color(200, 60, 60)));
        tr.addNpc(new Npc("gelo", "", 16, 5, new Color(170, 228, 250)).obj("gelo").still().stages(0, 18));
        game.maps.put(tr.name, tr);

        GameMap cb = new GameMap("Cabana do Eremita", 10, 8, 'f', false);
        cb.border('X'); cb.t[7][5] = 'D';
        cb.warps.add(new Warp(5, 7, "Trilha da Montanha", 5, 8));
        cb.addNpc(new Npc("eremita", "Eremita", 3, 3, new Color(110, 80, 60)));
        cb.addNpc(new Npc("fogao", "", 7, 2, new Color(75, 75, 85)).obj("fogao_off").still());
        game.maps.put(cb.name, cb);

        GameMap pc = new GameMap("Pico Estrelado", 20, 20, 'n', true);
        pc.snowy = true; pc.clear = 'n';
        for (int k = 0; k < 45; k++) pc.t[r.nextInt(20)][r.nextInt(20)] = '#';
        pc.border('#');
        pc.rect(8, 6, 5, 5, 'c');                         // cratera
        pc.t[5][7] = 'R'; pc.t[5][13] = 'R'; pc.t[11][7] = 'R'; pc.t[11][13] = 'R';
        pc.rect(9, 11, 2, 9, '=');
        pc.warps.add(new Warp(9, 19, "Trilha da Montanha", 16, 1));
        pc.warps.add(new Warp(10, 19, "Trilha da Montanha", 16, 1));
        pc.addNpc(new Npc("cintila", "", 10, 8, new Color(255, 230, 110)).obj("cintila").stages(19, 19));
        pc.addNpc(new Npc("treno", "", 15, 12, new Color(160, 110, 60)).obj("treno").still());
        game.maps.put(pc.name, pc);
    }
}
