---
name: mkdocs
description: Use when adding, moving, or editing documentation pages in tchalanet-docs — covers MkDocs Material configuration, nav structure, numeric prefix conventions, ADR process, sync-ref-docs.sh workflow, and mkdocs serve/build commands.
allowed-tools: Read, Write, Edit, Glob, Grep, Bash
---

# MkDocs — tchalanet-docs

## Démarrage

```bash
cd tchalanet-docs
source venv/bin/activate      # venv Python 3.12 local
mkdocs serve                  # http://localhost:8000 (hot-reload)
mkdocs build --strict         # build complet, échoue sur liens cassés
```

---

## Configuration — `mkdocs.yml`

Fichier racine de `tchalanet-docs/`. Il contrôle :

- `site_name`, `site_url`
- `theme:` Material — options (palette, features, icons)
- `nav:` — arbre de navigation (à maintenir synchronisé avec les fichiers)
- `plugins:` — search, minify, etc.
- `markdown_extensions:`

**Toute nouvelle page doit être ajoutée dans `nav:`** sinon elle n'apparaît pas dans la sidebar.

---

## Convention de structure

```
docs/
├─ 00-guidelines/   ← règles et politiques (lecture en premier)
├─ 01-architecture/ ← cartes système (backend, edge, frontend, infra, sécurité)
├─ 02-functional/   ← domaines, features, flows
├─ 03-adr/          ← Architecture Decision Records
└─ 99-links/        ← pointeurs et docs near-code synchronisées
   └─ _ref/         ← géré par sync-ref-docs.sh — ne pas éditer manuellement
```

Le préfixe numérique (`00-`, `01-`, …) contrôle l'ordre dans la sidebar. Ne pas le changer sans mettre à jour `mkdocs.yml`.

---

## Ajouter une page

1. Créer le fichier `.md` dans le bon répertoire
2. Ajouter l'entrée dans `nav:` de `mkdocs.yml`
3. Vérifier l'affichage avec `mkdocs serve`

---

## ADR (Architecture Decision Record)

Format fichier : `docs/03-adr/ADR-NNNN-{titre-kebab}.md`

Structure minimale :

```markdown
# ADR-NNNN — Titre

## Statut

Accepté | Révoqué par ADR-MMMM

## Contexte

Pourquoi cette décision était nécessaire.

## Décision

Ce qui a été décidé.

## Conséquences

Impact positif / négatif / neutre.
```

- Numérotation séquentielle après le dernier ADR existant
- **Immuable après fusion** — pour réviser, créer un nouvel ADR et mettre le statut "Révoqué par ADR-NNNN"

---

## Sync docs near-code

```bash
bash scripts/sync-ref-docs.sh
```

Copie les docs des sous-projets (`tchalanet-server/docs/`, `apps/`, etc.) vers `docs/99-links/_ref/`. Toujours exécuter après avoir modifié des docs techniques dans les sous-projets.

Ne **jamais** éditer `docs/99-links/_ref/` manuellement — les changements seront écrasés au prochain sync.

---

## Material theme — fonctionnalités utiles

```yaml
# mkdocs.yml
theme:
  name: material
  features:
    - navigation.tabs # onglets en haut
    - navigation.sections # sections dans la sidebar
    - navigation.expand # expand automatique
    - search.highlight
    - content.code.copy # bouton copier dans les blocs de code
```

---

## Markdown extensions utiles

```yaml
markdown_extensions:
  - admonition # !!! note / warning / tip
  - pymdownx.details # blocs repliables
  - pymdownx.superfences
  - pymdownx.tabbed:
      alternate_style: true
  - attr_list
  - tables
```

Exemple admonition :

```markdown
!!! warning "Attention"
Ne pas modifier les fichiers dans `_ref/` directement.
```
