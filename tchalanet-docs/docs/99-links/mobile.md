# Mobile — liens (local)

## Mobile app docs (near-code)

- [Rules](./_ref/openspec/context/40-mobile-rules.md)
- [Version guard](./_ref/openspec/context/05-version-guard.md)
- [Non-negotiables](./_ref/openspec/context/10-non-negotiables.md)

> D’autres docs: `_ref/openspec/context/`.

---

## Règle “published bundle” (MkDocs)

- MkDocs ne peut lier que des fichiers présents dans `tchalanet-docs/docs/`.
- Les docs near-code sont copiées dans `99-links/_ref/**` par un script de sync.
- On n’édite jamais `_ref/**`; on édite les sources près du code.

### Sync avant serve/build

```bash
./tchalanet-docs/scripts/sync-ref-docs.sh
cd tchalanet-docs
mkdocs serve   # ou mkdocs build
```
