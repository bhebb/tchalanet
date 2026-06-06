import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { TchPageError } from '@tch/ui/components';

@Component({
  selector: 'tch-not-found-page',
  imports: [TchPageError, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <tch-page-error
      code="404"
      [title]="'common.notFound.title' | translate"
      [message]="'common.notFound.message' | translate"
      backRoute="/public"
      [backLabel]="'common.notFound.back' | translate"
    />
  `,
})
export class NotFoundPage {}
