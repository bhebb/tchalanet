export type FeatureKey = string;
export type FeatureVariant = { name: string; payload?: any };

export interface FeatureContext {
  userId?: string;
  tenantId?: string;
  country?: string;
  appName?: string;
  [k: string]: unknown;
}

export interface FeatureClient {
  isEnabled(flag: FeatureKey, defaultValue?: boolean): boolean;
  getVariant(flag: FeatureKey): FeatureVariant | null;
  refresh(): Promise<void> | void;
  updateContext(ctx: Partial<FeatureContext>): void;
  changes$: import('rxjs').Observable<void>; // émet quand les flags changent
}
