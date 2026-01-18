# Domaine Featureflags

> Ce fichier est un **template** pour documenter le domaine backend.
> Copie/complète les sections ci-dessous (voir `docs/DOMAIN_TEMPLATE.md`).

---

## 1. Rôle du domaine

**Responsabilité principale**

> Décris en une phrase la responsabilité clé du domaine.
> **Ce que le domaine fait**

- ...
  **Ce que le domaine ne fait pas**
- ...

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- ...

### Invariants métier

- ...
  > Valeur métier clé :
  > ...

---

## 3. Cas d’utilisation (ports d’entrée)

- ...

---

## 4. Ports de sortie (dépendances externes)

- ...

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapper infra.web.model ↔ application.command/query.model
- Records immuables pour DTO simples, Lombok si nécessaire

---

## 6. Règles métier importantes

- ...

---

## 7. Intégration avec les autres domaines

Dépend de : ...
Utilisé par : ...

---

## 8. Notes techniques

- Multi-tenant ? RLS ? Intégrations externes ?

---
