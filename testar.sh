#!/bin/sh
# Compila e executa todos os testes, cada um numa pasta temporaria propria
# (os testes recusam rodar onde ja exista uma partida salva).
#   sh testar.sh                 todos os testes
#   sh testar.sh WorldMapTest    apenas os indicados
set -eu

game_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$game_dir"

build_dir=$(mktemp -d)
trap 'rm -rf "$build_dir"' EXIT
javac -encoding UTF-8 --release 17 -d "$build_dir" ./*.java

if [ "$#" -gt 0 ]; then
    tests="$*"
else
    tests=$(for f in *Test.java; do printf '%s\n' "${f%.java}"; done)
fi

failed=""
for test in $tests; do
    work=$(mktemp -d)
    ln -s "$game_dir/assets" "$work/assets"
    if (cd "$work" && java -Djava.awt.headless=true -cp "$build_dir" "$test" > saida.txt 2>&1); then
        printf 'ok    %-22s %s\n' "$test" "$(tail -n 1 "$work/saida.txt")"
        rm -rf "$work"
    else
        printf 'FALHA %-22s (saida e imagens em %s)\n' "$test" "$work"
        tail -n 15 "$work/saida.txt" | sed 's/^/      /'
        failed="$failed $test"
    fi
done

if [ -n "$failed" ]; then
    printf '\nFalharam:%s\n' "$failed"
    exit 1
fi
printf '\nTodos os testes passaram.\n'
