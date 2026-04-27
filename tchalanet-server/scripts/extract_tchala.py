#!/usr/bin/env python3
"""
Simple extractor for lotto.ht/tchala pages.
Usage:
  python3 scripts/extract_tchala.py --url https://lotto.ht/tchala --out-dir=src/main/resources/tchala
Or with a local file:
  python3 scripts/extract_tchala.py --file /tmp/tchala.html --out-dir=src/main/resources/tchala

This script is a best-effort scraper that extracts number, title and meaning. It writes CSV per language.
"""
import argparse
import csv
import sys
from pathlib import Path

try:
    from bs4 import BeautifulSoup
    import requests
except Exception as e:
    print("Missing dependency: please install beautifulsoup4 and requests: pip install beautifulsoup4 requests")
    sys.exit(1)


def fetch(url):
    r = requests.get(url, timeout=30)
    r.raise_for_status()
    return r.text


def parse(html, lang_hint=None):
    soup = BeautifulSoup(html, "html.parser")
    # Heuristic: look for entries with a number and text --> adapt if site markup changes
    items = []

    # Many Tchala pages list entries in <li> or <div class="tchala-item">
    candidates = soup.select('li, div')
    for c in candidates:
        text = c.get_text(separator='|').strip()
        # Try to find a number token 0..99 at start
        parts = [p.strip() for p in text.split('|') if p.strip()]
        if not parts:
            continue
        # heuristic: find a number token
        for p in parts:
            if p.isdigit() and 0 <= int(p) <= 99:
                number = int(p)
                # remaining pieces as title/meaning
                rest = [x for x in parts if x != p]
                title = rest[0] if rest else ''
                meaning = rest[1] if len(rest) > 1 else ''
                items.append((number, title, meaning))
                break
    return items


def write_csv(items, out_file, lang):
    out_file = Path(out_file)
    out_file.parent.mkdir(parents=True, exist_ok=True)
    with out_file.open('w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f, delimiter=';')
        writer.writerow(['number', 'title', 'meaning', 'tags', 'lang', 'note'])
        for number, title, meaning in items:
            writer.writerow([number, title, meaning, '', lang, ''])


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--url', help='URL to fetch')
    ap.add_argument('--file', help='Local HTML file to parse')
    ap.add_argument('--out-dir', default='src/main/resources/tchala')
    ap.add_argument('--lang', default='fr')
    args = ap.parse_args()

    if not args.url and not args.file:
        ap.error('either --url or --file is required')

    if args.url:
        html = fetch(args.url)
    else:
        html = Path(args.file).read_text(encoding='utf-8')

    items = parse(html, args.lang)
    out = Path(args.out_dir) / f'import_tchala_{args.lang}.csv'
    write_csv(items, out, args.lang)
    print(f'Wrote {len(items)} rows to {out}')


if __name__ == '__main__':
    main()

