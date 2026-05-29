import { TranslatePipe } from '@ngx-translate/core';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

import { TchLink } from '@tchl/types';

@Component({
  selector: 'tchl-overlay-nav',
  standalone: true,
  imports: [CommonModule, RouterModule, MatIconModule, TranslatePipe],
  templateUrl: './overlay-nav.component.html',
  styleUrls: ['./overlay-nav.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OverlayNavComponent {
  open = input(false);
  items = input<TchLink[]>([]);
  requestClose = output<void>();
  private readonly router = inject(Router);

  constructor() {
    // lock scroll when open
    effect(() => {
      const active = this.open();
      document.documentElement.classList.toggle('menu-open', active);
      document.documentElement.classList.toggle('no-scroll', active);
      document.body.classList.toggle('no-scroll', active);
    });
  }

  onClose() {
    this.requestClose.emit();
  }

  handleNav(path?: string) {
    if (path) {
      this.router.navigateByUrl(path);
    }
    this.onClose();
  }
}
