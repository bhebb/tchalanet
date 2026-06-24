import { Injectable, Signal, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { catchError, distinctUntilChanged, filter, map, of, shareReplay, startWith, switchMap } from 'rxjs';

import { NavigationSection } from '@tch/api';
import { PageModelApi, PageRuntimeResponse, PrivateShellRuntime } from '@tch/page-model';

import { AuthSessionService } from '../../../core/auth/auth-session.service';
import {
  CASHIER_NAVIGATION,
  PLATFORM_NAVIGATION,
  TENANT_ADMIN_NAVIGATION,
  PrivateSpace,
} from './private-navigation.model';

@Injectable({ providedIn: 'root' })
export class PrivateShellService {
  private readonly auth = inject(AuthSessionService);
  private readonly api = inject(PageModelApi);
  private readonly router = inject(Router);

  readonly shellLoadError = signal(false);

  // Re-fetch the shell whenever the space changes (platform ↔ admin/cashier).
  // shareReplay(1) without defer avoids stale cache across logout/re-login cycles.
  readonly page$ = this.router.events.pipe(
    filter((e): e is NavigationEnd => e instanceof NavigationEnd),
    startWith(null),
    map(() => this.router.url),
    distinctUntilChanged((a, b) => urlToSpace(a) === urlToSpace(b)),
    switchMap(url => {
      if (url.startsWith('/app/platform')) return this.api.getPlatformPage();
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
    const sections = this.shell()?.navigationDrawer?.sections;
    if (sections?.length) return sections as NavigationSection[];
    const space = this.space();
    if (space === 'platform') return PLATFORM_NAVIGATION;
    if (space === 'admin') return TENANT_ADMIN_NAVIGATION;
    return CASHIER_NAVIGATION;
  });
}

function urlToSpace(url: string): 'platform' | 'other' {
  return url.startsWith('/app/platform') ? 'platform' : 'other';
}
