# Feature PublicDrawResults

> Surface publique (anonymous) pour afficher les résultats de tirages globaux.  
> Pas de contexte tenant requis — données de référence globales uniquement.

---

## Rôle

BFF read-only pour l'affichage public des résultats de tirages. Consomme `core.drawresult` via `QueryBus`.  
Utilisé par le site public, les terminaux POS et le widget PageModel `public_draw_results`.

---

## Endpoints

```http
GET /public/draw-results/slots
```
Liste non paginée des slots actifs — vue légère pour mobile/terminal/affichage.  
Retourne : métadonnées slot, dernier résultat disponible, prochain tirage attendu (countdown), `history=[]`.

```http
GET /public/draw-results/slots/details
```
Liste non paginée avec historique récent par slot.  
Paramètre `historyLimit` : défaut 5, max 10.

```http
GET /public/draw-results/history
```
Historique paginé avec recherche avancée.  
Supporte : filtre par plage de dates, slot, provider.

---

## Frontières

- Appelle `core.drawresult` uniquement — jamais `core.draw` ni `core.draw_channel` (tenant-scoped)
- Sources de vérité : `result_slot` + `draw_result` (globaux)
- Les DTOs ne exposent pas d'UUID internes par défaut
- Pas de contexte tenant requis (anonymous)
- Pas de mutation — lecture seule

---

## Intégration PageModel

`PublicDrawResultsProvider` (source : `public_draw_results`) :
- Appelle `QueryBus.send(new ListPublicDrawResultSlotsQuery(..., false, 0))` directement
- Ne passe pas par les endpoints HTTP
- Ne consulte pas `core.draw` (tenant-scoped)

---

## Références

- Domaine : `core/drawresult/DOMAIN_DRAWRESULT.md`
- Slot global : `catalog/resultslot/CATALOG_RESULTSLOT.md`
- Pipeline résultats : `tchalanet-docs/docs/02-functional/flows/draw-execution.md`
- Intégration PageModel : `features/pagemodel/FEATURE_PAGEMODEL.md`
