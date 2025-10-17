import { effect, inject, Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { PageActions, selectError, selectIsLoading, selectPage } from '@tchl/data-access/page';
import { I18nFacade } from './i18n.facade';

@Injectable({ providedIn: 'root' })
export class PageFacade {
  private store = inject(Store);
  private i18nFacade = inject(I18nFacade);

  // ✅ Signals directement depuis le store
  loading = this.store.selectSignal(selectIsLoading);
  error = this.store.selectSignal(selectError);
  page = this.store.selectSignal(selectPage);

  constructor() {
    // ✅ effect() = réaction auto, pas besoin d’unsubscribe
    effect(() => {
      const p = this.page(); // lit le signal
      if (!p) return;

      const langs = p.langs ?? ['fr'];
      const current = p.currentLang ?? 'fr';
      const bundle = p.i18n ?? {};
      this.i18nFacade.initFromPage(langs, current, bundle);
    });
  }

  load(context: string, tenantId: string) {
    this.store.dispatch(PageActions.loadPage({ context, tenantId }));
  }

  fallback() {
    this.store.dispatch(PageActions.fallbackRequested());
  }
}
