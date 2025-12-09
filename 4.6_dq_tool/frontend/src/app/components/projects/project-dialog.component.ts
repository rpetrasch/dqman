
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatIconModule } from '@angular/material/icon';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DqProject } from '../../models/dq-project.model';
import { DqFlow } from '../../models/dq-flow.model';
import { DqFlowService } from '../../services/dq-flow.service';

@Component({
    selector: 'app-project-dialog',
    standalone: true,
    imports: [
        MatDialogModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule,
        MatDatepickerModule,
        MatNativeDateModule,
        MatIconModule,
        FormsModule,
        CommonModule
    ],
    templateUrl: './project-dialog.component.html',
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
    mat-form-field {
      margin-bottom: 8px;
    }
  `]
})
export class ProjectDialogComponent implements OnInit {
    project: DqProject;
    statusOptions = ['CREATED', 'STARTED', 'SUCCESS', 'FAILED'];
    flows: DqFlow[] = [];
    selectedFlowId: number | undefined;
    isEditMode: boolean;

    constructor(
        public dialogRef: MatDialogRef<ProjectDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: DqProject | null,
        private dqFlowService: DqFlowService
    ) {
        if (data) {
            this.project = { ...data };
            this.selectedFlowId = data.flow?.id;
            this.isEditMode = true;
        } else {
            this.project = {
                name: '',
                description: '',
                status: 'CREATED',
                createdDate: new Date().toISOString(),
                startedDate: new Date().toISOString(),
                finishedDate: ''
            };
            this.isEditMode = false;
        }
    }

    ngOnInit(): void {
        this.dqFlowService.getAllFlows().subscribe(data => {
            this.flows = data;
        });
    }

    onFlowChange(flowId: number): void {
        this.selectedFlowId = flowId;
        const selectedFlow = this.flows.find(f => f.id === flowId);
        if (selectedFlow) {
            this.project.flow = selectedFlow;
        }
    }

    onStatusChange(newStatus: string): void {
        this.project.status = newStatus;
        if (newStatus === 'SUCCESS' || newStatus === 'FAILED') {
            if (!this.project.startedDate) {
                this.project.startedDate = this.project.createdDate;
            }
            this.project.finishedDate = new Date().toISOString();
        }
    }

    onCancel(): void {
        this.dialogRef.close();
    }

    onSave(): void {
        // Ensure flow is set based on selection
        if (this.selectedFlowId) {
            const selectedFlow = this.flows.find(f => f.id === this.selectedFlowId);
            if (selectedFlow) {
                this.project.flow = selectedFlow;
            }
        } else {
            this.project.flow = undefined;
        }
        this.dialogRef.close(this.project);
    }
}
