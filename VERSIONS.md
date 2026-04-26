# VERSIONS.md — Tchalanet (Source de vérité)

Règle : aucune version (runtime/build/service) ne doit changer sans :

1. mise à jour de ce fichier
2. mise à jour du wrapper correspondant (Maven/Nx/pnpm)
3. mise à jour des images docker (compose/infra)
4. note dans le changelog si impact prod

---

## Sources de vérité (où lire/éditer)

- Backend runtime/build : `tchalanet-server/pom.xml` (+ `./mvnw`)
- Infra images/tags : `tchalanet-infra/envs/common/compose.env` + `compose/*`
- Web/Mobile (Nx) : `package.json` racine + `pnpm-lock.yaml`
- Version pnpm : `package.json#packageManager` + Corepack
- Edge service : `tchalanet-edge-service/package.json`

---

## 1) Backend (tchalanet-server)

- Java : 25 (défini dans `tchalanet-server/pom.xml`)
- Spring Boot : 4.0.3 (parent défini dans `tchalanet-server/pom.xml`)
- Build tool : Maven (wrapper présent : `./mvnw`)
- DB driver : PostgreSQL JDBC (version gérée via BOM ou dépendances Maven)
- Migration : Flyway (utilisé dans le POM)

### Notes backend

- `tchalanet-server/pom.xml` exige Java >= 25 via maven-enforcer.
- Certains modules (ex: `tchalanet-infra/keycloak/tchalanet-keycloak-provider/pom.xml`) sont actuellement configurés pour Java 21.
  - Toute homogénéisation Java 25 doit passer par une PR dédiée + build complet.
