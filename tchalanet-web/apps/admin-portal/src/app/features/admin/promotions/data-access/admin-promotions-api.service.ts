import { Injectable, inject } from '@angular/core';
import { TchBackendClient } from '@tch/api';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export type PromotionCampaignStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'INACTIVE' | 'ARCHIVED';

export interface PromotionRuleView {
  readonly id: { value: string };
  readonly ruleKey: string;
  readonly priority: number;
  readonly eligibility: readonly PromotionConfigItem[];
  readonly effects: readonly PromotionConfigItem[];
}

export interface PromotionConfigItem {
  readonly type: string;
  readonly params: Record<string, unknown>;
}

export interface PromotionCampaignView {
  readonly id: { value: string };
  readonly code: string;
  readonly name: string;
  readonly status: PromotionCampaignStatus;
  readonly priority: number;
  readonly startsAt: string | null;
  readonly endsAt: string | null;
  readonly rules: readonly PromotionRuleView[];
}

interface TchPageResponse<T> {
  readonly items?: readonly T[];
  readonly content?: readonly T[];
  readonly total?: number;
  readonly totalElements?: number;
  readonly page?: number;
  readonly size?: number;
  readonly totalPages?: number;
}

export interface PromotionCampaignPage {
  readonly items: readonly PromotionCampaignView[];
  readonly total: number;
}

export interface InstantiateMaryajGratisRequest {
  readonly payoutBaseAmount: number;
  readonly quantity: number;
  readonly choiceMode: 'AUTO_GENERATE' | 'SELLER_SELECTS';
  readonly generationStrategy?: 'RANDOM' | null;
  readonly regenerableBeforeConfirm: boolean;
  readonly maxRegenerationsBeforeConfirm: number;
}

export interface CreatePromotionCampaignRequest {
  readonly name: string;
  readonly description?: string | null;
  readonly startsAt: string;
  readonly endsAt?: string | null;
  readonly priority: number;
  readonly rules: readonly CreatePromotionRuleRequest[];
}

export interface CreatePromotionRuleRequest {
  readonly ruleKey: string;
  readonly priority: number;
  readonly eligibilityItems: readonly PromotionConfigItem[];
  readonly effectItems: readonly PromotionConfigItem[];
}

@Injectable({ providedIn: 'root' })
export class AdminPromotionsApiService {
  private readonly backend = inject(TchBackendClient);

  listCampaigns(): Observable<PromotionCampaignPage> {
    return this.backend
      .get<TchPageResponse<PromotionCampaignView>>('/admin/promotions/campaigns', {
        params: { page: '0', size: '20', sort: 'createdAt,desc' },
      })
      .pipe(
        map(page => ({
          items: page.items ?? page.content ?? [],
          total: page.total ?? page.totalElements ?? (page.items ?? page.content ?? []).length,
        })),
      );
  }

  instantiateDefaultMaryajGratis(
    request: InstantiateMaryajGratisRequest,
  ): Observable<PromotionCampaignView> {
    return this.backend.post<PromotionCampaignView>(
      '/admin/promotions/campaigns/templates/default-maryaj-gratis/instantiate',
      request,
    );
  }

  createCampaign(request: CreatePromotionCampaignRequest): Observable<PromotionCampaignView> {
    return this.backend.post<PromotionCampaignView>('/admin/promotions/campaigns', request);
  }

  activateCampaign(campaignId: string): Observable<PromotionCampaignView> {
    return this.backend.post<PromotionCampaignView>(
      `/admin/promotions/campaigns/${campaignId}/activate`,
      {},
    );
  }

  pauseCampaign(campaignId: string): Observable<PromotionCampaignView> {
    return this.backend.post<PromotionCampaignView>(
      `/admin/promotions/campaigns/${campaignId}/pause`,
      {},
    );
  }
}
