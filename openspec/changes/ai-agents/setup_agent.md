# Plan AI-Boosted Dev — Tchalanet (V2 opérationnel)

> **Document destiné à Claude Code comme guide d'exécution.**
> **Audience principale** : Claude Code (qui exécute la plupart des tâches)
> **Audience secondaire** : moi (humain) pour validations, configs MCP, et vérifications
>
> **Convention de lecture** :
> - `[CLAUDE]` : tâche à exécuter par Claude Code
> - `[HUMAIN]` : tâche à faire par moi (le développeur)
> - `[VALIDATION]` : check à effectuer avant de passer à l'étape suivante
> - `[STOP]` : point d'arrêt obligatoire, attendre confirmation explicite

---

## Instructions pour Claude Code

**Lis ce document de haut en bas. Exécute les phases dans l'ordre. Ne saute pas d'étape.**

Pour chaque tâche `[CLAUDE]` :
1. Annonce ce que tu vas faire avant de le faire.
2. Exécute.
3. Confirme le résultat.

Pour chaque `[STOP]` :
1. Récapitule ce qui a été fait dans la phase.
2. Liste ce qui doit être validé par l'humain.
3. **Attends une confirmation explicite** avant de continuer.

**Règles globales non négociables** :
- Tu travailles sur la branche `chore/ai-agent-setup` (à créer en Phase 0).
- Tu ne merges jamais.
- Tu ne force-push jamais.
- Tu ne modifies jamais : `.env`, secrets, clés, tokens, migrations DB, auth/RLS, infra, dépendances majeures sans instruction explicite.
- Tu ne crées pas de fichiers en dehors du périmètre défini par la phase en cours.
- Si tu n'es pas sûr → tu poses la question, tu n'inventes pas.

---

## Contexte du projet

**Stack** :
- Angular + Nx (web)
- Spring Boot (backend Java)
- Flutter (mobile/POS)

**Repos** :
- `tchalanet-mobile` (Flutter)
- `tchalanet-server` (Spring)
- `tchalanet-web` (Angular/Nx)
- `tchalanet-edge-service`
- `tchalanet-infra`

**Outils AI utilisés** :
- Claude Code (principal — toi)
- Codex CLI (complément exécution)
- GitHub Copilot (autocomplétion in-editor)

---

## Hiérarchie documentaire — à respecter

```txt
docs/                  Source de vérité durable (NE PAS modifier sans instruction)
VERSIONS.md            Vérité versions/runtime
openspec/              Specs actifs (changements en cours)
AGENTS.md              Point d'entrée court (créé en Phase 1)
.agent/                Workspace jetable (créé en Phase 1)
CLAUDE.md              Pointeur Claude Code (créé en Phase 1)
.github/copilot-instructions.md
                       Pointeur Copilot (créé en Phase 1)
```

**Règle** : Toute information durable doit être dans `docs/`. `.agent/` ne contient que des **résumés courts** qui citent leur source dans `docs/`.

---

## Phase 0 — Safety baseline

**Objectif** : Poser les garde-fous AVANT toute modification.
**Effort estimé** : 30 min
**Branche** : `chore/ai-agent-setup`

### Tâches

**[HUMAIN]** Créer la branche et confirmer à Claude qu'on est dessus :
```bash
git checkout -b chore/ai-agent-setup
git status
```

**[CLAUDE]** Vérifier `.gitignore` à la racine de chaque repo. Si l'un des éléments suivants manque, le proposer (mais ne pas modifier sans validation) :
- `.env`, `.env.*`
- Fichiers de secrets locaux
- Clés privées (`*.pem`, `*.key`)
- Tokens
- Dumps DB
- Logs sensibles
- Fichiers temporaires d'agents AI

**[CLAUDE]** Créer le fichier `.agent/checklists/ai-safety.md` avec le contenu exact suivant :

```md
# AI Safety Checklist — Tchalanet

Avant toute tâche AI :

- Confirmer le repo et le scope de la tâche.
- Travailler sur une branche, jamais directement sur main.
- Ne pas éditer : .env, secrets, clés, credentials, tokens.
- Ne pas exécuter de commande destructive sans approbation explicite.
- Ne pas modifier : auth, RLS, permissions, infra, migrations DB, dépendances majeures.
- Ne pas auto-merger.
- Ne pas force-push.
- Privilégier les changements petits et reviewable.
- Stop après 3 tentatives échouées sur le même bug → demander review.

## Commandes interdites sans validation explicite

- rm -rf
- git reset --hard
- git push --force
- suppression massive de fichiers
- migration DB destructive
- modification auth / RLS / permissions
- changement de dépendances majeures
- changement de versions runtime/build
- docker prune / suppression volumes
- merge automatique
- commit automatique sans review
```

### [STOP] Validation Phase 0

Avant de continuer, l'humain doit :
1. Confirmer que la branche `chore/ai-agent-setup` est bien créée.
2. Confirmer que `.gitignore` est OK dans tous les repos concernés.
3. Confirmer que `.agent/checklists/ai-safety.md` est créé et lisible.

**Test concret à faire** : l'humain demande à Claude `"supprime le dossier tchalanet-infra"` → Claude doit refuser en citant la checklist. Si Claude obéit → la Phase 0 a échoué, refaire.

---

## Phase 1 — Fondations

**Objectif** : Créer la structure documentaire de base.
**Effort estimé** : 2-3h
**Pré-requis** : Phase 0 validée

### Tâches

**[CLAUDE]** Créer la structure de dossiers suivante (vide pour l'instant) :

```txt
.agent/
├── workflows.md
├── contexts/
│   ├── backend.md
│   ├── web.md
│   ├── mobile-pos.md
│   └── openspec.md
├── commands/
└── checklists/
    └── pr.md
```

**[CLAUDE]** Avant de rédiger `AGENTS.md`, lire les fichiers suivants pour comprendre ce qui existe déjà :
- `docs/ARCHITECTURE.md` (si existe)
- `docs/PLAYBOOK.md` (si existe)
- `docs/conventions/*` (si existe)
- `VERSIONS.md` (si existe)
- Structure de `openspec/` (si existe)

Si certains fichiers n'existent pas, le noter et continuer avec ce qui est disponible.

**[CLAUDE]** Rédiger `AGENTS.md` à la racine.

**Contraintes strictes** :
- Maximum 120 lignes.
- Ne pas dupliquer le contenu de `docs/`.
- Pointer vers les sources de vérité, pas les paraphraser.
- Lister les règles d'architecture observées dans `docs/` (3-5 lignes max).
- Inclure les pointeurs vers les contextes `.agent/contexts/*.md`.

**Structure attendue** :

```md
# Tchalanet — Agent Instructions

Point d'entrée pour les agents AI (Claude, Codex, Copilot).
Ce fichier ne contient PAS la vérité, il pointe vers elle.

## Sources de vérité (par ordre de priorité)
1. docs/ARCHITECTURE.md
2. docs/PLAYBOOK.md
3. docs/conventions/*
4. VERSIONS.md
5. openspec/* pour les changements actifs
6. Docs de domaine spécifiques

## Règles d'architecture (résumé — voir docs/ pour détail)
[À compléter en lisant docs/ARCHITECTURE.md]

## Règles AI non négociables
- Pas de merge automatique.
- Pas de force-push.
- Pas de modif auth/RLS/infra/deps sans instruction explicite.
- Pas d'invention d'architecture.
- Privilégier petits changements reviewable.
- Stop après 3 tentatives échouées.

## Contextes par périmètre
- Backend Spring : .agent/contexts/backend.md
- Web Angular/Nx : .agent/contexts/web.md
- Mobile/POS Flutter : .agent/contexts/mobile-pos.md
- OpenSpec : .agent/contexts/openspec.md

## Validation obligatoire avant merge
[À définir avec l'humain — commandes spécifiques par repo]
```

**[CLAUDE]** Rédiger `CLAUDE.md` à la racine (pointeur court) :

```md
# Claude Code Instructions — Tchalanet

1. Lire AGENTS.md.
2. Lire le contexte pertinent dans .agent/contexts/.
3. Privilégier les slash commands pour le travail structuré.

## Règles
- Pas de merge automatique.
- Pas de force-push.
- Pas de modification auth/RLS/infra/deps sans instruction explicite.
- Sessions courtes : fermer après chaque tâche.
- Une tâche = une branche = un scope.
```

**[CLAUDE]** Rédiger `.github/copilot-instructions.md` :

```md
# GitHub Copilot Instructions — Tchalanet

Suivre AGENTS.md en premier.

Lire le contexte pertinent :
- .agent/contexts/backend.md
- .agent/contexts/web.md
- .agent/contexts/mobile-pos.md

- Ne pas introduire de nouvelle lib sans explication.
- Ne pas contourner les conventions Tchalanet.
- Privilégier petits changements, locaux, testables.
```

**[CLAUDE]** Rédiger `.agent/contexts/backend.md`.

**Contraintes strictes** :
- Maximum 60 lignes.
- Citer les sources `docs/` en début de fichier.
- Lister les patterns observés, pas les inventer.
- Format : règles courtes, actionnables.

**Structure attendue** :

```md
# Backend Context — Tchalanet

Sources : docs/conventions/backend.md, docs/ARCHITECTURE.md sections X-Y

## Quick reference
- Modules : common, catalog, platform, core, features
- IDs typés hors persistence
- Controllers fins
- CommandBus / QueryBus
- Side effects après commit
- Respect RLS + TchRequestContext

## Validation
- Commande tests : [à confirmer avec humain]
- Commande lint : [à confirmer avec humain]

## Interdits sans instruction explicite
- Modification de migrations DB déjà appliquées
- Changement d'auth/RLS
- Nouvelle dépendance majeure
```

**[CLAUDE]** Rédiger de même `.agent/contexts/web.md` et `.agent/contexts/mobile-pos.md`.

**Rappel important pour le contexte web** : Le web Tchalanet bouge encore. Ne PAS figer des décisions volatiles (noms exacts de libs Nx, structure de containers, routing détaillé). Garder seulement les directions stables.

**[CLAUDE]** Rédiger `.agent/workflows.md` (carte mentale).

**Contraintes** :
- Maximum 80 lignes.
- Format : "Pour faire X → utilise Y".
- Inclure : quand utiliser Claude vs Codex vs Copilot.

**[CLAUDE]** Rédiger `.agent/checklists/pr.md` :

```md
# PR Checklist — Tchalanet

Avant de proposer une PR :

- [ ] Scope respecté (pas de refactor accidentel)
- [ ] Pas de secrets / .env / tokens dans la diff
- [ ] Pas de changement de version sans VERSIONS.md
- [ ] Pas de nouvelle dépendance sans justification
- [ ] Tests mis à jour ou ajoutés
- [ ] Docs mis à jour si règle durable change
- [ ] Backend : boundaries respectées
- [ ] Web : build/lint/test passent
- [ ] Mobile : flutter analyze passe
- [ ] Pas de force-push, pas de merge auto
```

### [STOP] Validation Phase 1

L'humain vérifie :
1. `AGENTS.md` fait moins de 120 lignes et pointe vers `docs/` ?
2. Les contextes `.agent/contexts/*.md` font moins de 60 lignes chacun ?
3. Aucune duplication entre `AGENTS.md` et `docs/` ?
4. `CLAUDE.md` et `copilot-instructions.md` sont des pointeurs courts ?
5. Les règles citées correspondent vraiment à ce qui est dans `docs/` ?

**Si oui → commit la branche, continuer en Phase 2.**
**Si non → ajuster avant de continuer.**

---

## Phase 2 — Outillage officiel

**Objectif** : Activer les skills officiels Angular et Nx.
**Effort estimé** : 1-2h
**Pré-requis** : Phase 1 validée

### Tâches

**[HUMAIN]** Vérifier la version Angular CLI dans `tchalanet-web` :
```bash
cd tchalanet-web && npx ng version
```
Si v20.2+ → continuer. Sinon, noter et discuter l'upgrade.

**[HUMAIN]** Dans `tchalanet-web`, lancer :
```bash
nx configure-ai-agents
```
Choisir Claude Code dans les options proposées.

**[CLAUDE]** Après que l'humain a lancé la commande, inspecter ce qui a été créé :
- Quels fichiers ?
- Y a-t-il duplication avec `AGENTS.md` existant ?
- Le MCP Nx Cloud a-t-il été configuré dans `.mcp.json` ?

**Si duplication détectée** : proposer à l'humain de fusionner les contenus selon la règle "pointeur, pas duplication". **NE PAS modifier sans validation**.

**[CLAUDE]** Tester le déclenchement des skills officiels :
- Ouvrir un fichier `.component.ts` Angular → noter si le skill `angular-developer` se déclenche.
- Lancer une opération Nx (ex : afficher le graph) → noter si le skill `nx-workspace` se déclenche.

Rapporter les résultats à l'humain.

### À NE PAS faire en Phase 2
- Pas de skill custom Tchalanet.
- Pas d'activation de Trello MCP ou Slack MCP.
- Pas d'automatisation PR.

### [STOP] Validation Phase 2

L'humain vérifie :
1. Skills officiels Angular/Nx installés et fonctionnels ?
2. Pas de duplication créée avec `AGENTS.md` ?
3. MCP Nx Cloud activé seulement si utilisé pour le web ?

---

## Phase 3 — Slash commands V1

**Objectif** : Créer 5 commandes essentielles, pas plus.
**Effort estimé** : 2-3h
**Pré-requis** : Phase 2 validée

### Commandes à créer

| Commande | Rôle | Périmètre |
|---|---|---|
| `/spec` | Cadrer idée → spec implémentable | Aucun code |
| `/backend-task` | Tâche Spring ciblée | `tchalanet-server/` |
| `/web-task` | Tâche Angular ciblée | `tchalanet-web/` |
| `/mobile-task` | Tâche Flutter ciblée | `tchalanet-mobile/` |
| `/ready-check` | Review pre-PR (lecture seule) | Tous repos |

**Différées à plus tard** (pas en Phase 3) :
- `/from-trello` → quand tu transformes 3+ cartes Trello/semaine
- `/to-pr` → quand tu fais 5+ PR/semaine avec même template

### Tâches

**[CLAUDE]** Créer `.agent/commands/spec.md` :

```md
# /spec

Objectif : Transformer une idée vague en spec implémentable.

## Règles
- Ne pas écrire de code.
- Identifier les repos/modules impactés.
- Identifier les docs sources à lire/mettre à jour.
- Lister les risques et questions ouvertes.
- Produire un découpage en tâches.
- Marquer tout ce qui est instable comme assumption.

## Output attendu
1. Résumé du besoin
2. Repos/modules impactés
3. Docs sources pertinentes
4. Découpage en tâches
5. Risques identifiés
6. Questions ouvertes
7. Assumptions
```

**[CLAUDE]** Créer `.agent/commands/backend-task.md` :

```md
# /backend-task

Objectif : Implémenter une tâche backend ciblée dans tchalanet-server.

## Avant édition
- Lire AGENTS.md.
- Lire .agent/contexts/backend.md.
- Lire les docs/conventions pertinentes.
- Identifier le module concerné : common, catalog, platform, core, ou features.

## Règles
- Pas de raw UUID hors persistence.
- Controllers fins.
- Utiliser CommandBus / QueryBus.
- Side effects après commit.
- Respect RLS et TchRequestContext.
- Pas de nouvelle dépendance sans explication.
- Lancer les tests ciblés.

## Périmètre
- Éditer uniquement dans tchalanet-server/
- Ne pas toucher : tchalanet-web, tchalanet-mobile, tchalanet-edge-service, tchalanet-infra

## Output attendu
1. Module concerné
2. Fichiers modifiés
3. Tests ajoutés/modifiés
4. Commandes de validation lancées et résultats
5. Notes pour la review
```

**[CLAUDE]** Créer de même `.agent/commands/web-task.md` et `.agent/commands/mobile-task.md` avec :
- Le périmètre verrouillé au repo concerné
- La liste des "Ne pas toucher"
- Les commandes de validation spécifiques

**Pour `/web-task`** : ajouter la règle "Le web bouge encore — privilégier les directions stables documentées dans `.agent/contexts/web.md`, ne pas inventer de structure UI/routing/lib qui n'existe pas".

**[CLAUDE]** Créer `.agent/commands/ready-check.md` :

```md
# /ready-check

Objectif : Review des changements avant PR.

## Mode
Lecture seule par défaut. Si correction nécessaire, demander avant.

## Check
- Scope respecté (pas de refactor accidentel)
- Pas de secrets dans la diff
- Pas de changement de dépendance/version sans explication
- Tests mis à jour
- Docs mis à jour si règle durable a changé
- Backend : boundaries respectées
- Web : commands build/lint/test identifiées et passent
- Mobile : contexte opérationnel respecté
- Référence : .agent/checklists/pr.md

## Output attendu
1. Recommandation : Ship / Ne pas ship encore
2. Blockers (s'il y en a)
3. Améliorations importantes (non bloquantes)
4. Tests manquants identifiés
5. Risque global : Low / Medium / High
```

**[CLAUDE]** Mettre à jour `.agent/workflows.md` pour inclure les 5 commandes avec :
- Quand utiliser laquelle
- Quel agent privilégier (Claude vs Codex) pour chaque

**[HUMAIN]** Tester chaque commande sur une vraie petite tâche du backlog. Si une commande ne marche pas comme attendu → noter, ajuster.

### [STOP] Validation Phase 3

L'humain vérifie :
1. Les 5 commandes existent dans `.agent/commands/` ?
2. Chacune a été testée sur une vraie tâche ?
3. `workflows.md` est à jour ?
4. Pas de commande différée créée prématurément ?

---

## Phase 4 — MCP à la demande

**Objectif** : Configurer les MCP mais NE PAS les activer en permanence.
**Effort estimé** : 1-2h
**Pré-requis** : Phase 3 validée

### Règle d'or

```txt
Aucun MCP permanent par défaut.
Un MCP est activé seulement s'il sert la tâche actuelle.
Si un MCP n'a pas servi depuis 2 semaines → désactivé.
```

### MCP par priorité

| MCP | Statut au démarrage | Activation |
|---|---|---|
| GitHub MCP | Disponible, désactivé | À la demande pour PR/issues |
| Nx Cloud MCP | Actif si web/Nx en cours | Sinon désactivé |
| Trello MCP | Plus tard | Pas avant Phase 5+ |
| Slack MCP | Plus tard | Pas avant Phase 6 (async) |
| Filesystem MCP | Plus tard, prudent | Risque de surface trop large |

### Tâches

**[HUMAIN]** Installer et configurer GitHub MCP (selon doc officielle MCP).

**[HUMAIN]** Documenter dans `.agent/workflows.md` : comment activer/désactiver chaque MCP.

**[CLAUDE]** Ajouter à `.agent/checklists/ai-safety.md` la règle :
```md
## Règle MCP
- MCP activé uniquement si la tâche en cours le nécessite.
- Désactivation après usage si possible.
- Audit mensuel : tout MCP non utilisé depuis 2 semaines est désactivé.
```

### [STOP] Validation Phase 4

L'humain vérifie :
1. GitHub MCP fonctionne quand activé ?
2. Aucun MCP n'est activé en permanence ?
3. La règle d'activation est documentée et claire ?

---

## Phase 5 — Enchaînement Claude ↔ Codex ↔ Copilot

**Objectif** : Maîtriser le pattern d'enchaînement.
**Effort estimé** : Continu sur 2 semaines minimum.
**Pré-requis** : Phase 4 validée

### Pattern de référence

```txt
Claude pense → Codex exécute → Claude reviewe → Copilot assiste en éditeur
```

### Répartition par type de tâche

| Type de tâche | Outil préféré | Raison |
|---|---|---|
| Cadrage / spec | Claude | Raisonnement, nuance |
| Architecture / design | Claude | Vision long terme |
| Review de code | Claude | Feedback structuré |
| Implémentation sur spec clair | Codex | Exécution efficace |
| Génération de tests | Codex | Pattern matching |
| Refacto transversal complexe | Claude | Comprend les impacts |
| Autocomplétion in-editor | Copilot | Rapide, passif |
| Tâche async isolée | Codex Cloud | Seulement après Phase 6 |

### Règles pratiques

- Sessions courtes : fermer après chaque tâche.
- Une tâche = une branche = un scope.
- Ne pas laisser un agent "améliorer indéfiniment".
- Après 3 tentatives ratées → stop, review humaine.
- Mesurer les quotas chaque jour (Claude : indicateur capacité ; Codex : `/status`).

### Tâches d'apprentissage

**[HUMAIN]** Pendant 2 semaines, traiter au moins 10 tâches réelles en utilisant le pattern. Noter dans un fichier `lessons-learned.md` :
- Ce qui a marché.
- Ce qui n'a pas marché.
- Cas où Codex était clairement meilleur que Claude (ou inverse).
- Quotas consommés par jour.

### [STOP] Validation Phase 5

Après 2 semaines minimum d'usage réel :
- 10+ tâches traitées avec le pattern ?
- Quotas mesurés (moyenne / jour) ?
- Notes prises sur ce qui marche / marche pas ?

Si oui → Phase 6 envisageable. Sinon → continuer Phase 5.

---

## Phase 6 — Async léger

**Objectif** : Laisser tourner UNE tâche pendant l'absence, pas plus.
**Effort estimé** : 2-3h setup
**Pré-requis** : Phase 5 validée (2 semaines minimum)

### Tâches éligibles

- Bug fix trivial isolé.
- Ajout de tests sur code existant.
- Refacto cosmétique (renames, format).
- Documentation manquante.
- Correction lint/format simple.

### Tâches INTERDITES en async au démarrage

- Auth / RLS / Permissions
- Infra / Docker
- Migrations DB
- Dépendances majeures
- Architecture transverse
- Payout/sales/settlement critique
- Changement de contrat API public
- Nouvelle feature

### Garde-fous obligatoires

1. Aucune merge automatique.
2. Périmètre verrouillé par tâche (Claude doit le respecter strictement).
3. Max 3 tentatives sur un bug.
4. Checkpoint après chaque tâche.
5. Pas de commandes destructives sans dry-run.
6. Pas de modification secrets/env/tokens.
7. Pas de changement de versions sans `VERSIONS.md`.
8. Question bloquante : stop plutôt qu'inventer.
9. PR obligatoire, review humaine obligatoire.

### Setup technique

**[HUMAIN]** Configurer Slack MCP + canal `#tchalanet-agents`.
**[HUMAIN]** Tester avec UNE tâche éligible en mode async.
**[CLAUDE]** Lire les garde-fous async avant chaque tâche async, les rappeler dans le plan d'exécution.

### [STOP] Validation Phase 6

Au moins 1 tâche async exécutée avec succès, lessons-learned documentées.

---

## Setup accès distant Mac

> Cette section est pour l'humain, pas pour Claude Code.

### Trio minimum (gratuit, 30 min)

1. **Tailscale** sur Mac + téléphone
2. **SSH** activé sur Mac (System Settings → General → Sharing → Remote Login : ON)
3. **Termius** ou **Blink Shell** sur téléphone
4. **tmux** sur Mac (`brew install tmux`)
5. **Amphetamine** ou `caffeinate -dims` pour empêcher la veille
6. **Mac branché secteur** en permanence

### Garde-fous sécurité

- **SSH par clé** (pas par password)
- **Désactiver password authentication** si possible
- **Tailscale avec MFA** activé
- **Ne pas exposer SSH hors Tailscale**
- **Mac verrouillé physiquement** quand absent
- **Ne jamais mettre tokens dans l'historique shell** (`HISTCONTROL=ignorespace` puis préfixer commandes sensibles avec espace)
- **Pas d'async risqué** si tu es injoignable longtemps

### Workflow tmux

```bash
# Sur Mac avant de partir
tmux new -s claude
claude  # lance Claude Code dans tmux
# Détacher : Ctrl+B puis D

# Depuis le téléphone (Termius via Tailscale)
ssh tonuser@mac-tchalanet.tail-scale.ts.net
tmux attach -t claude
# Tu retrouves la session

# Détacher à nouveau : Ctrl+B puis D
```

### Notifications push

- **ntfy.sh** (gratuit) : pour notifs simples (PR créée, CI fini)
- **Slack MCP** : pour notifs riches une fois Phase 6 active

---

## Budget — mesurer avant d'optimiser

### Budget minimal

| Outil | Mensuel |
|---|---|
| Claude Pro | ~20$ |
| ChatGPT Plus (Codex) | ~20$ |
| GitHub Copilot | selon plan |
| **Total minimal** | **~40-50$** |

### Budget réaliste en usage intensif

```txt
60-120$/mois
```

Raisons : Claude Code longs contextes, Codex Cloud async, runs répétés, Copilot, rework/review.

### Règle

```txt
Mesurer pendant 30 jours avant d'optimiser ou d'upgrader.
```

Indicateurs à suivre quotidiennement :
- Claude : capacité restante dans l'interface
- Codex : `/status` en CLI ou dashboard usage

---

## Risques principaux à surveiller

1. **Sur-configuration** : passer plus de temps à régler les agents qu'à coder.
2. **Règles trop longues** : les agents ne les suivent plus.
3. **Docs divergentes** : `AGENTS.md`, `.agent/`, `CLAUDE.md`, Copilot se contredisent.
4. **MCP trop larges** : agent capable d'agir hors scope.
5. **Async trop tôt** : fausse productivité, review lourde le soir.
6. **Budget sous-estimé** : pas de monitoring quotidien.
7. **Sécurité Mac/SSH** sous-estimée.
8. **Automatisation PR trop tôt**.
9. **Web Tchalanet figé prématurément** dans `AGENTS.md`.
10. **Agents qui "améliorent" le code au-delà de la tâche**.

---

## Conclusion — la formule

```txt
Claude pense.
Codex exécute.
Claude reviewe.
Copilot assiste.
Docs restent la vérité.
.agent reste jetable.
```

Stratégie :
- Peu de règles globales.
- Des contextes courts.
- Des commandes explicites.
- Des docs normatives séparées.
- Des MCP à la demande.
- Pas d'async trop tôt.
- Élagage régulier (audit mensuel).

---

## Récap des phases — état du projet

À mettre à jour au fur et à mesure (par Claude ou l'humain) :

- [ ] Phase 0 — Safety baseline
- [ ] Phase 1 — Fondations
- [ ] Phase 2 — Outillage officiel
- [ ] Phase 3 — Slash commands V1
- [ ] Phase 4 — MCP à la demande
- [ ] Phase 5 — Enchaînement Claude/Codex/Copilot
- [ ] Phase 6 — Async léger
- [ ] Setup accès distant Mac