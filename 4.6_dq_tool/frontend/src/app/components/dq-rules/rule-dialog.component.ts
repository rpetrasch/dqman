
import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { DqRule, RuleTypeOptions } from '../../models/dq-rule.model';

@Component({
  selector: 'app-rule-dialog',
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
  templateUrl: './rule-dialog.component.html',
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
export class RuleDialogComponent {
  rule: DqRule;
  ruleTypeOptions = RuleTypeOptions;
  isEditMode: boolean;
  ruleTypeDefault = this.ruleTypeOptions[0];

  constructor(
    public dialogRef: MatDialogRef<RuleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DqRule | null
  ) {
    if (data) {
      this.rule = { ...data };
      // Ensure sourceTableFieldList is initialized
      if (!this.rule.sourceTableFieldList) {
        this.rule.sourceTableFieldList = [];
      }
      this.isEditMode = true;
    } else {
      this.rule = {
        name: '',
        description: '',
        ruleType: this.ruleTypeDefault,
        ruleValue: '',
        sourceTableFieldList: []
      };
      this.isEditMode = false;
    }
  }

  addSourceField(): void {
    if (!this.rule.sourceTableFieldList) {
      this.rule.sourceTableFieldList = [];
    }
    this.rule.sourceTableFieldList.push('');
  }

  removeSourceField(index: number): void {
    if (this.rule.sourceTableFieldList) {
      this.rule.sourceTableFieldList.splice(index, 1);
    }
  }

  trackByIndex(index: number): number {
    return index;
  }

  onRuleTypeChange(newType: string): void {
    this.rule.ruleType = newType;
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    this.dialogRef.close(this.rule);
  }
}
