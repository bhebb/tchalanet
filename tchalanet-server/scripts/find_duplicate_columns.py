#!/usr/bin/env python3
from pathlib import Path
import re

p = Path('src/main/resources/db/migration/V70__audit_table.sql')
s = p.read_text(encoding='utf-8')

# split into create table blocks
pattern = re.compile(r"CREATE\s+TABLE\s+([^\(\s]+)\s*\((.*?)\)\s*;", re.IGNORECASE | re.DOTALL)

issues = []
for m in pattern.finditer(s):
    name = m.group(1)
    body = m.group(2)
    # extract column names (simple heuristic: lines with 'name type')
    cols = []
    for line in body.splitlines():
        line = line.strip()
        if not line or line.startswith('--') or line.upper().startswith('CONSTRAINT'):
            continue
        # remove trailing comma
        if line.endswith(','):
            line = line[:-1]
        # match column definitions like 'tenant_id uuid NULL' or 'id uuid NOT NULL'
        cm = re.match(r'"?([a-zA-Z0-9_]+)"?\s+', line)
        if cm:
            cols.append(cm.group(1))
    seen = set()
    dup = []
    for c in cols:
        if c in seen:
            dup.append(c)
        else:
            seen.add(c)
    if dup:
        issues.append((name, dup, cols))

if not issues:
    print('No duplicate columns found')
else:
    for name, dup, cols in issues:
        print(f'Table: {name} duplicated columns: {dup}')
        # print context
        print('Columns order:')
        print(', '.join(cols))
        print('---')

