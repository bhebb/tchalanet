# Versions

Ce site ne duplique pas les versions runtime/build/infra.
La source de vérité est le fichier à la racine du monorepo :

- [`VERSIONS.md`](../../../VERSIONS.md)

Règles :

- Toute PR qui change une version doit mettre à jour `VERSIONS.md`.
- Les agents IA doivent respecter `VERSIONS.md` et appliquer le **Version Guard** (OpenSpec).
