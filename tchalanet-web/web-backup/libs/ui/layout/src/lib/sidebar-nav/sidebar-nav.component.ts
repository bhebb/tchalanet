import { TranslatePipe } from '@ngx-translate/core';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { TchLink } from '@tchl/types';

@Component({
  selector: 'tchl-sidebar-nav',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, TranslatePipe],
  templateUrl: './sidebar-nav.component.html',
  styleUrls: ['./sidebar-nav.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarNavComponent {
  router = inject(Router);

  links = input.required<TchLink[]>();
  features = input<string[] | undefined>();

  mini = signal(false); // mode "mini rail" si tu veux plus tard

  processedLinks = computed(() =>
    (this.links() || [])
      .map((l, i) => ({
        ...l,
        _key: l.path || l.labelKey || i,
        _visible: this.isAllowed(l),
      }))
      .filter(l => l._visible),
  );

  toggleMini() {
    this.mini.update(v => !v);
  }

  isAllowed(link: TchLink) {
    return !link.flag || this.features()?.includes(link.flag);
  }

  isActive(link: TchLink) {
    if (!link.path) return false;
    return this.router.isActive(link.path, {
      paths: 'subset',
      queryParams: 'ignored',
      fragment: 'ignored',
      matrixParams: 'ignored',
    });
  }

  go(path?: string) {
    if (path) {
      this.router.navigateByUrl(path);
    }
  }
}
