import { HttpClient } from '@angular/common/http';
import {
  EnvironmentProviders,
  Injectable,
  InjectionToken,
  PLATFORM_ID,
  inject,
  makeEnvironmentProviders,
  provideAppInitializer,
  signal,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { firstValueFrom } from 'rxjs';

export type TchAppId = 'public-portal' | 'admin-portal' | 'platform-portal';

export interface TchFirebaseRuntimeConfig {
  readonly apiKey: string;
  readonly authDomain: string;
  readonly projectId: string;
  readonly storageBucket: string;
  readonly messagingSenderId: string;
  readonly appId: string;
  readonly measurementId?: string;
}

export interface TchRuntimeConfig {
  readonly appId: TchAppId;
  readonly production: boolean;
  readonly apiBaseUrl: string;
  readonly authBaseUrl: string;
  readonly assetsBaseUrl: string;
  readonly enableSandbox: boolean;
  readonly firebase: TchFirebaseRuntimeConfig;
  readonly firebaseAuthEmulatorUrl: string | null;
}

export interface TchRuntimeConfigProviderOptions {
  readonly fallback: TchRuntimeConfig;
  readonly runtimeConfigPath?: string | null;
}

export const TCH_RUNTIME_CONFIG_OPTIONS =
  new InjectionToken<TchRuntimeConfigProviderOptions>('TCH_RUNTIME_CONFIG_OPTIONS');

export const TCH_RUNTIME_CONFIG = new InjectionToken<TchRuntimeConfig>('TCH_RUNTIME_CONFIG', {
  factory: () => inject(TchRuntimeConfigStore).config(),
});

@Injectable({ providedIn: 'root' })
export class TchRuntimeConfigStore {
  private readonly configSignal = signal<TchRuntimeConfig>({
    appId: 'public-portal',
    production: true,
    apiBaseUrl: '/api/v1',
    authBaseUrl: '/auth',
    assetsBaseUrl: '/assets',
    enableSandbox: false,
    firebaseAuthEmulatorUrl: null,
    firebase: {
      apiKey: '',
      authDomain: '',
      projectId: '',
      storageBucket: '',
      messagingSenderId: '',
      appId: '',
    },
  });

  readonly config = this.configSignal.asReadonly();

  setConfig(config: TchRuntimeConfig): void {
    this.configSignal.set(config);
  }
}

@Injectable({ providedIn: 'root' })
export class TchRuntimeConfigLoader {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly options = inject(TCH_RUNTIME_CONFIG_OPTIONS);
  private readonly store = inject(TchRuntimeConfigStore);

  async load(): Promise<void> {
    const fallback = this.options.fallback;
    this.store.setConfig(fallback);

    if (!this.options.runtimeConfigPath || !isPlatformBrowser(this.platformId)) {
      return;
    }

    try {
      const override = await firstValueFrom(
        this.http.get<Partial<TchRuntimeConfig>>(this.options.runtimeConfigPath),
      );
      this.store.setConfig(mergeRuntimeConfig(fallback, override));
    } catch {
      this.store.setConfig(fallback);
    }
  }
}

export function provideTchRuntimeConfig(
  options: TchRuntimeConfigProviderOptions,
): EnvironmentProviders {
  return makeEnvironmentProviders([
    { provide: TCH_RUNTIME_CONFIG_OPTIONS, useValue: options },
    provideAppInitializer(() => inject(TchRuntimeConfigLoader).load()),
  ]);
}

function mergeRuntimeConfig(
  fallback: TchRuntimeConfig,
  override: Partial<TchRuntimeConfig>,
): TchRuntimeConfig {
  return {
    ...fallback,
    ...override,
    firebase: {
      ...fallback.firebase,
      ...(override.firebase ?? {}),
    },
  };
}
