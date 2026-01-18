# Infra — liens (local)

## Docs infra (near-code)

- [README infra](./_ref/infra/README.md)
- [Keycloak config](./_ref/infra/WEB-KEYCLOAK-CONFIG.md)
- [Allowed hosts](./_ref/infra/VITE-ALLOWED-HOSTS.md)

> D’autres docs infra: `./_ref/infra/`.

---

## Règle “published bundle” (MkDocs)

- MkDocs ne peut lier que des fichiers présents dans `tchalanet-docs/docs/`.
- Les docs near-code sont copiées dans `99-links/_ref/infra/` par un script de sync.
- On n’édite jamais `_ref/**`; on édite les sources près du code.

### Sync avant serve/build

```bash
./tchalanet-docs/scripts/sync-ref-docs.sh
cd tchalanet-docs
mkdocs serve   # ou mkdocs build
```
