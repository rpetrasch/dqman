
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { DqIntegration, IntegrationTypeOptions } from '../../models/dq-integration.model';

@Component({
  selector: 'app-integration-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    FormsModule,
    CommonModule
  ],
  templateUrl: './integration-dialog.component.html',
  styles: [`
    .full-width {
      width: 100%;
    }
    .form-row {
      display: flex;
      gap: 16px;
    }
    .half-width {
      flex: 1;
    }
    .quarter-width {
      width: 25%;
    }
    .three-quarter-width {
      width: 75%;
    }
    mat-form-field {
      margin-bottom: 8px;
    }
  `]
})
export class IntegrationDialogComponent {
  integration: DqIntegration;
  typeOptions = IntegrationTypeOptions;
  isEditMode: boolean;
  typeDefault = this.typeOptions[0];

  constructor(
    public dialogRef: MatDialogRef<IntegrationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DqIntegration | null
  ) {
    if (data) {
      this.integration = { ...data };
      this.isEditMode = true;
    } else {
      this.integration = {
        name: '',
        description: '',
        type: this.typeDefault,
        url: '',
        user: '',
        password: ''
      };
      this.isEditMode = false;
    }
  }

  onTypeChange(newType: string): void {
    this.integration.type = newType;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.integration);
  }
}
