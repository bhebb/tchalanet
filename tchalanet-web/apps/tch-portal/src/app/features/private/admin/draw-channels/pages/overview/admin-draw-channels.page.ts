import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchLoading, TchErrorPanel, TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '../../../../../../core/api/error-feedback-copy';
import { ErrorViewModel, toErrorViewModel } from '../../../../../../core/api/local-error-routing';
import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { AdminEmptyStateComponent } from '../../../../shared/admin-ui/admin-empty-state.component';

import { AdminDrawChannelsApiService } from '../../data-access/admin-draw-channels-api.service';
import { DrawChannelProviderView, DrawChannelSlotConfigView } from '../../data-access/admin-draw-channels.models';
import { DrawChannelsSummaryComponent } from '../../components/draw-channels-summary/draw-channels-summary.component';
import { DrawChannelProviderCardComponent } from '../../components/draw-channel-provider-card/draw-channel-provider-card.component';

type ActiveFilter = 'all' | 'active' | 'todo' | 'inactive' | 'error';
type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-draw-channels-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    MatButtonModule,
    AdminPageShellComponent,
    AdminEmptyStateComponent,
    TchLoading,
    TchErrorPanel,
    TchSectionError,
    DrawChannelsSummaryComponent,
    DrawChannelProviderCardComponent,
  ],
  templateUrl: './admin-draw-channels.page.html',
  styleUrls: ['./admin-draw-channels.page.scss'],
})
export class AdminDrawChannelsPage implements OnInit {
  private readonly api      = inject(AdminDrawChannelsApiService);
  private readonly router   = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly pageState    = signal<PageState>('loading');
  readonly pageError    = signal<ErrorViewModel | null>(null);
  readonly actionNotice = signal<string | null>(null);
  readonly allProviders = signal<DrawChannelProviderView[]>([]);
  readonly activeFilter = signal<ActiveFilter>('all');
  readonly searchQuery  = signal<string>('');

  readonly filteredProviders = computed(() => {
    const filter = this.activeFilter();
    const query  = this.searchQuery().trim().toLowerCase();
    return this.allProviders()
      .filter(p => {
        if (filter === 'active')   return p.tenantStatus === 'ACTIVE';
        if (filter === 'todo')     return p.tenantStatus === 'INACTIVE' || p.tenantStatus === 'NEEDS_CONFIG';
        if (filter === 'inactive') return p.tenantStatus === 'INACTIVE';
        if (filter === 'error')    return p.resultAcquisition.sourceStatus === 'ERROR';
        return true;
      })
      .filter(p =>
        !query ||
        p.providerLabel.toLowerCase().includes(query) ||
        p.providerCode.toLowerCase().includes(query),
      );
  });

  readonly filters: { key: ActiveFilter; label: string }[] = [
    { key: 'all',      label: 'Tous' },
    { key: 'active',   label: 'Actifs' },
    { key: 'todo',     label: 'À configurer' },
    { key: 'inactive', label: 'Inactifs' },
    { key: 'error',    label: 'Erreur source' },
  ];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.actionNotice.set(null);
    this.api.getDrawChannelProviders().subscribe({
      next: data => { this.allProviders.set(data); this.pageState.set('ready'); },
      error: (err: unknown) => {
        this.pageError.set(this.errorViewModel(err));
        this.pageState.set('error');
      },
    });
  }

  onConfigure(provider: DrawChannelProviderView): void {
    this.actionNotice.set(`Configuration de ${provider.providerLabel} bientot disponible.`);
  }

  onViewProviderResults(provider: DrawChannelProviderView): void {
    // TODO(results): enable once /app/admin/results page exists
    this.router.navigate(['/app/admin/results'], { queryParams: { provider: provider.providerCode } });
  }

  onViewSlotResults(event: { provider: DrawChannelProviderView; slot: DrawChannelSlotConfigView }): void {
    // TODO(results): enable once /app/admin/results page exists
    this.router.navigate(['/app/admin/results'], {
      queryParams: { provider: event.provider.providerCode, slot: event.slot.slotKey },
    });
  }

  private errorViewModel(err: unknown): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, 'admin.setup.draw_channels', 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
