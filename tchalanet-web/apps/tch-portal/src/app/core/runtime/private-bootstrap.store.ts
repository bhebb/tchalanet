import { Injectable, computed, signal } from '@angular/core';

import {
  AuthenticatedUserView,
  BootstrapStatus,
  EntitlementsView,
  PageModelRef,
  PrivateSpace,
  RuntimeNotificationSummary,
  RuntimeReadinessView,
  TenantContextView,
} from './private-bootstrap.model';

@Injectable({ providedIn: 'root' })
export class PrivateBootstrapStore {
  private readonly statusSignal = signal<BootstrapStatus>('idle');
  private readonly spaceSignal = signal<PrivateSpace | null>(null);
  private readonly userSignal = signal<AuthenticatedUserView | null>(null);
  private readonly tenantContextSignal = signal<TenantContextView | null>(null);
  private readonly entitlementsSignal = signal<EntitlementsView | null>(null);
  private readonly readinessSignal = signal<RuntimeReadinessView | null>(null);
  private readonly notificationsSignal = signal<RuntimeNotificationSummary | null>(null);
  private readonly pageModelRefSignal = signal<PageModelRef | null>(null);
  private readonly errorSignal = signal<unknown | null>(null);

  readonly status = this.statusSignal.asReadonly();
  readonly space = this.spaceSignal.asReadonly();
  readonly user = this.userSignal.asReadonly();
  readonly tenantContext = this.tenantContextSignal.asReadonly();
  readonly entitlements = this.entitlementsSignal.asReadonly();
  readonly readiness = this.readinessSignal.asReadonly();
  readonly notifications = this.notificationsSignal.asReadonly();
  readonly pageModelRef = this.pageModelRefSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly ready = computed(() => this.statusSignal() === 'ready' || this.statusSignal() === 'partial');
  readonly unreadCount = computed(() => this.notificationsSignal()?.unreadCount ?? 0);
  readonly criticalCount = computed(() => this.notificationsSignal()?.criticalCount ?? 0);

  setLoading(): void {
    this.statusSignal.set('loading');
    this.errorSignal.set(null);
  }

  setBootstrap(data: {
    space: PrivateSpace;
    user: AuthenticatedUserView;
    tenantContext: TenantContextView | null;
    entitlements: EntitlementsView;
    readiness: RuntimeReadinessView;
    notifications: RuntimeNotificationSummary;
    pageModelRef: PageModelRef;
    partial: boolean;
  }): void {
    this.spaceSignal.set(data.space);
    this.userSignal.set(data.user);
    this.tenantContextSignal.set(data.tenantContext);
    this.entitlementsSignal.set(data.entitlements);
    this.readinessSignal.set(data.readiness);
    this.notificationsSignal.set(data.notifications);
    this.pageModelRefSignal.set(data.pageModelRef);
    this.statusSignal.set(data.partial ? 'partial' : 'ready');
  }

  setError(error: unknown): void {
    this.errorSignal.set(error);
    this.statusSignal.set('error');
  }

  updateNotifications(summary: RuntimeNotificationSummary): void {
    this.notificationsSignal.set(summary);
  }

  reset(): void {
    this.statusSignal.set('idle');
    this.spaceSignal.set(null);
    this.userSignal.set(null);
    this.tenantContextSignal.set(null);
    this.entitlementsSignal.set(null);
    this.readinessSignal.set(null);
    this.notificationsSignal.set(null);
    this.pageModelRefSignal.set(null);
    this.errorSignal.set(null);
  }
}
