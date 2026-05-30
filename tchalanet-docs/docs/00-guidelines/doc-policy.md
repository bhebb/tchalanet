# Politique documentaire Tchalanet

**Version**: 2.0.0 | **Date**: 2026-05-30

---

## Objectif

1. **Trouver vite** la bonne doc (IA / dev / business) — un endroit par type d'information
2. **Éviter la duplication** — une source de vérité par type d'info, rien en triple
3. **Garder la doc à jour** — mise à jour en même commit que le code qui change la règle
4. **Avoir un workflow SDD rapide** — OpenSpec comme atelier temporaire, pas comme source normative

---

## Hiérarchie de vérité documentaire

En cas de conflit entre deux sources, la priorité est :

```
1. Code livré + tests verts       (état réel — pas "code idéal")
2. ADR accepté                     (intention architecturale)
3. <projet>/docs/ARCHITECTURE.md
4. <projet>/docs/conventions/*
5. src/**/DOMAIN_*.md              (invariants métier locaux)
6. tchalanet-docs/docs/*           (portail cross-project)
7. openspec/context/*              (router IA)
8. openspec/changes/*              (atelier WIP — non normatif)
```

> Si le code contredit une règle normative → c'est une **dette explicite** à tracker, pas une vérité à accepter.

---

## Où vit chaque type de document

| Type | Emplacement | Cycle de vie | Mise à jour |
|---|---|---|---|
| Normes, guidelines stables | `tchalanet-docs/docs/00-guidelines/` | Permanent | Rare (décision d'équipe) |
| Architecture maps | `tchalanet-docs/docs/01-architecture/` | Permanent | Refactors structurants |
| Domaines métier (vue fonctionnelle) | `tchalanet-docs/docs/02-functional/domains/` | Permanent | Évolutions domaine |
| Flows métier observables | `tchalanet-docs/docs/02-functional/flows/` | Permanent | Évolutions flux |
| ADR | `tchalanet-docs/docs/03-adr/` | Immutable | Nouvelles décisions uniquement |
| Liens vers composants | `tchalanet-docs/docs/99-links/` | Permanent | Suivi des moves |
| Architecture composant | `<projet>/docs/ARCHITECTURE.md` | Permanent | Refactors structurants |
| Conventions techniques | `<projet>/docs/conventions/` | Permanent | **Même commit que le code** |
| Règles métier domaine | `src/**/DOMAIN_*.md` | Permanent | Même commit que le domaine |
| Context packs IA | `openspec/context/` | Permanent, léger | Quand convention change |
| Specs features WIP | `openspec/changes/<id>/` | **Temporaire** | Quotidien pendant la feature |

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

**Contenu autorisé :**
- vocabulaire métier du domaine
- états et transitions
- invariants métier
- événements publiés/consommés
- décisions métier locales
- diagrammes simples si utiles

**Ne contient pas :**
- conventions Java/Spring générales → `docs/conventions/`
- détails JPA/Flyway génériques → `docs/conventions/`
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

**Ne copie pas les règles des docs.** Exemple :

```markdown
# backend-core-context
Lire dans cet ordre :
1. tchalanet-server/docs/ARCHITECTURE.md
2. tchalanet-server/docs/conventions/command_query_handlers.md
3. tchalanet-server/docs/conventions/typed_ids.md
```

---

## Documentation Ownership par projet

Chaque projet own sa propre documentation technique :

| Projet | ARCHITECTURE.md | conventions/ |
|---|---|---|
| `tchalanet-server` | architecture Java/Spring | CQRS, persistence, RLS, batch, cache, typed IDs... |
| `tchalanet-web` | architecture Angular/Nx | NgRx, theming, page model, i18n... |
| `tchalanet-mobile` | architecture Flutter/Ionic | secure storage, offline sync, terminal binding... |
| `tchalanet-infra` | architecture infra | déploiement, secrets, réseau, volumes... |
| `tchalanet-edge-service` | architecture edge | proxy rules, auth flow... |

`tchalanet-docs` = portail cross-project (comportements observables, maps système, ADRs, guidelines partagées, liens).

**Règles critiques :**
- Les règles web ne vivent pas dans `tchalanet-server`
- Les règles mobile ne vivent pas dans `tchalanet-server`
- Les principes partagés frontend/mobile peuvent vivre dans `tchalanet-docs/docs/00-guidelines/`

---

## Cycle de vie OpenSpec

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

**Pas d'archive.** Suppression après preuve d'extraction, pas avant.
Les changes actives ne sont jamais nettoyées par une passe documentaire générale —
elles sont nettoyées uniquement au moment de leur livraison.

Voir `tchalanet-docs/docs/06-openspec/archive-policy.md` pour les règles complètes.

---

## Workflow de modification d'une règle

> Quand une règle technique ou métier change, suivre cette cascade dans l'ordre.

### Étape 1 — Modifier la source canonique near-code

C'est **toujours** le premier fichier à modifier.

| Type de règle | Fichier à modifier en premier |
|---|---|
| Architecture backend (couches, dépendances) | `tchalanet-server/docs/ARCHITECTURE.md` |
| Typed IDs | `tchalanet-server/docs/conventions/typed_ids.md` |
| RLS / multi-tenant | `tchalanet-server/docs/conventions/persistence/rls.md` |
| Events / effets de bord | `tchalanet-server/docs/conventions/event_model.md` |
| Tests | `tchalanet-server/docs/conventions/testing.md` |
| Persistence / Flyway | `tchalanet-server/docs/conventions/persistence/persistence.md` |
| Frontend Angular | `tchalanet-web/docs/conventions/` |
| Infra / Docker | `tchalanet-infra/docs/conventions/` |
| Edge service | `tchalanet-edge-service/docs/conventions/` |
| Domaine métier | `tchalanet-server/src/**/DOMAIN_*.md` |

### Étape 2 — Mettre à jour le skill IA correspondant

Après avoir modifié la convention near-code, mettre à jour le skill `.agents/skills/*/SKILL.md` correspondant.

> ⚠️ Ne jamais modifier un skill sans avoir d'abord modifié la convention source.

| Convention modifiée | Skill à mettre à jour |
|---|---|
| `ARCHITECTURE.md` | `.agents/skills/backend-architecture/SKILL.md` |
| `typed_ids.md` | `.agents/skills/backend-typed-ids/SKILL.md` |
| `rls.md` | `.agents/skills/backend-rls/SKILL.md` |
| `event_model.md` | `.agents/skills/backend-events/SKILL.md` |
| `testing.md` | `.agents/skills/backend-testing/SKILL.md` |
| `persistence.md` | `.agents/skills/backend-persistence/SKILL.md` |

### Étape 3 — Mettre à jour la doc fonctionnelle (si impact métier)

Seulement si la règle a un impact visible côté business ou architecture globale :

- Domaine ou workflow → `tchalanet-docs/docs/02-functional/`
- Décision structurante → `tchalanet-docs/docs/03-adr/` (ADR obligatoire)
- Architecture map → `tchalanet-docs/docs/01-architecture/`

> ❌ Ne jamais commencer par la doc fonctionnelle.
> ✅ Toujours commencer par la source near-code.

### Résumé visuel

```
1. near-code (convention source)
        ↓
2. .agents/skills/ (résumé IA)
        ↓
3. tchalanet-docs/docs/ (si impact métier ou archi)
```

---

## Validation automatique (`pnpm docs:check`)

À lancer avant chaque PR :

**Erreurs (bloquantes) :**
- Liens Markdown cassés
- `openspec/changes/archive/` présent
- Context pack référencé dans `00-index.md` mais fichier absent
- Référence `.claude/skills/` (doit être `.agents/skills/`)

**Avertissements (revue humaine) :**
- Convention potentiellement impactée par les fichiers modifiés
- DOMAIN_*.md potentiellement impacté
- Flow fonctionnel potentiellement impacté

Les warnings nécessitent une revue humaine, pas de fix automatique.

---

## Checklist PR — Documentation impact

```markdown
## Documentation impact
- [ ] J'ai changé une règle technique → convention doc mise à jour (même commit)
- [ ] J'ai changé une règle métier → DOMAIN_*.md revu
- [ ] J'ai changé un workflow observable → flow doc revu
- [ ] J'ai livré une change OpenSpec → extraction complétée et change supprimée
- [ ] J'ai lancé `pnpm docs:check`
```

---

## Règles de placement (décision rapide)

| Question | Destination |
|---|---|
| C'est stable et partagé entre projets ? | `tchalanet-docs/docs/00-guidelines/` |
| C'est une architecture map ? | `tchalanet-docs/docs/01-architecture/` |
| C'est du métier (domaine ou workflow) ? | `tchalanet-docs/docs/02-functional/` |
| C'est une décision technique structurante ? | `tchalanet-docs/docs/03-adr/` |
| C'est technique spécifique à un projet ? | `<projet>/docs/conventions/` |
| C'est une règle métier locale d'un domaine ? | `src/**/DOMAIN_*.md` |
| C'est une spec en cours ? | `openspec/changes/<change-id>/` |

---

**Maintenu par** : équipe Tchalanet  
**Dernière mise à jour** : 2026-05-30
