import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'tch-admin-detail-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-detail-layout.component.html',
  styleUrls: ['./admin-detail-layout.component.scss'],
})
export class AdminDetailLayoutComponent {}
