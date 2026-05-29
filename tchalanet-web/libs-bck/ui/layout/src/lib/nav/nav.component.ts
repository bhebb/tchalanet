import { TranslatePipe } from '@ngx-translate/core';
import { menu } from 'ionicons/icons';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';

import { TchLink } from '@tchl/types';

@Component({
  selector: 'tchl-nav',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    TranslatePipe,
    MatIconModule,
    MatMenuModule,
    RouterLinkActive,
    RouterLink,
  ],
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavComponent {
  items = input.required<TchLink[] | undefined>();
  currentPath = input<string>('/');
  selectNavItem = output<string>();
  protected readonly menu = menu;
  private readonly router = inject(Router);

  onSelect(path: string) {
    if (path && path !== this.currentPath()) {
      this.selectNavItem.emit(path);
    }
  }

  isActive(path: string): boolean {
    if (!path) return false;
    return this.router.isActive(path, {
      paths: path === '/' ? 'exact' : 'subset',
      queryParams: 'ignored',
      fragment: 'ignored',
      matrixParams: 'ignored',
    });
  }
}
