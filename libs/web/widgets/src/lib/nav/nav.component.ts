import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { TranslatePipe } from '@ngx-translate/core';
import { TchLink } from '@tchl/types';
import { menu } from 'ionicons/icons';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'tchl-nav',
  standalone: true,
  imports: [CommonModule, MatButtonModule, TranslatePipe, MatIconModule, MatMenuModule],
  templateUrl: './nav.component.html',
  styleUrls: ['./nav.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavComponent {
  protected readonly menu = menu;
  items = input.required<TchLink[] | undefined>();
  currentPath = input<string>('/');

  select = output<string>();

  onSelect(path: string) {
    console.log('onSelect', path);
    if (path && path !== this.currentPath()) {
      this.select.emit(path);
    }
  }
}
