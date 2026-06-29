import { Injectable } from '@angular/core';
import { TranslationTree, TranslationValue } from './i18n.types';

type FlatTranslations = Record<string, TranslationValue>;

const forbiddenPathParts = new Set(['__proto__', 'prototype', 'constructor']);

function isTranslationTree(value: TranslationValue): value is TranslationTree {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

@Injectable({ providedIn: 'root' })
export class I18nMergerService {
  merge(localTranslations: TranslationTree, backendOverrides: TranslationTree): TranslationTree {
    const localFlat = this.flatten(localTranslations);
    const backendFlat = this.flatten(backendOverrides);

    return this.unflatten({
      ...localFlat,
      ...backendFlat,
    });
  }

  private flatten(
    translations: TranslationTree,
    prefix = '',
    output: FlatTranslations = {},
  ): FlatTranslations {
    Object.entries(translations).forEach(([key, value]) => {
      if (forbiddenPathParts.has(key)) {
        return;
      }

      const nextKey = prefix ? `${prefix}.${key}` : key;
      if (isTranslationTree(value)) {
        this.flatten(value, nextKey, output);
        return;
      }

      output[nextKey] = value;
    });

    return output;
  }

  private unflatten(translations: FlatTranslations): TranslationTree {
    const result: Record<string, TranslationValue> = {};

    Object.entries(translations).forEach(([path, value]) => {
      const parts = path.split('.').filter(part => !forbiddenPathParts.has(part));
      if (parts.length === 0) {
        return;
      }

      let cursor = result;
      parts.forEach((part, index) => {
        if (index === parts.length - 1) {
          cursor[part] = value;
          return;
        }

        const next = cursor[part];
        if (!isTranslationTree(next)) {
          cursor[part] = {};
        }

        cursor = cursor[part] as Record<string, TranslationValue>;
      });
    });

    return result;
  }
}
