import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { TchErrorPanel } from '@tch/ui/components';
import { AdminSectionCardComponent } from '../../../../shared/admin-ui/admin-section-card.component';
import { CreateSellerTerminalRequest, SellerTerminalApi } from '../../../seller-terminal-api.service';

@Component({
  selector: 'tch-create-seller-terminal-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminSectionCardComponent,
    TchErrorPanel,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './create-seller-terminal.dialog.html',
  styleUrls: ['./create-seller-terminal.dialog.scss'],
})
export class CreateSellerTerminalDialog {
  private readonly api = inject(SellerTerminalApi);
  private readonly dialogRef = inject(MatDialogRef<CreateSellerTerminalDialog>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    terminalCode: ['', [Validators.required, Validators.maxLength(64)]],
    displayName: ['', [Validators.required, Validators.maxLength(180)]],
    firstName: [''],
    lastName: [''],
    phoneNumber: [''],
    commissionRate: [null as number | null, [Validators.min(0), Validators.max(100)]],
    initialPin: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
  });

  submit(): void {
    if (this.form.invalid || this.saving()) return;

    const v = this.form.value;
    const req: CreateSellerTerminalRequest = {
      terminalCode: v.terminalCode!,
      displayName: v.displayName!,
      firstName: v.firstName || null,
      lastName: v.lastName || null,
      phoneNumber: v.phoneNumber || null,
      commissionRate: v.commissionRate ?? null,
      initialPin: v.initialPin!,
    };

    this.saving.set(true);
    this.error.set(null);

    this.api.create(req).subscribe({
      next: () => {
        this.saving.set(false);
        this.snackBar.open('Seller-terminal créé avec succès.', 'OK', { duration: 3000 });
        this.dialogRef.close({ reload: true });
      },
      error: (err: unknown) => {
        this.saving.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la création.');
      },
    });
  }
}
