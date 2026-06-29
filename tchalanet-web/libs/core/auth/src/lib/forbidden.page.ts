import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';

import { LanguageSwitcher } from '@tch/core/i18n';

@Component({
  imports: [RouterLink, TranslatePipe, LanguageSwitcher],
  selector: 'tch-forbidden-page',
  templateUrl: './forbidden.page.html',
  styleUrl: './forbidden.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ForbiddenPage {}
