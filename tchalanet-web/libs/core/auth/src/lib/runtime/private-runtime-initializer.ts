import { Injectable, inject } from '@angular/core';
import { TranslateService, TranslationObject } from '@ngx-translate/core';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { ThemeStore } from '@tch/ui/theme';
import { Observable, tap } from 'rxjs';

import { RuntimeBootstrapResponse } from './private-bootstrap.model';
import { PrivateBootstrapService } from './private-bootstrap.service';
import { PrivateBootstrapStore } from './private-bootstrap.store';

@Injectable({ providedIn: 'root' })
export class PrivateRuntimeInitializer {
  private readonly bootstrapApi = inject(PrivateBootstrapService);
  private readonly bootstrapStore = inject(PrivateBootstrapStore);
  private readonly translate = inject(TranslateService);
  private readonly theme = inject(ThemeStore);
  private readonly settings = inject(RuntimeSettingsStore);

  initialize(): Observable<RuntimeBootstrapResponse> {
    this.bootstrapStore.setLoading();
    return this.bootstrapApi.bootstrap().pipe(
      tap(response => {
        this.applyBootstrap(response);
      }),
    );
  }

  private applyBootstrap(response: RuntimeBootstrapResponse): void {
    this.mergeI18n(response);

    // Theme + settings come from the bootstrap payload — no separate runtime HTTP calls.
    this.theme.applyBootstrapTheme({
      presetCode: response.theme?.presetCode,
      mode: response.theme?.mode,
      tokens: response.theme?.tokens,
    });
    this.settings.applyBootstrapSettings({
      locale: response.settings?.locale ?? response.user.preferredLocale ?? 'fr',
      timezone: response.settings?.timezone ?? response.user.preferredTimezone ?? 'America/Toronto',
      currency: response.settings?.currency ?? 'HTG',
      features: response.settings?.features ?? {},
    });

    const partial = (response.notices?.length ?? 0) > 0;

    this.bootstrapStore.setBootstrap({
      space: response.space,
      user: response.user,
      tenantContext: response.tenantContext,
      entitlements: response.entitlements,
      readiness: response.readiness,
      notifications: response.notifications,
      navigationDrawer: response.navigationDrawer ?? null,
      pageModelRef: response.pageModelRef,
      partial,
    });
  }

  private mergeI18n(response: RuntimeBootstrapResponse): void {
    const locale =
      response.i18n?.locale ?? response.settings?.locale ?? response.user.preferredLocale;
    const messages = response.i18n?.messages ?? {};
    if (!locale) {
      return;
    }
    if (Object.keys(messages).length > 0) {
      this.translate.setTranslation(locale, messages as unknown as TranslationObject, true);
    }
  }
}
