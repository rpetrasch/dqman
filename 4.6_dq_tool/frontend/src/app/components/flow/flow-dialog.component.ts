
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DqFlow, DqFlowStep, FlowStatusOptions } from '../../models/dq-flow.model';
import { FlowStepDialogComponent } from './flow-step-dialog.component';

@Component({
    selector: 'app-flow-dialog',
    standalone: true,
    imports: [
        MatDialogModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatTableModule,
        MatIconModule,
        FormsModule,
        CommonModule
    ],
    templateUrl: './flow-dialog.component.html',
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
    .steps-container {
        margin-top: 16px;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        padding: 8px;
    }
  `]
})
export class FlowDialogComponent {
    flow: DqFlow;
    statusOptions = FlowStatusOptions;
    isEditMode: boolean;

    stepsDataSource = new MatTableDataSource<DqFlowStep>([]);
    displayedStepColumns: string[] = ['name', 'type', 'description', 'actions'];

    constructor(
        public dialogRef: MatDialogRef<FlowDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: DqFlow | null,
        private dialog: MatDialog
    ) {
        if (data) {
            this.flow = JSON.parse(JSON.stringify(data)); // Deep copy to avoid mutating original steps array
            this.isEditMode = true;
        } else {
            this.flow = {
                name: '',
                description: '',
                status: 'CREATED',
                steps: [],
                createdDate: new Date().toISOString(),
                modifiedDate: new Date().toISOString()
            };
            this.isEditMode = false;
        }
        this.stepsDataSource.data = this.flow.steps;
    }

    onCancel(): void {
        this.dialogRef.close();
    }

    onSave(): void {
        this.flow.modifiedDate = new Date().toISOString();
        this.dialogRef.close(this.flow);
    }

    addStep(): void {
        const dialogRef = this.dialog.open(FlowStepDialogComponent, {
            width: '500px'
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.flow.steps.push(result);
                this.refreshSteps();
            }
        });
    }

    editStep(step: DqFlowStep, index: number): void {
        const dialogRef = this.dialog.open(FlowStepDialogComponent, {
            width: '500px',
            data: step
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result) {
                this.flow.steps[index] = result;
                this.refreshSteps();
            }
        });
    }

    deleteStep(index: number): void {
        this.flow.steps.splice(index, 1);
        this.refreshSteps();
    }

    refreshSteps() {
        this.stepsDataSource.data = [...this.flow.steps];
    }
}
