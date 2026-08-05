#!/usr/bin/env python3
"""Audit du corpus i18n trilingue (fr/en/ht) — web et mobile.

Mesure les cinq défauts identifiés par l'audit initial :
  1. fuite d'identifiants techniques dans la copy utilisateur
  2. dérive de clés (manquantes / orphelines) entre locales
  3. français résiduel dans les locales dérivées
  4. orthographe kreyòl (IPN)
  5. intégrité technique (placeholders, valeurs vides)

Usage :
    python3 scripts/i18n-audit.py            # rapport complet
    python3 scripts/i18n-audit.py --summary  # compteurs seulement
    python3 scripts/i18n-audit.py --strict   # sortie != 0 si un défaut subsiste

Référence éditoriale : openspec/changes/i18n-trilingual-editorial-pass-v1/design.md
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
from collections import defaultdict

ROOTS = {
    "web": "tchalanet-web/libs/shared-assets/public/assets/i18n",
    "mobile": "tchalanet-mobile/assets/i18n",
}
SOURCE_LOCALE = "fr"
LOCALES = ("fr", "en", "ht")

PLACEHOLDER = re.compile(r"\{\{?\s*[\w.]+\s*\}?\}")

# --- 1. termes bannis de la copy (registre domaine qui fuit dans le registre public) -----
BANNED = {
    "seller-terminal": re.compile(r"seller[-_ ]?terminals?", re.I),
    "tenant": re.compile(r"\btenants?\b", re.I),
    "role-code": re.compile(r"\b(TENANT_ADMIN|SUPER_ADMIN|SELLER_TERMINAL|OPERATOR)\b"),
    "retired-concept": re.compile(r"\b(outlets?|cashiers?|caissiers?)\b", re.I),
    "placeholder-junk": re.compile(r"\b(TODO|FIXME|Lorem|undefined|null)\b"),
}
# le HT localise volontairement tenant -> santral/espas ; « tenant » y est donc aussi une fuite.

# Exception assumée (design.md) : le kreyòl garde « Tèminal POS » là où FR dit « Vendeur ».
# Le terme est installé chez les vendeurs bòlèt. Ce n'est pas un défaut en HT — mais
# `seller-terminal`, l'identifiant de code, reste interdit dans les trois langues.
HT_ALLOWED = re.compile(r"tèminal\s+POS", re.I)

# --- 3. français résiduel ---------------------------------------------------------------
FR_MARKERS = re.compile(
    r"\b(le|la|les|un|une|des|du|de|au|aux|et|ou|est|sont|pour|dans|sur|avec|par|"
    r"votre|vos|cette|ces|ce|cet|vous|nous|qui|que|plus|sans|tous|toutes|"
    r"aucun|aucune|veuillez|vérifier|vérification|connexion|paramètres|utilisateur|"
    r"utilisateurs|gestion|nouveau|nouvelle|modifier|supprimer|enregistrer|annuler|"
    r"fermer|ouvrir|créer|choisir|sélectionner|afficher|masquer|chargement|erreur|"
    r"réessayer|disponible|indisponible|actif|active|inactif|montant|numéro|numéros|"
    r"tirage|tirages|vente|ventes|vendeur|vendeurs|solde|détails|"
    r"prénom|adresse|téléphone|courriel|passe|jour|jours|heure|heures)\b",
    re.IGNORECASE,
)
FR_DIACRITIC = re.compile(r"[éèêëàâçôûùïî]")

# --- 4. orthographe kreyòl (IPN) --------------------------------------------------------
# NE PAS ajouter apre/kounye/nimewo/rezilta/menm : ces formes SONT correctes en IPN.
HT_SPELLING = {
    "anko": "ankò",
    "pwoblem": "pwoblèm",
    "sevis": "sèvis",
    "tike": "tikè",
    "kod": "kòd",
    "sistem": "sistèm",
    "lot": "lòt",
}


def flatten(obj, prefix=""):
    out = {}
    if isinstance(obj, dict):
        for k, v in obj.items():
            out.update(flatten(v, f"{prefix}.{k}" if prefix else k))
    elif isinstance(obj, list):
        for i, v in enumerate(obj):
            out.update(flatten(v, f"{prefix}[{i}]"))
    else:
        out[prefix] = obj
    return out


def load(root: str) -> dict[str, dict[str, object]]:
    data = {}
    for loc in LOCALES:
        locdir = os.path.join(root, loc)
        if not os.path.isdir(locdir):
            continue
        merged = {}
        for fname in sorted(os.listdir(locdir)):
            if not fname.endswith(".json"):
                continue
            with open(os.path.join(locdir, fname), encoding="utf-8") as fh:
                parsed = json.load(fh)
            merged.update(
                {f"{fname[:-5]}:{k}": v for k, v in flatten(parsed).items()}
            )
        data[loc] = merged
    return data


def audit(project: str, root: str, verbose: bool) -> dict[str, int]:
    data = load(root)
    counts: dict[str, int] = {}
    src = data[SOURCE_LOCALE]

    print(f"\n{'=' * 72}\n{project.upper()}  —  {root}\n{'=' * 72}")
    for loc in LOCALES:
        print(f"  {loc}: {len(data[loc])} clés")

    # 2. dérive de clés
    for loc in LOCALES:
        if loc == SOURCE_LOCALE:
            continue
        missing = sorted(set(src) - set(data[loc]))
        orphan = sorted(set(data[loc]) - set(src))
        counts[f"{loc}.missing"] = len(missing)
        counts[f"{loc}.orphan"] = len(orphan)
        print(f"\n  [clés] {loc}: {len(missing)} manquantes, {len(orphan)} orphelines")
        if verbose:
            for k in missing[:20]:
                print(f"      manquante  {k}")
            for k in orphan[:20]:
                print(f"      orpheline  {k}")

    # 1. fuites techniques
    print()
    for loc in LOCALES:
        hits = defaultdict(list)
        for k, v in data[loc].items():
            if not isinstance(v, str):
                continue
            probe = HT_ALLOWED.sub("", v) if loc == "ht" else v
            for label, rx in BANNED.items():
                if rx.search(probe):
                    hits[label].append((k, v))
        total = sum(len(x) for x in hits.values())
        counts[f"{loc}.leak"] = total
        detail = ", ".join(f"{lbl}={len(x)}" for lbl, x in sorted(hits.items()))
        print(f"  [fuite technique] {loc}: {total}" + (f"  ({detail})" if detail else ""))
        if verbose:
            for label, items in sorted(hits.items()):
                for k, v in items[:8]:
                    print(f"      {label:18s} {k}\n           {v!r}")

    # 3. français résiduel
    print()
    for loc in ("en", "ht"):
        residual = []
        for k in set(src) & set(data[loc]):
            a, b = src[k], data[loc][k]
            if not (isinstance(a, str) and isinstance(b, str)):
                continue
            if a.strip() != b.strip() or len(a.strip()) < 3:
                continue
            if FR_MARKERS.search(a) or (loc == "en" and FR_DIACRITIC.search(a)):
                residual.append((k, a))
        counts[f"{loc}.residual_fr"] = len(residual)
        print(f"  [français résiduel] {loc}: {len(residual)}")
        if verbose:
            for k, a in sorted(residual)[:15]:
                print(f"      {k}\n           {a!r}")

    # 4. orthographe kreyòl
    print()
    ht_errors = []
    for k, v in data.get("ht", {}).items():
        if not isinstance(v, str):
            continue
        for bad, good in HT_SPELLING.items():
            if re.search(rf"\b{bad}\b", v):
                ht_errors.append((k, bad, good, v))
    counts["ht.spelling"] = len(ht_errors)
    print(f"  [orthographe kreyòl] ht: {len(ht_errors)}")
    if verbose:
        for k, bad, good, v in ht_errors[:15]:
            print(f"      {bad} → {good}   {k}\n           {v!r}")

    # 5. intégrité technique
    print()
    ph_bad = []
    for loc in ("en", "ht"):
        for k in set(src) & set(data[loc]):
            a, b = src[k], data[loc][k]
            if isinstance(a, str) and isinstance(b, str):
                if sorted(PLACEHOLDER.findall(a)) != sorted(PLACEHOLDER.findall(b)):
                    ph_bad.append((loc, k, a, b))
    counts["placeholder_mismatch"] = len(ph_bad)
    empty = [
        (loc, k)
        for loc in LOCALES
        for k, v in data[loc].items()
        if isinstance(v, str) and not v.strip()
    ]
    counts["empty"] = len(empty)
    print(f"  [placeholders] écarts: {len(ph_bad)}")
    print(f"  [valeurs vides] {len(empty)}")
    if verbose:
        for loc, k, a, b in ph_bad[:10]:
            print(f"      {loc} {k}\n           fr: {a!r}\n           {loc}: {b!r}")

    return counts


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--summary", action="store_true", help="compteurs seulement")
    parser.add_argument("--strict", action="store_true", help="sortie != 0 si défaut")
    parser.add_argument("--project", choices=sorted(ROOTS), help="limiter à un projet")
    args = parser.parse_args()

    roots = {args.project: ROOTS[args.project]} if args.project else ROOTS
    total = 0
    for project, root in roots.items():
        if not os.path.isdir(root):
            print(f"!! introuvable : {root} (lancer depuis la racine du dépôt)")
            return 2
        counts = audit(project, root, verbose=not args.summary)
        total += sum(counts.values())

    print(f"\n{'=' * 72}\nTotal défauts : {total}\n{'=' * 72}")
    if args.strict and total:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
