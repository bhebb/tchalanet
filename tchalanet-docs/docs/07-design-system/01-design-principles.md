# 01 Design Principles

> Status: normative  
> Scope: Web, Mobile, POS, Landing

## Intent

Tchalanet doit être :

- fiable pour des opérations de vente et de paiement;
- rapide pour les vendeurs;
- lisible sur mobile et POS;
- crédible pour les tenants et administrateurs;
- assez expressif pour la landing page sans polluer les écrans opérationnels.

## Principles

### 1. Operational first

Les écrans POS et vendeur privilégient l’action et la lisibilité.

```text
Vendre > Vérifier > Payer > Synchroniser > Consulter
```

### 2. One dominant action

Chaque écran opérationnel doit avoir une action principale évidente.

Sur POS home :

```text
VENDRE TICKET
```

est la seule action full-width dominante.

### 3. No marketing inside POS

La landing page peut être expressive.  
Le POS ne doit pas afficher de contenu marketing, témoignages, plans ou sections longues.

### 4. Semantic colors beat brand colors

Les couleurs de statut ne doivent pas être remplacées par des couleurs de marque.

```text
Success = vert
Warning = orange/brun warning
Error = rouge
Primary action = bleu
Secondary support = violet
Marketing CTA = orange
```

### 5. Money and ticket codes are first-class typography

Les montants, codes ticket, références et totaux doivent être plus lisibles que leurs labels.
