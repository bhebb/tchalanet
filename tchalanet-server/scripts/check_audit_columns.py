#!/usr/bin/env python3
import re
from pathlib import Path

MIG_DIR = Path('src/main/resources/db/migration')
V70 = MIG_DIR / 'V70__audit_table.sql'
RE_CREATE = re.compile(r"CREATE\s+TABLE(?:\s+IF\s+NOT\s+EXISTS)?\s+([a-zA-Z0-9_.]+)\s*\(", re.IGNORECASE)

# read V70 blocks
text_v70 = V70.read_text(encoding='utf-8')
# find audit table blocks
blocks = {}
for m in RE_CREATE.finditer(text_v70):
    name = m.group(1)
    start = m.end()
    # find matching closing ); for this CREATE
    # naive approach: find next "CONSTRAINT .*_pkey PRIMARY KEY" and then the closing ");" after it
    # safer: find the next ")\n;" sequence after start
    idx = text_v70.find('\n);', start)
    if idx == -1:
        idx = text_v70.find('\n\n', start)
    block = text_v70[m.start(): idx+3] if idx!=-1 else text_v70[m.start():]
    blocks[name] = block

# gather all migration create tables (base tables)
base_tables = set()
for f in MIG_DIR.glob('V*.sql'):
    txt = f.read_text(encoding='utf-8')
    for m in RE_CREATE.finditer(txt):
        name = m.group(1)
        # ignore public.* aud entries if any
        if name.lower().endswith('_aud'):
            continue
        base_tables.add(name.split('.')[-1])

# required audit columns
req_cols = ['created_at', 'created_by', 'updated_at', 'updated_by', 'deleted_at', 'version']

report = []
for tbl in sorted(base_tables):
    aud_name = f'public.{tbl}_aud'
    aud_exists = aud_name in blocks
    missing = []
    if aud_exists:
        blk = blocks[aud_name].lower()
        for col in req_cols:
            if col not in blk:
                missing.append(col)
    else:
        missing = req_cols.copy()
    report.append((tbl, aud_exists, missing))

# Also list audit tables without base
aud_only = []
for name in sorted(blocks.keys()):
    if not name.startswith('public.'):
        continue
    base = name.split('.',1)[1]
    if base.endswith('_aud'):
        base_tbl = base[:-4]
        if base_tbl not in base_tables:
            aud_only.append(base)

# print report
print('Audit columns verification report')
print('Required cols: ' + ', '.join(req_cols))
print('')
for tbl,aud_exists,missing in report:
    status = 'OK' if aud_exists and not missing else 'MISSING'
    print(f"{tbl:30} | aud_exists={str(aud_exists):5} | missing={missing} | {status}")

print('\nAudit tables without corresponding base table:')
for a in aud_only:
    print(' - ' + a)

# exit code 0

