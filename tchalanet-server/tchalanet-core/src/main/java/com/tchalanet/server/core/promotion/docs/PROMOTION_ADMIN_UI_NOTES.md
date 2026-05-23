# Admin UI notes — Promotion rules

## V1 UI simple

Écran tenant admin : Promotions.

Colonnes :

```text
Code | Nom | Type | Active | Période | Priorité | Offline | Dernière modification
```

Actions :

```text
Créer
Modifier
Activer / désactiver
Dupliquer
Archiver
Tester une règle
```

## Formulaire V1

Champs communs :

```text
code
name
active
rule_type
engine_type
schema_version
priority
stackable
exclusive_group
starts_at
ends_at
timezone
offline_allowed
```

Conditions : UI guidée par type.

Effets : UI guidée par type.

Ne pas exposer un JSON editor brut aux tenant admins en V1, sauf mode super-admin/dev.

## Multiplicateur variable par tenant

Pour `PAYOUT_MULTIPLIER_OVERRIDE`, le formulaire doit permettre :

```text
gameCode
prizeRank
baseMultiplier optionnel informatif
appliedMultiplier requis
```

Exemple :

```text
Jeu: BOLET
Rang: FIRST
Multiplicateur appliqué: 60
Période: 25 décembre 00:00 -> 25 décembre 12:00
Timezone: America/Port-au-Prince
```

## Testeur de règle

Permettre à l'admin d'entrer :

```text
date/heure de vente
outlet
terminal
jeu
mise
total panier
rang de prix
```

Le résultat doit afficher :

```text
règles matchées
règles ignorées + raison
effets générés
snapshots qui seraient stockées
```
