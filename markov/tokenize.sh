#!/usr/bin/env bash
#
#  Преобразует произвольный .txt-файл в токенизированный формат,
#  пригодный для цепей Маркова:
#    • каждое слово и знак пунктуации — отдельный токен
#    • токены разделены одиночными пробелами
#    • регистр сохраняется
#  Результат записывается в  <имя_файла>_tokenized.txt  рядом с исходником.
#
#  Использование:
#      ./tokenize.sh path/to/book.txt
#

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <input.txt>" >&2
  exit 1
fi

INPUT="$1"
[[ -f "$INPUT" ]] || { echo "File not found: $INPUT" >&2; exit 1; }

DIR=$(dirname "$INPUT")
BASE=$(basename "$INPUT")
NAME="${BASE%.*}"
OUTPUT="${DIR}/${NAME}_tokenized.txt"

# --- токенизация -----------------------------------------------------------

perl -0777 -pe '
    s/\r//g;
    s/\.{3}/ … /g;
    s/([[:punct:]])/ $1 /g;
    s/[0-9]+/ /g;
    s/[A-Za-z]+/ /g;
    s/[*\/{}\[\];()&=+]+/ /g;
    s/\s+/ /g;
    s/^\s+|\s+$//g;
' "$INPUT" > "$OUTPUT"

echo "Tokenized file written to: $OUTPUT"
