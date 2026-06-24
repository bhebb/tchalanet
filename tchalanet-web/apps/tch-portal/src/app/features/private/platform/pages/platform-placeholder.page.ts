import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../shared/admin-ui/admin-page-shell.component';

@Component({
  selector: 'tch-platform-placeholder-page',
  imports: [TranslatePipe, AdminPageShellComponent, AdminEmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tch-admin-page-shell [title]="titleKey() | translate" [description]="descriptionKey() | translate">
      <tch-admin-empty-state
        [icon]="icon()"
        [title]="'platform.placeholder.title' | translate"
        [message]="'platform.placeholder.message' | translate"
      />
    </tch-admin-page-shell>
  `,
})
export class PlatformPlaceholderPage {
  private readonly route = inject(ActivatedRoute);

  readonly titleKey = computed(() => this.route.snapshot.data['titleKey'] ?? 'platform.placeholder.title');
  readonly descriptionKey = computed(() => this.route.snapshot.data['descriptionKey'] ?? 'platform.placeholder.description');
  readonly icon = computed(() => this.route.snapshot.data['icon'] ?? 'hourglass_empty');
}
