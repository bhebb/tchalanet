// Theme dev sandbox — toggleable panel to exercise the theme (presets, light/dark, density, M3 type
// scale, fonts) and inspect colour roles against real Material components. Dev-only: the launcher is
// hidden unless isDevMode(). Kept on purpose; activate with the floating 🧪 button.
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  PLATFORM_ID,
  computed,
  inject,
  isDevMode,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { TchRuntimeConfigStore } from '@tch/shared-config';
import { ThemeDensity, ThemeMode, ThemeStore } from '@tch/ui/theme';
import {
  AdminEmptyState,
  TchActionButton,
  TchConfirmDialog,
  TchConfirmDialogData,
  TchErrorPanel,
  TchLoading,
  TchNotice,
  TchStatusBadge,
  TchSubmitButton,
} from '@tch/ui/components';

import { AdminSectionCardComponent } from '@tch/ui/console';
import { AdminNextStepsCardComponent } from '@tch/ui/console';
import { TchIdentityCardComponent } from '@tch/ui/console';
import { purgeOidcBrowserCache } from './purge-oidc-cache';

interface Swatch {
  readonly label: string;
  readonly token: string;
  readonly value: string;
}

const COLOR_TOKENS: readonly string[] = [
  '--tch-color-primary',
  '--tch-color-on-primary',
  '--tch-color-primary-container',
  '--tch-color-secondary',
  '--tch-color-secondary-container',
  '--tch-color-accent',
  '--tch-color-surface',
  '--tch-color-surface-container',
  '--tch-color-on-surface',
  '--tch-color-outline',
  '--tch-color-error',
  '--tch-color-background',
  '--tch-header-bg',
  '--tch-footer-bg',
  '--mat-sys-primary',
  '--mat-sys-secondary',
  '--mat-sys-tertiary',
];

const STORAGE_KEY = 'tch.theme-sandbox.open';

@Component({
  selector: 'tch-theme-sandbox',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    AdminEmptyState,
    AdminNextStepsCardComponent,
    AdminSectionCardComponent,
    TchActionButton,
    TchErrorPanel,
    TchIdentityCardComponent,
    TchLoading,
    TchNotice,
    TchStatusBadge,
    TchSubmitButton,
  ],
  template: `
    @if (devMode) {
      <button type="button" class="launcher" (click)="toggle()" [attr.aria-expanded]="open()">
        🧪 {{ open() ? 'Fermer' : 'Theme' }}
      </button>

      @if (open()) {
        <aside class="panel" role="dialog" aria-label="Theme sandbox">
          <header class="panel__bar">
            <strong>🧪 Theme sandbox</strong>
            <button type="button" class="panel__close" (click)="toggle()" aria-label="Fermer">✕</button>
            <label>
              Preset
              <select [ngModel]="theme.activeTheme().activePresetKey" (ngModelChange)="setPreset($event)">
                @for (p of theme.presets(); track p.id) {
                  <option [value]="p.id">{{ p.id }}</option>
                }
              </select>
            </label>
            <label>
              Mode
              <select [ngModel]="theme.activeTheme().mode" (ngModelChange)="setMode($event)">
                @for (m of modes; track m) {
                  <option [value]="m">{{ m }}</option>
                }
              </select>
            </label>
            <label>
              Densité
              <select [ngModel]="theme.activeTheme().density" (ngModelChange)="setDensity($event)">
                @for (d of densities; track d) {
                  <option [value]="d">{{ d }}</option>
                }
              </select>
            </label>
            <button type="button" (click)="refresh()">↻</button>
            <button type="button" (click)="purgeOidc()" title="Purge OIDC cache">🔑 Purge OIDC</button>
          </header>

          <section class="block">
            <h4>Couleurs (rôles)</h4>
            <div class="swatches">
              @for (s of swatches(); track s.token) {
                <div class="swatch">
                  <span class="swatch__chip" [style.background]="'var(' + s.token + ')'"></span>
                  <span class="swatch__meta">
                    <code>{{ s.label }}</code>
                    <code class="swatch__val">{{ s.value }}</code>
                  </span>
                </div>
              }
            </div>
          </section>

          <section class="block">
            <h4>Typo (échelle M3 bridgée)</h4>
            <p style="font-size: var(--tch-font-size-display-lg); line-height: var(--tch-line-height-display-lg)">Display</p>
            <p style="font-size: var(--tch-font-size-headline-lg)">Headline</p>
            <p style="font-size: var(--tch-font-size-title-md)">Title</p>
            <p style="font-size: var(--tch-font-size-body-md)">Body — <span style="font-family: var(--tch-font-family)">police active</span></p>
          </section>

          <section class="block">
            <h4>Composants Material (densité)</h4>
            <div class="row">
              <button mat-flat-button color="primary">Filled</button>
              <button mat-stroked-button>Outlined</button>
              <button mat-button>Text</button>
            </div>
            <mat-form-field appearance="outline">
              <mat-label>Champ</mat-label>
              <input matInput placeholder="tape ici" />
            </mat-form-field>
            <div class="row">
              <mat-checkbox checked>Case</mat-checkbox>
              <mat-slide-toggle checked>Toggle</mat-slide-toggle>
            </div>
            <mat-chip-set>
              <mat-chip>Chip A</mat-chip>
              <mat-chip>Chip B</mat-chip>
            </mat-chip-set>
          </section>

          <section class="block">
            <h4>UI Kit — Messaging</h4>
            <tch-notice type="info"><strong>Info</strong> — statut du tenant.</tch-notice>
            <tch-notice type="success"><strong>Succès</strong> — action effectuée.</tch-notice>
            <tch-notice type="warning"><strong>Attention</strong> — maintenance prévue.</tch-notice>
            <tch-notice type="error"><strong>Erreur</strong> — connexion échouée.</tch-notice>
            <div class="row">
              <button mat-stroked-button (click)="openConfirm('standard')">Dialog confirm</button>
              <button mat-stroked-button (click)="openConfirm('destructive')">Dialog destructif</button>
              <button mat-stroked-button (click)="openConfirm('sensitive')">Dialog sensible</button>
            </div>
          </section>

          <section class="block">
            <h4>UI Kit — Boutons</h4>
            <p class="kit-caption">Submit (full-width, mobile)</p>
            <div class="kit-stack">
              <tch-submit-button label="Provisionner" [loading]="submitLoading()" (click)="pingSubmit()" />
              <tch-submit-button label="Désactivé" [disabled]="true" />
              <tch-submit-button label="Secondaire (block)" variant="secondary" />
            </div>
            <p class="kit-caption">Action (compact, inline)</p>
            <div class="row">
              <button tch-action variant="primary">Primary</button>
              <button tch-action variant="secondary">Secondary</button>
              <button tch-action variant="tertiary">Tertiary</button>
            </div>
          </section>

          <section class="block">
            <h4>UI Kit — Statuts</h4>
            <div class="row">
              <tch-status-badge status="ready" label="Prêt" />
              <tch-status-badge status="pending" label="En attente" />
              <tch-status-badge status="warning" label="Alerte" />
              <tch-status-badge status="blocked" label="Bloqué" />
              <tch-status-badge status="missing" label="Manquant" />
            </div>
          </section>

          <section class="block">
            <h4>UI Kit — États</h4>
            <tch-loading label="Chargement…" />
            <tch-error-panel
              title="Échec du chargement"
              message="Impossible de joindre le serveur."
              retryLabel="Réessayer"
              [showRetry]="true"
            />
            <tch-admin-empty-state
              icon="inbox"
              title="Aucun tenant"
              message="Commencez par provisionner un tenant."
              actionLabel="Onboarding"
            />
          </section>

          <section class="block">
            <h4>UI Kit — Cards</h4>
            <p class="kit-caption">IdentityCard — default</p>
            <tch-identity-card
              eyebrow="Identité du tenant"
              title="Grand Pari Haïti"
              code="GPH-7729-LOT"
              status="DRAFT"
              statusTone="neutral"
              icon="shield"
              [meta]="identityMeta"
            />
            <p class="kit-caption">IdentityCard — compact (right rail)</p>
            <tch-identity-card
              variant="compact"
              eyebrow="Identité du tenant"
              title="Grand Pari Haïti"
              code="GPH-7729-LOT"
              status="DRAFT"
              statusTone="neutral"
              icon="shield"
              [meta]="identityMeta"
            />
            <tch-admin-section-card title="Paramètres régionaux" icon="public">
              <p style="margin: 0; color: var(--tch-color-on-surface-variant)">Contenu de section.</p>
            </tch-admin-section-card>
            <tch-admin-next-steps-card [steps]="nextSteps" />
          </section>
        </aside>
      }
    }
  `,
  styles: [
    `
      .launcher {
        position: fixed;
        right: 1rem;
        bottom: 1rem;
        z-index: var(--tch-z-toast, 60);
        padding: 0.5rem 0.8rem;
        border: 1px solid var(--tch-color-outline);
        border-radius: var(--tch-radius-pill);
        background: var(--tch-color-surface-container-high);
        color: var(--tch-color-on-surface);
        cursor: pointer;
        box-shadow: var(--tch-elevation-2);
      }
      .panel {
        position: fixed;
        left: 50%;
        bottom: 1rem;
        transform: translateX(-50%);
        z-index: var(--tch-z-toast, 60);
        width: min(100% - 2rem, 960px);
        max-height: min(92dvh, 960px);
        overflow: auto;
        overscroll-behavior: contain;
        padding: 1.25rem;
        border: 1px solid var(--tch-color-outline);
        border-radius: var(--tch-radius-lg);
        background: var(--tch-color-surface);
        color: var(--tch-color-on-surface);
        box-shadow: var(--tch-elevation-3);
      }
      .panel__bar {
        position: sticky;
        top: -1.25rem;
        z-index: 1;
        display: flex;
        flex-wrap: wrap;
        align-items: center;
        gap: 0.5rem;
        margin: -1.25rem -1.25rem 0.75rem;
        padding: 0.75rem 1.25rem;
        background: var(--tch-color-surface);
        border-bottom: 1px solid var(--tch-color-outline-variant);
      }
      .panel__close {
        margin-left: auto;
        width: 2rem;
        height: 2rem;
        border-radius: var(--tch-radius-pill);
        border: 1px solid var(--tch-color-outline);
        background: var(--tch-color-surface-container-high);
        color: var(--tch-color-on-surface);
        cursor: pointer;
      }
      .panel__bar label {
        display: inline-flex;
        gap: 0.3rem;
        align-items: center;
        font-size: 0.8rem;
      }
      select,
      .panel__bar button {
        padding: 0.3rem 0.45rem;
        border: 1px solid var(--tch-color-outline);
        border-radius: var(--tch-radius-md);
        background: var(--tch-color-surface);
        color: var(--tch-color-on-surface);
      }
      .block {
        padding-top: 0.5rem;
        border-top: 1px solid var(--tch-color-outline-variant);
      }
      .block h4 {
        margin: 0.5rem 0;
        font-size: 0.85rem;
        color: var(--tch-color-on-surface-variant);
      }
      .swatches {
        display: grid;
        gap: 0.4rem;
        grid-template-columns: 1fr 1fr;
      }
      .swatch {
        display: flex;
        align-items: center;
        gap: 0.4rem;
      }
      .swatch__chip {
        flex: 0 0 auto;
        width: 1.6rem;
        height: 1.6rem;
        border-radius: var(--tch-radius-sm);
        border: 1px solid var(--tch-color-outline-variant);
      }
      .swatch__meta {
        display: flex;
        flex-direction: column;
        min-width: 0;
      }
      .swatch__meta code {
        font-size: 0.62rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .swatch__val {
        color: var(--tch-color-on-surface-variant);
      }
      .row {
        display: flex;
        flex-wrap: wrap;
        gap: 0.6rem;
        align-items: center;
        margin: 0.4rem 0;
      }
      p {
        margin: 0.25rem 0;
      }
      .kit-stack {
        display: grid;
        gap: 0.5rem;
        margin: 0.4rem 0;
      }
      .kit-caption {
        margin: 0.6rem 0 0.2rem;
        font-size: 0.7rem;
        font-weight: 600;
        text-transform: uppercase;
        letter-spacing: 0.04em;
        color: var(--tch-color-on-surface-variant);
      }
      tch-notice {
        display: block;
        margin-bottom: 0.4rem;
      }
      tch-identity-card,
      tch-admin-section-card,
      tch-admin-next-steps-card,
      tch-admin-empty-state,
      tch-error-panel {
        display: block;
        margin-bottom: 0.6rem;
      }
    `,
  ],
})
export class ThemeSandboxComponent {
  private readonly document = inject(DOCUMENT);
  private readonly dialog = inject(MatDialog);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly runtimeConfig = inject(TchRuntimeConfigStore);
  private readonly isBrowser = isPlatformBrowser(this.platformId);
  readonly theme = inject(ThemeStore);
  readonly devMode = isDevMode() && this.runtimeConfig.config().enableSandbox;
  readonly modes: readonly ThemeMode[] = ['light', 'dark', 'system'];
  readonly densities: readonly ThemeDensity[] = ['comfortable', 'compact', 'dense'];
  readonly open = signal(this.restoreOpen());
  private readonly tick = signal(0);

  /** UI-kit showcase sample data. */
  readonly submitLoading = signal(false);
  readonly identityMeta = [
    { label: 'Type', value: 'BORLETTE' },
    { label: 'Devise', value: 'HTG' },
    { label: 'Fuseau', value: 'America/Port-au-Prince' },
  ];
  readonly nextSteps = [
    { icon: 'person_add', label: 'Ajouter un admin tenant' },
    { icon: 'casino', label: 'Configurer les tirages' },
    { icon: 'point_of_sale', label: 'Créer un seller-terminal' },
  ];

  /** Briefly flip the submit button into its loading state for the demo. */
  pingSubmit(): void {
    this.submitLoading.set(true);
    setTimeout(() => this.submitLoading.set(false), 1500);
  }

  openConfirm(mode: 'standard' | 'destructive' | 'sensitive'): void {
    const base: TchConfirmDialogData = {
      title:
        mode === 'sensitive' ? 'Accéder à ce tenant en mode support ?' : 'Confirmer l’action ?',
      message:
        mode === 'sensitive'
          ? 'Vous allez accéder à l’espace admin de ce tenant avec vos droits superadmin. Cette action sera auditée.'
          : 'Cette opération sera appliquée immédiatement.',
      confirmLabel: mode === 'destructive' ? 'Suspendre' : 'Confirmer',
      cancelLabel: 'Annuler',
      destructive: mode === 'destructive' || mode === 'sensitive',
      sensitive: mode === 'sensitive',
      requireReason: mode === 'sensitive',
      reasonLabel: mode === 'sensitive' ? 'Motif de l’accès' : undefined,
      auditLabel: mode === 'sensitive' ? 'Action de support sécurisée' : undefined,
      icon: mode === 'sensitive' ? 'admin_panel_settings' : undefined,
    };
    this.dialog.open(TchConfirmDialog, { data: base });
  }

  readonly swatches = computed<readonly Swatch[]>(() => {
    this.tick(); // recompute when controls change / refresh
    if (!this.open()) {
      return [];
    }
    if (!this.isBrowser) {
      return [];
    }
    const cs = getComputedStyle(this.document.body);
    return COLOR_TOKENS.map((token) => ({
      label: token.replace('--tch-', '').replace('--mat-sys-', 'mat:'),
      token,
      value: cs.getPropertyValue(token).trim() || '—',
    }));
  });

  toggle(): void {
    const next = !this.open();
    this.open.set(next);
    if (this.isBrowser) {
      localStorage.setItem(STORAGE_KEY, next ? '1' : '0');
    }
    this.refresh();
  }

  setPreset(value: string): void {
    this.theme.setPreset(value);
    this.refresh();
  }

  setMode(value: ThemeMode): void {
    this.theme.setMode(value);
    this.refresh();
  }

  setDensity(value: ThemeDensity): void {
    this.theme.setDensity(value);
    this.refresh();
  }

  purgeOidc(): void {
    purgeOidcBrowserCache();
  }

  refresh(): void {
    // Defer so the DOM/theme classes are applied before we read computed values.
    setTimeout(() => this.tick.update((n) => n + 1));
  }

  private restoreOpen(): boolean {
    if (!this.isBrowser) {
      return false;
    }
    return localStorage.getItem(STORAGE_KEY) === '1';
  }
}
