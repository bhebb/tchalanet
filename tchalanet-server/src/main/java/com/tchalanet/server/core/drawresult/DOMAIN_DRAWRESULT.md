# Domaine Catalog DrawResult

> Référentiel des résultats publiés (par slot, date, jeu). Sert de lookup stable pour exposition publique et reporting. La logique de calcul des résultats réside ailleurs (core.draw).

---

## 1. Rôle du domaine

**Responsabilité principale**

> Stocker et exposer les résultats de tirage publiés (globaux ou par slot), avec métadonnées (occurredAt, slotKey, gameCode, resultPayload).

**Ce que le domaine fait**

- Enregistre les résultats publiés (payload normalisé).
- Permet recherche par slot/date/jeu.
- Expose une API de lecture (public/platform/admin).

**Ce que le domaine ne fait pas**

- Calculer les résultats (settlement) — core.draw / core.sales.
- Gérer les payouts (core.payout).

---

## 2. Modèle métier (agrégats / entités)

### Entités / agrégats principaux

- `DrawResult` — (id, occurredAt, slotKey, gameCode, payload, status).

### Invariants métier

- La combinaison (occurredAt, slotKey, gameCode) doit être unique/logiquement cohérente.

> Valeur métier clé :
> Servir de source de vérité des résultats publiés à des fins d’exposition et d’audit.

---

## 3. Cas d’utilisation (ports d’entrée)

- `ListDrawResultsQuery` — lister par filtres.
- `GetDrawResultQuery` — obtenir par clés.
- (Admin) `PublishDrawResultCommand` — publier/mettre à jour un résultat.

---

## 4. Ports de sortie (dépendances externes)

- `DrawResultReaderPort` — lecture référentiel.
- `DrawResultWriterPort` — écriture référentiel (admin/ingestion).

---

## 5. Mapping & DTOs (convention)

- MapStruct pour mapping entity ↔ projection `DrawResultResponse`.
- IDs wrappers côté web; UUID en JPA.

---

## 6. Règles métier importantes

- La donnée doit être immuable après publication (ou versionnée).
- Audit recommandé (Envers) pour traçabilité.

---

## 7. Intégration avec les autres domaines

Dépend de : `catalog.resultslot` (slotKey), `catalog.game` (gameCode).

Utilisé par : exposition publique (features/publicdraw), reporting.

---

## 8. Notes techniques

- Scoping: selon décision, table peut être globale (BaseEntity) ou tenant-scoped (BaseTenantEntity) si les résultats sont tenantisés.
- RLS: s’applique si tenant-scoped; sinon, sécurité via scope public/platform/admin.
- SDR possible (`/_sdr/drawresults`) pour admin.
