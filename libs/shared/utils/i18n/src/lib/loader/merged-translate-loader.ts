//create the loader because we need to merge the label keys, They can be like: home.nave or home:{nav}

import { Inject, Injectable, InjectionToken } from '@angular/core';
import { TranslateLoader, TranslationObject } from '@ngx-translate/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, forkJoin, map, Observable, of } from 'rxjs';
import { I18nMergerService } from '@tchl/utils/i18n';

// Définir un token d'injection pour les options
export const MERGED_TRANSLATE_LOADER_OPTIONS = new InjectionToken<MergedTranslateLoaderOptions>(
  'MERGED_TRANSLATE_LOADER_OPTIONS',
);

// Interface pour les options
export interface MergedTranslateLoaderOptions {
  assetsPrefix?: string; // default: '/assets/i18n/'
  backendPath?: string; // e.g. '/api/i18n/'
}

@Injectable()
export class MergedTranslateLoader implements TranslateLoader {
  constructor(
    private http: HttpClient,
    private i18nMergerService: I18nMergerService,
    @Inject(MERGED_TRANSLATE_LOADER_OPTIONS)
    private opts: MergedTranslateLoaderOptions = {},
  ) {}

  getTranslation(lang: string): Observable<TranslationObject> {
    const assets$ = this.http.get<Record<string, any>>(`${this.opts.assetsPrefix}${lang}.json`);
    const backend$ = this.opts.backendPath
      ? this.http
          .get<Record<string, any>>(`${this.opts.backendPath}?lang=${lang}`)
          .pipe(
            catchError((err: HttpErrorResponse) => {
              console.error(`Error loading translation for ${lang}:`, err);
              return of({});
            }),
          )
      : of({});

    // Merge with backend overriding assets
    return forkJoin([assets$, backend$]).pipe(map(([a, b]) => this.i18nMergerService.merge(a, b)));
  }
}
