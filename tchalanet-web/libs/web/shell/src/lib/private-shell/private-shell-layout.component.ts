import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  model,
  output,
} from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';

import { ActionItem, NavigationSection } from '@tch/api';
import { TchBrand, TchSidebarNav, TchUserMenu } from '@tch/ui/components';

import { ShellFeedbackOutletComponent } from '../feedback/shell-feedback-outlet.component';
import { ShellFeedbackVerbosity } from '../feedback/shell-feedback.model';

@Component({
  selector: 'tch-private-shell-layout',
  imports: [ShellFeedbackOutletComponent, TchBrand, TchSidebarNav, TchUserMenu, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'closeDrawer()' },
  templateUrl: './private-shell-layout.component.html',
  styleUrl: './private-shell-layout.component.scss',
})
export class PrivateShellLayoutComponent {
  readonly brand = input.required<ActionItem>();
  readonly titleKey = input('');
  readonly primary = input<readonly ActionItem[]>([]);
  readonly sections = input<readonly NavigationSection[]>([]);
  readonly secondary = input<readonly ActionItem[]>([]);
  readonly userName = input('');
  readonly darkMode = input(false);
  readonly feedbackVerbosity = input<ShellFeedbackVerbosity>('standard');
  readonly drawerOpen = model(false);

  readonly themeToggled = output<void>();
  readonly profileRequested = output<void>();
  readonly settingsRequested = output<void>();
  readonly logoutRequested = output<void>();

  readonly themeIcon = computed(() => (this.darkMode() ? 'light_mode' : 'dark_mode'));

  toggleDrawer(): void {
    this.drawerOpen.update(open => !open);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }
}
