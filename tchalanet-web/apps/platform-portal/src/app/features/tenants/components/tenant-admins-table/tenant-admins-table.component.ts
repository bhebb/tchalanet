import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { TenantAdminView } from '../../data-access/platform-tenants-api.service';

@Component({
  selector: 'tch-tenant-admins-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslatePipe, DatePipe, MatTableModule],
  templateUrl: './tenant-admins-table.component.html',
  styleUrls: ['./tenant-admins-table.component.scss'],
})
export class TenantAdminsTableComponent {
  private readonly translate = inject(TranslateService);

  readonly admins = input<readonly TenantAdminView[]>([]);

  readonly columns = ['displayName', 'email', 'status', 'createdAt'];

  adminStatusLabel(status: string): string {
    const key = `platform.tenants.adminStatus.${status.toLowerCase()}`;
    const translated = this.translate.instant(key);
    return translated === key ? status : translated;
  }
}
