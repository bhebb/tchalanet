import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';

@Component({
  selector: 'tch-platform-placeholder-page',
  imports: [TranslatePipe, AdminPageShellComponent, AdminEmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './platform-placeholder.page.html',
})
export class PlatformPlaceholderPage {
  private readonly route = inject(ActivatedRoute);

  readonly titleKey = computed(() => this.route.snapshot.data['titleKey'] ?? 'platform.placeholder.title');
  readonly descriptionKey = computed(() => this.route.snapshot.data['descriptionKey'] ?? 'platform.placeholder.description');
  readonly icon = computed(() => this.route.snapshot.data['icon'] ?? 'hourglass_empty');
}
