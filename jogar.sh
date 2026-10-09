#!/bin/sh
set -eu

game_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$game_dir"

if ! command -v javac >/dev/null 2>&1 || ! command -v java >/dev/null 2>&1; then
    printf '%s\n' 'Instale o JDK 17 ou superior para abrir Aventura em Aurora.' >&2
    exit 1
fi

mkdir -p .build
javac -encoding UTF-8 --release 17 -d .build ./*.java
if [ "${1:-}" = "--compilar" ]; then
    printf '%s\n' 'Compilacao concluida.'
    exit 0
fi
exec java -cp .build Game
