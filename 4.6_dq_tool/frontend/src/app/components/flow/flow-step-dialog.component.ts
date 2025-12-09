
import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DqFlowStep, FlowStepTypeOptions } from '../../models/dq-flow.model';
import { DataIntegrationService } from '../../services/data-integration.service';
import { DqRuleService } from '../../services/dq-rule.service';
import { DataIntegration } from '../../models/data-integration.model';
import { DqRule } from '../../models/dq-rule.model';

@Component({
  selector: 'app-flow-step-dialog',
  standalone: true,
  imports: [
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
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
            <mat-select [(ngModel)]="step.type" (selectionChange)="onTypeChange()">
                <mat-option *ngFor="let type of typeOptions" [value]="type">
                    {{ type }}
                </mat-option>
            </mat-select>
        </mat-form-field>

        <!-- Show Integration selector for DATA_SOURCE or DATA_SINK -->
        <mat-form-field *ngIf="step.type === 'DATA SOURCE' || step.type === 'DATA SINK'" 
                        style="width: 100%; margin-bottom: 8px;">
          <mat-label>Integration</mat-label>
          <mat-select [(ngModel)]="selectedIntegrationId">
            <mat-option *ngFor="let integration of integrations" [value]="integration.id">
              {{ integration.name }} ({{ integration.type }})
            </mat-option>
          </mat-select>
        </mat-form-field>

        <!-- Show DQ Rule selector for DQ_RULE -->
        <mat-form-field *ngIf="step.type === 'DQ RULE'" 
                        style="width: 100%; margin-bottom: 8px;">
          <mat-label>DQ Rule</mat-label>
          <mat-select [(ngModel)]="selectedRuleId">
            <mat-option *ngFor="let rule of rules" [value]="rule.id">
              {{ rule.name }} ({{ rule.ruleType }})
            </mat-option>
          </mat-select>
        </mat-form-field>

        <mat-form-field style="width: 100%; margin-bottom: 8px;">
          <mat-label>Description</mat-label>
          <textarea matInput [(ngModel)]="step.description"></textarea>
        </mat-form-field>

        <!-- Step Position Flags -->
        <div style="display: flex; gap: 16px; margin-bottom: 8px;">
          <mat-checkbox [(ngModel)]="step.isInitial">
            Initial Step (no input)
          </mat-checkbox>
          <mat-checkbox [(ngModel)]="step.isFinal">
            Final Step (no output)
          </mat-checkbox>
        </div>
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
export class FlowStepDialogComponent implements OnInit {
  step: DqFlowStep;
  typeOptions = FlowStepTypeOptions;
  isEditMode: boolean;
  integrations: DataIntegration[] = [];
  rules: DqRule[] = [];
  selectedIntegrationId: number | undefined;
  selectedRuleId: number | undefined;

  constructor(
    public dialogRef: MatDialogRef<FlowStepDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DqFlowStep | null,
    private integrationService: DataIntegrationService,
    private ruleService: DqRuleService
  ) {
    if (data) {
      this.step = { ...data };
      this.isEditMode = true;

      // Extract integrationId and ruleId from the step
      // First try direct ID fields (preferred), then fall back to nested objects
      if ((data as any).integrationId) {
        this.selectedIntegrationId = (data as any).integrationId;
      } else if ((data as any).integration) {
        this.selectedIntegrationId = (data as any).integration.id;
      }

      if ((data as any).ruleId) {
        this.selectedRuleId = (data as any).ruleId;
      } else if ((data as any).rule) {
        this.selectedRuleId = (data as any).rule.id;
      }
    } else {
      this.step = {
        name: '',
        description: '',
        type: this.typeOptions[0],
        isInitial: false,
        isFinal: false
      };
      this.isEditMode = false;
    }
  }

  ngOnInit(): void {
    // Load integrations and rules
    this.integrationService.getAllIntegrations().subscribe(data => {
      this.integrations = data;
    });

    this.ruleService.getAllRules().subscribe(data => {
      this.rules = data;
    });
  }

  onTypeChange(): void {
    // Reset selections when type changes
    this.selectedIntegrationId = undefined;
    this.selectedRuleId = undefined;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    // Add the selected integration or rule ID to the step data
    const stepData = {
      ...this.step,
      integrationId: this.selectedIntegrationId,
      ruleId: this.selectedRuleId
    };
    this.dialogRef.close(stepData);
  }
}
