import { ViewportScroller } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  effect,
  inject,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  TchConfirmDialog,
  type TchConfirmDialogData,
  TchErrorPanel,
  TchLoading,
  TchSectionError,
} from '@tch/ui/components';
import { AdminPageShellComponent, AdminRefreshButtonComponent } from '@tch/ui/console';
import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';
import { PromotionCampaignView } from '../../data-access/admin-promotions-api.service';
import { AdminMaryajGratisStore } from './admin-maryaj-gratis.store';
import { MaryajGenerationPanelComponent } from './components/maryaj-generation-panel.component';
import { MaryajGameSettingsPanelComponent } from './components/maryaj-game-settings-panel.component';
import { MaryajOfferPanelComponent } from './components/maryaj-offer-panel.component';

@Component({
  selector: 'tch-admin-maryaj-gratis-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    AdminRefreshButtonComponent,
    MaryajGenerationPanelComponent,
    MaryajGameSettingsPanelComponent,
    MaryajOfferPanelComponent,
    TchErrorPanel,
    TchLoading,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './admin-maryaj-gratis.page.html',
  styleUrls: ['./admin-maryaj-gratis.page.scss'],
  providers: [AdminMaryajGratisStore],
})
export class AdminMaryajGratisPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly viewportScroller = inject(ViewportScroller);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  private readonly queryParamMap = toSignal(this.route.queryParamMap, {
    initialValue: this.route.snapshot.queryParamMap,
  });
  readonly store = inject(AdminMaryajGratisStore);
  readonly fromSetup = computed(() => this.queryParamMap().get('from') === 'setup');

  constructor() {
    effect(() => {
      if (this.store.state() === 'ready') {
        this.scrollToFragment(this.route.snapshot.fragment);
      }
    });
  }

  ngOnInit(): void {
    this.route.fragment
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(fragment => this.scrollToFragment(fragment));
    this.store.load();
  }

  openGameSettings(game: TenantGamePricingView): void {
    void this.router.navigate(['/app/admin/games', game.gameCode, 'settings'], {
      queryParams: {
        returnTo: 'maryaj-gratis',
        ...(this.fromSetup() ? { from: 'setup' } : {}),
      },
    });
  }

  confirmDisableGame(gameCode: string): void {
    const game = this.store.maryajGame();
    const name = game?.gameName ?? gameCode;
    this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.gamesPricing.confirm.disableTitle', { name }),
          message: this.translate.instant('admin.gamesPricing.confirm.disableMessage'),
          confirmLabel: this.translate.instant('admin.gamesPricing.confirm.disableAction'),
          cancelLabel: this.translate.instant('common.cancel'),
          destructive: true,
          icon: 'block',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.confirmed) this.store.disableGame(gameCode);
      });
  }

  confirmPauseOffer(campaign: PromotionCampaignView): void {
    this.dialog
      .open<TchConfirmDialog, TchConfirmDialogData, { confirmed: boolean }>(TchConfirmDialog, {
        data: {
          title: this.translate.instant('admin.maryajGratis.offer.confirmPause.title'),
          message: this.translate.instant('admin.maryajGratis.offer.confirmPause.message'),
          confirmLabel: this.translate.instant('admin.maryajGratis.offer.confirmPause.action'),
          cancelLabel: this.translate.instant('common.cancel'),
          destructive: true,
          icon: 'pause_circle',
        },
      })
      .afterClosed()
      .subscribe(result => {
        if (result?.confirmed) this.store.pause(campaign);
      });
  }

  private scrollToFragment(fragment: string | null): void {
    if (!fragment || this.store.state() !== 'ready') return;
    setTimeout(() => this.viewportScroller.scrollToAnchor(fragment));
  }
}
