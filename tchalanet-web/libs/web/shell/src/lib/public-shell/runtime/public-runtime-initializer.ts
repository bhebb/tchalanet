import { Injectable, inject } from '@angular/core';
import { TranslateService, TranslationObject } from '@ngx-translate/core';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { ThemeStore } from '@tch/ui/theme';
import { Observable, catchError, map, tap } from 'rxjs';

import { PublicBootstrapResponse } from './public-bootstrap.model';
import { PublicBootstrapService } from './public-bootstrap.service';
import { PublicBootstrapStore } from './public-bootstrap.store';
import { PublicFallbackBundleService } from './public-fallback-bundle.service';

@Injectable({ providedIn: 'root' })
export class PublicRuntimeInitializer {
  private readonly bootstrapApi = inject(PublicBootstrapService);
  private readonly bootstrapStore = inject(PublicBootstrapStore);
  private readonly fallback = inject(PublicFallbackBundleService);
  private readonly translate = inject(TranslateService);
  private readonly theme = inject(ThemeStore);
  private readonly settings = inject(RuntimeSettingsStore);

  initialize(locale?: string): Observable<PublicBootstrapResponse> {
    this.bootstrapStore.setLoading();
    return this.bootstrapApi.bootstrap(locale).pipe(
      catchError(() =>
        this.fallback.load().pipe(
          tap(() => this.bootstrapStore.setOfflineFallback()),
          map(bundle => bundle.publicBootstrap),
        ),
      ),
      tap(response => this.applyBootstrap(response)),
    );
  }

  private applyBootstrap(response: PublicBootstrapResponse): void {
    this.mergeI18n(response);

    // Theme + settings come from the bootstrap payload — no separate runtime HTTP calls.
    // Public theme carries mode/colors only (no preset/tokens), so apply the default preset + mode.
    this.theme.applyBootstrapTheme({ mode: response.theme.mode, tokens: null });
    this.settings.applyBootstrapSettings({
      locale: response.settings.locale,
      timezone: response.settings.timezone,
      currency: response.settings.defaultCurrency,
      features: response.settings.features,
    });

    const partial = (response.notices?.length ?? 0) > 0;

    this.bootstrapStore.setBootstrap({
      settings: response.settings,
      theme: response.theme,
      navigation: response.navigation,
      readiness: response.readiness,
      pageModelRef: response.pageModelRef,
      partial,
    });
  }

  /** Overlay bootstrap-delivered i18n on top of the local fallback bundle (ngx-translate merge). */
  private mergeI18n(response: PublicBootstrapResponse): void {
    const lang = response.i18n.lang;
    const messages = response.i18n.messages;
    if (lang && Object.keys(messages).length > 0) {
      this.translate.setTranslation(lang, messages as unknown as TranslationObject, true);
    }
  }
}
