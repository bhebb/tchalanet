// libs/feature/src/lib/feature.provider.ts
import { Provider } from '@angular/core';

import { MemoryFeatureClient } from './clients/memory.client';
import { UnleashProxyClient } from './clients/unleash-proxy.client';
import { FEATURE_CLIENT, FEATURE_CONTEXT, FEATURE_INITIAL } from './feature.tokens';

export type FeatureProviderConfig =
  | { kind: 'memory'; initial?: string[] }
  | { kind: 'unleash'; url: string; clientKey: string; appName: string; refreshInterval?: number };

export function provideFeatureClient(cfg: FeatureProviderConfig): Provider[] {
  return [
    {
      provide: FEATURE_CLIENT,
      deps: [FEATURE_INITIAL, FEATURE_CONTEXT],
      useFactory: (initial: string[] | null, ctx: any) => {
        if (cfg.kind === 'unleash') {
          return new UnleashProxyClient({ ...cfg, context: ctx || {} });
        }
        return new MemoryFeatureClient(initial ?? []);
      }
    }
  ];
}
