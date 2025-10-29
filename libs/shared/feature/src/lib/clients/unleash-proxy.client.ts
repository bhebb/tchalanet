import { UnleashClient } from 'unleash-proxy-client';

import { BehaviorSubject } from 'rxjs';

import { FeatureClient, FeatureContext, FeatureVariant } from '../feature.types';

export class UnleashProxyClient implements FeatureClient {
  private client: any;
  private changes = new BehaviorSubject<void>(undefined);
  changes$ = this.changes.asObservable();

  constructor(opts: {
    url: string;
    clientKey: string;
    appName: string;
    refreshInterval?: number;
    context?: FeatureContext;
  }) {
     this.client = new UnleashClient({
      url: opts.url,
      clientKey: opts.clientKey,
      appName: opts.appName,
      refreshInterval: opts.refreshInterval ?? 15,
      context: opts.context ?? {},
    });

    this.client.on('ready', () => this.changes.next());
    this.client.on('update', () => this.changes.next());
    this.client.start();
  }

  isEnabled(flag: string, def = false) {
    return this.client.isEnabled(flag) ?? def;
  }
  getVariant(flag: string): FeatureVariant | null {
    return this.client.getVariant(flag) ?? null;
  }
  async refresh() {
    await this.client.forceFetch();
    this.changes.next();
  }
  updateContext(ctx: Partial<FeatureContext>) {
    this.client.updateContext(ctx);
  }
}
