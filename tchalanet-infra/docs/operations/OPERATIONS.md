# Operations

## Topologie supportée

La topologie opérationnelle standard est :

- Traefik
- PostgreSQL
- Redis
- API
- Edge service

Firebase est le serveur d'authentification externe. L'infra ne démarre pas de
serveur d'auth local en staging ou production.

## Local IDE

```bash
make local-ide-up ENV=dev
```

Lance Traefik, PostgreSQL et Firebase Auth Emulator.

```bash
make local-ide-up-redis ENV=dev
```

Ajoute Redis.

## API en container

```bash
make local-api-up ENV=dev
make local-api-smoke ENV=dev
```

## Produit local

```bash
make local-product-up ENV=dev
```

## Staging / production

```bash
make up-staging
make up-prod
```

## Smoke

```bash
make smoke-staging
```

Le smoke staging vérifie API, Edge et Web.

## Runbooks

Procédures pas-à-pas pour les opérations critiques :

| Runbook | Quand l'utiliser |
|---|---|
| [RB-00 — Secrets & variables checklist](runbooks/RB-00-secrets-checklist.md) | **Lire en premier** — inventaire complet de tous les secrets requis staging + prod |
| [RB-01 — Provisionnement staging](runbooks/RB-01-staging-provision.md) | Première mise en service ou recréation complète du serveur staging |
| [RB-02 — Déploiement web CF Pages](runbooks/RB-02-web-cf-pages.md) | Mise en place du web Angular multi-app sur Cloudflare Pages |
| [RB-03 — Distribution mobile Android](runbooks/RB-03-mobile-distribution.md) | Build et distribution d'une version Android staging via Firebase App Distribution |

---

## Backup PostgreSQL

Les backups sont **automatisés et vérifiés** : workflow `db-backup.yml`, dump
`pg_dump -Fc` + rôles, vérification par restauration réelle, chiffrement `age`,
envoi vers Cloudflare R2, et répétition de restauration hebdomadaire.

| Sujet | Document |
| --- | --- |
| Fonctionnement, cadences, rétention, clés | [`BACKUP-RESTORE.md`](./BACKUP-RESTORE.md) |
| Restaurer / répéter une restauration | [`BACKUP-RESTORE.md`](./BACKUP-RESTORE.md) |
| Reprise après perte totale du serveur | [`runbooks/RB-06-disaster-recovery.md`](./runbooks/RB-06-disaster-recovery.md) |
| Où vivent les secrets | [`runbooks/RB-00-secrets-checklist.md`](./runbooks/RB-00-secrets-checklist.md) |

> Cette section décrivait auparavant une procédure manuelle vers Backblaze B2,
> chiffrée par `openssl` avec une passphrase, et une rétention 30/90 jours.
> Rien de cela n'a été mis en œuvre. Un opérateur la suivant pendant un
> incident se serait cru couvert. Elle est remplacée par les documents
> ci-dessus, qui décrivent ce qui tourne réellement.

Point encore ouvert, hérité de l'ancienne section et toujours pertinent :
**activer le verrouillage d'objets (Object Lock) sur le bucket de backup.**
Aujourd'hui, quelqu'un détenant les clés R2 peut supprimer les backups. C'est la
dernière faiblesse connue du dispositif, consignée dans `BACKUP-RESTORE.md`.
