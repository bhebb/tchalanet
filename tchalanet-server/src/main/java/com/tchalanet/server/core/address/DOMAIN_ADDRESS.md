# Domaine Address

> Ce fichier est un **template** pour documenter le domaine backend.
> Copie/complète les sections ci-dessous (voir `docs/DOMAIN_TEMPLATE.md`).

---

# Domaine core.address — Adresses (tenant/outlet/user)

> Centralise la gestion d’adresses postales et de contact pour tenants, outlets, et (optionnel) utilisateurs.

> Functional overview (MkDocs): `tchalanet-docs/docs/02-functional/domains/catalog.md` (address reference)

---

## 1. Rôle du domaine

- CRUD d’adresses (tenant/outlet/user).
- Fournir lookup et validation (format, pays, phone/email optionnel).

**Ne fait pas**

- Géocodage avancé (ex: Google Maps) — externe.

---

## 2. Modèle & invariants

- `Address`: id, tenantId?, outletId?, userId?, line1, line2?, city, state?, country, postalCode?, phone?, email?, status.
- Invariants:
  - format de phone/email validé si présent.
  - country codes ISO.

---

## 3. Use Cases

- `CreateAddressCommandHandler`
- `UpdateAddressCommandHandler`
- `AssignAddressToOutletCommandHandler`
- `AssignAddressToTenantCommandHandler`
- `ListTenantAddressesQueryHandler`

---

## 4. Ports

- `AddressRepoPort`

---

## 5. Mapping & DTOs

- MapStruct; DTO `AddressRequest` / `AddressResponse`.

---

## 6. Notes techniques

- Tenant-scoped si rattaché à tenant/outlet → `BaseTenantEntity`.
- RLS appliqué.
- Wrappers d’ID hors JPA; UUID en JPA.

---

## 7. Incohérences / TODO

- Définir si une adresse peut être partagée entre tenant/outlet/user.
- Structurer la validation par pays (formats).
