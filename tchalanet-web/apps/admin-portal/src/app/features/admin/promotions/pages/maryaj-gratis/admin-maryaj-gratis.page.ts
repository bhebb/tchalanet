import { ViewportScroller } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, effect, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminDetailLayoutComponent, AdminPageShellComponent } from '@tch/ui/console';
import { TenantGameView } from '../../../games-admin-api.service';
import { TenantGamePricingView } from '../../../games-pricing/data-access/admin-games-pricing.models';
import { GameSettingsDialog } from '../../../pages/games/dialogs/game-settings.dialog';
import { AdminMaryajGratisStore } from './admin-maryaj-gratis.store';
import { MaryajConfigSummaryComponent } from './components/maryaj-config-summary.component';
import { MaryajGameSettingsPanelComponent } from './components/maryaj-game-settings-panel.component';
import { MaryajOfferPanelComponent } from './components/maryaj-offer-panel.component';

@Component({
  selector: 'tch-admin-maryaj-gratis-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    AdminDetailLayoutComponent,
    AdminPageShellComponent,
    MaryajConfigSummaryComponent,
    MaryajGameSettingsPanelComponent,
    MaryajOfferPanelComponent,
    TchErrorPanel,
    TchLoading,
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
      data: { game: this.toDialogGame(game) },
      width: '480px',
    });
    ref.afterClosed().subscribe(ok => {
      if (ok) this.store.load();
    });
  }

  regenerableLabel(): string {
    return this.store.form.controls.regenerableBeforeConfirm.value ? 'Oui' : 'Non';
  }

  private scrollToFragment(fragment: string | null): void {
    if (!fragment || this.store.state() !== 'ready') return;
    setTimeout(() => this.viewportScroller.scrollToAnchor(fragment));
  }

  private toDialogGame(game: TenantGamePricingView): TenantGameView {
    return {
      gameCode: game.gameCode,
      catalogName: game.gameName,
      displayName: game.gameName,
      category: null,
      enabled: game.tenantStatus === 'ACTIVE' || game.tenantStatus === 'NEEDS_CONFIG',
      visibleInPos: true,
      displayOrder: 0,
      minStake: game.limits.minStake,
      maxStake: game.limits.maxStake,
      availabilityEnabled: false,
      availabilityDays: null,
      startLocalTime: null,
      endLocalTime: null,
      readyForSale: game.readiness.status === 'READY',
      betOptions: game.odds,
    };
  }
}
