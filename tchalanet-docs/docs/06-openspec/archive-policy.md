# OpenSpec Archive Policy

**Version**: 2.0.0 | **Date**: 2026-05-30

---

## Règle fondamentale

**`openspec/changes/archive/` est interdit.**

Il n'existe pas de dossier `archive/` dans `openspec/changes/`. Un dossier archive est le signe que l'extraction n'a pas été faite.

---

## Lifecycle d'une change livrée

```
openspec/changes/<id>/     ← atelier WIP pendant la feature
        │
        ▼ feature livrée
        │
        ├─ [ ] décisions durables extraites vers leur destination
        ├─ [ ] conventions mises à jour si nécessaire
        ├─ [ ] ADR créé si décision architecturale
        ├─ [ ] context pack IA mis à jour si nécessaire
        ├─ [ ] rien de durable restant dans la change
        └─ [ ] supprimer openspec/changes/<id>/
```

**Suppression après preuve d'extraction, jamais avant.**

---

## Où vont les éléments durables

| Élément | Destination |
|---|---|
| Norme stable (s'applique à plusieurs projets) | `tchalanet-docs/docs/00-guidelines/` |
| Convention technique (spécifique à un projet) | `<projet>/docs/conventions/` |
| Décision architecturale | `tchalanet-docs/docs/03-adr/` |
| Context pack IA | `openspec/context/` |
| Rien de durable | Suppression directe |

---

## Ce qui est interdit

- Créer `openspec/changes/archive/` ou tout sous-dossier `archive/`
- Déplacer une change livrée dans un dossier d'archive au lieu d'extraire
- Supprimer une change sans avoir complété la checklist d'extraction
- Laisser une change livrée en place "pour référence future"

---

## Changes actives

Les changes actives ne sont **jamais** nettoyées par une passe documentaire générale.
Elles sont nettoyées uniquement au moment de leur livraison, par leur propre checklist d'extraction.

Identifier une change comme active = ne pas y toucher, même lors d'un audit.

---

**Maintenu par** : équipe Tchalanet  
**Dernière mise à jour** : 2026-05-30
