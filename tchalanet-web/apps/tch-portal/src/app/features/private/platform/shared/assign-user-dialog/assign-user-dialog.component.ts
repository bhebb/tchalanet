import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';

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
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './assign-user-dialog.component.html',
})
export class AssignUserDialog implements OnInit {
  private readonly dialogRef = inject(MatDialogRef<AssignUserDialog, AssignUserResult>);
  private readonly identityApi = inject(IdentityUserCrudApi);
  private readonly destroyRef = inject(DestroyRef);

  readonly search = new FormControl('');
  readonly options = signal<IdentityUserView[]>([]);
  readonly loading = signal(false);
  readonly selected = signal<IdentityUserView | null>(null);

  ngOnInit(): void {
    this.search.valueChanges.pipe(
      debounceTime(250),
      distinctUntilChanged(),
      switchMap(q => {
        if (typeof q !== 'string') return [];
        this.loading.set(true);
        this.selected.set(null);
        return this.identityApi.searchUnassigned(q);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: users => {
        this.options.set(users);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  displayFn(user: IdentityUserView | string | null): string {
    if (!user || typeof user === 'string') return user ?? '';
    return user.displayName || user.email || user.id;
  }

  select(user: IdentityUserView): void {
    this.selected.set(user);
  }

  confirm(): void {
    const u = this.selected();
    if (!u) return;
    this.dialogRef.close({ userId: u.id, displayName: u.displayName, email: u.email });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
