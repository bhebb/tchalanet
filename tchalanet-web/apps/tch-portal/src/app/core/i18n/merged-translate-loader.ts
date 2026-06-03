import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, InjectionToken, inject } from '@angular/core';
import { TranslateLoader } from '@ngx-translate/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';

import { ApiResponse, TranslationTree } from '../../shared/types';
import { I18nMergerService } from './i18n-merger.service';

export interface I18nBundleResponse {
  readonly locale?: string;
  readonly surfaces?: Record<string, Record<string, string>>;
  readonly translations?: TranslationTree;
}

export interface MergedTranslateLoaderOptions {
  readonly assetsPrefix: string;
  readonly assetsSuffix: string;
  readonly backendPath: string;
  readonly surfaces: readonly string[];
}

export const MERGED_TRANSLATE_LOADER_OPTIONS =
  new InjectionToken<MergedTranslateLoaderOptions>('MERGED_TRANSLATE_LOADER_OPTIONS');

@Injectable()
export class MergedTranslateLoader implements TranslateLoader {
  private readonly http = inject(HttpClient);
  private readonly merger = inject(I18nMergerService);
  private readonly options = inject(MERGED_TRANSLATE_LOADER_OPTIONS);

  getTranslation(lang: string): Observable<TranslationTree> {
    return forkJoin({
      local: this.loadLocal(lang),
      backend: this.loadBackendOverrides(lang),
    }).pipe(map(({ local, backend }) => this.merger.merge(local, backend)));
  }

  private loadLocal(lang: string): Observable<TranslationTree> {
    return this.http
      .get<TranslationTree>(
        `${this.options.assetsPrefix}${encodeURIComponent(lang)}${this.options.assetsSuffix}`,
      )
      .pipe(catchError(() => of({})));
  }

  private loadBackendOverrides(lang: string): Observable<TranslationTree> {
    let params = new HttpParams().set('locale', lang);
    this.options.surfaces.forEach((surface) => {
      params = params.append('surface', surface);
    });

    return this.http
      .get<ApiResponse<I18nBundleResponse> | I18nBundleResponse | TranslationTree>(
        this.options.backendPath,
        { params },
      )
      .pipe(
        map((response) => normalizeBackendTranslations(response)),
        catchError(() => of({})),
      );
  }
}

export function normalizeBackendTranslations(
  response: ApiResponse<I18nBundleResponse> | I18nBundleResponse | TranslationTree,
): TranslationTree {
  const payload = isApiResponse(response) ? response.data : response;
  if (!isRecord(payload)) {
    return {};
  }

  if (isRecord(payload['translations'])) {
    return payload['translations'] as TranslationTree;
  }

  if (isRecord(payload['surfaces'])) {
    return flattenSurfaceBundle(payload['surfaces']);
  }

  return payload as TranslationTree;
}

function flattenSurfaceBundle(surfaces: Record<string, unknown>): TranslationTree {
  return Object.values(surfaces).reduce<TranslationTree>((accumulator, surfaceTranslations) => {
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
