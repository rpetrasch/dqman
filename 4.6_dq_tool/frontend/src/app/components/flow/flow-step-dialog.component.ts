
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DqFlowStep, FlowStepTypeOptions } from '../../models/dq-flow.model';

@Component({
  selector: 'app-flow-step-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    FormsModule,
    CommonModule
  ],
  template: `
    <h2 mat-dialog-title>{{ isEditMode ? 'Edit Step' : 'Add New Step' }}</h2>
    <mat-dialog-content>
      <div style="display: flex; flex-direction: column;">
        <mat-form-field style="width: 100%; margin-bottom: 8px;">
          <mat-label>Name</mat-label>
          <input matInput [(ngModel)]="step.name" required />
        </mat-form-field>

        <mat-form-field style="width: 100%; margin-bottom: 8px;">
            <mat-label>Type</mat-label>
            <mat-select [(ngModel)]="step.type">
                <mat-option *ngFor="let type of typeOptions" [value]="type">
                    {{ type }}
                </mat-option>
            </mat-select>
        </mat-form-field>

        <mat-form-field style="width: 100%; margin-bottom: 8px;">
          <mat-label>Description</mat-label>
          <textarea matInput [(ngModel)]="step.description"></textarea>
        </mat-form-field>
      </div>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" (click)="onSave()" [disabled]="!step.name">
        {{ isEditMode ? 'Update' : 'Add' }}
      </button>
    </mat-dialog-actions>
  `
})
export class FlowStepDialogComponent {
  step: DqFlowStep;
  typeOptions = FlowStepTypeOptions;
  isEditMode: boolean;

  constructor(
    public dialogRef: MatDialogRef<FlowStepDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DqFlowStep | null
  ) {
    if (data) {
      this.step = { ...data };
      this.isEditMode = true;
    } else {
      this.step = {
        name: '',
        description: '',
        type: this.typeOptions[0]
      };
      this.isEditMode = false;
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.step);
  }
}
