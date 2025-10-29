import { distinctUntilChanged, map } from 'rxjs';

import { computed,inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { BreakpointObserver } from '@angular/cdk/layout';

// MQ constants que tu avais déjà
export const MQ = {
  HANDSET: '(max-width: 767.98px)',
  TABLET: '(min-width: 768px) and (max-width: 1023.98px)',
  DESKTOP: '(min-width: 1024px)',
};

@Injectable({ providedIn: 'root' })
export class TchBreakpointService {
  private bp = inject(BreakpointObserver);

  private _handset = toSignal(
    this.bp.observe(MQ.HANDSET).pipe(
      map(r => r.matches),
      distinctUntilChanged()
    ),
    { initialValue: false },
  );

  private _tablet = toSignal(
    this.bp.observe(MQ.TABLET).pipe(
      map(r => r.matches),
      distinctUntilChanged()
    ),
    { initialValue: false },
  );

  private _desktop = toSignal(
    this.bp.observe(MQ.DESKTOP).pipe(
      map(r => r.matches),
      distinctUntilChanged()
    ),
    { initialValue: false },
  );

  readonly handset = computed(() => this._handset());
  readonly tablet = computed(() => this._tablet());
  readonly desktop = computed(() => this._desktop());

  // syntactic sugar
  readonly isHandset = this.handset;
  readonly isTablet = this.tablet;
  readonly isDesktop = this.desktop;
}
