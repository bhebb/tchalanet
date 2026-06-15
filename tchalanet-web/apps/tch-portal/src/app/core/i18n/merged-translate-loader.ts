import { HttpClient } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { TranslateLoader } from '@ngx-translate/core';
import { ApiResponse } from '@tch/api';
import { Observable, catchError, of } from 'rxjs';

import { TranslationTree } from '../../shared/types';

export interface I18nBundleResponse {
  readonly locale?: string;
  readonly surfaces?: Record<string, Record<string, string>>;
  readonly translations?: TranslationTree;
}

export interface MergedTranslateLoaderOptions {
  readonly assetsPrefix: string;
  readonly assetsSuffix: string;
}

export const MERGED_TRANSLATE_LOADER_OPTIONS = new InjectionToken<MergedTranslateLoaderOptions>(
  'MERGED_TRANSLATE_LOADER_OPTIONS',
);

/**
 * Local-only translate loader. It loads the bundled `fr/en/ht` fallback for a language.
 *
 * Backend translations are no longer fetched here: they are delivered inside the runtime bootstrap
 * response (`/public/runtime/bootstrap`, `/runtime/private`) and overlaid on top via
 * `TranslateService.setTranslation(lang, messages, shouldMerge=true)` by the runtime initializers.
 * Local bundles remain only as an offline fallback.
 */
@Injectable()
export class MergedTranslateLoader implements TranslateLoader {
  private readonly http = inject(HttpClient);
  private readonly options = inject(MERGED_TRANSLATE_LOADER_OPTIONS);

  getTranslation(lang: string): Observable<TranslationTree> {
    return this.http
      .get<TranslationTree>(
        `${this.options.assetsPrefix}${encodeURIComponent(lang)}${this.options.assetsSuffix}`,
      )
      .pipe(catchError(() => of<TranslationTree>({})));
  }
}

/**
 * Normalize a backend i18n bundle (surface-grouped or flat) into a flat translation tree.
 * Retained as a pure helper: the bootstrap path can reuse it to flatten surface bundles.
 */
export function normalizeBackendTranslations(
  response: ApiResponse<I18nBundleResponse> | I18nBundleResponse | TranslationTree,
  surfaceOrder: readonly string[] = [],
): TranslationTree {
  const payload = isApiResponse(response) ? response.data : response;
  if (!isRecord(payload)) {
    return {};
  }

  if (isRecord(payload['translations'])) {
    return payload['translations'] as TranslationTree;
  }

  if (isRecord(payload['surfaces'])) {
    return flattenSurfaceBundle(payload['surfaces'], surfaceOrder);
  }

  return payload as TranslationTree;
}

function flattenSurfaceBundle(
  surfaces: Record<string, unknown>,
  surfaceOrder: readonly string[],
): TranslationTree {
  const orderedSurfaces =
    surfaceOrder.length > 0
      ? surfaceOrder.map(surface => surfaces[surface])
      : Object.values(surfaces);

  return orderedSurfaces.reduce<TranslationTree>((accumulator, surfaceTranslations) => {
    if (!isRecord(surfaceTranslations)) {
      return accumulator;
    }

    return {
      ...accumulator,
      ...(surfaceTranslations as Record<string, string>),
    };
  }, {});
}

function isApiResponse(value: unknown): value is ApiResponse<I18nBundleResponse> {
  return isRecord(value) && isRecord(value['data']);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
