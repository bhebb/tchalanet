# Tchalanet – Backend Server

Tchalanet est une plateforme multi-tenant pour la gestion de jeux de loterie / borlette
(web + mobile). Ce dépôt contient le **backend** (Spring Boot + PostgreSQL + RLS).

---

## 1. Architecture

Le backend suit une architecture **Hexagonale + CQRS** :

- `domain` : modèle métier pur, sans dépendance framework.
- `application` : use cases, commands/queries, ports.
- `infra` : adapters (web, persistence, external, batch, config).

Les détails complets se trouvent dans :

- [`ARCHITECTURE.md`](./ARCHITECTURE.md)
- [`CODE_STYLE.md`](./CODE_STYLE.md)

---

## 2. Domaines (bounded contexts) actuels

Backends principaux :

- `accesscontrol` — permissions & rôles par tenant.
- `audit` — événements d’audit & révisions Envers.
- `draw` — tirages & résultats.
- `ticket` — tickets et paiement.
- `session` — sessions POS/vendeurs.
- `tenantconfig` — configuration fonctionnelle du tenant.
- `pagemodel` — configuration dynamique des pages publiques/privées.
- `identity` — identité applicative (profil utilisateur, hors auth Keycloak).

Chaque domaine possède son propre `DOMAIN.md` (en cours de généralisation).

---

## 3. Stack technique

- Java 25 / Spring Boot 4/ maven
- PostgreSQL + RLS
- Keycloak (authentification / tokens)
- Unleash (feature flags)
- Redis (cache)
- Traefik (reverse proxy)
- Docker / Docker Compose

---

## 4. Démarrage local (simplifié)

Selon ton setup, les commandes peuvent varier. Exemple :

```bash
# 1. Lancer les dépendances (DB, Keycloak, etc.)
docker compose up -d

# 2. Lancer le backend
./gradlew :server:bootRun
```

Les détails d’environnement (variables, profiles) sont documentés dans les fichiers
`docker-compose.yml`, scripts et docs internes.

---

## 5. Contribution

1. Lire `ARCHITECTURE.md` et `CODE_STYLE.md`.
2. Pour tout nouveau domaine, partir du template :
   - [`DOMAIN_TEMPLATE.md`](./DOMAIN_TEMPLATE.md)
3. Pour les PR :
   - respecter la structure des packages,
   - écrire des tests pour les use cases et/ou le domaine,
   - ne pas introduire de dépendance framework dans `domain`.

---

## 6. AI / Copilot

Les assistants (Copilot, ChatGPT, etc.) doivent suivre :

- [`ARCHITECTURE.md`](./ARCHITECTURE.md)
- [`CODE_STYLE.md`](./CODE_STYLE.md)
- [`.copilot/README.md`](./.copilot/README.md)

Tout code généré qui ne respecte pas ces documents doit être corrigé avant merge.

---

## 7. Licence / Mentions

(TBD selon ton choix de licence : MIT, Apache 2.0, proprietaire, etc.)
