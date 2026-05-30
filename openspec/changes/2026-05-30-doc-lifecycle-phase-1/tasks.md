# Tasks — Phase 1 : Policy + Inventory

**Règle :** Validation humaine avant toute modification de fichier source.  
**Ne pas toucher :** `ai-agents/`, `setup-ai-agents-slice-first/`

---

## 1. Audit documentaire initial

- [ ] Lister tous les dossiers sous `openspec/changes/` et classifier : livré | actif | obsolète
- [ ] Lister tous les dossiers sous `tchalanet-docs/docs/` et identifier les orphelins
- [ ] Relancer `scripts/docs/inventory-docs.py` pour un inventaire frais
- [ ] Produire tableau : dossier / statut / action recommandée

## 2. Mettre à jour `doc-policy.md`

- [ ] Ouvrir `tchalanet-docs/docs/00-guidelines/doc-policy.md`
- [ ] Ajouter hiérarchie de vérité documentaire (8 niveaux — voir `design.md` dans la spec globale)
- [ ] Ajouter section "Cycle de vie OpenSpec" avec checklist d'extraction
- [ ] Ajouter règles de contenu par type (DOMAIN_*, conventions, tchalanet-docs, openspec/context)
- [ ] Ajouter "Documentation Ownership" par projet
- [ ] Ajouter severity policy (errors vs warnings) et PR Checklist docs
- [ ] Corriger `.claude/skills/` → `.agents/skills/`
- [ ] Bump version + date

## 3. Réécrire `archive-policy.md`

- [ ] Ouvrir `tchalanet-docs/docs/06-openspec/archive-policy.md`
- [ ] Interdire `openspec/changes/archive/`
- [ ] Documenter la checklist d'extraction avant suppression
- [ ] Bump version

## 4. Nettoyer `openspec/context/00-index.md`

- [ ] Lister chaque pack référencé
- [ ] Vérifier existence de chaque fichier (`openspec/context/*.md`)
- [ ] Retirer les entrées fantômes
- [ ] Ajouter note : "Router IA — pointe vers fichiers canoniques, ne recopie pas les règles"

## 5. Corriger les liens cassés

- [ ] `DOCUMENTATION.md` (root) : `.claude/skills/` → `.agents/skills/`, bump version
- [ ] Grep sur tous les fichiers référençant `.claude/skills/` et corriger

## 6. Vérifier conventions tchalanet-server (lecture seule)

- [ ] Lire `tchalanet-server/docs/ARCHITECTURE.md` — référence tous les composants ? à jour ?
- [ ] Lister fichiers dans `tchalanet-server/docs/conventions/`
- [ ] Identifier conventions manquantes (batch, cache, persistence, RLS, etc.)
- [ ] Produire tableau : convention / existe / à jour / action
- [ ] **Ne pas modifier le contenu — identifier les gaps uniquement**

## 7. Identifier flows manquants (lecture seule)

- [ ] Lister workflows dans `tchalanet-docs/docs/02-functional/flows/`
- [ ] Identifier workflows métier clés manquants
- [ ] Produire liste TODO (sans créer les flows)
