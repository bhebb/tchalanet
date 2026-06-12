import { Injectable, computed, signal } from '@angular/core';

import { BootstrapStatus, PageModelRef } from './private-bootstrap.model';
import {
  PublicNavigationModel,
  PublicReadinessView,
  PublicSettingsView,
  PublicThemeView,
} from './public-bootstrap.model';

@Injectable({ providedIn: 'root' })
export class PublicBootstrapStore {
  private readonly statusSignal = signal<BootstrapStatus>('idle');
  private readonly settingsSignal = signal<PublicSettingsView | null>(null);
  private readonly themeSignal = signal<PublicThemeView | null>(null);
  private readonly navigationSignal = signal<PublicNavigationModel | null>(null);
  private readonly readinessSignal = signal<PublicReadinessView | null>(null);
  private readonly pageModelRefSignal = signal<PageModelRef | null>(null);
  private readonly errorSignal = signal<unknown | null>(null);
  private readonly isOfflineFallbackSignal = signal<boolean>(false);

  readonly status = this.statusSignal.asReadonly();
  readonly settings = this.settingsSignal.asReadonly();
  readonly theme = this.themeSignal.asReadonly();
  readonly navigation = this.navigationSignal.asReadonly();
  readonly readiness = this.readinessSignal.asReadonly();
  readonly pageModelRef = this.pageModelRefSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly isOfflineFallback = this.isOfflineFallbackSignal.asReadonly();

  readonly ready = computed(
    () => this.statusSignal() === 'ready' || this.statusSignal() === 'partial',
  );

  setLoading(): void {
    this.statusSignal.set('loading');
    this.errorSignal.set(null);
  }

  setBootstrap(data: {
    settings: PublicSettingsView;
    theme: PublicThemeView;
    navigation: PublicNavigationModel;
    readiness: PublicReadinessView;
    pageModelRef: PageModelRef;
    partial: boolean;
  }): void {
    this.settingsSignal.set(data.settings);
    this.themeSignal.set(data.theme);
    this.navigationSignal.set(data.navigation);
    this.readinessSignal.set(data.readiness);
    this.pageModelRefSignal.set(data.pageModelRef);
    this.statusSignal.set(data.partial ? 'partial' : 'ready');
  }

  setError(error: unknown): void {
    this.errorSignal.set(error);
    this.statusSignal.set('error');
  }

  setOfflineFallback(): void {
    this.isOfflineFallbackSignal.set(true);
  }

  reset(): void {
    this.statusSignal.set('idle');
    this.settingsSignal.set(null);
    this.themeSignal.set(null);
    this.navigationSignal.set(null);
    this.readinessSignal.set(null);
    this.pageModelRefSignal.set(null);
    this.errorSignal.set(null);
    this.isOfflineFallbackSignal.set(false);
  }
}
