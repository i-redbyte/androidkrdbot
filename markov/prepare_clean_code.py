#!/usr/bin/env python3
"""Extract Russian prose sentences from the supplied Clean Code PDF.

Usage: python3 markov/prepare_clean_code.py /path/to/clean-code.pdf
The generated corpus is local to the machine building the bot.
"""

import argparse
import re
from pathlib import Path

import pdfplumber

from prepare_corpus import clean_sentence, format_sentence, split_sentences

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_OUTPUT = ROOT / "src/main/resources/markov/clean_code_sentences.txt"
RUSSIAN_LETTERS = re.compile(r"[А-ЯЁа-яё]")
LETTERS = re.compile(r"[A-Za-zА-ЯЁа-яё]")
TOKEN = re.compile(r"[A-Za-zА-ЯЁа-яё0-9]+|[^\w\s]")
CODE_START = re.compile(
    r"^(?:public|private|protected|class|interface|package|import|return|throw|"
    r"if\s*\(|for\s*\(|while\s*\(|assert|static|void|int|String|//|/\*|\*|@Test)\b",
    re.IGNORECASE,
)


def is_prose_line(line: str) -> bool:
    stripped = line.strip()
    if not stripped or CODE_START.match(stripped):
        return False
    if any(marker in stripped for marker in ("{", "}", "//", "/*", "*/", "=>", "==")):
        return False
    letters = LETTERS.findall(stripped)
    return len(letters) >= 3 and len(RUSSIAN_LETTERS.findall(stripped)) / len(letters) >= 0.55


def is_heading_line(line: str, following: str) -> bool:
    stripped = line.strip()
    next_line = following.strip()
    return (
        len(stripped) <= 62
        and len(stripped.split()) <= 7
        and not stripped.endswith((".", "!", "?", ":", ",", ";", "—", "-"))
        and re.match(r"^[А-ЯЁ]", stripped) is not None
        and re.match(r"^[А-ЯЁ]", next_line) is not None
    )


def page_prose(page) -> str:
    raw = page.extract_text(x_tolerance=2) or ""
    # Remove line-break hyphenation before filtering the lines.
    raw = re.sub(r"([А-ЯЁа-яё])-\n(?=[А-ЯЁа-яё])", r"\1", raw)
    lines = raw.splitlines()
    if lines:
        lines = lines[1:]  # running head or chapter title
    if lines and re.fullmatch(r"\s*\d+\s*", lines[-1]):
        lines.pop()
    return " ".join(
        line.strip()
        for index, line in enumerate(lines)
        if is_prose_line(line)
        and not is_heading_line(line, lines[index + 1] if index + 1 < len(lines) else "")
    )


def usable_sentence(tokens: list[str]) -> str | None:
    sentence = clean_sentence(format_sentence(tokens))
    sentence = re.sub(r"^С любезного разрешения [^)]+\)\s+", "", sentence)
    sentence = re.sub(r"^.{1,45}\s+(?=В этой главе\b)", "", sentence)
    words = re.findall(r"[А-ЯЁа-яё]+", sentence)
    if not 8 <= len(words) <= 70:
        return None
    letters = LETTERS.findall(sentence)
    if not letters or len(RUSSIAN_LETTERS.findall(sentence)) / len(letters) < 0.78:
        return None
    if not re.match(r'^[«„“"(]*[А-ЯЁ]', sentence):
        return None
    if any(marker in sentence for marker in ("{", "}", "//", "/*", "*/", "==", "->")):
        return None
    if any(marker in sentence for marker in ("•", "", "")):
        return None
    if sentence.count("(") > 3 or sentence.count(";") > 2:
        return None
    if re.search(r"\b(?:листинг|оглавление|алфавитный указатель)\b", sentence, re.IGNORECASE):
        return None
    if len(sentence) > 900:
        return None
    return re.sub(r"\s*\[[^\]]{1,30}\]", "", sentence)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()

    prose = []
    with pdfplumber.open(args.pdf) as document:
        # Physical pages 20–384 contain the book's main prose and first appendix.
        for page_number in range(20, min(384, len(document.pages)) + 1):
            prose.append(page_prose(document.pages[page_number - 1]))
    tokens = TOKEN.findall(" ".join(prose))
    sentences = []
    seen = set()
    for segment in split_sentences(tokens):
        sentence = usable_sentence(segment)
        if sentence and sentence not in seen:
            sentences.append(sentence)
            seen.add(sentence)

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text("\n".join(sentences) + "\n", encoding="utf-8")
    print(f"Wrote {len(sentences)} Clean Code sentences to {args.output}")


if __name__ == "__main__":
    main()
