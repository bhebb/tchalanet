import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ActionItem } from '@tch/api';
import { TchBrand, TchLangThemeGroup, TchSidebarNav } from '@tch/ui/components';
import { ThemeSwitcherComponent } from '@tch/ui/theme';
import { catchError, defer, of } from 'rxjs';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcher } from '../../../core/i18n';
import { AppRuntimeStore } from '../../../core/runtime';
import { PageModelApi, PrivateShellRuntime } from '@tch/page-model';

@Component({
  imports: [
    LanguageSwitcher,
    RouterOutlet,
    TchBrand,
    TchLangThemeGroup,
    TchSidebarNav,
    ThemeSwitcherComponent,
    TranslatePipe,
  ],
  selector: 'tch-private-shell-page',
  template: `
    <div class="private-shell">
      <header class="top-app-bar">
        <tch-brand [brand]="shell()?.navigationDrawer?.brand" />
        @if (titleKey()) {
          <strong>{{ titleKey() | translate }}</strong>
        }
        <div class="utilities">
          <tch-lang-theme-group>
            <tch-language-switcher />
            <tch-theme-switcher />
          </tch-lang-theme-group>
          <button type="button" (click)="logout()">
            {{ 'dashboard.actions.logout' | translate }}
          </button>
        </div>
      </header>

      <div class="workspace">
        <tch-sidebar-nav
          [primary]="primary()"
          [sections]="shell()?.navigationDrawer?.sections ?? []"
          [secondary]="shell()?.navigationDrawer?.secondary ?? []"
        />

        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [
    `
      .private-shell {
        --comp-private-border: var(--tch-color-outline-variant);
        min-height: 100vh;
      }

      .top-app-bar {
        align-items: center;
        border-bottom: 1px solid var(--comp-private-border);
        display: flex;
        justify-content: space-between;
        min-height: 4rem;
        padding: 0 1.5rem;
      }

      .utilities {
        align-items: center;
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        justify-content: flex-end;
      }

      .workspace {
        display: grid;
        grid-template-columns: minmax(12rem, 16rem) 1fr;
        min-height: calc(100vh - 4rem);
      }

      .content {
        min-width: 0;
      }

      @media (max-width: 720px) {
        .top-app-bar {
          align-items: flex-start;
          flex-direction: column;
          gap: 0.75rem;
          padding-block: 1rem;
        }

        .workspace {
          grid-template-columns: 1fr;
        }

      }
    `,
  ],
})
export class PrivateShellPage {
  private readonly auth = inject(AuthSessionService);
  private readonly runtime = inject(AppRuntimeStore);
  private readonly pageModelApi = inject(PageModelApi);

  private readonly pageRuntime = toSignal(
    defer(() =>
      this.auth.session().roles.includes('SUPER_ADMIN')
        ? this.pageModelApi.getPlatformPage()
        : this.pageModelApi.getTenantPage(),
    ).pipe(catchError(() => of(undefined))),
    { initialValue: undefined },
  );

  readonly shell = computed<PrivateShellRuntime | undefined>(() => {
    const shell = this.pageRuntime()?.shell;
    return shell?.type === 'private' ? shell : undefined;
  });
  readonly titleKey = computed(() => this.shell()?.topAppBar.titleKey ?? '');
  readonly primary = computed(() => {
    const resolved = this.shell()?.navigationDrawer.primary ?? [];
    return resolved.length ? resolved : fallbackDestinations(this.auth.session().roles);
  });

  constructor() {
    this.runtime.initPrivateRuntime();
  }

  logout(): void {
    void this.auth.logout();
  }
}

function fallbackDestinations(roles: readonly string[]): readonly ActionItem[] {
  const route = roles.includes('SUPER_ADMIN')
    ? '/app/platform'
    : roles.includes('TENANT_ADMIN')
      ? '/app/admin'
      : '/app/cashier';
  const labelKey = roles.includes('SUPER_ADMIN')
    ? 'dashboard.titles.platform'
    : roles.includes('TENANT_ADMIN')
      ? 'dashboard.titles.admin'
      : 'dashboard.titles.cashier';
  return [{ id: 'dashboard', labelKey, destination: { kind: 'route', value: route } }];
}
