import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { DqRule } from '../dq-rule.model';
import { DqRuleService } from '../dq-rule.service';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dq-rules',
  imports: [MatTableModule, MatButtonModule, MatCardModule, MatFormFieldModule, MatInputModule, FormsModule, CommonModule],
  templateUrl: './dq-rules.html',
  styleUrl: './dq-rules.css'
})
export class DqRulesComponent implements OnInit {
  rules: DqRule[] = [];
  displayedColumns: string[] = ['name', 'description', 'ruleType', 'targetTable', 'actions'];

  newRule: DqRule = {
    name: '',
    description: '',
    ruleType: '',
    ruleValue: '',
    targetTable: '',
    targetColumn: ''
  };

  editingRule: DqRule | null = null;

  constructor(
    private dqRuleService: DqRuleService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.loadRules();
  }

  loadRules(): void {
    this.dqRuleService.getAllRules().subscribe((data: DqRule[]) => {
      this.rules = data;
      // Manually trigger change detection to avoid NG0100 error
      this.cdr.detectChanges();
    });
  }

  createRule(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    console.log('createRule called', this.newRule);

    this.dqRuleService.createRule(this.newRule).subscribe({
      next: () => {
        console.log('Rule created successfully');
        this.resetForm();
        this.cdr.detectChanges();
        this.loadRules();
      },
      error: (err) => {
        console.error('Error creating rule:', err);
      }
    });
  }

  editRule(rule: DqRule): void {
    this.editingRule = { ...rule };
  }

  updateRule(): void {
    if (this.editingRule && this.editingRule.id) {
      this.dqRuleService.updateRule(this.editingRule.id, this.editingRule).subscribe(() => {
        this.loadRules();
        this.editingRule = null;
      });
    }
  }

  deleteRule(id: number | undefined): void {
    if (id) {
      this.dqRuleService.deleteRule(id).subscribe(() => {
        this.loadRules();
      });
    }
  }

  resetForm(): void {
    this.newRule = {
      name: '',
      description: '',
      ruleType: '',
      ruleValue: '',
      targetTable: '',
      targetColumn: ''
    };
  }

  cancelEdit(): void {
    this.editingRule = null;
  }
}
