import { Injectable, Signal, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { catchError, distinctUntilChanged, filter, map, of, shareReplay, startWith, switchMap } from 'rxjs';

import { ActionItem, NavigationSection } from '@tch/api';
import { PageModelApi, PageRuntimeResponse, PrivateShellRuntime } from '@tch/page-model';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import { SupportAccessStore } from '../../../core/access/support-access.store';
import {
  CASHIER_NAVIGATION,
  PLATFORM_NAVIGATION,
  TENANT_ADMIN_NAVIGATION,
  PrivateSpace,
} from './private-navigation.model';

@Injectable({ providedIn: 'root' })
export class PrivateShellService {
  private readonly auth = inject(AuthSessionService);
  private readonly supportAccess = inject(SupportAccessStore);
  private readonly api = inject(PageModelApi);
  private readonly router = inject(Router);

  readonly shellLoadError = signal(false);

  // Re-fetch the shell whenever the space changes (platform ↔ admin/cashier).
  // shareReplay(1) without defer avoids stale cache across logout/re-login cycles.
  readonly page$ = this.router.events.pipe(
    filter((e): e is NavigationEnd => e instanceof NavigationEnd),
    startWith(null),
    map(() => this.router.url),
    distinctUntilChanged((a, b) => runtimeKey(a) === runtimeKey(b)),
    switchMap(url => {
      if (url.startsWith('/app/platform')) return this.api.getPlatformPage(platformLogicalId(url));
      if (url.startsWith('/app/admin') || url.startsWith('/app/cashier')) return this.api.getTenantPage();
      // /app/profile and other shared routes: fall back to role
      return this.auth.session().roles.includes('SUPER_ADMIN')
        ? this.api.getPlatformPage()
        : this.api.getTenantPage();
    }),
    shareReplay(1),
  );

  readonly page = toSignal<PageRuntimeResponse | undefined>(
    this.page$.pipe(
      catchError(() => {
        this.shellLoadError.set(true);
        return of(undefined);
      }),
    ),
    { initialValue: undefined },
  );

  readonly shell = computed<PrivateShellRuntime | undefined>(() => {
    const s = this.page()?.shell;
    return s?.type === 'private' ? (s as PrivateShellRuntime) : undefined;
  });

  private readonly navEnd = toSignal(
    this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)),
    { initialValue: null },
  );

  readonly space: Signal<PrivateSpace> = computed<PrivateSpace>(() => {
    this.navEnd();
    const url = this.router.url;
    if (url.startsWith('/app/platform')) return 'platform';
    if (url.startsWith('/app/admin')) return 'admin';
    return 'cashier';
  });

  readonly navigation: Signal<readonly NavigationSection[]> = computed(() => {
    const space = this.space();
    if (space === 'admin' && this.supportAccess.isActive()) return TENANT_ADMIN_NAVIGATION;
    const sections = this.shell()?.navigationDrawer?.sections;
    if (sections?.length) return space === 'platform'
      ? canonicalizePlatformNavigation(sections as NavigationSection[])
      : sections as NavigationSection[];
    if (space === 'platform') return PLATFORM_NAVIGATION;
    if (space === 'admin') return TENANT_ADMIN_NAVIGATION;
    return CASHIER_NAVIGATION;
  });
}

function runtimeKey(url: string): string {
  if (url.startsWith('/app/platform')) return `platform:${platformLogicalId(url)}`;
  return 'other';
}

function platformLogicalId(url: string): string {
  if (url.startsWith('/app/platform/dashboard')) return 'private.dashboard.superadmin';
  return 'private.dashboard.superadmin.ops';
}

function canonicalizePlatformNavigation(
  sections: readonly NavigationSection[],
): readonly NavigationSection[] {
  return sections.map(section => ({
    ...section,
    items: section.items.map(canonicalizePlatformAction),
  }));
}

function canonicalizePlatformAction(item: ActionItem): ActionItem {
  const children = item.children?.map(canonicalizePlatformAction);
  const value = item.destination?.value;
  const destination =
    item.destination?.kind === 'route' && value === '/app/platform/ops/health'
      ? { ...item.destination, value: '/app/platform' }
      : item.destination;

  return {
    ...item,
    destination,
    children,
  };
}
