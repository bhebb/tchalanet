import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { Observable, map } from 'rxjs';

import { IdentityUserCrudApi, IdentityUserView } from '../identity-user-crud-api.service';

export interface AssignUserResult {
  userId: string;
  displayName: string | null;
  email: string | null;
}

@Component({
  selector: 'tch-assign-user-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatDialogModule,
    MatButtonModule,
    TchSearchSelect,
  ],
  templateUrl: './assign-user-dialog.component.html',
})
export class AssignUserDialog {
  private readonly dialogRef = inject(MatDialogRef<AssignUserDialog, AssignUserResult>);
  private readonly identityApi = inject(IdentityUserCrudApi);

  readonly selected = signal<TchSearchOption<IdentityUserView> | null>(null);

  readonly searchUsers = (query: string): Observable<readonly TchSearchOption<IdentityUserView>[]> =>
    this.identityApi.searchUnassigned(query).pipe(
      map(users => users.map(user => this.toUserOption(user))),
    );

  select(user: TchSearchOption | null): void {
    this.selected.set(user as TchSearchOption<IdentityUserView> | null);
  }

  confirm(): void {
    const u = this.selected()?.data;
    if (!u) return;
    this.dialogRef.close({ userId: u.id, displayName: u.displayName, email: u.email });
  }

  cancel(): void {
    this.dialogRef.close();
  }

  private toUserOption(user: IdentityUserView): TchSearchOption<IdentityUserView> {
    return {
      id: user.id,
      title: user.displayName || user.email || user.id,
      subtitle: user.email,
      badge: user.status,
      icon: 'person',
      data: user,
    };
  }
}
