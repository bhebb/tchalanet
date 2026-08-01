import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Shared two-column layout for routed admin forms.
 *
 * The form element remains owned by the feature so validation and submit
 * semantics stay local. This component only standardizes the main field
 * column, optional preview/summary rail, and action footer.
 */
@Component({
  selector: 'tch-admin-form-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-form-layout.component.html',
  styleUrls: ['./admin-form-layout.component.scss'],
})
export class AdminFormLayoutComponent {}
