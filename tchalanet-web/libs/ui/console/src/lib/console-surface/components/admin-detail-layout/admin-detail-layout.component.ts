import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'tch-admin-detail-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-detail-layout.component.html',
  styleUrls: ['./admin-detail-layout.component.scss'],
})
export class AdminDetailLayoutComponent {
  /** Use when the summary belongs to the document flow instead of a desktop right rail. */
  readonly singleColumn = input(false);
}
