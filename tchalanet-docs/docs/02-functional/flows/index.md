# Workflows (Flows transverses)

**Workflows utilisateur croisant plusieurs domaines**

Ces workflows décrivent les parcours utilisateur de bout en bout (UI → Backend → DB).

---

## 📋 Workflows

### [Vente ticket (Sell Ticket)](sell-ticket.md)

POS / Web / Mobile → Backend (sales + limits) → DB  
États : choix numéros → validation limites → émission → impression

### [Vérification ticket publique (Verify Ticket)](verify-ticket.md)

Client scanne QR → Backend valide signature → retourne statut  
États : VALID / CANCELLED / PENDING_SYNC / EXPIRED

### [Exécution tirage (Draw Execution)](draw-execution.md)

Draw planifié → Tirage → Résultats publiés → Settlement tickets → Claims créés  
Pipeline : scheduled → executed → settled

---

## 🎯 Principe

Chaque workflow contient :

- Diagramme séquence (Mermaid ou PlantUML)
- Étapes détaillées (UI, API calls, backend handlers, DB)
- États intermédiaires
- Cas d'erreur (retry, rollback, compensation)

**Pas de code** ici → voir implémentations via [99-links](../../99-links/index.md).

---

**Dernière mise à jour** : 2026-01-17
