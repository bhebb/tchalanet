import { ViewportScroller } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  effect,
  inject,
} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import {
  GAME_SETTINGS_DIALOG_SURFACE_CONFIG,
  GameSettingsDialog,
} from '../../../games-pricing/components/dialogs/game-settings.dialog';
import { toTenantGameSettingsView } from '../../../games-pricing/data-access/tenant-game-settings-adapter';
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
  private readonly viewportScroller = inject(ViewportScroller);
  private readonly destroyRef = inject(DestroyRef);
  private readonly dialog = inject(MatDialog);
  private readonly translate = inject(TranslateService);
  readonly store = inject(AdminMaryajGratisStore);

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
    const ref = this.dialog.open(GameSettingsDialog, {
      data: { game: toTenantGameSettingsView(game) },
      ...GAME_SETTINGS_DIALOG_SURFACE_CONFIG,
    });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.store.load();
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
