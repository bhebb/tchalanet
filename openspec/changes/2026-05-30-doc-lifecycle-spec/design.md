# Design — Lifecycle documentaire Tchalanet

Ce fichier contient les décisions de design et les specs détaillées pour les 3 phases.  
Les tâches actives (Phase 1) sont dans `tasks.md`.

---

## Hiérarchie de vérité documentaire

En cas de conflit entre deux sources :

```
1. Code livré + tests verts       (état réel — pas "code idéal")
2. ADR accepté                     (intention architecturale)
3. <projet>/docs/ARCHITECTURE.md
4. <projet>/docs/conventions/*
5. src/**/DOMAIN_*.md              (invariants métier du domaine)
6. tchalanet-docs/docs/*           (portail cross-project)
7. openspec/context/*              (router IA)
8. openspec/changes/*              (atelier WIP — non normatif)
```

> Si le code contredit une règle normative → c'est une **dette explicite** à tracker, pas une vérité à accepter.

---

## Règles par type de document

### `tchalanet-docs/docs/` — portail fonctionnel

Contient des descriptions fonctionnelles stables lisibles sans lire le code.  
Ne copie pas les règles techniques détaillées ni les règles métier exhaustives des domaines.

```
✅ "Un ticket peut être vérifié publiquement par code public/QR."
✅ "Le payout terrain passe par une vérification POS avant exécution."
❌ Recopier les classes, handlers, tables, packages, statuts internes.
```

Quand un détail appartient à un projet ou domaine, `tchalanet-docs` **pointe** vers la source canonique.

### `<projet>/docs/conventions/` — comment faire

Un fichier par sujet technique. Mis à jour **en même commit** que le code qui change la règle.  
Si une règle s'applique à plusieurs projets → elle monte vers `tchalanet-docs/docs/00-guidelines/`.

### `src/**/DOMAIN_*.md` — règles métier locales

Contenu autorisé :
- vocabulaire métier du domaine
- états et transitions
- invariants métier
- événements publiés/consommés
- décisions métier locales
- diagrammes simples si utiles

Ne contient pas :
- conventions Java/Spring générales (→ `docs/conventions/`)
- détails JPA/Flyway génériques (→ `docs/conventions/`)
- règles déjà dans `docs/conventions/*`
- historique long de discussions

Si une règle s'applique à plusieurs domaines → elle monte vers `<projet>/docs/conventions/`.

### `openspec/context/` — router IA, pas résumé

Chaque pack contient uniquement :
- objectif du pack
- fichiers canoniques à lire (chemins exacts)
- ordre de lecture recommandé
- exclusions / pièges connus
- liens vers specs actives si nécessaire

Ne copie pas les règles des docs. Exemple :

```markdown
# backend-core-context
Lire dans cet ordre :
1. tchalanet-server/docs/ARCHITECTURE.md
2. tchalanet-server/docs/conventions/command_query_handlers.md
3. tchalanet-server/docs/conventions/typed_ids.md
```

---

## Lifecycle d'une spec OpenSpec

```
openspec/changes/<id>/     ← atelier WIP pendant la feature
        │
        ▼ feature livrée → checklist d'extraction :
        ├─ [ ] décisions durables extraites vers leur destination
        ├─ [ ] conventions mises à jour si nécessaire
        ├─ [ ] ADR créé si décision architecturale
        ├─ [ ] context pack IA mis à jour si nécessaire
        ├─ [ ] rien de durable restant dans la change
        └─ [ ] supprimer openspec/changes/<id>/
```

Pas d'archive. Suppression après preuve d'extraction, pas avant.  
Changes actives → nettoyées uniquement au moment de leur livraison, jamais par une passe générale.

---

## Documentation Ownership par projet

Chaque projet own sa propre documentation technique :

| Projet | ARCHITECTURE.md | conventions/ |
|---|---|---|
| `tchalanet-server` | architecture server | Java, Spring, CQRS, persistence, RLS, batch, cache... |
| `tchalanet-web` | architecture Angular/Nx | NgRx, theming, page model, i18n... |
| `tchalanet-mobile` | architecture Flutter | secure storage, offline sync, terminal binding... |
| `tchalanet-infra` | architecture infra | déploiement, secrets, réseau, volumes... |
| `tchalanet-edge-service` | architecture edge | proxy rules, auth flow... |

`tchalanet-docs` = portail cross-project (comportements observables, maps système, ADRs, guidelines partagées, liens vers docs des projets).

**Règles critiques :**
- Les règles web ne vivent pas dans `tchalanet-server`
- Les principes partagés frontend/mobile peuvent vivre dans `tchalanet-docs/docs/00-guidelines/`

---

## Checklist PR — Documentation impact

À ajouter dans les PR templates :

```markdown
## Documentation impact
- [ ] J'ai changé une règle technique → convention doc mise à jour
- [ ] J'ai changé une règle métier → DOMAIN_*.md revu
- [ ] J'ai changé un workflow observable → flow doc revu
- [ ] J'ai livré une change OpenSpec → extraction complétée et change supprimée
- [ ] J'ai lancé `pnpm docs:check`
```

**Severity de `pnpm docs:check` :**
- Erreurs (bloquantes) : liens cassés, `openspec/changes/archive/` présent, context pack référencé mais absent
- Warnings (revue humaine) : convention potentiellement impactée, DOMAIN potentiellement impacté, flow potentiellement impacté

---

## Phase 2 — OpenSpec archive extraction

**Dépend de :** Phase 1 complétée.  
**Durée estimée :** 2-3 jours. **Risque :** Moyen.

### Archives à traiter

**`archive/2026-05-05-audit-project-docs-and-mkdocs`**  
Contenu probable : audit documentaire (cartographie, orphelins, packs manquants).  
Éléments durables attendus : canonical ownership map → `doc-policy.md`. Tâches complétées → DELETE.

**`archive/2026-05-07-organize-ai-agent-files`**  
Contenu probable : organisation .agents/ .claude/.  
Éléments durables : tout déjà dans `AGENTS.md` + `archive-policy.md`. → DELETE direct après confirmation.

### Processus pour chaque archive

1. Lire entièrement
2. Classer chaque élément : norme stable | ADR | context pack IA | rien de durable
3. Proposer matrice d'extraction (destination exacte)
4. **Attendre validation Stevens**
5. Appliquer les extractions
6. Supprimer l'archive après checklist

### Règles d'extraction

- Norme stable → `tchalanet-docs/docs/00-guidelines/` (si transverse) ou `<projet>/docs/conventions/`
- ADR → `tchalanet-docs/docs/03-adr/` (décision architecturale avec why/alternatives/conséquences)
- Context pack IA → `openspec/context/` (router uniquement, pas copie de règles)
- Rien de durable → DELETE sans extraction

---

## Phase 3 — Full audits + prévention

**Dépend de :** Phase 1 + Phase 2 complétées.  
**Durée estimée :** 2-3 semaines. **Risque :** Haut.

### Processus par composant

1. **Audit** : lister ce qui existe, classifier (à jour | obsolète | manquant)
2. **Validation** : proposer matrice à Stevens, attendre approbation
3. **Exécution** : créer/modifier/supprimer selon plan validé
4. **Vérification** : `pnpm docs:check`, valider liens et policies

### tchalanet-server *(priorité haute — dette existante)*

- Vérifier `docs/ARCHITECTURE.md` : tous les composants ? à jour ?
- Inventorier `docs/conventions/` : quels fichiers, lesquels manquent (batch, cache, persistence, RLS, etc.)
- Inventorier `src/**/DOMAIN_*.md` : existence, cohérence de surface
- Produire matrice : créer | mettre à jour | supprimer

### tchalanet-infra *(priorité haute — dette existante)*

- Analyser tous les docs dans `tchalanet-infra/docs/`
- Évaluer : utile pour créer/modifier un module infra ? (oui/non/obsolète)
- Identifier conventions manquantes (déploiement, secrets, réseau, volumes)
- Produire liste : garder | mettre à jour | supprimer | créer

### tchalanet-web / mobile / edge *(priorité moyenne — prévention)*

Pour chacun :
- Lister docs existants
- Vérifier cohérence surface avec le code
- Identifier conventions manquantes
- Établir le formalisme minimal **avant** que la dette s'accumule

### tchalanet-docs *(priorité moyenne — cleanup + prévention)*

- Dossiers orphelins (`02-domains/`, `03-apps/`, `05-decisions/`) : fusionner | rediriger | supprimer
- `99-reference/` : rapports générés → exclus de nav MkDocs ou suppression
- `02-functional/flows/` : identifier workflows métier clés manquants
