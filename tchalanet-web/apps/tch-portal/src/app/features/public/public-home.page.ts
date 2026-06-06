import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';
import { TchErrorPanel, TchLoading } from '@tch/shared/ui';
import { catchError, map, of, startWith } from 'rxjs';

import { PageModelApi } from '../../core/pagemodel';
import { ActionItem, PageShell } from '../../shared/types';
import { PageModelComponent } from '../pagemodel/page-model.component';
import { PublicShellComponent } from './shell/public-shell.component';

type PublicHomeState =
  | { readonly status: 'loading' }
  | { readonly status: 'error' }
  | {
      readonly status: 'ready';
      readonly response: import('../../shared/types').PublicPageModelResponse;
    };

/**
 * Public home page. Loads the `public.home` PageModel from the backend and renders it through the
 * widget engine inside the public shell. Works without an authenticated session.
 */
@Component({
  selector: 'tch-public-home-page',
  imports: [PublicShellComponent, PageModelComponent, TranslatePipe, TchLoading, TchErrorPanel],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
      <tch-page-shell [shell]="shell()">
          @switch (state().status) {
              @case ('loading') {
                  <tch-loading [label]="'common.loading' | translate"/>
              }
              @case ('error') {
                  <tch-error-panel
                          [title]="'common.error.title' | translate"
                          [message]="'public.home.loadError' | translate"
                  />
              }
              @case ('ready') {
                  <tch-page-model [pageModel]="pageModel()!" [dynamic]="dynamic()"/>
              }
          }
      </tch-page-shell>
  `,
})
export class PublicHomePage {
  private readonly api = inject(PageModelApi);

  readonly state = toSignal(
    this.api.getPublicPage('public.home').pipe(
      map(response => ({ status: 'ready', response }) as PublicHomeState),
      catchError(() => of({ status: 'error' } as PublicHomeState)),
      startWith({ status: 'loading' } as PublicHomeState),
    ),
    { initialValue: { status: 'loading' } as PublicHomeState },
  );

  readonly pageModel = computed(() => {
    const s = this.state();
    return s.status === 'ready' ? s.response.pageModel : undefined;
  });

  readonly dynamic = computed(() => {
    const s = this.state();
    return s.status === 'ready' ? s.response.dynamic : undefined;
  });

  readonly shell = computed<PageShell | undefined>(() => {
    const pageShell = this.pageModel()?.shell;
    const widgets = this.dynamic()?.widgets;
    const header = readShellFragment(widgets?.['shell.header']);
    const footer = readShellFragment(widgets?.['shell.footer']);

    if (!header && !footer) {
      return pageShell;
    }

    return {
      ...pageShell,
      brand: header?.brand,
      primary: header?.primary,
      actions: header?.secondary ?? header?.actions,
      mobile: header?.primary,
      footer: {
        ...pageShell?.footer,
        props: footer
          ? {
              descriptionKey: footer.descriptionKey,
              statusKey: footer.statusKey,
              copyrightKey: footer.copyrightKey,
              columns: footer.columns,
            }
          : pageShell?.footer?.props,
        nav: {
          primary: footer?.primary,
          secondary: footer?.secondary,
        },
      },
    };
  });
}

interface ShellFragmentPayload {
  readonly brand?: ActionItem;
  readonly primary?: readonly ActionItem[];
  readonly secondary?: readonly ActionItem[];
  readonly actions?: readonly ActionItem[];
  readonly columns?: readonly FooterColumnPayload[];
  readonly descriptionKey?: string;
  readonly statusKey?: string;
  readonly copyrightKey?: string;
}

interface FooterColumnPayload {
  readonly titleKey: string;
  readonly links: readonly ActionItem[];
}

function readShellFragment(value: unknown): ShellFragmentPayload | undefined {
  if (!value || typeof value !== 'object') {
    return undefined;
  }

  const record = value as ShellFragmentPayload & Record<string, unknown>;
  return {
    brand: isActionItem(record.brand) ? record.brand : undefined,
    primary: actionItems(record.primary),
    secondary: actionItems(record.secondary),
    actions: actionItems(record.actions),
    columns: footerColumns(record['columns']),
    descriptionKey: stringValue(record['descriptionKey']),
    statusKey: stringValue(record['statusKey']),
    copyrightKey: stringValue(record['copyrightKey']),
  };
}

function actionItems(value: unknown): readonly ActionItem[] | undefined {
  return Array.isArray(value) ? value.filter(isActionItem) : undefined;
}

function isActionItem(value: unknown): value is ActionItem {
  return !!value && typeof value === 'object' && typeof (value as ActionItem).id === 'string';
}

function footerColumns(value: unknown): readonly FooterColumnPayload[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }
  return value.flatMap((column) => {
    if (!column || typeof column !== 'object') {
      return [];
    }
    const record = column as Record<string, unknown>;
    const titleKey = stringValue(record['titleKey']);
    const links = actionItems(record['links']);
    return titleKey && links ? [{ titleKey, links }] : [];
  });
}

function stringValue(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}
