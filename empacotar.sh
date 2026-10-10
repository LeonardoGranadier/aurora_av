#!/bin/sh
# Gera a versao distribuivel em dist/.
#   sh empacotar.sh              dist/AventuraEmAurora/ (jar + assets + lancadores) e um .zip
#   sh empacotar.sh --instalador tambem um instalador nativo com Java embutido (jpackage, JDK 17+),
#                                no formato do sistema atual (deb/rpm no Linux, msi/exe no Windows, dmg no macOS)
# Na versao distribuida, partida e preferencias ficam na pasta de dados do usuario (veja GameFiles.java).
set -eu

game_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$game_dir"

name=AventuraEmAurora
version=${AURORA_VERSAO:-1.0.0}
out=dist/$name
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT

rm -rf "$out" "dist/$name.zip"
mkdir -p "$out"

# Apenas o codigo do jogo: testes e o gerador de sprites ficam fora do pacote.
sources=$(ls ./*.java | grep -v 'Test\.java$' | grep -v '/MakeAssets\.java$')
# shellcheck disable=SC2086
javac -encoding UTF-8 --release 17 -d "$classes" $sources
jar --create --file "$out/$name.jar" --main-class Aurora -C "$classes" .
cp -R assets "$out/assets"
rm -rf "$out/assets/audio"              # WAVs gerados por "java Audio" sao opcionais; o jogo sintetiza o audio
cp LEIA-ME.txt "$out/"

cat > "$out/jogar.sh" <<'EOF'
#!/bin/sh
cd "$(dirname -- "$0")" && exec java -jar AventuraEmAurora.jar
EOF
chmod +x "$out/jogar.sh"
printf '@echo off\r\ncd /d "%%~dp0"\r\nstart "" javaw -jar AventuraEmAurora.jar\r\n' > "$out/jogar.bat"

if command -v zip >/dev/null 2>&1; then
    (cd dist && zip -qr "$name.zip" "$name")
    printf 'Pacote: dist/%s.zip\n' "$name"
fi
printf 'Pasta:  %s (requer Java 17+ instalado)\n' "$out"

if [ "${1:-}" = "--instalador" ]; then
    if ! command -v jpackage >/dev/null 2>&1; then
        printf '%s\n' 'jpackage nao encontrado: instale um JDK 17+ completo.' >&2
        exit 1
    fi
    jpackage --input "$out" --main-jar "$name.jar" --main-class Aurora \
        --name "$name" --app-version "$version" --dest dist \
        --description "Aventura em Aurora" --vendor "Aventura em Aurora"
    printf 'Instalador gerado em dist/\n'
fi
