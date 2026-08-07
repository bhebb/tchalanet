# Providers supportés

Cette page indique quels providers Tchalanet connaît pour la POC et comment les
résultats doivent être attendus.

!!! warning "Périmètre POC"
    Pour les nouveaux santrals de la POC, seuls **New York (NY)** et
    **Florida (FL)** sont activés automatiquement. Les autres providers peuvent
    être disponibles dans le catalogue, mais le client ou l'équipe support doit
    les activer explicitement.

## Tableau des providers

| Provider | Code | Mode POC | Résultats attendus après tirage | Activation |
|---|---:|---|---:|---|
| New York | NY | Automatique | 5 à 30 min | Activé par défaut pour nouveaux santrals |
| Florida | FL | Automatique | 5 à 30 min | Activé par défaut pour nouveaux santrals |
| Georgia | GA | Automatique disponible, non activé par défaut | 10 à 45 min si activé | Activation client/support |
| Texas | TX | Disponible, non activé par défaut | 10 à 60 min si activé | Activation client/support |
| Pennsylvania | PA | Disponible, non activé par défaut | 10 à 45 min si activé | Activation client/support |
| New Jersey | NJ | Automatique disponible, non activé par défaut | 10 à 45 min si activé | Activation client/support; résultat parfois partiel selon le feed |
| California | CA | Automatique disponible, non activé par défaut | 10 à 60 min si activé | Activation client/support |
| Ohio | OH | Automatique à valider live | 10 à 45 min si auth/feed valide | Activation support après validation live |
| Michigan | MI | Disponible, non activé par défaut | 10 à 60 min si activé | Activation client/support |
| Tennessee | TN | Manuel | Saisie manuelle après publication officielle | Activation client/support |
| Illinois | IL | Manuel | Saisie manuelle après publication officielle | Activation client/support |
| Missouri | MO | Manuel | Saisie manuelle après publication officielle | Activation client/support |
| Minnesota | MN | Manuel | Saisie manuelle après publication officielle | Activation client/support |

## Comment lire le délai

Le délai est compté après l'heure officielle du tirage du provider. Il dépend de
trois choses :

1. le moment où le provider publie réellement le résultat ;
2. la fenêtre de recherche automatique de Tchalanet ;
3. la qualité du résultat reçu.

Un résultat automatique peut rester **provisoire** si Tchalanet doit attendre
une confirmation ou si les données reçues sont incomplètes. Un résultat manuel
doit être saisi par une personne autorisée et confirmé selon les permissions.

## Quand un provider est manuel

Un provider est manuel si Tchalanet ne dispose pas encore d'un client automatique
fiable pour récupérer ses résultats, ou si l'intégration n'est pas activée pour
la POC. Dans ce cas :

- le tirage peut être vendu normalement si le canal est actif ;
- le résultat doit être saisi manuellement après publication officielle ;
- le règlement ne doit pas partir avant confirmation.

## Points à vérifier pendant la POC

- NY et FL sont visibles et activés sur les nouveaux santrals attendus.
- GA, NJ et CA sont disponibles en automatique sur staging US, mais restent opt-in.
- OH doit encore être validé en conditions live avant promesse client.
- Les autres providers sont visibles comme disponibles ou configurables, mais
  pas activés automatiquement.
- Un provider manuel n'affiche pas un résultat automatique inventé.
- Un résultat incomplet reste dans un statut non final.
- L'audit est visible après saisie manuelle, confirmation ou correction.
