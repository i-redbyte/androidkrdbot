#!/usr/bin/env python3
"""Turn the existing whitespace-tokenized book into sentence-level training data.

Usage: python3 markov/prepare_corpus.py [input] [output]
The input is the committed legacy corpus. Each output line is one sentence.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "clean_code_tokenized.txt"
TARGET = ROOT.parent / "src/main/resources/markov/lenin_sentences.txt"
ABBREVIATIONS = {"г", "гг", "гл", "др", "им", "напр", "н", "пр", "рис", "см", "ст", "стр", "т", "тд", "тп"}


def remove_footnotes(tokens: list[str]) -> list[str]:
    cleaned = []
    depth = 0
    for token in tokens:
        if token == "[":
            depth += 1
        elif token == "]" and depth:
            depth -= 1
        elif depth == 0:
            cleaned.append(token)
    return cleaned


def split_sentences(tokens: list[str]) -> list[list[str]]:
    sentences = []
    current = []
    for index, token in enumerate(tokens):
        current.append(token)
        if token not in {".", "!", "?"}:
            continue
        previous = tokens[index - 1] if index else ""
        # Initials, abbreviations, decimal parts and numbered headings are not sentences.
        if token == "." and (
            previous.lower() in ABBREVIATIONS
            or (len(previous) == 1 and previous.isalpha())
            or previous.isdigit()
        ):
            continue
        sentences.append(current)
        current = []
    return sentences


def format_sentence(tokens: list[str]) -> str:
    text = " ".join(tokens)
    text = re.sub(r"\s+([,.!?;:%»\]\)])", r"\1", text)
    text = re.sub(r"([«\[(])\s+", r"\1", text)
    text = re.sub(r"(?<=\w)\s+-\s+(?=\w)", "-", text)
    return text.strip()


def clean_sentence(text: str) -> str:
    # Closing quotes and ellipses sometimes belong to the preceding sentence.
    text = re.sub(r"^[\s»….)]+", "", text)
    while text.count("«") > text.count("»"):
        text = text.replace("«", "", 1)
    while text.count("»") > text.count("«"):
        index = text.rfind("»")
        text = text[:index] + text[index + 1:]
    return text.strip()


def main() -> None:
    source = Path(sys.argv[1]) if len(sys.argv) > 1 else SOURCE
    target = Path(sys.argv[2]) if len(sys.argv) > 2 else TARGET
    tokens = source.read_text(encoding="utf-8").split()

    # Front matter and editorial notes are poor material for short replies.
    start = tokens.index("Предисловие")
    notes = tokens.index("Примечания") if "Примечания" in tokens else len(tokens)
    tokens = remove_footnotes(tokens[start:notes])

    sentences = []
    for sentence in split_sentences(tokens):
        words = [token for token in sentence if any(char.isalpha() for char in token)]
        if not 3 <= len(words) <= 150:
            continue
        # Heading fragments are mostly uppercase, unlike body sentences.
        if sum(word.isupper() and len(word) > 1 for word in words) > len(words) // 3:
            continue
        rendered = clean_sentence(format_sentence(sentence))
        if re.search(r"\b(?:Глава|Предисловие|Послесловие|Примечания)\b", rendered):
            continue
        if re.match(r"^Автор (?:Август|Москва|Петроград)\b", rendered):
            continue
        if re.match(r"^\d+[.)]\s", rendered):
            continue
        if not re.match(r'^[«„“"(]*[А-ЯЁA-Z]', rendered):
            continue
        sentences.append(rendered)

    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text("\n".join(sentences) + "\n", encoding="utf-8")
    print(f"Wrote {len(sentences)} sentences to {target}")


if __name__ == "__main__":
    main()
