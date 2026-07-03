/**
 * Parseurs de query params pour les pages liste/CRUD (URL = source de vérité).
 * Une seule implémentation partagée — évite que chaque page réinvente son parsing
 * (cf. feature-playbook §2 : params standard `q`/`status`/`sort`/`page`/`size`).
 */

/** Entier ≥ 0 (page, size). Valeur absente/invalide → `fallback`. */
export function numberParam(value: string | null | undefined, fallback: number): number {
  if (value === null || value === undefined || value.trim() === '') return fallback;
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed >= 0 ? parsed : fallback;
}

/** Date `YYYY-MM-DD` (filtres de date). Format invalide → `''`. */
export function dateParam(value: string | null | undefined): string {
  if (value === null || value === undefined) return '';
  return /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : '';
}

/** Texte trimé (recherche `q`). Absent → `''`. */
export function textParam(value: string | null | undefined): string {
  return value?.trim() ?? '';
}

/**
 * Valeur contrainte à un ensemble autorisé (statut, tri…). Hors-ensemble → `fallback`.
 * Remplace les gardes `isXxx()` recopiées dans chaque page.
 */
export function enumParam<T extends string>(
  value: string | null | undefined,
  allowed: readonly T[],
  fallback: T,
): T {
  return value != null && (allowed as readonly string[]).includes(value) ? (value as T) : fallback;
}
