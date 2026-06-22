import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';

import { TchLoading, TchErrorPanel } from '@tch/ui/components';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { TenantConfigApiService } from '../../tenant-config-api.service';

type HealthState = 'valid' | 'invalid' | 'unknown';

interface ConfigHealth {
  locale: HealthState;
  communication: HealthState;
  document: HealthState;
  rules: HealthState;
  games: HealthState;
}

@Component({
  selector: 'tch-admin-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
  ],
  templateUrl: './admin-config.page.html',
  styleUrls: ['./admin-config.page.scss'],
})
export class AdminConfigPage implements OnInit {
  private readonly api = inject(TenantConfigApiService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly traceId = signal<string | null>(null);
  readonly config = signal<unknown>(null);
  readonly commConfig = signal<unknown>(null);
  readonly docConfig = signal<unknown>(null);

  readonly rawJson = () => JSON.stringify(this.config(), null, 2);
  readonly localeJson = () => {
    const cfg = this.config() as Record<string, unknown> | null;
    return JSON.stringify(cfg?.['locale'] ?? cfg, null, 2);
  };
  readonly commJson = () => JSON.stringify(this.commConfig(), null, 2);
  readonly docJson = () => JSON.stringify(this.docConfig(), null, 2);

  readonly health = signal<ConfigHealth>({
    locale: 'unknown',
    communication: 'unknown',
    document: 'unknown',
    rules: 'unknown',
    games: 'unknown',
  });

  readonly healthItems = computed(() => [
    { key: 'locale', label: 'Locale', state: this.health().locale },
    { key: 'communication', label: 'Communication', state: this.health().communication },
    { key: 'document', label: 'Document', state: this.health().document },
    { key: 'rules', label: 'Règles', state: this.health().rules },
    { key: 'games', label: 'Jeux', state: this.health().games },
  ]);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.api.getTenantConfig().subscribe({
      next: v => {
        this.config.set(v);
        this.loading.set(false);
        this.evaluateHealth(v);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; errorId?: string; requestId?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.traceId.set(pd?.errorId ?? pd?.requestId ?? null);
        this.loading.set(false);
      },
    });

    this.api.getCommunicationConfig().subscribe({
      next: v => this.commConfig.set(v),
      error: () => {},
    });

    this.api.getDocumentConfig().subscribe({
      next: v => this.docConfig.set(v),
      error: () => {},
    });
  }

  private evaluateHealth(cfg: unknown): void {
    const c = cfg as Record<string, unknown> | null;
    const hasLocale = !!(c?.['locale'] || c?.['defaultLanguage'] || c?.['defaultLocale']);
    const hasComm = this.commConfig() != null;
    const hasDoc = this.docConfig() != null;

    this.health.set({
      locale: hasLocale ? 'valid' : 'unknown',
      communication: hasComm ? 'valid' : 'unknown',
      document: hasDoc ? 'valid' : 'unknown',
      rules: 'unknown',
      games: 'unknown',
    });
  }
}
