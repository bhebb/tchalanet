# Backend — liens (local)

## Server (near-code)

- [ARCHITECTURE](./_ref/server/ARCHITECTURE.md)
- [PLAYBOOK](./_ref/server/PLAYBOOK.md)
- [ROUTING_AND_API_PATHS_V1](./_ref/server/ROUTING_AND_API_PATHS_V1.md)
- [RLS](./_ref/server/rls.md)

> Les autres docs sont disponibles sous: `./_ref/server/`.

---

## Règle “published bundle” (MkDocs)

- MkDocs ne peut lier que des fichiers présents dans `tchalanet-docs/docs/`.
- Les docs near-code sont copiées dans `99-links/_ref/server/` par un script de sync.
- On n’édite jamais `_ref/**`; on édite les sources près du code.

### Sync avant serve/build

```bash
./tchalanet-docs/scripts/sync-ref-docs.sh
cd tchalanet-docs
mkdocs serve   # ou mkdocs build
```
