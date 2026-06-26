import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { TchLoading, TchErrorPanel } from '@tch/ui/components';

import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import {
  AdminOverviewApiService,
  TenantAdminOverviewView,
  ReadinessSection,
  TenantSetupView,
} from '../../../admin-overview-api.service';

const REQUIRED_SETUP_SECTION_IDS = ['identity', 'address', 'games_pricing', 'draws'] as const;

type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-complete-tenant-config-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    AdminPageShellComponent,
    TchLoading,
    TchErrorPanel,
  ],
  templateUrl: './admin-complete-tenant-config.page.html',
  styleUrls: ['./admin-complete-tenant-config.page.scss'],
})
export class AdminCompleteTenantConfigPage implements OnInit {
  private readonly api = inject(AdminOverviewApiService);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly pageError = signal<string | null>(null);
  readonly overview = signal<TenantAdminOverviewView | null>(null);

  readonly setup = computed<TenantSetupView | null>(() => this.overview()?.setup ?? null);
  readonly header = computed(() => this.overview()?.header ?? null);

  readonly requiredTotalCount = REQUIRED_SETUP_SECTION_IDS.length;
  readonly requiredCompletedCount = computed(() =>
    REQUIRED_SETUP_SECTION_IDS.filter(id => this.sectionMap().get(id)?.status === 'READY').length,
  );
  readonly progressPct = computed(() =>
    Math.round((this.requiredCompletedCount() / this.requiredTotalCount) * 100),
  );

  readonly sectionMap = computed<Map<string, ReadinessSection>>(() => {
    const sections = this.overview()?.sections ?? [];
    return new Map(sections.map(s => [s.id, s]));
  });

  readonly canCreateSellerTerminal = computed(() => this.setup()?.canCreateSellerTerminal ?? false);

  readonly setupCards = computed(() => {
    const map = this.sectionMap();
    return [
      this.cardOf(map, 'identity',      'admin.setup.section.identity',      'verified_user',   null),
      this.cardOf(map, 'address',       'admin.setup.section.address',       'location_on',     null),
      this.cardOf(map, 'games_pricing', 'admin.setup.section.games',         'casino',          '/app/admin/games-pricing'),
      this.cardOf(map, 'draws',         'admin.setup.section.drawChannels',  'event_repeat',        '/app/admin/draw-channels'),
      this.cardOf(map, 'theme',         'admin.setup.section.theme',         'palette',         '/app/admin/appearance'),
      this.cardOf(map, 'promotions',    'admin.setup.section.promotions',    'redeem',          '/app/admin/promotions'),
    ];
  });

  readonly loading = computed(() => this.pageState() === 'loading');
  readonly error = computed(() => this.pageState() === 'error' ? this.pageError() : null);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.pageState.set('loading');
    this.pageError.set(null);
    this.api.getOverview().subscribe({
      next: data => {
        this.overview.set(data);
        this.pageState.set('ready');
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.pageError.set(pd?.title ?? this.translate.instant('admin.setup.error.load'));
        this.pageState.set('error');
      },
    });
  }

  sectionStatus(id: string): 'READY' | 'MISSING' | 'UNKNOWN' {
    const s = this.sectionMap().get(id);
    return (s?.status as 'READY' | 'MISSING' | 'UNKNOWN') ?? 'UNKNOWN';
  }

  statusIcon(status: string): string {
    if (status === 'READY') return 'check_circle';
    if (status === 'MISSING') return 'radio_button_unchecked';
    return 'help_outline';
  }

  statusClass(status: string): string {
    if (status === 'READY') return 'status--ready';
    if (status === 'MISSING') return 'status--missing';
    return 'status--unknown';
  }

  isBlocking(id: string): boolean {
    return this.setup()?.blockingSteps?.includes(id.toUpperCase()) ?? false;
  }

  private cardOf(
    map: Map<string, ReadinessSection>,
    id: string,
    labelKey: string,
    icon: string,
    route: string | null,
  ) {
    const s = map.get(id);
    return { id, labelKey, icon, route: route ?? s?.route ?? null, status: s?.status ?? 'UNKNOWN' };
  }
}
