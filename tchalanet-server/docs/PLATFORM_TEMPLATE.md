# Platform Capability `platform.<name>` — <Titre court>

> **Archetype** : Application Service Module.  
> **Placer ce fichier** : `tchalanet-platform/src/main/java/com/tchalanet/server/platform/<name>/PLATFORM_<NAME>.md`

---

## 1. Rôle

**Responsabilité principale**

> Une phrase. Ex. : « Enregistrer et exposer les traces d'audit des opérations critiques. »

**Ce que ce module fait**

- 3 à 5 puces.

**Ce que ce module ne fait pas**

- Ce qui appartient à core ou à un autre module platform.
- Un résultat incorrect ici ne doit PAS causer de perte financière directe, de litige réglementaire ou de tirage erroné — sinon c'est core.

---

## 2. Structure

```text
platform/<name>/
  api/
    <Name>Api.java        ← Interface unique consommée par les autres modules
    model/                ← Records immuables (pas de JPA entities, pas de Spring MVC types)
  internal/
    service/              ← Implémentation de l'api
    persistence/          ← JPA entities, repositories (si stateful)
    web/                  ← Controllers admin (optionnel, /api/v1/platform/**)
    event/                ← Listeners d'events core (optionnel)
    adapter/              ← Adapters externes (HTTP, mail, push…)
    cache/                ← Cache specs (optionnel)
    config/               ← Spring @Configuration
```

---

## 3. API publique (`api/`)

```java
// platform/<name>/api/<Name>Api.java
public interface <Name>Api {
  // Méthodes exposées aux autres modules
  // Uniquement des types immuables (records, primitifs, typed IDs)
  // Pas de JPA entities, pas de Spring MVC types
}
```

**Autorisé dans api/** : interfaces, records immuables, typed IDs.  
**Interdit dans api/** : JPA entities, classes internal, repositories, Spring MVC request/response.

---

## 4. Transactions

- Par défaut : rejoint la transaction de l'appelant (`@Transactional(propagation = REQUIRED)`).
- Exception documentée : si `REQUIRES_NEW` est nécessaire (ex. audit de failure), le justifier ici.
- Ne jamais démarrer une transaction implicite depuis un context async sans lier `TchRequestContext`.

---

## 5. Dépendances

**Autorisé** :
- `common.*`
- `catalog.<x>.api`

**Interdit** :
- `core.*`
- `features.*`
- `platform.<autre>.internal`

---

## 6. Événements

- Ce module peut écouter des events publiés par `core/`.
- Ce module peut publier des events techniques/applicatifs.
- `core/` ne doit PAS écouter les events publiés par ce module.

---

## 7. Points d'attention

- RLS actif sur les tables ? (`tenant_id` géré par context, pas filtré en Java)
- Context obligatoire : `TchRequestContext` ou context système explicite si appelé depuis batch/scheduler.
- Collaboration avec d'autres modules platform : utiliser des events ou un ADR exception.
