import { DatePipe, JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import {
  AdminEmptyStateComponent,
  AdminPageShellComponent,
  AdminSectionCardComponent,
} from '@tch/ui/console';
import {
  AdminPromotionsApiService,
  CreatePromotionCampaignRequest,
  PromotionCampaignStatus,
  PromotionCampaignView,
} from '../../data-access/admin-promotions-api.service';

type PageState = 'loading' | 'ready' | 'error';

const DEFAULT_CAMPAIGN_JSON: CreatePromotionCampaignRequest = {
  name: 'Promotion Maryaj gratis personnalisée',
  description: 'Ajoute une ligne Maryaj gratuite selon les critères configurés.',
  startsAt: new Date().toISOString(),
  endsAt: null,
  priority: 100,
  rules: [
    {
      ruleKey: 'maryaj-gratis-custom',
      priority: 100,
      eligibilityItems: [
        { type: 'MIN_PAID_TOTAL', params: { amount: '1' } },
      ],
      effectItems: [
        {
          type: 'FREE_GAME_LINE',
          params: {
            gameCode: 'HT_MARYAJ_GRATUIT',
            payoutBaseAmount: '50',
            quantity: '1',
            choiceMode: 'AUTO_GENERATE',
            generationStrategy: 'RANDOM',
            regenerableBeforeConfirm: 'true',
            maxRegenerationsBeforeConfirm: '3',
          },
        },
      ],
    },
  ],
};

@Component({
  selector: 'tch-admin-promotion-campaigns-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    JsonPipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchErrorPanel,
    TchLoading,
  ],
  templateUrl: './admin-promotion-campaigns.page.html',
  styleUrls: ['./admin-promotion-campaigns.page.scss'],
})
export class AdminPromotionCampaignsPage implements OnInit {
  private readonly api = inject(AdminPromotionsApiService);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly state = signal<PageState>('loading');
  readonly error = signal<string | null>(null);
  readonly saving = signal(false);
  readonly campaigns = signal<readonly PromotionCampaignView[]>([]);

  readonly activeCampaigns = computed(() => this.campaigns().filter(c => c.status === 'ACTIVE'));

  readonly form = this.fb.nonNullable.group({
    payload: [JSON.stringify(DEFAULT_CAMPAIGN_JSON, null, 2), [Validators.required, jsonValidator]],
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.state.set('loading');
    this.error.set(null);
    this.api.listCampaigns().subscribe({
      next: page => {
        this.campaigns.set(page.items);
        this.state.set('ready');
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; detail?: string } })?.error;
        this.error.set(pd?.title ?? pd?.detail ?? 'Impossible de charger les campagnes.');
        this.state.set('error');
      },
    });
  }

  createCampaign(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const request = JSON.parse(this.form.getRawValue().payload) as CreatePromotionCampaignRequest;
    this.saving.set(true);
    this.api.createCampaign(request).subscribe({
      next: campaign => {
        this.campaigns.update(items => [campaign, ...items]);
        this.saving.set(false);
        this.snackBar.open('Campagne créée.', 'OK', { duration: 3000 });
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string; detail?: string } })?.error;
        this.saving.set(false);
        this.snackBar.open(pd?.title ?? pd?.detail ?? 'Erreur lors de la création.', 'OK', {
          duration: 5000,
        });
      },
    });
  }

  statusLabel(status: PromotionCampaignStatus): string {
    switch (status) {
      case 'ACTIVE': return 'Actif';
      case 'PAUSED': return 'En pause';
      case 'DRAFT': return 'Brouillon';
      case 'INACTIVE': return 'Inactif';
      case 'ARCHIVED': return 'Archivé';
    }
  }
}

function jsonValidator(control: { value: string }) {
  try {
    JSON.parse(control.value);
    return null;
  } catch {
    return { jsonInvalid: true };
  }
}
