import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { catchError, defer, of } from 'rxjs';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { LanguageSwitcherComponent } from '../../../core/i18n';
import { PageModelApi } from '../../../core/pagemodel';
import { AppRuntimeStore } from '../../../core/runtime';
import { NavigationDestination, PrivateShellRuntime } from '../../../shared/types';

@Component({
  imports: [LanguageSwitcherComponent, RouterLink, RouterOutlet, TranslatePipe],
  selector: 'tch-private-shell-page',
  template: `
    <div class="private-shell">
      <header class="top-app-bar">
        <a routerLink="/public" class="brand">Tchalanet</a>
        @if (titleKey()) {
          <strong>{{ titleKey() | translate }}</strong>
        }
        <div class="utilities">
          <tch-language-switcher />
          <button type="button" (click)="logout()">
            {{ 'dashboard.actions.logout' | translate }}
          </button>
        </div>
      </header>

      <div class="workspace">
        <nav class="side-nav" aria-label="Private navigation">
          @for (item of destinations(); track item.id) {
            <a [routerLink]="item.destination.value">{{ item.labelKey | translate }}</a>
          }
        </nav>

        <main class="content">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [
    `
      .private-shell {
        min-height: 100vh;
      }

      .top-app-bar {
        align-items: center;
        border-bottom: 1px solid var(--tch-color-border, #d8dee6);
        display: flex;
        justify-content: space-between;
        min-height: 4rem;
        padding: 0 1.5rem;
      }

      .brand {
        color: inherit;
        font-weight: 700;
        text-decoration: none;
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

      .side-nav {
        border-right: 1px solid var(--tch-color-border, #d8dee6);
        display: flex;
        flex-direction: column;
        gap: 0.5rem;
        padding: 1.5rem;
      }

      .side-nav a {
        color: inherit;
        text-decoration: none;
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

        .side-nav {
          border-bottom: 1px solid var(--tch-color-border, #d8dee6);
          border-right: 0;
          flex-direction: row;
          flex-wrap: wrap;
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
  readonly titleKey = computed(() => readTitleKey(this.shell()?.topAppBar));
  readonly destinations = computed(() => {
    const resolved = readDestinations(this.shell()?.navigationDrawer);
    return resolved.length ? resolved : fallbackDestinations(this.auth.session().roles);
  });

  constructor() {
    this.runtime.initPrivateRuntime();
  }

  logout(): void {
    void this.auth.logout();
  }
}

interface DrawerDestination {
  readonly id: string;
  readonly labelKey: string;
  readonly destination: NavigationDestination & { readonly kind: 'route' };
}

function readTitleKey(topAppBar: Readonly<Record<string, unknown>> | undefined): string {
  const title = recordValue(topAppBar?.['title']);
  return stringValue(title?.['labelKey']) ?? '';
}

function readDestinations(drawer: Readonly<Record<string, unknown>> | undefined): readonly DrawerDestination[] {
  const direct = destinationArray(drawer?.['topDestinations']);
  const sections = Array.isArray(drawer?.['sections']) ? drawer['sections'] : [];
  const grouped = sections.flatMap((section) => {
    const record = recordValue(section);
    return destinationArray(record?.['destinations']);
  });
  const footer = destinationArray(drawer?.['footerDestinations']);
  return [...direct, ...grouped, ...footer];
}

function destinationArray(value: unknown): readonly DrawerDestination[] {
  return Array.isArray(value) ? value.flatMap(readDestination) : [];
}

function readDestination(value: unknown): readonly DrawerDestination[] {
  const record = recordValue(value);
  const destination = recordValue(record?.['destination']);
  const id = stringValue(record?.['id']);
  const labelKey = stringValue(record?.['labelKey']);
  const kind = stringValue(destination?.['kind']);
  const route = stringValue(destination?.['value']);
  return id && labelKey && kind === 'route' && route
    ? [{ id, labelKey, destination: { kind: 'route', value: route } }]
    : [];
}

function recordValue(value: unknown): Readonly<Record<string, unknown>> | undefined {
  return value && typeof value === 'object' && !Array.isArray(value)
    ? value as Readonly<Record<string, unknown>>
    : undefined;
}

function stringValue(value: unknown): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

function fallbackDestinations(roles: readonly string[]): readonly DrawerDestination[] {
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
