import { Injectable } from '@angular/core';
export interface MergeOptions {
  arrayStrategy?: 'replace' | 'merge';   // default: 'replace'
  nullStrategy?: 'keep' | 'ignore' | 'delete'; // default: 'keep'
  escapeDot?: boolean;                   // default: false (active si tu as des clés avec des points)
}

const FORBIDDEN = new Set(['__proto__', 'prototype', 'constructor']);


@Injectable({ providedIn: 'root' })
export class I18nMergerService {
  /**
   * Flatten un objet hiérarchisé en clés pointées ("a.b.c": value).
   */
  flatten(obj: any, prefix = '', out: Record<string, any> = {}): Record<string, any> {
    Object.keys(obj ?? {}).forEach(key => {
      const val = obj[key];
      const p = prefix ? `${prefix}.${key}` : key;
      if (val && typeof val === 'object' && !Array.isArray(val)) {
        this.flatten(val, p, out);
      } else {
        out[p] = val;
      }
    });
    return out;
  }

  /**
   * Recompose un objet hiérarchisé depuis des clés pointées.
   */
  unflatten(flat: Record<string, any>): any {
    const result: any = {};
    for (const [path, value] of Object.entries(flat)) {
      const parts = path.split('.');
      let cur = result;
      parts.forEach((part, idx) => {
        if (idx === parts.length - 1) {
          cur[part] = value;
        } else {
          cur[part] ??= {};
          cur = cur[part];
        }
      });
    }
    return result;
  }

  /**
   * Fusionne assets (hiérarchisés) et backend (plat ou hiérarchisé).
   * Backend est toujours prioritaire en cas de conflit.
   */
  merge(assets: any, backend: any): any {
    const assetsFlat = this.flatten(assets);
    const backendFlat = this.flatten(backend);

    // Merge avec priorité backend
    const mergedFlat = { ...assetsFlat, ...backendFlat };

    return this.unflatten(mergedFlat);
  }

  /**
   * (Optionnel) Détecter et logger les clés écrasées par le backend.
   */
  logConflicts(assets: any, backend: any, lang: string) {
    const assetsFlat = this.flatten(assets);
    const backendFlat = this.flatten(backend);

    const conflicts: string[] = [];
    for (const key of Object.keys(backendFlat)) {
      if (key in assetsFlat && assetsFlat[key] !== backendFlat[key]) {
        conflicts.push(key);
      }
    }
    if (conflicts.length) {
      console.warn(`[i18n:${lang}] backend overrides:`, conflicts);
    }
  }
}
