# CLAUDE.md — tchalanet-docs

> **Lire d'abord** : `../CLAUDE.md` (règles transverses, secrets, OpenSpec)

## OpenSpec local

```text
tchalanet-docs/openspec/
```

Toutes les changes documentation (MkDocs, ADR, guides) vivent ici.

Archiver via :

```bash
cd tchalanet-docs
openspec archive <change-id> --yes
```

## Périmètre

Ce projet est **autonome**. Ne pas inspecter ni modifier les autres projets sauf demande explicite.

## Vérification contexte (obligatoire avant analyse ou édition)

```bash
pwd
git branch --show-current
git status --short
git log -1 --oneline
find . -maxdepth 3 -type d -name openspec
```

---

## Stack documentation

| Élément     | Valeur                    |
| ----------- | ------------------------- |
| Outil       | MkDocs + Material theme   |
| Python      | 3.12 (venv local `venv/`) |
| Config      | `mkdocs.yml` (racine)     |
| Dépendances | `requirements.txt`        |

---

## Skills docs (`tchalanet-docs/.claude/skills/`)

`mkdocs`

---

## Structure `docs/`

```
docs/
├─ 00-guidelines/     ← règles, constitution, politique doc, glossaire, versions
├─ 01-architecture/   ← cartes d'architecture (backend, edge, frontend, infra, sécurité)
├─ 02-functional/
│  ├─ domains/        ← une page par domaine métier (25+ domaines)
│  ├─ features/       ← i18n, news, notifications, pagemodel, reporting, stats
│  └─ flows/          ← sell-ticket, draw-execution, claim-payout, verify-ticket
├─ 03-adr/            ← Architecture Decision Records (ADR-NNNN-titre.md)
└─ 99-links/          ← pointeurs vers docs near-code de chaque sous-projet
   └─ _ref/           ← sync automatique depuis les sous-projets
```

---

## Où mettre quoi

| Type de contenu                  | Destination                                                   |
| -------------------------------- | ------------------------------------------------------------- |
| Décision d'architecture          | `docs/03-adr/ADR-NNNN-titre.md`                               |
| Nouveau domaine métier           | `docs/02-functional/domains/{domaine}.md`                     |
| Nouvelle feature transverse      | `docs/02-functional/features/{feature}.md`                    |
| Flux utilisateur                 | `docs/02-functional/flows/{flux}.md`                          |
| Carte d'architecture             | `docs/01-architecture/{sous-projet}-map.md`                   |
| Doc near-code (dans sous-projet) | Reste dans le sous-projet — `sync-ref-docs.sh` la synchronise |

---

## Sync docs near-code

Le script `scripts/sync-ref-docs.sh` copie les docs techniques des sous-projets vers `docs/99-links/_ref/`. Exécuter après avoir modifié des docs dans `tchalanet-server/docs/`, `apps/`, etc.

```bash
bash scripts/sync-ref-docs.sh
```

---

## ADR — processus

1. Créer `docs/03-adr/ADR-NNNN-{titre-kebab}.md`
2. Sections obligatoires : **Contexte**, **Décision**, **Conséquences**
3. Numéroter en séquence après le dernier ADR existant
4. Ne jamais modifier un ADR passé — créer un nouvel ADR qui le révise

---

## Do ✅

- Respecter les préfixes numériques (`00-`, `01-`, …) pour l'ordre de navigation
- Une page = un sujet clair — pas de pages fourre-tout
- Synchroniser après modification de docs near-code (`sync-ref-docs.sh`)
- ADRs immuables après fusion — réviser via un nouvel ADR

## Don't ❌

- Dupliquer du contenu déjà présent dans les sous-projets (pointer via `99-links`)
- Modifier `docs/99-links/_ref/` manuellement (géré par le script)
- Créer des pages sans les ajouter à `mkdocs.yml`

---

## Commandes

```bash
cd tchalanet-docs
source venv/bin/activate     # activer le venv Python 3.12
mkdocs serve                 # dev server → http://localhost:8000
mkdocs build                 # génère site/ (vérification des liens)
mkdocs build --strict        # échoue sur warnings (liens cassés, etc.)
```
