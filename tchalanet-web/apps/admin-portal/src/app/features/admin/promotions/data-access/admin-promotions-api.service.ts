import { Injectable, inject } from '@angular/core';
import { TchBackendClient, TchPage } from '@tch/api';
import { Observable } from 'rxjs';

export type PromotionCampaignStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED' | 'INACTIVE' | 'ARCHIVED';

export type PromotionIdRef = string | { readonly value?: string; readonly id?: string };

export function promotionIdValue(id: PromotionIdRef | null | undefined): string | null {
  if (!id) return null;
  if (typeof id === 'string') return id;
  return id.value ?? id.id ?? null;
}

export interface PromotionRuleView {
  readonly id: PromotionIdRef;
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
  readonly id: PromotionIdRef;
  readonly code: string;
  readonly name: string;
  readonly status: PromotionCampaignStatus;
  readonly priority: number;
  readonly startsAt: string | null;
  readonly endsAt: string | null;
  readonly rules: readonly PromotionRuleView[];
}

export interface MaryajQuantityTier {
  readonly minPaidAmount: number;
  readonly maxPaidAmount: number | null;
  readonly quantity: number;
}

export interface InstantiateMaryajGratisRequest {
  readonly payoutBaseAmount: number;
  readonly quantityMode: 'FIXED' | 'PER_PAID_AMOUNT' | 'TIERED_PAID_AMOUNT';
  readonly quantity: number;
  readonly stepPaidAmount?: number | null;
  readonly quantityPerStep?: number | null;
  readonly maxQuantity?: number | null;
  readonly quantityTiers?: readonly MaryajQuantityTier[] | null;
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

export interface UpdatePromotionRuleEffectsRequest {
  readonly items: readonly PromotionConfigItem[];
}

@Injectable({ providedIn: 'root' })
export class AdminPromotionsApiService {
  private readonly backend = inject(TchBackendClient);

  listCampaigns(): Observable<TchPage<PromotionCampaignView>> {
    return this.backend.getPage<PromotionCampaignView>(
      '/admin/promotions/campaigns',
      {
        params: { page: '0', size: '20', sort: 'createdAt,desc' },
      },
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

  updateRuleEffects(
    campaignId: string,
    ruleId: string,
    request: UpdatePromotionRuleEffectsRequest,
  ): Observable<PromotionCampaignView> {
    return this.backend.patch<PromotionCampaignView>(
      `/admin/promotions/campaigns/${campaignId}/rules/${ruleId}/effects`,
      request,
    );
  }
}
