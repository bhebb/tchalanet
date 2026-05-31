# Guide agent terrain (vendeur / caissier)

## Ce que cette page répond

Je suis vendeur ou caissier sur un terminal POS. Par où je commence ?

---

## Votre rôle

En tant qu'agent terrain, vous opérez un terminal POS dans un outlet :

- Vous vendez et vérifiez des tickets
- Vous payez les gagnants sur le terrain
- Vous travaillez online ou offline selon la connectivité
- Vous ouvrez et fermez vos sessions de vente

---

## Parcours recommandé

### 1. Comprendre le système

1. [Qu'est-ce que Tchalanet ?](../../00-overview/what-is-tchalanet.md) — votre rôle dans la plateforme

### 2. Mettre en service votre terminal

| Étape | Flow |
|---|---|
| Premier démarrage du terminal | [Liaison terminal](../flows/terminal-binding.md) |
| Ouvrir votre session de vente | [Ouverture de session](../flows/session-opening.md) |

### 3. Opérer au quotidien

| Action | Flow |
|---|---|
| Vendre un ticket | [Vente de ticket](../flows/sell-ticket.md) |
| Vérifier un ticket gagnant | [Vérification de ticket](../flows/verify-ticket.md) |
| Payer un gagnant sur le terrain | [Paiement terrain (payout)](../flows/payout-field-flow.md) |
| Travailler sans connexion | [Synchronisation offline](../flows/offline-sync.md) |

---

## Cas courants

**Je veux vendre un ticket** → Voir [Vente de ticket](../flows/sell-ticket.md).
La vente normale retourne `ACCEPTED (201)`. Une grosse vente peut passer en `PENDING_APPROVAL` — attendez la validation admin.

**Le client dit avoir un ticket gagnant** → Voir [Vérification de ticket](../flows/verify-ticket.md).
Vérifiez le code avant tout paiement.

**Je n'ai pas de connexion internet** → Voir [Synchronisation offline](../flows/offline-sync.md).
Le terminal continue de fonctionner offline. Les ventes se synchronisent dès que la connexion revient.

**Ma session refuse de démarrer** → Vérifiez que le terminal est lié (`status: ACTIVE`) → [Liaison terminal](../flows/terminal-binding.md).

---

## Glossaire rapide

| Terme | Sens |
|---|---|
| **Session** | Période de vente ouverte sur votre terminal |
| **Ticket ACCEPTED** | Vente confirmée, ticket valide |
| **Ticket PENDING_APPROVAL** | En attente de validation par l'admin POS |
| **Payout** | Paiement d'un ticket gagnant sur le terrain |
| **Offline mode** | Le terminal vend sans connexion internet |
