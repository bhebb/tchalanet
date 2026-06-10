import { Injectable, Signal, computed, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

import { NavigationSection } from '@tch/api';

import {
  CASHIER_NAVIGATION,
  PLATFORM_NAVIGATION,
  TENANT_ADMIN_NAVIGATION,
  PrivateSpace,
} from './private-navigation.model';

@Injectable({ providedIn: 'root' })
export class PrivateShellService {
  private readonly router = inject(Router);

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
    if (space === 'platform') return PLATFORM_NAVIGATION;
    if (space === 'admin') return TENANT_ADMIN_NAVIGATION;
    return CASHIER_NAVIGATION;
  });
}
