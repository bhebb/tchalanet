# Proposal — Guides visuels admin orientés actions

**Date :** 2026-08-09
**Type :** Documentation utilisateur — guides visuels admin
**Branch :** `codex/roadmap-client-pilot-docs-main`
**Statut :** Proposé

---

## Why

Les PDF et captures admin commencent à couvrir les bons écrans, mais certains parcours restent ambigus pour un admin terrain.

Exemple : on dit "bloquer un numéro", mais si la capture ne montre pas clairement le bouton, le formulaire, le champ du numéro et le résultat attendu, l'utilisateur ne sait pas où agir ni comment vérifier que l'action a réussi.

Les admins de bòlèt ne doivent pas deviner le fonctionnement à partir d'une capture décorative. Le guide doit être un support d'exécution : voir l'action, faire l'action, vérifier le résultat.

---

## What

Transformer les guides admin visuels en parcours actionnables.

Chaque parcours critique doit suivre ce format :

1. **Objectif métier** — ce que l'admin veut faire.
2. **Où aller** — lien/menu exact dans l'app, avec le libellé affiché.
3. **Où cliquer** — capture annotée avec flèche ou repère numéroté.
4. **Quoi remplir** — champ(s) importants, valeur d'exemple si utile.
5. **Résultat attendu** — ce qui confirme que l'action est terminée.
6. **Erreur fréquente** — quoi vérifier si le bouton/action n'apparaît pas ou si l'action échoue.

Parcours prioritaires :

- Bloquer un numéro.
- Limiter la vente sur un numéro.
- Bloquer un machann.
- Réinitialiser le PIN d'un machann.
- Créer un machann.
- Vendre un ticket en mode admin/test.
- Contrôler un tirage spécifique.
- Proposer un résultat manuel si le résultat fournisseur n'est pas arrivé.
- Lire un rapport machann.
- Retrouver et vérifier un ticket.

---

## Impact

- `tchalanet-docs/docs/assets/guides/admin/` — nouvelles captures de menu ouvert, dialogue, formulaire et état après action.
- `output/pdf/guide-admin-operations-tchalanet-draft.pdf` — parcours opérationnels enrichis avec flèches et résultats attendus.
- `output/pdf/guide-admin-configuration-tchalanet-draft.pdf` — parcours de configuration enrichis seulement pour les actions de démarrage.
- `tchalanet-web/apps/web-e2e/src/mobile/admin-guide-screenshots.spec.ts` — captures reproductibles pour les écrans nécessaires au guide.
- Pages d'aide MkDocs admin/vendeur — intégration progressive des mêmes captures et parcours.

---

## Non-goals

- Refaire toute la navigation admin.
- Changer les règles métier.
- Ajouter de nouvelles fonctionnalités produit.
- Traduire tout le guide final en anglais/français/créole dans ce change.
- Produire les captures mobile vendeur finales.

---

## Acceptance Criteria

- Les parcours critiques ne reposent plus sur une capture sans action visible.
- Chaque action destructive ou sensible montre au moins : bouton/menu, confirmation/formulaire, résultat attendu.
- Les captures utilisent les libellés réels de l'app (`Machann`, `Bloke`, `Anrejistre`, `Limit`, `Tiraj`).
- Les annotations ne masquent pas les champs ou boutons utiles.
- Les PDF sont régénérés et rendus en PNG pour vérification visuelle.
- La spec Playwright permet de régénérer les nouvelles captures sans manipulation manuelle.
