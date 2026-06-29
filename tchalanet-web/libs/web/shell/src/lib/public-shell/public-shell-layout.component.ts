import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ActionItem } from '@tch/api';
import { LabelPipe, PublicShellRuntime } from '@tch/page-model';
import { LanguageOption } from '@tch/ui/components';

import { ShellFeedbackOutletComponent } from '../feedback/shell-feedback-outlet.component';
import { PublicFooter } from './public-footer';
import { PublicHeader } from './public-header';

@Component({
  selector: 'tch-public-shell-layout',
  imports: [LabelPipe, PublicFooter, PublicHeader, ShellFeedbackOutletComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './public-shell-layout.component.html',
  styleUrl: './public-shell-layout.component.scss',
})
export class PublicShellLayoutComponent {
  readonly shell = input<PublicShellRuntime | undefined>();
  readonly languages = input<readonly LanguageOption[]>([]);
  readonly currentLanguage = input('');

  readonly languageSelected = output<string>();
  readonly loginRequested = output<ActionItem | undefined>();
}
