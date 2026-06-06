import { BreakpointObserver } from '@angular/cdk/layout';
import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { distinctUntilChanged, map } from 'rxjs';

export const TCH_BREAKPOINTS = {
  handset: '(max-width: 767.98px)',
  tablet: '(min-width: 768px) and (max-width: 1023.98px)',
  desktop: '(min-width: 1024px)',
} as const;

@Injectable({ providedIn: 'root' })
export class TchBreakpointService {
  private readonly bp = inject(BreakpointObserver);
  private readonly handsetSignal = this.match(TCH_BREAKPOINTS.handset, false);
  private readonly tabletSignal = this.match(TCH_BREAKPOINTS.tablet, false);
  private readonly desktopSignal = this.match(TCH_BREAKPOINTS.desktop, true);

  readonly handset = computed(() => this.handsetSignal());
  readonly tablet = computed(() => this.tabletSignal());
  readonly desktop = computed(() => this.desktopSignal());
  readonly isHandset = this.handset;
  readonly isTablet = this.tablet;
  readonly isDesktop = this.desktop;

  private match(query: string, initialValue: boolean) {
    return toSignal(
      this.bp.observe(query).pipe(map((result) => result.matches), distinctUntilChanged()),
      { initialValue },
    );
  }
}
