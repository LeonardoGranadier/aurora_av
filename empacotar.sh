#!/bin/sh
# Gera a versao distribuivel em dist/.
#   sh empacotar.sh              dist/AventuraEmAurora/ (jar + assets + lancadores) e um .zip
#   sh empacotar.sh --instalador tambem um instalador nativo com Java embutido (jpackage, JDK 17+),
#                                no formato do sistema atual (deb/rpm no Linux, msi/exe no Windows, dmg no macOS)
#   sh empacotar.sh --portatil   tambem dist/AventuraEmAurora-<sistema>.zip: pasta com Java embutido, sem instalar
#                                (o formato preferido na itch.io; gere em cada sistema ou pela automacao do GitHub)
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

# Apenas o codigo do jogo: testes e ferramentas (sprites, capturas da loja) ficam fora do pacote.
sources=$(ls ./*.java | grep -v 'Test\.java$' | grep -v '/MakeAssets\.java$' | grep -v '/Capturas\.java$' \
    | grep -v '/Characters\.java$' | grep -v '/Tiles\.java$')
# shellcheck disable=SC2086
javac -encoding UTF-8 --release 17 -d "$classes" $sources

# Bibliotecas opcionais (controle). O jar as encontra em lib/ pelo Class-Path do manifesto.
[ -f lib/jamepad-2.30.0.0.jar ] || sh ./bibliotecas.sh || printf '%s\n' 'Sem bibliotecas: o pacote funcionara so com teclado.'
manifest=$(mktemp)
printf 'Main-Class: Aurora\n' > "$manifest"
if ls lib/*.jar >/dev/null 2>&1; then
    mkdir -p "$out/lib" && cp lib/*.jar "$out/lib/"
    printf 'Class-Path: %s\n' "$(cd "$out" && ls lib/*.jar | tr '\n' ' ')" >> "$manifest"
fi
jar --create --file "$out/$name.jar" --manifest "$manifest" -C "$classes" .
rm -f "$manifest"
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

for option in "$@"; do
    if [ "$option" != "--instalador" ] && [ "$option" != "--portatil" ]; then continue; fi
    if ! command -v jpackage >/dev/null 2>&1; then
        printf '%s\n' 'jpackage nao encontrado: instale um JDK 17+ completo.' >&2
        exit 1
    fi
    if [ "$option" = "--instalador" ]; then
        jpackage --input "$out" --main-jar "$name.jar" --main-class Aurora \
            --name "$name" --app-version "$version" --dest dist \
            --description "Aventura em Aurora" --vendor "Aventura em Aurora"
        printf 'Instalador gerado em dist/\n'
    else
        case "$(uname -s)" in
            Linux*) system=linux ;; Darwin*) system=mac ;; MINGW*|MSYS*|CYGWIN*) system=windows ;; *) system=$(uname -s) ;;
        esac
        portable="dist/$name-$system"
        rm -rf "$portable" "$portable.zip"
        jpackage --type app-image --input "$out" --main-jar "$name.jar" --main-class Aurora \
            --name "$name" --app-version "$version" --dest "$portable"
        if command -v zip >/dev/null 2>&1; then (cd "$portable" && zip -qr "$game_dir/$portable.zip" .); fi
        printf 'Pacote portatil: %s (com Java embutido)\n' "$portable"
    fi
done
