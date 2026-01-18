# Tchalanet Docs — Workflow

## Published bundle (MkDocs)

MkDocs ne peut lier que des fichiers présents dans `tchalanet-docs/docs/`.
Les docs “source of truth” vivent près du code (apps/, libs/, tchalanet-server/docs/, tchalanet-infra/docs/, openspec/).

Nous synchronisons une copie de référence avant serve/build:

```bash
./scripts/sync-ref-docs.sh
cd tchalanet-docs
mkdocs serve   # ou mkdocs build
```

## Règles

- Ne pas éditer `docs/99-links/_ref/**` (généré).
- Éditer uniquement les sources near-code.
- Si vous ajoutez une nouvelle doc near-code, mettez à jour le script `scripts/sync-ref-docs.sh` pour la copier.

## Structure

- `docs/02-functional/` — Docs métier publiées (Domaines, Flows, Features)
- `docs/99-links/` — Pages de liens vers copies `_ref` (validées par MkDocs)
- `docs/99-links/_ref/` — Copies synchronisées des docs near-code (lecture seule)
