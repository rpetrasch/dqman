import { Component, OnInit, ChangeDetectorRef, ViewChild, AfterViewInit } from '@angular/core';
import { DqRule } from '../../models/dq-rule.model';
import { DqRuleService } from '../../services/dq-rule.service';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { RuleDialogComponent } from './rule-dialog.component';
import { ConfirmationDialogComponent } from '../shared/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-dq-rules',
  standalone: true,
  imports: [
    MatTableModule,
    MatSortModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    CommonModule,
    MatDialogModule,
    MatIconModule
  ],
  templateUrl: './dq-rules.html',
  styleUrl: './dq-rules.css'
})
export class DqRulesComponent implements OnInit, AfterViewInit {
  dataSource = new MatTableDataSource<DqRule>([]);
  displayedColumns: string[] = ['id', 'name', 'description', 'ruleType', 'sourceFields', 'actions'];

  @ViewChild(MatSort) sort!: MatSort;

  // Filter values
  globalFilter = '';
  columnFilters = {
    id: '',
    name: '',
    description: '',
    ruleType: '',
    sourceFields: ''
  };

  constructor(
    private dqRuleService: DqRuleService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.dataSource.filterPredicate = this.createFilter();
    this.loadRules();
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
  }

  loadRules(): void {
    this.dqRuleService.getAllRules().subscribe((data: DqRule[]) => {
      this.dataSource.data = data;
      this.cdr.detectChanges();
    });
  }

  createFilter(): (data: DqRule, filter: string) => boolean {
    return (data: DqRule, filter: string): boolean => {
      const searchTerms = JSON.parse(filter);

      // Convert sourceTableFieldList to searchable string
      const sourceFieldsStr = data.sourceTableFieldList?.join(', ').toLowerCase() || '';

      // Global Filter
      const globalMatch = !searchTerms.global ||
        (data.id?.toString().toLowerCase().includes(searchTerms.global) ||
          data.name.toLowerCase().includes(searchTerms.global) ||
          data.description?.toLowerCase().includes(searchTerms.global) ||
          data.ruleType?.toLowerCase().includes(searchTerms.global) ||
          sourceFieldsStr.includes(searchTerms.global));

      // Column Filters
      const idMatch = !searchTerms.id || data.id?.toString().toLowerCase().includes(searchTerms.id);
      const nameMatch = !searchTerms.name || data.name.toLowerCase().includes(searchTerms.name);
      const descMatch = !searchTerms.description || data.description?.toLowerCase().includes(searchTerms.description);
      const typeMatch = !searchTerms.ruleType || data.ruleType?.toLowerCase().includes(searchTerms.ruleType);
      const sourceFieldsMatch = !searchTerms.sourceFields || sourceFieldsStr.includes(searchTerms.sourceFields);

      var result = globalMatch && idMatch && nameMatch && descMatch && typeMatch && sourceFieldsMatch;
      if (result === undefined) {
        result = true;
      }
      return result;
    };
  }

  applyGlobalFilter(event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    this.globalFilter = filterValue.trim().toLowerCase();
    this.updateFilter();
  }

  applyColumnFilter(column: string, event: Event) {
    const filterValue = (event.target as HTMLInputElement).value;
    // @ts-ignore
    this.columnFilters[column] = filterValue.trim().toLowerCase();
    this.updateFilter();
  }

  updateFilter() {
    this.dataSource.filter = JSON.stringify({
      global: this.globalFilter,
      ...this.columnFilters
    });
  }

  openRuleDialog(rule?: DqRule): void {
    const dialogRef = this.dialog.open(RuleDialogComponent, {
      width: '800px',
      data: rule || null
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        if (rule && rule.id) {
          this.updateRule(rule.id, result);
        } else {
          this.createRule(result);
        }
      }
    });
  }

  createRule(rule: DqRule): void {
    this.dqRuleService.createRule(rule).subscribe({
      next: () => {
        this.loadRules();
      },
      error: (err) => {
        console.error('Error creating rule:', err);
      }
    });
  }

  updateRule(id: number, rule: DqRule): void {
    this.dqRuleService.updateRule(id, rule).subscribe(() => {
      this.loadRules();
    });
  }

  deleteRule(id: number | undefined): void {
    if (id) {
      const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
        width: '350px',
        data: { title: 'Delete Rule', message: 'Are you sure you want to delete this rule?' }
      });

      dialogRef.afterClosed().subscribe(result => {
        if (result) {
          this.dqRuleService.deleteRule(id).subscribe(() => {
            this.loadRules();
          });
        }
      });
    }
  }
}
