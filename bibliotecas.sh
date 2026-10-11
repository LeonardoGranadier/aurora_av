#!/bin/sh
# Baixa as bibliotecas opcionais para lib/ (conferindo o SHA-1 publicado no Maven Central).
# Hoje: Jamepad (controles via SDL2). Sem elas, o jogo funciona só com o teclado.
set -eu

game_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$game_dir/lib"
cd "$game_dir/lib"

repo=https://repo1.maven.org/maven2
sha1() { if command -v sha1sum >/dev/null 2>&1; then sha1sum "$1" | cut -d' ' -f1; else shasum -a 1 "$1" | cut -d' ' -f1; fi; }
fetch() {   # caminho no repositório, arquivo, sha1
    if [ -f "$2" ] && [ "$(sha1 "$2")" = "$3" ]; then return 0; fi
    if command -v curl >/dev/null 2>&1; then curl -fsSL -o "$2.tmp" "$repo/$1/$2"
    else wget -q -O "$2.tmp" "$repo/$1/$2"; fi
    if [ "$(sha1 "$2.tmp")" != "$3" ]; then
        rm -f "$2.tmp"; printf 'Soma de verificação inválida: %s\n' "$2" >&2; return 1
    fi
    mv "$2.tmp" "$2"
}
fetch com/badlogicgames/jamepad/jamepad/2.30.0.0 jamepad-2.30.0.0.jar 45560285a49564bcb1fc62e147ca8b577eab6012
fetch com/badlogicgames/gdx/gdx-jnigen-loader/2.2.0 gdx-jnigen-loader-2.2.0.jar f8bff67f1788d5f52fb8c21b2cdcee3b93dfb069
printf '%s\n' 'Bibliotecas prontas em lib/.'
