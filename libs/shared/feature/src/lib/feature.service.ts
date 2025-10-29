import { map, startWith } from 'rxjs';

import { inject, Injectable } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { FEATURE_CLIENT } from './feature.tokens';
import { FeatureClient, FeatureContext } from './feature.types';

@Injectable({ providedIn: 'root' })
export class FeatureService {
  private client = inject<FeatureClient>(FEATURE_CLIENT);
  readonly bump = toSignal(
    this.client.changes$.pipe(
      startWith(void 0),
      map(() => Date.now()),
    ),
    { initialValue: 0 },
  );

  isEnabled = (flag?: string, def = true) => {
    if (!flag) return true;
    this.bump();
    return this.client.isEnabled(flag, def);
  };

  updateContext(ctx: Partial<FeatureContext>) {
    this.client.updateContext(ctx);
  }
  refresh() {
    return this.client.refresh();
  }
}
