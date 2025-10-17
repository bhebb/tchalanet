import { TchLink } from '@tchl/types';
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe } from '@ngx-translate/core';

@Component({
  standalone: true,
  selector: 'tchl-sidebar-nav',
  imports: [CommonModule, RouterModule, MatIconModule, TranslatePipe],
  template: `
    <nav
      class="side"
      [class.mini]="mini()"
      role="navigation"
      [attr.aria-label]="'Navigation latérale' | translate"
    >
      <div class="header">
        <span class="ttl">{{ 'nav.menu' | translate }}</span>
        <button
          type="button"
          class="mini-toggle"
          (click)="toggleSideNav()"
          [attr.aria-label]="
            mini() ? ('Expand navigation' | translate) : ('Collapse navigation' | translate)
          "
        >
          <mat-icon>
            {{ mini() ? 'chevron_right' : 'chevron_left' }}
          </mat-icon>
        </button>
      </div>

      <div class="nav-list">
        @for (item of processedLinks(); track item._key) {
        <a
          class="nav-item"
          [class.active]="isItemActive(item)"
          [routerLink]="item.path"
          (click)="handleNavigation(item)"
        >
          @if (item.icon) {
          <mat-icon class="nav-icon">{{ item.icon }}</mat-icon>
          }
          <span class="nav-label">{{ item.labelKey | translate }}</span>

          @if (item.children && item.children.length) {
          <div class="nav-children">
            @for (child of item.children; track child.path) { @if (isChildAllowed(child)) {
            <a
              class="nav-child"
              [routerLink]="child.path"
              (click)="handleChildNavigation(child, $event)"
            >
              @if (child.icon) {
              <mat-icon class="child-icon">{{ child.icon }}</mat-icon>
              }
              <span class="child-label">
                {{ child.labelKey | translate }}
              </span>
            </a>
            } }
          </div>
          }
        </a>
        }
      </div>
    </nav>
  `,
  styleUrls: ['./sidenav.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarNavComponent {
  links = input.required<TchLink[]>();
  features = input<string[] | undefined>();
  navigation = input<(path: string) => void>();

  // États
  mini = signal(false);

  // Computed pour traiter les liens
  processedLinks = computed(() =>
    (this.links() || [])
      .map((l, i) => ({
        ...l,
        _key: l.path || l.labelKey || i,
        _visible: this.isLinkAllowed(l),
      }))
      .filter(l => l._visible),
  );

  // Méthodes de navigation et de filtrage
  isLinkAllowed = (link: TchLink) => !link.feature || this.features()?.includes(link.feature);

  isChildAllowed = (child: TchLink) => !child.feature || this.features()?.includes(child.feature);

  isItemActive = (item: TchLink) => {
    // Logique de détection de l'élément actif
    return false; // À implémenter avec la logique de routage
  };

  toggleSideNav() {
    this.mini.update(current => !current);
  }

  handleNavigation(item: TchLink) {
    if (item.path && this.navigation()) {
      // this.navigation.emit(item.path);
    }
  }

  handleChildNavigation(child: TchLink, event: Event) {
    event.stopPropagation();
    if (child.path && this.navigation()) {
      // this.navigation()(child.path);
    }
  }
}
