import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '@ngx-translate/core';

import {
  PlatformAdminApi,
  TenantProvisioningPreviewView,
  TenantProvisioningProfile,
  TenantProvisioningRequest,
  TenantProvisioningResultView,
  TenantType,
} from './platform-admin-api.service';

const TENANT_TYPES: readonly TenantType[] = ['BORLETTE', 'RESEAU', 'AMBULANT'];
const PROFILES: readonly TenantProvisioningProfile[] = [
  'MINIMAL',
  'DEFAULT_HAITI_LOTTERY',
  'DEMO',
];

/**
 * Minimal SUPER_ADMIN dashboard: create a tenant (with an optional first tenant-admin via
 * `initialAdminEmail`) and an optional read-only preview. Compact and operational — not analytics.
 */
@Component({
  selector: 'tch-super-admin-dashboard-page',
  imports: [ReactiveFormsModule, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="dash">
      <h1>{{ 'platform.onboarding.title' | translate }}</h1>

      <form class="dash__form" [formGroup]="form" (ngSubmit)="provision()">
        <label>
          <span>{{ 'platform.onboarding.code' | translate }}</span>
          <input type="text" formControlName="code" autocomplete="off" />
        </label>
        <label>
          <span>{{ 'platform.onboarding.name' | translate }}</span>
          <input type="text" formControlName="name" autocomplete="off" />
        </label>
        <label>
          <span>{{ 'platform.onboarding.type' | translate }}</span>
          <select formControlName="type">
            @for (type of tenantTypes; track type) {
              <option [value]="type">{{ type }}</option>
            }
          </select>
        </label>
        <label>
          <span>{{ 'platform.onboarding.profile' | translate }}</span>
          <select formControlName="profile">
            @for (profile of profiles; track profile) {
              <option [value]="profile">{{ profile }}</option>
            }
          </select>
        </label>
        <label>
          <span>{{ 'platform.onboarding.timezone' | translate }}</span>
          <input type="text" formControlName="timezone" />
        </label>
        <label>
          <span>{{ 'platform.onboarding.currency' | translate }}</span>
          <input type="text" formControlName="currency" maxlength="3" />
        </label>
        <label>
          <span>{{ 'platform.onboarding.initialAdminEmail' | translate }}</span>
          <input type="email" formControlName="initialAdminEmail" autocomplete="off" />
        </label>

        <div class="dash__actions">
          <button type="button" [disabled]="form.invalid || busy()" (click)="preview()">
            {{ 'platform.onboarding.preview' | translate }}
          </button>
          <button type="submit" [disabled]="form.invalid || busy()">
            {{ 'platform.onboarding.provision' | translate }}
          </button>
        </div>
      </form>

      @if (previewResult(); as p) {
        <div class="dash__panel" role="status">
          <h2>{{ 'platform.onboarding.previewResult' | translate }}</h2>
          <p>{{ 'platform.onboarding.domains' | translate }}: {{ p.includedDomains.join(', ') }}</p>
          @if (p.warnings.length) {
            <p class="dash__warn">{{ p.warnings.join(' · ') }}</p>
          }
        </div>
      }

      @if (result(); as r) {
        <div class="dash__panel dash__panel--ok" role="status">
          <h2>{{ 'platform.onboarding.success' | translate }}</h2>
          <p>{{ r.tenantCode }} — {{ r.tenantId }}</p>
          @if (r.initialAdminUserId) {
            <p>{{ 'platform.onboarding.adminCreated' | translate }}: {{ r.initialAdminUserId }}</p>
          }
        </div>
      }

      @if (error()) {
        <div class="dash__panel dash__panel--error" role="alert">
          {{ 'platform.onboarding.error' | translate }}
        </div>
      }
    </section>
  `,
  styles: [
    `
      .dash {
        display: grid;
        gap: 1.5rem;
        max-width: 640px;
        padding: 2rem;
        color: var(--tch-color-foreground, var(--mat-sys-on-surface));
      }
      .dash__form {
        display: grid;
        gap: 1rem;
      }
      label {
        display: grid;
        gap: 0.35rem;
        font-weight: 600;
      }
      input,
      select {
        padding: 0.5rem 0.65rem;
        border-radius: var(--tch-radius-control, 8px);
        border: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
        background: var(--tch-color-surface, var(--mat-sys-surface));
        color: inherit;
      }
      .dash__actions {
        display: flex;
        gap: 0.75rem;
      }
      button {
        padding: 0.6rem 1.1rem;
        border-radius: var(--tch-radius-control, 8px);
        border: 1px solid var(--tch-color-primary, var(--mat-sys-primary));
        background: var(--tch-color-primary, var(--mat-sys-primary));
        color: var(--tch-color-primary-contrast, var(--mat-sys-on-primary));
        font-weight: 600;
        cursor: pointer;
      }
      button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      .dash__actions button[type='button'] {
        background: transparent;
        color: var(--tch-color-primary, var(--mat-sys-primary));
      }
      .dash__panel {
        padding: 1rem 1.25rem;
        border-radius: var(--tch-radius-control, 8px);
        border: 1px solid var(--tch-color-outline, var(--mat-sys-outline-variant));
      }
      .dash__panel--ok {
        border-color: var(--tch-color-primary, var(--mat-sys-primary));
      }
      .dash__panel--error {
        border-color: var(--tch-color-error, var(--mat-sys-error));
        color: var(--tch-color-error, var(--mat-sys-error));
      }
      .dash__warn {
        color: var(--tch-color-error, var(--mat-sys-error));
      }
    `,
  ],
})
export class SuperAdminDashboardPage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(PlatformAdminApi);

  readonly tenantTypes = TENANT_TYPES;
  readonly profiles = PROFILES;

  readonly busy = signal(false);
  readonly error = signal(false);
  readonly previewResult = signal<TenantProvisioningPreviewView | null>(null);
  readonly result = signal<TenantProvisioningResultView | null>(null);

  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    type: ['BORLETTE' as TenantType, Validators.required],
    profile: ['MINIMAL' as TenantProvisioningProfile, Validators.required],
    timezone: ['America/Port-au-Prince', Validators.required],
    currency: ['HTG', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    initialAdminEmail: ['', Validators.email],
  });

  preview(): void {
    if (this.form.invalid) {
      return;
    }
    this.start();
    this.api.previewTenant(this.payload()).subscribe({
      next: view => {
        this.previewResult.set(view);
        this.busy.set(false);
      },
      error: () => this.fail(),
    });
  }

  provision(): void {
    if (this.form.invalid) {
      return;
    }
    this.start();
    this.api.provisionTenant(this.payload()).subscribe({
      next: view => {
        this.result.set(view);
        this.busy.set(false);
      },
      error: () => this.fail(),
    });
  }

  private payload(): TenantProvisioningRequest {
    const value = this.form.getRawValue();
    return {
      code: value.code,
      name: value.name,
      type: value.type,
      timezone: value.timezone,
      currency: value.currency,
      profile: value.profile,
      initialAdminEmail: value.initialAdminEmail || undefined,
    };
  }

  private start(): void {
    this.busy.set(true);
    this.error.set(false);
    this.previewResult.set(null);
    this.result.set(null);
  }

  private fail(): void {
    this.busy.set(false);
    this.error.set(true);
  }
}
