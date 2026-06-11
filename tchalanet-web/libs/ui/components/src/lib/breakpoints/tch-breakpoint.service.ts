import { BreakpointObserver } from '@angular/cdk/layout';
import { computed, inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { distinctUntilChanged, map } from 'rxjs';

// M3 window-size class boundaries — must stay in sync with _breakpoints.scss $bps.
export const TCH_BREAKPOINTS = {
  compact:    '(max-width: 599.98px)',
  medium:     '(min-width: 600px) and (max-width: 839.98px)',
  expanded:   '(min-width: 840px) and (max-width: 1199.98px)',
  large:      '(min-width: 1200px) and (max-width: 1599.98px)',
  extraLarge: '(min-width: 1600px)',
} as const;

@Injectable({ providedIn: 'root' })
export class TchBreakpointService {
  private readonly bp = inject(BreakpointObserver);

  readonly compact    = this.match(TCH_BREAKPOINTS.compact,    true);
  readonly medium     = this.match(TCH_BREAKPOINTS.medium,     false);
  readonly expanded   = this.match(TCH_BREAKPOINTS.expanded,   false);
  readonly large      = this.match(TCH_BREAKPOINTS.large,      false);
  readonly extraLarge = this.match(TCH_BREAKPOINTS.extraLarge, false);

  /** True for compact or medium — single-pane layouts. */
  readonly isNarrow = computed(() => this.compact() || this.medium());
  /** True for expanded and above — multi-pane layouts. */
  readonly isWide = computed(() => this.expanded() || this.large() || this.extraLarge());

  private match(query: string, initialValue: boolean) {
    return toSignal(
      this.bp.observe(query).pipe(map((r) => r.matches), distinctUntilChanged()),
      { initialValue },
    );
  }
}
