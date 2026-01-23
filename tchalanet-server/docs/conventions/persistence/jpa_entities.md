---

```md
# JPA Entities — Choix de la Base Class

## Décision

Le choix entre `BaseEntity` et `BaseTenantEntity` dépend **uniquement de l’ownership des données**, pas du scope API.

---

## Utiliser `BaseTenantEntity` si :

- La ligne appartient à **un seul tenant**
- La table a `tenant_id NOT NULL`
- Les policies RLS s’appliquent

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

❌ Table tenantée sans `tenant_id`  
❌ Entité tenantée étendant `BaseEntity`  
❌ Dupliquer les champs audit manuellement

---

## Règle d’or

👉 **Le tenant est une propriété SQL, pas applicative.**  
Les entités doivent refléter cette réalité.

## Relations JPA (conseil Tchalanet)

- `@ManyToMany` : INTERDIT
- `@OneToMany` : INTERDIT par défaut (sauf exception documentée)
- `fetch = EAGER` : INTERDIT

Préférer :

- stocker des IDs (`UUID` côté JPA) plutôt que des relations
- `@ManyToOne(fetch = LAZY)` seulement si même agrégat et usage lecture
- pas de cascade remove sauf si c'est vraiment justifié.
