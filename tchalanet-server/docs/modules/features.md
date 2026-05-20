# Module features — Vertical Slice / BFF

## Rôle

`features` expose des endpoints orientés écran/flow/navigation.

## Pattern

```text
features/<feature>/<slice>/
  web/
  app/
  model/
  mapper/
```

Si une slice contient moins de 3 classes par rôle, elle peut rester flat.

## Règle principale

`features` est un leaf module : personne ne doit le consommer côté Java.

Le contrat public d’une feature est :

```text
HTTP route
Request/Response DTOs
OpenAPI
```

Pas de `features/<feature>/api` par défaut.

## Si deux features partagent quelque chose

Ne pas créer une API feature commune. Extraire vers :

```text
platform.<capability>.api
core.<domain>.api
catalog.<catalog>.api
common uniquement si purement technique
```
