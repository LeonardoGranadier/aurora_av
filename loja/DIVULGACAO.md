# Divulgação — Aventura em Aurora

Página: https://lgcal65.itch.io/aventura-em-aurora

Material pronto nesta pasta:

| Arquivo | Uso |
|---|---|
| `gifs/aventura-em-aurora-pt-leve.gif` / `-en-leve.gif` | **GIF principal leve (1,5 MB)**: vila, pesca, barco e aurora. Use este no devlog da itch.io (limite de 3 MB) e em redes com limite de tamanho |
| `gifs/aventura-em-aurora-pt.mp4` / `-en.mp4` | O mesmo trecho em vídeo MP4 (0,8 MB, 1280 × 960): o mais nítido, para X, Bluesky, Reddit e Discord |
| `gifs/aventura-em-aurora-pt.gif` / `-en.gif` | Versão completa do GIF (7 MB), para lugares sem limite de tamanho |
| `gifs/vila-pt.gif` / `-en.gif` | Passeio pela vila na primavera |
| `gifs/pesca-pt.gif` / `-en.gif` | Minijogo de pesca, da boia à captura |
| `gifs/alto-mar.gif` | A Gaivota navegando (sem texto na tela: serve para os dois idiomas) |
| `gifs/aurora.gif` | A aurora sobre a vila à noite (sem texto) |
| `capturas/*.png` | Imagens estáticas (1280 × 960) |
| `capa.png` | Capa (630 × 500) |

Os GIFs são gerados pelo próprio jogo (`Gifs.java`): depois de uma atualização visual, rode
`java -Daurora.dados=$(mktemp -d) -cp .build Gifs` (com o jogo compilado em `.build/` por `sh jogar.sh --compilar`).
As versões leves e o MP4 saem do GIF completo com o ffmpeg:

```
cd loja/gifs && for l in pt en; do
  ffmpeg -y -i aventura-em-aurora-$l.gif -vf "scale=1280:960:flags=neighbor,format=yuv420p" -c:v libx264 -preset slow -crf 20 -movflags +faststart aventura-em-aurora-$l.mp4
  ffmpeg -y -i aventura-em-aurora-$l.gif -vf "fps=10,split[a][b];[a]palettegen=max_colors=128:stats_mode=diff[p];[b][p]paletteuse=dither=none:diff_mode=rectangle" aventura-em-aurora-$l-leve.gif
done
```

## Ordem sugerida

1. **Dia 1:** devlog de lançamento na itch.io (abaixo) e post nos grupos brasileiros.
2. **Dia 2:** Reddit (um subreddit por dia, nunca o mesmo texto em vários no mesmo dia).
3. **Dias 2 a 7:** um GIF por dia no X/Bluesky/Instagram, cada um mostrando uma coisa (vila, pesca, barco, aurora).
4. **Sábados:** poste uma imagem nova com #screenshotsaturday.
5. Responda **todos** os comentários nos primeiros dias: é o que mais faz o post subir e o que traz as melhores ideias.

Regras que evitam remoção: leia as regras de cada comunidade antes; poste como desenvolvedor ("eu fiz"), não como anúncio;
mostre o jogo (GIF) em vez de só um link; não poste o mesmo texto em vários lugares ao mesmo tempo.

---

## Devlog de lançamento na itch.io (aba Devlog › Write a new post)

**Título:** Aventura em Aurora 1.0 está no ar! 🌅 · **Post type:** Major Update or Launch · **Cover image:** `capa.png`

Nos anexos (Attachments), deixe os três pacotes `aventura-em-aurora-windows/mac/linux` (versão 1.0.0).

> Depois de muitos meses, **Aventura em Aurora** chegou à versão 1.0!
>
> É um jogo de exploração tranquilo, em pixel art, sobre uma vila à beira-mar, um farol apagado e uma estrelinha que caiu do céu. São **9 capítulos** de história, com pesca, culinária, jardinagem, estações do ano, um museu para completar (peixes raros, insetos e fósseis), um barco para navegar até o alto-mar e uma vila cheia de moradores com rotinas e aniversários.
>
> **O que tem nesta versão**
> - 9 capítulos, do farol apagado ao eclipse que ameaça o céu de Aurora
> - 30 conquistas, 12 peixes raros, 10 insetos e 8 fósseis
> - A Gaivota, seu próprio barco, e a Ilha da Tartaruga
> - Rádio com músicas de cada região e o jornal do dia
> - Português e inglês, controle (Xbox, PlayStation, Switch Pro, Steam Deck) e opções de acessibilidade
>
> O jogo é gratuito. Se você jogar, eu adoraria saber **onde travou, o que achou confuso e do que mais gostou**: deixe um comentário aqui embaixo. As próximas atualizações vão sair a partir do que vocês disserem.
>
> Obrigado por jogar! 💛

*(No começo do texto, use o botão de imagem do editor com `gifs/aventura-em-aurora-pt-leve.gif`: a itch.io aceita até 3 MB.)*

**English version (same devlog, below the Portuguese text):**

> **Aventura em Aurora 1.0 is out!** A cozy pixel-art exploration game about a seaside village, a dark lighthouse and a little star that fell from the sky. 9 story chapters, fishing, cooking, gardening, seasons, a museum to complete (rare fish, bugs and fossils), your own boat and a village full of residents with routines and birthdays. Free to play, in Portuguese and English, with controller support. I'd love to hear where you got stuck, what confused you and what you liked most — leave a comment!

---

## Grupos brasileiros (Facebook, Discord, WhatsApp, r/brdev e comunidades de gamedev BR)

> Oi, pessoal! Terminei meu primeiro jogo e queria compartilhar com vocês 😊
>
> **Aventura em Aurora** é um jogo de exploração tranquilo em pixel art, no estilo dos Pokémon de Game Boy e de Stardew Valley: uma vila à beira-mar, um farol apagado e uma estrelinha que caiu na montanha. Tem 9 capítulos, pesca, culinária, estações do ano, museu, barco e uma vila cheia de moradores.
>
> É de graça, roda em Windows, Mac e Linux (não precisa instalar Java) e é todo em português.
>
> 👉 https://lgcal65.itch.io/aventura-em-aurora
>
> Qualquer opinião ajuda muito, principalmente se vocês travarem em alguma parte!

*(Anexe `gifs/aventura-em-aurora-pt-leve.gif` ou o vídeo `gifs/aventura-em-aurora-pt.mp4`.)*

---

## Reddit (em inglês)

Use **um** destes por dia, sempre com o vídeo (`aventura-em-aurora-en.mp4`) ou um GIF anexado como mídia do post (não como link).

**r/IndieGaming** — título:
> After months of work, I finished my first game: a cozy pixel-art adventure about a dark lighthouse and a fallen star

**r/indiegames** — título:
> My cozy exploration game Aventura em Aurora just hit 1.0 — fishing, a museum to fill, your own boat and an aurora at the end

**r/PixelArt** (use o `aurora.gif` ou `alto-mar.gif`, sem texto na tela) — título:
> The night sky after you finish my game (all pixel art drawn in code)

**r/cozygames** — título:
> I made a free cozy game: fishing, cooking, bugs, fossils and a village by the sea

Primeiro comentário (cole logo depois de postar):
> Hi! I'm the dev. Aventura em Aurora is a free cozy adventure inspired by Game Boy Pokémon, Zelda and Stardew Valley: 9 story chapters, seasons, fishing, cooking, a museum (rare fish, bugs, fossils), your own boat and a village with routines and birthdays. Windows/Mac/Linux, Portuguese and English, controller support.
> https://lgcal65.itch.io/aventura-em-aurora
> I'd love to know where you get stuck — I'm updating it based on feedback!

---

## X (Twitter), Bluesky, Threads, Mastodon

Um por dia, cada um com o GIF indicado.

1. (`aventura-em-aurora-en.mp4`)
> My cozy pixel-art adventure Aventura em Aurora is out! 🌅 A seaside village, a dark lighthouse and a fallen star. Free on itch.io 👇
> https://lgcal65.itch.io/aventura-em-aurora
> #indiegame #pixelart #cozygames #gamedev

2. (`pesca-en.gif`)
> Fishing in Aventura em Aurora: 8 common fish, 12 rare ones that only bite in the right season, hour and weather 🎣
> #indiegame #pixelart #cozygames

3. (`alto-mar.gif`)
> Fix up your own little boat, the Seagull, and sail to the open sea ⛵
> #indiegame #pixelart #indiedev

4. (`aurora.gif`)
> Finish the story and the night sky never looks the same again ✨
> #pixelart #indiegame #screenshotsaturday

5. (`vila-pt.gif`, em português)
> Fiz um jogo! 🌅 Aventura em Aurora: exploração tranquila em pixel art, todo em português e de graça.
> https://lgcal65.itch.io/aventura-em-aurora
> #jogosindie #pixelart #gamedevbr #indiegame

Sábado: poste uma imagem nova com **#screenshotsaturday** (é a tag semanal mais vista de jogos indie).

---

## Discord (servidores de gamedev e de jogos indie, no canal de divulgação/"showcase")

> **Aventura em Aurora 1.0** — cozy pixel-art adventure 🌅
> Seaside village, dark lighthouse, fallen star. 9 chapters, fishing, cooking, seasons, museum, your own boat. Free · Win/Mac/Linux · PT/EN · controller.
> https://lgcal65.itch.io/aventura-em-aurora

---

## Imprensa e criadores de conteúdo (opcional)

Canais pequenos de YouTube/Twitch de jogos indie e cozy costumam aceitar jogos gratuitos. Mensagem curta:

> Olá! Fiz um jogo indie gratuito, **Aventura em Aurora**: exploração tranquila em pixel art, com pesca, culinária, museu e barco, em português e inglês (6 a 10 horas). Achei que combinaria com o seu canal. Página e GIFs: https://lgcal65.itch.io/aventura-em-aurora — se quiser, é só jogar; qualquer opinião ajuda muito. Obrigado!

---

## Acompanhar

- itch.io › **Analytics**: visitas, downloads e de onde vieram (anote qual post trouxe mais gente).
- Comentários e avaliações na página: responda e anote os problemas em uma lista para a versão 1.0.1.
