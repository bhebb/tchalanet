import { FeatureClient, FeatureContext, FeatureVariant } from '../feature.types';
import { BehaviorSubject, Observable } from 'rxjs';

export class MemoryFeatureClient implements FeatureClient {
  private enabled = new Set<string>();
  private ctx: FeatureContext = {};
  private changes = new BehaviorSubject<void>(undefined);
  changes$: Observable<void> = this.changes.asObservable();

  constructor(initial: string[] = []) {
    initial.forEach(f => this.enabled.add(f));
  }

  isEnabled(flag: string, def = false) {
    return this.enabled.has(flag) || def;
  }

  getVariant(): FeatureVariant | null {
    return null;
  }

  refresh() {
    this.changes.next();
  }

  updateContext(ctx: Partial<FeatureContext>) {
    this.ctx = { ...this.ctx, ...ctx };
    this.refresh();
  }
}
