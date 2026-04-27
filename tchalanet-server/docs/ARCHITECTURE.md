# Tchalanet – Architecture Applicative (Server)

Ce document décrit l'architecture du backend **tchalanet-server**.
Il est **structurel** : frontières, responsabilités, règles non négociables.
Les détails d'implémentation et patterns concrets vivent dans `docs/conventions/*`.

## Objectifs

- Stabiliser l'architecture (hexagonal + CQRS) dans les domaines critiques (`core/`)
- Livrer vite via des **vertical slices** (`features/`)
- Garder `common/` strictement technique
- Clarifier le rôle de `catalog/` (référentiels read-mostly)
- Uniformiser le **contrat HTTP** : 2xx = `ApiResponse<T>` ; erreurs = `ProblemDetail` (RFC7807)

## Hiérarchie de documentation (source de vérité)

En cas de conflit :

1. `docs/ARCHITECTURE.md` (structure & frontières)
2. `docs/PLAYBOOK.md` (workflow & DoD)
3. `docs/conventions/*` (normes techniques détaillées)
4. OpenSpec (dérivé, jamais source de vérité)

---

## Règl# Tchalanet – Architecture Applicative (Server)

Ce document décrit l'architecture du backend **liCe document décrit l'architecture du backend **`cIl est **structurel** : frontières, responsabilités, règles nondoLes détails d'implémentation et patterns concrets vivent dans `docs/conventios\*## Objectifs

- Stabiliser l'architecture (hexagonal + CQRS) dans les domaines critiqom- Stabilise# - Livrer vite via des **vertical slices** (`features/`)
- Garder `common/` strictemt.- Garder `common/` strictement technique
- Clarifier l(p- Clarifier le rôle de `catalog/` (rér? Uniformiser le **contrat HTTP** : 2xx = `ApiResponse<T>` ; ema## Hiérarchie de documentation (source de vérité)
  En cas de conflit :

1. `docs/ARCHITECTURE.
`En cas de conflit :
1. `docs/ARCHITECTURE.md` (strudo1. `docs/ARCHITECT`c2. `docs/PLAYBOOK.md` (workflow & DoD)
1. `docs/coes3. `docs/conventions/\*` (normes technan4. OpenSpec (dérivé, jamais source de vérité)

---

#, ---

## Règl# Tchalanet – Architecture Applicadr##deCe document décrit l'architecture du backend **liCe docur- Stabiliser l'architecture (hexagonal + CQRS) dans les domaines critiqom- Stabilise# - Livrer vite via des **vertical slices\*\* (`features/`)

- Garder `common/` strictemt.- Garder `common/` strictement technique
- Clarifier l(p- Clarifier le rôle de `catalog/au- Garder `common/`strictemt.- Garder`common/` strictement technique
- Clarifier l(p- Clarifier le rôle de `catalog/` (rér? Uniformiser po- Clarifier l(p- Clarifier le rôle de `catalog/` (rér? Uniformises/En cas de conflit :

1. `docs/ARCHITECTURE.
`En cas de conflit :
1. `docs/ARCHITECTURE.md` (strudo1. `docs/ARCHITECT`c2. `docs/PLAYBOOK.md` (workflow & DoD)
1. `docs/coes3.ie1. `docs/ARCHITECTen`En cas de conflit :
do1. `docs/ARCHITECTUle3. `docs/coes3. `docs/conventions/\*` (normes technan4. OpenSpec (dérivé, jamais source dnd---
   #, ---

## Règl# Tchalanet – Architecture Applicadr##deCe document décrit l'architecture du ban#,md## R? - Garder `common/` strictemt.- Garder `common/` strictement technique

- Clarifier l(p- Clarifier le rôle de `catalog/au- Garder `common/`strictemt.- Garder`common/` strictement technique
- Clarifier l(p- Clarifier le rôle de `catalog/` (rér? 3- Clarifier l(p- Clarifier le rôle de `catalog/au- Garder `common/`ab- Clarifier l(p- Clarifier le rôle de `catalog/`(rér? Uniformiser po- Clarifier l(p- Clarifier le rôle de`catalo`A1. `docs/ARCHITECTURE.
  `En cas de conflit :

1. `docs/ARCHITECTURE.md` (strudo1. `docs/ARCHITECT`c2. `docs/PLAYBOOK.md` (workflow & DoD)
2. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCHITECTen`En cas de conflit :
do1. `docs/ARCHITECTUle3. `docs/N do1. `docs/ARCHITECTUle3. `docs/coes3. `docs/conventionsni#, ---

## Règl# Tchalanet – Architecture Applicadr##deCe document décrit l'architecture du ban#,md## R? - Garderty## R?s- Clarifier l(p- Clarifier le rôle de `catalog/au- Garder `common/`strictemt.- Garder`common/` strictement technique

- Clarifier l(p- Clarifier le rôle de `catalog/` \* - Clarifier l(p- Clarifier le rôle de `catalog/` (rér? 3- Clarifier l(p- Clarifier le rôle de `catalog/au- Garder 3`En cas de conflit :

1. `docs/ARCHITECTURE.md` (strudo1. `docs/ARCHITECT`c2. `docs/PLAYBOOK.md` (workflow & DoD)
2. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCHITECTen`En cas de conflit :
do1. `docs/ARCHITECTUle3. `doc (1. `docs/ARCHITECTUn/3. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. exns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `d, do1. `docs/ARCHITECTUle3. `docs/N do1. `docs/ARCHITECTUle3. `docs/coes3. `docs/rr## Règl# Tchalanet – Architecture Applicadr##deCe document décrit l'architecture du ban#,md##us- Clarifier l(p- Clarifier le rôle de `catalog/`* - Clarifier l(p- Clarifier le rôle de`catalog/`(rér?  3- Clarifier l(p- Clarifier le rôle de`catalog/au- Garder 3`En cas de conflit :
3. `docs/ARCHITECTURE.md` (strudo1. `docs/ARpr1. `docs/ARCHITECTURE.md`(strudo1.`docs/ARCHITECT`c2. `docs/PLAYBOOK.md` (workflow & DoD)
4. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCom3. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `docs/ARCHITECTUle3. `doc (1. `docs/ARCHITECTUn/3. `docs/coes3.ie1. `docs/ jns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. exns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `d, do1. `ot1. `docs/ARCHITECTURE.md`(strudo1.`docs/ARpr1. `docs/ARCHITECTURE.md` (strudo1. `docs/ARCHITECT`c2. `docs/PLAYBOOK.md` (workflow & DoD)
5. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCom3. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `docs/ARCHITECTUle3. `doc (1. `docs/ARCHITECTUn/3. `docs/coes3.ie1. `docs/ jns1., 3. `docs/coes3.ie1. `docs/`En cas de conflit :
   ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCom3. `docs/coes3.ie1. `docs/`En cas d??s1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `dtens1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `din3. `docs/coes3.ie1. `docs/`En cas de conflit :
   ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCom3. `docs/coes3.ie1. `docs/`En cas de conflit :
   ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `docs/ARCHITECTUle3. `doc (1. `docs/ARCHITECTUn/3. `docs/coes3.ie1. `docs/ jns1., 3. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coe? ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `desns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `diqns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCom3. `docs/coes3.ie1. `docs/`En cas d??s1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `dtens1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1e ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `docs/ARCom3. `docs/coes3.ie1. `docs/`En cas de conflit :
ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `docs/ARCHITECTUle3. `doc (1. `docs/ARCHITECTUn/3. `docs/coes3.ie1. `docs/ jns1., 3. `TFns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `doins1. `docs/ARCHITECTUnt3. `docs/coe? ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `desns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `diqns1. `docs/ARCHITECTUnt3. `docs/coes3.i**ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `docs/ARCHITECTUle3. `doc (1. `docs/ARCHITECTUn/3. `docs/coes3.ie1. `docs/ jns1., 3. `TFns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `doins1. `docs/ARCHITECTUnt3. `docs/coe? ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `desns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. " ns1. `docs/ARCHITECTUnt3. `docs/coes3.ie1. `disdo1. `diqns1. `docs/ARCHITECTUnt3. `docs/coes3.iauto=validate`

- Tables tenant-scoped : `tenant_id` + indexes + policies RLS cohérentes
- Envers si audit requis
  **Wrappers d'ID** :
- Domain/Application : wrappers uniquement
- `UUID` brut : uniquement JPA entities / repositories / SQL
  Voir : `docs/conventions/persistence.md`, `docs/conventions/jpa_entities.md`, `docs/conventions/typed_ids.md`, `docs/conventions/rls.md`.

---

## 11) Batch & After-Commit

**Jobs** : `core.<bc>.infra.batch`

- Pas d'auto-run au démarrage
- Orchestration via scheduler ou ops endpoints
  **After-commit** : événements & side-effects via listeners after-commit
- Éviter les écritures cross-domain dans la transaction critique
  Voir : `docs/conventions/batch.md` + `docs/conventions/handler_command.md`.

---

## 12) Cache (Caffeine + Redis)

Cache 2 niveaux :

- **L1** : Caffeine
- **L2** : Redis
  **Règles** :
- TTL & specs via provider (pas hardcodé)
- Feature-flag possible pour Redis
- Pas de cache sur opérations critiques d'argent
  Voir : `docs/conventions/cache.md`.

---

## 13) Erreurs - Tables tens- Envers si audit requis

**Wrappers d'ID** :

- Domain/Application : wrapnt**Wrappers d'ID** :
- Dle- Domain/ApplicatiRe- `UUID` brut : uniquement JPA entities /teVoir : `docs/conventions/persistence.md`, `docs/conventionseu---

## 11) Batch & After-Commit

**Jobs** : `core.<bc>.infra.batch`

- Pas d'auto-run au démarrage
- Orchestration via scheduler ou ops evi##`A**Jobs** : `core.<bc>.infrri- Pas d'auto-run au démarrage
- uv- Orchestration via schedulerAp**After-commit** : événements & side-effecten- Éviter les écritures cross-domain dans la transaction critique
  Voir eVoir : `docs/conventions/batch.md` + `docs/conventions/handler_coco---

## 12) Cache (Caffeine + Redis)

Cache 2 niveaux :

- **L1** : Caffeine
  er## ?ache 2 niveaux :
- **L1** : Cle- **L1** : Caffey)- **L2** : Redis
  _ap**Règles** :
  -- - TTL & spec_ - Feature-flag possible pour Redis
- Pas /J- Pas de cache sur opérations crteVoir : `docs/conventions/cache.md`.

---

## 13) Efe---

## 13) Erreurs - Tables tens- f?#en**Wrappers d'ID** :

- Domain/Application : wrapnt\*\*me- Domain/Applicatine- Dle- Domain/ApplicatiRe- `UUID` brut cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs && wc -l ARCHITECTURE.md PLAYBOOK.md README.md 2>&1
  cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && mv web_api_clean.md web_api.md && mv routing_and_path_clean.md routing_and_path.md
  cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && ls -lh web_api.md routing_and_path.md
  cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && wc -l web_api.md && echo "---" && tail -20 web_api.md
  cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && mv pagination_clean.md pagination.md
  cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && wc -l pagination.md && echo "---" && head -30 pagination.md
  cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/typed_ids_clean.md << 'EOF'

# Typed IDs (Wrappers) Policy

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server (`common/`, `core/`, `catalog/`, `features/`)  
> **Last reviewed**: 2026-01-20

---

## 1) Goal

Prevent UUID mixups, enforce domain boundaries, and make method signatures self-documenting.
**Typed IDs are mandatory everywhere outside persistence.**

---

## 2) Core rule (NON-NEGOTIABLE)

**Outside persistence, NEVER use raw `UUID`.  
Always use typed ID wrappers**:
`TenantId`, `TicketId`, `PayoutId`, `TerminalId`, …
This rule applies to:

- domain models
- commands / queries
- handlers / application services
- controllers (method parameters)
- domain & application events
- application-level DTOs
  If you see a `UUID` in these layers, it is a violation.

---

## 3) Where `UUID` is allowed (CLOSED LIST)

Raw `UUID` is allowed **only** in the following places:

- JPA entities (`*JpaEntity`)
- Spring Data repositories / JDBC # Typed IDs (Wrappers) Policy
  > **Status**: NORMATIVE  
  > **Applies to**: tchalanet-server (`common/`, `corow> **Status**: NORMATIVE  
  > ch> **Applies to**: tchalapr> **Last reviewed**: 2026-01-20

---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1) Goal

Prevent UUID miy.##
-Prevent UWh**Typed IDs are mandatory everywhere outside persistence.**

---

## 2) Core rule (NON-NEGOTIn/---

## 2) Core rule (NON-NEGOTIABLE)

**Outside persistenceat##n **Outside persistence, NEVER usraAlways use typed ID wrappers\*\*:
`TenantId`, `io`TenantId`, `TicketId`, `PayouonThis rule applies to:

- domain models
- commands / qpe- domain models
- coes- commands / qpe- handlers / applicic- controllers (method parametersID- domain & application events
- xa- application-level DTOs
  If coIf you see a `UUID` in on---

## 3) Where `UUID` is allowed (CLOSED LIST)

Raw `U(U##D Raw `UUID` is allowed **only** in the foll= - JPA entities (`\*JpaEntity`)

- Spring Data repositoriId- Spring Data repositories / > **Status**: NORMATIVE
  > **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> **Applies to**: tchalapr> **Last reviewed**: 2026-01-20

---

## 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##
-Preven\*/Prevent U s-Prevent UWh\*\*Type(S---

## 2) Core rule (NON-NEGOTIn/---

## 2) Core rule (NON-NEGOTIABLE)

ll##al## 2) Core rule (NON-NEGOTIABLEg **Outside persistenceat##n **Ou n`TenantId`, `io`TenantId`, `TicketId`, `PayouonThis rule applies to:

- domain models
- comMU- domain models
- commands / qpe- domain models
- coes- commands / al- commands / q t- coes- commands / qpe- handleT\*- xa- application-level DTOs
  If coIf you see a `UUID` in on---

## 3) Where `UUID` is allowed (CLOSED LISTrsIf coIf you see a `UUID` in e## 3) Where `UUID` is allowed (CreRaw `U(U##D Raw `UUID` is allowed **only**# - Spring Data repositoriId- Spring Data repositories / > **Status**: NORMATIVE

> an> **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> \*\*Applies o---

## 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1/tPrevent UdG## 1)

##

Prevent Ue ## 1) Goal
Prevee ##
ne PtoPrevent UUID miy.##

- }-Preven\*/Prevent Ump## 2) Core rule (NON-NEGOTIn/---

## 2) CorV4## 2) Core rule (NON-NEGOTIABLEenll##al## 2) Core rule (NON-NEGOim- domain models

- comMU- domain models
- commands / qpe- domain models
- coes- commands / al- commands / q t- coes- commands / qpe- handleTja- comMU- domaid - commands / qpe- domat- coes- commands / al- commandckIf coIf you see a `UUID` in on---

## 3) Where `UUID` is allowed (CLOSED LISTrsIf coIf you see a c## 3) Where `UUID` is allowed (C S> an> **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> \*\*Applies o---

## 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1 ?# 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent U# f(---

## ye## 1nvPrevent USp## 1)

##

Prevent Ue ## 1) Goal
Prevety ##
ID PdiPrevent UUID miy.##faat## co## 1/tPrevent UdG## 1)
ic ##
Prevent Ue ## 1) Goalam PerPrevee ##
ne PtoPrev`
ne PtoPrenv-
}-Preven\*/Prevent Ump# to ## 2) CorV4## 2) Core rule (NON-NEGOTIABLEenll##al##om- comMU- domain models

- commands / qpe- domain models
- coes- commands / al- commands / q - commands / qpe- domck- coes- commands / al- command ## 3) Where `UUID` is allowed (CLOSED LISTrsIf coIf you see a c## 3) Where `UUID` is allowed (C S> an> **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> \*ca## 1) Goal
  f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1 ?# 1) Goal

f(---

## 1) Goal

Prevent UUID mickf(---

## et## 1d)Prevent Urn## 1)

##

Prevent Ue ## 1) Goal
Preve

- ##
  im PicPrevent UUID miy.##fans## co## 1 ?# 1) Goal
  f(---

# hf(---

## 1) Goal

Prer)## 1prPrevent Uni## 1)

##

Prevent U# f(---

## ye## 1nns

ID P->## ye## 1nvPrevebl ##
Prevent Ue ## 1) Goal= PmmPrevety ##
ID PdiPrev\*_ID PdiPrent ic ##
Prevent Ue ## 1) Goalam PerPrevee ##
ne PtoPre`
 Prevsane PtoPrev`
ne PtoPrenv-
}-Preven_/Prsene PtoPrenon}-Preven\*/Pas- commands / qpe- domain models

- coes- commands / al- commands / q - commands / qpe- domck- coes- es- coes- commands / al- commandouf(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1 ?# 1) Goal

f(---

## 1) Goal

Prevent UUID mickf(---

## et## 1d)Prevent Urn## 1)

##

Prevent Ue ## 1) Goal
Preve

- ##
  im PicPrevent UUID miy.##fanbu## 1 tPrevent Uet## 1)

##

Prevent Ue ## 1) Goal
Preve c ##
nd PuePrevent UUID miy.##fae ## co## 1 ?# 1) Goal
f(---
#haf(---

## 1) Goal

Preia## 1xIPrevent Uor## et## 1d)Prevent UrUs ##
Prevent Ue ## 1) Goalat Pn
Preve

- ##
  im PicPreve \* ##prim af(---

# hf(---

## 1) Goal

Prer)## 1prPrevent Uni#ch# hftu## 1) GorPrer)## 1## ##
Prevent U# f(---

## ye) P \*## ye## 1nns

m.ID P->## ye## er Prevent Ue ## 1) Goal= PnvID PdiPrev**ID PdiPrent ic ##
Prevenn. Prevent Ue ## 1) Goalam Per**ne PtoPre`
 Prevsane PtoPrev`
ne PtoP*J Prevsaneesne PtoPrenv-
}-Prrs}-Preven*/PaE- coes- comcd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && mv typed_ids_clean.md typed_ids.md
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && wc -l typed_ids.md && echo "---" && head -40 typed_ids.md
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && mv jpa_entities_clean.md jpa_entities.md && mv persistence_clean.md persistence.md && mv rls_clean.md rls.md
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && wc -l jpa_entities.md persistence.md rls.md && echo "---" && head -20 jpa_entities.md
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && mv handler_command_clean.md handler_command.md && mv security_permissions_clean.md security_permissions.md
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && wc -l handler_command.md security_permissions.md && echo "---" && head -30 handler_command.md
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && rm -f _\_clean.md && ls -la _.md | wc -l
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && echo "Fichiers MD actuels:" && ls -1 \*.md
cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/testing_clean.md << 'EOF'

# Testing Rules (Server)

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server  
> **Goal**: fast, reliable tests that validate business logic and prevent regressions without over-testing Spring.

---

## 1) Unit tests (DEFAULT)

**Unit tests are the default.**  
They validate domain + application behavior with **in-memory ports** (preferred) or minimal fakes.

### MUST

- **JUnit 5**
- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assertions with `assertAll(...)`** when relevant
- Use `@Nested` for scenarios
- Prefer **in-memory ports** over mocks
- Method names MUST be Java-compatible **camelCase**
- Use `@DisplayName("should <expected> when <condition>")` on test methods (canonical report description)
- Naming convention for methods: `should<Expected>When<Condition>`

### MUST NOT

- Don't test Spring wiring in unit tests
- Don't mock everything (avoid testing mocks instead of l# Testing Rules (Server)
  > **Status**: NORMATIVE  
  > **Applies to**: tchalanet-server  
  > **Goal**: fast,@N> **Status**: NORMATIVEn > **Applies to**: tchalala> **Goal**: fast, reliable tests th@D---

## 1) Unit tests (DEFAULT)

**Unit tests are the default.**  
They validate domain + application behavior with= ##..\*\*Unit tests are the defas They validate domain + applicati ### MUST

- **JUnit 5**
- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assnt- **JUnn - **AssertJ mi- **Group assertions with `assertAll(...)`\*\* when relevant
  f- Use `@Nested` for scenarios
- Prefer **in-memory ports\*##- Prefer **in-memory ports\*_ti- Method names MUST be Java-compatible_ - Use `@DisplayName("should <expected> when <conditJW- Naming convention for methods: `should<Expected>When<Condition>`

### MUST NOT

- Don't test Spring wiri- ### MUST NOT
- Don't test Spring wiring in unit tests
- Don't mocen- Don't tesch- Don't mock everything (avoid testing lt> **Status**: NORMATIVE
  > **Applies to**: tchalanet-server  
  > **Goal**: fast,s:> **Applies to**: tchala k> **Goal**: fast,@N> **Status**: NOnc## 1) Unit tests (DEFAULT)
  > **Unit tests are the default.**  
  > They validate domain + application behavior withlo**Unit tests are the defa MThey validate domain + applicatitg- **JUnit 5\*\*
- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assnt- **JUnn - **AssertJ mi- **Groupnt- **AssertJ ur- **Group assnt- **JUnn - **AssertJ mi- \*\*Group assertions le f- Use `@Nested` for scenarios
- Prefer **in-memory ports\*##- Prefer **in-memory ports**ti- MemD- Prefer **in-memory ports\*##-rs### MUST NOT
- Don't test Spring wiri- ### MUST NOT
- Don't test Spring wiring in unit tests
- Don't mocen- Don't tesch- Don't mock everything (avoid testing lt> **Status**: NORMATIVE
  > **Applies to**: tchalanet-serpl- Don't teste- Don't test Spring wiring in unit tenc- Don't mocen- Don't tesch- Don't mock e:> **Applies to**: tchalanet-server  
  > **Goal**: fast,s:> **Applies to**: tchala k> **Goal\*s > **Goal**: fast,s:> **Applies to**ts**Unit tests are the default.**  
  > They validate domain + application behavior withlo**Unit tests are the de iThey validate domain + applicatimm- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assnt- **JUnn - **AssertJ mi- **Groupnt- **AssertJ uat- **Group assnt- **JUnn - **AssertJ mi- **Groupnt- **Assertor- Prefer **in-memory ports\*##- Prefer **in-memory ports**ti- MemD- Prefer **in-memory ports\*##-rs### MUST NOT
- Don't test Spring wiri- ### MUST NOT
- Donup- Don't test Spring wiri- ### MUST NOT
- Don't test Spring wiring in unit tests
- Don't mocencd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && mv testing_clean.md testing.md
  cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/typed_ids_fixed.md << 'EOF'

# Typed IDs (Wrappers) Policy

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server (`common/`, `core/`, `catalog/`, `features/`)  
> **Last reviewed**: 2026-01-20

---

## 1) Goal

Prevent UUID mixups, enforce domain boundaries, and make method signatures self-documenting.
**Typed IDs are mandatory everywhere outside persistence.**

---

## 2) Core rule (NON-NEGOTIABLE)

**Outside persistence, NEVER use raw `UUID`.  
Always use typed ID wrappers**:
`TenantId`, `TicketId`, `PayoutId`, `TerminalId`, …
This rule applies to:

- domain models
- commands / queries
- handlers / application services
- controllers (method parameters)
- domain & application events
- application-level DTOs
  If you see a `UUID` in these layers, it is a violation.

---

## 3) Where `UUID` is allowed (CLOSED LIST)

Raw `UUID` is allowed **only** in the following places:

- JPA entities (`*JpaEntity`)
- Spring Data repositories / JDBC # Typed IDs (Wrappers) Policy
  > **Status**: NORMATIVE  
  > **Applies to**: tchalanet-server (`common/`, `corow> **Status**: NORMATIVE  
  > ch> **Applies to**: tchalapr> **Last reviewed**: 2026-01-20

---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1) Goal

Prevent UUID miy.##
-Prevent UWh**Typed IDs are mandatory everywhere outside persistence.**

---

## 2) Core rule (NON-NEGOTIn/---

## 2) Core rule (NON-NEGOTIABLE)

**Outside persistenceat##n **Outside persistence, NEVER usraAlways use typed ID wrappers\*\*:
`TenantId`, `io`TenantId`, `TicketId`, `PayouonThis rule applies to:

- domain models
- commands / qpe- domain models
- coes- commands / qpe- handlers / applicic- controllers (method parametersID- domain & application events
- xa- application-level DTOs
  If coIf you see a `UUID` in on---

## 3) Where `UUID` is allowed (CLOSED LIST)

Raw `U(U##D Raw `UUID` is allowed **only** in the foll= - JPA entities (`\*JpaEntity`)

- Spring Data repositoriId- Spring Data repositories / > **Status**: NORMATIVE
  > **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> **Applies to**: tchalapr> **Last reviewed**: 2026-01-20

---

## 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##
-Preven\*/Prevent U s-Prevent UWh\*\*Type(S---

## 2) Core rule (NON-NEGOTIn/---

## 2) Core rule (NON-NEGOTIABLE)

ll##al## 2) Core rule (NON-NEGOTIABLEg **Outside persistenceat##n **Ou n`TenantId`, `io`TenantId`, `TicketId`, `PayouonThis rule applies to:

- domain models
- comMU- domain models
- commands / qpe- domain models
- coes- commands / al- commands / q t- coes- commands / qpe- handleT\*- xa- application-level DTOs
  If coIf you see a `UUID` in on---

## 3) Where `UUID` is allowed (CLOSED LISTrsIf coIf you see a `UUID` in e## 3) Where `UUID` is allowed (CreRaw `U(U##D Raw `UUID` is allowed **only**# - Spring Data repositoriId- Spring Data repositories / > **Status**: NORMATIVE

> an> **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> \*\*Applies o---

## 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1/tPrevent UdG## 1)

##

Prevent Ue ## 1) Goal
Prevee ##
ne PtoPrevent UUID miy.##

- }-Preven\*/Prevent Ump## 2) Core rule (NON-NEGOTIn/---

## 2) CorV4## 2) Core rule (NON-NEGOTIABLEenll##al## 2) Core rule (NON-NEGOim- domain models

- comMU- domain models
- commands / qpe- domain models
- coes- commands / al- commands / q t- coes- commands / qpe- handleTja- comMU- domaid - commands / qpe- domat- coes- commands / al- commandckIf coIf you see a `UUID` in on---

## 3) Where `UUID` is allowed (CLOSED LISTrsIf coIf you see a c## 3) Where `UUID` is allowed (C S> an> **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> \*\*Applies o---

## 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1 ?# 1) Goal

f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent U# f(---

## ye## 1nvPrevent USp## 1)

##

Prevent Ue ## 1) Goal
Prevety ##
ID PdiPrevent UUID miy.##faat## co## 1/tPrevent UdG## 1)
ic ##
Prevent Ue ## 1) Goalam PerPrevee ##
ne PtoPrev`
ne PtoPrenv-
}-Preven\*/Prevent Ump# to ## 2) CorV4## 2) Core rule (NON-NEGOTIABLEenll##al##om- comMU- domain models

- commands / qpe- domain models
- coes- commands / al- commands / q - commands / qpe- domck- coes- commands / al- command ## 3) Where `UUID` is allowed (CLOSED LISTrsIf coIf you see a c## 3) Where `UUID` is allowed (C S> an> **Applies to**: tchalanet-server (lu> **Applies to**: tchalae > ch> \*ca## 1) Goal
  f(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1 ?# 1) Goal

f(---

## 1) Goal

Prevent UUID mickf(---

## et## 1d)Prevent Urn## 1)

##

Prevent Ue ## 1) Goal
Preve

- ##
  im PicPrevent UUID miy.##fans## co## 1 ?# 1) Goal
  f(---

# hf(---

## 1) Goal

Prer)## 1prPrevent Uni## 1)

##

Prevent U# f(---

## ye## 1nns

ID P->## ye## 1nvPrevebl ##
Prevent Ue ## 1) Goal= PmmPrevety ##
ID PdiPrev\*_ID PdiPrent ic ##
Prevent Ue ## 1) Goalam PerPrevee ##
ne PtoPre`
 Prevsane PtoPrev`
ne PtoPrenv-
}-Preven_/Prsene PtoPrenon}-Preven\*/Pas- commands / qpe- domain models

- coes- commands / al- commands / q - commands / qpe- domck- coes- es- coes- commands / al- commandouf(---

## 1) Goal

Prevent UUID mixups, enforce domai c---

## 1)

##

Prevent Ue ## 1) Goal
Prevent UUID miy.##fa## (f(---

## co## 1 ?# 1) Goal

f(---

## 1) Goal

Prevent UUID mickf(---

## et## 1d)Prevent Urn## 1)

##

Prevent Ue ## 1) Goal
Preve

- ##
  im PicPrevent UUID miy.##fanbu## 1 tPrevent Uet## 1)

##

Prevent Ue ## 1) Goal
Preve c ##
nd PuePrevent UUID miy.##fae ## co## 1 ?# 1) Goal
f(---
#haf(---

## 1) Goal

Preia## 1xIPrevent Uor## et## 1d)Prevent UrUs ##
Prevent Ue ## 1) Goalat Pn
Preve

- ##
  im PicPreve \* ##prim af(---

# hf(---

## 1) Goal

Prer)## 1prPrevent Uni#ch# hftu## 1) GorPrer)## 1## ##
Prevent U# f(---

## ye) P \*## ye## 1nns

m.ID P->## ye## er Prevent Ue ## 1) Goal= PnvID PdiPrev**ID PdiPrent ic ##
Prevenn. Prevent Ue ## 1) Goalam Per**ne PtoPre`
 Prevsane PtoPrev`
ne PtoP*J Prevsaneesne PtoPrenv-
}-Prrs}-Preven*/PaE- coes- commands / al- commands / q - commands / qpe- domck- coes- er## 1) Goal
Prevent UUID mixups, enforce domai c---

## 1)cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/jpa_entities_fixed.md << 'EOF'

# JPA Entities — Choix de la Base Class

## Status

## **NORMATIVE**

## Décision

## Le choix entre `BaseEntity` et `BaseTenantEntity` dépend **uniquement de l'ownership des données**, pas du scope API.

## Utiliser `BaseTenantEntity` si :

- La ligne appartient à **un seul tenant**
- La table a `tenant_id NOT NULL`
- Les policies RLS s'appliquent
  **Exemples** :
- `ticket`
- `sale`
- `payout`
- `ledger_entry`
- `limit_policy`
- `draw` (tenant)

---

## Utiliser `BaseEntity` si :

- Donnée **globale / plateforme**
- Pas de `tenant_id`
- Pas de RLS
  **Exemples** :
- `tenant`
- `result_slot`
- `draw_result`
- catalogues globaux

---

## Anti-patterns (INTERDITS)

- ❌ Table tenantée sans `tenant_id`
- ❌ Entité tenantée étendant `BaseEntity`
- ❌ Dupliquer les champs audit manuellement

---

## Règle d'or

👉 **Le tenant est une propriété SQL, pas applicative.**  
Les entités doivent refléter c# JPA Entities — Choix de la Base Class

## Status

## **NORMATIVE**

## Décision

Le choix entre `BaseEntity`ER## Status
**NORMATIVE**

---

## Décisionnt\*\*NORMATfe---

## Déci :##*ILe choix en*P---

## Utiliser `BaseTenantEntity` si :

- La ligne appartient à **un seul tenant**
- La table a `tenant_id NOT NULL`
  i ##?m- La ligne appartient à \*\*un seulde- La table a `tenant_id NOT NULL`
- Les poti- Les policies RLS s'appliquent
  ts**Exemples** :
- `ticket`
- `ser- `ticket`
- en- `sale`
  mv- `payoti- `ledgerd.- `limit_policy.m- cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/handler_command_fixed.md << 'EOF'

# Command / Query Handlers & Bus (Server)

## Status

## **NORMATIVE**

## Objectif

Standardiser l'exécution des use-cases via **CommandBus / QueryBus**, garantir :

- placement correct (core/features/catalog),
- typed IDs partout (hors persistence),
- transactions + after-commit,
- handlers simples et testables.

---

## 1) Placement (rappel)

- **core/\<bc\>** : use-cases critiques (argent/tickets/tirages/limites/audit…)
- **features/\<slice\>** : orchestration/BFF (agrégation multi-domaines)
- **catalog/\<ref\>** : lookup/read-mostly/crud simple
- **common** : infra/bus/tx/types (pas de métier)

---

## 2) Handler template (MUST)

### 2.1 Command handler (write)

- `record` command model : `core.<bc>.application.command.model`
- handler : `core.<bc>.application.command.handler`
- `@UseCase`
- `@TchTx` sur la méthode `handle(...)`
- typed IDs (wrappers) dans command + ports
- événements : \*\*After# Command / Query Handlers & Bus (Server)

## Status

## **NORMATIVE**

## Objectif

Standardiser l'exécution des u C## Status
**NORMATIVE**

---

## Objectif

lR\*\*NORMAT

---

## Objecal##icStandardisor- placement correct (core/features/catalog),

- typed IDs partout (hors persistenbl- typed IDs partout (hors persistence),
- tc)- transactions + after-commit,
- handltI- handlers simples et testabl);---

## 1) Placement (rappel)

- ##ev- **core/\<bc\>** : useCa- **features/\<slice\>** : orchestration/BFF (agrégation multi-domaines)
- **ca
  }- **catalog/\<ref\>\*\* : lookup/read-mostly/crud simple
- **common** : in-o- **common** : infra/bus/tx/types (pas de métier)
  -- d---

## 2) Handler template (MUST)

### 2.1 CommandST##Le### 2.1 Command handler (wriur- `record` command model : `cond- handler : `core.<bc>.application.command.handler`

- `@UseCaseas- `@UseCase`
- `@TchTx` sur la méthode `handle(..s - `@TchTx` l? typed IDs (wrappers) dans command + per- événements : **After# Command / Query im## Status
  **NORMATIVE\*\*

---

## Objectif

Standardiser l'exécutio>`\*\*NORMATom---

## ObjecC>## `Standardiser**NORMATIVE**

---

## Objectif

lR\*\*NORMAT
le---

## Objece ##inlR\*\*NORMATon ---

## O2 ## gl- typed IDs partout (hors persistenbl- typed IDs partout (hors persismu- tc)- transactions + after-commit,

- handltI- handlers simples et testabl)se- handltI- handlers simples et teste## 1) Placement (rappel)
- ##ev- **core/er- ##ev- **core/\<bc\>e - **ca
  }- **catalog/\<ref\>\*\* : lookup/read-mostly/crud simple
- **common** : in-o- **common** : infra/bu c}- **- - **common** : in-o- **common\*\* : infra/bus/tx/types (,
  -- d---

## 2) Handler template (MUST)

### 2.1 CommandST##Le### 2.1 CMU## 2) JU### 2.1 CommandST##Le### 2.1te- `@UseCaseas- `@UseCase`

- `@TchTx` sur la méthode `handle(..s - `@TchTx`l? typed IDs (wrappers) dans command + per- événements- -`@TchTx` sur la méthoan**NORMATIVE**

---

## Objectif

Standardiser l'exécutio>`\*\*NORMATom---

## ObjecC>## `Standardiser**NORMATIVE**

---

## Objectif

lR\*\*NORMAT
le---
##ne---

## Objecs/##nvStandardis m## ObjecC>## `Standardiser\**NORMATIVE*ma---

##cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions && echo "=== Fichiers corrigés ===" && wc -l testing.md typed_ids.md jpa_entities.md handler_command.md
rm /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/testing.md
cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/testing.md << 'EOF'

# Testing Rules (Server)

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server  
> **Goal**: fast, reliable tests that validate business logic and prevent regressions without over-testing Spring.

---

## 1) Unit tests (DEFAULT)

**Unit tests are the default.**  
They validate domain + application behavior with **in-memory ports** (preferred) or minimal fakes.

### MUST

- **JUnit 5**
- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assertions with `assertAll(...)`** when relevant
- Use `@Nested` for scenarios
- Prefer **in-memory ports** over mocks
- Method names MUST be Java-compatible **camelCase**
- Use `@DisplayName("should <expected> when <condition>")` on test methods (canonical report description)
- Naming convention for methods: `should<Expected>When<Condition>`

### MUST NOT

- Don't test Spring wiring in unit tests
- Don't mock everything (avoid testing mocks instead of logic)

# Testing Rules (Server)

> **Status**: NORMATIVE  
> **Applies to**: tchalanet-server  
> **Goal**:@N> **Status**: NORMATIVEn > **Applies to**: tchalala> **Goal**: fast, reliable tests th@D---

## 1) Unit tests (DEFAULT)

**Unit tests are the default.**  
They validate domain + application behavior with= ##..\*\*Unit tests are the defas They validate domain + applicati ### MUST

- **JUnit 5**
- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assnt- **JUnn - **AssertJ mi- **Group assertions with `assertAll(...)`\*\* when relevant
  f- Use `@Nested` for scenarios
- Prefer **in-memory ports\*##- Prefer **in-memory ports\*\*ti- Method names MUST be Java-compatibler:- Use `@DisplayName("should <expected> when <conditcl- Naming convention for methods: `should<Expected>When<Condition>`

### MUST NOT

- Don't test Spring wirine### MUST NOT
- Don't test Spring wiring in unit tests
- Don't moc- - Don't tesch- Don't mock everything (avoid testing tc# Testing Rules (Server)
  > **Status**: NORMATIVE  
  > **Applieha> **Status**: NORMATIVEra> **Applies to**: tchalaem> **Goal**:@N> **Status**: NORMATIV b## 1) Unit tests (DEFAULT)
  > **Unit tests are the default.**  
  > They validate domain + application behavio P**Unit tests are the defaUsThey validate domain + applicatis - **JUnit 5**
- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assnt- **JUnn - **AssertJ mi- **Groupot- **AssertJ ru- **Group assnt- **JUnn - **AssertJ mi- \*\*Group assertions na f- Use `@Nested` for scenarios
- Prefer **in-memory ports\*##- Prefer **in-memory ports**ti- Me `- Prefer **in-memory ports\*##- o### MUST NOT
- Don't test Spring wirine### MUST NOT
- Don't test Spring wiring in unit tests
- Don't moc- - Don't tesch- Don't mock everything (avoid testing tc# Testing Rules (Server)
  > **Status**: NORMATIVE  
  > **Apn'- Don't tes u- Don't test Spring wiring in unit te g- Don't moc- - Don't tesch- Don't mock on> **Status**: NORMATIVE  
  > **Applieha> **Status**: NORMATIVEra> **Applies to**: tchalaem>ss> **Applieha> **Status**RL**Unit tests are the default.**  
  > They validate domain + application behavio P**Unit tests are the defaUsThey validate domainonThey validate domain + applicatima- **AssertJ ONLY** (no `org.junit.jupiter.api.Assertions.*`)
- **Group assnt- **JUnn - **AssertJ mi- **Groupot- **Ass**- **Group assnt- **JUnn - **AssertJ mi- **Groupot- **AssertrC- Prefer **in-memory ports*##- Prefer **in-memory ports**ti- Me `- Prefer \*\*in-memory ports*##- o### MUST NOT
- Don't test Spring wirine### MUST NOT
- Don e- Don't test Spring wirine### MUST NOT
- Don't test Spring wiring in unit testscat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/command_query_handlers.md << 'EOF'

# Handlers (CQRS) — Commands & Queries

> **Status**: NORMATIVE  
> **Applies to**: `core/**`, `catalog/**` (light CQRS), `features/**` (orchestration only)  
> **Last reviewed**: 2026-01-20

## This document defines the canonical way to implement **CommandHandlers** and **QueryHandlers** in Tchalanet.

## 1) Placement (MUST)

- Commands: `core.<bc>.application.command.handler`
- Command models: `core.<bc>.application.command.model`
- Queries: `core.<bc>.application.query.handler`
- Query models: `core.<bc>.application.query.model`
  Ports:
- `core.<bc>.application.port.out.*` OR `core.<bc>.port.out.*` (choose one convention and keep it consistent)
  Infra adapters:
- `core.<bc>.infra.persistence`
- `core.<bc>.infra.web`
- `core.<bc>.infra.event`
- `core.<bc>.infra.batch`

---

## 2) Shared rules (Commands & Queries) — MUST

- Handlers are **application layer**. No JPA entities, no repositories, no Sp# Handlers (CQRS) — Commands & Queries
  > **Status**: NORMATIVE  
  > **Applies to**: `core/**`, `catalog/**` (lighd`> **Status**: NORMATIVE  
**Applies t.
**Applies to**: `core/ex> **Last reviewed**: 2026-01-20
  > This document defines the canonical way to implement \*\*Comm fThis document defines the canoti---

## 1) Placement (MUST)

- Commands: `core.<bc>.application.command.handler`
- Command models: `core.<bc>s ##rf- Commands: `core.<bc.
- Command models: `core.<bc>.application.command.mFT- Queries: `core.<bc>.application.query.handler`
- Queri- Query models: `core.<bc>.application.query.mootPorts:
- `core.<bc>.application.port.out.*` OR `coac- `col Infra adapters:
- `core.<bc>.infra.persistence`
- `core.<bc>.infra.web`
- `core.<bc>.infra.event`
- `core.<b -- `core.<bc>.iri- `core.<bc>.infra.web`
- `corot- `core.<bc>.infra.eve e- `core.<bc>.infra.batchmm---

## 2) Shared rules (in##ev- Handlers are **application layer**. No JPA ens-> **Status**: NORMATIVE

> **Applies to**: `core/**`, `catalog/**` (lighd`> **Status**: NORMATIVE  
**Applies t.
or> **Applies to**: `core/an> **Applies t.
> **Applies to**: `core/ex> **Last reviewed**: 2026-01-20
> co> **Applies terThis document defines the canonical way to implement \*\*Cer## 1) Placement (MUST)

- Commands: `core.<bc>.application.command.handler`
- Command models: `cr.- Commands: `core.<bctI- Command models: `core.<bc>s ##rf- Commands: `corio- Command models: `core.<bc>.application.command.mFT- Qet- Queri- Query models: `core.<bc>.application.query.mootPorts:
- `core.<bc>.application.port.out.*`al- `core.<bc>.application.port.out.*` OR `coac- `col Infra adat.- `core.<bc>.infra.persistence`
- `core.<bc>.infra.web`
- `core.<bha- `core.<bc>.infra.web`
- `cor.D- `core.<bc>.infra.evemp- `core.<b -- `core.<bc>on- `corot- `core.<bc>.infra.eve e- `core.<bc>.infra c## 2) Shared rules (in##ev- Handlers are **application layerke> **Applies to**: `core/**`, `catalog/**` (lighd`> **Status**: NORMATIVE
  > **Applies t.
  > or> **ApptP> **Applies t.
  > or> **Applies to**: `core/an> **Applies t.
  > **Applies to\*Caor> **Appliesma> **Applies to**: `core/ex> **Last review vco> **Applies terThis document defines the canonical wayso- Commands: `core.<bc>.application.command.handler`
- Command models: `cr.- Commands: `core.<bctdE- Command models: `cr.- Commands: `core.<bctI- Com;
- `core.<bc>.application.port.out.*`al- `core.<bc>.application.port.out.*` OR `coac- `col Infra adat.- `core.<bc>.infra.persistence`
- `core.<bc>.infra.web`
- `core.<bha- `core.<bc>.infra.web`
- `cor.D- `core.<bc>.iha- `core.<bc>.infra.web`
- `core.<bha- `core.<bc>.infra.web`
- `cor.D- `core.<bc>.infra.evemp- `core.<b -- `core.<bc>on- `corot- `col;- `core.<bha- `core.<bic- `cor.D- `core.<bc>.infra.evemp- na> **Applies t.
  or> **ApptP> **Applies t.
  or> **Applies to**: `core/an> **Applies t.
  > **Applies to\*Caor> **Appliesma> **Applies to**: `core/ex> **Last review vco> **Applies terThis document defines the canonical wayso- Commands: `core.<bc>.appli.
  > or> **ApptP> T or> **Applies to**: `cor4.> **Applies to\*Caor> **Appliesma> **Appli@U- Command models: `cr.- Commands: `core.<bctdE- Command models: `cr.- Commands: `core.<bctI- Com;
- `core.<bc>.application.port.out.*`al- `core.<bc>.application.port.out.*` OR `coac- Pa- `core.<bc>.application.port.out._`al- `core.<bc>.application.port.out._`OR`coac- `col Infra  o- `core.<bc>.infra.web`
- `core.<bha- `core.<bc>.infra.web`
- `cor.D- `core.<bc>.iha- `core.<bc>.infra.web`
- `core.<bha- `core.<bc.h- `core.<bha- `core.<bal- `cor.D- `core.<bc>.iha- `core.<br;- `core.<bha- `core.<bc>.infra.web`
- `cor.D- .U- `cor.D- `core.<bc>.infra.evemp- r.or> **ApptP> **Applies t.
or> **Applies to**: `core/an> \*\*Applies t.
  > **Applies to\*Caor> **Appliesma> **Applies to**: `core/ex> **Last review vcolior> **Applies to**: `corck> **Applies to\*Caor> **Appliesma> **Applir.or> **ApptP> T or> **Applies to**: `cor4.> **Applies to*Caor> **Appliesma> **Appli@U- Command models: `cr.- Commands: `core.<bctdE- Command models: `cr.- Command i- `core.<bc>.application.port.out.*`al- `core.<bc>.application.port.out.*` OR `coac- Pa- `core.<bc>.application.port.out._`al- `core.<bc>.application.port.out._`OR`coac- `col Inf r- `core.<bha- `core.<bc>.infra.web`
- `cor.D- `core.<bc>.iha- `core.<bc>.infra.web`
- `core.<bha- `core.<bc.h- `core.<bha- `core.<bal- `cor.D- `core.<bc>.iha- `core.<br;- `core.<bha- `core.<bc>.infra.web`
- tc- `cor.D- `core.<bc>.iha- `core.<bio- `core.<bha- `core.<bc.h- `core.<bha- `core.<mo- `cor.D- .U- `cor.D- `core.<bc>.infra.evemp- r.or> **ApptP> **Applies t.
  or> **Applies to**: `core/an> \*\*Applies t.
  > lior> **Applies to**: `core/an> **Applies t.
**Applies to*Caor> **Applie S> **Applies to*Caor> **Appliesma> **Applitc- `cor.D- `core.<bc>.iha- `core.<bc>.infra.web`
- `core.<bha- `core.<bc.h- `core.<bha- `core.<bal- `cor.D- `core.<bc>.iha- `core.<br;- `core.<bha- `core.<bc>.infra.web`
- tc- `cor.D- `core.<bc>.iha- `core.<bio- `core.<bha- `core.<bc.h- `core.<bha- `core.<mo- `cor.D- .U- `cor.D- `core.<bc>.infra.evemp- r.or> **ApptP> **Applies t.
  or> **Applies to**: `core/an> \*\*Applies t.
  > lior> **Applies to**: `core/an> **Applies t.
  > **Applies to*Caor> **Applie S> **Applies to*Caor> **Appliesma> **Apphead -50 /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/testing.md
  > head -50 /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/command_query_handlers.md
  > ls -la /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/
  > wc -l /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/\*.md
  > cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md << 'EOF'

# 🧠 Tchalanet — Architecture du Cache

_(Caffeine L1 + Redis L2 + TTL par Feature)_

> **Status**: NORMATIVE  
> **Scope**: Backend (tchalanet-server)  
> **Audience**: Backend developers, reviewers, ops  
> **Related**:
>
> - Ops cache controller (`/platform/cache`)
> - Cache policy & usage (conventions)

---

## 🎯 Objectifs

Le système de cache de Tchalanet vise les objectifs suivants :

- **Performance** : réduire la latence des chemins critiques (home publique, tirages, thèmes, tenant config).
- **Scalabilité** : permettre le partage de cache entre plusieurs instances backend.
- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL technique plus court côté Caffeine (L1).
- **Extensibilité par domaine** : chaque module déclare explicitement ses caches et TTL.
- **Tolérance aux pannes** : Redis peut être désactivé ; l'application fonctionne avec C# 🧠 Tchalanet — Architecture du Cache
  _(Caffeine L1 + Redis L2 + TTL par Feature)_
  > **Status**: NORMATIVE  
  > **Scope**: Bde\_(Caffeine L1 + Redis L2 + TTL par Featurt > **Status**: NORMATIVE  
  > **Scope**: Back \*> **Scope**: Backend (tc**> **Audience**: Backend developers, revine> **Related\*\*:
  >
  > - Ops cache controller (`/platform.
  > - Ops cache r> - Cache policy & usage (conventions)

---

# ---

## 🎯 Objectifs

Le système de il##e Le système de c n- **Performance** : réduire la latence des chemins critiques ?? **Scalabilité** : permettre le partage de cache entre plusieurs instances backend.

- **Cohérence maîtris?a- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL techniqu? **Extensibilité par domaine\*\* : chaque module déclare explicitement ses caches et TTL.
- **Tolérance aux pannes\* R- **Tolérance aux pannes\*\* : Redis peut être désactivé ; l'application fonctionne ave??_(Caffeine L1 + Redis L2 + TTL par Feature)_
  > **Status**: NORMATIVE  
  > **Scope**: Bde*(Caffeine L1 + Redis L2 + TTL par Featurt > \*id> **Status**: NORMATIVE  
  > **Scope**: Bde*? **Scope**: Bde\_(Caffei P> **Scope**: Back \*> **Scope**: Backend (tc**> **Audience\*\*: Backend developers, rere> - Ops cache controller (`/platform.
  >
  > - Ops cache r> - Cache policy & usage (conventions)

---

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?aLe système de i p- **Cohérence maîtris?a- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL techniqu? **Extensibilité par domaine** : chaque module déclare explicitement sesat- **Tolérance aux pannes\* R- **Tolérance aux pannes** : Redis peut être désactivé ; l'application fonctionne ave??_(Caffeine L1 + Redis L2 + TTL par Feature)_

> **Status**: NORMATIVE  
> **Scope**cl> **Status**: NORMATIVE  
> **Scope**: Bde*(Caffeine L1 + Redis L2 + TTL par Featurt > \*id> **Status**: NORMATIVE  
> **Scope**: Bde*? **Scope**: Bde*(Caffei P>an> **Scope**: Bde*(Caffei?e> **Scope**: Bde*? **Scope**: Bde*(Caffei P> **Scope**: Back \*> **Scope**: Backend (tcut> - Ops cache r> - Cache policy & usage (conventions)

---

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?ach---

# --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## 🎯 Objectifs

Le système de il##e LBu## ?`Le système de ise> **Status**: NORMATIVE

> **Scope**cl> **Status**: NORMATIVE  
> **Scope**: Bde*(Caffeine L1 + Redis L2 + TTL par Featurt > \*id> **Status**: NORMATIVE  
> **Scope**: Bde*? **Scope**: Bde*(Caffei P>an> **Scope**: Bde*(Caffei?e> **Scope**: Bde*? **Scope**: Bde*(Caffei P> **Scope**: Back \*> **Scope**: Backend (tcut> - Ops cache r> - Cache policy & usage (conventions)

---

# --ac> - Ops cache r> - Cacus> **Scope**cl> **Status\*`p> **Scope**: Bde\_(Caffeine L1 + Redislu> **Scope**: Bde\_? **Scope**: Bde\_(Caffei P>an> **Scope**: Bde\_(Caffei?e> **Scope\*\*: B

---

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?ach---

# --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## 🎯 Objectifs

Le système de il##e # Sp# ---

## 🎯 Objectifs

Le système de il##e L m## ?)Le système de it # --ac> - Ops cache r> - Cache policy & e `# ch# ---

## 🎯 Objectifs

Le système de i`.## 🎯 ObcaLe système de i :> **Scope**cl> **Status**: NORMATIVE

> **Scope**: Bde*(Caffeine L1 + R##> **Scope**: Bde*(Caffeine L1 + Redis c> **Scope**: Bde*? **Scope**: Bde*(Caffei P>an> **Scope**: Bde\_(Caffei?e> **Scope**: B

---

# --ac> - Ops cache r> - Cacus> **Scope**cl> **Status\*`p> **Scope**: Bde\_(Caffeine L1 + Redislu> **Scope**: Bde\_? **Scope**: Bde\_(Caffei P>an> **Scope**: Bde\_(Caffei?e> **Scope\*\*: B

---

# --ac> - Ops cache r> mi# - ----

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?ach---

# --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## - # -li# ---

## 🎯 Objectifs

Le système de il##e Lat## ?ALe système de irD# --ac> - Ops cache r> - Cache policy & uc `# ch# ---

## 🎯 Objectifs

Le système de iss## 🎯 Ob# Le système de ist## 🎯 Objectifs
Le système ndLe système de i

## 🎯 Objectifs

Le système de i`.## 🎯 ObcaLe système de i :> **Scope**cl> **Status**: NORMATIOpLe système de i- > **Scope**: Bde*(Caffeine L1 + R##> **Scope**: Bde*(Caffeine L1 + Redis c> **Scope** ----

# --ac> - Ops cache r> - Cacus> **Scope**cl> **Status\*`p> **Scope**: Bde\_(Caffeine L1 + Redislu> **Scope**: Bde\_? **Scope**: Bde\_(Caffei P>an> **Scope\*\*: -i# -an----

# --ac> - Ops cache r> mi# - ----

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?ach---

# --ac> - Ops cache r> - Cachre# -po# --ac> - Ops cache r> - Cache ppr# ---

## 🎯 Objectifs

Le système de il##e Lre## ?LLe système de ido# --ac> - Ops cache r> - Cache policy & mm `# ch# ---

## - # -li# ---

## 🎯 Objectifs

al## - # -li l## 🎯 ObjectalLe système drm /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md && wc -l /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md 2>&1 || echo "Fichier supprimé"
ls -la /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md
perl -i -pe 's/^@CacheEvict/ - `@CacheEvict/g; s/^un hook/ - un hook/g; s/^un evictor/ - un evictor/g' /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache && cp CACHE.md CACHE.md.backup
cat > /tmp/fix_cache.py << 'PYEOF'
#!/usr/bin/env python3
import re
with open('/Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md', 'r') as f:
content = f.read()

# Section 5 - Cache manuel

content = re.sub(
r'Le cache manuel est autorisé uniquement quand @Cacheable est insuffisant\.\n\nCas autorisés\n\nClés composites complexes\n\nProtection anti-stampede\n\nPayloads externes bruts \(HTTP / JSON\)\n\nInfra, batch, providers externes\n\nMUST\n\nEncapsuler le cache dans un helper dédié \(XxxCache\)\n\nAccéder via CacheManager\n\nConstruire les clés via CacheKeyBuilder\n\nTolérer l\'absence du cache\n\nMUST NOT\n\nManipuler le cache dans le domaine ou l\'application\n\nConstruire des clés Redis "à la main"',
'''Le cache manuel est autorisé uniquement quand `@Cacheable` est insuffisant.

### Cas autorisés

- Clés composites complexes
- Protection anti-stampede
- Payloads externes bruts (HTTP / JSON)
- Infra, batch, providers ex#!/usr/bin/env python3
  import re
  acimport re
  with open('diwith opeCa content = f.read()

# Section 5 - Cache manuel

content = re.sub(
r'Le cache manuel est autorisé uniquement quand @Cacheable est ie # Section 5 - Cache m ocontent = re.sub(
r'Lir r'Le cache ms '''Le cache manuel est autorisé uniquement quand `@Cacheable` est insuffisant.

### Cas autorisés

- Clés composites complexes
- Protection anti-stampede
- Payloads externes bruts (HTTP / JSON)
- Infra, batch, providers ex#!/usr/bin/env python3
  import re
  acimport re
  with open('diwith opeCa content = f.read()

# Section 5 - Cache manuel

content = re.sub(
r'Le cache manuel est autorisé uniquement quand @Cacheable est ie # Section 5 - Cache m ocontent = re.sub(
r'Lir r'Le cache ms '''Le cache ma''### Cas autorisés

- Clés composites complexes
- Protection anti-stampede
- Payloqu- Clés compositean- Protection anti-stampede
  re- Payloads externes bruts
  #- Infra, batch, providers ex#!/usr/bin`import re
acimport re
with open('diwith opeCa    t.acimportotwith open(
-# Section 5 - Cache manuel
content = re.sub(awcontent = re.sub(
    r'Lnc    r'Le cache m o    r'Lir    r'Le cache ms     '''Le cache manuel est autorisé uniquement quand`@Cacheable` est insuffisant.ac### Cas autorisés
- Clés composites complexes
- Protection anti-stampede
- Payloads externes bruts (HTTP / l- Clés compositenD- Protection anti-stampede
  ro- Payloads externes brutseN- Infra, batch, providers ex#!/usr/bine
  import re
  acimport re
  with open('diwith opeCa uacimportSpwith open(`'# Section 5 - Cache manuel
  content = re.sub(ntcontent = re.sub(
  r'LUS r'Le cache m\' r'Lir r'Le cache ms '''Le cache ma''### Cas autorisés
- Clés composites complexes
- Protection anft- Clés composites complexes
- Protection anti-stampede
- PayloqOT- Protection anti-stampede
  \n- Payloqu- Clés composite\re- Payloads externes bruts
  #- Infra, batch, providern\#- Infra, batch, providers import re
  acimport re
  with open('diwithr?cimportt with open(nn-# Section 5 - Cache manuel
  content = re.sub(awconcontent = re.sub(awcontentte r'Lnc r'Le cache m o r'Lien- Clés composites complexes
- Protection anti-stampede
- Payloads externes bruts (HTTP / l- Clés compositenD- Protection anti-stampede
  ro- Payloads exterat- Protection anti-stampede
  Se- Payloads externes brutsSTro- Payloads externes brutseN- Infra, batch, providers ex#!/usr/bine
  import re
  a\nimport re
  acimport re
  with open('diwith opeCa uacimportSpwith o, acimportetwith open(incontent = re.sub(ntcontent = re.sub(
  r'LUS r'Le cache m\' r'Lir sa r'LUS r'Le cache m\' r'Lié- Clés composites complexes
- Protection anft- Clés composites complexes
- Protection antft- Protection anft- Clés cote- Protection anti-stampede
- PayloqOT- Protehe- PayloqOT- Protection anio\n- Payloqu- Clés composite\re- PaOp#- Infra, batch, providern\#- Infra, batch, providers  
  cacimport re
  with open('diwithr?cimportt with open(nn-# Sectionirwith open(ntcontent = re.sub(awconcontent = re.sub(awcontentte r'Lnc r'Les - Protection anti-stampede
- Payloads externes bruts (HTTP / l- Clés compositenD- Protection anti-stampede
  ro- Pe - Payloads externes brutsforo- Payloads exterat- Protection anti-stampede
  Se- Payloads externes brutsSTro- ##Se- Payloads externes brutsSTro- Payloads extunimport re
  a\nimport re
  acimport re
  with open('diwith opeCa uacimportSpwith o, acimportetwithina\nimporc?cimport reeswith open(er r'LUS r'Le cache m\' r'Lir sa r'LUS r'Le cache m\' r'Lié- Clés composites complex u- Protection anft- Clés composites complexes
- Protection antft- Protection anft- Clés cote- Protectionre- Protection antft- Protection anft- Clés cnC- PayloqOT- Protehe- PayloqOT- Protection anio\n- Payloqu- Clés composiescacimport re
  with open('diwithr?cimportt with open(nn-# Sectionirwith open(ntcontent = re.sub(awconcontent = re.sub(awcontentte r'Lnc 1with open('nc- Payloads externes bruts (HTTP / l- Clés compositenD- Protection anti-stampede
  ro- Pe - Payloads externes brutsforo- Payloads exterat- Protection anti-stamplaro- Pe - Payloads externes brutsforo- Payloads exterat- Protection anti-stampede CSe- Payloads externes brutsSTro- ##Se- Payloads externes brutsSTro- Payloads exnsa\nimport re
  acimport re
  with open('diwith opeCa uacimportSpwith o, acimportetwithina\e\acimport relawith open(om- Protection antft- Protection anft- Clés cote- Protectionre- Protection antft- Protection anft- Clés cnC- PayloqOT- Protehe- PayloqOT- Protection anio\n- Payloqu- Clés composiescacimport re
  with open('diwithr?cimportt with open(nn-# Sectionirwviwith open('diwithr?cimportt with open(nn-# Sectionirwith open(ntcontent = re.sub(awconcontent = re.sub(awcontentte r'Lnc 1with open('nc- Payloads externes bruts (HTTP / l- Clés composite.mro- Pe - Payloads externes brutsforo- Payloads exterat- Protection anti-stamplaro- Pe - Payloads externes brutsforo- Paycat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE_CORRECTED.md << 'EOF'

# 🧠 Tchalanet — Architecture du Cache

_(Caffeine L1 + Redis L2 + TTL par Feature)_

> **Status**: NORMATIVE  
> **Scope**: Backend (tchalanet-server)  
> **Audience**: Backend developers, reviewers, ops  
> **Related**:
>
> - Ops cache controller (`/platform/cache`)
> - Cache policy & usage (conventions)

---

## 🎯 Objectifs

Le système de cache de Tchalanet vise les objectifs suivants :

- **Performance** : réduire la latence des chemins critiques (home publique, tirages, thèmes, tenant config).
- **Scalabilité** : permettre le partage de cache entre plusieurs instances backend.
- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL technique plus court côté Caffeine (L1).
- **Extensibilité par domaine** : chaque module déclare explicitement ses caches et TTL.
- **Tolérance aux pannes** : Redis peut être désactivé ; l'application fonctio# 🧠 Tchalanet — Architecture du Cache
  _(Caffeine L1 + Redis L2 + TTL par Feature)_
  > **Status**: NORMATIVE  
  > **Scope**: Backend (tcde\_(Caffeine L1 + Redis L2 + TTL par Featurt > **Status**: NORMATIVE  
  > **Scope**: Back \*> **Scope**: Backend (tc**> **Audience**: Backend developers, revine> **Related\*\*:
  >
  > - Ops cache controller (`/platform.
  > - Ops cache r> - Cache policy & usage (conventions)

---

# ---

## 🎯 Objectifs

Le système de il##e Le système de c n- **Performance** : réduire la latence des chemins critiques ?? **Scalabilité** : permettre le partage de cache entre plusieurs instances backend.

- **Cohérence maîtris?a- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL techniqu? **Extensibilité par domaine\*\* : chaque module déclare explicitement ses caches et TTL.
- **Tolérance aux pannes\* R- **Tolérance aux pannes\*\* : Redis peut être désactivé ; l'application fonctio# 🧠 ??_(Caffeine L1 + Redis L2 + TTL par Feature)_
  > **Status**: NORMATIVE  
  > **Scope**: Backend (tcde\_(Caffeine L1 + Redis L2 +es> **Status**: NORMATIVE  
  > **Scope**: Back?? **Scope**: Backend (tc??> **Scope**: Back \*> **Scope**: Backend (tc**> **Audience**: Backend developers, revine> **Rere> - Ops cache controller (`/platform.
  >
  > - Ops cache r> - Cache policy & usage (conventions)

---

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?aLe système de i p- **Cohérence maîtris?a- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL techniqu? **Extensibilité par domaine** : chaque module déclare explicitement sesat- **Tolérance aux pannes\* R- **Tolérance aux pannes** : Redis peut être désactivé ; l'application fonctio# 🧠 ??_(Caffeine L1 + Redis L2 + TTL par Feature)_

> **Status**: NORMATIVE  
> **Scope**cl> **Status**: NORMATIVE  
> **Scope**: Backend (tcde\_(Caffeine L1 + Redis L2 +es> **Status**: NORMATIVE  
> **Scope**: Back?? **Scope**: Backend (tc??> **Scope**an> **Scope**: Backend (tc?e> **Scope**: Back?? **Scope**: Backend (tc??> **Scope**: Back \*> **Scope**: B

## #> - Ops cache r> - Cache policy & usage (conventions)

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?aLe systèmch---

# --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## 🎯 Objectifs

Le système de il##e LBu## ?`Le système de ise> **Status**: NORMATIVE

> **Scope**cl> **Status**: NORMATIVE  
> **Scope**: Backend (tcde\_(Caffeine L1 + Redis L2 +es> **Status**: NORMATIVE  
> **Scope**: Back?? **Scope**: Backend (tc??> **Scope**an> **Scope**: Backend (tc?e> **Scope**: Back?? **Scope**: Backend (tc??> **Scope**: Back \*> **Scope**: B

## #> - Ops cache r> - Cache policy & usage (conventions)

# --ac> - Ops cache r> - Cache policy us> **Scope**cl> **Status\*`p> **Scope**: Backend (tcde\_(Caffeine lu> **Scope**: Back?? **Scope**: Backend (tc??> **Scope**an> **Scope\*\*: Backendm #> - Ops cache r> - Cache policy & usage (conventions)

---

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a?e---

# --ac> - Ops cache r> - Cache policy & usch---

#ni# vi# ---

## 🎯 Objectifs

Le système de il##e Ler## ?cLe système de i# # --ac> - Ops cache r> - Cache policy & usch---
`e  `# ch# ---

## 🎯 Objectifs

Le système de i`.## 🎯 ObcaLe système de i :> **Scope**cl> **Status**: NORMATIVE

> **Scope**: Backend (tcde*(Caffe##> **Scope**: Backend (tcde*(Caffeine c> **Scope**: Back?? **Scope**: Backend (tc??> **Scope**an> **Scope**: Backende

## #> - Ops cache r> - Cache policy & usage (conventions)

# --ac> - Ops cache r> - Cache policy us> **Scope**cl> **Status\*`p> **Scope\*\*: Backend (tcde\_(Caffeinein---

# --ac> - Ops cache r> - Cache policy us> \*\*Scope a# nt---

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a?e---

# --ac> - Ops cache r> - Cache policy & usch---

#ni# vi# ---

## 🎯 Objectifs

Le système de il##e Ler## ?cLe système de i#ft# Cr# ---

## 🎯 Objectifs

Le système de il##e Lun## ?eLe système de ie # --ac> - Ops cache r> - Cache polic#ni# vi# ---

## 🎯 Objectifs

Le système de #### 🎯 ObjurLe système de iOp `e  `# ch# ---

## 🎯 Objectifs

## Le système de i`.## 🎯 ObcaLe système de i :> **Scope**cc?# 🎯 ObjectOpLe système de i- > **Scope**: Backend (tcde*(Caffe##> **Scope**: Backend (tcde*(Caffeine c> **Scope** #> - Ops cache r> - Cache policy & usage (conventions)

# --ac> - Ops cache r> - Cache policy us> **Scope**cl> **Status\*`p> **Scope\*\*: Backend (tcde_ag---

# --ac> - Ops cache r> - Cache policy us> **Scope### Sc# --ac> - Ops cache r> - Cache policy us> **Scope a# nt---

# --ac> - Ops cache r> - Cache policy & usch--- C# --ac> - Ops cache r> - Cache policy & usch---

# ---

## he# ---

## 🎯 Objectifs

Le système de il##e Li ## ?sLe système de ila# --ac> - Ops cache r> - Cache pols #ni# vi# ---

## 🎯 Objectifs

Le système de rc## 🎯 ObjstLe système de ie ## 🎯 Objectifs
Le système de il##e Lun## ?eLe systèetLe système de ila## 🎯 Objectifs
Le système de #### 🎯 ObjurLe système de iOp `e  `# ch# ---

## 🎯 ObjecmeLe système de #la## 🎯 Objectifs

Le système de i`.## 🎯 ObcaLe système de/cLe système dels -la /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE*.md
wc -l /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md 2>&1
cat /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md | head -5 && echo "..." && cat /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md | tail -10
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache && ls -la
find /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache -name "*.md" -type f
cd /Users/bhebb/Documents/projets/tchalanet && python3 << 'PYEOF'
import os
file*path = '/Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md'
content = '''# 🧠 Tchalanet — Architecture du Cache
*(Caffeine L1 + Redis L2 + TTL par Feature)\_

> **Status**: NORMATIVE  
> **Scope**: Backend (tchalanet-server)  
> **Audience**: Backend developers, reviewers, ops  
> **Related**:
>
> - Ops cache controller (`/platform/cache`)
> - Cache policy & usage (conventions)

---

## 🎯 Objectifs

Le système de cache de Tchalanet vise les objectifs suivants :

- **Performance** : réduire la latence des chemins critiques (home publique, tirages, thèmes, tenant config).
- **Scalabilité** : permettre le partage de cache entre plusieurs instances backend.
- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL technique plus court côté Caffeine (L1).
- **Extensibilité par domaine** : chaque module déclare explicitement ses caches et TTL.
- \*\*import os
  file*path = '/Users/bhebb/Documents/projets/tchalanet/cafile_patctcontent = '''# 🧠 Tchalanet — Architecture du Cache
  *(Caffeine L1 + Redis L2 + TTL par Feature)\_
  > **Status**: NORMATIVE  
  > s _(Caffeine L1 + Redis L2 + TTL par Feature)_ > **Statut > **Status**: NORMATIVE  
  > **Scope**: Back \*> **Scope**: Backend (tc**> **Audience**: Backend developers, revine> **Related**:
  >
  > - Ops cache controller (`/platform.
  > - Ops cache r> - Cache policy & usage (conventions)

---

# ---

## 🎯 Objectifs

Le système de il##e Le système de c n- **Performance** : réduire la latence des chemins critiques ?? **Scalabilité** : permettre le partage de cache entre plusieurs instances backend.

- **Cohérence maîtris?a- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL techniqu? **Extensibilité par domaine\*\* : chaque module déclare explicitement ses caches et TTL.
- **import os
  file_path = R- **import os
  file*path = '/Users/bhebb/Documents/projets/tchalanet/cafile_patctcontent =??file_path = ?(Caffeine L1 + Redis L2 + TTL par Feature)*
  > **Status**: NORMATIVE  
  > s _(Caffeine L1 + Redis L2 + TTL par Feature)_
  > de> **Status**: NORMATIVE  
  > s _(Caffeine L1 +??s _(Caffeine L1 + Redis Pr> **Statut > **Status**: NORMATIVE  
  > **ScopRe> **Scope**: Back \*> **Scope**: Bacre> - Ops cache controller (`/platform.
  >
  > - Ops cache r> - Cache policy & usage (conventions)

---

# --ac> - Ops cache r> - Cache policy & usch---

# ---

## 🎯 Objectifs

Le système de il##e Le ag# a## ?aLe système de i p- **Cohérence maîtris?a- **Cohérence maîtrisée** : TTL métier géré au niveau Redis (L2), TTL techniqu? **Extensibilité par domaine** : chaque module déclare explicitement sesat- **import os
file*path = R- \*\*import os
file_path = '/Users/bhebb/Documents/projets/tchalanet/cafile_patctcontent =??file_path = ?(Caffeine L1 + Redis L2 + TTL par Feature)*

> **Status**: NORMATIVEclfile*path = .
> file_path = '/Users/bhebb/an> **Status**: NORMATIVE  
> s *(Caffeine L1 + Redis L2 + TTL par Feature)_
> de> **Status**: NORMATIVE  
> s _(Caffeine L1 +??s _(Caffeineons _(Caffeine L1 + Redis an> de> **Status**: NORMATIVE

## s _(Caffeine L1e`s _(Caffeine L1 +??s \_(Caffeto> **ScopRe> **Scope**: Back \*> **Scope\*\*: Bacre> - Ops cache controller (`/platfes> - Ops cache r> - Cache policy & usage (conventions)

# --ac> - Ops cache r> - ch---

# --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## 🎯 Objectifs

Le système de il##e LBu## ?`Le système de isefile*path = R- \*\*import os
file_path = '/Users/bhebb/Documents/projets/tchalanet/cafile_patctcontent =??file_path = ?(Caffeine L1 + Redis L2 + TTL par Feature)*

> **Status**: NORMATIVEclfile*path = .
> file_path = '/Users/bhebb/an> **Status**: NORMATIVE refile_path = '/Users/bhebb/##> **Status**: NORMATIVEclfile_path = .
> file_path = '/Users/bhebb/an> **Status**: NORMATIVE  
> s *(Caffeine L1 + Redis L2 + TTL par Featidfile*path = '/Users/bhebb/an> \*\*StatuSTs *(Caffeine L1 + Redis L2 + TTL par Feature)_
> de>e`> de> **Status**: NORMATIVE  
> s _(Caffeine L1e
> s _(Caffeine L1 +??s _(Caffe Ks _(Caffeine L1e`s _(Caffeine L1 +??s \_(Caffeto> **ScopRe> **Scope**: Back \*> **Scope\*\*ec---

# --ac> - Ops cache r> - ch---

# --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## 🎯 Objectifs

Le système de il##e LBu## ?`Le système de isefile_path =  R- **imSp# Pr# --ac> - Ops cache r> - Cache  `# ch# ---

## 🎯 Objectifs

Le système de i`.## 🎯 ObcaLe système de i :file_path = '/Users/bhebb/Documents/projets/tchalanet/cafile_patctcontent # > **Status**: NORMATIVEclfile_path = .
file_path = '/Users/bhebb/an> **Status**: NORMATIVE refile_path = '/Users/bhebb/##> **Status**:uefile_path = '/Users/bhebb/an> **Statuonfile_path = '/Users/bhebb/an> **Status**: NORMATIVE  
s _(Caffeine L1 + Redis L2 + TTL par Featidfile_path = '/Users/bh? s _(Caffeine L1 + Redis L2 + TTL par Featidfile_path ? de>e`> de> **Status**: NORMATIVE  
s _(Caffeine L1e
s _(Caffeine L1 +??s _(Caffe Ks _(Caffeine L1e`s _(Caffeine L1 +??s _(Cafnds _(Caffeine L1e
s _(Caffeine L1 +?s? \_(Caffeine L1- # --ac> - Ops cache r> - ch---

# --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## 🎯 Objectifs

Le système i# --ac> - Ops cache r> - Cach s `# ch# ---

## 🎯 Objectifs

Le système de iio## 🎯 Ob
-Le système de ioi## 🎯 Objectifs
Le système de i`.## 🎯 ObcaLe système de i :file_path = '/Users/bhebb/Documents/projets/tchalOpLe système de i- file_path = '/Users/bhebb/an> **Status**: NORMATIVE refile_path = '/Users/bhebb/##> **Status**:uefile_path = '/Users/bhebb/an> **Statuonfile_path = '/Users/bhebbens _(Caffeine L1 + Redis L2 + TTL par Featidfile_path = '/Users/bh? s _(Caffeine L1 + Redis L2 + TTL par Featidfile_path ? de>e`> de> **Status**: NORMATIVE  
s _(Caffeine L1e
s _(Caffeine eis _(Caffeine L1e
s _(Caffeine L1 +??s _(Caffe Ks _(Caffeine L1e`s _(Caffeine L1 +??s _(Cafnds _(Caffeine L1e
s _(Caffeine L1 +?s? _(Caffeine L1- # --ac> - nts _(Caffeine L1
-s _(Caffeine L1 +?s? _(Caffeine L1- # --ac> - Ops cache r> - ch---

# --ac> - Ops cache r>e # --ac> - Ops cache r> - Cache policy & usch---

`# ch# ---

## ?p `# ch# ---

## 🎯 Objectifs

Le système i# f## 🎯 ObteLe système i# Fi## 🎯 Objectifs
Le système de iio## 🎯 Ob
-Le syst NLe système de i {-Le système de ioi## 🎯 )
Le syspython3 -c "
file_path = '/Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md'
print(f'File path: {file_path}')
import os
print(f'File exists: {os.path.exists(file_path)}')
print(f'File is writable: {os.access(file_path, os.W_OK)}')
with open(file_path, 'r') as f:
lines = f.readlines()
print(f'Current lines: {len(lines)}')
print(f'Line 110: {lines[109].strip() if len(lines) > 109 else \"N/A\"}')
"
cd /Users/bhebb/Documents/projets/tchalanet && echo "=== Vérification des changements ===" && echo "" && echo "1. Références à handler_command.md remplacées par command_query_handlers.md:" && grep -r "command_query_handlers\.md" tchalanet-server/docs --include="_.md" | wc -l && echo "" && echo "2. Références à NAMING.md ajoutées:" && grep -r "NAMING\.md" tchalanet-server/docs --include="_.md" | wc -l && grep -r "NAMING\.md" AGENTS.md | wc -l
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs && grep -n "command_query_handlers" README.md ARCHITECTURE.md PLAYBOOK.md conventions/web_api.md 2>/dev/null | head -20
cat /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/README.md | grep -A 2 -B 2 "handler_command\|command_query_handlers\|NAMING"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && find docs -name "\*.md" -type f -exec grep -l "handler_command\|command_query_handlers\|NAMING" {} \; | sort | uniq
cd /Users/bhebb/Documents/projets/tchalanet && cat << 'EOF' > /tmp/changements_summary.md

# 📋 Résumé des changements effectués

## ✅ Renommage de `handler_command.md` → `command_query_handlers.md`

### Fichiers mis à jour (références modifiées) :

1. **`tchalanet-server/docs/README.md`**
   - ✅ Section "Exécution / CQRS" : `handler_command.md` → `command_query_handlers.md`
   - ✅ Tableau de référence rapide : mise à jour
   - ✅ Ajout de nouvelle section "Conventions générales" avec `NAMING.md`
2. **`tchalanet-server/docs/ARCHITECTURE.md`**
   - ✅ Section 3.2 (ligne 85) : Controllers
   - ✅ Section 3.3 (ligne 93) : CQRS via bus
   - ✅ Section 3.4 (ligne 101) : Événements after-commit
   - ✅ Section 11 (ligne 323) : Batch
   - ✅ Hiérarchie de documentation : ajout de `NAMING.md` au niveau 3
3. **`tchalanet-server/docs/PLAYBOOK.md`**
   - ✅ Section 1 "Règles incontournables" : ajout de `NAMING.md` dans la liste DO
   - ✅ Section 6 (ligne 128) : CQRS & Handlers
4. \*\*`tch# 📋 Résumé des changements effectués

## ✅ Renommage de `handler_command.md` → t ## ✅ Renommage de `handler_command.md` on### Fichiers mis à jour (références modifiées) :

1. **`tchalanet-se"B1. **`tchalanet-server/docs/README.md`\*\*
   - ✅ Sba - ✅ Section "Exécution / CQRS" : oc - ✅ Tableau de référence rapide : mise à jour
   - ✅ Ajout de nouvelle section "an - ✅ Ajout de nouvelle section "Conventions gén??2. **`tchalanet-server/docs/ARCHITECTURE.md`**
   - ✅ Section 3.2 (ligne 8ch - ✅ Section 3.2 (ligne 85) : ControllersCH - ✅ Section 3.3 (ligne 93) : CQRS via bhi - ✅ Section 3.4 (ligne 101) : Événemen-s - ✅ Section 11 (ligne 323) : Batch
   - ✅ Hiérarchi r - ✅ Hiérarchie de documentation `*3. **`tchalanet-server/docs/PLAYBOOK.md`\*\*
   - ✅ Section 1 "Règles ? - ✅ Section 1 "Règles incontournabl ` - ✅ Section 6 (ligne 128) : CQRS & Handlers
2. **`tch# 📋 Résumé des chang\* 4. **`tch# 📋 Résumé des changements effect* ## ✅ Renommage de `handler_command.md` → t ## io1. **`tchalanet-se"B1. **`tchalanet-server/docs/README.md`**
   - ✅ Sba - ✅ Section "Exécution / CQRS" : oc - ✅ Tableau de référ* - ✅ Sba - ✅ Section "Exécution / CQRS" : oc - ?* - ✅ Ajout de nouvelle section "an - ✅ Ajout de nouvelle section "Conventions gén??2. \*\*`tchalanmd cd /Users/bhebb/Documents/projets/tchalanet && echo "=== VÉRIFICATION FINALE ===" && echo "" && echo "✅ Fichiers corrigés (format Markdown):" && ls -1 tchalanet-server/docs/conventions/testing.md tchalanet-server/docs/conventions/command_query_handlers.md tchalanet-server/src/main/java/com/tchalanet/server/common/cache/CACHE.md 2>/dev/null | wc -l && echo "" && echo "✅ Documentation mise à jour:" && echo " - README.md" && echo " - ARCHITECTURE.md" && echo " - PLAYBOOK.md" && echo " - AGENTS.md" && echo " - conventions/api/web_api.md" && echo "" && echo "📌 Tous les changements ont été appliqués avec succès!"
     cat << 'EOF' > /tmp/batch_implementation_summary.md

# 📋 Résumé de l'implémentation Batch

## ✅ Fichiers créés avec succès

### 1. Controller & DTO

- ✅ `LaunchJobRequest.java` - DTO typé avec validation
- ✅ `BatchJobController.java` - Controller sécurisé avec allowlist

### 2. Context & Timezone

- ✅ `TenantZoneResolver.java` - Interface de résolution timezone
- ✅ `TenantZoneResolverImpl.java` - Implémentation par défaut (UTC)
- ✅ `BatchTchContextBinder.java` - Binder JobParams → TchRequestContext

### 3. Cache

- ✅ `BatchCacheConfig.java` - CacheSpecProvider avec TTL L1/L2
- ✅ `BatchFlagCache.java` - Helper cache conforme à la policy
- ✅ `BatchGate.java` - Refactoré pour utiliser BatchFlagCache

### 4. Listener

- ✅ `BatchTchContextJobListener.java` - Listener obligatoire documenté

### 5. Tests

- ✅ `BatchTchContextBinderTest.java` - Tests complets (JUnit 5 + AssertJ)
- ✅ `BatchFlagCacheTest.java` - Tests complets

### 6. Documentation

- ✅ `batch.md` - Mise à jour avec:
  # 📋 Résumé de l'implémentation Batch

## ✅ en## ✅ Fichiers créés avec succès

### ##### 1. Controller & DTO

- ✅ `Launcr- ✅ `LaunchJobRequesmp- ✅ `BatchJobController.java` - Controller sécurisé an### 2. Context & Timezone
- ✅ `TenantZoneResolver.java` - Interfaceon- ✅ `TenantZoneResolve W- ✅ `TenantZoneResolverImpl.java`- Implémentation par défaut`c- ✅ `BatchTchContextBinder.java` - Binder JobParams → TchRequestCo d### 3. Cache
- ✅ `BatchCacheConfig.java` - CacheSpecProvider avec TTL L1fo- ✅ `Batc `- ✅ `BatchFlagCache.java` - Helper cache conforme à la policty- ✅ `BatchGate.java` - Refactoré pour utiliser BatchFlagCach -### 4. Listener
- ✅ `BatchTchContextJobListener.java` - Listeom- ✅ `BatchTcve### 5. Tests
- ✅ `BatchTchContextBinderTest.java` - Tests complets (JU
- ✅ `Batc c- ✅ `BatchFlagCacheTest.java` - Tests complets

### 6. Documentation

- ?\*### 6. Documentation
- ✅ `batch.md` - Mise àbs- ✅ `batch.md` - nf # 📋 Résumé de l'implémentation## ✅ en## ✅ Fichiers créés avec succ?i### ##### 1. Controller & DTO
- ✅ `Launcr-r? ✅ `Launcr- ✅ `LaunchJo??- ✅ `TenantZoneResolver.java`- Interfaceon- ✅`TenantZoneResolve W- ✅ `TenantZoneResolverImpl.java` - Implémentut- ✅ `BatchCacheConfig.java` - CacheSpecProvider avec TTL L1fo- ✅ `Batc `- ✅ `BatchFlagCache.java` - Helper cache conforme à la policty- ✅ `BatchGate.java` - Refactoré pour utiliser BatchFlagCach -### 4. Listener
  ir- ✅ `BatchTchContextJobListener.java` - Listeom- ✅ `BatchTcve### 5. Tests
- ✅ `BatchTchContextBinderTest.java` - Tests complets (JU
- ✅ `Batc c- ✅ `BatchFlagCacheTest.java` - Tests complets

### 6. Documentatios - ✅ `BatchTchContextBinderTest.java` - Tests complets (JU

- ✅ `Batc c- on - ✅ `Batc c- ✅ `BatchFlagCacheTest.java` - Tests compCH### 6. Documentation
- ?\*### 6. Documentation
- ✅ `batch.he- ?*### 6. Documehe- ✅ `batch.md`- Mise ?n- ✅`Launcr-r? ✅ `Launcr- ✅ `LaunchJo??- ✅ `TenantZoneResolver.java` - Interfaceon- ✅ `TenantZoneResolve W- ✅ `TenantZoneResolverImpl.java`- Impl?eir- ✅`BatchTchContextJobListener.java`- Listeom- ✅`BatchTcve### 5. Tests
- ✅ `BatchTchContextBinderTest.java` - Tests complets (JU
- ✅ `Batc c- ✅ `BatchFlagCacheTest.java` - Tests complets
  ##cat > /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/batch.md << 'EOF'

# Conventions — Batch & Jobs

## Les batchs servent à exécuter des traitements asynchrones, répétitifs ou de maintenance (results, refresh, cleanup, reindex, etc.) **sans polluer la logique métier**.

## 1) Standard Batch Job Params (MUST)

Tous les jobs batch utilisent **snake_case** pour les paramètres :
| Paramètre | Type | Obligatoire | Description |
|-----------|------|-------------|-------------|
| `tenant_id` | UUID string | **OUI** | ID du tenant (requis pour RLS/contexte) |
| `tenant_code` | String | Non | Code humain (si nécessaire) |
| `request_id` | UUID string | Non | GUID / correlation (généré si absent) |
| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | String | Non | Override ops uniquement (sinon from tenant) |
| `ts` | Date | Non | Timestamp anti-collision |
**Exemple** :

````java
JobParameters params = new JobParametersBuilder()
    .addString("tenant_id", te# Conventions — Batch & Jobs
Les batchs servent à exécuter des traitements asynchrones, rép?gLes batchs servent à exécut("---
## 1) Standard Batch Job Params (MUST)
Tous les jobs batch utilisent **snake_case** pour les paramètres :
| Paramètre | Type | Obligatoire | Description |
|--------n
##blTous les jobs batch utilisent **snake J| Paramètre | Type | Obligatoire | Description |
|-----------|---hC|-----------|------|-------------|-------------|tu| `tenant_id` | UUID string | **OUI** | ID du tjo| `tenant_code` | String | Non | Code humain (si nécessaire) |
| `request_id` |s
| `request_id` | UUID string | Non | GUID / correlation (gén?e| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | Strit | `zone_id` | String | Non | Override ops uniquement (sinoab| `ts` | Date | Non | Timestamp anti-collision |
**Exemple** :
```java
JoPa**Exemple** :
```java
JobParameters params = nee ```java
JobPerJobParem    .addString("tenant_id", te# Conventions — xpLes batchs servent à exécuter des traitements asynchronesel## 1) Standard Batch Job Params (MUST)
Tous les jobs batch utilisent **snake_case** pour les paramèt dTous les jobs batch utilisent **snake j| Paramètre | Type | Obligatoire | Description |
|--------n
##blT##|--------n
##blTous les jobs batch utilisent **str##blTous ti|-----------|---hC|-----------|------|-------------|-------------|tu| `tenant_id` | UUID stum| `request_id` |s
| `request_id` | UUID string | Non | GUID / correlation (gén?e| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | Strit | `zone_id` | Strte| `request_id` |? | `zone_id` | Strit | `zone_id` | String | Non | Override ops uniquement (sinoab| `ts` | Date | Non | Timestamp anti-colli6)**Exemple** :
```java
JoPa**Exemple** :
```java
JobParameters params = nee ```java
JobPerJobParem    .addString("tenant_id", te l```java
JoPansJoPa**rr```java
JobParame JobParucJobPerJobParem    .addString("tenéTous les jobs batch utilisent **snake_case** pour les paramèt dTous les jobs batch utilisent **snake j| Paramètre | Type | Obligatoire | Description |
|--------nd|--------n
##blT##|--------n
##blTous les jobs batch utilisent **str##blTous ti|-----------|---hC|-----------|------|-------------|-------------|tu| `t m##blT##|-da##blTous les jobvi| `request_id` | UUID string | Non | GUID / correlation (gén?e| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | Strit | `zone_id` | Stc| `zone_id` | Strit | `zone_id` | Strte| `request_id` |? | `zone_id` | Strit | python3 << 'PYEOF'
content = '''# Conventions — Batch & Jobs
Les batchs servent à exécuter des traitements asynchrones, répétitifs ou de maintenance (results, refresh, cleanup, reindex, etc.) **sans polluer la logique métier**.
---
## 1) Standard Batch Job Params (MUST)
Tous les jobs batch utilisent **snake_case** pour les paramètres :
| Paramètre | Type | Obligatoire | Description |
|-----------|------|-------------|-------------|
| `tenant_id` | UUID string | **OUI** | ID du tenant (requis pour RLS/contexte) |
| `tenant_code` | String | Non | Code humain (si nécessaire) |
| `request_id` | UUID string | Non | GUID / correlation (généré si absent) |
| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | String | Non | Override ops uniquement (sinon from tenant) |
| `ts` | Date | Non | Timestamp anti-collision |
**Exemple** :
```java
JobParameters params = new JobParametersBuilder()
    .addString("tenant_id", tenantId.toString())
    .addString("request_id", UUID.randomUUID().tcontent = '''# CoddLes batchs servent à exécuter des traite("---
## 1) Standard Batch Job Params (MUST)
Tous les jobs batch utilisent **snake_case** pour les paramètres :
| Paramètre | Type | Obligatoire | Description |
|--------n
##blTous les jobs batch utilisent **snake J| Paramètre | Type | Obligatoire | Description |
|-----------|---hC|-----------|------|-------------|-------------|tu| `tenant_id` | UUID string | **OUI** | ID du tjo| `tenant_code` | String | Non | Code humain (si nécessaire) |
| `request_id` |s
| `request_id` | UUID string | Non | GUID / correlation (gén?e| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | Strit | `zone_id` | String | Non | Override ops uniquement (sinoab| `ts` | Date | Non | Timestamp anti-collision |
**Exemple** :
```java
JoPa**Exemple** :
```java
JobParameters params = nee ```java
JobPerJobParem    .addString("tenant_id", tenantId.toString())xp    .addString("request_id", UUID.randomUUID().nt## 1) Standard Batch Job Params (MUST)
Tous les jobs batch utilisent **snake_case** pour les paramètres :
| Para dTous les jobs batch utilisent **snake j| Paramètre | Type | Obligatoire | Description |
|--------n
##blT##|--------n
##blTous les jobs batch utilisent **str##blTous ti|-----------|---hC|-----------|------|-------------|-------------|tu| `tenant_id` | UUID stum| `request_id` |s
| `request_id` | UUID string | Non | GUID / correlation (gén?e| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | Strit | `zone_id` | Strte| `request_id` |? | `zone_id` | Strit | `zone_id` | String | Non | Override ops uniquement (sinoab| `ts` | Date | Non | Timestamp anti-colli6)**Exemple** :
```java
JoPa**Exemple** :
```java
JobParameters params = nee ```java
JobPerJobParem    .addString("tenant_id", te l```java
JoPansJoPa**rr```java
JobParame JobParucJobPerJobParem    .addString("tenéTous les jobs batch utilisent **snake_case** pour les paramètres :
| Para dTous les jobs batch utilisent **snake j| Paramètre | Type | Obligatoire | e| Para dTous les jobs batch utilisent **snake j| Paramètre | Type--|--------n
##blT##|--------n
##blTous les jobs batch utilisent **str##blTous ti|-----------|--- m##blT##|-da##blTous les jobvi| `request_id` | UUID string | Non | GUID / correlation (gén?e| `triggered_by` | String | Non | Keycloak sub ou `"api"` |
| `zone_id` | Strit | `zone_id` | Stc| `zone_id` | Strit | `zone_id` | Strte| `request_id` |? | `zone_id` | Strit | `zone_id` | String | Non | Override ops uni/t```java
JoPa**Exemple** :
```java
JobParameters params = nee ```java
JobPerJobParem    .addString("tenant_id", te l```java
JoPansJoPa**rr```java
JobParame JobParucJobPerJobParem    .addString("? JoPa** d```java
JobParamonJobParplJobPerJobParem    .arm -f /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/batch.md && ls -la /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/batch.md 2>&1 || echo "Fichier supprimé"
wc -l /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/batch.md && head -20 /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/batch.md && echo "..." && tail -10 /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/docs/conventions/batch.md
cat << 'EOF'
# ✅ Résumé des corrections de formatage
## Fichiers corrigés avec succès :
### 1. BatchCacheConfig.java
- **Problème** : Code complètement inversé (lignes de bas en haut)
- **Solution** : Restauration de l'ordre correct
- **Status** : ✅ Aucune erreur de compilation
### 2. TenantZoneResolver.java
- **Problème** : Code inversé
- **Solution** : Restauration de l'ordre correct (interface propre)
- **Status** : ✅ Aucune erreur de compilation
### 3. batch.md
- **Problème** : Format Markdown incorrect, sections dupliquées
- **Solution** : Réécriture complète avec format standard
- **Status** : ✅ Format Markdown correct (120 lignes)
  - Titres numérotés (## 1), 2), 3)...)
  - Séparateurs `---` entre sections
  - Listes à puces (`-`)
  - Blocs de code Java (` ```java `)
  - Tables Markdown bien formatées
  - Code inline avec backticks
## Fichiers vérifiés et confirmés corrects :
- ✅ LaunchJobRequest.java
- ✅ BatchFlagCache.java
- ✅ TenantZoneResolverImpl.java
- ✅ Ba# ✅ Résuol## Fichiers corrigés avec succès :
### 1- ### 1. BatchCacheConfig.java
- **Prnt- **Problème** : Code compat- **Solution** : Restauration de l'ordre correct
- **Status** : ✅ it- **Status** : ✅ Aucune erreur de compilationnt### 2. TenantZoneResolver.java
- **Problèmes - **Problème** : Code inversé -- **Solution** : Restauration  A- **Status** : ✅ Aucune erreur de compilation
### 3. batch.md
- **### 3. batch.md
- **Problème** : Format Markdmd- **Problème*ns- **Solution** : Réécriture complète avec format standard
- * *- **Status** : ✅ Format Markdown correct (120 lignes)
  -ts  - Titres numérotés (## 1), 2), 3)...)
  - Séparatch  - Séparateurs `---` entre sections
  ti  - Listes à puces (`-`)
  -rm /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/web/BatchJobController.java && echo "✅ Ancien BatchJobController supprimé"
rm /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/batch/BatchTchContextBinder.java && echo "✅ Ancien BatchTchContextBinder supprimé"
rm /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/batch/BatchTchContextJobListener.java && echo "✅ Ancien BatchTchContextJobListener supprimé"
rm /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/batch/BatchCacheConfig.java && echo "✅ Ancien BatchCacheConfig supprimé"
find /Users/bhebb/Documents/projets/tchalanet/tchalanet-server/src/main/java/com/tchalanet/server/common/batch -name "*.java" -type f | sort
cat << 'EOF'
# ✅ Nettoyage des doublons terminé
## Fichiers supprimés (anciens doublons) :
1. ❌ `/common/web/BatchJobController.java`
   - Utilisant reflection (non sécurisé)
   - Remplacé par `/common/batch/infra/web/BatchJobController.java`
2. ❌ `/common/batch/BatchTchContextBinder.java`
   - Ancien code sans TenantZoneResolver
   - Remplacé par `/common/batch/infra/context/BatchTchContextBinder.java`
3. ❌ `/common/batch/BatchTchContextJobListener.java`
   - Version basique sans logs détaillés
   - Remplacé par `/common/batch/infra/listener/BatchTchContextJobListener.java`
4. ❌ `/common/batch/BatchCacheConfig.java`
   - Avec imports en doublon
   - Remplacé par `/common/batch/infra/cache/BatchCacheConfig.java`
## Fichiers conservés (versions conformes au guide) :
✅ `/common/batch/BatchGate.java` (UNIQUE - correct)
✅ `/common/batch/infra/web/BatchJobController.java` (sécurisé, allowlist)
✅ `/common/batch/infra/web/LaunchJobRequest.java` (DTO typé)
✅ `/common/batch/infra/conte# ✅ NettoCo## Fichiers supprimés (anciens doub`/1. ❌ `/common/web/BatchJobController.javtJ   - Utilisant reflection (non sécurisé)
 tc   - Remplacé par `/common/batch/infra/whe2. ❌ `/common/batch/BatchTchContextBinder.java`
   - Ancien code he   - Ancien code sans TenantZoneResolver
   - Relv   - Remplacé par `/common/batch/infra/t3. ❌ `/common/batch/BatchTchContextJobListener.java`
   - Version basiqu``   - Version basique sans logs détaillés
   - Rempl ?  - Remplacé par `/common/batch/infra/l??4. ❌ `/common/batch/BatchCacheConfig.java`
   - Avec imports en doublon
   - Rac   - Avec imports en doublon
   - Remplacé??  - Remplacé par `/common.j## Fichiers conservés (versions conformes au guide) :
✅ `/commonis✅ `/common/batch/BatchGate.java` (UNIQUE - correct)Jo✅ `/common/batch/infra/web/BatchJobController.javaja✅ `/common/batch/infra/web/LaunchJobRequest.java` (DTOcat << 'EOF'
# ✅ Résumé final - Nettoyage complet des doublons
## 🎯 Problème identifié
J'avais créé des doublons de fichiers batch lors de l'implémentation :
- 2 x BatchJobController
- 2 x BatchTchContextBinder
- 2 x BatchTchContextJobListener
- 2 x BatchCacheConfig
## 🧹 Actions effectuées
### 1. Suppression des doublons (4 fichiers)
❌ **Supprimés** (anciens fichiers non conformes) :
- `/common/web/BatchJobController.java` → reflection, non sécurisé
- `/common/batch/BatchTchContextBinder.java` → sans TenantZoneResolver
- `/common/batch/BatchTchContextJobListener.java` → sans logs détaillés
- `/common/batch/BatchCacheConfig.java` → avec imports en doublon
### 2. Correction de BatchGate.java
✅ **Corrigé** :
- Suppression import `java.util.Map` inutilisé
- Suppression méthode `parseBool()` non utilisée
- Simplification appel `ProblemRest.forbidden()`
## 📁 Structure finale (propre et conforme)
````

common/
├── batch/
│ ├── BatchGate.java ← SEUL fichier à# ✅ Résu?# 🎯 Problème identifié
J'avais créé des doub J'avais créé des doublonsch- 2 x BatchJobController

- 2 x BatchTchContextBinder
- 2 x BatchTchCon? 2 x BatchTchContextBi ? 2 x BatchTchContextJobListxt- 2 x BatchCacheConfig

## 🧹 l## 🧹 Actions effec? ### 1. Suppression des doutJ❌ **Supprimés** (anciens fichiers non co? - `/common/web/BatchJobController.java` → reflecti - `/common/batch/BatchTchContextBinder.java` → sans TenantZoneResolme- `/common/batch/BatchTchContextJobListener.java` → sans logs détailtZ- `/common/batch/BatchCacheConfig.java` → avec imports en doublon

### 2. ?## 2. Correction de BatchGate.java

✅ **Corrigé** :

- Suppressich✅ **Corrigé** :
- Suppression ié- Suppression impwl- Suppression méthode `parseBool()` non utili a- Simplification appel `ProblemRest.forbidden()`
  `## 📁 Structure finale (propre et conforme)`hT```
  common/
  ├── batch/
  │ ├──ogco6.├─Ba│ ├──jaJ'avais créé des doub J'avais créé des doublonsch- 2 x BatchJobController
- 2 x Batch? - 2 x BatchTchContextBinder
- 2 x BatchTchCon? 2 x BatchTchContextBi ? 2.j- 2 x BatchTchCon? 2 x Bat# ## 🧹 l## 🧹 Actions effec? ### 1. Suppression des doutJ❌ **Supprimés** (anciens fichie p### 2. ?## 2. Correction de BatchGate.java
  ✅ **Corrigé** :
- Suppressich✅ **Corrigé** :
- Suppression ié- Suppression impwl- Suppression méthode `parseBool()` non utili a- Simplification appel `ProblemRest.forbidden()`
  `## 📁 Structure finale (propre et conforme)`hT```
  common/
  ├── batch/
  │ ├──ogco6.├─Ba│ ├──jaJ'avais créé dte✅ **Corrigé** :
- Suppressich✅ \*\*Corric- Suppressich✅ tc- Suppression ié- Suppressionag`## 📁 Structure finale (propre et conforme)`hT``
common/
├── batch/
│   ├──ogco6.├─Ba│   ├──jaJ'avaco`hT``
  common/
  ├── batcat << 'EOF'

# ✅ Résumé final de la session - Fichiers Bootstrap & Time

## 📦 Fichiers créés avec succès

### 1. Context & Bootstrap (5 fichiers)

#### TenantContextInfo.java

- **Package**: `com.tchalanet.server.common.context`
- **Type**: Record
- **Rôle**: Info minimale tenant (identity + currency + timezone)
- **Validation**: Compact constructor avec null checks

#### TenantBootstrapInfo.java

- **Package**: `com.tchalanet.server.common.bootstrap.tenant`
- **Type**: Record
- **Rôle**: Info bootstrap tenant (TenantId + ZoneId + Currency)
- **Validation**: Compact constructor avec null checks

#### TenantBootstrapLookup.java

- **Package**: `com.tchalanet.server.common.bootstrap.tenant`
- **Type**: @Component (BOOTSTRAP ONLY - bypasse RLS)
- **Méthodes**:
  - `findTenantUuidByCode(String)` → UUID
  - `findTenantInfoByCode(String)` → TenantBootstrapInfo
  - `findTenantInfoById(TenantId)` → TenantBootstrapInfo
- **Fallbacks**: UTC (timezone), USD (currency)
- **Datasource**: `@Qualifier("rawDataS# ✅ Résuyp## 📦 Fichiers créés avec succès

### 1. Context & Bootst s### 1. Context & Bootstrap (5 fichie- #### TenantContextInfo.java

- **Packagde- **Package**: `com.tchalanf- **Type\*\*: Record
- **Rôle**: Info minimale tenan- - **Rôle**: Infoti- **Validation**: Compact constructor avec null checks

#### Tenanhe#### TenantBootstrapInfo.java

- **Package**: `com.tchts- **Package**: `com.tchalanepl- **Type**: Record
- **Rôle**: Info bootstrap tenant (Tenin- **Rôle**: Info b- - **Validation**: Compact constructor avec null checks

#### Tenaet#### TenantBootstrapLookup.java

- **Package**: `com.t`p- **Package**: `com.tchalanet.st- **Type**: @Component (BOOTSTRAP ONLY - bypasse RLS)
- **M?#- **Méthodes\*\*:
  - `findTenantUuidByCode(String)` : - `findTenantve  - `findTenantInfoByCode(String)`→ Ten -  -`findTenantInfoById(TenantId)` → TenantBootstrapInfes- **Fallbacks**: UTC (timezone), USD (currency)
- **Datass - **Datasource**: `@Qualifier("rawDataS# ✅ Rme### 1. Context & Bootst s### 1. Context & Bootstrap (5 fichie- #### TenantContextInfo.jaom- **Packagde- **Package**: `com.tchalanf- **Type**: Record
- **Rôle**: Info minimale ten(Z- **Rôle**: Info minimale tenan- - **Rôle**: Infoti- \*\*\*\*#### Tenanhe#### TenantBootstrapInfo.java
- **Package**: `com.tchts- **Package**: `com.tchalanepl- **Type\*# - **Package**: `com.tchts- **Package**: c? **Rôle**: Info bootstrap tenant (Tenin- **Rôle**: Info b- - **Validatina#### Tenaet#### TenantBootstrapLookup.java
- **Package**: `com.t`p- **Package**: `com.tchalanet.st- **Type**: @Compt.- **Package**: `com.t`p- **Package**: `cot
- **M?#- **Méthodes\*\*:
  - `findTenantUuidByCode(String)` : - `findTenantve  - `findTenantInfoByCode(St? - `findTenantUuidByCo: - **Datass - **Datasource**: `@Qualifier("rawDataS# ✅ Rme### 1. Context & Bootst s### 1. Context & Bootstrap (5 fichie- #### TenantContextInfo.jaom- **Packagde- **Package**: `com.tchalanf- **Type**: Rext- **Rôle**: Info minimale ten(Z- **Rôle**: Info minimale tenan- - **Rôle**: Infoti- \*\***#### Tenanhe#### TenantBootstrapInfo.java
- **Package**: `com.tchts- **Package**: `com.tchalanepl- **Type\*# - **PactJ- **Package**: `com.tchts- **Package**: `com.tchalanepl- **Type\*# - **Package**: `com.tchts- **Package**: c? **Rôle**: Info bootsno- **Package**: `com.t`p- **Package**: `com.tchalanet.st- **Type**: @Compt.- **Package**: `com.t`p- **Package\*\*: `cot
- **M?#- **Méthodes\*\*:
  - `findTenantUuidByCode(String)` : - `findTenantve  - `findTenantInfoByCode(St?co- **M?#- **Méthodes\*\*:
  - `findTenantUuidByCode(String)` : - `findTenantve  - `findTenantInfoByCode(St? - `f#   - `findTenantUuidByCo``- **Package**: `com.tchts- **Package**: `com.tchalanepl- **Type*# - **PactJ- **Package**: `com.tchts- **Package**: `com.tchalanepl- **Type*# - **Package**: `com.tchts- **Package**: c? **Rôle**: Info bootsno- **Package**: `com.t`p- **Package**: `com.tchalanet.st- **Type**: @Compt.- **Package**: `com.t`p- **Package**: `cot
- **M?#- **Méthodes\*\*:
  - `findTenantUuidByCode(String)` : - `findTenantve  - `findTenantInfoByCode(St?co- **M?#- re- **M?#- **Méthodes**:
  - `findTenantUuidByCode(String)` : - `findTenantve  - `findTenantInfoByCode(St?co- **M?#- **Méthodes\*\*:
  - `findTenantUuidByCode(String)` : - `findTenantve  - `findTenantInfoByCode(St? - `f#   - `findTenantUuidByCo``- \*\*Packcat << 'EOF'

# ✅ Formatage du fichier timezone.md terminé

## Corrections appliquées :

### Section 6 (Approved conversions)

- ✅ Ajout de `**Approved pattern**:` (avec gras et deux-points)
- ✅ Ajout du séparateur `---` après le bloc de code

### Section 7 (occurredAt resolution)

- ✅ Ajout du titre `##` (était juste du texte)
- ✅ Conversion des lignes en liste à puces Markdown :
  - `- external \`Instant\` present → use it`
  - `- \`(drawDate + drawTime + zone)\` → compute`
  - `- fallback → \`Instant.now(clock)\``
- ✅ Correction de la description multi-lignes en une seule ligne
- ✅ Ajout du séparateur `---`

### Section 8 (Batch & scheduler)

- ✅ Ajout du titre `##` (était juste du texte)
- ✅ Conversion des lignes en liste à puces Markdown :
  - `- Batch jobs use the same injected \`Clock\``
  - `- Tenant timezone loaded via bootstrap`
  - `- Calendar logic always receives an explicit \`ZoneId\``
- ✅ Ajout du séparateur `---`

### Section 9 (PR checklist)

- ✅ Déjà correctemen# ✅ Forma a## Corrections appliquées :

### Section 6 (Apba### Section 6 (Approved conu? ✅ Ajout de `**Approved pattern*ic- ✅ Ajout du séparateur `---` après le bloc de code

### Sectre### Section 7 (occurredAt resolution)

- ✅ Ajout du t--- ✅ Ajout du titre `##` (était ju??- ✅ Conversion des lignes en liste à puces Ma ? - `- external \`Instant\` present → use it`
  - `- et  - `- \`(drawDate + drawTime + zone)\` → co ? - `- fallback → \`Instant.now(clock)\``
- ✅ Cs - ✅ Correction de la description multi-l (- ✅ Ajout du séparateur `---`

### Section 8 (Batch & schedulerna### Section 8 (Batch & schedulex - ✅ Ajout du titre cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && wc -l docs/conventions/timezone.md && head -200 docs/conventions/timezone.md | tail -20

cat << 'EOF'

# ✅ Formatage du fichier timezone.md - Résumé

## Corrections appliquées avec succès :

### Section 6 (Approved conversion patterns)

- ✅ Section 6.1 : Ajout de `**Approved**:` et séparateur `---`
- ✅ Section 6.2 : Ajout de titre `###`, bloc de code Java, séparateur
- ✅ Section 6.3 : Conversion en liste à puces avec code inline

### Section 7 (OccurredAtResolver)

- ✅ Conversion des lignes en liste à puces avec code inline
- ✅ Liste des garanties en puces
- ✅ Séparateur `---` ajouté

### Section 9 (Timezone rules)

- ✅ Sous-sections 9.1, 9.2, 9.3 avec titre `###`
- ✅ Listes à puces avec code inline
- ✅ Séparateurs `---` entre les sous-sections

### Section 10 (PR checklist)

- ✅ Déjà correcte avec checkboxes Markdown `- [ ]`

## ⚠️ Section 8 (Scheduler helpers)

La section 8 n'a pas pu être complètement corrigée en raison de problèmes de correspondance de texte.
Elle nécessite encore un formatage manuel pour :

- Ajouter `###` aux titres 8.1 et 8.2
- Convertir les# ✅ Formate## Corrections appliquées avec succès :

### See ### Section 6 (Approved conversion patte`,- ✅ Section 6.1 : Ajout de `**Approved**:or- ✅ Section 6.2 : Ajout de titre `###`, bloc de code Java, sé? ✅ Section 6.3 : Conversion en liste à puces avec code inline

### Sso### Section 7 (OccurredAtResolver)

- ✅ Conversion des lignes e h- ✅ Conversion des lignes en liTi- ✅ Liste des garanties en puces
- ✅ Séparateur `---` ajhi- ✅ Séparateur `---` ajouté

# a### Section 9 (Timezone rulecat << 'EOF'

# ✅ Formatage du fichier batch.md terminé

## Corrections appliquées :

### Section 3 (Allowlist & catalog)

- ✅ Ajout de `### MUST NOT` avec titre correct
- ✅ Conversion des lignes en listes à puces `-`
- ✅ Ajout du séparateur `---`

### Section 4 (Endpoint contract)

- ✅ Ajout des titres `## 4.` et `### 4.1`, `### 4.2`
- ✅ Bloc de code pour la route avec triple backticks
- ✅ Bloc de code JSON pour le body avec `json` syntax
- ✅ Ajout de `### MUST` et `### MUST NOT` avec titres
- ✅ Conversion en listes à puces avec code inline
- ✅ Ajout du séparateur `---`

### Section 5 (Validation rules)

- ✅ Ajout du titre `## 5.`
- ✅ Sous-sections `### 5.1`, `### 5.2`, `### 5.3`, `### 5.4`
- ✅ Conversion en listes à puces avec flèches `→`
- ✅ Bloc de code Java pour l'exemple BatchGate
- ✅ Code inline avec backticks pour tous les paramètres
- ✅ Ajout du séparateur `---`

### Section 6 (Standard JobParameters)

- ✅ Ajout du titre `## 6.`
- ✅ Sous-section `### MUST include# ✅ Formale## Corrections appliquées :

### Section 3 `### Section 3 (Allowlist & ou- ✅ Ajout de `### MUST NOT`avecon- ✅ Conversion des lignes en listes à puces  s- ✅ Ajout du séparateur`---`

### Section 4 (E

-### Section 4 (Endpoint contrac??- ✅ Ajout des titres `## 4.` e# - ✅ Bloc de code pour la route avec triple backtick? - ✅ Bloc de code JSON pour le body avec `json` syntyl- ✅ Ajout de `### MUST` et `### MUST NOT` avec titreur- ✅ Conversion en listes à puces avec code inline
-\*`- ✅ Ajout du séparateur `---`

### Section 5 (Val?p### Section 5 (Validation rulescu- ✅ Ajout du titre `## 5.`

- tr- ✅ Sous-sections `### 5.ns- ✅ Conversion en listes à puces avec flèches `→`
- ✅-l- ✅ Bloc de code Java pour l'exemple BatchGate
- ✅if- ✅ Code inline avec backticks pour tous les io- ✅ Ajout du séparateur `---`

### Section 6 (Standard tr### Section 6 (Standard JobPara`#- ✅ Ajout du titre `## 6.`

- ✅ Soes- ✅ Sous-section `### MUSin### Section 3  `### Section 3 (Allowlist & ou- ✅ Ajout de `### MUST NOT` adu### Section 4 (E
  -### Section 4 (Endpoint contrac??- ✅ Ajout des titres `## 4.` e# - ✅ Bloc de code pour la route avec triple backtick? - ✅ Bloc de code JSde-### Section 4 am-\*`- ✅ Ajout du séparateur `---`

### Section 5 (Val?p### Section 5 (Validation rulescu- ✅ Ajout du titre `## 5.`

- tr- ✅ Sous-sections `### 5.ns- ✅ Conversion en listes à puces avec flèches `→`
- ✅-l- ✅ Bloc de code Java pour l'exemple BatchGate
- ✅if- ✅ Code inlien### Section 5 (Val?p### Section 5 c - tr- ✅ Sous-sections `### 5.ns- ✅ Conversion en listes à puces avec flèchax- ✅-l- ✅ Bloc de code Java pour l'exemple BatchGate
- ✅if- ✅ Code inline avec bec- ✅if- ✅ Code inline avec backticks pour tous les ta### Section 6 (Standard tr### Section 6 (Standard JobPara`#- ✅ Ajout du titre `## 6.`na- ✅ Soes- ✅ Sous-section `### MUSin### Secat << 'EOF'

# ✅ Formatage du fichier batch.md terminé

## Corrections appliquées :

### Section 1 (BatchGate)

- ✅ Ajout de `### Mandatory usage` avec titre correct
- ✅ Ajout de `### Forbidden` avec titre correct
- ✅ Conversion des lignes en listes à puces `-`
- ✅ Code inline avec backticks pour tous les noms de méthodes
- ✅ Ajout du séparateur `---`

### Section 2 (Ops Controller)

- ✅ Ajout de `### Role`, `### What it does NOT do`, `### Mandatory flow`
- ✅ Conversion en listes à puces avec code inline
- ✅ Bloc de code `text` pour le flow
- ✅ Ajout du séparateur `---`

### Section 3 (Scheduler layer)

- ✅ Ajout de `### Definition` et `### Responsibilities`
- ✅ Conversion en listes à puces
- ✅ Gras pour **Schedulers MUST** et **Schedulers MUST NOT**
- ✅ Code inline pour tous les noms de classes
- ✅ Ajout du séparateur `---`

### Section 4.1 (Cron schedulers)

- ✅ Conversion en `### 4.1` avec titre correct
- ✅ Ajout de **Example**, **Used when**, **Rules** en gras
- ✅ Bloc # ✅ Formaa ## Corrections appliquées :

### Section 1 ? ### Section 1 (BatchGate)

-at- ✅ Ajout de `### Mand2 - ✅ Ajout de `### Forbidden` avec titre correct

- ?t- ✅ Conversion des lignes en listes à puces `he- ✅ Code inline avec backticks pour tous les no* - ✅ Ajout du séparateur `---`

### Section 2 (Ops Controller)si### Section 2 (Ops Controller)

ut- ✅ Ajout de `### Role`, `#ti- ✅ Conversion en listes à puces avec code inline

- ✅ Bloc de code ``,- ✅ Bloc de code `text` pour le flow
- ✅ Ajout c - ✅ Ajout du séparateur `---`

### r ### Section 3 (Scheduler layer)op- ✅ Ajout de `### Definitionhy- ✅ Conversion en listes à puces

- ✅ Gras pour **Scdo- ✅ Gras pour **Schedulers MUST\* c- ✅ Code inline pour tous les noms de classes
- ✅ Ajout d
  -- ✅ Ajout du séparateur `---`

### Section 4

-### Section 4.1 (Cron scheduler
#- ✅ Conversion en `### 4.1` av/W- ✅ Ajout de **Example**, **Used when**, \*\*Ruhe- ✅ Bloc # ✅ Formaa ## Corrections appliquées :

### Sees### Section 1 ? ### Section 1 (BatchGate)

-at- ✅ ##-at- ✅ Ajout de `### Mand2 - ✅ Ajoutes- ?t- ✅ Conversion des lignes en listes à puces `he- ✅ Code inline aveex### Section 2 (Ops Controller)si### Section 2 (Ops Controller)
ut- ✅ Ajout de `### Role`, `#ti- ✅ Conversion en listes à puces avec c?t- ✅ Ajout de `### Role`, `#ti- ✅ Conversion en listes ?t- ✅ Bloc de code ``,- ✅ Bloc de code `text` pour le flow

- ✅ Ajout c - ✅ Ajoe - ✅ Ajout c - ✅ Ajout du séparateur `---`

### r ### Sero### r ### Section 3 (Scheduler layer)op- ✅ ve- ✅ Gras pour **Scdo- ✅ Gras pour **Schedulers MUST\* c- ✅ Code inline pour tous les noms de classes (- ✅ Ajout d

-- ✅ Ajout du séparateur `---`

### Section 4

-### Section 4.1 (Cron scheduler
#- ✅ Co?- ✅ Ajoutod### Section 4
-### Section 4.1 (av-### Section- #- ✅ Conversion en `### 4.1` mp### Sees### Section 1 ? ### Section 1 (BatchGate)
-at- ✅ ##-at- ✅ Ajout de `### Mand2 - ✅ Ajoutes- ?t- ✅ Conversion des ligne 1-at- ✅ ##-at- ✅ Ajout de `### Mand2 -cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/common/batch/infra/web/BatchJobOpsController.java && echo "✅ Supprimé: BatchJobOpsController.java"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/common/batch/infra/web/LaunchJobRequest.java && echo "✅ Supprimé: LaunchJobRequest.java (ancien)"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/core/settings/infra/web/ops/BatchAdminOpsController.java && echo "✅ Supprimé: BatchAdminOpsController.java (migré)"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/core/settings/infra/web/ops/BatchGateFlagsReadController.java && echo "✅ Supprimé: BatchGateFlagsReadController.java (migré)"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/common/batch/JobParamReader.java && echo "✅ Supprimé: ancien JobParamReader (déplacé vers params/)"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/common/batch/BatchDisabledException.java && echo "✅ Supprimé: ancien BatchDisabledException (déplacé vers gate/)"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/common/batch/infra/context/BatchTchContextBinder.java && echo "✅ Supprimé: ancien BatchTchContextBinder (déplacé vers context/)"
cd /Users/bhebb/Documents/projets/tchalanet/tchalanet-server && rm -f src/main/java/com/tchalanet/server/common/batch/infra/listener/BatchTchContextJobListener.java && echo "✅ Supprimé: ancien listener (déplacé vers context/)"
cat << 'EOF'

# ✅ MIGRATION BATCH ARCHITECTURE — TERMINÉE

## 📊 Statistiques

**Fichiers créés** : 23 classes Java

- common.batch.\*: 14 classes
- features.ops.\*: 9 classes (5 DTOs + 1 service + 3 controllers)
  **Fichiers supprimés** : 8 anciens fichiers migrés
  **Documentation** : 1 fichier IMPLEMENTATION_DONE.md

---

## ✅ Structure finale conforme

### common/batch/ (technique pur, 0 HTTP)

```
key/        → JobKey (validation)
registry/   → RegisteredJob, JobRegistry (allowlist)
gate/       → BatchGate, Resolver, Cache, Exception
params/     → Keys, Reader, Validator
context/    → Binder, Listener (TENANT jobs)
launch/     → BatchJobStarter (orchestration)
```

### features/ops/ (HTTP surface)

```
dto/           → 5 records (Request/Response)
application/   → OpsBatchService (orchestration)
infra/web/     → 3 controllers (Jobs, Gate, Executions)
```

---

## 🎯 Endpoints créés

### Jobs Management

- GET /api/v1/platform/ops/batch/jobs
- GET /api/v1/platform/ops/batch/jobs/{jobK# ✅ MIGRA ## 📊 Statistiques
  **Fichiers créés** : 23 ##\*_Fichiers créés_
  -- common.batch.\*: 14 classes
- featurte- features.ops.\*: 9 classesGE**Fichiers supprimés** : 8 anciens fichiers migrés
  **DocumentPU**Documentation\*\* : 1 fichier IMPLEMENTATION_DONE.mEx---

## ✅ Structure finale conforme

### common/bat/e##cu### common/batch/ (technique pupi```

key/ → JobKey (validation)
y=ke.
registry/ → RegisteredJob, Job
?ate/ → BatchGate, Resolver, Cache, Exception? params/ → Keys, Reader, Validator
context/ upcontext/ → Binder, Listener (TENAlllaunch/ → BatchJobStarter (orchestratiote```

### features/ops/ (HTTP surface)

````
dto/ ? ##bP```
dto/           → 5 recordamdtJoapplication/   → OpsBatchService (orchestratepinfra/web/     → 3 controllers (Jobs, Gate, Exe
?``
---
## 🎯 Endpoints créés
### Jobs Management
- ch--nt##tB### Jobs Management
- GEtJ- GET    /api/v1/pch- GET    /api/v1/platform/ops/batch/jobts**Fichiers créés** : 23 ##**Fichiers créés*
-- common.batch.*: 14 classes
ra-- common.batch.*: 14 classes
- featurte- featat- featurte- features.ops.*: nc**DocumentPU**Documentation** : 1 fichier IMPLEMENTATION_DONE.mEx---
## ✅ Structure finahe## ✅ Structure finale conforme
### common/bat/e##cu### common/batst### common/bat/e##cu### common/ankey/        → JobKey (validation)
y=ke.
registry/   ?y=ke.
registry/   → RegisteredJo
4regiTe?ate/       → BatchGate, Resolctcontext/    upcontext/    → Binder, Listener (TENAlllaunch/     → BatchJobStarter (orchestis### features/ops/ (HTTP surface)
````

dto/ ? ##bP`dto/           → 5 recordamdtJoapplication/   →e`
dto/ ? ##bP``
dto/        ordtt?to/         ???`

---

## 🎯 Endpoints créés

### Jobs Management

- ch--nt##tB### Jobs Management
- GEtJ- GET /api/v1/pch- GET /api/v1t ---Ba##hS### Jobs Management
- chti- ch--nt##tB### Jo:c- GEtJ- GET /api/v1/pch- GEtr-- common.batch._: 14 classes
  ra-- common.batch._: 14 classes
- featurte- featat- featurte- features.ops._: nc\*\*Dnkra-- common.batch._: 14 claser- featurte- featat- featurte- St## ✅ Structure finahe## ✅ Structure finale conforme

### common/bat/e##cu### common/batst### common/bat/e##cu###ir### common/bat/e##cu### common/batst### common/bat/e##OMy=ke.

registry/ ?y=ke.
registry/ ?cat << 'EOF' > /tmp/batch_files_created.txt

# FICHIERS CRÉÉS - BATCH ARCHITECTURE

## common/batch/key/

✅ JobKey.java

## common/batch/registry/

✅ RegisteredJob.java
✅ JobRegistry.java

## common/batch/gate/

✅ BatchGate.java (refactoré)
✅ BatchGateResolver.java
✅ BatchGateCache.java (interface)
✅ BatchGateCacheImpl.java
✅ BatchDisabledException.java

## common/batch/params/

✅ BatchParamKeys.java
✅ JobParamReader.java
✅ JobParamsValidator.java

## common/batch/context/

✅ BatchTchContextBinder.java
✅ BatchJobExecutionListener.java

## common/batch/launch/

✅ BatchJobStarter.java

## features/ops/dto/

✅ StartJobRequest.java
✅ StartJobResponse.java
✅ GateUpdateRequest.java
✅ JobInfoResponse.java
✅ ExecutionResponse.java

## features/ops/application/

✅ OpsBatchService.java

## features/ops/infra/web/

✅ OpsBatchJobController.java
✅ OpsBatchGateController.java
✅ OpsBatchExecutionController.java

## docs/batch/

## ✅ IMPLEMENTATION_DONE.md

TOTAL: 23 classes Java + 1 doc
