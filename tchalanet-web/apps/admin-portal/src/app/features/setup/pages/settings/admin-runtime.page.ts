import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { RuntimeApiService } from '../../data-access/runtime-api.service';

@Component({
  selector: 'tch-admin-runtime-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    TranslatePipe,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TchAsyncViewComponent,
    TchAsyncReadyDirective,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './admin-runtime.page.html',
  styleUrls: ['./admin-runtime.page.scss'],
})
export class AdminRuntimePage {
  private readonly api = inject(RuntimeApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly fromSetup = this.route.snapshot.queryParamMap.get('from') === 'setup';
  readonly backRoute = this.fromSetup ? '/app/admin/setup' : '/app/admin/company/settings';
  readonly backLabel = this.fromSetup
    ? 'admin.setup.backToSetup'
    : 'admin.settings.title';
  readonly linkQueryParams = this.fromSetup ? { from: 'setup' } : undefined;
  readonly runtime = this.api.tenantRuntimeResource();
  readonly runtimeError = resourceErrorVm(this.runtime, 'admin.setup.runtime');
  readonly runtimeIsEmpty = () => false;

  languageLabel(code: string): string {
    return this.translate.instant(`admin.settings.runtime.languages.${code}`);
  }

  runtimeStatusTone(status: string): AdminStatusTone {
    const map: Record<string, AdminStatusTone> = {
      ACTIVE: 'success',
      DRAFT: 'neutral',
      SUSPENDED: 'warning',
      ARCHIVED: 'danger',
    };
    return map[status] ?? 'neutral';
  }
}
