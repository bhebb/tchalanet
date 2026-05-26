# DOMAIN_OUTLET — Outlet V1

## Rôle

`core.outlet` représente le canal opérationnel où la vente est réalisée.

Un outlet peut être : boutique, kiosque, point mobile, banque régionale, institution partenaire, autre tenant partenaire représenté simplement, bureau régional, point de vente ambulant modélisé comme mobile outlet.

## Décision

Une institution partenaire n'est pas un seller. Une banque partenaire n'est pas un seller. Elles sont des outlets, ou plus tard des partners liés à des outlets.

## OutletKind

```java
public enum OutletKind {
    OWNED_SHOP,
    KIOSK,
    MOBILE_POINT,
    BANK_BRANCH,
    PARTNER_INSTITUTION,
    PARTNER_TENANT,
    REGIONAL_OFFICE
}
```

## Données principales

```text
outlet
  id
  tenant_id
  code
  display_name
  kind
  zone_id nullable
  status
  partner_ref nullable
  metadata_json nullable
  audit columns
```

## Responsabilités

`core.outlet` connaît l'existence de l'outlet, son tenant, son statut, son type, sa zone, ses métadonnées institutionnelles simples, et s'il peut être utilisé pour une opération.

`core.outlet` ne connaît pas la vente elle-même, la commission seller, la promotion, les limites seller, la session POS, le prepaid financier.

## Validation opérationnelle

Une vente doit valider : outlet exists, outlet belongs to tenant, outlet status allows sale, outlet is compatible with terminal/session.

## Partner futur

On ne crée pas `core.partner` en V1. Extraction possible plus tard si besoin de contrat partenaire, multi-outlets, commission avancée, settlement, reporting ou relation réelle avec autre tenant.

## Impact Sales

Sales snapshotte `outlet_id` et, si utile : `outlet_kind_snapshot`, `outlet_zone_id_snapshot`, `partner_ref_snapshot nullable`.
